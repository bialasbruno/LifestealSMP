package dev.lifesteal.core.listener;

import dev.lifesteal.core.heart.HeartItemFactory;
import dev.lifesteal.core.heart.HeartService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handles right-clicking with a Heart item to permanently gain +1 maximum heart.
 *
 * <p>Paper may fire {@link PlayerInteractEvent} once for each hand. A one-tick player guard
 * allows either hand to consume a Heart while ensuring one physical click can never consume
 * two items when both hands contain Hearts.</p>
 */
public final class HeartUseListener implements Listener {

    private final Plugin plugin;
    private final HeartService heartService;
    private final HeartItemFactory itemFactory;
    private final String maximumHeartsMessage;
    private final Set<UUID> handledThisTick = new HashSet<>();

    public HeartUseListener(Plugin plugin, HeartService heartService, HeartItemFactory itemFactory,
                            String maximumHeartsMessage) {
        this.plugin = plugin;
        this.heartService = heartService;
        this.itemFactory = itemFactory;
        this.maximumHeartsMessage = maximumHeartsMessage;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        EquipmentSlot hand = event.getHand();
        if (hand != EquipmentSlot.HAND && hand != EquipmentSlot.OFF_HAND) {
            return;
        }

        Player player = event.getPlayer();
        PlayerInventory inventory = player.getInventory();
        ItemStack heldItem = hand == EquipmentSlot.HAND
                ? inventory.getItemInMainHand()
                : inventory.getItemInOffHand();

        if (!itemFactory.isHeart(heldItem)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        if (!handledThisTick.add(uuid)) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> handledThisTick.remove(uuid));

        event.setCancelled(true);

        HeartService.HeartConsumptionResult result = heartService.consumeHeart(player);
        if (!result.consumed()) {
            player.sendMessage(Component.text(maximumHeartsMessage, NamedTextColor.RED));
            return;
        }

        consumeOne(inventory, hand, heldItem);

        player.sendMessage(Component.text(
                "Your life force grows: " + result.heartsBefore() + " \u2665 -> " + result.heartsAfter() + " \u2665",
                NamedTextColor.LIGHT_PURPLE));
        player.playSound(
                player.getLocation(),
                "serverpack:heart_consume",
                SoundCategory.PLAYERS,
                0.9f,
                1.0f
        );
    }

    private void consumeOne(PlayerInventory inventory, EquipmentSlot hand, ItemStack item) {
        int newAmount = item.getAmount() - 1;
        if (newAmount <= 0) {
            setHandItem(inventory, hand, null);
        } else {
            item.setAmount(newAmount);
            setHandItem(inventory, hand, item);
        }
    }

    private void setHandItem(PlayerInventory inventory, EquipmentSlot hand, ItemStack item) {
        if (hand == EquipmentSlot.HAND) {
            inventory.setItemInMainHand(item);
        } else {
            inventory.setItemInOffHand(item);
        }
    }
}
