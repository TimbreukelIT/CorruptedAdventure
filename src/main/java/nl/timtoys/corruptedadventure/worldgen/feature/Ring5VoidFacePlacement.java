package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import nl.timtoys.corruptedadventure.registry.ModPlacementModifierTypes;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.Ring5Field;

import java.util.List;
import java.util.stream.Stream;

/**
 * Placement modifier {@code corruptedadventure:ring5_void_face} (95, V11-A): moves a candidate column onto rock that faces
 * the void, where the outer band's nullite lies: a wall beside the void (a scar, a crater, a floe's edge; within
 * {@link #WALL_REACH} of a void column) at a height between the floe's underside and its top, else the underside of a
 * floe ({@link #UNDERSIDE} blocks up from it) or of a floating chunk over the column. Nothing on the inner band's ground
 * (no underside) or over the empty void. Pure in the terrain; the height from the placement's random.
 */
public final class Ring5VoidFacePlacement extends PlacementModifier {
    public static final Ring5VoidFacePlacement INSTANCE = new Ring5VoidFacePlacement();
    public static final MapCodec<Ring5VoidFacePlacement> CODEC = MapCodec.unit(() -> INSTANCE);
    static final int WALL_REACH = 2;
    static final int UNDERSIDE = 2;

    private Ring5VoidFacePlacement() {
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
        Ring5World world = Ring5Terrain.world(context.getLevel());
        int x = pos.getX();
        int z = pos.getZ();
        if (world == null || !world.core(x, z)) {
            return Stream.empty();
        }
        Ring5Field.Column column = world.field().column(x, z);
        if (column.isVoid()) {
            List<Ring5Field.Fragment> fragments = world.field().fragmentsWithin(x, z, x, z);
            if (fragments.isEmpty()) {
                return Stream.empty();
            }
            Ring5Field.Fragment fragment = fragments.get(random.nextInt(fragments.size()));
            return Stream.of(new BlockPos(x, fragment.bottom() + random.nextInt(UNDERSIDE + 1), z));
        }
        double bottom = column.bottom();
        if (!Double.isFinite(bottom)) {
            return Stream.empty();
        }
        int low = Mth.floor(bottom) + 1;
        int top = world.top(x, z);
        if (world.voidDistance(x, z, WALL_REACH) <= WALL_REACH && top > low) {
            return Stream.of(new BlockPos(x, low + random.nextInt(top - low + 1), z));
        }
        return Stream.of(new BlockPos(x, low + random.nextInt(UNDERSIDE + 1), z));
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifierTypes.RING5_VOID_FACE.get();
    }
}
