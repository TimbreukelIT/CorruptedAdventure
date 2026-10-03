package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.Optional;

/**
 * Configuration of {@code corruptedadventure:surface_pool} ({@link PoolLayout}).
 *
 * @param radius               radius of the water before the ragged edge, blocks
 * @param edgeJitter           how deep each of the 8 notches around the edge may cut in, as a fraction of the radius
 * @param depth                1 or 2; at 2 only the core (one column in from the edge) is 2 deep
 * @param ground               the top block of every column of the pool and its rim must be one of these
 * @param maxStep              how far a column may rise above the water level (it is dug down to it)
 * @param rim                  replaces the top block of a ring around the water
 * @param rimPlants            plants on the rim
 * @param shoreColumn          a column (sugar cane) on rim blocks next to the water
 * @param rimDecoration        on the rim, e.g. a salt crystal
 * @param underwaterDecoration in the water on the floor, e.g. a waterlogged salt crystal
 * @param trees                trees on the outer rim
 */
public record SurfacePoolConfiguration(FloatProvider radius, float edgeJitter, IntProvider depth, HolderSet<Block> ground,
                                       int maxStep, Rim rim, Optional<Scatter> rimPlants, Optional<Column> shoreColumn,
                                       Optional<Scatter> rimDecoration, Optional<Scatter> underwaterDecoration,
                                       Optional<Trees> trees) implements FeatureConfiguration {
    public static final Codec<SurfacePoolConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FloatProvider.codec(1.0F, PoolLayout.MAX_RADIUS).fieldOf("radius").forGetter(SurfacePoolConfiguration::radius),
            Codec.floatRange(0.0F, 0.6F).fieldOf("edge_jitter").forGetter(SurfacePoolConfiguration::edgeJitter),
            IntProvider.codec(1, 2).fieldOf("depth").forGetter(SurfacePoolConfiguration::depth),
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("ground").forGetter(SurfacePoolConfiguration::ground),
            Codec.intRange(0, 3).fieldOf("max_step").forGetter(SurfacePoolConfiguration::maxStep),
            Rim.CODEC.fieldOf("rim").forGetter(SurfacePoolConfiguration::rim),
            Scatter.CODEC.optionalFieldOf("rim_plants").forGetter(SurfacePoolConfiguration::rimPlants),
            Column.CODEC.optionalFieldOf("shore_column").forGetter(SurfacePoolConfiguration::shoreColumn),
            Scatter.CODEC.optionalFieldOf("rim_decoration").forGetter(SurfacePoolConfiguration::rimDecoration),
            Scatter.CODEC.optionalFieldOf("underwater_decoration").forGetter(SurfacePoolConfiguration::underwaterDecoration),
            Trees.CODEC.optionalFieldOf("trees").forGetter(SurfacePoolConfiguration::trees)
    ).apply(instance, SurfacePoolConfiguration::new));

    /** The ring around the water: {@code width} columns (1–2) whose top block becomes {@code state}. */
    public record Rim(BlockStateProvider state, int width) {
        public static final Codec<Rim> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockStateProvider.CODEC.fieldOf("state").forGetter(Rim::state),
                Codec.intRange(1, 2).fieldOf("width").forGetter(Rim::width)
        ).apply(instance, Rim::new));
    }

    /** One block per spot with {@code chance}, if it can survive there. */
    public record Scatter(BlockStateProvider state, float chance) {
        public static final Codec<Scatter> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockStateProvider.CODEC.fieldOf("state").forGetter(Scatter::state),
                Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(Scatter::chance)
        ).apply(instance, Scatter::new));
    }

    /** A column of {@code height} blocks per spot with {@code chance}, as far as each block can survive. */
    public record Column(BlockStateProvider state, IntProvider height, float chance) {
        public static final Codec<Column> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockStateProvider.CODEC.fieldOf("state").forGetter(Column::state),
                IntProvider.codec(1, 4).fieldOf("height").forGetter(Column::height),
                Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(Column::chance)
        ).apply(instance, Column::new));
    }

    /**
     * {@code count} trees picked by weight from {@code features}, on rim spots where {@code spot} holds (checked at the
     * block above the rim), trunks at least {@link PoolLayout#TREE_EDGE} from the chunk edge and
     * {@link PoolLayout#TREE_SPACING} apart.
     */
    public record Trees(SimpleWeightedRandomList<Holder<ConfiguredFeature<?, ?>>> features, IntProvider count, BlockPredicate spot) {
        public static final Codec<Trees> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                SimpleWeightedRandomList.wrappedCodec(ConfiguredFeature.CODEC).fieldOf("features").forGetter(Trees::features),
                IntProvider.codec(0, 2).fieldOf("count").forGetter(Trees::count),
                BlockPredicate.CODEC.fieldOf("spot").forGetter(Trees::spot)
        ).apply(instance, Trees::new));
    }
}
