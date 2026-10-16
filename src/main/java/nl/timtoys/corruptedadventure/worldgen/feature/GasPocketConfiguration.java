package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Settings of {@link GasPocketFeature}.
 *
 * @param gas    the gas block
 * @param width  each side of the pocket (blocks, at most {@link GasPocketFeature#MAX_WIDTH})
 * @param height the pocket's height
 * @param crust  how many blocks of ground stay above it
 */
public record GasPocketConfiguration(BlockState gas, IntProvider width, IntProvider height, IntProvider crust) implements FeatureConfiguration {
    public static final Codec<GasPocketConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("gas").forGetter(GasPocketConfiguration::gas),
            IntProvider.codec(1, GasPocketFeature.MAX_WIDTH).fieldOf("width").forGetter(GasPocketConfiguration::width),
            IntProvider.codec(1, 3).fieldOf("height").forGetter(GasPocketConfiguration::height),
            IntProvider.codec(1, 4).fieldOf("crust").forGetter(GasPocketConfiguration::crust)
    ).apply(instance, GasPocketConfiguration::new));
}
