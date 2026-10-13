package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The islands of ring 4 (package 61) on the test seed: placement, mirror holes, lakes, falls and lifts. */
class Ring4IslandsTest {
    private static IslandField field;
    private static final List<IslandShape> SHAPES = new ArrayList<>();
    private static int cellSize;

    @BeforeAll
    static void place() {
        field = IslandFieldTest.field();
        cellSize = field.settings().cellSize();
        // A wedge across the whole ring, both sides of the x axis.
        for (int i = 5900 / cellSize; i <= 8100 / cellSize; i++) {
            for (int j = -1400 / cellSize; j <= 1400 / cellSize; j++) {
                SHAPES.addAll(field.cell(i, j));
            }
        }
    }

    private static double progress(IslandShape s) {
        double d = IslandFieldTest.LAYOUT.effectiveDistance(s.x(), s.z());
        return (d - 6000.0) / 2000.0;
    }

    @Test
    void moreBiggerAndHigherIslandsTowardsRing5() {
        int inner = 0;
        int outer = 0;
        double innerTop = 0.0;
        double outerTop = 0.0;
        int giants = 0;
        for (IslandShape s : SHAPES) {
            double p = progress(s);
            if (p < 0.35) {
                inner++;
                innerTop = Math.max(innerTop, s.top());
            } else if (p > 0.65) {
                outer++;
                outerTop = Math.max(outerTop, s.top());
            }
            giants += s.kind() == IslandShape.GIANT ? 1 : 0;
        }
        System.out.printf("islands: %d (inner third %d, outer third %d, giants %d), highest top inner %.0f, outer %.0f%n",
                SHAPES.size(), inner, outer, giants, innerTop, outerTop);
        assertTrue(outer > 3 * inner, inner + " inner, " + outer + " outer");
        assertTrue(inner > 0);
        assertTrue(giants > 0);
        assertTrue(outerTop > innerTop + 40.0, innerTop + " / " + outerTop);
    }

    /** Sizes as diameters (Q4 = A); each island with its hole, lake and outlet within one cell (review 59 #6). */
    @Test
    void sizesAndReachFitTheGrid() {
        for (IslandShape s : SHAPES) {
            double diameter = 2.0 * s.a() * Math.sqrt(s.stretch());
            switch (s.kind()) {
                case IslandShape.CLOD -> assertTrue(diameter >= 5.0 - 1e-9 && diameter <= 15.0 + 1e-9, "clod " + diameter);
                case IslandShape.MIDDLE -> assertTrue(diameter >= 20.0 - 1e-9 && diameter <= 60.0 + 1e-9, "middle " + diameter);
                default -> assertTrue(diameter >= 80.0 - 1e-9 && diameter <= 150.0 + 1e-9, "giant " + diameter);
            }
            assertTrue(s.groundReach() <= cellSize, "reach " + s.groundReach());
            assertTrue(s.view().radius() <= cellSize);
            assertTrue(s.top() <= IslandPlacement.MAX_TOP);
        }
    }

    /** Only in ring 4 proper; low islands on the inside: bottom 15–40 above the ground, tops around Y 90–130. */
    @Test
    void islandsHangFreeAboveTheirGround() {
        int inner = 0;
        for (IslandShape s : SHAPES) {
            RingPosition position = IslandFieldTest.LAYOUT.sampleOverworld(s.x(), s.z());
            assertTrue(!position.inTransition() && position.ring() == Ring.RING_4);
            double bottom = s.top() - 1.0 - s.thickness() * IslandShape.MAX_UNDERSIDE;
            assertTrue(bottom >= Math.max(63.0, s.groundY()) + IslandPlacement.MIN_CLEARANCE - 1e-9, "bottom " + bottom);
            if (progress(s) < 0.15) {
                inner++;
                assertTrue(bottom <= Math.max(63.0, s.groundY()) + 70.0, "inner bottom " + bottom + " over " + s.groundY());
                assertTrue(s.top() < 175.0, "inner top " + s.top());
            }
        }
        assertTrue(inner > 0);
    }

    /** Under every island its hole: the surface under the centre lies below the ground it was torn from. */
    @Test
    void everyIslandHasItsMirrorHoleUnderIt() {
        for (IslandShape s : SHAPES) {
            int x = (int) Math.floor(s.x());
            int z = (int) Math.floor(s.z());
            IslandField.Column c = field.column(x, z);
            assertTrue(c.surface() <= s.groundY() - Math.min(IslandShape.EDGE_DEPTH, 0.25 * s.thickness()) + 1e-9,
                    "hole at " + x + ", " + z + ": " + c.surface() + " vs " + s.groundY());
            assertTrue(c.islandHole() > 0.0);
            if (s.kind() != IslandShape.CLOD) {
                assertTrue(c.islandMask() > 0.0);
                assertTrue(field.thin(x, s.highest(), z) > 0.0 || c.springY() == s.highest(), "island block at the top");
                if (field.islandsAt(x, z).size() == 1) {
                    assertEquals(0.0, field.thin(x, s.highest() + 2, z));
                }
            }
        }
    }

