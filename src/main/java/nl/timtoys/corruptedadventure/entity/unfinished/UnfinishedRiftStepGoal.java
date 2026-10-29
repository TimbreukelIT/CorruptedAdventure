package nl.timtoys.corruptedadventure.entity.unfinished;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.config.UnfinishedConfig;

import java.util.OptionalDouble;

/**
 * Verspringen, the memory of The Hollow (its arm): he sinks into his void for {@code riftStepSinkTicks}, comes out
 * {@code riftStepDistance} from his target (behind it first, then fanning round it) on ground he can stand on, and
 * rises. Also how he follows a target that ran far: beyond {@code riftStepFarDistance} he goes at once. Sunken he cannot
 * be hit. No ground near the target: he rises where he sank.
 */
final class UnfinishedRiftStepGoal extends UnfinishedAttackGoal {
    private static final int[] ANGLES = {0, 40, -40, 80, -80, 120, -120, 160, -160};
    private boolean stepped;

    UnfinishedRiftStepGoal(TheUnfinished boss) {
        super(boss, Graft.HOLLE);
    }

    @Override
    protected int cooldown() {
        UnfinishedConfig config = ServerConfig.UNFINISHED;
        return UnfinishedConfig.cooldown(config.riftStepCooldownMin, config.riftStepCooldownMax, roll());
    }

    @Override
    protected int duration() {
        return ServerConfig.UNFINISHED.riftStepSinkTicks.get() * 2;
    }

    @Override
    protected boolean urgent(LivingEntity target) {
        double far = ServerConfig.UNFINISHED.riftStepFarDistance.get();
        return boss.distanceToSqr(target) > far * far;
    }

    @Override
    protected boolean wants(LivingEntity target) {
        // Off cooldown he also steps when his target keeps out of reach of the slam.
        return boss.distanceToSqr(target) > 12.0 * 12.0;
    }

    @Override
    protected void begin(ServerLevel level, LivingEntity target) {
        int sink = ServerConfig.UNFINISHED.riftStepSinkTicks.get();
        stepped = false;
        boss.triggerAnim(UnfinishedAnimations.ACTION, UnfinishedAnimations.RIFT_SINK);
        // Once his head is under, nothing reaches him until he is half out again.
        boss.setSunkenWindow(boss.tickCount + sink / 2, boss.tickCount + sink + sink / 2);
        UnfinishedEffects.riftCrack(level, boss);
    }

    @Override
    protected void act(ServerLevel level, LivingEntity target, int tick) {
        if (tick != ServerConfig.UNFINISHED.riftStepSinkTicks.get()) {
            return;
        }
        stepped = true;
        UnfinishedEffects.riftBurst(level, boss);
        Vec3 spot = find(target);
        if (spot != null) {
            boss.teleportTo(spot.x, spot.y, spot.z);
            boss.snapYaw(TheUnfinished.yawTo(spot, target.position()));
        }
        boss.triggerAnim(UnfinishedAnimations.ACTION, UnfinishedAnimations.RIFT_RISE);
        UnfinishedEffects.riftBurst(level, boss);
    }

    @Override
    protected void end(ServerLevel level) {
        if (!stepped) {
            // Broken off before the step (a stagger, a lost target): the window set ahead must not make him untouchable.
            boss.setSunkenWindow(-1, -1);
        }
    }

    private Vec3 find(LivingEntity target) {
        double distance = ServerConfig.UNFINISHED.riftStepDistance.get();
        float yaw = target.getYHeadRot();
        for (int angle : ANGLES) {
            float rad = (yaw + angle) * Mth.DEG_TO_RAD;
            // The look vector is (-sin, cos); behind is the opposite way.
            double x = target.getX() + Mth.sin(rad) * distance;
            double z = target.getZ() - Mth.cos(rad) * distance;
            OptionalDouble ground = UnfinishedGround.surface(boss.level(), x, z);
            if (ground.isPresent() && boss.level().getWorldBorder().isWithinBounds(x, z)
                    && Math.abs(ground.getAsDouble() - target.getY()) < 16.0) {
                return new Vec3(x, ground.getAsDouble(), z);
            }
        }
        return null;
    }
}
