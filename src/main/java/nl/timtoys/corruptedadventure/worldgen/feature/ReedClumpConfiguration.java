package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Settings of {@link ReedClumpFeature}.
 *
 * @param radius    the clump's radius around the origin (a disc)
 * @param density   per column of the disc: the chance of a stem
 * @param minHeight the lowest stem (blocks)
 * @param maxHeight the highest stem
 */
public record ReedClumpConfiguration(int radius, float density, int minHeight, int maxHeight) implements FeatureConfiguration {
    public static final Codec<ReedClumpConfiguration> CODEC = RecordCodecBuilder.<ReedClumpConfiguration>create(instance -> instance.group(
            Codec.intRange(0, 7).fieldOf("radius").forGetter(ReedClumpConfiguration::radius),
            Codec.floatRange(0.0F, 1.0F).fieldOf("density").forGetter(ReedClumpConfiguration::density),
            Codec.intRange(1, 3).fieldOf("min_height").forGetter(ReedClumpConfiguration::minHeight),
            Codec.intRange(1, 3).fieldOf("max_height").forGetter(ReedClumpConfiguration::maxHeight)
    ).apply(instance, ReedClumpConfiguration::new)).validate(ReedClumpConfiguration::check);

    private static DataResult<ReedClumpConfiguration> check(ReedClumpConfiguration config) {
        return config.minHeight <= config.maxHeight ? DataResult.success(config)
                : DataResult.error(() -> "reed_clump needs min_height <= max_height");
    }
}
