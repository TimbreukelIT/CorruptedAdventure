package nl.timtoys.corruptedadventure.worldgen.surface;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.worldgen.chunk.RingChunkGenerator;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicField;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicOutput;

/**
 * Surface rule condition {@code corruptedadventure:volcanic_threshold}: true where a per-column output of the ring 3
 * terrain ({@code volcanic_field}) lies in {@code [min, max)}. The ring 3 surface uses it for what the noises cannot
 * know: the inside of active craters ({@code caldera_mask}, {@code caldera_dormant}), the volcano flanks
 * ({@code volcano_rise}) and the banks of the lava channels ({@code lava_bank}) (change 39c). 39f: also the per-block
 * outputs {@code thin_kind} (the thin solids, painted whole), {@code tread} and {@code vein} (patterns in Y).
 * <p>
 * Reads the noise chunk's own instance of the field, which {@link RingChunkGenerator} hands to its surface pass, so
 * the surface shares the column cache of the terrain and sees exactly its values. Without a field (another generator
 * or dimension) the condition is false, with one warning per world in the log. The condition remembers its last column.
 */
public record VolcanicThresholdCondition(VolcanicOutput.Output output, double min, double max)
        implements SurfaceRules.ConditionSource {
    public static final MapCodec<VolcanicThresholdCondition> MAP_CODEC = RecordCodecBuilder.<VolcanicThresholdCondition>mapCodec(
            instance -> instance.group(
                    VolcanicOutput.Output.CODEC.fieldOf("output").forGetter(VolcanicThresholdCondition::output),
                    Codec.DOUBLE.fieldOf("min").forGetter(VolcanicThresholdCondition::min),
                    Codec.DOUBLE.fieldOf("max").forGetter(VolcanicThresholdCondition::max)
            ).apply(instance, VolcanicThresholdCondition::new)).validate(VolcanicThresholdCondition::check);

    public static final KeyDispatchDataCodec<VolcanicThresholdCondition> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    /** Random states (one per world and dimension) already warned about a missing field. */
    private static final Set<RandomState> WARNED = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private static DataResult<VolcanicThresholdCondition> check(VolcanicThresholdCondition condition) {
        if (!condition.output.forSurface()) {
            return DataResult.error(() -> "volcanic_threshold needs a per-column or surface output, got " + condition.output.getSerializedName());
        }
        return condition.min < condition.max ? DataResult.success(condition)
                : DataResult.error(() -> "volcanic_threshold needs min < max");
    }

    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
        return CODEC;
    }

    @Override
    public SurfaceRules.Condition apply(SurfaceRules.Context context) {
        VolcanicField field = RingChunkGenerator.surfaceVolcanicField();
        if (field == null) {
            if (WARNED.add(context.randomState)) {
                CorruptedAdventure.LOGGER.warn("volcanic_threshold used without ring 3 terrain in the noise chunk; the condition is false");
            }
            return () -> false;
        }
        if (!output.perColumn()) {
            // Per block: the field's column table remembers the column, so only the block's own test remains.
            return () -> {
                double value = VolcanicOutput.blockValue(field, output, context.blockX, context.blockY, context.blockZ);
                return value >= min && value < max;
            };
        }
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
                    double value = VolcanicOutput.columnValue(field.column(x, z), output);
                    result = value >= min && value < max;
                }
                return result;
            }
        };
    }
}
