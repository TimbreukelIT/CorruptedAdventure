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
 * {@code corruptedadventure:ground_patch} (change 47): a ragged, possibly stretched patch of other ground, optionally
 * with a raised core: lava tongues of blackstone, mud pools, dead coral reefs. Only the listed ground is replaced, so
 * the patch follows the terrain; the raised layers stack only on air. Reads the frozen {@code WORLD_SURFACE_WG}
 * heightmap and its own chunk's blocks and writes only its own chunk: columns outside it are left out (the placement
 * margin keeps most patches whole).
 */
public final class GroundPatchFeature extends Feature<GroundPatchConfiguration> {
    public static final int MAX_RADIUS = 6;
    private static final int EDGE_POINTS = 8;
    /** How much less of the core each raised layer covers (as a share of the patch). */
    private static final float RAISE_STEP = 0.35F;

    public GroundPatchFeature(Codec<GroundPatchConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<GroundPatchConfiguration> context) {
        GroundPatchConfiguration config = context.config();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int radius = config.radius().sample(random);
        int depth = config.depth().sample(random);
        int raise = config.raise().sample(random);
        float angle = random.nextFloat() * Mth.TWO_PI;
        float[] edge = new float[EDGE_POINTS];
        for (int i = 0; i < EDGE_POINTS; i++) {
            edge[i] = 0.7F + 0.3F * random.nextFloat();
        }
        int minX = origin.getX() & ~15;
        int minZ = origin.getZ() & ~15;
        int reach = Mth.ceil(radius * config.stretch()) + 1;
        boolean changed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dz = -reach; dz <= reach; dz++) {
            for (int dx = -reach; dx <= reach; dx++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                if (x < minX || x >= minX + 16 || z < minZ || z >= minZ + 16) {
                    continue;
                }
                float d = distance(dx, dz, radius, config.stretch(), angle, edge);
                if (d > 1.0F) {
                    continue;
                }
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                if (!level.getBlockState(pos.set(x, top, z)).is(config.replaceable())) {
                    continue;
                }
                for (int y = top; y > top - depth; y--) {
                    if (level.getBlockState(pos.set(x, y, z)).is(config.replaceable())) {
                        level.setBlock(pos, config.material().getState(random, pos), Block.UPDATE_CLIENTS);
                        changed = true;
                    }
                }
                for (int layer = 1; layer <= raise && d <= 1.0F - layer * RAISE_STEP; layer++) {
                    BlockState above = level.getBlockState(pos.set(x, top + layer, z));
                    if (!above.isAir()) {
                        break;
                    }
                    level.setBlock(pos, config.raised().getState(random, pos), Block.UPDATE_CLIENTS);
                }
            }
        }
        return changed;
    }

    /** Distance of (dx, dz) from the centre as a share of the ragged, stretched edge there (≤ 1 = inside). */
    static float distance(int dx, int dz, int radius, float stretch, float angle, float[] edge) {
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        float u = (dx * cos + dz * sin) / stretch;
        float v = -dx * sin + dz * cos;
        float index = (float) ((Mth.atan2(v, u) + Math.PI) / (2.0 * Math.PI) * edge.length);
        int below = Mth.floor(index);
        float scale = Mth.lerp(index - below, edge[Math.floorMod(below, edge.length)], edge[Math.floorMod(below + 1, edge.length)]);
        return Mth.sqrt(u * u + v * v) / (radius * scale);
    }
}
