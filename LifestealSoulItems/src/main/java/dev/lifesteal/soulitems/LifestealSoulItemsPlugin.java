package dev.lifesteal.soulitems;

import dev.lifesteal.soulitems.api.LifestealSoulItemsApi;
import dev.lifesteal.soulitems.item.SoulItemFactory;
import dev.lifesteal.soulitems.mining.SoulPickaxeMiningListener;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class LifestealSoulItemsPlugin extends JavaPlugin implements LifestealSoulItemsApi {

    private SoulItemFactory itemFactory;

    @Override
    public void onEnable() {
        itemFactory = new SoulItemFactory(new NamespacedKey(this, "soul_pickaxe"));
        getServer().getPluginManager().registerEvents(
                new SoulPickaxeMiningListener(this, itemFactory), this);
        getServer().getServicesManager().register(
                LifestealSoulItemsApi.class, this, this, ServicePriority.Normal);
        getLogger().info("LifestealSoulItems v" + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        itemFactory = null;
    }

    @Override
    public ItemStack createSoulPickaxe() {
        return requireFactory().createSoulPickaxe();
    }

    @Override
    public boolean isSoulPickaxe(ItemStack item) {
        return requireFactory().isSoulPickaxe(item);
    }

    private SoulItemFactory requireFactory() {
        if (itemFactory == null) {
            throw new IllegalStateException("LifestealSoulItems is not enabled");
        }
        return itemFactory;
    }
}
