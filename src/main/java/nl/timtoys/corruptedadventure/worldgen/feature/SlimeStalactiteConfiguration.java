package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.Codec;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Configuration of {@code corruptedadventure:slime_stalactite}.
 *
 * @param stalactite the stalactite block (thickness and direction are set per block)
 * @param length     number of blocks, 1 to {@link #MAX_LENGTH}
 */
public record SlimeStalactiteConfiguration(BlockState stalactite, IntProvider length) implements FeatureConfiguration {
    public static final int MAX_LENGTH = 8;

    public static final Codec<SlimeStalactiteConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("stalactite").forGetter(SlimeStalactiteConfiguration::stalactite),
            IntProvider.codec(1, MAX_LENGTH).fieldOf("length").forGetter(SlimeStalactiteConfiguration::length)
    ).apply(instance, SlimeStalactiteConfiguration::new));
}
