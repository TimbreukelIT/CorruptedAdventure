package nl.timtoys.corruptedadventure.condition;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The safe spot of a void teleport (package 91, {@code golf-1-conditions.md} §2), on a small fake level: never in a
 * block or fluid, always on a sturdy walkable floor, 1–5 blocks away, never in the outer band, never next to a drop of
 * more than 3 or above the void; no spot, no teleport.
 */
class VoidTeleportTargetTest {
    private static final int FLOOR = 63;
    private static final int FEET = FLOOR + 1;
    private static final BlockPos PLAYER = new BlockPos(0, FEET, 0);
    private static final int RUNS = 400;

    /** Blocks by position, air elsewhere; the build height of the overworld. */
    private static final class FakeLevel implements BlockGetter {
        private final Map<BlockPos, BlockState> blocks = new HashMap<>();

        FakeLevel set(int x, int y, int z, BlockState state) {
            blocks.put(new BlockPos(x, y, z), state);
            return this;
        }

        /** A square of {@code state} from {@code -radius} to {@code radius} at {@code y}. */
        FakeLevel layer(int y, int radius, BlockState state) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    set(x, y, z, state);
                }
            }
            return this;
        }

        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return getBlockState(pos).getFluidState();
        }

        @Override
        public int getHeight() {
            return 384;
        }

        @Override
        public int getMinBuildHeight() {
            return -64;
        }
    }

    private static VoidTeleportTarget.Terrain terrain(FakeLevel level, BiPredicate<Integer, Integer> columns) {
        return new VoidTeleportTarget.Terrain() {
            @Override
            public BlockGetter blocks() {
                return level;
            }

            @Override
            public boolean allowsColumn(int x, int z) {
                return columns.test(x, z);
            }
        };
    }

    private static Optional<BlockPos> find(VoidTeleportTarget.Terrain terrain, long seed) {
        return VoidTeleportTarget.find(terrain, PLAYER, 1, 5, RandomSource.create(seed));
    }

    private static FakeLevel flatStone() {
        return new FakeLevel().layer(FLOOR, 12, Blocks.STONE.defaultBlockState());
    }

    @Test
    void onFlatGroundEveryTargetIsAirOnStoneOneToFiveBlocksAway() {
        FakeLevel level = flatStone();
        VoidTeleportTarget.Terrain terrain = terrain(level, (x, z) -> true);
        int found = 0;
        for (long seed = 0; seed < RUNS; seed++) {
            Optional<BlockPos> target = find(terrain, seed);
            if (target.isEmpty()) {
                continue;
            }
            found++;
            BlockPos spot = target.get();
            assertEquals(FEET, spot.getY(), "on the floor, not floating: " + spot);
            assertTrue(level.getBlockState(spot).isAir() && level.getBlockState(spot.above()).isAir());
            assertTrue(level.getBlockState(spot.below()).is(Blocks.STONE));
            double distance = Math.hypot(spot.getX() - PLAYER.getX(), spot.getZ() - PLAYER.getZ());
            assertTrue(distance >= 1.0 && distance <= 5.0, "distance " + distance);
        }
        assertEquals(RUNS, found, "open flat ground always has a spot");
    }

    @Test
    void stepsUpAndDownWithinTheRangeButNeverIntoBlocks() {
        // Stone up to feet + 1 everywhere except a pit: the only open feet blocks are at feet + 2.
        FakeLevel level = flatStone().layer(FEET, 12, Blocks.STONE.defaultBlockState())
                .layer(FEET + 1, 12, Blocks.STONE.defaultBlockState());
        VoidTeleportTarget.Terrain terrain = terrain(level, (x, z) -> true);
        for (long seed = 0; seed < RUNS; seed++) {
            BlockPos spot = find(terrain, seed).orElseThrow();
            assertEquals(FEET + 2, spot.getY());
        }
        // Solid from the floor up to feet + 4: no open feet block within the range, no teleport.
        FakeLevel buried = flatStone();
        for (int y = FEET; y <= FEET + 4; y++) {
            buried.layer(y, 12, Blocks.STONE.defaultBlockState());
        }
        assertTrue(find(terrain(buried, (x, z) -> true), 1).isEmpty());
    }

    @Test
    void neverInWaterOrLavaOrOnMagmaOrNextToLava() {
        FakeLevel level = flatStone();
        // A strip of water at the feet, a magma floor and a lava pool in the floor elsewhere.
        for (int x = -12; x <= 12; x++) {
            level.set(x, FEET, 2, Blocks.WATER.defaultBlockState());
            level.set(x, FLOOR, -2, Blocks.MAGMA_BLOCK.defaultBlockState());
        }
        level.set(3, FLOOR, 0, Blocks.LAVA.defaultBlockState());
        VoidTeleportTarget.Terrain terrain = terrain(level, (x, z) -> true);
        for (long seed = 0; seed < RUNS; seed++) {
            Optional<BlockPos> target = find(terrain, seed);
            if (target.isEmpty()) {
                continue;
            }
            BlockPos spot = target.get();
            assertTrue(level.getBlockState(spot).getFluidState().isEmpty(), "in water: " + spot);
            assertFalse(level.getBlockState(spot.below()).is(Blocks.MAGMA_BLOCK), "on magma: " + spot);
            assertTrue(Math.abs(spot.getX() - 3) > 1 || Math.abs(spot.getZ()) > 1, "next to lava: " + spot);
        }
    }

    @Test
    void neverOnFireCactusOrPowderSnow() {
        FakeLevel level = flatStone();
        for (int x = -12; x <= 12; x++) {
            for (int z = -12; z <= 12; z++) {
                if ((x + z) % 3 == 0) {
                    level.set(x, FEET, z, Blocks.FIRE.defaultBlockState());
                } else if ((x + z) % 3 == 1) {
                    level.set(x, FEET, z, Blocks.POWDER_SNOW.defaultBlockState());
                } else {
                    level.set(x, FEET, z, Blocks.SWEET_BERRY_BUSH.defaultBlockState());
                }
            }
        }
        VoidTeleportTarget.Terrain terrain = terrain(level, (x, z) -> true);
        for (long seed = 0; seed < 50; seed++) {
            assertTrue(find(terrain, seed).isEmpty(), "a spot among fire, powder snow and berry bushes");
        }
    }

    @Test
    void neverIntoTheOuterBand() {
        // Only columns with x < 0 are inner band.
        FakeLevel level = flatStone();
        VoidTeleportTarget.Terrain terrain = terrain(level, (x, z) -> x < 0);
        int found = 0;
        for (long seed = 0; seed < RUNS; seed++) {
            Optional<BlockPos> target = find(terrain, seed);
            if (target.isPresent()) {
                found++;
                assertTrue(target.get().getX() < 0, "outer band: " + target.get());
            }
        }
        assertTrue(found > RUNS * 9 / 10, "half the directions still lead inward, 12 tries: " + found);
    }

    @Test
    void neverNextToADropOfMoreThanThreeOrAboveTheVoid() {
        // Ground only for x <= 1; beyond it a fall into nothing (a rift, the void).
        FakeLevel level = new FakeLevel();
        for (int x = -12; x <= 1; x++) {
            for (int z = -12; z <= 12; z++) {
                level.set(x, FLOOR, z, Blocks.STONE.defaultBlockState());
            }
        }
        VoidTeleportTarget.Terrain terrain = terrain(level, (x, z) -> true);
        for (long seed = 0; seed < RUNS; seed++) {
            find(terrain, seed).ifPresent(spot -> assertTrue(spot.getX() <= 0, "at the rim of a drop: " + spot));
        }
        // A step of 3 down next to the spot is fine, 4 is not.
        FakeLevel step3 = new FakeLevel().set(0, FLOOR, 0, Blocks.STONE.defaultBlockState())
                .layer(FLOOR - 3, 3, Blocks.STONE.defaultBlockState());
        assertTrue(VoidTeleportTarget.hasNoDropAround(step3, new BlockPos(0, FLOOR, 0)));
        FakeLevel step4 = new FakeLevel().set(0, FLOOR, 0, Blocks.STONE.defaultBlockState())
                .layer(FLOOR - 4, 3, Blocks.STONE.defaultBlockState());
        assertFalse(VoidTeleportTarget.hasNoDropAround(step4, new BlockPos(0, FLOOR, 0)));
        // Lava or water one block down next to the spot, on top of stone: no spot (the stone is no safe step).
        FakeLevel lavaStep = new FakeLevel().set(0, FLOOR, 0, Blocks.STONE.defaultBlockState())
                .layer(FLOOR - 2, 3, Blocks.STONE.defaultBlockState()).set(1, FLOOR - 1, 0, Blocks.LAVA.defaultBlockState());
        assertFalse(VoidTeleportTarget.hasNoDropAround(lavaStep, new BlockPos(0, FLOOR, 0)));
        FakeLevel waterStep = new FakeLevel().set(0, FLOOR, 0, Blocks.STONE.defaultBlockState())
                .layer(FLOOR - 2, 3, Blocks.STONE.defaultBlockState()).set(0, FLOOR - 1, 1, Blocks.WATER.defaultBlockState());
        assertFalse(VoidTeleportTarget.hasNoDropAround(waterStep, new BlockPos(0, FLOOR, 0)));
        FakeLevel dryStep = new FakeLevel().set(0, FLOOR, 0, Blocks.STONE.defaultBlockState())
                .layer(FLOOR - 2, 3, Blocks.STONE.defaultBlockState());
        assertTrue(VoidTeleportTarget.hasNoDropAround(dryStep, new BlockPos(0, FLOOR, 0)));
        // Nothing at all: never a spot.
        assertTrue(find(terrain(new FakeLevel(), (x, z) -> true), 3).isEmpty());
    }

    @Test
    void aFixedDistanceStillFindsSpots() {
        // teleportMinBlocks = teleportMaxBlocks = 1: one of the four neighbouring columns.
        VoidTeleportTarget.Terrain terrain = terrain(flatStone(), (x, z) -> true);
        for (long seed = 0; seed < 50; seed++) {
            BlockPos spot = VoidTeleportTarget.find(terrain, PLAYER, 1, 1, RandomSource.create(seed)).orElseThrow();
            assertEquals(1, Math.abs(spot.getX()) + Math.abs(spot.getZ()), "a neighbour: " + spot);
        }
    }

    @Test
    void noAllowedColumnNoTeleport() {
        assertTrue(find(terrain(flatStone(), (x, z) -> false), 5).isEmpty());
    }

    @Test
    void aSlabOrCarpetAtTheFeetIsNoSpot() {
        FakeLevel level = flatStone().layer(FEET, 12, Blocks.WHITE_CARPET.defaultBlockState());
        assertTrue(find(terrain(level, (x, z) -> true), 7).isEmpty());
        FakeLevel fences = flatStone().layer(FLOOR, 12, Blocks.OAK_FENCE.defaultBlockState());
        assertTrue(find(terrain(fences, (x, z) -> true), 7).isEmpty(), "a fence top is not sturdy");
    }
}
