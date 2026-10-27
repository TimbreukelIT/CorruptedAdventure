package nl.timtoys.corruptedadventure.worldgen.chunk;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import nl.timtoys.corruptedadventure.config.CommonConfig;
import nl.timtoys.corruptedadventure.config.RingSettings;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.Ring5Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarmupPlanTest {
    private static final RingSettings DEFAULTS = new RingSettings(CommonConfig.DEFAULT_OUTER_RADII,
            CommonConfig.DEFAULT_VOID_OUTER_RADIUS, CommonConfig.DEFAULT_BOUNDARY_NOISE,
            CommonConfig.DEFAULT_TRANSITION_WIDTH, CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES);
    /**
     * Rings about as narrow as {@link RingSettings#validate} allows with the default noise and zones: ring 4 has 244
     * blocks between its zones, so the overworld margin (48) and the Nether's (48 × 4) both have to give way.
     */
    private static final RingSettings NARROW = new RingSettings(List.of(300, 700, 1200, 1700, 2200), 2600,
            CommonConfig.DEFAULT_BOUNDARY_NOISE, CommonConfig.DEFAULT_TRANSITION_WIDTH,
            CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES);
    private static final long[] SEEDS = {1L, 12345L, -987654321L};
    private static final List<WarmupPlan.Point> CASTLES = List.of(new WarmupPlan.Point(9000, 500),
            new WarmupPlan.Point(-500, 9000), new WarmupPlan.Point(-9000, -500), new WarmupPlan.Point(500, -9000));
    /** A stand-in glitch field: a sector between 200° and 230°, so the search has to walk. */
    private static final WarmupPlan.ColumnTest GLITCH = (x, z) -> {
        double degrees = Math.toDegrees(Math.atan2(z, x));
        double angle = degrees < 0 ? degrees + 360.0 : degrees;
        return angle >= 200.0 && angle <= 230.0;
    };
    private static final List<WarmupPlan.Dimension> DIMENSIONS = List.of(
            new WarmupPlan.Dimension(Level.OVERWORLD, new WarmupPlan.Ring5Spots(CASTLES, GLITCH)),
            new WarmupPlan.Dimension(Level.NETHER, null));

    @Test
    void everySpotLiesInTheRegionItIsNamedFor() {
        for (long seed : SEEDS) {
            assertInTheirRegions(new RingLayout(DEFAULTS, seed), DIMENSIONS, true);
        }
    }

    /** With narrow rings the edge spots keep out of the zones and in order, in both dimensions. */
    @Test
    void narrowRingsKeepEverySpotInItsRegionAndInOrder() {
        assertTrue(NARROW.validate().isSuccess());
        for (long seed : SEEDS) {
            RingLayout layout = new RingLayout(NARROW, seed);
            // Band sides are not promised in a ring 5 this narrow: its zones cover more than the band spots' shares.
            assertInTheirRegions(layout, DIMENSIONS, false);
            assertEdgesInOrder(layout, WarmupPlan.spots(layout, DIMENSIONS, WorldgenWarmup.ROUNDS));
        }
    }

    @Test
    void edgeSpotsLieInOrderWithTheDefaults() {
        RingLayout layout = new RingLayout(DEFAULTS, 12345L);
        assertEdgesInOrder(layout, WarmupPlan.spots(layout, DIMENSIONS, WorldgenWarmup.ROUNDS));
    }

    @Test
    void withoutAGlitchFieldTheSpotIsAPlainInnerBandColumn() {
        RingLayout layout = new RingLayout(DEFAULTS, 12345L);
        List<WarmupPlan.Dimension> none = List.of(
                new WarmupPlan.Dimension(Level.OVERWORLD, new WarmupPlan.Ring5Spots(CASTLES, (x, z) -> false)));
        List<WarmupPlan.Spot> spots = WarmupPlan.spots(layout, none, WorldgenWarmup.ROUNDS);
        assertEquals(WorldgenWarmup.ROUNDS, counts(spots, Level.OVERWORLD).get(WarmupPlan.NO_GLITCH_FIELD));
        assertFalse(counts(spots, Level.OVERWORLD).containsKey("ring_5_glitch_field"));
        assertInTheirRegions(layout, none, true);
    }

    @Test
    void everyRegionTypeOncePerRound() {
        List<WarmupPlan.Spot> spots = defaultSpots();
        Map<String, Long> overworld = counts(spots, Level.OVERWORLD);
        Map<String, Long> nether = counts(spots, Level.NETHER);
        for (int ring = 1; ring <= 5; ring++) {
            for (String part : List.of("_inner", "_middle", "_outer")) {
                assertEquals(WorldgenWarmup.ROUNDS, overworld.get("ring_" + ring + part), "ring_" + ring + part);
                assertEquals(WorldgenWarmup.ROUNDS, nether.get("ring_" + ring + part), "nether ring_" + ring + part);
            }
        }
        // The 256-block zones (3|4, 4|5) get a spot per half in the overworld; the Nether's are a quarter as wide.
        for (String zone : List.of("zone_3_4", "zone_4_5")) {
            assertEquals(WorldgenWarmup.ROUNDS, overworld.get(zone + "_inner_half"), zone);
            assertEquals(WorldgenWarmup.ROUNDS, overworld.get(zone + "_outer_half"), zone);
            assertEquals(WorldgenWarmup.ROUNDS, nether.get(zone), "nether " + zone);
        }
        for (String zone : List.of("zone_1_2", "zone_2_3", "zone_5_void")) {
            assertEquals(WorldgenWarmup.ROUNDS, overworld.get(zone), zone);
            assertEquals(WorldgenWarmup.ROUNDS, nether.get(zone), "nether " + zone);
        }
        for (String region : List.of("void_edge", "ring_5_inner_band", "ring_5_glitch_field", "ring_5_outer_band",
                "ring_5_castle")) {
            assertEquals(WorldgenWarmup.ROUNDS, overworld.get(region), region);
        }
        assertEquals(WorldgenWarmup.ROUNDS, nether.get("void_edge"));
        assertFalse(nether.containsKey("ring_5_castle"));
        assertTrue(spots.size() >= 60 && spots.size() <= 120, "chunks: " + spots.size());
    }

    @Test
    void spotsStayOffTheAxesAndChangeAngleEveryRound() {
        List<WarmupPlan.Spot> spots = defaultSpots();
        for (WarmupPlan.Spot spot : spots) {
            if (spot.region().equals("ring_5_castle")) {
                continue;
            }
            double degrees = Math.toDegrees(Math.atan2(spot.z(), spot.x()));
            double fromAxis = Math.abs(degrees - 90.0 * Math.round(degrees / 90.0));
            // The block position rounds the angle by a fraction of a degree even at ring 1's inner spot.
            assertTrue(fromAxis > WarmupPlan.AXIS_CLEARANCE - 1.0, spot + " " + degrees);
        }
        List<WarmupPlan.Spot> rounds = spots.stream()
                .filter(spot -> spot.dimension().equals(Level.OVERWORLD) && spot.region().equals("ring_5_inner_band"))
                .toList();
        assertNotEquals(rounds.get(0).chunk(), rounds.get(1).chunk());
    }

    @Test
    void theDimensionsTakeTurns() {
        List<WarmupPlan.Spot> spots = defaultSpots();
        for (int i = 0; i < 20; i++) {
            assertEquals(i % 2 == 0 ? Level.OVERWORLD : Level.NETHER, spots.get(i).dimension(), "spot " + i);
        }
        assertEquals(spots, defaultSpots());
    }

    @Test
    void alongFindsTheEffectiveDistance() {
        RingLayout layout = new RingLayout(DEFAULTS, 12345L);
        for (double distance : new double[] {400.0, 3500.0, 8850.0, 9982.0}) {
            for (double angle = 0.3; angle < 6.2; angle += 0.7) {
                WarmupPlan.Point point = WarmupPlan.along(layout, distance, angle);
                assertEquals(distance, layout.effectiveDistance(point.x(), point.z()), 0.01);
            }
        }
    }

    private static List<WarmupPlan.Spot> defaultSpots() {
        return WarmupPlan.spots(new RingLayout(DEFAULTS, 12345L), DIMENSIONS, WorldgenWarmup.ROUNDS);
    }

    /** @param bandSides also check that ring 5's band spots lie on their side of the band line */
    private static void assertInTheirRegions(RingLayout layout, List<WarmupPlan.Dimension> dimensions, boolean bandSides) {
        for (WarmupPlan.Spot spot : WarmupPlan.spots(layout, dimensions, WorldgenWarmup.ROUNDS)) {
            int scale = RingLayout.scale(spot.dimension());
            double x = spot.x() * (double) scale;
            double z = spot.z() * (double) scale;
            RingPosition position = layout.sampleOverworld(x, z);
            String region = spot.region();
            String where = layout.settings().ringOuterRadii() + " " + spot + " -> " + position;
            boolean innerBand = region.equals("ring_5_inner_band") || region.equals(WarmupPlan.NO_GLITCH_FIELD)
                    || region.equals("ring_5_glitch_field");
            if (region.equals("ring_5_castle")) {
                assertTrue(CASTLES.contains(new WarmupPlan.Point(spot.x(), spot.z())), where);
            } else if (innerBand || region.equals("ring_5_outer_band")) {
                assertEquals(Ring.RING_5, position.ring(), where);
                assertFalse(position.inTransition(), where);
                if (region.equals("ring_5_glitch_field")) {
                    assertTrue(GLITCH.test(spot.x(), spot.z()), where);
                }
                if (bandSides) {
                    assertEquals(innerBand, Ring5Band.of(layout).inner(x, z), where);
                }
            } else if (region.startsWith("ring_")) {
                assertEquals(Ring.byIndex(region.charAt(5) - '1'), position.ring(), where);
                assertFalse(position.inTransition(), where);
            } else if (region.startsWith("zone_")) {
                assertTrue(position.inTransition(), where);
                assertEquals(Ring.byIndex(region.charAt(5) - '1'), position.inner(), where);
            } else {
                assertEquals("void_edge", region, where);
                assertEquals(Ring.VOID, position.ring(), where);
                assertFalse(position.inTransition(), where);
            }
        }
    }

    /** Per dimension, ring and round: the inner edge spot lies inside the middle, the middle inside the outer edge. */
    private static void assertEdgesInOrder(RingLayout layout, List<WarmupPlan.Spot> spots) {
        for (ResourceKey<Level> dimension : List.of(Level.OVERWORLD, Level.NETHER)) {
            int scale = RingLayout.scale(dimension);
            for (int ring = 1; ring <= 5; ring++) {
                List<Double> inner = distances(layout, spots, dimension, "ring_" + ring + "_inner", scale);
                List<Double> middle = distances(layout, spots, dimension, "ring_" + ring + "_middle", scale);
                List<Double> outer = distances(layout, spots, dimension, "ring_" + ring + "_outer", scale);
                for (int round = 0; round < WorldgenWarmup.ROUNDS; round++) {
                    String where = dimension.location() + " ring " + ring + " round " + round;
                    assertTrue(inner.get(round) < middle.get(round), where + ": " + inner + " " + middle);
                    assertTrue(middle.get(round) < outer.get(round), where + ": " + middle + " " + outer);
                }
            }
        }
    }

    private static List<Double> distances(RingLayout layout, List<WarmupPlan.Spot> spots, ResourceKey<Level> dimension,
                                          String region, int scale) {
        return spots.stream().filter(spot -> spot.dimension().equals(dimension) && spot.region().equals(region))
                .map(spot -> layout.effectiveDistance(spot.x() * (double) scale, spot.z() * (double) scale))
                .toList();
    }

    private static Map<String, Long> counts(List<WarmupPlan.Spot> spots, ResourceKey<Level> dimension) {
        return spots.stream().filter(spot -> spot.dimension().equals(dimension))
                .collect(Collectors.groupingBy(WarmupPlan.Spot::region, Collectors.counting()));
    }
}
