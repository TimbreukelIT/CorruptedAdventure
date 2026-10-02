package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * The shape of a {@code rubble} boulder, without world access: a small irregular clump of columns, each standing on
 * its own ground (so every block rests on terrain or on the block below it), the bigger ones half buried (the ground
 * block under each column becomes boulder too). The footprint shifts into the origin's chunk, so the feature only
 * reads and writes its own chunk.
 */
final class RubbleShape {
    static final int MAX_SIZE = 3;
    /** A column whose ground lies further from the origin's ground is left out: no towers on a slope. */
    private static final int MAX_GROUND_STEP = 1;

    private RubbleShape() {
    }

    /** What the shape reads of the world. */
    interface Terrain {
        /** Y of the top terrain block of the column ({@code WORLD_SURFACE_WG} − 1). */
        int groundTop(int x, int z);

        /** Whether a boulder may stand on (and, half buried, replace) this block. */
        boolean solid(int x, int y, int z);

        /** Whether a boulder block may go here (air or a replaceable plant). */
        boolean free(int x, int y, int z);
    }

    record Column(int dx, int dz, int height) {
    }

    /** The columns of a boulder of {@code size}, as offsets from its origin column. */
    static List<Column> columns(int size, RandomSource random) {
        List<Column> columns = new ArrayList<>();
        switch (size) {
            case 1 -> columns.add(new Column(0, 0, 1));
            case 2 -> {
                int peak = random.nextInt(4);
                int dropped = random.nextFloat() < 0.35F ? (peak + 1 + random.nextInt(3)) % 4 : -1;
                for (int i = 0; i < 4; i++) {
                    if (i != dropped) {
                        columns.add(new Column(i & 1, i >> 1, i == peak ? 2 : 1));
                    }
                }
            }
            default -> {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        boolean corner = dx != 0 && dz != 0;
                        if (dx == 0 && dz == 0) {
                            columns.add(new Column(0, 0, 2 + random.nextInt(2)));
                        } else if (!corner) {
                            columns.add(new Column(dx, dz, 1 + random.nextInt(2)));
                        } else if (random.nextBoolean()) {
                            columns.add(new Column(dx, dz, 1));
                        }
                    }
                }
            }
        }
        return columns;
    }

    /**
     * The blocks of one boulder near {@code origin}, bottom to top per column; the buried ground blocks come first in
     * each column. Empty if the origin column has no solid ground.
     */
    static List<BlockPos> blocks(BlockPos origin, int size, RandomSource random, Terrain terrain) {
        List<Column> columns = columns(size, random);
        int low = 0;
        int high = 0;
        for (Column column : columns) {
            low = Math.min(low, Math.min(column.dx(), column.dz()));
            high = Math.max(high, Math.max(column.dx(), column.dz()));
        }
        int minX = origin.getX() & ~15;
        int minZ = origin.getZ() & ~15;
        int x0 = minX + Mth.clamp(origin.getX() - minX, -low, 15 - high);
        int z0 = minZ + Mth.clamp(origin.getZ() - minZ, -low, 15 - high);
        int ground = terrain.groundTop(x0, z0);
        List<BlockPos> blocks = new ArrayList<>();
        if (!terrain.solid(x0, ground, z0) || !terrain.solid(x0, ground - 1, z0)) {
            return blocks;
        }
        boolean buried = size > 1;
        for (Column column : columns) {
            int x = x0 + column.dx();
            int z = z0 + column.dz();
            int top = terrain.groundTop(x, z);
            // Solid under the ground block too: never bury a block that hangs over a cave (sand left by a carver).
            if (Math.abs(top - ground) > MAX_GROUND_STEP || !terrain.solid(x, top, z) || !terrain.solid(x, top - 1, z)) {
                continue;
            }
            if (buried) {
                blocks.add(new BlockPos(x, top, z));
            }
            for (int k = 1; k <= column.height(); k++) {
                if (!terrain.free(x, top + k, z)) {
                    break;
                }
                blocks.add(new BlockPos(x, top + k, z));
            }
        }
        return blocks;
    }
}
