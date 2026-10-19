package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.RandomSupport;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where the giant verdant trees stand (ring 4 decor D, 2026-10-19): one beside each horseshoe gorge of
 * {@code verdant_falls} (the big falls; {@link IslandField#gorgesNear}), on the flat collar behind the amphitheatre's
 * wall, looking out over the falls. A gorge tries {@link #ANGLES} × {@link #BEHIND} places behind its wall line in an
 * order of its own and takes the first that is flat dry ground, clear of the falls and the climbing knobs; none fits:
 * no tree.
 * <p>
 * Pure: the gorges, the columns and the falls are functions of the seed, so every chunk finds the same spot, and the
 * chunk that holds it places the tree ({@link GiantTreePlacement}); other trees keep clear of it
 * ({@link GiantTreeClearance}). Remembered per field.
 */
final class VerdantGiantTrees {
    /** The places tried round the arc, as shares of a quarter turn from its back (0) towards its ends (±1). */
    static final double[] ANGLES = {0.0, 0.3, -0.3, 0.55, -0.55, 0.75, -0.75};
    /** How far behind the (unwobbled) wall line a place lies: past the rim lake and the wall's wobble. */
    static final double[] BEHIND = {17.0, 21.0, 25.0};
    /** The ground within {@link #FLAT_REACH} of the trunk lies within {@link #FLAT_TOLERANCE} of its own height. */
    static final int FLAT_REACH = 3;
    static final double FLAT_TOLERANCE = 1.0;
    /** No still water within this many blocks of the trunk (the crown may reach over a lake, the roots not into it). */
    static final int DRY_REACH = 5;
    static final double MAX_SLOPE = 0.5;
    /** The crown's reach and one more: no fall or climbing knob under the tree. */
    static final int FALL_CLEARANCE = 13;
    static final int KNOB_CLEARANCE = 13;
    /** A gorge's spot lies at most this far from its centre (the half width and depth stay under 80, plus BEHIND). */
    static final double MAX_SPOT_DISTANCE = 120.0;
    private static final long SALT = 0x61A7_7EE5L;

    private static final Map<IslandField, Map<Integer, Optional<Spot>>> MEMO = Collections.synchronizedMap(new WeakHashMap<>());

    /** A giant tree's trunk column (its −x/−z corner) and the height of the ground's top block there. */
    record Spot(int x, int z, int ground) {
    }

    private VerdantGiantTrees() {
    }

    /** The giant trees whose trunk lies within {@code reach} blocks of the box (the gorges near its middle). */
    static List<Spot> near(IslandField field, int minX, int minZ, int maxX, int maxZ, int reach) {
        List<Spot> found = new ArrayList<>();
        for (IslandField.GorgeSite gorge : field.gorgesNear((minX + maxX) >> 1, (minZ + maxZ) >> 1)) {
            double nearX = Mth.clamp(gorge.centreX(), minX, maxX);
            double nearZ = Mth.clamp(gorge.centreZ(), minZ, maxZ);
            if (Math.hypot(nearX - gorge.centreX(), nearZ - gorge.centreZ()) > MAX_SPOT_DISTANCE + reach) {
                continue;
            }
            spot(field, gorge).filter(s -> s.x >= minX - reach && s.x <= maxX + reach && s.z >= minZ - reach && s.z <= maxZ + reach)
                    .ifPresent(found::add);
        }
        return found;
    }

    /** The gorge's giant tree, if a place fits. */
    static Optional<Spot> spot(IslandField field, IslandField.GorgeSite gorge) {
        Map<Integer, Optional<Spot>> spots = MEMO.computeIfAbsent(field, f -> new ConcurrentHashMap<>());
        Optional<Spot> known = spots.get(gorge.index());
        if (known == null) {
            // Pure: racing threads find the same spot.
            known = find(field, gorge);
            spots.put(gorge.index(), known);
        }
        return known;
    }

    /** The places in the gorge's own order, the first that fits. */
    static Optional<Spot> find(IslandField field, IslandField.GorgeSite gorge) {
        List<double[]> places = new ArrayList<>();
        for (double angle : ANGLES) {
            for (double behind : BEHIND) {
                places.add(new double[]{angle, behind});
            }
        }
        // The gorge's own order: a shuffle seeded on its index and centre (both pure in the seed).
        long seed = RandomSupport.mixStafford13(SALT ^ gorge.index() * 0x9E37_79B9L ^ Double.doubleToLongBits(gorge.centreX())
                ^ Long.rotateLeft(Double.doubleToLongBits(gorge.centreZ()), 21));
        for (int i = places.size() - 1; i > 0; i--) {
            seed = RandomSupport.mixStafford13(seed + i);
            int j = (int) Math.floorMod(seed, (long) (i + 1));
            Collections.swap(places, i, j);
        }
        for (double[] place : places) {
            int[] at = position(gorge, place[0] * 0.5 * Math.PI, place[1]);
            Optional<Spot> spot = fits(field, at[0], at[1]);
            if (spot.isPresent()) {
                return spot;
            }
        }
        return Optional.empty();
    }

    /**
     * The block {@code behind} blocks behind the gorge's wall line at angle {@code phi} along the normal of its half
     * ellipse (side axis a, outward w: E(φ) = (R sin φ, −D cos φ)).
     */
    static int[] position(IslandField.GorgeSite gorge, double phi, double behind) {
        double r = gorge.halfWidth();
        double d = gorge.depth();
        double sin = Math.sin(phi);
        double cos = Math.cos(phi);
        double na = sin / r;
        double nw = -cos / d;
        double len = Math.hypot(na, nw);
        double a = r * sin + behind * na / len;
        double w = -d * cos + behind * nw / len;
        // side = (−outZ, outX), out = (outX, outZ).
        double x = gorge.centreX() - a * gorge.outZ() + w * gorge.outX();
        double z = gorge.centreZ() + a * gorge.outX() + w * gorge.outZ();
        return new int[]{Mth.floor(x), Mth.floor(z)};
    }

    /** Whether a giant tree fits at (x, z): flat, dry ground of the terrain itself, clear of falls and knobs. */
    static Optional<Spot> fits(IslandField field, int x, int z) {
        IslandField.Column centre = field.column(x, z);
        double ground = centre.surface();
        if (centre.islandMask() > 0.0 || centre.cliff() >= 0.5 || centre.rim() >= 0.5 || field.slope(x, z) >= MAX_SLOPE
                || ground < 65.0) {
            return Optional.empty();
        }
        for (int dz = -DRY_REACH; dz <= DRY_REACH; dz++) {
            for (int dx = -DRY_REACH; dx <= DRY_REACH; dx++) {
                IslandField.Column c = field.column(x + dx, z + dz);
                if (c.lakeTop() != Integer.MIN_VALUE || c.islandMask() > 0.0) {
                    return Optional.empty();
                }
                if (Math.abs(dx) <= FLAT_REACH && Math.abs(dz) <= FLAT_REACH && Math.abs(c.surface() - ground) > FLAT_TOLERANCE) {
                    return Optional.empty();
                }
            }
        }
        if (!field.waterfallsWithin(x - FALL_CLEARANCE, z - FALL_CLEARANCE, x + FALL_CLEARANCE, z + FALL_CLEARANCE).isEmpty()
                || !field.climbKnobs(x - KNOB_CLEARANCE, z - KNOB_CLEARANCE, x + KNOB_CLEARANCE, z + KNOB_CLEARANCE).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Spot(x, z, Mth.ceil(ground) - 1));
    }
}
