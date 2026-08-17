package dev.lifesteal.souls.listener;

import dev.lifesteal.souls.data.SoulAccount;
import dev.lifesteal.souls.playtime.PlaytimeTracker;
import dev.lifesteal.souls.service.SoulService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerLifecycleListener implements Listener {

    private final SoulService soulService;
    private final PlaytimeTracker playtimeTracker;

    public PlayerLifecycleListener(SoulService soulService, PlaytimeTracker playtimeTracker) {
        this.soulService = soulService;
        this.playtimeTracker = playtimeTracker;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        SoulAccount account = soulService.loadPlayer(
                event.getPlayer().getUniqueId(), event.getPlayer().getName());
        playtimeTracker.join(event.getPlayer(), account);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        playtimeTracker.quit(event.getPlayer());
        soulService.unloadPlayer(event.getPlayer().getUniqueId());
    }
}
