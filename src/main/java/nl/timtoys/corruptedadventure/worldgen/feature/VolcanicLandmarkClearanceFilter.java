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
 * Placement filter {@code corruptedadventure:volcanic_landmark_clearance} (G2 ring 3): keeps a position that is more
 * than {@code margin} blocks outside the reach of every ring 3 landmark, so the ember woods' trees, logs, stumps and
 * flora leave room round the ember giant and never grow into its crown or hollow. Ground features get the same check
 * through {@link VolcanicFilter.Builder#ground()}. Pure in the seed and the terrain, like the landmarks themselves.
 */
public final class VolcanicLandmarkClearanceFilter extends PlacementFilter {
    public static final MapCodec<VolcanicLandmarkClearanceFilter> CODEC = Codec.intRange(0, 16).fieldOf("margin")
            .xmap(VolcanicLandmarkClearanceFilter::new, filter -> filter.margin);

    private final int margin;

    public VolcanicLandmarkClearanceFilter(int margin) {
        this.margin = margin;
    }

    public static VolcanicLandmarkClearanceFilter of(int margin) {
        return new VolcanicLandmarkClearanceFilter(margin);
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        return !VolcanicLandmarks.near(context.getLevel(), VolcanicLandmarks.ALL, pos.getX(), pos.getZ(), margin);
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifierTypes.VOLCANIC_LANDMARK_CLEARANCE.get();
    }
}
