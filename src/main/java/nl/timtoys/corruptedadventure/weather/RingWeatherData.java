package nl.timtoys.corruptedadventure.weather;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.ring.Ring;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/**
 * The ring weather of one dimension ({@code <world>/data/corruptedadventure_ring_weather.dat} for the overworld): one
 * state per ring with weather types. A missing ring, an unknown type name or a type of another ring loads as
 * {@code clear} with a freshly drawn duration and a warning; loading never throws.
 */
public final class RingWeatherData extends SavedData {
    public static final String FILE_NAME = CorruptedAdventure.MOD_ID + "_ring_weather";

    private static final String RINGS = "rings";

    private final ResourceKey<Level> dimension;
    private final EnumMap<Ring, RingWeatherState> states = new EnumMap<>(Ring.class);
    /** Not saved: the {@code doWeatherCycle} value the clients last heard of (null = not checked yet). */
    private Boolean syncedCycleRunning;

    private RingWeatherData(ResourceKey<Level> dimension) {
        this.dimension = dimension;
    }

    /** New data: every ring with weather starts with {@code fresh.apply(ring)} (a clear spell). */
    static RingWeatherData create(ResourceKey<Level> dimension, Function<Ring, RingWeatherState> fresh) {
        RingWeatherData data = new RingWeatherData(dimension);
        RingWeatherType.ringsWithWeather(dimension).forEach(ring -> data.states.put(ring, fresh.apply(ring)));
        data.setDirty();
        return data;
    }

    static RingWeatherData load(CompoundTag tag, ResourceKey<Level> dimension, Function<Ring, RingWeatherState> fresh) {
        RingWeatherData data = new RingWeatherData(dimension);
        CompoundTag rings = tag.getCompound(RINGS);
        for (Ring ring : RingWeatherType.ringsWithWeather(dimension)) {
            data.states.put(ring, loadRing(rings, ring, dimension, fresh));
        }
        return data;
    }

    private static RingWeatherState loadRing(CompoundTag rings, Ring ring, ResourceKey<Level> dimension,
                                             Function<Ring, RingWeatherState> fresh) {
        String key = ring.getSerializedName();
        if (!rings.contains(key, Tag.TAG_COMPOUND)) {
            CorruptedAdventure.LOGGER.warn("No saved ring weather for {} in {}; starting clear", key, dimension.location());
            return fresh.apply(ring);
        }
        RingWeatherState state = RingWeatherState.CODEC.parse(NbtOps.INSTANCE, rings.get(key))
                .resultOrPartial(error -> CorruptedAdventure.LOGGER.warn(
                        "Unreadable ring weather for {} in {} ({}); starting clear", key, dimension.location(), error))
                .orElse(null);
        if (state == null) {
            return fresh.apply(ring);
        }
        if (!state.type().allowedIn(dimension, ring) || state.isStill()) {
            CorruptedAdventure.LOGGER.warn("Saved ring weather {} does not fit {} in {}; starting clear",
                    state.type().getSerializedName(), key, dimension.location());
            return fresh.apply(ring);
        }
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag rings = new CompoundTag();
        states.forEach((ring, state) -> RingWeatherState.CODEC.encodeStart(NbtOps.INSTANCE, state)
                .ifSuccess(encoded -> rings.put(ring.getSerializedName(), encoded)));
        tag.put(RINGS, rings);
        return tag;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    /** The weather of a ring; {@link RingWeatherState#STILL} for a ring without ring weather. */
    public RingWeatherState get(Ring ring) {
        return states.getOrDefault(ring, RingWeatherState.STILL);
    }

    /** Only for rings with weather (others are ignored). */
    void put(Ring ring, RingWeatherState state) {
        if (states.containsKey(ring)) {
            states.put(ring, state);
            setDirty();
        }
    }

    /** The rings with weather and their state, in ring order (read-only). */
    public Map<Ring, RingWeatherState> states() {
        return Collections.unmodifiableMap(states);
    }

    Boolean syncedCycleRunning() {
        return syncedCycleRunning;
    }

    void syncedCycleRunning(boolean running) {
        syncedCycleRunning = running;
    }
}
