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
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Placement filter {@code corruptedadventure:ring4_filter} (golf 2, 75): keeps a position whose column of the pure
 * ring 4 terrain ({@link Ring4Terrain}) has every listed per-column output in [{@code min}, {@code max}), the outputs
 * of the surface condition {@code ring4_threshold} ({@code slope} included: {@link IslandField#slope}). With
 * {@code ground_only} the found surface ({@code WORLD_SURFACE_WG}) must also lie within {@code tolerance} of the
 * column's terrain surface: nothing on islands, rock bridges, arches or bridge decks, whose tops stand far above it.
 * {@code fall_clearance} keeps a position that far from every recorded waterfall ({@link IslandField#waterfallsWithin})
 * and {@code knob_clearance} that far from every climbing knob of a gorge ({@link IslandField#climbKnobs}): the falls,
 * their lips and the climbing routes of {@code verdant_falls} stay free (F4 = A).
 * <p>
 * Without ring 4 terrain (another dimension or generator) nothing passes. Reads only the pure column and the frozen
 * worldgen heightmap, so the result does not depend on generation order (like {@link VolcanicFilter} in ring 3).
 */
public final class Ring4Filter extends PlacementFilter {
    static final int DEFAULT_TOLERANCE = 3;
    /** The outputs a filter may read: one 2D value per column. */
    static final Set<IslandOutput.Output> COLUMN_OUTPUTS = Set.of(IslandOutput.Output.HEIGHT, IslandOutput.Output.SURFACE,
            IslandOutput.Output.ISLAND_MASK, IslandOutput.Output.ISLAND_HOLE, IslandOutput.Output.RIVER, IslandOutput.Output.FJORD,
            IslandOutput.Output.CLIFF, IslandOutput.Output.RIM, IslandOutput.Output.SLOPE, IslandOutput.Output.SUNDERED);

    /** One output in [{@code min}, {@code max}). */
    public record Range(IslandOutput.Output output, double min, double max) {
        public static final Codec<Range> CODEC = RecordCodecBuilder.<Range>create(instance -> instance.group(
                IslandOutput.Output.CODEC.fieldOf("output").forGetter(Range::output),
                Codec.DOUBLE.optionalFieldOf("min", -Double.MAX_VALUE).forGetter(Range::min),
                Codec.DOUBLE.optionalFieldOf("max", Double.MAX_VALUE).forGetter(Range::max)
        ).apply(instance, Range::new)).validate(Range::validate);

        private static DataResult<Range> validate(Range range) {
            if (!COLUMN_OUTPUTS.contains(range.output)) {
                return DataResult.error(() -> "ring4_filter needs per-column outputs, got " + range.output.getSerializedName());
            }
            return range.min < range.max ? DataResult.success(range) : DataResult.error(() -> "ring4_filter needs min < max");
        }

        boolean test(double value) {
            return value >= min && value < max;
        }

        double read(IslandField field, int x, int z) {
            return output == IslandOutput.Output.SLOPE ? field.slope(x, z) : IslandOutput.columnValue(field.column(x, z), output);
        }
    }

    public static final MapCodec<Ring4Filter> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Range.CODEC.listOf().optionalFieldOf("ranges", List.of()).forGetter(filter -> filter.ranges),
            Codec.BOOL.optionalFieldOf("ground_only", false).forGetter(filter -> filter.groundOnly),
            Codec.intRange(0, 16).optionalFieldOf("tolerance", DEFAULT_TOLERANCE).forGetter(filter -> filter.tolerance),
            Codec.intRange(0, 16).optionalFieldOf("fall_clearance", 0).forGetter(filter -> filter.fallClearance),
            Codec.intRange(0, 16).optionalFieldOf("knob_clearance", 0).forGetter(filter -> filter.knobClearance)
    ).apply(instance, Ring4Filter::new));

    private final List<Range> ranges;
    private final boolean groundOnly;
    private final int tolerance;
    private final int fallClearance;
    private final int knobClearance;

    public Ring4Filter(List<Range> ranges, boolean groundOnly, int tolerance, int fallClearance, int knobClearance) {
        this.ranges = List.copyOf(ranges);
        this.groundOnly = groundOnly;
        this.tolerance = tolerance;
        this.fallClearance = fallClearance;
        this.knobClearance = knobClearance;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        IslandField field = Ring4Terrain.field(context.getLevel());
        if (field == null) {
            return false;
        }
        int x = pos.getX();
        int z = pos.getZ();
        for (Range range : ranges) {
            if (!range.test(range.read(field, x, z))) {
                return false;
            }
        }
        if (groundOnly && !onGround(context.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z), field.column(x, z).surface(), tolerance)) {
            return false;
        }
        if (fallClearance > 0 && !field.waterfallsWithin(x - fallClearance, z - fallClearance, x + fallClearance, z + fallClearance).isEmpty()) {
            return false;
        }
        return knobClearance <= 0 || field.climbKnobs(x - knobClearance, z - knobClearance, x + knobClearance, z + knobClearance).isEmpty();
    }

    /** Whether {@code surface} (first open block) lies within {@code tolerance} of the terrain's surface. */
    static boolean onGround(int surface, double terrain, int tolerance) {
        return Math.abs(surface - Mth.ceil(terrain)) <= tolerance;
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifierTypes.RING4_FILTER.get();
    }

    /** Collects ranges. */
    public static final class Builder {
        private final List<Range> ranges = new ArrayList<>();
        private boolean groundOnly;
        private int fallClearance;
        private int knobClearance;

        public Builder range(IslandOutput.Output output, double min, double max) {
            ranges.add(new Range(output, min, max));
            return this;
        }

        public Builder below(IslandOutput.Output output, double max) {
            return range(output, -Double.MAX_VALUE, max);
        }

        public Builder above(IslandOutput.Output output, double min) {
            return range(output, min, Double.MAX_VALUE);
        }

        /** On the terrain's own surface (not on an island, arch or bridge deck), within {@link #DEFAULT_TOLERANCE}. */
        public Builder ground() {
            this.groundOnly = true;
            return this;
        }

        /** At least {@code blocks} from every recorded waterfall (the column of its water). */
        public Builder clearOfFalls(int blocks) {
            this.fallClearance = blocks;
            return this;
        }

        /** At least {@code blocks} from every climbing knob of a gorge. */
        public Builder clearOfKnobs(int blocks) {
            this.knobClearance = blocks;
            return this;
        }

        public Ring4Filter build() {
            return new Ring4Filter(ranges, groundOnly, DEFAULT_TOLERANCE, fallClearance, knobClearance);
        }
    }
}
