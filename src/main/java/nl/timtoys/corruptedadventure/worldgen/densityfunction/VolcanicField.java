package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicCaches.CellMemo;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicCaches.CinderNeighbourhood;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicCaches.Decisions;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicCaches.Neighbourhood;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicCaches.RiverTile;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * {@code corruptedadventure:volcanic_field}: the ring 3 overworld terrain as 2D shapes plus thin 3D layers. As a
 * density function it is the surface height of the ring 3 core (without the blend into the zone 2→3, which the JSON
 * does); {@link VolcanicOutput} reads its other outputs (masks, lava, islands, cave guard, solids and voids).
 * <p>
 * Shapes, from the bottom up:
 * <ul>
 *   <li>the plain (39d): highlands and lowlands ({@link Ring3Relief}), swells, rolling waves and hills, with a landform
 *   per biome: ash dunes with gullies and hoodoos, gentle ember lowland, sulfur terraces with mounds and sinkholes,
 *   stepped basalt plateaus with columnar cliffs and lava tubes, a peak range with arêtes, titan bones, a terraced
 *   seabed with reef ridges and sea stacks; rift valleys and cinder cone fields across the ring;</li>
 *   <li>volcanoes on a jittered grid ({@link Volcano}): great horizon volcanoes, ordinary cones and low calderas, each
 *   with a crater that is active (lava lake) or dormant (flat floor, no lava). Most stand where the region axes give
 *   {@code volcanic_peaks}; a band of mostly great ones stands just behind the zone 2→3, visible from {@code dry_riverbeds};</li>
 *   <li>lava rivers ({@link LavaRivers}): traced downhill from the active volcanoes;</li>
 *   <li>floating islands in clusters above low active craters and above rivers.</li>
 * </ul>
 * Lava lies in the open space below a column's lava top ({@link Column#lavaTop}, filled by the aquifer of
 * {@code RingChunkGenerator}); a cave guard band keeps caves out of the rock around lakes and rivers.
 * <p>
 * Pure: a function of the seed (through the noises), the ring layout and x, z. The caches only remember results.
 * Decisions per volcano read the noises at exact positions, never a {@code flat_cache} (whose value inside a noise
 * chunk is the 4-block grid value), so every chunk sees the same volcanoes.
 */
public final class VolcanicField implements DensityFunction {
    private static final long WORLD_SALT = 991;
    private static final double CORE_FADE = 160.0;
    /** The relief (highlands, landforms) grows over this many blocks behind the zone 2→3, so the zone needs no cliff. */
    private static final double RELIEF_FADE = 240.0;
    private static final double WARP_OFFSET = 5000.5;
    /** Depth of a lava river's bed below its lava level. */
    private static final int CHANNEL_DEPTH = 2;
    /** Height of a river's banks above its lava (levees fill lower ground beside it). */
    private static final double CHANNEL_BANK = 2.5;
    /** Width (blocks) of the scorched bank beside the lava, whatever the river's width. */
    private static final double BANK_BLOCKS = 3.0;
    private static final double HILL_HEIGHT = 40.0;
    private static final double PEAK_RIDGE_HEIGHT = 55.0;
    /** Amplitude (blocks) of the micro relief on the plain. */
    private static final double MICRO_RELIEF = 2.0;
    /** Below this the crags would barely show; the column gets none (and skips their 3D noise). */
    private static final double MIN_ROUGHNESS = 0.15;
    private static final double PLAIN_SOFT_CAP = 190.0;
    private static final double RIB_COS = Math.cos(0.6);
    private static final double RIB_SIN = Math.sin(0.6);
    private static final double RIDGE_HEIGHT = 5.0;
    private static final int MAX_ISLANDS = 6;
    private static final int MAX_SOLIDS = 4;
    /** Highest top of a thin shape (below the top slide of the ring 3 stack). */
    static final double THIN_TOP = 296.0;
    private static final double ISLAND_CLEARANCE = 10.0;
    private static final double[] INITIAL_RADII = {6.0, 12.0, 18.0};

