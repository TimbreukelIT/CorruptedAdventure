package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * {@code corruptedadventure:rock_pillars}: free-standing rock shapes (needles, boulders, fins with an optional hole =
 * arch), at most one per cell of {@code cell_size}. Density +1 inside a shape, −1 elsewhere; meant to be combined with
 * the terrain by {@code max}, per block, outside the interpolation (thin shapes do not survive interpolation).
 * <p>
 * A cell gets a shape with {@code chance}; the shape is drawn from {@code shapes} (by weight) and placed so that it lies
 * entirely inside the cell minus {@code margin}, so a column only ever looks at its own cell. The cell is dropped when
 * {@code weight} (lowest value over the footprint) is below {@code min_weight}, when the footprint comes within
 * {@code exclude_below} blocks of {@code exclude} (a distance field, e.g. the slot network), or when {@code floor}
 * varies more than {@code max_floor_step} over the footprint (no shapes on walls or terraces). {@code weight} scales
 * height and radius. Every column of a shape runs down to the lowest floor point of the footprint − 2, or to
 * {@code root_y} if that is lower: {@code floor} is the smooth terrain height and knows nothing of caves, so a shape
 * over a cave mouth would otherwise hang in the air; with a deep root it continues down through the cave as a column.
 * <p>
 * Pure: the cell hash comes from the {@code hash} noise (seeded), the caches only remember results.
 */
public final class RockPillars implements DensityFunction {
    private static final long CELL_SALT = 7;
    private static final int SAMPLES = 3;

    public static final MapCodec<RockPillars> MAP_CODEC = RecordCodecBuilder.<RockPillars>mapCodec(instance -> instance.group(
                    NoiseHolder.CODEC.fieldOf("hash").forGetter(r -> r.hash),
                    Codec.intRange(8, 1024).fieldOf("cell_size").forGetter(r -> r.cellSize),
                    Codec.doubleRange(0.0, 1.0).fieldOf("chance").forGetter(r -> r.chance),
                    Codec.intRange(0, 16).optionalFieldOf("margin", 1).forGetter(r -> r.margin),
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("weight").forGetter(r -> r.weight),
                    Codec.doubleRange(0.0, 1.0).optionalFieldOf("min_weight", 0.3).forGetter(r -> r.minWeight),
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("floor").forGetter(r -> r.floor),
                    DensityFunction.HOLDER_HELPER_CODEC.optionalFieldOf("exclude").forGetter(r -> r.exclude),
                    Codec.doubleRange(0.0, 1024.0).optionalFieldOf("exclude_below", 0.0).forGetter(r -> r.excludeBelow),
                    Codec.doubleRange(0.0, 1024.0).optionalFieldOf("max_floor_step", 1024.0).forGetter(r -> r.maxFloorStep),
                    Codec.intRange(-64, 320).optionalFieldOf("root_y").forGetter(r -> r.rootY),
                    ExtraCodecs.nonEmptyList(WeightedShape.CODEC.listOf()).fieldOf("shapes").forGetter(r -> r.shapes)
            ).apply(instance, RockPillars::new))
            .validate(RockPillars::validate);

    public static final KeyDispatchDataCodec<RockPillars> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private final NoiseHolder hash;
    private final int cellSize;
    private final double chance;
    private final int margin;
    private final DensityFunction weight;
    private final double minWeight;
    private final DensityFunction floor;
    private final Optional<DensityFunction> exclude;
    private final double excludeBelow;
    private final double maxFloorStep;
    private final Optional<Integer> rootY;
    private final List<WeightedShape> shapes;
    private final int totalWeight;
    private final ColumnTable<Column> columns = new ColumnTable<>(Column::new);
    private final ThreadOwned<CellCache> cells = new ThreadOwned<>(CellCache::new);

    public RockPillars(NoiseHolder hash, int cellSize, double chance, int margin, DensityFunction weight, double minWeight,
                       DensityFunction floor, Optional<DensityFunction> exclude, double excludeBelow, double maxFloorStep,
                       Optional<Integer> rootY, List<WeightedShape> shapes) {
        this.hash = hash;
        this.cellSize = cellSize;
        this.chance = chance;
        this.margin = margin;
        this.weight = weight;
        this.minWeight = minWeight;
        this.floor = floor;
        this.exclude = exclude;
        this.excludeBelow = excludeBelow;
        this.maxFloorStep = maxFloorStep;
        this.rootY = rootY;
        this.shapes = List.copyOf(shapes);
        this.totalWeight = shapes.stream().mapToInt(WeightedShape::weight).sum();
    }

