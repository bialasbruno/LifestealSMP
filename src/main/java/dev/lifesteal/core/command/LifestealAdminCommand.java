package dev.lifesteal.core.command;

import dev.lifesteal.core.heart.HeartItemFactory;
import dev.lifesteal.core.heart.HeartService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

/**
 * {@code /lifesteal <sethearts|givebrokenheart|giveheart> ...} - administrative and testing
 * commands, all gated behind {@code lifesteal.admin}.
 *
 * <p><b>Known limitation (v0.1):</b> all three subcommands require the target player to be
 * online, since {@link HeartService} applies changes directly to a live {@link Player}'s
 * attribute. Offline targets are rejected with a clear error message rather than silently
 * failing. A future version could write directly to the repository for offline players and
 * apply the change on their next join.</p>
 */
public final class LifestealAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("sethearts", "givebrokenheart", "giveheart");
    private static final int MAX_GIVE_AMOUNT = 64;

    private final HeartService heartService;
    private final HeartItemFactory itemFactory;
    private final int minimumHearts;
    private final int maximumHearts;

    public LifestealAdminCommand(HeartService heartService, HeartItemFactory itemFactory,
                                  int minimumHearts, int maximumHearts) {
        this.heartService = heartService;
        this.itemFactory = itemFactory;
        this.minimumHearts = minimumHearts;
        this.maximumHearts = maximumHearts;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                              @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(usageMessage());
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "sethearts" -> handleSetHearts(sender, args);
            case "givebrokenheart" -> handleGive(sender, args, itemFactory::createBrokenHeart, "Broken Heart");
            case "giveheart" -> handleGive(sender, args, itemFactory::createHeart, "Heart");
            default -> {
                sender.sendMessage(Component.text("Unknown subcommand: " + args[0], NamedTextColor.RED));
                sender.sendMessage(usageMessage());
                yield true;
            }
        };
    }

    private boolean handleSetHearts(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(Component.text("Usage: /lifesteal sethearts <player> <amount>", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(offlineMessage(args[1]));
            return true;
        }

        Integer amount = parseInt(sender, args[2]);
        if (amount == null) {
            return true;
        }
        if (amount < minimumHearts || amount > maximumHearts) {
            sender.sendMessage(Component.text(
                    "Amount must be between " + minimumHearts + " and " + maximumHearts + ".", NamedTextColor.RED));
            return true;
        }

        heartService.setHearts(target, amount);
        sender.sendMessage(Component.text(
                "Set " + target.getName() + "'s hearts to " + amount + "/" + maximumHearts + ".", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args, IntFunction<ItemStack> factory, String itemLabel) {
        if (args.length < 2 || args.length > 3) {
            sender.sendMessage(Component.text(
                    "Usage: /lifesteal " + args[0] + " <player> [amount]", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(offlineMessage(args[1]));
            return true;
        }

        int amount = 1;
        if (args.length == 3) {
            Integer parsed = parseInt(sender, args[2]);
            if (parsed == null) {
                return true;
            }
            amount = parsed;
        }
        if (amount < 1 || amount > MAX_GIVE_AMOUNT) {
            sender.sendMessage(Component.text("Amount must be between 1 and " + MAX_GIVE_AMOUNT + ".", NamedTextColor.RED));
            return true;
        }

        target.getInventory().addItem(factory.apply(amount));
        sender.sendMessage(Component.text(
                "Gave " + amount + "x " + itemLabel + " to " + target.getName() + ".", NamedTextColor.GREEN));
        return true;
    }

    private Integer parseInt(CommandSender sender, String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException exception) {
            sender.sendMessage(Component.text("'" + raw + "' is not a valid number.", NamedTextColor.RED));
            return null;
        }
    }

    private Component offlineMessage(String name) {
        return Component.text("Player '" + name + "' is not online.", NamedTextColor.RED);
    }

    private Component usageMessage() {
        return Component.text("Usage: /lifesteal <sethearts|givebrokenheart|giveheart> ...", NamedTextColor.RED);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS;
        }
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        return List.of();
    }
}
