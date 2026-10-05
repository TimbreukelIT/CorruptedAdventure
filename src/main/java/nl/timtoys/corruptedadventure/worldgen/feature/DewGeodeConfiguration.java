package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * Configuration of {@code corruptedadventure:dew_geode}.
 *
 * @param shape        the geometry, identical to the settings of the density function {@code dew_geodes} (a test compares them)
 * @param regionNoise  the region noise of the density function's {@code region}, sampled at (x, 0, z)
 * @param cellNoise    the seeded cell hash of the density function's {@code noise}
 * @param sourceChance chance that a block of the cavity wall becomes a dew source
 * @param budChance    chance per free side of a dew source of a pre-grown dew crystal bud
 * @param moldChance   chance per free side of a dew source (without a bud) of a patch of glow mold next to it
 */
public record DewGeodeConfiguration(DewGeodeShape.Settings shape, ResourceKey<NormalNoise.NoiseParameters> regionNoise,
                                    ResourceKey<NormalNoise.NoiseParameters> cellNoise, double sourceChance, double budChance,
                                    double moldChance) implements FeatureConfiguration {
    public static final Codec<DewGeodeConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DewGeodeShape.Settings.CODEC.fieldOf("shape").forGetter(DewGeodeConfiguration::shape),
            ResourceKey.codec(Registries.NOISE).fieldOf("region_noise").forGetter(DewGeodeConfiguration::regionNoise),
            ResourceKey.codec(Registries.NOISE).fieldOf("cell_noise").forGetter(DewGeodeConfiguration::cellNoise),
            Codec.doubleRange(0.0, 1.0).fieldOf("source_chance").forGetter(DewGeodeConfiguration::sourceChance),
            Codec.doubleRange(0.0, 1.0).fieldOf("bud_chance").forGetter(DewGeodeConfiguration::budChance),
            Codec.doubleRange(0.0, 1.0).fieldOf("mold_chance").forGetter(DewGeodeConfiguration::moldChance)
    ).apply(instance, DewGeodeConfiguration::new));
}
