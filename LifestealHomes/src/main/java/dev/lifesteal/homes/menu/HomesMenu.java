package dev.lifesteal.homes.menu;

import dev.lifesteal.homes.config.HomesSettings;
import dev.lifesteal.homes.data.HomeRepository;
import dev.lifesteal.homes.data.StoredHome;
import dev.lifesteal.homes.message.MessageService;
import dev.lifesteal.homes.rules.HomeLimitRules;
import dev.lifesteal.homes.teleport.HomeTeleportService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class HomesMenu implements Listener {

    private static final int SIZE = 54;
    private static final int HOMES_PER_PAGE = 36;
    private static final int PROFILE_SLOT = 4;
    private static final int PREVIOUS_SLOT = 48;
    private static final int CLOSE_SLOT = 49;
    private static final int NEXT_SLOT = 50;
    private static final int CONFIRM_SIZE = 27;
    private static final int CONFIRM_ACCEPT_SLOT = 11;
    private static final int CONFIRM_HOME_SLOT = 13;
    private static final int CONFIRM_CANCEL_SLOT = 15;

    private final HomeRepository repository;
    private final HomeTeleportService teleports;
    private final MessageService messages;
    private final Supplier<HomesSettings> settingsSupplier;

    public HomesMenu(
            HomeRepository repository,
            HomeTeleportService teleports,
            MessageService messages,
            Supplier<HomesSettings> settingsSupplier) {
        this.repository = repository;
        this.teleports = teleports;
        this.messages = messages;
        this.settingsSupplier = settingsSupplier;
    }

    public void open(Player player, int requestedPage) {
        HomesSettings settings = settingsSupplier.get();
        List<StoredHome> homes = repository.findAll(player.getUniqueId());
        int limit = resolveLimit(player, settings);
        int pages = pageCount(homes.size(), limit);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));

        MainHolder holder = new MainHolder(player.getUniqueId(), page, pages);
        Inventory inventory = Bukkit.createInventory(
                holder, SIZE, messages.render(settings.menu().title(), Map.of()));
        holder.inventory = inventory;
        fill(inventory, settings.menu().fillerMaterial());

        inventory.setItem(PROFILE_SLOT, profileItem(player, homes.size(), limit, settings.menu()));
        renderHomeArea(inventory, holder, homes, limit, page, settings.menu());
        inventory.setItem(CLOSE_SLOT, namedItem(Material.BARRIER, settings.menu().close(), List.of(), Map.of()));
        if (page > 0) {
            inventory.setItem(PREVIOUS_SLOT, namedItem(
                    Material.ARROW, settings.menu().previousPage(), List.of(), Map.of()));
        }
        if (page + 1 < pages) {
            inventory.setItem(NEXT_SLOT, namedItem(
                    Material.ARROW, settings.menu().nextPage(), List.of(), Map.of()));
        }

        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof MainHolder holder) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)
                    || !holder.playerId.equals(player.getUniqueId())
                    || event.getClickedInventory() != top) {
                return;
            }
            handleMainClick(player, holder, event);
            return;
        }
        if (top.getHolder() instanceof ConfirmHolder holder) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)
                    || !holder.playerId.equals(player.getUniqueId())
                    || event.getClickedInventory() != top) {
                return;
            }
            handleConfirmClick(player, holder, event.getRawSlot());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof MainHolder || holder instanceof ConfirmHolder) {
            event.setCancelled(true);
        }
    }

    private void handleMainClick(Player player, MainHolder holder, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }
        if (slot == PREVIOUS_SLOT && holder.page > 0) {
            open(player, holder.page - 1);
            return;
        }
        if (slot == NEXT_SLOT && holder.page + 1 < holder.pages) {
            open(player, holder.page + 1);
            return;
        }

        StoredHome home = holder.homesBySlot.get(slot);
        if (home == null) {
            return;
        }
        if (event.isShiftClick() && event.getClick() == ClickType.SHIFT_RIGHT) {
            if (!player.hasPermission("lifestealhomes.delete")) {
                messages.send(player, settingsSupplier.get().message("no-permission"));
                return;
            }
            openConfirmation(player, home, holder.page);
        } else if (event.getClick() == ClickType.LEFT) {
            player.closeInventory();
            teleports.start(player, home);
        }
    }

    private void handleConfirmClick(Player player, ConfirmHolder holder, int slot) {
        if (slot == CONFIRM_CANCEL_SLOT) {
            open(player, holder.returnPage);
            return;
        }
        if (slot != CONFIRM_ACCEPT_SLOT) {
            return;
        }

        repository.find(player.getUniqueId(), holder.homeKey).ifPresentOrElse(home -> {
            repository.delete(player.getUniqueId(), home.key());
            messages.send(
                    player,
                    settingsSupplier.get().message("home-deleted"),
                    Map.of("home", home.name()));
        }, () -> messages.send(
                player,
                settingsSupplier.get().message("home-not-found"),
                Map.of("home", holder.homeName)));
        open(player, holder.returnPage);
    }

    private void openConfirmation(Player player, StoredHome home, int returnPage) {
        HomesSettings settings = settingsSupplier.get();
        Map<String, String> replacements = Map.of("home", home.name());
        ConfirmHolder holder = new ConfirmHolder(
                player.getUniqueId(), home.key(), home.name(), returnPage);
        Inventory inventory = Bukkit.createInventory(
                holder,
                CONFIRM_SIZE,
                messages.render(settings.menu().confirmTitle(), replacements));
        holder.inventory = inventory;
        fill(inventory, settings.menu().fillerMaterial());
        inventory.setItem(CONFIRM_ACCEPT_SLOT, namedItem(
                Material.RED_CONCRETE,
                settings.menu().confirmAccept(),
                settings.menu().confirmAcceptLore(),
                replacements));
        inventory.setItem(CONFIRM_HOME_SLOT, homeItem(home, settings.menu()));
        inventory.setItem(CONFIRM_CANCEL_SLOT, namedItem(
                Material.LIME_CONCRETE,
                settings.menu().confirmCancel(),
                List.of(),
                replacements));
        player.openInventory(inventory);
    }

    private void renderHomeArea(
            Inventory inventory,
            MainHolder holder,
            List<StoredHome> homes,
            int limit,
            int page,
            HomesSettings.Menu menu) {
        int offset = page * HOMES_PER_PAGE;
        for (int localIndex = 0; localIndex < HOMES_PER_PAGE; localIndex++) {
            int absoluteIndex = offset + localIndex;
            int slot = 9 + localIndex;
            if (absoluteIndex < homes.size()) {
                StoredHome home = homes.get(absoluteIndex);
                inventory.setItem(slot, homeItem(home, menu));
                holder.homesBySlot.put(slot, home);
            } else if (limit == HomeLimitRules.UNLIMITED || absoluteIndex < limit) {
                inventory.setItem(slot, namedItem(
                        menu.availableMaterial(), menu.availableName(), menu.availableLore(), Map.of()));
            } else if (absoluteIndex == limit) {
                inventory.setItem(slot, namedItem(
                        menu.lockedMaterial(), menu.lockedName(), menu.lockedLore(), Map.of()));
            }
        }
    }

    private ItemStack profileItem(
            Player player, int used, int limit, HomesSettings.Menu menu) {
        String limitText = limit == HomeLimitRules.UNLIMITED ? "∞" : Integer.toString(limit);
        Map<String, String> replacements = Map.of(
                "player", player.getName(),
                "used", Integer.toString(used),
                "limit", limitText);
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(player);
        meta.displayName(messages.itemText(menu.profileName(), replacements));
        meta.lore(messages.itemLore(menu.profileLore(), replacements));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack homeItem(StoredHome home, HomesSettings.Menu menu) {
        Map<String, String> replacements = Map.of(
                "home", home.name(),
                "world", home.worldName(),
                "x", Integer.toString((int) Math.floor(home.x())),
                "y", Integer.toString((int) Math.floor(home.y())),
                "z", Integer.toString((int) Math.floor(home.z())));
        return namedItem(menu.homeMaterial(), menu.homeName(), menu.homeLore(), replacements);
    }

    private ItemStack namedItem(
            Material material,
            String name,
            List<String> lore,
            Map<String, String> replacements) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.itemText(name, replacements));
        if (!lore.isEmpty()) {
            meta.lore(messages.itemLore(lore, replacements));
        }
        item.setItemMeta(meta);
        return item;
    }

    private void fill(Inventory inventory, Material material) {
        ItemStack filler = namedItem(material, "", List.of(), Map.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private int resolveLimit(Player player, HomesSettings settings) {
        return HomeLimitRules.resolve(
                settings.defaultLimit(),
                settings.maximumPermissionLimit(),
                player::hasPermission);
    }

    static int pageCount(int homeCount, int limit) {
        int visible = limit == HomeLimitRules.UNLIMITED
                ? Math.max(1, homeCount + 1)
                : Math.max(1, Math.max(homeCount, limit));
        return Math.max(1, (visible + HOMES_PER_PAGE - 1) / HOMES_PER_PAGE);
    }

    private static final class MainHolder implements InventoryHolder {

        private final UUID playerId;
        private final int page;
        private final int pages;
        private final Map<Integer, StoredHome> homesBySlot = new HashMap<>();
        private Inventory inventory;

        private MainHolder(UUID playerId, int page, int pages) {
            this.playerId = playerId;
            this.page = page;
            this.pages = pages;
        }

        @Override
        public @NotNull Inventory getInventory() {
            if (inventory == null) {
                throw new IllegalStateException("Homes inventory is not initialized");
            }
            return inventory;
        }
    }

    private static final class ConfirmHolder implements InventoryHolder {

        private final UUID playerId;
        private final String homeKey;
        private final String homeName;
        private final int returnPage;
        private Inventory inventory;

        private ConfirmHolder(UUID playerId, String homeKey, String homeName, int returnPage) {
            this.playerId = playerId;
            this.homeKey = homeKey;
            this.homeName = homeName;
            this.returnPage = returnPage;
        }

        @Override
        public @NotNull Inventory getInventory() {
            if (inventory == null) {
                throw new IllegalStateException("Confirmation inventory is not initialized");
            }
            return inventory;
        }
    }
}
