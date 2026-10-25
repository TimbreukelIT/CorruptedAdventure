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

/**
 * Placement filter {@code corruptedadventure:ring_2_landmark_clearance} (G2 ring 2): keeps a position more than
 * {@code margin} blocks outside the footprint of every ring 2 landmark of {@code kind} ({@link Ring2FlatLandmarks}).
 * The oasis palms use it so that no palm crown from a neighbouring chunk reaches into the giant palm or its pool: the
 * landmark then depends on its own chunks only, in either order of generation. Pure in the seed and the terrain.
 */
public final class Ring2LandmarkClearanceFilter extends PlacementFilter {
    public static final MapCodec<Ring2LandmarkClearanceFilter> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ring2FlatLandmarks.Kind.CODEC.fieldOf("kind").forGetter(filter -> filter.kind),
            Codec.intRange(0, 16).fieldOf("margin").forGetter(filter -> filter.margin)
    ).apply(instance, Ring2LandmarkClearanceFilter::new));

    private final Ring2FlatLandmarks.Kind kind;
    private final int margin;

    public Ring2LandmarkClearanceFilter(Ring2FlatLandmarks.Kind kind, int margin) {
        this.kind = kind;
        this.margin = margin;
    }

    public static Ring2LandmarkClearanceFilter of(Ring2FlatLandmarks.Kind kind, int margin) {
        return new Ring2LandmarkClearanceFilter(kind, margin);
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        return !Ring2FlatLandmarks.near(context.getLevel(), kind, pos.getX(), pos.getZ(), margin);
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifierTypes.RING_2_LANDMARK_CLEARANCE.get();
    }
}
