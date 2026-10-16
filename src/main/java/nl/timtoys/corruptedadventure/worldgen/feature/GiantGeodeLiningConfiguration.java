package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Settings of {@link GiantGeodeLiningFeature}: the three layers of the lining from the hollow out, the lamps set into
 * the inner layer of the dome, and the clusters on the inner wall.
 *
 * @param inner         the inner wall (purity crystal)
 * @param middle        the middle layer (calcite)
 * @param outer         the outer shell (bleached netherrack)
 * @param lamp          an inner-wall block of the dome that is a lamp instead
 * @param cluster       the cluster on the inner wall (an {@code AmethystClusterBlock}, turned away from the wall)
 * @param clusterChance chance that an open block against the inner wall (not on the floor) gets a cluster
 * @param lampChance    chance that an inner-wall block above the geode's centre is a lamp
 */
public record GiantGeodeLiningConfiguration(BlockState inner, BlockState middle, BlockState outer, BlockState lamp, BlockState cluster,
                                            float clusterChance, float lampChance) implements FeatureConfiguration {
    public static final Codec<GiantGeodeLiningConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("inner").forGetter(GiantGeodeLiningConfiguration::inner),
            BlockState.CODEC.fieldOf("middle").forGetter(GiantGeodeLiningConfiguration::middle),
            BlockState.CODEC.fieldOf("outer").forGetter(GiantGeodeLiningConfiguration::outer),
            BlockState.CODEC.fieldOf("lamp").forGetter(GiantGeodeLiningConfiguration::lamp),
            BlockState.CODEC.fieldOf("cluster").forGetter(GiantGeodeLiningConfiguration::cluster),
            Codec.floatRange(0.0F, 1.0F).fieldOf("cluster_chance").forGetter(GiantGeodeLiningConfiguration::clusterChance),
            Codec.floatRange(0.0F, 1.0F).fieldOf("lamp_chance").forGetter(GiantGeodeLiningConfiguration::lampChance)
    ).apply(instance, GiantGeodeLiningConfiguration::new));
}
