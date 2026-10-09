package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * {@code corruptedadventure:impact_crater} (change 47, G2-WB9): the pit an old lava bomb left on the peaks, a bowl
 * 1–2 deep with a scorched floor, obsidian shards on the floor and the rim and a glowing core in the middle. Only dug
 * where every column of the bowl is listed ground at the origin's level (±1), so it never opens a wall or a cave; the
 * floor is solid ground below the dug blocks, with no fluid under it. Reads the frozen {@code WORLD_SURFACE_WG}
 * heightmap and its own chunk; writes only its own chunk.
 */
public final class ImpactCraterFeature extends Feature<ImpactCraterConfiguration> {
    public static final int MAX_RADIUS = 3;

    public ImpactCraterFeature(Codec<ImpactCraterConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<ImpactCraterConfiguration> context) {
        ImpactCraterConfiguration config = context.config();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int radius = config.radius().sample(random);
        int minX = origin.getX() & ~15;
        int minZ = origin.getZ() & ~15;
        if (origin.getX() - radius - 1 < minX || origin.getX() + radius + 1 >= minX + 16
                || origin.getZ() - radius - 1 < minZ || origin.getZ() + radius + 1 >= minZ + 16) {
            return false;
        }
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ()) - 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dz = -radius - 1; dz <= radius + 1; dz++) {
            for (int dx = -radius - 1; dx <= radius + 1; dx++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                if (Math.abs(ground - top) > 1 || !level.getBlockState(pos.set(x, ground, z)).is(config.ground())
                        || !level.getBlockState(pos.set(x, ground + 1, z)).isAir() || fluidBelow(level, pos.set(x, ground, z))) {
                    return false;
                }
            }
        }
        for (int dz = -radius - 1; dz <= radius + 1; dz++) {
            for (int dx = -radius - 1; dx <= radius + 1; dx++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                int depth = depth(dx, dz, radius);
                float shard = random.nextFloat();
                if (depth == 0) {
                    // The rim: a shard thrown out now and then.
                    if (Mth.sqrt(dx * dx + dz * dz) <= radius + 1.0F && shard < config.shardChance() * 0.5F) {
                        level.setBlock(pos.set(x, ground, z), config.shards().getState(random, pos), Block.UPDATE_CLIENTS);
                    }
                    continue;
                }
                for (int y = ground; y > ground - depth; y--) {
                    level.setBlock(pos.set(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                pos.set(x, ground - depth, z);
                boolean centre = dx == 0 && dz == 0;
                level.setBlock(pos, centre ? config.core()
                        : shard < config.shardChance() ? config.shards().getState(random, pos) : config.floor().getState(random, pos),
                        Block.UPDATE_CLIENTS);
            }
        }
        return true;
    }

    /** Whether a fluid lies in the 3 blocks under {@code ground}: the pit would open it to the air (plan §0.6). */
    private static boolean fluidBelow(WorldGenLevel level, BlockPos.MutableBlockPos ground) {
        for (int i = 0; i < 3; i++) {
            if (!level.getFluidState(ground.move(0, -1, 0)).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** How deep the bowl is at (dx, dz): 2 in the middle, 1 towards the edge, 0 outside. */
    static int depth(int dx, int dz, int radius) {
        float d = Mth.sqrt(dx * dx + dz * dz) / (radius + 0.5F);
        return d >= 1.0F ? 0 : d < 0.5F ? 2 : 1;
    }
}
