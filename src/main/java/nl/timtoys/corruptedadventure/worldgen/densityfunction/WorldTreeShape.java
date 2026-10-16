package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleBinaryOperator;

/**
 * The shape of one world tree of ring 4 (74, Q7 = A: all terrain): a giant {@code moonpetal} standing on a half
 * torn-out island ({@link IslandShape#TREE}), its roots reaching from under the island down into the ground beyond the
 * island's hole, so they hold the island over its hole lake. Trunk 8–12 across with a flared, fluted foot; 6–8 branches
 * curving up into the crown; the crown a dome over the trunk with a lumpy cluster of blossom at every branch's end.
 * <p>
 * Every part is a convex body (a vertical trunk whose radius only shrinks upwards, tubes of spheres along curves,
 * ellipsoids), so a part meets a vertical column in one range of blocks: {@link #slice} computes the column's ranges
 * once, and a block then only compares its Y. The crown's outer shell is thinned per block by a hash, so its edge is
 * leafy, not a smooth ellipsoid. Pure value: a function of its parameters.
 */
final class WorldTreeShape {
    /** How far the trunk reaches into the island under it. */
    static final int ROOTED = 4;
    /** The flare of the trunk's foot (blocks, on top of the radius) and the height over which it fades. */
    private static final double FLARE = 3.5;
    private static final double FLARE_HEIGHT = 5.0;
    /** The trunk tapers by this share from its foot to the crown. */
    private static final double TAPER = 0.3;
    /** The fluting of the trunk: radius × (1 ± this), five ribs round. */
    private static final double FLUTE = 0.1;
    /** The thickness (blocks) of the crown's thinned outer shell, and the share of its blocks that are leaves. */
    static final double SHELL = 1.6;
    private static final int SHELL_KEEP = 150;
    /** Spacing (blocks) of the spheres along a branch or root. */
    private static final double TUBE_STEP = 0.7;

    final double x;
    final double z;
    final IslandShape island;
    /** The first block above the island's top at the trunk: where the tree stands. */
    final int base;
    /** The highest block of the crown. */
    final int top;
    final double trunkRadius;
    final int trunkTop;
    private final double flutePhase;
    final List<Tube> branches;
    final List<Tube> roots;
    final List<Blob> leaves;
    private final long seed;
    /** How far any part reaches from the trunk's axis (blocks). */
    final double reach;

