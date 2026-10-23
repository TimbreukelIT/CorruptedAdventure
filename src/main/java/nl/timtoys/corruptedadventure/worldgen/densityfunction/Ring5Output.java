package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * {@code corruptedadventure:ring_5_output}: one output of a {@code ring_5_field} ({@code field}, usually a reference to
 * {@code ring_5/overworld/field}), like {@code island_output} for ring 4. All outputs of one field share its column
 * cache: the noise chunk's memo maps equal fields to one instance.
 */
public final class Ring5Output implements DensityFunction {
    public static final MapCodec<Ring5Output> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("field").forGetter(Ring5Output::field),
            Output.CODEC.fieldOf("output").forGetter(Ring5Output::output)
    ).apply(instance, Ring5Output::new));

    public static final KeyDispatchDataCodec<Ring5Output> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    public enum Output implements StringRepresentable {
        /** 0..1: how far the ground has fallen away (0 solid ground, 1 void without any chunk); router erosion (V12-A). */
        BREAKUP("breakup", 0.0, 1.0),
        /** 0..1: the castle surroundings (1 on a pinnacle's plateau, falling to 0 across its belt). */
        CASTLES("castles", 0.0, 1.0),
        /** The top of the solid ground (without scars, void and caves), {@link Ring5Field#VOID_HEIGHT} over the void. */
        HEIGHT("height", Ring5Field.VOID_HEIGHT, 320.0),
        /** 0..1: a scar open to the void (1) and its rim ({@code void_scars}; the inner band's cracks in 83). */
        SCARS("scars", 0.0, 1.0),
        /** 0..1: the glitch fields' blocky shapes (their weight; the shifted layers and floating cubes lie where it is 1; 83). */
        GLITCH("glitch", 0.0, 1.0),
        /** 0..1: how far an upside-down hill hangs under the floe ({@code inverted_hills}; 1 = 40 blocks). */
        INVERTED("inverted", 0.0, 1.0),
        /** The preliminary surface (blocks): the ground's top; over the void below the world's bottom (no surface). */
        INITIAL_HEIGHT("initial_height", Ring5Field.VOID_HEIGHT, 320.0),
        /** 3D, the terrain's density, exact per block, −1..1. */
        TERRAIN("terrain", -1.0, 1.0),
        /** 3D, ring 5's own caves (93): the code of {@link Ring5Caves.Kind} (0 none, corridor, chamber, hole, hole rim). */
        CAVES("caves", 0.0, 4.0),
        /** 0..2: how far vanilla's damped caves are closed in the column (93, V93-1; 0 open deep in the inner band). */
        CAVE_CLOSURE("cave_closure", 0.0, Ring5Field.CAVES_CLOSED);

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
    private volatile Ring5Field resolved;

    public Ring5Output(DensityFunction field, Output output) {
        this.field = field;
        this.output = output;
        this.resolved = Ring5Field.resolveIfBound(field);
    }

    public DensityFunction field() {
        return field;
    }

    public Output output() {
        return output;
    }

    private Ring5Field resolved() {
        Ring5Field f = resolved;
        if (f == null) {
            f = Ring5Field.resolve(field);
            if (f == null) {
                throw new IllegalStateException("ring_5_output needs a ring_5_field, got " + field);
            }
            resolved = f;
        }
        return f;
    }

    @Override
    public double compute(FunctionContext context) {
        Ring5Field f = resolved();
        int x = context.blockX();
        int z = context.blockZ();
        return switch (output) {
            case BREAKUP -> f.breakupAt(x, z);
            case CASTLES -> f.column(x, z).castle();
            case HEIGHT -> f.column(x, z).height();
            case SCARS -> f.column(x, z).scars();
            case INVERTED -> f.column(x, z).inverted();
            case GLITCH -> f.column(x, z).glitch();
            case INITIAL_HEIGHT -> f.initialHeight(x, z);
            case TERRAIN -> f.terrain(x, context.blockY(), z);
            case CAVES -> f.caveCode(x, context.blockY(), z);
            case CAVE_CLOSURE -> f.column(x, z).closure();
        };
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new Ring5Output(field.mapAll(visitor), output));
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
        return o instanceof Ring5Output other && field.equals(other.field) && output == other.output;
    }

    @Override
    public int hashCode() {
        return 31 * field.hashCode() + output.hashCode();
    }
}
