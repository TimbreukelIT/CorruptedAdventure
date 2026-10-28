package nl.timtoys.corruptedadventure.item.lens;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.network.LensScanPayload;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LensScanTest {
    @Test
    void aSectionGivesItsGlitchBlocksInsideTheSphereOnly() {
        Block glitch = ModBlocks.GLITCH_BLOCK.get();
        PalettedContainer<BlockState> states = new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY,
                Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES);
        states.set(0, 0, 0, glitch.defaultBlockState());
        states.set(15, 15, 15, glitch.defaultBlockState());
        states.set(8, 8, 8, glitch.defaultBlockState());
        states.set(9, 8, 8, Blocks.STONE.defaultBlockState());
        // Section at (32, -16, 0); the centre near its middle, radius 9: (8, 8, 8) inside, the corners (13.9 away) not.
        Vec3 centre = new Vec3(32 + 8.5, -16 + 8.5, 8.5);
        List<BlockPos> found = new ArrayList<>();
        LensScan.collect(states, glitch, 32, -16, 0, centre, 9, found);
        assertEquals(List.of(new BlockPos(40, -8, 8)), found);

        found.clear();
        LensScan.collect(states, glitch, 32, -16, 0, centre, 32, found);
        assertEquals(Set.of(new BlockPos(32, -16, 0), new BlockPos(47, -1, 15), new BlockPos(40, -8, 8)), new HashSet<>(found));

        found.clear();
        LensScan.collect(states, glitch, 32, -16, 0, new Vec3(200, 0, 0), 32, found);
        assertTrue(found.isEmpty(), "a section beyond the sphere reads nothing");
    }

    @Test
    void theNearestAreKeptWhenThereAreTooMany() {
        List<BlockPos> found = new ArrayList<>();
        for (int x = 20; x >= 0; x--) {
            found.add(new BlockPos(x, 0, 0));
        }
        assertEquals(List.of(new BlockPos(0, 0, 0), new BlockPos(1, 0, 0), new BlockPos(2, 0, 0)),
                LensScan.nearest(found, Vec3.ZERO, 3));
        assertEquals(found, LensScan.nearest(found, Vec3.ZERO, 21), "under the cap nothing changes");
    }

    @Test
    void aHoleIsMarkedAtTheLowestFloorNextToIt() {
        FakeGround ground = new FakeGround();
        // A hole of Ø 3 round (100, 50); the floor next to it at 4 (first air at 5), one side a little higher.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ground.open(100 + dx, 50 + dz);
            }
        }
        ground.floorEverywhere(5);
        ground.floor(102, 50, 7);
        List<BlockPos> holes = LensScan.holes(ground, 100, 50, 32);
        assertEquals(9, holes.size());
        assertTrue(holes.stream().allMatch(pos -> pos.getY() == 4), "the floor's own height: " + holes);
        assertTrue(ground.floorReads <= LensScan.FLOOR_SAMPLES, "a few floor samples per hole, not one per column");
    }

    @Test
    void separateHolesTakeTheirOwnFloorAndAHoleWithoutAFloorIsLeftOut() {
        FakeGround ground = new FakeGround();
        ground.open(0, 0);
        ground.floor(1, 0, -20);
        ground.floor(-1, 0, -20);
        ground.floor(0, 1, -20);
        ground.floor(0, -1, -20);
        ground.open(10, 0);
        ground.floor(11, 0, 9);
        ground.floor(9, 0, 9);
        ground.floor(10, 1, 9);
        ground.floor(10, -1, 9);
        ground.open(20, 0); // no floor round it (open void, unloaded): nowhere to put it
        List<BlockPos> holes = LensScan.holes(ground, 0, 0, 32);
        assertEquals(Set.of(new BlockPos(0, -21, 0), new BlockPos(10, 8, 0)), new HashSet<>(holes));
    }

    @Test
    void onlyColumnsWithinTheRadiusCountAndTheNearestAreKept() {
        FakeGround ground = new FakeGround();
        ground.open(40, 0);
        ground.floorEverywhere(0);
        assertTrue(LensScan.holes(ground, 0, 0, 32).isEmpty(), "40 blocks away is outside the scan");

        FakeGround wide = new FakeGround();
        for (int x = -32; x <= 32; x++) {
            for (int z = -32; z <= 32; z++) {
                if ((x + z & 1) == 0) {
                    wide.open(x, z);
                }
            }
        }
        wide.floorEverywhere(0);
        List<BlockPos> holes = LensScan.holes(wide, 0, 0, 32);
        assertEquals(LensScanPayload.MAX_HOLES, holes.size());
        // Half the columns are open, so the nearest 256 lie within about 13 (the 256th at √164).
        assertTrue(holes.stream().allMatch(pos -> pos.getX() * pos.getX() + pos.getZ() * pos.getZ() <= 164),
                "the nearest columns are kept");
    }

    @Test
    void aLookingPlayerIsScannedAtOnceThenEverySecond() {
        assertTrue(LensWatch.due(null, 1234L));
        assertFalse(LensWatch.due(1254L, 1253L));
        assertTrue(LensWatch.due(1254L, 1254L));
        assertEquals(20, LensWatch.SCAN_TICKS);
    }

    /** Open columns and floor tops by hand; counts the floor reads. */
    private static final class FakeGround implements LensScan.Ground {
        private final Set<Long> open = new HashSet<>();
        private final Map<Long, Integer> floors = new HashMap<>();
        private Integer everywhere;
        int floorReads;

        void open(int x, int z) {
            open.add(key(x, z));
        }

        void floor(int x, int z, int top) {
            floors.put(key(x, z), top);
        }

        void floorEverywhere(int top) {
            everywhere = top;
        }

        @Override
        public boolean openBelow(int x, int z) {
            return open.contains(key(x, z));
        }

        @Override
        public int floorTop(int x, int z) {
            floorReads++;
            if (open.contains(key(x, z))) {
                return Integer.MIN_VALUE;
            }
            Integer top = floors.get(key(x, z));
            return top != null ? top : everywhere != null ? everywhere : Integer.MIN_VALUE;
        }

        private static long key(int x, int z) {
            return (long) x << 32 | (z & 0xFFFFFFFFL);
        }
    }
}
