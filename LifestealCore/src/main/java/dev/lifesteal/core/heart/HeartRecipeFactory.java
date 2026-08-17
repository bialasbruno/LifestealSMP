package dev.lifesteal.core.heart;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

/**
 * Builds and (un)registers the shaped recipe that turns 2 Broken Hearts + 7 Diamonds into
 * 1 Heart:
 *
 * <pre>
 * B D B
 * D D D
 * D D D
 * </pre>
 *
 * <p>The Broken Heart ingredient uses a {@link RecipeChoice.ExactChoice} built from a real
 * plugin-created item, so an item that only shares the base material (e.g. a plain
 * {@link HeartConstants#BROKEN_HEART_MATERIAL}) or an anvil-renamed lookalike will not match
 * on its own. {@link dev.lifesteal.core.listener.CraftingListener} adds a second, explicit
 * PDC check on top of this as defense in depth.</p>
 */
public final class HeartRecipeFactory {

    private final Plugin plugin;
    private final HeartItemFactory itemFactory;

    public HeartRecipeFactory(Plugin plugin, HeartItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    public NamespacedKey key() {
        return new NamespacedKey(plugin, "heart_from_broken_hearts");
    }

    public void register() {
        NamespacedKey key = key();
        // Defensive: avoids "duplicate recipe key" errors if the plugin is reloaded in-place.
        Bukkit.removeRecipe(key);

        ItemStack result = itemFactory.createHeart(1);
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape("BDB", "DDD", "DDD");

        RecipeChoice brokenHeartChoice = new RecipeChoice.ExactChoice(itemFactory.createBrokenHeart(1));
        RecipeChoice diamondChoice = new RecipeChoice.MaterialChoice(Material.DIAMOND);

        recipe.setIngredient('B', brokenHeartChoice);
        recipe.setIngredient('D', diamondChoice);

        Bukkit.addRecipe(recipe);
    }

    public void unregister() {
        Bukkit.removeRecipe(key());
    }
}
