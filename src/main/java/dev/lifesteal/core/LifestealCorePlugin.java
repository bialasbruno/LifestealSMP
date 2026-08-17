package dev.lifesteal.core;

import dev.lifesteal.core.api.LifestealCoreApi;
import dev.lifesteal.core.command.HeartsCommand;
import dev.lifesteal.core.command.LifestealAdminCommand;
import dev.lifesteal.core.command.ReviveCommand;
import dev.lifesteal.core.config.LifestealConfig;
import dev.lifesteal.core.data.PlayerHeartRepository;
import dev.lifesteal.core.data.SQLitePlayerHeartRepository;
import dev.lifesteal.core.elimination.EliminationService;
import dev.lifesteal.core.heart.HeartItemFactory;
import dev.lifesteal.core.heart.HeartKeys;
import dev.lifesteal.core.heart.HeartRecipeFactory;
import dev.lifesteal.core.heart.HeartRules;
import dev.lifesteal.core.heart.HeartService;
import dev.lifesteal.core.listener.CraftingListener;
import dev.lifesteal.core.listener.HeartUseListener;
import dev.lifesteal.core.listener.PlayerDeathListener;
import dev.lifesteal.core.listener.PlayerJoinListener;
import dev.lifesteal.core.listener.ReviveTotemListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;

public final class LifestealCorePlugin extends JavaPlugin implements LifestealCoreApi {

    private PlayerHeartRepository repository;
    private HeartService heartService;
    private EliminationService eliminationService;
    private HeartRecipeFactory recipeFactory;

    @Override
    public int getHearts(UUID playerId) {
        if (heartService == null) {
            throw new IllegalStateException("LifestealCore is not enabled");
        }
        return heartService.getHearts(playerId);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        LifestealConfig config = new LifestealConfig(getConfig());

        File databaseFile = new File(getDataFolder(), "data.db");
        repository = new SQLitePlayerHeartRepository(databaseFile, getLogger());

        heartService = new HeartService(this, repository, config);
        eliminationService = new EliminationService(this, repository, heartService, config);

        HeartKeys keys = new HeartKeys(this);
        int reviveReturnHearts = HeartRules.clamp(
                config.reviveReturnHearts(), config.minimumHearts(), config.maximumHearts());
        HeartItemFactory itemFactory = new HeartItemFactory(keys, reviveReturnHearts);

        recipeFactory = new HeartRecipeFactory(this, itemFactory);
        recipeFactory.register();

        registerListeners(config, itemFactory);
        registerCommands(config, itemFactory);
        eliminationService.start();

        // Covers a /reload: listeners just got (re)registered, but any already-online players
        // won't fire a new PlayerJoinEvent, so load them explicitly.
        for (Player player : getServer().getOnlinePlayers()) {
            if (eliminationService.preparePlayerJoin(player)) {
                heartService.loadPlayer(player.getUniqueId(), player.getName());
            }
        }

        getLogger().info("LifestealCore v" + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (recipeFactory != null) {
            recipeFactory.unregister();
        }
        if (eliminationService != null) {
            eliminationService.stop();
        }
        if (heartService != null) {
            heartService.shutdownAndSaveAll();
        }
        if (repository != null) {
            repository.close();
        }
        getLogger().info("LifestealCore disabled.");
    }

    private void registerListeners(LifestealConfig config, HeartItemFactory itemFactory) {
        var pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(
                new PlayerJoinListener(this, heartService, eliminationService), this);
        pluginManager.registerEvents(
                new PlayerDeathListener(
                        heartService,
                        itemFactory,
                        eliminationService,
                        config.dropBrokenHeartOnPvpDeath()),
                this);
        pluginManager.registerEvents(
                new HeartUseListener(this, heartService, itemFactory, config.maximumHeartsMessage()), this);
        pluginManager.registerEvents(
                new CraftingListener(itemFactory, recipeFactory.key()), this);
        pluginManager.registerEvents(new ReviveTotemListener(itemFactory), this);
    }

    private void registerCommands(LifestealConfig config, HeartItemFactory itemFactory) {
        var heartsCommand = new HeartsCommand(heartService, config.maximumHearts());
        var adminCommand = new LifestealAdminCommand(
                heartService, itemFactory, config.minimumHearts(), config.maximumHearts());
        var reviveCommand = new ReviveCommand(this, eliminationService, itemFactory);

        PluginCommand heartsCmd = getCommand("hearts");
        if (heartsCmd != null) {
            heartsCmd.setExecutor(heartsCommand);
        } else {
            getLogger().warning("Could not register /hearts - check plugin.yml.");
        }

        PluginCommand lifestealCmd = getCommand("lifesteal");
        if (lifestealCmd != null) {
            lifestealCmd.setExecutor(adminCommand);
            lifestealCmd.setTabCompleter(adminCommand);
        } else {
            getLogger().warning("Could not register /lifesteal - check plugin.yml.");
        }

        PluginCommand reviveCmd = getCommand("revive");
        if (reviveCmd != null) {
            reviveCmd.setExecutor(reviveCommand);
            reviveCmd.setTabCompleter(reviveCommand);
        } else {
            getLogger().warning("Could not register /revive - check plugin.yml.");
        }
    }
}
