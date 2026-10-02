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
 * <p>
 * The one place for the fluid rules per ring: {@link #noFluid} (void edge) and {@link #waterOnly} (ring 2); change 35
 * adds the Nether's fluid levels here.
 */
public record RingFluidPicker(ResourceKey<Level> dimension, Aquifer.FluidPicker vanilla) implements Aquifer.FluidPicker {
    private static final Aquifer.FluidStatus NO_FLUID = new Aquifer.FluidStatus(DimensionType.MIN_Y * 2, Blocks.AIR.defaultBlockState());
    /** Vanilla's global picker fills open space below this Y with lava (the overworld's lava layer). */
    static final int LAVA_LAYER_TOP = -54;

    /** Whether worldgen keeps all open space at this position free of fluid. */
    static boolean noFluid(RingPosition position) {
        return position.outer() == Ring.VOID;
    }

    /**
     * Whether local aquifer lava turns into water here: in overworld ring 2 above vanilla's lava layer ("the water
     * under the ground"). The lava layer itself stays. Switches hard on {@link RingPosition#ring()}, so a lava cell
     * across the 1↔2 boundary is water on the ring 2 side.
     */
    static boolean waterOnly(ResourceKey<Level> dimension, RingPosition position, int y) {
        return dimension == Level.OVERWORLD && position.ring() == Ring.RING_2 && y >= LAVA_LAYER_TOP;
    }

    @Override
    public Aquifer.FluidStatus computeFluid(int x, int y, int z) {
        return computeFluid(RingColumnCache.ringAt(dimension, x, z), x, y, z);
    }

    Aquifer.FluidStatus computeFluid(RingPosition position, int x, int y, int z) {
        return noFluid(position) ? NO_FLUID : vanilla.computeFluid(x, y, z);
    }
}
