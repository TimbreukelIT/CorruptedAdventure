package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * {@code corruptedadventure:canyon_details}: the fine forms of the ring 2 canyon country, per block outside the
 * interpolation, around {@code argument}: {@code min(max(argument, rocks, additions), cuts)}. {@code rocks} are
 * {@code rock_pillars} functions: their solid intervals go into the column table here, so a block costs one table
 * lookup instead of one per rock function (a {@code max} around them would ask each of them for every block).
 * <ul>
 *     <li><b>Slots</b>: narrow deep fractures along the open edges of {@code slot_network} ({@link CellEdges}), each
 *     edge with its own width, depth and undercut; bridges (the top blocks stay) and chockstones at hashed spots
 *     along the edge. The bottom never lies below {@code min_bottom} or {@code canyon_floor} + {@code floor_clearance}.</li>
 *     <li><b>Ledges</b> on the cream strata bands and <b>niches</b> in the band below them, on steep walls only
 *     (slope of {@code height} ≥ {@code min_slope}), and the <b>rim</b>: the top blocks of a wall under
 *     {@code plateau_surface} stick out like a ledge (caprock), with a narrow undercut below. They work on the shell:
 *     {@code terrain} interpolated exactly like the noise chunk interpolates it ({@link CellInterpolation}), so also
 *     away from the block being filled. A ledge is the wall moved outward along the column's uphill direction (an
 *     open block is solid if the shell {@code reach} blocks uphill is solid), a niche or undercut the wall eroded (a
 *     solid block opens if the shell {@code depth} blocks downhill is open).</li>
 * </ul>
 * A ledge never floats: a ledge block also needs the next block towards the wall to be solid in the end result (a
 * chain of at most a few blocks, followed through the neighbouring columns), the face behind a ledge is sealed
 * against cave mouths, and ledges stay out of the low, ragged part of a wall ({@code min_height}).
 * <p>
 * Everything per column is computed once (a {@link ColumnTable}); a block outside the column's y window costs one
 * table lookup on top of {@code argument}. The 2D inputs are read at the cell corners (every 4 blocks, where the noise
 * chunk caches its {@code flat_cache} markers) and interpolated bilinearly, like the terrain itself.
 * <p>
 * Pure: a function of the seed (through the noises and the slot network) and x, y, z.
 */
public final class CanyonDetails implements DensityFunction {
    private static final int MAX_FEATURES = 32;
    private static final int MAX_ROCKS = 8;
    /** Steps from a ledge block to its wall that are followed; ledges reach at most a few blocks out. */
    private static final int MAX_CHAIN = ColumnTable.MARGIN - 1;
    /** Depth (blocks across the wall) of the face that an add feature keeps solid. */
    private static final double SEAL_BLOCKS = 2.0;
    /** Shell value below which unsealed terrain counts as skin (where noise caves open their mouths). */
    private static final double SKIN = 0.25;

