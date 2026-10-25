package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import nl.timtoys.corruptedadventure.registry.ModPlacementModifierTypes;

import java.util.EnumSet;
import java.util.Set;

/**
 * Placement filter {@code corruptedadventure:volcanic_landmark_clearance} (G2 ring 3): keeps a position that is more
 * than {@code margin} blocks outside the reach of every ember giant ({@link VolcanicLandmarks.Kind#EMBER_GIANT}), so the
 * ember woods' trees and fallen logs leave room round it and never grow into its crown. Pure in the seed and the
 * terrain, like the landmarks themselves.
 */
public final class VolcanicLandmarkClearanceFilter extends PlacementFilter {
    public static final MapCodec<VolcanicLandmarkClearanceFilter> CODEC = Codec.intRange(0, 16).fieldOf("margin")
            .xmap(VolcanicLandmarkClearanceFilter::new, filter -> filter.margin);

    private static final Set<VolcanicLandmarks.Kind> KEPT_CLEAR = EnumSet.of(VolcanicLandmarks.Kind.EMBER_GIANT);

    private final int margin;

    public VolcanicLandmarkClearanceFilter(int margin) {
        this.margin = margin;
    }

    public static VolcanicLandmarkClearanceFilter of(int margin) {
        return new VolcanicLandmarkClearanceFilter(margin);
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        return !VolcanicLandmarks.near(context.getLevel(), KEPT_CLEAR, pos.getX(), pos.getZ(), margin);
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifierTypes.VOLCANIC_LANDMARK_CLEARANCE.get();
    }
}
