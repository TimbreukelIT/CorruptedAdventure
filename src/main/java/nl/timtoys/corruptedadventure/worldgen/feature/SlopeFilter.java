package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import nl.timtoys.corruptedadventure.registry.ModPlacementModifierTypes;

import java.util.Locale;

/**
 * Placement filter {@code corruptedadventure:slope}: compares the {@code WORLD_SURFACE_WG} height of the columns
 * within {@code radius} (a disc) with the position's own column, and keeps the position if that measure lies in
 * [{@code min}, {@code max}]. {@code rise} = how much the highest column stands above this one (a high minimum finds
 * the foot of a wall, not its top edge); {@code relief} = the largest difference either way (a low maximum finds
 * flat ground, away from walls and edges); {@code drop} = how far the lowest column lies below this one (a high
 * minimum finds the top edge of a wall: ring 4's gorge rims, golf 2).
 * <p>
 * During the features step this heightmap shows the terrain alone, also in the neighbouring chunks, so the filter
 * does not depend on generation order. A radius up to 8 stays inside the 3 × 3 chunks a feature may read.
 */
public final class SlopeFilter extends PlacementFilter {
    public static final MapCodec<SlopeFilter> CODEC = RecordCodecBuilder.<SlopeFilter>mapCodec(instance -> instance.group(
            Codec.intRange(1, 8).fieldOf("radius").forGetter(filter -> filter.radius),
            Measure.CODEC.optionalFieldOf("measure", Measure.RELIEF).forGetter(filter -> filter.measure),
            Codec.INT.optionalFieldOf("min", Integer.MIN_VALUE).forGetter(filter -> filter.min),
            Codec.INT.optionalFieldOf("max", Integer.MAX_VALUE).forGetter(filter -> filter.max)
    ).apply(instance, SlopeFilter::new)).validate(SlopeFilter::validate);

    private final int radius;
    private final Measure measure;
    private final int min;
    private final int max;

    public SlopeFilter(int radius, Measure measure, int min, int max) {
        this.radius = radius;
        this.measure = measure;
        this.min = min;
        this.max = max;
    }

    /** The foot of a wall: some column within {@code radius} stands at least {@code minRise} higher. */
    public static SlopeFilter wallFoot(int radius, int minRise) {
        return new SlopeFilter(radius, Measure.RISE, minRise, Integer.MAX_VALUE);
    }

    /** The top edge of a wall: some column within {@code radius} lies at least {@code minDrop} lower. */
    public static SlopeFilter rimTop(int radius, int minDrop) {
        return new SlopeFilter(radius, Measure.DROP, minDrop, Integer.MAX_VALUE);
    }

    /** Flat ground: no column within {@code radius} differs by more than {@code maxRelief}. */
    public static SlopeFilter flat(int radius, int maxRelief) {
        return new SlopeFilter(radius, Measure.RELIEF, Integer.MIN_VALUE, maxRelief);
    }

    private static DataResult<SlopeFilter> validate(SlopeFilter filter) {
        return filter.min <= filter.max ? DataResult.success(filter)
                : DataResult.error(() -> "slope: min " + filter.min + " is above max " + filter.max);
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        int value = measure(measure, radius, pos.getX(), pos.getZ(), (x, z) -> context.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z));
        return min <= value && value <= max;
    }

    /** The measure at x, z over the disc of {@code radius}, on any height function. */
    static int measure(Measure measure, int radius, int x, int z, Heights heights) {
        int own = heights.at(x, z);
        int value = 0;
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if ((dx != 0 || dz != 0) && dx * dx + dz * dz <= radius * radius) {
                    int difference = heights.at(x + dx, z + dz) - own;
                    value = Math.max(value, switch (measure) {
                        case RISE -> difference;
                        case DROP -> -difference;
                        case RELIEF -> Math.abs(difference);
                    });
                }
            }
        }
        return value;
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifierTypes.SLOPE.get();
    }

    @FunctionalInterface
    interface Heights {
        int at(int x, int z);
    }

    public enum Measure implements StringRepresentable {
        RISE,
        RELIEF,
        DROP;

        public static final Codec<Measure> CODEC = StringRepresentable.fromEnum(Measure::values);

        private final String serializedName = name().toLowerCase(Locale.ROOT);

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
