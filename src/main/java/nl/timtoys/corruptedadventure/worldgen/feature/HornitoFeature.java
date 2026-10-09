package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * {@code corruptedadventure:hornito} (change 47, G2-WB8): a spatter cone 2–4 high on the basalt flows, rock with a
 * glowing cap over its throat. Each layer is a ragged disc a little narrower than the one below; a column whose ground
 * lies lower is filled down to it (at most {@link #MAX_FILL}), so the cone never floats. Reads the frozen
 * {@code WORLD_SURFACE_WG} heightmap and its own chunk; writes only its own chunk (its radius stays below the margin of
 * its placement).
 */
public final class HornitoFeature extends Feature<HornitoConfiguration> {
    public static final int MAX_HEIGHT = 4;
    /** The widest layer's reach from the centre. */
    public static final int MAX_REACH = 3;
    private static final int MAX_FILL = 3;

    public HornitoFeature(Codec<HornitoConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<HornitoConfiguration> context) {
        HornitoConfiguration config = context.config();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int base = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ()) - 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(origin.getX(), base, origin.getZ());
        if (!level.getBlockState(pos).is(config.ground()) || !level.getBlockState(pos.move(0, 1, 0)).isAir()) {
            return false;
        }
        int height = config.height().sample(random);
        int minX = origin.getX() & ~15;
        int minZ = origin.getZ() & ~15;
        for (int dz = -MAX_REACH; dz <= MAX_REACH; dz++) {
            for (int dx = -MAX_REACH; dx <= MAX_REACH; dx++) {
                float jitter = random.nextFloat() * 0.6F;
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int layers = layers(dx, dz, height, jitter);
                if (layers == 0 || x < minX || x >= minX + 16 || z < minZ || z >= minZ + 16) {
                    continue;
                }
                int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                if (ground > base + 1 || ground < base - MAX_FILL) {
                    continue;
                }
                for (int y = ground + 1; y <= base + layers; y++) {
                    if (!level.getBlockState(pos.set(x, y, z)).isAir()) {
                        break;
                    }
                    boolean throat = dx == 0 && dz == 0 && y >= base + layers - 1;
                    BlockState state = (throat ? config.cap() : config.body()).getState(random, pos);
                    level.setBlock(pos, state, Block.UPDATE_CLIENTS);
                }
            }
        }
        return true;
    }

    /** How many layers stand on the column (dx, dz) of a cone {@code height} high; 0 outside it. */
    static int layers(int dx, int dz, int height, float jitter) {
        float distance = Mth.sqrt(dx * dx + dz * dz);
        int layers = 0;
        for (int k = 0; k < height; k++) {
            float radius = 0.5F + (height - k) * 0.65F + jitter - 0.3F;
            if (distance <= Math.min(radius, MAX_REACH)) {
                layers = k + 1;
            }
        }
        return layers;
    }
}
