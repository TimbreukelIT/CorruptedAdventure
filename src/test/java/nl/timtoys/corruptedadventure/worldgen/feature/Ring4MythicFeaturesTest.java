package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The pure parts of the package 75 features (glow under the islands, what hangs from them, coral, filters). */
class Ring4MythicFeaturesTest {
    /**
     * Teststop 5a: light ≥ 1 on the dry land under the islands. A source sits 1 to spacing − 2 into its cell, so every
     * block of the cell lies within 4 blocks (horizontal steps) of it: a source of light 5 or more reaches all of them
     * on level ground.
     */
    @Test
    void everyBlockOfAGlowCellIsWithinReachOfItsSource() {
        int spacing = Ring4MythicFeatures.GLOW_SPACING;
        RandomSource random = RandomSource.create(42);
        Set<Integer> offsets = new java.util.HashSet<>();
        for (int i = 0; i < 1000; i++) {
            offsets.add(IslandGlowFeature.offset(random, spacing));
        }
        assertEquals(Set.of(1, 2), offsets);
        for (int sx : offsets) {
            for (int sz : offsets) {
                for (int x = 0; x < spacing; x++) {
                    for (int z = 0; z < spacing; z++) {
                        assertTrue(Math.abs(x - sx) + Math.abs(z - sz) <= 4, "block " + x + "," + z + " from " + sx + "," + sz);
                    }
                }
            }
        }
        assertEquals(0, 16 % spacing, "cells divide the chunk");
        // Where the drawn spot fails, the cell's other columns follow, nearest first; every column once.
        List<int[]> spots = IslandGlowFeature.spots(1, 2, spacing);
        assertEquals(spacing * spacing, spots.size());
        assertEquals(1, spots.get(0)[0]);
        assertEquals(2, spots.get(0)[1]);
        for (int i = 1; i < spots.size(); i++) {
            int before = Math.abs(spots.get(i - 1)[0] - 1) + Math.abs(spots.get(i - 1)[1] - 2);
            int now = Math.abs(spots.get(i)[0] - 1) + Math.abs(spots.get(i)[1] - 2);
            assertTrue(before <= now);
        }
    }

    /** The scan for the ground: the first block that is not air with air above; a start inside a solid passes its air first. */
    @Test
    void theGroundIsTheFirstBlockUnderAir() {
        // Solid up to 64, air 65..69, an island's underside from 70.
        assertEquals(64, IslandGlowFeature.topBlock(68, 60, y -> y > 64 && y < 70));
        assertEquals(64, IslandGlowFeature.topBlock(72, 60, y -> y > 64 && y < 70));
        assertEquals(Integer.MIN_VALUE, IslandGlowFeature.topBlock(68, 66, y -> y > 64 && y < 70), "the ground lies below the scan");
        assertEquals(Integer.MIN_VALUE, IslandGlowFeature.topBlock(68, 60, y -> false), "solid all the way: no open space");
    }

    @Test
    void whatHangsUnderAnIsland() {
        IslandUndersideConfiguration config = new IslandUndersideConfiguration(null, null, 0.05F, 2, 7, 0.004F, 40, 0.04F, 0.4F, 0.3F);
        assertEquals(IslandUndersideFeature.Kind.CLIMB, IslandUndersideFeature.kind(0.003F, config, false));
        assertEquals(IslandUndersideFeature.Kind.DANGLE, IslandUndersideFeature.kind(0.01F, config, false));
        assertEquals(IslandUndersideFeature.Kind.ACCENT, IslandUndersideFeature.kind(0.06F, config, false));
        assertEquals(IslandUndersideFeature.Kind.NONE, IslandUndersideFeature.kind(0.5F, config, false));
        // B2-3 b: in a curtain most columns climb, the rest shifts up behind them.
        assertEquals(IslandUndersideFeature.Kind.CLIMB, IslandUndersideFeature.kind(0.2F, config, true));
        assertEquals(IslandUndersideFeature.Kind.DANGLE, IslandUndersideFeature.kind(0.32F, config, true));
        assertEquals(IslandUndersideFeature.Kind.NONE, IslandUndersideFeature.kind(0.5F, config, true));
        // A climbing strand under a low island ends one block above the floor (gap − 1 blocks long).
        assertEquals(29, IslandUndersideFeature.length(IslandUndersideFeature.Kind.CLIMB, 30, 5, 40));
        // Under a high island it only dangles, never closer than two blocks to the floor.
        assertEquals(5, IslandUndersideFeature.length(IslandUndersideFeature.Kind.CLIMB, 60, 5, 40));
        assertEquals(1, IslandUndersideFeature.length(IslandUndersideFeature.Kind.DANGLE, 3, 5, 40));
        assertEquals(0, IslandUndersideFeature.length(IslandUndersideFeature.Kind.DANGLE, 2, 5, 40));
        assertEquals(1, IslandUndersideFeature.length(IslandUndersideFeature.Kind.ACCENT, 10, 5, 40));
        assertEquals(0, IslandUndersideFeature.length(IslandUndersideFeature.Kind.NONE, 10, 5, 40));
    }

