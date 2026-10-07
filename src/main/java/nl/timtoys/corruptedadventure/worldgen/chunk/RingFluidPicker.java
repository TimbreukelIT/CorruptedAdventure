package nl.timtoys.corruptedadventure.worldgen.chunk;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Aquifer;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingColumnCache;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicField;

import javax.annotation.Nullable;

import java.util.Map;

/**
 * No sea fluid from the start of the ring 5 → void transition outwards; everywhere else the vanilla picker. Starting
 * at the zone instead of at the boundary keeps the terrain that dips towards the edge dry, so no water or lava wall
 * stands at the edge of the world. Local aquifer fluids are handled by {@link RingAquifer}.
 * <p>
 * The one place for the fluid rules per ring: {@link #noFluid} (void edge), {@link #waterOnlyFrom} (ring 2),
 * {@link #ring3LavaColumn}, {@link #ring3DryFrom} and {@link #ring3SealBelow} (overworld ring 3), each per column (a Y
 * bound), so that {@link RingAquifer}'s per-block path only compares Y (change 39e), and the fluid levels per ring
 * ({@code levels}, from the
 * generator's {@code fluid_levels}: the default fluid fills open space up to and including that Y instead of up to the
 * dimension's sea level; Nether ring 2 and 3 have a lower lava sea, overworld ring 3 has no sea above Y 0).
 */
public record RingFluidPicker(ResourceKey<Level> dimension, Aquifer.FluidPicker vanilla, Map<Ring, Integer> levels) implements Aquifer.FluidPicker {
    private static final Aquifer.FluidStatus NO_FLUID = new Aquifer.FluidStatus(DimensionType.MIN_Y * 2, Blocks.AIR.defaultBlockState());
    /** Vanilla's global picker fills open space below this Y with lava (the overworld's lava layer). */
    static final int LAVA_LAYER_TOP = -54;
    /** Overworld ring 3 has no water at or above this Y. */
    static final int RING_3_DRY_FROM = 0;
    /** The overworld's sea level (noise settings), which ring 3's neighbours keep. */
    static final int SEA_LEVEL = 63;

    public RingFluidPicker {
        levels = Map.copyOf(levels);
    }

    public RingFluidPicker(ResourceKey<Level> dimension, Aquifer.FluidPicker vanilla) {
        this(dimension, vanilla, Map.of());
    }

    /** Whether worldgen keeps all open space at this position free of fluid. */
    static boolean noFluid(RingPosition position) {
        return position.outer() == Ring.VOID;
    }

    /**
     * Local aquifer lava turns into water in overworld ring 2 from this Y up: above vanilla's lava layer ("the water
     * under the ground"); the lava layer itself stays. {@link Integer#MAX_VALUE} (never) elsewhere. Switches hard on
     * {@link RingPosition#ring()}, so a lava cell across the 1↔2 boundary is water on the ring 2 side.
     */
    static int waterOnlyFrom(ResourceKey<Level> dimension, RingPosition position) {
        return dimension == Level.OVERWORLD && position.ring() == Ring.RING_2 ? LAVA_LAYER_TOP : Integer.MAX_VALUE;
    }

    /**
     * Whether open space in this column is lava where the column's lava is ({@link VolcanicField.Column#lavaAt}): the
     * lava lakes of active craters and the lava channels, in overworld ring 3. Switches hard on
     * {@link RingPosition#ring()}, like the other rules. The lava is placed by {@link RingAquifer}, not by this picker: a
     * picker fluid also spreads through vanilla's aquifer sampling into neighbouring open space (lava sheets beside a
     * crater).
     */
    static boolean ring3LavaColumn(ResourceKey<Level> dimension, RingPosition position, @Nullable VolcanicField field) {
        return dimension == Level.OVERWORLD && position.ring() == Ring.RING_3 && field != null;
    }

    /**
     * Water turns into air in overworld ring 3 from this Y up: from Y 0 ("the water was stolen"; deep aquifers below
     * stay). {@link Integer#MAX_VALUE} (never) elsewhere. The sea itself is already below Y 0 there
     * ({@code fluid_levels.ring_3} of the preset); this also dries the local aquifer pockets in caves.
     */
    static int ring3DryFrom(ResourceKey<Level> dimension, RingPosition position) {
        return dimension == Level.OVERWORLD && position.ring() == Ring.RING_3 ? RING_3_DRY_FROM : Integer.MAX_VALUE;
    }

    /**
     * Open space stays rock below this Y: below the sea level in ring 3's half of its transition zones (2→3 and 3→4);
     * {@link Integer#MIN_VALUE} (never) elsewhere. The neighbours keep their sea (Y 63) and aquifer water up to the
     * middle of the zone and ring 3 is dry; without a seal, water on their side would stand against air on ring 3's
     * side and pour over on the first update. The surface of the zone 2→3 lies above Y 66 (ring 2's beds stop there),
     * so there only caves and carvers are closed; in the zone 3→4 also a surface dip below Y 63 is filled (ring 4 is
     * still placeholder terrain).
     */
    static int ring3SealBelow(ResourceKey<Level> dimension, RingPosition position) {
        return dimension == Level.OVERWORLD && position.ring() == Ring.RING_3 && position.inTransition()
                ? SEA_LEVEL : Integer.MIN_VALUE;
    }

    @Override
    public Aquifer.FluidStatus computeFluid(int x, int y, int z) {
        return computeFluid(RingColumnCache.ringAt(dimension, x, z), x, y, z);
    }

    Aquifer.FluidStatus computeFluid(RingPosition position, int x, int y, int z) {
        if (noFluid(position)) {
            return NO_FLUID;
        }
        Aquifer.FluidStatus status = vanilla.computeFluid(x, y, z);
        Integer level = levels.get(position.ring());
        if (level != null && dimension == Level.OVERWORLD && y < LAVA_LAYER_TOP) {
            // The overworld's lava layer stays vanilla's; a ring's level only moves the sea above it.
            return status;
        }
        // Vanilla's status fills below its level; "up to and including Y" is one more. The fluid itself is vanilla's.
        return level == null ? status : new Aquifer.FluidStatus(level + 1, status.at(Integer.MIN_VALUE));
    }
}
