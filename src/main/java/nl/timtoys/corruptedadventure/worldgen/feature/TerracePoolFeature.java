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
 * {@code corruptedadventure:terrace_pool} (change 47d, decision 4b-2): a boiling pool of the sulfur fields in a basin of
 * travertine: the pool flush with the ground (one colour per pool), a raised lip around it and a band of terrace steps
 * outside the lip. Only columns whose top lies at the pool's level (the lip and terraces one block either way) and is
 * listed ground take part, so the pool follows level ground. Reads the frozen {@code WORLD_SURFACE_WG} heightmap and
 * its own chunk; writes only its own chunk (the placement margin keeps the pool whole).
 */
public final class TerracePoolFeature extends Feature<TerracePoolConfiguration> {
    public static final int MAX_RADIUS = 3;
    /** The lip and the terraces around the pool, in blocks. */
    static final double LIP = 1.2;
    static final double TERRACE = 2.6;
    /** How far the pool with its terraces reaches from its centre. */
    public static final int MAX_REACH = MAX_RADIUS + 3;
    private static final int EDGE_POINTS = 8;

    public TerracePoolFeature(Codec<TerracePoolConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<TerracePoolConfiguration> context) {
        TerracePoolConfiguration config = context.config();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int radius = config.radius().sample(random);
        float angle = random.nextFloat() * Mth.TWO_PI;
        float[] edge = new float[EDGE_POINTS];
        for (int i = 0; i < EDGE_POINTS; i++) {
            edge[i] = 0.75F + 0.25F * random.nextFloat();
        }
        BlockState fill = config.fill().getState(random, origin);
        int level0 = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ()) - 1;
        int minX = origin.getX() & ~15;
        int minZ = origin.getZ() & ~15;
        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int reach = radius + 3;
        for (int dz = -reach; dz <= reach; dz++) {
            for (int dx = -reach; dx <= reach; dx++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                if (x < minX || x >= minX + 16 || z < minZ || z >= minZ + 16) {
                    continue;
                }
                // Distance in blocks to the ragged edge's scale (≤ radius = pool).
                double blocks = GroundPatchFeature.distance(dx, dz, radius, 1.0F, angle, edge) * radius;
                if (blocks > radius + TERRACE) {
                    continue;
                }
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                boolean pool = blocks <= radius;
                if (pool ? top != level0 : Math.abs(top - level0) > 1 || !level.getBlockState(pos.set(x, top, z)).is(config.ground())) {
                    continue;
                }
                if (pool) {
                    if (level.getBlockState(pos.set(x, top, z)).is(config.ground())) {
                        level.setBlock(pos, fill, Block.UPDATE_CLIENTS);
                        placed = true;
                    }
                } else if (blocks <= radius + LIP) {
                    level.setBlock(pos.set(x, top, z), config.lip().getState(random, pos), Block.UPDATE_CLIENTS);
                    if (top == level0 && level.getBlockState(pos.set(x, top + 1, z)).isAir()) {
                        level.setBlock(pos, config.lip().getState(random, pos), Block.UPDATE_CLIENTS);
                    }
                    placed = true;
                } else {
                    level.setBlock(pos.set(x, top, z), config.terrace().getState(random, pos), Block.UPDATE_CLIENTS);
                    placed = true;
                }
            }
        }
        return placed;
    }
}
