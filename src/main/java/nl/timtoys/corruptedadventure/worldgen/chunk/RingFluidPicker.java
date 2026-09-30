package nl.timtoys.corruptedadventure.worldgen.chunk;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Aquifer;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingColumnCache;
import nl.timtoys.corruptedadventure.ring.RingPosition;

/**
 * No sea fluid from the start of the ring 5 → void transition outwards; everywhere else the vanilla picker. Starting
 * at the zone instead of at the boundary keeps the terrain that dips towards the edge dry, so no water or lava wall
 * stands at the edge of the world. Local aquifer fluids are handled by {@link RingAquifer}.
 */
public record RingFluidPicker(ResourceKey<Level> dimension, Aquifer.FluidPicker vanilla) implements Aquifer.FluidPicker {
    private static final Aquifer.FluidStatus NO_FLUID = new Aquifer.FluidStatus(DimensionType.MIN_Y * 2, Blocks.AIR.defaultBlockState());

    /** Whether worldgen keeps all open space at this position free of fluid. */
    static boolean noFluid(RingPosition position) {
        return position.outer() == Ring.VOID;
    }

    @Override
    public Aquifer.FluidStatus computeFluid(int x, int y, int z) {
        return computeFluid(RingColumnCache.ringAt(dimension, x, z), x, y, z);
    }

    Aquifer.FluidStatus computeFluid(RingPosition position, int x, int y, int z) {
        return noFluid(position) ? NO_FLUID : vanilla.computeFluid(x, y, z);
    }
}
