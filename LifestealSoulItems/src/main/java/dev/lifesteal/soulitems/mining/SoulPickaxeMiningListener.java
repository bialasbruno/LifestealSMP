package dev.lifesteal.soulitems.mining;

import dev.lifesteal.soulitems.item.SoulItemFactory;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class SoulPickaxeMiningListener implements Listener {

    private static final double FACE_TRACE_DISTANCE = 8.0D;

    private final Plugin plugin;
    private final SoulItemFactory itemFactory;
    private final Set<UUID> areaMiningPlayers = new HashSet<>();

    public SoulPickaxeMiningListener(Plugin plugin, SoulItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (areaMiningPlayers.contains(player.getUniqueId())) {
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!itemFactory.isSoulPickaxe(tool)) {
            return;
        }

        Block center = event.getBlock();
        if (!Tag.MINEABLE_PICKAXE.isTagged(center.getType())) {
            return;
        }
        BlockFace hitFace = findHitFace(player, center);
        MiningPlane plane = MiningPlane.fromHitFace(hitFace);
        plugin.getServer().getScheduler().runTask(
                plugin, () -> breakSurroundingBlocks(player, center, plane));
    }

    private void breakSurroundingBlocks(Player player, Block center, MiningPlane plane) {
        if (!player.isOnline()) {
            return;
        }

        UUID playerId = player.getUniqueId();
        areaMiningPlayers.add(playerId);
        try {
            for (MiningPlane.BlockOffset offset : plane.surroundingOffsets()) {
                ItemStack tool = player.getInventory().getItemInMainHand();
                if (!itemFactory.isSoulPickaxe(tool)) {
                    break;
                }

                Block target = center.getRelative(offset.x(), offset.y(), offset.z());
                if (target.getType().isAir()
                        || !Tag.MINEABLE_PICKAXE.isTagged(target.getType())) {
                    continue;
                }
                player.breakBlock(target);
            }
        } finally {
            areaMiningPlayers.remove(playerId);
        }
    }

    private BlockFace findHitFace(Player player, Block center) {
        RayTraceResult trace = player.rayTraceBlocks(
                FACE_TRACE_DISTANCE, FluidCollisionMode.NEVER);
        if (trace != null
                && center.equals(trace.getHitBlock())
                && trace.getHitBlockFace() != null) {
            return trace.getHitBlockFace();
        }

        double x = player.getEyeLocation().getX() - (center.getX() + 0.5D);
        double y = player.getEyeLocation().getY() - (center.getY() + 0.5D);
        double z = player.getEyeLocation().getZ() - (center.getZ() + 0.5D);
        double absoluteX = Math.abs(x);
        double absoluteY = Math.abs(y);
        double absoluteZ = Math.abs(z);
        if (absoluteY >= absoluteX && absoluteY >= absoluteZ) {
            return y >= 0.0D ? BlockFace.UP : BlockFace.DOWN;
        }
        if (absoluteX >= absoluteZ) {
            return x >= 0.0D ? BlockFace.EAST : BlockFace.WEST;
        }
        return z >= 0.0D ? BlockFace.SOUTH : BlockFace.NORTH;
    }
}
