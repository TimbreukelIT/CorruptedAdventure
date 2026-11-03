package nl.timtoys.corruptedadventure.entity.unfinished;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The ground he tears open to climb out of at his arrival: a lens of {@code length} across his way to the target and
 * {@code width} along it, open down to the void for a while. Only columns that may unload as a whole
 * ({@link UnfinishedUnload#count}: natural ground down to the bottom of the world, no block entity, no fluid in it or
 * beside it), so it never takes a build or anything over one. When the tear loads back in, its middle comes back as a
 * shallow scar (fase-4-nightmares-concept.md, V1 = B): the smaller lens {@code depth} blocks deep with a floor of
 * voidstone (which breathes void motes), and round it a rim of voidstone and corrupted dirt on natural ground. Falling
 * into it is safe. Server-side.
 */
final class UnfinishedTear {
    /** Added to the lens radii, so that a 3 × 5 lens is a ragged lens and not a cross. */
    private static final double FULLNESS = 0.35;

    /**
     * The planned tear.
     *
     * @param columns every column that opens for a while
     * @param scar    those of the scar (x, z as {@link #key}); empty when the scar is off
     * @param rim     those round the scar whose top turns to its rim
     * @param forward unit vector from the tear toward his target
     * @param surface the highest top of the tear's columns + 1 (the hole's surface for the void's catch)
     */
    record Plan(BlockPos centre, List<UnfinishedUnload.Column> columns, Set<Long> scar, Set<Long> rim, Vec3 forward,
                int surface) {
        boolean isEmpty() {
            return columns.isEmpty();
        }

        boolean isScar(int x, int z) {
            return scar.contains(key(x, z));
        }

        boolean isRim(int x, int z) {
            return rim.contains(key(x, z));
        }

        /** What the scar's blocks come back as once the tear loads back in; its floor never lies below {@code minY}. */
        UnfinishedUnload.RestoreAs scarShape(int depth, int minY) {
            BlockState air = Blocks.AIR.defaultBlockState();
            BlockState voidstone = ModBlocks.VOIDSTONE.get().defaultBlockState();
            BlockState dirt = ModBlocks.CORRUPTED_DIRT.get().defaultBlockState();
            return (column, y, original) -> {
                int top = column.top();
                if (isScar(column.x(), column.z())) {
                    int floor = Math.max(top - depth, minY);
                    if (y > floor) {
                        return air;
                    }
                    return y == floor ? voidstone : null;
                }
                if (isRim(column.x(), column.z()) && y == top && UnfinishedNature.isGround(original)) {
                    // A ragged rim: voidstone and corrupted dirt in a fixed pattern.
                    return Math.floorMod(column.x() * 31 + column.z() * 17, 3) == 0 ? dirt : voidstone;
                }
                return null;
            };
        }
    }

    private UnfinishedTear() {
    }

    /** The tear round {@code centre} facing {@code toward}; {@code scarLength} 0 = no scar (and no rim). */
    static Plan plan(ServerLevel level, BlockPos centre, Vec3 toward, int length, int width, int scarLength, int scarWidth) {
        Vec3 forward = forward(centre, toward);
        List<UnfinishedUnload.Column> columns = new ArrayList<>();
        Set<Long> scar = new HashSet<>();
        Set<Long> rim = new HashSet<>();
        int reach = Mth.ceil(Math.max(length, width) / 2.0 + FULLNESS);
        int surface = centre.getY();
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                // Along and across his way, from the middle of the column.
                double along = dx * forward.x + dz * forward.z;
                double across = dx * forward.z - dz * forward.x;
                if (!inLens(across, along, length, width)) {
                    continue;
                }
                int x = centre.getX() + dx;
                int z = centre.getZ() + dz;
                if (UnfinishedUnload.count(level, x, z) <= 0) {
                    continue;
                }
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                columns.add(new UnfinishedUnload.Column(x, z, top));
                surface = Math.max(surface, top + 1);
                if (scarLength > 0 && inLens(across, along, scarLength, scarWidth)) {
                    scar.add(key(x, z));
                } else if (scarLength > 0 && inLens(across, along, scarLength + 2, scarWidth + 2)) {
                    rim.add(key(x, z));
                }
            }
        }
        return new Plan(centre, columns, scar, rim, forward, surface);
    }

    /** Whether a point {@code across, along} of the middle lies in a lens {@code length} across and {@code width} along. */
    static boolean inLens(double across, double along, double length, double width) {
        double a = across / (length / 2.0 + FULLNESS);
        double b = along / (width / 2.0 + FULLNESS);
        return a * a + b * b <= 1.0;
    }

    /** Unit vector from {@code centre} toward {@code toward}, flat; north when they coincide. */
    static Vec3 forward(BlockPos centre, Vec3 toward) {
        Vec3 flat = new Vec3(toward.x - (centre.getX() + 0.5), 0.0, toward.z - (centre.getZ() + 0.5));
        return flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, -1.0) : flat.normalize();
    }

    static long key(int x, int z) {
        return (long) x << 32 | (z & 0xFFFFFFFFL);
    }
}
