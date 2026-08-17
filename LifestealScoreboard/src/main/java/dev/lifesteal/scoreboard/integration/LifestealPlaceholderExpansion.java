package dev.lifesteal.scoreboard.integration;

import dev.lifesteal.scoreboard.placeholder.PlaceholderResolver;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Internal PlaceholderAPI expansion for the public %lifesteal_*% placeholders. */
final class LifestealPlaceholderExpansion extends PlaceholderExpansion {

    private static final List<String> PLACEHOLDERS = List.of(
            "%lifesteal_hearts%",
            "%lifesteal_balance%",
            "%lifesteal_money%",
            "%lifesteal_souls%",
            "%lifesteal_kills%",
            "%lifesteal_deaths%");

    private final Plugin plugin;
    private final PlaceholderResolver placeholders;

    LifestealPlaceholderExpansion(Plugin plugin, PlaceholderResolver placeholders) {
        this.plugin = plugin;
        this.placeholders = placeholders;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "lifesteal";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public @NotNull List<String> getPlaceholders() {
        return PLACEHOLDERS;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(
            @Nullable OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null) {
            return null;
        }
        Player player = offlinePlayer.getPlayer();
        if (player == null || !player.isOnline()) {
            return null;
        }
        return placeholders.resolveExternal(player, params);
    }
}
