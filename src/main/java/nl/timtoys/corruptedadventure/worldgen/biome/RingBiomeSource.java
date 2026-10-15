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
import nl.timtoys.corruptedadventure.worldgen.densityfunction.KeyedSlots;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
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
 * Optional {@code transition_splits}, keyed the same way (verdant_falls): two biomes in one zone. From the share
 * {@link TransitionSplit#at} of the zone outwards its {@link TransitionSplit#outer} source takes over from the
 * boundary's transition source, which keeps the inner part (zone 3→4: {@code steam_fens} on ring 3's rim, then
 * {@code verdant_falls} at {@link IslandField#VERDANT_EDGE_SHARE}, where the terrain's mix ends). The split lies on the
 * zone's own axis ({@link RingPosition#blend}), so it follows the noisy boundary like the terrain does. A split needs a
 * transition at its boundary.
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

    private static final Codec<Map<Ring, TransitionSplit>> SPLITS_CODEC = Codec.unboundedMap(Ring.CODEC, TransitionSplit.CODEC);

    public static final MapCodec<RingBiomeSource> CODEC = RecordCodecBuilder.<RingBiomeSource>mapCodec(instance -> instance.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(source -> source.dimension),
            RINGS_CODEC.fieldOf("rings").forGetter(source -> source.rings),
            TRANSITIONS_CODEC.optionalFieldOf("transitions", Map.of()).forGetter(source -> source.transitions),
            IslandBiomes.CODEC.optionalFieldOf("island_biomes").forGetter(source -> Optional.ofNullable(source.islandBiomes)),
            SPLITS_CODEC.optionalFieldOf("transition_splits", Map.of()).forGetter(source -> source.splits)
    ).apply(instance, (dimension, rings, transitions, islandBiomes, splits) ->
            new RingBiomeSource(dimension, rings, transitions, islandBiomes.orElse(null), splits)))
            .validate(source -> validateSplits(source.transitions, source.splits).map(valid -> source));

    /** A height biome reaches this many blocks below its island's bottom. */
    static final int BELOW_ISLAND = 4;

    private final ResourceKey<Level> dimension;
    private final Map<Ring, BiomeSource> rings;
    private final Map<Ring, BiomeSource> transitions;
    @Nullable
    private final IslandBiomes islandBiomes;
    private final Map<Ring, TransitionSplit> splits;
    // The islands of the running server's level, looked up when a quart needs them.
    private final IslandLookup currentIslands;

    public RingBiomeSource(ResourceKey<Level> dimension, Map<Ring, BiomeSource> rings, Map<Ring, BiomeSource> transitions) {
        this(dimension, rings, transitions, null);
    }

    public RingBiomeSource(ResourceKey<Level> dimension, Map<Ring, BiomeSource> rings, Map<Ring, BiomeSource> transitions,
                           @Nullable IslandBiomes islandBiomes) {
        this(dimension, rings, transitions, islandBiomes, Map.of());
    }

    public RingBiomeSource(ResourceKey<Level> dimension, Map<Ring, BiomeSource> rings, Map<Ring, BiomeSource> transitions,
                           @Nullable IslandBiomes islandBiomes, Map<Ring, TransitionSplit> splits) {
        this.dimension = dimension;
        this.rings = new EnumMap<>(rings);
        // EnumMap's copy constructor rejects an empty plain Map.
        this.transitions = transitions.isEmpty() ? Map.of() : new EnumMap<>(transitions);
        this.islandBiomes = islandBiomes;
        this.splits = splits.isEmpty() ? Map.of() : new EnumMap<>(splits);
        this.currentIslands = new LastColumn(() -> IslandFields.current(dimension));
    }

    /**
     * The running server's islands, one lookup per column (review 59 #5): the biome of a column is asked quart by quart
     * up the column, so the islands of recent columns are remembered in a small lock-free cache (review 61 #5: no
     * thread-local on the worker threads), started afresh for another field (another level or server).
     */
    static final class LastColumn implements IslandLookup {
        private final java.util.function.Supplier<IslandField> field;
        private volatile Cache cache;

        LastColumn(java.util.function.Supplier<IslandField> field) {
            this.field = field;
        }

        @Override
        public List<IslandField.Island> islandsAt(int x, int z) {
            IslandField current = field.get();
            if (current == null) {
                // No server (or no islands): drop the islands of the last one (review 61b #8).
                cache = null;
                return List.of();
            }
            Cache known = cache;
            if (known == null || known.field != current) {
                known = new Cache(current, new KeyedSlots<>());
                cache = known;
            }
            return known.columns.get(KeyedSlots.key(x, z), key -> List.copyOf(current.islandsAt(x, z)));
        }

        private record Cache(IslandField field, KeyedSlots<List<IslandField.Island>> columns) {
        }
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

    static DataResult<Map<Ring, TransitionSplit>> validateSplits(Map<Ring, BiomeSource> transitions, Map<Ring, TransitionSplit> splits) {
        return transitions.keySet().containsAll(splits.keySet())
                ? DataResult.success(splits)
                : DataResult.error(() -> "Ring biome source splits a zone without a transition: splits " + splits.keySet()
                        + ", transitions " + transitions.keySet());
    }

    /**
     * The outer part of a transition zone ({@code transition_splits}): from the share {@code at} of the zone (its blend,
     * 0 at the inner edge) outwards {@code outer} picks the biome.
     */
    public record TransitionSplit(double at, BiomeSource outer) {
        public static final Codec<TransitionSplit> CODEC = RecordCodecBuilder.<TransitionSplit>create(instance -> instance.group(
                Codec.DOUBLE.fieldOf("at").forGetter(TransitionSplit::at),
                BiomeSource.CODEC.fieldOf("outer").forGetter(TransitionSplit::outer)
        ).apply(instance, TransitionSplit::new)).validate(split -> split.at() > 0.0 && split.at() < 1.0
                ? DataResult.success(split)
                : DataResult.error(() -> "A transition split must lie inside its zone, 0 < at < 1: " + split.at()));
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        Stream<Holder<Biome>> sources = Stream.of(rings.values().stream(), transitions.values().stream(),
                        splits.values().stream().map(TransitionSplit::outer))
                .flatMap(stream -> stream)
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
        BiomeSource transition = position.inTransition() ? transition(position) : null;
        BiomeSource source = transition != null ? transition : rings.get(position.ring());
        return source.getNoiseBiome(x, y, z, sampler);
    }

    /** The transition source of a position in a zone, or null: its split's outer part from the split on. */
    @Nullable
    private BiomeSource transition(RingPosition position) {
        TransitionSplit split = splits.get(position.inner());
        return split != null && position.blend() >= split.at() ? split.outer() : transitions.get(position.inner());
    }

    /**
     * The height biome at this block, or null. Of the column's islands only those count whose reach (from
     * {@link #BELOW_ISLAND} under the bottom up) holds the block, whose top reaches {@link IslandBiomes#minTop} and whose
     * centre lies in ring 4; of those, the one of {@link IslandField#uppermost}. So an island under a higher one keeps
     * its own height biome, also when the higher one does not count.
     */
    @Nullable
    private Holder<Biome> heightBiome(RingLayout layout, IslandLookup islands, int x, int y, int z) {
        List<IslandField.Island> counting = new ArrayList<>();
        for (IslandField.Island island : islands.islandsAt(x, z)) {
            if (y >= island.bottom() - BELOW_ISLAND && island.top() >= islandBiomes.minTop()
                    && layout.sample(dimension, island.x(), island.z()).ring() == Ring.RING_4) {
                counting.add(island);
            }
        }
        IslandField.Island island = IslandField.uppermost(counting);
        if (island == null) {
            return null;
        }
        return Ring4Band.of(layout).blighted(island.x(), island.z()) ? islandBiomes.blighted() : islandBiomes.mythic();
    }

    /** The ring 4 islands covering a column (block x, z). */
    @FunctionalInterface
    interface IslandLookup {
        List<IslandField.Island> islandsAt(int x, int z);
    }
}
