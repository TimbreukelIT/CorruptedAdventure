package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import nl.timtoys.corruptedadventure.registry.ModPlacementModifierTypes;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicField;

import java.util.stream.Stream;

/**
 * Placement modifier {@code corruptedadventure:terrain_height} (change 41): moves a position to the ring 3 terrain's
 * surface height in its column ({@link VolcanicField.Column#height}, the top of the ground itself). Unlike a heightmap
 * it never lands on what stands above the ground: floating islands, arches, needles, ribs, or the lava of a river or
 * crater. A pure decision of the seed and x, z ({@link Ring3Terrain}); without ring 3 terrain no position remains.
 */
public final class TerrainHeightPlacement extends PlacementModifier {
    public static final TerrainHeightPlacement INSTANCE = new TerrainHeightPlacement();
    public static final MapCodec<TerrainHeightPlacement> CODEC = MapCodec.unit(INSTANCE);

    private TerrainHeightPlacement() {
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
        VolcanicField field = Ring3Terrain.field(context.getLevel());
        if (field == null) {
            return Stream.empty();
        }
        return Stream.of(new BlockPos(pos.getX(), Mth.floor(field.column(pos.getX(), pos.getZ()).height()), pos.getZ()));
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifierTypes.TERRAIN_HEIGHT.get();
    }
}
