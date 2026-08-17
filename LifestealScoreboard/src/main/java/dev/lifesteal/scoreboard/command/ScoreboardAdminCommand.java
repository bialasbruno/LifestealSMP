package dev.lifesteal.scoreboard.command;

import dev.lifesteal.scoreboard.LifestealScoreboardPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/** Administrative entry point for safe runtime configuration reloads. */
public final class ScoreboardAdminCommand implements CommandExecutor, TabCompleter {

    private final LifestealScoreboardPlugin plugin;

    public ScoreboardAdminCommand(LifestealScoreboardPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(Component.text(
                    "Usage: /lifestealscoreboard reload", NamedTextColor.RED));
            return true;
        }

        plugin.reloadScoreboards();
        sender.sendMessage(Component.text(
                "LifestealScoreboard configuration reloaded.", NamedTextColor.GREEN));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1
                && "reload".startsWith(args[0].toLowerCase(Locale.ROOT))) {
            return List.of("reload");
        }
        return List.of();
    }
}
