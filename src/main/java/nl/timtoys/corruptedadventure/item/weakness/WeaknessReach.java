package nl.timtoys.corruptedadventure.item.weakness;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Who a weakness item reaches (server): never the user, never spectators. */
final class WeaknessReach {
    private WeaknessReach() {
    }

    /**
     * Living entities whose eyes are within {@code range} of {@code user}'s eyes, inside the cone of {@code coneDegrees}
     * (half angle) round its look direction and in its line of sight. A player only where {@code user} can harm it
     * (the server's pvp setting and teams).
     */
    static List<LivingEntity> inCone(ServerLevel level, Player user, double range, double coneDegrees) {
        Vec3 eyes = user.getEyePosition();
        Vec3 look = user.getLookAngle();
        double minCos = Math.cos(Math.toRadians(coneDegrees));
        AABB box = new AABB(eyes, eyes).inflate(range);
        return level.getEntitiesOfClass(LivingEntity.class, box, target -> {
            if (!reachable(user, target) || target instanceof Player player && !user.canHarmPlayer(player)) {
                return false;
            }
            Vec3 toTarget = target.getEyePosition().subtract(eyes);
            double distance = toTarget.length();
            return distance <= range && (distance < 1.0E-4 || toTarget.dot(look) / distance >= minCos)
                    && user.hasLineOfSight(target);
        });
    }

    /**
     * Living entities other than players whose hitbox comes within {@code radius} of {@code user}'s eyes, through walls.
     * The hitbox rather than the feet, so a large flyer counts by its nearest part.
     */
    static List<LivingEntity> inRadius(ServerLevel level, Player user, double radius) {
        Vec3 eyes = user.getEyePosition();
        AABB box = new AABB(eyes, eyes).inflate(radius);
        double radiusSqr = radius * radius;
        return level.getEntitiesOfClass(LivingEntity.class, box, target -> reachable(user, target)
                && !(target instanceof Player) && nearestPoint(target.getBoundingBox(), eyes).distanceToSqr(eyes) <= radiusSqr);
    }

    private static Vec3 nearestPoint(AABB box, Vec3 point) {
        return new Vec3(Math.clamp(point.x, box.minX, box.maxX), Math.clamp(point.y, box.minY, box.maxY),
                Math.clamp(point.z, box.minZ, box.maxZ));
    }

    private static boolean reachable(Player user, LivingEntity target) {
        return target != user && target.isAlive() && !target.isSpectator();
    }
}
