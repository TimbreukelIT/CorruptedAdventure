package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Settings of {@link ImpactCraterFeature}.
 *
 * @param radius      the pit's radius (2–{@link ImpactCraterFeature#MAX_RADIUS})
 * @param floor       the scorched floor of the pit
 * @param shards      what is strewn on the floor and the rim (obsidian shards)
 * @param shardChance chance per floor or rim block of a shard
 * @param core        the block at the centre of the floor (the bomb's glowing core)
 * @param ground      the ground the pit may be dug in
 */
public record ImpactCraterConfiguration(IntProvider radius, BlockStateProvider floor, BlockStateProvider shards, float shardChance,
                                        BlockState core, HolderSet<Block> ground) implements FeatureConfiguration {
    public static final Codec<ImpactCraterConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IntProvider.codec(2, ImpactCraterFeature.MAX_RADIUS).fieldOf("radius").forGetter(ImpactCraterConfiguration::radius),
            BlockStateProvider.CODEC.fieldOf("floor").forGetter(ImpactCraterConfiguration::floor),
            BlockStateProvider.CODEC.fieldOf("shards").forGetter(ImpactCraterConfiguration::shards),
            Codec.floatRange(0.0F, 1.0F).fieldOf("shard_chance").forGetter(ImpactCraterConfiguration::shardChance),
            BlockState.CODEC.fieldOf("core").forGetter(ImpactCraterConfiguration::core),
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("ground").forGetter(ImpactCraterConfiguration::ground)
    ).apply(instance, ImpactCraterConfiguration::new));
}
