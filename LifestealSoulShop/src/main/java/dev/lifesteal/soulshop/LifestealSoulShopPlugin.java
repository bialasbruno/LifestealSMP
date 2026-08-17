package dev.lifesteal.soulshop;

import dev.lifesteal.soulitems.api.LifestealSoulItemsApi;
import dev.lifesteal.soulshop.command.SoulShopCommand;
import dev.lifesteal.soulshop.config.SoulShopSettings;
import dev.lifesteal.soulshop.menu.SoulShopMenu;
import dev.lifesteal.soulshop.message.MessageService;
import dev.lifesteal.souls.api.LifestealSoulsApi;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class LifestealSoulShopPlugin extends JavaPlugin {

    private SoulShopSettings settings;
    private SoulShopMenu menu;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        migrateConfig();
        settings = SoulShopSettings.load(getConfig(), getLogger());

        RegisteredServiceProvider<LifestealSoulsApi> soulsRegistration =
                getServer().getServicesManager().getRegistration(LifestealSoulsApi.class);
        if (soulsRegistration == null) {
            getLogger().severe("LifestealSouls API is unavailable; disabling LifestealSoulShop.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        RegisteredServiceProvider<LifestealSoulItemsApi> itemsRegistration =
                getServer().getServicesManager().getRegistration(LifestealSoulItemsApi.class);
        if (itemsRegistration == null) {
            getLogger().severe(
                    "LifestealSoulItems API is unavailable; disabling LifestealSoulShop.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        MessageService messages = new MessageService(this);
        menu = new SoulShopMenu(
                this,
                soulsRegistration.getProvider(),
                itemsRegistration.getProvider(),
                messages,
                this::settings);
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

    private void migrateConfig() {
        int version = getConfig().contains("config-version", true)
                ? getConfig().getInt("config-version", 1)
                : 1;
        if (version >= 3) {
            return;
        }

        if (version < 2) {
            migrateLaunchProduct();
        }
        if (version < 3 && getConfig().getStringList("menu.product-lore").equals(List.of(
                "<gray>A netherite tool inhabited by restless souls.</gray>",
                "<dark_purple>Their whispers guide every strike.</dark_purple>",
                "",
                "<gray>Price:</gray> <light_purple>{price} Souls</light_purple>",
                "",
                "<green>Click to buy</green>"))) {
            getConfig().set("menu.product-lore", null);
        }
        getConfig().set("config-version", 3);
        saveConfig();
        getLogger().info("Migrated LifestealSoulShop configuration to version 3.");
    }

    private void migrateLaunchProduct() {
        getConfig().set("product.material", null);
        getConfig().set("product.amount", null);
        if (getConfig().getLong("product.price", 100L) == 100L) {
            getConfig().set("product.price", 2_500L);
        }
        if ("<aqua><bold>Diamond Pickaxe</bold></aqua>"
                .equals(getConfig().getString("menu.product-name"))) {
            getConfig().set("menu.product-name", null);
        }
        if (getConfig().getStringList("menu.product-lore").equals(List.of(
                "",
                "<gray>Price:</gray> <light_purple>{price} Souls</light_purple>",
                "",
                "<green>Click to buy</green>"))) {
            getConfig().set("menu.product-lore", null);
        }
        if ("<green>Purchased Diamond Pickaxe for {price} Souls.</green>"
                .concat(" <gray>Balance: {balance}</gray>")
                .equals(getConfig().getString("messages.success"))) {
            getConfig().set("messages.success", null);
        }
    }
}
