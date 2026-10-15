package nl.timtoys.corruptedadventure.worldgen.surface;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.worldgen.chunk.RingChunkGenerator;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandOutput;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.RingGated;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Surface rule condition {@code corruptedadventure:ring4_threshold} (verdant_falls, Q7 = A): true where a 2D value of
 * the ring 4 terrain lies in {@code [min, max)}, like {@code volcanic_threshold} for ring 3. The ring 4 surface uses it
 * for what the noises cannot know: the travertine rims of the basins and the lips of the falls
 * ({@code ring_4/overworld/rim}, calcite) and the exact walls ({@code ring_4/overworld/cliff}, stone).
 * <p>
 * The value is a shared density function key rather than an output name, so the terrain decides what lies behind it:
 * either a constant (a stub until the terrain fills it) or one {@code island_output} of the ring 4 field, behind
 * references and caches. The condition reads that output from the noise chunk's own instance of the field, which
 * {@link RingChunkGenerator} hands to its surface pass, so the surface shares the column cache of the terrain and sees
 * exactly its values. Without a field (another generator or dimension) the condition is false, with one warning per
 * world in the log. 2D only: read once per column; the condition remembers its last column.
 */
public record Ring4ThresholdCondition(Holder<DensityFunction> function, double min, double max) implements SurfaceRules.ConditionSource {
    public static final MapCodec<Ring4ThresholdCondition> MAP_CODEC = RecordCodecBuilder.<Ring4ThresholdCondition>mapCodec(
            instance -> instance.group(
                    DensityFunction.CODEC.fieldOf("density_function").forGetter(Ring4ThresholdCondition::function),
                    Codec.DOUBLE.fieldOf("min").forGetter(Ring4ThresholdCondition::min),
                    Codec.DOUBLE.fieldOf("max").forGetter(Ring4ThresholdCondition::max)
            ).apply(instance, Ring4ThresholdCondition::new)).validate(Ring4ThresholdCondition::check);

    public static final KeyDispatchDataCodec<Ring4ThresholdCondition> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    /** Random states (one per world and dimension) already warned about a missing field. */
    private static final Set<RandomState> WARNED = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private static DataResult<Ring4ThresholdCondition> check(Ring4ThresholdCondition condition) {
        return condition.min < condition.max ? DataResult.success(condition) : DataResult.error(() -> "ring4_threshold needs min < max");
    }

    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
        return CODEC;
    }

    @Override
    public SurfaceRules.Condition apply(SurfaceRules.Context context) {
        IslandField field = RingChunkGenerator.surfaceIslandField();
        if (field == null) {
            if (WARNED.add(context.randomState)) {
                CorruptedAdventure.LOGGER.warn("ring4_threshold used without ring 4 terrain in the noise chunk; the condition is false");
            }
            return () -> false;
        }
        DensityFunction reader = reader(function.value(), field);
        return new SurfaceRules.Condition() {
            private boolean known;
            private int lastX;
            private int lastZ;
            private boolean result;

            @Override
            public boolean test() {
                int x = context.blockX;
                int z = context.blockZ;
                if (!known || x != lastX || z != lastZ) {
                    known = true;
                    lastX = x;
                    lastZ = z;
                    double value = reader.compute(new DensityFunction.SinglePointContext(x, 0, z));
                    result = value >= min && value < max;
                }
                return result;
            }
        };
    }

    /**
     * What {@code function} reads, on {@code field}: its constant, or its {@code island_output} on {@code field} (looking
     * through references, caches and ring gates). Throws for anything else, and for the 3D outputs.
     */
    static DensityFunction reader(DensityFunction function, IslandField field) {
        DensityFunction current = function;
        for (int depth = 0; depth < 16 && current != null; depth++) {
            if (current instanceof IslandOutput output) {
                if (output.output() == IslandOutput.Output.THIN || output.output() == IslandOutput.Output.CAVE_LIFT) {
                    throw new IllegalArgumentException("ring4_threshold reads 2D outputs only, got " + output.output().getSerializedName());
                }
                return new IslandOutput(field, output.output());
            }
            if (current.minValue() == current.maxValue()) {
                return DensityFunctions.constant(current.minValue());
            }
            current = RingGated.unwrap(current);
        }
        throw new IllegalArgumentException("ring4_threshold reads a constant or one island_output, got " + function);
    }
}
