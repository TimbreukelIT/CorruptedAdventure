package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

/**
 * {@code corruptedadventure:ground_vent} (change 47): a vent in the ground's top (a fumarole, a smouldering vent) with a
 * ragged collar around it (sulfur crust, scorched stone) and a few blocks under it that turn into what the vent
 * brought up (sulfur ore, magma). The vent needs open air above and replaceable ground; the collar replaces only
 * ground tops with air above. Reads the frozen {@code WORLD_SURFACE_WG} heightmap and its own chunk; writes only its own
 * chunk (columns outside it are left out).
 */
public final class GroundVentFeature extends Feature<GroundVentConfiguration> {
    public static final int MAX_COLLAR = 3;

    public GroundVentFeature(Codec<GroundVentConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<GroundVentConfiguration> context) {
        GroundVentConfiguration config = context.config();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int minX = origin.getX() & ~15;
        int minZ = origin.getZ() & ~15;
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ()) - 1;
        BlockPos vent = new BlockPos(origin.getX(), top, origin.getZ());
        if (!openGround(level, config, vent)) {
            return false;
        }
        level.setBlock(vent, config.vent(), Block.UPDATE_CLIENTS);

        int radius = config.collarRadius().sample(random);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int x = vent.getX() + dx;
                int z = vent.getZ() + dz;
                // Always draw, so the ring does not depend on which columns lie in the chunk.
                boolean keep = random.nextFloat() < config.collarChance();
                if ((dx == 0 && dz == 0) || dx * dx + dz * dz > radius * radius + radius || !keep
                        || x < minX || x >= minX + 16 || z < minZ || z >= minZ + 16) {
                    continue;
                }
                pos.set(x, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1, z);
                if (Math.abs(pos.getY() - top) <= 1 && openGround(level, config, pos)) {
                    level.setBlock(pos, config.collar().getState(random, pos), Block.UPDATE_CLIENTS);
                }
            }
        }

        int count = config.belowCount().sample(random);
        for (int i = 0; i < count; i++) {
            int x = vent.getX() + random.nextInt(3) - 1;
            int z = vent.getZ() + random.nextInt(3) - 1;
            int y = top - 1 - random.nextInt(3);
            if (x < minX || x >= minX + 16 || z < minZ || z >= minZ + 16) {
                continue;
            }
            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            for (OreConfiguration.TargetBlockState target : config.below()) {
                if (target.target.test(state, random)) {
                    level.setBlock(pos, target.state, Block.UPDATE_CLIENTS);
                    break;
                }
            }
        }
        return true;
    }

    private static boolean openGround(WorldGenLevel level, GroundVentConfiguration config, BlockPos ground) {
        return level.getBlockState(ground).is(config.ground()) && level.getBlockState(ground.above()).isAir();
    }
}
