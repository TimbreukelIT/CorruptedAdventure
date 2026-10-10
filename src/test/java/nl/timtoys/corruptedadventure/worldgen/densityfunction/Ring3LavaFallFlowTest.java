package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 47e on the test seed: where the lava of a fall may flow ({@link LavaFallFlow}), so the ash drifts stay out of it and a
 * fall that may reach water is not made. The trace is pure: the same in a fresh field, on two threads, in any order.
 */
class Ring3LavaFallFlowTest {
    private static VolcanicField field;
    private static List<Volcano.LavaFall> falls;

    @BeforeAll
    static void setUp() {
        field = VolcanicFieldTest.field();
        Map<Long, Volcano.LavaFall> found = new LinkedHashMap<>();
        int size = field.settings().cellSize();
        for (int i = -6200 / size - 1; i <= 6200 / size; i++) {
            for (int j = -6200 / size - 1; j <= 6200 / size; j++) {
                Volcano v = field.volcano(i, j);
                if (v != null) {
                    v.islands().forEach(island -> island.falls().forEach(f -> found.putIfAbsent(LavaFallFlow.key(f.fallX(), f.fallZ()), f)));
                }
            }
        }
        for (int i = -6200 / VolcanoPlacement.ISLAND_CELL - 1; i <= 6200 / VolcanoPlacement.ISLAND_CELL; i++) {
            for (int j = -6200 / VolcanoPlacement.ISLAND_CELL - 1; j <= 6200 / VolcanoPlacement.ISLAND_CELL; j++) {
                field.channelIslands(i, j).forEach(island -> island.falls().forEach(f -> found.putIfAbsent(LavaFallFlow.key(f.fallX(), f.fallZ()), f)));
            }
        }
        for (int i = -6200 / Landmarks.FLOATING_CELL - 1; i <= 6200 / Landmarks.FLOATING_CELL; i++) {
            for (int j = -6200 / Landmarks.FLOATING_CELL - 1; j <= 6200 / Landmarks.FLOATING_CELL; j++) {
                Landmarks.FloatingVolcano fv = field.floatingVolcano(i, j);
                if (fv != null) {
                    fv.falls().forEach(f -> found.putIfAbsent(LavaFallFlow.key(f.fallX(), f.fallZ()), f));
                }
            }
        }
        falls = new ArrayList<>(found.values());
    }

    @Test
    void everyFallThatIsMadeHasAFlowAroundItsFoot() {
        int made = 0;
        int wet = 0;
        int cut = 0;
        int largest = 0;
        for (Volcano.LavaFall fall : falls) {
            LavaFallFlow.Flow flow = field.fallFlow(fall);
            if (flow.isEmpty()) {
                continue;
            }
            made++;
            assertTrue(flow.contains(fall.fallX(), fall.fallZ()), "the flow starts at the fall's own column");
            wet += flow.wet() ? 1 : 0;
            cut += flow.cut() ? 1 : 0;
            largest = Math.max(largest, flow.columns().length);
        }
        System.out.println("47e: falls " + falls.size() + ", made " + made + ", wet " + wet + ", cut " + cut + ", largest flow " + largest);
        assertTrue(made > 20, "falls made " + made);
        assertEquals(0, cut, "a flow reaches past LavaFallFlow.REACH");
    }

    @Test
    void theAshLayerUnderTheArchipelagoFallOfIntAfrLiesInAFlow() {
        // INT-afr (afr-det-a vs afr-det-b): an ash layer at (2.628, 116, -2.654) on the slope under a fall of the
        // archipelago was taken by the flowing lava in one world only.
        List<LavaFallFlow.Flow> near = field.fallFlowsNear(2620, -2660, 2635, -2645);
        assertFalse(near.isEmpty(), "a fall flows near the archipelago's foot");
        assertTrue(near.stream().anyMatch(f -> f.contains(2628, -2654)), "the ash layer's column lies in a flow");
    }

    @Test
    void flowsArePureWhateverTheThreadOrOrder() throws Exception {
        List<Volcano.LavaFall> sample = falls.subList(0, Math.min(40, falls.size()));
        List<Volcano.LavaFall> reversed = new ArrayList<>(sample);
        java.util.Collections.reverse(reversed);
        VolcanicField a = VolcanicFieldTest.field();
        VolcanicField b = VolcanicFieldTest.field();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Map<Long, LavaFallFlow.Flow>> forward = pool.submit(() -> traceAll(a, sample));
            Future<Map<Long, LavaFallFlow.Flow>> backward = pool.submit(() -> traceAll(b, reversed));
            Map<Long, LavaFallFlow.Flow> fa = forward.get();
            Map<Long, LavaFallFlow.Flow> fb = backward.get();
            for (Volcano.LavaFall fall : sample) {
                long key = LavaFallFlow.key(fall.fallX(), fall.fallZ());
                LavaFallFlow.Flow x = fa.get(key);
                LavaFallFlow.Flow y = fb.get(key);
                assertArrayEquals(x.columns(), y.columns(), "flow of the fall at " + fall);
                assertEquals(x.wet(), y.wet());
                // And the same as the shared field's.
                assertTrue(Arrays.equals(x.columns(), field.fallFlow(fall).columns()));
            }
        } finally {
            pool.shutdown();
        }
    }

    private static Map<Long, LavaFallFlow.Flow> traceAll(VolcanicField f, List<Volcano.LavaFall> list) {
        Map<Long, LavaFallFlow.Flow> out = new LinkedHashMap<>();
        for (Volcano.LavaFall fall : list) {
            out.put(LavaFallFlow.key(fall.fallX(), fall.fallZ()), f.fallFlow(fall));
        }
        return out;
    }
}
