package dev.lifesteal.souls.playtime;

import dev.lifesteal.souls.config.SoulsSettings;
import dev.lifesteal.souls.data.PlaytimeRewardResult;
import dev.lifesteal.souls.data.SoulAccount;
import dev.lifesteal.souls.message.MessageService;
import dev.lifesteal.souls.service.SoulService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/** Accumulates active online time and persists it in coarse, crash-bounded batches. */
public final class PlaytimeTracker {

    private static final long MAX_TICK_ELAPSED_MILLIS = 5_000L;

    private final Plugin plugin;
    private final SoulService soulService;
    private final MessageService messages;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private SoulsSettings settings;
    private BukkitTask task;
    private long lastTickNanos;
    private long lastFlushNanos;

    public PlaytimeTracker(
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
        long now = System.nanoTime();
        lastTickNanos = now;
        lastFlushNanos = now;
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void updateSettings(SoulsSettings settings) {
        this.settings = settings;
    }

    public void join(Player player, SoulAccount account) {
        sessions.put(
                player.getUniqueId(),
                new Session(player.getName(), account.activeProgressMillis(), System.nanoTime()));
    }

    public void markActivity(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (session != null) {
            session.lastActivityNanos = System.nanoTime();
            session.playerName = player.getName();
        }
    }

    public void quit(Player player) {
        Session session = sessions.remove(player.getUniqueId());
        if (session != null) {
            flush(player.getUniqueId(), session, player);
        }
    }

    public void stopAndFlush() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        tick();
        for (Map.Entry<UUID, Session> entry : sessions.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            flush(entry.getKey(), entry.getValue(), player);
        }
        sessions.clear();
    }

    private void tick() {
        long now = System.nanoTime();
        long elapsedMillis = Math.min(
                MAX_TICK_ELAPSED_MILLIS,
                Math.max(0L, TimeUnit.NANOSECONDS.toMillis(now - lastTickNanos)));
        lastTickNanos = now;

        SoulsSettings current = settings;
        if (current.playtimeEnabled() && elapsedMillis > 0L) {
            long idleTimeoutNanos = TimeUnit.MILLISECONDS.toNanos(current.idleTimeoutMillis());
            for (Session session : sessions.values()) {
                if (now - session.lastActivityNanos <= idleTimeoutNanos) {
                    session.pendingActiveMillis = Math.addExact(
                            session.pendingActiveMillis, elapsedMillis);
                }
            }
        }

        boolean periodicFlush = now - lastFlushNanos
                >= TimeUnit.MILLISECONDS.toNanos(current.flushIntervalMillis());
        for (Map.Entry<UUID, Session> entry : sessions.entrySet()) {
            Session session = entry.getValue();
            boolean rewardDue = session.persistedProgressMillis + session.pendingActiveMillis
                    >= current.playtimeRewardIntervalMillis();
            if (session.pendingActiveMillis > 0L && (periodicFlush || rewardDue)) {
                Player player = plugin.getServer().getPlayer(entry.getKey());
                flush(entry.getKey(), session, player);
            }
        }
        if (periodicFlush) {
            lastFlushNanos = now;
        }
    }

    private void flush(UUID playerId, Session session, Player onlinePlayer) {
        long elapsedMillis = session.pendingActiveMillis;
        if (elapsedMillis <= 0L) {
            return;
        }
        try {
            PlaytimeRewardResult result = soulService.addActivePlaytime(
                    playerId, session.playerName, elapsedMillis);
            session.pendingActiveMillis = 0L;
            session.persistedProgressMillis = result.activeProgressMillis();
            if (result.credited() > 0L && onlinePlayer != null && onlinePlayer.isOnline()) {
                messages.send(
                        onlinePlayer,
                        settings.playtimeRewardMessage(),
                        Map.of(
                                "amount", Long.toString(result.credited()),
                                "balance", Long.toString(result.balance())));
            }
        } catch (RuntimeException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Failed to save active playtime for " + playerId + "; it will be retried.",
                    exception);
        }
    }

    private static final class Session {
        private String playerName;
        private long persistedProgressMillis;
        private long pendingActiveMillis;
        private long lastActivityNanos;

        private Session(String playerName, long persistedProgressMillis, long lastActivityNanos) {
            this.playerName = playerName;
            this.persistedProgressMillis = persistedProgressMillis;
            this.lastActivityNanos = lastActivityNanos;
        }
    }
}
