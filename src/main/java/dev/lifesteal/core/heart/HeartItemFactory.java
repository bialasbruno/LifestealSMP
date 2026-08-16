package dev.lifesteal.core.heart;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Creates and recognizes the plugin's custom items.
 *
 * <p>Recognition is always done through a {@link PersistentDataType#BYTE} marker in the
 * item's {@link org.bukkit.persistence.PersistentDataContainer}, never through display name
 * or lore - those can trivially be faked with an anvil, a PDC marker cannot.</p>
 */
public final class HeartItemFactory {

    /** Resource-pack item models from the serverpack namespace. */
    private static final NamespacedKey BROKEN_HEART_MODEL = new NamespacedKey("serverpack", "broken_heart");
    private static final NamespacedKey HEART_MODEL = new NamespacedKey("serverpack", "heart");

    private final HeartKeys keys;

    public HeartItemFactory(HeartKeys keys) {
        this.keys = keys;
    }

    public Material brokenHeartBaseMaterial() {
        return HeartConstants.BROKEN_HEART_MATERIAL;
    }

    public Material heartBaseMaterial() {
        return HeartConstants.HEART_MATERIAL;
    }

    public ItemStack createBrokenHeart(int amount) {
        ItemStack item = new ItemStack(HeartConstants.BROKEN_HEART_MATERIAL, amount);
        item.editMeta(meta -> {
            meta.displayName(plain("Broken Heart", NamedTextColor.RED));
            meta.lore(List.of(
                    plain("A shattered fragment of a fallen warrior's life force.", NamedTextColor.GRAY),
                    plain("Combine seven Diamonds and two of these", NamedTextColor.DARK_GRAY),
                    plain("to restore a full Heart.", NamedTextColor.DARK_GRAY)
            ));
            meta.getPersistentDataContainer().set(keys.brokenHeart, PersistentDataType.BYTE, (byte) 1);
            meta.setItemModel(BROKEN_HEART_MODEL);
            meta.setEnchantmentGlintOverride(true);
        });
        return item;
    }

    public ItemStack createHeart(int amount) {
        ItemStack item = new ItemStack(HeartConstants.HEART_MATERIAL, amount);
        item.editMeta(meta -> {
            meta.displayName(plain("Heart", NamedTextColor.LIGHT_PURPLE));
            meta.lore(List.of(
                    plain("Right-click to permanently gain +1 maximum heart.", NamedTextColor.GRAY)
            ));
            meta.getPersistentDataContainer().set(keys.heart, PersistentDataType.BYTE, (byte) 1);
            meta.setItemModel(HEART_MODEL);
            meta.setEnchantmentGlintOverride(true);
        });
        return item;
    }

    public boolean isBrokenHeart(ItemStack item) {
        return hasMarker(item, HeartConstants.BROKEN_HEART_MATERIAL, keys.brokenHeart);
    }

    public boolean isHeart(ItemStack item) {
        return hasMarker(item, HeartConstants.HEART_MATERIAL, keys.heart);
    }

    private boolean hasMarker(ItemStack item, Material expectedMaterial, NamespacedKey key) {
        if (item == null || item.getType() != expectedMaterial) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        Byte marker = meta.getPersistentDataContainer().get(key, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    /** A non-italic plain-colored line of text; Adventure defaults item lore/names to italic. */
    private static Component plain(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}
