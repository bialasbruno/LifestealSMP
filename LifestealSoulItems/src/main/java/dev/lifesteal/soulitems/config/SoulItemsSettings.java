package dev.lifesteal.soulitems.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public record SoulItemsSettings(
        String soulPickaxeName,
        List<String> soulPickaxeLore,
        String reloadedMessage,
        String usageMessage,
        String noPermissionMessage) {

    private static final String DEFAULT_NAME =
            "<gradient:#22d3ee:#8b5cf6><bold>Soul Pickaxe</bold></gradient>";
    private static final List<String> DEFAULT_LORE = List.of(
            "<gray>A netherite tool inhabited by restless souls.</gray>",
            "<dark_purple>Their whispers guide every strike.</dark_purple>",
            "<aqua>Mines a 3x3 area.</aqua>");

    public SoulItemsSettings {
        Objects.requireNonNull(soulPickaxeName, "soulPickaxeName");
        soulPickaxeLore = List.copyOf(soulPickaxeLore);
        Objects.requireNonNull(reloadedMessage, "reloadedMessage");
        Objects.requireNonNull(usageMessage, "usageMessage");
        Objects.requireNonNull(noPermissionMessage, "noPermissionMessage");
    }

    public static SoulItemsSettings load(FileConfiguration config, Logger logger) {
        return new SoulItemsSettings(
                readNonBlank(config, logger, "soul-pickaxe.name", DEFAULT_NAME),
                readLore(config),
                config.getString(
                        "messages.reloaded",
                        "<green>LifestealSoulItems configuration reloaded.</green>"),
                config.getString("messages.usage", "<red>Usage: /soulitems reload</red>"),
                config.getString(
                        "messages.no-permission",
                        "<red>You do not have permission to use this command.</red>"));
    }

    private static List<String> readLore(FileConfiguration config) {
        if (!config.contains("soul-pickaxe.lore", true)) {
            return DEFAULT_LORE;
        }
        return List.copyOf(config.getStringList("soul-pickaxe.lore"));
    }

    private static String readNonBlank(
            FileConfiguration config, Logger logger, String path, String fallback) {
        String value = config.getString(path, fallback).trim();
        if (value.isEmpty()) {
            logger.warning("'" + path + "' cannot be blank; using the default value.");
            return fallback;
        }
        return value;
    }
}
