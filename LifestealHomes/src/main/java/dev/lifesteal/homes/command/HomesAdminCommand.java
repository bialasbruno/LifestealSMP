package dev.lifesteal.homes.command;

import dev.lifesteal.homes.LifestealHomesPlugin;
import dev.lifesteal.homes.message.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class HomesAdminCommand implements CommandExecutor {

    private final LifestealHomesPlugin plugin;
    private final MessageService messages;

    public HomesAdminCommand(LifestealHomesPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
            messages.send(sender, plugin.settings().message("admin-usage"));
            return true;
        }
        try {
            plugin.reloadHomesSettings();
            messages.send(sender, plugin.settings().message("reloaded"));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Could not reload config.yml: " + exception.getMessage());
            messages.send(sender, plugin.settings().message("reload-failed"));
        }
        return true;
    }
}
