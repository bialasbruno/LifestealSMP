package dev.lifesteal.souls.command;

import dev.lifesteal.souls.afk.AfkZone;
import dev.lifesteal.souls.afk.AfkZoneTracker;
import dev.lifesteal.souls.config.SoulsSettings;
import dev.lifesteal.souls.message.MessageService;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Supplier;

/** Teleports players to the exact center of the configured AFK cuboid. */
public final class AfkCommand implements CommandExecutor {

    private final Plugin plugin;
    private final MessageService messages;
    private final Supplier<SoulsSettings> settingsSupplier;
    private final AfkZone zone = new AfkZone();

    public AfkCommand(
            Plugin plugin,
            MessageService messages,
            Supplier<SoulsSettings> settingsSupplier) {
        this.plugin = plugin;
        this.messages = messages;
        this.settingsSupplier = settingsSupplier;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        SoulsSettings settings = settingsSupplier.get();
        if (!(sender instanceof Player player)) {
            messages.send(sender, settings.playerOnlyMessage());
            return true;
        }
        if (args.length > 0 || !zone.isConfigured(settings)) {
            messages.send(player, settings.afkUnavailableMessage());
            return true;
        }

        World world = plugin.getServer().getWorld(settings.afkWorldName());
        if (world == null) {
            messages.send(player, settings.afkUnavailableMessage());
            return true;
        }
        Location destination = zone.center(world, settings, player.getYaw());
        if (destination.getY() < world.getMinHeight()
                || destination.getY() >= world.getMaxHeight()) {
            messages.send(player, settings.afkUnavailableMessage());
            return true;
        }

        boolean teleported = player.teleport(
                destination, PlayerTeleportEvent.TeleportCause.COMMAND);
        if (teleported) {
            messages.sendActionBar(
                    player,
                    settings.afkTeleportedMessage(),
                    Map.of(
                            "time",
                            AfkZoneTracker.formatTime(
                                    settings.afkRewardIntervalMillis() / 1_000L)));
        } else {
            messages.send(player, settings.afkUnavailableMessage());
        }
        return true;
    }
}