    private static DataResult<RockPillars> validate(RockPillars pillars) {
        if (pillars.cellSize - 2 * pillars.margin < 4) {
            return DataResult.error(() -> "rock_pillars needs cell_size - 2 * margin >= 4");
        }
        return pillars.totalWeight > 0 ? DataResult.success(pillars) : DataResult.error(() -> "rock_pillars needs a shape weight > 0");
    }

    @Override
    public double compute(FunctionContext context) {
        Column column = columns.get(context.blockX(), context.blockZ(), this::fillColumn);
        int y = context.blockY();
        return column.contains(y) ? 1.0 : -1.0;
    }

    /** The intervals of solid blocks at a column: [lo1, hi1] or [lo1, hi1, lo2, hi2], or none. */
    int[] solidAt(int x, int z) {
        Column column = columns.get(x, z, this::fillColumn);
        return switch (column.count) {
            case 0 -> new int[0];
            case 1 -> new int[]{column.lo1, column.hi1};
            default -> new int[]{column.lo1, column.hi1, column.lo2, column.hi2};
        };
    }

    private void fillColumn(Column column, int x, int z) {
        Placed placed = cells.get().find(this, Math.floorDiv(x, cellSize), Math.floorDiv(z, cellSize));
        column.count = 0;
        if (placed != null) {
            placed.shape.column(placed, x + 0.5, z + 0.5, column);
        }
    }

    /** The shape of cell (i, j), or null. Pure. */
    Placed place(int i, int j) {
        long cellHash = SeededHash.hash(hash, i, j, CELL_SALT);
        if (SeededHash.unit(cellHash, 0) >= chance) {
            return null;
        }
        Shape shape = pick(SeededHash.unit(cellHash, 1));
        Shape.Draw draw = shape.draw(cellHash, hash, i, j, cellSize);
        double room = cellSize / 2.0 - margin;
        // Shrink until the footprint fits (a few rounds: not every part of a footprint shrinks with the shape).
        for (int round = 0; round < 8 && (draw.halfX() > room || draw.halfZ() > room); round++) {
            draw = draw.scaled(0.98 * Math.min(room / draw.halfX(), room / draw.halfZ()));
        }
        if (draw.halfX() > room || draw.halfZ() > room) {
            return null;
        }
        double cx = i * cellSize + margin + draw.halfX() + SeededHash.unit(cellHash, 2) * (cellSize - 2.0 * margin - 2.0 * draw.halfX());
        double cz = j * cellSize + margin + draw.halfZ() + SeededHash.unit(cellHash, 3) * (cellSize - 2.0 * margin - 2.0 * draw.halfZ());
        double lowestWeight = Double.MAX_VALUE;
        double lowestFloor = Double.MAX_VALUE;
        double highestFloor = -Double.MAX_VALUE;
        for (int a = 0; a < SAMPLES; a++) {
            for (int b = 0; b < SAMPLES; b++) {
                double sx = cx + draw.halfX() * (a - 1);
                double sz = cz + draw.halfZ() * (b - 1);
                lowestWeight = Math.min(lowestWeight, onGrid(weight, sx, sz));
                double f = onGrid(floor, sx, sz);
                lowestFloor = Math.min(lowestFloor, f);
                highestFloor = Math.max(highestFloor, f);
            }
        }
        if (lowestWeight < minWeight || highestFloor - lowestFloor > maxFloorStep) {
            return null;
        }
        if (exclude.isPresent() && exclude.get().compute(new SinglePointContext(Mth.floor(cx), 0, Mth.floor(cz)))
                < Math.hypot(draw.halfX(), draw.halfZ()) + excludeBelow) {
            return null;
        }
        double w = Math.min(1.0, lowestWeight);
        int base = Mth.floor(lowestFloor) - 2;
        if (rootY.isPresent()) {
            base = Math.min(base, rootY.get());
        }
        return new Placed(shape, draw.weighted(w), cx, cz, Mth.floor(lowestFloor), Mth.floor(highestFloor), base, cellHash);
    }

