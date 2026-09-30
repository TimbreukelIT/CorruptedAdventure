package nl.timtoys.corruptedadventure.worldgen.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Picks the biome source of the ring a position lies in ({@link RingLayout}) and lets that source choose the biome.
 * Each ring's source is a normal biome source ({@code minecraft:fixed} now, {@code minecraft:multi_noise} later), so
 * variants within a ring stay data-driven.
 * <p>
 * A biome source knows neither its dimension nor the world seed: the dimension is a codec field, the seeded layout
 * comes from {@link RingLayout#current()}. Transition zones switch hard at the boundary (blend 0.5) until transition
 * biomes get their own optional field.
 */
public final class RingBiomeSource extends BiomeSource {
    private static final Codec<Map<Ring, BiomeSource>> RINGS_CODEC =
            Codec.unboundedMap(Ring.CODEC, BiomeSource.CODEC).validate(RingBiomeSource::validateRings);

    public static final MapCodec<RingBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(source -> source.dimension),
            RINGS_CODEC.fieldOf("rings").forGetter(source -> source.rings)
    ).apply(instance, RingBiomeSource::new));

    private final ResourceKey<Level> dimension;
    private final Map<Ring, BiomeSource> rings;

    public RingBiomeSource(ResourceKey<Level> dimension, Map<Ring, BiomeSource> rings) {
        this.dimension = dimension;
        this.rings = new EnumMap<>(rings);
    }

    static DataResult<Map<Ring, BiomeSource>> validateRings(Map<Ring, BiomeSource> rings) {
        Ring[] missing = Arrays.stream(Ring.values()).filter(ring -> !rings.containsKey(ring)).toArray(Ring[]::new);
        return missing.length == 0
                ? DataResult.success(rings)
                : DataResult.error(() -> "Ring biome source is missing rings: " + Arrays.toString(missing));
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return rings.values().stream().flatMap(source -> source.possibleBiomes().stream());
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        return getNoiseBiome(RingLayout.current(), x, y, z, sampler);
    }

    /** @param x quart position, like {@link #getNoiseBiome(int, int, int, Climate.Sampler)} */
    Holder<Biome> getNoiseBiome(RingLayout layout, int x, int y, int z, Climate.Sampler sampler) {
        Ring ring = layout.sample(dimension, QuartPos.toBlock(x), QuartPos.toBlock(z)).ring();
        return rings.get(ring).getNoiseBiome(x, y, z, sampler);
    }
}
