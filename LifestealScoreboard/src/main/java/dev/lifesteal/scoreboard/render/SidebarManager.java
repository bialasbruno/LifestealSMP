package dev.lifesteal.scoreboard.render;

import dev.lifesteal.scoreboard.config.ScoreboardSettings;
import dev.lifesteal.scoreboard.placeholder.PlaceholderResolver;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/** Owns sidebar lifecycle, refresh scheduling, and session-only player toggles. */
public final class SidebarManager implements Listener {

    private final Plugin plugin;
    private final PlaceholderResolver placeholders;
    private final ScoreboardManager scoreboardManager;
    private final MiniMessage miniMessage = MiniMessage.builder().strict(true).build();
    private final Map<UUID, PlayerSidebar> sidebars = new HashMap<>();
    private final Set<UUID> hiddenPlayers = new HashSet<>();
    private final Set<UUID> loggedRenderFailures = new HashSet<>();
    private ScoreboardSettings settings;
    private BukkitTask refreshTask;

    public SidebarManager(
            Plugin plugin,
            PlaceholderResolver placeholders,
            ScoreboardManager scoreboardManager) {
        this.plugin = plugin;
        this.placeholders = placeholders;
        this.scoreboardManager = scoreboardManager;
    }

    public void start(ScoreboardSettings initialSettings) {
        reload(initialSettings);
    }

    public void reload(ScoreboardSettings newSettings) {
        stopRefreshTask();
        settings = newSettings;
        loggedRenderFailures.clear();

        if (!settings.enabled()) {
            closeAll();
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!hiddenPlayers.contains(player.getUniqueId())) {
                refreshPlayer(player, true);
            }
        }
        refreshTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin, this::refreshAll,
                settings.updateIntervalTicks(), settings.updateIntervalTicks());
    }

    public ToggleResult toggle(Player player) {
        if (!settings.enabled()) {
            return ToggleResult.GLOBALLY_DISABLED;
        }
        UUID playerId = player.getUniqueId();
        if (hiddenPlayers.remove(playerId)) {
            refreshPlayer(player, true);
            return ToggleResult.ENABLED;
        }

        hiddenPlayers.add(playerId);
        closeSidebar(player);
        return ToggleResult.DISABLED;
    }

    public void shutdown() {
        stopRefreshTask();
        closeAll();
        hiddenPlayers.clear();
        loggedRenderFailures.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline() && settings.enabled()
                    && !hiddenPlayers.contains(playerId)) {
                refreshPlayer(player, true);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        PlayerSidebar sidebar = sidebars.remove(playerId);
        if (sidebar != null) {
            sidebar.destroy();
        }
        hiddenPlayers.remove(playerId);
        loggedRenderFailures.remove(playerId);
    }

    private void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!hiddenPlayers.contains(player.getUniqueId())) {
                refreshPlayer(player, false);
            }
        }
    }

    private void refreshPlayer(Player player, boolean attach) {
        UUID playerId = player.getUniqueId();
        try {
            PlayerSidebar sidebar = sidebars.computeIfAbsent(
                    playerId, ignored -> new PlayerSidebar(player, scoreboardManager));
            if (attach) {
                sidebar.attach(player);
            }
            sidebar.render(player, settings, placeholders, miniMessage);
            loggedRenderFailures.remove(playerId);
        } catch (RuntimeException exception) {
            if (loggedRenderFailures.add(playerId)) {
                plugin.getLogger().log(
                        Level.SEVERE,
                        "Failed to render the sidebar for " + player.getName()
                                + "; further identical refresh failures will be suppressed.",
                        exception);
            }
        }
    }

    private void closeSidebar(Player player) {
        PlayerSidebar sidebar = sidebars.remove(player.getUniqueId());
        if (sidebar != null) {
            sidebar.close(player);
        }
        loggedRenderFailures.remove(player.getUniqueId());
    }

    private void closeAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            closeSidebar(player);
        }
        for (PlayerSidebar sidebar : sidebars.values()) {
            sidebar.destroy();
        }
        sidebars.clear();
    }

    private void stopRefreshTask() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    public enum ToggleResult {
        ENABLED,
        DISABLED,
        GLOBALLY_DISABLED
    }
}
