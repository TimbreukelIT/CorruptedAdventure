package nl.timtoys.corruptedadventure.weather;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import static nl.timtoys.corruptedadventure.weather.RingWeatherState.FADE_TICKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RingWeatherStateTest {
    private static final float EPSILON = 1.0E-6F;

    @Test
    void intensityFadesInAndOut() {
        int duration = 12_000;
        assertEquals(0.0F, at(RingWeatherType.SANDSTORM, duration, duration).intensity(), EPSILON);
        assertEquals(0.5F, at(RingWeatherType.SANDSTORM, duration - FADE_TICKS / 2, duration).intensity(), EPSILON);
        assertEquals(1.0F, at(RingWeatherType.SANDSTORM, duration - FADE_TICKS, duration).intensity(), EPSILON);
        assertEquals(1.0F, at(RingWeatherType.SANDSTORM, duration / 2, duration).intensity(), EPSILON);
        assertEquals(1.0F, at(RingWeatherType.SANDSTORM, FADE_TICKS, duration).intensity(), EPSILON);
        assertEquals(0.25F, at(RingWeatherType.SANDSTORM, FADE_TICKS / 4, duration).intensity(), EPSILON);
        assertEquals(0.0F, at(RingWeatherType.SANDSTORM, 0, duration).intensity(), EPSILON);
    }

    @Test
    void theAshCloudWarnsForTwentySeconds() {
        int duration = 3_600;
        assertEquals(400, RingWeatherType.ASH_CLOUD.fadeTicks());
        assertEquals(FADE_TICKS, RingWeatherType.ERUPTION.fadeTicks());
        assertEquals(0.5F, at(RingWeatherType.ASH_CLOUD, duration - 200, duration).intensity(), EPSILON);
        assertEquals(1.0F, at(RingWeatherType.ASH_CLOUD, duration - 400, duration).intensity(), EPSILON);
        assertEquals(0.25F, at(RingWeatherType.ASH_CLOUD, 100, duration).intensity(), EPSILON);
    }

    @Test
    void shortWeatherNeverReachesFullStrength() {
        int duration = FADE_TICKS; // shorter than fade-in + fade-out
        assertEquals(0.5F, at(RingWeatherType.ASH_RAIN, duration / 2, duration).intensity(), EPSILON);
        for (int remaining = 0; remaining <= duration; remaining += 20) {
            assertTrue(at(RingWeatherType.ASH_RAIN, remaining, duration).intensity() <= 0.5F + EPSILON);
        }
    }

    @Test
    void clearAndStillHaveNoIntensity() {
        assertEquals(0.0F, at(RingWeatherType.CLEAR, 6_000, 12_000).intensity());
        assertEquals(0.0F, RingWeatherState.STILL.intensity());
        assertTrue(RingWeatherState.STILL.isStill());
    }

    @Test
    void countDownStopsAtZeroAndValuesAreClamped() {
        assertEquals(0, at(RingWeatherType.SANDSTORM, 10, 100).countDown(20).remainingTicks());
        assertEquals(80, at(RingWeatherType.SANDSTORM, 100, 100).countDown(20).remainingTicks());
        assertEquals(100, new RingWeatherState(RingWeatherType.SANDSTORM, 500, 100, 0).remainingTicks());
        assertEquals(0, new RingWeatherState(RingWeatherType.SANDSTORM, -5, 100, 0).remainingTicks());
        assertEquals(0, new RingWeatherState(RingWeatherType.SANDSTORM, 5, -1, 0).durationTicks());
    }

    @Test
    void startKeepsTheSeedOnlyForWeather() {
        assertEquals(42, RingWeatherState.start(RingWeatherType.SANDSTORM, 100, 42).seed());
        assertEquals(0, RingWeatherState.start(RingWeatherType.CLEAR, 100, 42).seed());
    }

    @Test
    void codecRoundTrip() {
        RingWeatherState state = new RingWeatherState(RingWeatherType.TOXIC_RAIN, 1234, 20_000, -987_654_321);
        Tag nbt = RingWeatherState.CODEC.encodeStart(NbtOps.INSTANCE, state).getOrThrow();
        assertEquals(state, RingWeatherState.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow());
        assertEquals(state, RingWeatherState.CODEC.parse(JsonOps.INSTANCE,
                RingWeatherState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow()).getOrThrow());
    }

    @Test
    void codecWithoutSeedReadsZero() {
        var json = com.google.gson.JsonParser.parseString(
                "{\"type\":\"sandstorm\",\"remaining_ticks\":10,\"duration_ticks\":20}");
        assertEquals(new RingWeatherState(RingWeatherType.SANDSTORM, 10, 20, 0),
                RingWeatherState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void streamCodecRoundTrip() {
        for (RingWeatherState state : new RingWeatherState[]{RingWeatherState.STILL,
                new RingWeatherState(RingWeatherType.REVERSE_RAIN, 1, 24_000, Integer.MIN_VALUE),
                new RingWeatherState(RingWeatherType.CLEAR, 180_000, 180_000, 0)}) {
            ByteBuf buffer = Unpooled.buffer();
            RingWeatherState.STREAM_CODEC.encode(buffer, state);
            assertEquals(state, RingWeatherState.STREAM_CODEC.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        }
    }

    private static RingWeatherState at(RingWeatherType type, int remaining, int duration) {
        return new RingWeatherState(type, remaining, duration, 0);
    }
}
