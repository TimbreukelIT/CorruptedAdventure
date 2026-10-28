package nl.timtoys.corruptedadventure.entity.unfinished;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Where the void pull drags a player: the nearest void column (no block at all) round them, searched in square rings of
 * growing size on a 2-block grid (once per player at the start of a pull, ≤ ~650 heightmap lookups, loaded chunks only).
 */
final class VoidEdge {
    private static final int STEP = 2;

    private VoidEdge() {
    }

    static Optional<Vec3> nearest(Level level, Vec3 from, int search) {
        int cx = (int) Math.floor(from.x);
        int cz = (int) Math.floor(from.z);
        for (int ring = STEP; ring <= search; ring += STEP) {
            Vec3 best = null;
            double bestDistance = Double.MAX_VALUE;
            for (int dx = -ring; dx <= ring; dx += STEP) {
                for (int dz = -ring; dz <= ring; dz += STEP) {
                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) {
                        continue;
                    }
                    if (!UnfinishedGround.isVoid(level, cx + dx, cz + dz)) {
                        continue;
                    }
                    double distance = (double) dx * dx + (double) dz * dz;
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = new Vec3(cx + dx + 0.5, from.y, cz + dz + 0.5);
                    }
                }
            }
            if (best != null) {
                return Optional.of(best);
            }
        }
        return Optional.empty();
    }
}
