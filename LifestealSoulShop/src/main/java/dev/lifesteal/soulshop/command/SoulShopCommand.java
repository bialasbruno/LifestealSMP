package dev.lifesteal.soulshop.command;

import dev.lifesteal.soulshop.LifestealSoulShopPlugin;
import dev.lifesteal.soulshop.config.SoulShopSettings;
import dev.lifesteal.soulshop.menu.SoulShopMenu;
import dev.lifesteal.soulshop.message.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class SoulShopCommand implements TabExecutor {

    private final LifestealSoulShopPlugin plugin;
    private final SoulShopMenu menu;
    private final MessageService messages;

    public SoulShopCommand(
            LifestealSoulShopPlugin plugin, SoulShopMenu menu, MessageService messages) {
        this.plugin = plugin;
        this.menu = menu;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        SoulShopSettings settings = plugin.settings();
        if (args.length == 0) {
            if (!sender.hasPermission("lifestealsoulshop.use")) {
                messages.send(sender, settings.noPermissionMessage());
            } else if (!(sender instanceof Player player)) {
                messages.send(sender, settings.playerOnlyMessage());
            } else {
                menu.open(player);
            }
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("lifestealsoulshop.admin")) {
                messages.send(sender, settings.noPermissionMessage());
            } else {
                plugin.reloadShopSettings();
                messages.send(sender, plugin.settings().reloadedMessage());
            }
            return true;
        }

        messages.send(sender, settings.usageMessage());
        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1
                && sender.hasPermission("lifestealsoulshop.admin")
                && "reload".startsWith(args[0].toLowerCase())) {
            return List.of("reload");
        }
        return List.of();
    }
}
