package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Settings of {@link SinkholeGasFeature}.
 *
 * @param gas       the gas block
 * @param minDepth  a sinkhole column gets gas only where its floor lies this far below the surface
 * @param deepDepth from this depth on, two layers instead of one
 */
public record SinkholeGasConfiguration(BlockState gas, int minDepth, int deepDepth) implements FeatureConfiguration {
    public static final Codec<SinkholeGasConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("gas").forGetter(SinkholeGasConfiguration::gas),
            Codec.intRange(1, 32).fieldOf("min_depth").forGetter(SinkholeGasConfiguration::minDepth),
            Codec.intRange(1, 32).fieldOf("deep_depth").forGetter(SinkholeGasConfiguration::deepDepth)
    ).apply(instance, SinkholeGasConfiguration::new));
}
