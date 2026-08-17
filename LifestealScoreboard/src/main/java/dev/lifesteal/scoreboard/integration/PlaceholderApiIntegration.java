package dev.lifesteal.scoreboard.integration;

import dev.lifesteal.scoreboard.placeholder.PlaceholderResolver;
import org.bukkit.plugin.Plugin;

/** Loads PlaceholderAPI-specific classes only when the optional plugin is enabled. */
public final class PlaceholderApiIntegration {

    private PlaceholderApiIntegration() {
    }

    public static PlaceholderApiBridge register(
            Plugin plugin, PlaceholderResolver placeholders) {
        LifestealPlaceholderExpansion expansion =
                new LifestealPlaceholderExpansion(plugin, placeholders);
        if (!expansion.register()) {
            throw new IllegalStateException(
                    "PlaceholderAPI rejected the 'lifesteal' expansion registration");
        }
        return expansion::unregister;
    }
}
