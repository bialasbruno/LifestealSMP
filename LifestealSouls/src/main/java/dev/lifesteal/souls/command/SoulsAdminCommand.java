package dev.lifesteal.souls.command;

import dev.lifesteal.souls.LifestealSoulsPlugin;
import dev.lifesteal.souls.config.SoulsSettings;
import dev.lifesteal.souls.data.PurchaseResult;
import dev.lifesteal.souls.data.SoulMutation;
import dev.lifesteal.souls.data.SoulTransaction;
import dev.lifesteal.souls.message.MessageService;
import dev.lifesteal.souls.service.SoulService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class SoulsAdminCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of(
            "help", "balance", "add", "remove", "set", "purchase", "history", "reload");
    private static final DateTimeFormatter HISTORY_TIME = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final LifestealSoulsPlugin plugin;
    private final SoulService soulService;
    private final MessageService messages;
    private final PlayerTargetResolver targets;

    public SoulsAdminCommand(
            LifestealSoulsPlugin plugin,
            SoulService soulService,
            MessageService messages) {
        this.plugin = plugin;
        this.soulService = soulService;
        this.messages = messages;
        this.targets = new PlayerTargetResolver(plugin.getServer(), soulService);
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        SoulsSettings settings = plugin.settings();
        if (!sender.hasPermission("lifestealsouls.admin")) {
            messages.send(sender, settings.noPermissionMessage());
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        try {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "balance" -> balance(sender, args);
                case "add" -> add(sender, args);
                case "remove" -> remove(sender, args);
                case "set" -> set(sender, args);
                case "purchase" -> purchase(sender, args);
                case "history" -> history(sender, args);
                case "reload" -> reload(sender, args);
                default -> {
                    messages.send(sender, settings.invalidCommandMessage());
                    yield true;
                }
            };
        } catch (IllegalArgumentException exception) {
            sender.sendMessage("Souls error: " + exception.getMessage());
            return true;
        }
    }

    private boolean balance(CommandSender sender, String[] args) {
        PlayerTarget target = requireTarget(sender, args, 2);
        if (target == null) {
            return true;
        }
        sender.sendMessage(target.lastKnownName() + " has "
                + soulService.getSouls(target.playerId()) + " Souls.");
        return true;
    }

    private boolean add(CommandSender sender, String[] args) {
        PlayerTarget target = requireTarget(sender, args, 3);
        if (target == null) {
            return true;
        }
        long amount = parsePositive(args[2], "amount");
        SoulMutation result = soulService.addAdmin(
                target.playerId(), target.lastKnownName(), amount, "admin:" + sender.getName());
        sender.sendMessage("Added " + amount + " Souls to " + target.lastKnownName()
                + ". New balance: " + result.balance() + '.');
        return true;
    }

    private boolean remove(CommandSender sender, String[] args) {
        PlayerTarget target = requireTarget(sender, args, 3);
        if (target == null) {
            return true;
        }
        long amount = parsePositive(args[2], "amount");
        Optional<SoulMutation> result = soulService.removeAdmin(
                target.playerId(), target.lastKnownName(), amount, "admin:" + sender.getName());
        if (result.isEmpty()) {
            sender.sendMessage("The player does not have enough Souls.");
            return true;
        }
        sender.sendMessage("Removed " + amount + " Souls from " + target.lastKnownName()
                + ". New balance: " + result.get().balance() + '.');
        return true;
    }

    private boolean set(CommandSender sender, String[] args) {
        PlayerTarget target = requireTarget(sender, args, 3);
        if (target == null) {
            return true;
        }
        long balance = parseNonNegative(args[2], "balance");
        SoulMutation result = soulService.setAdmin(
                target.playerId(), target.lastKnownName(), balance);
        sender.sendMessage("Set " + target.lastKnownName() + " to " + result.balance() + " Souls.");
        return true;
    }

    private boolean purchase(CommandSender sender, String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage("Webshop purchases can only be applied by the server console.");
            return true;
        }
        if (args.length != 4) {
            sender.sendMessage("Usage: /soulsadmin purchase <uuid> <amount> <transaction-id>");
            return true;
        }
        UUID playerId = UUID.fromString(args[1]);
        long amount = parsePositive(args[2], "amount");
        PurchaseResult result = soulService.applyPurchase(args[3], playerId, amount);
        if (result.applied()) {
            sender.sendMessage("Purchase applied. New balance: " + result.balance() + '.');
        } else {
            sender.sendMessage("Purchase was already applied; balance remains "
                    + result.balance() + '.');
        }
        return true;
    }

    private boolean history(CommandSender sender, String[] args) {
        PlayerTarget target = requireTarget(sender, args, 2);
        if (target == null) {
            return true;
        }
        long requestedLimit = args.length >= 3 ? parsePositive(args[2], "limit") : 10L;
        if (requestedLimit > 100L) {
            throw new IllegalArgumentException("History limit cannot exceed 100");
        }
        int limit = (int) requestedLimit;
        List<SoulTransaction> transactions = soulService.history(target.playerId(), limit);
        sender.sendMessage("Last " + transactions.size() + " Souls transaction(s) for "
                + target.lastKnownName() + ':');
        for (SoulTransaction transaction : transactions) {
            String reference = transaction.reference() == null
                    ? ""
                    : " [" + transaction.reference() + ']';
            sender.sendMessage("#" + transaction.id() + " "
                    + HISTORY_TIME.format(transaction.createdAt()) + " "
                    + transaction.type() + " "
                    + (transaction.amount() >= 0 ? "+" : "") + transaction.amount()
                    + " => " + transaction.balanceAfter() + reference);
        }
        return true;
    }

    private boolean reload(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("Usage: /soulsadmin reload");
            return true;
        }
        plugin.reloadSoulsSettings();
        sender.sendMessage("LifestealSouls configuration reloaded.");
        return true;
    }

    private PlayerTarget requireTarget(CommandSender sender, String[] args, int minimumLength) {
        if (args.length < minimumLength) {
            sender.sendMessage("Missing player or amount. Use /soulsadmin help.");
            return null;
        }
        Optional<PlayerTarget> target = targets.resolve(args[1]);
        if (target.isEmpty()) {
            sender.sendMessage("Unknown player. Use an online name, stored name, or UUID.");
            return null;
        }
        return target.get();
    }

    private long parsePositive(String input, String label) {
        long value = parseNonNegative(input, label);
        if (value == 0L) {
            throw new IllegalArgumentException(label + " must be positive");
        }
        return value;
    }

    private long parseNonNegative(String input, String label) {
        try {
            long value = Long.parseLong(input);
            if (value < 0L) {
                throw new IllegalArgumentException(label + " cannot be negative");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " must be a whole number");
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("LifestealSouls admin commands:");
        sender.sendMessage("/soulsadmin balance <player|uuid>");
        sender.sendMessage("/soulsadmin add <player|uuid> <amount>");
        sender.sendMessage("/soulsadmin remove <player|uuid> <amount>");
        sender.sendMessage("/soulsadmin set <player|uuid> <balance>");
        sender.sendMessage("/soulsadmin purchase <uuid> <amount> <transaction-id> (console only)");
        sender.sendMessage("/soulsadmin history <player|uuid> [limit]");
        sender.sendMessage("/soulsadmin reload");
    }

    @Override
    public @NotNull List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && List.of("balance", "add", "remove", "set", "history")
                .contains(args[0].toLowerCase(Locale.ROOT))) {
            List<String> names = plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .toList();
            return filter(names, args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                matches.add(option);
            }
        }
        return matches;
    }
}