    /**
     * @param height the tree's height from its foot to the top of the crown (80–140)
     * @param ground the ground's height at a position (where the roots end, buried)
     */
    WorldTreeShape(double x, double z, IslandShape island, double height, long hash, DoubleBinaryOperator ground) {
        this.x = x;
        this.z = z;
        this.island = island;
        this.seed = hash;
        this.base = island.highest() + 1;
        this.trunkRadius = 4.0 + 2.0 * SeededHash.unit(hash, 0);
        this.flutePhase = SeededHash.unit(hash, 1) * Math.PI * 2.0;
        double crown = Mth.clamp(0.32 * height, 26.0, 42.0);
        int crownTop = base + (int) Math.round(height) - 1;
        // The dome over the trunk; the trunk runs up into its middle.
        double domeRx = 0.6 * crown;
        double domeRy = 0.55 * domeRx;
        double domeY = crownTop + 0.5 - domeRy;
        this.trunkTop = Mth.floor(domeY);
        List<Blob> blobs = new ArrayList<>();
        blobs.add(new Blob(x, domeY, z, domeRx, domeRy));
        List<Tube> limbs = new ArrayList<>();
        int count = 6 + (int) (SeededHash.unit(hash, 2) * 3.0);
        double start = SeededHash.unit(hash, 3) * Math.PI * 2.0;
        double thickness = trunkRadius / 5.0;
        for (int b = 0; b < count; b++) {
            long h = SeededHash.mix(hash + 31L * (b + 1));
            // Golden-angle steps round the trunk, climbing it: the lower branches reach furthest (a broad canopy).
            double phi = start + b * 2.399963 + (SeededHash.unit(h, 0) - 0.5) * 0.5;
            double up = (b + SeededHash.unit(h, 1)) / count;
            double from = base + (0.4 + 0.4 * up) * (trunkTop - base);
            double length = crown * (0.6 + 0.3 * SeededHash.unit(h, 2)) * (1.0 - 0.35 * up);
            double rise = length * (0.25 + 0.3 * SeededHash.unit(h, 3));
            double endY = Math.min(from + rise, domeY + 2.0);
            double cos = Math.cos(phi);
            double sin = Math.sin(phi);
            limbs.add(Tube.curve(x, from, z, x + 0.55 * length * cos, from + 0.2 * (endY - from), z + 0.55 * length * sin,
                    x + length * cos, endY, z + length * sin, 2.6 * thickness, 1.3 * thickness));
            // A cluster of blossom at the branch's end: one big lump and two smaller ones beside it.
            double rx = 0.42 * crown * (0.85 + 0.3 * SeededHash.unit(h, 4));
            double ry = 0.6 * rx;
            double cy = Math.min(endY + 0.25 * ry, crownTop + 0.5 - ry);
            blobs.add(new Blob(x + length * cos, cy, z + length * sin, rx, ry));
            for (int side = -1; side <= 1; side += 2) {
                double a = phi + side * 0.9;
                double sub = 0.6 * rx;
                blobs.add(new Blob(x + length * cos + 0.45 * rx * Math.cos(a), cy - 0.1 * ry, z + length * sin + 0.45 * rx * Math.sin(a),
                        sub, 0.55 * sub));
            }
        }
        this.branches = List.copyOf(limbs);
        this.leaves = List.copyOf(blobs);
        // The roots: from under the island out over its hole and down into the ground beyond its rim.
        List<Tube> legs = new ArrayList<>();
        int rootCount = 7 + (int) (SeededHash.unit(hash, 4) * 4.0);
        double rootStart = SeededHash.unit(hash, 5) * Math.PI * 2.0;
        for (int i = 0; i < rootCount; i++) {
            long h = SeededHash.mix(hash + 977L * (i + 1));
            double sector = Math.PI * 2.0 / rootCount;
            double theta = rootStart + i * sector + (SeededHash.unit(h, 0) - 0.5) * 0.5 * sector;
            double cos = Math.cos(theta);
            double sin = Math.sin(theta);
            double edge = edge(theta, false);
            double hole = edge(theta, true);
            double rs = (0.3 + 0.25 * SeededHash.unit(h, 1)) * edge;
            double sx = x + rs * cos;
            double sz = z + rs * sin;
            double sy = island.bottomAt(island.distance(sx, sz), island.theta(sx, sz)) + 3.0;
            double re = hole + 6.0 + 8.0 * SeededHash.unit(h, 2);
            double ex = x + re * cos;
            double ez = z + re * sin;
            double ey = ground.applyAsDouble(ex, ez) - 5.0;
            double r0 = (2.4 + 1.2 * SeededHash.unit(h, 3)) * thickness;
            legs.add(Tube.curve(sx, sy, sz, x + 0.9 * re * cos, sy - 0.15 * (sy - ey), z + 0.9 * re * sin, ex, ey, ez,
                    r0, Math.max(1.2, 0.4 * r0 + 0.6)));
        }
        this.roots = List.copyOf(legs);
        int highest = Integer.MIN_VALUE;
        double far = (trunkRadius + FLARE) * (1.0 + FLUTE);
        for (Blob blob : leaves) {
            highest = Math.max(highest, Mth.floor(blob.y + blob.ry - 0.5));
            far = Math.max(far, Math.hypot(blob.x - x, blob.z - z) + blob.rx);
        }
        for (Tube tube : limbs) {
            far = Math.max(far, tube.far(x, z));
        }
        for (Tube tube : legs) {
            far = Math.max(far, tube.far(x, z));
        }
        this.top = highest;
        this.reach = far + 1.0;
    }

    /** How far from the trunk's axis the island's outline (or its hole's) lies along angle {@code theta}. */
    private double edge(double theta, boolean hole) {
        double cos = Math.cos(theta);
        double sin = Math.sin(theta);
        double r = 0.0;
        while (r < 200.0) {
            double px = x + r * cos;
            double pz = z + r * sin;
            if ((hole ? island.holeDistance(px, pz) : island.distance(px, pz)) >= 1.0) {
                return r;
            }
            r += 0.5;
        }
        return r;
    }

    /** The public view ({@link IslandField.WorldTree}). */
    IslandField.WorldTree view() {
        return new IslandField.WorldTree(Mth.floor(x), Mth.floor(z), base, top, (int) Math.ceil(reach));
    }

    /** Whether the tree may have a block in the column (its bounding square). */
    boolean touches(int bx, int bz) {
        return Math.abs(bx + 0.5 - x) <= reach && Math.abs(bz + 0.5 - z) <= reach;
    }

    /** Whether the tree's reach comes into the box (block coordinates, inclusive). */
    boolean reaches(int minX, int minZ, int maxX, int maxZ) {
        return x + reach >= minX && x - reach <= maxX + 1 && z + reach >= minZ && z - reach <= maxZ + 1;
    }

