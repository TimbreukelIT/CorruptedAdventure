package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.Ring5Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * {@code corruptedadventure:ring_5_field}: the overworld terrain of ring 5, on a grid of its own like ring 4's
 * {@link IslandField}: its own {@link Column}, {@link ColumnTable} and {@link Decisions}. As a density function it is
 * the break-up of a column (0 solid ground .. 1 void without any chunk); {@link Ring5Output} reads its other outputs.
 * <p>
 * The outer band (84, numbers in {@link Ring5Outer}): floes that break away towards the void, their undersides rising
 * from Y −40 at the band line to Y 20–40 at the edge; {@code void_scars} cut cracks and craters into them, {@code
 * inverted_hills} hang upside-down hills under them; floating chunks, narrow ridges and strips of stepping stones over the
 * void; the same field runs on past ring 5's boundary until the edge is empty. The four castle pinnacles (82 sites, 84
 * shape in {@link Ring5Castles}) with their ruined rims, void belts and two bridges each. The inner band (83, numbers in
 * {@link Ring5Inner}): dead ground with low hills, per region the shapes that are just wrong (stairs and pits, a regular
 * wave, blocky ground with shifted layers and floating cubes) and loose cracks down into the void.
 * <p>
 * The terrain is exact per block (no interpolation): a column's ground is a 2D top and underside, plus the exact spans of
 * the bridges, ridges and chunks over it, so walls, deck edges and cracks lie on the block.
 * <p>
 * The castle sites come from the world seed (the hash noise {@code ring_5_cells}) and exist on the server only: a client
 * has the layout's noise seed, not the world seed, and cannot derive them. Found through the router
 * ({@code RingChunkGenerator#ring5Field}) and, for readers without a random state, {@code Ring5Fields.current}.
 * <p>
 * Pure: a function of the seed (through the noises and {@code hash}), the ring layout and x, z. Decisions read their
 * inputs at exact positions, never through a {@code flat_cache}; the caches only remember results.
 */
public final class Ring5Field implements DensityFunction {
    public static final MapCodec<Ring5Field> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            NoiseHolder.CODEC.fieldOf("hash").forGetter(f -> f.hash),
            Noises.CODEC.fieldOf("noises").forGetter(f -> f.noises),
            Settings.CODEC.optionalFieldOf("settings", Settings.DEFAULT).forGetter(f -> f.settings)
    ).apply(instance, (hash, noises, settings) -> new Ring5Field(hash, noises, settings, RingLayout::current)));

    public static final KeyDispatchDataCodec<Ring5Field> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private static final long WORLD_SALT = 5005;
    /** The preliminary surface of the inner band: the lowest ground on a ring of this radius round the column, less a margin. */
    private static final double INITIAL_RADIUS = 6.0;
    private static final double INITIAL_MARGIN = 2.0;
    /**
     * The initial height of a void column: far enough below the world's bottom that the initial density
     * ({@code 0.390625 + 0.125 × (height − y)}) stays negative down to min_y, so the column has no preliminary surface
     * (at −64 it read as a surface at about Y −61).
     */
    static final double VOID_HEIGHT = -128.0;
    /** The four castle sites (V5-A): one per quarter, at this share of ring 5 (effective distance). */
    public static final int CASTLES = 4;
    static final double CASTLE_SHARE = 0.75;
    /** Each site turns up to this far (degrees) from its quarter's direction. */
    static final double CASTLE_JITTER = 8.0;
    /** The plateau's radius (blocks, V5-A: 80–110 across). */
    static final int CASTLE_MIN_RADIUS = 40;
    static final int CASTLE_MAX_RADIUS = 56;
    /** The pinnacle's top (V4-A). */
    static final int CASTLE_MIN_TOP = 120;
    static final int CASTLE_MAX_TOP = 140;
    /** The width of the void belt around the plateau (V5-A); the ruined rim takes its first 8–16 blocks. */
    static final int CASTLE_MIN_BELT = 40;
    static final int CASTLE_MAX_BELT = 80;
    /** Density per block away from a column's top and underside, as rings 3 and 4 (1/8), held to ±1. */
    private static final double DENSITY_PER_BLOCK = 0.125;
    /** Exact spans (bridges, ridges, chunks) one column keeps at most. */
    static final int MAX_SPANS = 8;

    private static final int FLOE_SALT = 8401;
    private static final int CRATER_SALT = 8402;
    private static final int HILL_SALT = 8403;
    private static final int FRAGMENT_SALT = 8404;
    private static final int RIDGE_SALT = 8405;
    private static final int STRIP_SALT = 8406;
    private static final int BRIDGE_SALT = 8407;
    private static final int RIM_SALT = 8408;
    private static final int DECK_SALT = 8409;

    private final NoiseHolder hash;
    private final Noises noises;
    private final Settings settings;
    private final Supplier<RingLayout> layout;
    private final ColumnTable<Column> columns = new ColumnTable<>(Column::new);
    private final Decisions decisions;

    /** @param layout the ring layout (the running server's; tests give their own) */
    Ring5Field(NoiseHolder hash, Noises noises, Settings settings, Supplier<RingLayout> layout) {
        this(hash, noises, settings, layout, new Decisions());
    }

    private Ring5Field(NoiseHolder hash, Noises noises, Settings settings, Supplier<RingLayout> layout,
                       Decisions decisions) {
        this.hash = hash;
        this.noises = noises;
        this.settings = settings;
        this.layout = layout;
        this.decisions = decisions;
    }

    Settings settings() {
        return settings;
    }

    Decisions decisions() {
        return decisions;
    }

    RingLayout ringLayout() {
        return layout.get();
    }

    // ---------------------------------------------------------------- density function

    @Override
    public double compute(FunctionContext context) {
        return column(context.blockX(), context.blockZ()).breakup;
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        NoiseHolder mappedHash = visitor.visitNoise(hash);
        Noises mappedNoises = noises.map(visitor);
        // A noise chunk's copy keeps the decisions of the random state's instance (same noises, same results).
        boolean same = mappedHash.equals(hash) && mappedNoises.equals(noises);
        return visitor.apply(new Ring5Field(mappedHash, mappedNoises, settings, layout, same ? decisions : new Decisions()));
    }

    @Override
    public double minValue() {
        return 0.0;
    }

    @Override
    public double maxValue() {
        return 1.0;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    // Equality without the caches: the noise chunk's memo then shares one instance between all its users.
    @Override
    public boolean equals(Object o) {
        return o instanceof Ring5Field f && hash.equals(f.hash) && noises.equals(f.noises)
                && settings.equals(f.settings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash, noises, settings);
    }

    /** The first {@code ring_5_field} anywhere inside {@code root} (e.g. a random state's final density), or null. */
    @Nullable
    public static Ring5Field find(DensityFunction root) {
        Ring5Field[] found = new Ring5Field[1];
        root.mapAll(new Visitor() {
            @Override
            public DensityFunction apply(DensityFunction function) {
                if (found[0] == null && function instanceof Ring5Field field) {
                    found[0] = field;
                }
                return function;
            }

            @Override
            public NoiseHolder visitNoise(NoiseHolder noise) {
                return noise;
            }
        });
        return found[0];
    }

    /** {@link #resolve}, or null while {@code function} is a holder reference that is not bound yet. */
    @Nullable
    static Ring5Field resolveIfBound(DensityFunction function) {
        try {
            return resolve(function);
        } catch (IllegalStateException unbound) {
            return null;
        }
    }

    /** {@code function} as a {@code ring_5_field}, looking through holders, markers and ring gates; null if none. */
    @Nullable
    static Ring5Field resolve(DensityFunction function) {
        DensityFunction current = function;
        for (int depth = 0; depth < 16 && current != null; depth++) {
            if (current instanceof Ring5Field field) {
                return field;
            }
            current = RingGated.unwrap(current);
        }
        return null;
    }

    // ---------------------------------------------------------------- read access (W-biomes, A, C, fase 3; read only)

    /** All 2D values of a column (cached per thread around the current chunk). */
    public Column column(int x, int z) {
        return columns.get(x, z, this::fill);
    }

    /**
     * The four castle sites (V5-A), decided once per world: index 0 is the site closest to east (+x) after the seed's
     * turn, then by growing angle (east → south → west → north: clockwise on a north-up map).
     */
    public List<CastleSite> castleSites() {
        List<CastleSite> known = decisions.sites;
        if (known == null) {
            // Pure: racing threads compute the same list.
            known = List.copyOf(placeCastles(layout.get(), salt()));
            decisions.sites = known;
        }
        return known;
    }

    /** The castle site whose plateau (its radius) covers the column, or null. */
    @Nullable
    public CastleSite castleAt(int x, int z) {
        for (CastleSite site : castleSites()) {
            if (site.distance(x, z) <= site.radius()) {
                return site;
            }
        }
        return null;
    }

    /**
     * The bridges whose deck comes into the box (blocks, inclusive): the two of each castle (V6-A, {@code siteIndex}
     * 0–3) and the narrow ridges of the outer band ({@code siteIndex} −1).
     */
    public List<Bridge> bridgesWithin(int minX, int minZ, int maxX, int maxZ) {
        List<Bridge> found = new ArrayList<>();
        for (Bridge bridge : castleBridges()) {
            if (bridge.touches(minX, minZ, maxX, maxZ)) {
                found.add(bridge);
            }
        }
        for (Bridge ridge : ridges()) {
            if (ridge.touches(minX, minZ, maxX, maxZ)) {
                found.add(ridge);
            }
        }
        return found;
    }

    /** The floating chunks and stepping stones of the outer band whose footprint comes into the box (blocks, inclusive). */
    public List<Fragment> fragmentsWithin(int minX, int minZ, int maxX, int maxZ) {
        List<Fragment> found = new ArrayList<>();
        int cell = Ring5Outer.FRAGMENT_CELL;
        int reach = (int) Math.ceil(Ring5Outer.FRAGMENT_MAX_RADIUS) + 1;
        for (int i = Math.floorDiv(minX - reach, cell); i <= Math.floorDiv(maxX + reach, cell); i++) {
            for (int j = Math.floorDiv(minZ - reach, cell); j <= Math.floorDiv(maxZ + reach, cell); j++) {
                Fragment fragment = fragment(i, j);
                if (fragment != null && fragment.touches(minX, minZ, maxX, maxZ)) {
                    found.add(fragment);
                }
            }
        }
        for (Strip strip : strips()) {
            for (Fragment stone : strip.stones()) {
                if (stone.touches(minX, minZ, maxX, maxZ)) {
                    found.add(stone);
                }
            }
        }
        return found;
    }

    /** How far the ground of the column has fallen away: 0 solid ground, 1 void without any chunk. */
    public double breakupAt(int x, int z) {
        return column(x, z).breakup;
    }

    /** Whether the column has ground under it (a floe, the inner band, a pinnacle; not the void, a scar or a deck only). */
    public boolean solidGround(int x, int z) {
        return !column(x, z).isVoid();
    }

    /** The preliminary surface of the column: its top, or {@link #VOID_HEIGHT} (no surface) over the void. */
    public double initialHeight(int x, int z) {
        Column column = column(x, z);
        if (!column.inner) {
            return column.isVoid() ? VOID_HEIGHT : column.top;
        }
        // The inner band (83): the lowest ground round the column (its shapes, not its cracks or cubes), less a margin.
        double initial = column.initial;
        if (Double.isNaN(initial)) {
            Ring5Inner inner = inner();
            initial = column.height;
            for (int k = 0; k < 8; k++) {
                double a = Math.PI / 4.0 * k;
                initial = Math.min(initial, inner.top(Mth.floor(x + 0.5 + INITIAL_RADIUS * Math.cos(a)),
                        Mth.floor(z + 0.5 + INITIAL_RADIUS * Math.sin(a))));
            }
            initial -= INITIAL_MARGIN;
            column.initial = initial;
        }
        return initial;
    }

    /**
     * The terrain's density at a block, exact: inside an exact span 1; in a void column −1; else 1/8 per block away from
     * the column's top and underside, held to ±1 (smooth enough for the ring blend of the zones 4→5 and 5→void).
     */
    public double terrain(int x, int y, int z) {
        Column column = column(x, z);
        for (int k = 0; k < column.spans; k++) {
            if (y >= column.spanBottom[k] && y < column.spanTop[k]) {
                return 1.0;
            }
        }
        if (column.isVoid()) {
            return -1.0;
        }
        if (column.layered && y >= column.top - Ring5Inner.LAYER_DEPTH && y < column.top + Ring5Inner.LAYER_DEPTH) {
            // The glitch fields' layers (83): each slice is the ground of a column a few blocks over.
            int[] shift = inner().layerShift(x, y, z);
            Column other = column(x + shift[0], z + shift[1]);
            return y < (other.isVoid() ? other.height : other.top) ? 1.0 : -1.0;
        }
        double density = DENSITY_PER_BLOCK * (column.top - y);
        if (column.bottom != Double.NEGATIVE_INFINITY) {
            density = Math.min(density, DENSITY_PER_BLOCK * (y - column.bottom));
        }
        return Mth.clamp(density, -1.0, 1.0);
    }

    // ---------------------------------------------------------------- columns

    private void fill(Column c, int x, int z) {
        c.reset();
        RingLayout l = layout.get();
        double effective = l.effectiveDistance(x, z);
        double signed = Ring5Band.of(l).signedDistance(x, z);
        double r5 = ring5Radius(l);
        CastleSite castle = nearestCastle(x, z);
        double castleDistance = castle.distance(x, z);
        boolean castleReach = castleDistance <= castle.radius() + castle.belt();
        if (castleReach) {
            fillCastle(c, x, z, castle, castleDistance);
        } else if (signed < 0.0) {
            fillInner(c, x, z);
        } else if (effective >= emptyFrom(l)) {
            c.setVoid();
        } else {
            fillOuter(c, x, z, effective, signed, r5);
        }
        addSpans(c, x, z, castleReach);
        if (c.isVoid() && !c.scarOpen) {
            c.breakup = c.spans > 0 ? 0.9 : 1.0;
        }
    }

    /** A column of the inner band: the ground with its regions' shapes, a loose crack, the glitch fields' layers. */
    private void fillInner(Column c, int x, int z) {
        Ring5Inner inner = inner();
        double top = mounded(x, z, inner.top(x, z));
        double glitch = inner.glitchWeight(x, z);
        c.glitch = glitch;
        c.inner = true;
        c.height = top;
        double crack = innerCrackDistance(x, z);
        if (crack <= 0.0) {
            c.setVoid();
            c.height = top;
            c.scarOpen = true;
            c.scars = 1.0;
            return;
        }
        c.scars = Mth.clamp(1.0 - crack / Ring5Outer.SCAR_RIM, 0.0, 1.0);
        c.top = top;
        c.bottom = Double.NEGATIVE_INFINITY;
        // Shifted layers only where the glitch fields hold alone (no half-blended layers).
        c.layered = glitch >= 0.999;
    }

    /** A column of the outer band or the outer edge (not near a castle): floe, scar or void. */
    private void fillOuter(Column c, int x, int z, double effective, double signed, double r5) {
        double progress = Ring5Outer.progress(effective, signed, r5);
        double share = Ring5Outer.share(progress, effective, r5);
        double[] floe = floeCell(x, z);
        if (floe[3] == 0.0) {
            c.setVoid();
            return;
        }
        double rot = noises.rot.getValue(x, 0.0, z);
        double top = mounded(x, z, groundTop(x, z, signed, progress));
        c.breakup = 0.4 * share;
        c.height = top;
        double scars = scarAt(x, z, rot);
        if (scars >= 1.0) {
            c.setVoid();
            c.scarOpen = true;
            c.scars = 1.0;
            c.height = top;
            return;
        }
        c.scars = scars;
        double rag = noises.detail.getValue(x * 0.5 + 731.5, 0.0, z * 0.5 - 211.5);
        double bottom = Ring5Outer.floeBottom(progress, rag);
        double hill = Ring5Outer.regionWeight(rot) * hillDepth(x, z);
        bottom -= hill;
        c.inverted = hill / Ring5Outer.HILL_MAX_DEPTH;
        bottom += Math.max(0.0, top - bottom - Ring5Outer.MIN_THICKNESS) * Ring5Outer.EDGE_THIN
                * (1.0 - Ring4Relief.smooth(floe[2], 0.0, Ring5Outer.EDGE_REACH));
        bottom = Math.max(bottom, Ring5Outer.BOTTOM_MIN);
        c.top = top;
        c.bottom = Math.min(bottom, top - Ring5Outer.MIN_THICKNESS);
    }

    /** The ground's top raised to the landing mound of a castle bridge whose deck ends higher than the ground (V6-A). */
    private double mounded(int x, int z, double top) {
        for (Bridge bridge : castleBridges()) {
            top = Math.max(top, bridge.endY() - Ring5Castles.MOUND_SLOPE * Math.hypot(x + 0.5 - bridge.toX(), z + 0.5 - bridge.toZ()));
        }
        return top;
    }

    /** A column within a castle's reach: plateau, ruined rim or void belt. */
    private void fillCastle(Column c, int x, int z, CastleSite site, double distance) {
        int rim = rimWidth(site);
        double wallRag = 0.5 + 0.5 * Mth.clamp(2.0 * noises.detail.getValue(x * 0.7, 0.0, z * 0.7), -1.0, 1.0);
        double bottom = Ring5Castles.PINNACLE_BOTTOM + Ring5Castles.WALL_RAG * wallRag;
        if (distance <= site.radius()) {
            c.top = site.top();
            c.bottom = bottom;
            c.castle = 1.0;
            c.height = c.top;
            return;
        }
        double out = distance - site.radius();
        if (out <= rim) {
            c.castle = Ring5Castles.RIM_VALUE - (Ring5Castles.RIM_VALUE - Ring5Castles.BELT_VALUE) * out / rim;
            int step = (int) Math.floor(out / Ring5Castles.RIM_STEP_WIDTH);
            double top = site.top() - (step + 1) * Ring5Castles.RIM_STEP_DROP;
            double underside = bottom + Ring5Castles.RIM_UNDERSIDE_RISE * out;
            double broken = 0.5 + Mth.clamp(noises.detail.getValue(x * 0.6 + 97.5, 0.0, z * 0.6 - 41.5), -0.5, 0.5);
            if (broken < Ring5Castles.RIM_MAX_BROKEN * out / rim || top - underside < Ring5Outer.MIN_THICK) {
                c.setVoid();
                return;
            }
            c.top = top;
            c.bottom = underside;
            c.breakup = 0.2;
            c.height = top;
            return;
        }
        c.castle = Ring5Castles.BELT_VALUE * (1.0 - (out - rim) / (site.belt() - rim));
        c.setVoid();
    }

    private void addSpans(Column c, int x, int z, boolean castleReach) {
        for (Bridge bridge : castleBridges()) {
            if (deckSpan(c, bridge, x, z)) {
                c.castle = Math.max(c.castle, Ring5Castles.BRIDGE_VALUE);
            }
        }
        if (castleReach) {
            return;
        }
        if (c.inner) {
            if (!c.isVoid()) {
                cubeSpans(c, x, z);
            }
            return;
        }
        List<Bridge> ridges = ridges();
        if (ridges.isEmpty()) {
            return;
        }
        RingLayout l = layout.get();
        double lineRadius = lineRadius(l);
        double cellSize = RingArc.perimeter(lineRadius) / ridges.size();
        int k = (int) Math.floor(RingArc.arc(x, z, lineRadius) / cellSize);
        for (int dk = -1; dk <= 1; dk++) {
            deckSpan(c, ridges.get(Math.floorMod(k + dk, ridges.size())), x, z);
            strips().get(Math.floorMod(k + dk, ridges.size())).span(c, x, z);
        }
        int cell = Ring5Outer.FRAGMENT_CELL;
        int ci = Math.floorDiv(x, cell);
        int cj = Math.floorDiv(z, cell);
        for (int di = -1; di <= 1; di++) {
            for (int dj = -1; dj <= 1; dj++) {
                Fragment fragment = fragment(ci + di, cj + dj);
                if (fragment != null) {
                    fragment.span(c, x, z);
                }
            }
        }
    }

    /** Adds the deck of a bridge or ridge over the column; whether it covers it. */
    private boolean deckSpan(Column c, Bridge bridge, int x, int z) {
        double dx = bridge.toX() - bridge.fromX();
        double dz = bridge.toZ() - bridge.fromZ();
        double length = Math.hypot(dx, dz);
        double px = x + 0.5 - bridge.fromX();
        double pz = z + 0.5 - bridge.fromZ();
        double along = (px * dx + pz * dz) / length;
        if (along < 0.0 || along > length) {
            return false;
        }
        double off = Math.abs(px * dz - pz * dx) / length;
        double half = bridge.halfWidth();
        if (off > half) {
            return false;
        }
        long h = CellShapes.cell(salt(), x, z, DECK_SALT);
        // Crumbling edges: only the outermost block may be missing, so the deck is never broken through.
        if (off > half - 1.0 && CellShapes.unit(h, 0) < 0.35) {
            return false;
        }
        double t = along / length;
        int top;
        double thick;
        if (bridge.siteIndex() < 0) {
            RingLayout l = layout.get();
            double signed = Ring5Band.of(l).signedDistance(x, z);
            double progress = Ring5Outer.progress(l.effectiveDistance(x, z), signed, ring5Radius(l));
            top = (int) Math.round(groundTop(x, z, Math.max(0.0, signed), Math.max(0.0, progress)));
            thick = bridge.sag();
        } else {
            top = (int) Math.round(Mth.lerp(t, bridge.deckY(), bridge.endY()) - bridge.sag() * 4.0 * t * (1.0 - t));
            double middle = 1.0 - 0.5 * (off / half) * (off / half);
            thick = Math.max(Ring5Outer.MIN_THICK, (Ring5Castles.THICK_MIN
                    + (Ring5Castles.THICK_MAX - Ring5Castles.THICK_MIN) * (0.5 + noises.detail.getValue(x * 0.8, 0.0, z * 0.8))) * middle);
            if (off < half - 1.0 && CellShapes.unit(h, 1) < Ring5Castles.DRIP_CHANCE) {
                thick += 2.0 + 4.0 * CellShapes.unit(h, 2);
            }
        }
        c.addSpan(top - (int) Math.round(thick), top);
        return true;
    }

    // ---------------------------------------------------------------- shapes (pure, no column cache)

    /** Ring 5's outer radius (effective distance). */
    private static double ring5Radius(RingLayout l) {
        return l.settings().ringOuterRadii().get(Ring.RING_5.ordinal());
    }

    /** The radius of the band line on average (between ring 4 and ring 5). */
    private static double lineRadius(RingLayout l) {
        List<Integer> radii = l.settings().ringOuterRadii();
        double inner = radii.get(Ring.RING_4.ordinal());
        return inner + 0.5 * (radii.get(Ring.RING_5.ordinal()) - inner);
    }

    /** Where the outer edge is empty (effective distance, V2-A: ~10,050). */
    static double emptyFrom(RingLayout l) {
        return Ring5Outer.emptyFrom(ring5Radius(l), l.settings().voidOuterRadius());
    }

    /** The inner band's shapes (83), made once per world. */
    Ring5Inner inner() {
        Ring5Inner known = decisions.inner;
        if (known == null) {
            known = new Ring5Inner(noises.hills, noises.rot, noises.glitch, salt());
            decisions.inner = known;
        }
        return known;
    }

    /** The inner band's surface (83) at a position, without cracks, layers and cubes. Pure. */
    private double innerTop(double x, double z) {
        return inner().top(Mth.floor(x), Mth.floor(z));
    }

    /** The floating cubes of the glitch fields over a column (83): exact spans. */
    private void cubeSpans(Column c, int x, int z) {
        if (c.glitch <= 0.0) {
            return;
        }
        // A cube lies inside its own cell, so only that cell's cube can cover the column.
        int[] cube = cube(Math.floorDiv(x, Ring5Inner.CUBE_CELL), Math.floorDiv(z, Ring5Inner.CUBE_CELL));
        if (cube != null && x >= cube[0] && x < cube[0] + cube[2] && z >= cube[1] && z < cube[1] + cube[2]) {
            c.addSpan(cube[3], cube[3] + cube[2]);
        }
    }

    /** The floating cube of cube cell (i, j) (83): {x0, z0, size, bottom}, or null. Cached, pure. */
    @Nullable
    int[] cube(int i, int j) {
        long key = (((long) i) << 32) ^ (j & 0xFFFFFFFFL);
        Optional<int[]> known = decisions.cubes.peek(key);
        if (known == null) {
            known = Optional.ofNullable(wholeInGlitch(inner().cube(i, j)));
            decisions.cubes.put(key, known);
        }
        return known.orElse(null);
    }

    /**
     * A cube is kept only where every column under it is glitch ground of the inner band (no outer band, castle reach or
     * crack under it): a cube cut by one of those edges would hang in lines. Pure: it asks the band, the castles and the
     * cracks directly, not the column table (a cube is computed inside a column).
     */
    @Nullable
    private int[] wholeInGlitch(@Nullable int[] cube) {
        if (cube == null) {
            return null;
        }
        Ring5Band band = Ring5Band.of(layout.get());
        for (int x = cube[0]; x < cube[0] + cube[2]; x++) {
            for (int z = cube[1]; z < cube[1] + cube[2]; z++) {
                CastleSite castle = nearestCastle(x, z);
                boolean glitch = band.inner(x, z) && inner().glitchWeight(x, z) > 0.0 && innerCrackDistance(x, z) > 0.0
                        && castle.distance(x, z) > castle.radius() + castle.belt();
                if (!glitch) {
                    return null;
                }
            }
        }
        return cube;
    }

    /**
     * How far (blocks) a column lies from the nearest loose crack of the inner band (83), at most 0 inside one; infinite
     * where none is near. For the terrain and the dam of the lava layer ({@code RingFluidPicker}). Pure.
     */
    public double innerCrackDistance(int x, int z) {
        int ci = Math.floorDiv(x, Ring5Inner.CRACK_CELL);
        int cj = Math.floorDiv(z, Ring5Inner.CRACK_CELL);
        double distance = Double.POSITIVE_INFINITY;
        for (int di = -1; di <= 1; di++) {
            for (int dj = -1; dj <= 1; dj++) {
                double[] crack = innerCrack(ci + di, cj + dj);
                if (crack != null) {
                    distance = Math.min(distance, Ring5Inner.crackDistance(crack, x + 0.5, z + 0.5));
                }
            }
        }
        return distance;
    }

    /** The loose crack of crack cell (i, j) if it lies well inside the inner band (off the band line and the zone 4→5). */
    @Nullable
    double[] innerCrack(int i, int j) {
        long key = (((long) i) << 32) ^ (j & 0xFFFFFFFFL);
        Optional<double[]> known = decisions.cracks.peek(key);
        if (known == null) {
            double[] crack = inner().crack(i, j);
            if (crack != null) {
                RingLayout l = layout.get();
                Ring5Band band = Ring5Band.of(l);
                double zoneEnd = Ring4Sundered.zoneEnd(l);
                for (double t : new double[]{0.0, 0.5, 1.0}) {
                    double px = crack[0] + crack[2] * crack[4] * t;
                    double pz = crack[1] + crack[3] * crack[4] * t;
                    if (band.signedDistance(px, pz) > -Ring5Inner.CRACK_CLEARANCE
                            || l.effectiveDistance(px, pz) < zoneEnd + Ring5Inner.CRACK_CLEARANCE) {
                        crack = null;
                        break;
                    }
                }
            }
            known = Optional.ofNullable(crack);
            decisions.cracks.put(key, known);
        }
        return known.orElse(null);
    }

    /** The ground's top in the outer band (a floe's, or where a floe would be): from the inner band's surface at the line. */
    private double groundTop(double x, double z, double signed, double progress) {
        double own = Ring5Outer.floeTop(progress, noises.relief.getValue(x, 0.0, z), noises.detail.getValue(x, 0.0, z));
        if (signed >= Ring5Outer.LINE_BLEND) {
            return own;
        }
        return Mth.lerp(Ring4Relief.smooth(signed, 0.0, Ring5Outer.LINE_BLEND), innerTop(x, z), own);
    }

    private CastleSite nearestCastle(double x, double z) {
        CastleSite best = null;
        double distance = Double.POSITIVE_INFINITY;
        for (CastleSite site : castleSites()) {
            double d = site.distance(x, z);
            if (d < distance) {
                best = site;
                distance = d;
            }
        }
        return best;
    }

    /** Whether a column lies within a castle's plateau, rim or belt, widened by {@code margin}. */
    private boolean nearCastle(double x, double z, double margin) {
        for (CastleSite site : castleSites()) {
            if (site.distance(x, z) <= site.radius() + site.belt() + margin) {
                return true;
            }
        }
        return false;
    }

    /** The width of a castle's ruined rim (8–16), from the salt. */
    int rimWidth(CastleSite site) {
        long h = CellShapes.cell(salt(), site.index(), 0, RIM_SALT);
        return Ring5Castles.RIM_MIN + (int) Math.floor((Ring5Castles.RIM_MAX - Ring5Castles.RIM_MIN + 1) * CellShapes.unit(h, 0));
    }

    /**
     * Whether a column has ground (the same rule as {@link #fill}, without the cache, for the decisions): not within a
     * castle's reach, the inner band, or a floe of the outer band that no scar opens.
     */
    boolean groundAt(double x, double z) {
        if (nearCastle(x, z, 0.0)) {
            return false;
        }
        RingLayout l = layout.get();
        if (Ring5Band.of(l).signedDistance(x, z) < 0.0) {
            return true;
        }
        if (l.effectiveDistance(x, z) >= emptyFrom(l)) {
            return false;
        }
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        return floeCell(bx, bz)[3] != 0.0 && scarAt(bx, bz, noises.rot.getValue(bx, 0.0, bz)) < 1.0;
    }

    /** The point of floe cell (i, j): {x, z}. */
    private double[] floePoint(int i, int j) {
        long h = CellShapes.cell(salt(), i, j, FLOE_SALT);
        return new double[]{(i + 0.1 + 0.8 * CellShapes.unit(h, 0)) * Ring5Outer.FLOE_CELL,
                (j + 0.1 + 0.8 * CellShapes.unit(h, 1)) * Ring5Outer.FLOE_CELL};
    }

    /** Whether floe cell (i, j) is a floe: its draw against the void's share at its point. Cached, pure. */
    boolean floeSolid(int i, int j) {
        long key = (((long) i) << 32) ^ (j & 0xFFFFFFFFL);
        Boolean known = decisions.floes.peek(key);
        if (known != null) {
            return known;
        }
        double[] p = floePoint(i, j);
        RingLayout l = layout.get();
        double effective = l.effectiveDistance(p[0], p[1]);
        double signed = Ring5Band.of(l).signedDistance(p[0], p[1]);
        double r5 = ring5Radius(l);
        double share = Ring5Outer.share(Ring5Outer.progress(effective, signed, r5), effective, r5);
        boolean solid = CellShapes.unit(CellShapes.cell(salt(), i, j, FLOE_SALT), 2) >= share;
        decisions.floes.put(key, solid);
        return solid;
    }

    /**
     * The floe cell of a column (on the bent grid): {i, j, distance (blocks) to the edge with the nearest void cell, or
     * {@link Ring5Outer#FLOE_CELL} if none is near, 1 if a floe / 0 if void}.
     */
    double[] floeCell(int x, int z) {
        double wx = x + Ring5Outer.FLOE_WARP * 2.0 * noises.detail.getValue(x * 0.25 + 1000.5, 0.0, z * 0.25)
                + Ring5Outer.FLOE_RAG * 2.0 * noises.detail.getValue(x + 3000.5, 0.0, z - 77.5);
        double wz = z + Ring5Outer.FLOE_WARP * 2.0 * noises.detail.getValue(z * 0.25 - 1000.5, 0.0, x * 0.25)
                + Ring5Outer.FLOE_RAG * 2.0 * noises.detail.getValue(z - 3000.5, 0.0, x + 77.5);
        int ci = Math.floorDiv(Mth.floor(wx), Ring5Outer.FLOE_CELL);
        int cj = Math.floorDiv(Mth.floor(wz), Ring5Outer.FLOE_CELL);
        double[][] points = new double[9][];
        int best = 0;
        double bestDistance = Double.MAX_VALUE;
        for (int k = 0; k < 9; k++) {
            points[k] = floePoint(ci + k / 3 - 1, cj + k % 3 - 1);
            double d = (wx - points[k][0]) * (wx - points[k][0]) + (wz - points[k][1]) * (wz - points[k][1]);
            if (d < bestDistance) {
                bestDistance = d;
                best = k;
            }
        }
        int bi = ci + best / 3 - 1;
        int bj = cj + best % 3 - 1;
        if (!floeSolid(bi, bj)) {
            return new double[]{bi, bj, 0.0, 0.0};
        }
        double edge = Ring5Outer.FLOE_CELL;
        for (int k = 0; k < 9; k++) {
            if (k == best || floeSolid(ci + k / 3 - 1, cj + k % 3 - 1)) {
                continue;
            }
            double ex = points[k][0] - points[best][0];
            double ez = points[k][1] - points[best][1];
            double dk = (wx - points[k][0]) * (wx - points[k][0]) + (wz - points[k][1]) * (wz - points[k][1]);
            edge = Math.min(edge, (dk - bestDistance) / (2.0 * Math.hypot(ex, ez)));
        }
        return new double[]{bi, bj, edge, 1.0};
    }

    /**
     * The {@code scars} value of a floe column in {@code void_scars} ({@code region_rot} &lt; 0): 1 open to the void (a
     * crack along a zero line of the scar noise, or a crater), falling to 0 over {@link Ring5Outer#SCAR_RIM} blocks.
     */
    private double scarAt(int x, int z, double rot) {
        double weight = Ring5Outer.regionWeight(-rot);
        if (weight <= 0.0) {
            return 0.0;
        }
        double width = Ring5Outer.CRACK_WIDTH * weight;
        double n = Math.abs(noises.scars.getValue(x, 0.0, z));
        double value = Mth.clamp(1.0 - (n - width) / width, 0.0, 1.0);
        int cell = Ring5Outer.CRATER_CELL;
        int ci = Math.floorDiv(x, cell);
        int cj = Math.floorDiv(z, cell);
        for (int di = -1; di <= 1 && value < 1.0; di++) {
            for (int dj = -1; dj <= 1; dj++) {
                long h = CellShapes.cell(salt(), ci + di, cj + dj, CRATER_SALT);
                if (CellShapes.unit(h, 0) >= Ring5Outer.CRATER_CHANCE) {
                    continue;
                }
                double radius = weight * (Ring5Outer.CRATER_MIN_RADIUS
                        + (Ring5Outer.CRATER_MAX_RADIUS - Ring5Outer.CRATER_MIN_RADIUS) * CellShapes.unit(h, 1));
                double cx = (ci + di + 0.2 + 0.6 * CellShapes.unit(h, 2)) * cell;
                double cz = (cj + dj + 0.2 + 0.6 * CellShapes.unit(h, 3)) * cell;
                double d = Math.hypot(x + 0.5 - cx, z + 0.5 - cz) + 1.5 * noises.detail.getValue(x * 0.9, 0.0, z * 0.9);
                value = Math.max(value, Mth.clamp(1.0 + (radius - d) / Ring5Outer.SCAR_RIM, 0.0, 1.0));
            }
        }
        return value;
    }

    /** How far (blocks) the upside-down hills of {@code inverted_hills} hang under a floe here (before their weight). */
    private double hillDepth(int x, int z) {
        int cell = Ring5Outer.HILL_CELL;
        int ci = Math.floorDiv(x, cell);
        int cj = Math.floorDiv(z, cell);
        double depth = 0.0;
        for (int di = -1; di <= 1; di++) {
            for (int dj = -1; dj <= 1; dj++) {
                long h = CellShapes.cell(salt(), ci + di, cj + dj, HILL_SALT);
                if (CellShapes.unit(h, 0) >= Ring5Outer.HILL_CHANCE) {
                    continue;
                }
                double radius = Ring5Outer.HILL_MIN_RADIUS + (Ring5Outer.HILL_MAX_RADIUS - Ring5Outer.HILL_MIN_RADIUS) * CellShapes.unit(h, 1);
                double cx = (ci + di + 0.25 + 0.5 * CellShapes.unit(h, 2)) * cell;
                double cz = (cj + dj + 0.25 + 0.5 * CellShapes.unit(h, 3)) * cell;
                double d = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
                if (d < radius) {
                    double full = Ring5Outer.HILL_MIN_DEPTH + (Ring5Outer.HILL_MAX_DEPTH - Ring5Outer.HILL_MIN_DEPTH) * CellShapes.unit(h, 4);
                    depth = Math.max(depth, full * Math.pow(1.0 - d / radius, 1.3));
                }
            }
        }
        return depth;
    }

    /** The floating chunk of fragment cell (i, j), or null. Cached, pure. */
    @Nullable
    Fragment fragment(int i, int j) {
        long key = (((long) i) << 32) ^ (j & 0xFFFFFFFFL);
        Optional<Fragment> known = decisions.fragments.peek(key);
        if (known == null) {
            known = Optional.ofNullable(placeFragment(i, j));
            decisions.fragments.put(key, known);
        }
        return known.orElse(null);
    }

    @Nullable
    private Fragment placeFragment(int i, int j) {
        int cell = Ring5Outer.FRAGMENT_CELL;
        long h = CellShapes.cell(salt(), i, j, FRAGMENT_SALT);
        double x = (i + 0.2 + 0.6 * CellShapes.unit(h, 0)) * cell;
        double z = (j + 0.2 + 0.6 * CellShapes.unit(h, 1)) * cell;
        RingLayout l = layout.get();
        double effective = l.effectiveDistance(x, z);
        double signed = Ring5Band.of(l).signedDistance(x, z);
        double r5 = ring5Radius(l);
        double empty = emptyFrom(l);
        if (signed < 0.0 || effective >= empty) {
            return null;
        }
        double progress = Ring5Outer.progress(effective, signed, r5);
        double edge = effective > r5 ? (effective - r5) / (empty - r5) : 0.0;
        if (CellShapes.unit(h, 2) >= Ring5Outer.fragmentChance(progress, edge)) {
            return null;
        }
        double radius = Math.max(Ring5Outer.FRAGMENT_MIN_RADIUS,
                Ring5Outer.fragmentMaxRadius(progress, edge) * (0.35 + 0.65 * CellShapes.unit(h, 3)));
        // Reach in effective distance grows at most ~1.2 per block (boundary noise); keep the whole chunk before the empty edge.
        if (effective + 1.3 * radius >= empty || nearCastle(x, z, radius + 4.0) || groundAt(x, z)) {
            return null;
        }
        double thickness = Math.max(Ring5Outer.MIN_THICK, radius * (0.5 + 0.7 * CellShapes.unit(h, 4)));
        double tilt = Ring5Outer.FRAGMENT_MAX_TILT * CellShapes.unit(h, 5);
        double lean = Math.tan(Math.toRadians(tilt)) * radius;
        int low = (int) Math.ceil(Ring5Outer.FRAGMENT_MIN_Y + thickness + lean);
        int high = (int) Math.floor(Ring5Outer.FRAGMENT_MAX_Y - lean);
        int top = low + (int) Math.floor((high - low + 1) * CellShapes.unit(h, 6));
        return new Fragment(x, z, radius, top - (int) Math.round(thickness), top, tilt, 360.0 * CellShapes.unit(h, 7));
    }

    /** The narrow ridges of the outer band (V6-A), one per ridge cell round the ring; decided once per world. */
    List<Bridge> ridges() {
        List<Bridge> known = decisions.ridges;
        if (known == null) {
            List<Bridge> ridges = new ArrayList<>();
            RingLayout l = layout.get();
            double lineRadius = lineRadius(l);
            int count = RingArc.cellCount(RingArc.perimeter(lineRadius), Ring5Outer.RIDGE_CELL);
            double cellSize = RingArc.perimeter(lineRadius) / count;
            for (int k = 0; k < count; k++) {
                long h = CellShapes.cell(salt(), k, 0, RIDGE_SALT);
                double along = (k + 0.5) * cellSize + (CellShapes.unit(h, 0) - 0.5) * 2.0 * Ring5Outer.RIDGE_JITTER;
                double[] ray = stripRay(l, along / lineRadius);
                double half = Ring5Outer.RIDGE_HALF_MIN + (Ring5Outer.RIDGE_HALF_MAX - Ring5Outer.RIDGE_HALF_MIN) * CellShapes.unit(h, 1);
                double thick = Ring5Outer.RIDGE_MIN_THICK + (Ring5Outer.RIDGE_MAX_THICK - Ring5Outer.RIDGE_MIN_THICK) * CellShapes.unit(h, 2);
                // A ridge's deck follows the ground's top; deckY and endY are its ends, sag holds its thickness.
                ridges.add(new Bridge(-1, ray[0], ray[1], ray[2], ray[3], half, (int) Math.round(ray[4]), (int) Math.round(ray[5]), thick));
            }
            known = List.copyOf(ridges);
            decisions.ridges = known;
        }
        return known;
    }

    /** The strips of stepping stones half-way between the ridges (V6-A), one per ridge cell; decided once per world. */
    List<Strip> strips() {
        List<Strip> known = decisions.strips;
        if (known == null) {
            List<Strip> strips = new ArrayList<>();
            RingLayout l = layout.get();
            double lineRadius = lineRadius(l);
            int count = RingArc.cellCount(RingArc.perimeter(lineRadius), Ring5Outer.RIDGE_CELL);
            double cellSize = RingArc.perimeter(lineRadius) / count;
            for (int k = 0; k < count; k++) {
                long h = CellShapes.cell(salt(), k, 0, STRIP_SALT);
                double along = (k + 1.0) * cellSize + (CellShapes.unit(h, 0) - 0.5) * 2.0 * Ring5Outer.RIDGE_JITTER;
                strips.add(placeStrip(l, stripRay(l, along / lineRadius), h));
            }
            known = List.copyOf(strips);
            decisions.strips = known;
        }
        return known;
    }

    /** A ray at {@code angle} from the band line out to {@link Ring5Outer#STRIP_END}: {fromX, fromZ, toX, toZ, topFrom, topTo}. */
    private double[] stripRay(RingLayout l, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        Ring5Band band = Ring5Band.of(l);
        double r5 = ring5Radius(l);
        double inner = l.settings().ringOuterRadii().get(Ring.RING_4.ordinal());
        double from = bisect(inner, r5 + 200.0, d -> band.signedDistance(d * cos, d * sin));
        double to = bisect(from, r5 + 200.0, d -> Ring5Outer.progress(l.effectiveDistance(d * cos, d * sin),
                band.signedDistance(d * cos, d * sin), r5) - Ring5Outer.STRIP_END);
        double fx = from * cos;
        double fz = from * sin;
        double tx = to * cos;
        double tz = to * sin;
        return new double[]{fx, fz, tx, tz, groundTop(fx, fz, 0.0, 0.0),
                groundTop(tx, tz, Ring5Outer.LINE_BLEND, Ring5Outer.STRIP_END)};
    }

    private static double bisect(double low, double high, java.util.function.DoubleUnaryOperator f) {
        for (int i = 0; i < 40; i++) {
            double mid = 0.5 * (low + high);
            if (f.applyAsDouble(mid) < 0.0) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return 0.5 * (low + high);
    }

    private Strip placeStrip(RingLayout l, double[] ray, long h) {
        double dx = ray[2] - ray[0];
        double dz = ray[3] - ray[1];
        double length = Math.hypot(dx, dz);
        double ux = dx / length;
        double uz = dz / length;
        Ring5Band band = Ring5Band.of(l);
        double r5 = ring5Radius(l);
        int count = (int) Math.floor(length / Ring5Outer.STONE_STEP) + 1;
        List<Fragment> stones = new ArrayList<>();
        int previous = Integer.MIN_VALUE;
        for (int i = 0; i < count; i++) {
            double x = ray[0] + ux * i * Ring5Outer.STONE_STEP;
            double z = ray[1] + uz * i * Ring5Outer.STONE_STEP;
            double signed = Math.max(0.0, band.signedDistance(x, z));
            double progress = Math.max(0.0, Ring5Outer.progress(l.effectiveDistance(x, z), signed, r5));
            int top = (int) Math.round(groundTop(x, z, signed, progress));
            if (previous != Integer.MIN_VALUE) {
                // Each stone at most one block above or below the last, so the strip can be jumped.
                top = Mth.clamp(top, previous - 1, previous + 1);
            }
            previous = top;
            if (groundAt(x, z) || nearCastle(x, z, Ring5Outer.STONE_RADIUS + 2.0)) {
                continue;
            }
            int thick = 3 + (int) Math.floor(2.0 * CellShapes.unit(h, 10 + i));
            stones.add(new Fragment(x, z, Ring5Outer.STONE_RADIUS, top - thick, top, 0.0, 0.0));
        }
        return new Strip(ray[0], ray[1], ux, uz, length, List.copyOf(stones));
    }

    // ---------------------------------------------------------------- castle bridges

    /** The two bridges of each castle (V6-A), decided once per world. */
    public List<Bridge> castleBridges() {
        List<Bridge> known = decisions.bridges;
        if (known == null) {
            List<Bridge> bridges = new ArrayList<>();
            for (CastleSite site : castleSites()) {
                bridges.addAll(planBridges(site));
            }
            known = List.copyOf(bridges);
            decisions.bridges = known;
        }
        return known;
    }

    /**
     * A castle's two bridges: the first along the inward ray to the first ground, the second at 30–60° from that ray to
     * the largest floe in reach (counted in floe cells).
     */
    private List<Bridge> planBridges(CastleSite site) {
        double length = Math.hypot(site.x(), site.z());
        double ux = -site.x() / length;
        double uz = -site.z() / length;
        double start = site.radius() + rimWidth(site) + 1.0;
        double first = landing(site, ux, uz, start, Ring5Castles.FAR_REACH);
        if (Double.isNaN(first)) {
            // The inner band lies within reach of every site (75 % of ring 5), so this means a broken layout.
            throw new IllegalStateException("No ground on the inward ray of castle " + site);
        }
        List<Bridge> bridges = new ArrayList<>(Ring5Castles.BRIDGES);
        bridges.add(bridge(site, 0, ux, uz, first));
        for (double reach : new double[]{Ring5Castles.REACH, Ring5Castles.FAR_REACH}) {
            double bestT = Double.NaN;
            double bestX = 0.0;
            double bestZ = 0.0;
            int bestSize = -1;
            for (double angle : Ring5Castles.SIDE_ANGLES) {
                for (int side = -1; side <= 1; side += 2) {
                    double a = Math.toRadians(side * angle);
                    double dx = ux * Math.cos(a) - uz * Math.sin(a);
                    double dz = ux * Math.sin(a) + uz * Math.cos(a);
                    double t = landing(site, dx, dz, start, reach);
                    if (Double.isNaN(t)) {
                        continue;
                    }
                    int size = floeSize(site.x() + dx * (t + Ring5Castles.LANDING_DEPTH), site.z() + dz * (t + Ring5Castles.LANDING_DEPTH));
                    if (size > bestSize || (size == bestSize && t < bestT)) {
                        bestSize = size;
                        bestT = t;
                        bestX = dx;
                        bestZ = dz;
                    }
                }
            }
            if (bestSize >= 0) {
                bridges.add(bridge(site, 1, bestX, bestZ, bestT));
                return bridges;
            }
        }
        throw new IllegalStateException("No ground for the second bridge of castle " + site);
    }

    /** How far from the site's centre along (dx, dz) the first ground lies that holds for a landing; NaN if none in reach. */
    private double landing(CastleSite site, double dx, double dz, double start, double reach) {
        for (double t = start; t <= start + reach; t += 1.0) {
            double x = site.x() + dx * t;
            double z = site.z() + dz * t;
            if (groundAt(x, z) && groundAt(x + dx * Ring5Castles.LANDING_DEPTH / 2.0, z + dz * Ring5Castles.LANDING_DEPTH / 2.0)
                    && groundAt(x + dx * Ring5Castles.LANDING_DEPTH, z + dz * Ring5Castles.LANDING_DEPTH)) {
                return t;
            }
        }
        return Double.NaN;
    }

    /** The size of the floe under a column, in floe cells joined to its own (8 neighbours), up to the cap. */
    int floeSize(double x, double z) {
        double[] cell = floeCell(Mth.floor(x), Mth.floor(z));
        if (cell[3] == 0.0) {
            return 0;
        }
        Set<Long> seen = new HashSet<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{(int) cell[0], (int) cell[1]});
        seen.add((((long) (int) cell[0]) << 32) ^ ((int) cell[1] & 0xFFFFFFFFL));
        while (!queue.isEmpty() && seen.size() < Ring5Castles.FLOE_COUNT_CAP) {
            int[] c = queue.poll();
            for (int di = -1; di <= 1; di++) {
                for (int dj = -1; dj <= 1; dj++) {
                    int i = c[0] + di;
                    int j = c[1] + dj;
                    long key = (((long) i) << 32) ^ (j & 0xFFFFFFFFL);
                    if (!seen.contains(key) && floeSolid(i, j)) {
                        double[] p = floePoint(i, j);
                        if (!nearCastle(p[0], p[1], 0.0)) {
                            seen.add(key);
                            queue.add(new int[]{i, j});
                        }
                    }
                }
            }
        }
        return Math.min(seen.size(), Ring5Castles.FLOE_COUNT_CAP);
    }

    /** The bridge {@code k} of a site along (dx, dz), landing {@code t} from its centre. */
    private Bridge bridge(CastleSite site, int k, double dx, double dz, double t) {
        long h = CellShapes.cell(salt(), site.index(), k, BRIDGE_SALT);
        double fx = site.x() + dx * (site.radius() - 2.0);
        double fz = site.z() + dz * (site.radius() - 2.0);
        double tx = site.x() + dx * (t + Ring5Castles.LANDING_OVERLAP);
        double tz = site.z() + dz * (t + Ring5Castles.LANDING_OVERLAP);
        double length = Math.hypot(tx - fx, tz - fz);
        RingLayout l = layout.get();
        double signed = Ring5Band.of(l).signedDistance(tx, tz);
        double landTop = signed < 0.0 ? innerTop(tx, tz)
                : groundTop(tx, tz, signed, Ring5Outer.progress(l.effectiveDistance(tx, tz), signed, ring5Radius(l)));
        int end = (int) Math.round(Math.max(landTop, site.top() - Ring5Castles.MAX_SLOPE * length));
        double half = Ring5Castles.HALF_MIN + (Ring5Castles.HALF_MAX - Ring5Castles.HALF_MIN) * CellShapes.unit(h, 0);
        double sag = Ring5Castles.SAG_MIN + (Ring5Castles.SAG_MAX - Ring5Castles.SAG_MIN) * CellShapes.unit(h, 1);
        return new Bridge(site.index(), fx, fz, tx, tz, half, site.top(), end, sag);
    }

    // ---------------------------------------------------------------- castle sites

    /** A pure per-world value for the decisions, from the hash noise. */
    long salt() {
        Long known = decisions.salt;
        if (known == null) {
            // Pure: racing threads compute the same value.
            known = SeededHash.hash(hash, 5432, -9876, WORLD_SALT);
            decisions.salt = known;
        }
        return known;
    }

    /**
     * The four sites: the first quarter's direction θ0 uniform in [0°, 90°), site k at θ0 + k · 90° ± {@link #CASTLE_JITTER},
     * each on its ray at the point whose effective distance is {@link #CASTLE_SHARE} of ring 5 (so the boundary noise
     * cannot move it out of the outer band), with its own radius, top and belt. Sorted as {@link #castleSites} says.
     */
    static List<CastleSite> placeCastles(RingLayout layout, long salt) {
        List<Integer> radii = layout.settings().ringOuterRadii();
        double inner = radii.get(Ring.RING_4.ordinal());
        double target = inner + CASTLE_SHARE * (radii.get(Ring.RING_5.ordinal()) - inner);
        double first = 90.0 * SeededHash.unit(salt, 0);
        List<double[]> raw = new ArrayList<>();
        for (int k = 0; k < CASTLES; k++) {
            double angle = first + 90.0 * k + CASTLE_JITTER * (2.0 * SeededHash.unit(salt, 1 + k) - 1.0);
            angle = ((angle % 360.0) + 360.0) % 360.0;
            double radians = Math.toRadians(angle);
            double distance = alongRay(layout, Math.cos(radians), Math.sin(radians), target);
            int radius = CASTLE_MIN_RADIUS + (int) Math.floor((CASTLE_MAX_RADIUS - CASTLE_MIN_RADIUS + 1) * SeededHash.unit(salt, 1 + CASTLES + k));
            int top = CASTLE_MIN_TOP + (int) Math.floor((CASTLE_MAX_TOP - CASTLE_MIN_TOP + 1) * SeededHash.unit(salt, 1 + 2 * CASTLES + k));
            int belt = CASTLE_MIN_BELT + (int) Math.floor((CASTLE_MAX_BELT - CASTLE_MIN_BELT + 1) * SeededHash.unit(salt, 1 + 3 * CASTLES + k));
            raw.add(new double[]{angle, Math.round(distance * Math.cos(radians)), Math.round(distance * Math.sin(radians)), radius, top, belt});
        }
        // Index 0 closest to east; then by growing angle from there (+x towards +z: clockwise on a north-up map).
        raw.sort(Comparator.comparingDouble(r -> Math.abs(signedAngle(r[0]))));
        double start = raw.getFirst()[0];
        raw.sort(Comparator.comparingDouble(r -> ((r[0] - start) % 360.0 + 360.0) % 360.0));
        List<CastleSite> sites = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            double[] r = raw.get(i);
            sites.add(new CastleSite(i, r[0], (int) r[1], (int) r[2], (int) r[3], (int) r[4], (int) r[5]));
        }
        return sites;
    }

    /** An angle in [0°, 360°) as (−180°, 180°]. */
    private static double signedAngle(double angle) {
        return angle > 180.0 ? angle - 360.0 : angle;
    }

    /**
     * The distance along the ray (cos, sin) from 0,0 where the effective distance is {@code target}: bisection between
     * the two radii the boundary noise allows (it moves the effective distance at most {@code boundaryNoise}).
     */
    static double alongRay(RingLayout layout, double cos, double sin, double target) {
        double margin = layout.settings().boundaryNoise() + 1.0;
        double low = target - margin;
        double high = target + margin;
        for (int i = 0; i < 40; i++) {
            double mid = 0.5 * (low + high);
            if (layout.effectiveDistance(mid * cos, mid * sin) < target) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return 0.5 * (low + high);
    }

    // ---------------------------------------------------------------- types

    /** The field's own grid (cells for later shapes). */
    public record Settings(int cellSize) {
        public static final Settings DEFAULT = new Settings(128);

        public static final Codec<Settings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(32, 512).fieldOf("cell_size").forGetter(Settings::cellSize)
        ).apply(instance, Settings::new));
    }

    /**
     * The noises of the terrain: {@code rot} and {@code glitch} the region axes of the router's temperature and vegetation
     * ({@code ring_5_rot}, {@code ring_5_glitch}, the same as {@code ring_5/overworld/region_rot} and {@code region_glitch}),
     * {@code hills} the inner band's hills, {@code relief} the floes' tops, {@code detail} rags and roughness,
     * {@code scars} the cracks of {@code void_scars}.
     */
    public record Noises(NoiseHolder rot, NoiseHolder glitch, NoiseHolder hills, NoiseHolder relief, NoiseHolder detail,
                         NoiseHolder scars) {
        public static final Codec<Noises> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                NoiseHolder.CODEC.fieldOf("rot").forGetter(Noises::rot),
                NoiseHolder.CODEC.fieldOf("glitch").forGetter(Noises::glitch),
                NoiseHolder.CODEC.fieldOf("hills").forGetter(Noises::hills),
                NoiseHolder.CODEC.fieldOf("relief").forGetter(Noises::relief),
                NoiseHolder.CODEC.fieldOf("detail").forGetter(Noises::detail),
                NoiseHolder.CODEC.fieldOf("scars").forGetter(Noises::scars)
        ).apply(instance, Noises::new));

        Noises map(Visitor visitor) {
            return new Noises(visitor.visitNoise(rot), visitor.visitNoise(glitch), visitor.visitNoise(hills), visitor.visitNoise(relief),
                    visitor.visitNoise(detail), visitor.visitNoise(scars));
        }
    }

    /**
     * A castle site (V5-A): {@code index} 0–3 ({@link #castleSites}), {@code angle} in degrees from east (+x) towards
     * south (+z), the plateau's centre {@code x}, {@code z} and {@code radius}, the pinnacle's {@code top} (the Y of its
     * highest block + 1) and the width of the void {@code belt} around it.
     */
    public record CastleSite(int index, double angle, int x, int z, int radius, int top, int belt) {
        public double distance(double px, double pz) {
            return Math.hypot(px - x, pz - z);
        }
    }

    /**
     * A bridge to a castle or a narrow ridge of the outer band (84, V6-A); {@code siteIndex} −1 for a ridge. The deck runs
     * from (fromX, fromZ) to (toX, toZ), {@code halfWidth} either side. A castle's bridge: its deck's top (the Y above
     * its highest block) is {@code deckY} at the plateau and {@code endY} where it lands, sagging {@code sag} in the
     * middle. A ridge follows the ground's top ({@code deckY}/{@code endY} its ends) and {@code sag} is its thickness.
     */
    public record Bridge(int siteIndex, double fromX, double fromZ, double toX, double toZ, double halfWidth, int deckY,
                         int endY, double sag) {
        /** Whether the deck's footprint comes into the box (blocks, inclusive). */
        public boolean touches(int minX, int minZ, int maxX, int maxZ) {
            double margin = halfWidth + 1.0;
            if (Math.max(fromX, toX) + margin < minX || Math.min(fromX, toX) - margin > maxX + 1
                    || Math.max(fromZ, toZ) + margin < minZ || Math.min(fromZ, toZ) - margin > maxZ + 1) {
                return false;
            }
            // The segment's distance to the box's centre against the box's half diagonal.
            double cx = (minX + maxX + 1) / 2.0;
            double cz = (minZ + maxZ + 1) / 2.0;
            double dx = toX - fromX;
            double dz = toZ - fromZ;
            double t = Mth.clamp(((cx - fromX) * dx + (cz - fromZ) * dz) / (dx * dx + dz * dz), 0.0, 1.0);
            double distance = Math.hypot(cx - fromX - t * dx, cz - fromZ - t * dz);
            return distance <= Math.hypot(maxX + 1 - minX, maxZ + 1 - minZ) / 2.0 + margin;
        }
    }

    /**
     * A floating chunk or stepping stone of the outer band (84): centre, radius, the lowest Y and the top (the Y above
     * its highest block) at its centre, tilt (degrees, up to 20) towards {@code direction} (degrees from +x to +z).
     */
    public record Fragment(double x, double z, double radius, int bottom, int top, double tilt, double direction) {
        public boolean touches(int minX, int minZ, int maxX, int maxZ) {
            return x + radius >= minX && x - radius <= maxX + 1 && z + radius >= minZ && z - radius <= maxZ + 1;
        }

        /** Adds the chunk's span over the column, if its footprint covers it: thickest in the middle, ≥ 3 at its rim. */
        void span(Column column, int bx, int bz) {
            double dx = bx + 0.5 - x;
            double dz = bz + 0.5 - z;
            double d = Math.hypot(dx, dz);
            if (d > radius) {
                return;
            }
            double a = Math.toRadians(direction);
            int columnTop = (int) Math.round(top + Math.tan(Math.toRadians(tilt)) * (dx * Math.cos(a) + dz * Math.sin(a)));
            double thickness = Ring5Outer.MIN_THICK + (top - bottom - Ring5Outer.MIN_THICK) * Math.pow(1.0 - d / radius, 0.7);
            column.addSpan(columnTop - (int) Math.round(thickness), columnTop);
        }
    }

    /** A strip of stepping stones along a ray of the outer band. */
    record Strip(double fromX, double fromZ, double ux, double uz, double length, List<Fragment> stones) {
        void span(Column column, int x, int z) {
            double px = x + 0.5 - fromX;
            double pz = z + 0.5 - fromZ;
            double along = px * ux + pz * uz;
            if (along < -Ring5Outer.STONE_RADIUS || along > length + Ring5Outer.STONE_RADIUS
                    || Math.abs(px * uz - pz * ux) > Ring5Outer.STONE_RADIUS) {
                return;
            }
            for (Fragment stone : stones) {
                stone.span(column, x, z);
            }
        }
    }

    /** The 2D values of one column. */
    public static final class Column {
        /** The Y of the ground's surface (the block above the highest rock); NaN over the void. */
        double top;
        /** The Y under which the column is open (void); −∞ for full ground down to the bedrock; NaN over the void. */
        double bottom;
        /** 0..1: how far the ground has fallen away (1 void without any chunk). */
        double breakup;
        /** 0..1: the castle surroundings (1 on the plateau, lower on its rim, belt and bridges). */
        double castle;
        /** 0..1: a scar open to the void (1), and its rim. */
        double scars;
        /** 0..1: how far an upside-down hill hangs under the floe (1 = 40 blocks). */
        double inverted;
        /** The ground's top without scars (a scar column: its floe's top), {@link #VOID_HEIGHT} over the void. */
        double height;
        /** 0..1: the glitch fields' weight (83). */
        double glitch;
        /** The inner band's preliminary surface, NaN until asked for. */
        double initial;
        boolean inner;
        /** The glitch fields' shifted layers apply (83). */
        boolean layered;
        boolean scarOpen;
        int spans;
        final int[] spanBottom = new int[MAX_SPANS];
        final int[] spanTop = new int[MAX_SPANS];

        void reset() {
            top = 0.0;
            bottom = Double.NEGATIVE_INFINITY;
            breakup = 0.0;
            castle = 0.0;
            scars = 0.0;
            inverted = 0.0;
            height = VOID_HEIGHT;
            glitch = 0.0;
            initial = Double.NaN;
            inner = false;
            layered = false;
            scarOpen = false;
            spans = 0;
        }

        void setVoid() {
            top = Double.NaN;
            bottom = Double.NaN;
            height = VOID_HEIGHT;
        }

        void addSpan(int from, int to) {
            if (spans < MAX_SPANS && to > from) {
                spanBottom[spans] = from;
                spanTop[spans] = to;
                spans++;
            }
        }

        public double top() {
            return top;
        }

        public double bottom() {
            return bottom;
        }

        public double breakup() {
            return breakup;
        }

        public double castle() {
            return castle;
        }

        public double scars() {
            return scars;
        }

        public double inverted() {
            return inverted;
        }

        public double height() {
            return height;
        }

        public double glitch() {
            return glitch;
        }

        public boolean isVoid() {
            return Double.isNaN(top);
        }
    }

    /**
     * Per-world decisions: pure functions of the seed and the layout, shared by all copies of one wired field (the noise
     * chunk makes a copy per chunk), so they are made once per world; the slots only remember pure per-cell results.
     */
    static final class Decisions {
        volatile Long salt;
        volatile List<CastleSite> sites;
        volatile List<Bridge> bridges;
        volatile List<Bridge> ridges;
        volatile List<Strip> strips;
        final KeyedSlots<Boolean> floes = new KeyedSlots<>(1024);
        final KeyedSlots<Optional<Fragment>> fragments = new KeyedSlots<>(256);
        volatile Ring5Inner inner;
        final KeyedSlots<Optional<int[]>> cubes = new KeyedSlots<>(256);
        final KeyedSlots<Optional<double[]>> cracks = new KeyedSlots<>(64);
    }
}
