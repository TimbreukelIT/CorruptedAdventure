package nl.timtoys.corruptedadventure.entity.lostkeeper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.condition.Condition;
import nl.timtoys.corruptedadventure.condition.ConditionDose;

/**
 * The green cloud the poison breath leaves where it ends: for a few seconds every player in it gets poison points on the
 * ring 4 meter (creative and spectators not, {@link ConditionDose}). Server-side, kept by its keeper.
 */
final class KeeperPoisonCloud {
    private static final int PARTICLE_TICKS = 4;

    private final Vec3 centre;
    private final double radius;
    private int ticksLeft;

    KeeperPoisonCloud(Vec3 centre, double radius, int ticks) {
        this.centre = centre;
        this.radius = radius;
        this.ticksLeft = ticks;
    }

    /** One tick: the dose to everyone inside, now and then its particles. False once it has drifted away. */
    boolean tick(ServerLevel level, float dosePerTick) {
        if (ticksLeft-- <= 0) {
            return false;
        }
        ConditionDose dose = ConditionDose.of(Condition.POISON, dosePerTick);
        for (ServerPlayer player : level.players()) {
            if (player.isAlive() && player.getBoundingBox().getCenter().distanceToSqr(centre) <= radius * radius) {
                dose.apply(player);
            }
        }
        if (ticksLeft % PARTICLE_TICKS == 0) {
            KeeperEffects.cloud(level, centre, radius);
        }
        return true;
    }
}
