package nl.timtoys.corruptedadventure.client.item;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;

import java.util.Collection;

/**
 * The line segments the sky glass lens draws, as plain ints (x0, y0, z0, x1, y1, z1 per segment, block corners). A set of
 * blocks gets the outline of their union: only the edges where the surface folds, so a cluster of glitch blocks or a
 * hole's columns is one shape and not a grid. Pure; built once per payload, not per frame.
 */
final class LensOutline {
    /** The two axes across each edge axis (x: y and z; y: x and z; z: x and y). */
    private static final int[][] ACROSS = {{1, 2}, {0, 2}, {0, 1}};

    private LensOutline() {
    }

    /**
     * The folding edges of the union of the blocks: an edge is drawn where one or three of the four blocks round it are
     * in the set, or two that only touch along it (diagonally); two side by side make a flat face, four a solid.
     */
    static int[] union(Collection<BlockPos> blocks) {
        LongSet filled = new LongOpenHashSet(blocks.size());
        for (BlockPos pos : blocks) {
            filled.add(pos.asLong());
        }
        IntArrayList out = new IntArrayList();
        int[] start = new int[3];
        int[] cell = new int[3];
        boolean[] round = new boolean[4];
        for (int axis = 0; axis < 3; axis++) {
            LongSet done = new LongOpenHashSet();
            int u = ACROSS[axis][0];
            int v = ACROSS[axis][1];
            for (BlockPos pos : blocks) {
                for (int du = 0; du <= 1; du++) {
                    for (int dv = 0; dv <= 1; dv++) {
                        start[0] = pos.getX();
                        start[1] = pos.getY();
                        start[2] = pos.getZ();
                        start[u] += du;
                        start[v] += dv;
                        if (!done.add(BlockPos.asLong(start[0], start[1], start[2]))) {
                            continue;
                        }
                        // The four blocks round the edge: start − (0 or 1) along u and along v.
                        int count = 0;
                        for (int k = 0; k < 4; k++) {
                            System.arraycopy(start, 0, cell, 0, 3);
                            cell[u] -= k & 1;
                            cell[v] -= k >> 1;
                            round[k] = filled.contains(BlockPos.asLong(cell[0], cell[1], cell[2]));
                            count += round[k] ? 1 : 0;
                        }
                        boolean diagonal = count == 2 && round[0] == round[3];
                        if (count == 1 || count == 3 || diagonal) {
                            out.add(start[0]);
                            out.add(start[1]);
                            out.add(start[2]);
                            out.add(start[0] + (axis == 0 ? 1 : 0));
                            out.add(start[1] + (axis == 1 ? 1 : 0));
                            out.add(start[2] + (axis == 2 ? 1 : 0));
                        }
                    }
                }
            }
        }
        return out.toIntArray();
    }

    /** The 12 edges of the box from (x0, y0, z0) to (x1, y1, z1) (block corners). */
    static int[] box(int x0, int y0, int z0, int x1, int y1, int z1) {
        IntArrayList out = new IntArrayList(72);
        for (int a = 0; a <= 1; a++) {
            for (int b = 0; b <= 1; b++) {
                int x = a == 0 ? x0 : x1;
                int y = a == 0 ? y0 : y1;
                int yb = b == 0 ? y0 : y1;
                int zb = b == 0 ? z0 : z1;
                out.addElements(out.size(), new int[]{x0, y, zb, x1, y, zb});
                out.addElements(out.size(), new int[]{x, y0, zb, x, y1, zb});
                out.addElements(out.size(), new int[]{x, yb, z0, x, yb, z1});
            }
        }
        return out.toIntArray();
    }
}
