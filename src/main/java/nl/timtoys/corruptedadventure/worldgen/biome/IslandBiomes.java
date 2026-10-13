package nl.timtoys.corruptedadventure.worldgen.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

/**
 * The height biomes of ring 4's islands ({@code island_biomes} of {@link RingBiomeSource}, package 59): an island whose
 * top lies at {@code min_top} or higher gets {@code mythic} or {@code blighted} as a whole, after the band at its centre.
 *
 * @param minTop lowest top (block Y) of an island that gets a height biome
 */
public record IslandBiomes(Holder<Biome> mythic, Holder<Biome> blighted, int minTop) {
    public static final int DEFAULT_MIN_TOP = 140;

    public static final Codec<IslandBiomes> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Biome.CODEC.fieldOf("mythic").forGetter(IslandBiomes::mythic),
            Biome.CODEC.fieldOf("blighted").forGetter(IslandBiomes::blighted),
            Codec.intRange(-64, 320).optionalFieldOf("min_top", DEFAULT_MIN_TOP).forGetter(IslandBiomes::minTop)
    ).apply(instance, IslandBiomes::new));
}
