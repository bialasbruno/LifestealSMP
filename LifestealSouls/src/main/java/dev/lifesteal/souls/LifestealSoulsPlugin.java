package dev.lifesteal.souls;

import dev.lifesteal.souls.api.LifestealSoulsApi;
import dev.lifesteal.souls.command.SoulsAdminCommand;
import dev.lifesteal.souls.command.SoulsCommand;
import dev.lifesteal.souls.config.SoulsSettings;
import dev.lifesteal.souls.data.SQLiteSoulRepository;
import dev.lifesteal.souls.data.SoulAccount;
import dev.lifesteal.souls.data.SoulRepository;
import dev.lifesteal.souls.integration.ScoreboardCurrencyIntegration;
import dev.lifesteal.souls.menu.SoulLeaderboardMenu;
import dev.lifesteal.souls.listener.KillRewardListener;
import dev.lifesteal.souls.listener.PlayerActivityListener;
import dev.lifesteal.souls.listener.PlayerLifecycleListener;
import dev.lifesteal.souls.message.MessageService;
import dev.lifesteal.souls.playtime.PlaytimeTracker;
import dev.lifesteal.souls.service.SoulService;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;

public final class LifestealSoulsPlugin extends JavaPlugin implements LifestealSoulsApi {

    private SoulRepository repository;
    private SoulService soulService;
    private PlaytimeTracker playtimeTracker;
    private ScoreboardCurrencyIntegration scoreboardIntegration;
    private SoulsSettings settings;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = SoulsSettings.load(getConfig(), getLogger());
        warnAboutReservedAfkZone();

        repository = new SQLiteSoulRepository(new File(getDataFolder(), "data.db"), getLogger());
        soulService = new SoulService(repository, settings);
        MessageService messages = new MessageService(this);
        playtimeTracker = new PlaytimeTracker(this, soulService, messages, settings);

        SoulLeaderboardMenu leaderboardMenu = new SoulLeaderboardMenu(soulService);
        registerCommands(messages, leaderboardMenu);
        registerListeners(messages, leaderboardMenu);
        getServer().getServicesManager().register(
                LifestealSoulsApi.class, this, this, ServicePriority.Normal);

        scoreboardIntegration = new ScoreboardCurrencyIntegration(this, this);
        getServer().getPluginManager().registerEvents(scoreboardIntegration, this);
        scoreboardIntegration.start();

        for (Player player : getServer().getOnlinePlayers()) {
            SoulAccount account = soulService.loadPlayer(player.getUniqueId(), player.getName());
            playtimeTracker.join(player, account);
        }
        playtimeTracker.start();

        getLogger().info("LifestealSouls v" + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (scoreboardIntegration != null) {
            scoreboardIntegration.stop();
            scoreboardIntegration = null;
        }
        if (playtimeTracker != null) {
            playtimeTracker.stopAndFlush();
            playtimeTracker = null;
        }
        getServer().getServicesManager().unregisterAll(this);
        if (repository != null) {
            repository.close();
            repository = null;
        }
        soulService = null;
        getLogger().info("LifestealSouls disabled.");
    }

    @Override
    public long getSouls(UUID playerId) {
        requireEnabled();
        return soulService.getSouls(playerId);
    }

    @Override
    public boolean trySpend(UUID playerId, long amount, String reason) {
        requireEnabled();
        if (amount <= 0L) {
            throw new IllegalArgumentException("Spend amount must be positive");
        }
        return soulService.trySpend(playerId, amount, reason);
    }

    public SoulsSettings settings() {
        return settings;
    }

    public void reloadSoulsSettings() {
        reloadConfig();
        settings = SoulsSettings.load(getConfig(), getLogger());
        soulService.updateSettings(settings);
        playtimeTracker.updateSettings(settings);
        warnAboutReservedAfkZone();
    }

    private void registerCommands(
            MessageService messages, SoulLeaderboardMenu leaderboardMenu) {
        PluginCommand souls = getCommand("souls");
        if (souls != null) {
            souls.setExecutor(
                    new SoulsCommand(soulService, messages, leaderboardMenu, this::settings));
        } else {
            getLogger().severe("Could not register /souls; check plugin.yml.");
        }

        SoulsAdminCommand adminExecutor = new SoulsAdminCommand(this, soulService, messages);
        PluginCommand admin = getCommand("soulsadmin");
        if (admin != null) {
            admin.setExecutor(adminExecutor);
            admin.setTabCompleter(adminExecutor);
        } else {
            getLogger().severe("Could not register /soulsadmin; check plugin.yml.");
        }
    }

    private void registerListeners(
            MessageService messages, SoulLeaderboardMenu leaderboardMenu) {
        var pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(leaderboardMenu, this);
        pluginManager.registerEvents(
                new PlayerLifecycleListener(soulService, playtimeTracker), this);
        pluginManager.registerEvents(new PlayerActivityListener(playtimeTracker), this);
        pluginManager.registerEvents(
                new KillRewardListener(soulService, messages, this::settings), this);
    }

    private void warnAboutReservedAfkZone() {
        if (settings.afkZoneEnabled()) {
            getLogger().warning(
                    "afk-zone.enabled is true, but AFK zone rewards are reserved for a later release."
                            + " No AFK rewards will be granted.");
        }
    }

    private void requireEnabled() {
        if (soulService == null) {
            throw new IllegalStateException("LifestealSouls is not enabled");
        }
    }
}
