package nl.timtoys.corruptedadventure.worldgen.chunk;

import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import nl.timtoys.corruptedadventure.config.RingSettings;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.Ring5Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Where the worldgen warm-up ({@link WorldgenWarmup}) generates its chunks: every region type of each ring dimension,
 * placed from the world's ring layout (its radii and zones, no fixed numbers). Per ring its inner edge, middle and
 * outer edge (clear of the zones), every zone between rings (both halves of a wide zone, else the boundary), the void
 * edge; in a dimension with ring 5 terrain also ring 5's inner band, a glitch field, its outer band and a castle site.
 * A round visits every type once, each at a new angle (golden-angle steps, kept off the axes); the dimensions' rounds
 * are interleaved, so consecutive chunks lie in different rings and dimensions.
 * <p>
 * Pure: the same layout and inputs give the same spots.
 */
final class WarmupPlan {
    /** Into the ring from the edge of its zone, in blocks of the dimension; at most {@link #MAX_MARGIN_SHARE} of it. */
    static final double EDGE_MARGIN = 48.0;
    /**
     * Share of a ring's width (between its zones) an edge spot keeps from the zone at most, so in a narrow ring the
     * inner edge, middle and outer edge stay apart and in order.
     */
    static final double MAX_MARGIN_SHARE = 0.25;
    /**
     * A zone gets two spots (one per half) when half of it is at least this wide, in blocks of the dimension: the wide
     * zones with terrain of their own (3|4, 4|5). A chunk on the boundary of a narrower zone has both of its sides.
     */
    static final double SPLIT_ZONE_HALF = 128.0;
    /** No spot (except a castle site) closer than this to an axis, in degrees. */
    static final double AXIS_CLEARANCE = 12.0;
    /** Ring 1 has no inner zone; its "inner edge" lies at this share of its width (spawn covers the centre). */
    private static final double RING_1_INNER_SHARE = 0.25;
    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));
    private static final double START_ANGLE = Math.toRadians(20.0);
    private static final double SEARCH_STEP = Math.toRadians(3.0);
    private static final int SEARCH_STEPS = 120;
    private static final int BISECTIONS = 40;

    /**
     * The region of the glitch field's spot when the search found none along the inner band: it is a plain inner band
     * column then.
     */
    static final String NO_GLITCH_FIELD = "ring_5_inner_band_no_glitch_field";

    private WarmupPlan() {
    }

    /** A spot to generate: block position {@code x}, {@code z} of the dimension; {@code region} names its type. */
    record Spot(ResourceKey<Level> dimension, String region, int x, int z) {
        ChunkPos chunk() {
            return new ChunkPos(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
        }
    }

    /** A test on an overworld column (blocks), e.g. "lies in a glitch field". */
    @FunctionalInterface
    interface ColumnTest {
        boolean test(int x, int z);
    }

    /** An overworld block column. */
    record Point(double x, double z) {
    }

    /**
     * What a dimension with ring 5 terrain adds: its castle sites (overworld blocks, one per round in turn; may be
     * empty) and the test for a glitch field (searched for along the inner band).
     */
    record Ring5Spots(List<Point> castles, ColumnTest glitchField) {
    }

    /** A ring dimension to warm up; {@code ring5} null without ring 5 terrain (the Nether). */
    record Dimension(ResourceKey<Level> key, @Nullable Ring5Spots ring5) {
    }

    /** All spots of {@code rounds} rounds over {@code dimensions}, in generation order. */
    static List<Spot> spots(RingLayout layout, List<Dimension> dimensions, int rounds) {
        List<List<Spot>> perDimension = new ArrayList<>();
        for (int i = 0; i < dimensions.size(); i++) {
            perDimension.add(spots(layout, dimensions.get(i), rounds, i));
        }
        return interleave(perDimension);
    }

    private static List<Spot> spots(RingLayout layout, Dimension dimension, int rounds, int dimensionIndex) {
        List<Target> targets = targets(layout, dimension);
        int scale = RingLayout.scale(dimension.key());
        List<Spot> spots = new ArrayList<>();
        for (int round = 0; round < rounds; round++) {
            for (int i = 0; i < targets.size(); i++) {
                // A different start per dimension; every spot of every round on its own angle.
                double angle = offAxis(START_ANGLE + dimensionIndex + GOLDEN_ANGLE * (round * targets.size() + i));
                Placed placed = targets.get(i).at(layout, angle, round);
                spots.add(new Spot(dimension.key(), placed.region(),
                        (int) Math.floor(placed.point().x() / scale), (int) Math.floor(placed.point().z() / scale)));
            }
        }
        return spots;
    }

    /** One list in turn from each, until all are used up. */
    private static List<Spot> interleave(List<List<Spot>> lists) {
        List<Spot> merged = new ArrayList<>();
        int longest = lists.stream().mapToInt(List::size).max().orElse(0);
        for (int i = 0; i < longest; i++) {
            for (List<Spot> list : lists) {
                if (i < list.size()) {
                    merged.add(list.get(i));
                }
            }
        }
        return merged;
    }

    /** A region type: where it lies in a round, on a given angle. */
    @FunctionalInterface
    private interface Target {
        Placed at(RingLayout layout, double angle, int round);
    }

    private record Placed(Point point, String region) {
    }

    /** The region types of a dimension, kind by kind (all inner edges, all middles, ...): neighbours differ in ring. */
    private static List<Target> targets(RingLayout layout, Dimension dimension) {
        RingSettings settings = layout.settings();
        List<Integer> radii = settings.ringOuterRadii();
        int scale = RingLayout.scale(dimension.key());
        int rings = radii.size();
        // Per ring, between its zones (RingSettings.validate keeps that span positive): inner < middle < outer.
        double[] inner = new double[rings];
        double[] middle = new double[rings];
        double[] outer = new double[rings];
        for (int ring = 0; ring < rings; ring++) {
            double from = ring == 0 ? 0.0 : zoneEnd(settings, ring - 1);
            double to = zoneStart(settings, ring);
            double margin = Math.min(EDGE_MARGIN * scale, MAX_MARGIN_SHARE * (to - from));
            inner[ring] = ring == 0 ? from + RING_1_INNER_SHARE * (to - from) : from + margin;
            middle[ring] = (from + to) / 2.0;
            outer[ring] = to - margin;
        }
        List<Target> targets = new ArrayList<>();
        for (int ring = 0; ring < rings; ring++) {
            targets.add(radial(name(ring) + "_inner", inner[ring]));
        }
        for (int ring = 0; ring < rings; ring++) {
            targets.add(radial(name(ring) + "_middle", middle[ring]));
        }
        for (int ring = 0; ring < rings; ring++) {
            targets.add(radial(name(ring) + "_outer", outer[ring]));
        }
        for (int boundary = 0; boundary < rings; boundary++) {
            String zone = "zone_" + (boundary + 1) + "_" + (boundary + 2 > rings ? "void" : boundary + 2);
            double radius = radii.get(boundary);
            double half = settings.transitionWidth(boundary) / 2.0;
            if (half / scale >= SPLIT_ZONE_HALF) {
                targets.add(radial(zone + "_inner_half", radius - half / 2.0));
                targets.add(radial(zone + "_outer_half", radius + half / 2.0));
            } else {
                targets.add(radial(zone, radius));
            }
        }
        targets.add(radial("void_edge", (zoneEnd(settings, rings - 1) + settings.voidOuterRadius()) / 2.0));
        Ring5Spots ring5 = dimension.ring5();
        if (ring5 != null) {
            // Ring 5's band line moves at most AMPLITUDE_SHARE of the ring around INNER_SHARE: these stay on their side
            // (with the default rings; in a narrow ring 5 they stay between its edge spots and its middle).
            int five = Ring.RING_5.ordinal();
            double from = radii.get(Ring.RING_4.ordinal());
            double width = radii.get(five) - from;
            double innerShare = (Ring5Band.INNER_SHARE - Ring5Band.AMPLITUDE_SHARE) / 2.0;
            double outerShare = (1.0 + Ring5Band.INNER_SHARE + Ring5Band.AMPLITUDE_SHARE) / 2.0;
            double innerBand = Math.clamp(from + width * innerShare, inner[five], middle[five]);
            double outerBand = Math.clamp(from + width * outerShare, middle[five], outer[five]);
            targets.add(radial("ring_5_inner_band", innerBand));
            targets.add((l, angle, round) -> search(l, innerBand, angle, ring5.glitchField()));
            targets.add(radial("ring_5_outer_band", outerBand));
            List<Point> castles = ring5.castles();
            if (!castles.isEmpty()) {
                targets.add((l, angle, round) -> new Placed(castles.get(round % castles.size()), "ring_5_castle"));
            }
        }
        return targets;
    }

    private static String name(int ring) {
        return "ring_" + (ring + 1);
    }

    /** Effective distance where the zone of {@code boundary} starts (its inner edge). */
    private static double zoneStart(RingSettings settings, int boundary) {
        return settings.ringOuterRadii().get(boundary) - settings.transitionWidth(boundary) / 2.0;
    }

    /** Effective distance where the zone of {@code boundary} ends (its outer edge). */
    private static double zoneEnd(RingSettings settings, int boundary) {
        return settings.ringOuterRadii().get(boundary) + settings.transitionWidth(boundary) / 2.0;
    }

    private static Target radial(String region, double distance) {
        return (layout, angle, round) -> new Placed(along(layout, distance, angle), region);
    }

    /**
     * The first column along the circle of {@code distance} from {@code angle} on that passes {@code test}, else the
     * start as {@link #NO_GLITCH_FIELD}.
     */
    private static Placed search(RingLayout layout, double distance, double angle, ColumnTest test) {
        for (int step = 0; step < SEARCH_STEPS; step++) {
            double candidate = angle + step * SEARCH_STEP;
            if (offAxis(candidate) != candidate) {
                continue;
            }
            Point point = along(layout, distance, candidate);
            if (test.test((int) Math.floor(point.x()), (int) Math.floor(point.z()))) {
                return new Placed(point, "ring_5_glitch_field");
            }
        }
        return new Placed(along(layout, distance, angle), NO_GLITCH_FIELD);
    }

    /**
     * The overworld column on the ray at {@code angle} whose effective distance (with the boundary noise) is
     * {@code distance}: the ring layout's own measure, so the spot lies in the region it is meant for.
     */
    static Point along(RingLayout layout, double distance, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        // The boundary noise shifts the effective distance by at most boundaryNoise: the crossing lies in between.
        double noise = layout.settings().boundaryNoise() + 1.0;
        double low = Math.max(0.0, distance - noise);
        double high = distance + noise;
        for (int i = 0; i < BISECTIONS; i++) {
            double middle = (low + high) / 2.0;
            if (layout.effectiveDistance(middle * cos, middle * sin) < distance) {
                low = middle;
            } else {
                high = middle;
            }
        }
        double radius = (low + high) / 2.0;
        return new Point(radius * cos, radius * sin);
    }

    /** {@code angle} (radians), turned away from an axis if it lies within {@link #AXIS_CLEARANCE}. */
    static double offAxis(double angle) {
        double clearance = Math.toRadians(AXIS_CLEARANCE);
        double quarter = Math.PI / 2.0;
        double within = angle - Math.floor(angle / quarter) * quarter;
        if (within < clearance) {
            return angle + clearance;
        }
        if (within > quarter - clearance) {
            return angle - clearance;
        }
        return angle;
    }
}
