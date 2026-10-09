package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Settings of {@link FireGeodeFeature}: the three layers of the lining, from the cavity out (each one block thick, the
 * shell of {@link FireGeodeShape#LINING}), and the buds on the inner wall.
 *
 * @param inner     the inner wall (fire crystal)
 * @param middle    the middle layer (blackstone)
 * @param outer     the outer shell (smooth basalt)
 * @param budChance the chance that an open cavity block against the inner wall gets a bud or cluster
 */
public record FireGeodeConfiguration(BlockState inner, BlockState middle, BlockState outer, float budChance) implements FeatureConfiguration {
    public static final Codec<FireGeodeConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("inner").forGetter(FireGeodeConfiguration::inner),
            BlockState.CODEC.fieldOf("middle").forGetter(FireGeodeConfiguration::middle),
            BlockState.CODEC.fieldOf("outer").forGetter(FireGeodeConfiguration::outer),
            Codec.floatRange(0.0F, 1.0F).fieldOf("bud_chance").forGetter(FireGeodeConfiguration::budChance)
    ).apply(instance, FireGeodeConfiguration::new));
}
