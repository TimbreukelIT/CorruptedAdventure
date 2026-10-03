package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The pure core of {@code corruptedadventure:surface_fossil}: where the bones of one (rotated) template go, without
 * world access. The footprint shifts into the origin's chunk, at least {@link #MARGIN} from its edge, so the feature
 * only reads and writes its own chunk. The template sinks a share of its height below the lowest ground of its
 * footprint (half buried; higher ground buries more). Weathering removes blocks; what no longer connects to the
 * ground through the remaining bones (diagonals count, the ribs of vanilla's fossils only touch diagonally) is left
 * out, so no bone floats.
 */
final class FossilLayout {
    /** Distance of every bone from the chunk edge. */
    static final int MARGIN = 1;
    /** The widest footprint that fits in a chunk with {@link #MARGIN} on both sides. */
    static final int MAX_SPAN = 16 - 2 * MARGIN;

    private FossilLayout() {
    }

    /** One block of a template, relative to its rotated bounding box (all coordinates ≥ 0). */
    record Bone(int x, int y, int z, BlockState state) {
    }

    /** A bone in the world. */
    record Placed(BlockPos pos, BlockState state) {
    }

    /** What the layout reads of the world. */
    interface Terrain {
        /** Y of the top terrain block of the column ({@code WORLD_SURFACE_WG} − 1). */
        int groundTop(int x, int z);

        /** Whether this block is ground a fossil may lie in. */
        boolean ground(int x, int y, int z);

        /** Whether a bone may go here: air, a replaceable plant or ground. */
        boolean free(int x, int y, int z);
    }

    /** Whether the template fits in a chunk in this rotation. */
    static boolean fits(List<Bone> bones) {
        return !bones.isEmpty() && span(bones, true) <= MAX_SPAN && span(bones, false) <= MAX_SPAN;
    }

    /** How many layers of a template {@code height} high lie below the lowest ground: at least 1, at most all but one. */
    static int buriedLayers(int height, float buried) {
        return height <= 1 ? 1 : Mth.clamp(Math.round(buried * height), 1, height - 1);
    }

    /**
     * The bones to place, centred on {@code originX, originZ} as far as the chunk allows; empty if the template does
     * not fit or a column of its footprint has no ground on top. Draws one number per bone (weathering) in template
     * order, whatever is kept.
     */
    static List<Placed> place(List<Bone> bones, int originX, int originZ, float buried, float integrity, RandomSource random,
                              Terrain terrain) {
        if (!fits(bones)) {
            return List.of();
        }
        int spanX = span(bones, true);
        int spanZ = span(bones, false);
        int height = 0;
        for (Bone bone : bones) {
            height = Math.max(height, bone.y() + 1);
        }
        int minX = originX & ~15;
        int minZ = originZ & ~15;
        int x0 = minX + Mth.clamp(originX - minX - spanX / 2, MARGIN, 16 - MARGIN - spanX);
        int z0 = minZ + Mth.clamp(originZ - minZ - spanZ / 2, MARGIN, 16 - MARGIN - spanZ);

        Map<Long, Integer> tops = new HashMap<>();
        int lowest = Integer.MAX_VALUE;
        for (Bone bone : bones) {
            int x = x0 + bone.x();
            int z = z0 + bone.z();
            long column = BlockPos.asLong(x, 0, z);
            if (!tops.containsKey(column)) {
                int top = terrain.groundTop(x, z);
                if (!terrain.ground(x, top, z)) {
                    return List.of();
                }
                tops.put(column, top);
                lowest = Math.min(lowest, top);
            }
        }
        int y0 = lowest + 1 - buriedLayers(height, buried);

        Map<BlockPos, BlockState> kept = new HashMap<>();
        List<BlockPos> order = new ArrayList<>();
        for (Bone bone : bones) {
            boolean weathered = random.nextFloat() >= integrity;
            BlockPos pos = new BlockPos(x0 + bone.x(), y0 + bone.y(), z0 + bone.z());
            if (!weathered && terrain.free(pos.getX(), pos.getY(), pos.getZ())) {
                kept.put(pos, bone.state());
                order.add(pos);
            }
        }

        // Keep only bones connected to one that lies in or on the ground.
        Set<BlockPos> connected = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos pos : order) {
            if (pos.getY() <= tops.get(BlockPos.asLong(pos.getX(), 0, pos.getZ())) + 1 && connected.add(pos)) {
                queue.add(pos);
            }
        }
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        BlockPos next = pos.offset(dx, dy, dz);
                        if (kept.containsKey(next) && connected.add(next)) {
                            queue.add(next);
                        }
                    }
                }
            }
        }
        List<Placed> placed = new ArrayList<>(connected.size());
        for (BlockPos pos : order) {
            if (connected.contains(pos)) {
                placed.add(new Placed(pos, kept.get(pos)));
            }
        }
        return placed;
    }

    private static int span(List<Bone> bones, boolean alongX) {
        int max = -1;
        for (Bone bone : bones) {
            max = Math.max(max, alongX ? bone.x() : bone.z());
        }
        return max + 1;
    }
}
