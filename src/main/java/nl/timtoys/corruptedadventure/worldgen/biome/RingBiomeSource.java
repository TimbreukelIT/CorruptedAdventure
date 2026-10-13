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
import nl.timtoys.corruptedadventure.ring.Ring4Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import nl.timtoys.corruptedadventure.worldgen.chunk.IslandFields;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
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
 * <p>
 * Optional {@code island_biomes} (overworld, package 59): the height hook of ring 4. An island of the ring 4
 * {@link IslandField} whose centre lies in ring 4 and whose top reaches {@link IslandBiomes#minTop} gets a height biome
 * as a whole, also where it hangs over a transition zone: from {@link #BELOW_ISLAND} blocks under its bottom up, within
 * its footprint, never two biomes stacked on one island. The band at its centre ({@link Ring4Band}) picks the biome.
 * Lower islands and the ground keep the ground biome. The islands come from the router ({@link IslandFields}), the
 * same instance the terrain uses.
 */
public final class RingBiomeSource extends BiomeSource {
    private static final Codec<Map<Ring, BiomeSource>> RINGS_CODEC =
            Codec.unboundedMap(Ring.CODEC, BiomeSource.CODEC).validate(RingBiomeSource::validateRings);

    private static final Codec<Map<Ring, BiomeSource>> TRANSITIONS_CODEC =
            Codec.unboundedMap(Ring.CODEC, BiomeSource.CODEC).validate(RingBiomeSource::validateTransitions);

    public static final MapCodec<RingBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(source -> source.dimension),
            RINGS_CODEC.fieldOf("rings").forGetter(source -> source.rings),
            TRANSITIONS_CODEC.optionalFieldOf("transitions", Map.of()).forGetter(source -> source.transitions),
            IslandBiomes.CODEC.optionalFieldOf("island_biomes").forGetter(source -> Optional.ofNullable(source.islandBiomes))
    ).apply(instance, (dimension, rings, transitions, islandBiomes) ->
            new RingBiomeSource(dimension, rings, transitions, islandBiomes.orElse(null))));

    /** A height biome reaches this many blocks below its island's bottom. */
    static final int BELOW_ISLAND = 4;

    private final ResourceKey<Level> dimension;
    private final Map<Ring, BiomeSource> rings;
    private final Map<Ring, BiomeSource> transitions;
    @Nullable
    private final IslandBiomes islandBiomes;
    // The islands of the running server's level, looked up when a quart needs them.
    private final IslandLookup currentIslands;

    public RingBiomeSource(ResourceKey<Level> dimension, Map<Ring, BiomeSource> rings, Map<Ring, BiomeSource> transitions) {
        this(dimension, rings, transitions, null);
    }

    public RingBiomeSource(ResourceKey<Level> dimension, Map<Ring, BiomeSource> rings, Map<Ring, BiomeSource> transitions,
                           @Nullable IslandBiomes islandBiomes) {
        this.dimension = dimension;
        this.rings = new EnumMap<>(rings);
        // EnumMap's copy constructor rejects an empty plain Map.
        this.transitions = transitions.isEmpty() ? Map.of() : new EnumMap<>(transitions);
        this.islandBiomes = islandBiomes;
        this.currentIslands = (x, z) -> {
            IslandField field = IslandFields.current(dimension);
            return field == null ? null : field.islandAt(x, z);
        };
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
        Stream<Holder<Biome>> sources = Stream.concat(rings.values().stream(), transitions.values().stream())
                .flatMap(source -> source.possibleBiomes().stream());
        return islandBiomes == null ? sources : Stream.concat(sources, Stream.of(islandBiomes.mythic(), islandBiomes.blighted()));
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        return getNoiseBiome(RingLayout.current(), x, y, z, sampler);
    }

    /** @param x quart position, like {@link #getNoiseBiome(int, int, int, Climate.Sampler)} */
    Holder<Biome> getNoiseBiome(RingLayout layout, int x, int y, int z, Climate.Sampler sampler) {
        return getNoiseBiome(layout, currentIslands, x, y, z, sampler);
    }

    /** The same with the islands given (tests). */
    Holder<Biome> getNoiseBiome(RingLayout layout, IslandLookup islands, int x, int y, int z, Climate.Sampler sampler) {
        RingPosition position = layout.sample(dimension, QuartPos.toBlock(x), QuartPos.toBlock(z));
        if (islandBiomes != null && dimension == Level.OVERWORLD && (position.inner() == Ring.RING_4 || position.outer() == Ring.RING_4)) {
            // Sampled in the middle of the quart.
            Holder<Biome> height = heightBiome(layout, islands, QuartPos.toBlock(x) + 2, QuartPos.toBlock(y) + 2, QuartPos.toBlock(z) + 2);
            if (height != null) {
                return height;
            }
        }
        BiomeSource transition = position.inTransition() ? transitions.get(position.inner()) : null;
        BiomeSource source = transition != null ? transition : rings.get(position.ring());
        return source.getNoiseBiome(x, y, z, sampler);
    }

    /** The height biome of the island over this block, or null (no island, too low, outside ring 4, below it). */
    @Nullable
    private Holder<Biome> heightBiome(RingLayout layout, IslandLookup islands, int x, int y, int z) {
        IslandField.Island island = islands.islandAt(x, z);
        if (island == null || island.top() < islandBiomes.minTop() || y < island.bottom() - BELOW_ISLAND
                || layout.sample(dimension, island.x(), island.z()).ring() != Ring.RING_4) {
            return null;
        }
        return Ring4Band.of(layout).blighted(island.x(), island.z()) ? islandBiomes.blighted() : islandBiomes.mythic();
    }

    /** The ring 4 island covering a column (block x, z), or null. */
    @FunctionalInterface
    interface IslandLookup {
        @Nullable
        IslandField.Island islandAt(int x, int z);
    }
}
