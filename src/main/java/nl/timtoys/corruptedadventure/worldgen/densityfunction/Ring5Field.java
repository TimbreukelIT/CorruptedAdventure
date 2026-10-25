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
 * {@link Ring5Inner}, relief revised in 83b): rolling dead ground with ridges and cliffs, per region the shapes that are
 * just wrong (stairs and pits, a regular wave, blocky ground with shifted layers and floating cubes), loose cracks down
 * into the void and the corrupt spots (83b, {@link Ring5CorruptSpots}): shifted chunks and a chunk fallen into the void.
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
    /** A corrupt spot's chunks keep this far from the band line and the zone 4→5, and this far off a castle's reach (83b). */
    static final double SPOT_CLEARANCE = 32.0;
    static final double SPOT_CASTLE_CLEARANCE = 16.0;
    /** Open intervals of the own caves (93) one column keeps at most. */
    static final int MAX_CAVES = 8;
    /**
     * Vanilla's damped caves (93, V93-1 A) only deep in the inner band: closed ({@link #CAVES_CLOSED}) within these many
     * blocks of the band line, the zone 4→5, a castle's reach and an opening into the void (cracks, spot holes, void-cave
     * holes), fully open {@link #NATURAL_FADE} further in.
     */
    static final double NATURAL_LINE = 24.0;
    static final double NATURAL_ZONE = 32.0;
    static final double NATURAL_CASTLE = 16.0;
    static final double NATURAL_OPENING = 8.0;
    static final double NATURAL_FADE = 12.0;
    static final double CAVES_CLOSED = 2.0;
    /** Vanilla's noise caves only this many blocks under the ground's top (or under a glitch field's layers); above it the entrances. */
    static final int NATURAL_COVER = 8;
    /** The own caves (93) keep this far off a corrupt spot's chunks and a doppelganger's squares (blocks, Chebyshev). */
    static final int CAVE_CLEARANCE = 8;
    /** How far a glitch field's shifted layers reach round the column's top (83; {@link Column#layered}). */
    public static final int LAYER_DEPTH = Ring5Inner.LAYER_DEPTH;
    /** How deep the rock rim round a void cave's hole is (93; {@link #holeRimTop}). */
    public static final int HOLE_RIM_DEPTH = Ring5Caves.RIM_DEPTH;
    /** The outer band's void caves (93, V93-8 A) lie at most this far beyond the band line (its signed distance). */
    static final double OUTER_CAVE_REACH = 300.0;

    private static final int FLOE_SALT = 8401;
    private static final int CRATER_SALT = 8402;
    private static final int HILL_SALT = 8403;
    private static final int FRAGMENT_SALT = 8404;
    private static final int RIDGE_SALT = 8405;
    private static final int STRIP_SALT = 8406;
    private static final int BRIDGE_SALT = 8407;
    private static final int RIM_SALT = 8408;
    private static final int DECK_SALT = 8409;
    private static final int OUTER_FLOOR_SALT = 9307;

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

    /**
     * The preliminary surface of the column: its top, or {@link #VOID_HEIGHT} (no surface) over the void. In the inner band
     * (83) it is the ground's height, also on a crack.
     */
    public double initialHeight(int x, int z) {
        Column column = column(x, z);
        if (!column.inner) {
            return column.isVoid() ? VOID_HEIGHT : column.top;
        }
        // The inner band (83): the lowest ground round the column (its shapes, not its cracks or cubes), less a margin.
        double initial = column.initial;
        if (Double.isNaN(initial)) {
            initial = column.height;
            for (int k = 0; k < 8; k++) {
                double a = Math.PI / 4.0 * k;
                initial = Math.min(initial, copiedGround(Mth.floor(x + 0.5 + INITIAL_RADIUS * Math.cos(a)),
                        Mth.floor(z + 0.5 + INITIAL_RADIUS * Math.sin(a))));
            }
            initial -= INITIAL_MARGIN;
            column.initial = initial;
        }
        return initial;
    }

    /**
     * The terrain's density at a block, exact: inside an exact span 1; in one of the own caves (93) or a void column −1;
     * else 1/8 per block away from the column's top and underside, held to ±1 (smooth enough for the ring blend of the
     * zones 4→5 and 5→void).
     */
    public double terrain(int x, int y, int z) {
        Column column = column(x, z);
        for (int k = 0; k < column.spans; k++) {
            if (y >= column.spanBottom[k] && y < column.spanTop[k]) {
                return 1.0;
            }
        }
        for (int k = 0; k < column.caves; k++) {
            if (y >= column.caveBottom[k] && y < column.caveTop[k]) {
                return -1.0;
            }
        }
        if (column.isVoid()) {
            return -1.0;
        }
        if (column.layered && y >= column.top - Ring5Inner.LAYER_DEPTH && y < column.top + Ring5Inner.LAYER_DEPTH) {
            // The glitch fields' layers (83): each slice is the ground of a column a few blocks over. In a doppelganger's
            // copy (93) the shift is the source's, mirrored.
            Ring5Copies.Pair copy = column.copy;
            int[] shift = copy == null ? inner().layerShift(x, y, z) : inner().layerShift(column.sourceX, y, column.sourceZ);
            int dx = copy != null && copy.alongX() ? -shift[0] : shift[0];
            int dz = copy != null && !copy.alongX() ? -shift[1] : shift[1];
            Column other = column(x + dx, z + dz);
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
        // A doppelganger's target (93): inside its blending edge the column is its source's mirror image.
        Ring5Copies.Pair pair = copyPair(Ring5Copies.cellOf(x), Ring5Copies.cellOf(z));
        if (pair != null && pair.inTarget(x, z)) {
            if (pair.core(x, z)) {
                int sx = pair.sourceX(x, z);
                int sz = pair.sourceZ(x, z);
                fillAt(c, sx, sz);
                c.copy = pair;
                c.sourceX = sx;
                c.sourceZ = sz;
                // A shifted layer copies a column a few blocks over: only where that one is a copy too.
                c.layered &= pair.depth(x, z) >= pair.edge() + Ring5Inner.MAX_SHIFT;
                if (c.inner && !c.isVoid()) {
                    c.room = Math.min(c.room, naturalRoom(x, z));
                }
            } else {
                fillAt(c, x, z);
                blendCopy(c, pair, x, z);
            }
            // Vanilla's caves run at the real column, not the source: they and the dam see the openings round both.
            if (c.inner && !c.isVoid()) {
                c.opening = openingDistance(x, z);
                c.closure = closure(c.room, c.opening);
            }
            return;
        }
        fillAt(c, x, z);
    }

    private void fillAt(Column c, int x, int z) {
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
        if (castleReach || c.isVoid()) {
            return;
        }
        if (c.inner) {
            innerCaves(c, x, z);
            c.opening = Math.min(c.crack, chamberHoleDistance(x, z));
            c.room = naturalRoom(l, signed, effective, castleDistance - castle.radius() - castle.belt());
            c.closure = closure(c.room, c.opening);
        } else {
            Ring5Caves.Chamber chamber = outerChamber(Ring5Caves.outerCellOf(x), Ring5Caves.outerCellOf(z));
            if (chamber != null) {
                chamberCave(c, chamber, x, z);
            }
        }
    }

    /**
     * A target column in a doppelganger's blending edge (93, V93-7 A): its own column with the ground blended towards the
     * source's (the copy's weight); no shifted layers there (they would copy a column a few blocks over).
     */
    private void blendCopy(Column c, Ring5Copies.Pair pair, int x, int z) {
        if (!c.inner || c.isVoid()) {
            return;
        }
        double source = innerGround(pair.sourceX(x, z), pair.sourceZ(x, z));
        double weight = pair.weight(x, z);
        c.top = Mth.lerp(weight, c.top, source);
        // The pit of whichever ground weighs most.
        if (weight >= 0.5) {
            c.pit = inner().pitFloor(pair.sourceX(x, z), pair.sourceZ(x, z));
        }
        c.height = c.top;
        c.layered = false;
    }

    /** The own caves of an inner column (93): the corridors and shafts of its net cell's net, its chamber cell's void cave. */
    private void innerCaves(Column c, int x, int z) {
        Ring5Caves.Net net = cavesNet(Ring5Caves.netCellOf(x), Ring5Caves.netCellOf(z));
        if (net != null && net.near(x, z, 0)) {
            for (Ring5Caves.Box box : net.boxes) {
                if (box.covers(x, z)) {
                    c.addCave(box.y0(), box.y1(), Ring5Caves.Kind.CORRIDOR);
                }
            }
        }
        Ring5Caves.Chamber chamber = chamber(Ring5Caves.chamberCellOf(x), Ring5Caves.chamberCellOf(z));
        if (chamber != null) {
            chamberCave(c, chamber, x, z);
        }
    }

    /**
     * How far a column lies inside the room of vanilla's damped caves (93, V93-1 A), its openings aside: off the band line,
     * the zone 4→5 and a castle's reach ({@code castleGap} blocks off it); at most 0 where they are closed.
     */
    private static double naturalRoom(RingLayout l, double signed, double effective, double castleGap) {
        return Math.min(Math.min(-signed - NATURAL_LINE, effective - Ring4Sundered.zoneEnd(l) - NATURAL_ZONE),
                castleGap - NATURAL_CASTLE);
    }

    private double naturalRoom(int x, int z) {
        RingLayout l = layout.get();
        CastleSite castle = nearestCastle(x, z);
        return naturalRoom(l, Ring5Band.of(l).signedDistance(x, z), l.effectiveDistance(x, z),
                castle.distance(x, z) - castle.radius() - castle.belt());
    }

    /** The closure of vanilla's damped caves (93): {@link #CAVES_CLOSED} within {@link #NATURAL_OPENING} of an opening. */
    private static double closure(double room, double opening) {
        return CAVES_CLOSED * (1.0 - Ring4Relief.smooth(Math.min(room, opening - NATURAL_OPENING), 0.0, NATURAL_FADE));
    }

    private static void chamberCave(Column c, Ring5Caves.Chamber chamber, int x, int z) {
        int[] out = c.chamberOut;
        if (chamber.near(x, z, 0.0) && chamber.column(x, z, out)) {
            c.addCave(out[0], out[1], Ring5Caves.Kind.of(out[2]));
            c.rimTop = out[3];
        }
    }

    /**
     * A column of the inner band: the ground with its regions' shapes, shifted in a corrupt spot's chunk; a loose crack or
     * a spot's fallen chunk (void); the glitch fields' layers.
     */
    private void fillInner(Column c, int x, int z) {
        Ring5Inner inner = inner();
        Ring5CorruptSpots.Spot spot = corruptSpot(Ring5CorruptSpots.cellOf(x), Ring5CorruptSpots.cellOf(z));
        double top = mounded(x, z, inner.top(x, z) + spotShift(spot, x, z));
        double glitch = inner.glitchWeight(x, z);
        c.glitch = glitch;
        c.inner = true;
        c.height = top;
        double crack = innerCrackDistance(x, z, spot);
        c.crack = crack;
        if (crack <= 0.0) {
            c.setVoid();
            c.height = top;
            c.scarOpen = true;
            c.scars = 1.0;
            return;
        }
        c.scars = Mth.clamp(1.0 - crack / Ring5Outer.SCAR_RIM, 0.0, 1.0);
        c.pit = inner.pitFloor(x, z);
        c.top = top;
        c.bottom = Double.NEGATIVE_INFINITY;
        // Shifted layers only where the glitch fields hold alone (no half-blended layers), and not within a layer's
        // sideways shift of a corrupt spot: a slice there would copy a chunk lifted or sunk by up to 30 and could hang loose.
        c.layered = glitch >= 0.999 && (spot == null || !spot.near(x, z, Ring5Inner.MAX_SHIFT));
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
            known = new Ring5Inner(noises.hills, noises.rot, noises.glitch, salt(), this::reliefWeight, this::lineWeight);
            decisions.inner = known;
        }
        return known;
    }

    /**
     * The inner band's relief weight (83b): 0 up to the zone 4→5's outer edge (the fracture zone blends the old ground),
     * 1 from {@link Ring5Inner#RELIEF_FADE} after it (effective distance).
     */
    private double reliefWeight(double x, double z) {
        RingLayout l = layout.get();
        double zoneEnd = Ring4Sundered.zoneEnd(l);
        // The boundary noise moves the effective distance at most boundaryNoise: far out the weight is 1 without it.
        if (Math.sqrt(x * x + z * z) >= zoneEnd + Ring5Inner.RELIEF_FADE + l.settings().boundaryNoise() + 1.0) {
            return 1.0;
        }
        return Ring4Relief.smooth(l.effectiveDistance(x, z) - zoneEnd, 0.0, Ring5Inner.RELIEF_FADE);
    }

    /**
     * The inner band's weight of the full relief against the floe range (83b): 0 on and past the band line, 1 from
     * {@link Ring5Inner#LINE_FADE} inside it ({@link Ring5Band#signedDistance}).
     */
    private double lineWeight(double x, double z) {
        RingLayout l = layout.get();
        // The line lies at most its amplitude inside its mean radius and the effective distance at most boundaryNoise
        // inside the radius: well inside that the weight is 1 without the band's noise.
        List<Integer> radii = l.settings().ringOuterRadii();
        double inner = radii.get(Ring.RING_4.ordinal());
        double width = radii.get(Ring.RING_5.ordinal()) - inner;
        double nearest = inner + (Ring5Band.INNER_SHARE - Ring5Band.AMPLITUDE_SHARE) * width;
        if (Math.sqrt(x * x + z * z) + l.settings().boundaryNoise() + 1.0 <= nearest - Ring5Inner.LINE_FADE) {
            return 1.0;
        }
        return 1.0 - Ring4Relief.smooth(Ring5Band.of(l).signedDistance(x, z), -Ring5Inner.LINE_FADE, 0.0);
    }

    /** The inner band's surface (83) at a position, without cracks, corrupt spots, layers and cubes. Pure. */
    private double innerTop(double x, double z) {
        return inner().top(Mth.floor(x), Mth.floor(z));
    }

    /** The inner band's surface with the corrupt spots' shifts (83b), without cracks, holes, layers and cubes. Pure. */
    double innerGround(int x, int z) {
        return inner().top(x, z) + spotShift(corruptSpot(Ring5CorruptSpots.cellOf(x), Ring5CorruptSpots.cellOf(z)), x, z);
    }

    /** The shift of a corrupt spot's chunk at the column (0 outside a spot and on its fallen chunk). */
    private static int spotShift(@Nullable Ring5CorruptSpots.Spot spot, int x, int z) {
        if (spot == null) {
            return 0;
        }
        int k = spot.chunkAt(x, z);
        return k < 0 ? 0 : spot.shift()[k];
    }

    /** The corrupt spots' shapes (83b), made once per world. */
    Ring5CorruptSpots corruptSpots() {
        Ring5CorruptSpots known = decisions.spotShapes;
        if (known == null) {
            known = new Ring5CorruptSpots(salt(), noises.hills);
            decisions.spotShapes = known;
        }
        return known;
    }

    /**
     * The corrupt spot of spot cell (i, j) (83b) if every chunk of it lies well inside the inner band: off the band line and
     * the zone 4→5 by {@link #SPOT_CLEARANCE}, off every castle's reach by {@link #SPOT_CASTLE_CLEARANCE}. Cached, pure.
     */
    @Nullable
    Ring5CorruptSpots.Spot corruptSpot(int i, int j) {
        long key = KeyedSlots.key(i, j);
        Optional<Ring5CorruptSpots.Spot> known = decisions.spots.peek(key);
        if (known == null) {
            known = Optional.ofNullable(keepSpot(corruptSpots().spot(i, j)));
            decisions.spots.put(key, known);
        }
        return known.orElse(null);
    }

    @Nullable
    private Ring5CorruptSpots.Spot keepSpot(@Nullable Ring5CorruptSpots.Spot spot) {
        if (spot == null) {
            return null;
        }
        RingLayout l = layout.get();
        Ring5Band band = Ring5Band.of(l);
        double zoneEnd = Ring4Sundered.zoneEnd(l);
        int c = Ring5CorruptSpots.CHUNK;
        for (int k = 0; k < spot.size(); k++) {
            double x0 = spot.chunkX()[k] * c;
            double z0 = spot.chunkZ()[k] * c;
            for (double[] p : new double[][]{{x0, z0}, {x0 + c, z0}, {x0, z0 + c}, {x0 + c, z0 + c}, {x0 + c / 2.0, z0 + c / 2.0}}) {
                if (band.signedDistance(p[0], p[1]) > -SPOT_CLEARANCE || l.effectiveDistance(p[0], p[1]) < zoneEnd + SPOT_CLEARANCE
                        || nearCastle(p[0], p[1], SPOT_CASTLE_CLEARANCE)) {
                    return null;
                }
            }
        }
        return spot;
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
     * A cube is kept only where every column under it is glitch ground of the inner band (no outer band, castle reach,
     * crack or corrupt spot under it): a cube cut by one of those edges would hang in lines. Pure: it asks the band, the castles and the
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
                Ring5CorruptSpots.Spot spot = corruptSpot(Ring5CorruptSpots.cellOf(x), Ring5CorruptSpots.cellOf(z));
                boolean glitch = band.inner(x, z) && inner().glitchWeight(x, z) > 0.0 && innerCrackDistance(x, z, spot) > 0.0
                        && castle.distance(x, z) > castle.radius() + castle.belt() && (spot == null || spot.chunkAt(x, z) < 0);
                // Nor on a doppelganger's square (93): the copy's edge would cut a source's cube, the blend would move a
                // target's ground under its own.
                Ring5Copies.Pair pair = copyPair(Ring5Copies.cellOf(x), Ring5Copies.cellOf(z));
                if (!glitch || pair != null && (pair.inSource(x, z) || pair.inTarget(x, z))) {
                    return null;
                }
            }
        }
        return cube;
    }

    /**
     * How far (blocks) a column lies from the nearest opening into the void of the inner band: a loose crack (83) or a
     * corrupt spot's fallen chunk (83b); at most 0 exactly on the open columns, infinite where none is near. For the
     * terrain, the rim's {@code scars} and the dam of the lava layer ({@code RingFluidPicker}). Pure.
     */
    public double innerCrackDistance(int x, int z) {
        return innerCrackDistance(x, z, corruptSpot(Ring5CorruptSpots.cellOf(x), Ring5CorruptSpots.cellOf(z)));
    }

    /** {@link #innerCrackDistance} with the column's corrupt spot already looked up (the spot of its own cell, or null). */
    private double innerCrackDistance(int x, int z, @Nullable Ring5CorruptSpots.Spot spot) {
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
        // Every chunk of a spot keeps a chunk from its cell's edge, so only the own cell's hole can lie within the dam.
        if (spot != null) {
            distance = Math.min(distance, corruptSpots().holeDistance(spot, x, z));
        }
        return distance;
    }

    /**
     * How far (blocks) a column of the inner band lies from the nearest opening into the void, the underground ones of 93
     * included: {@link #innerCrackDistance} and the holes of the void caves; in a doppelganger's target also those of its
     * source (mirrored). At most 0 on an open column. For the dam of the lava layer ({@code RingFluidPicker}, per fluid
     * block): kept on the column.
     */
    public double innerOpeningDistance(int x, int z) {
        Column column = column(x, z);
        if (Double.isNaN(column.opening)) {
            column.opening = openingDistance(x, z);
        }
        return column.opening;
    }

    /** {@link #innerOpeningDistance} from the shapes, not the column table (also inside a column's fill). Pure. */
    private double openingDistance(int x, int z) {
        double distance = Math.min(innerCrackDistance(x, z), chamberHoleDistance(x, z));
        Ring5Copies.Pair pair = copyPair(Ring5Copies.cellOf(x), Ring5Copies.cellOf(z));
        if (pair != null && pair.inTarget(x, z)) {
            int sx = pair.sourceX(x, z);
            int sz = pair.sourceZ(x, z);
            distance = Math.min(distance, Math.min(innerCrackDistance(sx, sz), chamberHoleDistance(sx, sz)));
        }
        return distance;
    }

    /** How far (blocks) a column lies from a hole of its chamber cell's void cave (93); infinite without one. Pure. */
    double chamberHoleDistance(int x, int z) {
        // A chamber keeps CHAMBER_MARGIN (40) inside its cell: wider than the dam, so only the own cell's holes count.
        Ring5Caves.Chamber chamber = chamber(Ring5Caves.chamberCellOf(x), Ring5Caves.chamberCellOf(z));
        return chamber == null ? Double.POSITIVE_INFINITY : chamber.holeDistance(x, z);
    }

    /** The code of ring 5's own caves at a block (93, {@link Ring5Caves.Kind}): the DF {@code ring_5/overworld/caves}. */
    public int caveCode(int x, int y, int z) {
        Column column = column(x, z);
        for (int k = 0; k < column.caves; k++) {
            if (y >= column.caveBottom[k] && y < column.caveTop[k]) {
                return column.caveKind[k];
            }
        }
        if (column.rimTop != Integer.MIN_VALUE && y >= column.rimTop - Ring5Caves.RIM_DEPTH && y < column.rimTop) {
            return Ring5Caves.Kind.RIM.code();
        }
        return Ring5Caves.Kind.NONE.code();
    }

    /** Whether a block lies in one of ring 5's own caves (93: a net's corridor or shaft, a void cave, a hole). */
    public boolean ownCave(int x, int y, int z) {
        Column column = column(x, z);
        for (int k = 0; k < column.caves; k++) {
            if (y >= column.caveBottom[k] && y < column.caveTop[k]) {
                return true;
            }
        }
        return false;
    }

    /**
     * The top (exclusive) of the rock rim round a void cave's hole (93) in this column, within 2 of a hole: the lowest
     * open block of the cave's floor, the rim the {@link #HOLE_RIM_DEPTH} blocks under it ({@link Ring5Caves.Kind#RIM}); {@link Integer#MIN_VALUE} elsewhere. For
     * the hole's voidstone rim and wall (the surface rule {@code ring_5_ground}).
     */
    public int holeRimTop(int x, int z) {
        return column(x, z).rimTop;
    }

    /**
     * Whether the column lies on the floor of one of {@code corrupted_wastes}' pits (83): its sand (walk 1 review; the
     * surface rule {@code ring_5_ground}). The pit's shape only: the biome and the band are the rule's. A doppelganger's
     * target has its source's pit, as it has its source's ground.
     */
    public boolean pitFloor(int x, int z) {
        return column(x, z).pit;
    }

    /**
     * Where vanilla's damped caves (93, V93-1 A) may cut a block: 0 not at all (closed, an exact span, a glitch field's
     * shifted layers, void), 1 only the entrances ({@link #NATURAL_COVER} under the top), 2 also the noise caves. Exact
     * shapes stay whole: a bridge, a cube or a layer has density 1 right at its visible face. The DF {@code
     * ring_5/overworld/cave_zone}.
     */
    public int caveZone(int x, int y, int z) {
        Column column = column(x, z);
        if (column.isVoid() || column.closure >= CAVES_CLOSED) {
            return 0;
        }
        for (int k = 0; k < column.spans; k++) {
            if (y >= column.spanBottom[k] && y < column.spanTop[k]) {
                return 0;
            }
        }
        double top = column.top;
        if (column.layered) {
            top -= Ring5Inner.LAYER_DEPTH;
            if (y >= top) {
                return 0;
            }
        }
        return y < top - NATURAL_COVER && y >= column.bottom + NATURAL_COVER ? 2 : 1;
    }

    /** The own caves' shapes (93), made once per world. */
    Ring5Caves caves() {
        Ring5Caves known = decisions.caveShapes;
        if (known == null) {
            known = new Ring5Caves(salt());
            decisions.caveShapes = known;
        }
        return known;
    }

    /** The doppelgangers' shapes (93), made once per world. */
    Ring5Copies copies() {
        Ring5Copies known = decisions.copyShapes;
        if (known == null) {
            known = new Ring5Copies(salt());
            decisions.copyShapes = known;
        }
        return known;
    }

    /** The corridor net of net cell (i, j) (93) if it lies wholly deep in the inner band, off spots and copies. Cached, pure. */
    @Nullable
    Ring5Caves.Net cavesNet(int i, int j) {
        long key = KeyedSlots.key(i, j);
        Optional<Ring5Caves.Net> known = decisions.nets.peek(key);
        if (known == null) {
            Ring5Caves.Net net = caves().net(i, j);
            known = Optional.ofNullable(net != null && netDeepInner(net)
                    && !nearSpotOrCopy(net.minX, net.minZ, net.maxX, net.maxZ) ? net : null);
            decisions.nets.put(key, known);
        }
        return known.orElse(null);
    }

    /** Whether the box round a net lies deep in the inner band (samples every 16 blocks and its far edges). */
    private boolean netDeepInner(Ring5Caves.Net net) {
        for (int x = net.minX; x <= net.maxX + 15; x += 16) {
            for (int z = net.minZ; z <= net.maxZ + 15; z += 16) {
                if (!deepInner(Math.min(x, net.maxX), Math.min(z, net.maxZ), SPOT_CLEARANCE, SPOT_CASTLE_CLEARANCE)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Whether a box (blocks, inclusive) comes within {@link #CAVE_CLEARANCE} of a corrupt spot's chunk or a doppelganger's
     * square (93): the own caves keep off both, as the glitch cubes do (a spot's shift or fallen chunk would cut a cave
     * open, a copy's edge would cut it off).
     */
    private boolean nearSpotOrCopy(int minX, int minZ, int maxX, int maxZ) {
        int x0 = minX - CAVE_CLEARANCE;
        int z0 = minZ - CAVE_CLEARANCE;
        int x1 = maxX + CAVE_CLEARANCE;
        int z1 = maxZ + CAVE_CLEARANCE;
        for (int i = Ring5CorruptSpots.cellOf(x0); i <= Ring5CorruptSpots.cellOf(x1); i++) {
            for (int j = Ring5CorruptSpots.cellOf(z0); j <= Ring5CorruptSpots.cellOf(z1); j++) {
                Ring5CorruptSpots.Spot spot = corruptSpot(i, j);
                if (spot != null && spot.touches(x0, z0, x1, z1)) {
                    return true;
                }
            }
        }
        for (int i = Ring5Copies.cellOf(x0); i <= Ring5Copies.cellOf(x1); i++) {
            for (int j = Ring5Copies.cellOf(z0); j <= Ring5Copies.cellOf(z1); j++) {
                Ring5Copies.Pair pair = copyPair(i, j);
                if (pair != null && pair.touches(x0, z0, x1, z1)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The void cave of chamber cell (i, j) (93) if it lies wholly deep in the inner band, off spots and copies. Cached, pure. */
    @Nullable
    Ring5Caves.Chamber chamber(int i, int j) {
        long key = KeyedSlots.key(i, j);
        Optional<Ring5Caves.Chamber> known = decisions.chambers.peek(key);
        if (known == null) {
            Ring5Caves.Chamber chamber = caves().chamber(i, j);
            if (chamber != null) {
                double reach = chamber.radius() * (1.0 + Ring5Caves.RAGGED);
                for (int k = 0; k <= 8; k++) {
                    double a = Math.PI / 4.0 * k;
                    double r = k == 8 ? 0.0 : reach;
                    if (!deepInner(chamber.x() + r * Math.cos(a), chamber.z() + r * Math.sin(a), SPOT_CLEARANCE, SPOT_CASTLE_CLEARANCE)) {
                        chamber = null;
                        break;
                    }
                }
                if (chamber != null && nearSpotOrCopy(Mth.floor(chamber.x() - reach), Mth.floor(chamber.z() - reach),
                        Mth.floor(chamber.x() + reach), Mth.floor(chamber.z() + reach))) {
                    chamber = null;
                }
            }
            known = Optional.ofNullable(chamber);
            decisions.chambers.put(key, known);
        }
        return known.orElse(null);
    }

    /**
     * The void cave of outer cell (i, j) (93, V93-8 A): only in a thick floe near the band line, where every sample round it
     * is floe ground at least {@link Ring5Caves#OUTER_MIN_THICKNESS} thick (no scar, no castle); its floor between the
     * floe's underside and top with rock to spare. Cached, pure (the samples use a column of their own, not the table).
     */
    @Nullable
    Ring5Caves.Chamber outerChamber(int i, int j) {
        long key = KeyedSlots.key(i, j);
        Optional<Ring5Caves.Chamber> known = decisions.outerChambers.peek(key);
        if (known == null) {
            known = Optional.ofNullable(placeOuterChamber(i, j));
            decisions.outerChambers.put(key, known);
        }
        return known.orElse(null);
    }

    @Nullable
    private Ring5Caves.Chamber placeOuterChamber(int i, int j) {
        Ring5Caves.Chamber candidate = caves().outerCandidate(i, j);
        if (candidate == null) {
            return null;
        }
        RingLayout l = layout.get();
        Ring5Band band = Ring5Band.of(l);
        double reach = candidate.radius() * (1.0 + Ring5Caves.RAGGED) + 2.0;
        double signed = band.signedDistance(candidate.x(), candidate.z());
        if (signed < reach + Ring5Outer.LINE_BLEND || signed > OUTER_CAVE_REACH) {
            return null;
        }
        double r5 = ring5Radius(l);
        double empty = emptyFrom(l);
        Column sample = new Column();
        double bottom = Double.NEGATIVE_INFINITY;
        double top = Double.POSITIVE_INFINITY;
        for (int k = 0; k < 17; k++) {
            double a = Math.PI / 4.0 * k;
            double r = k == 16 ? 0.0 : k < 8 ? reach : 0.5 * reach;
            int x = Mth.floor(candidate.x() + r * Math.cos(a));
            int z = Mth.floor(candidate.z() + r * Math.sin(a));
            double effective = l.effectiveDistance(x, z);
            double s = band.signedDistance(x, z);
            if (s < 0.0 || effective >= empty || nearCastle(x, z, SPOT_CASTLE_CLEARANCE)) {
                return null;
            }
            sample.reset();
            fillOuter(sample, x, z, effective, s, r5);
            if (sample.isVoid() || sample.scars > 0.0) {
                return null;
            }
            bottom = Math.max(bottom, sample.bottom);
            top = Math.min(top, sample.top);
        }
        if (top - bottom < Ring5Caves.OUTER_MIN_THICKNESS) {
            return null;
        }
        int low = (int) Math.ceil(bottom) + Ring5Caves.OUTER_FLOOR_ROCK;
        int high = (int) Math.floor(top) - Ring5Caves.OUTER_ROOF_ROCK - candidate.height();
        if (high < low) {
            return null;
        }
        return Ring5Caves.outerChamber(candidate, low, high, CellShapes.cell(salt(), i, j, OUTER_FLOOR_SALT));
    }

    /**
     * The doppelganger of copy cell (i, j) (93, V93-6 A) if both its squares lie in quiet inner band: deep inside it (as the
     * corrupt spots), no crack or spot hole within 16, no corrupt spot within 8. Cached, pure.
     */
    @Nullable
    Ring5Copies.Pair copyPair(int i, int j) {
        long key = KeyedSlots.key(i, j);
        Optional<Ring5Copies.Pair> known = decisions.pairs.peek(key);
        if (known == null) {
            Ring5Copies.Pair pair = null;
            for (int attempt = 0; attempt < Ring5Copies.ATTEMPTS && pair == null && copies().has(i, j); attempt++) {
                Ring5Copies.Pair tried = copies().pair(i, j, attempt);
                if (quietSquare(tried.sx(), tried.sz()) && quietSquare(tried.tx(), tried.tz())) {
                    pair = tried;
                }
            }
            known = Optional.ofNullable(pair);
            decisions.pairs.put(key, known);
        }
        return known.orElse(null);
    }

    /** Whether a doppelganger's square (low corner) lies in quiet inner band; samples every 8 blocks and its far edge. */
    private boolean quietSquare(int x0, int z0) {
        int last = Ring5Copies.SIZE - 1;
        for (int dx = 0; dx <= last + 7; dx += 8) {
            for (int dz = 0; dz <= last + 7; dz += 8) {
                int x = x0 + Math.min(dx, last);
                int z = z0 + Math.min(dz, last);
                Ring5CorruptSpots.Spot spot = corruptSpot(Ring5CorruptSpots.cellOf(x), Ring5CorruptSpots.cellOf(z));
                // Samples 8 apart: a column within 8 of a spot lies within 12 of a sample (spots of other cells keep 16 off).
                if (!deepInner(x, z, SPOT_CLEARANCE, SPOT_CASTLE_CLEARANCE) || innerCrackDistance(x, z, spot) <= 16.0
                        || spot != null && spot.near(x, z, 12)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** The inner band's ground at a column with the doppelgangers (93): a target shows its source's, blended at its edge. */
    private double copiedGround(int x, int z) {
        Ring5Copies.Pair pair = copyPair(Ring5Copies.cellOf(x), Ring5Copies.cellOf(z));
        if (pair == null || !pair.inTarget(x, z)) {
            return innerGround(x, z);
        }
        double source = innerGround(pair.sourceX(x, z), pair.sourceZ(x, z));
        return pair.core(x, z) ? source : Mth.lerp(pair.weight(x, z), innerGround(x, z), source);
    }

    /**
     * Whether a position lies deep in the inner band: {@code clearance} inside the band line, {@code clearance} past the
     * zone 4→5 and {@code castleClearance} off every castle's reach (the rule of the corrupt spots, 83b).
     */
    private boolean deepInner(double x, double z, double clearance, double castleClearance) {
        RingLayout l = layout.get();
        return Ring5Band.of(l).signedDistance(x, z) <= -clearance
                && l.effectiveDistance(x, z) >= Ring4Sundered.zoneEnd(l) + clearance && !nearCastle(x, z, castleClearance);
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

    /**
     * The ground's top in the outer band (a floe's, or where a floe would be): from the inner band's surface at the line,
     * held to the floes' range (83b: the inner relief bends into it towards the line, but its glitch blocks, pits and
     * stairs may still stick out a little).
     */
    private double groundTop(double x, double z, double signed, double progress) {
        double own = Ring5Outer.floeTop(progress, noises.relief.getValue(x, 0.0, z), noises.detail.getValue(x, 0.0, z));
        if (signed >= Ring5Outer.LINE_BLEND) {
            return own;
        }
        double inner = Mth.clamp(innerTop(x, z), Ring5Outer.TOP_MIN, Ring5Outer.TOP_MAX);
        return Mth.lerp(Ring4Relief.smooth(signed, 0.0, Ring5Outer.LINE_BLEND), inner, own);
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
        double landTop = signed < 0.0 ? innerGround(Mth.floor(tx), Mth.floor(tz))
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
        /** The inner band's distance to a crack or spot hole (83/83b), infinite elsewhere. */
        double crack;
        /** The own caves (93): open intervals [bottom, top) and their kind's code. */
        int caves;
        final int[] caveBottom = new int[MAX_CAVES];
        final int[] caveTop = new int[MAX_CAVES];
        final int[] caveKind = new int[MAX_CAVES];
        /** The top of a void cave's hole rim (rock, {@link Ring5Caves#RIM_DEPTH} deep), else {@link Integer#MIN_VALUE}. */
        int rimTop;
        /** 0..{@link #CAVES_CLOSED}: how far vanilla's damped caves are closed here (93; 0 open). */
        double closure;
        /** How far vanilla's damped caves keep from the band line, the zone 4→5 and a castle's reach (93); −∞ elsewhere. */
        double room;
        /** {@link #innerOpeningDistance} of the column (93), NaN until known. */
        double opening;
        /** Scratch for {@link Ring5Caves.Chamber#column} (93): no allocation per column. */
        final int[] chamberOut = new int[4];
        /** On the floor of a {@code corrupted_wastes} pit ({@link Ring5Inner#pitFloor}; a copy: its source's). */
        boolean pit;
        /** A doppelganger's copy (93): the pair, and the source column this one mirrors; null elsewhere. */
        Ring5Copies.Pair copy;
        int sourceX;
        int sourceZ;

        void reset() {
            crack = Double.POSITIVE_INFINITY;
            caves = 0;
            rimTop = Integer.MIN_VALUE;
            pit = false;
            closure = CAVES_CLOSED;
            room = Double.NEGATIVE_INFINITY;
            opening = Double.NaN;
            copy = null;
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

        void addCave(int from, int to, Ring5Caves.Kind kind) {
            for (int k = 0; k < caves; k++) {
                if (caveBottom[k] == from && caveTop[k] == to) {
                    return;
                }
            }
            if (caves < MAX_CAVES && to > from) {
                caveBottom[caves] = from;
                caveTop[caves] = to;
                caveKind[caves] = kind.code();
                caves++;
            }
        }

        /** 0..{@link #CAVES_CLOSED}: how far vanilla's damped caves are closed in this column (93). */
        public double closure() {
            return closure;
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

        /** Whether a glitch field's shifted layers apply ({@link Ring5Inner#LAYER_DEPTH} round the top; 83). */
        public boolean layered() {
            return layered;
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
        volatile Ring5CorruptSpots spotShapes;
        final KeyedSlots<Optional<Ring5CorruptSpots.Spot>> spots = new KeyedSlots<>(64);
        volatile Ring5Caves caveShapes;
        final KeyedSlots<Optional<Ring5Caves.Net>> nets = new KeyedSlots<>(64);
        final KeyedSlots<Optional<Ring5Caves.Chamber>> chambers = new KeyedSlots<>(64);
        final KeyedSlots<Optional<Ring5Caves.Chamber>> outerChambers = new KeyedSlots<>(16);
        volatile Ring5Copies copyShapes;
        final KeyedSlots<Optional<Ring5Copies.Pair>> pairs = new KeyedSlots<>(16);
    }
}
