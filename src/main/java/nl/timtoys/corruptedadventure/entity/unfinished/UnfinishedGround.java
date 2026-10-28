package nl.timtoys.corruptedadventure.entity.unfinished;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.OptionalDouble;

/**
 * The ground under a colossus that does not path-find: the highest motion-blocking block (leaves aside) over five points
 * of his footprint, so he climbs over trees, rocks and buildings instead of through them. A column without any block is
 * void; he never steps where his centre would hang over it, and an unloaded column stops him too (it is never loaded).
 */
final class UnfinishedGround {
    /** Blocks from his centre to the four outer samples (his torso is ~5 blocks wide). */
    private static final int REACH = 2;
    private static final int[][] SAMPLES = {{0, 0}, {REACH, 0}, {-REACH, 0}, {0, REACH}, {0, -REACH}};

    private UnfinishedGround() {
    }

    /** The surface Y at {@code x, z}; empty over void or unloaded ground. */
    static OptionalDouble surface(Level level, double x, double z) {
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        int best = Integer.MIN_VALUE;
        for (int[] sample : SAMPLES) {
            int sx = bx + sample[0];
            int sz = bz + sample[1];
            if (!level.hasChunkAt(new BlockPos(sx, 0, sz))) {
                return OptionalDouble.empty();
            }
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz);
            if (top <= level.getMinBuildHeight()) {
                if (sample[0] == 0 && sample[1] == 0) {
                    return OptionalDouble.empty();
                }
                continue;
            }
            best = Math.max(best, top);
        }
        return OptionalDouble.of(best);
    }

    /** Whether the column at {@code x, z} is void: no block at all (only for loaded chunks; unloaded is not void). */
    static boolean isVoid(Level level, int x, int z) {
        return level.hasChunkAt(new BlockPos(x, 0, z))
                && level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) <= level.getMinBuildHeight();
    }
}
