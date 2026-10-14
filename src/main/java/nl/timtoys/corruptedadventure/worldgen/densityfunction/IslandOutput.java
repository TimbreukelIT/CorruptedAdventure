package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * {@code corruptedadventure:island_output}: one output of an {@code island_field} ({@code field}, usually a reference
 * to {@code ring_4/overworld/islands}), like {@code volcanic_output} for ring 3. All outputs of one field share its
 * column cache: the noise chunk's memo maps equal fields to one instance.
 */
public final class IslandOutput implements DensityFunction {
    public static final MapCodec<IslandOutput> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("field").forGetter(IslandOutput::field),
            Output.CODEC.fieldOf("output").forGetter(IslandOutput::output)
    ).apply(instance, IslandOutput::new));

    public static final KeyDispatchDataCodec<IslandOutput> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    public enum Output implements StringRepresentable {
        /** The base terrain's surface height (blocks), without the islands' holes and without caves. */
        HEIGHT("height", -64.0, 320.0),
        /** The surface height with the holes (what the terrain builds; blocks). */
        SURFACE("surface", -64.0, 320.0),
        /** A lower bound of the surface around the column (the preliminary surface). */
        INITIAL_HEIGHT("initial_height", -64.0, 320.0),
        /** 0..1: an island in the column. */
        ISLAND_MASK("island_mask", 0.0, 1.0),
        /** 0..1: an island's mirror lake under it (1 on the lake's floor). */
        ISLAND_HOLE("island_hole", 0.0, 1.0),
        /** 0..1: a river's channel, 1 on its bed. */
        RIVER("river", 0.0, 1.0),
        /** 0..1: a fjord's channel, 1 on its floor. */
        FJORD("fjord", 0.0, 1.0),
        /** 0..1: the cliff 3→4's wall (1 on it) or scree slope (62). */
        CLIFF("cliff", 0.0, 1.0),
        /** 0..1: how far the ground is sundered here (63; the cracks and loose chunks of the zone 4→5). */
        SUNDERED("sundered", 0.0, 1.0),
        /** 3D, not interpolated: +1 inside an island, −1 in a carved opening, 0 elsewhere (the terrain decides). */
        THIN("thin", -1.0, 1.0),
        /** 3D: added to the density of vanilla's caves, 0 where they run freely, up to 2 where they are gone. */
        CAVE_LIFT("cave_lift", 0.0, 2.0);

        public static final StringRepresentable.EnumCodec<Output> CODEC = StringRepresentable.fromEnum(Output::values);

        private final String name;
        private final double min;
        private final double max;

        Output(String name, double min, double max) {
            this.name = name;
            this.min = min;
            this.max = max;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private final DensityFunction field;
    private final Output output;
    // Resolved once: right away for a mapped (wired) field, on first use for a decoded holder reference.
    private volatile IslandField resolved;

    public IslandOutput(DensityFunction field, Output output) {
        this.field = field;
        this.output = output;
        this.resolved = IslandField.resolveIfBound(field);
    }

    public DensityFunction field() {
        return field;
    }

    public Output output() {
        return output;
    }

    private IslandField resolved() {
        IslandField f = resolved;
        if (f == null) {
            f = IslandField.resolve(field);
            if (f == null) {
                throw new IllegalStateException("island_output needs an island_field, got " + field);
            }
            resolved = f;
        }
        return f;
    }

    @Override
    public double compute(FunctionContext context) {
        IslandField f = resolved();
        int x = context.blockX();
        int z = context.blockZ();
        return switch (output) {
            case INITIAL_HEIGHT -> f.initialHeight(x, z);
            case THIN -> f.thin(x, context.blockY(), z);
            case CAVE_LIFT -> f.caveLift(x, context.blockY(), z);
            case ISLAND_MASK -> f.islandColumn(x, z).islandMask;
            default -> columnValue(f.column(x, z), output);
        };
    }

    /** A per-column output of one column. */
    public static double columnValue(IslandField.Column column, Output output) {
        return switch (output) {
            case HEIGHT -> column.height;
            case SURFACE -> column.surface;
            case ISLAND_MASK -> column.islandMask;
            case ISLAND_HOLE -> column.islandHole;
            case RIVER -> column.river;
            case FJORD -> column.fjord;
            case CLIFF -> column.cliff;
            case SUNDERED -> column.sundered;
            case INITIAL_HEIGHT, THIN, CAVE_LIFT -> throw new IllegalArgumentException(output + " is not a column value");
        };
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new IslandOutput(field.mapAll(visitor), output));
    }

    @Override
    public double minValue() {
        return output.min;
    }

    @Override
    public double maxValue() {
        return output.max;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof IslandOutput other && field.equals(other.field) && output == other.output;
    }

    @Override
    public int hashCode() {
        return 31 * field.hashCode() + output.hashCode();
    }
}
