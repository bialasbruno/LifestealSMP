package dev.lifesteal.core.listener;

import dev.lifesteal.core.heart.HeartItemFactory;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

/**
 * Defense-in-depth guard for the Heart recipe.
 *
 * <p>{@link dev.lifesteal.core.heart.HeartRecipeFactory} already restricts the Broken Heart
 * slots with a {@code RecipeChoice.ExactChoice}, which should already reject any item that
 * doesn't match the real item's metadata (including its PDC marker). This listener adds an
 * explicit, independent check on top of that: for this specific recipe, every ingredient that
 * merely *looks* like a Broken Heart (same base material) must carry the real plugin marker,
 * or the craft is invalidated outright. A security-sensitive recipe should never rely on a
 * single layer of matching.</p>
 */
public final class CraftingListener implements Listener {

    private final HeartItemFactory itemFactory;
    private final NamespacedKey heartRecipeKey;

    public CraftingListener(HeartItemFactory itemFactory, NamespacedKey heartRecipeKey) {
        this.itemFactory = itemFactory;
        this.heartRecipeKey = heartRecipeKey;
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (!(recipe instanceof Keyed keyedRecipe) || !keyedRecipe.getKey().equals(heartRecipeKey)) {
            return;
        }

        CraftingInventory inventory = event.getInventory();
        for (ItemStack ingredient : inventory.getMatrix()) {
            if (ingredient == null) {
                continue;
            }
            boolean looksLikeBrokenHeart = ingredient.getType() == itemFactory.brokenHeartBaseMaterial();
            if (looksLikeBrokenHeart && !itemFactory.isBrokenHeart(ingredient)) {
                // A fake/renamed item is sitting in the grid alongside a real one - invalidate
                // the whole craft rather than trust the matrix matcher alone.
                inventory.setResult(null);
                return;
            }
        }
    }
}
