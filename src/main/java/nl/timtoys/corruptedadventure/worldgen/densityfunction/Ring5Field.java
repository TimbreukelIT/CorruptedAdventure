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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * {@code corruptedadventure:ring_5_field}: the overworld terrain of ring 5 (package 82), on a grid of its own like ring
 * 4's {@link IslandField}: its own {@link Column}, {@link ColumnTable} and {@link Decisions}. As a density function it is
 * the break-up of a column (0 solid ground .. 1 void without any chunk); {@link Ring5Output} reads its other outputs.
 * <p>
 * Golf 0 is the skeleton: the four castle sites are real (V5-A), the terrain is a temporary shape to show them (until
 * 83/84): the placeholder surface ({@link #SURFACE_BASE} + {@link #SURFACE_NOISE} × noise) lowered by a step of
 * {@link #BAND_STEP} blocks in the outer band, per castle a pinnacle (its disc from its top down to
 * {@link #PINNACLE_BOTTOM}, void below) in a belt of void down past the world's bottom. The other accessors
 * ({@link #bridgesWithin}, {@link #fragmentsWithin}) are empty until 84.
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
            NoiseHolder.CODEC.fieldOf("placeholder").forGetter(f -> f.placeholder),
            Settings.CODEC.optionalFieldOf("settings", Settings.DEFAULT).forGetter(f -> f.settings)
    ).apply(instance, (hash, placeholder, settings) -> new Ring5Field(hash, placeholder, settings, RingLayout::current)));

    public static final KeyDispatchDataCodec<Ring5Field> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private static final long WORLD_SALT = 5005;
    /** Skeleton surface (until 83/84): the placeholder terrain's surface, Y 85 + 8 × noise (as ring 5's placeholder). */
    static final double SURFACE_BASE = 85.0;
    static final double SURFACE_NOISE = 8.0;
    /** Skeleton: the outer band lies this many blocks lower (times the band value), so the band line shows on maps. */
    static final double BAND_STEP = 6.0;
    /** Where a pinnacle's walls end (V3-A: void below). */
    static final int PINNACLE_BOTTOM = -40;
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
    /** The width of the void belt around the plateau (V5-A). */
    static final int CASTLE_MIN_BELT = 40;
    static final int CASTLE_MAX_BELT = 80;
    /** Density per block of the terrain output, as rings 3 and 4 (1/8), clamped to ±1 so the cell interpolation keeps walls in place. */
    private static final double DENSITY_PER_BLOCK = 0.125;

    private final NoiseHolder hash;
    private final NoiseHolder placeholder;
    private final Settings settings;
    private final Supplier<RingLayout> layout;
    private final ColumnTable<Column> columns = new ColumnTable<>(Column::new);
    private final Decisions decisions;

    /** @param layout the ring layout (the running server's; tests give their own) */
    Ring5Field(NoiseHolder hash, NoiseHolder placeholder, Settings settings, Supplier<RingLayout> layout) {
        this(hash, placeholder, settings, layout, new Decisions());
    }

    private Ring5Field(NoiseHolder hash, NoiseHolder placeholder, Settings settings, Supplier<RingLayout> layout, Decisions decisions) {
        this.hash = hash;
        this.placeholder = placeholder;
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
        NoiseHolder mappedPlaceholder = visitor.visitNoise(placeholder);
        // A noise chunk's copy keeps the decisions of the random state's instance (same noises, same results).
        boolean same = mappedHash.equals(hash) && mappedPlaceholder.equals(placeholder);
        return visitor.apply(new Ring5Field(mappedHash, mappedPlaceholder, settings, layout, same ? decisions : new Decisions()));
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
        return o instanceof Ring5Field f && hash.equals(f.hash) && placeholder.equals(f.placeholder) && settings.equals(f.settings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash, placeholder, settings);
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

    /** The bridges (to the castles and the narrow ridges of the outer band, V6-A) whose reach comes into the box. 84. */
    public List<Bridge> bridgesWithin(int minX, int minZ, int maxX, int maxZ) {
        return List.of();
    }

    /** The floating chunks of the outer band whose reach comes into the box. 84. */
    public List<Fragment> fragmentsWithin(int minX, int minZ, int maxX, int maxZ) {
        return List.of();
    }

    /** How far the ground of the column has fallen away: 0 solid ground, 1 void without any chunk. */
    public double breakupAt(int x, int z) {
        return column(x, z).breakup;
    }

    /** Whether the column has ground under it (not the void). Golf 0: everything but the castle belts. */
    public boolean solidGround(int x, int z) {
        return column(x, z).breakup < 1.0;
    }

    /** The preliminary surface of the column: its top, or {@link #VOID_HEIGHT} (no surface) over the void. */
    public double initialHeight(int x, int z) {
        Column column = column(x, z);
        return column.isVoid() ? VOID_HEIGHT : column.top;
    }

    /**
     * The terrain's density at a block (the skeleton until 83/84): rock between the column's bottom and top, 1/8 per
     * block away from them, held to ±1 so the noise chunk's interpolation between a pinnacle and its belt leaves the wall
     * half-way; −1 in the void.
     */
    public double terrain(int x, int y, int z) {
        Column column = column(x, z);
        if (column.isVoid()) {
            return -1.0;
        }
        double density = DENSITY_PER_BLOCK * (column.top - y);
        if (column.bottom != Double.NEGATIVE_INFINITY) {
            density = Math.min(density, DENSITY_PER_BLOCK * (y - column.bottom));
        }
        return Mth.clamp(density, -1.0, 1.0);
    }

    // ---------------------------------------------------------------- columns

    private void fill(Column column, int x, int z) {
        RingLayout ringLayout = layout.get();
        CastleSite castle = null;
        double castleDistance = Double.POSITIVE_INFINITY;
        for (CastleSite site : castleSites()) {
            double distance = site.distance(x, z);
            if (distance < castleDistance) {
                castle = site;
                castleDistance = distance;
            }
        }
        if (castle != null && castleDistance <= castle.radius()) {
            column.top = castle.top();
            column.bottom = PINNACLE_BOTTOM;
            column.breakup = 0.0;
            column.castle = 1.0;
            return;
        }
        if (castle != null && castleDistance <= castle.radius() + castle.belt()) {
            column.top = Double.NaN;
            column.bottom = Double.NaN;
            column.breakup = 1.0;
            column.castle = 1.0 - (castleDistance - castle.radius()) / castle.belt();
            return;
        }
        double band = Ring5Band.of(ringLayout).value(x, z);
        column.top = SURFACE_BASE + SURFACE_NOISE * placeholder.getValue(x, 0.0, z) - BAND_STEP * Math.max(0.0, band);
        column.bottom = Double.NEGATIVE_INFINITY;
        column.breakup = 0.0;
        column.castle = 0.0;
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

    /** The field's own grid (cells for the chunks and bridges of 84). */
    public record Settings(int cellSize) {
        public static final Settings DEFAULT = new Settings(128);

        public static final Codec<Settings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(32, 512).fieldOf("cell_size").forGetter(Settings::cellSize)
        ).apply(instance, Settings::new));
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

    /** A bridge to a castle or a narrow ridge of the outer band (84, V6-A); {@code siteIndex} −1 for a ridge. */
    public record Bridge(int siteIndex, double fromX, double fromZ, double toX, double toZ, double halfWidth, int deckY) {
    }

    /** A floating chunk of the outer band (84): centre, radius, bottom, top and tilt (degrees). */
    public record Fragment(double x, double z, double radius, int bottom, int top, double tilt) {
    }

    /** The 2D values of one column. */
    public static final class Column {
        /** The Y of the ground's surface (the block above the highest rock); NaN over the void. */
        double top;
        /** The Y under which the column is open (void); −∞ for full ground down to the bedrock; NaN over the void. */
        double bottom;
        /** 0..1: how far the ground has fallen away (1 void without any chunk). */
        double breakup;
        /** 0..1: the castle surroundings (1 on the plateau, falling to 0 across its belt). */
        double castle;

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

        public boolean isVoid() {
            return Double.isNaN(top);
        }
    }

    /**
     * Per-world decisions: pure functions of the seed and the layout, shared by all copies of one wired field (the noise
     * chunk makes a copy per chunk), so they are made once per world.
     */
    static final class Decisions {
        volatile Long salt;
        volatile List<CastleSite> sites;
    }
}
