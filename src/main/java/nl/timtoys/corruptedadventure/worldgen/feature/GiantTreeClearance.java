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
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;

/**
 * Placement filter {@code corruptedadventure:giant_tree_clearance} (ring 4 decor D, 2026-10-19): keeps a position at
 * least {@code clearance} blocks (horizontally) from every giant verdant tree's trunk ({@link VerdantGiantTrees}), so
 * the emergent trees of {@code verdant_falls} never grow into a giant's crown. Pure; draws nothing.
 */
public final class GiantTreeClearance extends PlacementFilter {
    public static final MapCodec<GiantTreeClearance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(1, 32).fieldOf("clearance").forGetter(filter -> filter.clearance)
    ).apply(instance, GiantTreeClearance::new));

    private final int clearance;

    public GiantTreeClearance(int clearance) {
        this.clearance = clearance;
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        IslandField field = Ring4Terrain.field(context.getLevel());
        if (field == null) {
            return true;
        }
        int x = pos.getX();
        int z = pos.getZ();
        for (VerdantGiantTrees.Spot spot : VerdantGiantTrees.near(field, x, z, x, z, clearance)) {
            // From the 2 × 2 trunk's middle.
            if (Math.hypot(spot.x() + 0.5 - x, spot.z() + 0.5 - z) < clearance) {
                return false;
            }
        }
        return true;
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifierTypes.GIANT_TREE_CLEARANCE.get();
    }
}
