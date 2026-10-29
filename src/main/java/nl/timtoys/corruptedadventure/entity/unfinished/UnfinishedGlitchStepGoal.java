package nl.timtoys.corruptedadventure.entity.unfinished;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.config.UnfinishedConfig;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalDouble;

/**
 * Glitchsprong, his own: he freezes and jerks ({@code glitchFreezeTicks}, the warning), then stutters toward his target
 * frame by frame, a lag spike made flesh: {@code glitchStepsMin}–{@code Max} jumps of a few blocks, each a little
 * sideways and after an uneven pause, never sliding (the client snaps, {@link TheUnfinished#isGlitching}). Before each
 * jump a pillar of glitch pixels stands where he will be, so the run reads; the last one lands
 * {@code glitchStrikeDistance} from the target with a frame blow ({@code glitchStrikeRadius}) in front of him, marked by
 * a ring on the ground. No ground ahead: the run ends where he is.
 */
final class UnfinishedGlitchStepGoal extends UnfinishedAttackGoal {
    /** Most degrees a step turns off the straight line to the target. */
    private static final float SIDESTEP = 55.0F;
    /** Most blocks a step may climb or drop. */
    private static final double MAX_RISE = 8.0;
    private int stepsLeft;
    private int nextStep;
    private boolean done;
    @Nullable
    private Vec3 spot;
    private boolean last;
    /** Where the frame blow lands, fixed when the last spot is marked: stepping out of the ring is the dodge. */
    private Vec3 strikeAt = Vec3.ZERO;

    UnfinishedGlitchStepGoal(TheUnfinished boss) {
        super(boss, null);
    }

    @Override
    protected int cooldown() {
        UnfinishedConfig config = ServerConfig.UNFINISHED;
        return UnfinishedConfig.cooldown(config.glitchCooldownMin, config.glitchCooldownMax, roll());
    }

    @Override
    protected int duration() {
        UnfinishedConfig config = ServerConfig.UNFINISHED;
        return config.glitchFreezeTicks.get() + config.glitchStepsMax.get() * (config.glitchPauseMax.get() + 1) + 1;
    }

    @Override
    protected boolean finished() {
        return done;
    }

    @Override
    protected boolean wants(LivingEntity target) {
        UnfinishedConfig config = ServerConfig.UNFINISHED;
        double distance = horizontal(boss.position(), target.position());
        return distance >= config.glitchMinRange.get() && distance <= config.glitchMaxRange.get();
    }

    @Override
    protected void begin(ServerLevel level, LivingEntity target) {
        UnfinishedConfig config = ServerConfig.UNFINISHED;
        RandomSource random = boss.getRandom();
        int min = config.glitchStepsMin.get();
        stepsLeft = min + random.nextInt(Math.max(1, config.glitchStepsMax.get() - min + 1));
        nextStep = config.glitchFreezeTicks.get();
        done = false;
        spot = null;
        last = false;
        boss.setGlitching(true);
        UnfinishedEffects.glitchFreeze(level, boss);
    }

    @Override
    protected void act(ServerLevel level, LivingEntity target, int tick) {
        if (done) {
            return;
        }
        int telegraph = ServerConfig.UNFINISHED.glitchTelegraphTicks.get();
        if (spot == null && tick >= nextStep - telegraph) {
            spot = plan(target);
            if (spot == null) {
                finish();
                return;
            }
            if (last) {
                strikeAt = strikeCentre(spot, target.position());
            }
        }
        if (spot != null && tick < nextStep && (nextStep - tick) % 2 == 0) {
            UnfinishedEffects.glitchMark(level, spot, last ? strikeAt : null,
                    ServerConfig.UNFINISHED.glitchStrikeRadius.get());
        }
        if (tick < nextStep || spot == null) {
            return;
        }
        Vec3 from = boss.position();
        boss.teleportTo(spot.x, spot.y, spot.z);
        boss.snapYaw(TheUnfinished.yawTo(spot, target.position()));
        UnfinishedEffects.glitchJump(level, from, spot);
        stepsLeft--;
        if (last) {
            strike(level, strikeAt);
            finish();
            return;
        }
        spot = null;
        UnfinishedConfig config = ServerConfig.UNFINISHED;
        int pauseMin = config.glitchPauseMin.get();
        nextStep = tick + pauseMin + boss.getRandom().nextInt(Math.max(1, config.glitchPauseMax.get() - pauseMin + 1));
    }

