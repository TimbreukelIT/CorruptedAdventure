package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Configuration of {@code corruptedadventure:spore_sprouts_patch}.
 *
 * @param plant   the ground plant
 * @param radius  radius of the patch around the origin's column, 1 to {@link #MAX_RADIUS}
 * @param density chance per column of the patch that a plant stands there
 */
public record SporeSproutsConfiguration(BlockState plant, IntProvider radius, float density) implements FeatureConfiguration {
    public static final int MAX_RADIUS = 4;

    public static final Codec<SporeSproutsConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("plant").forGetter(SporeSproutsConfiguration::plant),
            IntProvider.codec(1, MAX_RADIUS).fieldOf("radius").forGetter(SporeSproutsConfiguration::radius),
            Codec.floatRange(0.0F, 1.0F).fieldOf("density").forGetter(SporeSproutsConfiguration::density)
    ).apply(instance, SporeSproutsConfiguration::new));
}
