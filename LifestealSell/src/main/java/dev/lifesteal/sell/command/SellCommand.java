package dev.lifesteal.sell.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SellCommand implements TabExecutor {

    private static final String SHOP_COMMAND_NAMESPACE = "economyshopgui";

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(
                    "This command can only be used by a player.", NamedTextColor.RED));
            return true;
        }

        switch (SellAction.from(args)) {
            case OPEN_GUI -> dispatch(player, "sellgui");
            case SELL_HELD_MATERIAL -> sellHeldMaterial(player);
            case SHOW_HELP -> sendHelp(player);
        }
        return true;
    }

    private void sellHeldMaterial(Player player) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        Material material = heldItem.getType();
        if (material.isAir()) {
            player.sendMessage(Component.text(
                    "Hold the item type you want to sell.", NamedTextColor.RED));
            return;
        }
        if (heldItem.hasItemMeta()
                && !heldItem.getItemMeta().getPersistentDataContainer().isEmpty()) {
            player.sendMessage(Component.text(
                    "Custom items cannot be sold with this command.", NamedTextColor.RED));
            return;
        }

        PlayerInventory inventory = player.getInventory();
        ItemStack reference = heldItem.clone();
        List<HiddenItem> differentVariants = hideDifferentVariants(inventory, reference);
        try {
            dispatch(player, "sellall " + material.name().toLowerCase(Locale.ROOT));
        } finally {
            restoreItems(player, differentVariants);
        }
    }

    private List<HiddenItem> hideDifferentVariants(
            PlayerInventory inventory, ItemStack reference) {
        List<HiddenItem> hiddenItems = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack candidate = inventory.getItem(slot);
            if (candidate == null
                    || candidate.getType() != reference.getType()
                    || candidate.isSimilar(reference)) {
                continue;
            }
            hiddenItems.add(new HiddenItem(slot, candidate.clone()));
            inventory.setItem(slot, null);
        }
        return hiddenItems;
    }

    private void restoreItems(Player player, List<HiddenItem> hiddenItems) {
        PlayerInventory inventory = player.getInventory();
        for (HiddenItem hiddenItem : hiddenItems) {
            ItemStack current = inventory.getItem(hiddenItem.slot());
            if (current == null || current.getType().isAir()) {
                inventory.setItem(hiddenItem.slot(), hiddenItem.item());
                continue;
            }

            Map<Integer, ItemStack> leftovers = inventory.addItem(hiddenItem.item());
            leftovers.values().forEach(item ->
                    player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
    }

    private void dispatch(Player player, String command) {
        if (!player.performCommand(SHOP_COMMAND_NAMESPACE + ":" + command)) {
            player.sendMessage(Component.text(
                    "The shop command is currently unavailable.", NamedTextColor.RED));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("Sell commands", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/sell", NamedTextColor.YELLOW)
                .append(Component.text(" - Open the sell GUI.", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/sell hand", NamedTextColor.YELLOW)
                .append(Component.text(
                        " - Sell every item of the type held in your hand.",
                        NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/sell help", NamedTextColor.YELLOW)
                .append(Component.text(" - Show this help message.", NamedTextColor.GRAY)));
    }

    @Override
    public @NotNull List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String input = args[0].toLowerCase(Locale.ROOT);
        return List.of("hand", "help").stream()
                .filter(suggestion -> suggestion.startsWith(input))
                .toList();
    }

    private record HiddenItem(int slot, ItemStack item) {}
}
