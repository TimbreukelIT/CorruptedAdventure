package nl.timtoys.corruptedadventure.worldgen.chunk;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Aquifer;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.Ring5Band;
import nl.timtoys.corruptedadventure.ring.RingColumnCache;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.Ring5Field;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.ThreadOwned;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicField;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.ZoneFracture;

import javax.annotation.Nullable;

import java.util.Map;

/**
 * No sea fluid from the start of the ring 5 → void transition outwards, nor in overworld ring 5 ({@link #ring5Dry}) and
 * the fracture zone 4→5 ({@link #fractureZone});
 * everywhere else the vanilla picker. Starting
 * at the zone instead of at the boundary keeps the terrain that dips towards the edge dry, so no water or lava wall
 * stands at the edge of the world. Local aquifer fluids are handled by {@link RingAquifer}.
 * <p>
 * The one place for the fluid rules per ring: {@link #noFluid} (void edge), {@link #waterOnlyFrom} (rings 2 and 4),
 * {@link #ring3LavaColumn}, {@link #ring3DryFrom} and {@link #ring3SealBelow} (overworld ring 3),
 * {@link #ring4DryBelow}, {@link #ring4DamBelow}, {@link #ring4NoLava} and {@link #ring4WaterColumn} (overworld ring 4,
 * package 59; {@link #verdantWater}: also the {@code verdant_falls} strip of ring 3's half of the zone 3→4),
 * {@link #ring5Dry}, {@link #ring5LavaColumn} and {@link #ring5Dam} (overworld ring 5, package 82), each per
 * column (a Y bound), so that {@link RingAquifer}'s per-block path only compares Y (change 39e), and the fluid levels per ring
 * ({@code levels}, from the
 * generator's {@code fluid_levels}: the default fluid fills open space up to and including that Y instead of up to the
 * dimension's sea level; Nether ring 2 and 3 have a lower lava sea, overworld ring 3 has no sea above Y 0).
 */
public final class RingFluidPicker implements Aquifer.FluidPicker {
    private static final Aquifer.FluidStatus NO_FLUID = new Aquifer.FluidStatus(DimensionType.MIN_Y * 2, Blocks.AIR.defaultBlockState());
    /** Vanilla's global picker fills open space below this Y with lava (the overworld's lava layer). */
    static final int LAVA_LAYER_TOP = -54;
    /** Overworld ring 3 has no water at or above this Y. */
    public static final int RING_3_DRY_FROM = 0;
    /** The overworld's sea level (noise settings), which ring 3's neighbours keep. */
    static final int SEA_LEVEL = 63;
    /**
     * The dam of ring 5's lava layer (82, V9-B): the inner band keeps vanilla's lava layer only this far (in
     * {@link Ring5Band#signedDistance}, radial blocks) inside the line between the bands; the strip up to the line holds
     * rock below the layer's top, so no lava reaches the outer band. At least ~{@code RING_5_DAM / 5} blocks wide where
     * the line's noise is steepest.
     */
    static final double RING_5_DAM = 32.0;
    /**
     * The dam of ring 5's lava layer round the loose cracks of the inner band (83): within this many blocks of a crack no
     * lava and no open space below Y −54; the crack itself stays open down into the void.
     */
    static final double RING_5_CRACK_DAM = 6.0;
    /** Ring 5's band (and the inner band's cracks) in the running server's world (worldgen); tests give their own. */
    static final Ring5Bands CURRENT_BANDS = new Ring5Bands() {
        @Override
        public double signedDistance(int x, int z) {
            return Ring5Band.of(RingLayout.current()).signedDistance(x, z);
        }

        @Override
        public double crackDistance(int x, int z) {
            // Without the field the dam round the cracks cannot be decided: fail loud rather than let the lava through.
            Ring5Field field = Ring5Fields.current(Level.OVERWORLD);
            if (field == null) {
                throw new IllegalStateException("ring 5 terrain of the overworld is not registered: no crack dam for the lava layer");
            }
            return field.innerOpeningDistance(x, z);
        }
    };

