package nl.timtoys.corruptedadventure.entity.hollow;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import nl.timtoys.corruptedadventure.config.HollowConfig;
import nl.timtoys.corruptedadventure.config.ServerConfig;

/**
 * Veegslag: the giant arm's knuckles sweep a wide arc across its front, low over the ground. Everyone in the arc is
 * hit and knocked away from it; behind it, above its head or beyond its reach is safe.
 */
public final class HollowSweep {
    /** Blocks below and above its feet the arc reaches. */
    private static final double BELOW = 1.0;
    private static final double ABOVE = 3.5;

    private HollowSweep() {
    }

    /**
     * The sweep toward {@code yaw} (fixed when the arm was drawn back, so a player can see where it goes).
     *
     * @return how many it hit
     */
    public static int strike(ServerLevel level, TheHollow hollow, float yaw) {
        HollowConfig config = ServerConfig.HOLLOW;
        double reach = hollow.getBbWidth() * 0.5 + config.sweepReach.get();
        double arc = config.sweepArcDegrees.get();
        float damage = config.sweepDamage.get().floatValue();
        double push = config.sweepPush.get();
        AABB box = new AABB(hollow.getX() - reach - 1.0, hollow.getY() - BELOW, hollow.getZ() - reach - 1.0,
                hollow.getX() + reach + 1.0, hollow.getY() + ABOVE, hollow.getZ() + reach + 1.0);
        int hits = 0;
        for (LivingEntity victim : HollowStrike.victims(level, box)) {
            double dx = victim.getX() - hollow.getX();
            double dz = victim.getZ() - hollow.getZ();
            double edge = Math.sqrt(dx * dx + dz * dz) - victim.getBbWidth() * 0.5;
            if (edge <= reach && HollowStrikeShapes.inArc(yaw, dx, dz, arc)
                    && HollowStrike.hit(level, hollow, victim, damage, hollow.position(), push, 0.35)) {
                hits++;
            }
        }
        HollowEffects.sweep(level, hollow, yaw, reach * 0.75, (float) arc);
        return hits;
    }

    /** Minecraft's facing vector of {@code yaw} degrees, horizontal: {@code {x, z}}. */
    static double[] facing(float yaw) {
        float rad = yaw * Mth.DEG_TO_RAD;
        return new double[] {-Mth.sin(rad), Mth.cos(rad)};
    }
}
