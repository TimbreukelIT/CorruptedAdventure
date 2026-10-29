package nl.timtoys.corruptedadventure.entity.layered;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Where the parts of The Layered's model are, for the server (the head part, the held player) and the renderer (turning
 * the model onto a wall) alike, so what you see and what you hit agree. Points are given in its body frame: blocks
 * {@code up} from its feet and {@code forward} along where it faces (the model in
 * {@code geo/entity/the_layered.geo.json}, 16 px = 1 block).
 * <p>
 * On a wall the whole model turns nose-up by 90° around {@link #WALL_PIVOT_UP} / {@link #WALL_PIVOT_FORWARD}: the belly
 * lies against the wall (the wall is in front of it, where it faces), the head points up and the abdomen hangs below its
 * hitbox. The pivot is chosen so that the leg tips (its feet) touch the wall at the front of the 2.5-block hitbox and the
 * middle of the body sits in the middle of the hitbox.
 */
public final class LayeredPose {
    public static final double WALL_PIVOT_UP = 1.45;
    public static final double WALL_PIVOT_FORWARD = -0.2;
    /** The living head (the head part's centre). */
    public static final double HEAD_UP = 1.6;
    public static final double HEAD_FORWARD = 1.7;
    /** The tip of the beak: bites are measured from here. */
    public static final double BEAK_UP = 0.4;
    public static final double BEAK_FORWARD = 2.5;
    /** The feet of a held player, hanging in the beak. */
    public static final double HELD_UP = 0.35;
    public static final double HELD_FORWARD = 2.4;
    /** On a wall a held player stays this far from the wall's face (half a player plus a little). */
    private static final double HELD_WALL_GAP = 0.4;
    private static final double HALF_WIDTH = 1.25;

    private LayeredPose() {
    }

    /**
     * The world offset from the entity's position of a body-frame point, with the body facing {@code yaw} (degrees) and
     * {@code wall} how far it has turned onto a wall (0 on the ground, 1 on a wall).
     */
    public static Vec3 offset(double up, double forward, float yaw, float wall) {
        double angle = wall * Mth.HALF_PI;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double ru = up - WALL_PIVOT_UP;
        double rf = forward - WALL_PIVOT_FORWARD;
        double u = WALL_PIVOT_UP + ru * cos + rf * sin;
        double f = WALL_PIVOT_FORWARD + rf * cos - ru * sin;
        return horizontal(u, f, yaw);
    }

    /** The held player's feet: in the beak, and on a wall never inside the wall. */
    public static Vec3 heldOffset(float yaw, float wall) {
        Vec3 point = offset(HELD_UP, HELD_FORWARD, yaw, wall);
        if (wall <= 0.0F) {
            return point;
        }
        double forward = forwardOf(point, yaw);
        double limit = HALF_WIDTH - HELD_WALL_GAP;
        return forward > limit ? point.subtract(direction(yaw).scale(forward - limit)) : point;
    }

    private static Vec3 horizontal(double up, double forward, float yaw) {
        Vec3 dir = direction(yaw);
        return new Vec3(dir.x * forward, up, dir.z * forward);
    }

    /** Minecraft's facing for a yaw: 0 is south (+z), 90 is west (-x). */
    private static Vec3 direction(float yaw) {
        float rad = yaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(rad), 0.0, Mth.cos(rad));
    }

    private static double forwardOf(Vec3 offset, float yaw) {
        Vec3 dir = direction(yaw);
        return offset.x * dir.x + offset.z * dir.z;
    }
}
