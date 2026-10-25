package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

/**
 * {@code corruptedadventure:giant_hoodoo} (G2 ring 2): the eye-catcher of {@code hoodoo_fields}, a giant hoodoo of
 * 55–75 above the floor with one or two shorter giants beside it ({@link Ring2RockLandmarks.Hoodoo}): shafts in the
 * canyon strata swelling and narrowing with their bands, a neck and a balanced cap of scorched sandstone like the
 * ordinary hoodoos, fallen rock round their feet. The shafts run {@value Ring2RockLandmarks#ROOT} blocks under the pure
 * floor, so they never float on a slightly lower real floor. Every chunk writes only its own part.
 */
public final class GiantHoodooFeature extends Feature<NoneFeatureConfiguration> {
    private static final int FLAGS = Block.UPDATE_CLIENTS;
    /** Blocks of scorched sandstone under the cap (the neck's top, as on the ordinary hoodoos). */
    private static final int NECK = 2;
    /** Fallen rock: the ring round each foot it lies in (from the shaft out) and its share of the columns there. */
    private static final double RUBBLE_RING = 5.0;
    private static final double RUBBLE = 0.14;

    public GiantHoodooFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ChunkPos chunk = new ChunkPos(context.origin());
        boolean placed = false;
        for (Ring2RockLandmarks.Hoodoo hoodoo : Ring2RockLandmarks.hoodoosNear(level, chunk.getMinBlockX(), chunk.getMinBlockZ(),
                chunk.getMaxBlockX(), chunk.getMaxBlockZ())) {
            placed |= write(level, chunk, hoodoo);
        }
        return placed;
    }

    private static boolean write(WorldGenLevel level, ChunkPos chunk, Ring2RockLandmarks.Hoodoo hoodoo) {
        Ring2PureTerrain terrain = Ring2PureTerrain.of(level);
        int[] box = hoodoo.box();
        int minX = Math.max(box[0], chunk.getMinBlockX());
        int maxX = Math.min(box[2], chunk.getMaxBlockX());
        int minZ = Math.max(box[1], chunk.getMinBlockZ());
        int maxZ = Math.min(box[3], chunk.getMaxBlockZ());
        if (minX > maxX || minZ > maxZ) {
            return false;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (Ring2RockLandmarks.Pillar pillar : hoodoo.pillars()) {
            for (int y = pillar.ground() - Ring2RockLandmarks.ROOT; y <= pillar.top(); y++) {
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockState state = block(terrain, hoodoo.seed(), pillar, x, y, z);
                        if (state != null) {
                            level.setBlock(pos.set(x, y, z), state, FLAGS);
                            placed = true;
                        }
                    }
                }
            }
        }
        rubble(level, hoodoo, minX, minZ, maxX, maxZ, pos);
        return placed;
    }

    /** The block of the pillar at (x, y, z), or null outside it. */
    static BlockState block(Ring2PureTerrain terrain, long seed, Ring2RockLandmarks.Pillar pillar, int x, int y, int z) {
        if (Ring2RockLandmarks.inPillar(seed, pillar, x, y, z)) {
            boolean cap = y >= pillar.capBottom() - NECK;
            return cap ? ModBlocks.SCORCHED_SANDSTONE.get().defaultBlockState() : terrain.stratum(x, y, z);
        }
        return null;
    }

    /** Fallen rock round the feet: single blocks on bare ground, never on a pillar. */
    private static void rubble(WorldGenLevel level, Ring2RockLandmarks.Hoodoo hoodoo, int minX, int minZ, int maxX, int maxZ,
                               BlockPos.MutableBlockPos pos) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                double nearest = Double.MAX_VALUE;
                boolean onPillar = false;
                for (Ring2RockLandmarks.Pillar pillar : hoodoo.pillars()) {
                    double d = Math.hypot(x + 0.5 - pillar.x(), z + 0.5 - pillar.z()) - pillar.foot();
                    onPillar |= d < 1.5;
                    nearest = Math.min(nearest, d);
                }
                if (onPillar || nearest > RUBBLE_RING || Ring2RockLandmarks.hash(hoodoo.seed(), x, 0, z, 21) >= RUBBLE) {
                    continue;
                }
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                BlockState ground = level.getBlockState(pos.set(x, y - 1, z));
                if (!isFloor(ground) || !level.getBlockState(pos.set(x, y, z)).isAir()) {
                    continue;
                }
                double pick = Ring2RockLandmarks.hash(hoodoo.seed(), x, 1, z, 22);
                BlockState rock = pick < 0.6 ? ModBlocks.CANYON_COBBLESTONE.get().defaultBlockState()
                        : pick < 0.8 ? ModBlocks.SCORCHED_SANDSTONE.get().defaultBlockState()
                        : Ring2PureTerrain.stratumBlock(Mth.floor(pick * 37) % 3);
                level.setBlock(pos, rock, FLAGS);
            }
        }
    }

    /** The bare ground of the hoodoo floor that fallen rock may lie on. */
    private static boolean isFloor(BlockState state) {
        return state.is(Blocks.RED_SAND) || state.is(Blocks.GRAVEL) || state.is(ModBlocks.CRACKED_CLAY.get())
                || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.RED_SANDSTONE);
    }
}
