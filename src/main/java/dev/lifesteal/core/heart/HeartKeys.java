package dev.lifesteal.core.heart;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * Holds the {@link NamespacedKey}s used to mark custom items via
 * {@link org.bukkit.persistence.PersistentDataContainer}.
 *
 * <p>Custom items must never be identified by display name or lore alone - the PDC marker
 * is the single source of truth for "is this a real Broken Heart / Heart".</p>
 */
public final class HeartKeys {

    public final NamespacedKey brokenHeart;
    public final NamespacedKey heart;

    public HeartKeys(Plugin plugin) {
        this.brokenHeart = new NamespacedKey(plugin, "broken_heart");
        this.heart = new NamespacedKey(plugin, "heart");
    }
}
