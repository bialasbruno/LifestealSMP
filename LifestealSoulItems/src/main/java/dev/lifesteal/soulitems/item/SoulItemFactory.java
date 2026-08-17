package dev.lifesteal.soulitems.item;

import dev.lifesteal.soulitems.config.SoulItemsSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.function.Supplier;
import java.util.logging.Logger;

public final class SoulItemFactory {

    private static final NamespacedKey SOUL_PICKAXE_MODEL =
            new NamespacedKey("serverpack", "soul_pickaxe");

    private final NamespacedKey soulPickaxeMarker;
    private final Supplier<SoulItemsSettings> settingsSupplier;
    private final Logger logger;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public SoulItemFactory(
            NamespacedKey soulPickaxeMarker,
            Supplier<SoulItemsSettings> settingsSupplier,
            Logger logger) {
        this.soulPickaxeMarker = soulPickaxeMarker;
        this.settingsSupplier = settingsSupplier;
        this.logger = logger;
    }

    public ItemStack createSoulPickaxe() {
        SoulItemsSettings settings = settingsSupplier.get();
        ItemStack item = new ItemStack(Material.NETHERITE_PICKAXE);
        item.editMeta(meta -> {
            meta.displayName(render(settings.soulPickaxeName()));
            meta.lore(settings.soulPickaxeLore().stream().map(this::render).toList());
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

    private Component render(String text) {
        try {
            return miniMessage.deserialize(text).decoration(TextDecoration.ITALIC, false);
        } catch (RuntimeException exception) {
            logger.warning("Invalid MiniMessage item text: " + exception.getMessage());
            return Component.text(text).decoration(TextDecoration.ITALIC, false);
        }
    }
}
