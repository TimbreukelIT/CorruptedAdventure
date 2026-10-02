package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.Optional;

/**
 * {@code corruptedadventure:canyon_spring}: a wall spring in the canyon floor ({@link SpringLayout}). Walks the
 * columns of the origin chunk in a random order and tries up to {@code candidates} wall feet (the terrain rises at
 * least {@code wall_height} within three blocks), each with its own random shape, and builds the first layout that
 * fits; otherwise nothing.
 * <p>
 * The water is set in vanilla's settled state and no fluid tick is scheduled (worldgen marks only blocks such as
 * mushrooms for post-processing, never water, so nothing ticks when the chunk becomes full either). It cannot reach a
 * neighbouring chunk: everything lies inside this chunk and the trench and pool are closed. After a player changes a
 * block next to it, vanilla simply works out the water again on the server.
 * <p>
 * Runs last among the canyon features of its step: it only digs where the floor is still bare floor with air above,
 * so no boulder of the same chunk ends up over the trench or pool.
 */
public final class CanyonSpringFeature extends Feature<CanyonSpringConfiguration> {
    /** The 14 × 14 columns one block inside the chunk. */
    private static final int COLUMNS = 14 * 14;
    /** Steps through the columns that visit each once (coprime with 196 = 2² · 7²). */
    private static final int[] STRIDES = {1, 3, 5, 9, 11, 13, 15, 17, 19, 23, 25, 27, 29, 31, 33, 37, 39, 41, 43, 45, 47};

    public CanyonSpringFeature(Codec<CanyonSpringConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<CanyonSpringConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        CanyonSpringConfiguration config = context.config();
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        SpringLayout.Terrain terrain = terrain(level, config);
        // All columns one block inside the chunk, in a random order; only wall feet cost an attempt.
        int start = random.nextInt(COLUMNS);
        int stride = STRIDES[random.nextInt(STRIDES.length)];
        int attempts = 0;
        for (int i = 0; i < COLUMNS && attempts < config.candidates(); i++) {
            int index = (start + i * stride) % COLUMNS;
            int x = minX + 1 + index % 14;
            int z = minZ + 1 + index / 14;
            int turn = random.nextInt(4);
            for (int d = 0; d < 4 && attempts < config.candidates(); d++) {
                Direction away = Direction.from2DDataValue(turn + d);
                if (!SpringLayout.isWallFoot(terrain, x, z, away, config.wallHeight())) {
                    continue;
                }
                attempts++;
                Optional<SpringLayout.Layout> layout = SpringLayout.build(terrain, minX, minZ, x, z, away, params(config, random),
                        config.wallHeight(), config.minWaterY(), config.maxFloorY());
                if (layout.isPresent()) {
                    place(level, random, config, layout.get());
                    return true;
                }
            }
        }
        return false;
    }

    static SpringLayout.Params params(CanyonSpringConfiguration config, RandomSource random) {
        int springHeight = config.springHeight().sample(random);
        int length = config.streamLength().sample(random);
        int bendAt = length >= 3 && random.nextBoolean() ? 1 + random.nextInt(length - 2) : 0;
        boolean bendLeft = random.nextBoolean();
        int width = config.poolSize().sample(random);
        int poolLength = config.poolSize().sample(random);
        int shift = random.nextInt(width);
        int depth = config.poolDepth().sample(random);
        return new SpringLayout.Params(springHeight, length, bendAt, bendLeft, width, poolLength, shift, depth);
    }

    private static SpringLayout.Terrain terrain(WorldGenLevel level, CanyonSpringConfiguration config) {
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        return new SpringLayout.Terrain() {
            @Override
            public int surface(int x, int z) {
                return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
            }

            @Override
            public boolean solid(int x, int y, int z) {
                BlockState state = level.getBlockState(probe.set(x, y, z));
                return state.getFluidState().isEmpty() && state.blocksMotion() && state.isCollisionShapeFullBlock(level, probe);
            }

            @Override
            public boolean floor(int x, int y, int z) {
                return level.getBlockState(probe.set(x, y, z)).is(config.floor());
            }

            @Override
            public boolean air(int x, int y, int z) {
                return level.getBlockState(probe.set(x, y, z)).isAir();
            }

            @Override
            public boolean gravity(int x, int y, int z) {
                return level.getBlockState(probe.set(x, y, z)).getBlock() instanceof FallingBlock;
            }
        };
    }

    private static void place(WorldGenLevel level, RandomSource random, CanyonSpringConfiguration config, SpringLayout.Layout layout) {
        for (BlockPos pos : layout.carve()) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        for (SpringLayout.Water water : layout.water()) {
            level.setBlock(water.pos(), Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, water.blockLevel()), Block.UPDATE_CLIENTS);
        }
        for (BlockPos pos : layout.rim()) {
            level.setBlock(pos, config.rim().getState(random, pos), Block.UPDATE_CLIENTS);
        }
        for (BlockPos spot : layout.plantSpots()) {
            if (random.nextFloat() >= config.plantChance()) {
                continue;
            }
            if (layout.besideWater().contains(spot) && random.nextFloat() < config.sugarCane()) {
                int height = 1 + random.nextInt(3);
                BlockState cane = Blocks.SUGAR_CANE.defaultBlockState();
                for (int h = 0; h < height && level.isEmptyBlock(spot.above(h)) && cane.canSurvive(level, spot.above(h)); h++) {
                    level.setBlock(spot.above(h), cane, Block.UPDATE_CLIENTS);
                }
                continue;
            }
            BlockState plant = config.rimPlants().getState(random, spot);
            if (!level.isEmptyBlock(spot) || !plant.canSurvive(level, spot)) {
                continue;
            }
            if (plant.getBlock() instanceof DoublePlantBlock) {
                if (level.isEmptyBlock(spot.above())) {
                    DoublePlantBlock.placeAt(level, plant, spot, Block.UPDATE_CLIENTS);
                }
            } else {
                level.setBlock(spot, plant, Block.UPDATE_CLIENTS);
            }
        }
    }
}