    /** The coral's blocks and their wall fans stay in the chunk (23c). */
    @Test
    void coralStaysInItsChunk() {
        assertTrue(LocalCoralFeature.inside(new BlockPos(1, 60, 14), 0, 0));
        assertFalse(LocalCoralFeature.inside(new BlockPos(0, 60, 5), 0, 0));
        assertFalse(LocalCoralFeature.inside(new BlockPos(15, 60, 5), 0, 0));
        assertFalse(LocalCoralFeature.inside(new BlockPos(5, 60, 16), 0, 0));
        assertTrue(LocalCoralFeature.inside(new BlockPos(-15, 60, -2), -1, -1));
    }

    /** A vine between two islands: from edge to edge, face to face (no gaps a player falls through), sagging between. */
    @Test
    void aVineRunsFaceToFaceFromEdgeToEdgeAndSags() {
        IslandField.IslandPair pair = new IslandField.IslandPair(null, null, 10, 20, 120, 100, 34, 38, 112, 95, false);
        for (int offset : new int[]{0, IslandVinesFeature.SECOND_OFFSET}) {
            List<BlockPos> rope = IslandVinesFeature.rope(pair, 0.08F, offset);
            assertEquals(rope.size(), Set.copyOf(rope).size(), "each block once");
            for (int i = 1; i < rope.size(); i++) {
                assertEquals(1, rope.get(i).distManhattan(rope.get(i - 1)), "face to face at " + rope.get(i));
            }
            int lowest = rope.stream().mapToInt(BlockPos::getY).min().orElseThrow();
            int lowerEnd = Math.min(rope.get(0).getY(), rope.get(rope.size() - 1).getY());
            assertTrue(lowest < lowerEnd, "it sags below both ends");
        }
        List<BlockPos> rope = IslandVinesFeature.rope(pair, 0.08F, 0);
        assertEquals(new BlockPos(10, 118, 20), rope.get(0), "two under the first edge's top");
        assertEquals(new BlockPos(34, 110, 38), rope.get(rope.size() - 1), "two under the second edge's top");
        assertTrue(IslandVinesFeature.rope(new IslandField.IslandPair(null, null, 5, 5, 100, 90, 5, 5, 100, 90, false), 0.08F, 0).isEmpty());
    }

    @Test
    void groundOnlyKeepsTheTerrainsOwnSurface() {
        assertTrue(Ring4Filter.onGround(65, 64.2, 3));
        assertTrue(Ring4Filter.onGround(68, 64.2, 3));
        assertFalse(Ring4Filter.onGround(80, 64.2, 3), "an island's or a bridge deck's top");
    }

    /** Backlog r. 367/415: water on the frozen heightmaps, between the floor and the first open block. */
    @Test
    void frozenWaterLiesBetweenTheFloorAndTheFirstOpenBlock() {
        assertTrue(Ring4Filter.water(62, 60, 63));
        assertTrue(Ring4Filter.water(60, 60, 63));
        assertFalse(Ring4Filter.water(63, 60, 63), "the open block over the water");
        assertFalse(Ring4Filter.water(64, 64, 64), "dry land");
    }

    /** Review golf 3: under a floating island the pure terrain says where the water stands (ring 4's sea, a lake). */
    @Test
    void waterUnderAnIslandFollowsTheTerrain() {
        assertTrue(Ring4Filter.waterUnderIsland(62, 60.4, Integer.MIN_VALUE, true), "the sea's top block over the ground");
        assertTrue(Ring4Filter.waterUnderIsland(61, 60.4, Integer.MIN_VALUE, true));
        assertFalse(Ring4Filter.waterUnderIsland(60, 60.4, Integer.MIN_VALUE, true), "in the ground");
        assertFalse(Ring4Filter.waterUnderIsland(63, 60.4, Integer.MIN_VALUE, true), "air over the sea");
        assertFalse(Ring4Filter.waterUnderIsland(62, 60.4, Integer.MIN_VALUE, false), "no flat sea outside ring 4's own");
        assertTrue(Ring4Filter.waterUnderIsland(70, 66.0, 72, false), "a lake above the sea");
        assertFalse(Ring4Filter.waterUnderIsland(72, 66.0, 72, true), "over the lake (its top is exclusive)");
    }
}
