package dev.lifesteal.souls.listener;

import dev.lifesteal.souls.afk.AfkZone;
import dev.lifesteal.souls.config.SoulsSettings;
import dev.lifesteal.souls.message.MessageService;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.function.Supplier;

/** Blocks player-caused damage across the AFK zone boundary in both directions. */
public final class AfkPvpProtectionListener implements Listener {

    private final MessageService messages;
    private final Supplier<SoulsSettings> settingsSupplier;
    private final AfkZone zone = new AfkZone();

    public AfkPvpProtectionListener(
            MessageService messages, Supplier<SoulsSettings> settingsSupplier) {
        this.messages = messages;
        this.settingsSupplier = settingsSupplier;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = resolvePlayer(event.getDamager());
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        SoulsSettings settings = settingsSupplier.get();
        if (!settings.afkPvpDisabled()
                || (!zone.contains(attacker.getLocation(), settings)
                        && !zone.contains(victim.getLocation(), settings))) {
            return;
        }

        event.setCancelled(true);
        messages.sendActionBar(attacker, settings.afkPvpDisabledMessage(), java.util.Map.of());
    }

    private static Player resolvePlayer(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            return shooter instanceof Player player ? player : null;
        }
        if (damager instanceof Tameable tameable) {
            AnimalTamer owner = tameable.getOwner();
            return owner instanceof Player player ? player : null;
        }
        if (damager instanceof TNTPrimed tnt && tnt.getSource() instanceof Player player) {
            return player;
        }
        return null;
    }
}
