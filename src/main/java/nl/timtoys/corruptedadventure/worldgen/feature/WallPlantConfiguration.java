package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Settings of {@link WallPlantFeature}.
 *
 * @param plants the wall plants (blocks with a horizontal {@code facing} away from the wall)
 * @param chance per open block of the column beside a wall: the chance of a plant
 * @param height how far up the column the feature looks
 */
public record WallPlantConfiguration(BlockStateProvider plants, float chance, int height) implements FeatureConfiguration {
    public static final Codec<WallPlantConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockStateProvider.CODEC.fieldOf("plants").forGetter(WallPlantConfiguration::plants),
            Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(WallPlantConfiguration::chance),
            Codec.intRange(1, 64).fieldOf("height").forGetter(WallPlantConfiguration::height)
    ).apply(instance, WallPlantConfiguration::new));
}
