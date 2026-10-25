package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import nl.timtoys.corruptedadventure.registry.ModPlacementModifierTypes;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicField;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicOutput;

import java.util.ArrayList;
import java.util.List;

/**
 * Placement filter {@code corruptedadventure:volcanic_filter} (change 47): keeps a position whose column of the pure
 * ring 3 terrain ({@link Ring3Terrain}) has every listed per-column output in [{@code min}, {@code max}), the same
 * outputs as the surface condition {@code volcanic_threshold}. With {@code ground_only} the found surface
 * ({@code WORLD_SURFACE_WG}) must also lie within {@code tolerance} blocks of the column's terrain height: nothing on
 * the crests of needles, arches, ribs and pillars, on islands or on bridge decks, whose tops stand far above it.
 * <p>
 * Without ring 3 terrain (another dimension or generator) nothing passes. Reads only the pure column and the frozen
 * worldgen heightmap, so the result does not depend on generation order.
 */
public final class VolcanicFilter extends PlacementFilter {
    static final int DEFAULT_TOLERANCE = 3;

    /** One output in [{@code min}, {@code max}). */
    public record Range(VolcanicOutput.Output output, double min, double max) {
        public static final Codec<Range> CODEC = RecordCodecBuilder.<Range>create(instance -> instance.group(
                VolcanicOutput.Output.CODEC.fieldOf("output").forGetter(Range::output),
                Codec.DOUBLE.optionalFieldOf("min", -Double.MAX_VALUE).forGetter(Range::min),
                Codec.DOUBLE.optionalFieldOf("max", Double.MAX_VALUE).forGetter(Range::max)
        ).apply(instance, Range::new)).validate(Range::validate);

        private static DataResult<Range> validate(Range range) {
            if (!range.output.perColumn()) {
                return DataResult.error(() -> "volcanic_filter needs per-column outputs, got " + range.output.getSerializedName());
            }
            return range.min < range.max ? DataResult.success(range) : DataResult.error(() -> "volcanic_filter needs min < max");
        }

        boolean test(VolcanicField.Column column) {
            double value = VolcanicOutput.columnValue(column, output);
            return value >= min && value < max;
        }
    }

    public static final MapCodec<VolcanicFilter> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Range.CODEC.listOf().optionalFieldOf("ranges", List.of()).forGetter(filter -> filter.ranges),
            Codec.BOOL.optionalFieldOf("ground_only", false).forGetter(filter -> filter.groundOnly),
            Codec.intRange(0, 16).optionalFieldOf("tolerance", DEFAULT_TOLERANCE).forGetter(filter -> filter.tolerance),
            Codec.intRange(-1, 32).optionalFieldOf("landmark_margin", -1).forGetter(filter -> filter.landmarkMargin)
    ).apply(instance, VolcanicFilter::new));

    /**
     * The clearance of {@link Builder#ground()} round the biome landmarks (G2 ring 3, review A): at least the reach of
     * the small dressing (stones, fans, vents, hornitos, reefs, casts), so nothing stands half in a landmark that is
     * placed after it.
     */
    public static final int LANDMARK_MARGIN = 6;

    private final List<Range> ranges;
    private final boolean groundOnly;
    private final int tolerance;
    /** Keep this far outside the reach of every ring 3 landmark ({@link VolcanicLandmarks}); −1 = no check. */
    private final int landmarkMargin;

    public VolcanicFilter(List<Range> ranges, boolean groundOnly, int tolerance, int landmarkMargin) {
        this.ranges = List.copyOf(ranges);
        this.groundOnly = groundOnly;
        this.tolerance = tolerance;
        this.landmarkMargin = landmarkMargin;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        VolcanicField field = Ring3Terrain.field(context.getLevel());
        if (field == null) {
            return false;
        }
        VolcanicField.Column column = field.column(pos.getX(), pos.getZ());
        return test(column, groundOnly ? context.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX(), pos.getZ()) : 0)
                && (landmarkMargin < 0 || !VolcanicLandmarks.near(context.getLevel(), VolcanicLandmarks.ALL, pos.getX(), pos.getZ(), landmarkMargin));
    }

    /** The ranges and, with {@code ground_only}, the surface (first open block above the ground) against the column. */
    boolean test(VolcanicField.Column column, int surface) {
        for (Range range : ranges) {
            if (!range.test(column)) {
                return false;
            }
        }
        return !groundOnly || onGround(surface, column.height(), tolerance);
    }

    /** Whether {@code surface} (first open block) lies within {@code tolerance} of the terrain height. */
    static boolean onGround(int surface, double height, int tolerance) {
        return Math.abs(surface - Mth.ceil(height)) <= tolerance;
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifierTypes.VOLCANIC_FILTER.get();
    }

    /** Collects ranges; {@link #ground()} adds the usual "plain ground" rules of plan §0. */
    public static final class Builder {
        private final List<Range> ranges = new ArrayList<>();
        private boolean groundOnly;
        private int landmarkMargin = -1;
        private int tolerance = DEFAULT_TOLERANCE;

        public Builder range(VolcanicOutput.Output output, double min, double max) {
            ranges.add(new Range(output, min, max));
            return this;
        }

        public Builder below(VolcanicOutput.Output output, double max) {
            return range(output, -Double.MAX_VALUE, max);
        }

        public Builder above(VolcanicOutput.Output output, double min) {
            return range(output, min, Double.MAX_VALUE);
        }

        public Builder groundOnly(int tolerance) {
            this.groundOnly = true;
            this.tolerance = tolerance;
            return this;
        }

        /**
         * Plain ground (plan §0.2–0.3): no lava bank, no cliff, no volcano flank, not in a crater or a collapse (39f);
         * the found surface on the terrain itself, so not on a thin solid either.
         */
        public Builder ground() {
            return below(VolcanicOutput.Output.LAVA_BANK, 0.1)
                    .below(VolcanicOutput.Output.CLIFF, 0.3)
                    .below(VolcanicOutput.Output.VOLCANO_RISE, 20.0)
                    .below(VolcanicOutput.Output.CALDERA_MASK, 0.001)
                    .below(VolcanicOutput.Output.COLLAPSE, 0.001)
                    .groundOnly(DEFAULT_TOLERANCE)
                    .clearOfLandmarks(LANDMARK_MARGIN);
        }

        /** Keeps {@code margin} blocks outside the reach of every ring 3 landmark (−1 = no check). */
        public Builder clearOfLandmarks(int margin) {
            this.landmarkMargin = margin;
            return this;
        }

        public VolcanicFilter build() {
            return new VolcanicFilter(ranges, groundOnly, tolerance, landmarkMargin);
        }
    }
}
