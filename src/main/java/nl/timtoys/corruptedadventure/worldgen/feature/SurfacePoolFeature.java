package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.Optional;

/**
 * {@code corruptedadventure:surface_pool}: a small pool of still water in a closed basin with a rim ({@link PoolLayout}),
 * the brine pools of the salt flats and the hidden oases of the dune sea (change 30b). Reads the
 * {@code WORLD_SURFACE_WG} heightmap and blocks of its own chunk only; everything but tree crowns lies at least one
 * block from the chunk edge.
 * <p>
 * The water is source blocks in their settled state (no fluid tick scheduled; worldgen never post-processes water, see
 * {@link CanyonSpringFeature}). A pool comes first among its biome's features of the step: later features (pillars,
 * rocks) see the old terrain in the heightmap and check the blocks themselves.
 */
public final class SurfacePoolFeature extends Feature<SurfacePoolConfiguration> {
    public SurfacePoolFeature(Codec<SurfacePoolConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<SurfacePoolConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        SurfacePoolConfiguration config = context.config();
        BlockPos origin = context.origin();
        float[] edge = new float[PoolLayout.EDGE_POINTS];
        float radius = config.radius().sample(random);
        for (int i = 0; i < edge.length; i++) {
            edge[i] = 1.0F - config.edgeJitter() * random.nextFloat();
        }
        PoolLayout.Params params = new PoolLayout.Params(radius, edge, config.depth().sample(random), config.rim().width(), config.maxStep());
        Optional<PoolLayout.Layout> found = PoolLayout.build(terrain(level, config), origin.getX() & ~15, origin.getZ() & ~15,
                origin.getX(), origin.getZ(), params);
        if (found.isEmpty()) {
            return false;
        }
        PoolLayout.Layout layout = found.get();
        for (BlockPos pos : layout.carve()) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        for (BlockPos pos : layout.water()) {
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        for (BlockPos pos : layout.rim()) {
            level.setBlock(pos, config.rim().state().getState(random, pos), Block.UPDATE_CLIENTS);
        }
        config.trees().ifPresent(trees -> placeTrees(context, trees, layout));
        config.shoreColumn().ifPresent(column -> {
            for (BlockPos ground : layout.besideWater()) {
                if (random.nextFloat() < column.chance()) {
                    placeColumn(level, random, column, ground.above());
                }
            }
        });
        config.rimPlants().ifPresent(plants -> {
            for (BlockPos ground : layout.rim()) {
                if (random.nextFloat() < plants.chance()) {
                    placePlant(level, plants.state().getState(random, ground.above()), ground.above());
                }
            }
        });
        config.rimDecoration().ifPresent(decoration -> {
            for (BlockPos ground : layout.rim()) {
                if (random.nextFloat() < decoration.chance()) {
                    placePlant(level, decoration.state().getState(random, ground.above()), ground.above());
                }
            }
        });
        config.underwaterDecoration().ifPresent(decoration -> {
            for (BlockPos floor : layout.floor()) {
                BlockPos pos = floor.above();
                if (random.nextFloat() < decoration.chance()) {
                    BlockState state = decoration.state().getState(random, pos);
                    // Only waterloggable blocks, waterlogged: the basin keeps a source in every water block.
                    if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)
                            && state.canSurvive(level, pos)) {
                        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
                    }
                }
            }
        });
        return true;
    }

    private static PoolLayout.Terrain terrain(WorldGenLevel level, SurfacePoolConfiguration config) {
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        return new PoolLayout.Terrain() {
            @Override
            public int surface(int x, int z) {
                return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
            }

            @Override
            public boolean ground(int x, int y, int z) {
                return level.getBlockState(probe.set(x, y, z)).is(config.ground());
            }

            @Override
            public boolean solid(int x, int y, int z) {
                BlockState state = level.getBlockState(probe.set(x, y, z));
                return state.getFluidState().isEmpty() && state.isCollisionShapeFullBlock(level, probe);
            }
        };
    }

    /** Up to {@code count} trees on the trunk spots of {@link PoolLayout#trunks}. */
    private static void placeTrees(FeaturePlaceContext<SurfacePoolConfiguration> context, SurfacePoolConfiguration.Trees trees,
                                   PoolLayout.Layout layout) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        int count = trees.count().sample(random);
        for (BlockPos trunk : PoolLayout.trunks(layout, context.origin().getX() & ~15, context.origin().getZ() & ~15, count, random,
                spot -> trees.spot().test(level, spot))) {
            Optional<Holder<ConfiguredFeature<?, ?>>> tree = trees.features().getRandomValue(random);
            tree.ifPresent(feature -> feature.value().place(level, context.chunkGenerator(), random, trunk));
        }
    }

    private static void placeColumn(WorldGenLevel level, RandomSource random, SurfacePoolConfiguration.Column column, BlockPos start) {
        int height = column.height().sample(random);
        for (int h = 0; h < height; h++) {
            BlockPos pos = start.above(h);
            BlockState state = column.state().getState(random, pos);
            if (!level.isEmptyBlock(pos) || !state.canSurvive(level, pos)) {
                return;
            }
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
    }

    private static void placePlant(WorldGenLevel level, BlockState plant, BlockPos pos) {
        if (!level.isEmptyBlock(pos) || !plant.canSurvive(level, pos)) {
            return;
        }
        if (plant.getBlock() instanceof DoublePlantBlock) {
            if (level.isEmptyBlock(pos.above())) {
                DoublePlantBlock.placeAt(level, plant, pos, Block.UPDATE_CLIENTS);
            }
        } else {
            level.setBlock(pos, plant, Block.UPDATE_CLIENTS);
        }
    }
}
