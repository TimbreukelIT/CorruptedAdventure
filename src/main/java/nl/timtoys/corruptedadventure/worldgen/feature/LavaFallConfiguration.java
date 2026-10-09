package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Settings of {@link LavaFallFeature}.
 *
 * @param chance       the share of columns that try to be a fall's source (only a wall over lava can be one)
 * @param minDrop      how far the source lies at least above the lava it falls into
 * @param maxPerChunk  at most this many sources per chunk (decision 16)
 * @param biomes       the biomes of both the wall and the column it falls down (the ring 3 core, not the transitions)
 */
public record LavaFallConfiguration(float chance, int minDrop, int maxPerChunk, HolderSet<Biome> biomes) implements FeatureConfiguration {
    public static final Codec<LavaFallConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(LavaFallConfiguration::chance),
            Codec.intRange(4, 128).fieldOf("min_drop").forGetter(LavaFallConfiguration::minDrop),
            Codec.intRange(1, 8).fieldOf("max_per_chunk").forGetter(LavaFallConfiguration::maxPerChunk),
            RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(LavaFallConfiguration::biomes)
    ).apply(instance, LavaFallConfiguration::new));
}
