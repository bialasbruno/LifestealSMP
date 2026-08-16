package dev.lifesteal.core.config;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Typed, immutable view of {@code config.yml}. The defaults passed to every {@code getX} call
 * match the specification exactly (10 starting / 1 minimum / 20 maximum hearts) so the plugin
 * behaves correctly even if a value is missing from the file on disk.
 */
public final class LifestealConfig {

    private final int startingHearts;
    private final int minimumHearts;
    private final int maximumHearts;
    private final boolean dropBrokenHeartOnPvpDeath;
    private final String maximumHeartsMessage;

    public LifestealConfig(FileConfiguration config) {
        this.startingHearts = config.getInt("hearts.starting", 10);
        this.minimumHearts = config.getInt("hearts.minimum", 1);
        this.maximumHearts = config.getInt("hearts.maximum", 20);
        this.dropBrokenHeartOnPvpDeath = config.getBoolean("broken-heart.drop-on-pvp-death", true);
        this.maximumHeartsMessage = config.getString(
                "messages.maximum-hearts", "You already have the maximum number of hearts.");

        if (minimumHearts < 1) {
            throw new IllegalArgumentException("hearts.minimum must be at least 1");
        }
        if (maximumHearts < minimumHearts) {
            throw new IllegalArgumentException("hearts.maximum must be greater than or equal to hearts.minimum");
        }
        if (startingHearts < minimumHearts || startingHearts > maximumHearts) {
            throw new IllegalArgumentException("hearts.starting must be between hearts.minimum and hearts.maximum");
        }
    }

    public int startingHearts() {
        return startingHearts;
    }

    public int minimumHearts() {
        return minimumHearts;
    }

    public int maximumHearts() {
        return maximumHearts;
    }

    public boolean dropBrokenHeartOnPvpDeath() {
        return dropBrokenHeartOnPvpDeath;
    }

    public String maximumHeartsMessage() {
        return maximumHeartsMessage;
    }
}
