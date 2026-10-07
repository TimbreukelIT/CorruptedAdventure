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
 * The one place for the fluid rules per ring: {@link #noFluid} (void edge), {@link #waterOnly} (ring 2),
 * {@link #ring3Lava}, {@link #ring3Dry} and {@link #ring3Seal} (overworld ring 3) and the fluid levels per ring ({@code levels}, from the
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
     * Whether local aquifer lava turns into water here: in overworld ring 2 above vanilla's lava layer ("the water
     * under the ground"). The lava layer itself stays. Switches hard on {@link RingPosition#ring()}, so a lava cell
     * across the 1↔2 boundary is water on the ring 2 side.
     */
    static boolean waterOnly(ResourceKey<Level> dimension, RingPosition position, int y) {
        return dimension == Level.OVERWORLD && position.ring() == Ring.RING_2 && y >= LAVA_LAYER_TOP;
    }

    /**
     * Whether open space is lava in overworld ring 3: the lava lakes of active craters and the lava channels, up to the
     * column's lava top ({@link VolcanicField.Column#lavaAt}). Switches hard on {@link RingPosition#ring()}, like the
     * other rules. The lava is placed by {@link RingAquifer}, not by this picker: a picker fluid also spreads through
     * vanilla's aquifer sampling into neighbouring open space (lava sheets beside a crater).
     */
    static boolean ring3Lava(ResourceKey<Level> dimension, RingPosition position, @Nullable VolcanicField field, int x, int y, int z) {
        return dimension == Level.OVERWORLD && position.ring() == Ring.RING_3 && field != null && field.column(x, z).lavaAt(y);
    }

    /**
     * Whether water turns into air in overworld ring 3: from Y 0 up ("the water was stolen"; deep aquifers below stay).
     * The sea itself is already below Y 0 there ({@code fluid_levels.ring_3} of the preset); this also dries the local
     * aquifer pockets in caves.
     */
    static boolean ring3Dry(ResourceKey<Level> dimension, RingPosition position, int y) {
        return dimension == Level.OVERWORLD && position.ring() == Ring.RING_3 && y >= RING_3_DRY_FROM;
    }

    /**
     * Whether open space stays rock instead: below the sea level in ring 3's half of its transition zones (2→3 and
     * 3→4). The neighbours keep their sea (Y 63) and aquifer water up to the middle of the zone and ring 3 is dry;
     * without a seal, water on their side would stand against air on ring 3's side and pour over on the first update.
     * The surface of the zone 2→3 lies above Y 66 (ring 2's beds stop there), so there only caves and carvers are
     * closed; in the zone 3→4 also a surface dip below Y 63 is filled (ring 4 is still placeholder terrain).
     */
    static boolean ring3Seal(ResourceKey<Level> dimension, RingPosition position, int y) {
        return dimension == Level.OVERWORLD && position.ring() == Ring.RING_3 && position.inTransition() && y < SEA_LEVEL;
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