    /** The tree's blocks in column (bx, bz), or null for none. */
    Slice slice(int bx, int bz) {
        if (!touches(bx, bz)) {
            return null;
        }
        Slice slice = new Slice(seed);
        double px = bx + 0.5;
        double pz = bz + 0.5;
        trunk(px, pz, slice);
        double[] range = new double[2];
        for (Tube tube : branches) {
            if (tube.range(px, pz, range)) {
                slice.add(range, Slice.WOOD);
            }
        }
        for (Tube tube : roots) {
            if (tube.range(px, pz, range)) {
                slice.add(range, Slice.WOOD);
            }
        }
        // The crown's core is all leaves, the shell around it (the rest of each ellipsoid) thinned per block.
        for (Blob blob : leaves) {
            if (blob.range(px, pz, SHELL, range)) {
                slice.add(range, Slice.LEAVES);
            }
        }
        for (Blob blob : leaves) {
            if (blob.range(px, pz, 0.0, range)) {
                slice.add(range, Slice.SHELL);
            }
        }
        return slice.count == 0 ? null : slice;
    }

    /** The trunk in a column: from {@link #ROOTED} under the base up to where its radius falls short of the column. */
    private void trunk(double px, double pz, Slice slice) {
        double dx = px - x;
        double dz = pz - z;
        double rho = Math.sqrt(dx * dx + dz * dz);
        double flute = 1.0 + FLUTE * Math.sin(5.0 * Math.atan2(dz, dx) + flutePhase);
        int lo = base - ROOTED;
        if (trunkRadiusAt(lo + 0.5) * flute < rho) {
            return;
        }
        // The radius only shrinks upwards: the highest block whose centre is within it.
        int a = lo;
        int b = trunkTop;
        if (trunkRadiusAt(b + 0.5) * flute >= rho) {
            a = b;
        }
        while (a < b) {
            int mid = (a + b + 1) >>> 1;
            if (trunkRadiusAt(mid + 0.5) * flute >= rho) {
                a = mid;
            } else {
                b = mid - 1;
            }
        }
        slice.add(lo, a, Slice.LOG);
    }

    /** The trunk's radius at height {@code y}: tapering up, flared at the foot. */
    double trunkRadiusAt(double y) {
        double up = Math.max(0.0, y - base);
        double taper = 1.0 - TAPER * Math.min(1.0, up / Math.max(1.0, trunkTop - base));
        return trunkRadius * taper + FLARE * Math.exp(-up / FLARE_HEIGHT);
    }

    /** Whether the tree has no block in column (bx, bz) from {@code lo} to {@code hi} (inclusive). */
    boolean clear(int bx, int bz, int lo, int hi) {
        Slice slice = slice(bx, bz);
        return slice == null || slice.max < lo || slice.min > hi || !slice.overlaps(lo, hi);
    }

    /** A tube of spheres along a quadratic curve, its radius changing linearly from start to end. */
    record Tube(double[] px, double[] py, double[] pz, double[] pr, double minX, double minZ, double maxX, double maxZ) {
        static Tube curve(double x0, double y0, double z0, double x1, double y1, double z1, double x2, double y2, double z2,
                          double r0, double r2) {
            double length = Math.sqrt(Mth.square(x1 - x0) + Mth.square(y1 - y0) + Mth.square(z1 - z0))
                    + Math.sqrt(Mth.square(x2 - x1) + Mth.square(y2 - y1) + Mth.square(z2 - z1));
            int n = Math.max(2, (int) Math.ceil(length / TUBE_STEP) + 1);
            double[] px = new double[n];
            double[] py = new double[n];
            double[] pz = new double[n];
            double[] pr = new double[n];
            double minX = Double.MAX_VALUE;
            double minZ = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double maxZ = -Double.MAX_VALUE;
            for (int k = 0; k < n; k++) {
                double t = k / (n - 1.0);
                double u = 1.0 - t;
                px[k] = u * u * x0 + 2.0 * u * t * x1 + t * t * x2;
                py[k] = u * u * y0 + 2.0 * u * t * y1 + t * t * y2;
                pz[k] = u * u * z0 + 2.0 * u * t * z1 + t * t * z2;
                pr[k] = r0 + (r2 - r0) * t;
                minX = Math.min(minX, px[k] - pr[k]);
                minZ = Math.min(minZ, pz[k] - pr[k]);
                maxX = Math.max(maxX, px[k] + pr[k]);
                maxZ = Math.max(maxZ, pz[k] + pr[k]);
            }
            return new Tube(px, py, pz, pr, minX, minZ, maxX, maxZ);
        }