    private final ResourceKey<Level> dimension;
    private final Aquifer.FluidPicker vanilla;
    private final Map<Ring, Integer> levels;
    // The per-block path (vf-perf2): the levels by ring ordinal (null: none), and per ring the last status made from a
    // vanilla status. Vanilla's picker hands out a few fixed statuses, so
    // this skips a map lookup and an allocation per open block. Racy but safe: the entries are immutable.
    private final Integer[] levelByRing;
    private final Derived[] derived;
    private final Ring5Bands bands;

    public RingFluidPicker(ResourceKey<Level> dimension, Aquifer.FluidPicker vanilla, Map<Ring, Integer> levels) {
        this(dimension, vanilla, levels, CURRENT_BANDS);
    }

    /** @param bands ring 5's band ({@link Ring5Band#signedDistance}): the running server's in worldgen, a fixed one in tests */
    RingFluidPicker(ResourceKey<Level> dimension, Aquifer.FluidPicker vanilla, Map<Ring, Integer> levels, Ring5Bands bands) {
        this.dimension = dimension;
        this.bands = bands;
        this.vanilla = vanilla;
        this.levels = Map.copyOf(levels);
        this.levelByRing = new Integer[Ring.values().length];
        this.levels.forEach((ring, level) -> levelByRing[ring.ordinal()] = level);
        this.derived = new Derived[Ring.values().length];
    }

