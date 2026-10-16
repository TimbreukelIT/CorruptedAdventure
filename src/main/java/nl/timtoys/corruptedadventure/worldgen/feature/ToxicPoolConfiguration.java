package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Settings of {@link ToxicPoolFeature}.
 *
 * @param fluid  the pool's source block
 * @param radius the pool's radius (blocks, at most {@link ToxicPoolFeature#MAX_RADIUS})
 * @param depth  how deep the middle is (the rim is one deep)
 */
public record ToxicPoolConfiguration(BlockState fluid, IntProvider radius, IntProvider depth) implements FeatureConfiguration {
    public static final Codec<ToxicPoolConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("fluid").forGetter(ToxicPoolConfiguration::fluid),
            IntProvider.codec(1, ToxicPoolFeature.MAX_RADIUS).fieldOf("radius").forGetter(ToxicPoolConfiguration::radius),
            IntProvider.codec(1, 3).fieldOf("depth").forGetter(ToxicPoolConfiguration::depth)
    ).apply(instance, ToxicPoolConfiguration::new));
}
