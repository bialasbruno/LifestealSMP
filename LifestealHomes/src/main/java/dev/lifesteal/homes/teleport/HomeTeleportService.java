package dev.lifesteal.homes.teleport;

import dev.lifesteal.homes.config.HomesSettings;
import dev.lifesteal.homes.data.StoredHome;
import dev.lifesteal.homes.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class HomeTeleportService implements Listener {

    private final Plugin plugin;
    private final MessageService messages;
    private final Supplier<HomesSettings> settingsSupplier;
    private final Map<UUID, PendingTeleport> pending = new HashMap<>();

    public HomeTeleportService(
            Plugin plugin, MessageService messages, Supplier<HomesSettings> settingsSupplier) {
        this.plugin = plugin;
        this.messages = messages;
        this.settingsSupplier = settingsSupplier;
    }

    public void start(Player player, StoredHome home) {
        cancel(player.getUniqueId(), false, null);

        World world = findWorld(home.worldName());
        if (world == null) {
            messages.send(
                    player,
                    settingsSupplier.get().message("world-unavailable"),
                    Map.of("world", home.worldName()));
            return;
        }

        Location destination = new Location(
                world, home.x(), home.y(), home.z(), home.yaw(), home.pitch());
        HomesSettings settings = settingsSupplier.get();
        if (settings.teleportDelaySeconds() == 0) {
            complete(player, home, destination);
            return;
        }

        Location origin = player.getLocation().clone();
        PendingTeleport operation = new PendingTeleport(origin, settings.teleportDelaySeconds());
        pending.put(player.getUniqueId(), operation);
        operation.task = new BukkitRunnable() {
            @Override
            public void run() {
                PendingTeleport current = pending.get(player.getUniqueId());
                if (current != operation || !player.isOnline()) {
                    cancel();
                    return;
                }
                if (operation.secondsRemaining > 0) {
                    messages.actionBar(
                            player,
                            settingsSupplier.get().message("teleport-countdown"),
                            Map.of(
                                    "seconds", Integer.toString(operation.secondsRemaining),
                                    "home", home.name()));
                    operation.secondsRemaining--;
                    return;
                }

                pending.remove(player.getUniqueId());
                cancel();
                complete(player, home, destination);
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        PendingTeleport operation = pending.get(event.getPlayer().getUniqueId());
        HomesSettings settings = settingsSupplier.get();
        if (operation == null || !settings.cancelOnMove() || event.getTo() == null) {
            return;
        }

        Location destination = event.getTo();
        Location origin = operation.origin;
        double tolerance = settings.movementTolerance();
        if (destination.getWorld() != origin.getWorld()
                || distanceSquared(destination, origin) > tolerance * tolerance) {
            cancel(
                    event.getPlayer().getUniqueId(),
                    true,
                    settings.message("teleport-cancelled-move"));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && settingsSupplier.get().cancelOnDamage()) {
            cancel(
                    player.getUniqueId(),
                    true,
                    settingsSupplier.get().message("teleport-cancelled-damage"));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer().getUniqueId(), false, null);
    }

    public void shutdown() {
        for (PendingTeleport operation : pending.values()) {
            if (operation.task != null) {
                operation.task.cancel();
            }
        }
        pending.clear();
    }

    private void complete(Player player, StoredHome home, Location destination) {
        boolean teleported = player.teleport(destination, PlayerTeleportEvent.TeleportCause.COMMAND);
        if (!teleported) {
            messages.send(player, settingsSupplier.get().message("teleport-failed"));
            return;
        }
        player.setFallDistance(0.0F);
        messages.send(
                player,
                settingsSupplier.get().message("teleported"),
                Map.of("home", home.name()));
    }

    private World findWorld(String name) {
        World exact = Bukkit.getWorld(name);
        if (exact != null) {
            return exact;
        }
        return Bukkit.getWorlds().stream()
                .filter(world -> world.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    private void cancel(UUID playerId, boolean notify, String template) {
        PendingTeleport operation = pending.remove(playerId);
        if (operation == null) {
            return;
        }
        if (operation.task != null) {
            operation.task.cancel();
        }
        Player player = Bukkit.getPlayer(playerId);
        if (notify && player != null && template != null) {
            messages.send(player, template);
        }
    }

    private static double distanceSquared(Location first, Location second) {
        double x = first.getX() - second.getX();
        double y = first.getY() - second.getY();
        double z = first.getZ() - second.getZ();
        return x * x + y * y + z * z;
    }

    private static final class PendingTeleport {

        private final Location origin;
        private int secondsRemaining;
        private BukkitTask task;

        private PendingTeleport(Location origin, int secondsRemaining) {
            this.origin = origin;
            this.secondsRemaining = secondsRemaining;
        }
    }
}
