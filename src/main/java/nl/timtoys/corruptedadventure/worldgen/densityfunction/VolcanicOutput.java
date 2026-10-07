package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * {@code corruptedadventure:volcanic_output}: one output of a {@code volcanic_field} ({@code field}, usually a
 * reference to {@code ring_3/overworld/field}). All outputs of one field share its column cache: the noise chunk's
 * memo maps equal fields to one instance.
 */
public final class VolcanicOutput implements DensityFunction {
    public static final MapCodec<VolcanicOutput> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("field").forGetter(VolcanicOutput::field),
            Output.CODEC.fieldOf("output").forGetter(VolcanicOutput::output)
    ).apply(instance, VolcanicOutput::new));

    public static final KeyDispatchDataCodec<VolcanicOutput> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    public enum Output implements StringRepresentable {
        /** Initial height: a lower bound of the surface within 16 blocks (for the preliminary surface). */
        INITIAL_HEIGHT("initial_height", -64.0, 320.0),
        /** 0..1: inside a crater, 1 on the floor. */
        CALDERA_MASK("caldera_mask", 0.0, 1.0),
        /** 0..1: inside a dormant crater, 1 on the floor. */
        CALDERA_DORMANT("caldera_dormant", 0.0, 1.0),
        /** 0..1: lava channel bed, 1 in the middle. */
        LAVA_CHANNEL("lava_channel", 0.0, 1.0),
        /** 0..1: a floating island above the column. */
        ISLAND_MASK("island_mask", 0.0, 1.0),
        /** 0..1: steepness of volcano flanks and crater walls, for crags. */
        ROUGHNESS("roughness", 0.0, 1.0),
        /** 3D: +1 where caves stay out (around lava lakes and channels), −1 elsewhere. */
        GUARD("guard", -1.0, 1.0),
        /** 3D: density of the floating islands. */
        ISLANDS("islands", -1.0, 1.0);

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
    // Resolved once: right away for a mapped (wired) field, on first use for a decoded holder reference, which is only
    // bound after the registries load. A reference to anything but a volcanic_field fails on that first use.
    private volatile VolcanicField resolved;

    public VolcanicOutput(DensityFunction field, Output output) {
        this.field = field;
        this.output = output;
        this.resolved = VolcanicField.resolveIfBound(field);
    }

    public DensityFunction field() {
        return field;
    }

    public Output output() {
        return output;
    }

    private VolcanicField resolved() {
        VolcanicField f = resolved;
        if (f == null) {
            f = VolcanicField.resolve(field);
            if (f == null) {
                throw new IllegalStateException("volcanic_output needs a volcanic_field, got " + field);
            }
            resolved = f;
        }
        return f;
    }

    @Override
    public double compute(FunctionContext context) {
        VolcanicField f = resolved();
        int x = context.blockX();
        int z = context.blockZ();
        return switch (output) {
            case INITIAL_HEIGHT -> f.initialHeight(x, z);
            case CALDERA_MASK -> f.column(x, z).calderaMask;
            case CALDERA_DORMANT -> f.column(x, z).dormant;
            case LAVA_CHANNEL -> f.column(x, z).channel;
            case ISLAND_MASK -> f.column(x, z).islandMask;
            case ROUGHNESS -> f.column(x, z).roughness;
            case GUARD -> f.guard(x, context.blockY(), z);
            case ISLANDS -> f.islands(x, context.blockY(), z);
        };
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new VolcanicOutput(field.mapAll(visitor), output));
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
        return o instanceof VolcanicOutput other && field.equals(other.field) && output == other.output;
    }

    @Override
    public int hashCode() {
        return 31 * field.hashCode() + output.hashCode();
    }
}
