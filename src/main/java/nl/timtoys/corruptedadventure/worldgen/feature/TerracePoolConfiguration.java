package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Settings of {@link TerracePoolFeature}.
 *
 * @param fill    what boils in the pool (one draw per pool: every pool has one colour)
 * @param lip     the raised lip around the pool (travertine)
 * @param terrace the steps around the lip, from the lip outwards
 * @param radius  a pool's radius (before the ragged edge and the stretch)
 * @param ground  the ground the pool and its terraces may replace
 * @param cell    the grid of the clusters (blocks); at most one cluster per cell
 * @param chance  the share of cells with a cluster
 * @param pools   the pools a cluster tries to lay out (tries that crowd an earlier pool are dropped)
 * @param biome   the biome at a pool's centre
 */
public record TerracePoolConfiguration(BlockStateProvider fill, BlockStateProvider lip, BlockStateProvider terrace, IntProvider radius,
                                       HolderSet<Block> ground, int cell, float chance, IntProvider pools, ResourceKey<Biome> biome)
        implements FeatureConfiguration {
    public static final Codec<TerracePoolConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockStateProvider.CODEC.fieldOf("fill").forGetter(TerracePoolConfiguration::fill),
            BlockStateProvider.CODEC.fieldOf("lip").forGetter(TerracePoolConfiguration::lip),
            BlockStateProvider.CODEC.fieldOf("terrace").forGetter(TerracePoolConfiguration::terrace),
            IntProvider.codec(1, TerracePoolFeature.MAX_RADIUS).fieldOf("radius").forGetter(TerracePoolConfiguration::radius),
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("ground").forGetter(TerracePoolConfiguration::ground),
            Codec.intRange(TerracePoolLayout.MIN_CELL, TerracePoolLayout.MAX_CELL).fieldOf("cell").forGetter(TerracePoolConfiguration::cell),
            Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(TerracePoolConfiguration::chance),
            IntProvider.codec(1, TerracePoolLayout.MAX_POOLS).fieldOf("pools").forGetter(TerracePoolConfiguration::pools),
            ResourceKey.codec(Registries.BIOME).fieldOf("biome").forGetter(TerracePoolConfiguration::biome)
    ).apply(instance, TerracePoolConfiguration::new));
}
