package dev.lifesteal.souls.afk;

import dev.lifesteal.souls.config.SoulsSettings;
import dev.lifesteal.souls.data.SoulMutation;
import dev.lifesteal.souls.message.MessageService;
import dev.lifesteal.souls.service.SoulService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/** Displays a live countdown and rewards continuous time inside the AFK cuboid. */
public final class AfkZoneTracker {

    private final Plugin plugin;
    private final SoulService soulService;
    private final MessageService messages;
    private final AfkZone zone = new AfkZone();
    private final Map<UUID, Long> remainingSeconds = new HashMap<>();
    private SoulsSettings settings;
    private BukkitTask task;

    public AfkZoneTracker(
            Plugin plugin,
            SoulService soulService,
            MessageService messages,
            SoulsSettings settings) {
        this.plugin = plugin;
        this.soulService = soulService;
        this.messages = messages;
        this.settings = settings;
    }

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (UUID playerId : remainingSeconds.keySet()) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null) {
                messages.clearPersistentActionBar(player);
            }
        }
        remainingSeconds.clear();
    }

    public void updateSettings(SoulsSettings settings) {
        this.settings = settings;
        resetAllPlayers();
    }

    public boolean isInside(Player player) {
        return zone.contains(player.getLocation(), settings);
    }

    private void tick() {
        SoulsSettings current = settings;
        if (!zone.isConfigured(current)) {
            resetAllPlayers();
            return;
        }

        Set<UUID> currentlyInside = new HashSet<>();
        long intervalSeconds = Math.max(
                1L, TimeUnit.MILLISECONDS.toSeconds(current.afkRewardIntervalMillis()));
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!zone.contains(player.getLocation(), current)) {
                continue;
            }

            UUID playerId = player.getUniqueId();
            currentlyInside.add(playerId);
            long seconds = remainingSeconds.getOrDefault(playerId, intervalSeconds);
            if (seconds <= 0L) {
                remainingSeconds.put(
                        playerId, reward(player, current) ? intervalSeconds : 0L);
                continue;
            }

            messages.sendPersistentActionBar(
                    player,
                    current.afkCountdownMessage(),
                    Map.of(
                            "amount", Long.toString(current.afkRewardAmount()),
                            "time", formatTime(seconds)));
            remainingSeconds.put(playerId, seconds - 1L);
        }

        Set<UUID> playersWhoLeft = new HashSet<>(remainingSeconds.keySet());
        playersWhoLeft.removeAll(currentlyInside);
        for (UUID playerId : playersWhoLeft) {
            remainingSeconds.remove(playerId);
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null) {
                messages.clearPersistentActionBar(player);
            }
        }
    }

    private boolean reward(Player player, SoulsSettings current) {
        try {
            SoulMutation result = soulService.rewardAfk(player.getUniqueId(), player.getName());
            if (result.amount() <= 0L) {
                return true;
            }
            messages.sendActionBar(
                    player,
                    current.afkRewardMessage(),
                    Map.of(
                            "amount", Long.toString(result.amount()),
                            "balance", Long.toString(result.balance())));
            return true;
        } catch (RuntimeException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Failed to grant an AFK reward to " + player.getUniqueId()
                            + "; it will be retried.",
                    exception);
            return false;
        }
    }

    private void resetAllPlayers() {
        if (remainingSeconds.isEmpty()) {
            return;
        }
        for (UUID playerId : remainingSeconds.keySet()) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null) {
                messages.clearPersistentActionBar(player);
            }
        }
        remainingSeconds.clear();
    }

    public static String formatTime(long totalSeconds) {
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
