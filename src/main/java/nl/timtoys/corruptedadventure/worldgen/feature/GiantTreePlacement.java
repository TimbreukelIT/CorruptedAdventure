package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import nl.timtoys.corruptedadventure.registry.ModPlacementModifierTypes;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;

import java.util.stream.Stream;

/**
 * Placement modifier {@code corruptedadventure:giant_tree} (ring 4 decor D, 2026-10-19): the trunk column of each giant
 * verdant tree ({@link VerdantGiantTrees}) that lies in this chunk, at the chunk's origin height (a heightmap modifier
 * follows). The spots are pure in the seed, so each tree lands in exactly one chunk whatever the order of generation;
 * draws nothing from the placement random.
 */
public final class GiantTreePlacement extends PlacementModifier {
    public static final GiantTreePlacement INSTANCE = new GiantTreePlacement();
    public static final MapCodec<GiantTreePlacement> CODEC = MapCodec.unit(() -> INSTANCE);

    private GiantTreePlacement() {
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
        IslandField field = Ring4Terrain.field(context.getLevel());
        if (field == null) {
            return Stream.empty();
        }
        int minX = pos.getX() & ~15;
        int minZ = pos.getZ() & ~15;
        return VerdantGiantTrees.near(field, minX, minZ, minX + 15, minZ + 15, 0).stream()
                .map(spot -> new BlockPos(spot.x(), pos.getY(), spot.z()));
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifierTypes.GIANT_TREE.get();
    }
}
