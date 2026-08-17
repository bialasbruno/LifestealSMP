package dev.lifesteal.spawn.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

public record SpawnSettings(
        boolean voidRescueEnabled,
        Set<String> enabledWorldNames,
        int triggerOffsetBelowMinHeight,
        String destinationWorldName,
        boolean useWorldSpawn,
        double destinationX,
        double destinationY,
        double destinationZ,
        float destinationYaw,
        float destinationPitch,
        String rescuedMessage,
        String noPermissionMessage,
        String usageMessage,
        String reloadedMessage,
        String soundName,
        float soundVolume,
        float soundPitch) {

    public SpawnSettings {
        enabledWorldNames = Set.copyOf(enabledWorldNames);
        Objects.requireNonNull(destinationWorldName, "destinationWorldName");
        Objects.requireNonNull(rescuedMessage, "rescuedMessage");
        Objects.requireNonNull(noPermissionMessage, "noPermissionMessage");
        Objects.requireNonNull(usageMessage, "usageMessage");
        Objects.requireNonNull(reloadedMessage, "reloadedMessage");
        Objects.requireNonNull(soundName, "soundName");
    }

    public static SpawnSettings load(FileConfiguration config, Logger logger) {
        return new SpawnSettings(
                config.getBoolean("void-rescue.enabled", true),
                readWorldNames(config, logger),
                readInt(
                        config,
                        logger,
                        "void-rescue.trigger-offset-below-min-height",
                        0,
                        256,
                        5),
                readNonBlank(config, logger, "void-rescue.destination.world", "spawn"),
                config.getBoolean("void-rescue.destination.use-world-spawn", true),
                readDouble(
                        config, logger, "void-rescue.destination.x", -30_000_000D, 30_000_000D, 0.5D),
                readDouble(config, logger, "void-rescue.destination.y", -2_048D, 2_048D, 100D),
                readDouble(
                        config, logger, "void-rescue.destination.z", -30_000_000D, 30_000_000D, 0.5D),
                (float) readDouble(
                        config, logger, "void-rescue.destination.yaw", -360D, 360D, 0D),
                (float) readDouble(
                        config, logger, "void-rescue.destination.pitch", -90D, 90D, 0D),
                config.getString(
                        "messages.rescued",
                        "<aqua>You fell into the void and returned to spawn.</aqua>"),
                config.getString(
                        "messages.no-permission",
                        "<red>You do not have permission to use this command.</red>"),
                config.getString(
                        "messages.usage", "<red>Usage: /lifestealspawn reload</red>"),
                config.getString(
                        "messages.reloaded",
                        "<green>LifestealSpawn configuration reloaded.</green>"),
                readNonBlank(
                        config,
                        logger,
                        "sound.name",
                        "minecraft:entity.enderman.teleport"),
                (float) readDouble(config, logger, "sound.volume", 0D, 10D, 0.7D),
                (float) readDouble(config, logger, "sound.pitch", 0D, 2D, 1.2D));
    }

    public boolean isEnabledInWorld(String worldName) {
        return enabledWorldNames.contains(worldName.toLowerCase(Locale.ROOT));
    }

    private static Set<String> readWorldNames(FileConfiguration config, Logger logger) {
        List<String> configured = config.getStringList("void-rescue.enabled-worlds");
        if (configured.isEmpty()) {
            return Set.of("spawn");
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String worldName : configured) {
            String candidate = worldName.trim().toLowerCase(Locale.ROOT);
            if (!candidate.isEmpty()) {
                normalized.add(candidate);
            }
        }
        if (normalized.isEmpty()) {
            logger.warning("'void-rescue.enabled-worlds' has no valid names; using spawn.");
            return Set.of("spawn");
        }
        return normalized;
    }

    private static String readNonBlank(
            FileConfiguration config, Logger logger, String path, String fallback) {
        String value = config.getString(path, fallback).trim();
        if (value.isEmpty()) {
            logger.warning("'" + path + "' cannot be blank; using " + fallback + '.');
            return fallback;
        }
        return value;
    }

    private static int readInt(
            FileConfiguration config,
            Logger logger,
            String path,
            int minimum,
            int maximum,
            int fallback) {
        int value = config.getInt(path, fallback);
        if (value < minimum || value > maximum) {
            logger.warning("'" + path + "' must be between " + minimum + " and " + maximum
                    + "; using " + fallback + '.');
            return fallback;
        }
        return value;
    }

    private static double readDouble(
            FileConfiguration config,
            Logger logger,
            String path,
            double minimum,
            double maximum,
            double fallback) {
        double value = config.getDouble(path, fallback);
        if (!Double.isFinite(value) || value < minimum || value > maximum) {
            logger.warning("'" + path + "' must be between " + minimum + " and " + maximum
                    + "; using " + fallback + '.');
            return fallback;
        }
        return value;
    }
}
