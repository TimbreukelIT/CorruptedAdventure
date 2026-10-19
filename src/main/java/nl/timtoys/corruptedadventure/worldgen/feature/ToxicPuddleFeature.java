package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * {@code corruptedadventure:toxic_puddle} (ring 4 decor B, 2026-10-19): a small puddle of toxic sludge, one block deep
 * in level dry ground, in a rim of {@code rim} soil (mud) with dead plants round it. The puddle grows from its centre
 * to {@code size} columns ({@link SludgePuddle}: closed, in one chunk, no water near); the rim and the plants only
 * follow when the puddle is placed. A ground object ({@link GroundObjects}): its own random, the frozen heightmaps, its
 * own chunk; the plants only fill air.
 */
public final class ToxicPuddleFeature extends Feature<ToxicPuddleFeature.Config> {
    /** The farthest a rim block lies from the puddle's centre. */
    static final int REACH = 4;
    private static final long SALT = 0x70_C1C_9DL;

    /**
     * @param sludge      the puddle's source block
     * @param size        how many columns of sludge (1–7)
     * @param rim         the soil round the puddle
     * @param plants      the plants on the rim
     * @param plantChance per rim column, the chance of a plant
     */
    public record Config(BlockState sludge, IntProvider size, BlockStateProvider rim, BlockStateProvider plants, float plantChance)
            implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockState.CODEC.fieldOf("sludge").forGetter(Config::sludge),
                IntProvider.codec(1, 7).fieldOf("size").forGetter(Config::size),
                BlockStateProvider.CODEC.fieldOf("rim").forGetter(Config::rim),
                BlockStateProvider.CODEC.fieldOf("plants").forGetter(Config::plants),
                Codec.floatRange(0.0F, 1.0F).fieldOf("plant_chance").forGetter(Config::plantChance)
        ).apply(instance, Config::new));
    }

    public ToxicPuddleFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        Config config = context.config();
        RandomSource random = GroundObjects.random(level.getSeed(), context.origin(), SALT);
        BlockPos centre = GroundObjects.fit(context.origin(), REACH);
        Set<BlockPos> pool = shape(centre, config.size().sample(random), random);
        // Draw the rim and the plants for every column of the square first: the draws never depend on the ground.
        List<BlockPos> rim = new ArrayList<>();
        List<BlockState> soils = new ArrayList<>();
        List<BlockState> plants = new ArrayList<>();
        for (int dz = -REACH; dz <= REACH; dz++) {
            for (int dx = -REACH; dx <= REACH; dx++) {
                float wide = random.nextFloat();
                float plant = random.nextFloat();
                BlockPos column = centre.offset(dx, 0, dz);
                BlockState soil = config.rim().getState(random, column);
                BlockState state = config.plants().getState(random, column);
                double d = distance(pool, column);
                if (d == 0.0 || d > 2.3 || d > 1.5 && wide < 0.5F) {
                    continue;
                }
                rim.add(column);
                soils.add(soil);
                plants.add(plant < config.plantChance() ? state : null);
            }
        }
        if (!SludgePuddle.place(level, config.sludge(), pool)) {
            return false;
        }
        int top = GroundObjects.surface(level, centre.getX(), centre.getZ()) - 1;
        for (int i = 0; i < rim.size(); i++) {
            BlockPos column = rim.get(i);
            if (!GroundObjects.dry(level, column.getX(), column.getZ()) || GroundObjects.surface(level, column.getX(), column.getZ()) - 1 != top) {
                continue;
            }
            BlockPos ground = new BlockPos(column.getX(), top, column.getZ());
            if (SludgePuddle.soil(level.getBlockState(ground))) {
                level.setBlock(ground, soils.get(i), Block.UPDATE_CLIENTS);
            }
            if (plants.get(i) != null) {
                GroundObjects.placeDecor(level, ground.above(), plants.get(i));
            }
        }
        return true;
    }

    /** The puddle's columns (x, 0, z): the centre, then {@code size − 1} grown onto a neighbour of the ones so far. */
    static Set<BlockPos> shape(BlockPos centre, int size, RandomSource random) {
        Set<BlockPos> pool = new LinkedHashSet<>();
        BlockPos start = new BlockPos(centre.getX(), 0, centre.getZ());
        pool.add(start);
        List<BlockPos> order = new ArrayList<>(List.of(start));
        for (int i = 1; i < size; i++) {
            BlockPos from = order.get(random.nextInt(order.size()));
            BlockPos next = from.relative(Direction.from2DDataValue(random.nextInt(4)));
            if (Math.abs(next.getX() - start.getX()) <= 1 && Math.abs(next.getZ() - start.getZ()) <= 1 && pool.add(next)) {
                order.add(next);
            }
        }
        return pool;
    }

    /** The distance from {@code column} to the nearest pool column (0 inside). */
    private static double distance(Set<BlockPos> pool, BlockPos column) {
        double best = Double.MAX_VALUE;
        for (BlockPos p : pool) {
            best = Math.min(best, Math.sqrt(p.distSqr(column)));
        }
        return best;
    }
}