    public RingFluidPicker(ResourceKey<Level> dimension, Aquifer.FluidPicker vanilla) {
        this(dimension, vanilla, Map.of());
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public Aquifer.FluidPicker vanilla() {
        return vanilla;
    }

    public Map<Ring, Integer> levels() {
        return levels;
    }

    /** Whether worldgen keeps all open space at this position free of fluid. */
    static boolean noFluid(RingPosition position) {
        return position.outer() == Ring.VOID;
    }

    /**
     * Local aquifer lava turns into water in overworld ring 2, ring 4 and its dams from this Y up: above vanilla's lava
     * layer ("the water under the ground"; ring 4 has no lava at all, 59). In ring 2 the lava layer itself stays; ring 4
     * has none ({@link #ring4DryBelow}). {@link Integer#MAX_VALUE} (never) elsewhere. Switches hard on
     * {@link RingPosition#ring()}, so a lava cell across the 1↔2 boundary is water on the ring 2 side; ring 4's rule
     * covers the whole zone 4→5 (59, decision of the user), so no water meets lava at the 4|5 midline: the seam lies at
     * the zone's outer edge, where ring 5 is dry (82, {@link #ring5Dry}; its lava layer starts behind ring 4's dam).
     */
    static int waterOnlyFrom(ResourceKey<Level> dimension, RingPosition position) {
        return dimension == Level.OVERWORLD && (position.ring() == Ring.RING_2 || ring4NoLava(dimension, position))
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
        return dimension == Level.OVERWORLD && position.ring() == Ring.RING_3 && !verdantWater(dimension, position) && field != null;
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
     * Whether the column may hold ring 4's recorded water ({@link IslandField.Column#fallTop}: waterfalls, springs, lakes
     * above the sea), placed by {@link RingAquifer}: in overworld ring 4 and in the {@code verdant_falls} strip
     * ({@link #verdantWater}), with the islands present.
     */
    static boolean ring4WaterColumn(ResourceKey<Level> dimension, RingPosition position, @Nullable IslandField islands) {
        return dimension == Level.OVERWORLD && (position.ring() == Ring.RING_4 || verdantWater(dimension, position)) && islands != null;
    }

    /**
     * Whether a position lies in the {@code verdant_falls} part of the overworld zone 3→4: from
     * {@link IslandField#VERDANT_EDGE_SHARE} of the zone outwards, where the terrain is ring 4's alone
     * ({@code ring_blend} {@code mix.ring_3}). Ring 4's water branch holds there too, also in ring 3's half (its lakes,
     * springs and falls on the descent), and ring 3's terrain lava does not. Ring 3's seal below Y 63, the drying of
     * vanilla's water from Y 0 up and the dams stay as they are.
     */
    static boolean verdantWater(ResourceKey<Level> dimension, RingPosition position) {
        return dimension == Level.OVERWORLD && position.inTransition() && position.inner() == Ring.RING_3
                && position.blend() >= IslandField.VERDANT_EDGE_SHARE;
    }

    /**
     * Whether open space in the column is decided by ring 4's own water rule instead of vanilla's local aquifer (73):
     * overworld ring 4, including its half of the zone 3→4, but not the zone 4→5 (its own sea at Y 0). There every open
     * block up to the sea's top block is water and above it air: one flat level for every cave, so no water stands
     * beside air (vanilla's local levels and empty cells hung walls of water in ring 4's caves, 5a), and caves under the
     * sea that open into it are full. The tunnel net's sealed spaces ({@code Ring4Tunnels}) come before this rule.
     */
    public static boolean ring4Flooded(ResourceKey<Level> dimension, RingPosition position) {
        return dimension == Level.OVERWORLD && position.ring() == Ring.RING_4 && !fractureZone(dimension, position);
    }

    /**
     * Whether a position lies in the overworld zone 4→5, both halves: the fracture zone (85, V7-A), dry. No sea, no
     * aquifer water or lava: open space is air from vanilla's lava layer up ({@code RingAquifer}), below it the dam of
     * ring 4 ({@link #ring4DamBelow}) holds rock, so ring 5's lava layer starts only behind the zone. Ring 4's own sea
     * stops against the rim of ground that its terrain keeps at the zone's inner edge, and the calm first strip is
     * sealed ({@link #fractureSealBelow}).
     */
    public static boolean fractureZone(ResourceKey<Level> dimension, RingPosition position) {
        return dimension == Level.OVERWORLD && position.inTransition() && position.inner() == Ring.RING_4
                && position.outer() == Ring.RING_5;
    }

    /**
     * Open space stays rock below the sea level in the calm first share of the zone 4→5 (review 5a,
     * {@link ZoneFracture#CALM_SHARE}, where no crack cuts): ring 4's sea stands at Y 62 beside the zone's dry air, so a
     * cave across the inner edge would hold water on one side and air on the other. {@link Integer#MIN_VALUE} elsewhere.
     */
    static int fractureSealBelow(ResourceKey<Level> dimension, RingPosition position) {
        return fractureZone(dimension, position) && position.blend() < ZoneFracture.CALM_SHARE ? SEA_LEVEL : Integer.MIN_VALUE;
    }

    /**
     * Ring 5's band at a column: {@link Ring5Band#signedDistance} (negative in the inner band), and how far the column lies
     * from an opening into the void of the inner band ({@link Ring5Field#innerOpeningDistance}: loose cracks, corrupt spots'
     * holes and, since 93, the holes of the void caves; at most 0 inside one).
     */
    @FunctionalInterface
    interface Ring5Bands {
        double signedDistance(int x, int z);

        default double crackDistance(int x, int z) {
            return Double.POSITIVE_INFINITY;
        }
    }

    /**
     * Whether ring 5's fluid rule holds in the column (82, V9-B): overworld ring 5 (switching hard on
     * {@link RingPosition#ring()}), its half of the zone 4→5 included (85), not the void edge ({@link #noFluid}). Ring 5 is
     * dry: no sea, no aquifer water, no local lava; open space is air, except vanilla's lava layer below Y −54 in the
     * inner band ({@link #ring5LavaColumn}) with its dam ({@link #ring5Dam}). In the zone 4→5 the zone's rule comes first
     * ({@link #fractureZone}: dry, and ring 4's dam below Y −54), so the lava layer starts only behind it.
     */
    static boolean ring5Dry(ResourceKey<Level> dimension, RingPosition position) {
        return dimension == Level.OVERWORLD && position.ring() == Ring.RING_5 && !noFluid(position);
    }

    /**
     * Whether a column of ring 5 ({@link #ring5Dry}) keeps vanilla's lava layer below Y −54 (V9-B): the inner band, more
     * than {@link #RING_5_DAM} inside the line ({@code signedDistance}: {@link Ring5Band#signedDistance}) and more than
     * {@link #RING_5_CRACK_DAM} from a loose crack ({@code crackDistance}, 83: the cracks reach down into the void).
     */
    static boolean ring5LavaColumn(double signedDistance, double crackDistance) {
        return signedDistance < -RING_5_DAM && crackDistance > RING_5_CRACK_DAM;
    }

    /**
     * Whether a column of ring 5 ({@link #ring5Dry}) is the dam of its lava layer, where open space below Y −54 stays
     * rock: the strip of the inner band up to the line, and the strip round a loose crack (83; not the crack itself, which
     * is open down into the void). The outer band beyond is dry down to the void.
     */
    static boolean ring5Dam(double signedDistance, double crackDistance) {
        return signedDistance < 0.0 && crackDistance > 0.0
                && (signedDistance >= -RING_5_DAM || crackDistance <= RING_5_CRACK_DAM);
    }

    @Override
    public Aquifer.FluidStatus computeFluid(int x, int y, int z) {
        return computeFluid(RingColumnCache.ringAt(dimension, x, z), x, y, z);
    }

    /**
     * This picker for one noise chunk (vf-perf2): the same answers, but the ring lookup per open block goes to the
     * thread's {@link RingColumnCache} without a {@code ThreadLocal.get} (slow in the worldgen threads). A noise chunk
     * is used by one thread at a time; a shared picker stays this one.
     */
    public Aquifer.FluidPicker forNoiseChunk() {
        ThreadOwned<RingColumnCache> columns = new ThreadOwned<>(RingColumnCache::forThread);
        return (x, y, z) -> computeFluid(columns.get().at(dimension, x, z), x, y, z);
    }

    Aquifer.FluidStatus computeFluid(RingPosition position, int x, int y, int z) {
        if (noFluid(position) || fractureZone(dimension, position)) {
            return NO_FLUID;
        }
        if (ring5Dry(dimension, position)) {
            // Dry, but the inner band keeps vanilla's lava layer (the band is only asked down there).
            if (y >= LAVA_LAYER_TOP) {
                return NO_FLUID;
            }
            double band = bands.signedDistance(x, z);
            return band < -RING_5_DAM && ring5LavaColumn(band, bands.crackDistance(x, z)) ? vanilla.computeFluid(x, y, z) : NO_FLUID;
        }
        Aquifer.FluidStatus status = vanilla.computeFluid(x, y, z);
        Integer level = levelByRing[position.ring().ordinal()];
        if (level == null || dimension == Level.OVERWORLD && y < LAVA_LAYER_TOP) {
            // No level of its own, or the overworld's lava layer, which stays vanilla's: a ring's level only moves the sea
            // above it.
            return status;
        }
        // Vanilla's status fills below its level; "up to and including Y" is one more. The fluid itself is vanilla's.
        int slot = position.ring().ordinal();
        Derived known = derived[slot];
        if (known != null && known.source() == status) {
            return known.result();
        }
        Aquifer.FluidStatus result = new Aquifer.FluidStatus(level + 1, status.at(Integer.MIN_VALUE));
        derived[slot] = new Derived(status, result);
        return result;
    }

    /** A status with a ring's level ({@code result}) made from vanilla's status {@code source}. */
    private record Derived(Aquifer.FluidStatus source, Aquifer.FluidStatus result) {
    }
}
