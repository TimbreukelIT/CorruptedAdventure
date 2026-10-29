package nl.timtoys.corruptedadventure.entity.unfinished;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The ground he tears open to climb out of at his arrival: a lens of {@code length} across his way to the target and
 * {@code width} along it, and in its middle the smaller lens of the void scar that stays. Only columns that may unload as
 * a whole ({@link UnfinishedUnload#count}: natural ground down to the bottom of the world, no block entity, no fluid in it
 * or beside it), so it never takes a build or anything over one. Server-side.
 */
final class UnfinishedTear {
    /** Added to the lens radii, so that a 3 × 5 lens is a ragged lens and not a cross. */
    private static final double FULLNESS = 0.35;

    /**
     * The planned tear.
     *
     * @param tear    columns that open for a while and come back
     * @param scar    columns that open for good (empty when the scar is off)
     * @param forward unit vector from the tear toward his target
     * @param surface the highest top of the tear's columns + 1 (the hole's surface for the void's catch)
     */
    record Plan(BlockPos centre, List<UnfinishedUnload.Column> tear, List<UnfinishedUnload.Column> scar, Vec3 forward,
                int surface) {
        boolean isEmpty() {
            return tear.isEmpty() && scar.isEmpty();
        }

        List<UnfinishedUnload.Column> all() {
            List<UnfinishedUnload.Column> all = new ArrayList<>(tear);
            all.addAll(scar);
            return all;
        }
    }

    private UnfinishedTear() {
    }

    /** The tear round {@code centre} facing {@code toward}; {@code scarLength} 0 = no scar. */
    static Plan plan(ServerLevel level, BlockPos centre, Vec3 toward, int length, int width, int scarLength, int scarWidth) {
        Vec3 forward = forward(centre, toward);
        List<UnfinishedUnload.Column> tear = new ArrayList<>();
        List<UnfinishedUnload.Column> scar = new ArrayList<>();
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
                UnfinishedUnload.Column column = new UnfinishedUnload.Column(x, z, top);
                surface = Math.max(surface, top + 1);
                if (scarLength > 0 && inLens(across, along, scarLength, scarWidth)) {
                    scar.add(column);
                } else {
                    tear.add(column);
                }
            }
        }
        return new Plan(centre, tear, scar, forward, surface);
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
}
