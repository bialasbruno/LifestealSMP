package dev.lifesteal.core.listener;

import dev.lifesteal.core.heart.HeartItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/** Prevents the custom Revive Totem from acting as a vanilla Totem of Undying. */
public final class ReviveTotemListener implements Listener {

    private final HeartItemFactory itemFactory;

    public ReviveTotemListener(HeartItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        EquipmentSlot hand = event.getHand();
        ItemStack item = hand == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();
        if (!itemFactory.isReviveTotem(item)) {
            return;
        }

        event.setCancelled(true);
        player.sendMessage(Component.text(
                "A Revive Totem can only be used with /revive <player>.",
                NamedTextColor.RED));
    }
}
