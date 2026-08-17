package dev.lifesteal.core.listener;

import dev.lifesteal.core.elimination.EliminationService;
import dev.lifesteal.core.heart.HeartService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

/**
 * Loads a player's persisted heart state when they join and drops it from the in-memory
 * cache when they quit. Their data remains safely stored in the database either way.
 */
public final class PlayerJoinListener implements Listener {

    private final Plugin plugin;
    private final HeartService heartService;
    private final EliminationService eliminationService;

    public PlayerJoinListener(
            Plugin plugin, HeartService heartService, EliminationService eliminationService) {
        this.plugin = plugin;
        this.heartService = heartService;
        this.eliminationService = eliminationService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!eliminationService.preparePlayerJoin(player)) {
            return;
        }
        heartService.loadPlayer(player.getUniqueId(), player.getName());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        heartService.unloadPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        // Reapply on the next tick, after the respawn entity has been fully initialized.
        plugin.getServer().getScheduler().runTask(plugin, () -> heartService.reapplyPlayer(player));
    }
}
