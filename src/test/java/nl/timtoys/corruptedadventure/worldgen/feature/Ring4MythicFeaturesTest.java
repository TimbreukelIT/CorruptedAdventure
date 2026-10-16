package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

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
        IslandUndersideConfiguration config = new IslandUndersideConfiguration(null, null, 0.05F, 2, 7, 0.004F, 40, 0.04F);
        assertEquals(IslandUndersideFeature.Kind.CLIMB, IslandUndersideFeature.kind(0.003F, config));
        assertEquals(IslandUndersideFeature.Kind.DANGLE, IslandUndersideFeature.kind(0.01F, config));
        assertEquals(IslandUndersideFeature.Kind.ACCENT, IslandUndersideFeature.kind(0.06F, config));
        assertEquals(IslandUndersideFeature.Kind.NONE, IslandUndersideFeature.kind(0.5F, config));
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

    @Test
    void groundOnlyKeepsTheTerrainsOwnSurface() {
        assertTrue(Ring4Filter.onGround(65, 64.2, 3));
        assertTrue(Ring4Filter.onGround(68, 64.2, 3));
        assertFalse(Ring4Filter.onGround(80, 64.2, 3), "an island's or a bridge deck's top");
    }
}
