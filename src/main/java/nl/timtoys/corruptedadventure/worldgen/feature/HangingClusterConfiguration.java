package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Settings of {@link HangingClusterFeature}.
 *
 * @param state     the hanging blocks (a crystal cluster or bud; turned to face down)
 * @param radius    the columns around the origin that may get one (a square, inside the chunk)
 * @param chance    the share of those columns that get one
 * @param maxDepth  how far below the top the underside may lie
 * @param minDrop   how much open air there must be below the underside
 */
public record HangingClusterConfiguration(BlockStateProvider state, int radius, float chance, int maxDepth, int minDrop)
        implements FeatureConfiguration {
    public static final Codec<HangingClusterConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockStateProvider.CODEC.fieldOf("state").forGetter(HangingClusterConfiguration::state),
            Codec.intRange(0, HangingClusterFeature.MAX_RADIUS).fieldOf("radius").forGetter(HangingClusterConfiguration::radius),
            Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(HangingClusterConfiguration::chance),
            Codec.intRange(1, 64).fieldOf("max_depth").forGetter(HangingClusterConfiguration::maxDepth),
            Codec.intRange(1, 32).fieldOf("min_drop").forGetter(HangingClusterConfiguration::minDrop)
    ).apply(instance, HangingClusterConfiguration::new));
}
