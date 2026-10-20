package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Settings of {@link GiantMoonpetalFeature}.
 *
 * @param log    the trunk (upright)
 * @param wood   the roots and boughs (bark on every side)
 * @param leaves the crown
 */
public record GiantMoonpetalConfiguration(BlockState log, BlockState wood, BlockState leaves) implements FeatureConfiguration {
    public static final Codec<GiantMoonpetalConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("log").forGetter(GiantMoonpetalConfiguration::log),
            BlockState.CODEC.fieldOf("wood").forGetter(GiantMoonpetalConfiguration::wood),
            BlockState.CODEC.fieldOf("leaves").forGetter(GiantMoonpetalConfiguration::leaves)
    ).apply(instance, GiantMoonpetalConfiguration::new));
}
