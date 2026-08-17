package dev.lifesteal.soulitems;

import dev.lifesteal.soulitems.api.LifestealSoulItemsApi;
import dev.lifesteal.soulitems.command.SoulItemsCommand;
import dev.lifesteal.soulitems.config.SoulItemsSettings;
import dev.lifesteal.soulitems.item.SoulItemFactory;
import dev.lifesteal.soulitems.mining.SoulPickaxeMiningListener;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class LifestealSoulItemsPlugin extends JavaPlugin implements LifestealSoulItemsApi {

    private SoulItemFactory itemFactory;
    private SoulItemsSettings settings;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = SoulItemsSettings.load(getConfig(), getLogger());
        itemFactory = new SoulItemFactory(
                new NamespacedKey(this, "soul_pickaxe"), this::settings, getLogger());
        getServer().getPluginManager().registerEvents(
                new SoulPickaxeMiningListener(this, itemFactory), this);
        getServer().getServicesManager().register(
                LifestealSoulItemsApi.class, this, this, ServicePriority.Normal);

        PluginCommand command = getCommand("soulitems");
        if (command == null) {
            getLogger().severe("Could not register /soulitems; check plugin.yml.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        SoulItemsCommand executor = new SoulItemsCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
        getLogger().info("LifestealSoulItems v" + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        itemFactory = null;
        settings = null;
    }

    @Override
    public ItemStack createSoulPickaxe() {
        return requireFactory().createSoulPickaxe();
    }

    @Override
    public boolean isSoulPickaxe(ItemStack item) {
        return requireFactory().isSoulPickaxe(item);
    }

    public SoulItemsSettings settings() {
        if (settings == null) {
            throw new IllegalStateException("LifestealSoulItems settings are not loaded");
        }
        return settings;
    }

    public void reloadSoulItemsSettings() {
        reloadConfig();
        settings = SoulItemsSettings.load(getConfig(), getLogger());
    }

    private SoulItemFactory requireFactory() {
        if (itemFactory == null) {
            throw new IllegalStateException("LifestealSoulItems is not enabled");
        }
        return itemFactory;
    }
}
