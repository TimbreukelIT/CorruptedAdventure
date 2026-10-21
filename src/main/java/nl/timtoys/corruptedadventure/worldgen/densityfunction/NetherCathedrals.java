package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.DensityFunction;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code corruptedadventure:nether_cathedrals} (87): the white, silent Nether ring 5 ({@code white_halls} with the rare
 * {@code hollow_cathedral}, V10-A) as one pure field of cells. Per cell of {@link CathedralSettings#cellSize} Nether
 * blocks at most one cathedral: a round hall, its whole footprint clear of both zones with a margin (the
 * {@code edge} function, a {@code ring_edge} as wide as the zone plus the margin, is 0 on its outline), with one giant column in the middle; every cell also has its own
 * offset of the column grid, so the grid is not one endless lattice. Two outputs, from the same settings:
 * <ul>
 *     <li>{@link Output#MASK}: 1 inside a cathedral, 0 outside, over its rim (the Nether router's temperature of ring 5,
 *     so the biome source puts {@code hollow_cathedral} at ≥ 0.5);</li>
 *     <li>{@link Output#TERRAIN}: the density of Nether ring 5 (the ring's input of {@code ring_blend/nether}), exact per
 *     block, so it is never interpolated: a flat floor whose top is {@link HallSettings#floorTop} (solid down to the
 *     bedrock), raised rectangular plains on the tiles between the columns, a flat ceiling from
 *     {@link HallSettings#ceiling} up, square columns on the grid from floor to ceiling, and in a cathedral the roof
 *     lifted to {@link HallSettings#roof} over its rim (a smoothstep, no stair).</li>
 * </ul>
 * Nothing in Nether ring 5 is open below the floor, so there is no lava anywhere ({@code fluid_levels.ring_5}).
 * <p>
 * Pure: from the seeded cell hash ({@code cells}), the plains noise ({@code plains}, the region noise of ring 5), the
 * {@code edge} function and the position. A cell is decided once per world: the copies the noise chunk makes of the wired
 * function share one map of decided cells ({@link #mapAll}), as {@link GiantGeodes} does; a per-thread memory of the last
 * columns and tiles sits in front of it.
 */
public final class NetherCathedrals implements DensityFunction {
    /** The density's bounds, like the terrain scripts' {@code clamp(..., -2.5, 2.5)}, so the zone blends see the same scale. */
    static final double BOUND = 2.5;
    /** Density per block above or below a surface (the scripts' {@code (height - y) / 8}). */
    static final double SLOPE = 0.125;
    /**
     * The highest lowest-solid Y of a cathedral roof: vanilla's Nether surface paints netherrack from {@code below_top 5}
     * (Y 122) up, before any biome branch, so a roof at 122 or higher would not be white stone.
     */
    static final int ROOF_MAX = 121;
    /** Blocks between the points tested on a cathedral's outline against {@code edge} (the boundary noise has ripples). */
    static final double OUTLINE_SPACING = 4.0;
    private static final int COLUMN_MEMORY = 256;
    private static final int TILE_MEMORY = 64;
    private static final int CELL_MEMORY = 8;
    /** The shared map only holds the cells of one ring; cleared if something asks far more (it never should). */
    private static final int SHARED_LIMIT = 1 << 14;

    public static final MapCodec<NetherCathedrals> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Output.CODEC.fieldOf("output").forGetter(NetherCathedrals::output),
            NoiseHolder.CODEC.fieldOf("cells").forGetter(d -> d.cells),
            NoiseHolder.CODEC.fieldOf("plains").forGetter(d -> d.plains),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("edge").forGetter(d -> d.edge),
            Settings.CODEC.fieldOf("settings").forGetter(NetherCathedrals::settings)
    ).apply(instance, NetherCathedrals::new));

    public static final KeyDispatchDataCodec<NetherCathedrals> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private final Output output;
    private final NoiseHolder cells;
    private final NoiseHolder plains;
    private final DensityFunction edge;
    private final Settings settings;
    private final Map<Long, Cell> decided;
    // Lazy: a Memory is only made for a thread that reads the terrain (never for the mask, which reads the shared map).
    private final ThreadOwned<Memory> memory = new ThreadOwned<>(Memory::new);

    public NetherCathedrals(Output output, NoiseHolder cells, NoiseHolder plains, DensityFunction edge, Settings settings) {
        this(output, cells, plains, edge, settings, new ConcurrentHashMap<>());
    }

    private NetherCathedrals(Output output, NoiseHolder cells, NoiseHolder plains, DensityFunction edge, Settings settings,
                             Map<Long, Cell> decided) {
        this.output = output;
        this.cells = cells;
        this.plains = plains;
        this.edge = edge;
        this.settings = settings;
        this.decided = decided;
    }

    public Output output() {
        return output;
    }

    public Settings settings() {
        return settings;
    }

    @Override
    public double compute(FunctionContext context) {
        int x = context.blockX();
        int z = context.blockZ();
        if (output == Output.MASK) {
            return mask(x, z);
        }
        Column column = memory.get().column(this, x, z);
        return column.density(context.blockY());
    }

    // ------------------------------------------------------------------ read access (features, surface, commands)

    /**
     * 1 inside the cathedral over (x, z), 0 outside it, linear over its rim. Straight from the shared map: the climate
     * sampler of the random state is one object for every thread.
     */
    public double mask(int x, int z) {
        Cathedral cathedral = cellAt(x, z).cathedral();
        return cathedral == null ? 0.0 : cathedral.mask(x, z);
    }

    /** The cathedral whose footprint holds column (x, z), or null. */
    @Nullable
    public Cathedral cathedralAt(int x, int z) {
        Cathedral cathedral = cellAt(x, z).cathedral();
        return cathedral != null && cathedral.distance(x, z) < cathedral.radius() ? cathedral : null;
    }

    /** Every cathedral whose centre lies in the box (inclusive), ordered by cell. */
    public List<Cathedral> cathedralsWithin(int minX, int minZ, int maxX, int maxZ) {
        int size = settings.cathedrals().cellSize();
        List<Cathedral> found = new ArrayList<>();
        for (int cz = Math.floorDiv(minZ, size); cz <= Math.floorDiv(maxZ, size); cz++) {
            for (int cx = Math.floorDiv(minX, size); cx <= Math.floorDiv(maxX, size); cx++) {
                Cathedral cathedral = shared(cx, cz).cathedral();
                if (cathedral != null && cathedral.x() >= minX && cathedral.x() <= maxX && cathedral.z() >= minZ && cathedral.z() <= maxZ) {
                    found.add(cathedral);
                }
            }
        }
        return found;
    }

    /** Whether column (x, z) is a column from floor to ceiling: one of the grid's or a cathedral's giant one. */
    public boolean pillarAt(int x, int z) {
        return columnOf(x, z).solid();
    }

    /** The Y of the floor's top block at column (x, z): {@link HallSettings#floorTop}, raised on a plain. */
    public int floorTop(int x, int z) {
        return columnOf(x, z).floorTop();
    }

    /** The lowest solid Y of the ceiling over column (x, z). */
    public int ceiling(int x, int z) {
        return columnOf(x, z).ceiling();
    }

    /**
     * A reader with its own memory, for one thread at a time (a chunk's surface pass, a feature): the accessors on this
     * object share a per-thread memory that is replaced when another thread asks, so a reader that many threads use at
     * once should be one per user.
     */
    public Reader reader() {
        return new Reader();
    }

    /** The cell holding column (x, z): its grid offset and cathedral, decided once per world. Pure. */
    public Cell cellAt(int x, int z) {
        int size = settings.cathedrals().cellSize();
        return shared(Math.floorDiv(x, size), Math.floorDiv(z, size));
    }

    /** The first {@code nether_cathedrals} with the terrain output anywhere inside {@code root} (a final density), or null. */
    @Nullable
    public static NetherCathedrals find(DensityFunction root) {
        NetherCathedrals[] found = new NetherCathedrals[1];
        root.mapAll(new Visitor() {
            @Override
            public DensityFunction apply(DensityFunction function) {
                if (found[0] == null && function instanceof NetherCathedrals cathedrals && cathedrals.output == Output.TERRAIN) {
                    found[0] = cathedrals;
                }
                return function;
            }

            @Override
            public NoiseHolder visitNoise(NoiseHolder holder) {
                return holder;
            }
        });
        return found[0];
    }

    // ------------------------------------------------------------------ the cells

    private Column columnOf(int x, int z) {
        return memory.get().column(this, x, z);
    }

    /** The cell from the map shared by the copies of this wired function, deciding it the first time. */
    private Cell shared(int cellX, int cellZ) {
        long key = key(cellX, cellZ);
        Cell known = decided.get(key);
        if (known == null) {
            if (decided.size() > SHARED_LIMIT) {
                decided.clear();
            }
            // Pure, so two threads deciding the same cell at once put the same answer.
            known = cellOf(cellX, cellZ);
            decided.put(key, known);
        }
        return known;
    }

    /**
     * Cell (cellX, cellZ): its grid offset, and with chance {@code chance} a cathedral: the first of {@code attempts}
     * candidates whose footprint is clear of both zones. A candidate's footprint plus {@code clearance} lies inside the
     * cell, so no two halls touch and no column of a neighbouring cell's grid stands in one. Pure.
     */
    Cell cellOf(int cellX, int cellZ) {
        CathedralSettings c = settings.cathedrals();
        HallSettings h = settings.halls();
        long hash = SeededHash.hash(cells, cellX, cellZ, 0);
        int gridX = (int) (SeededHash.unit(hash, 0) * h.grid());
        int gridZ = (int) (SeededHash.unit(hash, 1) * h.grid());
        Cathedral cathedral = null;
        if (SeededHash.unit(hash, 2) < c.chance()) {
            for (int attempt = 0; attempt < c.attempts() && cathedral == null; attempt++) {
                long a = SeededHash.hash(cells, cellX, cellZ, 1 + attempt);
                int radius = pick(c.radius(), SeededHash.unit(a, 0));
                int rim = pick(c.rim(), SeededHash.unit(a, 1));
                int pillar = pick(c.pillarRadius(), SeededHash.unit(a, 2));
                int inset = radius + c.clearance();
                int span = c.cellSize() - 2 * inset;
                int x = cellX * c.cellSize() + inset + (int) (SeededHash.unit(a, 3) * span);
                int z = cellZ * c.cellSize() + inset + (int) (SeededHash.unit(a, 4) * span);
                if (clearOfTheZones(x, z, radius)) {
                    cathedral = new Cathedral(x, z, radius, rim, pillar);
                }
            }
        }
        return new Cell(cellX, cellZ, gridX, gridZ, cathedral);
    }

    /** The centre and points of the outline every {@link #OUTLINE_SPACING} blocks where {@code edge} is 0. */
    private boolean clearOfTheZones(int x, int z, int radius) {
        if (edge(x, z) > 0.0) {
            return false;
        }
        int points = (int) Math.ceil(2.0 * Math.PI * radius / OUTLINE_SPACING);
        for (int i = 0; i < points; i++) {
            double angle = i * 2.0 * Math.PI / points;
            if (edge(x + (int) Math.round(Math.cos(angle) * radius), z + (int) Math.round(Math.sin(angle) * radius)) > 0.0) {
                return false;
            }
        }
        return true;
    }

    private double edge(int x, int z) {
        return edge.compute(new SinglePointContext(x, 0, z));
    }

    /** An integer of [min, max] from a uniform value in [0, 1). */
    static int pick(Range range, double unit) {
        int min = (int) range.min();
        int max = (int) range.max();
        return Math.min(max, min + (int) Math.floor(unit * (max - min + 1)));
    }

    private static long key(int a, int b) {
        return (long) a << 32 | b & 0xFFFFFFFFL;
    }

    // ------------------------------------------------------------------ columns, plains

    /** The column (x, z) of the terrain: floor, ceiling and whether it is solid from bottom to top. Pure. */
    Column columnAt(int x, int z, Memory memory) {
        HallSettings h = settings.halls();
        int size = settings.cathedrals().cellSize();
        Cell cell = memory.cell(this, Math.floorDiv(x, size), Math.floorDiv(z, size));
        Cathedral cathedral = cell.cathedral();
        int ceiling = h.ceiling();
        if (cathedral != null) {
            double distance = cathedral.distance(x, z);
            if (distance <= cathedral.pillarRadius()) {
                return new Column(h.floorTop(), ceiling, true);
            }
            if (distance < cathedral.radius()) {
                double t = Math.min(1.0, (cathedral.radius() - distance) / cathedral.rim());
                ceiling += (int) Math.round((h.roof() - h.ceiling()) * RingBlend.smoothstep(t));
            }
        }
        if (gridPillar(cell, x, z)) {
            return new Column(h.floorTop(), ceiling, true);
        }
        return new Column(h.floorTop() + memory.plain(this, cell, x, z), ceiling, false);
    }

    /** Whether (x, z) lies in one of the cell's grid columns that stands: wholly inside the cell, clear of its hall and the zones. */
    private boolean gridPillar(Cell cell, int x, int z) {
        HallSettings h = settings.halls();
        int u = Math.floorMod(x - cell.originX(settings) - cell.gridX(), h.grid());
        int v = Math.floorMod(z - cell.originZ(settings) - cell.gridZ(), h.grid());
        if (u >= h.pillarSize() || v >= h.pillarSize()) {
            return false;
        }
        return pillarStands(cell, x - u, z - v);
    }

    /** The grid column whose low corner is (px, pz) stands. */
    boolean pillarStands(Cell cell, int px, int pz) {
        HallSettings h = settings.halls();
        int x0 = cell.originX(settings) + h.cellMargin();
        int z0 = cell.originZ(settings) + h.cellMargin();
        int x1 = cell.originX(settings) + settings.cathedrals().cellSize() - 1 - h.cellMargin();
        int z1 = cell.originZ(settings) + settings.cathedrals().cellSize() - 1 - h.cellMargin();
        int last = h.pillarSize() - 1;
        if (px < x0 || pz < z0 || px + last > x1 || pz + last > z1) {
            return false;
        }
        Cathedral cathedral = cell.cathedral();
        if (cathedral != null && cathedral.distanceToBox(px, pz, px + last, pz + last) <= cathedral.radius() + settings.cathedrals().clearance()) {
            return false;
        }
        return edge(px + h.pillarSize() / 2, pz + h.pillarSize() / 2) <= 0.0;
    }

    /**
     * The height of the plain on tile (i, j) of the cell's grid (its corners are the centres of four grid columns),
     * clipped to the cell: 0 near its hall or the zones, else from the plains noise at the tile's centre, quantised to
     * {@code plains_height}. Pure.
     */
    int plainOfTile(Cell cell, int i, int j) {
        HallSettings h = settings.halls();
        int size = settings.cathedrals().cellSize();
        int half = h.pillarSize() / 2;
        int ox = cell.originX(settings);
        int oz = cell.originZ(settings);
        int tx0 = ox + cell.gridX() + half + i * h.grid();
        int tz0 = oz + cell.gridZ() + half + j * h.grid();
        int x0 = Math.max(ox, tx0);
        int z0 = Math.max(oz, tz0);
        int x1 = Math.min(ox + size - 1, tx0 + h.grid() - 1);
        int z1 = Math.min(oz + size - 1, tz0 + h.grid() - 1);
        Cathedral cathedral = cell.cathedral();
        if (cathedral != null && cathedral.distanceToBox(x0, z0, x1, z1) <= cathedral.radius() + settings.cathedrals().clearance()) {
            return 0;
        }
        if (edge(x0, z0) > 0.0 || edge(x1, z0) > 0.0 || edge(x0, z1) > 0.0 || edge(x1, z1) > 0.0) {
            return 0;
        }
        double value = plains.getValue(tx0 + h.grid() * 0.5, 0.0, tz0 + h.grid() * 0.5);
        if (value < h.plainsThreshold()) {
            return 0;
        }
        int min = (int) h.plainsHeight().min();
        int max = (int) h.plainsHeight().max();
        return Math.min(max, min + (int) Math.floor((value - h.plainsThreshold()) / h.plainsStep()));
    }

    /** The tile of the cell's grid that holds (x, z): {i, j}. */
    int[] tileOf(Cell cell, int x, int z) {
        HallSettings h = settings.halls();
        int half = h.pillarSize() / 2;
        return new int[]{Math.floorDiv(x - cell.originX(settings) - cell.gridX() - half, h.grid()),
                Math.floorDiv(z - cell.originZ(settings) - cell.gridZ() - half, h.grid())};
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(array, this);
    }

    /**
     * Only the noises are wired, never a marker inside {@code edge}: like {@link GiantGeodes}, a cell must give the same
     * answer in every chunk that evaluates it. The first mapping (the random state wires the noise) starts a new map of
     * decided cells; the copies made from the wired function (one per noise chunk) keep it.
     */
    @Override
    public DensityFunction mapAll(Visitor visitor) {
        Visitor noisesOnly = new Visitor() {
            @Override
            public DensityFunction apply(DensityFunction function) {
                return function;
            }

            @Override
            public NoiseHolder visitNoise(NoiseHolder holder) {
                return visitor.visitNoise(holder);
            }
        };
        NoiseHolder wiredCells = visitor.visitNoise(cells);
        NoiseHolder wiredPlains = visitor.visitNoise(plains);
        boolean sameNoise = cells.noise() != null && wiredCells.noise() == cells.noise();
        return visitor.apply(new NetherCathedrals(output, wiredCells, wiredPlains, edge.mapAll(noisesOnly), settings,
                sameNoise ? decided : new ConcurrentHashMap<>()));
    }

    /** The map of decided cells (tests: which copies share it). */
    Map<Long, Cell> decided() {
        return decided;
    }

    @Override
    public double minValue() {
        return output == Output.MASK ? 0.0 : -BOUND;
    }

    @Override
    public double maxValue() {
        return output == Output.MASK ? 1.0 : BOUND;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    // ------------------------------------------------------------------ records

    public enum Output implements StringRepresentable {
        /** 2D, 0..1: the cathedral mask (the router's temperature of Nether ring 5). */
        MASK("mask"),
        /** 3D: the density of Nether ring 5's terrain. */
        TERRAIN("terrain");

        public static final Codec<Output> CODEC = StringRepresentable.fromEnum(Output::values);
        private final String name;

        Output(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    /**
     * A cell: its indices, the offset of its column grid (0 to {@code grid} − 1 on both axes) and its cathedral or null.
     */
    public record Cell(int cellX, int cellZ, int gridX, int gridZ, @Nullable Cathedral cathedral) {
        int originX(Settings settings) {
            return cellX * settings.cathedrals().cellSize();
        }

        int originZ(Settings settings) {
            return cellZ * settings.cathedrals().cellSize();
        }
    }

    /**
     * A cathedral: centre, the radius of its footprint (where the roof leaves the ceiling), the width of its rim (over which
     * the roof rises from the ceiling to the lifted roof) and the radius of its giant column.
     */
    public record Cathedral(int x, int z, int radius, int rim, int pillarRadius) {
        public double distance(double px, double pz) {
            return Math.hypot(px - x, pz - z);
        }

        /** 1 inside the rim, 0 at and beyond the footprint's edge, linear between. */
        public double mask(int px, int pz) {
            return Math.clamp((radius - distance(px, pz)) / rim, 0.0, 1.0);
        }

        /** The distance from the centre to the nearest point of the box (0 inside it). */
        double distanceToBox(int x0, int z0, int x1, int z1) {
            double dx = Math.max(0, Math.max(x0 - x, x - x1));
            double dz = Math.max(0, Math.max(z0 - z, z - z1));
            return Math.hypot(dx, dz);
        }
    }

    /** The column of the terrain: the floor's top block, the ceiling's lowest solid Y, and solid from bottom to top. */
    record Column(int floorTop, int ceiling, boolean solid) {
        double density(int y) {
            if (solid) {
                return BOUND;
            }
            double floor = (floorTop + 1 - y) * SLOPE;
            double roof = (y + 1 - ceiling) * SLOPE;
            return Math.clamp(Math.max(floor, roof), -BOUND, BOUND);
        }
    }

    /** {@code settings}: the cathedrals and the halls. */
    public record Settings(CathedralSettings cathedrals, HallSettings halls) {
        public static final Codec<Settings> CODEC = RecordCodecBuilder.<Settings>create(instance -> instance.group(
                CathedralSettings.CODEC.fieldOf("cathedrals").forGetter(Settings::cathedrals),
                HallSettings.CODEC.fieldOf("halls").forGetter(Settings::halls)
        ).apply(instance, Settings::new)).validate(Settings::validate);

        private static DataResult<Settings> validate(Settings settings) {
            CathedralSettings c = settings.cathedrals;
            HallSettings h = settings.halls;
            if (2 * ((int) c.radius().max() + c.clearance()) >= c.cellSize()) {
                return DataResult.error(() -> "nether_cathedrals: a cathedral with its clearance must fit in its cell");
            }
            // the smallest hall with the widest rim must still hold the widest giant column inside its rim
            if (c.pillarRadius().max() >= c.radius().min() - c.rim().max()) {
                return DataResult.error(() -> "nether_cathedrals: the giant column must stand inside the rim");
            }
            if (h.pillarSize() + 2 > h.grid()) {
                return DataResult.error(() -> "nether_cathedrals: the grid must leave room between its columns");
            }
            if (!(h.floorTop() < h.ceiling() && h.ceiling() <= h.roof())) {
                return DataResult.error(() -> "nether_cathedrals: floor < ceiling <= roof");
            }
            if (h.roof() > ROOF_MAX) {
                return DataResult.error(() -> "nether_cathedrals: the roof must lie below the Nether's netherrack and bedrock roof (Y " + ROOF_MAX + ")");
            }
            if (h.floorTop() + (int) h.plainsHeight().max() >= h.ceiling()) {
                return DataResult.error(() -> "nether_cathedrals: the highest plain must stay below the ceiling");
            }
            return DataResult.success(settings);
        }
    }

    /**
     * The cathedrals: the cell size, the chance of one per cell, the candidates tried, the radius of the footprint, the
     * rim, the giant column's radius, and the clearance (no grid column or plain this close to the footprint).
     */
    public record CathedralSettings(int cellSize, double chance, int attempts, Range radius, Range rim, Range pillarRadius, int clearance) {
        public static final Codec<CathedralSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(64, 2048).fieldOf("cell_size").forGetter(CathedralSettings::cellSize),
                Codec.doubleRange(0.0, 1.0).fieldOf("chance").forGetter(CathedralSettings::chance),
                Codec.intRange(1, 64).fieldOf("attempts").forGetter(CathedralSettings::attempts),
                Range.within(4.0, 512.0).fieldOf("radius").forGetter(CathedralSettings::radius),
                Range.within(1.0, 64.0).fieldOf("rim").forGetter(CathedralSettings::rim),
                Range.within(1.0, 128.0).fieldOf("pillar_radius").forGetter(CathedralSettings::pillarRadius),
                Codec.intRange(0, 256).fieldOf("clearance").forGetter(CathedralSettings::clearance)
        ).apply(instance, CathedralSettings::new));
    }

    /**
     * The halls: the floor's top block, the ceiling's and the cathedral roof's lowest solid Y, the column grid (spacing,
     * square size, kept this far inside its cell), and the plains (raised where the plains noise is at least
     * {@code plains_threshold}, one block per {@code plains_step} above it, within {@code plains_height}).
     */
    public record HallSettings(int floorTop, int ceiling, int roof, int grid, int pillarSize, int cellMargin,
                               double plainsThreshold, double plainsStep, Range plainsHeight) {
        public static final Codec<HallSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(0, 127).fieldOf("floor_top").forGetter(HallSettings::floorTop),
                Codec.intRange(0, 127).fieldOf("ceiling").forGetter(HallSettings::ceiling),
                Codec.intRange(0, 127).fieldOf("roof").forGetter(HallSettings::roof),
                Codec.intRange(4, 256).fieldOf("grid").forGetter(HallSettings::grid),
                Codec.intRange(1, 64).fieldOf("pillar_size").forGetter(HallSettings::pillarSize),
                Codec.intRange(0, 64).fieldOf("cell_margin").forGetter(HallSettings::cellMargin),
                Codec.DOUBLE.fieldOf("plains_threshold").forGetter(HallSettings::plainsThreshold),
                Codec.doubleRange(1e-6, 10.0).fieldOf("plains_step").forGetter(HallSettings::plainsStep),
                Range.within(0.0, 32.0).fieldOf("plains_height").forGetter(HallSettings::plainsHeight)
        ).apply(instance, HallSettings::new));
    }

    /** {@link #pillarAt}, {@link #floorTop} and {@link #ceiling} with a memory of their own (one user at a time). */
    public final class Reader {
        private final Memory memory = new Memory();

        private Reader() {
        }

        public boolean pillarAt(int x, int z) {
            return memory.column(NetherCathedrals.this, x, z).solid();
        }

        public int floorTop(int x, int z) {
            return memory.column(NetherCathedrals.this, x, z).floorTop();
        }

        public int ceiling(int x, int z) {
            return memory.column(NetherCathedrals.this, x, z).ceiling();
        }
    }

    /** Per thread: the last cells, tiles and columns it asked for (a chunk touches one to four cells). */
    static final class Memory {
        private final long[] cellKeys = new long[CELL_MEMORY];
        private final Cell[] cells = new Cell[CELL_MEMORY];
        private final long[] tileKeys = new long[TILE_MEMORY];
        private final Cell[] tileCells = new Cell[TILE_MEMORY];
        private final int[] tiles = new int[TILE_MEMORY];
        private final long[] columnKeys = new long[COLUMN_MEMORY];
        private final Column[] columns = new Column[COLUMN_MEMORY];

        Cell cell(NetherCathedrals owner, int cellX, int cellZ) {
            long key = key(cellX, cellZ);
            int index = (int) ((cellX * 31L + cellZ * 17L) & (CELL_MEMORY - 1));
            Cell cell = cells[index];
            if (cell == null || cellKeys[index] != key) {
                cell = owner.shared(cellX, cellZ);
                cells[index] = cell;
                cellKeys[index] = key;
            }
            return cell;
        }

        int plain(NetherCathedrals owner, Cell cell, int x, int z) {
            int[] tile = owner.tileOf(cell, x, z);
            long key = key(tile[0], tile[1]);
            int index = (int) ((tile[0] * 31L + tile[1] * 17L) & (TILE_MEMORY - 1));
            if (tileCells[index] != cell || tileKeys[index] != key) {
                tiles[index] = owner.plainOfTile(cell, tile[0], tile[1]);
                tileKeys[index] = key;
                tileCells[index] = cell;
            }
            return tiles[index];
        }

        Column column(NetherCathedrals owner, int x, int z) {
            long key = key(x, z);
            int index = (x & 15) | (z & 15) << 4;
            Column column = columns[index];
            if (column == null || columnKeys[index] != key) {
                column = owner.columnAt(x, z, this);
                columns[index] = column;
                columnKeys[index] = key;
            }
            return column;
        }
    }
}
