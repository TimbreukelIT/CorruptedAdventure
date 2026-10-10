package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.worldgen.feature.FireGeodeShape;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicCaches.CellMemo;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicCaches.CinderNeighbourhood;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicCaches.Decisions;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicCaches.Neighbourhood;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicCaches.RiverTile;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicCaches.TubeTile;

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
    /**
     * Width (blocks) beside a river's channel that features keep clear of ({@link Column#lavaBank}; the channel itself,
     * as wide as the river, comes on top).
     */
    private static final double BANK_BLOCKS = 3.0;
    /**
     * Width (blocks) of the scorched shore of the surface from the lava's edge, whatever the river's width (39g; the
     * dry slope of a wide channel used to be shore too).
     */
    private static final double SHORE_BLOCKS = 3.0;
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
    private static final int MAX_SOLIDS = 8;
    private static final int MAX_VOIDS = 8;
    private static final int MAX_PILLARS = 2;
    /**
     * Openings with walls one column may hold: six for the lava tubes, skylights and magma chambers (42), one for a
     * collapse's shaft (42b), which comes first.
     */
    private static final int MAX_CAVES = 7;
    /** Vanilla caves are gone from here up (42: no cave mouths under the floating islands, Y ~150-210). */
    private static final double CAVE_CEILING = 146.0;
    /** Vanilla caves are gone in a volcano's body from this far above the plain it stands on (42). */
    private static final double CAVE_VOLCANO_HEIGHT = 4.0;
    /** Over this many blocks below those heights the caves shrink to nothing (42). */
    private static final double CAVE_FADE = 20.0;
    /** The lift that closes every vanilla cave (their densities stay above about −1.5). */
    private static final double CAVE_LIFT = 2.0;
    /** The band around a lava river reaches at least this far (blocks) past the lava's edge (42b). */
    private static final double RIVER_GUARD = 6.0;
    /**
     * The band around the abyss's lava sea reaches at least this far past its edge (42c): the block beside the lava plus
     * the diagonal of a cell of the terrain's interpolation (5.7), so every cell corner a cave beside the lava is
     * interpolated from lies in the band.
     */
    private static final double ABYSS_GUARD = 8.0;
    /** A cut at least this deep gets walls of ring 3 rock below the surface's own package (42b). */
    private static final double DEEP_CUT = 8.0;
    /** Separate lava ranges and guard bands one column may hold (42). */
    private static final int MAX_BANDS = 6;
    /** Highest top of a thin shape (below the top slide of the ring 3 stack). */
    static final double THIN_TOP = 296.0;
    private static final double ISLAND_CLEARANCE = 10.0;
    private static final double[] INITIAL_RADII = {6.0, 12.0, 18.0};
    /** Warp (blocks) of a collapse's outline, from the column's warp noise. */
    private static final double COLLAPSE_WARP = 3.0;

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
    private final ThreadOwned<TubeTile> tubeTile = new ThreadOwned<>(TubeTile::new);
    private final ThreadOwned<CellMemo<FireGeodeShape.Geode>> geodeMemo = new ThreadOwned<>(CellMemo::new);
    // Scratch columns of the uncached fills (core heights, island probes); one per use, so they never nest.
    private final ThreadOwned<Column> heightScratch = new ThreadOwned<>(Column::new);
    private final ThreadOwned<Column> probeScratch = new ThreadOwned<>(Column::new);
    private final ThreadOwned<CellMemo<Landmarks.Needle>> needleMemo = new ThreadOwned<>(CellMemo::new);
    private final ThreadOwned<CellMemo<Landmarks.Arch>> riftArchMemo = new ThreadOwned<>(CellMemo::new);
    private final ThreadOwned<CellMemo<Landmarks.FloatingVolcano>> floatingMemo = new ThreadOwned<>(CellMemo::new);
    private final ThreadOwned<CellMemo<CellShapes.Collapse>> collapseMemo = new ThreadOwned<>(CellMemo::new);
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

    /**
     * {@code function} as a {@code volcanic_field}, looking through holder references, markers and ring gates; null if it
     * is none.
     */
    static VolcanicField resolve(DensityFunction function) {
        DensityFunction current = function;
        for (int depth = 0; depth < 16 && current != null; depth++) {
            if (current instanceof VolcanicField field) {
                return field;
            }
            current = RingGated.unwrap(current);
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

    /**
     * +1 where caves must stay out (rock around a lava lake or river; 42: around magma chambers, lava gutters and fire
     * geodes), −1 elsewhere.
     */
    double guard(int x, int y, int z) {
        return column(x, z).guard(y);
    }

    /**
     * 42: what the stack adds to the density of vanilla's caves ({@link Column#caveLift}), 0 where they run freely, up
     * to {@link #CAVE_LIFT} where they are gone: they shrink and close gradually towards a volcano's body and towards
     * {@link #CAVE_CEILING}.
     */
    double caveLift(int x, int y, int z) {
        return column(x, z).caveLift(y);
    }

    /** 42b: 1 where a block may be part of the wall of a deep cut ({@link Column#deepWall}), else 0; for the surface. */
    double deepWall(int x, int y, int z) {
        return column(x, z).deepWall(y) ? 1.0 : 0.0;
    }

    /** 42: which wall of a lava tube or magma chamber a block belongs to ({@link Column#caveWall}), for the surface. */
    double caveWall(int x, int y, int z) {
        return column(x, z).caveWall(y);
    }

    /**
     * 42: −1 inside the cavity of a fire geode where the terrain really dug it, +10 elsewhere (output
     * {@code fire_geodes}). The cavity can be missing where another shape takes the column (a thin solid, a full list of
     * openings), so the lining of 41b lines the open blocks of {@link #fireGeode} only.
     */
    double fireGeodes(int x, int y, int z) {
        FireGeodeShape.Geode geode = fireGeodeCell(Math.floorDiv(x, FireGeodeShape.CELL), Math.floorDiv(z, FireGeodeShape.CELL));
        return geode != null && geode.inCavity(x, y, z) && thin(x, y, z) < 0.0 ? -1.0 : 10.0;
    }

    /**
     * The thin shapes at a block, outside the terrain interpolation: +1 inside a thin solid (hoodoo, rib, column prism,
     * sea stack, needle, arch, bridge, pillar, rubble, the micro relief's raised blocks) or a floating island (39f: exact
     * per block, no longer interpolated), −1 inside a carved opening (a lava tube's tunnel, a collapse's overhang or
     * shaft), 0 elsewhere (the terrain decides). One column lookup per block.
     */
    double thin(int x, int y, int z) {
        return column(x, z).thin(y);
    }

    /** The kind of thin solid at a block ({@link Landmarks#KIND_BIOME} …), or −1 outside one (39f, for the surface). */
    double thinKind(int x, int y, int z) {
        return column(x, z).thinKind(y);
    }

    /**
     * 0..1 per block (39f): a pattern that changes with every block of height and slowly across the plain, so a threshold
     * picks patches whose edges follow the steps of the relief (the ember woods' ground).
     */
    double tread(int x, int y, int z) {
        Column c = column(x, z);
        if (Double.isNaN(c.treadPhase)) {
            c.treadPhase = 1.3 * relief.treadPhase(x + 0.5, z + 0.5);
        }
        double v = 0.37 * y + c.treadPhase;
        return v - Math.floor(v);
    }

    /** 0..1 per block (39f): thin, winding layers through the obsidian needles (their glowing veins below 0.07). */
    double vein(int x, int y, int z) {
        Column c = column(x, z);
        if (Double.isNaN(c.veinPhase)) {
            c.veinPhase = 2.5 * relief.veinPhase(x + 0.5, z + 0.5);
        }
        double v = y / 9.0 + c.veinPhase;
        return v - Math.floor(v);
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

    /** The lava tube system of tube cell (i, j) (42), traced once per world. */
    LavaTubes.TubeSystem tubeSystem(int i, int j) {
        long key = (long) i << 32 | j & 0xFFFFFFFFL;
        if (decisions.tubeSystems.size() > Decisions.LIMIT) {
            decisions.tubeSystems.clear();
        }
        return once(decisions.tubeSystems, key, () -> LavaTubes.trace(this, i, j));
    }

    /** The lava tube tile around (x, z) (42), remembered per thread. */
    private LavaTubes.Tile tubeTileFor(int x, int z) {
        int ti = Math.floorDiv(x, LavaTubes.TILE);
        int tj = Math.floorDiv(z, LavaTubes.TILE);
        TubeTile cached = tubeTile.get();
        if (!cached.filled || cached.ti != ti || cached.tj != tj) {
            long key = (long) ti << 32 | tj & 0xFFFFFFFFL;
            if (decisions.tubeTiles.size() > Decisions.LIMIT) {
                decisions.tubeTiles.clear();
            }
            cached.tile = once(decisions.tubeTiles, key, () -> LavaTubes.tile(this, ti, tj));
            cached.ti = ti;
            cached.tj = tj;
            cached.filled = true;
        }
        return cached.tile;
    }

    /** The fire geode of geode cell (i, j) (42), or null. */
    FireGeodeShape.Geode fireGeodeCell(int i, int j) {
        return cell(geodeMemo, decisions.geodes, i, j, FireGeodes::place);
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

    /** The deepest rift floor of rift-arch cell (i, j) ({@link Landmarks#riftFloor}), remembered per world. */
    double[] riftFloor(int i, int j) {
        long key = (long) i << 32 | j & 0xFFFFFFFFL;
        double[] known = decisions.riftFloors.get(key);
        if (known == null) {
            known = Landmarks.riftFloor(this, i, j);
            decisions.remember(decisions.riftFloors, key, known);
        }
        return known;
    }

    Landmarks.Arch riftArch(int i, int j) {
        return cell(riftArchMemo, decisions.riftArches, i, j, Landmarks::riftArch);
    }

    /** The collapse of cell (i, j) where the terrain uses it (39f), or null. */
    CellShapes.Collapse collapse(int i, int j) {
        return cell(collapseMemo, decisions.collapses, i, j, CellShapes::placeCollapse);
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

        c.reset(x, z);
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
                    addPillars(c, v, px, pz);
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
        // Collapses (39f): ragged sinkholes with stepped walls on the plains, the surface's openings to the caves. Not in
        // the ground a river follows (a collapse keeps clear of rivers, so the two never ask each other).
        CellShapes.Collapse collapse = null;
        double collapseQ = 2.0;
        double collapseCut = 0.0;
        if (mode != Mode.TRACE && r.core() > 0.0 && keep > 0.0) {
            collapse = collapse(Math.floorDiv(x, CellShapes.COLLAPSE_CELL), Math.floorDiv(z, CellShapes.COLLAPSE_CELL));
            if (collapse != null && Math.abs(px - collapse.x()) < collapse.reach() && Math.abs(pz - collapse.z()) < collapse.reach()) {
                collapseQ = collapse.q(px, pz, COLLAPSE_WARP * warpZ);
                collapseCut = keep * collapse.cut(collapseQ);
                if (collapseQ > 0.5 && collapseQ < 1.0) {
                    cliff = Math.max(cliff, 1.0);
                }
                c.collapse = 1.0 - VolcanicMath.smoothstep(0.9, 1.25, collapseQ);
            } else {
                collapse = null;
            }
        }
        // Micro relief (39d): a fine 2D wobble that breaks the straight one-block contour lines of smooth slopes, much
        // cheaper than 3D crags on the whole plain.
        double micro = r.fade() > 0.0 ? MICRO_RELIEF * r.fade() * keep * relief.microRelief(px, pz) : 0.0;
        double preHeight = plain + raise + sink + keep * detail + cinder + micro - collapseCut;
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
        double gorge = 0.0;
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
                if (riverNear > 0.0) {
                    // 42b: how deep the river's gorge lies below this column's ground (its walls, for the surface).
                    gorge = Math.max(0.0, preHeight - (level - CHANNEL_DEPTH));
                }
                if (along < 1.0 + BANK_BLOCKS / half) {
                    c.lavaBank = along < 1.0 ? 1.0 : Mth.clamp(1.0 - (along - 1.0) * half / BANK_BLOCKS, 0.0, 1.0);
                }
                double shore = (along - lavaEdge(preHeight, level)) * half;
                if (shore < SHORE_BLOCKS) {
                    c.lavaShore = shore <= 0.0 ? 1.0 : 1.0 - shore / SHORE_BLOCKS;
                }
                if (along < 1.0 && !c.hasLava()) {
                    c.lavaTop = level;
                    c.lavaBottom = level - CHANNEL_DEPTH - 3;
                }
                // 42b: at least RIVER_GUARD blocks past the lava's edge, more than a cell of the terrain's interpolation,
                // so no cell corner near the lava interpolates a cave into it.
                if (along < Math.max(1.8, 1.0 + RIVER_GUARD / half)) {
                    c.guardBottom = Math.min(c.guardBottom, level - 12);
                    c.guardTop = Math.max(c.guardTop, level + 4);
                }
            }
        }
        // 42c: also ABYSS_GUARD blocks past the sea's edge where the abyss tapers to its ends (a band of 0.3 half widths was
        // a block or less there, and a cave interpolated from the unguarded cell corners opened beside the lava).
        if (chasm < 1.3 || abyss != null && abyss.near(px, pz, ABYSS_GUARD)) {
            c.guardBottom = Math.min(c.guardBottom, Landmarks.ABYSS_FLOOR - 14);
            c.guardTop = Math.max(c.guardTop, Landmarks.ABYSS_LAVA + 6);
        }
        c.preHeight = preHeight;
        c.height = height;
        c.floorMargin = floorMargin(r, keep, riftStrength, chasm, height, riverNear);
        if (collapse != null && collapseQ < 1.0 + 18.0 / collapse.radius()) {
            c.floorMargin = Math.max(c.floorMargin, collapse.depth() + 2.0);
        }
        c.cutDepth = Math.max(c.floorMargin, gorge + 2.0);
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
            if (collapse != null) {
                ThinShapes.collapse(this, c, collapse, collapseQ, px, pz, height);
            }
            ThinShapes.landmarks(this, c, x, z, px, pz, r, height, abyss, chasm, warpX);
            Neighbourhood islandCells = channelNeighbourhood.get().aroundIslands(this, Math.floorDiv(x, VolcanoPlacement.ISLAND_CELL), Math.floorDiv(z, VolcanoPlacement.ISLAND_CELL));
            for (List<Volcano.Island> cell : islandCells.islands) {
                addIslands(c, cell, px, pz, warpX);
            }
            if (keep > 0.98 && r.fade() > 0.5) {
                ThinShapes.microSteps(this, c, px, pz, height);
            }
            if (r.core() > 0.9) {
                caves(c, x, z, px, pz);
            }
            landFall(c);
        }
        caveFade(c);
    }

    /** The lava tubes, skylights, magma chambers and fire geodes of a column (42). */
    private void caves(Column c, int x, int z, double px, double pz) {
        // The geode first: its cavity must not be crowded out of the column's openings by the tubes.
        FireGeodeShape.Geode geode = fireGeodeCell(Math.floorDiv(x, FireGeodeShape.CELL), Math.floorDiv(z, FireGeodeShape.CELL));
        // Its guard reaches one block past the lining, which still lies inside the cell (MARGIN).
        if (geode != null && Math.abs(x - geode.x()) <= geode.reach() + 1 && Math.abs(z - geode.z()) <= geode.reach() + 1) {
            int[] cavity = FireGeodes.cavity(geode, x, z);
            if (cavity != null) {
                c.addVoid(cavity[0], cavity[1]);
            }
            // Closed: no vanilla cave or carver opens it (a lava tube may).
            c.addDeepGuard(geode.y() - geode.reach() - 2, geode.y() + geode.reach() + 2);
        }
        LavaTubes.addTo(c, tubeTileFor(x, z), px, pz);
    }

    /**
     * Where vanilla caves close in this column (42, G2-WT3 = A): in a volcano's body (its cone and crater rim, from a
     * little above the plain it stands on; weighted by how high it rises) and everywhere above {@link #CAVE_CEILING}.
     */
    private static void caveFade(Column c) {
        c.caveFadeWeight = VolcanicMath.smoothstep(4.0, 16.0, c.rise);
        c.caveFadeFrom = c.height - c.rise + CAVE_VOLCANO_HEIGHT;
    }

    /**
     * Where the lava of a river ends across its channel (39g), in half widths: the channel's profile
     * ({@code 1 - smoothstep(0.55, 1, along)}) lowers the ground {@code preHeight} towards the bed two blocks below the
     * lava, and the lava lies where the ground drops below its level. The column's own ground stands for its
     * neighbours' (the shore is a few blocks wide; the terrain's interpolation moves it by a block or so).
     */
    static double lavaEdge(double preHeight, int level) {
        double above = preHeight - level;
        if (above <= 0.0) {
            return 1.0;
        }
        // The profile at the edge is above / (above + CHANNEL_DEPTH); smoothstep's inverse is 0.5 - sin(asin(1 - 2s) / 3).
        double s = 1.0 - above / (above + CHANNEL_DEPTH);
        return 0.55 + 0.45 * (0.5 - Math.sin(Math.asin(1.0 - 2.0 * s) / 3.0));
    }

    /**
     * Where a lava fall through this column lands (39f): on the ground, a lava lake, an island or a thin solid below its
     * top; no fall if something solid stands at its top.
     */
    private static void landFall(Column c) {
        if (c.fallTop == Integer.MIN_VALUE) {
            return;
        }
        int land = Mth.floor(c.height) - 2;
        if (c.hasLava() && c.lavaTop > land) {
            land = c.lavaTop;
        }
        for (int i = 0; i < c.islandCount; i++) {
            int top = Mth.floor(c.islandTop[i]);
            if (top <= c.fallTop + 1) {
                land = Math.max(land, top);
            }
        }
        for (int i = 0; i < c.solidCount; i++) {
            if (c.solidTop[i] < c.fallTop) {
                land = Math.max(land, c.solidTop[i] + 1);
            }
        }
        if (land > c.fallTop || c.thinKind(c.fallTop) >= 0) {
            c.fallTop = Integer.MIN_VALUE;
            return;
        }
        c.fallBottom = land;
        c.fallFlowBelow = land + 4;
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
                double d = island.distance(x, z, 0.18 * warpX);
                if (d < 1.0) {
                    c.addIslandInterval(island.topAt(d), island.bottomAt(d, island.theta(x, z)), d);
                }
                c.addFalls(island.falls());
            }
        }
    }

    /** The archipelago pillars of volcano {@code v} that may stand in this column (tested per block). */
    private void addPillars(Column c, Volcano v, double px, double pz) {
        if (v.pillars().isEmpty()) {
            return;
        }
        for (Volcano.Pillar pillar : v.pillars()) {
            if (Math.abs(px - pillar.x()) < pillar.reach() && Math.abs(pz - pillar.z()) < pillar.reach() && islandClear(v.islands().get(0))) {
                c.addPillar(pillar);
            }
        }
    }

    /**
     * Whether an island hangs free: at least {@link #ISLAND_CLEARANCE} blocks above the terrain under its centre, under
     * eight points below its hanging lobes and eight points of its rim (a neighbouring volcano may rise under a
     * cluster). Pure; remembered per thread.
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
        if (island.lowestAt(0.0) < coreHeightUncached(island.x(), island.z()) + ISLAND_CLEARANCE) {
            return false;
        }
        // Under the hanging lobes (their deepest point, about a third of the way out) and under the rim.
        double outer = island.a() * island.stretch();
        double[] radii = {Volcano.LOBE_D * outer, outer};
        double[] lowest = {island.lowestAt(Volcano.LOBE_D), island.lowestAt(0.7)};
        for (int n = 0; n < radii.length; n++) {
            for (int k = 0; k < 8; k++) {
                double angle = k * Math.PI / 4.0;
                double r = radii[n];
                if (lowest[n] < coreHeightUncached(island.x() + r * Math.cos(angle), island.z() + r * Math.sin(angle))
                        + ISLAND_CLEARANCE) {
                    return false;
                }
            }
        }
        return true;
    }

    // ---------------------------------------------------------------- read-only accessors (39f, for features and atmosphere)

    /**
     * A volcano as other systems see it: centre, the Y of its crater rim ({@code craterY}, where a plume starts), the
     * radius of the crater at its rim and of its foot, whether it is a great horizon volcano and whether it is dormant.
     */
    public record VolcanoInfo(int x, int z, int craterY, int craterRadius, int footRadius, boolean great, boolean dormant) {
    }

    /** A fumarole mound: centre, radius, height (already scaled by the sulfur fields' weight at its centre). */
    public record Mound(double x, double z, double radius, double height) {
    }

    /**
     * A sinkhole: centre, radius (for a collapse: the mean radius of its ragged rim), depth to its floor, and whether a
     * shaft leads on down towards the caves.
     */
    public record Sinkhole(double x, double z, double radius, double depth, boolean shaft) {
    }

    /** A point on the ground plane. */
    public record Point(double x, double z) {
    }

    /** A titan's ribcage: centre, reach, the vertebrae of its spine and the feet of its ribs. */
    public record Ribcage(double x, double z, double reach, List<Point> vertebrae, List<Point> ribFeet) {
    }

    private static VolcanoInfo info(Volcano v) {
        return new VolcanoInfo(Mth.floor(v.x()), Mth.floor(v.z()), Mth.floor(v.rim()), (int) Math.round(v.crest()),
                (int) Math.round(v.radius()), v.kind() == Volcano.Kind.GREAT, v.dormant());
    }

    private static VolcanoInfo info(Landmarks.FloatingVolcano f) {
        return new VolcanoInfo(Mth.floor(f.x()), Mth.floor(f.z()), Mth.floor(f.base() + f.cone()), (int) Math.round(f.crater()),
                (int) Math.round(f.radius()), false, false);
    }

    /**
     * Every volcano (great, cone, caldera, and the floating volcano) whose centre lies within {@code radius} blocks of
     * (x, z). Pure placement decisions (no chunks), thread-safe.
     */
    public List<VolcanoInfo> volcanoesWithin(int x, int z, int radius) {
        List<VolcanoInfo> found = new java.util.ArrayList<>();
        long r2 = (long) radius * radius;
        int size = settings.cellSize();
        for (int i = Math.floorDiv(x - radius, size); i <= Math.floorDiv(x + radius, size); i++) {
            for (int j = Math.floorDiv(z - radius, size); j <= Math.floorDiv(z + radius, size); j++) {
                Volcano v = volcano(i, j);
                if (v != null && sq(v.x() - x) + sq(v.z() - z) <= r2) {
                    found.add(info(v));
                }
            }
        }
        int cell = Landmarks.FLOATING_CELL;
        for (int i = Math.floorDiv(x - radius, cell); i <= Math.floorDiv(x + radius, cell); i++) {
            for (int j = Math.floorDiv(z - radius, cell); j <= Math.floorDiv(z + radius, cell); j++) {
                Landmarks.FloatingVolcano f = floatingDecision(i, j);
                if (f != null && sq(f.x() - x) + sq(f.z() - z) <= r2) {
                    found.add(info(f));
                }
            }
        }
        return found;
    }

    /** The nearest volcano (great, cone or caldera) in the 3 × 3 volcano cells around (x, z), or null. Thread-safe. */
    @Nullable
    public VolcanoInfo nearestVolcano(int x, int z) {
        int size = settings.cellSize();
        int ci = Math.floorDiv(x, size);
        int cj = Math.floorDiv(z, size);
        Volcano best = null;
        double bestD = Double.MAX_VALUE;
        for (int i = ci - 1; i <= ci + 1; i++) {
            for (int j = cj - 1; j <= cj + 1; j++) {
                Volcano v = volcano(i, j);
                if (v != null && sq(v.x() - x) + sq(v.z() - z) < bestD) {
                    best = v;
                    bestD = sq(v.x() - x) + sq(v.z() - z);
                }
            }
        }
        return best == null ? null : info(best);
    }

    /** The fumarole mound of the mound cell of (x, z) where the sulfur fields shape it, or null. Thread-safe. */
    @Nullable
    public Mound moundAt(int x, int z) {
        double[] m = CellShapes.moundShape(salt(), x + 0.5, z + 0.5);
        if (m == null) {
            return null;
        }
        double weight = regions(m[0], m[1]).sulfurS();
        return weight <= 0.0 ? null : new Mound(m[0], m[1], m[2], m[3] * weight);
    }

    /** The sulfur sinkhole of the sinkhole cell of (x, z) where the sulfur fields shape it, or null. Thread-safe. */
    @Nullable
    public Sinkhole sinkholeAt(int x, int z) {
        double[] s = CellShapes.sinkholeShape(salt(), x + 0.5, z + 0.5);
        if (s == null) {
            return null;
        }
        double weight = regions(s[0], s[1]).sulfurS();
        return weight <= 0.0 ? null : new Sinkhole(s[0], s[1], s[2], s[3] * weight, false);
    }

    /** The collapse (39f) of the collapse cell of (x, z), or null. Thread-safe. */
    @Nullable
    public Sinkhole collapseAt(int x, int z) {
        CellShapes.Collapse c = collapse(Math.floorDiv(x, CellShapes.COLLAPSE_CELL), Math.floorDiv(z, CellShapes.COLLAPSE_CELL));
        return c == null ? null : new Sinkhole(c.x(), c.z(), c.radius(), c.depth(), c.shaft());
    }

    /**
     * The fire crystal geode (42) of the geode cell of (x, z), or null: the cavity the terrain dug, which the lining of
     * 41b fills ({@link FireGeodeShape}; the whole geode and its lining lie inside the cell, a chunk inside one cell).
     * Where another shape takes a column (a thin solid, a full list of openings) a block of the shape's cavity may stay
     * rock: line only the open blocks (output {@code fire_geodes} is −1 exactly there). Pure placement decisions (no
     * chunks), thread-safe.
     */
    @Nullable
    public FireGeodeShape.Geode fireGeode(int x, int z) {
        return decision(decisions.geodes, Math.floorDiv(x, FireGeodeShape.CELL), Math.floorDiv(z, FireGeodeShape.CELL), FireGeodes::place);
    }

    /** The ribcage in the 3 × 3 ribcage cells around (x, z) whose centre is nearest, or null. Thread-safe. */
    @Nullable
    public Ribcage ribcageNear(int x, int z) {
        int ci = Math.floorDiv(x, TitanBones.CAGE_CELL);
        int cj = Math.floorDiv(z, TitanBones.CAGE_CELL);
        TitanBones.Cage best = null;
        for (int i = ci - 1; i <= ci + 1; i++) {
            for (int j = cj - 1; j <= cj + 1; j++) {
                TitanBones.Cage cage = cageDecision(i, j);
                if (cage != null && (best == null || sq(cage.x() - x) + sq(cage.z() - z) < sq(best.x() - x) + sq(best.z() - z))) {
                    best = cage;
                }
            }
        }
        if (best == null) {
            return null;
        }
        List<Point> vertebrae = new java.util.ArrayList<>(best.vx().length);
        for (int k = 0; k < best.vx().length; k++) {
            vertebrae.add(new Point(best.vx()[k], best.vz()[k]));
        }
        List<Point> feet = new java.util.ArrayList<>(best.ribs().size() * 2);
        for (TitanBones.Rib rib : best.ribs()) {
            for (double end : new double[]{rib.from(), rib.to()}) {
                // A broken rib ends in the air at its stump; only the ends at ±1 of the span stand on the ground.
                if (Math.abs(end) >= 1.0) {
                    feet.add(new Point(rib.x() + rib.cos() * end * rib.span(), rib.z() + rib.sin() * end * rib.span()));
                }
            }
        }
        return new Ribcage(best.x(), best.z(), best.reach(), List.copyOf(vertebrae), List.copyOf(feet));
    }

    private static double sq(double v) {
        return v * v;
    }

    /** The floating volcano of cell (i, j) from the per-world map only (no per-thread memo: any thread may ask). */
    private Landmarks.FloatingVolcano floatingDecision(int i, int j) {
        return decision(decisions.floating, i, j, Landmarks::floating);
    }

    private TitanBones.Cage cageDecision(int i, int j) {
        return decision(decisions.cages, i, j, TitanBones::cage);
    }

    @Nullable
    private <T> T decision(Map<Long, Optional<T>> map, int i, int j, CellDecision<T> decision) {
        long key = (long) i << 32 | j & 0xFFFFFFFFL;
        Optional<T> known = map.get(key);
        if (known == null) {
            known = Optional.ofNullable(decision.decide(this, i, j));
            decisions.remember(map, key, known);
        }
        return known.orElse(null);
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
        /**
         * 0..1: a lava river's channel and banks, 1 over the channel, falling to 0 over {@link #BANK_BLOCKS} beside it
         * (features keep clear of it).
         */
        double lavaBank;
        /**
         * 0..1 (39g): a lava river's scorched shore for the surface, 1 over the lava, falling to 0 over
         * {@link #SHORE_BLOCKS} from the lava's edge ({@link #lavaEdge}), whatever the river's width.
         */
        double lavaShore;
        /** 0..1: a cliff or steep wall of a landform (plateau risers, rift flanks, gully walls, sea stacks). */
        double cliff;
        /** How far the surface may drop near this column through a narrow cut (for {@link #initialHeight}). */
        double floorMargin;
        /** 0..1 (39f): a collapse's footprint, 1 inside its rim (where vanilla cave entrances may reach the surface). */
        double collapse;
        /**
         * 42b: how deep a cut near this column may reach below its ground (the abyss, a rift, a river's gorge, a collapse,
         * the landforms' risers): its blocks down to there may form the wall of the cut.
         */
        double cutDepth;
        /** A lava spring (a source that flows) at this Y, or {@link Integer#MIN_VALUE}. */
        int springY;
        /** Lava fills open space from {@link #lavaBottom} up to, not including, {@link #lavaTop}. */
        int lavaTop;
        int lavaBottom;
        /**
         * A lava fall (39f) through this column: falling lava in open space from {@link #fallBottom} up to and including
         * {@link #fallTop} (both {@link Integer#MIN_VALUE} without one); the aquifer lets the lowest blocks flow
         * ({@link #fallFlowBelow}) so the lava spreads where it lands.
         */
        int fallTop;
        int fallBottom;
        int fallFlowBelow;
        int guardBottom;
        int guardTop;
        int islandCount;
        final double[] islandTop = new double[MAX_ISLANDS];
        final double[] islandBottom = new double[MAX_ISLANDS];
        int solidCount;
        final int[] solidTop = new int[MAX_SOLIDS];
        final int[] solidBottom = new int[MAX_SOLIDS];
        final int[] solidKind = new int[MAX_SOLIDS];
        /**
         * Carved openings (lava tube tunnel, collapse overhang and shaft; 42: lava tubes, skylights, magma chambers, fire
         * geodes), {@code voidBottom[i]}..{@code voidTop[i]} inclusive. The aquifer keeps them free of water.
         */
        int voidCount;
        final int[] voidTop = new int[MAX_VOIDS];
        final int[] voidBottom = new int[MAX_VOIDS];
        /**
         * The walls of the lava tubes, skylights and magma chambers in this column (42): a skin (the smooth inner wall)
         * and a shell of rock around each opening, inclusive block ranges; skin blocks up to {@code crustTop} border lava
         * (a gutter or a chamber's lake). {@link #wallMin}..{@link #wallMax} covers them all.
         */
        int caveCount;
        final int[] skinBottom = new int[MAX_CAVES];
        final int[] skinTop = new int[MAX_CAVES];
        final int[] shellBottom = new int[MAX_CAVES];
        final int[] shellTop = new int[MAX_CAVES];
        final int[] crustTop = new int[MAX_CAVES];
        int wallMin;
        int wallMax;
        /**
         * Lava under the ground (42: a magma chamber's lake, a gutter), separate inclusive ranges, inside the openings only;
         * {@link #subLavaMin}..{@link #subLavaMax} covers them.
         */
        int subLavaCount;
        final int[] subLavaBottom = new int[MAX_BANDS];
        final int[] subLavaTop = new int[MAX_BANDS];
        int subLavaMin;
        int subLavaMax;
        /**
         * Rock kept free of vanilla caves and carvers (42: chambers, gutters, geodes), separate inclusive bands, openings
         * excepted; {@link #deepGuardMin}..{@link #deepGuardMax} covers them.
         */
        int deepGuardCount;
        final int[] deepGuardBottom = new int[MAX_BANDS];
        final int[] deepGuardTop = new int[MAX_BANDS];
        int deepGuardMin;
        int deepGuardMax;
        /** Vanilla caves are gone from this Y up, with this weight (42: a volcano's body, {@link #caveFade}). */
        double caveFadeFrom;
        double caveFadeWeight;
        /** Archipelago pillars standing in this column (39f), tested per block from {@link #pillarFloor} up. */
        int pillarCount;
        final Volcano.Pillar[] pillars = new Volcano.Pillar[MAX_PILLARS];
        int pillarFloor;
        // Phases of the per-block surface patterns (39f), computed when the surface first asks (NaN until then).
        double treadPhase;
        double veinPhase;
        int x;
        int z;
        // Scratch space of the fill (no allocation per column).
        final Ring3Relief.Detail scratch = new Ring3Relief.Detail();
        final double[] river = new double[3];
        final double[] span = new double[3];

        void reset(int x, int z) {
            this.x = x;
            this.z = z;
            height = 0.0;
            preHeight = 0.0;
            roughness = 0.0;
            calderaMask = 0.0;
            dormant = 0.0;
            channel = 0.0;
            islandMask = 0.0;
            rise = 0.0;
            lavaBank = 0.0;
            lavaShore = 0.0;
            cliff = 0.0;
            floorMargin = 0.0;
            collapse = 0.0;
            cutDepth = 0.0;
            springY = Integer.MIN_VALUE;
            lavaTop = Integer.MIN_VALUE;
            lavaBottom = Integer.MIN_VALUE;
            fallTop = Integer.MIN_VALUE;
            fallBottom = Integer.MIN_VALUE;
            fallFlowBelow = Integer.MIN_VALUE;
            guardBottom = Integer.MAX_VALUE;
            guardTop = Integer.MIN_VALUE;
            islandCount = 0;
            solidCount = 0;
            voidCount = 0;
            caveCount = 0;
            wallMin = Integer.MAX_VALUE;
            wallMax = Integer.MIN_VALUE;
            subLavaCount = 0;
            subLavaMin = Integer.MAX_VALUE;
            subLavaMax = Integer.MIN_VALUE;
            deepGuardCount = 0;
            deepGuardMin = Integer.MAX_VALUE;
            deepGuardMax = Integer.MIN_VALUE;
            caveFadeFrom = Double.MAX_VALUE;
            caveFadeWeight = 0.0;
            pillarCount = 0;
            pillarFloor = Integer.MAX_VALUE;
            treadPhase = Double.NaN;
            veinPhase = Double.NaN;
        }

        /** An island (or the floating volcano) between {@code bottom} and {@code top} (exact, per block, 39f). */
        void addIslandInterval(double top, double bottom, double d) {
            if (islandCount == MAX_ISLANDS || Math.floor(top - 1.0) < Math.ceil(bottom)) {
                return;
            }
            islandTop[islandCount] = top;
            islandBottom[islandCount] = bottom;
            islandCount++;
            islandMask = Math.max(islandMask, Mth.clamp((1.0 - d) * 4.0, 0.0, 1.0));
        }

        /** The spring and fall columns of an island's lava falls ({@link Volcano.LavaFall}). */
        void addFalls(List<Volcano.LavaFall> falls) {
            for (Volcano.LavaFall fall : falls) {
                if (fall.springX() == x && fall.springZ() == z) {
                    springY = fall.springY();
                }
                if (fall.fallX() == x && fall.fallZ() == z) {
                    fallTop = Math.max(fallTop, fall.springY());
                }
            }
        }

        void addSolid(double bottom, double top) {
            addSolid(bottom, top, Landmarks.KIND_BIOME);
        }

        /** A thin solid from {@code bottom} to {@code top}; {@code kind} ({@link Landmarks#KIND_BIOME} …) picks its surface. */
        void addSolid(double bottom, double top, int kind) {
            // Thin shapes bypass the stack's top slide (300 → 316): keep them below it.
            top = Math.min(top, THIN_TOP);
            if (solidCount == MAX_SOLIDS || top < bottom) {
                return;
            }
            solidBottom[solidCount] = Mth.floor(bottom);
            solidTop[solidCount] = Mth.floor(top);
            solidKind[solidCount] = kind;
            solidCount++;
        }

        /**
         * A carved opening from {@code bottom} to {@code top} (inclusive block Y). 42b: never inside the band around a
         * lava lake, river or the abyss's lava sea (whatever shape asks): the opening keeps its larger part outside it.
         */
        void addVoid(int bottom, int top) {
            if (top >= guardBottom && bottom <= guardTop) {
                if (top - guardTop >= guardBottom - bottom) {
                    bottom = guardTop + 1;
                } else {
                    top = guardBottom - 1;
                }
            }
            if (voidCount == MAX_VOIDS || top < bottom) {
                return;
            }
            voidBottom[voidCount] = bottom;
            voidTop[voidCount] = top;
            voidCount++;
        }

        /**
         * A lava tube, skylight or chamber in this column (42): its opening (carved; null if the column only holds its
         * walls), skin and shell (null if the column misses them), and the top of the skin that borders lava.
         */
        void addCave(@Nullable int[] opening, @Nullable int[] skin, @Nullable int[] shell, int crust) {
            if (opening != null) {
                addVoid(opening[0], opening[1]);
            }
            if (shell == null || caveCount == MAX_CAVES) {
                return;
            }
            // The walls stay a few blocks under the ground (a mouth, a skylight): up there the biome paints the rim.
            int wallTop = Mth.floor(height) - 3;
            skinBottom[caveCount] = skin == null ? Integer.MAX_VALUE : skin[0];
            skinTop[caveCount] = skin == null ? Integer.MIN_VALUE : Math.min(skin[1], wallTop);
            shellBottom[caveCount] = shell[0];
            shellTop[caveCount] = Math.min(shell[1], wallTop);
            if (shellTop[caveCount] < shellBottom[caveCount]) {
                return;
            }
            crustTop[caveCount] = crust;
            wallMin = Math.min(wallMin, shell[0]);
            wallMax = Math.max(wallMax, shell[1]);
            caveCount++;
        }

        /** Lava under the ground from {@code bottom} to {@code top} (inclusive), a range of its own. */
        void addSubLava(int bottom, int top) {
            if (subLavaCount < MAX_BANDS) {
                subLavaBottom[subLavaCount] = bottom;
                subLavaTop[subLavaCount] = top;
                subLavaCount++;
                subLavaMin = Math.min(subLavaMin, bottom);
                subLavaMax = Math.max(subLavaMax, top);
            }
        }

        /** Rock kept free of vanilla caves and carvers from {@code bottom} to {@code top} (inclusive), a band of its own. */
        void addDeepGuard(int bottom, int top) {
            if (deepGuardCount < MAX_BANDS) {
                deepGuardBottom[deepGuardCount] = bottom;
                deepGuardTop[deepGuardCount] = top;
                deepGuardCount++;
                deepGuardMin = Math.min(deepGuardMin, bottom);
                deepGuardMax = Math.max(deepGuardMax, top);
            }
        }

        /** Whether {@code y} lies in one of the column's guard bands (42). */
        public boolean deepGuardAt(int y) {
            if (y < deepGuardMin || y > deepGuardMax) {
                return false;
            }
            for (int i = 0; i < deepGuardCount; i++) {
                if (y >= deepGuardBottom[i] && y <= deepGuardTop[i]) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Which wall the (solid) block at {@code y} belongs to (42): 0 none, 1 the smooth skin of an opening, 2 its shell,
         * 3 skin bordering lava (a crust).
         */
        int caveWall(int y) {
            if (y < wallMin || y > wallMax) {
                return 0;
            }
            for (int i = 0; i < caveCount; i++) {
                if (y >= skinBottom[i] && y <= skinTop[i]) {
                    return y <= crustTop[i] ? 3 : 1;
                }
            }
            for (int i = 0; i < caveCount; i++) {
                if (y >= shellBottom[i] && y <= shellTop[i]) {
                    return 2;
                }
            }
            return 0;
        }

        /**
         * 42b: whether the block at {@code y} may be part of the wall of a deep cut beside the column (from its ground down
         * to {@link #cutDepth} and two more; only for cuts of {@link #DEEP_CUT} blocks or more).
         */
        boolean deepWall(int y) {
            return cutDepth >= DEEP_CUT && y < height && y >= height - cutDepth - 2.0;
        }

        /** Whether a thin solid (or a floating island) fills {@code y} in this column, whatever the terrain. */
        public boolean thinSolidAt(int y) {
            return thinKind(y) >= 0;
        }

        /** Bottom of the band around a lava lake, river or the abyss's lava sea kept free of caves (inclusive). */
        public int guardBottom() {
            return guardBottom;
        }

        /** Top of that band (inclusive), or {@link Integer#MIN_VALUE} without one. */
        public int guardTop() {
            return guardTop;
        }

        /** Whether {@code y} lies in a carved opening of this column. */
        public boolean voidAt(int y) {
            for (int i = 0; i < voidCount; i++) {
                if (y >= voidBottom[i] && y <= voidTop[i]) {
                    return true;
                }
            }
            return false;
        }

        /** +1 where caves stay out, −1 where they may run ({@link VolcanicField#guard}). */
        double guard(int y) {
            return y >= guardBottom && y <= guardTop || deepGuardAt(y) ? 1.0 : -1.0;
        }

        /**
         * The lift of vanilla's cave densities at {@code y} ({@link VolcanicField#caveLift}): rising over
         * {@link #CAVE_FADE} blocks to {@link #CAVE_LIFT} at {@link #CAVE_CEILING}, and in a volcano's body at
         * {@link #caveFadeFrom} (times its weight). A rising lift shrinks the caves gradually, so they end in rounded
         * tips instead of flat ceilings.
         */
        double caveLift(int y) {
            double lift = Mth.clamp((y - CAVE_CEILING + CAVE_FADE) / CAVE_FADE, 0.0, 1.0);
            if (caveFadeWeight > 0.0) {
                lift = Math.max(lift, caveFadeWeight * Mth.clamp((y - caveFadeFrom + CAVE_FADE) / CAVE_FADE, 0.0, 1.0));
            }
            return CAVE_LIFT * lift;
        }

        void addPillar(Volcano.Pillar pillar) {
            if (pillarCount < MAX_PILLARS) {
                pillars[pillarCount++] = pillar;
                pillarFloor = Math.min(pillarFloor, pillar.floorY());
            }
        }

        /**
         * The kind of thin solid at {@code y} ({@link Landmarks#KIND_BIOME} …, {@link Landmarks#KIND_ISLAND} for a
         * floating island), or −1 if the block belongs to none (the terrain decides).
         */
        int thinKind(int y) {
            for (int i = 0; i < solidCount; i++) {
                if (y >= solidBottom[i] && y <= solidTop[i]) {
                    return solidKind[i];
                }
            }
            if (pillarCount > 0 && y >= pillarFloor) {
                for (int i = 0; i < pillarCount; i++) {
                    if (pillars[i].contains(x, y, z)) {
                        return Landmarks.KIND_BASALT;
                    }
                }
            }
            return islandAt(y) ? Landmarks.KIND_ISLAND : -1;
        }

        /** +1 inside a thin solid or island, −1 inside a carved opening, 0 elsewhere (the terrain decides). */
        double thin(int y) {
            if (thinKind(y) >= 0) {
                return 1.0;
            }
            return voidAt(y) ? -1.0 : 0.0;
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

        /** The lowest lava under the ground (42: a chamber's lake, a gutter; inclusive), or {@link Integer#MAX_VALUE}. */
        public int subLavaMin() {
            return subLavaMin;
        }

        /** The highest lava under the ground (inclusive), or {@link Integer#MIN_VALUE} without any. */
        public int subLavaMax() {
            return subLavaMax;
        }

        /** The bottom of the lowest band kept free of vanilla caves and carvers (42), or {@link Integer#MAX_VALUE}. */
        public int deepGuardMin() {
            return deepGuardMin;
        }

        /** The top of the highest such band, or {@link Integer#MIN_VALUE}. */
        public int deepGuardMax() {
            return deepGuardMax;
        }

        /** The lowest carved opening's bottom in this column, or {@link Integer#MAX_VALUE} without any. */
        public int voidMin() {
            int min = Integer.MAX_VALUE;
            for (int i = 0; i < voidCount; i++) {
                min = Math.min(min, voidBottom[i]);
            }
            return min;
        }

        /** The highest carved opening's top in this column, or {@link Integer#MIN_VALUE} without any. */
        public int voidMax() {
            int max = Integer.MIN_VALUE;
            for (int i = 0; i < voidCount; i++) {
                max = Math.max(max, voidTop[i]);
            }
            return max;
        }

        /** Whether lava under the ground (42) fills the opening at {@code y}. */
        public boolean subLavaAt(int y) {
            if (y < subLavaMin || y > subLavaMax || !voidAt(y)) {
                return false;
            }
            for (int i = 0; i < subLavaCount; i++) {
                if (y >= subLavaBottom[i] && y <= subLavaTop[i]) {
                    return true;
                }
            }
            return false;
        }

        /** The Y of the column's lava spring (a source that flows, 39d), or {@link Integer#MIN_VALUE}. */
        public int springY() {
            return springY;
        }

        /** The top of the column's lava fall (inclusive, 39f), or {@link Integer#MIN_VALUE}. */
        public int fallTop() {
            return fallTop;
        }

        /** The bottom of the column's lava fall (inclusive), or {@link Integer#MIN_VALUE}. */
        public int fallBottom() {
            return fallBottom;
        }

        /** Below this Y (inclusive) the fall's lava is let flow, where it lands; {@link Integer#MIN_VALUE} without a fall. */
        public int fallFlowBelow() {
            return fallFlowBelow;
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

        /** Whether lava fills open space at {@code y} in this column (a lake, river, abyss sea, spring or fall). */
        public boolean lavaAt(int y) {
            return lavaAt(y, lavaTop, lavaBottom, springY) || y >= fallBottom && y <= fallTop;
        }

        /** The lava rule of a column from its bounds (shared with the aquifer's per-column table; falls apart). */
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
