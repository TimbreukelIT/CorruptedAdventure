package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Configuration of {@code corruptedadventure:mold_patch}.
 *
 * @param mold   the multiface block (its faces are set per block)
 * @param radius radius of the sphere around the origin, 1 to {@link #MAX_RADIUS}
 * @param chance chance that a free block next to terrain gets mold
 */
public record MoldPatchConfiguration(BlockState mold, IntProvider radius, float chance) implements FeatureConfiguration {
    public static final int MAX_RADIUS = 6;

    public static final Codec<MoldPatchConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("mold").forGetter(MoldPatchConfiguration::mold),
            IntProvider.codec(1, MAX_RADIUS).fieldOf("radius").forGetter(MoldPatchConfiguration::radius),
            Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(MoldPatchConfiguration::chance)
    ).apply(instance, MoldPatchConfiguration::new));
}
