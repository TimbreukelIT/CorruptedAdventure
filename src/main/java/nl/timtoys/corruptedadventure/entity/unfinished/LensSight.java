package nl.timtoys.corruptedadventure.entity.unfinished;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.item.SkyGlassLensItem;

import java.util.List;

/**
 * The weakness of The Unfinished (sky glass, phase 2's lens): whether any player is looking at him through a sky glass
 * lens right now, on the server ({@link SkyGlassLensItem#lookingThrough}, the same state {@code LensWatch} scans with).
 * Looking at him = within range, one of his sight points (his body's centre, the weak spots) inside a narrow cone round
 * the line of sight, and no block between. Creative players count (a builder can test it); spectators do not.
 */
final class LensSight {
    private LensSight() {
    }

    static boolean anyLooking(List<? extends Player> players, Vec3 body, List<Vec3> points, double range, double coneDegrees) {
        double cos = Math.cos(Math.toRadians(coneDegrees));
        for (Player player : players) {
            if (!player.isAlive() || player.isSpectator() || !SkyGlassLensItem.lookingThrough(player)
                    || player.distanceToSqr(body) > range * range) {
                continue;
            }
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getViewVector(1.0F);
            for (Vec3 point : points) {
                if (inCone(eye, look, point, cos) && clear(player, eye, point)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Whether {@code point} lies within the cone round {@code look} (a unit vector) from {@code eye}; pure, for tests. */
    static boolean inCone(Vec3 eye, Vec3 look, Vec3 point, double cos) {
        Vec3 to = point.subtract(eye);
        double length = to.length();
        return length < 1.0E-3 || to.dot(look) / length >= cos;
    }

    private static boolean clear(Player player, Vec3 eye, Vec3 point) {
        return player.level().clip(new ClipContext(eye, point, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player))
                .getType() == HitResult.Type.MISS;
    }
}
