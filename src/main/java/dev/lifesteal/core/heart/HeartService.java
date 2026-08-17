package dev.lifesteal.core.heart;

import dev.lifesteal.core.config.LifestealConfig;
import dev.lifesteal.core.data.PlayerHeartRepository;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Coordinates a player's max-heart state between three places: the in-memory cache (fast,
 * authoritative at runtime), the Bukkit {@link Attribute#MAX_HEALTH} attribute (what the
 * client actually renders), and the {@link PlayerHeartRepository} persistence layer.
 * Player loads are synchronous single-row SQLite reads, which prevents a late asynchronous
 * load from overwriting a death or Heart consumption. Runtime writes are submitted to one
 * dedicated executor, preserving the exact order in which gameplay mutations occurred.
 */
public final class HeartService {

    private final Plugin plugin;
    private final PlayerHeartRepository repository;
    private final LifestealConfig config;
    private final Map<UUID, Integer> heartsCache = new ConcurrentHashMap<>();
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "LifestealCore-Database");
        thread.setDaemon(true);
        return thread;
    });
    private volatile boolean acceptingWrites = true;

    public HeartService(Plugin plugin, PlayerHeartRepository repository, LifestealConfig config) {
        this.plugin = plugin;
        this.repository = repository;
        this.config = config;
    }

    /**
     * Loads (or creates) a player's heart record and applies it immediately. Safe to call for
     * a player who is already loaded (e.g. after a /reload).
     */
    public void loadPlayer(UUID uuid, String lastKnownName) {
        int stored = repository.findHearts(uuid).orElseGet(() -> {
            int starting = config.startingHearts();
            repository.upsertHearts(uuid, lastKnownName, starting);
            return starting;
        });

        int clamped = HeartRules.clamp(stored, config.minimumHearts(), config.maximumHearts());
        if (clamped != stored) {
            repository.upsertHearts(uuid, lastKnownName, clamped);
        }

        heartsCache.put(uuid, clamped);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            applyMaxHealth(player, clamped);
        }
    }

    /** Drops a player's in-memory state. Their data remains safely persisted in the database. */
    public void unloadPlayer(UUID uuid) {
        heartsCache.remove(uuid);
    }

    /** Reapplies the cached attribute after a respawn without touching persistence. */
    public void reapplyPlayer(Player player) {
        applyMaxHealth(player, getHearts(player.getUniqueId()));
    }

    /** Returns the cached max hearts for a player, or the configured starting value if unknown. */
    public int getHearts(UUID uuid) {
        return heartsCache.getOrDefault(uuid, config.startingHearts());
    }

    /** Administrative override: sets a player's max hearts to an exact, clamped value. */
    public void setHearts(Player player, int hearts) {
        int clamped = HeartRules.clamp(hearts, config.minimumHearts(), config.maximumHearts());
        heartsCache.put(player.getUniqueId(), clamped);
        applyMaxHealth(player, clamped);
        persistAsync(player.getUniqueId(), player.getName(), clamped);
    }

    /**
     * Restores an offline eliminated player after all previously queued heart mutations.
     * Waiting for this ordered database operation prevents a death save from overwriting a
     * near-immediate Revive Totem restore.
     */
    public void restoreEliminatedPlayer(UUID uuid, String lastKnownName, int hearts) {
        int clamped = HeartRules.clamp(
                hearts, config.minimumHearts(), config.maximumHearts());
        Future<?> future = databaseExecutor.submit(
                () -> repository.restoreEliminatedPlayer(uuid, lastKnownName, clamped));
        try {
            future.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while restoring eliminated player " + uuid, exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Failed to restore eliminated player " + uuid, exception.getCause());
        } catch (java.util.concurrent.TimeoutException exception) {
            future.cancel(true);
            throw new IllegalStateException("Timed out while restoring eliminated player " + uuid, exception);
        }
    }

    /** Applies a PvP death to the victim's heart count. */
    public PvpDeathResult applyPvpDeath(Player victim) {
        int before = getHearts(victim.getUniqueId());
        int minimum = config.minimumHearts();

        int after = HeartRules.applyPvpDeath(before, minimum);
        boolean shouldDrop = HeartRules.shouldDropBrokenHeart(before, minimum);
        boolean shouldEliminate = HeartRules.shouldEliminateOnPvpDeath(before, minimum);
        boolean reviveTotemEligible = HeartRules.isReviveTotemDropEligible(
                before, config.maximumHearts());

        heartsCache.put(victim.getUniqueId(), after);
        applyMaxHealth(victim, after);
        persistAsync(victim.getUniqueId(), victim.getName(), after);

        return new PvpDeathResult(
                before, after, shouldDrop, shouldEliminate, reviveTotemEligible);
    }

    /** Attempts to consume a Heart item for the given player. */
    public HeartConsumptionResult consumeHeart(Player player) {
        int current = getHearts(player.getUniqueId());
        int maximum = config.maximumHearts();

        if (!HeartRules.canConsumeHeart(current, maximum)) {
            return new HeartConsumptionResult(current, current, false);
        }

        int after = HeartRules.applyHeartConsumption(current, maximum);
        heartsCache.put(player.getUniqueId(), after);
        applyMaxHealth(player, after);
        persistAsync(player.getUniqueId(), player.getName(), after);

        return new HeartConsumptionResult(current, after, true);
    }

    /**
     * Stops accepting asynchronous writes, drains all queued mutations in order, and performs
     * one final blocking snapshot save. Called exactly once during plugin disable.
     */
    public void shutdownAndSaveAll() {
        acceptingWrites = false;
        databaseExecutor.shutdown();
        try {
            if (!databaseExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Database writer did not stop within 10 seconds; cancelling queued work.");
                databaseExecutor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            databaseExecutor.shutdownNow();
            plugin.getLogger().warning("Interrupted while waiting for database writes to finish.");
        }

        heartsCache.forEach((uuid, hearts) -> {
            Player player = Bukkit.getPlayer(uuid);
            String name = player != null ? player.getName() : null;
            try {
                repository.upsertHearts(uuid, name, hearts);
            } catch (Exception exception) {
                plugin.getLogger().log(Level.SEVERE, "Failed to persist hearts for " + uuid, exception);
            }
        });
    }

    private void applyMaxHealth(Player player, int hearts) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (attribute == null) {
            return;
        }
        double newMax = hearts * HeartConstants.HEALTH_PER_HEART;
        attribute.setBaseValue(newMax);
        if (player.getHealth() > newMax) {
            player.setHealth(newMax);
        }
    }

    private void persistAsync(UUID uuid, String name, int hearts) {
        if (!acceptingWrites) {
            return;
        }
        try {
            databaseExecutor.execute(() -> {
                try {
                    repository.upsertHearts(uuid, name, hearts);
                } catch (Exception exception) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to persist hearts for " + uuid, exception);
                }
            });
        } catch (RejectedExecutionException exception) {
            plugin.getLogger().log(Level.WARNING, "Database writer rejected a save for " + uuid, exception);
        }
    }

    public record PvpDeathResult(
            int heartsBefore,
            int heartsAfter,
            boolean shouldDropBrokenHeart,
            boolean shouldEliminate,
            boolean reviveTotemEligible) {
    }

    public record HeartConsumptionResult(int heartsBefore, int heartsAfter, boolean consumed) {
    }
}
