package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.List;
import java.util.Optional;

/**
 * {@code corruptedadventure:outcrop}: a rock of tapering knobs ({@link OutcropShape}): the salt chimneys of the salt
 * flats and the rock islands of the dune sea (change 30b). Reads and writes only its own chunk, at least one block
 * from its edge.
 * <p>
 * Pools come earlier in the same step and are not in the {@code WORLD_SURFACE_WG} heightmap, so the ground of each
 * column is found from the heightmap down through air and plants; a column whose ground is water or a pool's grass
 * rim makes the rock fail (all or nothing).
 */
public final class OutcropFeature extends Feature<OutcropConfiguration> {
    /** How far below the heightmap the ground may lie (a pool dug its columns down by at most 3). */
    private static final int MAX_DESCENT = 4;

    public OutcropFeature(Codec<OutcropConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<OutcropConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        OutcropConfiguration config = context.config();
        int knobs = config.knobs().sample(random);
        float[] radius = new float[knobs];
        for (int i = 0; i < knobs; i++) {
            radius[i] = config.radius().sample(random);
        }
        OutcropShape.Params params = new OutcropShape.Params(knobs, config.spread(), radius, config.height().sample(random),
                config.taper(), config.sink(), random.nextFloat() < config.overhangChance());
        BlockPos origin = context.origin();
        Optional<OutcropShape.Rock> found = OutcropShape.build(terrain(level, config.sink()), origin.getX() & ~15, origin.getZ() & ~15,
                origin.getX(), origin.getZ(), params, random);
        Optional<List<BlockState>> palette = config.palettes().getRandomValue(random);
        if (found.isEmpty() || palette.isEmpty()) {
            return false;
        }
        OutcropShape.Rock rock = found.get();
        int[] bands = bands(palette.get().size(), random);
        int lowest = rock.blocks().stream().mapToInt(BlockPos::getY).min().orElse(0);
        for (BlockPos pos : rock.blocks()) {
            level.setBlock(pos, palette.get().get(band(bands, pos.getY() - lowest)), Block.UPDATE_CLIENTS);
        }
        config.topDecoration().ifPresent(decoration -> {
            for (BlockPos spot : rock.tops()) {
                BlockState state = decoration.state().getState(random, spot);
                if (random.nextFloat() < decoration.chance() && state.canSurvive(level, spot)) {
                    level.setBlock(spot, state, Block.UPDATE_CLIENTS);
                }
            }
        });
        return true;
    }

    /**
     * The palette index per layer from the rock's lowest block up: bands of 2–3 blocks, starting at a random entry and
     * stepping through the palette. One block-long palettes give one band.
     */
    static int[] bands(int size, RandomSource random) {
        int[] layers = new int[OutcropShape.MAX_LAYERS];
        int index = random.nextInt(size);
        int y = 0;
        while (y < layers.length) {
            int thickness = 2 + random.nextInt(2);
            for (int k = 0; k < thickness && y < layers.length; k++, y++) {
                layers[y] = index;
            }
            index = (index + 1) % size;
        }
        return layers;
    }

    static int band(int[] bands, int layer) {
        return bands[Math.min(Math.max(layer, 0), bands.length - 1)];
    }

    private static OutcropShape.Terrain terrain(WorldGenLevel level, int sink) {
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        return new OutcropShape.Terrain() {
            @Override
            public int ground(int x, int z) {
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                int y = top;
                while (y > top - MAX_DESCENT && free(x, y, z)) {
                    y--;
                }
                BlockState ground = level.getBlockState(probe.set(x, y, z));
                if (!ground.getFluidState().isEmpty() || ground.is(Blocks.GRASS_BLOCK)) {
                    return Integer.MIN_VALUE;
                }
                // Solid down to the deepest buried block and one below: never over a carver's cave.
                for (int k = 0; k <= sink; k++) {
                    if (!solid(x, y - k, z)) {
                        return Integer.MIN_VALUE;
                    }
                }
                return y;
            }

            @Override
            public boolean free(int x, int y, int z) {
                BlockState state = level.getBlockState(probe.set(x, y, z));
                return state.isAir() || state.canBeReplaced() && state.getFluidState().isEmpty();
            }

            private boolean solid(int x, int y, int z) {
                probe.set(x, y, z);
                BlockState state = level.getBlockState(probe);
                return state.getFluidState().isEmpty() && !state.is(BlockTags.FEATURES_CANNOT_REPLACE)
                        && state.isFaceSturdy(level, probe, Direction.UP);
            }
        };
    }
}
