package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingPosition;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * {@code corruptedadventure:zone_fracture}: the fracture zone 4→5 (ring 5, package 85, V7-A, V8-A), wrapped around the
 * overworld's {@code noise_router.final_density}, so it cuts after the ring blend and ring 4 and ring 5 see the same
 * fracture: cracks become air down to their dry floor, raised floes, landmarks and loose chunks extra rock. The shape is
 * {@link ZoneFractureShape}; one owner (W-terrein).
 * <p>
 * Outside the zone 4→5 and in its first and last {@link ZoneFractureShape#CALM} blocks it is exactly {@link #argument}
 * (bit-equal; {@code fillArray} lets the argument fill the array and only touches points in the zone). The ground's height
 * under the cuts comes from the two terrains it blends: ring 4's {@link IslandField} and ring 5's {@link Ring5Field},
 * found in the argument when the router is wired (a fixed height where they are missing, as in tests).
 * <p>
 * {@link #mapAll} hands the visitor on to the argument, so the ring blend's gates and the fields found through the
 * router ({@code IslandField.find}, {@code Ring5Field.find}) stay where they were.
 */
public final class ZoneFracture implements DensityFunction {
    public static final MapCodec<ZoneFracture> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument").forGetter(ZoneFracture::argument),
            NoiseHolder.CODEC.fieldOf("hash").forGetter(ZoneFracture::hash)
    ).apply(instance, (argument, hash) -> new ZoneFracture(argument, hash, RingLayout::current)));

    public static final KeyDispatchDataCodec<ZoneFracture> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    /**
     * The calm first share of the zone 4→5 (no crack cuts there: {@link ZoneFractureShape#CALM} of the default 256 blocks),
     * for the fluid rules ({@code RingFluidPicker.fractureSealBelow}).
     */
    public static final double CALM_SHARE = ZoneFractureShape.CALM / 256.0;
    /** Shared with {@link ZoneFractureValue}: the same salt from the same hash noise. */
    static final long WORLD_SALT = 4505;
    /** The ground's height where neither terrain is found (tests with their own argument). */
    static final double NO_TERRAIN_HEIGHT = 72.0;
    /**
     * The terrains' density per block near their surface, for the ground under the blend: ring 4's terrain is scaled by
     * 0.64 in its noise router (about 1/8 · 0.64), ring 5's is 1/8.
     */
    private static final double RING_4_SLOPE = 0.08;
    private static final double RING_5_SLOPE = 0.125;
    private static final int FLOOR_SALT = 8510;

    private final DensityFunction argument;
    private final NoiseHolder hash;
    private final Supplier<RingLayout> layout;
    private final Shared shared;
    private final ColumnTable<Column> columns = new ColumnTable<>(Column::new);

    ZoneFracture(DensityFunction argument, NoiseHolder hash, Supplier<RingLayout> layout) {
        this(argument, hash, layout, new Shared());
    }

    private ZoneFracture(DensityFunction argument, NoiseHolder hash, Supplier<RingLayout> layout, Shared shared) {
        this.argument = argument;
        this.hash = hash;
        this.layout = layout;
        this.shared = shared;
    }

    public DensityFunction argument() {
        return argument;
    }

    public NoiseHolder hash() {
        return hash;
    }

    /** The first {@code zone_fracture} in {@code root} (e.g. a random state's final density), or null. */
    @Nullable
    public static ZoneFracture find(DensityFunction root) {
        return root instanceof ZoneFracture fracture ? fracture : null;
    }

    /**
     * How strongly the zone is fractured at an effective distance ({@link RingLayout#effectiveDistance}), 0..1: 0 outside
     * the zone 4→5 and on its edges ({@link ZoneFractureShape#CALM}), full in its middle.
     */
    public static double strength(RingLayout layout, double effectiveDistance) {
        return ZoneFractureShape.strength(layout, effectiveDistance);
    }

    /** Whether the block is cut open by a crack (air after the blend, from the crack's floor up). */
    public boolean crackAt(int x, int y, int z) {
        Column c = column(x, z);
        return c.info.kind == ZoneFractureShape.CRACK && y >= c.floor;
    }

    /** Whether the block is extra rock of a raised floe, a landmark or a loose chunk. */
    public boolean floeAt(int x, int y, int z) {
        Column c = column(x, z);
        return y >= c.chunkBottom && y < c.chunkTop
                || c.info.kind != ZoneFractureShape.CRACK && y >= c.extraBottom && y < c.extraTop;
    }

    /**
     * The landmarks of the zone (raised flat floes, ~650 apart, V8-A) whose top comes into the box (blocks, inclusive): for
     * the split rock (W-biomes, 86c).
     */
    public List<Landmark> landmarksWithin(int minX, int minZ, int maxX, int maxZ) {
        List<Landmark> found = new ArrayList<>();
        for (Landmark landmark : landmarks()) {
            if (landmark.x() + landmark.radius() >= minX && landmark.x() - landmark.radius() <= maxX
                    && landmark.z() + landmark.radius() >= minZ && landmark.z() - landmark.radius() <= maxZ) {
                found.add(landmark);
            }
        }
        return found;
    }

    /** All landmarks round the zone, decided once per world. */
    public List<Landmark> landmarks() {
        List<Landmark> known = shared.landmarks;
        if (known == null) {
            ZoneFractureShape shape = shape();
            List<Landmark> list = new ArrayList<>();
            for (int k = 0; k < shape.landmarkCount(); k++) {
                double[] l = shape.landmark(k);
                int x = Mth.floor(l[0]);
                int z = Mth.floor(l[1]);
                list.add(new Landmark(x, z, (int) Math.round(groundHeight(x, z) + l[3]), (int) l[2]));
            }
            known = List.copyOf(list);
            shared.landmarks = known;
        }
        return known;
    }

    // ---------------------------------------------------------------- density function

    @Override
    public double compute(FunctionContext context) {
        double value = argument.compute(context);
        int x = context.blockX();
        int z = context.blockZ();
        return mayCut(x, z) ? cut(value, x, context.blockY(), z) : value;
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        argument.fillArray(array, contextProvider);
        for (int i = 0; i < array.length; i++) {
            FunctionContext context = contextProvider.forIndex(i);
            int x = context.blockX();
            int z = context.blockZ();
            if (mayCut(x, z)) {
                array[i] = cut(array[i], x, context.blockY(), z);
            }
        }
    }

    /** A cheap test on the plain radius: whether the column can lie where the fracture cuts. */
    private boolean mayCut(int x, int z) {
        double[] annulus = shared.annulus;
        if (annulus == null) {
            RingLayout l = layout.get();
            double margin = l.settings().boundaryNoise() + 2.0;
            double inner = Ring4Sundered.zoneStart(l) + ZoneFractureShape.CALM - margin;
            double outer = Ring4Sundered.zoneEnd(l) - ZoneFractureShape.CALM + margin;
            annulus = new double[]{inner * inner, outer * outer};
            shared.annulus = annulus;
        }
        double r2 = (double) x * x + (double) z * z;
        return r2 > annulus[0] && r2 < annulus[1];
    }

    private double cut(double value, int x, int y, int z) {
        Column c = column(x, z);
        if (c.info.strength <= 0.0) {
            return value;
        }
        if (y >= c.chunkBottom && y < c.chunkTop) {
            return 1.0;
        }
        if (c.info.kind == ZoneFractureShape.CRACK) {
            return y >= c.floor ? -1.0 : value;
        }
        if (y >= c.extraBottom && y < c.extraTop) {
            return 1.0;
        }
        return value;
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        DensityFunction mapped = argument.mapAll(visitor);
        NoiseHolder mappedHash = visitor.visitNoise(hash);
        // A noise chunk's copy keeps the random state's decisions and terrains (same noise, same results).
        Shared next = mappedHash.equals(hash) ? shared : new Shared();
        if (next.terrains == null) {
            next.terrains = Terrains.of(mapped);
        }
        return visitor.apply(new ZoneFracture(mapped, mappedHash, layout, next));
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
        return o instanceof ZoneFracture f && argument.equals(f.argument) && hash.equals(f.hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(argument, hash);
    }

    // ---------------------------------------------------------------- columns

    ZoneFractureShape shape() {
        RingLayout current = layout.get();
        ZoneFractureShape known = shared.shape;
        if (known == null || known.layout() != current) {
            known = new ZoneFractureShape(current, SeededHash.hash(hash, 4321, -6789, WORLD_SALT));
            shared.shape = known;
        }
        return known;
    }

    Column column(int x, int z) {
        return columns.get(x, z, this::fill);
    }

    private void fill(Column c, int x, int z) {
        ZoneFractureShape shape = shape();
        shape.fill(c.info, x, z, c.landmark);
        c.floor = Integer.MAX_VALUE;
        c.extraBottom = Integer.MAX_VALUE;
        c.extraTop = Integer.MIN_VALUE;
        c.chunkBottom = Integer.MAX_VALUE;
        c.chunkTop = Integer.MIN_VALUE;
        c.ground = Double.NaN;
        if (c.info.strength <= 0.0) {
            return;
        }
        double ground = groundHeight(x, z);
        c.ground = ground;
        if (c.info.kind == ZoneFractureShape.CRACK) {
            // A dry floor of rubble: rough by a block or two.
            long h = CellShapes.cell(shape.salt(), x, z, FLOOR_SALT);
            c.floor = (int) Math.round(ground - c.info.depth + ZoneFractureShape.FLOOR_ROUGH * (2.0 * CellShapes.unit(h, 0) - 1.0));
        } else if (c.info.landmark >= 0) {
            Landmark landmark = landmarks().get(c.info.landmark);
            c.extraBottom = (int) Math.floor(ground - ZoneFractureShape.FLOE_ROOT);
            c.extraTop = landmark.topY();
        } else if (!Double.isNaN(c.info.floeLift) && c.info.floeLift > 0.0) {
            c.extraBottom = (int) Math.floor(ground - ZoneFractureShape.FLOE_ROOT);
            c.extraTop = (int) Math.round(ground + c.info.floeLift);
        }
        if (!Double.isNaN(c.info.chunkAbove)) {
            c.chunkTop = (int) Math.round(ground + c.info.chunkAbove);
            c.chunkBottom = c.chunkTop - (int) Math.round(c.info.chunkThickness);
        }
    }

    /**
     * The ground's height under the blend at a column: where the two terrains' densities (each about linear near its
     * surface) mix to zero with the ring blend's weight.
     */
    double groundHeight(int x, int z) {
        Terrains terrains = shared.terrains;
        if (terrains == null || terrains.islands() == null && terrains.ring5() == null) {
            return NO_TERRAIN_HEIGHT;
        }
        RingPosition position = layout.get().sampleOverworld(x, z);
        double weight = position.inTransition() ? RingBlend.smoothstep(position.blend()) : position.ring().ordinal() >= 4 ? 1.0 : 0.0;
        double ring4 = terrains.islands() != null ? terrains.islands().column(x, z).surface() : NO_TERRAIN_HEIGHT;
        double ring5 = terrains.ring5() != null ? terrains.ring5().column(x, z).top() : NO_TERRAIN_HEIGHT;
        if (Double.isNaN(ring5)) {
            ring5 = ring4;
        }
        double a = (1.0 - weight) * RING_4_SLOPE;
        double b = weight * RING_5_SLOPE;
        return (a * ring4 + b * ring5) / (a + b);
    }

    /** A landmark of the zone (V8-A): a raised flat floe with its top (the Y above its highest block) and radius. */
    public record Landmark(int x, int z, int topY, int radius) {
    }

    /** Per column: the shape's decisions and the cuts' heights. */
    static final class Column {
        final ZoneFractureShape.Info info = new ZoneFractureShape.Info();
        final double[] landmark = new double[4];
        double ground;
        /** A crack's lowest air block. */
        int floor;
        /** Extra rock of a raised floe or landmark, [bottom, top). */
        int extraBottom;
        int extraTop;
        /** A loose chunk, [bottom, top). */
        int chunkBottom;
        int chunkTop;
    }

    /** The terrains under the blend, found once in the wired router. */
    record Terrains(@Nullable IslandField islands, @Nullable Ring5Field ring5) {
        @Nullable
        static Terrains of(DensityFunction root) {
            try {
                return new Terrains(IslandField.find(root), Ring5Field.find(root));
            } catch (IllegalStateException unbound) {
                // A decoded router whose references are not bound yet: found when it is wired.
                return null;
            }
        }
    }

    /** Shared by a wired instance and the noise chunks' copies: pure per-world values. */
    static final class Shared {
        volatile ZoneFractureShape shape;
        volatile List<Landmark> landmarks;
        volatile double[] annulus;
        volatile Terrains terrains;
    }
}
