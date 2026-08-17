package dev.lifesteal.soulitems.mining;

import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.List;

public enum MiningPlane {
    HORIZONTAL,
    NORTH_SOUTH,
    EAST_WEST;

    public static MiningPlane fromHitFace(BlockFace face) {
        return switch (face) {
            case UP, DOWN -> HORIZONTAL;
            case NORTH, SOUTH -> NORTH_SOUTH;
            case EAST, WEST -> EAST_WEST;
            default -> throw new IllegalArgumentException("Unsupported hit face: " + face);
        };
    }

    public List<BlockOffset> surroundingOffsets() {
        List<BlockOffset> offsets = new ArrayList<>(8);
        for (int first = -1; first <= 1; first++) {
            for (int second = -1; second <= 1; second++) {
                if (first == 0 && second == 0) {
                    continue;
                }
                offsets.add(switch (this) {
                    case HORIZONTAL -> new BlockOffset(first, 0, second);
                    case NORTH_SOUTH -> new BlockOffset(first, second, 0);
                    case EAST_WEST -> new BlockOffset(0, second, first);
                });
            }
        }
        return List.copyOf(offsets);
    }

    public record BlockOffset(int x, int y, int z) {}
}
