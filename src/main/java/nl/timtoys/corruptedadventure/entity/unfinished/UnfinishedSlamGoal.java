package nl.timtoys.corruptedadventure.entity.unfinished;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.config.UnfinishedConfig;

/**
 * His main attack, with the grafted arm of The Hollow (or, once that fell away, his whole torso): he turns so that his
 * target stands where the fist lands, front-left of him ({@link #FIST_LEFT}, {@link #FIST_FORWARD}: the slam animation's
 * landing spot), raises it for {@code slamWindupTicks} and brings it down. Everything within {@code slamRadius} of that
 * spot (and a few blocks up or down) takes the attack damage and is thrown away from it. Whoever stepped away is safe.
 */
final class UnfinishedSlamGoal extends UnfinishedAttackGoal {
    static final double FIST_LEFT = 6.0;
    static final double FIST_FORWARD = 6.5;
    private static final double MAX_DISTANCE = 13.0;
    private static final double REACH_UP = 4.0;
    private static final double REACH_DOWN = 2.0;

    UnfinishedSlamGoal(TheUnfinished boss) {
        super(boss, null);
    }

    @Override
    protected int cooldown() {
        UnfinishedConfig config = ServerConfig.UNFINISHED;
        return UnfinishedConfig.cooldown(config.slamCooldownMin, config.slamCooldownMax, roll());
    }

    @Override
    protected int duration() {
        return Math.max(UnfinishedAnimations.SLAM_TICKS, ServerConfig.UNFINISHED.slamWindupTicks.get() + 10);
    }

    @Override
    protected boolean wants(LivingEntity target) {
        return boss.distanceToSqr(target) <= MAX_DISTANCE * MAX_DISTANCE;
    }

    @Override
    protected void begin(ServerLevel level, LivingEntity target) {
        boss.faceWithLocalPoint(target.position(), FIST_LEFT, FIST_FORWARD);
        boss.triggerAnim(UnfinishedAnimations.ACTION, UnfinishedAnimations.SLAM);
        UnfinishedEffects.slamWindup(level, boss);
    }

    @Override
    protected void act(ServerLevel level, LivingEntity target, int tick) {
        int windup = ServerConfig.UNFINISHED.slamWindupTicks.get();
        if (tick < windup - 8) {
            boss.faceWithLocalPoint(target.position(), FIST_LEFT, FIST_FORWARD);
        }
        if (tick != windup) {
            return;
        }
        Vec3 fist = boss.localToWorld(FIST_LEFT, 0.0, FIST_FORWARD);
        double radius = ServerConfig.UNFINISHED.slamRadius.get();
        float damage = (float) boss.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double knockback = boss.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
        AABB box = new AABB(fist.x - radius, fist.y - REACH_DOWN, fist.z - radius, fist.x + radius, fist.y + REACH_UP,
                fist.z + radius);
        for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != boss && e.isAlive())) {
            double dx = hit.getX() - fist.x;
            double dz = hit.getZ() - fist.z;
            if (dx * dx + dz * dz > radius * radius) {
                continue;
            }
            if (hit.hurt(boss.damageSources().mobAttack(boss), damage)) {
                hit.knockback(knockback * 0.5, -dx, -dz); // vanilla halves attack_knockback too
                hit.hurtMarked = true;
            }
        }
        UnfinishedEffects.slamHit(level, fist);
    }
}
