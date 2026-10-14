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
        assertEquals(6000.0 + 64.0 * 0.8, line, 1e-9);
        assertEquals(6000.0 - 64.0, Ring4Cliff.zoneStart(LAYOUT), 1e-9);
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
        long salt = FIELD.salt();
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += 0.013) {
            // The wall as seen at its foot: 2 blocks either side (the foot blends down over 12 blocks beyond the line).
            int[] inside = at(angle, -2.0);
            int[] outside = at(angle, 2.0);
            double arc = Ring4Cliff.arc(LAYOUT, inside[0], inside[1]);
            if (Ring4Cliff.gapAt(salt, arc) != null || Ring4Cliff.gapAt(salt, Ring4Cliff.arc(LAYOUT, outside[0], outside[1])) != null) {
                continue;
            }
            // The line wanders with the detail noise: only samples that really lie on either side count.
            if (FIELD.column(inside[0], inside[1]).wallDistance >= 0.0 || FIELD.column(outside[0], outside[1]).wallDistance <= 0.0) {
                continue;
            }
            double top = FIELD.column(inside[0], inside[1]).height();
            IslandField.Column foot = FIELD.column(outside[0], outside[1]);
            double drop = top - foot.height();
            walls++;
            assertTrue(drop >= Ring4Cliff.MIN_HEIGHT - 20.0 && drop <= Ring4Cliff.MAX_HEIGHT + 40.0, "drop " + drop + " at " + angle);
        }
        assertTrue(walls > 300, "walls " + walls);
    }

    /** Gaps: ravines and scree slopes every few hundred blocks. */
    @Test
    void gapsAreRavinesAndScreeSlopes() {
        int ravines = 0;
        int scree = 0;
        for (int k = -100; k < 100; k++) {
            Ring4Cliff.Gap gap = Ring4Cliff.gap(FIELD.salt(), k);
            if (gap != null) {
                if (gap.ravine()) {
                    ravines++;
                } else {
                    scree++;
                }
                assertTrue(gap.centre() - gap.halfWidth() - 4.0 > k * Ring4Cliff.GAP_CELL - Ring4Cliff.GAP_CELL);
            }
        }
        assertTrue(ravines > 40 && scree > 40, ravines + " ravines, " + scree + " scree slopes");
    }

    /** Where the wall stands exact: rock up to the plateau just inside, open from the foot up just beyond. */
    @Test
    void theWallIsExactPerBlock() {
        int checked = 0;
        long salt = FIELD.salt();
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += 0.05) {
            int[] inside = at(angle, -2.0);
            int[] outside = at(angle, 2.0);
            if (Ring4Cliff.gapAt(salt, Ring4Cliff.arc(LAYOUT, inside[0], inside[1])) != null) {
                continue;
            }
            IslandField.Column in = FIELD.column(inside[0], inside[1]);
            IslandField.Column out = FIELD.column(outside[0], outside[1]);
            if (in.wallDistance >= 0.0 || out.wallDistance < 0.0 || !in.wallExact || !out.wallExact) {
                continue;
            }
            int top = (int) Math.ceil(in.plateauY) - 1;
            assertEquals(1.0, FIELD.thin(inside[0], top, inside[1]), "rock at the top inside");
            assertEquals(1.0, FIELD.thin(inside[0], (int) Math.floor(in.footY), inside[1]));
            assertEquals(-1.0, FIELD.thin(outside[0], top, outside[1]), "open beyond");
            assertEquals(-1.0, FIELD.thin(outside[0], (int) Math.ceil(out.surface) + 1, outside[1]));
            checked++;
        }
        assertTrue(checked > 40, "checked " + checked);
    }

    /** Steaming falls: a spring walled in by the plateau, the fall beside it open down to its plunge pool. */
    @Test
    void fallsPourIntoTheirPlungePools() {
        List<IslandField.Fall> falls = new ArrayList<>();
        int perimeter = (int) Math.ceil(2.0 * Math.PI * Ring4Cliff.lineRadius(LAYOUT) / Ring4Cliff.FALL_CELL);
        for (int k = -perimeter / 2 - 1; k <= perimeter / 2 + 1; k++) {
            IslandField.Fall fall = FIELD.cliffFall(k);
            if (fall != null) {
                falls.add(fall);
            }
        }
        System.out.println("cliff falls: " + falls.size() + " of " + perimeter + " cells");
        assertTrue(falls.size() > perimeter / 6, "falls " + falls.size() + " of " + perimeter);
        for (IslandField.Fall fall : falls) {
            if (!FIELD.fallsOfChunk(fall.fallX() >> 4, fall.fallZ() >> 4).contains(fall)) {
                continue;
            }
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
    }
}
