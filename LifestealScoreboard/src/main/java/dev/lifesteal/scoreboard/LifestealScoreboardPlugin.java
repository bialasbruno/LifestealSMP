package dev.lifesteal.scoreboard;

import dev.lifesteal.core.api.LifestealCoreApi;
import dev.lifesteal.scoreboard.command.ScoreboardAdminCommand;
import dev.lifesteal.scoreboard.command.ScoreboardToggleCommand;
import dev.lifesteal.scoreboard.config.ScoreboardSettings;
import dev.lifesteal.scoreboard.integration.PlaceholderApiBridge;
import dev.lifesteal.scoreboard.integration.PlaceholderApiIntegration;
import dev.lifesteal.scoreboard.placeholder.PlaceholderResolver;
import dev.lifesteal.scoreboard.provider.CoreHeartProvider;
import dev.lifesteal.scoreboard.provider.CurrencyProviderRegistry;
import dev.lifesteal.scoreboard.render.SidebarManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.ScoreboardManager;

/** Lightweight presentation-only sidebar for LifestealSMP. */
public final class LifestealScoreboardPlugin extends JavaPlugin {

    private SidebarManager sidebarManager;
    private PlaceholderApiBridge placeholderApiBridge;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        LifestealCoreApi coreApi = findCoreApi();
        if (coreApi == null) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        ScoreboardManager bukkitScoreboards = getServer().getScoreboardManager();
        if (bukkitScoreboards == null) {
            getLogger().severe("The Bukkit scoreboard manager is unavailable; disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        CurrencyProviderRegistry currencyProviders =
                new CurrencyProviderRegistry(getServer().getServicesManager());
        PlaceholderResolver placeholders = new PlaceholderResolver(
                new CoreHeartProvider(coreApi), currencyProviders);
        sidebarManager = new SidebarManager(this, placeholders, bukkitScoreboards);

        getServer().getPluginManager().registerEvents(currencyProviders, this);
        getServer().getPluginManager().registerEvents(sidebarManager, this);
        registerCommands();

        ScoreboardSettings settings = ScoreboardSettings.load(getConfig(), getLogger());
        sidebarManager.start(settings);
        registerPlaceholderApi(placeholders);

        getLogger().info("LifestealScoreboard v" + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (placeholderApiBridge != null) {
            placeholderApiBridge.unregister();
            placeholderApiBridge = null;
        }
        if (sidebarManager != null) {
            sidebarManager.shutdown();
            sidebarManager = null;
        }
        getLogger().info("LifestealScoreboard disabled.");
    }

    public void reloadScoreboards() {
        reloadConfig();
        ScoreboardSettings settings = ScoreboardSettings.load(getConfig(), getLogger());
        sidebarManager.reload(settings);
    }

    private LifestealCoreApi findCoreApi() {
        Plugin corePlugin = getServer().getPluginManager().getPlugin("LifestealCore");
        if (corePlugin instanceof LifestealCoreApi coreApi && corePlugin.isEnabled()) {
            return coreApi;
        }
        getLogger().severe(
                "A compatible enabled LifestealCore version is required; disabling plugin.");
        return null;
    }

    private void registerCommands() {
        ScoreboardAdminCommand adminCommand = new ScoreboardAdminCommand(this);
        PluginCommand admin = getCommand("lifestealscoreboard");
        if (admin != null) {
            admin.setExecutor(adminCommand);
            admin.setTabCompleter(adminCommand);
        } else {
            getLogger().severe("Could not register /lifestealscoreboard; check plugin.yml.");
        }

        PluginCommand toggle = getCommand("scoreboard");
        if (toggle != null) {
            toggle.setExecutor(new ScoreboardToggleCommand(sidebarManager));
        } else {
            getLogger().severe("Could not register /scoreboard; check plugin.yml.");
        }
    }

    private void registerPlaceholderApi(PlaceholderResolver placeholders) {
        if (!getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            getLogger().info("PlaceholderAPI is not installed; internal placeholders remain available.");
            return;
        }
        try {
            placeholderApiBridge = PlaceholderApiIntegration.register(this, placeholders);
            getLogger().info("Registered the optional PlaceholderAPI expansion 'lifesteal'.");
        } catch (RuntimeException | LinkageError exception) {
            getLogger().log(
                    java.util.logging.Level.SEVERE,
                    "PlaceholderAPI integration failed; the sidebar will continue without it.",
                    exception);
        }
    }
}
