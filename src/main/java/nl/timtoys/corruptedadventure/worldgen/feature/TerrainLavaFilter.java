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
 * Placement filter {@code corruptedadventure:terrain_lava} (change 41): keeps a position with the ring 3 terrain's own
 * lava within {@code radius} blocks on every axis: the lava sea of the abysses, crater lakes, rivers and springs, as
 * the terrain decides them per column ({@link VolcanicField.Column#lavaAt}). A pure decision of the seed and x, y, z,
 * read through {@link Ring3Terrain}, never from world blocks: lava that neighbouring chunks place while they decorate
 * (lakes, springs, falls) would make the answer depend on the load order. Columns are sampled every {@code step}
 * blocks. Without ring 3 terrain nothing passes.
 */
public final class TerrainLavaFilter extends PlacementFilter {
    public static final MapCodec<TerrainLavaFilter> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(1, 8).fieldOf("radius").forGetter(filter -> filter.radius),
            Codec.intRange(1, 4).optionalFieldOf("step", 1).forGetter(filter -> filter.step)
    ).apply(instance, TerrainLavaFilter::new));

    private final int radius;
    private final int step;

    public TerrainLavaFilter(int radius, int step) {
        this.radius = radius;
        this.step = step;
    }

    public static TerrainLavaFilter within(int radius, int step) {
        return new TerrainLavaFilter(radius, step);
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        VolcanicField field = Ring3Terrain.field(context.getLevel());
        return field != null && near((x, z) -> {
            VolcanicField.Column column = field.column(x, z);
            return new Lava(column.lavaTop(), column.lavaBottom(), column.springY());
        }, pos.getX(), pos.getY(), pos.getZ(), radius, step);
    }

    /** Whether a column within {@code radius} (sampled every {@code step}) has lava within {@code radius} of {@code y}. */
    static boolean near(Columns columns, int x, int y, int z, int radius, int step) {
        for (int dz = -radius; dz <= radius; dz += step) {
            for (int dx = -radius; dx <= radius; dx += step) {
                Lava lava = columns.at(x + dx, z + dz);
                int top = lava.top();
                int bottom = lava.bottom();
                int spring = lava.spring();
                // The column's lava interval [bottom, top) or its spring, against [y − radius, y + radius].
                if (top != Integer.MIN_VALUE && bottom <= y + radius && top - 1 >= y - radius
                        || spring != Integer.MIN_VALUE && Math.abs(spring - y) <= radius) {
                    return true;
                }
            }
        }
        return false;
    }

    /** A column's lava: {@code [bottom, top)} and a spring, each {@link Integer#MIN_VALUE} if absent. */
    record Lava(int top, int bottom, int spring) {
        static final Lava NONE = new Lava(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    @FunctionalInterface
    interface Columns {
        Lava at(int x, int z);
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifierTypes.TERRAIN_LAVA.get();
    }
}
