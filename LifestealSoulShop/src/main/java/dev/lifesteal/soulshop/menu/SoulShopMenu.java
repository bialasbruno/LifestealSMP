package dev.lifesteal.soulshop.menu;

import dev.lifesteal.soulshop.config.SoulShopSettings;
import dev.lifesteal.soulshop.message.MessageService;
import dev.lifesteal.soulshop.purchase.PurchaseClickGuard;
import dev.lifesteal.soulshop.purchase.ShopPurchaseService;
import dev.lifesteal.souls.api.LifestealSoulsApi;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Three-row, read-only shop menu with one deliberately simple launch product. */
public final class SoulShopMenu implements Listener {

    private static final int INVENTORY_SIZE = 27;
    private static final int BALANCE_SLOT = 4;
    private static final int PRODUCT_SLOT = 13;
    private static final int CLOSE_SLOT = 22;

    private final Plugin plugin;
    private final LifestealSoulsApi soulsApi;
    private final ShopPurchaseService purchases;
    private final MessageService messages;
    private final Supplier<SoulShopSettings> settingsSupplier;
    private final PurchaseClickGuard clickGuard = new PurchaseClickGuard();

    public SoulShopMenu(
            Plugin plugin,
            LifestealSoulsApi soulsApi,
            MessageService messages,
            Supplier<SoulShopSettings> settingsSupplier) {
        this.plugin = plugin;
        this.soulsApi = soulsApi;
        this.purchases = new ShopPurchaseService(soulsApi);
        this.messages = messages;
        this.settingsSupplier = settingsSupplier;
    }

    public void open(Player player) {
        SoulShopSettings settings = settingsSupplier.get();
        ShopHolder holder = new ShopHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(
                holder, INVENTORY_SIZE, messages.render(settings.menuTitle(), Map.of()));
        holder.inventory = inventory;
        render(player, inventory, settings);
        player.openInventory(inventory);
    }

    public void refreshOpenMenus() {
        SoulShopSettings settings = settingsSupplier.get();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Inventory inventory = player.getOpenInventory().getTopInventory();
            if (inventory.getHolder() instanceof ShopHolder holder
                    && holder.playerId.equals(player.getUniqueId())) {
                render(player, inventory, settings);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof ShopHolder holder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)
                || !holder.playerId.equals(player.getUniqueId())
                || event.getClickedInventory() != topInventory) {
            return;
        }

        if (event.getRawSlot() == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }
        if (event.getRawSlot() != PRODUCT_SLOT
                || (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT)
                || !clickGuard.tryAcquire(player.getUniqueId(), System.nanoTime())) {
            return;
        }

        purchase(player, topInventory);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof ShopHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof ShopHolder
                && event.getPlayer() instanceof Player player) {
            clickGuard.release(player.getUniqueId());
        }
    }

    private void purchase(Player player, Inventory menuInventory) {
        SoulShopSettings settings = settingsSupplier.get();
        ItemStack reward = new ItemStack(settings.productMaterial(), settings.productAmount());

        try {
            ShopPurchaseService.Result result = purchases.purchase(
                    player.getUniqueId(),
                    settings.productPrice(),
                    canFit(player.getInventory().getStorageContents(), reward),
                    () -> deliver(player, reward));

            switch (result) {
                case SUCCESS -> purchaseSucceeded(player, menuInventory, settings);
                case INSUFFICIENT_SOULS -> purchaseFailed(
                        player,
                        settings,
                        settings.insufficientSoulsMessage(),
                        Map.of(
                                "price", Long.toString(settings.productPrice()),
                                "balance", Long.toString(soulsApi.getSouls(player.getUniqueId()))));
                case INVENTORY_FULL -> purchaseFailed(
                        player, settings, settings.inventoryFullMessage(), Map.of());
            }
        } catch (RuntimeException exception) {
            plugin.getLogger().severe("Purchase failed for " + player.getUniqueId() + ": "
                    + exception.getMessage());
            purchaseFailed(player, settings, settings.purchaseErrorMessage(), Map.of());
        }
    }

    private void purchaseSucceeded(
            Player player, Inventory menuInventory, SoulShopSettings settings) {
        long balance = soulsApi.getSouls(player.getUniqueId());
        messages.sendActionBar(
                player,
                settings.successMessage(),
                Map.of(
                        "price", Long.toString(settings.productPrice()),
                        "balance", Long.toString(balance)));
        playSound(
                player,
                settings.successSound(),
                settings.successSoundVolume(),
                settings.successSoundPitch());
        render(player, menuInventory, settings);
    }

    private void purchaseFailed(
            Player player,
            SoulShopSettings settings,
            String template,
            Map<String, String> replacements) {
        messages.sendActionBar(player, template, replacements);
        playSound(
                player,
                settings.failureSound(),
                settings.failureSoundVolume(),
                settings.failureSoundPitch());
    }

    private void render(Player player, Inventory inventory, SoulShopSettings settings) {
        ItemStack filler = createFiller(settings);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
        inventory.setItem(BALANCE_SLOT, createBalanceItem(player, settings));
        inventory.setItem(PRODUCT_SLOT, createProductIcon(settings));
        inventory.setItem(CLOSE_SLOT, createCloseItem(settings));
    }

    private ItemStack createFiller(SoulShopSettings settings) {
        ItemStack item = new ItemStack(settings.fillerMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBalanceItem(Player player, SoulShopSettings settings) {
        long balance = soulsApi.getSouls(player.getUniqueId());
        Map<String, String> replacements = Map.of("balance", Long.toString(balance));
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(player);
        meta.displayName(messages.renderItemText(settings.balanceName(), replacements));
        meta.lore(messages.renderItemLore(settings.balanceLore(), replacements));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createProductIcon(SoulShopSettings settings) {
        Map<String, String> replacements = Map.of(
                "price", Long.toString(settings.productPrice()),
                "amount", Integer.toString(settings.productAmount()));
        ItemStack item = new ItemStack(settings.productMaterial(), settings.productAmount());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.renderItemText(settings.productName(), replacements));
        meta.lore(messages.renderItemLore(settings.productLore(), replacements));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCloseItem(SoulShopSettings settings) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.renderItemText(settings.closeName(), Map.of()));
        item.setItemMeta(meta);
        return item;
    }

    private void deliver(Player player, ItemStack reward) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(reward);
        if (leftovers.isEmpty()) {
            return;
        }

        plugin.getLogger().warning("Inventory changed during purchase for "
                + player.getUniqueId() + "; dropping the paid item at the player's location.");
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    static boolean canFit(ItemStack[] contents, ItemStack item) {
        int remaining = item.getAmount();
        int maximumStackSize = item.getMaxStackSize();
        for (ItemStack existing : contents) {
            if (existing == null || existing.getType().isAir()) {
                remaining -= maximumStackSize;
            } else if (existing.isSimilar(item)) {
                remaining -= Math.max(0, maximumStackSize - existing.getAmount());
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    private void playSound(Player player, String sound, float volume, float pitch) {
        try {
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Could not play sound '" + sound + "': "
                    + exception.getMessage());
        }
    }

    private static final class ShopHolder implements InventoryHolder {

        private final UUID playerId;
        private Inventory inventory;

        private ShopHolder(UUID playerId) {
            this.playerId = playerId;
        }

        @Override
        public @NotNull Inventory getInventory() {
            if (inventory == null) {
                throw new IllegalStateException("SoulShop inventory is not initialized");
            }
            return inventory;
        }
    }
}
