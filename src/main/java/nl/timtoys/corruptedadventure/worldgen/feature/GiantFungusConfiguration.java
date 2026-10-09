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
 * @param lamp       the light blocks scattered in the cap (the cap itself for a fungus without light)
 * @param stalactite the hanging growths under the cap (its thickness and direction are set per block)
 * @param pillars    whether the pillar form (floor to ceiling) is allowed; else always the free-standing form
 * @param freeSize   the size range of the free-standing form; left out of the JSON when it is the default
 */
public record GiantFungusConfiguration(BlockState stem, BlockState cap, BlockState lamp, BlockState stalactite, boolean pillars,
                                       GiantFungusShape.FreeSize freeSize)
        implements FeatureConfiguration {
    public static final Codec<GiantFungusConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("stem").forGetter(GiantFungusConfiguration::stem),
            BlockState.CODEC.fieldOf("cap").forGetter(GiantFungusConfiguration::cap),
            BlockState.CODEC.fieldOf("lamp").forGetter(GiantFungusConfiguration::lamp),
            BlockState.CODEC.fieldOf("stalactite").forGetter(GiantFungusConfiguration::stalactite),
            Codec.BOOL.optionalFieldOf("pillars", true).forGetter(GiantFungusConfiguration::pillars),
            GiantFungusShape.FreeSize.CODEC.optionalFieldOf("free_size", GiantFungusShape.FreeSize.DEFAULT)
                    .forGetter(GiantFungusConfiguration::freeSize)
    ).apply(instance, GiantFungusConfiguration::new));

    /** The dripshroom fungi of Nether ring 2: the default free-standing size. */
    public GiantFungusConfiguration(BlockState stem, BlockState cap, BlockState lamp, BlockState stalactite, boolean pillars) {
        this(stem, cap, lamp, stalactite, pillars, GiantFungusShape.FreeSize.DEFAULT);
    }
}