    @Override
    protected void end(ServerLevel level) {
        boss.setGlitching(false);
        done = false;
        spot = null;
    }

    /** Over: the base ends it next tick, and the gap after an attack keeps the next one from following at once. */
    private void finish() {
        done = true;
    }

    /** The next spot: a step toward the target, a little sideways; the last one before it. Null: no ground. */
    @Nullable
    private Vec3 plan(LivingEntity target) {
        UnfinishedConfig config = ServerConfig.UNFINISHED;
        RandomSource random = boss.getRandom();
        Vec3 at = boss.position();
        Vec3 goal = target.position();
        double strike = config.glitchStrikeDistance.get();
        double left = horizontal(at, goal) - strike;
        double stepMax = config.glitchStepMax.get();
        double stepMin = Math.min(config.glitchStepMin.get(), stepMax);
        float toward = TheUnfinished.yawTo(at, goal);
        if (stepsLeft <= 1 || left <= stepMax) {
            last = true;
            // He comes out on his own side of the target, a little off the line.
            float back = TheUnfinished.yawTo(goal, at) + (random.nextFloat() - 0.5F) * 40.0F;
            Vec3 end = ground(goal.add(look(back).scale(strike)), at.y);
            return end != null ? end : ground(goal.add(look(TheUnfinished.yawTo(goal, at)).scale(strike)), at.y);
        }
        double length = stepMin + random.nextDouble() * (stepMax - stepMin);
        float yaw = toward + (random.nextFloat() * 2.0F - 1.0F) * SIDESTEP;
        Vec3 next = ground(at.add(look(yaw).scale(length)), at.y);
        return next != null ? next : ground(at.add(look(toward).scale(length)), at.y);
    }

    @Nullable
    private Vec3 ground(Vec3 at, double fromY) {
        OptionalDouble surface = UnfinishedGround.surface(boss.level(), at.x, at.z);
        if (surface.isEmpty() || Math.abs(surface.getAsDouble() - fromY) > MAX_RISE
                || !boss.level().getWorldBorder().isWithinBounds(at.x, at.z)) {
            return null;
        }
        return new Vec3(at.x, surface.getAsDouble(), at.z);
    }

    /** Where the frame blow lands: between him and the target, within reach of both. */
    private static Vec3 strikeCentre(Vec3 at, Vec3 target) {
        return at.add(target.subtract(at).multiply(1.0, 0.0, 1.0).scale(0.8));
    }

    private void strike(ServerLevel level, Vec3 centre) {
        UnfinishedConfig config = ServerConfig.UNFINISHED;
        double radius = config.glitchStrikeRadius.get();
        double base = boss.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
        float damage = config.glitchStrikeDamage.get().floatValue()
                * (float) (base > 0.0 ? boss.getAttributeValue(Attributes.ATTACK_DAMAGE) / base : 1.0);
        UnfinishedEffects.glitchStrike(level, centre);
        if (damage <= 0.0F || radius <= 0.0) {
            return;
        }
        AABB box = new AABB(centre, centre).inflate(radius, 4.0, radius);
        for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != boss && e.isAlive())) {
            double dx = hit.getX() - centre.x;
            double dz = hit.getZ() - centre.z;
            if (dx * dx + dz * dz > radius * radius) {
                continue;
            }
            if (hit.hurt(boss.damageSources().mobAttack(boss), damage)) {
                hit.knockback(0.6, -dx, -dz);
                hit.hurtMarked = true;
            }
        }
    }

    /** Vanilla's look vector for a yaw, flat. */
    private static Vec3 look(float yaw) {
        float rad = yaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(rad), 0.0, Mth.cos(rad));
    }

    private static double horizontal(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
