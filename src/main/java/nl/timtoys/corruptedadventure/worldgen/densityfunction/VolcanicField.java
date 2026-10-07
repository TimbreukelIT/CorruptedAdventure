package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.ring.RingLayout;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * {@code corruptedadventure:volcanic_field}: the ring 3 overworld terrain as 2D shapes plus two thin 3D layers. As a
 * density function it is the surface height of the ring 3 core (without the blend into the zone 2→3, which the JSON
 * does); {@link VolcanicOutput} reads its other outputs (masks, lava, islands, cave guard).
 * <p>
 * Shapes, from the bottom up:
 * <ul>
 *   <li>the ash plain: broad swells and rolling waves around {@code plain_y}; flatter in the sulfur band, stepped flow
 *   lobes in {@code basalt_flows}, low parallel ridges in the titan fields, a terraced bowl in the seabed;</li>
 *   <li>volcanoes on a jittered grid ({@link Volcano}): great horizon volcanoes, ordinary cones and low calderas, each
 *   with a crater that is active (lava lake) or dormant (flat floor, no lava). Most stand where the region axes give
 *   {@code volcanic_peaks}; a band of mostly great ones stands just behind the zone 2→3, visible from {@code dry_riverbeds};</li>
 *   <li>lava channels: a noise network cut into the plain, faded out on volcano flanks, in the seabed and titan fields;</li>
 *   <li>floating islands in clusters above low active craters and above channels.</li>
 * </ul>
 * Lava lies in the open space below a column's lava top ({@link Column#lavaTop}, filled by the aquifer of
 * {@code RingChunkGenerator}); a cave guard band keeps caves out of the rock around lakes and channels.
 * <p>
 * Pure: a function of the seed (through the noises), the ring layout and x, z. The caches only remember results.
 * Decisions per volcano read the noises at exact positions, never a {@code flat_cache} (whose value inside a noise
 * chunk is the 4-block grid value), so every chunk sees the same volcanoes.
 */
public final class VolcanicField implements DensityFunction {
    private static final long CELL_SALT = 31;
    private static final long ISLAND_CELL_SALT = 37;
    private static final int CELL_MARGIN = 16;
    /** Largest foot radius as a share of the cell: 0.8 leaves room for the warp of the outline (0.08 * radius * noise). */
    private static final double MAX_RADIUS_SHARE = 0.8;
    private static final double MIN_CONE_LIFT = 40.0;
    private static final double MIN_FLANK = 30.0;
    /** Islands above channels: cell of their grid (a cell carries at most one cluster). */
    private static final int ISLAND_CELL = 960;
    private static final int ISLAND_CANDIDATES = 24;
    private static final double CORE_FADE = 160.0;
    private static final double WARP_OFFSET = 5000.5;
    /** Effective distance behind the zone 2→3 where the horizon band of great volcanoes stands. */
    private static final double BAND_START = 60.0;
    private static final double BAND_END = 480.0;
    /** Depth of a lava channel's bed below its lava level, and the least height of the plain above that level. */
    private static final double CHANNEL_DEPTH = 3.0;
    private static final double CHANNEL_BANK = 3.0;
    private static final double HILL_HEIGHT = 24.0;
    private static final double RIB_COS = Math.cos(0.6);
    private static final double RIB_SIN = Math.sin(0.6);
    private static final double RIDGE_HEIGHT = 5.0;
    private static final int MAX_ISLANDS = 6;
    private static final double ISLAND_CLEARANCE = 10.0;
    private static final double[] INITIAL_RADII = {6.0, 12.0, 18.0};