    public static final MapCodec<CanyonDetails> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument").forGetter(d -> d.argument),
            DensityFunction.HOLDER_HELPER_CODEC.listOf().optionalFieldOf("rocks", List.of()).forGetter(d -> d.rocks),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("terrain").forGetter(d -> d.terrain),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("slot_network").forGetter(d -> d.slotNetwork),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("slot_weight").forGetter(d -> d.slotWeight),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("height").forGetter(d -> d.height),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("plateau_height").forGetter(d -> d.plateauHeight),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("plateau_surface").forGetter(d -> d.plateauSurface),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("canyon_floor").forGetter(d -> d.canyonFloor),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("wall_weight").forGetter(d -> d.wallWeight),
            DensityFunction.HOLDER_HELPER_CODEC.optionalFieldOf("wobble").forGetter(d -> d.wobble),
            NoiseHolder.CODEC.fieldOf("band_noise").forGetter(d -> d.bandNoise),
            NoiseHolder.CODEC.fieldOf("surface_noise").forGetter(d -> d.surfaceNoise),
            SlotSettings.CODEC.fieldOf("slots").forGetter(d -> d.slots),
            WallSettings.CODEC.fieldOf("walls").forGetter(d -> d.walls)
    ).apply(instance, CanyonDetails::new));

    public static final KeyDispatchDataCodec<CanyonDetails> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private final DensityFunction argument;
    private final List<DensityFunction> rocks;
    private final DensityFunction terrain;
    private final CellInterpolation shellAt;
    private final HeightGrid heightGrid;
    private final DensityFunction slotNetwork;
    private final DensityFunction slotWeight;
    private final DensityFunction height;
    private final DensityFunction plateauHeight;
    private final DensityFunction plateauSurface;
    private final DensityFunction canyonFloor;
    private final DensityFunction wallWeight;
    private final Optional<DensityFunction> wobble;
    private final NoiseHolder bandNoise;
    private final NoiseHolder surfaceNoise;
    private final SlotSettings slots;
    private final WallSettings walls;
    private final ColumnTable<Column> columns = new ColumnTable<>(Column::new);
    private volatile CellEdges network;

    public CanyonDetails(DensityFunction argument, List<DensityFunction> rocks, DensityFunction terrain, DensityFunction slotNetwork,
                         DensityFunction slotWeight,
                         DensityFunction height, DensityFunction plateauHeight, DensityFunction plateauSurface,
                         DensityFunction canyonFloor, DensityFunction wallWeight, Optional<DensityFunction> wobble,
                         NoiseHolder bandNoise, NoiseHolder surfaceNoise, SlotSettings slots, WallSettings walls) {
        this.argument = argument;
        this.rocks = List.copyOf(rocks);
        this.terrain = terrain;
        this.shellAt = new CellInterpolation(terrain);
        this.heightGrid = new HeightGrid(height);
        this.slotNetwork = slotNetwork;
        this.slotWeight = slotWeight;
        this.height = height;
        this.plateauHeight = plateauHeight;
        this.plateauSurface = plateauSurface;
        this.canyonFloor = canyonFloor;
        this.wallWeight = wallWeight;
        this.wobble = wobble;
        this.bandNoise = bandNoise;
        this.surfaceNoise = surfaceNoise;
        this.slots = slots;
        this.walls = walls;
    }

    @Override
    public double compute(FunctionContext context) {
        int x = context.blockX();
        int z = context.blockZ();
        Column column = columns.get(x, z, this::fillColumn);
        int y = context.blockY();
        if (y < column.lo || y > column.hi) {
            return argument.compute(context);
        }
        double value = argument.compute(context);
        if (column.rockAt(y)) {
            value = Math.max(value, 1.0);
        }
        boolean cut = false;
        if (column.hasFeatureAt(y)) {
            double s = shellAt.sample(x, y, z);
            if (s > 0.0) {
                if (column.seals(y, s)) {
                    value = Math.max(value, 1.0);
                } else {
                    cut = column.cuts(this, x, y, z, s);
                }
            } else if (value < 1.0 && column.adds(this, x, y, z, s, 0)) {
                value = 1.0;
            }
        }
        if (!cut && column.slot && y > column.slotBottom && y <= column.slotTop + 1.0
                && (y < column.bridgeLo || y > column.bridgeHi)) {
            double depth = column.slotTop - column.slotBottom;
            double frac = Mth.clamp((column.slotTop - y) / depth, 0.0, 1.0);
            double half = column.slotHalfWidth + column.slotUndercut * frac * frac / 2.0;
            if (wobble.isPresent()) {
                half += slots.wobble() * wobble.get().compute(context);
            }
            cut = column.slotDistance < half;
        }
        return cut ? Math.min(value, -1.0) : value;
    }

    private void fillColumn(Column column, int x, int z) {
        column.reset();
        for (DensityFunction rock : rocks) {
            int[] spans = RockPillars.resolve(rock).solidAt(x, z);
            for (int i = 0; i < spans.length; i += 2) {
                column.rock(spans[i], spans[i + 1]);
            }
        }
        Corners corners = new Corners(x, z);
        double surface = corners.sample(heightGrid);
        double slope = corners.slope(heightGrid);
        column.surface = surface;
        column.slope = slope;
        fillSlot(column, x, z, corners, surface);
        double wall = corners.sample(wallWeight);
        if (wall >= walls.minWeight() && slope >= walls.minSlope()) {
            fillWall(column, x, z, corners, surface, slope, Math.min(1.0, wall));
        }
    }

    private void fillSlot(Column column, int x, int z, Corners corners, double surface) {
        double weight = corners.sample(slotWeight);
        if (weight < slots.minWeight()) {
            return;
        }
        weight = Math.min(1.0, weight);
        CellEdges edges = network();
        CellEdges.Edge edge = edges.nearestOpenEdge(x + 0.5, z + 0.5);
        if (!edge.exists()) {
            return;
        }
        long seed = edge.seed();
        double halfWidth = slots.width().at(SeededHash.unit(seed, 1)) * weight / 2.0;
        double undercut = slots.undercut().at(SeededHash.unit(seed, 3));
        if (edge.distance() >= halfWidth + undercut / 2.0 + Math.abs(slots.wobble())) {
            return;
        }
        double depth = slots.depth().at(SeededHash.unit(seed, 2)) * weight;
        double bottom = Math.max(corners.sample(plateauHeight) - depth,
                Math.max(slots.minBottom(), corners.sample(canyonFloor) + slots.floorClearance()));
        if (surface < bottom + 2.0) {
            return;
        }
        column.slot = true;
        column.slotDistance = edge.distance();
        column.slotHalfWidth = halfWidth;
        column.slotUndercut = undercut;
        column.slotBottom = bottom;
        column.slotTop = surface;
        Bridge bridge = bridge(edge, bottom, surface);
        if (bridge != null) {
            column.bridgeLo = bridge.lo;
            column.bridgeHi = bridge.hi;
        }
        column.widen(Mth.floor(bottom) + 1, Mth.ceil(surface) + 1);
    }

    /** The bridge (or chockstone) at this position along the edge, or null. Kept blocks: [lo, hi]. */
    Bridge bridge(CellEdges.Edge edge, double bottom, double top) {
        double spacing = slots.bridgeSpacing();
        long segment = (long) Math.floor(edge.along() / spacing);
        long hash = SeededHash.mix(edge.seed() + segment * 0x9E3779B97F4A7C15L);
        double centre = (segment + 0.5) * spacing + (2.0 * SeededHash.unit(hash, 0) - 1.0) * slots.bridgeJitter();
        double length = slots.bridgeLength().at(SeededHash.unit(hash, 1));
        if (Math.abs(edge.along() - centre) > length / 2.0) {
            return null;
        }
        if (SeededHash.unit(hash, 2) < slots.chockstoneChance()) {
            double thickness = slots.chockstoneThickness().at(SeededHash.unit(hash, 3));
            double middle = bottom + (top - bottom) * (0.35 + 0.3 * SeededHash.unit(hash, 4));
            return new Bridge(Mth.floor(middle - thickness / 2.0), Mth.floor(middle + thickness / 2.0), true);
        }
        double thickness = slots.bridgeThickness().at(SeededHash.unit(hash, 3));
        return new Bridge(Mth.floor(top - thickness) + 1, Mth.ceil(top) + 1, false);
    }

    private void fillWall(Column column, int x, int z, Corners corners, double surface, double slope, double weight) {
        double plateau = corners.sample(plateauSurface);
        double floor = corners.sample(canyonFloor);
        int surfaceDepth = (int) (surfaceNoise.getValue(x, 0.0, z) * 2.75 + 3.125);
        double perBlock = slope / 8.0;
        Uphill uphill = new Uphill(x, z, corners.gradient(heightGrid));
        column.ux = uphill.ux;
        column.uz = uphill.uz;
        WallSettings w = walls;
        int index = 0;
        for (int period = 0; period < w.periods(); period++) {
            for (List<Integer> band : w.creamBands()) {
                int lo = w.bottom() + period * w.period() + band.get(0) + surfaceDepth;
                int hi = w.bottom() + period * w.period() + band.get(1) + surfaceDepth;
                int key = index++;
                if (hi < floor - 8.0 || lo > plateau + 2.0) {
                    continue;
                }
                double ledge = bandNoise.getValue(x, key * 64.0 + 0.5, z);
                if (ledge > w.ledgeThreshold()) {
                    double reach = w.ledgeReach().at(Mth.clamp((ledge - w.ledgeThreshold()) / w.ledgeRamp(), 0.0, 1.0)) * weight;
                    // Only against a wall that rises above the whole band within reach, so a ledge never hangs free,
                    // and not in the low part of a wall (see carriesLedge).
                    if (uphill.height(reach + 1.0) >= hi + 1.0 && carriesLedge(uphill, lo)) {
                        column.feature(lo, hi, true, reach, perBlock);
                    }
                }
                double niche = bandNoise.getValue(x, key * 64.0 + 32.5, z);
                if (niche > w.nicheThreshold()) {
                    double depth = w.nicheDepth().at(Mth.clamp((niche - w.nicheThreshold()) / w.nicheRamp(), 0.0, 1.0)) * weight;
                    // Height from a coarse field, so the floor of one niche is flat.
                    double coarse = bandNoise.getValue(x / 8.0, key * 64.0 + 16.5, z / 8.0);
                    int nicheHeight = Mth.floor(w.nicheHeight().at(Mth.clamp(coarse + 0.5, 0.0, 1.0)) + 0.5);
                    int nicheFloor = Math.max(lo - nicheHeight, Math.max(Mth.ceil(floor) + 1, w.nicheMinFloor()));
                    // The wall behind the niche must rise above its roof, so the roof stays attached.
                    if (lo - 1 - nicheFloor >= 2 && uphill.height(depth + 1.0) >= lo + 2.0) {
                        column.feature(nicheFloor, lo - 1, false, depth, perBlock);
                    }
                }
            }
        }
        if (surface < plateau - 1.0) {
            // Caprock: the top blocks of the wall (the plateau's top block is ceil(plateau) - 1) stick out.
            int rimTop = Mth.ceil(plateau) - 1;
            double t = Mth.clamp(0.5 + bandNoise.getValue(x / 4.0, -64.5, z / 4.0), 0.0, 1.0);
            int rimLo = rimTop - Mth.floor(w.rimDepth().at(t) + 0.5) + 1;
            double reach = w.rimReach().at(t) * weight;
            if (uphill.height(reach + 1.0) >= rimTop + 1.0 && carriesLedge(uphill, rimLo)) {
                column.feature(rimLo, rimTop, true, reach, perBlock);
                double undercut = w.rimUndercutDepth().at(t) * weight;
                if (uphill.height(undercut + 1.0) >= rimLo + 1.0) {
                    column.feature(rimLo - w.rimUndercutHeight(), rimLo - 1, false, undercut, perBlock);
                }
            }
        }
    }

    /**
     * Whether the wall is high enough under row {@code lo} to carry a ledge there: where the wall reaches {@code lo}
     * (uphill from the column), the terrain {@code foot_distance} blocks in front of it lies at least {@code min_height}
     * lower. Low on a wall (and on a low step) the wall noise makes the face too ragged: a ledge there broke into loose
     * bits over the floor. The test is made at the wall, not at the column, so all columns in front of one stretch of
     * wall agree and a ledge is never cut apart between them.
     */
    private boolean carriesLedge(Uphill uphill, int lo) {
        double wall = uphill.reaches(lo);
        return !Double.isNaN(wall) && lo - uphill.heightAt(wall - walls.ledgeFootDistance()) >= walls.ledgeMinHeight();
    }

    /** The {@code height} along the uphill direction (the gradient) from a column; whole blocks memoised. */
    private final class Uphill {
        private static final int BEHIND = 8;
        private final int x;
        private final int z;
        private final double ux;
        private final double uz;
        private final double[] memo = new double[16 + BEHIND];

        Uphill(int x, int z, double[] gradient) {
            this.x = x;
            this.z = z;
            double length = Math.hypot(gradient[0], gradient[1]);
            this.ux = length > 0.0 ? gradient[0] / length : 0.0;
            this.uz = length > 0.0 ? gradient[1] / length : 0.0;
            java.util.Arrays.fill(memo, Double.NaN);
        }

        /** The height {@code ceil(distance)} (1 … 15) blocks uphill. */
        double height(double distance) {
            return at(Mth.clamp(Mth.ceil(distance), 1, memo.length - 1 - BEHIND));
        }

        /** The height {@code d} whole blocks uphill (−8 … 15; negative = downhill). */
        private double at(int d) {
            int i = d + BEHIND;
            if (Double.isNaN(memo[i])) {
                memo[i] = new Corners(x + d * ux, z + d * uz).sample(heightGrid);
            }
            return memo[i];
        }

        /** The height a fractional distance uphill, linear between whole blocks. */
        double heightAt(double distance) {
            double d = Mth.clamp(distance, -BEHIND, memo.length - 1 - BEHIND);
            int d0 = Math.min(Mth.floor(d), memo.length - 2 - BEHIND);
            return Mth.lerp(d - d0, at(d0), at(d0 + 1));
        }

        /** The first distance uphill (from 4 blocks downhill) where the height reaches {@code level}; NaN if none. */
        double reaches(double level) {
            double previous = at(-4);
            if (previous >= level) {
                return -4.0;
            }
            for (int d = -3; d <= memo.length - 1 - BEHIND; d++) {
                double next = at(d);
                if (next >= level) {
                    return d - 1 + (level - previous) / (next - previous);
                }
                previous = next;
            }
            return Double.NaN;
        }
    }

    /**
     * Whether the next block from (x, y, z) towards the wall (one step along the main axis of the column's uphill
     * direction) is solid in the result: terrain that no wall feature erodes, or itself part of a ledge. Caves are only
     * known where an add feature seals the face (see {@link Column#seals}): unsealed skin does not count, deeper
     * terrain is taken as solid (slots are not looked at).
     */
    private boolean solidTowardsWall(Column column, int x, int y, int z, int step) {
        if (step >= MAX_CHAIN) {
            return false;
        }
        int nx = x;
        int nz = z;
        if (Math.abs(column.ux) >= Math.abs(column.uz)) {
            nx += column.ux > 0.0 ? 1 : -1;
        } else {
            nz += column.uz > 0.0 ? 1 : -1;
        }
        Column next = columns.get(nx, nz, this::fillColumn);
        double s = shellAt.sample(nx, y, nz);
        if (s > 0.0) {
            // Unsealed skin may be a cave mouth: not something to hang on to.
            return next.seals(y, s) || s >= SKIN && (!next.hasFeatureAt(y) || !next.cuts(this, nx, y, nz, s));
        }
        return next.hasFeatureAt(y) && next.adds(this, nx, y, nz, s, step + 1);
    }

    CellEdges network() {
        CellEdges resolved = network;
        if (resolved == null) {
            resolved = CellEdges.resolve(slotNetwork);
            network = resolved;
        }
        return resolved;
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new CanyonDetails(argument.mapAll(visitor), rocks.stream().map(r -> r.mapAll(visitor)).toList(),
                terrain.mapAll(visitor), slotNetwork.mapAll(visitor), slotWeight.mapAll(visitor), height.mapAll(visitor),
                plateauHeight.mapAll(visitor), plateauSurface.mapAll(visitor),
                canyonFloor.mapAll(visitor), wallWeight.mapAll(visitor), wobble.map(w -> w.mapAll(visitor)),
                visitor.visitNoise(bandNoise), visitor.visitNoise(surfaceNoise), slots, walls));
    }

    @Override
    public double minValue() {
        return Math.min(-1.0, argument.minValue());
    }

    @Override
    public double maxValue() {
        return Math.max(1.0, argument.maxValue());
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CanyonDetails d && argument.equals(d.argument) && rocks.equals(d.rocks) && terrain.equals(d.terrain)
                && slotNetwork.equals(d.slotNetwork)
                && slotWeight.equals(d.slotWeight) && height.equals(d.height) && plateauHeight.equals(d.plateauHeight)
                && plateauSurface.equals(d.plateauSurface) && canyonFloor.equals(d.canyonFloor) && wallWeight.equals(d.wallWeight)
                && wobble.equals(d.wobble) && bandNoise.equals(d.bandNoise) && surfaceNoise.equals(d.surfaceNoise)
                && slots.equals(d.slots) && walls.equals(d.walls);
    }

    @Override
    public int hashCode() {
        return Objects.hash(argument, rocks, terrain, slotNetwork, slotWeight, height, plateauHeight, plateauSurface, canyonFloor,
                wallWeight, wobble, bandNoise, surfaceNoise, slots, walls);
    }

    /** The column data (tests). */
    Column columnAt(int x, int z) {
        return columns.get(x, z, this::fillColumn);
    }

    record Bridge(int lo, int hi, boolean chockstone) {
    }

    /** Bilinear reads of 2D inputs from the four cell corners (4 blocks apart) around a position. */
    private static final class Corners {
        private final int x0;
        private final int z0;
        private final double fx;
        private final double fz;

        Corners(double x, double z) {
            x0 = Mth.floor(x / 4.0) * 4;
            z0 = Mth.floor(z / 4.0) * 4;
            fx = (x - x0) / 4.0;
            fz = (z - z0) / 4.0;
        }

        private double[] corners(Grid grid) {
            return new double[]{grid.at(x0, z0), grid.at(x0 + 4, z0), grid.at(x0, z0 + 4), grid.at(x0 + 4, z0 + 4)};
        }

        double sample(DensityFunction function) {
            return sample(Grid.of(function));
        }

        double sample(Grid grid) {
            double[] c = corners(grid);
            return Mth.lerp2(fx, fz, c[0], c[1], c[2], c[3]);
        }

        /** Gradient (blocks of height per block, x and z) of the bilinear surface. */
        double[] gradient(Grid grid) {
            double[] c = corners(grid);
            return new double[]{Mth.lerp(fz, c[1] - c[0], c[3] - c[2]) / 4.0, Mth.lerp(fx, c[2] - c[0], c[3] - c[1]) / 4.0};
        }

        double slope(Grid grid) {
            double[] g = gradient(grid);
            return Math.hypot(g[0], g[1]);
        }
    }

    /** A 2D input at the corners of the 4-block grid. */
    @FunctionalInterface
    interface Grid {
        double at(int x, int z);

        static Grid of(DensityFunction function) {
            return (x, z) -> function.compute(new SinglePointContext(x, 0, z));
        }
    }

    /**
     * {@code height} on the grid, remembered per thread: the uphill searches of the wall features read it well outside
     * the chunk, where the noise chunk's flat cache does not reach and every read would compute the whole height chain.
     */
    private static final class HeightGrid implements Grid {
        private static final int BITS = 10;
        private final DensityFunction height;
        private final ThreadOwned<Table> tables = new ThreadOwned<>(Table::new);

        HeightGrid(DensityFunction height) {
            this.height = height;
        }

        @Override
        public double at(int x, int z) {
            Table table = tables.get();
            long key = (long) x << 32 | z & 0xFFFFFFFFL;
            int slot = (int) (SeededHash.mix(key) >>> (64 - BITS));
            if (!table.filled[slot] || table.keys[slot] != key) {
                table.filled[slot] = true;
                table.keys[slot] = key;
                table.values[slot] = height.compute(new SinglePointContext(x, 0, z));
            }
            return table.values[slot];
        }

        private static final class Table {
            private final long[] keys = new long[1 << BITS];
            private final boolean[] filled = new boolean[1 << BITS];
            private final double[] values = new double[1 << BITS];
        }
    }

    /** Per-column data; reused by the {@link ColumnTable}. */
    static final class Column {
        int lo;
        int hi;
        double surface;
        double slope;
        double ux;
        double uz;
        boolean slot;
        double slotDistance;
        double slotHalfWidth;
        double slotUndercut;
        double slotBottom;
        double slotTop;
        int bridgeLo;
        int bridgeHi;
        int features;
        int rocks;
        final int[] rockLo = new int[MAX_ROCKS];
        final int[] rockHi = new int[MAX_ROCKS];
        final int[] featureLo = new int[MAX_FEATURES];
        final int[] featureHi = new int[MAX_FEATURES];
        final boolean[] featureAdds = new boolean[MAX_FEATURES];
        final double[] featureShift = new double[MAX_FEATURES];
        final double[] featureBound = new double[MAX_FEATURES];
        final double[] featureSeal = new double[MAX_FEATURES];

        void reset() {
            lo = Integer.MAX_VALUE;
            hi = Integer.MIN_VALUE;
            slot = false;
            bridgeLo = Integer.MAX_VALUE;
            bridgeHi = Integer.MIN_VALUE;
            features = 0;
            rocks = 0;
        }

        void rock(int from, int to) {
            if (rocks < MAX_ROCKS) {
                rockLo[rocks] = from;
                rockHi[rocks] = to;
                rocks++;
                widen(from, to);
            }
        }

        boolean rockAt(int y) {
            for (int i = 0; i < rocks; i++) {
                if (y >= rockLo[i] && y <= rockHi[i]) {
                    return true;
                }
            }
            return false;
        }

        boolean hasFeatureAt(int y) {
            for (int i = 0; i < features; i++) {
                if (y >= featureLo[i] && y <= featureHi[i]) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Whether an open block (shell {@code s} ≤ 0) of this column is part of a ledge or rim: in the rows of an add
         * feature, the wall {@code shift} blocks uphill is solid (the wall moved outward), and the next block towards
         * the wall is solid as well, so a ledge always hangs on to its wall, also where neighbouring columns have
         * their rows or their reach a block different.
         */
        boolean adds(CanyonDetails owner, int x, int y, int z, double s, int step) {
            for (int i = 0; i < features; i++) {
                if (featureAdds[i] && y >= featureLo[i] && y <= featureHi[i] && s > -featureBound[i]
                        && owner.shellAt.sample(x + ux * featureShift[i], y, z + uz * featureShift[i]) > 0.0) {
                    return owner.solidTowardsWall(this, x, y, z, step);
                }
            }
            return false;
        }

        /**
         * Whether a block inside the terrain (shell {@code s} > 0) is sealed by an add feature: the outer skin of the wall
         * (about two blocks) in the rows of a ledge or rim is always rock, also where a cave mouth would open it, so the
         * ledge in front of it has something to hang on to.
         */
        boolean seals(int y, double s) {
            for (int i = 0; i < features; i++) {
                if (featureAdds[i] && y >= featureLo[i] && y <= featureHi[i] && s < featureSeal[i]) {
                    return true;
                }
            }
            return false;
        }

        /** Whether a solid block (shell {@code s} > 0) is eroded: the shell {@code shift} blocks downhill is open. */
        boolean cuts(CanyonDetails owner, int x, int y, int z, double s) {
            for (int i = 0; i < features; i++) {
                if (!featureAdds[i] && y >= featureLo[i] && y <= featureHi[i] && s < featureBound[i]
                        && owner.shellAt.sample(x - ux * featureShift[i], y, z - uz * featureShift[i]) <= 0.0) {
                    return true;
                }
            }
            return false;
        }

        void widen(int from, int to) {
            lo = Math.min(lo, from);
            hi = Math.max(hi, to);
        }

        /**
         * A wall feature on rows [from, to]: the wall moved out ({@code adds}) or eroded by {@code shift} blocks.
         * {@code perBlock} is the shell's change per block across the wall here; it only bounds which blocks are
         * tested (generously: the wall noise changes the real gradient).
         */
        void feature(int from, int to, boolean adds, double shift, double perBlock) {
            if (features == MAX_FEATURES || to < from || shift <= 0.0) {
                return;
            }
            featureLo[features] = from;
            featureHi[features] = to;
            featureAdds[features] = adds;
            featureShift[features] = shift;
            featureBound[features] = 2.0 * shift * Math.max(perBlock, 0.125) + 0.25;
            featureSeal[features] = SEAL_BLOCKS * Math.max(perBlock, 0.125);
            features++;
            widen(from, to);
        }
    }

    /** Slot parameters: per edge a width, depth and undercut; bridges per {@code bridge_spacing} along the edge. */
    record SlotSettings(Range width, Range depth, Range undercut, double wobble, double minWeight, double minBottom,
                        double floorClearance, double bridgeSpacing, double bridgeJitter, Range bridgeLength,
                        Range bridgeThickness, double chockstoneChance, Range chockstoneThickness) {
        static final Codec<SlotSettings> CODEC = RecordCodecBuilder.<SlotSettings>create(instance -> instance.group(
                Range.within(1, 32).fieldOf("width").forGetter(SlotSettings::width),
                Range.within(1, 128).fieldOf("depth").forGetter(SlotSettings::depth),
                Range.within(0, 16).fieldOf("undercut").forGetter(SlotSettings::undercut),
                Codec.doubleRange(0, 8).optionalFieldOf("wobble", 0.0).forGetter(SlotSettings::wobble),
                Codec.doubleRange(0, 1).optionalFieldOf("min_weight", 0.5).forGetter(SlotSettings::minWeight),
                Codec.doubleRange(-64, 320).fieldOf("min_bottom").forGetter(SlotSettings::minBottom),
                Codec.doubleRange(0, 64).fieldOf("floor_clearance").forGetter(SlotSettings::floorClearance),
                Codec.doubleRange(4, 1024).fieldOf("bridge_spacing").forGetter(SlotSettings::bridgeSpacing),
                Codec.doubleRange(0, 512).fieldOf("bridge_jitter").forGetter(SlotSettings::bridgeJitter),
                Range.within(0, 64).fieldOf("bridge_length").forGetter(SlotSettings::bridgeLength),
                Range.within(1, 64).fieldOf("bridge_thickness").forGetter(SlotSettings::bridgeThickness),
                Codec.doubleRange(0, 1).fieldOf("chockstone_chance").forGetter(SlotSettings::chockstoneChance),
                Range.within(1, 64).fieldOf("chockstone_thickness").forGetter(SlotSettings::chockstoneThickness)
        ).apply(instance, SlotSettings::new)).validate(SlotSettings::validate);

        private static DataResult<SlotSettings> validate(SlotSettings s) {
            return s.bridgeJitter + s.bridgeLength.max() / 2.0 <= s.bridgeSpacing / 2.0
                    ? DataResult.success(s)
                    : DataResult.error(() -> "slots: bridge_jitter + bridge_length / 2 must stay within half the bridge_spacing");
        }
    }

    /**
     * Wall parameters. {@code cream_bands}: [first, last] block offsets within one strata period (from {@code bottom},
     * every {@code period} blocks, {@code periods} times), the same bands as the surface rules; the surface depth shifts
     * them like it shifts the strata boundaries.
     */
    record WallSettings(double minSlope, double minWeight, int bottom, int period, int periods, List<List<Integer>> creamBands,
                        double ledgeThreshold, double ledgeRamp, Range ledgeReach, double ledgeFootDistance, double ledgeMinHeight,
                        double nicheThreshold, double nicheRamp, int nicheMinFloor,
                        Range nicheHeight, Range nicheDepth, Range rimDepth, Range rimReach, int rimUndercutHeight, Range rimUndercutDepth) {
        static final Codec<WallSettings> CODEC = RecordCodecBuilder.<WallSettings>create(instance -> instance.group(
                Codec.doubleRange(0, 64).fieldOf("min_slope").forGetter(WallSettings::minSlope),
                Codec.doubleRange(0, 1).optionalFieldOf("min_weight", 0.5).forGetter(WallSettings::minWeight),
                BandSettings.CODEC.fieldOf("bands").forGetter(WallSettings::bandSettings),
                LedgeSettings.CODEC.fieldOf("ledges").forGetter(WallSettings::ledgeSettings),
                NicheSettings.CODEC.fieldOf("niches").forGetter(WallSettings::nicheSettings),
                RimSettings.CODEC.fieldOf("rim").forGetter(WallSettings::rimSettings)
        ).apply(instance, WallSettings::of));

        static WallSettings of(double minSlope, double minWeight, BandSettings bands, LedgeSettings ledges, NicheSettings niches, RimSettings rim) {
            return new WallSettings(minSlope, minWeight, bands.bottom, bands.period, bands.periods, bands.cream, ledges.threshold,
                    ledges.ramp, ledges.reach, ledges.footDistance, ledges.minHeight, niches.threshold, niches.ramp, niches.minFloor,
                    niches.height, niches.depth, rim.depth, rim.reach,
                    rim.undercutHeight, rim.undercutDepth);
        }

        BandSettings bandSettings() {
            return new BandSettings(bottom, period, periods, creamBands);
        }

        LedgeSettings ledgeSettings() {
            return new LedgeSettings(ledgeThreshold, ledgeRamp, ledgeReach, ledgeFootDistance, ledgeMinHeight);
        }

        NicheSettings nicheSettings() {
            return new NicheSettings(nicheThreshold, nicheRamp, nicheMinFloor, nicheHeight, nicheDepth);
        }

        RimSettings rimSettings() {
            return new RimSettings(rimDepth, rimReach, rimUndercutHeight, rimUndercutDepth);
        }
    }

    record BandSettings(int bottom, int period, int periods, List<List<Integer>> cream) {
        private static final Codec<List<Integer>> BAND = Codec.INT.listOf().validate(band -> band.size() == 2 && band.get(0) <= band.get(1)
                ? DataResult.success(band) : DataResult.error(() -> "A band needs [first, last], got " + band));

        static final Codec<BandSettings> CODEC = RecordCodecBuilder.<BandSettings>create(instance -> instance.group(
                Codec.intRange(-64, 320).fieldOf("bottom").forGetter(BandSettings::bottom),
                Codec.intRange(1, 384).fieldOf("period").forGetter(BandSettings::period),
                Codec.intRange(1, 64).fieldOf("periods").forGetter(BandSettings::periods),
                BAND.listOf().fieldOf("cream").forGetter(BandSettings::cream)
        ).apply(instance, BandSettings::new)).validate(b -> b.cream.stream().allMatch(band -> band.get(1) < b.period)
                ? DataResult.success(b) : DataResult.error(() -> "Cream bands must lie within one period"));
    }

    /**
     * {@code min_height}: where the wall reaches the lowest row of a ledge (or of the rim), the terrain
     * {@code foot_distance} blocks in front of it lies at least this much lower.
     */
    record LedgeSettings(double threshold, double ramp, Range reach, double footDistance, double minHeight) {
        static final Codec<LedgeSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.doubleRange(-2, 2).fieldOf("threshold").forGetter(LedgeSettings::threshold),
                Codec.doubleRange(0.001, 4).fieldOf("ramp").forGetter(LedgeSettings::ramp),
                Range.within(0, 16).fieldOf("reach").forGetter(LedgeSettings::reach),
                Codec.doubleRange(0, 8).optionalFieldOf("foot_distance", 3.0).forGetter(LedgeSettings::footDistance),
                Codec.doubleRange(0, 64).optionalFieldOf("min_height", 0.0).forGetter(LedgeSettings::minHeight)
        ).apply(instance, LedgeSettings::new));
    }

    /** {@code min_floor}: niche floors stay at or above this Y (above the preliminary surface, so never under water). */
    record NicheSettings(double threshold, double ramp, int minFloor, Range height, Range depth) {
        static final Codec<NicheSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.doubleRange(-2, 2).fieldOf("threshold").forGetter(NicheSettings::threshold),
                Codec.doubleRange(0.001, 4).fieldOf("ramp").forGetter(NicheSettings::ramp),
                Codec.intRange(-64, 320).optionalFieldOf("min_floor", 66).forGetter(NicheSettings::minFloor),
                Range.within(1, 32).fieldOf("height").forGetter(NicheSettings::height),
                Range.within(0, 32).fieldOf("depth").forGetter(NicheSettings::depth)
        ).apply(instance, NicheSettings::new));
    }

    record RimSettings(Range depth, Range reach, int undercutHeight, Range undercutDepth) {
        static final Codec<RimSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Range.within(0, 32).fieldOf("depth").forGetter(RimSettings::depth),
                Range.within(0, 16).fieldOf("reach").forGetter(RimSettings::reach),
                Codec.intRange(0, 16).fieldOf("undercut_height").forGetter(RimSettings::undercutHeight),
                Range.within(0, 16).fieldOf("undercut_depth").forGetter(RimSettings::undercutDepth)
        ).apply(instance, RimSettings::new));
    }
}
