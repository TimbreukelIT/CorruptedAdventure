package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The pure geometry of the sea-creature fossils of Nether ring 4's {@code bone_dunes} (78, Q15 = B, G5 = A), shared by the
 * density function {@code sea_fossils} (which makes them terrain) and the surface condition {@code sea_fossil} (which
 * paints them {@code bone_block}), so both agree on every block.
 * <p>
 * A fossil lies belly-down and half buried along a straight spine: a ribcage (arched ribs across the spine, tapering to
 * both ends, the spine on top), a neck sloping down to a hollow skull on the ground (open at the back and the mouth, two
 * eye sockets, protruding jaws with teeth) and a curving tail sinking into the dunes. Inside the ribcage, the neck and the
 * skull the floor is flat at {@link Fossil#floor} and open up to the bones (room for the fossil temples of phase 3); the
 * dunes ramp to that floor over {@link Settings#ramp} blocks round it. The gaps between the ribs are at least
 * {@link #MIN_GAP} wide.
 * <p>
 * Everything here is in block-centre coordinates: a block (x, y, z) is tested at (x + ½, y + ½, z + ½).
 */
public final class SeaFossilShape {
    private static final int SALT = 59;
    /** How many placements a cell tries, in order; the first that lies wholly in the dunes is the fossil of the cell. */
    public static final int ATTEMPTS = 128;
    /** Thickness of a rib along the spine. */
    static final double RIB_THICKNESS = 3.0;
    /** Radial thickness of a rib (and of the skull's shell: {@link #SHELL}). */
    static final double RIB_DEPTH = 2.5;
    static final double SHELL = 2.0;
    /** The narrowest gap between two ribs (walkable corridors, G5 = A). */
    public static final double MIN_GAP = 4.0;
    /** How far the ribs reach below the floor (half buried). */
    static final double BURIED = 6.0;
    static final double VERTEBRA_SPACING = 4.0;
    static final double VERTEBRA_RADIUS = 1.8;
    /** The bone mask of a column covers Y {@code floor − BELOW} .. {@code floor − BELOW + 63}. */
    public static final int BELOW = 8;
    /** A column's {@code flat_cache} value comes from its quart corner, at most this far away. */
    static final double QUART_REACH = 5.0;
    /** How far the jaws reach past the snout, as a share of the skull's length. */
    private static final double JAW_REACH = 0.25;

    private SeaFossilShape() {
    }

    /** {@link SeededHash} of a cell, whatever noise it comes from. */
    @FunctionalInterface
    public interface CellHash {
        long hash(int cellX, int cellZ, int salt);
    }

    /** A 2D function of the column (region, edge, natural floor height). */
    @FunctionalInterface
    public interface ColumnFunction {
        double at(int x, int z);
    }

    /** A closed range of doubles; in JSON {@code [min, max]} or a single number. */
    public record Span(double min, double max) {
        public static final Codec<Span> CODEC = Codec.either(Codec.DOUBLE, Codec.DOUBLE.listOf()).comapFlatMap(Span::decode,
                span -> span.min == span.max ? Either.left(span.min) : Either.right(List.of(span.min, span.max)));

        private static DataResult<Span> decode(Either<Double, List<Double>> either) {
            return either.map(value -> DataResult.success(new Span(value, value)), list -> list.size() == 2 && list.get(0) <= list.get(1)
                    ? DataResult.success(new Span(list.get(0), list.get(1)))
                    : DataResult.error(() -> "A span needs [min, max] with min <= max, got " + list));
        }

        double at(double t) {
            return min + t * (max - min);
        }
    }

    /**
     * @param cellSize   one fossil per cell at most; the whole fossil (with its ramp) stays inside its cell
     * @param ramp       how far round the fossil's floor the dunes ramp to it
     * @param region     the region axis must lie in this span over the fossil's whole rectangle (only the bone dunes)
     * @param sampleStep grid step of the placement test over that rectangle
     * @param burial     how deep the ribs' arch centre lies under the floor, as a share of the rib's height
     */
    public record Settings(int cellSize, int ramp, Span region, int sampleStep, Span cageLength, Span halfWidth, Span height,
                           Span ribPeriod, Span burial, Span neckLength, Span skullLength, Span skullWidth, Span skullHeight,
                           Span tailLength, Span tailSwing) {
        public static final Codec<Settings> CODEC = RecordCodecBuilder.<Settings>create(instance -> instance.group(
                Codec.intRange(64, 1024).fieldOf("cell_size").forGetter(Settings::cellSize),
                Codec.intRange(0, 32).fieldOf("ramp").forGetter(Settings::ramp),
                Span.CODEC.fieldOf("region").forGetter(Settings::region),
                Codec.intRange(1, 16).fieldOf("sample_step").forGetter(Settings::sampleStep),
                Span.CODEC.fieldOf("cage_length").forGetter(Settings::cageLength),
                Span.CODEC.fieldOf("half_width").forGetter(Settings::halfWidth),
                Span.CODEC.fieldOf("height").forGetter(Settings::height),
                Span.CODEC.fieldOf("rib_period").forGetter(Settings::ribPeriod),
                Span.CODEC.fieldOf("burial").forGetter(Settings::burial),
                Span.CODEC.fieldOf("neck_length").forGetter(Settings::neckLength),
                Span.CODEC.fieldOf("skull_length").forGetter(Settings::skullLength),
                Span.CODEC.fieldOf("skull_width").forGetter(Settings::skullWidth),
                Span.CODEC.fieldOf("skull_height").forGetter(Settings::skullHeight),
                Span.CODEC.fieldOf("tail_length").forGetter(Settings::tailLength),
                Span.CODEC.fieldOf("tail_swing").forGetter(Settings::tailSwing)
        ).apply(instance, Settings::new)).validate(Settings::validate);

        private static DataResult<Settings> validate(Settings s) {
            if (s.cellSize % 16 != 0) {
                return DataResult.error(() -> "sea fossil: cell_size must be a multiple of 16 (a chunk lies in one cell)");
            }
            if (s.ribPeriod.min - RIB_THICKNESS < MIN_GAP + 1.0) {
                return DataResult.error(() -> "sea fossil: rib_period too small for gaps of " + MIN_GAP + " (+1 for the block grid)");
            }
            if (s.burial.min < 0.0 || s.burial.max > 0.5) {
                return DataResult.error(() -> "sea fossil: burial must lie in [0, 0.5]");
            }
            if (s.height.min < 8.0 || s.halfWidth.min < 6.0 || s.skullWidth.min < 5.0 || s.skullHeight.min < 6.0) {
                return DataResult.error(() -> "sea fossil: too small to walk into");
            }
            if (s.height.max + 12.0 > 64 - BELOW) {
                return DataResult.error(() -> "sea fossil: too high for the column's bone mask");
            }
            if (s.cellSize - 2 * s.margin() < 4) {
                return DataResult.error(() -> "sea fossil: cell_size too small for the fossil");
            }
            return DataResult.success(s);
        }

        /** Distance from a cell edge to the nearest possible origin: the whole fossil with its ramp fits in the cell. */
        public int margin() {
            double front = cageLength.max * 0.5 + neckLength.max + skullLength.max * (1.0 + JAW_REACH);
            double back = cageLength.max * 0.5 + tailLength.max + VERTEBRA_RADIUS;
            double side = Math.max(Math.max(halfWidth.max, skullWidth.max), tailSwing.max + VERTEBRA_RADIUS);
            return (int) Math.ceil(Math.hypot(Math.max(front, back), side) + ramp) + 2;
        }
    }

    /** One rib across the spine at {@code s}; {@code broken} 0 = whole, −1/+1 = that side only up to {@code breakHeight}. */
    record Rib(double s, double halfWidth, double height, double burial, int broken, double breakHeight) {
    }

    /** One vertebra: centre ({@code x}, {@code y}, {@code z}) in world coordinates, radius, neural spine above it. */
    record Vertebra(double x, double y, double z, double radius, double spine) {
    }

    /**
     * A placed fossil. Local frame: {@code s} along the spine (towards the head), {@code l} across it; the ribcage spans
     * s ∈ [−{@code cageHalf}, {@code cageHalf}], the skull s ∈ [{@code skullStart}, {@code skullStart + skullLength}].
     *
     * @param floor the flat floor inside: the first open Y (blocks below it are solid)
     */
    public record Fossil(double x, double z, double cos, double sin, int floor, double cageHalf, double halfWidth, double height,
                         double burial, List<Rib> ribs, List<Vertebra> vertebrae, double skullStart, double skullLength,
                         double skullWidth, double skullHeight, double sMin, double sMax, double lMax, int ramp) {
        public double along(double px, double pz) {
            return (px - x) * cos + (pz - z) * sin;
        }

        public double across(double px, double pz) {
            return -(px - x) * sin + (pz - z) * cos;
        }

        /** Whether the column may hold anything of the fossil (bones, floor or ramp): its rectangle in the local frame. */
        public boolean touches(int blockX, int blockZ) {
            double px = blockX + 0.5;
            double pz = blockZ + 0.5;
            double s = along(px, pz);
            if (s < sMin - ramp || s > sMax + ramp) {
                return false;
            }
            return Math.abs(across(px, pz)) <= lMax + ramp;
        }

        /** The lowest Y of the column's bone mask. */
        public int maskBase() {
            return floor - BELOW;
        }

        /** Rib cross-section scale at s along the cage: 1 in the middle, ~0.55 at both ends. */
        double cageScale(double s) {
            double t = Math.min(1.0, Math.max(0.0, (s + cageHalf) / (2.0 * cageHalf)));
            return 0.55 + 0.45 * Math.sin(Math.PI * (0.1 + 0.8 * t));
        }

        /** Height of the ribcage's arch above the floor at s (the underside of the spine). */
        double cageTop(double s) {
            return height * cageScale(s);
        }

        double skullHalfWidth(double u) {
            return skullWidth * (1.0 - 0.6 * Math.pow(Math.min(1.0, Math.max(0.0, u)), 1.6));
        }

        double skullTop(double u) {
            return skullHeight * (1.0 - 0.65 * Math.min(1.0, Math.max(0.0, u)));
        }

        /** Half width of the flat floor (inside the bones) at s, or −1 outside the ribcage, neck and skull. */
        public double floorHalfWidth(double s) {
            if (s < -cageHalf || s > skullStart + skullLength) {
                return -1.0;
            }
            if (s <= cageHalf) {
                double f = cageScale(s);
                return atFloor(halfWidth * f, height * f, burial * height * f);
            }
            double skullBack = skullFloorHalfWidth(0.0);
            if (s < skullStart) {
                double cageFront = atFloor(halfWidth * cageScale(cageHalf), height * cageScale(cageHalf), burial * height * cageScale(cageHalf));
                double t = (s - cageHalf) / (skullStart - cageHalf);
                return cageFront + t * (skullBack - cageFront);
            }
            return skullFloorHalfWidth((s - skullStart) / skullLength);
        }

        private double skullFloorHalfWidth(double u) {
            double h = skullTop(u);
            return atFloor(skullHalfWidth(u), h, h * 0.2);
        }

        /** Lateral extent at the floor of an arch of half width a and height h above the floor with its centre c below it. */
        private static double atFloor(double a, double h, double c) {
            double b = h + c;
            return a * Math.sqrt(Math.max(0.0, 1.0 - (c / b) * (c / b)));
        }

        /**
         * How far the column lies outside the flat floor (0 inside it): to the nearest point of the floor's outline,
         * measured across where it can and along the spine past the ends.
         */
        public double floorDistance(double px, double pz) {
            double s = along(px, pz);
            double l = Math.abs(across(px, pz));
            double end = skullStart + skullLength;
            double clamped = Math.min(end, Math.max(-cageHalf, s));
            double ds = Math.abs(s - clamped);
            double dl = Math.max(0.0, l - floorHalfWidth(clamped));
            return Math.sqrt(ds * ds + dl * dl);
        }

        /** Floor weight of the column: 1 on the flat floor, falling smoothly to 0 at {@link #ramp} blocks outside it. */
        public double floorWeight(int blockX, int blockZ) {
            double d = floorDistance(blockX + 0.5, blockZ + 0.5);
            if (d <= 0.0) {
                return 1.0;
            }
            if (ramp <= 0 || d >= ramp) {
                return 0.0;
            }
            double t = 1.0 - d / ramp;
            return t * t * (3.0 - 2.0 * t);
        }

        /**
         * The bone of a column as a bit mask: bit i is the block at Y {@link #maskBase()} + i. Pure; one call per column
         * is enough for every block of it.
         */
        public long boneMask(int blockX, int blockZ) {
            double px = blockX + 0.5;
            double pz = blockZ + 0.5;
            double s = along(px, pz);
            double l = across(px, pz);
            long mask = 0L;
            for (Rib rib : ribs) {
                if (Math.abs(s - rib.s()) <= RIB_THICKNESS * 0.5) {
                    mask |= rib(rib, l);
                }
            }
            for (Vertebra v : vertebrae) {
                double dx = px - v.x();
                double dz = pz - v.z();
                double d2 = dx * dx + dz * dz;
                if (d2 < v.radius() * v.radius()) {
                    double half = Math.sqrt(v.radius() * v.radius() - d2);
                    mask |= bits(v.y() - half, v.y() + half);
                }
                if (v.spine() > 0.0 && d2 <= 0.6 * 0.6) {
                    mask |= bits(v.y(), v.y() + v.radius() + v.spine());
                }
            }
            mask |= skull(s, l);
            return mask;
        }

        private long rib(Rib rib, double l) {
            double c = rib.burial();
            double a = rib.halfWidth();
            double b = rib.height() + c;
            if (Math.abs(l) >= a) {
                return 0L;
            }
            long arch = band(l, a, b, c, RIB_DEPTH, -BURIED);
            if (rib.broken() != 0 && Math.signum(l) == rib.broken() && Math.abs(l) > 0.35 * a) {
                // snapped: on this side the rib hangs from the spine down to its break, the lower part is gone
                arch &= bits(rib.breakHeight() + floor, 1000.0);
            }
            return arch;
        }

        private long skull(double s, double l) {
            double u = (s - skullStart) / skullLength;
            if (u < 0.0 || u > 1.0 + JAW_REACH) {
                return 0L;
            }
            long mask = 0L;
            double w = skullHalfWidth(u);
            double h = skullTop(u);
            double c = h * 0.2;
            if (u <= 1.0 && Math.abs(l) < w) {
                mask = band(l, w, h + c, c, SHELL, -3.0);
                // the eye sockets: holes in the shell on both sides, a third back from the snout
                double ue = 0.3;
                double se = skullStart + ue * skullLength;
                double le = skullHalfWidth(ue) - 1.0;
                double ve = 0.5 * skullTop(ue);
                double re = Math.max(2.0, Math.min(3.5, skullWidth * 0.3));
                double ds = s - se;
                double dl = Math.abs(l) - le;
                double d2 = ds * ds + dl * dl;
                if (d2 < re * re) {
                    double half = Math.sqrt(re * re - d2);
                    mask &= ~bits(floor + ve - half, floor + ve + half);
                }
            }
            // the jaws: a bar along each side at the floor, reaching past the snout, with teeth on the front part
            if (u >= 0.15) {
                double jaw = skullHalfWidth(Math.min(u, 1.0)) * 0.85;
                double off = Math.abs(Math.abs(l) - jaw);
                if (off <= 1.0) {
                    // the jaw's top block is the one on the floor (Y floor); teeth stand on it, two high
                    mask |= bits(floor - 1.5, floor + 0.6);
                    double phase = (s - skullStart) / 3.0;
                    if (u >= 0.55 && off <= 0.6 && phase - Math.floor(phase) < 0.34) {
                        mask |= bits(floor + 0.6, floor + 2.6);
                    }
                }
            }
            return mask;
        }

        /**
         * An elliptic arch band at lateral offset l: outer half axes (a, b) round a centre c below the floor, the inside
         * {@code depth} thinner; only the part above {@code lowest} (relative to the floor).
         */
        private long band(double l, double a, double b, double c, double depth, double lowest) {
            double outer = b * Math.sqrt(Math.max(0.0, 1.0 - (l / a) * (l / a)));
            double ai = a - depth;
            double bi = b - depth;
            double base = floor - c;
            double low = Math.max(base - outer, floor + lowest);
            if (Math.abs(l) >= ai) {
                return bits(low, base + outer);
            }
            double inner = bi * Math.sqrt(Math.max(0.0, 1.0 - (l / ai) * (l / ai)));
            return bits(low, base - inner) | bits(base + inner, base + outer);
        }

        /** The bits of the blocks whose centre lies in [lo, hi] (absolute Y). */
        long bits(double lo, double hi) {
            int base = maskBase();
            int from = (int) Math.ceil(lo - 0.5) - base;
            int to = (int) Math.floor(hi - 0.5) - base;
            from = Math.max(from, 0);
            to = Math.min(to, 63);
            if (from > to) {
                return 0L;
            }
            long upper = to == 63 ? -1L : (1L << (to + 1)) - 1L;
            long lower = (1L << from) - 1L;
            return upper & ~lower;
        }
    }

    /**
     * The fossil of cell ({@code cellX}, {@code cellZ}): the first of {@link #ATTEMPTS} placements whose whole rectangle
     * (ramp included) lies in the bone dunes ({@code region} within {@link Settings#region}) and clear of the ring's
     * borders ({@code edge} = 0), or null. Pure.
     */
    @Nullable
    public static Fossil fossilOfCell(Settings settings, CellHash hash, int cellX, int cellZ, ColumnFunction region,
                                      ColumnFunction edge, ColumnFunction naturalFloor) {
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            Fossil fossil = candidate(settings, hash.hash(cellX, cellZ, SALT + attempt), cellX, cellZ, naturalFloor, 0);
            if (fits(settings, fossil, region, edge)) {
                return candidate(settings, hash.hash(cellX, cellZ, SALT + attempt), cellX, cellZ, naturalFloor, floorOf(settings, fossil, naturalFloor));
            }
        }
        return null;
    }

    /**
     * Whether everything of the fossil lies in the dunes and clear of the borders: its floor with the ramp round it (and
     * the jaws past the snout) and a square round every vertebra, on a grid of {@link Settings#sampleStep}, all
     * {@link #QUART_REACH} wider: the terrain picks the fossils' branch with the region as the noise chunk caches it
     * ({@code flat_cache}: the value of the column's quart corner, up to ~4.2 blocks away).
     */
    static boolean fits(Settings settings, Fossil fossil, ColumnFunction region, ColumnFunction edge) {
        double step = settings.sampleStep();
        double ramp = fossil.ramp() + 1.0 + QUART_REACH;
        double end = fossil.skullStart() + fossil.skullLength();
        for (double s = -fossil.cageHalf() - ramp; s <= fossil.sMax() + ramp; s += step) {
            double clamped = Math.min(end, Math.max(-fossil.cageHalf(), s));
            double half = Math.max(fossil.floorHalfWidth(clamped), s > end ? fossil.skullHalfWidth(1.0) : 0.0) + ramp;
            for (double l = -half; l <= half + 1e-9; l += step) {
                if (!clear(settings, fossil, s, l, region, edge)) {
                    return false;
                }
            }
            if (!clear(settings, fossil, s, half, region, edge)) {
                return false;
            }
        }
        for (Vertebra v : fossil.vertebrae()) {
            double s = fossil.along(v.x(), v.z());
            double l = fossil.across(v.x(), v.z());
            double around = v.radius() + QUART_REACH;
            for (double ds = -around; ds <= around + 1e-9; ds += step) {
                for (double dl = -around; dl <= around + 1e-9; dl += step) {
                    if (!clear(settings, fossil, s + ds, l + dl, region, edge)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean clear(Settings settings, Fossil fossil, double s, double l, ColumnFunction region, ColumnFunction edge) {
        int x = (int) Math.floor(fossil.x() + s * fossil.cos() - l * fossil.sin());
        int z = (int) Math.floor(fossil.z() + s * fossil.sin() + l * fossil.cos());
        double r = region.at(x, z);
        return r >= settings.region().min() && r <= settings.region().max() && edge.at(x, z) <= 0.0;
    }

    /** The flat floor: the mean natural floor over the fossil's floor outline, rounded. */
    static int floorOf(Settings settings, Fossil fossil, ColumnFunction naturalFloor) {
        double sum = 0.0;
        int n = 0;
        for (double s = -fossil.cageHalf(); s <= fossil.skullStart() + fossil.skullLength(); s += settings.sampleStep()) {
            double w = fossil.floorHalfWidth(s);
            for (double l = -w; l <= w; l += settings.sampleStep()) {
                sum += naturalFloor.at((int) Math.floor(fossil.x() + s * fossil.cos() - l * fossil.sin()),
                        (int) Math.floor(fossil.z() + s * fossil.sin() + l * fossil.cos()));
                n++;
            }
        }
        return (int) Math.round(sum / Math.max(1, n));
    }

    /** One placement of a cell, drawn from {@code h}, with its flat floor at {@code floor}. Pure. */
    static Fossil candidate(Settings settings, long h, int cellX, int cellZ, ColumnFunction naturalFloor, int floor) {
        int margin = settings.margin();
        int span = settings.cellSize() - 2 * margin;
        double x = cellX * settings.cellSize() + margin + SeededHash.unit(h, 1) * span;
        double z = cellZ * settings.cellSize() + margin + SeededHash.unit(h, 2) * span;
        double angle = SeededHash.unit(h, 3) * Math.PI * 2.0;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double cageHalf = settings.cageLength().at(SeededHash.unit(h, 4)) * 0.5;
        double halfWidth = settings.halfWidth().at(SeededHash.unit(h, 5));
        double height = settings.height().at(SeededHash.unit(h, 6));
        double period = settings.ribPeriod().at(SeededHash.unit(h, 7));
        double burial = settings.burial().at(SeededHash.unit(h, 8));
        double neck = settings.neckLength().at(SeededHash.unit(h, 9));
        double skullLength = settings.skullLength().at(SeededHash.unit(h, 10));
        double skullWidth = settings.skullWidth().at(SeededHash.unit(h, 11));
        double skullHeight = settings.skullHeight().at(SeededHash.unit(h, 12));
        double tail = settings.tailLength().at(SeededHash.unit(h, 13));
        double swing = settings.tailSwing().at(SeededHash.unit(h, 14)) * (SeededHash.unit(h, 15) < 0.5 ? -1.0 : 1.0);
        double skullStart = cageHalf + neck;
        double sMin = -cageHalf - tail - VERTEBRA_RADIUS;
        double sMax = skullStart + skullLength * (1.0 + JAW_REACH);
        double lMax = Math.max(Math.max(halfWidth, skullWidth), Math.abs(swing) + VERTEBRA_RADIUS);
        // a first fossil without ribs or vertebrae: the shape functions the ribs and the spine are built from
        Fossil frame = new Fossil(x, z, cos, sin, floor, cageHalf, halfWidth, height, burial, List.of(), List.of(), skullStart,
                skullLength, skullWidth, skullHeight, sMin, sMax, lMax, settings.ramp());

        int count = Math.max(2, (int) Math.floor(2.0 * cageHalf / period));
        double first = -cageHalf + (2.0 * cageHalf - (count - 1) * period) * 0.5;
        Rib[] ribs = new Rib[count];
        boolean lastMissing = true;
        int kept = 0;
        for (int k = 0; k < count; k++) {
            double s = first + k * period;
            double f = frame.cageScale(s);
            double u = SeededHash.unit(h, 100 + k);
            boolean inner = k > 0 && k < count - 1;
            if (inner && !lastMissing && u < 0.08) {
                lastMissing = true;
                continue; // a missing rib (never two in a row, never the end ribs)
            }
            lastMissing = false;
            int broken = u < 0.2 ? (SeededHash.unit(h, 200 + k) < 0.5 ? -1 : 1) : 0;
            double breakHeight = height * f * (0.3 + 0.4 * SeededHash.unit(h, 300 + k));
            ribs[kept++] = new Rib(s, halfWidth * f, height * f, burial * height * f, broken, breakHeight);
        }

        Vertebra[] vertebrae = new Vertebra[64];
        int n = 0;
        // the spine on top of the ribcage, with neural spines
        for (double s = -cageHalf; s <= cageHalf + 1e-9 && n < vertebrae.length; s += VERTEBRA_SPACING) {
            double f = frame.cageScale(s);
            vertebrae[n++] = vertebra(frame, s, 0.0, floor + frame.cageTop(s) + 1.0, VERTEBRA_RADIUS, 2.0 + 3.0 * f);
        }
        // the neck, sloping down to the back of the skull
        double neckTop = floor + frame.cageTop(cageHalf) + 1.0;
        double skullBack = floor + skullHeight * 0.8;
        for (double s = cageHalf + VERTEBRA_SPACING; s < skullStart + 2.0 && n < vertebrae.length; s += VERTEBRA_SPACING) {
            double t = (s - cageHalf) / (skullStart + 2.0 - cageHalf);
            vertebrae[n++] = vertebra(frame, s, 0.0, neckTop + t * (skullBack - neckTop), 1.6, 0.0);
        }
        // the tail: down from the back of the ribcage into the dunes, then along them, half buried, swinging aside
        double tailTop = floor + frame.cageTop(-cageHalf) + 1.0;
        for (double d = VERTEBRA_SPACING; d <= tail && n < vertebrae.length; d += VERTEBRA_SPACING) {
            double u = d / tail;
            double s = -cageHalf - d;
            double l = swing * u * Math.sin(Math.PI * u * 1.3);
            double radius = VERTEBRA_RADIUS - 0.9 * u;
            double px = x + s * cos - l * sin;
            double pz = z + s * sin + l * cos;
            double ground = naturalFloor.at((int) Math.floor(px), (int) Math.floor(pz)) - 0.5;
            double t = Math.min(1.0, u / 0.35);
            double y = tailTop + (ground - tailTop) * (t * t * (3.0 - 2.0 * t));
            vertebrae[n++] = new Vertebra(px, y, pz, radius, u < 0.5 ? 1.0 + (1.0 - 2.0 * u) : 0.0);
        }
        return new Fossil(x, z, cos, sin, floor, cageHalf, halfWidth, height, burial, List.of(java.util.Arrays.copyOf(ribs, kept)),
                List.of(java.util.Arrays.copyOf(vertebrae, n)), skullStart, skullLength, skullWidth, skullHeight, sMin, sMax, lMax,
                settings.ramp());
    }

    private static Vertebra vertebra(Fossil frame, double s, double l, double y, double radius, double spine) {
        return new Vertebra(frame.x() + s * frame.cos() - l * frame.sin(), y, frame.z() + s * frame.sin() + l * frame.cos(), radius, spine);
    }
}