    /**
     * A lake above the sea stays under its rim: every column of the 4-block grid just outside its band lies at least at
     * its top, so no open space beside it is below its water.
     */
    @Test
    void lakesAboveTheSeaStayUnderTheirRim() {
        int lakes = 0;
        for (IslandShape s : SHAPES) {
            IslandField.Details details = field.details(s);
            if (details.lakeTop() == Integer.MIN_VALUE) {
                continue;
            }
            lakes++;
            assertTrue(details.lakeTop() > 64);
            double radius = s.holeRadius();
            double reach = s.reach() * IslandShape.HOLE_SCALE + 20.0;
            for (int x = (int) Math.floor((s.x() - reach) / 4) * 4; x <= s.x() + reach; x += 4) {
                for (int z = (int) Math.floor((s.z() - reach) / 4) * 4; z <= s.z() + reach; z += 4) {
                    double dh = s.holeDistance(x, z);
                    if (dh >= 1.0 + 2.0 / radius && dh <= 1.0 + 14.0 / radius) {
                        assertTrue(field.surfaceUncached(x, z) >= details.lakeTop(), "rim " + x + ", " + z);
                    }
                }
            }
        }
        System.out.println("lakes above the sea: " + lakes);
    }

    /** Falls: a spring walled in by the island, the fall beside it free down to the water, at most 3 per chunk. */
    @Test
    void fallsPourFromSpringsIntoTheWater() {
        int falls = 0;
        int lifts = 0;
        Map<Long, Integer> perChunk = new HashMap<>();
        for (IslandShape s : SHAPES) {
            IslandField.Details details = field.details(s);
            lifts += details.lift() == null ? 0 : 1;
            if (details.lift() != null) {
                IslandField.Lift lift = details.lift();
                assertTrue(lift.top() > s.lowest());
                for (int y = lift.bottom() + 1; y <= lift.top(); y++) {
                    assertEquals(0.0, field.thin(lift.x(), y, lift.z()), "lift column " + lift);
                }
            }
            for (IslandField.Fall fall : details.falls()) {
                if (!field.fallsOfChunk(fall.fallX() >> 4, fall.fallZ() >> 4).contains(fall)) {
                    continue;
                }
                falls++;
                perChunk.merge((long) (fall.fallX() >> 4) << 32 | (fall.fallZ() >> 4) & 0xFFFFFFFFL, 1, Integer::sum);
                assertEquals(-1.0, field.thin(fall.springX(), fall.springY(), fall.springZ()), "spring " + fall);
                assertEquals(1.0, field.thin(fall.springX(), fall.springY() - 1, fall.springZ()));
                assertEquals(1.0, field.thin(fall.springX(), fall.springY() + 1, fall.springZ()));
                for (int y = fall.bottom(); y <= fall.springY(); y++) {
                    assertTrue(field.thin(fall.fallX(), y, fall.fallZ()) <= 0.0, "fall column " + fall + " at " + y);
                }
                IslandField.Column column = field.column(fall.fallX(), fall.fallZ());
                assertEquals(fall.springY(), column.fallTop());
                assertEquals(fall.bottom(), column.fallBottom());
                assertTrue(column.surface() <= fall.bottom() - 2.0, "lands in water: " + column.surface() + " " + fall);
                List<IslandField.Waterfall> listed = field.waterfallsWithin(fall.fallX(), fall.fallZ(), fall.fallX(), fall.fallZ());
                assertEquals(1, listed.size());
            }
        }
        System.out.printf("falls %d, lifts %d%n", falls, lifts);
        assertTrue(falls > 10, "falls " + falls);
        assertTrue(lifts > 2, "lifts " + lifts);
        perChunk.values().forEach(n -> assertTrue(n <= IslandField.FALLS_PER_CHUNK));
    }

    /** The public view: a footprint covering the island, its lowest and highest block. */
    @Test
    void publicViewCoversTheIsland() {
        IslandShape s = SHAPES.stream().filter(shape -> shape.kind() == IslandShape.MIDDLE).findFirst().orElseThrow();
        IslandField.Island island = field.islandAt((int) Math.floor(s.x()), (int) Math.floor(s.z()));
        assertNotNull(island);
        for (int dx = -40; dx <= 40; dx++) {
            for (int dz = -40; dz <= 40; dz++) {
                double px = Math.floor(s.x()) + dx + 0.5;
                double pz = Math.floor(s.z()) + dz + 0.5;
                if (s.interval(px, pz) != null) {
                    assertTrue(s.view().covers((int) Math.floor(px), (int) Math.floor(pz)));
                    int[] interval = s.interval(px, pz);
                    assertTrue(interval[0] >= s.view().bottom() && interval[1] <= s.view().top());
                }
            }
        }
    }
}
