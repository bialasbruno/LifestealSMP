package dev.lifesteal.core.heart;

import org.bukkit.Material;

/**
 * Constants shared across the heart system. Values that are meant to be server-configurable
 * (starting/min/max hearts) live in {@link dev.lifesteal.core.config.LifestealConfig} instead -
 * this class only holds values that are part of the plugin's fixed design.
 */
public final class HeartConstants {

    private HeartConstants() {
    }

    /** Vanilla max health points represented by a single heart. */
    public static final double HEALTH_PER_HEART = 2.0;

    /** Base vanilla material used for the Broken Heart custom item. */
    public static final Material BROKEN_HEART_MATERIAL = Material.GHAST_TEAR;

    /** Base vanilla material used for the full Heart custom item. */
    public static final Material HEART_MATERIAL = Material.NETHER_STAR;

    /** Base vanilla material used for the rare seasonal Revive Totem. */
    public static final Material REVIVE_TOTEM_MATERIAL = Material.TOTEM_OF_UNDYING;

    public static final int BROKEN_HEARTS_PER_HEART_RECIPE = 2;
    public static final int DIAMONDS_PER_HEART_RECIPE = 7;
}