    public static final MapCodec<VolcanicField> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            NoiseHolder.CODEC.fieldOf("hash").forGetter(f -> f.hash),
            NoiseHolder.CODEC.fieldOf("plains").forGetter(f -> f.plains),
            NoiseHolder.CODEC.fieldOf("rolling").forGetter(f -> f.rolling),
            NoiseHolder.CODEC.fieldOf("flows").forGetter(f -> f.flows),
            NoiseHolder.CODEC.fieldOf("heat").forGetter(f -> f.heat),
            NoiseHolder.CODEC.fieldOf("ground").forGetter(f -> f.ground),
            NoiseHolder.CODEC.fieldOf("channel").forGetter(f -> f.channel),
            NoiseHolder.CODEC.fieldOf("ridge").forGetter(f -> f.ridge),
            NoiseHolder.CODEC.fieldOf("warp").forGetter(f -> f.warp),
            VolcanicSettings.CODEC.optionalFieldOf("settings", VolcanicSettings.DEFAULT).forGetter(f -> f.settings)
    ).apply(instance, VolcanicField::new));

    public static final KeyDispatchDataCodec<VolcanicField> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private final NoiseHolder hash;
    private final NoiseHolder plains;
    private final NoiseHolder rolling;
    private final NoiseHolder flows;
    private final NoiseHolder heat;
    private final NoiseHolder ground;
    private final NoiseHolder channel;
    private final NoiseHolder ridge;
    private final NoiseHolder warp;
    private final VolcanicSettings settings;
    private final Supplier<RingLayout> layout;
    private final ColumnTable<Column> columns = new ColumnTable<>(Column::new);
    private final Decisions decisions;
    // The 3 x 3 cells around the last column, per thread: every column of a cell needs the same ones.
    private final ThreadOwned<Neighbourhood> neighbourhood = new ThreadOwned<>(Neighbourhood::new);
    private final ThreadOwned<Neighbourhood> channelNeighbourhood = new ThreadOwned<>(Neighbourhood::new);

    public VolcanicField(NoiseHolder hash, NoiseHolder plains, NoiseHolder rolling, NoiseHolder flows, NoiseHolder heat,
                         NoiseHolder ground, NoiseHolder channel, NoiseHolder ridge, NoiseHolder warp, VolcanicSettings settings) {
        this(hash, plains, rolling, flows, heat, ground, channel, ridge, warp, settings, RingLayout::current, new Decisions());
    }

    VolcanicField(NoiseHolder hash, NoiseHolder plains, NoiseHolder rolling, NoiseHolder flows, NoiseHolder heat,
                  NoiseHolder ground, NoiseHolder channel, NoiseHolder ridge, NoiseHolder warp, VolcanicSettings settings,
                  Supplier<RingLayout> layout) {
        this(hash, plains, rolling, flows, heat, ground, channel, ridge, warp, settings, layout, new Decisions());
    }

    private VolcanicField(NoiseHolder hash, NoiseHolder plains, NoiseHolder rolling, NoiseHolder flows, NoiseHolder heat,
                          NoiseHolder ground, NoiseHolder channel, NoiseHolder ridge, NoiseHolder warp, VolcanicSettings settings,
                          Supplier<RingLayout> layout, Decisions decisions) {
        this.hash = hash;
        this.plains = plains;
        this.rolling = rolling;
        this.flows = flows;
        this.heat = heat;
        this.ground = ground;
        this.channel = channel;
        this.ridge = ridge;
        this.warp = warp;
        this.settings = settings;
        this.layout = layout;
        this.decisions = decisions;
    }

    VolcanicSettings settings() {
        return settings;
    }

    // ---------------------------------------------------------------- density function

    @Override
    public double compute(FunctionContext context) {
        return column(context.blockX(), context.blockZ()).height;
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        NoiseHolder[] noises = {hash, plains, rolling, flows, heat, ground, channel, ridge, warp};
        NoiseHolder[] mapped = new NoiseHolder[noises.length];
        for (int i = 0; i < noises.length; i++) {
            mapped[i] = visitor.visitNoise(noises[i]);
        }
        // A noise chunk's copy keeps the per-cell decisions of the random state's instance (same noises, same results);
        // newly wired noises (another seed) start fresh.
        Decisions shared = Arrays.equals(mapped, noises) ? decisions : new Decisions();
        return visitor.apply(new VolcanicField(mapped[0], mapped[1], mapped[2], mapped[3], mapped[4], mapped[5], mapped[6], mapped[7],
                mapped[8], settings, layout, shared));
    }

    @Override
    public double minValue() {
        return -64.0;
    }

    @Override
    public double maxValue() {
        return 320.0;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    // Equality without the caches: the noise chunk's memo then shares one instance between all outputs.
    @Override
    public boolean equals(Object o) {
        return o instanceof VolcanicField f && hash.equals(f.hash) && plains.equals(f.plains) && rolling.equals(f.rolling)
                && flows.equals(f.flows) && heat.equals(f.heat) && ground.equals(f.ground) && channel.equals(f.channel)
                && ridge.equals(f.ridge) && warp.equals(f.warp) && settings.equals(f.settings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash, plains, rolling, flows, heat, ground, channel, ridge, warp, settings);
    }

    /** The first {@code volcanic_field} anywhere inside {@code root} (e.g. a random state's final density), or null. */
    @Nullable
    public static VolcanicField find(DensityFunction root) {
        VolcanicField[] found = new VolcanicField[1];
        root.mapAll(new Visitor() {
            @Override
            public DensityFunction apply(DensityFunction function) {
                if (found[0] == null && function instanceof VolcanicField field) {
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
    static VolcanicField resolveIfBound(DensityFunction function) {
        try {
            return resolve(function);
        } catch (IllegalStateException unbound) {
            return null;
        }
    }

    /** {@code function} as a {@code volcanic_field}, looking through holder references and markers; null if it is none. */
    static VolcanicField resolve(DensityFunction function) {
        DensityFunction current = function;
        for (int depth = 0; depth < 16; depth++) {
            if (current instanceof VolcanicField field) {
                return field;
            }
            if (current instanceof net.minecraft.world.level.levelgen.DensityFunctions.HolderHolder holder) {
                current = holder.function().value();
            } else if (current instanceof net.minecraft.world.level.levelgen.DensityFunctions.MarkerOrMarked marker) {
                current = marker.wrapped();
            } else {
                return null;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- public queries

    /** All 2D values of a column (cached per thread around the current chunk). */
    public Column column(int x, int z) {
        return columns.get(x, z, this::fill);
    }

    /**
     * A lower bound of the surface around a column (the lowest core height on rings of 6, 11 and 16 blocks, 8 points
     * each, turned against each other, minus 2), for the preliminary
     * surface: vanilla interpolates that between chunk corners, so on cones and in craters the real surface would
     * otherwise drop below it and miss the surface rules.
     */
    double initialHeight(int x, int z) {
        // Vanilla asks for the preliminary surface of the same columns (aquifer sample points on a fixed grid) from many
        // chunks; remembered per world like the cell decisions.
        // Keyed on the 4-block grid: the bound holds for every column of a quart (the rings reach 2 blocks further
        // than needed and the result keeps its 2-block margin), and 16 times fewer entries.
        int qx = x & ~3;
        int qz = z & ~3;
        long key = (long) qx << 32 | qz & 0xFFFFFFFFL;
        Double known = decisions.initialHeights.get(key);
        if (known == null) {
            known = lowestAround(qx + 2, qz + 2);
            decisions.remember(decisions.initialHeights, key, known);
        }
        return known;
    }

    private double lowestAround(int x, int z) {
        double lowest = coreHeightUncached(x, z);
        for (double radius : INITIAL_RADII) {
            for (int i = 0; i < 8; i++) {
                double angle = (i + radius / 16.0) * Math.PI / 4.0;
                lowest = Math.min(lowest, coreHeightUncached(x + radius * Math.cos(angle), z + radius * Math.sin(angle)));
            }
        }
        return lowest - 2.0;
    }

    /** +1 where caves must stay out (rock around a lava lake or channel), −1 elsewhere. */
    double guard(int x, int y, int z) {
        Column column = column(x, z);
        return y >= column.guardBottom && y <= column.guardTop ? 1.0 : -1.0;
    }

    /** Density of the floating islands: positive inside, −1 away from them. */
    double islands(int x, int y, int z) {
        Column column = column(x, z);
        double best = -1.0;
        for (int i = 0; i < column.islandCount; i++) {
            double d = Math.min(column.islandTop[i] - y, y - column.islandBottom[i]) / 3.0;
            best = Math.max(best, Math.min(1.0, d));
        }
        return best;
    }

    // ---------------------------------------------------------------- regions

    private double noise(NoiseHolder noise, double x, double z) {
        return noise.getValue(x, 0.0, z);
    }

    /** Region weights at a position, from the two region axes. */
    Regions regions(double x, double z) {
        return Regions.of(settings, noise(heat, x, z), noise(ground, x, z), coreFade(x, z));
    }

    /** 1 in the ring 3 core, falling to 0 over {@link #CORE_FADE} blocks towards both transition zones. */
    private double coreFade(double x, double z) {
        RingLayout ringLayout = layout.get();
        List<Integer> radii = ringLayout.settings().ringOuterRadii();
        double half = ringLayout.settings().transitionWidth() / 2.0;
        double distance = ringLayout.effectiveDistance(x, z);
        return VolcanicMath.smoothstep(radii.get(1) + half, radii.get(1) + half + CORE_FADE, distance)
                * (1.0 - VolcanicMath.smoothstep(radii.get(2) - half - CORE_FADE, radii.get(2) - half, distance));
    }

    /**
     * Region weights at a position. {@code core} fades the regional shapes (seabed, titan ridges, channels) out towards
     * the transition zones, so they never end in a wall against the neighbouring ring.
     */
    record Regions(double heat, double ground, double hot, double mid, double peaks, double flows, double sulfur, double ember,
                   double seabed, double titan, double core) {
        static Regions of(VolcanicSettings s, double h, double g, double core) {
            double hot = VolcanicMath.smoothstep(s.hot() - 0.02, s.hot() + 0.02, h);
            double warmish = VolcanicMath.smoothstep(s.cool() - 0.02, s.cool() + 0.02, h);
            double mid = warmish * (1.0 - hot);
            double mineral = 1.0 - VolcanicMath.smoothstep(-0.02, 0.02, g);
            // Seabed and titan shapes ramp up only inside their biome rectangles, so the slopes stay in the biome.
            double coolInside = 1.0 - VolcanicMath.smoothstep(s.cool() - 0.09, s.cool() - 0.01, h);
            double seabed = coolInside * (1.0 - VolcanicMath.smoothstep(s.seabedGround() - 0.09, s.seabedGround() - 0.01, g));
            double titan = coolInside * VolcanicMath.smoothstep(s.titanGround() + 0.01, s.titanGround() + 0.07, g);
            return new Regions(h, g, hot, mid, hot * mineral, hot * (1.0 - mineral), mid * mineral, mid * (1.0 - mineral),
                    seabed * core, titan * core, core);
        }
    }

    // ---------------------------------------------------------------- plain

    /** Height of the plain (everything except volcanoes and channels). */
    double plain(double x, double z, Regions r) {
        double swell = noise(plains, x, z);
        double wave = noise(rolling, x, z);
        double base = settings.plainY() + settings.plainAmp() * swell + settings.rollingAmp() * wave * (1.0 + 0.8 * r.ember());
        // Low hills and long ash ridges, so the plain between the volcanoes is never bare-flat. Both are coarser samples
        // of the existing noises (no extra noise to wire).
        double hill = noise(rolling, x * 0.45 + 1733.3, z * 0.45 - 911.7);
        base += HILL_HEIGHT * Math.pow(Math.max(0.0, hill), 1.4);
        double crest = 1.0 - Math.abs(noise(flows, x * 0.6 - 377.1, z * 0.6 + 2011.9));
        base += RIDGE_HEIGHT * (1.0 - r.flows()) * Math.pow(Math.max(0.0, crest - 0.7) / 0.3, 2.0);
        if (r.flows() > 0.0) {
            // Stepped lobes of old flows with steep fronts.
            base += r.flows() * 5.0 * VolcanicMath.stair(0.5 + 0.9 * noise(flows, x, z), 3, 0.35);
        }
        if (r.sulfur() > 0.0) {
            base = Mth.lerp(0.7 * r.sulfur(), base, settings.plainY() - 1.0 + 0.35 * (base - settings.plainY()));
        }
        if (r.titan() > 0.0) {
            double flat = settings.plainY() - 2.0 + 0.3 * (base - settings.plainY());
            // Long ribs: the ridge noise stretched along a turned axis (never along x or z) and bent by the warp.
            double along = x * RIB_COS + z * RIB_SIN;
            double across = -x * RIB_SIN + z * RIB_COS;
            double q = noise(ridge, across + 48.0 * noise(warp, x, z), along * 0.35);
            double rib = Math.max(0.0, 1.0 - Math.abs(q) / 0.12);
            double ridges = 8.0 * Math.pow(rib, 1.6) * (0.7 + 0.3 * wave);
            base = Mth.lerp(r.titan(), base, flat + ridges);
        }
        if (r.seabed() > 0.0) {
            double floor = 38.0 + 3.0 * wave + 2.5 * noise(flows, x * 1.7, z * 1.7);
            base = Mth.lerp(VolcanicMath.stair(r.seabed(), 3, 0.4), base, floor);
        }
        return base;
    }

    // ---------------------------------------------------------------- volcanoes

    /** The volcano of grid cell (i, j), or null. Pure. */
    Volcano place(int i, int j) {
        int size = settings.cellSize();
        long cell = SeededHash.hash(hash, i, j, CELL_SALT);
        double cx = i * size + CELL_MARGIN + SeededHash.unit(cell, 0) * (size - 2.0 * CELL_MARGIN);
        double cz = j * size + CELL_MARGIN + SeededHash.unit(cell, 1) * (size - 2.0 * CELL_MARGIN);
        RingLayout ringLayout = layout.get();
        List<Integer> radii = ringLayout.settings().ringOuterRadii();
        double half = ringLayout.settings().transitionWidth() / 2.0;
        double inner = radii.get(1) + half;
        double outer = radii.get(2) - half;
        double distance = ringLayout.effectiveDistance(cx, cz);
        if (distance < inner || distance > outer) {
            return null;
        }
        Regions r = regions(cx, cz);
        double behindZone = distance - inner;
        boolean band = behindZone >= BAND_START && behindZone <= BAND_END;
        double open = 1.0 - Math.max(r.seabed(), r.titan());
        double chance = (0.07 + 0.78 * r.peaks() + 0.2 * r.flows() + 0.08 * r.sulfur()) * open;
        if (band) {
            chance = Math.max(chance, 0.8 * (1.0 - r.seabed()));
        }
        if (SeededHash.unit(cell, 2) >= chance) {
            return null;
        }
        double greatChance = band ? 0.5 : 0.03 + 0.3 * r.peaks();
        double calderaChance = (1.0 - greatChance) * (0.22 + 0.3 * (1.0 - r.hot()));
        double kindRoll = SeededHash.unit(cell, 3);
        Volcano.Kind kind = kindRoll < greatChance ? Volcano.Kind.GREAT
                : kindRoll < greatChance + calderaChance ? Volcano.Kind.CALDERA : Volcano.Kind.CONE;
        double plainHere = plain(cx, cz, r);
        double u4 = SeededHash.unit(cell, 4);
        double u5 = SeededHash.unit(cell, 5);
        double u6 = SeededHash.unit(cell, 6);
        double u7 = SeededHash.unit(cell, 7);
        double u8 = SeededHash.unit(cell, 8);
        double u9 = SeededHash.unit(cell, 9);
        double rim;
        double radius;
        double crest;
        double floorRadius;
        double floor;
        double p;
        double q;
        boolean dormant;
        switch (kind) {
            case GREAT -> {
                rim = settings.greatMinY() + u4 * (settings.greatMaxY() - settings.greatMinY());
                radius = (rim - plainHere) * (1.25 + 0.3 * u5);
                crest = 45.0 + 25.0 * u6;
                floor = rim - (22.0 + 18.0 * u7);
                p = 1.6 + 0.3 * u9;
                q = 1.5 + 0.5 * u5;
                floorRadius = wallFoot(crest, rim - floor, q);
                dormant = false;
            }
            case CONE -> {
                // Higher cones where it is hottest.
                double lift = Math.min(1.0, u4 * (0.6 + 0.5 * r.peaks()));
                rim = settings.coneMinY() + lift * (settings.coneMaxY() - settings.coneMinY());
                // On a high plain (hills and ridges stack up) a cone still rises well above it.
                rim = Math.max(rim, plainHere + MIN_CONE_LIFT);
                radius = (rim - plainHere) * (1.35 + 0.45 * u5);
                crest = 16.0 + 0.13 * radius + 8.0 * u6;
                dormant = u8 < 0.3;
                double depth = 20.0 + 12.0 * u7 + (dormant ? 10.0 : 0.0);
                floor = rim - depth;
                p = 1.5 + 0.5 * u9;
                q = 1.4 + 0.6 * u5;
                floorRadius = wallFoot(crest, depth, q);
            }
            default -> {
                rim = plainHere + 22.0 + 22.0 * u4;
                crest = 65.0 + 55.0 * u5;
                radius = crest + 50.0 + 30.0 * u6;
                dormant = u8 < 0.45;
                floor = dormant ? plainHere - 10.0 + 4.0 * u7 : plainHere - 4.0 + 8.0 * u7;
                floor = Math.min(floor, rim - 20.0);
                p = 1.4 + 0.4 * u9;
                q = 1.3 + 0.5 * u7;
                floorRadius = wallFoot(crest, rim - floor, q);
            }
        }
        // A column only looks at the 3 x 3 cells around it: the foot (plus the warp of the outline) must stay inside.
        // The flank needs some width outside the crest (surface() divides by radius - crest).
        radius = Math.max(Math.min(radius, MAX_RADIUS_SHARE * size), crest + MIN_FLANK);
        double allowance = kind == Volcano.Kind.GREAT ? 0.4 : 1.0;
        if (behindZone < allowance * radius || outer - distance < 1.05 * radius) {
            return null;
        }
        double lift = rim - plainHere;
        double rimA2 = 1.0 + 0.035 * lift;
        double rimA3 = 0.5 + 0.025 * lift;
        double rimLow = rim - rimA2 - rimA3;
        double lake = Double.NaN;
        if (!dormant) {
            lake = Math.min(floor + Math.min(7.0, rim - floor - 13.0), rimLow - 6.0);
        }
        int gullies = 7 + (int) Math.round(radius / 30.0);
        double gullyDepth = kind == Volcano.Kind.CALDERA ? 0.08 : 0.16 + 0.1 * u6;
        long shape = SeededHash.hash(hash, i, j, CELL_SALT + 1);
        List<Volcano.Island> islands = !dormant && rim <= settings.islandMaxY() - 36.0
                && SeededHash.unit(shape, 0) < settings.islandChance()
                ? cluster(shape, cx, cz, floorRadius, rim + 30.0) : List.of();
        return new Volcano(cx, cz, kind, plainHere, rim, radius, crest, floorRadius, floor, p, q, dormant, lake,
                rimA2, SeededHash.unit(shape, 1) * Math.PI * 2.0, rimA3, SeededHash.unit(shape, 2) * Math.PI * 2.0,
                gullies, SeededHash.unit(shape, 3) * Math.PI * 2.0, gullyDepth, 0.08 * radius, islands);
    }

    /**
     * Radius of the crater floor: the wall of height {@code wall} gets the width for a slope of about 1.8 under the
     * rim (steep, but never a knife edge), and the floor keeps at least a third of the crest radius.
     */
    private static double wallFoot(double crest, double wall, double q) {
        return Math.max(crest / 3.0, crest - q * wall / 1.8);
    }

    /** A cluster of 1–4 islands around (x, z) within {@code spread}, the main one with its top at or above {@code lowestTop}. */
    private List<Volcano.Island> cluster(long seed, double x, double z, double spread, double lowestTop) {
        double maxTop = settings.islandMaxY() - 6.0;
        double minTop = Math.max(settings.islandMinY() + 4.0, lowestTop);
        if (minTop > maxTop) {
            return List.of();
        }
        double roll = SeededHash.unit(seed, 10);
        int count = roll < 0.25 ? 1 : roll < 0.6 ? 2 : roll < 0.85 ? 3 : 4;
        double baseTop = minTop + SeededHash.unit(seed, 11) * (maxTop - minTop);
        double angle0 = SeededHash.unit(seed, 12) * Math.PI * 2.0;
        double ox = x + Math.cos(angle0) * spread * 0.3 * SeededHash.unit(seed, 13);
        double oz = z + Math.sin(angle0) * spread * 0.3 * SeededHash.unit(seed, 13);
        List<Volcano.Island> islands = new ArrayList<>(count);
        double mainA = 9.0 + 6.0 * SeededHash.unit(seed, 14);
        for (int k = 0; k < count; k++) {
            int b = 20 + 8 * k;
            double a = k == 0 ? mainA : 5.0 + 5.0 * SeededHash.unit(seed, b);
            double ix = ox;
            double iz = oz;
            double top = baseTop;
            if (k > 0) {
                double angle = SeededHash.unit(seed, b + 1) * Math.PI * 2.0;
                double distance = mainA + 0.6 * a + 4.0 + 10.0 * SeededHash.unit(seed, b + 2);
                ix += Math.cos(angle) * distance;
                iz += Math.sin(angle) * distance;
                top = Mth.clamp(baseTop - 9.0 + 18.0 * SeededHash.unit(seed, b + 3), settings.islandMinY() + 2.0, settings.islandMaxY());
                top = Math.max(top, lowestTop - 6.0);
            }
            double stretch = 1.0 + 0.5 * SeededHash.unit(seed, b + 4);
            double thickness = Math.min(28.0, a * (1.4 + 0.8 * SeededHash.unit(seed, b + 5)));
            islands.add(new Volcano.Island(ix, iz, a, stretch, SeededHash.unit(seed, b + 6) * Math.PI, top, thickness));
        }
        return islands;
    }

    /** Island cluster above a lava channel in island cell (i, j), or none. Pure. */
    List<Volcano.Island> channelCluster(int i, int j) {
        long cell = SeededHash.hash(hash, i, j, ISLAND_CELL_SALT);
        if (SeededHash.unit(cell, 0) >= settings.islandChance() * 0.6) {
            return List.of();
        }
        for (int k = 0; k < ISLAND_CANDIDATES; k++) {
            double x = i * ISLAND_CELL + 64.0 + SeededHash.unit(cell, 1 + 2 * k) * (ISLAND_CELL - 128.0);
            double z = j * ISLAND_CELL + 64.0 + SeededHash.unit(cell, 2 + 2 * k) * (ISLAND_CELL - 128.0);
            Column probe = new Column();
            fill(probe, Mth.floor(x), Mth.floor(z), false);
            if (probe.channel >= 0.6) {
                return cluster(SeededHash.mix(cell + k), x, z, 10.0, probe.preHeight + 60.0);
            }
        }
        return List.of();
    }

    // ---------------------------------------------------------------- columns

    private double coreHeightUncached(double x, double z) {
        Column column = new Column();
        fill(column, Mth.floor(x), Mth.floor(z), false);
        return column.height;
    }

    private void fill(Column column, int x, int z) {
        fill(column, x, z, true);
    }

    /** Computes a column; {@code full} also gathers the islands (which need other columns' channels). */
    void fill(Column c, int x, int z, boolean full) {
        double px = x + 0.5;
        double pz = z + 0.5;
        Regions r = regions(px, pz);
        double plain = plain(px, pz, r);

        c.reset();
        double warpX = noise(warp, px, pz);
        double warpZ = noise(warp, px + WARP_OFFSET, pz - WARP_OFFSET);
        double raise = 0.0;
        double sink = 0.0;
        double influence = 0.0;
        int size = settings.cellSize();
        int ci = Math.floorDiv(x, size);
        int cj = Math.floorDiv(z, size);
        // A copy: checking an island computes other columns, which moves the per-thread neighbourhood.
        Volcano[] around = neighbourhood.get().around(this, ci, cj).volcanoes.clone();
        for (int di = -1; di <= 1; di++) {
            for (int dj = -1; dj <= 1; dj++) {
                Volcano v = around[(di + 1) * 3 + dj + 1];
                if (v == null) {
                    continue;
                }
                if (full) {
                    addIslands(c, v.islands(), px, pz, warpX);
                }
                double dx = px + warpX * v.warp() - v.x();
                double dz = pz + warpZ * v.warp() - v.z();
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist >= v.reach()) {
                    continue;
                }
                double angle = Math.atan2(dz, dx);
                double delta = v.surface(dist, angle, plain, 3.5 * warpZ) - plain;
                if (delta > 0.0) {
                    // Rounded where two flanks meet, but the rounding shrinks to nothing at a foot (delta -> 0), so a
                    // volcano's edge leaves no step.
                    double k = Math.min(4.0, Math.min(raise, delta));
                    raise = k > 0.0 ? VolcanicMath.smoothMax(raise, delta, k) : Math.max(raise, delta);
                } else if (delta < 0.0) {
                    sink = Math.min(sink, delta);
                }
                influence = Math.max(influence, 1.0 - VolcanicMath.smoothstep(0.7 * v.radius(), v.radius(), dist));
                c.roughness = Math.max(c.roughness, Mth.clamp((v.slope(dist) - 0.6) / 0.8, 0.0, 1.0));
                double crater = v.craterMask(dist);
                c.calderaMask = Math.max(c.calderaMask, crater);
                if (v.dormant()) {
                    c.dormant = Math.max(c.dormant, crater);
                } else if (dist < v.crest() && !c.hasLava()) {
                    c.lavaTop = Mth.floor(v.lake());
                    c.lavaBottom = Mth.floor(v.floor()) - 3;
                }
                if (!v.dormant() && dist < v.crest() + 6.0) {
                    int lake = Mth.floor(v.lake());
                    c.guardBottom = Math.min(c.guardBottom, lake - 16);
                    c.guardTop = Math.max(c.guardTop, lake + 4);
                }
            }
        }
        double preHeight = plain + raise + sink;
        c.preHeight = preHeight;
        c.rise = raise;

        // Lava channels: a noise network in the warm bands, away from volcanoes, the seabed and the titan fields.
        double along = Math.abs(noise(channel, px, pz)) / settings.channelWidth();
        double mask = VolcanicMath.smoothstep(settings.cool() - 0.01, settings.cool() + 0.06, r.heat())
                * (0.45 + 0.55 * VolcanicMath.smoothstep(-0.04, 0.04, r.ground()))
                * (1.0 - VolcanicMath.smoothstep(0.0, 0.15, Math.max(r.seabed(), r.titan()))) * (1.0 - influence) * r.core();
        // The lava level follows only the broad swell of the plain, so it steps down rarely; the bed is cut to below it,
        // through hills as a gorge. The swell minus the largest dip of the waves keeps the banks above the lava.
        int level = Mth.floor(settings.plainY() + settings.plainAmp() * noise(plains, px, pz)
                - 1.8 * settings.rollingAmp() - CHANNEL_BANK);
        double bed = level - CHANNEL_DEPTH;
        // The bed reaches its full depth over a short stretch of the mask, so a lava river ends cleanly instead of in
        // a fringe of one-block puddles.
        double depth = VolcanicMath.smoothstep(0.25, 0.5, mask);
        double profile = along >= 1.0 ? 0.0 : 1.0 - VolcanicMath.smoothstep(0.3, 1.0, along);
        double cut = depth * profile * Math.max(0.0, preHeight - bed);
        c.channel = depth * profile;
        c.height = preHeight - cut;
        // The surface's lava banks: the stretch where the channel holds lava (the rule below), up to its edge at 1.3.
        if (depth >= 0.85 && preHeight >= level + 2.0) {
            c.lavaBank = Mth.clamp(1.0 - along / 1.3, 0.0, 1.0);
        }
        if (depth >= 0.85 && along < 1.3 && preHeight >= level + 2.0 && !c.hasLava()) {
            c.lavaTop = level;
            c.lavaBottom = level - (int) CHANNEL_DEPTH - 3;
        }
        if (depth > 0.0 && along < 1.8) {
            c.guardBottom = Math.min(c.guardBottom, level - 12);
            c.guardTop = Math.max(c.guardTop, level + 4);
        }
        if (full) {
            Neighbourhood islandCells = channelNeighbourhood.get().aroundIslands(this, Math.floorDiv(x, ISLAND_CELL), Math.floorDiv(z, ISLAND_CELL));
            for (List<Volcano.Island> cell : islandCells.islands) {
                addIslands(c, cell, px, pz, warpX);
            }
        }
    }

    private void addIslands(Column c, List<Volcano.Island> islands, double x, double z, double warpX) {
        for (Volcano.Island island : islands) {
            if (Math.abs(x - island.x()) <= island.reach() && Math.abs(z - island.z()) <= island.reach() && islandClear(island)) {
                c.addIsland(island, x, z, warpX);
            }
        }
    }

    /**
     * Whether an island hangs free: at least {@link #ISLAND_CLEARANCE} blocks above the terrain under its centre and
     * under eight points of its rim (a neighbouring volcano may rise under a cluster). Pure; remembered per thread.
     */
    boolean islandClear(Volcano.Island island) {
        Boolean known = decisions.islands.get(island);
        if (known == null) {
            known = checkIsland(island);
            decisions.remember(decisions.islands, island, known);
        }
        return known;
    }

    private boolean checkIsland(Volcano.Island island) {
        if (island.bottomAt(0.0) < coreHeightUncached(island.x(), island.z()) + ISLAND_CLEARANCE) {
            return false;
        }
        for (int k = 0; k < 8; k++) {
            double angle = k * Math.PI / 4.0;
            double r = island.a() * island.stretch();
            if (island.bottomAt(0.7) < coreHeightUncached(island.x() + r * Math.cos(angle), island.z() + r * Math.sin(angle))
                    + ISLAND_CLEARANCE) {
                return false;
            }
        }
        return true;
    }

    /** All 2D values of one column. Heights in blocks. */
    public static final class Column {
        double height;
        double preHeight;
        double roughness;
        double calderaMask;
        double dormant;
        double channel;
        double islandMask;
        /** Height of the volcanoes above the plain (before the craters sink in), in blocks. */
        double rise;
        /** 0..1: a lava channel that holds lava, 1 in the middle, 0 from 1.3 half widths out (the banks). */
        double lavaBank;
        /** Lava fills open space from {@link #lavaBottom} up to, not including, {@link #lavaTop}. */
        int lavaTop;
        int lavaBottom;
        int guardBottom;
        int guardTop;
        int islandCount;
        final double[] islandTop = new double[MAX_ISLANDS];
        final double[] islandBottom = new double[MAX_ISLANDS];

        void reset() {
            height = 0.0;
            preHeight = 0.0;
            roughness = 0.0;
            calderaMask = 0.0;
            dormant = 0.0;
            channel = 0.0;
            islandMask = 0.0;
            rise = 0.0;
            lavaBank = 0.0;
            lavaTop = Integer.MIN_VALUE;
            lavaBottom = Integer.MIN_VALUE;
            guardBottom = Integer.MAX_VALUE;
            guardTop = Integer.MIN_VALUE;
            islandCount = 0;
        }

        void addIsland(Volcano.Island island, double x, double z, double warp) {
            double d = island.distance(x, z, 0.18 * warp);
            if (d >= 1.0 || islandCount == MAX_ISLANDS) {
                return;
            }
            islandTop[islandCount] = island.topAt(d);
            islandBottom[islandCount] = island.bottomAt(d);
            islandCount++;
            islandMask = Math.max(islandMask, Mth.clamp((1.0 - d) * 4.0, 0.0, 1.0));
        }

        public double height() {
            return height;
        }

        public double roughness() {
            return roughness;
        }

        /** Top of the lava (exclusive), or {@link Integer#MIN_VALUE} without lava. */
        public int lavaTop() {
            return lavaTop;
        }

        boolean hasLava() {
            return lavaTop != Integer.MIN_VALUE;
        }

        /** Whether lava fills open space at {@code y} in this column. */
        public boolean lavaAt(int y) {
            return y < lavaTop && y >= lavaBottom;
        }
    }

    /** The volcano of cell (i, j), or null; remembered across the noise chunks of one random state. */
    Volcano volcano(int i, int j) {
        long key = (long) i << 32 | j & 0xFFFFFFFFL;
        Optional<Volcano> known = decisions.volcanoes.get(key);
        if (known == null) {
            known = Optional.ofNullable(place(i, j));
            decisions.remember(decisions.volcanoes, key, known);
        }
        return known.orElse(null);
    }

    private List<Volcano.Island> channelIslands(int i, int j) {
        long key = (long) i << 32 | j & 0xFFFFFFFFL;
        List<Volcano.Island> known = decisions.channelIslands.get(key);
        if (known == null) {
            known = channelCluster(i, j);
            decisions.remember(decisions.channelIslands, key, known);
        }
        return known;
    }

    /**
     * Per-cell decisions (volcanoes, island cells, island checks): pure functions of the seed and the cell, shared by
     * all copies of one wired field (the noise chunk makes a copy per chunk), so they are made once per world, not once
     * per chunk. Only a cache; cleared when it grows past {@link #LIMIT} entries.
     */
    static final class Decisions {
        private static final int LIMIT = 100_000;
        final Map<Long, Optional<Volcano>> volcanoes = new ConcurrentHashMap<>();
        final Map<Long, List<Volcano.Island>> channelIslands = new ConcurrentHashMap<>();
        final Map<Volcano.Island, Boolean> islands = new ConcurrentHashMap<>();
        final Map<Long, Double> initialHeights = new ConcurrentHashMap<>();

        <K, V> void remember(Map<K, V> map, K key, V value) {
            if (map.size() > LIMIT) {
                map.clear();
            }
            map.putIfAbsent(key, value);
        }
    }

    /** The decisions of the 3 x 3 cells around one cell (volcano cells or island cells); only a cache. */
    private static final class Neighbourhood {
        private boolean filled;
        private int ci;
        private int cj;
        final Volcano[] volcanoes = new Volcano[9];
        @SuppressWarnings("unchecked")
        final List<Volcano.Island>[] islands = new List[9];

        Neighbourhood around(VolcanicField field, int i, int j) {
            if (!filled || i != ci || j != cj) {
                for (int k = 0; k < 9; k++) {
                    volcanoes[k] = field.volcano(i + k / 3 - 1, j + k % 3 - 1);
                }
                ci = i;
                cj = j;
                filled = true;
            }
            return this;
        }

        Neighbourhood aroundIslands(VolcanicField field, int i, int j) {
            if (!filled || i != ci || j != cj) {
                for (int k = 0; k < 9; k++) {
                    islands[k] = field.channelIslands(i + k / 3 - 1, j + k % 3 - 1);
                }
                ci = i;
                cj = j;
                filled = true;
            }
            return this;
        }
    }
}
