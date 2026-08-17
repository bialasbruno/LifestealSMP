package dev.lifesteal.soulitems.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class SoulItemFactory {

    private static final NamespacedKey SOUL_PICKAXE_MODEL =
            new NamespacedKey("serverpack", "soul_pickaxe");

    private final NamespacedKey soulPickaxeMarker;

    public SoulItemFactory(NamespacedKey soulPickaxeMarker) {
        this.soulPickaxeMarker = soulPickaxeMarker;
    }

    public ItemStack createSoulPickaxe() {
        ItemStack item = new ItemStack(Material.NETHERITE_PICKAXE);
        item.editMeta(meta -> {
            meta.displayName(plain("Soul Pickaxe", NamedTextColor.AQUA));
            meta.lore(List.of(
                    plain("A netherite tool inhabited by restless souls.", NamedTextColor.GRAY),
                    plain("Their whispers guide every strike.", NamedTextColor.DARK_PURPLE)));
            meta.addEnchant(
                    Enchantment.EFFICIENCY, SoulPickaxeDefinition.EFFICIENCY_LEVEL, true);
            meta.addEnchant(Enchantment.FORTUNE, SoulPickaxeDefinition.FORTUNE_LEVEL, true);
            meta.addEnchant(
                    Enchantment.UNBREAKING, SoulPickaxeDefinition.UNBREAKING_LEVEL, true);
            meta.addEnchant(Enchantment.MENDING, SoulPickaxeDefinition.MENDING_LEVEL, true);
            meta.getPersistentDataContainer().set(
                    soulPickaxeMarker, PersistentDataType.BYTE, (byte) 1);
            meta.setItemModel(SOUL_PICKAXE_MODEL);
        });
        return item;
    }

    public boolean isSoulPickaxe(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_PICKAXE) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        Byte marker = meta.getPersistentDataContainer().get(
                soulPickaxeMarker, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    private static Component plain(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}
