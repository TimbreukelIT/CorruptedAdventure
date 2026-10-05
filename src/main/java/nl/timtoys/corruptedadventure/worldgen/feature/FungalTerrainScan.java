package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What the Nether ring 2 features see as terrain (change 35, the determinism rule): air and the growths
 * ({@link GrowthPriority}, tag {@code fungal_growths}) are see-through, everything else (rock, lava, ores, dew sources)
 * is terrain. A feature that decides only on terrain gets the same answer whichever neighbouring chunk was decorated
 * first, because no feature changes terrain except by turning terrain into dew source (still terrain) or ore (idem).
 */
public final class FungalTerrainScan {
    /** Returned by {@link #floor} and {@link #ceiling} when there is nothing (also when the scan starts inside terrain). */
    public static final int NONE = Integer.MIN_VALUE;

    private FungalTerrainScan() {
    }

    public static boolean isTerrain(BlockState state) {
        return !state.isAir() && !GrowthPriority.isGrowth(state);
    }

    public static boolean isLava(BlockState state) {
        return state.is(Blocks.LAVA);
    }

    /** Y of the first terrain block at or below {@code y} (within {@code max} blocks), or {@link #NONE}; {@link #NONE} if {@code y} is terrain itself. */
    public static int floor(FungalWorld world, int x, int y, int z, int max) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
        if (isTerrain(world.get(pos))) {
            return NONE;
        }
        for (int i = 1; i <= max; i++) {
            pos.setY(y - i);
            if (isTerrain(world.get(pos))) {
                return y - i;
            }
        }
        return NONE;
    }

    /** Y of the first terrain block at or above {@code y} (within {@code max} blocks), or {@link #NONE}; {@link #NONE} if {@code y} is terrain itself. */
    public static int ceiling(FungalWorld world, int x, int y, int z, int max) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
        if (isTerrain(world.get(pos))) {
            return NONE;
        }
        for (int i = 1; i <= max; i++) {
            pos.setY(y + i);
            if (isTerrain(world.get(pos))) {
                return y + i;
            }
        }
        return NONE;
    }
}
