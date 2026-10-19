package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Settings of {@link TerraceDressingFeature}.
 *
 * @param minStep    the smallest step to the neighbouring column that counts as a wall (below) or a rim (above)
 * @param vine       the hanging plant on a wall (a {@code minecraft:vine}-like block with a face per side)
 * @param vineChance per wall: the chance of a vine hanging down it
 * @param maxVine    the longest vine
 * @param moss       the block a rim's grass turns into
 * @param mossChance per rim column: the chance of {@code moss}
 * @param carpet     a carpet on a rim
 * @param carpetChance per rim column: the chance of a carpet (only in open air)
 */
public record TerraceDressingConfiguration(int minStep, BlockState vine, float vineChance, int maxVine, BlockState moss, float mossChance,
                                           BlockState carpet, float carpetChance) implements FeatureConfiguration {
    public static final Codec<TerraceDressingConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, 16).fieldOf("min_step").forGetter(TerraceDressingConfiguration::minStep),
            BlockState.CODEC.fieldOf("vine").forGetter(TerraceDressingConfiguration::vine),
            Codec.floatRange(0.0F, 1.0F).fieldOf("vine_chance").forGetter(TerraceDressingConfiguration::vineChance),
            Codec.intRange(1, 16).fieldOf("max_vine").forGetter(TerraceDressingConfiguration::maxVine),
            BlockState.CODEC.fieldOf("moss").forGetter(TerraceDressingConfiguration::moss),
            Codec.floatRange(0.0F, 1.0F).fieldOf("moss_chance").forGetter(TerraceDressingConfiguration::mossChance),
            BlockState.CODEC.fieldOf("carpet").forGetter(TerraceDressingConfiguration::carpet),
            Codec.floatRange(0.0F, 1.0F).fieldOf("carpet_chance").forGetter(TerraceDressingConfiguration::carpetChance)
    ).apply(instance, TerraceDressingConfiguration::new));
}
