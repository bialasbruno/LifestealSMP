package dev.lifesteal.souls.listener;

import dev.lifesteal.souls.afk.AfkZone;
import dev.lifesteal.souls.afk.PortalRepelRules;
import dev.lifesteal.souls.config.SoulsSettings;
import dev.lifesteal.souls.message.MessageService;
import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Orientable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Prevents Nether travel from the AFK zone and immediately pushes players out of portals. */
public final class AfkPortalRepelListener implements Listener {

    private static final long REPEL_COOLDOWN_NANOS = 750_000_000L;

    private final MessageService messages;
    private final Supplier<SoulsSettings> settingsSupplier;
    private final AfkZone zone = new AfkZone();
    private final Map<UUID, Long> lastRepelAt = new HashMap<>();

    public AfkPortalRepelListener(
            MessageService messages, Supplier<SoulsSettings> settingsSupplier) {
        this.messages = messages;
        this.settingsSupplier = settingsSupplier;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event instanceof PlayerTeleportEvent || event.getTo() == null) {
            return;
        }
        SoulsSettings settings = settingsSupplier.get();
        if (!settings.afkPortalRepelEnabled() || !zone.contains(event.getTo(), settings)) {
            return;
        }

        Block portal = portalAt(event.getTo());
        if (portal != null) {
            repel(event.getPlayer(), portal, event.getFrom(), event.getTo(), settings);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        SoulsSettings settings = settingsSupplier.get();
        if (!settings.afkPortalRepelEnabled()
                || event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL
                || !zone.contains(event.getFrom(), settings)) {
            return;
        }

        event.setCancelled(true);
        Block portal = portalAt(event.getFrom());
        if (portal != null) {
            Location facingBack = event.getFrom().clone()
                    .subtract(event.getPlayer().getLocation().getDirection());
            repel(event.getPlayer(), portal, facingBack, event.getFrom(), settings);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastRepelAt.remove(event.getPlayer().getUniqueId());
    }

    private void repel(
            Player player,
            Block portal,
            Location previous,
            Location current,
            SoulsSettings settings) {
        long now = System.nanoTime();
        Long previousRepel = lastRepelAt.get(player.getUniqueId());
        if (previousRepel != null && now - previousRepel < REPEL_COOLDOWN_NANOS) {
            return;
        }
        lastRepelAt.put(player.getUniqueId(), now);

        PortalRepelRules.PortalAxis axis = portalAxis(portal);
        PortalRepelRules.PushDirection direction = PortalRepelRules.awayFromPortal(
                axis,
                previous.getX(),
                previous.getZ(),
                portal.getX() + 0.5D,
                portal.getZ() + 0.5D,
                previous.getX() - current.getX(),
                previous.getZ() - current.getZ());
        player.setVelocity(new Vector(
                direction.x() * settings.afkPortalHorizontalStrength(),
                settings.afkPortalVerticalStrength(),
                direction.z() * settings.afkPortalHorizontalStrength()));
        player.setFallDistance(0.0F);
        messages.sendActionBar(player, settings.afkPortalRepelMessage(), Map.of());
    }

    private static Block portalAt(Location location) {
        Block feet = location.getBlock();
        if (feet.getType() == Material.NETHER_PORTAL) {
            return feet;
        }
        Block head = feet.getRelative(0, 1, 0);
        return head.getType() == Material.NETHER_PORTAL ? head : null;
    }

    private static PortalRepelRules.PortalAxis portalAxis(Block portal) {
        if (portal.getBlockData() instanceof Orientable orientable
                && orientable.getAxis() == Axis.Z) {
            return PortalRepelRules.PortalAxis.Z;
        }
        return PortalRepelRules.PortalAxis.X;
    }
}
