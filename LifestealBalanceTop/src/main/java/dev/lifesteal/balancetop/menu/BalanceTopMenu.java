package dev.lifesteal.balancetop.menu;

import dev.lifesteal.balancetop.config.BalanceTopSettings;
import dev.lifesteal.balancetop.message.MessageService;
import dev.lifesteal.balancetop.model.LeaderboardSort;
import dev.lifesteal.balancetop.model.RankedBalanceEntry;
import dev.lifesteal.balancetop.service.BalanceFormatter;
import dev.lifesteal.balancetop.service.BalanceLeaderboardService;
import dev.lifesteal.balancetop.service.BalanceRanking;
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

/** Read-only, sortable and paginated GUI for the regular economy top 100. */
public final class BalanceTopMenu implements Listener {

    private static final int INVENTORY_SIZE = 54;
    private static final int ENTRIES_PER_PAGE = 45;
    private static final int PREVIOUS_SLOT = 45;
    private static final int REFRESH_SLOT = 47;
    private static final int PAGE_SLOT = 48;
    private static final int SORT_SLOT = 49;
    private static final int CLOSE_SLOT = 51;
    private static final int NEXT_SLOT = 53;

    private final BalanceLeaderboardService leaderboard;
    private final MessageService messages;
    private final BalanceTopSettings settings;

    public BalanceTopMenu(
            BalanceLeaderboardService leaderboard,
            MessageService messages,
            BalanceTopSettings settings) {
        this.leaderboard = leaderboard;
        this.messages = messages;
        this.settings = settings;
    }

    public void open(Player player) {
        open(player, 0, LeaderboardSort.BALANCE_DESCENDING, false);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof BalanceTopHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        switch (event.getRawSlot()) {
            case PREVIOUS_SLOT -> open(player, holder.page - 1, holder.sort, false);
            case REFRESH_SLOT -> open(player, holder.page, holder.sort, true);
            case SORT_SLOT -> open(player, 0, holder.sort.next(), false);
            case CLOSE_SLOT -> player.closeInventory();
            case NEXT_SLOT -> open(player, holder.page + 1, holder.sort, false);
            default -> {
                // Player heads and background are intentionally read-only.
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof BalanceTopHolder) {
            event.setCancelled(true);
        }
    }

    private void open(
            Player player, int requestedPage, LeaderboardSort sort, boolean forceRefresh) {
        var loaded = leaderboard.load(forceRefresh);
        if (loaded.isEmpty()) {
            player.closeInventory();
            messages.send(player, settings.economyUnavailableMessage());
            return;
        }

        List<RankedBalanceEntry> entries = BalanceRanking.sorted(loaded.orElseThrow(), sort);
        int pageCount = Math.max(1, (entries.size() + ENTRIES_PER_PAGE - 1) / ENTRIES_PER_PAGE);
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));

        BalanceTopHolder holder = new BalanceTopHolder(page, sort);
        Component title = Component.text("Balance Top ", NamedTextColor.DARK_AQUA)
                .decorate(TextDecoration.BOLD)
                .append(Component.text("• " + (page + 1) + "/" + pageCount,
                        NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false));
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, title);
        holder.inventory = inventory;
        fillBackground(inventory);

        int fromIndex = page * ENTRIES_PER_PAGE;
        int toIndex = Math.min(entries.size(), fromIndex + ENTRIES_PER_PAGE);
        if (fromIndex == toIndex) {
            inventory.setItem(22, createEmptyItem());
        } else {
            for (int index = fromIndex; index < toIndex; index++) {
                inventory.setItem(index - fromIndex, createPlayerItem(entries.get(index)));
            }
        }

        if (page > 0) {
            inventory.setItem(PREVIOUS_SLOT, navigationItem(
                    Material.ARROW, "Previous page", NamedTextColor.AQUA));
        }
        inventory.setItem(REFRESH_SLOT, navigationItem(
                Material.CLOCK, "Refresh ranking", NamedTextColor.GREEN));
        inventory.setItem(PAGE_SLOT, pageItem(page, pageCount, entries.size()));
        inventory.setItem(SORT_SLOT, sortItem(sort));
        inventory.setItem(CLOSE_SLOT, navigationItem(
                Material.BARRIER, "Close", NamedTextColor.RED));
        if (page + 1 < pageCount) {
            inventory.setItem(NEXT_SLOT, navigationItem(
                    Material.ARROW, "Next page", NamedTextColor.AQUA));
        }
        player.openInventory(inventory);
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

    private static ItemStack createPlayerItem(RankedBalanceEntry ranked) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(ranked.entry().playerId()));
        meta.displayName(Component.text(
                        "#" + ranked.rank() + " " + ranked.entry().playerName(),
                        NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.empty(),
                Component.text("Balance: ", NamedTextColor.GRAY)
                        .append(Component.text(
                                "$" + BalanceFormatter.format(ranked.entry().balance()),
                                NamedTextColor.GREEN))
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Position in TOP 100: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text("#" + ranked.rank(), NamedTextColor.WHITE))
                        .decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createEmptyItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("No players in the ranking", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack navigationItem(
            Material material, String name, NamedTextColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack pageItem(int page, int pageCount, int entryCount) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(
                        "Page " + (page + 1) + " of " + pageCount,
                        NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text(
                        entryCount + " of 100 ranking places filled",
                        NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack sortItem(LeaderboardSort sort) {
        ItemStack item = new ItemStack(Material.HOPPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Sort ranking", NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text(sort.displayName(), NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Click to change", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    private static final class BalanceTopHolder implements InventoryHolder {

        private final int page;
        private final LeaderboardSort sort;
        private Inventory inventory;

        private BalanceTopHolder(int page, LeaderboardSort sort) {
            this.page = page;
            this.sort = sort;
        }

        @Override
        public @NotNull Inventory getInventory() {
            if (inventory == null) {
                throw new IllegalStateException("Balance top inventory is not initialized");
            }
            return inventory;
        }
    }
}
