package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.EnumSet;
import java.util.Set;

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
        /** 0..1: lava river bed, 1 in the middle. */
        LAVA_CHANNEL("lava_channel", 0.0, 1.0),
        /** 0..1: a floating island above the column. */
        ISLAND_MASK("island_mask", 0.0, 1.0),
        /** 0..1: crags: steep volcano flanks and crater walls, cliffs, and a little on the plain (39d). */
        ROUGHNESS("roughness", 0.0, 1.0),
        /** Height of the volcanoes above the plain, in blocks (the surface's volcano flanks). */
        VOLCANO_RISE("volcano_rise", 0.0, 320.0),
        /** 0..1: a lava river, 1 over the lava, falling to 0 a few blocks beside it (the surface's scorched banks). */
        LAVA_BANK("lava_bank", 0.0, 1.0),
        /** 0..1: a cliff or steep wall of a landform (plateau risers, rift flanks, gully walls; 39d), rock in the surface. */
        CLIFF("cliff", 0.0, 1.0),
        /** 0..1 (39f): a collapse's footprint, 1 inside its rim (vanilla cave entrances may reach the surface there). */
        COLLAPSE("collapse", 0.0, 1.0),
        /**
         * 3D: +1 where caves stay out (around lava lakes and channels; 42: around magma chambers, lava gutters and fire
         * geodes), −1 elsewhere.
         */
        GUARD("guard", -1.0, 1.0),
        /**
         * 3D (42): added to the density of vanilla's caves, 0 where they run freely, up to 2 where they are gone (a
         * volcano's body, above Y ~146); rising over 20 blocks, so the caves shrink and close gradually.
         */
        CAVE_LIFT("cave_lift", 0.0, 2.0),
        /**
         * 3D (39d): the thin shapes, not interpolated: +1 inside a thin solid (hoodoos, ribs, prisms, needles, arches;
         * 39f: also the floating islands, pillars, rubble and the micro relief), −1 inside a carved opening (lava tube
         * tunnels; 39f: a collapse's overhang and shaft), 0 elsewhere.
         */
        THIN("thin", -1.0, 1.0),
        /**
         * 3D (39f, for the surface): the kind of thin solid at a block, −1 outside one (0 biome surface, 2 obsidian,
         * 3 basalt, 4 floating island, 5 rubble; {@link Landmarks}).
         */
        THIN_KIND("thin_kind", -1.0, 5.0),
        /** 3D (39f, for the surface): 0..1, changes with every block of height, slowly across the plain (patches that follow the steps). */
        TREAD("tread", 0.0, 1.0),
        /** 3D (39f, for the surface): 0..1, thin winding layers (the veins of the obsidian needles). */
        VEIN("vein", 0.0, 1.0),
        /** 1 where the column holds walls of a lava tube, skylight or magma chamber (42), else 0. */
        CAVE_WALLS("cave_walls", 0.0, 1.0),
        /**
         * 3D (42, for the surface): which wall of a lava tube, skylight or magma chamber a block belongs to: 0 none, 1 the
         * smooth inner skin, 2 the shell, 3 skin bordering lava (a crust).
         */
        CAVE_WALL("cave_wall", 0.0, 3.0),
        /** 3D (42): −1 inside the cavity of a fire crystal geode, +10 elsewhere (neutral under {@code min}). */
        FIRE_GEODES("fire_geodes", -1.0, 10.0);

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

        /** Whether this output is one value per column, stored in {@link VolcanicField.Column}. */
        public boolean perColumn() {
            return PER_COLUMN.contains(this);
        }

        /** Whether the surface may read this output per block (39f): the per-column ones and the surface patterns. */
        public boolean forSurface() {
            return perColumn() || this == THIN_KIND || this == TREAD || this == VEIN || this == CAVE_WALL;
        }
    }

    private static final Set<Output> PER_COLUMN = EnumSet.of(Output.CALDERA_MASK, Output.CALDERA_DORMANT,
            Output.LAVA_CHANNEL, Output.ISLAND_MASK, Output.ROUGHNESS, Output.VOLCANO_RISE, Output.LAVA_BANK, Output.CLIFF,
            Output.COLLAPSE, Output.CAVE_WALLS);

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
            case GUARD -> f.guard(x, context.blockY(), z);
            case CAVE_LIFT -> f.caveLift(x, context.blockY(), z);
            case THIN -> f.thin(x, context.blockY(), z);
            case THIN_KIND, TREAD, VEIN, CAVE_WALL -> blockValue(f, output, x, context.blockY(), z);
            case FIRE_GEODES -> f.fireGeodes(x, context.blockY(), z);
            default -> columnValue(f.column(x, z), output);
        };
    }

    /** {@code output} of one column; only for the per-column outputs ({@link Output#perColumn()}). */
    public static double columnValue(VolcanicField.Column column, Output output) {
        return switch (output) {
            case CALDERA_MASK -> column.calderaMask;
            case CALDERA_DORMANT -> column.dormant;
            case LAVA_CHANNEL -> column.channel;
            case ISLAND_MASK -> column.islandMask;
            case ROUGHNESS -> column.roughness;
            case VOLCANO_RISE -> column.rise;
            case LAVA_BANK -> column.lavaBank;
            case CLIFF -> column.cliff;
            case COLLAPSE -> column.collapse;
            case CAVE_WALLS -> column.caveCount > 0 ? 1.0 : 0.0;
            case INITIAL_HEIGHT, GUARD, CAVE_LIFT, THIN, THIN_KIND, TREAD, VEIN, CAVE_WALL, FIRE_GEODES ->
                    throw new IllegalArgumentException(output + " is not a column value");
        };
    }

    /**
     * One of the per-block surface outputs ({@link Output#THIN_KIND}, {@link Output#TREAD}, {@link Output#VEIN},
     * {@link Output#CAVE_WALL}) at a block.
     */
    public static double blockValue(VolcanicField field, Output output, int x, int y, int z) {
        return switch (output) {
            case THIN_KIND -> field.thinKind(x, y, z);
            case TREAD -> field.tread(x, y, z);
            case VEIN -> field.vein(x, y, z);
            case CAVE_WALL -> field.caveWall(x, y, z);
            default -> throw new IllegalArgumentException(output + " is not a per-block surface value");
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
