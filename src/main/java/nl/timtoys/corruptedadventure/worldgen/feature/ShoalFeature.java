package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.material.Fluids;

/**
 * {@code corruptedadventure:shoal} (ring 4 decor rollout group A, 2026-10-19): raises the floor of shallow sea water
 * into banks, atolls and islets where a noise is high, once per chunk over its own columns. White sand banks just
 * awash and small atolls in {@code crystal_lagoon}, grassy islets with shrubs, flowers and a collar of reeds in
 * {@code lotus_shallows}.
 * <p>
 * Every column is a pure function of the seed (the density's noise, its own random from its position) and the frozen
 * worldgen heightmaps; a column's neighbours (the water's edge) are asked the same function, never their blocks, so the
 * result does not depend on which chunk decorates first. Only sea-level water ({@code WORLD_SURFACE_WG} at the sea level)
 * is raised, never a lake, and the shoal only ever replaces water sources and air: no water is left against air below
 * the sea's top. The biome margin keeps it away from the trees of other biomes, whose crowns reach in from the
 * neighbouring chunks (and see a raised floor as terrain).
 */
public final class ShoalFeature extends Feature<ShoalConfiguration> {
    private static final long SALT = 0x5B0A_15L;
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    public ShoalFeature(Codec<ShoalConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<ShoalConfiguration> context) {
        WorldGenLevel level = context.level();
        ShoalConfiguration config = context.config();
        NormalNoise noise = level.getLevel().getChunkSource().randomState().getOrCreateNoise(config.shape().noise());
        Plan plan = new Plan(level, config, noise);
        ChunkPos chunk = new ChunkPos(context.origin());
        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
            for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
                int top = plan.top(x, z);
                if (top == Plan.NONE) {
                    continue;
                }
                int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                if (!open(level, pos, x, floor, top, z)) {
                    continue;
                }
                boolean land = top >= plan.sea - 1;
                for (int y = floor; y <= top; y++) {
                    pos.set(x, y, z);
                    BlockState state = y < top ? config.fill().getState(plan.random(x, y, z), pos)
                            : (land ? config.landTop() : config.bed()).getState(plan.random(x, y, z), pos);
                    level.setBlock(pos, state, FLAGS);
                }
                placed = true;
                decorate(level, config, plan, x, top, z, land);
            }
        }
        return placed;
    }

    /** Whether the column holds only water sources and air from its floor to the planned top (nothing hangs into it). */
    private static boolean open(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int floor, int top, int z) {
        for (int y = floor; y <= top + 1; y++) {
            BlockState state = level.getBlockState(pos.set(x, y, z));
            if (!state.isAir() && !(state.is(Blocks.WATER) && state.getFluidState().isSource())) {
                return false;
            }
        }
        return true;
    }

    /** The plants of one column, from its own random: on land plants, shrubs and reeds at the edge; under water bed plants. */
    private static void decorate(WorldGenLevel level, ShoalConfiguration config, Plan plan, int x, int top, int z, boolean land) {
        RandomSource random = plan.random(x, top + 1, z);
        float shrub = random.nextFloat();
        float plant = random.nextFloat();
        float reed = random.nextFloat();
        int shrubHeight = 1 + random.nextInt(2);
        int reedHeight = 2 + random.nextInt(2);
        BlockPos above = new BlockPos(x, top + 1, z);
        if (!land) {
            if (plant < config.bedChance() && level.getFluidState(above.above()).is(Fluids.WATER)) {
                BlockState state = config.bedPlants().getState(random, above);
                if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
                    state = state.setValue(BlockStateProperties.WATERLOGGED, true);
                }
                if (level.getFluidState(above).isSourceOfType(Fluids.WATER) && state.canSurvive(level, above)) {
                    level.setBlock(above, state, FLAGS);
                }
            }
            return;
        }
        if (!level.getBlockState(above).isAir()) {
            return;
        }
        if (reed < config.reedChance() && plan.atEdge(x, top, z)) {
            ReedClumpFeature.stem(level, new BlockPos.MutableBlockPos(x, top + 1, z), reedHeight);
        } else if (shrub < config.shrubChance()) {
            BlockState leaves = config.shrubs().getState(random, above);
            for (int i = 0; i < shrubHeight && level.getBlockState(above.above(i)).isAir(); i++) {
                level.setBlock(above.above(i), leaves, FLAGS);
            }
        } else if (plant < config.plantChance()) {
            BlockState state = config.landPlants().getState(random, above);
            if (!state.canSurvive(level, above)) {
                return;
            }
            if (state.getBlock() instanceof DoublePlantBlock) {
                if (level.isEmptyBlock(above.above())) {
                    DoublePlantBlock.placeAt(level, state, above, FLAGS);
                }
            } else {
                level.setBlock(above, state, FLAGS);
            }
        }
    }

    /** The pure per-column part: which columns rise and to what height. */
    static final class Plan {
        static final int NONE = Integer.MIN_VALUE;

        private final WorldGenLevel level;
        private final ShoalConfiguration config;
        private final NormalNoise noise;
        private final long seed;
        final int sea;

        Plan(WorldGenLevel level, ShoalConfiguration config, NormalNoise noise) {
            this.level = level;
            this.config = config;
            this.noise = noise;
            this.seed = level.getSeed();
            this.sea = level.getSeaLevel();
        }

        /** The column's new top block, or {@link #NONE} if it stays as it is. */
        int top(int x, int z) {
            int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
            int depth = sea - floor;
            if (surface != sea || depth < 1 || depth > config.maxDepth()) {
                return NONE;
            }
            double strength = config.shape().share(noise, x, z);
            if (strength <= 0.0) {
                return NONE;
            }
            int top = sea - 1 - config.sink() + (int) Math.round(strength * (config.sink() + config.rise()));
            if (top < floor || !inBiome(x, z)) {
                return NONE;
            }
            return top;
        }

        private boolean inBiome(int x, int z) {
            return Landmarks.storedBiomeAround(level, x, z, config.biomeMargin(), biome -> biome.is(config.biome()));
        }

        /** Whether a land top at {@code top} has open water beside it (the neighbours' planned tops, not their blocks). */
        boolean atEdge(int x, int top, int z) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int nx = x + direction.getStepX();
                int nz = z + direction.getStepZ();
                if (level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, nx, nz) != sea
                        || level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, nx, nz) > Math.min(top, sea - 1)) {
                    continue;
                }
                int theirs = top(nx, nz);
                if (theirs == NONE || theirs < Math.min(top, sea - 1)) {
                    return true;
                }
            }
            return false;
        }

        /** The column's own random at height y: the same in every world of the seed, whatever came before. */
        RandomSource random(int x, int y, int z) {
            return GroundObjects.random(seed, new BlockPos(x, y, z), SALT);
        }
    }
}
