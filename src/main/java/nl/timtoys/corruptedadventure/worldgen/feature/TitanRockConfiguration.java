package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.List;

/**
 * Settings of {@link TitanRockFeature}.
 *
 * @param templates the rock templates (each at most {@link TitanRockFeature#MAX_REACH} from its centre column)
 * @param cell      the grid of the rocks (blocks); at most one rock per cell
 * @param chance    the share of cells with a rock (where the biome and the ground allow it)
 * @param biome     the biome at the rock's centre
 * @param sink      how many blocks the rock sinks below the lowest ground of its footprint
 * @param materials the blocks of a template that are placed
 */
public record TitanRockConfiguration(List<ResourceLocation> templates, int cell, float chance, ResourceKey<Biome> biome, int sink,
                                     HolderSet<Block> materials)
        implements FeatureConfiguration {
    public static final Codec<TitanRockConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.nonEmptyList(ResourceLocation.CODEC.listOf()).fieldOf("templates").forGetter(TitanRockConfiguration::templates),
            Codec.intRange(64, 2048).fieldOf("cell").forGetter(TitanRockConfiguration::cell),
            Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(TitanRockConfiguration::chance),
            ResourceKey.codec(Registries.BIOME).fieldOf("biome").forGetter(TitanRockConfiguration::biome),
            Codec.intRange(0, 8).fieldOf("sink").forGetter(TitanRockConfiguration::sink),
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("materials").forGetter(TitanRockConfiguration::materials)
    ).apply(instance, TitanRockConfiguration::new));
}