    /**
     * A 2D input read only on the 4-block grid and interpolated bilinearly. Inside a noise chunk a {@code flat_cache}
     * gives the value of the grid point below a position (and the exact value outside the chunk); a cell overlaps several
     * chunks, so reading off the grid would let each chunk place the cell's shape differently (cut-off rocks at chunk
     * borders). Grid points read the same everywhere.
     */
    private static double onGrid(DensityFunction function, double x, double z) {
        int x0 = Mth.floor(x / 4.0) * 4;
        int z0 = Mth.floor(z / 4.0) * 4;
        return Mth.lerp2((x - x0) / 4.0, (z - z0) / 4.0,
                function.compute(new SinglePointContext(x0, 0, z0)), function.compute(new SinglePointContext(x0 + 4, 0, z0)),
                function.compute(new SinglePointContext(x0, 0, z0 + 4)), function.compute(new SinglePointContext(x0 + 4, 0, z0 + 4)));
    }

    private Shape pick(double unit) {
        int target = (int) (unit * totalWeight);
        for (WeightedShape entry : shapes) {
            target -= entry.weight;
            if (target < 0) {
                return entry.shape;
            }
        }
        return shapes.getLast().shape;
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new RockPillars(visitor.visitNoise(hash), cellSize, chance, margin, weight.mapAll(visitor), minWeight,
                floor.mapAll(visitor), exclude.map(e -> e.mapAll(visitor)), excludeBelow, maxFloorStep, rootY, shapes));
    }

    @Override
    public double minValue() {
        return -1.0;
    }

    @Override
    public double maxValue() {
        return 1.0;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    /** {@code function} as a {@code rock_pillars}, looking through holder references and markers. */
    static RockPillars resolve(DensityFunction function) {
        DensityFunction current = function;
        for (int depth = 0; depth < 16; depth++) {
            if (current instanceof RockPillars pillars) {
                return pillars;
            }
            if (current instanceof net.minecraft.world.level.levelgen.DensityFunctions.HolderHolder holder) {
                current = holder.function().value();
            } else if (current instanceof net.minecraft.world.level.levelgen.DensityFunctions.MarkerOrMarked marker) {
                current = marker.wrapped();
            } else {
                break;
            }
        }
        throw new IllegalStateException("Expected a corruptedadventure:rock_pillars density function, got " + function);
    }

    int cellSize() {
        return cellSize;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RockPillars r && hash.equals(r.hash) && cellSize == r.cellSize && chance == r.chance && margin == r.margin
                && weight.equals(r.weight) && minWeight == r.minWeight && floor.equals(r.floor) && exclude.equals(r.exclude)
                && excludeBelow == r.excludeBelow && maxFloorStep == r.maxFloorStep && rootY.equals(r.rootY) && shapes.equals(r.shapes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash, cellSize, chance, margin, weight, minWeight, floor, exclude, excludeBelow, maxFloorStep, rootY, shapes);
    }

    /** Solid blocks of one column: up to two closed intervals (two around the hole of an arch). */
    static final class Column {
        int count;
        int lo1;
        int hi1;
        int lo2;
        int hi2;

        boolean contains(int y) {
            return count > 0 && y >= lo1 && y <= hi1 || count > 1 && y >= lo2 && y <= hi2;
        }

        void add(int lo, int hi) {
            if (hi < lo) {
                return;
            }
            if (count == 0) {
                lo1 = lo;
                hi1 = hi;
            } else {
                lo2 = lo;
                hi2 = hi;
            }
            count++;
        }
    }

    /**
     * A shape in a cell. {@code floor}/{@code ceilingFloor} are the lowest/highest floor over the footprint,
     * {@code base} = lowest − 2, where every column starts.
     */
    record Placed(Shape shape, Shape.Draw draw, double x, double z, int floor, int ceilingFloor, int base, long hash) {
    }

    record WeightedShape(int weight, Shape shape) {
        static final Codec<WeightedShape> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(0, 10_000).fieldOf("weight").forGetter(WeightedShape::weight),
                Shape.CODEC.fieldOf("shape").forGetter(WeightedShape::shape)
        ).apply(instance, WeightedShape::new));
    }

    /** Direct-mapped per-thread cache of cell decisions; only a cache. */
    private static final class CellCache {
        private static final int SIZE = 16;
        private final long[] keys = new long[SIZE];
        private final boolean[] filled = new boolean[SIZE];
        private final Placed[] placed = new Placed[SIZE];

        Placed find(RockPillars pillars, int i, int j) {
            long key = (long) i << 32 | j & 0xFFFFFFFFL;
            int slot = (i & 3) | (j & 3) << 2;
            if (!filled[slot] || keys[slot] != key) {
                filled[slot] = true;
                keys[slot] = key;
                placed[slot] = pillars.place(i, j);
            }
            return placed[slot];
        }
    }
}
