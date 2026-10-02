package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import nl.timtoys.corruptedadventure.registry.ModPlacementModifierTypes;

/**
 * Placement filter {@code corruptedadventure:height_band}: keeps a position whose Y lies in [{@code min_y},
 * {@code max_y}]. After a heightmap placement this separates the levels of the canyonlands, which share their ground
 * blocks: canyon floors and steps (below ~Y 100), talus slopes and the plateau with its mesas (above ~Y 118).
 * Vanilla only has filters relative to a heightmap.
 */
public final class HeightBandFilter extends PlacementFilter {
    public static final MapCodec<HeightBandFilter> CODEC = RecordCodecBuilder.<HeightBandFilter>mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("min_y", Integer.MIN_VALUE).forGetter(filter -> filter.minY),
            Codec.INT.optionalFieldOf("max_y", Integer.MAX_VALUE).forGetter(filter -> filter.maxY)
    ).apply(instance, HeightBandFilter::new)).validate(HeightBandFilter::validate);

    private final int minY;
    private final int maxY;

    public HeightBandFilter(int minY, int maxY) {
        this.minY = minY;
        this.maxY = maxY;
    }

    public static HeightBandFilter between(int minY, int maxY) {
        return new HeightBandFilter(minY, maxY);
    }

    public static HeightBandFilter below(int maxY) {
        return new HeightBandFilter(Integer.MIN_VALUE, maxY);
    }

    public static HeightBandFilter above(int minY) {
        return new HeightBandFilter(minY, Integer.MAX_VALUE);
    }

    private static DataResult<HeightBandFilter> validate(HeightBandFilter filter) {
        return filter.minY <= filter.maxY ? DataResult.success(filter)
                : DataResult.error(() -> "height_band: min_y " + filter.minY + " is above max_y " + filter.maxY);
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        return minY <= pos.getY() && pos.getY() <= maxY;
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifierTypes.HEIGHT_BAND.get();
    }
}