    public static final MapCodec<VolcanicField> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            NoiseHolder.CODEC.fieldOf("hash").forGetter(f -> f.hash),
            NoiseHolder.CODEC.fieldOf("plains").forGetter(f -> f.plains),
            NoiseHolder.CODEC.fieldOf("rolling").forGetter(f -> f.rolling),
            NoiseHolder.CODEC.fieldOf("flows").forGetter(f -> f.flows),
            NoiseHolder.CODEC.fieldOf("heat").forGetter(f -> f.heat),
            NoiseHolder.CODEC.fieldOf("ground").forGetter(f -> f.ground),
            NoiseHolder.CODEC.fieldOf("ridge").forGetter(f -> f.ridge),
            NoiseHolder.CODEC.fieldOf("warp").forGetter(f -> f.warp),
            Ring3Relief.MAP_CODEC.fieldOf("relief").forGetter(f -> f.relief),
            VolcanicSettings.CODEC.optionalFieldOf("settings", VolcanicSettings.DEFAULT).forGetter(f -> f.settings)
    ).apply(instance, VolcanicField::new));

    public static final KeyDispatchDataCodec<VolcanicField> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private final NoiseHolder hash;
    private final NoiseHolder plains;
    private final NoiseHolder rolling;
    private final NoiseHolder flows;
    private final NoiseHolder heat;
    private final NoiseHolder ground;
    private final NoiseHolder ridge;
    private final NoiseHolder warp;
    private final Ring3Relief relief;
    private final VolcanicSettings settings;
    private final Supplier<RingLayout> layout;
    private final ColumnTable<Column> columns = new ColumnTable<>(Column::new);
    private final Decisions decisions;
    // The 3 x 3 cells around the last column, per thread: every column of a cell needs the same ones.
    private final ThreadOwned<Neighbourhood> neighbourhood = new ThreadOwned<>(Neighbourhood::new);
    private final ThreadOwned<Neighbourhood> channelNeighbourhood = new ThreadOwned<>(Neighbourhood::new);
    private final ThreadOwned<CinderNeighbourhood> cinderNeighbourhood = new ThreadOwned<>(CinderNeighbourhood::new);
    private final ThreadOwned<RiverTile> riverTile = new ThreadOwned<>(RiverTile::new);
    // Scratch columns of the uncached fills (core heights, island probes); one per use, so they never nest.
    private final ThreadOwned<Column> heightScratch = new ThreadOwned<>(Column::new);
    private final ThreadOwned<Column> probeScratch = new ThreadOwned<>(Column::new);
    private final ThreadOwned<CellMemo<Landmarks.Needle>> needleMemo = new ThreadOwned<>(CellMemo::new);
    private final ThreadOwned<CellMemo<Landmarks.Arch>> riftArchMemo = new ThreadOwned<>(CellMemo::new);
    private final ThreadOwned<CellMemo<Landmarks.FloatingVolcano>> floatingMemo = new ThreadOwned<>(CellMemo::new);
    private final ThreadOwned<CellMemo<Landmarks.Abyss>> abyssMemo = new ThreadOwned<>(CellMemo::new);
    private final ThreadOwned<CellMemo<TitanBones.Rib>> ribMemo = new ThreadOwned<>(CellMemo::new);
    private final ThreadOwned<CellMemo<TitanBones.Cage>> cageMemo = new ThreadOwned<>(CellMemo::new);

    public VolcanicField(NoiseHolder hash, NoiseHolder plains, NoiseHolder rolling, NoiseHolder flows, NoiseHolder heat,
                         NoiseHolder ground, NoiseHolder ridge, NoiseHolder warp, Ring3Relief relief, VolcanicSettings settings) {
        this(hash, plains, rolling, flows, heat, ground, ridge, warp, relief, settings, RingLayout::current, new Decisions());
    }

    VolcanicField(NoiseHolder hash, NoiseHolder plains, NoiseHolder rolling, NoiseHolder flows, NoiseHolder heat,
                  NoiseHolder ground, NoiseHolder ridge, NoiseHolder warp, Ring3Relief relief, VolcanicSettings settings,
                  Supplier<RingLayout> layout) {
        this(hash, plains, rolling, flows, heat, ground, ridge, warp, relief, settings, layout, new Decisions());
    }

    private VolcanicField(NoiseHolder hash, NoiseHolder plains, NoiseHolder rolling, NoiseHolder flows, NoiseHolder heat,
                          NoiseHolder ground, NoiseHolder ridge, NoiseHolder warp, Ring3Relief relief, VolcanicSettings settings,
                          Supplier<RingLayout> layout, Decisions decisions) {
        this.hash = hash;
        this.plains = plains;
        this.rolling = rolling;
        this.flows = flows;
        this.heat = heat;
        this.ground = ground;
        this.ridge = ridge;
        this.warp = warp;
        this.relief = relief;
        this.settings = settings;
        this.layout = layout;
        this.decisions = decisions;
    }

    VolcanicSettings settings() {
        return settings;
    }

    Ring3Relief relief() {
        return relief;
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
        NoiseHolder[] noises = {hash, plains, rolling, flows, heat, ground, ridge, warp};
        NoiseHolder[] mapped = new NoiseHolder[noises.length];
        for (int i = 0; i < noises.length; i++) {
            mapped[i] = visitor.visitNoise(noises[i]);
        }
        Ring3Relief mappedRelief = relief.map(visitor);
        // A noise chunk's copy keeps the per-cell decisions of the random state's instance (same noises, same results);
        // newly wired noises (another seed) start fresh.
        Decisions shared = Arrays.equals(mapped, noises) && mappedRelief.equals(relief) ? decisions : new Decisions();
        return visitor.apply(new VolcanicField(mapped[0], mapped[1], mapped[2], mapped[3], mapped[4], mapped[5], mapped[6], mapped[7],
                mappedRelief, settings, layout, shared));
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
                && flows.equals(f.flows) && heat.equals(f.heat) && ground.equals(f.ground) && ridge.equals(f.ridge)
                && warp.equals(f.warp) && relief.equals(f.relief) && settings.equals(f.settings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash, plains, rolling, flows, heat, ground, ridge, warp, relief, settings);
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
     * A lower bound of the surface around a column (the lowest core height on rings of 6, 12 and 18 blocks around the
     * quart's centre, 8 points each, turned against each other, minus 2; 39d: each sample also less the depth of the
     * cuts that may lie between the samples, {@link Column#floorMargin}), for the preliminary
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
        double lowest = coreFloorUncached(x, z);
        for (double radius : INITIAL_RADII) {
            for (int i = 0; i < 8; i++) {
                double angle = (i + radius / 16.0) * Math.PI / 4.0;
                lowest = Math.min(lowest, coreFloorUncached(x + radius * Math.cos(angle), z + radius * Math.sin(angle)));
            }
        }
        return lowest - 2.0;
    }

    /** The core height of a column less the depth of the cuts that may lie near it ({@link Column#floorMargin}). */
    private double coreFloorUncached(double x, double z) {
        Column column = heightScratch.get();
        fill(column, Mth.floor(x), Mth.floor(z), Mode.HEIGHT);
        return column.height - column.floorMargin;
    }

    /** +1 where caves must stay out (rock around a lava lake or river), −1 elsewhere. */
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

    /**
     * The thin shapes at a block, outside the terrain interpolation: +1 inside a thin solid (hoodoo, rib, column prism,
     * sea stack, needle, arch, bridge, pillar), −1 inside a carved opening (a lava tube's tunnel), 0 elsewhere (the
     * terrain decides). One column lookup per block for both.
     */
    double thin(int x, int y, int z) {
        Column column = column(x, z);
        for (int i = 0; i < column.solidCount; i++) {
            if (y >= column.solidBottom[i] && y <= column.solidTop[i]) {
                return 1.0;
            }
        }
        return y >= column.voidBottom && y <= column.voidTop ? -1.0 : 0.0;
    }

    // ---------------------------------------------------------------- regions

    private double noise(NoiseHolder noise, double x, double z) {
        return noise.getValue(x, 0.0, z);
    }

    /** Region weights at a position, from the two region axes. */
    Regions regions(double x, double z) {
        RingLayout ringLayout = layout.get();
        List<Integer> radii = ringLayout.settings().ringOuterRadii();
        double half = ringLayout.settings().transitionWidth() / 2.0;
        double distance = ringLayout.effectiveDistance(x, z);
        double inner = radii.get(1) + half;
        double outer = radii.get(2) - half;
        double towardsOuter = 1.0 - VolcanicMath.smoothstep(outer - CORE_FADE, outer, distance);
        double core = VolcanicMath.smoothstep(inner, inner + CORE_FADE, distance) * towardsOuter;
        double fade = VolcanicMath.smoothstep(inner, inner + RELIEF_FADE, distance) * towardsOuter;
        return Regions.of(settings, noise(heat, x, z), noise(ground, x, z), core, fade);
    }

    /**
     * Region weights at a position. {@code core} fades the regional shapes (seabed, titan ridges, rivers) out towards
     * the transition zones, so they never end in a wall against the neighbouring ring. The {@code …S} weights are the
     * landform weights (39d): 0 on a biome's border and 1 a little inside it, so neighbouring landforms never meet; they
     * include {@code fade} (the relief grows behind the zone 2→3). {@code riftOpen} keeps rifts out of the seabed, the
     * titan fields and the sulfur basin, fading across those borders.
     */
    record Regions(double heat, double ground, double hot, double mid, double peaks, double flows, double sulfur, double ember,
                   double ashen, double seabed, double titan, double core, double fade, double peaksS, double flowsS,
                   double sulfurS, double emberS, double ashenS, double seabedS, double titanS, double riftOpen) {
        /** Width (in region noise) of the ramp from a biome's border to its landform at full strength. */
        private static final double RAMP = 0.07;

        static Regions of(VolcanicSettings s, double h, double g, double core, double fade) {
            double hot = VolcanicMath.smoothstep(s.hot() - 0.02, s.hot() + 0.02, h);
            double warmish = VolcanicMath.smoothstep(s.cool() - 0.02, s.cool() + 0.02, h);
            double mid = warmish * (1.0 - hot);
            double mineral = 1.0 - VolcanicMath.smoothstep(-0.02, 0.02, g);
            // Seabed and titan shapes ramp up only inside their biome rectangles, so the slopes stay in the biome.
            double coolInside = 1.0 - VolcanicMath.smoothstep(s.cool() - 0.09, s.cool() - 0.01, h);
            double seabedRaw = coolInside * (1.0 - VolcanicMath.smoothstep(s.seabedGround() - 0.09, s.seabedGround() - 0.01, g));
            double titanRaw = coolInside * VolcanicMath.smoothstep(s.titanGround() + 0.01, s.titanGround() + 0.07, g);
            double cool = 1.0 - warmish;
            double seabedBand = cool * (1.0 - VolcanicMath.smoothstep(s.seabedGround() - 0.02, s.seabedGround() + 0.02, g));
            double titanBand = cool * VolcanicMath.smoothstep(s.titanGround() - 0.02, s.titanGround() + 0.02, g);
            double ashen = Math.max(0.0, cool - seabedBand - titanBand);

            double r = RAMP;
            double inHot = VolcanicMath.smoothstep(s.hot(), s.hot() + r, h);
            double inMid = VolcanicMath.smoothstep(s.cool(), s.cool() + r, h) * (1.0 - VolcanicMath.smoothstep(s.hot() - r, s.hot(), h));
            double inCool = 1.0 - VolcanicMath.smoothstep(s.cool() - r, s.cool(), h);
            double gMineral = 1.0 - VolcanicMath.smoothstep(-r, 0.0, g);
            double gGrown = VolcanicMath.smoothstep(0.0, r, g);
            double seabedS = inCool * (1.0 - VolcanicMath.smoothstep(s.seabedGround() - r, s.seabedGround(), g));
            double titanS = inCool * VolcanicMath.smoothstep(s.titanGround(), s.titanGround() + r, g);
            double ashenS = inCool * VolcanicMath.smoothstep(s.seabedGround(), s.seabedGround() + r, g)
                    * (1.0 - VolcanicMath.smoothstep(s.titanGround() - r, s.titanGround(), g));
            // Rifts stay out of the seabed, titan and sulfur rectangles, fading over a ramp on both sides of the border.
            double coolWide = 1.0 - VolcanicMath.smoothstep(s.cool() - r, s.cool() + r, h);
            double seabedWide = coolWide * (1.0 - VolcanicMath.smoothstep(s.seabedGround() - r, s.seabedGround() + r, g));
            double titanWide = coolWide * VolcanicMath.smoothstep(s.titanGround() - r, s.titanGround() + r, g);
            double sulfurWide = VolcanicMath.smoothstep(s.cool() - r, s.cool() + r, h) * (1.0 - VolcanicMath.smoothstep(s.hot() - r, s.hot() + r, h))
                    * (1.0 - VolcanicMath.smoothstep(-r, r, g));
            double riftOpen = (1.0 - Math.max(seabedWide, Math.max(titanWide, sulfurWide))) * fade;
            return new Regions(h, g, hot, mid, hot * mineral, hot * (1.0 - mineral), mid * mineral, mid * (1.0 - mineral),
                    ashen, seabedRaw * core, titanRaw * core, core, fade,
                    inHot * gMineral * fade, inHot * gGrown * fade, inMid * gMineral * fade, inMid * gGrown * fade,
                    ashenS * fade, seabedS * fade, titanS * fade, riftOpen);
        }
    }

    // ---------------------------------------------------------------- plain

    /**
     * Height of the smooth plain (everything except volcanoes, rivers and the detail of the landforms): the ground the
     * volcanoes stand on, so ridges and hills run up their flanks.
     */
    double plain(double x, double z, Regions r) {
        double swell = noise(plains, x, z);
        double wave = noise(rolling, x, z);
        double offset = Ring3Relief.EDGE_OFFSET;
        if (r.fade() > 0.0) {
            double highAmp = 1.0 - 0.6 * r.sulfurS() - 0.4 * r.titanS();
            offset += r.fade() * (relief.highlandOffset(x, z) - Ring3Relief.EDGE_OFFSET) * highAmp;
        }
        // Each biome sits at its own level: the peak range high, the sulfur basin and the ember lowland low.
        offset += 35.0 * r.peaksS() + 10.0 * r.flowsS() - 6.0 * r.emberS() - 14.0 * r.sulfurS();
        double base = settings.plainY() + settings.plainAmp() * swell + offset;
        base += settings.rollingAmp() * wave * (1.0 - 0.5 * r.sulfurS() - 0.5 * r.ashenS());
        double hill = noise(rolling, x * 0.45 + 1733.3, z * 0.45 - 911.7);
        if (hill > 0.0) {
            // Hills everywhere, strongest in the peak range, soft in the ember lowland, few between the dunes and terraces.
            double share = 0.55 + 0.45 * r.peaksS() - 0.05 * r.emberS() - 0.3 * r.ashenS() - 0.4 * r.sulfurS()
                    - 0.15 * r.flowsS() - 0.2 * r.titanS();
            base += HILL_HEIGHT * share * Math.pow(hill, 1.4);
        }
        double crest = 1.0 - Math.abs(noise(flows, x * 0.6 - 377.1, z * 0.6 + 2011.9));
        base += RIDGE_HEIGHT * (1.0 - r.flows()) * (1.0 - r.ashenS()) * Math.pow(Math.max(0.0, crest - 0.7) / 0.3, 2.0);
        if (r.flows() > 0.0) {
            // Stepped lobes of old flows with steep fronts.
            base += r.flows() * 5.0 * VolcanicMath.stair(0.5 + 0.9 * noise(flows, x, z), 3, 0.35);
        }
        if (r.peaksS() > 0.0) {
            base += PEAK_RIDGE_HEIGHT * r.peaksS() * relief.peakRidges(x, z);
        }
        if (base > PLAIN_SOFT_CAP) {
            // The peak range tops out around Y 200–240, so the cones on it stay below the great volcanoes.
            base = PLAIN_SOFT_CAP + 0.5 * (base - PLAIN_SOFT_CAP);
        }
        if (r.titan() > 0.0) {
            double flat = settings.plainY() + offset - 2.0 + 0.4 * (base - settings.plainY() - offset);
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
            if (r.seabedS() > 0.0) {
                floor += 6.0 * r.seabedS() * relief.reefRidges(x, z);
            }
            base = Mth.lerp(VolcanicMath.stair(r.seabed(), 3, 0.4), base, floor);
        }
        return base;
    }

    // ---------------------------------------------------------------- volcanoes

    /** The volcano of grid cell (i, j), or null. Pure ({@link VolcanoPlacement}). */
    Volcano place(int i, int j) {
        return VolcanoPlacement.place(this, i, j);
    }

    /** Island cluster above a lava river in island cell (i, j), or none. Pure. */
    List<Volcano.Island> channelCluster(int i, int j) {
        return VolcanoPlacement.channelCluster(this, i, j);
    }

    NoiseHolder hashNoise() {
        return hash;
    }

    RingLayout ringLayout() {
        return layout.get();
    }

    /** The scratch column of the island probes (one per thread). */
    Column probeColumn() {
        return probeScratch.get();
    }

    // ---------------------------------------------------------------- rivers and cinder cones (39d)

    /** A pure per-world value for the hash grids ({@link CellShapes}), from the hash noise. */
    long salt() {
        Long known = decisions.salt;
        if (known == null) {
            // Pure: racing threads compute the same value; the boxed holder is never seen half-written.
            known = SeededHash.hash(hash, 12345, -6789, WORLD_SALT);
            decisions.salt = known;
        }
        return known;
    }

    double warpAt(double x, double z) {
        return noise(warp, x, z);
    }

    /** The smooth plain at (x, z) (no volcanoes, landforms or rivers). */
    double plainAt(double x, double z) {
        return plain(x, z, regions(x, z));
    }

    /** The ground a river follows (everything except rivers and cinder cones). */
    double traceGround(double x, double z) {
        return traceColumn(x, z).preHeight;
    }

    /** The column without rivers and cinder cones (what a river is traced over). */
    Column traceColumn(double x, double z) {
        return traceColumn(x, z, new Column());
    }

    /** {@link #traceColumn(double, double)} into a reused column. */
    Column traceColumn(double x, double z, Column into) {
        fill(into, Mth.floor(x), Mth.floor(z), Mode.TRACE);
        return into;
    }

    /** Whether a river may flow on at (x, z): not into the seabed or the titan fields, not into a transition zone. */
    boolean riverMayFlow(double x, double z) {
        Regions r = regions(x, z);
        return r.core() > 0.6 && r.seabed() < 0.3 && r.titan() < 0.5;
    }

    /** The rivers of volcano cell (i, j), traced once per world. */
    List<LavaRivers.River> riversOf(int i, int j) {
        long key = (long) i << 32 | j & 0xFFFFFFFFL;
        // A per-key future: the map only stores it (cheap, inside the map's lock), the tracing runs outside; a thread
        // asking at the same time waits for the first one. Tracing reads only other maps (volcanoes, abysses).
        return once(decisions.rivers, key, () -> LavaRivers.trace(this, i, j));
    }

    private LavaRivers.Tile riverTileAt(int ti, int tj) {
        long key = (long) ti << 32 | tj & 0xFFFFFFFFL;
        if (decisions.riverTiles.size() > Decisions.LIMIT) {
            decisions.riverTiles.clear();
        }
        return once(decisions.riverTiles, key, () -> LavaRivers.tile(this, ti, tj));
    }

    /** The value of a per-key future, computed by the first thread that asks (outside the map's locks). */
    private static <T> T once(Map<Long, java.util.concurrent.FutureTask<T>> map, long key, java.util.concurrent.Callable<T> work) {
        java.util.concurrent.FutureTask<T> task = map.get(key);
        if (task == null) {
            task = map.computeIfAbsent(key, k -> new java.util.concurrent.FutureTask<>(work));
        }
        task.run();
        try {
            return task.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        }
    }

    /** The nearest river around (x, z) ({@link LavaRivers#nearest}); false if none is near. */
    boolean nearestRiver(double x, double z, double[] out) {
        LavaRivers.Tile tile = tileFor(x, z);
        return tile.segments().length > 0 && LavaRivers.nearest(tile, x, z, out);
    }

    /** The river tile around (x, z), remembered per thread. */
    LavaRivers.Tile tileFor(double x, double z) {
        int ti = Math.floorDiv(Mth.floor(x), LavaRivers.TILE);
        int tj = Math.floorDiv(Mth.floor(z), LavaRivers.TILE);
        RiverTile cached = riverTile.get();
        if (!cached.filled || cached.ti != ti || cached.tj != tj) {
            cached.tile = riverTileAt(ti, tj);
            cached.ti = ti;
            cached.tj = tj;
            cached.filled = true;
        }
        return cached.tile;
    }

    /** 0..1: where cinder cone fields lie (clusters of a few kilometres). */
    double cinderFieldAt(double x, double z) {
        return VolcanicMath.smoothstep(0.08, 0.3, noise(warp, x * 0.3 + 7000.5, z * 0.3 - 7000.5));
    }

    /** Whether a cinder cone of foot radius {@code radius} may stand at (x, z): clear of volcanoes, rivers and rifts. */
    boolean cinderMayStand(double x, double z, double radius) {
        int size = settings.cellSize();
        int ci = Math.floorDiv(Mth.floor(x), size);
        int cj = Math.floorDiv(Mth.floor(z), size);
        for (int di = -1; di <= 1; di++) {
            for (int dj = -1; dj <= 1; dj++) {
                Volcano v = volcano(ci + di, cj + dj);
                if (v != null) {
                    double dx = x - v.x();
                    double dz = z - v.z();
                    if (Math.sqrt(dx * dx + dz * dz) < v.radius() + radius + 24.0) {
                        return false;
                    }
                }
            }
        }
        double[] river = new double[3];
        for (int k = 0; k <= 8; k++) {
            double px = k == 8 ? x : x + Math.cos(k * Math.PI / 4.0) * radius;
            double pz = k == 8 ? z : z + Math.sin(k * Math.PI / 4.0) * radius;
            if (nearestRiver(px, pz, river) && (river[0] - 1.0) * river[2] < 16.0) {
                return false;
            }
        }
        Ring3Relief.Detail detail = new Ring3Relief.Detail();
        return relief.rift(x, z, 40.0 * noise(warp, x, z), detail) < 2.0;
    }

    /** Whether no volcano's foot comes within {@code distance} of (x, z) (beyond its radius). */
    boolean clearOfVolcanoes(double x, double z, double distance) {
        int size = settings.cellSize();
        int ci = Math.floorDiv(Mth.floor(x), size);
        int cj = Math.floorDiv(Mth.floor(z), size);
        int reach = 1 + (int) Math.ceil(distance / size);
        for (int di = -reach; di <= reach; di++) {
            for (int dj = -reach; dj <= reach; dj++) {
                Volcano v = volcano(ci + di, cj + dj);
                if (v != null && Math.hypot(x - v.x(), z - v.z()) < v.radius() + distance) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Whether a shape of {@code radius} at (x, z) comes within 12 blocks of an abyss (in the 3 × 3 abyss cells around it). */
    boolean nearAbyss(double x, double z, double radius) {
        int ci = Math.floorDiv(Mth.floor(x), Landmarks.ABYSS_CELL);
        int cj = Math.floorDiv(Mth.floor(z), Landmarks.ABYSS_CELL);
        for (int di = -1; di <= 1; di++) {
            for (int dj = -1; dj <= 1; dj++) {
                Landmarks.Abyss a = abyss(ci + di, cj + dj);
                if (a != null && Math.abs(a.along(x, z)) <= a.length() * 0.5 + radius + 12.0
                        && Math.abs(a.normal(x, z)) <= a.width() + radius + 12.0) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Whether a lava river's edge comes within {@code distance} of (x, z). */
    boolean riverWithin(double x, double z, double distance) {
        double[] river = new double[3];
        return nearestRiver(x, z, river) && (river[0] - 1.0) * river[2] < distance;
    }

    /** The cut of a rift valley at (x, z) (blocks, already weighted by where rifts may lie). */
    double riftCut(double x, double z, Ring3Relief.Detail detail) {
        Regions r = regions(x, z);
        if (r.riftOpen() <= 0.0) {
            return 0.0;
        }
        return r.riftOpen() * relief.rift(x, z, 40.0 * noise(warp, x, z), detail);
    }

    /** A pure decision for one grid cell (a static method of the shape's class). */
    @FunctionalInterface
    interface CellDecision<T> {
        @Nullable
        T decide(VolcanicField field, int i, int j);
    }

    /**
     * The decision of cell (i, j): from the per-thread memo of the last cell (most columns ask for the same one), else
     * from the per-world map, else made now.
     */
    @Nullable
    private <T> T cell(ThreadOwned<CellMemo<T>> memo, Map<Long, Optional<T>> map, int i, int j, CellDecision<T> decision) {
        CellMemo<T> last = memo.get();
        if (last.filled && last.i == i && last.j == j) {
            return last.value;
        }
        long key = (long) i << 32 | j & 0xFFFFFFFFL;
        Optional<T> known = map.get(key);
        if (known == null) {
            known = Optional.ofNullable(decision.decide(this, i, j));
            decisions.remember(map, key, known);
        }
        last.i = i;
        last.j = j;
        last.value = known.orElse(null);
        last.filled = true;
        return last.value;
    }

    Landmarks.Needle needle(int i, int j) {
        return cell(needleMemo, decisions.needles, i, j, Landmarks::needle);
    }

    Landmarks.Arch riftArch(int i, int j) {
        return cell(riftArchMemo, decisions.riftArches, i, j, Landmarks::riftArch);
    }

    Landmarks.FloatingVolcano floating(int i, int j) {
        return cell(floatingMemo, decisions.floating, i, j, Landmarks::floating);
    }

    private Landmarks.Abyss abyss(int i, int j) {
        return cell(abyssMemo, decisions.abysses, i, j, Landmarks::abyss);
    }

    /** The floating volcano of cell (i, j), or null (for tests and the landmark list). */
    Landmarks.FloatingVolcano floatingVolcano(int i, int j) {
        return floating(i, j);
    }

    /** The abyss of cell (i, j), or null (for tests and the landmark list). */
    Landmarks.Abyss abyssAt(int i, int j) {
        return abyss(i, j);
    }

    CinderCones.Cone cinder(int i, int j) {
        long key = (long) i << 32 | j & 0xFFFFFFFFL;
        Optional<CinderCones.Cone> known = decisions.cinders.get(key);
        if (known == null) {
            known = Optional.ofNullable(CinderCones.place(this, i, j));
            decisions.remember(decisions.cinders, key, known);
        }
        return known.orElse(null);
    }

    TitanBones.Rib rib(int i, int j) {
        return cell(ribMemo, decisions.ribs, i, j, TitanBones::rib);
    }

    TitanBones.Cage cage(int i, int j) {
        return cell(cageMemo, decisions.cages, i, j, TitanBones::cage);
    }

    // ---------------------------------------------------------------- columns

    /** What a column fill computes: everything, the height (no islands or thin shapes), or the ground a river follows. */
    enum Mode {
        FULL, HEIGHT, TRACE
    }

    private double coreHeightUncached(double x, double z) {
        Column column = heightScratch.get();
        fill(column, Mth.floor(x), Mth.floor(z), Mode.HEIGHT);
        return column.height;
    }

    private void fill(Column column, int x, int z) {
        fill(column, x, z, Mode.FULL);
    }

    /** Computes a column (see {@link Mode}). */
    void fill(Column c, int x, int z, Mode mode) {
        double px = x + 0.5;
        double pz = z + 0.5;
        Regions r = regions(px, pz);
        double plain = plain(px, pz, r);

        c.reset();
        boolean full = mode == Mode.FULL;
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
                // Towards the crest the volcano stands on the plain at its centre, so the rim (and a lava lake) stays
                // level whatever hills or ridges run under the flank.
                double anchor = 1.0 - VolcanicMath.smoothstep(v.crest(), v.radius(), dist);
                double footing = plain + anchor * (v.plain() - plain);
                double delta = v.surface(dist, angle, footing, 3.5 * warpZ) - plain;
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

        // The landforms, faded out on volcano flanks.
        double keep = 1.0 - influence;
        double detail = 0.0;
        double cliff = 0.0;
        double gullyFloor = 0.0;
        double riftStrength = 0.0;
        double plateau = 0.0;
        double tube = 0.0;
        Ring3Relief.Detail d = c.scratch;
        if (keep > 0.0 && r.fade() > 0.0) {
            if (r.ashenS() > 0.0) {
                relief.dunes(px, pz, d);
                detail += r.ashenS() * d.height;
                cliff = Math.max(cliff, 0.9 * r.ashenS() * d.cliff);
                gullyFloor = r.ashenS() * d.floor;
            }
            if (r.sulfurS() > 0.0) {
                long salt = salt();
                detail += r.sulfurS() * (relief.terraces(px, pz) + CellShapes.mound(salt, px, pz) - CellShapes.sinkhole(salt, px, pz));
            }
            if (r.flowsS() > 0.0) {
                double level = relief.plateauLevel(px, pz);
                plateau = r.flowsS() * Ring3Relief.PLATEAU_STEP * level;
                detail += plateau;
                double f = level - Math.floor(level);
                cliff = Math.max(cliff, r.flowsS() * VolcanicMath.smoothstep(0.0, 0.12, f) * (1.0 - VolcanicMath.smoothstep(0.88, 1.0, f)));
                d.reset();
                tube = r.flowsS() > 0.3 ? relief.lavaTube(px, pz, d) : 0.0;
                if (tube > 0.0) {
                    double wall = VolcanicMath.smoothstep(0.0, 0.45, tube);
                    detail -= r.flowsS() * 12.0 * wall * d.floor;
                    cliff = Math.max(cliff, d.floor * (1.0 - wall) * 2.0 * r.flowsS());
                    if (d.floor >= 0.5) {
                        tube = 0.0; // collapsed: a trench, no tunnel
                    }
                }
            }
            if (r.riftOpen() > 0.0) {
                double cut = relief.rift(px, pz, 40.0 * warpX, d);
                riftStrength = r.riftOpen() * d.floor;
                if (cut > 0.0) {
                    detail -= r.riftOpen() * cut;
                    cliff = Math.max(cliff, r.riftOpen() * d.cliff);
                }
            }
        }
        double cinder = 0.0;
        if (mode != Mode.TRACE && r.core() > 0.0) {
            CinderCones.Cone[] cones = cinderNeighbourhood.get().around(this, Math.floorDiv(x, CinderCones.CELL),
                    Math.floorDiv(z, CinderCones.CELL)).cones.clone();
            for (CinderCones.Cone cone : cones) {
                if (cone == null) {
                    continue;
                }
                double dx = px - cone.x();
                double dz = pz - cone.z();
                if (Math.abs(dx) >= cone.reach() || Math.abs(dz) >= cone.reach()) {
                    continue;
                }
                double h = cone.at(Math.sqrt(dx * dx + dz * dz));
                if (h > cinder) {
                    cinder = h;
                }
            }
            cinder *= keep;
        }
        // Micro relief (39d): a fine 2D wobble that breaks the straight one-block contour lines of smooth slopes, much
        // cheaper than 3D crags on the whole plain.
        double micro = r.fade() > 0.0 ? MICRO_RELIEF * r.fade() * keep * relief.microRelief(px, pz) : 0.0;
        double preHeight = plain + raise + sink + keep * detail + cinder + micro;
        c.rise = raise + cinder;
        c.cliff = Math.min(1.0, keep * cliff);
        Landmarks.Abyss abyss = r.core() > 0.0
                ? abyss(Math.floorDiv(x, Landmarks.ABYSS_CELL), Math.floorDiv(z, Landmarks.ABYSS_CELL)) : null;
        double chasm = abyss == null ? 2.0 : abyss.across(px, pz);
        if (chasm < 1.0) {
            // Stepped walls almost straight down to a lava sea near Y 0.
            double wall = VolcanicMath.stair(VolcanicMath.smoothstep(0.55, 1.0, chasm), 4, 0.3);
            preHeight = Math.min(preHeight, Landmarks.ABYSS_FLOOR + wall * (preHeight - Landmarks.ABYSS_FLOOR));
            c.cliff = Math.max(c.cliff, chasm > 0.5 ? 1.0 : 0.0);
            c.rise = 0.0;
        }
        if (mode == Mode.TRACE) {
            c.preHeight = preHeight;
            c.height = preHeight;
            return;
        }

        // The abyss's lava sea comes first; a river pouring into it ends there.
        if (chasm < 1.0 && !c.hasLava()) {
            c.lavaTop = Landmarks.ABYSS_LAVA;
            c.lavaBottom = Landmarks.ABYSS_FLOOR - 12;
        }

        // Lava rivers.
        double height = preHeight;
        double riverNear = 0.0;
        if (r.core() > 0.0) {
            double[] river = c.river;
            if (c.calderaMask <= 0.0 && nearestRiver(px, pz, river)) {
                double along = river[0];
                int level = Mth.floor(river[1]);
                double half = river[2];
                // Levees: lower ground beside a river is raised to its banks, so the lava never stands against air.
                double levee = 1.0 - VolcanicMath.smoothstep(1.3, 3.0, along);
                if (levee > 0.0 && preHeight < level + CHANNEL_BANK) {
                    preHeight += levee * (level + CHANNEL_BANK - preHeight);
                }
                double profile = along >= 1.0 ? 0.0 : 1.0 - VolcanicMath.smoothstep(0.55, 1.0, along);
                double bed = level - CHANNEL_DEPTH;
                height = preHeight - profile * Math.max(0.0, preHeight - bed);
                c.channel = profile;
                riverNear = 1.0 - VolcanicMath.smoothstep(2.5, 4.0, along);
                if (along < 1.0 + BANK_BLOCKS / half) {
                    c.lavaBank = along < 1.0 ? 1.0 : Mth.clamp(1.0 - (along - 1.0) * half / BANK_BLOCKS, 0.0, 1.0);
                }
                if (along < 1.0 && !c.hasLava()) {
                    c.lavaTop = level;
                    c.lavaBottom = level - CHANNEL_DEPTH - 3;
                }
                if (along < 1.8) {
                    c.guardBottom = Math.min(c.guardBottom, level - 12);
                    c.guardTop = Math.max(c.guardTop, level + 4);
                }
            }
        }
        if (chasm < 1.3) {
            c.guardBottom = Math.min(c.guardBottom, Landmarks.ABYSS_FLOOR - 14);
            c.guardTop = Math.max(c.guardTop, Landmarks.ABYSS_LAVA + 6);
        }
        c.preHeight = preHeight;
        c.height = height;
        c.floorMargin = floorMargin(r, keep, riftStrength, chasm, height, riverNear);
        c.roughness = Math.max(c.roughness, 0.9 * c.cliff);
        if (cinder > 4.0) {
            c.roughness = Math.max(c.roughness, 0.5 * (1.0 - riverNear));
        }
        if (c.roughness < MIN_ROUGHNESS) {
            // The crags (a 3D noise per cell corner) are only worth their cost where they show: 0 skips them entirely.
            c.roughness = 0.0;
        }

        if (full) {
            if (keep > 0.0 && r.fade() > 0.0) {
                ThinShapes.thinShapes(this, c, px, pz, r, keep, gullyFloor, plateau, tube, height);
            }
            ThinShapes.landmarks(this, c, x, z, px, pz, r, height, around, abyss, chasm, warpX);
            Neighbourhood islandCells = channelNeighbourhood.get().aroundIslands(this, Math.floorDiv(x, VolcanoPlacement.ISLAND_CELL), Math.floorDiv(z, VolcanoPlacement.ISLAND_CELL));
            for (List<Volcano.Island> cell : islandCells.islands) {
                addIslands(c, cell, px, pz, warpX);
            }
        }
    }

    /**
     * How far the surface may drop within ~18 blocks of a column through a cut narrower than the sampling of
     * {@link #initialHeight}: the abyss down to its floor, rift valleys, sinkholes, lava tubes and plateau risers,
     * gullies between the dunes, river beds.
     */
    private static double floorMargin(Regions r, double keep, double riftStrength, double chasm, double height, double riverNear) {
        double margin = riverNear > 0.0 ? 4.0 : 0.0;
        if (chasm < 1.6) {
            margin = Math.max(margin, height - Landmarks.ABYSS_FLOOR + 2.0);
        }
        if (keep > 0.0) {
            if (riftStrength > 0.0) {
                margin = Math.max(margin, (20.0 + 20.0 * riftStrength) * riftStrength + 2.0);
            }
            if (r.sulfurS() > 0.0) {
                margin = Math.max(margin, 17.0);
            }
            if (r.flowsS() > 0.0) {
                margin = Math.max(margin, 14.0);
            }
            if (r.ashenS() > 0.0) {
                margin = Math.max(margin, 14.0);
            }
        }
        return margin;
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
        /** Height of the volcanoes (and cinder cones) above the plain (before the craters sink in), in blocks. */
        double rise;
        /** 0..1: a lava river, 1 over the lava, falling to 0 over {@link #BANK_BLOCKS} beside it (the banks). */
        double lavaBank;
        /** 0..1: a cliff or steep wall of a landform (plateau risers, rift flanks, gully walls, sea stacks). */
        double cliff;
        /** How far the surface may drop near this column through a narrow cut (for {@link #initialHeight}). */
        double floorMargin;
        /** The kind of thin solid standing in the column ({@link Landmarks#KIND_BONE} …), 0 for none. */
        double solidKind;
        /** A lava spring (a source that flows) at this Y, or {@link Integer#MIN_VALUE}. */
        int springY;
        /** Lava fills open space from {@link #lavaBottom} up to, not including, {@link #lavaTop}. */
        int lavaTop;
        int lavaBottom;
        int guardBottom;
        int guardTop;
        int islandCount;
        final double[] islandTop = new double[MAX_ISLANDS];
        final double[] islandBottom = new double[MAX_ISLANDS];
        int solidCount;
        final int[] solidTop = new int[MAX_SOLIDS];
        final int[] solidBottom = new int[MAX_SOLIDS];
        /** A carved opening (lava tube) from {@link #voidBottom} to {@link #voidTop}, inclusive. */
        int voidBottom;
        int voidTop;
        // Scratch space of the fill (no allocation per column).
        final Ring3Relief.Detail scratch = new Ring3Relief.Detail();
        final double[] river = new double[3];
        final double[] span = new double[3];

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
            cliff = 0.0;
            solidKind = 0.0;
            floorMargin = 0.0;
            springY = Integer.MIN_VALUE;
            lavaTop = Integer.MIN_VALUE;
            lavaBottom = Integer.MIN_VALUE;
            guardBottom = Integer.MAX_VALUE;
            guardTop = Integer.MIN_VALUE;
            islandCount = 0;
            solidCount = 0;
            voidBottom = Integer.MAX_VALUE;
            voidTop = Integer.MIN_VALUE;
        }

        void addIsland(Volcano.Island island, double x, double z, double warp) {
            double d = island.distance(x, z, 0.18 * warp);
            if (d >= 1.0 || islandCount == MAX_ISLANDS) {
                return;
            }
            addIslandInterval(island.topAt(d), island.bottomAt(d), d);
            if (island.springs() > 0 && island.springAt(Mth.floor(x), Mth.floor(z))) {
                // The source sits on the island's top; it is scheduled to flow (RingAquifer) and pours off the edge.
                setSpring(Mth.floor(island.topAt(d)) + 1);
            }
        }

        /**
         * A lava spring at {@code y}: the islands are interpolated (their top may lie a block off the exact shape), so the
         * spring stands on a thin solid of its own with a thin opening at its block, which the aquifer then fills.
         */
        void setSpring(int y) {
            springY = y;
            addSolid(y - 1, y - 1, 0);
            voidBottom = y;
            voidTop = y;
        }

        void addIslandInterval(double top, double bottom, double d) {
            if (islandCount == MAX_ISLANDS) {
                return;
            }
            islandTop[islandCount] = top;
            islandBottom[islandCount] = bottom;
            islandCount++;
            islandMask = Math.max(islandMask, Mth.clamp((1.0 - d) * 4.0, 0.0, 1.0));
        }

        void addSolid(double bottom, double top) {
            addSolid(bottom, top, 0);
        }

        /**
         * A thin solid from {@code bottom} to {@code top}; {@code kind} (bone, obsidian, basalt) colours its surface, but
         * only where it stands on the ground: the surface rules see the whole column, so under an arch the ground keeps
         * its own surface.
         */
        void addSolid(double bottom, double top, int kind) {
            // Thin shapes bypass the stack's top slide (300 → 316): keep them below it.
            top = Math.min(top, THIN_TOP);
            if (solidCount == MAX_SOLIDS || top < bottom) {
                return;
            }
            solidBottom[solidCount] = Mth.floor(bottom);
            solidTop[solidCount] = Mth.floor(top);
            solidCount++;
            if (bottom <= height + 1.0) {
                solidKind = Math.max(solidKind, kind);
            }
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

        /** Bottom of the lava (inclusive), or {@link Integer#MIN_VALUE} without lava. */
        public int lavaBottom() {
            return lavaBottom;
        }

        /** The Y of the column's lava spring (a source that flows, 39d), or {@link Integer#MIN_VALUE}. */
        public int springY() {
            return springY;
        }

        boolean hasLava() {
            return lavaTop != Integer.MIN_VALUE;
        }

        /** Whether a floating island (or the floating volcano) is solid at {@code y} in this column. */
        public boolean islandAt(int y) {
            for (int i = 0; i < islandCount; i++) {
                if (y >= islandBottom[i] && y <= islandTop[i] - 1.0) {
                    return true;
                }
            }
            return false;
        }

        /** Whether lava fills open space at {@code y} in this column (a lake, river, abyss sea, or a spring). */
        public boolean lavaAt(int y) {
            return lavaAt(y, lavaTop, lavaBottom, springY);
        }

        /** The lava rule of a column from its bounds (shared with the aquifer's per-column table). */
        public static boolean lavaAt(int y, int lavaTop, int lavaBottom, int springY) {
            return y < lavaTop && y >= lavaBottom || y == springY;
        }

        /** Whether the lava at {@code y} is a spring: a source that is scheduled to flow (39d). */
        public boolean springAt(int y) {
            return y == springY;
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

    List<Volcano.Island> channelIslands(int i, int j) {
        long key = (long) i << 32 | j & 0xFFFFFFFFL;
        List<Volcano.Island> known = decisions.channelIslands.get(key);
        if (known == null) {
            known = channelCluster(i, j);
            decisions.remember(decisions.channelIslands, key, known);
        }
        return known;
    }


}
