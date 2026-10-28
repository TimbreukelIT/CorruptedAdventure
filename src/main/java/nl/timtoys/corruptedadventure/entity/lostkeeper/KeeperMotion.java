package nl.timtoys.corruptedadventure.entity.lostkeeper;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The pure arithmetic of The Lost Keeper's swimming: turning a heading towards another with a limited angle per tick, and
 * the yaw and pitch of a heading (Minecraft's conventions: yaw 0 looks to +z, pitch positive looks down).
 */
public final class KeeperMotion {
    private KeeperMotion() {
    }

    /**
     * {@code from} turned towards {@code to} by at most {@code maxRadians} (both unit vectors; the result is one too).
     * Turning right round (opposite headings) goes over the top, so it never gets stuck.
     */
    public static Vec3 turn(Vec3 from, Vec3 to, double maxRadians) {
        double dot = Mth.clamp(from.dot(to), -1.0, 1.0);
        double angle = Math.acos(dot);
        if (angle <= maxRadians) {
            return to;
        }
        Vec3 axis = from.cross(to);
        if (axis.lengthSqr() < 1.0E-9) {
            axis = from.cross(new Vec3(0.0, 1.0, 0.0));
            if (axis.lengthSqr() < 1.0E-9) {
                axis = new Vec3(1.0, 0.0, 0.0);
            }
        }
        return rotate(from, axis.normalize(), maxRadians).normalize();
    }

    /** {@code v} turned round the unit {@code axis} by {@code radians} (Rodrigues). */
    static Vec3 rotate(Vec3 v, Vec3 axis, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return v.scale(cos).add(axis.cross(v).scale(sin)).add(axis.scale(axis.dot(v) * (1.0 - cos)));
    }

    /** The yaw (degrees) of a heading. */
    public static float yaw(Vec3 heading) {
        return (float) (Mth.atan2(-heading.x, heading.z) * Mth.RAD_TO_DEG);
    }

    /** The pitch (degrees) of a unit heading. */
    public static float pitch(Vec3 heading) {
        return (float) (Math.asin(Mth.clamp(-heading.y, -1.0, 1.0)) * Mth.RAD_TO_DEG);
    }

    /** The head's "up" for a look of {@code pitch}/{@code yaw}: the look tipped 90 degrees upwards. */
    public static Vec3 up(float pitch, float yaw) {
        return Vec3.directionFromRotation(pitch - 90.0F, yaw);
    }
}
