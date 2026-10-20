package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.Ring4Band;
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
                // 74: a world tree's half torn-out island.
                case IslandShape.TREE -> assertTrue(diameter >= WorldTrees.ISLAND_MIN - 1e-9
                        && diameter <= WorldTrees.ISLAND_MIN + WorldTrees.ISLAND_SPREAD + 1e-9, "tree " + diameter);
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
                // 74: a sky lake's basin lowers the top at the centre. The dome (topAt) sinks a hair within the centre's
                // block: where the top lies just over a whole block, the block below is the column's highest.
                boolean hair = s.top() - Math.floor(s.top()) < 0.05 && field.thin(x, s.highest() - 1, z) > 0.0;
                assertTrue(field.thin(x, s.highest(), z) > 0.0 || hair || c.springY() == s.highest()
                        || c.skyLakeFloor() != Integer.MIN_VALUE, "island block at the top");
                // 74: a world tree's trunk stands on its island.
                if (field.islandsAt(x, z).size() == 1 && s.kind() != IslandShape.TREE) {
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
            assertTrue(details.lakeTop() > 63, "a lake above the sea rises above Y 62");
            double radius = s.holeRadius();
            double reach = s.reach() * IslandShape.HOLE_SCALE + 20.0;
            for (int x = (int) Math.floor((s.x() - reach) / 4) * 4; x <= s.x() + reach; x += 4) {
                for (int z = (int) Math.floor((s.z() - reach) / 4) * 4; z <= s.z() + reach; z += 4) {
                    double dh = s.holeDistance(x, z);
                    if (dh >= 1.0 && dh <= 1.0 + 14.0 / radius) {
                        assertTrue(field.surfaceUncached(x, z) >= details.lakeTop(), "rim " + x + ", " + z);
                    }
                }
            }
        }
        System.out.println("lakes above the sea: " + lakes);
    }

    /**
     * Decor rollout C: about four in ten clods of the mythical band pour a small fall; the blighted band's stay dry. A
     * clod's fall never pushes a bigger island's fall out of its chunk's cap, nor stands near a lift.
     */
    @Test
    void mythicalClodsPourSmallFalls() {
        Ring4Band band = Ring4Band.of(IslandFieldTest.LAYOUT);
        int mythical = 0;
        int pouring = 0;
        List<IslandField.Fall> clodFalls = new ArrayList<>();
        for (IslandShape s : SHAPES) {
            if (s.kind() != IslandShape.CLOD) {
                continue;
            }
            List<IslandField.Fall> falls = field.details(s).falls();
            assertTrue(falls.size() <= 1, "a clod pours at most one fall");
            clodFalls.addAll(falls);
            if (band.blighted(s.x(), s.z())) {
                assertEquals(0, falls.size(), "a blighted clod stays dry");
            } else {
                mythical++;
                pouring += falls.size();
            }
        }
        System.out.printf("mythical clods %d, with a fall %d%n", mythical, pouring);
        assertTrue(pouring > 0.1 * mythical && pouring <= 0.6 * mythical, pouring + " of " + mythical);
        for (IslandShape s : SHAPES) {
            if (s.kind() == IslandShape.CLOD) {
                continue;
            }
            IslandField.Details details = field.details(s);
            for (IslandField.Fall fall : details.falls()) {
                List<IslandField.Fall> kept = field.fallsOfChunk(fall.fallX() >> 4, fall.fallZ() >> 4);
                assertTrue(kept.contains(fall) || kept.stream().noneMatch(clodFalls::contains), "pushed out by a clod's fall: " + fall);
            }
            IslandField.Lift lift = details.lift();
            if (lift != null) {
                assertTrue(clodFalls.stream().noneMatch(f -> Math.abs(f.fallX() - lift.x()) + Math.abs(f.fallZ() - lift.z())
                        < IslandField.LIFT_FALL_CLEARANCE), "a clod's fall beside " + lift);
            }
        }
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
                // The fall lands in the water: the ground lies under the water's top block (fall.bottom() − 1). The lowest
                // open ground, as the fall's own check (surfaceUncached): a peat sinkhole's bowl may open the column deeper.
                assertTrue(column.lowestSurface() < fall.bottom() - 1.0, "lands in water: " + column.lowestSurface() + " " + fall);
                List<IslandField.Waterfall> listed = field.waterfallsWithin(fall.fallX(), fall.fallZ(), fall.fallX(), fall.fallZ());
                assertEquals(1, listed.size());
            }
        }
        System.out.printf("falls %d, lifts %d%n", falls, lifts);
        assertTrue(falls > 10, "falls " + falls);
        assertTrue(lifts > 2, "lifts " + lifts);
        perChunk.values().forEach(n -> assertTrue(n <= IslandField.FALLS_PER_CHUNK));
    }

    /**
     * Lift package: every lift away from the falls is listed by its own chunk and its own column (the feature fills it
     * there); the reach filter of {@link IslandField#liftsWithin} drops none against a scan of every island around the
     * chunk; no listed lift stands near a fall.
     */
    @Test
    void everyLiftIsListedByItsOwnChunk() {
        int lifts = 0;
        int nearFalls = 0;
        for (IslandShape s : SHAPES) {
            IslandField.Lift lift = field.details(s).lift();
            if (lift == null) {
                continue;
            }
            lifts++;
            int minX = lift.x() & ~15;
            int minZ = lift.z() & ~15;
            List<IslandField.Lift> listed = field.liftsWithin(minX, minZ, minX + 15, minZ + 15);
            boolean nearFall = field.waterfallsWithin(lift.x() - 5, lift.z() - 5, lift.x() + 5, lift.z() + 5).stream()
                    .anyMatch(f -> Math.abs(f.x() - lift.x()) + Math.abs(f.z() - lift.z()) < IslandField.LIFT_FALL_CLEARANCE);
            nearFalls += nearFall ? 1 : 0;
            assertEquals(!nearFall, listed.contains(lift), "own chunk " + lift);
            assertEquals(!nearFall, field.liftsWithin(lift.x(), lift.z(), lift.x(), lift.z()).contains(lift), "own column " + lift);
            assertTrue(lift.top() - lift.bottom() >= 3, "a column to ride " + lift);
            // Every island the chunk could see, without the reach filter.
            for (IslandShape other : field.shapesWithin(minX, minZ, minX + 15, minZ + 15)) {
                IslandField.Lift theirs = field.details(other).lift();
                if (theirs != null && theirs.x() >> 4 == minX >> 4 && theirs.z() >> 4 == minZ >> 4
                        && field.waterfallsWithin(theirs.x() - 5, theirs.z() - 5, theirs.x() + 5, theirs.z() + 5).stream()
                        .noneMatch(f -> Math.abs(f.x() - theirs.x()) + Math.abs(f.z() - theirs.z()) < IslandField.LIFT_FALL_CLEARANCE)) {
                    assertTrue(listed.contains(theirs), "filtered away " + theirs);
                }
            }
        }
        System.out.printf("lifts %d, of which near a fall %d%n", lifts, nearFalls);
        assertTrue(lifts - nearFalls > 2, "lifts " + lifts + ", near falls " + nearFalls);
    }

    /** Review 61 #4: no two islands grow into each other, also across cells (apart, or stacked 6 blocks apart). */
    @Test
    void noIslandsGrowIntoEachOther() {
        assertNoOverlap(SHAPES);
        // Review 61b #4: on other seeds too (the outer band, where giants and archipelagos meet across cells).
        for (long seed : new long[]{1L, 42L, 987654321L}) {
            IslandField other = IslandFieldTest.field(seed);
            List<IslandShape> shapes = new ArrayList<>();
            for (int i = 7200 / cellSize; i <= 8000 / cellSize; i++) {
                for (int j = -1000 / cellSize; j <= 1000 / cellSize; j++) {
                    shapes.addAll(other.cell(i, j));
                }
            }
            assertTrue(shapes.size() > 50, "seed " + seed + ": " + shapes.size());
            assertNoOverlap(shapes);
        }
    }

    private static void assertNoOverlap(List<IslandShape> shapes) {
        for (int a = 0; a < shapes.size(); a++) {
            for (int b = a + 1; b < shapes.size(); b++) {
                IslandShape s = shapes.get(a);
                IslandShape t = shapes.get(b);
                boolean apart = Math.hypot(s.x() - t.x(), s.z() - t.z()) > s.reach() + t.reach();
                boolean stacked = s.lowest() > t.highest() + 6 || t.lowest() > s.highest() + 6;
                assertTrue(apart || stacked, s + " / " + t);
            }
        }
    }

    /** Review 61b #3: a lake above the sea only in a hole no other island's hole comes near (no lake over a sea hole). */
    @Test
    void lakesAboveTheSeaLieAlone() {
        int lakes = 0;
        for (IslandShape s : SHAPES) {
            if (field.details(s).lakeTop() == Integer.MIN_VALUE) {
                continue;
            }
            lakes++;
            double own = s.reach() * IslandShape.HOLE_SCALE + IslandShape.CAVE_BAND;
            for (IslandShape t : SHAPES) {
                if (!t.equals(s)) {
                    assertTrue(Math.hypot(t.x() - s.x(), t.z() - s.z()) >= own + t.reach() * IslandShape.HOLE_SCALE + 2.0, s + " / " + t);
                }
            }
        }
        assertTrue(lakes > 0, "no lake above the sea");
    }

    /**
     * Review 61 #1 and #3: a fall into the sea starts right above its top block (Y 62, the fall's bottom 63), and no
     * column inside a hole holds a lake higher than that hole's own.
     */
    @Test
    void seaFallsStartRightAboveTheSeaAndLakesStayInTheirOwnHole() {
        int seaFalls = 0;
        int checked = 0;
        for (IslandShape s : SHAPES) {
            IslandField.Details details = field.details(s);
            for (IslandField.Fall fall : details.falls()) {
                if (field.islandColumn(fall.fallX(), fall.fallZ()).lakeTop() == Integer.MIN_VALUE) {
                    seaFalls++;
                    assertEquals(63, fall.bottom(), fall.toString());
                }
            }
            double reach = s.reach() * IslandShape.HOLE_SCALE;
            for (int x = (int) Math.floor(s.x() - reach); x <= s.x() + reach; x += 3) {
                for (int z = (int) Math.floor(s.z() - reach); z <= s.z() + reach; z += 3) {
                    if (s.holeDistance(x, z) < 1.0) {
                        int lake = field.islandColumn(x, z).lakeTop();
                        checked++;
                        assertTrue(details.lakeTop() == Integer.MIN_VALUE ? lake == Integer.MIN_VALUE || lake <= 64 + 200
                                : lake <= details.lakeTop(), x + ", " + z + ": " + lake + " in a hole of " + details.lakeTop());
                        if (details.lakeTop() == Integer.MIN_VALUE) {
                            assertEquals(Integer.MIN_VALUE, lake, "a hole the sea fills has no lake: " + x + ", " + z);
                        }
                    }
                }
            }
        }
        assertTrue(seaFalls > 10, "sea falls " + seaFalls);
        assertTrue(checked > 1000);
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
