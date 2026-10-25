package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The blocks of one ring 3 landmark ({@link VolcanicLandmarks}), in world positions, before any chunk writes its part
 * ({@link VolcanicLandmarkFeature}). Pure in the site and the pure terrain: the same plan in every chunk that asks.
 * <ul>
 *   <li>{@link #objects}: blocks at fixed heights (from the pure ground), written over whatever is there
 *   ({@link GroundObjects#placeObject}); air clears a hollow. {@link #feet}: a column of a block from the column's own
 *   ground down into it, up to under a planned block, so nothing floats where the real ground lies lower than the pure
 *   one;</li>
 *   <li>{@link #covers}: a column's own ground recoloured and raised, relative to its frozen {@code WORLD_SURFACE_WG}
 *   height, so a flow or a dune follows the real ground block for block;</li>
 *   <li>{@link #decor}: plants and layers on a block of the plan, only into air and only when the support is in the
 *   same chunk; {@link #groundDecor}: loose blocks or plants on a column's own ground.</li>
 * </ul>
 */
final class VolcanicPlan {
    /** A column filled with {@code state} from its own ground {@code sink} blocks deep up to under {@code bottom}. */
    record Foot(int x, int z, int bottom, int sink, BlockState state) {
    }

    /**
     * A column's own ground: its top {@code depth} solid blocks become {@code fill}, then {@code raise} blocks of
     * {@code raised} go on top (into air or replaceable plants), then {@code cap} (a layer, may be null) over them.
     */
    record Cover(int depth, BlockState fill, int raise, BlockState raised, BlockState cap) {
    }

    /** A plant or layer at {@code pos} that needs the block at {@code support}. */
    record Decor(BlockPos pos, BlockState state, BlockPos support) {
    }

    private final int x;
    private final int z;
    private final int reach;
    final Map<BlockPos, BlockState> objects = new LinkedHashMap<>();
    final List<Foot> feet = new ArrayList<>();
    /** Per column (x, 0, z). */
    final Map<BlockPos, Cover> covers = new LinkedHashMap<>();
    final List<Decor> decor = new ArrayList<>();
    /** Per column (x, 0, z): a block on its own ground (into air). */
    final Map<BlockPos, BlockState> groundDecor = new LinkedHashMap<>();

    /** An empty plan for a landmark anchored at (x, z) that keeps every block within {@code reach} of it. */
    VolcanicPlan(int x, int z, int reach) {
        this.x = x;
        this.z = z;
        this.reach = reach;
    }

    /** Whether (px, pz) lies within the landmark's reach. */
    boolean inReach(int px, int pz) {
        return Math.abs(px - x) <= reach && Math.abs(pz - z) <= reach;
    }

    /** {@code state} at {@code pos} unless the plan already holds a block there or it lies out of reach. */
    void put(BlockPos pos, BlockState state) {
        if (inReach(pos.getX(), pos.getZ())) {
            objects.putIfAbsent(pos.immutable(), state);
        }
    }

    /** {@code state} at {@code pos} over what the plan holds there (within reach). */
    void set(BlockPos pos, BlockState state) {
        if (inReach(pos.getX(), pos.getZ())) {
            objects.put(pos.immutable(), state);
        }
    }

    void foot(int px, int pz, int bottom, int sink, BlockState state) {
        if (inReach(px, pz)) {
            feet.add(new Foot(px, pz, bottom, sink, state));
        }
    }

    /** The column's cover (the first one planned for a column stays). */
    void cover(int px, int pz, Cover cover) {
        if (inReach(px, pz)) {
            covers.putIfAbsent(new BlockPos(px, 0, pz), cover);
        }
    }

    void decor(BlockPos pos, BlockState state, BlockPos support) {
        if (inReach(pos.getX(), pos.getZ()) && inReach(support.getX(), support.getZ())) {
            decor.add(new Decor(pos.immutable(), state, support.immutable()));
        }
    }

    void groundDecor(int px, int pz, BlockState state) {
        if (inReach(px, pz)) {
            groundDecor.putIfAbsent(new BlockPos(px, 0, pz), state);
        }
    }

    /** Whether the plan holds a solid block (no air) at {@code pos}. */
    boolean solid(BlockPos pos) {
        BlockState state = objects.get(pos);
        return state != null && !state.isAir();
    }

    /** Every column (x, 0, z) the plan writes into, in plan order. */
    List<BlockPos> columns() {
        Map<BlockPos, Boolean> seen = new LinkedHashMap<>();
        objects.keySet().forEach(pos -> seen.putIfAbsent(new BlockPos(pos.getX(), 0, pos.getZ()), true));
        feet.forEach(foot -> seen.putIfAbsent(new BlockPos(foot.x(), 0, foot.z()), true));
        covers.keySet().forEach(column -> seen.putIfAbsent(column, true));
        decor.forEach(d -> seen.putIfAbsent(new BlockPos(d.pos().getX(), 0, d.pos().getZ()), true));
        groundDecor.keySet().forEach(column -> seen.putIfAbsent(column, true));
        return List.copyOf(seen.keySet());
    }

    boolean isEmpty() {
        return objects.isEmpty() && covers.isEmpty();
    }
}
