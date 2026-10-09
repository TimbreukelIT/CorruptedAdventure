package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Settings of {@link AshFootprintsFeature}.
 *
 * @param cell   the grid of the trails (blocks); at most one trail per cell
 * @param chance the share of cells with a trail
 * @param block  the full ash block a print may dent
 * @param layer  the layered ash (a print in a drift, or the dent in full ash)
 * @param cast   the figure a trail may end at
 * @param biomes where a trail runs: it stops for good at its first step outside them
 */
public record AshFootprintsConfiguration(int cell, float chance, Block block, Block layer, AshCastConfiguration cast, HolderSet<Biome> biomes)
        implements FeatureConfiguration {
    public static final Codec<AshFootprintsConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(32, 512).fieldOf("cell").forGetter(AshFootprintsConfiguration::cell),
            Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(AshFootprintsConfiguration::chance),
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").forGetter(AshFootprintsConfiguration::block),
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("layer").forGetter(AshFootprintsConfiguration::layer),
            AshCastConfiguration.CODEC.fieldOf("cast").forGetter(AshFootprintsConfiguration::cast),
            RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(AshFootprintsConfiguration::biomes)
    ).apply(instance, AshFootprintsConfiguration::new));
}
