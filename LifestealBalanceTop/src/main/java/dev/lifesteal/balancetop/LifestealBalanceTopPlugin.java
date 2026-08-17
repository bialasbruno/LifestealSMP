package dev.lifesteal.balancetop;

import dev.lifesteal.balancetop.command.BalanceTopCommand;
import dev.lifesteal.balancetop.command.BalanceTopCommandInterceptor;
import dev.lifesteal.balancetop.config.BalanceTopSettings;
import dev.lifesteal.balancetop.economy.VaultEconomyGateway;
import dev.lifesteal.balancetop.menu.BalanceTopMenu;
import dev.lifesteal.balancetop.message.MessageService;
import dev.lifesteal.balancetop.service.BalanceLeaderboardService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class LifestealBalanceTopPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        BalanceTopSettings settings = BalanceTopSettings.load(getConfig());
        MessageService messages = new MessageService();
        VaultEconomyGateway economy = new VaultEconomyGateway(getServer().getServicesManager());
        BalanceLeaderboardService leaderboard = new BalanceLeaderboardService(
                this, economy, settings.cacheMillis(), settings.includeZeroBalances());
        BalanceTopMenu menu = new BalanceTopMenu(leaderboard, messages, settings);
        BalanceTopCommand command = new BalanceTopCommand(menu, messages, settings);

        PluginCommand pluginCommand = getCommand("baltop");
        if (pluginCommand == null) {
            getLogger().severe("Could not register /baltop; check plugin.yml.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        pluginCommand.setExecutor(command);

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(economy, this);
        pluginManager.registerEvents(menu, this);
        pluginManager.registerEvents(new BalanceTopCommandInterceptor(command), this);

        if (!economy.available()) {
            getLogger().warning(
                    "VaultUnlocked is loaded, but no economy provider is registered yet.");
        }
        getLogger().info("LifestealBalanceTop v" + getPluginMeta().getVersion() + " enabled.");
    }
}
