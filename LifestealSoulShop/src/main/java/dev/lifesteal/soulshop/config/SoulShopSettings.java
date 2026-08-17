package dev.lifesteal.soulshop.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Logger;

public record SoulShopSettings(
        long productPrice,
        Material fillerMaterial,
        String menuTitle,
        String balanceName,
        List<String> balanceLore,
        String productName,
        List<String> productLore,
        String closeName,
        String successMessage,
        String insufficientSoulsMessage,
        String inventoryFullMessage,
        String purchaseErrorMessage,
        String noPermissionMessage,
        String playerOnlyMessage,
        String usageMessage,
        String reloadedMessage,
        String successSound,
        float successSoundVolume,
        float successSoundPitch,
        String failureSound,
        float failureSoundVolume,
        float failureSoundPitch) {

    private static final Material DEFAULT_FILLER = Material.GRAY_STAINED_GLASS_PANE;

    public SoulShopSettings {
        Objects.requireNonNull(fillerMaterial, "fillerMaterial");
        Objects.requireNonNull(menuTitle, "menuTitle");
        Objects.requireNonNull(balanceName, "balanceName");
        balanceLore = List.copyOf(balanceLore);
        Objects.requireNonNull(productName, "productName");
        productLore = List.copyOf(productLore);
        Objects.requireNonNull(closeName, "closeName");
        Objects.requireNonNull(successMessage, "successMessage");
        Objects.requireNonNull(insufficientSoulsMessage, "insufficientSoulsMessage");
        Objects.requireNonNull(inventoryFullMessage, "inventoryFullMessage");
        Objects.requireNonNull(purchaseErrorMessage, "purchaseErrorMessage");
        Objects.requireNonNull(noPermissionMessage, "noPermissionMessage");
        Objects.requireNonNull(playerOnlyMessage, "playerOnlyMessage");
        Objects.requireNonNull(usageMessage, "usageMessage");
        Objects.requireNonNull(reloadedMessage, "reloadedMessage");
        Objects.requireNonNull(successSound, "successSound");
        Objects.requireNonNull(failureSound, "failureSound");
    }

    public static SoulShopSettings load(FileConfiguration config, Logger logger) {
        return new SoulShopSettings(
                readLong(config, logger, "product.price", 1L, 1_000_000_000L, 2_500L),
                readMaterial(config, logger, "menu.filler-material", DEFAULT_FILLER, true),
                config.getString(
                        "menu.title",
                        "<gradient:#38bdf8:#c084fc><bold>Soul Shop</bold></gradient>"),
                config.getString("menu.balance-name", "<aqua><bold>Your Souls</bold></aqua>"),
                readStringList(
                        config,
                        "menu.balance-lore",
                        List.of("", "<gray>Balance:</gray> <white>{balance}</white>")),
                config.getString(
                        "menu.product-name",
                        "<gradient:#22d3ee:#8b5cf6><bold>Soul Pickaxe</bold></gradient>"),
                readStringList(
                        config,
                        "menu.product-lore",
                        List.of(
                                "<gray>A netherite tool inhabited by restless souls.</gray>",
                                "<dark_purple>Their whispers guide every strike.</dark_purple>",
                                "",
                                "<gray>Price:</gray> <light_purple>{price} Souls</light_purple>",
                                "",
                                "<green>Click to buy</green>")),
                config.getString("menu.close-name", "<red><bold>Close</bold></red>"),
                config.getString(
                        "messages.success",
                        "<green>Purchased Soul Pickaxe for {price} Souls.</green>"
                                + " <gray>Balance: {balance}</gray>"),
                config.getString(
                        "messages.insufficient-souls",
                        "<red>You need {price} Souls.</red> <gray>Balance: {balance}</gray>"),
                config.getString("messages.inventory-full", "<red>Your inventory is full.</red>"),
                config.getString(
                        "messages.purchase-error",
                        "<red>The purchase could not be completed. Try again.</red>"),
                config.getString(
                        "messages.no-permission",
                        "<red>You do not have permission to use this command.</red>"),
                config.getString(
                        "messages.player-only",
                        "<red>This command can only be used by a player.</red>"),
                config.getString("messages.usage", "<red>Usage: /soulshop [reload]</red>"),
                config.getString(
                        "messages.reloaded",
                        "<green>LifestealSoulShop configuration reloaded.</green>"),
                readNonBlank(
                        config,
                        logger,
                        "sounds.success.name",
                        "minecraft:entity.experience_orb.pickup"),
                readFloat(config, logger, "sounds.success.volume", 0.0F, 10.0F, 0.8F),
                readFloat(config, logger, "sounds.success.pitch", 0.0F, 2.0F, 1.2F),
                readNonBlank(
                        config,
                        logger,
                        "sounds.failure.name",
                        "minecraft:entity.villager.no"),
                readFloat(config, logger, "sounds.failure.volume", 0.0F, 10.0F, 0.7F),
                readFloat(config, logger, "sounds.failure.pitch", 0.0F, 2.0F, 1.0F));
    }

    private static Material readMaterial(
            FileConfiguration config,
            Logger logger,
            String path,
            Material fallback,
            boolean requireItem) {
        String configured = config.getString(path, fallback.name());
        Material material = Material.matchMaterial(configured.toUpperCase(Locale.ROOT));
        if (material == null || material.isAir() || (requireItem && !material.isItem())) {
            logger.warning("'" + path + "' is not a valid item material; using " + fallback + '.');
            return fallback;
        }
        return material;
    }

    private static long readLong(
            FileConfiguration config,
            Logger logger,
            String path,
            long minimum,
            long maximum,
            long fallback) {
        long value = config.getLong(path, fallback);
        if (value < minimum || value > maximum) {
            logger.warning("'" + path + "' must be between " + minimum + " and " + maximum
                    + "; using " + fallback + '.');
            return fallback;
        }
        return value;
    }

    private static float readFloat(
            FileConfiguration config,
            Logger logger,
            String path,
            float minimum,
            float maximum,
            float fallback) {
        double configured = config.getDouble(path, fallback);
        if (!Double.isFinite(configured) || configured < minimum || configured > maximum) {
            logger.warning("'" + path + "' must be between " + minimum + " and " + maximum
                    + "; using " + fallback + '.');
            return fallback;
        }
        return (float) configured;
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

    private static List<String> readStringList(
            FileConfiguration config, String path, List<String> fallback) {
        List<String> values = config.getStringList(path);
        return values.isEmpty() ? fallback : values;
    }
}
