package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Settings of {@link SplitRockFeature} (ring 4 decor D).
 *
 * @param body      the rock, per block
 * @param crack     the faces of the split, per block
 * @param foot      the lowest {@link SplitRockFeature#FOOT} blocks above the ground, per block
 * @param height    the taller half's height above the floe
 * @param shorter   the other half's height as a share of the taller one's
 * @param length    the rock's half length along the split (blocks; cut to fit the floe)
 * @param lean      how far each half leans out at its top (blocks)
 */
public record SplitRockConfiguration(BlockStateProvider body, BlockStateProvider crack, BlockStateProvider foot, IntProvider height,
                                     FloatProvider shorter, FloatProvider length, FloatProvider lean)
        implements FeatureConfiguration {
    public static final Codec<SplitRockConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockStateProvider.CODEC.fieldOf("body").forGetter(SplitRockConfiguration::body),
            BlockStateProvider.CODEC.fieldOf("crack").forGetter(SplitRockConfiguration::crack),
            BlockStateProvider.CODEC.fieldOf("foot").forGetter(SplitRockConfiguration::foot),
            IntProvider.codec(8, SplitRockFeature.MAX_HEIGHT).fieldOf("height").forGetter(SplitRockConfiguration::height),
            FloatProvider.codec(0.5F, 1.0F).fieldOf("shorter").forGetter(SplitRockConfiguration::shorter),
            FloatProvider.codec(1.5F, SplitRockFeature.MAX_LENGTH).fieldOf("length").forGetter(SplitRockConfiguration::length),
            FloatProvider.codec(0.0F, SplitRockFeature.MAX_LEAN).fieldOf("lean").forGetter(SplitRockConfiguration::lean)
    ).apply(instance, SplitRockConfiguration::new));
}
