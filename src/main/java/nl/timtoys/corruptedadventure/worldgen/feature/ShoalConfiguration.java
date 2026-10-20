package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Settings of {@link ShoalFeature}.
 *
 * @param biome          only columns of this biome
 * @param biomeMargin    and only where the four columns this far away are of it too (keeps the shoals off the trees of
 *                       the neighbouring biomes)
 * @param shape          the strength per column (0–1): the share of the density at the column
 * @param maxDepth       only water this deep or shallower is raised
 * @param sink           strength 0 puts the top this far under the water's top block
 * @param rise           strength 1 puts the top this far over it
 * @param bed            the top block under water
 * @param fill           the blocks under the top
 * @param landTop        the top block at or over the water's top
 * @param landPlants     plants on the land tops, with {@code plantChance}
 * @param plantChance    per land column
 * @param shrubs         leaves stacked 1–2 high on a land top, with {@code shrubChance}
 * @param shrubChance    per land column
 * @param reedChance     per land column at the water's edge: a stem of reeds
 * @param bedPlants      plants on the top under water, with {@code bedChance}
 * @param bedChance      per column under water
 */
public record ShoalConfiguration(ResourceKey<Biome> biome, int biomeMargin, ClusterDensity shape, int maxDepth, int sink, int rise,
                                 BlockStateProvider bed, BlockStateProvider fill, BlockStateProvider landTop,
                                 BlockStateProvider landPlants, float plantChance, BlockStateProvider shrubs, float shrubChance,
                                 float reedChance, BlockStateProvider bedPlants, float bedChance) implements FeatureConfiguration {
    public static final Codec<ShoalConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceKey.codec(Registries.BIOME).fieldOf("biome").forGetter(ShoalConfiguration::biome),
            Codec.intRange(0, 15).fieldOf("biome_margin").forGetter(ShoalConfiguration::biomeMargin),
            ClusterDensity.CODEC.fieldOf("shape").forGetter(ShoalConfiguration::shape),
            Codec.intRange(1, 16).fieldOf("max_depth").forGetter(ShoalConfiguration::maxDepth),
            Codec.intRange(0, 16).fieldOf("sink").forGetter(ShoalConfiguration::sink),
            Codec.intRange(0, 4).fieldOf("rise").forGetter(ShoalConfiguration::rise),
            BlockStateProvider.CODEC.fieldOf("bed").forGetter(ShoalConfiguration::bed),
            BlockStateProvider.CODEC.fieldOf("fill").forGetter(ShoalConfiguration::fill),
            BlockStateProvider.CODEC.fieldOf("land_top").forGetter(ShoalConfiguration::landTop),
            BlockStateProvider.CODEC.fieldOf("land_plants").forGetter(ShoalConfiguration::landPlants),
            Codec.floatRange(0.0F, 1.0F).fieldOf("plant_chance").forGetter(ShoalConfiguration::plantChance),
            BlockStateProvider.CODEC.fieldOf("shrubs").forGetter(ShoalConfiguration::shrubs),
            Codec.floatRange(0.0F, 1.0F).fieldOf("shrub_chance").forGetter(ShoalConfiguration::shrubChance),
            Codec.floatRange(0.0F, 1.0F).fieldOf("reed_chance").forGetter(ShoalConfiguration::reedChance),
            BlockStateProvider.CODEC.fieldOf("bed_plants").forGetter(ShoalConfiguration::bedPlants),
            Codec.floatRange(0.0F, 1.0F).fieldOf("bed_chance").forGetter(ShoalConfiguration::bedChance)
    ).apply(instance, ShoalConfiguration::new));
}
