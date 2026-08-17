package dev.lifesteal.soulitems.mining;

import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiningPlaneTest {

    @Test
    void mapsBlockFacesToThePerpendicularMiningPlane() {
        assertEquals(MiningPlane.HORIZONTAL, MiningPlane.fromHitFace(BlockFace.UP));
        assertEquals(MiningPlane.HORIZONTAL, MiningPlane.fromHitFace(BlockFace.DOWN));
        assertEquals(MiningPlane.NORTH_SOUTH, MiningPlane.fromHitFace(BlockFace.NORTH));
        assertEquals(MiningPlane.NORTH_SOUTH, MiningPlane.fromHitFace(BlockFace.SOUTH));
        assertEquals(MiningPlane.EAST_WEST, MiningPlane.fromHitFace(BlockFace.EAST));
        assertEquals(MiningPlane.EAST_WEST, MiningPlane.fromHitFace(BlockFace.WEST));
        assertThrows(
                IllegalArgumentException.class,
                () -> MiningPlane.fromHitFace(BlockFace.SELF));
    }

    @Test
    void everyPlaneContainsEightUniqueNeighboursWithoutTheCenter() {
        for (MiningPlane plane : MiningPlane.values()) {
            List<MiningPlane.BlockOffset> offsets = plane.surroundingOffsets();
            assertEquals(8, offsets.size());
            assertEquals(8, new HashSet<>(offsets).size());
            assertFalse(offsets.contains(new MiningPlane.BlockOffset(0, 0, 0)));
        }
    }

    @Test
    void offsetsStayInsideTheSelectedThreeByThreePlane() {
        assertTrue(MiningPlane.HORIZONTAL.surroundingOffsets().stream()
                .allMatch(offset -> offset.y() == 0));
        assertTrue(MiningPlane.NORTH_SOUTH.surroundingOffsets().stream()
                .allMatch(offset -> offset.z() == 0));
        assertTrue(MiningPlane.EAST_WEST.surroundingOffsets().stream()
                .allMatch(offset -> offset.x() == 0));
    }
}