        /** How far (blocks) the tube reaches horizontally from (cx, cz). */
        double far(double cx, double cz) {
            double far = 0.0;
            for (int k = 0; k < px.length; k++) {
                far = Math.max(far, Math.hypot(px[k] - cx, pz[k] - cz) + pr[k]);
            }
            return far;
        }

        /** The heights the tube covers on the vertical line through (x, z), into {@code range}; false for none. */
        boolean range(double x, double z, double[] range) {
            if (x < minX || x > maxX || z < minZ || z > maxZ) {
                return false;
            }
            double lo = Double.MAX_VALUE;
            double hi = -Double.MAX_VALUE;
            for (int k = 0; k < px.length; k++) {
                double dx = x - px[k];
                double dz = z - pz[k];
                double r = pr[k];
                double h = dx * dx + dz * dz;
                if (h < r * r) {
                    double half = Math.sqrt(r * r - h);
                    lo = Math.min(lo, py[k] - half);
                    hi = Math.max(hi, py[k] + half);
                }
            }
            range[0] = lo;
            range[1] = hi;
            return lo <= hi;
        }
    }

    /** An ellipsoid of leaves (horizontal radius {@code rx}, vertical {@code ry}). */
    record Blob(double x, double y, double z, double rx, double ry) {
        /** The heights the ellipsoid, shrunk by {@code inset} blocks, covers on the line through (px, pz). */
        boolean range(double px, double pz, double inset, double[] range) {
            double ax = rx - inset;
            double ay = ry - inset;
            if (ax <= 0.0 || ay <= 0.0) {
                return false;
            }
            double dx = px - x;
            double dz = pz - z;
            double q = (dx * dx + dz * dz) / (ax * ax);
            if (q >= 1.0) {
                return false;
            }
            double half = ay * Math.sqrt(1.0 - q);
            range[0] = y - half;
            range[1] = y + half;
            return true;
        }
    }

    /**
     * The tree's blocks in one column: ranges with their part, in priority order (trunk, then branches and roots, then
     * the crown's core, then its thinned shell: the first range holding a block decides).
     */
    static final class Slice {
        static final byte LOG = 1;
        static final byte WOOD = 2;
        static final byte LEAVES = 3;
        /** The crown's outer shell: leaves where the block's hash keeps them. */
        static final byte SHELL = 4;
        private final long seed;
        int count;
        int[] lo = new int[8];
        int[] hi = new int[8];
        byte[] part = new byte[8];
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        Slice(long seed) {
            this.seed = seed;
        }

        /** A continuous range: the blocks whose centre lies in it. */
        void add(double[] range, byte kind) {
            add(Mth.ceil(range[0] - 0.5), Mth.floor(range[1] - 0.5), kind);
        }

        void add(int from, int to, byte kind) {
            if (from > to) {
                return;
            }
            if (count == lo.length) {
                lo = java.util.Arrays.copyOf(lo, count * 2);
                hi = java.util.Arrays.copyOf(hi, count * 2);
                part = java.util.Arrays.copyOf(part, count * 2);
            }
            lo[count] = from;
            hi[count] = to;
            part[count] = kind;
            count++;
            min = Math.min(min, from);
            max = Math.max(max, to);
        }

        WorldTreePart part(int x, int y, int z) {
            if (y < min || y > max) {
                return WorldTreePart.NONE;
            }
            for (int k = 0; k < count; k++) {
                if (y < lo[k] || y > hi[k]) {
                    continue;
                }
                switch (part[k]) {
                    case LOG:
                        return WorldTreePart.LOG;
                    case WOOD:
                        return WorldTreePart.WOOD;
                    case LEAVES:
                        return WorldTreePart.LEAVES;
                    default:
                        if (kept(x, y, z)) {
                            return WorldTreePart.LEAVES;
                        }
                }
            }
            return WorldTreePart.NONE;
        }

        boolean solid(int x, int y, int z) {
            return part(x, y, z) != WorldTreePart.NONE;
        }

        /** Whether any range (the shell counted whole) overlaps [from, to]. */
        boolean overlaps(int from, int to) {
            for (int k = 0; k < count; k++) {
                if (lo[k] <= to && hi[k] >= from) {
                    return true;
                }
            }
            return false;
        }

        private boolean kept(int x, int y, int z) {
            long h = SeededHash.mix(seed ^ x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L);
            return (h >>> 56) < SHELL_KEEP;
        }
    }
}
