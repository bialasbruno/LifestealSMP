package dev.lifesteal.core.listener;

import dev.lifesteal.core.elimination.EliminationService;
import dev.lifesteal.core.heart.HeartItemFactory;
import dev.lifesteal.core.heart.HeartService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Applies the lifesteal heart-loss mechanic on PvP deaths.
 *
 * <p>Attribution uses Paper's combat-tracker result exposed by {@link Player#getKiller()}.
 * Projectile and other deaths attributed by Paper to a player count as PvP; deaths without an
 * identified player killer remain non-PvP and never change hearts.</p>
 */
public final class PlayerDeathListener implements Listener {

    private final HeartService heartService;
    private final HeartItemFactory itemFactory;
    private final EliminationService eliminationService;
    private final boolean dropBrokenHeartOnPvpDeath;

    public PlayerDeathListener(
            HeartService heartService,
            HeartItemFactory itemFactory,
            EliminationService eliminationService,
            boolean dropBrokenHeartOnPvpDeath) {
        this.heartService = heartService;
        this.itemFactory = itemFactory;
        this.eliminationService = eliminationService;
        this.dropBrokenHeartOnPvpDeath = dropBrokenHeartOnPvpDeath;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null) {
            // Environmental death or a death Paper could not attribute to a player.
            return;
        }
        if (killer.getUniqueId().equals(victim.getUniqueId())) {
            // Player "killed themselves" in an unusual way; not a genuine PvP kill.
            return;
        }

        HeartService.PvpDeathResult result = heartService.applyPvpDeath(victim);

        if (dropBrokenHeartOnPvpDeath && result.shouldDropBrokenHeart()) {
            World world = victim.getWorld();
            Location location = victim.getLocation();
            world.dropItemNaturally(location, itemFactory.createBrokenHeart(1));
        }

        if (result.reviveTotemEligible()
                && eliminationService.claimSeasonalReviveTotem(victim, killer)) {
            victim.getWorld().dropItemNaturally(
                    victim.getLocation(), itemFactory.createReviveTotem(1));
            Bukkit.broadcast(Component.text(
                    killer.getName() + " claimed " + victim.getName()
                            + "'s one Revive Totem for this season!",
                    NamedTextColor.GOLD));
        }

        if (result.shouldEliminate()) {
            eliminationService.eliminateAfterDeath(victim);
        }
    }
}
