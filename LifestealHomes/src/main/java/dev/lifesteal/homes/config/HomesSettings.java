package dev.lifesteal.homes.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record HomesSettings(
        int defaultLimit,
        int maximumPermissionLimit,
        int maximumNameLength,
        int teleportDelaySeconds,
        boolean cancelOnMove,
        boolean cancelOnDamage,
        double movementTolerance,
        Menu menu,
        Map<String, String> messages) {

    public static HomesSettings load(ConfigurationSection config) {
        int defaultLimit = nonNegative(config.getInt("limits.default", 1), "limits.default");
        int maximumPermissionLimit = positive(
                config.getInt("limits.maximum-permission-limit", 100),
                "limits.maximum-permission-limit");
        int maximumNameLength = positive(
                config.getInt("homes.maximum-name-length", 16),
                "homes.maximum-name-length");
        int teleportDelay = nonNegative(
                config.getInt("teleport.delay-seconds", 3), "teleport.delay-seconds");
        double tolerance = config.getDouble("teleport.movement-tolerance", 0.2D);
        if (!Double.isFinite(tolerance) || tolerance < 0.0D) {
            throw new IllegalArgumentException("teleport.movement-tolerance must be non-negative");
        }

        Menu menu = new Menu(
                string(config, "menu.title", "<gradient:#7dd3fc:#c084fc><bold>Your homes</bold></gradient>"),
                material(config, "menu.filler-material", Material.GRAY_STAINED_GLASS_PANE),
                material(config, "menu.home-material", Material.ENDER_PEARL),
                material(config, "menu.available-material", Material.LIME_STAINED_GLASS_PANE),
                material(config, "menu.locked-material", Material.RED_STAINED_GLASS_PANE),
                string(config, "menu.profile.name", "<aqua><bold>{player}</bold></aqua>"),
                strings(config, "menu.profile.lore", List.of(
                        "<gray>Homes:</gray> <white>{used}/{limit}</white>",
                        "",
                        "<dark_gray>More slots are unlocked by ranks.</dark_gray>")),
                string(config, "menu.home.name", "<aqua><bold>{home}</bold></aqua>"),
                strings(config, "menu.home.lore", List.of(
                        "<gray>World:</gray> <white>{world}</white>",
                        "<gray>Location:</gray> <white>{x}, {y}, {z}</white>",
                        "",
                        "<yellow>Left click</yellow> <gray>to teleport</gray>",
                        "<red>Shift + right click</red> <gray>to delete</gray>")),
                string(config, "menu.available.name", "<green>Available home slot</green>"),
                strings(config, "menu.available.lore", List.of("<gray>Use</gray> <white>/sethome [name]</white>")),
                string(config, "menu.locked.name", "<red>Locked home slot</red>"),
                strings(config, "menu.locked.lore", List.of("<gray>Unlock it with a higher rank.</gray>")),
                string(config, "menu.previous-page", "<yellow>Previous page</yellow>"),
                string(config, "menu.next-page", "<yellow>Next page</yellow>"),
                string(config, "menu.close", "<red>Close</red>"),
                string(config, "menu.confirm.title", "<red><bold>Delete {home}?</bold></red>"),
                string(config, "menu.confirm.accept", "<red><bold>Delete home</bold></red>"),
                strings(config, "menu.confirm.accept-lore", List.of("<gray>This cannot be undone.</gray>")),
                string(config, "menu.confirm.cancel", "<green>Keep home</green>"));

        Map<String, String> messages = new LinkedHashMap<>();
        ConfigurationSection messageSection = config.getConfigurationSection("messages");
        if (messageSection != null) {
            for (String key : messageSection.getKeys(false)) {
                if (messageSection.isString(key)) {
                    messages.put(key, messageSection.getString(key, ""));
                }
            }
        }

        return new HomesSettings(
                defaultLimit,
                maximumPermissionLimit,
                maximumNameLength,
                teleportDelay,
                config.getBoolean("teleport.cancel-on-move", true),
                config.getBoolean("teleport.cancel-on-damage", true),
                tolerance,
                menu,
                Map.copyOf(messages));
    }

    public String message(String key) {
        return messages.getOrDefault(key, "<red>Missing message: " + key + "</red>");
    }

    private static int positive(int value, String path) {
        if (value <= 0) {
            throw new IllegalArgumentException(path + " must be positive");
        }
        return value;
    }

    private static int nonNegative(int value, String path) {
        if (value < 0) {
            throw new IllegalArgumentException(path + " must be non-negative");
        }
        return value;
    }

    private static String string(ConfigurationSection config, String path, String fallback) {
        return config.getString(path, fallback);
    }

    private static List<String> strings(ConfigurationSection config, String path, List<String> fallback) {
        List<String> configured = config.getStringList(path);
        return configured.isEmpty() ? fallback : List.copyOf(configured);
    }

    private static Material material(ConfigurationSection config, String path, Material fallback) {
        String value = config.getString(path, fallback.name());
        Material result = Material.matchMaterial(value);
        if (result == null || result == Material.AIR) {
            throw new IllegalArgumentException(path + " is not a valid item material: " + value);
        }
        return result;
    }

    public record Menu(
            String title,
            Material fillerMaterial,
            Material homeMaterial,
            Material availableMaterial,
            Material lockedMaterial,
            String profileName,
            List<String> profileLore,
            String homeName,
            List<String> homeLore,
            String availableName,
            List<String> availableLore,
            String lockedName,
            List<String> lockedLore,
            String previousPage,
            String nextPage,
            String close,
            String confirmTitle,
            String confirmAccept,
            List<String> confirmAcceptLore,
            String confirmCancel) {}
}
