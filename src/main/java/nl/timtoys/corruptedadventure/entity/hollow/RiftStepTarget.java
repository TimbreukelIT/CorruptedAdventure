package nl.timtoys.corruptedadventure.entity.hollow;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Where a rift step comes out: behind the target (or near a point, for a reaction), on ground, with room for the whole hitbox, dry and inside the world
 * border. Tries the straight spot first, then fans out sideways and nearer/farther, and a few blocks up and down.
 */
final class RiftStepTarget {
    private static final int[] ANGLES = {0, 30, -30, 60, -60, 90, -90};
    private static final double[] DISTANCE_STEPS = {0.0, 1.0, -1.0};
    private static final double[] NEAR_DISTANCES = {0.0, 2.0, 4.0};
    private static final int UP = 3;
    private static final int DOWN = 4;

    private RiftStepTarget() {
    }

    static Optional<Vec3> behind(ServerLevel level, Entity hollow, LivingEntity target, double distance) {
        float yaw = target.getYHeadRot();
        for (double step : DISTANCE_STEPS) {
            double d = Math.max(1.0, distance + step);
            for (int angle : ANGLES) {
                float rad = (yaw + angle) * Mth.DEG_TO_RAD;
                // The look vector is (-sin, cos); behind is the opposite way.
                double x = target.getX() + Mth.sin(rad) * d;
                double z = target.getZ() - Mth.cos(rad) * d;
                Optional<Vec3> spot = standable(level, hollow, x, Mth.floor(target.getY()), z);
                if (spot.isPresent()) {
                    return spot;
                }
            }
        }
        return Optional.empty();
    }

    /** Near {@code point}: the point itself, then rings of 2 and 4 blocks round it. */
    static Optional<Vec3> near(ServerLevel level, Entity hollow, Vec3 point) {
        for (double d : NEAR_DISTANCES) {
            for (int angle = 0; angle < 360; angle += d == 0.0 ? 360 : 45) {
                float rad = angle * Mth.DEG_TO_RAD;
                Optional<Vec3> spot = standable(level, hollow, point.x + Mth.cos(rad) * d, Mth.floor(point.y),
                        point.z + Mth.sin(rad) * d);
                if (spot.isPresent()) {
                    return spot;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<Vec3> standable(ServerLevel level, Entity hollow, double x, int baseY, double z) {
        for (int dy = UP; dy >= -DOWN; dy--) {
            int y = baseY + dy;
            BlockPos below = BlockPos.containing(x, y - 1, z);
            if (!level.hasChunkAt(below) || !level.getWorldBorder().isWithinBounds(below)) {
                return Optional.empty();
            }
            if (level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
                continue;
            }
            Vec3 feet = new Vec3(x, y, z);
            AABB box = hollow.getType().getDimensions().makeBoundingBox(feet);
            if (level.noCollision(hollow, box) && !level.containsAnyLiquid(box)) {
                return Optional.of(feet);
            }
        }
        return Optional.empty();
    }
}
