package dev.lifesteal.spawn.rescue;

import dev.lifesteal.souls.LifestealSoulsPlugin;
import dev.lifesteal.souls.afk.AfkZone;
import dev.lifesteal.souls.config.SoulsSettings;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

public final class AfkDestinationResolver {

    private final Plugin plugin;
    private final AfkZone afkZone = new AfkZone();

    public AfkDestinationResolver(Plugin plugin) {
        this.plugin = plugin;
    }

    public Location resolve(float yaw) {
        Plugin candidate = plugin.getServer().getPluginManager().getPlugin("LifestealSouls");
        if (!(candidate instanceof LifestealSoulsPlugin soulsPlugin)
                || !candidate.isEnabled()) {
            return null;
        }

        SoulsSettings settings = soulsPlugin.settings();
        if (!afkZone.isConfigured(settings)) {
            return null;
        }

        World world = plugin.getServer().getWorld(settings.afkWorldName());
        if (world == null) {
            return null;
        }
        Location destination = afkZone.center(world, settings, yaw);
        if (destination.getY() < world.getMinHeight()
                || destination.getY() >= world.getMaxHeight()) {
            return null;
        }
        return destination;
    }
}
