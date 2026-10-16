package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Settings of {@link PillarCladdingFeature}.
 *
 * @param crystal       the skin of the columns the surface paints (purity crystal)
 * @param lamp          a skin block that is a lamp instead
 * @param cluster       the cluster on the skin (an {@code AmethystClusterBlock}, turned away from it)
 * @param clusterChance chance that an open block against the skin (sides, or under the head) gets a cluster
 * @param lampChance    chance that a skin block with an open face is a lamp
 * @param regionMin     the columns stand only where the region axis is at least this (the surface paints them from 0.20)
 */
public record PillarCladdingConfiguration(BlockState crystal, BlockState lamp, BlockState cluster, float clusterChance, float lampChance,
                                          double regionMin) implements FeatureConfiguration {
    public static final Codec<PillarCladdingConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("crystal").forGetter(PillarCladdingConfiguration::crystal),
            BlockState.CODEC.fieldOf("lamp").forGetter(PillarCladdingConfiguration::lamp),
            BlockState.CODEC.fieldOf("cluster").forGetter(PillarCladdingConfiguration::cluster),
            Codec.floatRange(0.0F, 1.0F).fieldOf("cluster_chance").forGetter(PillarCladdingConfiguration::clusterChance),
            Codec.floatRange(0.0F, 1.0F).fieldOf("lamp_chance").forGetter(PillarCladdingConfiguration::lampChance),
            Codec.DOUBLE.fieldOf("region_min").forGetter(PillarCladdingConfiguration::regionMin)
    ).apply(instance, PillarCladdingConfiguration::new));
}
