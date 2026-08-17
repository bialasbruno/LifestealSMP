package dev.lifesteal.core.elimination;

import com.destroystokyo.paper.profile.PlayerProfile;
import dev.lifesteal.core.config.LifestealConfig;
import dev.lifesteal.core.data.EliminationRecord;
import dev.lifesteal.core.data.PlayerHeartRepository;
import dev.lifesteal.core.heart.HeartRules;
import dev.lifesteal.core.heart.HeartService;
import io.papermc.paper.ban.BanListType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/** Coordinates seasonal Revive Totem claims, temporary eliminations and revives. */
public final class EliminationService {

    private static final long EXPIRY_CHECK_PERIOD_TICKS = 20L * 60L;
    private static final long RESTORE_RETRY_BAN_SECONDS = 60L;

    private final Plugin plugin;
    private final PlayerHeartRepository repository;
    private final HeartService heartService;
    private final LifestealConfig config;
    private BukkitTask expiryTask;

    public EliminationService(
            Plugin plugin,
            PlayerHeartRepository repository,
            HeartService heartService,
            LifestealConfig config) {
        this.plugin = plugin;
        this.repository = repository;
        this.heartService = heartService;
        this.config = config;
    }

    /** Starts the minute-level cleanup that restores naturally expired eliminations. */
    public void start() {
        processExpiredEliminations();
        expiryTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin, this::processExpiredEliminations,
                EXPIRY_CHECK_PERIOD_TICKS, EXPIRY_CHECK_PERIOD_TICKS);
    }

    public void stop() {
        if (expiryTask != null) {
            expiryTask.cancel();
            expiryTask = null;
        }
    }

    /** Returns true only for a victim's first maximum-heart PvP death in this season. */
    public boolean claimSeasonalReviveTotem(Player victim, Player killer) {
        try {
            return repository.claimReviveTotemDrop(
                    config.seasonId(), victim.getUniqueId(), killer.getUniqueId(), Instant.now());
        } catch (RuntimeException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Failed to claim seasonal Revive Totem for " + victim.getUniqueId(),
                    exception);
            return false;
        }
    }

    /** Schedules the actual ban for the tick after Paper finishes processing the death. */
    public void eliminateAfterDeath(Player victim) {
        UUID uuid = victim.getUniqueId();
        String name = victim.getName();
        plugin.getServer().getScheduler().runTask(plugin, () -> eliminate(uuid, name));
    }

    private void eliminate(UUID uuid, String name) {
        Instant bannedUntil = Instant.now().plus(config.eliminationBanDuration());
        repository.saveElimination(uuid, name, bannedUntil);

        PlayerProfile profile = Bukkit.createProfile(uuid, name);
        String banReason = renderedBanReason();
        profileBanList().addBan(profile, banReason, bannedUntil, plugin.getName());

        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            online.kick(Component.text(banReason, NamedTextColor.RED));
        }

        Bukkit.broadcast(Component.text(
                name + " was eliminated for " + config.eliminationBanDuration().toHours()
                        + " hours.",
                NamedTextColor.RED));
    }

    /**
     * Repairs an expired elimination before the normal heart load, or rejects an impossible
     * early join if the native ban list was externally modified.
     */
    public boolean preparePlayerJoin(Player player) {
        Optional<EliminationRecord> elimination = repository.findElimination(player.getUniqueId());
        if (elimination.isEmpty()) {
            return true;
        }

        EliminationRecord record = elimination.get();
        if (record.bannedUntil().isAfter(Instant.now())) {
            player.kick(Component.text(renderedBanReason(), NamedTextColor.RED));
            return false;
        }

        restore(record, naturalReturnHearts());
        player.sendMessage(Component.text(
                "Your elimination has expired. You return with "
                        + naturalReturnHearts() + " hearts.",
                NamedTextColor.GREEN));
        return true;
    }

    /** Attempts to revive an actively eliminated player by their last known name. */
    public ReviveResult revive(String playerName) {
        Optional<EliminationRecord> found = repository.findEliminationByName(playerName);
        if (found.isEmpty()) {
            return new ReviveResult(ReviveStatus.NOT_ELIMINATED, playerName, 0);
        }

        EliminationRecord record = found.get();
        if (!record.bannedUntil().isAfter(Instant.now())) {
            restore(record, naturalReturnHearts());
            return new ReviveResult(
                    ReviveStatus.BAN_ALREADY_EXPIRED, record.lastKnownName(), naturalReturnHearts());
        }

        int restoredHearts = reviveReturnHearts();
        restore(record, restoredHearts);

        Bukkit.broadcast(Component.text(
                record.lastKnownName() + " was revived and will return with "
                        + restoredHearts + " hearts!",
                NamedTextColor.GOLD));
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.playSound(online.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
        }

        return new ReviveResult(ReviveStatus.SUCCESS, record.lastKnownName(), restoredHearts);
    }

    public List<String> activeEliminatedNames() {
        return repository.findActiveEliminatedNames(Instant.now());
    }

    private void processExpiredEliminations() {
        Instant now = Instant.now();
        for (EliminationRecord record : repository.findExpiredEliminations(now)) {
            try {
                restore(record, naturalReturnHearts());
                plugin.getLogger().info(
                        "Elimination expired for " + record.lastKnownName()
                                + "; restored " + naturalReturnHearts() + " hearts.");
            } catch (RuntimeException exception) {
                plugin.getLogger().log(
                        Level.SEVERE,
                        "Failed to restore expired elimination for " + record.playerUuid(),
                        exception);
            }
        }
    }

    private void restore(EliminationRecord record, int hearts) {
        ProfileBanList banList = profileBanList();
        PlayerProfile profile = Bukkit.createProfile(
                record.playerUuid(), record.lastKnownName());
        banList.pardon(profile);

        try {
            heartService.restoreEliminatedPlayer(
                    record.playerUuid(), record.lastKnownName(), hearts);
        } catch (RuntimeException exception) {
            Instant now = Instant.now();
            Instant retryUntil = record.bannedUntil().isAfter(now)
                    ? record.bannedUntil()
                    : now.plusSeconds(RESTORE_RETRY_BAN_SECONDS);
            banList.addBan(profile, renderedBanReason(), retryUntil, plugin.getName());
            throw exception;
        }
    }

    private int naturalReturnHearts() {
        return HeartRules.clamp(
                config.eliminationReturnHearts(), config.minimumHearts(), config.maximumHearts());
    }

    private int reviveReturnHearts() {
        return HeartRules.clamp(
                config.reviveReturnHearts(), config.minimumHearts(), config.maximumHearts());
    }

    private ProfileBanList profileBanList() {
        return Bukkit.getBanList(BanListType.PROFILE);
    }

    private String renderedBanReason() {
        return config.eliminationBanReason()
                .replace("{hours}", Long.toString(config.eliminationBanDuration().toHours()))
                .replace("{return_hearts}", Integer.toString(naturalReturnHearts()));
    }

    public enum ReviveStatus {
        SUCCESS,
        NOT_ELIMINATED,
        BAN_ALREADY_EXPIRED
    }

    public record ReviveResult(ReviveStatus status, String playerName, int restoredHearts) {
    }
}
