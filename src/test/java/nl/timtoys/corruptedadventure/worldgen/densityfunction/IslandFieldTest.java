package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The skeleton of ring 4's island field (59): found through the router, read access in place, no islands yet. */
class IslandFieldTest {
    private static final IslandField FIELD = new IslandField(TestFunctions.noise(20260930L, 0, 1.0), IslandField.Settings.DEFAULT);

    @Test
    void foundInsideARouterTree() {
        DensityFunction root = DensityFunctions.add(DensityFunctions.constant(1.0),
                DensityFunctions.max(DensityFunctions.zero(), DensityFunctions.flatCache(FIELD)));
        // mapAll rebuilds the tree, so the field found is an equal copy.
        assertEquals(FIELD, IslandField.find(root));
        assertNull(IslandField.find(DensityFunctions.add(DensityFunctions.constant(1.0), DensityFunctions.zero())));
    }

    @Test
    void aNoiseChunkCopyIsEqualAndKeepsTheDecisions() {
        DensityFunction copy = FIELD.mapAll(new DensityFunction.Visitor() {
            @Override
            public DensityFunction apply(DensityFunction function) {
                return function;
            }
        });
        assertEquals(FIELD, copy);
        assertEquals(FIELD.hashCode(), copy.hashCode());
    }

    @Test
    void skeletonHasNoIslandsAndEmptyColumns() {
        for (int x = 6000; x <= 8000; x += 97) {
            for (int z = -500; z <= 500; z += 89) {
                assertNull(FIELD.islandAt(x, z));
                IslandField.Column column = FIELD.column(x, z);
                assertEquals(0.0, column.islandMask());
                assertEquals(Integer.MIN_VALUE, column.fallTop());
                assertEquals(0.0, FIELD.compute(new DensityFunction.SinglePointContext(x, 120, z)));
            }
        }
        assertTrue(FIELD.islandsWithin(6000, -500, 8000, 500).isEmpty());
        assertTrue(FIELD.liftsWithin(6000, -500, 8000, 500).isEmpty());
        assertTrue(FIELD.waterfallsWithin(6000, -500, 8000, 500).isEmpty());
        assertTrue(FIELD.worldTrees(6000, -500, 8000, 500).isEmpty());
    }

    /** Review 59 (2nd): one rule for overlapping islands, whatever the order: bottom, top, then the centre. */
    @Test
    void overlappingIslandsAreChosenByOneRuleWhateverTheOrder() {
        IslandField.Island low = new IslandField.Island(7000.0, 0.0, 20.0, 100, 160);
        IslandField.Island high = new IslandField.Island(7010.0, 0.0, 20.0, 130, 150);
        IslandField.Island sameBottomHigherTop = new IslandField.Island(7005.0, 0.0, 20.0, 130, 170);
        IslandField.Island twin = new IslandField.Island(7006.0, 0.0, 20.0, 130, 170);
        List<IslandField.Island> islands = new ArrayList<>(List.of(low, high, sameBottomHigherTop, twin));
        for (int i = 0; i < 8; i++) {
            Collections.shuffle(islands, new Random(i));
            assertEquals(twin, IslandField.uppermost(islands));
        }
        assertEquals(high, IslandField.uppermost(List.of(low, high)));
        assertNull(IslandField.uppermost(List.of()));
    }

    @Test
    void islandCoversItsFootprint() {
        IslandField.Island island = new IslandField.Island(7000.5, 10.5, 20.0, 110, 150);
        assertTrue(island.covers(7000, 10));
        assertTrue(island.covers(7019, 10));
        assertTrue(!island.covers(7022, 10));
        assertTrue(island.reaches(7015, 25, 7100, 100));
        assertTrue(!island.reaches(7030, 40, 7100, 100));
    }
}
