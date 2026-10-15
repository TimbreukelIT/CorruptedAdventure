package nl.timtoys.corruptedadventure.worldgen.feature;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The column scan of the climbing strands (verdant_falls): ledges with their gaps, top down. */
class LedgeStrandsTest {
    /** A column of solid blocks except at {@code air}; {@code soft} blocks are not air but have no sturdy underside. */
    private static List<LedgeStrands.Overhang> scan(int top, int minY, Set<Integer> air, Set<Integer> soft) {
        return LedgeStrands.overhangs(top, minY, air::contains, y -> !soft.contains(y));
    }

    private static Set<Integer> range(int from, int to) {
        return java.util.stream.IntStream.rangeClosed(from, to).boxed().collect(java.util.stream.Collectors.toSet());
    }

    @Test
    void stackedLedgesEachWithTheirGap() {
        // Top 100; air 90–95 under the first ledge (96), 80–84 under the second (85), then solid down to 63.
        Set<Integer> air = new java.util.HashSet<>(range(90, 95));
        air.addAll(range(80, 84));
        List<LedgeStrands.Overhang> found = scan(100, 63, air, Set.of());
        assertEquals(List.of(new LedgeStrands.Overhang(96, 89), new LedgeStrands.Overhang(85, 79)), found);
        assertEquals(6, found.get(0).gap());
        assertEquals(5, found.get(1).gap());
    }

    @Test
    void aGapBelowTheLowestYHasNoFloor() {
        List<LedgeStrands.Overhang> found = scan(100, 63, range(50, 95), Set.of());
        assertEquals(List.of(new LedgeStrands.Overhang(96, LedgeStrands.NO_FLOOR)), found);
        assertEquals(0, found.get(0).gap());
    }

    @Test
    void noLedgeUnderASoftBlockAndNoneInTheOpenAboveTheTop() {
        // A strand block (95) with air below is no ledge; the sturdy block above it is no ledge either (no air right below).
        assertEquals(List.of(), scan(100, 63, range(90, 94), Set.of(95)));
        // The top itself is air (the scan starts in the open): the first block below with air under it is a ledge.
        Set<Integer> air = new java.util.HashSet<>(range(98, 100));
        air.addAll(range(90, 96));
        assertEquals(List.of(new LedgeStrands.Overhang(97, 89)), scan(100, 63, air, Set.of()));
    }

    @Test
    void aSolidColumnHasNoLedge() {
        assertTrue(scan(100, 63, Set.of(), Set.of()).isEmpty());
    }

    @Test
    void configurationRejectsAnEmptyGapRangeAndTooMuchChance() {
        String strand = "{\"Name\": \"minecraft:hanging_roots\", \"Properties\": {\"waterlogged\": \"false\"}}";
        String ok = "{\"strand\": " + strand + ", \"accent\": " + strand + ", \"strand_chance\": 0.5, \"accent_chance\": 0.2,"
                + " \"min_gap\": 3, \"max_gap\": 24, \"reach\": 4}";
        assertTrue(LedgeStrandConfiguration.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(ok)).result().isPresent());
        assertTrue(LedgeStrandConfiguration.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString(ok.replace("\"min_gap\": 3", "\"min_gap\": 30"))).error().isPresent());
        assertTrue(LedgeStrandConfiguration.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString(ok.replace("0.2", "0.6"))).error().isPresent());
    }
}
