package nl.timtoys.corruptedadventure.worldgen.feature;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.level.Level;
import nl.timtoys.corruptedadventure.config.RingSettings;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RingChanceFilterTest {
    private static final List<Integer> RADII = List.of(1500, 3500, 5500, 7500, 9500);
    private static final RingLayout LAYOUT = new RingLayout(new RingSettings(RADII, 10000, 96, 128), 12345L);
    private static final int REACH = 96 + 64;
    private static final double EPSILON = 1e-9;

    @Test
    void curveRunsFromTheFirstToTheSecondChance() {
        assertEquals(0.2, RingChanceFilter.curve(0.2, 1.0, 0.0, 1.0, 0.0), EPSILON);
        assertEquals(0.6, RingChanceFilter.curve(0.2, 1.0, 0.0, 1.0, 0.5), EPSILON);
        assertEquals(1.0, RingChanceFilter.curve(0.2, 1.0, 0.0, 1.0, 1.0), EPSILON);
        // Decreasing.
        assertEquals(0.8, RingChanceFilter.curve(1.0, 0.2, 0.0, 1.0, 0.25), EPSILON);
    }

    @Test
    void curveIsZeroBeforeStartAndStretchedAfterIt() {
        assertEquals(0.0, RingChanceFilter.curve(0.4, 1.0, 0.5, 1.0, 0.49), EPSILON);
        assertEquals(0.4, RingChanceFilter.curve(0.4, 1.0, 0.5, 1.0, 0.5), EPSILON);
        assertEquals(0.7, RingChanceFilter.curve(0.4, 1.0, 0.5, 1.0, 0.75), EPSILON);
    }

    @Test
    void exponentBendsTheCurve() {
        assertEquals(0.25, RingChanceFilter.curve(0.0, 1.0, 0.0, 2.0, 0.5), EPSILON);
        assertEquals(Math.sqrt(0.5), RingChanceFilter.curve(0.0, 1.0, 0.0, 0.5, 0.5), EPSILON);
    }

    @Test
    void transitionSignalIsTheBlendOfItsOwnBoundaryOnly() {
        assertEquals(0.3, RingChanceFilter.transition(Ring.RING_1, new RingPosition(Ring.RING_1, Ring.RING_2, 0.3)).orElseThrow(), EPSILON);
        assertTrue(RingChanceFilter.transition(Ring.RING_1, RingPosition.within(Ring.RING_1)).isEmpty());
        assertTrue(RingChanceFilter.transition(Ring.RING_1, RingPosition.within(Ring.RING_2)).isEmpty());
        assertTrue(RingChanceFilter.transition(Ring.RING_1, new RingPosition(Ring.RING_2, Ring.RING_3, 0.3)).isEmpty());
    }

    @Test
    void chanceFollowsTheBlendInTheZoneAndIsZeroElsewhere() {
        RingChanceFilter linear = RingChanceFilter.transition(Ring.RING_1, 0.0, 1.0);
        for (int angle = 0; angle < 360; angle += 15) {
            boolean sawZone = false;
            for (int distance = 0; distance <= RADII.get(1) + REACH; distance += 8) {
                int x = (int) Math.floor(distance * Math.cos(Math.toRadians(angle)));
                int z = (int) Math.floor(distance * Math.sin(Math.toRadians(angle)));
                RingPosition position = LAYOUT.sample(Level.OVERWORLD, x, z);
                boolean inZone = position.inTransition() && position.inner() == Ring.RING_1;
                double expected = inZone ? position.blend() : 0.0;
                assertEquals(expected, linear.chance(LAYOUT, Level.OVERWORLD, x, z), EPSILON, "angle " + angle + " at " + distance);
                sawZone |= inZone;
            }
            assertTrue(sawZone, "no zone of boundary 1 at angle " + angle);
        }
    }

    @Test
    void progressSignalRunsAcrossRingOneAndIsZeroBeyondIt() {
        RingChanceFilter linear = RingChanceFilter.progress(Ring.RING_1, 0.0, 1.0, 0.0, 1.0);
        assertEquals(0.0, linear.chance(LAYOUT, Level.OVERWORLD, 0, 0), EPSILON);
        for (int angle = 0; angle < 360; angle += 15) {
            double radians = Math.toRadians(angle);
            double previous = 0.0;
            double last = 0.0;
            boolean leftRing = false;
            for (int distance = 0; distance <= RADII.get(0) + REACH; distance += 4) {
                int x = (int) Math.floor(distance * Math.cos(radians));
                int z = (int) Math.floor(distance * Math.sin(radians));
                double chance = linear.chance(LAYOUT, Level.OVERWORLD, x, z);
                if (LAYOUT.sample(Level.OVERWORLD, x, z).ring() == Ring.RING_1) {
                    // The boundary noise can be locally steeper than the ray, so allow small dips.
                    assertTrue(chance > previous - 0.02, "chance " + chance + " at " + distance + ", angle " + angle);
                    previous = Math.max(previous, chance);
                    last = chance;
                } else {
                    leftRing = true;
                    assertEquals(0.0, chance, EPSILON, "beyond ring 1 at " + distance + ", angle " + angle);
                }
            }
            assertTrue(leftRing, "never left ring 1 at angle " + angle);
            assertTrue(last > 0.98, "chance near the edge only " + last + " at angle " + angle);
        }
        // Deep in ring 2 there is no ring 1 progress.
        assertEquals(0.0, linear.chance(LAYOUT, Level.OVERWORLD, RADII.get(0) + REACH + 100, 0), EPSILON);
    }

    @Test
    void progressShapeIsZeroBeforeStartAndReachesTheSecondChanceAtTheEdge() {
        RingChanceFilter shaped = RingChanceFilter.progress(Ring.RING_1, 0.0, 0.12, 0.33, 2.0);
        RingChanceFilter linear = RingChanceFilter.progress(Ring.RING_1, 0.0, 1.0, 0.0, 1.0);
        for (int x = 0; x < RADII.get(0) - REACH; x += 25) {
            double t = linear.chance(LAYOUT, Level.OVERWORLD, x, 0);
            double chance = shaped.chance(LAYOUT, Level.OVERWORLD, x, 0);
            assertEquals(RingChanceFilter.curve(0.0, 0.12, 0.33, 2.0, t), chance, EPSILON);
            if (t < 0.33) {
                assertEquals(0.0, chance, EPSILON);
            }
        }
        assertEquals(0.12, RingChanceFilter.curve(0.0, 0.12, 0.33, 2.0, 1.0), EPSILON);
    }

    @Test
    void progressSignalScalesTheNether() {
        RingChanceFilter linear = RingChanceFilter.progress(Ring.RING_1, 0.0, 1.0, 0.0, 1.0);
        for (int x = -400; x <= 400; x += 37) {
            for (int z = -400; z <= 400; z += 41) {
                assertEquals(linear.chance(LAYOUT, Level.OVERWORLD, x * 4, z * 4), linear.chance(LAYOUT, Level.NETHER, x, z), EPSILON);
            }
        }
    }

    @Test
    void codecRoundTripsAndFillsDefaults() {
        String json = """
                {"signal": "transition", "ring": "ring_1", "from_chance": 1.0, "to_chance": 0.2}""";
        RingChanceFilter filter = decode(json).getOrThrow();
        JsonElement encoded = RingChanceFilter.CODEC.codec().encodeStart(JsonOps.INSTANCE, filter).getOrThrow();
        assertEquals(JsonParser.parseString(json), encoded);

        RingChanceFilter shaped = decode("""
                {"signal": "transition", "ring": "ring_1", "from_chance": 0.0, "to_chance": 1.0, "start": 0.5, "exponent": 2.0}""").getOrThrow();
        JsonElement shapedJson = RingChanceFilter.CODEC.codec().encodeStart(JsonOps.INSTANCE, shaped).getOrThrow();
        assertEquals(0.5, shapedJson.getAsJsonObject().get("start").getAsDouble(), EPSILON);
        assertEquals(2.0, shapedJson.getAsJsonObject().get("exponent").getAsDouble(), EPSILON);

        String progress = """
                {"signal": "progress", "ring": "ring_1", "from_chance": 0.0, "to_chance": 0.12, "start": 0.33, "exponent": 2.0}""";
        JsonElement progressJson = RingChanceFilter.CODEC.codec().encodeStart(JsonOps.INSTANCE, decode(progress).getOrThrow()).getOrThrow();
        assertEquals(JsonParser.parseString(progress), progressJson);
    }

    @Test
    void codecRejectsInvalidShapes() {
        assertTrue(decode("""
                {"signal": "transition", "ring": "void", "from_chance": 0.0, "to_chance": 1.0}""").isError());
        assertTrue(decode("""
                {"signal": "progress", "ring": "void", "from_chance": 0.0, "to_chance": 1.0}""").isError());
        assertTrue(decode("""
                {"signal": "transition", "ring": "ring_1", "from_chance": 0.0, "to_chance": 1.5}""").isError());
        assertTrue(decode("""
                {"signal": "transition", "ring": "ring_1", "from_chance": 0.0, "to_chance": 1.0, "start": 1.0}""").isError());
        assertTrue(decode("""
                {"signal": "transition", "ring": "ring_1", "from_chance": 0.0, "to_chance": 1.0, "exponent": 0.0}""").isError());
        assertTrue(decode("""
                {"signal": "nonsense", "ring": "ring_1", "from_chance": 0.0, "to_chance": 1.0}""").isError());
    }

    private static DataResult<RingChanceFilter> decode(String json) {
        return RingChanceFilter.CODEC.codec().parse(JsonOps.INSTANCE, JsonParser.parseString(json));
    }
}
