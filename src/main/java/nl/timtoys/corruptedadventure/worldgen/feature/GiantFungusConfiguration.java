package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Configuration of {@code corruptedadventure:giant_fungus}: the blocks of a giant fungus. The shape is
 * {@link GiantFungusShape}; another configuration with other blocks gives another kind of giant fungus (Nether ring 3's).
 *
 * @param stem       the stem, with its axis set to Y
 * @param cap        the cap
 * @param lamp       the light blocks scattered in the cap
 * @param stalactite the hanging growths under the cap (its thickness and direction are set per block)
 * @param pillars    whether the pillar form (floor to ceiling) is allowed; else always the free-standing form
 */
public record GiantFungusConfiguration(BlockState stem, BlockState cap, BlockState lamp, BlockState stalactite, boolean pillars)
        implements FeatureConfiguration {
    public static final Codec<GiantFungusConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("stem").forGetter(GiantFungusConfiguration::stem),
            BlockState.CODEC.fieldOf("cap").forGetter(GiantFungusConfiguration::cap),
            BlockState.CODEC.fieldOf("lamp").forGetter(GiantFungusConfiguration::lamp),
            BlockState.CODEC.fieldOf("stalactite").forGetter(GiantFungusConfiguration::stalactite),
            Codec.BOOL.optionalFieldOf("pillars", true).forGetter(GiantFungusConfiguration::pillars)
    ).apply(instance, GiantFungusConfiguration::new));
}
