package nl.timtoys.corruptedadventure.worldgen.chunk;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Aquifer;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingColumnCache;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicField;

import javax.annotation.Nullable;

import java.util.Map;

/**
 * No sea fluid from the start of the ring 5 → void transition outwards; everywhere else the vanilla picker. Starting
 * at the zone instead of at the boundary keeps the terrain that dips towards the edge dry, so no water or lava wall
 * stands at the edge of the world. Local aquifer fluids are handled by {@link RingAquifer}.
 * <p>
 * The one place for the fluid rules per ring: {@link #noFluid} (void edge), {@link #waterOnlyFrom} (rings 2 and 4),
 * {@link #ring3LavaColumn}, {@link #ring3DryFrom} and {@link #ring3SealBelow} (overworld ring 3),
 * {@link #ring4DryBelow}, {@link #ring4DamBelow}, {@link #ring4NoLava} and {@link #ring4FallColumn} (overworld ring 4,
 * package 59), each per
 * column (a Y bound), so that {@link RingAquifer}'s per-block path only compares Y (change 39e), and the fluid levels per ring
 * ({@code levels}, from the
 * generator's {@code fluid_levels}: the default fluid fills open space up to and including that Y instead of up to the
 * dimension's sea level; Nether ring 2 and 3 have a lower lava sea, overworld ring 3 has no sea above Y 0).
 */
public record RingFluidPicker(ResourceKey<Level> dimension, Aquifer.FluidPicker vanilla, Map<Ring, Integer> levels) implements Aquifer.FluidPicker {
    private static final Aquifer.FluidStatus NO_FLUID = new Aquifer.FluidStatus(DimensionType.MIN_Y * 2, Blocks.AIR.defaultBlockState());
    /** Vanilla's global picker fills open space below this Y with lava (the overworld's lava layer). */
    static final int LAVA_LAYER_TOP = -54;
    /** Overworld ring 3 has no water at or above this Y. */
    public static final int RING_3_DRY_FROM = 0;
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
     * Local aquifer lava turns into water in overworld rings 2 and 4 from this Y up: above vanilla's lava layer ("the
     * water under the ground"; ring 4 has no lava at all, 59). In ring 2 the lava layer itself stays; ring 4 has none
     * ({@link #ring4DryBelow}). {@link Integer#MAX_VALUE} (never) elsewhere. Switches hard on {@link RingPosition#ring()},
     * so a lava cell across the 1↔2 boundary is water on the ring 2 side.
     */
    static int waterOnlyFrom(ResourceKey<Level> dimension, RingPosition position) {
        return dimension == Level.OVERWORLD && (position.ring() == Ring.RING_2 || position.ring() == Ring.RING_4)
                ? LAVA_LAYER_TOP : Integer.MAX_VALUE;
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

    /**
     * Open space is air below this Y in overworld ring 4 (59, decision of the user: dry, no lava): vanilla's lava layer
     * (and any aquifer fluid) below Y −54 stays empty. Not in its dams ({@link #ring4DamBelow}), which hold rock there;
     * {@link Integer#MIN_VALUE} (never) elsewhere. Switches hard on {@link RingPosition#ring()}.
     */
    static int ring4DryBelow(ResourceKey<Level> dimension, RingPosition position) {
        return dimension == Level.OVERWORLD && position.ring() == Ring.RING_4 && !ring4Dam(position)
                ? LAVA_LAYER_TOP : Integer.MIN_VALUE;
    }

    /**
     * Open space stays rock below this Y in the dams around ring 4's dry bottom ({@link #ring4DryBelow}), 59: the whole
     * zone 4→5 (both halves), so the lava layer of ring 5 starts only beyond it, and ring 4's half of the zone 3→4, so
     * the carver lava in ring 3's half (sealed below Y 63 by {@link #ring3SealBelow}) is walled in as well.
     * {@link Integer#MIN_VALUE} (never) elsewhere.
     */
    static int ring4DamBelow(ResourceKey<Level> dimension, RingPosition position) {
        return dimension == Level.OVERWORLD && ring4Dam(position) ? LAVA_LAYER_TOP : Integer.MIN_VALUE;
    }

    private static boolean ring4Dam(RingPosition position) {
        return position.inTransition() && (position.inner() == Ring.RING_4
                || position.outer() == Ring.RING_4 && position.ring() == Ring.RING_4);
    }

    /**
     * Whether no carver may place lava in this column (59): overworld ring 4 and its dams. Vanilla carvers put lava at
     * and below their lava level (Y −56) without asking the aquifer; there {@link RingAquifer#carverLava} answers
     * instead (a dry bottom stays air, a dam is not cut). Elsewhere carvers keep vanilla's lava.
     */
    static boolean ring4NoLava(ResourceKey<Level> dimension, RingPosition position) {
        return dimension == Level.OVERWORLD && (position.ring() == Ring.RING_4 || ring4Dam(position));
    }

    /**
     * Whether the column may hold one of ring 4's recorded waterfalls ({@link IslandField.Column#fallTop}), placed by
     * {@link RingAquifer}: in overworld ring 4 with its islands present.
     */
    static boolean ring4FallColumn(ResourceKey<Level> dimension, RingPosition position, @Nullable IslandField islands) {
        return dimension == Level.OVERWORLD && position.ring() == Ring.RING_4 && islands != null;
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
