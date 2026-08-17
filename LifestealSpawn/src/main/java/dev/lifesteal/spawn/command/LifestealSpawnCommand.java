package dev.lifesteal.spawn.command;

import dev.lifesteal.spawn.LifestealSpawnPlugin;
import dev.lifesteal.spawn.config.SpawnSettings;
import dev.lifesteal.spawn.message.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class LifestealSpawnCommand implements TabExecutor {

    private final LifestealSpawnPlugin plugin;
    private final MessageService messages;

    public LifestealSpawnCommand(LifestealSpawnPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        SpawnSettings settings = plugin.settings();
        if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
            messages.send(sender, settings.usageMessage());
            return true;
        }
        if (!sender.hasPermission("lifestealspawn.admin")) {
            messages.send(sender, settings.noPermissionMessage());
            return true;
        }

        plugin.reloadSpawnSettings();
        messages.send(sender, plugin.settings().reloadedMessage());
        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1
                && sender.hasPermission("lifestealspawn.admin")
                && "reload".startsWith(args[0].toLowerCase())) {
            return List.of("reload");
        }
        return List.of();
    }
}
