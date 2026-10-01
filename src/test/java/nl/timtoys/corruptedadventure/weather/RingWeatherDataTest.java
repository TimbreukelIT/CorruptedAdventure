package nl.timtoys.corruptedadventure.weather;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import nl.timtoys.corruptedadventure.ring.Ring;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RingWeatherDataTest {
    private static final RingWeatherState FRESH = RingWeatherState.start(RingWeatherType.CLEAR, 4321, 0);
    private static final Function<Ring, RingWeatherState> FRESH_FOR = ring -> FRESH;

    @Test
    void newDataHasEveryRingWithWeather() {
        RingWeatherData data = RingWeatherData.create(Level.OVERWORLD, FRESH_FOR);
        assertEquals(List.of(Ring.RING_2, Ring.RING_3, Ring.RING_4, Ring.RING_5), List.copyOf(data.states().keySet()));
        assertTrue(data.states().values().stream().allMatch(FRESH::equals));
        assertSame(RingWeatherState.STILL, data.get(Ring.RING_1));
        assertSame(RingWeatherState.STILL, data.get(Ring.VOID));
        assertTrue(data.isDirty());
    }

    @Test
    void saveLoadRoundTrip() {
        RingWeatherData data = RingWeatherData.create(Level.OVERWORLD, FRESH_FOR);
        data.put(Ring.RING_2, new RingWeatherState(RingWeatherType.SANDSTORM, 1000, 12_000, 77));
        data.put(Ring.RING_3, new RingWeatherState(RingWeatherType.ERUPTION, 5, 20_000, -3));
        data.put(Ring.RING_1, new RingWeatherState(RingWeatherType.SANDSTORM, 5, 10, 1)); // ignored: no weather in ring 1
        RingWeatherData loaded = RingWeatherData.load(data.save(new CompoundTag(), null), Level.OVERWORLD, ring -> {
            throw new AssertionError("nothing is missing");
        });
        assertEquals(data.states(), loaded.states());
        assertSame(RingWeatherState.STILL, loaded.get(Ring.RING_1));
        assertFalse(loaded.isDirty());
    }

    @Test
    void missingRingStartsClear() {
        CompoundTag tag = RingWeatherData.create(Level.OVERWORLD, FRESH_FOR).save(new CompoundTag(), null);
        tag.getCompound("rings").remove("ring_4");
        RingWeatherState other = RingWeatherState.start(RingWeatherType.CLEAR, 999, 0);
        RingWeatherData loaded = RingWeatherData.load(tag, Level.OVERWORLD, ring -> ring == Ring.RING_4 ? other : FRESH);
        assertEquals(other, loaded.get(Ring.RING_4));
        assertEquals(FRESH, loaded.get(Ring.RING_2));
    }

    @Test
    void emptyTagStartsClearEverywhere() {
        RingWeatherData loaded = RingWeatherData.load(new CompoundTag(), Level.OVERWORLD, FRESH_FOR);
        assertEquals(4, loaded.states().size());
        assertTrue(loaded.states().values().stream().allMatch(FRESH::equals));
    }

    @Test
    void unknownTypeNameOrForeignTypeStartsClear() {
        RingWeatherData data = RingWeatherData.create(Level.OVERWORLD, FRESH_FOR);
        data.put(Ring.RING_2, new RingWeatherState(RingWeatherType.SANDSTORM, 1000, 12_000, 1));
        data.put(Ring.RING_3, new RingWeatherState(RingWeatherType.ERUPTION, 1000, 12_000, 1));
        CompoundTag tag = data.save(new CompoundTag(), null);
        tag.getCompound("rings").getCompound("ring_2").putString("type", "renamed_storm");
        tag.getCompound("rings").getCompound("ring_3").putString("type", "toxic_rain"); // a ring 4 type
        tag.getCompound("rings").getCompound("ring_4").putString("remaining_ticks", "not a number");
        RingWeatherData loaded = RingWeatherData.load(tag, Level.OVERWORLD, FRESH_FOR);
        assertEquals(FRESH, loaded.get(Ring.RING_2));
        assertEquals(FRESH, loaded.get(Ring.RING_3));
        assertEquals(FRESH, loaded.get(Ring.RING_4));
        assertEquals(FRESH, loaded.get(Ring.RING_5));
    }

    @Test
    void savedStillStateStartsClear() {
        RingWeatherData data = RingWeatherData.create(Level.OVERWORLD, FRESH_FOR);
        CompoundTag tag = data.save(new CompoundTag(), null);
        tag.getCompound("rings").getCompound("ring_5").putInt("duration_ticks", 0);
        RingWeatherState other = RingWeatherState.start(RingWeatherType.CLEAR, 1, 0);
        assertEquals(other, RingWeatherData.load(tag, Level.OVERWORLD, ring -> other).get(Ring.RING_5));
    }

    @Test
    void netherHasNoRings() {
        assertTrue(RingWeatherData.create(Level.NETHER, FRESH_FOR).states().isEmpty());
    }
}
