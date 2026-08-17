package dev.lifesteal.soulitems.api;

import org.bukkit.inventory.ItemStack;

/** Public item factory used by LifestealSMP plugins. */
public interface LifestealSoulItemsApi {

    /** Creates a new, genuine Soul Pickaxe. */
    ItemStack createSoulPickaxe();

    /** Checks the protected PDC marker instead of trusting a display name or lore. */
    boolean isSoulPickaxe(ItemStack item);
}
