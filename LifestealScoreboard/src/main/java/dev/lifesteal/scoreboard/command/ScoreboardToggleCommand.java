package dev.lifesteal.scoreboard.command;

import dev.lifesteal.scoreboard.render.SidebarManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** Session-only /scoreboard toggle command. */
public final class ScoreboardToggleCommand implements CommandExecutor {

    private final SidebarManager sidebarManager;

    public ScoreboardToggleCommand(SidebarManager sidebarManager) {
        this.sidebarManager = sidebarManager;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(
                    "Only players can toggle their sidebar.", NamedTextColor.RED));
            return true;
        }
        if (args.length != 0) {
            player.sendMessage(Component.text("Usage: /scoreboard", NamedTextColor.RED));
            return true;
        }

        SidebarManager.ToggleResult result = sidebarManager.toggle(player);
        switch (result) {
            case ENABLED -> player.sendMessage(Component.text(
                    "The scoreboard is now enabled.", NamedTextColor.GREEN));
            case DISABLED -> player.sendMessage(Component.text(
                    "The scoreboard is now disabled for this session.", NamedTextColor.YELLOW));
            case GLOBALLY_DISABLED -> player.sendMessage(Component.text(
                    "The scoreboard is disabled by the server.", NamedTextColor.RED));
        }
        return true;
    }
}
