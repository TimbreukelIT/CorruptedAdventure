package nl.timtoys.corruptedadventure.entity.lostkeeper;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Lost Keeper's body: a chain of {@link #SEGMENTS} segments behind the head, as points ("nodes"). Node 0 is the neck
 * joint behind the head, node i the back end of segment i. Each tick the neck goes where the head puts it and every next
 * node is pulled after the one before it, at exactly {@link #SEGMENT_LENGTH} ("follow the leader"), so the body trails
 * the head's path like a serpent's. The server and the client both run it from the head's movement (as vanilla's ender
 * dragon does with its parts), so nothing of it is synced; its hitboxes ({@link KeeperPart}) and the model's segments
 * (client) sit on these nodes.
 * <p>
 * The numbers match the model ({@code tools/textures/lost_keeper_model.py}: AXIS_Y, NECK_Z, SEG_LEN, SEGMENTS).
 */
public final class KeeperBody {
    public static final int SEGMENTS = 12;
    /** Blocks between two nodes (26 model units). */
    public static final double SEGMENT_LENGTH = 26.0 / 16.0;
    /** Blocks from the head's centre back to the neck joint (25.6 model units). */
    public static final double NECK = 25.6 / 16.0;
    /** Height of the head's centre (and the body's axis in the model) above the entity's position (20 model units). */
    public static final double AXIS = 20.0 / 16.0;

    private final double[] x = new double[SEGMENTS + 1];
    private final double[] y = new double[SEGMENTS + 1];
    private final double[] z = new double[SEGMENTS + 1];
    private final double[] xo = new double[SEGMENTS + 1];
    private final double[] yo = new double[SEGMENTS + 1];
    private final double[] zo = new double[SEGMENTS + 1];
    private boolean placed;

    public boolean isPlaced() {
        return placed;
    }

    /** The neck joint of a head whose entity position is {@code pos}, looking along {@code look} (a unit vector). */
    public static Vec3 neck(Vec3 pos, Vec3 look) {
        return new Vec3(pos.x - look.x * NECK, pos.y + AXIS - look.y * NECK, pos.z - look.z * NECK);
    }

    /** Lays the body out straight behind the neck, level, against the horizontal look direction (yaw in degrees). */
    public void reset(Vec3 neck, float yaw) {
        double bx = Mth.sin(yaw * Mth.DEG_TO_RAD);
        double bz = -Mth.cos(yaw * Mth.DEG_TO_RAD);
        for (int i = 0; i <= SEGMENTS; i++) {
            x[i] = neck.x + bx * SEGMENT_LENGTH * i;
            y[i] = neck.y;
            z[i] = neck.z + bz * SEGMENT_LENGTH * i;
        }
        System.arraycopy(x, 0, xo, 0, x.length);
        System.arraycopy(y, 0, yo, 0, y.length);
        System.arraycopy(z, 0, zo, 0, z.length);
        placed = true;
    }

    /** One tick: the neck to its new place, every further node pulled after the one before it. */
    public void follow(Vec3 neck) {
        System.arraycopy(x, 0, xo, 0, x.length);
        System.arraycopy(y, 0, yo, 0, y.length);
        System.arraycopy(z, 0, zo, 0, z.length);
        x[0] = neck.x;
        y[0] = neck.y;
        z[0] = neck.z;
        for (int i = 1; i <= SEGMENTS; i++) {
            pull(i);
        }
    }

    /** Node i back to exactly one segment length from node i - 1, along the line between them. */
    private void pull(int i) {
        double dx = x[i] - x[i - 1];
        double dy = y[i] - y[i - 1];
        double dz = z[i] - z[i - 1];
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-6) {
            // On top of each other (a teleport): straight behind the segment before, or simply below.
            if (i >= 2) {
                dx = x[i - 1] - x[i - 2];
                dy = y[i - 1] - y[i - 2];
                dz = z[i - 1] - z[i - 2];
                length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            }
            if (length < 1.0E-6) {
                dx = 0.0;
                dy = -1.0;
                dz = 0.0;
                length = 1.0;
            }
        }
        double scale = SEGMENT_LENGTH / length;
        x[i] = x[i - 1] + dx * scale;
        y[i] = y[i - 1] + dy * scale;
        z[i] = z[i - 1] + dz * scale;
    }

    /**
     * The tail whip: the nodes behind {@code pivot} swing round it horizontally by {@code radians} (positive: from +x
     * towards +z), with the rest of the chain following on the next {@link #follow}.
     */
    public void swing(int pivot, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        for (int i = pivot + 1; i <= SEGMENTS; i++) {
            double dx = x[i] - x[pivot];
            double dz = z[i] - z[pivot];
            x[i] = x[pivot] + dx * cos - dz * sin;
            z[i] = z[pivot] + dx * sin + dz * cos;
        }
    }

    /**
     * The ring of its body: every node from 1 on is drawn by {@code share} (0–1) towards its place on a circle of
     * {@code radius} round ({@code cx}, {@code cz}), the arc length from the head behind it (against {@code direction},
     * the way the head swims round), its axis {@code lift[i]} above the floor {@code floorY}. With the pull of
     * {@link #follow} the chain then lies along the ring instead of cutting inside it.
     */
    public void wrap(Vec3 head, double cx, double floorY, double cz, double radius, int direction, double[] lift, double share) {
        if (radius < 1.0E-3) {
            return;
        }
        double angle = Math.atan2(head.z - cz, head.x - cx);
        for (int i = 1; i <= SEGMENTS; i++) {
            double at = angle - direction * (NECK + i * SEGMENT_LENGTH) / radius;
            x[i] += (cx + Math.cos(at) * radius - x[i]) * share;
            y[i] += (floorY + lift[i] - y[i]) * share;
            z[i] += (cz + Math.sin(at) * radius - z[i]) * share;
        }
    }

    /**
     * Without its swimming power (on land, or falling): every node from 1 on that has air below it sinks by up to
     * {@code fall} blocks, so the body comes to lie on the ground instead of floating. {@code radius} is half the body's
     * thickness there (blocks).
     */
    public void settle(Level level, double fall, double radius) {
        BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();
        for (int i = 1; i <= SEGMENTS; i++) {
            below.set(x[i], y[i] - radius - 0.05, z[i]);
            if (level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
                y[i] -= fall;
            }
        }
    }

    public Vec3 node(int i) {
        return new Vec3(x[i], y[i], z[i]);
    }

    /** Node i between the last tick and this one. */
    public Vec3 node(int i, float partialTick) {
        return new Vec3(Mth.lerp(partialTick, xo[i], x[i]), Mth.lerp(partialTick, yo[i], y[i]),
                Mth.lerp(partialTick, zo[i], z[i]));
    }

    public Vec3 oldNode(int i) {
        return new Vec3(xo[i], yo[i], zo[i]);
    }
}
