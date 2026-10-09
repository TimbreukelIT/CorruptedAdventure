package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import nl.timtoys.corruptedadventure.registry.ModPlacementModifierTypes;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicField;

/**
 * Placement filter {@code corruptedadventure:volcanic_near} (change 47b): keeps a position where some column of the
 * pure ring 3 terrain within {@code radius} (a disc, sampled every {@code step} blocks) has its output in the range,
 * e.g. "near a titan bone" ({@code solid_kind} 1). Pure: reads only the terrain columns, so neighbouring chunks may be
 * read at any time.
 */
public final class VolcanicNearFilter extends PlacementFilter {
    public static final MapCodec<VolcanicNearFilter> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            VolcanicFilter.Range.CODEC.fieldOf("range").forGetter(filter -> filter.range),
            Codec.intRange(1, 16).fieldOf("radius").forGetter(filter -> filter.radius),
            Codec.intRange(1, 4).optionalFieldOf("step", 2).forGetter(filter -> filter.step)
    ).apply(instance, VolcanicNearFilter::new));

    private final VolcanicFilter.Range range;
    private final int radius;
    private final int step;

    public VolcanicNearFilter(VolcanicFilter.Range range, int radius, int step) {
        this.range = range;
        this.radius = radius;
        this.step = step;
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        VolcanicField field = Ring3Terrain.field(context.getLevel());
        if (field == null) {
            return false;
        }
        for (int dz = -radius; dz <= radius; dz += step) {
            for (int dx = -radius; dx <= radius; dx += step) {
                if (dx * dx + dz * dz <= radius * radius && range.test(field.column(pos.getX() + dx, pos.getZ() + dz))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifierTypes.VOLCANIC_NEAR.get();
    }
}
