package dev.lifesteal.homes;

import dev.lifesteal.homes.command.HomeCommands;
import dev.lifesteal.homes.command.HomesAdminCommand;
import dev.lifesteal.homes.config.HomesSettings;
import dev.lifesteal.homes.data.HomeRepository;
import dev.lifesteal.homes.data.SQLiteHomeRepository;
import dev.lifesteal.homes.menu.HomesMenu;
import dev.lifesteal.homes.message.MessageService;
import dev.lifesteal.homes.teleport.HomeTeleportService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

public final class LifestealHomesPlugin extends JavaPlugin {

    private HomesSettings settings;
    private HomeRepository repository;
    private HomeTeleportService teleports;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = HomesSettings.load(getConfig());
        repository = new SQLiteHomeRepository(new File(getDataFolder(), "homes.db").toPath());

        MessageService messages = new MessageService(getLogger());
        teleports = new HomeTeleportService(this, messages, this::settings);
        HomesMenu menu = new HomesMenu(repository, teleports, messages, this::settings);
        HomeCommands commands = new HomeCommands(repository, menu, teleports, messages, this::settings);

        getServer().getPluginManager().registerEvents(teleports, this);
        getServer().getPluginManager().registerEvents(menu, this);
        for (String name : List.of("home", "homes", "sethome", "delhome")) {
            PluginCommand command = requireCommand(name);
            command.setExecutor(commands);
            command.setTabCompleter(commands);
        }
        requireCommand("lifestealhomes").setExecutor(new HomesAdminCommand(this, messages));
        getLogger().info("LifestealHomes v" + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (teleports != null) {
            teleports.shutdown();
            teleports = null;
        }
        if (repository != null) {
            repository.close();
            repository = null;
        }
        settings = null;
    }

    public HomesSettings settings() {
        if (settings == null) {
            throw new IllegalStateException("LifestealHomes settings are not loaded");
        }
        return settings;
    }

    public void reloadHomesSettings() {
        reloadConfig();
        HomesSettings loaded = HomesSettings.load(getConfig());
        settings = loaded;
    }

    private PluginCommand requireCommand(String name) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Command /" + name + " is missing from plugin.yml");
        }
        return command;
    }
}
