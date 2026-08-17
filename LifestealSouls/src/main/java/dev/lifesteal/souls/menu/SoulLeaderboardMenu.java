package dev.lifesteal.souls.menu;

import dev.lifesteal.souls.data.SoulAccount;
import dev.lifesteal.souls.service.SoulService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Read-only inventory presenting the ten highest Souls balances. */
public final class SoulLeaderboardMenu implements Listener {

    private static final int INVENTORY_SIZE = 27;
    private static final int[] RANK_SLOTS = {4, 3, 5, 10, 11, 12, 13, 14, 15, 16};
    private static final Component TITLE = Component.text("Topka Souls", NamedTextColor.DARK_PURPLE)
            .decorate(TextDecoration.BOLD);

    private final SoulService soulService;

    public SoulLeaderboardMenu(SoulService soulService) {
        this.soulService = soulService;
    }

    public void open(Player player) {
        LeaderboardHolder holder = new LeaderboardHolder();
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, TITLE);
        holder.inventory = inventory;
        fillBackground(inventory);

        List<SoulAccount> accounts = soulService.top(RANK_SLOTS.length);
        if (accounts.isEmpty()) {
            inventory.setItem(13, createEmptyItem());
        } else {
            for (int index = 0; index < accounts.size(); index++) {
                inventory.setItem(
                        RANK_SLOTS[index], createPlayerItem(accounts.get(index), index + 1));
            }
        }
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof LeaderboardHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof LeaderboardHolder) {
            event.setCancelled(true);
        }
    }

    private static void fillBackground(Inventory inventory) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.empty());
        filler.setItemMeta(meta);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private static ItemStack createPlayerItem(SoulAccount account, int rank) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(account.playerId()));
        meta.displayName(Component.text("#" + rank + " " + account.lastKnownName(), rankColor(rank))
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.empty(),
                Component.text("Souls: ", NamedTextColor.GRAY)
                        .append(Component.text(account.balance(), NamedTextColor.AQUA))
                        .decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createEmptyItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Brak graczy w rankingu", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private static NamedTextColor rankColor(int rank) {
        return switch (rank) {
            case 1 -> NamedTextColor.GOLD;
            case 2 -> NamedTextColor.WHITE;
            case 3 -> NamedTextColor.RED;
            default -> NamedTextColor.LIGHT_PURPLE;
        };
    }

    private static final class LeaderboardHolder implements InventoryHolder {

        private Inventory inventory;

        @Override
        public @NotNull Inventory getInventory() {
            if (inventory == null) {
                throw new IllegalStateException("Leaderboard inventory is not initialized");
            }
            return inventory;
        }
    }
}
