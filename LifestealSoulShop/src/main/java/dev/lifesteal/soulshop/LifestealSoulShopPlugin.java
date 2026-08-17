package dev.lifesteal.soulshop;

import dev.lifesteal.soulshop.command.SoulShopCommand;
import dev.lifesteal.soulshop.config.SoulShopSettings;
import dev.lifesteal.soulshop.menu.SoulShopMenu;
import dev.lifesteal.soulshop.message.MessageService;
import dev.lifesteal.souls.api.LifestealSoulsApi;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class LifestealSoulShopPlugin extends JavaPlugin {

    private SoulShopSettings settings;
    private SoulShopMenu menu;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = SoulShopSettings.load(getConfig(), getLogger());

        RegisteredServiceProvider<LifestealSoulsApi> registration =
                getServer().getServicesManager().getRegistration(LifestealSoulsApi.class);
        if (registration == null) {
            getLogger().severe("LifestealSouls API is unavailable; disabling LifestealSoulShop.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        MessageService messages = new MessageService(this);
        menu = new SoulShopMenu(this, registration.getProvider(), messages, this::settings);
        getServer().getPluginManager().registerEvents(menu, this);

        PluginCommand soulShop = getCommand("soulshop");
        if (soulShop == null) {
            getLogger().severe("Could not register /soulshop; check plugin.yml.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        SoulShopCommand executor = new SoulShopCommand(this, menu, messages);
        soulShop.setExecutor(executor);
        soulShop.setTabCompleter(executor);

        getLogger().info("LifestealSoulShop v" + getPluginMeta().getVersion() + " enabled.");
    }

    public SoulShopSettings settings() {
        if (settings == null) {
            throw new IllegalStateException("LifestealSoulShop settings are not loaded");
        }
        return settings;
    }

    public void reloadShopSettings() {
        reloadConfig();
        settings = SoulShopSettings.load(getConfig(), getLogger());
        if (menu != null) {
            menu.refreshOpenMenus();
        }
    }
}
