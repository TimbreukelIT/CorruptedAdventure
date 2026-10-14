package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.ring.Ring4Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * {@code corruptedadventure:island_field}: the overworld terrain of ring 4: the base terrain ({@link Ring4Relief},
 * package 60) and the floating islands with their mirror lakes, lifts and waterfalls (61), on a grid of its own: ring
 * 3's {@link VolcanicField} caps a column at 6 islands and 8 solids, too few for ring 4's dense islands. As a density
 * function it is the island mask of a column (0..1); {@link IslandOutput} reads its other outputs (heights, the
 * per-block islands).
 * <p>
 * The biome source's height hook ({@code RingBiomeSource}) and the aquifer's water branch ({@code RingAquifer}) read it
 * through {@link #find} on the router, like {@link VolcanicField}.
 * <p>
 * Pure: a function of the seed (through the noises and {@code hash}), the ring layout and x, z. Decisions read their
 * inputs at exact positions, never through a {@code flat_cache}; the caches only remember results.
 */
public final class IslandField implements DensityFunction {
    public static final MapCodec<IslandField> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            NoiseHolder.CODEC.fieldOf("hash").forGetter(f -> f.hash),
            Ring4Relief.MAP_CODEC.fieldOf("relief").forGetter(f -> f.relief),
            Settings.CODEC.optionalFieldOf("settings", Settings.DEFAULT).forGetter(f -> f.settings),
            DensityFunction.HOLDER_HELPER_CODEC.optionalFieldOf("plateau").forGetter(f -> java.util.Optional.ofNullable(f.plateau))
    ).apply(instance, (hash, relief, settings, plateau) -> new IslandField(hash, relief, settings, plateau.orElse(null),
            RingLayout::current, new Decisions())));

    public static final KeyDispatchDataCodec<IslandField> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    /** The order of overlapping islands ({@link #uppermost}): bottom, top, then the centre as a fixed key. */
    private static final Comparator<Island> OVERLAP = Comparator.comparingInt(Island::bottom).thenComparingInt(Island::top)
            .thenComparingDouble(Island::x).thenComparingDouble(Island::z);
    private static final long WORLD_SALT = 4004;
    /** The preliminary surface: the lowest surface on rings of these radii around a quart's centre, less a margin. */
    private static final double[] INITIAL_RADII = {6.0, 12.0};
    private static final double INITIAL_MARGIN = 2.0;
    /** Islands one column may hold (overlapping footprints at different heights; more are dropped). */
    static final int MAX_ISLANDS = 8;
    /** An island reaches at most this far from its centre (a giant: 150 across, stretched 1.15, ragged). */
    static final double MAX_REACH = 110.0;
    /** The calm first share of the zone 4→5 (no cracks; sealed under the sea by the aquifer), for the fluid rules. */
    public static final double SUNDERED_CALM_SHARE = Ring4Sundered.CALM_SHARE;
    /** Recorded waterfalls per chunk at most (the cap of the water branch). */
    static final int FALLS_PER_CHUNK = 3;
    /** What the stack adds to vanilla's caves around a lake above the sea (closes them). */
    private static final double CAVE_LIFT = 2.0;
    /** How far (blocks) under a lake's deepest floor the caves stay closed. */
    private static final int CAVE_FLOOR_MARGIN = 14;

    private final NoiseHolder hash;
    private final Ring4Relief relief;
    private final Settings settings;
    /**
     * Ring 3's terrain ({@code ring_3/overworld/field}, a {@link VolcanicField}): the height of the plateau above the
     * cliff 3→4 ({@link Ring4Cliff}). Null: the plateau stands {@link #NO_PLATEAU} above the foot.
     */
    @Nullable
    private final DensityFunction plateau;
    private volatile VolcanicField ring3;
    private final Supplier<RingLayout> layout;
    private final ColumnTable<Column> columns = new ColumnTable<>(Column::new);
    private final Decisions decisions;
    // The islands of the 3 x 3 cells around a cell, for recent cells (review 61 #6: lock-free, shared by threads).
    private final KeyedSlots<List<IslandShape>> neighbourhoods = new KeyedSlots<>();
    /** Without ring 3's terrain the plateau stands this far above the foot. */
    private static final double NO_PLATEAU = 60.0;

    public IslandField(NoiseHolder hash, Ring4Relief relief, Settings settings) {
        this(hash, relief, settings, null, RingLayout::current, new Decisions());
    }

    /**
     * @param plateau ring 3's terrain for the cliff 3→4, or null
     * @param layout  the ring layout (the running server's; tests give their own)
     */
    IslandField(NoiseHolder hash, Ring4Relief relief, Settings settings, @Nullable DensityFunction plateau, Supplier<RingLayout> layout) {
        this(hash, relief, settings, plateau, layout, new Decisions());
    }

    private IslandField(NoiseHolder hash, Ring4Relief relief, Settings settings, @Nullable DensityFunction plateau,
                        Supplier<RingLayout> layout, Decisions decisions) {
        this.hash = hash;
        this.relief = relief;
        this.settings = settings;
        this.plateau = plateau;
        this.ring3 = plateau == null ? null : VolcanicField.resolveIfBound(plateau);
        this.layout = layout;
        this.decisions = decisions;
    }

    /** Ring 3's terrain for the plateau, or null. */
    @Nullable
    private VolcanicField ring3() {
        VolcanicField f = ring3;
        if (f == null && plateau != null) {
            f = VolcanicField.resolve(plateau);
            ring3 = f;
        }
        return f;
    }

    Settings settings() {
        return settings;
    }

    Ring4Relief relief() {
        return relief;
    }

    RingLayout ringLayout() {
        return layout.get();
    }

    // ---------------------------------------------------------------- density function

    @Override
    public double compute(FunctionContext context) {
        return column(context.blockX(), context.blockZ()).islandMask;
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        NoiseHolder mapped = visitor.visitNoise(hash);
        Ring4Relief mappedRelief = relief.map(visitor);
        DensityFunction mappedPlateau = plateau == null ? null : plateau.mapAll(visitor);
        // A noise chunk's copy keeps the cell decisions of the random state's instance (same noises, same results).
        boolean same = mapped.equals(hash) && mappedRelief.equals(relief) && Objects.equals(mappedPlateau, plateau);
        return visitor.apply(new IslandField(mapped, mappedRelief, settings, mappedPlateau, layout, same ? decisions : new Decisions()));
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
        return o instanceof IslandField f && hash.equals(f.hash) && relief.equals(f.relief) && settings.equals(f.settings)
                && Objects.equals(plateau, f.plateau);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash, relief, settings, plateau);
    }

    /** The first {@code island_field} anywhere inside {@code root} (e.g. a random state's final density), or null. */
    @Nullable
    public static IslandField find(DensityFunction root) {
        IslandField[] found = new IslandField[1];
        root.mapAll(new Visitor() {
            @Override
            public DensityFunction apply(DensityFunction function) {
                if (found[0] == null && function instanceof IslandField field) {
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
    static IslandField resolveIfBound(DensityFunction function) {
        try {
            return resolve(function);
        } catch (IllegalStateException unbound) {
            return null;
        }
    }

    /** {@code function} as an {@code island_field}, looking through holders, markers and ring gates; null if none. */
    @Nullable
    static IslandField resolve(DensityFunction function) {
        DensityFunction current = function;
        for (int depth = 0; depth < 16 && current != null; depth++) {
            if (current instanceof IslandField field) {
                return field;
            }
            current = RingGated.unwrap(current);
        }
        return null;
    }

    // ---------------------------------------------------------------- read access (W-biomes, B, A; read only)

    /** All 2D values of a column (cached per thread around the current chunk). */
    public Column column(int x, int z) {
        Column column = columns.get(x, z, IslandField::reset);
        ensureSurface(column);
        ensureIslands(column);
        return column;
    }

    /**
     * The island values of a column only (review 60 #5): its islands, spring, lake and fall, without the base terrain's
     * noise; the surface values ({@link Column#height}, {@link Column#surface}, …) are not filled by this call.
     */
    public Column islandColumn(int x, int z) {
        Column column = columns.get(x, z, IslandField::reset);
        ensureIslands(column);
        return column;
    }

    /** All islands whose footprint covers the column. */
    public List<Island> islandsAt(int x, int z) {
        List<Island> found = islandsWithin(x, z, x, z);
        found.removeIf(island -> !island.covers(x, z));
        return found;
    }

    /** The island of the column where islands overlap ({@link #uppermost}), or null. */
    @Nullable
    public Island islandAt(int x, int z) {
        return uppermost(islandsAt(x, z));
    }

    /**
     * The one rule for overlapping islands (review 59): the island with the highest bottom, then the highest top, then
     * the largest centre x and z, so the choice never depends on the order of the list. Null for none.
     */
    @Nullable
    public static Island uppermost(Collection<Island> islands) {
        return islands.stream().max(OVERLAP).orElse(null);
    }

    /** The islands whose footprint may reach into the box (block coordinates, inclusive). */
    public List<Island> islandsWithin(int minX, int minZ, int maxX, int maxZ) {
        List<Island> found = new ArrayList<>();
        for (IslandShape shape : shapesWithin(minX, minZ, maxX, maxZ)) {
            Island island = shape.view();
            if (island.reaches(minX, minZ, maxX, maxZ)) {
                found.add(island);
            }
        }
        return found;
    }

    /** The positions of the lifts (updrafts) in the box: beside about one in four middle and giant islands. */
    public List<Lift> liftsWithin(int minX, int minZ, int maxX, int maxZ) {
        List<Lift> found = new ArrayList<>();
        for (IslandShape shape : shapesWithin(minX, minZ, maxX, maxZ)) {
            Lift lift = details(shape).lift();
            if (lift != null && lift.x() >= minX && lift.x() <= maxX && lift.z() >= minZ && lift.z() <= maxZ) {
                found.add(lift);
            }
        }
        return found;
    }

    /**
     * The waterfalls in the box (off islands; the cliff 3→4 and the world trees follow in 62 and 74), as the terrain
     * places them: at most {@link #FALLS_PER_CHUNK} per chunk.
     */
    public List<Waterfall> waterfallsWithin(int minX, int minZ, int maxX, int maxZ) {
        List<Waterfall> found = new ArrayList<>();
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                for (Fall fall : fallsOfChunk(cx, cz)) {
                    if (fall.fallX() >= minX && fall.fallX() <= maxX && fall.fallZ() >= minZ && fall.fallZ() <= maxZ) {
                        found.add(new Waterfall(fall.fallX(), fall.fallZ(), fall.springY(), fall.bottom()));
                    }
                }
            }
        }
        return found;
    }

    /** The world trees in the box (74). */
    public List<WorldTree> worldTrees(int minX, int minZ, int maxX, int maxZ) {
        return List.of();
    }

    // ---------------------------------------------------------------- outputs ({@link IslandOutput})

    /**
     * A lower bound of the surface around a column (the lowest surface on rings of 6 and 12 blocks around the quart's
     * centre, 8 points each, less 2; the sinkholes and island holes near it counted at their deepest), for the
     * preliminary surface: vanilla's aquifer decides from it whether open space below the sea level holds the sea, so
     * it must not lie above the floor of a lake, a river or a fjord (pitfall Y 55). Remembered per world on the 4-block
     * grid.
     */
    double initialHeight(int x, int z) {
        int qx = x & ~3;
        int qz = z & ~3;
        long key = KeyedSlots.key(qx, qz);
        Double known = decisions.initialHeights.get(key);
        if (known == null) {
            known = lowestAround(qx + 2, qz + 2);
            decisions.remember(decisions.initialHeights, key, known);
        }
        return known;
    }

    private double lowestAround(int x, int z) {
        double lowest = surfaceUncached(x, z);
        for (double radius : INITIAL_RADII) {
            for (int i = 0; i < 8; i++) {
                double angle = (i + radius / 16.0) * Math.PI / 4.0;
                lowest = Math.min(lowest, surfaceUncached(x + radius * Math.cos(angle), z + radius * Math.sin(angle)));
            }
        }
        lowest = Math.min(lowest, sinkholeFloorNear(x, z));
        lowest = Math.min(lowest, holeFloorNear(x, z));
        lowest = Math.min(lowest, poolFloorNear(x, z));
        return lowest - INITIAL_MARGIN;
    }

    /** The bed of a cliff fall's plunge pool within 12 blocks of (x, z) (62), or +infinity. */
    private double poolFloorNear(int x, int z) {
        RingLayout ringLayout = layout.get();
        if (Math.abs(ringLayout.effectiveDistance(x, z) - Ring4Cliff.lineRadius(ringLayout)) > 40.0) {
            return Double.POSITIVE_INFINITY;
        }
        double reach = INITIAL_RADII[INITIAL_RADII.length - 1] + Ring4Cliff.POOL_RADIUS;
        for (Fall fall : cliffFallsNear(Ring4Cliff.arc(ringLayout, x, z))) {
            if (Math.hypot(fall.fallX() + 0.5 - x, fall.fallZ() + 0.5 - z) < reach) {
                return Ring4Cliff.POOL_BED;
            }
        }
        return Double.POSITIVE_INFINITY;
    }

    /**
     * The deepest floor of a peat sinkhole whose bowl comes within 12 blocks of (x, z) (the rings of
     * {@link #lowestAround}), or +infinity. Only sinkholes the peat really cuts count (review 60 #2): elsewhere the
     * cell's centre may lie in a trough or a fjord, which must not pull the surface down under dry land.
     */
    private double sinkholeFloorNear(int x, int z) {
        return sinkholeFloorWithin(x, z, INITIAL_RADII[INITIAL_RADII.length - 1]);
    }

    /** The deepest floor of a peat sinkhole (that the peat really cuts) whose bowl comes within {@code ring} of (x, z). */
    double sinkholeFloorWithin(double x, double z, double ring) {
        double reach = ring + Ring4Relief.SINKHOLE_MAX_RADIUS;
        int cell = Ring4Relief.SINKHOLE_CELL;
        double floor = Double.POSITIVE_INFINITY;
        for (int i = Math.floorDiv((int) (x - reach), cell); i <= Math.floorDiv((int) (x + reach), cell); i++) {
            for (int j = Math.floorDiv((int) (z - reach), cell); j <= Math.floorDiv((int) (z + reach), cell); j++) {
                double[] hole = Ring4Relief.sinkhole(i, j, salt());
                if (hole == null || Math.hypot(hole[0] - x, hole[1] - z) - hole[2] > ring) {
                    continue;
                }
                // The sinkhole is deepest at its centre.
                Ring4Relief.Sample s = new Ring4Relief.Sample();
                relief.sample(hole[0], hole[1], band(hole[0], hole[1]), s, salt());
                if (!Double.isNaN(s.sinkholeFloor)) {
                    floor = Math.min(floor, s.sinkholeFloor);
                }
            }
        }
        return floor;
    }

    /** The deepest floor of an island hole whose hole comes within 14 blocks of (x, z), or +infinity. */
    private double holeFloorNear(int x, int z) {
        double floor = Double.POSITIVE_INFINITY;
        double reach = INITIAL_RADII[INITIAL_RADII.length - 1] + 2.0;
        for (IslandShape shape : shapesNear(x, z)) {
            double holeReach = shape.reach() * IslandShape.HOLE_SCALE + reach;
            if (Math.abs(shape.x() - x) < holeReach && Math.abs(shape.z() - z) < holeReach
                    && shape.holeDistance(x, z) < 1.0 + reach / shape.holeRadius()) {
                floor = Math.min(floor, shape.deepestFloor());
            }
        }
        return floor;
    }

    /** The base terrain's surface at a position (no holes), exact. */
    double baseHeight(double x, double z) {
        Ring4Relief.Sample s = new Ring4Relief.Sample();
        relief.sample(x, z, band(x, z), s, salt());
        return s.height;
    }

    /**
     * The lowest open ground at a position: the surface with the cliff 3→4, the islands' holes and outlets, and a peat
     * sinkhole's floor (which the terrain cuts per block, review 61 #6); exact and not cached.
     */
    double surfaceUncached(double x, double z) {
        Ring4Relief.Sample s = new Ring4Relief.Sample();
        relief.sample(x, z, band(x, z), s, salt());
        double surface = withHoles(cliffed(x, z, s.height, null), x, z, shapesNear(Mth.floor(x), Mth.floor(z)), null);
        surface = rimmed(x, z, surface);
        Sunder sunder = sunder(x, z);
        if (sunder.kind() != Sunder.NONE) {
            surface = Math.min(surface, Ring4Sundered.ABYSS_FLOOR);
        }
        return Double.isNaN(s.sinkholeFloor) ? surface : Math.min(surface, s.sinkholeFloor);
    }

    // ---------------------------------------------------------------- the cliff 3→4 (62)

    /**
     * The surface of ring 4's input with the cliff 3→4 ({@link Ring4Cliff}) from the base terrain {@code base} (the
     * foot); unchanged away from the zone 3→4. {@code out} (if given) gets {cliff mask 0..1, distance beyond the line,
     * plateau, foot with its plunge pools, 1 where the wall is exact here (no gap) else 0}.
     */
    private double cliffed(double x, double z, double base, @Nullable double[] out) {
        RingLayout ringLayout = layout.get();
        double line = Ring4Cliff.lineRadius(ringLayout);
        double effective = ringLayout.effectiveDistance(x, z);
        if (out != null) {
            out[0] = 0.0;
            out[1] = Double.POSITIVE_INFINITY;
            out[4] = 0.0;
            out[5] = 0.0;
        }
        if (effective > line + Ring4Cliff.SCREE_OUT + Ring4Cliff.POOL_RADIUS + 2.0 * Ring4Cliff.JITTER
                || effective < Ring4Cliff.zoneStart(ringLayout) - 16.0) {
            return base;
        }
        double distance = effective - line + Ring4Cliff.JITTER * relief.detailAt(x, z);
        double arc = Ring4Cliff.arc(ringLayout, x, z);
        Ring4Cliff.Gap gap = Ring4Cliff.gapAt(salt(), arc, Ring4Cliff.perimeter(layout.get()));
        double plateau = plateauAt(x, z, base);
        // The foot beside the wall stays at least MIN_HEIGHT under the plateau: ring 4's own rise beyond the line (hills,
        // cascades) must not turn the wall into a slope, so the ground blends down to a foot next to it.
        double outside = base;
        if (distance >= 0.0) {
            // Held at the wall's value out to 4 blocks (the wall is measured there), then blended back to the terrain.
            double w = 1.0 - Ring4Relief.smooth(distance, 4.0, 2.0 * Ring4Cliff.WALL_BAND);
            outside = Mth.lerp(w, base, Math.min(base, plateau - Ring4Cliff.MIN_HEIGHT));
        }
        double foot = outside;
        boolean nearFall = false;
        for (Fall fall : cliffFallsNear(arc)) {
            nearFall |= Math.max(Math.abs(x - fall.springX()), Math.abs(z - fall.springZ())) <= Ring4Cliff.FALL_CLEARANCE
                    || Math.max(Math.abs(x - fall.fallX()), Math.abs(z - fall.fallZ())) <= Ring4Cliff.FALL_CLEARANCE;
            double d = Math.hypot(x - (fall.fallX() + 0.5), z - (fall.fallZ() + 0.5)) / Ring4Cliff.POOL_RADIUS;
            if (d < 1.0) {
                // A plunge pool: a bowl below the sea, so the fall lands in water.
                foot = Math.min(foot, Ring4Cliff.POOL_BED + d * d * (Math.max(base, IslandShape.SEA_LEVEL) - Ring4Cliff.POOL_BED));
            }
        }
        double surface = Ring4Cliff.surface(distance, plateau, distance < 0.0 ? base : foot, gap, arc);
        if (out != null) {
            double gapWeight = gap == null ? 0.0 : gap.weight(arc);
            double wall = 1.0 - Ring4Relief.smooth(Math.abs(distance), 2.0, 5.0);
            double scree = gap != null && !gap.ravine() && distance > -Ring4Cliff.SCREE_IN && distance < Ring4Cliff.SCREE_OUT ? 1.0 : 0.0;
            out[0] = Math.max(wall * (1.0 - gapWeight), gapWeight * scree);
            out[1] = distance;
            out[2] = plateau;
            out[3] = foot;
            out[4] = gapWeight == 0.0 ? 1.0 : 0.0;
            out[5] = nearFall ? 1.0 : 0.0;
        }
        return surface;
    }

    /** The plateau's height above the cliff at a column: ring 3's height, kept 40–80 above the foot {@code base}. */
    private double plateauAt(double x, double z, double base) {
        VolcanicField volcanic = ring3();
        // The foot is referred to the line on the same ray, so the plateau above a wall is one height along it (the
        // ground of ring 4 beside the line varies by more than the wall's 40 blocks in a few blocks).
        RingLayout ringLayout = layout.get();
        double line = Ring4Cliff.lineRadius(ringLayout);
        // Ring 3's height is read at the line too: the plateau is one height along the ray (a wall of 6 blocks has one top).
        double angle = Math.atan2(z, x);
        double lineX = line * Math.cos(angle);
        double lineZ = line * Math.sin(angle);
        double foot = baseHeight(lineX, lineZ);
        double ring3Height = volcanic == null ? foot + NO_PLATEAU
                : volcanic.column(Mth.floor(lineX), Mth.floor(lineZ)).height();
        return Ring4Cliff.plateau(ring3Height, foot);
    }

    /** The line's distance at a block column (with its wandering), for the falls. */
    private double cliffDistance(int x, int z) {
        return layout.get().effectiveDistance(x, z) - Ring4Cliff.lineRadius(layout.get()) + Ring4Cliff.JITTER * relief.detailAt(x, z);
    }

    /** The cliff's falls of the fall cells around a position along the line. */
    private List<Fall> cliffFallsNear(double arc) {
        int k = Ring4Cliff.fallCellOf(arc, Ring4Cliff.perimeter(layout.get()));
        List<Fall> found = new ArrayList<>(3);
        for (int dk = -1; dk <= 1; dk++) {
            Fall fall = cliffFall(k + dk);
            if (fall != null) {
                found.add(fall);
            }
        }
        return found;
    }

    /** The fall of fall cell {@code k} of the cliff, decided once per world, or null. */
    @Nullable
    Fall cliffFall(int k) {
        java.util.Optional<Fall> known = decisions.cliffFalls.get(k);
        if (known == null) {
            known = java.util.Optional.ofNullable(placeCliffFall(k));
            decisions.remember(decisions.cliffFalls, k, known);
        }
        return known.orElse(null);
    }

    /**
     * A steaming fall from the top of the cliff (62): at the cell's position along the line, the first column beyond the
     * line is the fall, the one inward beside it the spring (one block under the plateau's top, walled in by the
     * plateau on its other three sides); not in a gap, the fall in ring 4. It lands in its plunge pool (the sea).
     */
    @Nullable
    private Fall placeCliffFall(int k) {
        double arc = Ring4Cliff.fallArc(salt(), k, Ring4Cliff.perimeter(layout.get()));
        if (Double.isNaN(arc)) {
            return null;
        }
        RingLayout ringLayout = layout.get();
        double line = Ring4Cliff.lineRadius(ringLayout);
        double angle = arc / line;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        int[] step = Math.abs(cos) >= Math.abs(sin) ? new int[]{cos > 0 ? 1 : -1, 0} : new int[]{0, sin > 0 ? 1 : -1};
        // The radius on the ray where the effective distance reaches the line (the boundary noise shifts it).
        double r = line;
        for (int n = 0; n < 4; n++) {
            r += line - ringLayout.effectiveDistance(r * cos, r * sin);
        }
        for (int out = -12; out <= 12; out++) {
            int fx = Mth.floor((r + out) * cos);
            int fz = Mth.floor((r + out) * sin);
            int sx = fx - step[0];
            int sz = fz - step[1];
            if (cliffDistance(fx, fz) < 0.0 || cliffDistance(sx, sz) >= 0.0) {
                continue;
            }
            if (Ring4Cliff.gapAt(salt(), Ring4Cliff.arc(ringLayout, fx, fz), Ring4Cliff.perimeter(ringLayout)) != null
                    || Ring4Cliff.gapAt(salt(), Ring4Cliff.arc(ringLayout, sx, sz), Ring4Cliff.perimeter(ringLayout)) != null
                    || ringLayout.sampleOverworld(fx, fz).ring() != nl.timtoys.corruptedadventure.ring.Ring.RING_4) {
                return null;
            }
            double plateau = plateauAt(sx, sz, baseHeight(sx, sz));
            int springY = Mth.ceil(plateau) - 2;
            int[][] walls = {{-step[0], -step[1]}, {step[1], step[0]}, {-step[1], -step[0]}};
            for (int[] wall : walls) {
                int wx = sx + wall[0];
                int wz = sz + wall[1];
                if (cliffDistance(wx, wz) >= 0.0 || Mth.ceil(plateauAt(wx, wz, baseHeight(wx, wz))) - 1 < springY) {
                    return null;
                }
            }
            if (springY <= (int) IslandShape.SEA_LEVEL + 8) {
                return null;
            }
            return new Fall(sx, sz, springY, fx, fz, (int) IslandShape.SEA_LEVEL);
        }
        return null;
    }

    /**
     * The surface {@code base} with the holes and outlets of {@code shapes} cut into it; {@code hole} (if given) gets
     * the island-hole mask (0..1, 1 well inside a hole).
     */
    private static double withHoles(double base, double x, double z, List<IslandShape> shapes, @Nullable double[] hole) {
        double surface = base;
        for (IslandShape shape : shapes) {
            double reach = shape.groundReach();
            if (Math.abs(x - shape.x()) > reach || Math.abs(z - shape.z()) > reach) {
                continue;
            }
            double floor = shape.holeFloor(x, z);
            if (!Double.isNaN(floor)) {
                surface = Math.min(surface, floor);
                if (hole != null) {
                    hole[0] = Math.max(hole[0], Mth.clamp((1.0 - shape.holeDistance(x, z)) * 4.0, 0.0, 1.0));
                }
            }
            surface = Math.min(surface, shape.outletFloor(x, z));
        }
        return surface;
    }

    /**
     * Per block: +1 inside an island, −1 in a carved opening (a spring, a peat sinkhole's bowl), 0 elsewhere (the
     * terrain decides). The base terrain is only computed for columns inside a sinkhole's disc.
     */
    double thin(int x, int y, int z) {
        Column c = columns.get(x, z, IslandField::reset);
        ensureIslands(c);
        double thin = c.thin(y);
        if (thin == 0.0 && c.sinkholeDisc) {
            ensureSurface(c);
            if (!Double.isNaN(c.sinkholeFloor) && y >= Mth.ceil(c.sinkholeFloor) && y <= c.height + 2.0) {
                return -1.0;
            }
        }
        if (thin == 0.0 && c.wallNear) {
            ensureSurface(c);
            if (c.wallExact && Math.abs(c.wallDistance) < Ring4Cliff.WALL_BAND) {
                return wall(c, x, y, z);
            }
        }
        return thin;
    }

    /**
     * The cliff 3→4 (62), exact per block within {@link Ring4Cliff#WALL_BAND} of its line, whatever the 4-block grid
     * blends: rock up to the plateau on the inside, open from the foot up on the outside. 5a: not a straight, flat wall:
     * the face moves in and out with a 3D roughness (strata as ledges and overhangs, knobs and clefts; at most
     * {@link Ring4Cliff#ROUGHNESS}, so the band's edges stay the plateau and the foot), and the top edge crumbles down
     * near the line. Beside a fall it stays smooth: the spring needs its walls.
     */
    private double wall(Column c, int x, int y, int z) {
        int plateauTop = Mth.ceil(c.plateauY) - 1;
        if (y > plateauTop + 2) {
            return 0.0;
        }
        double face = c.wallDistance;
        int top = plateauTop;
        if (!c.nearFall) {
            face += Mth.clamp(Ring4Cliff.ROUGHNESS * relief.rough(x, y, z), -Ring4Cliff.ROUGHNESS, Ring4Cliff.ROUGHNESS);
            // The top crumbles where the edge is: fully at the line, not at all from 4 blocks inside.
            double edge = 1.0 - Ring4Relief.smooth(-c.wallDistance, 1.0, 4.0);
            top -= (int) Math.round(Ring4Cliff.CRUMBLE * edge * Math.max(0.0, relief.crumble(x, z)));
        }
        double floor = c.wallDistance < 0.0 ? Math.min(c.footY, c.plateauY - Ring4Cliff.MIN_HEIGHT) : c.surface;
        if (face < 0.0) {
            // Rock, up to the (crumbled) top; above it open up to the plateau's own top.
            if (y <= top) {
                return y >= Mth.floor(floor) - 2 ? 1.0 : 0.0;
            }
            return -1.0;
        }
        return y >= Mth.ceil(floor) ? -1.0 : 0.0;
    }

    /** Per block: what the stack adds to the density of vanilla's caves: closed under and beside a lake above the sea. */
    double caveLift(int x, int y, int z) {
        Column c = columns.get(x, z, IslandField::reset);
        ensureIslands(c);
        return y >= c.caveLiftFrom && y <= c.caveLiftTo ? CAVE_LIFT : 0.0;
    }

    // ---------------------------------------------------------------- cells

    /** A pure per-world value for the hash grids, from the hash noise. */
    long salt() {
        Long known = decisions.salt;
        if (known == null) {
            // Pure: racing threads compute the same value.
            known = SeededHash.hash(hash, 4321, -8765, WORLD_SALT);
            decisions.salt = known;
        }
        return known;
    }

    NoiseHolder hashNoise() {
        return hash;
    }

    private double band(double x, double z) {
        return Ring4Band.of(layout.get()).value(x, z);
    }

    /** The candidates of a grid cell, decided once per world ({@link IslandPlacement#candidates}). */
    List<IslandShape> candidates(int i, int j) {
        return decisions.once(decisions.candidates, KeyedSlots.key(i, j), () -> IslandPlacement.candidates(this, i, j));
    }

    /** The islands of a grid cell, decided once per world ({@link IslandPlacement#place}). */
    List<IslandShape> cell(int i, int j) {
        return decisions.once(decisions.cells, KeyedSlots.key(i, j), () -> IslandPlacement.place(this, i, j));
    }

    /** Whether a candidate stays ({@link IslandPlacement#kept}), decided once per world. */
    boolean kept(IslandShape shape) {
        return decisions.once(decisions.kept, shape, () -> IslandPlacement.kept(this, shape));
    }

    /** The islands of the 3 x 3 cells around the cell of (x, z): all that can reach the column (review 59 #6). */
    List<IslandShape> shapesNear(int x, int z) {
        int cell = settings.cellSize();
        int i = Math.floorDiv(x, cell);
        int j = Math.floorDiv(z, cell);
        return neighbourhoods.get(KeyedSlots.key(i, j), key -> {
            List<IslandShape> all = new ArrayList<>();
            for (int di = -1; di <= 1; di++) {
                for (int dj = -1; dj <= 1; dj++) {
                    all.addAll(cell(i + di, j + dj));
                }
            }
            return List.copyOf(all);
        });
    }

    /** The islands of the cells that can reach into the box. */
    List<IslandShape> shapesWithin(int minX, int minZ, int maxX, int maxZ) {
        int cell = settings.cellSize();
        List<IslandShape> found = new ArrayList<>();
        for (int i = Math.floorDiv(minX, cell) - 1; i <= Math.floorDiv(maxX, cell) + 1; i++) {
            for (int j = Math.floorDiv(minZ, cell) - 1; j <= Math.floorDiv(maxZ, cell) + 1; j++) {
                found.addAll(cell(i, j));
            }
        }
        return found;
    }

    /** The lake, falls and lift of an island, decided once per world ({@link IslandPlacement#details}). */
    Details details(IslandShape shape) {
        return decisions.once(decisions.details, shape, () -> IslandPlacement.details(this, shape));
    }

    /**
     * The falls whose falling column lies in chunk (cx, cz), at most {@link #FALLS_PER_CHUNK}: the first ones by x,
     * then z, so the cap does not depend on the order chunks are made in.
     */
    List<Fall> fallsOfChunk(int cx, int cz) {
        long key = KeyedSlots.key(cx, cz);
        List<Fall> known = decisions.chunkFalls.get(key);
        if (known == null) {
            int minX = cx << 4;
            int minZ = cz << 4;
            List<Fall> falls = new ArrayList<>();
            for (IslandShape shape : shapesWithin(minX, minZ, minX + 15, minZ + 15)) {
                if (Math.abs(shape.x() - (minX + 8)) > shape.reach() + 10 || Math.abs(shape.z() - (minZ + 8)) > shape.reach() + 10) {
                    continue;
                }
                for (Fall fall : details(shape).falls()) {
                    if (fall.fallX() >> 4 == cx && fall.fallZ() >> 4 == cz) {
                        falls.add(fall);
                    }
                }
            }
            // The cliff 3→4 (62): the falls of the fall cells along the chunk, where the chunk lies near the line.
            RingLayout ringLayout = layout.get();
            if (Math.abs(ringLayout.effectiveDistance(minX + 8, minZ + 8) - Ring4Cliff.lineRadius(ringLayout)) < 40.0) {
                // The fall cells around each corner (also across the seam of the angle, −π | π).
                java.util.Set<Integer> cells = new java.util.TreeSet<>();
                for (int corner = 0; corner < 4; corner++) {
                    double arc = Ring4Cliff.arc(ringLayout, minX + 16 * (corner & 1), minZ + 16 * (corner >> 1));
                    int k = Ring4Cliff.fallCellOf(arc, Ring4Cliff.perimeter(ringLayout));
                    cells.add(k - 1);
                    cells.add(k);
                    cells.add(k + 1);
                }
                for (int k : cells) {
                    Fall fall = cliffFall(k);
                    if (fall != null && fall.fallX() >> 4 == cx && fall.fallZ() >> 4 == cz) {
                        falls.add(fall);
                    }
                }
            }
            falls.sort(Comparator.comparingInt(Fall::fallX).thenComparingInt(Fall::fallZ).thenComparingInt(Fall::springY));
            known = List.copyOf(falls.subList(0, Math.min(FALLS_PER_CHUNK, falls.size())));
            decisions.remember(decisions.chunkFalls, key, known);
        }
        return known;
    }

    private static void reset(Column column, int x, int z) {
        column.reset(x, z);
    }

    /** Computes the base terrain and the surface (with the holes) of a column once. */
    private void ensureSurface(Column c) {
        if (c.surfaceFilled) {
            return;
        }
        Ring4Relief.Sample s = new Ring4Relief.Sample();
        relief.sample(c.x, c.z, band(c.x, c.z), s, salt());
        double[] cliff = new double[6];
        c.height = cliffed(c.x, c.z, s.height, cliff);
        c.cliff = cliff[0];
        c.wallDistance = cliff[1];
        c.plateauY = cliff[2];
        c.footY = cliff[3];
        c.wallExact = cliff[4] > 0.0;
        c.nearFall = cliff[5] > 0.0;
        c.river = s.river;
        c.fjord = s.fjord;
        c.sinkholeFloor = s.sinkholeFloor;
        double[] hole = {0.0};
        c.surface = withHoles(c.height, c.x, c.z, shapesNear(c.x, c.z), hole);
        c.islandHole = hole[0];
        // The sundered zone 4→5 (63): cracks drop the ground into the abyss, loose chunks float above it.
        c.surface = rimmed(c.x, c.z, c.surface);
        Sunder sunder = sunder(c.x, c.z);
        c.sundered = sunder.strength();
        c.sunderKind = sunder.kind();
        if (sunder.kind() == Sunder.CRACK) {
            c.surface = Math.min(c.surface, Ring4Sundered.ABYSS_FLOOR);
        } else if (sunder.kind() == Sunder.LOOSE) {
            // A loose chunk: no ground under it, its own thickness floats at its offset above the ground it was.
            // Not flat: the detail noise lifts the top a few blocks and thickens or thins the slab (review 63: blocky).
            double detail = relief.detailAt(c.x, c.z);
            // 5a: rounded, not a slab: thin at its outline, full thickness LOOSE_TAPER blocks in, the top dipping a little
            // towards the rim.
            double taper = Ring4Relief.smooth(sunder.inside(), 0.0, Ring4Sundered.LOOSE_TAPER);
            double top = c.surface + sunder.offset() + 3.0 * detail - 2.0 * (1.0 - taper);
            double thickness = sunder.thickness() * (1.0 + 0.4 * detail) * (0.3 + 0.7 * Math.sqrt(taper));
            c.addIsland((int) Math.ceil(top - thickness), (int) Math.floor(top));
            c.surface = Math.min(c.surface, Ring4Sundered.ABYSS_FLOOR);
        }
        c.surfaceFilled = true;
    }

    /** The ground at least {@link Ring4Sundered#RIM_TOP} in the zone 4→5 and the rim before it (5a), else unchanged. */
    private double rimmed(double x, double z, double surface) {
        RingLayout ringLayout = layout.get();
        return Ring4Sundered.inRim(ringLayout, ringLayout.effectiveDistance(x, z)) ? Math.max(surface, Ring4Sundered.RIM_TOP) : surface;
    }

    /** How a column lies in the sundered zone (63): its kind, the loose chunk's offset and thickness, the strength. */
    private record Sunder(int kind, double offset, double thickness, double strength, double inside) {
        static final int NONE = 0;
        static final int CRACK = 1;
        static final int LOOSE = 2;
    }

    /** The sundering of a column (63); {@link Sunder#NONE} outside the zone 4→5, on a bridge, and in ground. */
    private Sunder sunder(double x, double z) {
        RingLayout ringLayout = layout.get();
        double effective = ringLayout.effectiveDistance(x, z);
        if (effective < Ring4Sundered.zoneStart(ringLayout) || effective > Ring4Sundered.zoneEnd(ringLayout)) {
            return new Sunder(Sunder.NONE, 0.0, 0.0, 0.0, 0.0);
        }
        double strength = Ring4Sundered.strength(ringLayout, effective);
        if (strength <= 0.0 || Ring4Sundered.calm(ringLayout, effective)) {
            return new Sunder(Sunder.NONE, 0.0, 0.0, strength, 0.0);
        }
        long salt = salt();
        double along = Ring4Sundered.along(ringLayout, x, z);
        if (Ring4Sundered.onBridge(salt, along, Ring4Sundered.perimeter(ringLayout))) {
            return new Sunder(Sunder.NONE, 0.0, 0.0, strength, 0.0);
        }
        // 5a: the grid is bent first, so the cracks curve.
        double s = Ring4Sundered.WARP_SCALE;
        double wx = x + Ring4Sundered.WARP * relief.warpAt(x * s, z * s);
        double wz = z + Ring4Sundered.WARP * relief.warpAt(z * s + 5000.5, x * s - 5000.5);
        double[] chunk = Ring4Sundered.chunk(salt, wx, wz);
        int i = (int) chunk[0];
        int j = (int) chunk[1];
        double crack = Ring4Sundered.crack(strength);
        double[] loose = Ring4Sundered.loose(salt, i, j, strength);
        if (loose != null) {
            // A loose chunk with a ragged, rounded outline inside its cell; the rest of the cell is abyss.
            double outline = crack + Ring4Sundered.LOOSE_MARGIN + Ring4Sundered.RAG * relief.crumble(wx * 0.5, wz * 0.5);
            if (chunk[2] < outline) {
                return new Sunder(Sunder.CRACK, 0.0, 0.0, strength, 0.0);
            }
            return new Sunder(Sunder.LOOSE, loose[0], loose[1], strength, chunk[2] - outline);
        }
        if (chunk[2] < crack) {
            return new Sunder(Sunder.CRACK, 0.0, 0.0, strength, 0.0);
        }
        return new Sunder(Sunder.NONE, 0.0, 0.0, strength, 0.0);
    }

    /**
     * Computes the islands of a column once: their solid blocks, a spring, the lake above the sea (and the band kept
     * free of caves), the recorded fall through the column.
     */
    private void ensureIslands(Column c) {
        if (c.islandsFilled) {
            return;
        }
        // The loose chunks' solids come with the surface, so they are added before the islands' own.
        ensureSurface(c);
        c.islandsFilled = true;
        double[] sinkhole = Ring4Relief.sinkhole(Math.floorDiv(c.x, Ring4Relief.SINKHOLE_CELL),
                Math.floorDiv(c.z, Ring4Relief.SINKHOLE_CELL), salt());
        c.sinkholeDisc = sinkhole != null && Math.hypot(c.x - sinkhole[0], c.z - sinkhole[1]) < sinkhole[2];
        RingLayout ringLayout = layout.get();
        double line = Ring4Cliff.lineRadius(ringLayout);
        c.wallNear = Math.abs(ringLayout.effectiveDistance(c.x, c.z) - line) < Ring4Cliff.WALL_BAND + Ring4Cliff.JITTER + 2.0;
        if (c.wallNear) {
            for (Fall fall : cliffFallsNear(Ring4Cliff.arc(ringLayout, c.x, c.z))) {
                if (fall.springX() == c.x && fall.springZ() == c.z && fallsOfChunk(fall.fallX() >> 4, fall.fallZ() >> 4).contains(fall)) {
                    c.springY = fall.springY();
                }
            }
        }
        double px = c.x + 0.5;
        double pz = c.z + 0.5;
        // Review 61 #3: a column inside a hole takes that hole's lake (the lowest, where holes overlap; a hole the sea
        // fills has none), a column only in lake bands the lowest of those lakes, so a higher lake never stands in a
        // lower hole.
        boolean inHole = false;
        int holeLake = Integer.MAX_VALUE;
        int bandLake = Integer.MAX_VALUE;
        for (IslandShape shape : shapesNear(c.x, c.z)) {
            double reach = shape.groundReach();
            if (Math.abs(px - shape.x()) > reach || Math.abs(pz - shape.z()) > reach) {
                continue;
            }
            double d = shape.distance(px, pz);
            if (d < 1.0) {
                int[] interval = shape.interval(px, pz);
                if (interval != null) {
                    c.addIsland(interval[0], interval[1]);
                    c.islandMask = Math.max(c.islandMask, Mth.clamp((1.0 - d) * 4.0, 0.0, 1.0));
                }
            }
            // The hole and its lake on block positions, like the surface.
            double dh = shape.holeDistance(c.x, c.z);
            double radius = shape.holeRadius();
            if (dh >= 1.0 + IslandShape.CAVE_BAND / radius) {
                continue;
            }
            Details details = details(shape);
            if (dh < 1.0) {
                inHole = true;
                holeLake = Math.min(holeLake, details.lakeTop());
            } else if (details.lakeTop() != Integer.MIN_VALUE && dh < 1.0 + IslandShape.LAKE_BAND / radius) {
                bandLake = Math.min(bandLake, details.lakeTop());
            }
            if (details.lakeTop() != Integer.MIN_VALUE) {
                c.caveLiftFrom = Math.min(c.caveLiftFrom, details.lakeBottom() - CAVE_FLOOR_MARGIN);
                c.caveLiftTo = Math.max(c.caveLiftTo, details.lakeTop() + 2);
            }
            for (Fall fall : details.falls()) {
                if (fall.springX() == c.x && fall.springZ() == c.z && fallsOfChunk(fall.fallX() >> 4, fall.fallZ() >> 4).contains(fall)) {
                    c.springY = fall.springY();
                }
            }
        }
        int lake = inHole ? holeLake : bandLake;
        c.lakeTop = lake == Integer.MAX_VALUE ? Integer.MIN_VALUE : lake;
        for (Fall fall : fallsOfChunk(c.x >> 4, c.z >> 4)) {
            if (fall.fallX() == c.x && fall.fallZ() == c.z && fall.springY() > c.fallTop) {
                c.fallTop = fall.springY();
                c.fallBottom = fall.bottom();
                // Only the block right above the water flows (review 61 #1).
                c.fallFlowBelow = fall.bottom();
            }
        }
    }

    // ---------------------------------------------------------------- data

    /**
     * The tunable numbers of the field. Every field is optional in JSON; missing ones take {@link #DEFAULT}.
     *
     * @param cellSize cell of the island grid (an island's radius never exceeds it)
     */
    public record Settings(int cellSize) {
        public static final Settings DEFAULT = new Settings(128);

        public static final Codec<Settings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(32, 1024).optionalFieldOf("cell_size", DEFAULT.cellSize).forGetter(Settings::cellSize)
        ).apply(instance, Settings::new));
    }

    /**
     * A floating island: centre, footprint radius (blocks) and its lowest and highest block.
     */
    public record Island(double x, double z, double radius, int bottom, int top) {
        public boolean covers(int blockX, int blockZ) {
            double dx = blockX - x;
            double dz = blockZ - z;
            return dx * dx + dz * dz <= radius * radius;
        }

        boolean reaches(int minX, int minZ, int maxX, int maxZ) {
            return x + radius >= minX && x - radius <= maxX && z + radius >= minZ && z - radius <= maxZ;
        }
    }

    /** A lift (updraft) from {@code bottom} (the water's top block) up to {@code top} in one column. */
    public record Lift(int x, int z, int bottom, int top) {
    }

    /** A recorded waterfall: falling water in one column from {@code top} down to {@code bottom}. */
    public record Waterfall(int x, int z, int top, int bottom) {
    }

    /** A world tree standing on the ground at {@code baseY}, its crown reaching {@code top}. */
    public record WorldTree(int x, int z, int baseY, int top) {
    }

    /**
     * An island's waterfall: the spring (a source that flows) at ({@code springX}, {@code springY}, {@code springZ}),
     * and the falling water beside it in column ({@code fallX}, {@code fallZ}) from {@code springY} down to
     * {@code bottom} (the first block above the water it falls into).
     */
    record Fall(int springX, int springZ, int springY, int fallX, int fallZ, int bottom) {
    }

    /**
     * What belongs to one island (stage 2, {@link IslandPlacement#details}): the top (exclusive) of its own lake above
     * the sea ({@link Integer#MIN_VALUE}: none, the sea fills the hole), its hole's deepest floor, its falls and lift.
     */
    record Details(int lakeTop, int lakeBottom, List<Fall> falls, @Nullable Lift lift) {
    }

    /** All 2D values of one column. Heights in blocks. */
    public static final class Column {
        boolean surfaceFilled;
        boolean islandsFilled;
        /** The base terrain's surface (without the islands' holes). */
        double height;
        /** The surface with the holes. */
        double surface;
        /** 0..1: a river's channel. */
        double river;
        /** 0..1: a fjord's channel. */
        double fjord;
        /** 0..1: an island in this column. */
        double islandMask;
        /** 0..1: how far the ground is sundered here (63; 0 outside the zone 4→5). */
        double sundered;
        /** {@link Ring4Sundered} kind of the column: {@code Sunder.NONE}, {@code CRACK} or {@code LOOSE}. */
        int sunderKind;
        /** 0..1: an island's mirror lake (1 well inside the hole). */
        double islandHole;
        /** Whether the column lies in the disc of a peat sinkhole's cell (then the base terrain decides its floor). */
        boolean sinkholeDisc;
        /** 0..1: the cliff 3→4's wall or scree slope (62). */
        double cliff;
        /** Near the cliff's line (then {@link #wallDistance} and the rest are filled with the surface). */
        boolean wallNear;
        /** Blocks beyond the cliff's line (inside negative), +infinity away from it. */
        double wallDistance;
        /** The plateau's top above the cliff and the foot under it (with its plunge pools). */
        double plateauY;
        double footY;
        /** Whether the wall stands exact here (no ravine or scree slope). */
        boolean wallExact;
        /** Whether a cliff fall's spring or fall column lies within {@link Ring4Cliff#FALL_CLEARANCE} (the wall stays smooth). */
        boolean nearFall;
        /** The floor of a peat sinkhole, cut per block ({@link #thin}); NaN outside one. */
        double sinkholeFloor;
        /** The islands' solid blocks: {@link #islandCount} ranges [{@code islandLo}, {@code islandHi}] (inclusive). */
        int islandCount;
        final int[] islandLo = new int[MAX_ISLANDS];
        final int[] islandHi = new int[MAX_ISLANDS];
        /** A spring's block (open, filled with flowing water), else {@link Integer#MIN_VALUE}. */
        int springY;
        /** The top (exclusive) of a lake above the sea in this column, else {@link Integer#MIN_VALUE}. */
        int lakeTop;
        /** The blocks where vanilla's caves are closed (around a lake above the sea); none if from > to. */
        int caveLiftFrom;
        int caveLiftTo;
        /**
         * A recorded waterfall through this column: falling water in open space from {@link #fallBottom} up to and
         * including {@link #fallTop} (both {@link Integer#MIN_VALUE} without one); the aquifer lets the lowest blocks,
         * at and below {@link #fallFlowBelow}, flow so the water spreads where it lands.
         */
        int fallTop;
        int fallBottom;
        int fallFlowBelow;
        int x;
        int z;

        void reset(int x, int z) {
            this.x = x;
            this.z = z;
            surfaceFilled = false;
            islandsFilled = false;
            height = 0.0;
            surface = 0.0;
            river = 0.0;
            fjord = 0.0;
            islandMask = 0.0;
            islandHole = 0.0;
            sinkholeDisc = false;
            sinkholeFloor = Double.NaN;
            sundered = 0.0;
            sunderKind = Sunder.NONE;
            cliff = 0.0;
            wallNear = false;
            wallDistance = Double.POSITIVE_INFINITY;
            plateauY = 0.0;
            footY = 0.0;
            wallExact = false;
            nearFall = false;
            islandCount = 0;
            springY = Integer.MIN_VALUE;
            lakeTop = Integer.MIN_VALUE;
            caveLiftFrom = Integer.MAX_VALUE;
            caveLiftTo = Integer.MIN_VALUE;
            fallTop = Integer.MIN_VALUE;
            fallBottom = Integer.MIN_VALUE;
            fallFlowBelow = Integer.MIN_VALUE;
        }

        void addIsland(int lo, int hi) {
            if (islandCount < MAX_ISLANDS) {
                islandLo[islandCount] = lo;
                islandHi[islandCount] = hi;
                islandCount++;
            }
        }

        /** +1 inside an island, −1 in a spring's block, 0 elsewhere. */
        double thin(int y) {
            if (y == springY) {
                return -1.0;
            }
            for (int k = 0; k < islandCount; k++) {
                if (y >= islandLo[k] && y <= islandHi[k]) {
                    return 1.0;
                }
            }
            return 0.0;
        }

        public double height() {
            return height;
        }

        public double surface() {
            return surface;
        }

        public double islandMask() {
            return islandMask;
        }

        public double islandHole() {
            return islandHole;
        }

        public double sinkholeFloor() {
            return sinkholeFloor;
        }

        /** The lowest open ground of the column: {@link #surface}, or a peat sinkhole's floor below it. */
        public double lowestSurface() {
            return Double.isNaN(sinkholeFloor) ? surface : Math.min(surface, sinkholeFloor);
        }

        public double cliff() {
            return cliff;
        }

        public double sundered() {
            return sundered;
        }

        /** The top (exclusive) of the still sea of the abyss in this column (Y 0), else {@link Integer#MIN_VALUE}. */
        public int abyssSeaTop() {
            return sunderKind == Sunder.NONE ? Integer.MIN_VALUE : Ring4Sundered.SEA_TOP + 1;
        }

        /** The lowest block of the open abyss in this column (its floor), else {@link Integer#MAX_VALUE}. */
        public int abyssFloor() {
            return sunderKind == Sunder.NONE ? Integer.MAX_VALUE : (int) Math.floor(Ring4Sundered.ABYSS_FLOOR);
        }

        public int springY() {
            return springY;
        }

        public int lakeTop() {
            return lakeTop;
        }

        public int caveLiftFrom() {
            return caveLiftFrom;
        }

        public int caveLiftTo() {
            return caveLiftTo;
        }

        public int fallTop() {
            return fallTop;
        }

        public int fallBottom() {
            return fallBottom;
        }

        public int fallFlowBelow() {
            return fallFlowBelow;
        }
    }

    /**
     * Per-world decisions and remembered results: pure functions of the seed and the cell, shared by all copies of one
     * wired field (the noise chunk makes a copy per chunk), so they are made once per world. Only a cache; a map is
     * cleared when it grows past {@link #LIMIT} entries.
     */
    static final class Decisions {
        static final int LIMIT = 100_000;
        final Map<Long, List<IslandShape>> candidates = new ConcurrentHashMap<>();
        final Map<IslandShape, Boolean> kept = new ConcurrentHashMap<>();
        final Map<Integer, java.util.Optional<Fall>> cliffFalls = new ConcurrentHashMap<>();
        final Map<Long, List<IslandShape>> cells = new ConcurrentHashMap<>();
        final Map<IslandShape, Details> details = new ConcurrentHashMap<>();
        final Map<Long, List<Fall>> chunkFalls = new ConcurrentHashMap<>();
        final Map<Long, Double> initialHeights = new ConcurrentHashMap<>();
        volatile Long salt;

        <K, V> void remember(Map<K, V> map, K key, V value) {
            if (map.size() > LIMIT) {
                map.clear();
            }
            map.putIfAbsent(key, value);
        }

        /**
         * The remembered value of {@code key}, or {@code decide}'s (pure) value, remembered. Not {@code computeIfAbsent}:
         * a decision may ask other decisions of the same map.
         */
        <K, V> V once(Map<K, V> map, K key, java.util.function.Supplier<V> decide) {
            V known = map.get(key);
            if (known == null) {
                known = decide.get();
                remember(map, key, known);
            }
            return known;
        }
    }
}
