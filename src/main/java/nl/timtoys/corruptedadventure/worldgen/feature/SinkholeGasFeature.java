package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;

/**
 * {@code corruptedadventure:sinkhole_gas} (package 76, Q10 = A): one or two layers of swamp gas on the floor of the
 * peat sinkholes. Per chunk, per column of the pure terrain ({@link IslandField.Column#sinkholeFloor}): where the bowl
 * lies at least {@code min_depth} below the surface, the air right above its real floor (found by a short scan around
 * the floor's height) fills with gas, two layers from {@code deep_depth} on. Only air above a dry, solid floor takes
 * gas (a flooded sinkhole stays as it is). Placed once per chunk at its corner; writes only in its own chunk.
 */
public final class SinkholeGasFeature extends Feature<SinkholeGasConfiguration> {
    /** How far around the floor's height the scan looks for the real floor. */
    static final int SCAN = 3;

    public SinkholeGasFeature(Codec<SinkholeGasConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<SinkholeGasConfiguration> context) {
        WorldGenLevel level = context.level();
        IslandField field = Ring4Terrain.field(level);
        if (field == null) {
            return false;
        }
        SinkholeGasConfiguration config = context.config();
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        if (!anySinkhole(field, minX, minZ)) {
            return false;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                IslandField.Column column = field.column(x, z);
                double floor = column.sinkholeFloor();
                double depth = column.height() - floor;
                if (Double.isNaN(floor) || depth < config.minDepth()) {
                    continue;
                }
                int layers = depth >= config.deepDepth() ? 2 : 1;
                int bottom = Mth.ceil(floor) - 1;
                int x0 = x;
                int z0 = z;
                int ground = IslandGlowFeature.topBlock(bottom + SCAN, bottom - SCAN, y -> level.getBlockState(pos.set(x0, y, z0)).isAir());
                if (ground == Integer.MIN_VALUE) {
                    continue;
                }
                BlockState below = level.getBlockState(pos.set(x, ground, z));
                if (!below.getFluidState().isEmpty()) {
                    continue;
                }
                for (int y = ground + 1; y <= ground + layers; y++) {
                    if (!level.getBlockState(pos.set(x, y, z)).isAir()) {
                        break;
                    }
                    level.setBlock(pos, config.gas(), Block.UPDATE_CLIENTS);
                    placed = true;
                }
            }
        }
        return placed;
    }

    /**
     * Whether a sinkhole's bowl reaches into the chunk, asked on a grid of {@link #PROBE} (a bowl's open floor is at least
     * ~2.7 blocks in radius, wider than the grid's farthest point from its nodes, 2.1), so a chunk without one costs 36
     * columns instead of 256.
     */
    private static boolean anySinkhole(IslandField field, int minX, int minZ) {
        for (int dx = 0; dx < 16; dx += PROBE) {
            for (int dz = 0; dz < 16; dz += PROBE) {
                if (!Double.isNaN(field.column(minX + dx, minZ + dz).sinkholeFloor())) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The probe grid: 0, 3, …, 15. */
    static final int PROBE = 3;
}
