package nl.timtoys.corruptedadventure.entity.unfinished;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** A steady horizontal pull on a player toward a point (the lure and the void pull); sneaking resists. */
final class UnfinishedPull {
    /** Share of the pull a sneaking player still feels. */
    static final double SNEAK_SHARE = 0.4;
    /** Within this many blocks of the point the pull stops (no jitter on top of it). */
    private static final double DEAD_ZONE = 1.5;

    private UnfinishedPull() {
    }

    static void toward(Player player, Vec3 point, double strength) {
        double dx = point.x - player.getX();
        double dz = point.z - player.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < DEAD_ZONE) {
            return;
        }
        double pull = strength * (player.isShiftKeyDown() ? SNEAK_SHARE : 1.0) / length;
        player.push(dx * pull, 0.0, dz * pull);
        player.hurtMarked = true;
    }
}
