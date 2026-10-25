package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * {@code corruptedadventure:salt_ridge} (G2 ring 2, decision G2-R2-3): pressure ridges on the salt flats. Where the patch
 * noise is high, every crack of the crust (a top block {@code edge}: the polygon edges the surface rule draws in salt
 * block) gets one block of {@code ridge} on top, with short breaks; where three or four cracks meet, now and then two.
 * The hexagons stand out as low white walls.
 * <p>
 * One placement per chunk; it walks the chunk's own columns in a fixed order and reads only its own blocks (the crust is
 * surface rule, the pools and pillars of this chunk came before it), so it is the same in every generation order.
 */
public final class SaltRidgeFeature extends Feature<SaltRidgeConfiguration> {
    public SaltRidgeFeature(Codec<SaltRidgeConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<SaltRidgeConfiguration> context) {
        WorldGenLevel level = context.level();
        SaltRidgeConfiguration config = context.config();
        RandomSource random = context.random();
        NormalNoise patches = level.getLevel().getChunkSource().randomState().getOrCreateNoise(config.patches());
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        int[] top = new int[256];
        boolean[] edge = new boolean[256];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dz = 0; dz < 16; dz++) {
            for (int dx = 0; dx < 16; dx++) {
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, minX + dx, minZ + dz) - 1;
                top[dz * 16 + dx] = y;
                edge[dz * 16 + dx] = y <= config.maxY() && level.getBlockState(pos.set(minX + dx, y, minZ + dz)).equals(config.edge())
                        && level.isEmptyBlock(pos.set(minX + dx, y + 1, minZ + dz));
            }
        }
        boolean placed = false;
        for (int dz = 0; dz < 16; dz++) {
            for (int dx = 0; dx < 16; dx++) {
                int i = dz * 16 + dx;
                // Both rolls always, so a column's result never depends on the columns before it.
                float gapRoll = random.nextFloat();
                float junctionRoll = random.nextFloat();
                if (!edge[i] || NoiseBandFilter.sample(patches, 1.0, minX + dx, minZ + dz) < config.patchMin()) {
                    continue;
                }
                int height = height(neighbours(edge, top, dx, dz), gapRoll, junctionRoll, config.gap(), config.junctionChance());
                for (int h = 1; h <= height; h++) {
                    pos.set(minX + dx, top[i] + h, minZ + dz);
                    if (!level.isEmptyBlock(pos)) {
                        break;
                    }
                    level.setBlock(pos, config.ridge(), Block.UPDATE_CLIENTS);
                    placed = true;
                }
            }
        }
        return placed;
    }

    /** Cracks beside column (dx, dz) at the same height, in the chunk (the edge columns count fewer: no junction there). */
    static int neighbours(boolean[] edge, int[] top, int dx, int dz) {
        int count = 0;
        int y = top[dz * 16 + dx];
        int[][] sides = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] side : sides) {
            int nx = dx + side[0];
            int nz = dz + side[1];
            if (nx >= 0 && nx < 16 && nz >= 0 && nz < 16 && edge[nz * 16 + nx] && top[nz * 16 + nx] == y) {
                count++;
            }
        }
        return count;
    }

    /** The ridge on a crack column: 0 (a break), 1, or 2 at a junction of three or four cracks. */
    static int height(int neighbours, float gapRoll, float junctionRoll, float gap, float junctionChance) {
        if (gapRoll < gap) {
            return 0;
        }
        return neighbours >= 3 && junctionRoll < junctionChance ? 2 : 1;
    }
}
