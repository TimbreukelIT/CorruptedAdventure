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
import nl.timtoys.corruptedadventure.ring.RingPosition;

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
 * comes from {@link RingLayout#current()}.
 * <p>
 * Optional {@code transitions}, keyed by the ring on the inside of a boundary ({@code ring_1} = boundary 1→2): that
 * source fills the whole transition zone of the boundary, water included. Boundaries without one switch hard at
 * blend 0.5.
 */
public final class RingBiomeSource extends BiomeSource {
    private static final Codec<Map<Ring, BiomeSource>> RINGS_CODEC =
            Codec.unboundedMap(Ring.CODEC, BiomeSource.CODEC).validate(RingBiomeSource::validateRings);

    private static final Codec<Map<Ring, BiomeSource>> TRANSITIONS_CODEC =
            Codec.unboundedMap(Ring.CODEC, BiomeSource.CODEC).validate(RingBiomeSource::validateTransitions);

    public static final MapCodec<RingBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(source -> source.dimension),
            RINGS_CODEC.fieldOf("rings").forGetter(source -> source.rings),
            TRANSITIONS_CODEC.optionalFieldOf("transitions", Map.of()).forGetter(source -> source.transitions)
    ).apply(instance, RingBiomeSource::new));

    private final ResourceKey<Level> dimension;
    private final Map<Ring, BiomeSource> rings;
    private final Map<Ring, BiomeSource> transitions;

    public RingBiomeSource(ResourceKey<Level> dimension, Map<Ring, BiomeSource> rings, Map<Ring, BiomeSource> transitions) {
        this.dimension = dimension;
        this.rings = new EnumMap<>(rings);
        // EnumMap's copy constructor rejects an empty plain Map.
        this.transitions = transitions.isEmpty() ? Map.of() : new EnumMap<>(transitions);
    }

    static DataResult<Map<Ring, BiomeSource>> validateRings(Map<Ring, BiomeSource> rings) {
        Ring[] missing = Arrays.stream(Ring.values()).filter(ring -> !rings.containsKey(ring)).toArray(Ring[]::new);
        return missing.length == 0
                ? DataResult.success(rings)
                : DataResult.error(() -> "Ring biome source is missing rings: " + Arrays.toString(missing));
    }

    static DataResult<Map<Ring, BiomeSource>> validateTransitions(Map<Ring, BiomeSource> transitions) {
        return transitions.containsKey(Ring.VOID)
                ? DataResult.error(() -> "Ring biome source has a transition for void, but there is no boundary beyond the void")
                : DataResult.success(transitions);
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.concat(rings.values().stream(), transitions.values().stream())
                .flatMap(source -> source.possibleBiomes().stream());
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        return getNoiseBiome(RingLayout.current(), x, y, z, sampler);
    }

    /** @param x quart position, like {@link #getNoiseBiome(int, int, int, Climate.Sampler)} */
    Holder<Biome> getNoiseBiome(RingLayout layout, int x, int y, int z, Climate.Sampler sampler) {
        RingPosition position = layout.sample(dimension, QuartPos.toBlock(x), QuartPos.toBlock(z));
        BiomeSource transition = position.inTransition() ? transitions.get(position.inner()) : null;
        BiomeSource source = transition != null ? transition : rings.get(position.ring());
        return source.getNoiseBiome(x, y, z, sampler);
    }
}
