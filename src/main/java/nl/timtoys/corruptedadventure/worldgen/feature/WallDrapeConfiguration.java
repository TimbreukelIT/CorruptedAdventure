package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Settings of {@link WallDrapeFeature}.
 *
 * @param height     how far up the wall the drape climbs from the origin
 * @param vineChance the chance per block of the climb that a vine grows on the wall
 * @param rootChance the chance that an air block under an overhang gets hanging roots
 */
public record WallDrapeConfiguration(IntProvider height, float vineChance, float rootChance) implements FeatureConfiguration {
    public static final Codec<WallDrapeConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IntProvider.codec(1, 24).fieldOf("height").forGetter(WallDrapeConfiguration::height),
            Codec.floatRange(0.0F, 1.0F).fieldOf("vine_chance").forGetter(WallDrapeConfiguration::vineChance),
            Codec.floatRange(0.0F, 1.0F).fieldOf("root_chance").forGetter(WallDrapeConfiguration::rootChance)
    ).apply(instance, WallDrapeConfiguration::new));
}
