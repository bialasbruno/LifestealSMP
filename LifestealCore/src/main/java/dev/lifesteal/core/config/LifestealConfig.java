package dev.lifesteal.core.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.time.Duration;

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
    private final String seasonId;
    private final Duration eliminationBanDuration;
    private final int eliminationReturnHearts;
    private final int reviveReturnHearts;
    private final String eliminationBanReason;

    public LifestealConfig(FileConfiguration config) {
        this.startingHearts = config.getInt("hearts.starting", 10);
        this.minimumHearts = config.getInt("hearts.minimum", 1);
        this.maximumHearts = config.getInt("hearts.maximum", 20);
        this.dropBrokenHeartOnPvpDeath = config.getBoolean("broken-heart.drop-on-pvp-death", true);
        this.maximumHeartsMessage = config.getString(
                "messages.maximum-hearts", "You already have the maximum number of hearts.");
        String configuredSeasonId = config.getString("season.id", "1");
        this.seasonId = configuredSeasonId == null ? "" : configuredSeasonId.trim();
        long banDurationHours = config.getLong("elimination.ban-duration-hours", 24L);
        this.eliminationBanDuration = Duration.ofHours(banDurationHours);
        this.eliminationReturnHearts = config.getInt("elimination.return-hearts", 3);
        this.reviveReturnHearts = config.getInt("revive.return-hearts", 10);
        this.eliminationBanReason = config.getString(
                "elimination.ban-reason",
                "You were eliminated with one heart. You can return in {hours} hours with "
                        + "{return_hearts} hearts or be revived.");

        if (minimumHearts < 1) {
            throw new IllegalArgumentException("hearts.minimum must be at least 1");
        }
        if (maximumHearts < minimumHearts) {
            throw new IllegalArgumentException("hearts.maximum must be greater than or equal to hearts.minimum");
        }
        if (startingHearts < minimumHearts || startingHearts > maximumHearts) {
            throw new IllegalArgumentException("hearts.starting must be between hearts.minimum and hearts.maximum");
        }
        if (seasonId.isBlank()) {
            throw new IllegalArgumentException("season.id must not be blank");
        }
        if (banDurationHours < 1) {
            throw new IllegalArgumentException("elimination.ban-duration-hours must be at least 1");
        }
        if (eliminationReturnHearts < 1) {
            throw new IllegalArgumentException("elimination.return-hearts must be at least 1");
        }
        if (reviveReturnHearts < 1) {
            throw new IllegalArgumentException("revive.return-hearts must be at least 1");
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

    public String seasonId() {
        return seasonId;
    }

    public Duration eliminationBanDuration() {
        return eliminationBanDuration;
    }

    public int eliminationReturnHearts() {
        return eliminationReturnHearts;
    }

    public int reviveReturnHearts() {
        return reviveReturnHearts;
    }

    public String eliminationBanReason() {
        return eliminationBanReason;
    }
}
