package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * {@code corruptedadventure:toxic_pool} (package 76; contract ring 4 §3: only sources in closed basins, never against
 * water): a small bowl of toxic sludge sunk into flat ground. The pool lies in the ground at the origin's height (the
 * top block under {@code WORLD_SURFACE_WG}): one deep at the rim, {@code depth} in the middle. It is only placed when
 * the whole bowl lies in the origin's chunk on level ground, every block of it is solid now, and every side and the
 * bottom of every pool block is either more pool or solid, dry ground ({@link #closed}): no sludge ever touches water
 * or air beside or below it, so nothing flows and nothing mixes. The sources get no tick and stay as set.
 */
public final class ToxicPoolFeature extends Feature<ToxicPoolConfiguration> {
    public static final int MAX_RADIUS = 4;

    public ToxicPoolFeature(Codec<ToxicPoolConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<ToxicPoolConfiguration> context) {
        WorldGenLevel level = context.level();
        ToxicPoolConfiguration config = context.config();
        RandomSource random = context.random();
        BlockPos top = context.origin().below();
        Set<BlockPos> pool = shape(top, config.radius().sample(random), config.depth().sample(random));
        int chunkX = top.getX() >> 4;
        int chunkZ = top.getZ() >> 4;
        for (BlockPos pos : pool) {
            // One block from the chunk's edge: every block {@link #closed} reads is the own chunk's.
            if (pos.getX() >> 4 != chunkX || pos.getZ() >> 4 != chunkZ || (pos.getX() & 15) == 0 || (pos.getX() & 15) == 15
                    || (pos.getZ() & 15) == 0 || (pos.getZ() & 15) == 15
                    || level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX(), pos.getZ()) - 1 != top.getY()
                    || !solid(level.getBlockState(pos))) {
                return false;
            }
        }
        if (!closed(pool, pos -> solid(level.getBlockState(pos)))) {
            return false;
        }
        for (BlockPos pos : pool) {
            level.setBlock(pos, config.fluid(), Block.UPDATE_CLIENTS);
        }
        return true;
    }

    /** Ground that holds a pool: not air, no fluid, not a plant. */
    private static boolean solid(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && !state.canBeReplaced();
    }

    /**
     * The pool's blocks around {@code top} (the top block of the middle column): a disc of {@code radius}, one deep at
     * the rim and {@code depth} deep where the column lies well inside.
     */
    static Set<BlockPos> shape(BlockPos top, int radius, int depth) {
        Set<BlockPos> pool = new LinkedHashSet<>();
        int limit = radius * radius + radius;
        int inner = (radius - 1) * (radius - 1);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > limit) {
                    continue;
                }
                int deep = d2 <= inner ? depth : 1;
                for (int dy = 0; dy < deep; dy++) {
                    pool.add(top.offset(dx, -dy, dz));
                }
            }
        }
        return pool;
    }

    /** Whether every side and the bottom of every pool block is more pool or {@code solid}. */
    static boolean closed(Set<BlockPos> pool, Predicate<BlockPos> solid) {
        for (BlockPos pos : pool) {
            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP) {
                    continue;
                }
                BlockPos next = pos.relative(direction);
                if (!pool.contains(next) && !solid.test(next)) {
                    return false;
                }
            }
        }
        return true;
    }
}
