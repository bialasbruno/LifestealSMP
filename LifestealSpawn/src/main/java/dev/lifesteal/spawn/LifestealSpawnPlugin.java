package dev.lifesteal.spawn;

import dev.lifesteal.spawn.command.LifestealSpawnCommand;
import dev.lifesteal.spawn.config.SpawnSettings;
import dev.lifesteal.spawn.message.MessageService;
import dev.lifesteal.spawn.rescue.VoidRescueListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class LifestealSpawnPlugin extends JavaPlugin {

    private SpawnSettings settings;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = SpawnSettings.load(getConfig(), getLogger());

        MessageService messages = new MessageService(this);
        getServer().getPluginManager().registerEvents(
                new VoidRescueListener(this, messages, this::settings), this);

        PluginCommand command = getCommand("lifestealspawn");
        if (command == null) {
            getLogger().severe("Could not register /lifestealspawn; check plugin.yml.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        LifestealSpawnCommand executor = new LifestealSpawnCommand(this, messages);
        command.setExecutor(executor);
        command.setTabCompleter(executor);

        getLogger().info("LifestealSpawn v" + getPluginMeta().getVersion() + " enabled.");
    }

    public SpawnSettings settings() {
        if (settings == null) {
            throw new IllegalStateException("LifestealSpawn settings are not loaded");
        }
        return settings;
    }

    public void reloadSpawnSettings() {
        reloadConfig();
        settings = SpawnSettings.load(getConfig(), getLogger());
    }
}
