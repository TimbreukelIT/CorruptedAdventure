package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The bridges of ring 5 (84, V6-A): two natural bridges per castle, the narrow ridges of the outer band. */
class CastleBridgesTest {
    /**
     * On 40 seeds: per castle exactly two bridges, 5–9 wide, sagging 2–6, the deck falling at most 0.5 per block; the
     * first along the inward ray to ground, the second 30–60° off it onto a floe; both land on ground and their decks are
     * never broken through.
     */
    @Test
    void twoWholeBridgesPerCastleLandOnGround() {
        for (long seed = Ring5FieldTest.SEED; seed < Ring5FieldTest.SEED + 40; seed++) {
            Ring5Field field = Ring5FieldTest.field(seed);
            List<Ring5Field.Bridge> bridges = field.castleBridges();
            assertEquals(8, bridges.size());
            for (Ring5Field.CastleSite site : field.castleSites()) {
                List<Ring5Field.Bridge> own = bridges.stream().filter(b -> b.siteIndex() == site.index()).toList();
                assertEquals(2, own.size(), "seed " + seed);
                for (int k = 0; k < 2; k++) {
                    Ring5Field.Bridge bridge = own.get(k);
                    double width = 2.0 * bridge.halfWidth();
                    assertTrue(width >= 5.0 && width <= 9.0, "width " + width);
                    assertTrue(bridge.sag() >= 2.0 && bridge.sag() <= 6.0);
                    double length = Math.hypot(bridge.toX() - bridge.fromX(), bridge.toZ() - bridge.fromZ());
                    assertEquals(site.top(), bridge.deckY());
                    assertTrue(bridge.deckY() - bridge.endY() <= 0.5 * length + 1.0, "slope, seed " + seed);
                    // Direction against the inward ray.
                    double ix = -site.x();
                    double iz = -site.z();
                    double dx = bridge.toX() - bridge.fromX();
                    double dz = bridge.toZ() - bridge.fromZ();
                    double angle = Math.toDegrees(Math.acos(Math.min(1.0, (ix * dx + iz * dz) / Math.hypot(ix, iz) / Math.hypot(dx, dz))));
                    if (k == 0) {
                        assertTrue(angle < 0.5, "first bridge off the inward ray: " + angle);
                    } else {
                        assertTrue(angle >= 29.5 && angle <= 60.5, "second bridge at " + angle + "°, seed " + seed);
                    }
                    int ex = (int) Math.floor(bridge.toX());
                    int ez = (int) Math.floor(bridge.toZ());
                    assertTrue(field.solidGround(ex, ez), "bridge " + k + " of " + site + " ends over the void, seed " + seed);
                    assertDeckWhole(field, bridge, length);
                }
            }
        }
    }

    /** Along its middle (inside the crumbling edge) the deck holds a solid block under its top at every column. */
    private static void assertDeckWhole(Ring5Field field, Ring5Field.Bridge bridge, double length) {
        double ux = (bridge.toX() - bridge.fromX()) / length;
        double uz = (bridge.toZ() - bridge.fromZ()) / length;
        for (double s = 0.0; s <= length; s += 0.5) {
            for (double off = -(bridge.halfWidth() - 1.05); off <= bridge.halfWidth() - 1.05; off += 0.5) {
                double px = bridge.fromX() + ux * s - uz * off;
                double pz = bridge.fromZ() + uz * s + ux * off;
                int x = (int) Math.floor(px);
                int z = (int) Math.floor(pz);
                // The column's own projection (its centre), as the field computes it.
                double along = (x + 0.5 - bridge.fromX()) * ux + (z + 0.5 - bridge.fromZ()) * uz;
                double centreOff = Math.abs((x + 0.5 - bridge.fromX()) * uz - (z + 0.5 - bridge.fromZ()) * ux);
                if (along < 0.0 || along > length || centreOff > bridge.halfWidth() - 1.0) {
                    continue;
                }
                double t = along / length;
                int top = (int) Math.round(bridge.deckY() + t * (bridge.endY() - bridge.deckY()) - bridge.sag() * 4.0 * t * (1.0 - t));
                assertTrue(field.terrain(x, top - 1, z) > 0.0, "deck broken at " + x + ", " + (top - 1) + ", " + z);
            }
        }
    }

    /** Ridges: 3–5 wide, one per ridge cell round the ring (~400–600 apart), from the band line outwards. */
    @Test
    void ridgesAreNarrowAndRoundTheRing() {
        Ring5Field field = Ring5FieldTest.field();
        List<Ring5Field.Bridge> ridges = field.ridges();
        assertTrue(ridges.size() >= 100 && ridges.size() <= 120, "ridges " + ridges.size());
        for (Ring5Field.Bridge ridge : ridges) {
            assertEquals(-1, ridge.siteIndex());
            assertTrue(2.0 * ridge.halfWidth() >= 3.0 && 2.0 * ridge.halfWidth() <= 5.0);
            assertTrue(Math.hypot(ridge.toX(), ridge.toZ()) > Math.hypot(ridge.fromX(), ridge.fromZ()) + 300.0);
        }
        for (int k = 0; k < ridges.size(); k++) {
            Ring5Field.Bridge a = ridges.get(k);
            Ring5Field.Bridge b = ridges.get((k + 1) % ridges.size());
            double gap = Math.hypot(a.fromX() - b.fromX(), a.fromZ() - b.fromZ());
            assertTrue(gap >= 280.0 && gap <= 720.0, "ridge gap " + gap);
        }
    }

    /** {@code bridgesWithin} finds every bridge and ridge from every chunk box its deck passes through. */
    @Test
    void bridgesWithinFindsEveryBridgeFromEveryChunkItTouches() {
        Ring5Field field = Ring5FieldTest.field();
        List<Ring5Field.Bridge> all = new java.util.ArrayList<>(field.castleBridges());
        all.addAll(field.ridges().subList(0, 12));
        for (Ring5Field.Bridge bridge : all) {
            double length = Math.hypot(bridge.toX() - bridge.fromX(), bridge.toZ() - bridge.fromZ());
            double ux = (bridge.toX() - bridge.fromX()) / length;
            double uz = (bridge.toZ() - bridge.fromZ()) / length;
            for (double s = 0.0; s <= length; s += 2.0) {
                for (double off = -bridge.halfWidth(); off <= bridge.halfWidth(); off += bridge.halfWidth()) {
                    int cx = Math.floorDiv((int) Math.floor(bridge.fromX() + ux * s - uz * off), 16);
                    int cz = Math.floorDiv((int) Math.floor(bridge.fromZ() + uz * s + ux * off), 16);
                    assertTrue(field.bridgesWithin(cx * 16, cz * 16, cx * 16 + 15, cz * 16 + 15).contains(bridge),
                            "chunk " + cx + ", " + cz + " misses " + bridge);
                }
            }
        }
    }
}
