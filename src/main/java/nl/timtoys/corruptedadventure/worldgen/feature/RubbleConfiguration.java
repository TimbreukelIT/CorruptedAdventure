package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Configuration of {@code corruptedadventure:rubble}.
 *
 * @param stateProvider the material, sampled once per boulder
 * @param size          1 = one block, 2 = a 2 × 2 clump, 3 = a rounded 3 × 3 clump up to three high
 */
public record RubbleConfiguration(BlockStateProvider stateProvider, IntProvider size) implements FeatureConfiguration {
    public static final Codec<RubbleConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockStateProvider.CODEC.fieldOf("state_provider").forGetter(RubbleConfiguration::stateProvider),
            IntProvider.codec(1, RubbleShape.MAX_SIZE).fieldOf("size").forGetter(RubbleConfiguration::size)
    ).apply(instance, RubbleConfiguration::new));
}
