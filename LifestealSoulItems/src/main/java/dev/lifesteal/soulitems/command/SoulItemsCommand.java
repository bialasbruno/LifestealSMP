package dev.lifesteal.soulitems.command;

import dev.lifesteal.soulitems.LifestealSoulItemsPlugin;
import dev.lifesteal.soulitems.config.SoulItemsSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

public final class SoulItemsCommand implements CommandExecutor, TabCompleter {

    private final LifestealSoulItemsPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public SoulItemsCommand(LifestealSoulItemsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        SoulItemsSettings settings = plugin.settings();
        if (!sender.hasPermission("lifestealsoulitems.admin")) {
            send(sender, settings.noPermissionMessage());
            return true;
        }
        if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
            send(sender, settings.usageMessage());
            return true;
        }

        plugin.reloadSoulItemsSettings();
        send(sender, plugin.settings().reloadedMessage());
        return true;
    }

    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (!sender.hasPermission("lifestealsoulitems.admin") || args.length != 1) {
            return List.of();
        }
        return "reload".startsWith(args[0].toLowerCase(Locale.ROOT))
                ? List.of("reload")
                : List.of();
    }

    private void send(CommandSender sender, String text) {
        try {
            sender.sendMessage(miniMessage.deserialize(text));
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Invalid MiniMessage command text: "
                    + exception.getMessage());
            sender.sendMessage(Component.text(text));
        }
    }
}
