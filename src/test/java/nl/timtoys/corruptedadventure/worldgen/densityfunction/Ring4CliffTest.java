package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The cliff 3→4 (package 62) on the test seed: a wall 40–80 high at the zone's outer edge, gaps, steaming falls. */
class Ring4CliffTest {
    private static final IslandField FIELD = IslandFieldTest.field();
    private static final RingLayout LAYOUT = IslandFieldTest.LAYOUT;

    /** The block on the ray at {@code angle} where the effective distance reaches {@code line + offset}. */
    private static int[] at(double angle, double offset) {
        double target = Ring4Cliff.lineRadius(LAYOUT) + offset;
        // Bisection on the radius: the boundary noise (up to 96 blocks) makes the effective distance non-linear.
        double lo = target - 400.0;
        double hi = target + 400.0;
        for (int n = 0; n < 80; n++) {
            double mid = 0.5 * (lo + hi);
            if (LAYOUT.effectiveDistance(mid * Math.cos(angle), mid * Math.sin(angle)) < target) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        double r = 0.5 * (lo + hi);
        return new int[]{(int) Math.floor(r * Math.cos(angle)), (int) Math.floor(r * Math.sin(angle))};
    }

    @Test
    void theLineStandsAtTheOuterEdgeOfTheZoneOnRing4sSide() {
        double line = Ring4Cliff.lineRadius(LAYOUT);
        // Zone 3|4 is 256 wide by default (verdant_falls), so its half is 128.
        assertEquals(6000.0 + 128.0 * 0.8, line, 1e-9);
        assertEquals(6000.0 - 128.0, Ring4Cliff.zoneStart(LAYOUT), 1e-9);
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += 0.3) {
            int[] p = at(angle, 0.0);
            assertEquals(Ring.RING_4, LAYOUT.sampleOverworld(p[0], p[1]).ring(), "the wall lies in ring 4's half");
        }
    }

    @Test
    void plateauStays40To80AboveTheFoot() {
        assertEquals(103.0, Ring4Cliff.plateau(90.0, 63.0));
        assertEquals(120.0, Ring4Cliff.plateau(120.0, 63.0));
        assertEquals(143.0, Ring4Cliff.plateau(200.0, 63.0));
    }

