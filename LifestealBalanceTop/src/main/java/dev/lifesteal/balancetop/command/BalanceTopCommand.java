package dev.lifesteal.balancetop.command;

import dev.lifesteal.balancetop.config.BalanceTopSettings;
import dev.lifesteal.balancetop.menu.BalanceTopMenu;
import dev.lifesteal.balancetop.message.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** Opens the replacement balance leaderboard GUI. */
public final class BalanceTopCommand implements CommandExecutor {

    public static final String PERMISSION = "lifestealbalancetop.use";

    private final BalanceTopMenu menu;
    private final MessageService messages;
    private final BalanceTopSettings settings;

    public BalanceTopCommand(
            BalanceTopMenu menu, MessageService messages, BalanceTopSettings settings) {
        this.menu = menu;
        this.messages = messages;
        this.settings = settings;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        execute(sender, args);
        return true;
    }

    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, settings.playerOnlyMessage());
            return;
        }
        if (!player.hasPermission(PERMISSION)) {
            messages.send(player, settings.noPermissionMessage());
            return;
        }
        if (args.length > 0) {
            messages.send(player, settings.usageMessage());
            return;
        }
        menu.open(player);
    }
}
