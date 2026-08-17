package dev.lifesteal.spawn.rescue;

import dev.lifesteal.spawn.config.SpawnSettings;
import dev.lifesteal.spawn.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class VoidRescueListener implements Listener {

    private static final long WARNING_INTERVAL_NANOS = Duration.ofSeconds(30L).toNanos();

    private final Plugin plugin;
    private final MessageService messages;
    private final Supplier<SpawnSettings> settingsSupplier;
    private final AfkDestinationResolver afkDestinationResolver;
    private final Set<UUID> rescuingPlayers = new HashSet<>();
    private long lastDestinationWarningNanos;

    public VoidRescueListener(
            Plugin plugin,
            MessageService messages,
            Supplier<SpawnSettings> settingsSupplier) {
        this.plugin = plugin;
        this.messages = messages;
        this.settingsSupplier = settingsSupplier;
        this.afkDestinationResolver = new AfkDestinationResolver(plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location destination = event.getTo();
        World world = destination.getWorld();
        SpawnSettings settings = settingsSupplier.get();
        Location afkDestination = resolveAfkDestination(player, settings, destination);
        if (afkDestination != null) {
            rescue(player, afkDestination, settings.afkRescuedMessage(), settings);
            return;
        }
        if (VoidRescueRules.shouldRescue(
                settings, world.getName(), world.getMinHeight(), destination.getY())) {
            rescueToSpawn(player, settings);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVoidDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.VOID
                || !(event.getEntity() instanceof Player player)) {
            return;
        }

        SpawnSettings settings = settingsSupplier.get();
        Location afkDestination = resolveAfkDestination(player, settings, player.getLocation());
        if (afkDestination != null
                && rescue(player, afkDestination, settings.afkRescuedMessage(), settings)) {
            event.setCancelled(true);
            return;
        }
        if (settings.voidRescueEnabled()
                && settings.isEnabledInWorld(player.getWorld().getName())
                && rescueToSpawn(player, settings)) {
            event.setCancelled(true);
        }
    }

    private boolean rescueToSpawn(Player player, SpawnSettings settings) {
        Location destination = resolveDestination(settings);
        if (destination == null) {
            return false;
        }
        return rescue(player, destination, settings.rescuedMessage(), settings);
    }

    private boolean rescue(
            Player player,
            Location destination,
            String message,
            SpawnSettings settings) {
        UUID playerId = player.getUniqueId();
        if (!rescuingPlayers.add(playerId)) {
            return true;
        }

        destination.getChunk().load();
        boolean teleported = player.teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN);
        if (!teleported) {
            rescuingPlayers.remove(playerId);
            return false;
        }

        player.setVelocity(new Vector(0D, 0D, 0D));
        player.setFallDistance(0F);
        messages.sendActionBar(player, message);
        playSound(player, settings);
        plugin.getServer().getScheduler().runTask(
                plugin, () -> rescuingPlayers.remove(playerId));
        return true;
    }

    private Location resolveAfkDestination(
            Player player, SpawnSettings settings, Location currentLocation) {
        if (!settings.afkRescueEnabled()
                || currentLocation.getY() >= settings.afkRescueTriggerY()) {
            return null;
        }

        Location destination = afkDestinationResolver.resolve(player.getYaw());
        if (destination == null
                || !VoidRescueRules.shouldRescueAfk(
                        settings,
                        currentLocation.getWorld().getName(),
                        destination.getWorld().getName(),
                        currentLocation.getY())) {
            return null;
        }
        if (destination.getY() < settings.afkRescueTriggerY()) {
            warnDestination("The /afk destination is below the AFK rescue threshold;"
                    + " teleport cancelled to avoid a loop.");
            return null;
        }
        return destination;
    }

    private Location resolveDestination(SpawnSettings settings) {
        World world = findWorld(settings.destinationWorldName());
        if (world == null) {
            warnDestination("Destination world '" + settings.destinationWorldName()
                    + "' is not loaded. Is the Multiverse world name correct?");
            return null;
        }

        Location destination = settings.useWorldSpawn()
                ? world.getSpawnLocation().clone()
                : new Location(
                        world,
                        settings.destinationX(),
                        settings.destinationY(),
                        settings.destinationZ(),
                        settings.destinationYaw(),
                        settings.destinationPitch());

        if (VoidRescueRules.shouldRescue(
                settings, world.getName(), world.getMinHeight(), destination.getY())) {
            warnDestination("Configured destination is below the void rescue threshold;"
                    + " teleport cancelled to avoid a loop.");
            return null;
        }
        return destination;
    }

    private World findWorld(String configuredName) {
        World exact = Bukkit.getWorld(configuredName);
        if (exact != null) {
            return exact;
        }
        for (World world : Bukkit.getWorlds()) {
            if (world.getName().equalsIgnoreCase(configuredName)) {
                return world;
            }
        }
        return null;
    }

    private void warnDestination(String message) {
        long now = System.nanoTime();
        if (lastDestinationWarningNanos == 0L
                || now - lastDestinationWarningNanos >= WARNING_INTERVAL_NANOS) {
            lastDestinationWarningNanos = now;
            plugin.getLogger().warning(message);
        }
    }

    private void playSound(Player player, SpawnSettings settings) {
        try {
            player.playSound(
                    player.getLocation(),
                    settings.soundName(),
                    settings.soundVolume(),
                    settings.soundPitch());
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Could not play sound '" + settings.soundName() + "': "
                    + exception.getMessage());
        }
    }
}