    /** Away from the gaps, the ground drops 40–80 blocks within a few blocks across the line. */
    @Test
    void theWallDrops40To80() {
        int walls = 0;
        int within = 0;
        long salt = FIELD.salt();
        double period = Ring4Cliff.perimeter(LAYOUT);
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += 0.013) {
            // Samples on either side of the line, within the wall's 4 blocks (the foot is held at the wall's value there).
            int[] inside = at(angle, -2.0);
            int[] outside = at(angle, 2.0);
            if (Ring4Cliff.gapAt(salt, Ring4Cliff.arc(LAYOUT, inside[0], inside[1]), period) != null
                    || Ring4Cliff.gapAt(salt, Ring4Cliff.arc(LAYOUT, outside[0], outside[1]), period) != null) {
                continue;
            }
            IslandField.Column in = FIELD.column(inside[0], inside[1]);
            IslandField.Column out = FIELD.column(outside[0], outside[1]);
            // The line wanders with the detail noise: only samples that really lie on either side count.
            if (in.wallDistance >= 0.0 || out.wallDistance <= 0.0 || out.wallDistance > 4.0) {
                continue;
            }
            walls++;
            // The plateau above the foot: at least 40 (the spec's lower bound, exact). At most 80 where the ground beside
            // the wall lies within 80 of the plateau; a steeper ring 4 rise beyond the line is terrain, counted below.
            // Per column, exact: the column's own plateau above its own foot (the clamp of Ring4Cliff.plateau and the foot
            // held at the plateau less 40 within the wall's 4 blocks).
            double drop = out.plateauY - out.footY;
            assertTrue(drop >= Ring4Cliff.MIN_HEIGHT - 1e-9, "drop " + drop + " at " + angle);
            if (drop <= Ring4Cliff.MAX_HEIGHT + 1e-9) {
                within++;
            }
        }
        System.out.printf("walls %d, of which within 40–80: %d%n", walls, within);
        assertTrue(within >= walls * 0.9, "walls within 40–80: " + within + " of " + walls);
        assertTrue(walls > 300, "walls " + walls);
    }

    /** Gaps: ravines and scree slopes every few hundred blocks, all round the line. */
    @Test
    void gapsAreRavinesAndScreeSlopes() {
        double period = Ring4Cliff.perimeter(LAYOUT);
        int count = Ring4Cliff.cellCount(period, Ring4Cliff.GAP_CELL);
        double cellSize = period / count;
        int ravines = 0;
        int scree = 0;
        for (int k = 0; k < count; k++) {
            Ring4Cliff.Gap gap = Ring4Cliff.gap(FIELD.salt(), k, cellSize, period);
            if (gap != null) {
                if (gap.ravine()) {
                    ravines++;
                } else {
                    scree++;
                }
                assertTrue(gap.centre() >= k * cellSize && gap.centre() < (k + 1) * cellSize, "gap " + k + " in its cell");
            }
        }
        assertTrue(ravines > 40 && scree > 40, ravines + " ravines, " + scree + " scree slopes");
    }

    /**
     * The seam of the angle (x < 0, z = 0): the gap cells are periodic round the line, so a position and the same
     * position one perimeter further on see the same gap, for any salt, and every gap is found from both sides of its
     * centre (the cell indices wrap at the seam, they do not run past it).
     */
    @Test
    void gapsArePeriodicRoundTheSeam() {
        double period = Ring4Cliff.perimeter(LAYOUT);
        int count = Ring4Cliff.cellCount(period, Ring4Cliff.GAP_CELL);
        double cellSize = period / count;
        for (long salt : new long[]{FIELD.salt(), 1L, 42L, 987654321L}) {
            int gaps = 0;
            for (double arc = 0.0; arc < period; arc += 7.0) {
                Ring4Cliff.Gap here = Ring4Cliff.gapAt(salt, arc, period);
                Ring4Cliff.Gap there = Ring4Cliff.gapAt(salt, arc + period, period);
                assertEquals(here == null, there == null, "gap status at arc " + arc);
                if (here != null) {
                    gaps++;
                    assertEquals(0.0, Ring4Cliff.loopDistance(here.centre(), there.centre(), period), 1e-9, "arc " + arc);
                    assertEquals(here.weight(arc), there.weight(arc + period), 1e-9, "weight at arc " + arc);
                }
            }
            assertTrue(gaps > 0 || salt != FIELD.salt(), "gaps on salt " + salt);
            for (int k = 0; k < count; k++) {
                Ring4Cliff.Gap gap = Ring4Cliff.gap(salt, k, cellSize, period);
                if (gap == null) {
                    continue;
                }
                for (double side : new double[]{-0.5, 0.5}) {
                    double arc = Ring4Cliff.wrap(gap.centre() + side * gap.halfWidth(), period);
                    Ring4Cliff.Gap found = Ring4Cliff.gapAt(salt, arc, period);
                    assertTrue(found != null, "gap " + k + " found at arc " + arc);
                    assertEquals(0.0, Ring4Cliff.loopDistance(found.centre(), gap.centre(), period), 1e-9, "same gap " + k);
                }
            }
        }
        // The same across the seam on the ground: columns one block either side of z = 0 at x < 0 have the same height.
        for (int x = -7000; x >= -7040; x -= 3) {
            double h1 = FIELD.column(x, 1).height();
            double h2 = FIELD.column(x, -1).height();
            assertTrue(Math.abs(h1 - h2) < 3.0, "ground across the seam at x " + x + ": " + h1 + " / " + h2);
        }
    }

    /**
     * The wall is exact per block but rough (5a): just past the roughness inside the line it is plateau rock up to the
     * top, just past it outside it is open; between, the face moves in and out with the height (ledges, overhangs,
     * clefts), so many columns change between rock and open within their height.
     */
    @Test
    void theWallIsRoughButBounded() {
        int checked = 0;
        int rough = 0;
        int crumbled = 0;
        long salt = FIELD.salt();
        double period = Ring4Cliff.perimeter(LAYOUT);
        double reach = Ring4Cliff.ROUGHNESS + 0.6;
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += 0.02) {
            int[] inside = at(angle, -reach - 0.5);
            int[] outside = at(angle, reach + 0.5);
            if (Ring4Cliff.gapAt(salt, Ring4Cliff.arc(LAYOUT, inside[0], inside[1]), period) != null
                    || Ring4Cliff.gapAt(salt, Ring4Cliff.arc(LAYOUT, outside[0], outside[1]), period) != null) {
                continue;
            }
            IslandField.Column in = FIELD.column(inside[0], inside[1]);
            IslandField.Column out = FIELD.column(outside[0], outside[1]);
            if (in.wallDistance > -reach || in.wallDistance <= -Ring4Cliff.WALL_BAND || out.wallDistance < reach
                    || out.wallDistance >= Ring4Cliff.WALL_BAND || !in.wallExact || !out.wallExact || in.nearFall || out.nearFall) {
                continue;
            }
            int top = (int) Math.ceil(in.plateauY) - 1;
            assertEquals(1.0, FIELD.thin(inside[0], top, inside[1]), "plateau rock at the top inside at " + angle);
            // Review 5a #4: above the plateau's top the terrain decides inside (no forced opening where nothing crumbled).
            assertEquals(0.0, FIELD.thin(inside[0], top + 1, inside[1]), "terrain above the top inside at " + angle);
            assertEquals(0.0, FIELD.thin(inside[0], top + 2, inside[1]), "terrain above the top inside at " + angle);
            assertEquals(-1.0, FIELD.thin(outside[0], (int) Math.ceil(out.plateauY) - 1, outside[1]), "open beyond at " + angle);
            checked++;
            // On the line itself: does the face move with the height?
            int[] line = at(angle, 0.0);
            IslandField.Column on = FIELD.column(line[0], line[1]);
            if (on.nearFall || !on.wallExact) {
                continue;
            }
            int rock = 0;
            int open = 0;
            int lineTop = (int) Math.ceil(on.plateauY) - 1;
            // The top crumbles on the line, by at most CRUMBLE blocks.
            int crumble = lineTop - FIELD.wallTop(line[0], line[1]);
            assertTrue(crumble >= 0 && crumble <= Ring4Cliff.CRUMBLE, "crumble " + crumble + " at " + angle);
            crumbled += crumble > 0 ? 1 : 0;
            for (int y = lineTop - 30; y <= lineTop; y++) {
                double t = FIELD.thin(line[0], y, line[1]);
                rock += t > 0.0 ? 1 : 0;
                open += t < 0.0 ? 1 : 0;
            }
            rough += rock > 0 && open > 0 ? 1 : 0;
        }
        System.out.printf("wall columns checked %d, rough on the line %d, crumbled %d%n", checked, rough, crumbled);
        assertTrue(checked > 40, "checked " + checked);
        assertTrue(rough > checked / 4, "rough columns " + rough + " of " + checked);
        assertTrue(crumbled > 0, "crumbled columns " + crumbled);
    }

    /**
     * Beside a fall the wall stays smooth (5a): within {@link Ring4Cliff#FALL_CLEARANCE} of its spring and fall column
     * no roughness and no crumbling, rock up to the plateau's top inside the line and open from the ground up outside.
     */
    @Test
    void theWallStaysSmoothBesideAFall() {
        int perimeter = Ring4Cliff.cellCount(Ring4Cliff.perimeter(LAYOUT), Ring4Cliff.FALL_CELL);
        int checked = 0;
        for (int k = 0; k < perimeter; k++) {
            IslandField.Fall fall = FIELD.cliffFall(k);
            if (fall == null) {
                continue;
            }
            for (int dx = -Ring4Cliff.FALL_CLEARANCE; dx <= Ring4Cliff.FALL_CLEARANCE; dx++) {
                for (int dz = -Ring4Cliff.FALL_CLEARANCE; dz <= Ring4Cliff.FALL_CLEARANCE; dz++) {
                    int x = fall.springX() + dx;
                    int z = fall.springZ() + dz;
                    IslandField.Column c = FIELD.column(x, z);
                    // The spring and the fall's own column are carved and filled by the aquifer; islands and sinkholes
                    // have their own rules.
                    if (!c.nearFall || !c.wallExact || Math.abs(c.wallDistance) >= Ring4Cliff.WALL_BAND || c.islandCount > 0
                            || c.sinkholeDisc || x == fall.springX() && z == fall.springZ() || x == fall.fallX() && z == fall.fallZ()) {
                        continue;
                    }
                    int plateauTop = (int) Math.ceil(c.plateauY) - 1;
                    assertEquals(plateauTop, FIELD.wallTop(x, z), "no crumbling beside a fall at " + x + " " + z);
                    double floor = c.wallDistance < 0.0 ? Math.min(c.footY, c.plateauY - Ring4Cliff.MIN_HEIGHT) : c.surface;
                    for (int y = (int) Math.floor(floor) - 2; y <= plateauTop + 2; y++) {
                        double expected = c.wallDistance < 0.0 ? (y <= plateauTop ? 1.0 : 0.0) : (y >= Math.ceil(floor) ? -1.0 : 0.0);
                        assertEquals(expected, FIELD.thin(x, y, z), "smooth wall beside a fall at " + x + " " + y + " " + z);
                    }
                    checked++;
                }
            }
        }
        System.out.println("wall columns beside falls checked: " + checked);
        assertTrue(checked > 40, "checked " + checked);
    }

    /** Steaming falls: a spring walled in by the plateau, the fall beside it open down to its plunge pool. */
    @Test
    void fallsPourIntoTheirPlungePools() {
        List<IslandField.Fall> falls = new ArrayList<>();
        int perimeter = Ring4Cliff.cellCount(Ring4Cliff.perimeter(LAYOUT), Ring4Cliff.FALL_CELL);
        for (int k = 0; k < perimeter; k++) {
            IslandField.Fall fall = FIELD.cliffFall(k);
            if (fall != null) {
                falls.add(fall);
            }
        }
        System.out.println("cliff falls: " + falls.size() + " of " + perimeter + " cells");
        assertTrue(falls.size() > perimeter / 6, "falls " + falls.size() + " of " + perimeter);
        // The chunk cap (3 falls per chunk) may drop a fall from the terrain; those are counted, not skipped silently:
        // at most a tenth of the falls may be capped, and the rest are checked.
        int capped = 0;
        int checked = 0;
        for (IslandField.Fall fall : falls) {
            if (!FIELD.fallsOfChunk(fall.fallX() >> 4, fall.fallZ() >> 4).contains(fall)) {
                capped++;
                continue;
            }
            checked++;
            assertEquals(-1.0, FIELD.thin(fall.springX(), fall.springY(), fall.springZ()), "spring " + fall);
            assertEquals(1.0, FIELD.thin(fall.springX(), fall.springY() + 1, fall.springZ()), "lip above the spring");
            assertEquals(63, fall.bottom());
            for (int y = fall.bottom(); y <= fall.springY(); y++) {
                assertTrue(FIELD.thin(fall.fallX(), y, fall.fallZ()) <= 0.0, "fall column " + fall + " at " + y);
            }
            IslandField.Column column = FIELD.column(fall.fallX(), fall.fallZ());
            assertTrue(column.surface() < 62.0, "plunge pool " + column.surface());
            assertEquals(fall.springY(), column.fallTop());
        }
        System.out.printf("falls checked %d, capped by the chunk cap %d%n", checked, capped);
        assertTrue(checked >= falls.size() * 0.9, "checked " + checked + " of " + falls.size());
    }
}
