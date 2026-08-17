package dev.lifesteal.scoreboard.placeholder;

import org.bukkit.entity.Player;

import java.util.Objects;

/** Per-refresh context for resolving player and server values. */
public record PlaceholderContext(Player player) {

    public PlaceholderContext {
        Objects.requireNonNull(player, "player");
    }
}
