package dev.lifesteal.sell;

import dev.lifesteal.sell.command.SellCommand;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class LifestealSellPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        PluginCommand command = getCommand("sell");
        if (command == null) {
            getLogger().severe("Could not register /sell; check plugin.yml.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        SellCommand executor = new SellCommand();
        command.setExecutor(executor);
        command.setTabCompleter(executor);
        getLogger().info("LifestealSell v" + getPluginMeta().getVersion() + " enabled.");
    }
}
