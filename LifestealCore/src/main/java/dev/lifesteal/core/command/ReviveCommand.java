package dev.lifesteal.core.command;

import dev.lifesteal.core.elimination.EliminationService;
import dev.lifesteal.core.heart.HeartItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

/** {@code /revive <player>} consumes a held Revive Totem after a successful revive. */
public final class ReviveCommand implements CommandExecutor, TabCompleter {

    private final Plugin plugin;
    private final EliminationService eliminationService;
    private final HeartItemFactory itemFactory;

    public ReviveCommand(
            Plugin plugin, EliminationService eliminationService, HeartItemFactory itemFactory) {
        this.plugin = plugin;
        this.eliminationService = eliminationService;
        this.itemFactory = itemFactory;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(Component.text("Usage: /revive <player>", NamedTextColor.RED));
            return true;
        }

        HeldTotem heldTotem = findHeldTotem(player.getInventory());
        if (heldTotem == null) {
            player.sendMessage(Component.text(
                    "Hold a real Revive Totem in either hand.", NamedTextColor.RED));
            return true;
        }

        try {
            EliminationService.ReviveResult result = eliminationService.revive(args[0]);
            switch (result.status()) {
                case SUCCESS -> {
                    consumeOne(player.getInventory(), heldTotem);
                    player.sendMessage(Component.text(
                            "Revived " + result.playerName() + " with "
                                    + result.restoredHearts() + " hearts.",
                            NamedTextColor.GREEN));
                }
                case NOT_ELIMINATED -> player.sendMessage(Component.text(
                        "Player '" + args[0] + "' is not currently eliminated.",
                        NamedTextColor.RED));
                case BAN_ALREADY_EXPIRED -> player.sendMessage(Component.text(
                        result.playerName() + "'s ban had already expired. The totem was not consumed.",
                        NamedTextColor.YELLOW));
            }
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to revive " + args[0], exception);
            player.sendMessage(Component.text(
                    "Revive failed safely. The totem was not consumed.", NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return eliminationService.activeEliminatedNames().stream()
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                .toList();
    }

    private HeldTotem findHeldTotem(PlayerInventory inventory) {
        ItemStack mainHand = inventory.getItemInMainHand();
        if (itemFactory.isReviveTotem(mainHand)) {
            return new HeldTotem(EquipmentSlot.HAND, mainHand);
        }
        ItemStack offHand = inventory.getItemInOffHand();
        if (itemFactory.isReviveTotem(offHand)) {
            return new HeldTotem(EquipmentSlot.OFF_HAND, offHand);
        }
        return null;
    }

    private void consumeOne(PlayerInventory inventory, HeldTotem heldTotem) {
        ItemStack item = heldTotem.item();
        int newAmount = item.getAmount() - 1;
        ItemStack replacement = newAmount <= 0 ? null : item;
        if (replacement != null) {
            replacement.setAmount(newAmount);
        }
        if (heldTotem.slot() == EquipmentSlot.HAND) {
            inventory.setItemInMainHand(replacement);
        } else {
            inventory.setItemInOffHand(replacement);
        }
    }

    private record HeldTotem(EquipmentSlot slot, ItemStack item) {
    }
}
