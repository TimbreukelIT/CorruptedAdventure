package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import nl.timtoys.corruptedadventure.registry.ModPlacementModifierTypes;

/**
 * Placement filter {@code corruptedadventure:landmark_clearance} (ring 4 decor B, 2026-10-19): keeps a position that is
 * more than {@code margin} blocks outside every blighted landmark's footprint ({@link BlightedLandmarks}), so trees,
 * stumps and puddles leave the drowned giant, the stone circle and the carcass free. Pure in the seed and the terrain,
 * like the landmarks themselves.
 */
public final class LandmarkClearanceFilter extends PlacementFilter {
    public static final MapCodec<LandmarkClearanceFilter> CODEC = Codec.intRange(0, 16).fieldOf("margin")
            .xmap(LandmarkClearanceFilter::new, filter -> filter.margin);

    private final int margin;

    public LandmarkClearanceFilter(int margin) {
        this.margin = margin;
    }

    public static LandmarkClearanceFilter of(int margin) {
        return new LandmarkClearanceFilter(margin);
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        return !BlightedLandmarks.near(context.getLevel(), pos.getX(), pos.getZ(), margin);
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifierTypes.LANDMARK_CLEARANCE.get();
    }
}
