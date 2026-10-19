package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Settings of {@link BoulderFeature}.
 *
 * @param stone       the rock, per block
 * @param mossyTop    a top block of the rock instead of {@code stone}, with {@code mossChance}
 * @param mossChance  per top block: the chance of {@code mossyTop}
 * @param carpet      a carpet on the top, with {@code carpetChance}
 * @param carpetChance per top block: the chance of a carpet on it (only in open air)
 * @param radius      the horizontal half-width of the rock (per axis, ≤ {@link BoulderFeature#MAX_RADIUS})
 * @param height      how far the rock rises above the ground at its middle
 * @param submerged   (decor A) on the floor under water instead of on dry land
 */
public record BoulderConfiguration(BlockStateProvider stone, BlockStateProvider mossyTop, float mossChance, BlockState carpet,
                                   float carpetChance, FloatProvider radius, FloatProvider height, boolean submerged)
        implements FeatureConfiguration {
    public static final Codec<BoulderConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockStateProvider.CODEC.fieldOf("stone").forGetter(BoulderConfiguration::stone),
            BlockStateProvider.CODEC.fieldOf("mossy_top").forGetter(BoulderConfiguration::mossyTop),
            Codec.floatRange(0.0F, 1.0F).fieldOf("moss_chance").forGetter(BoulderConfiguration::mossChance),
            BlockState.CODEC.fieldOf("carpet").forGetter(BoulderConfiguration::carpet),
            Codec.floatRange(0.0F, 1.0F).fieldOf("carpet_chance").forGetter(BoulderConfiguration::carpetChance),
            FloatProvider.codec(0.5F, BoulderFeature.MAX_RADIUS).fieldOf("radius").forGetter(BoulderConfiguration::radius),
            FloatProvider.codec(0.5F, 6.0F).fieldOf("height").forGetter(BoulderConfiguration::height),
            Codec.BOOL.optionalFieldOf("submerged", false).forGetter(BoulderConfiguration::submerged)
    ).apply(instance, BoulderConfiguration::new));

    /** A rock on dry land (the decor proef's boulders). */
    public BoulderConfiguration(BlockStateProvider stone, BlockStateProvider mossyTop, float mossChance, BlockState carpet,
                                float carpetChance, FloatProvider radius, FloatProvider height) {
        this(stone, mossyTop, mossChance, carpet, carpetChance, radius, height, false);
    }
}
