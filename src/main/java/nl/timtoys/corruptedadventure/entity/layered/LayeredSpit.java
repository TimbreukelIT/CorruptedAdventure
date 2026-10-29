package nl.timtoys.corruptedadventure.entity.layered;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.LayeredConfig;
import nl.timtoys.corruptedadventure.entity.nightmare.Nightmare;

import java.util.List;

/**
 * One cloud of dust and bone splinters spat by a dead head ({@link LayeredSpitGoal}): it flies in a straight line at
 * {@code spitSpeed} for at most {@code spitRange} + 2 blocks and bursts on the first living thing within
 * {@code spitRadius} (not a Nightmare or a held player: {@code spitDamage} times its strength and {@code spitBlindSeconds}
 * of blindness) or on a block. No entity of its own: The Layered keeps its clouds and moves them every tick
 * ({@link TheLayered#tick}); they are not saved. Server-side.
 */
public final class LayeredSpit {
    private static final double EXTRA_RANGE = 2.0;

    private Vec3 position;
    private final Vec3 step;
    private double left;

    public LayeredSpit(Vec3 from, Vec3 toward) {
        LayeredConfig settings = TheLayered.settings();
        Vec3 direction = toward.subtract(from);
        this.position = from;
        this.step = direction.lengthSqr() > 1.0E-6 ? direction.normalize().scale(settings.spitSpeed.get()) : Vec3.ZERO;
        this.left = settings.spitRange.get() + EXTRA_RANGE;
    }

    public Vec3 position() {
        return position;
    }

    /** Moves the cloud one tick; false once it has burst or flown its range. */
    public boolean tick(ServerLevel level, TheLayered layered) {
        if (left <= 0.0 || step == Vec3.ZERO) {
            return false;
        }
        LayeredConfig settings = TheLayered.settings();
        double radius = settings.spitRadius.get();
        Vec3 next = position.add(step);
        BlockHitResult wall = level.clip(new ClipContext(position, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                layered));
        Vec3 end = wall.getType() == HitResult.Type.MISS ? next : wall.getLocation();
        LivingEntity victim = firstHit(level, layered, position, end, radius);
        if (victim != null) {
            float damage = settings.spitDamage.get().floatValue() * layered.strength();
            if (victim.hurt(layered.damageSources().mobAttack(layered), damage)) {
                int blind = settings.spitBlindSeconds.get() * 20;
                if (blind > 0) {
                    victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, blind), layered);
                }
            }
            LayeredEffects.spitBurst(level, victim.getEyePosition());
            return false;
        }
        LayeredEffects.spitCloud(level, end, radius);
        if (wall.getType() != HitResult.Type.MISS) {
            LayeredEffects.spitBurst(level, end);
            return false;
        }
        left -= step.length();
        position = next;
        return true;
    }

    private static LivingEntity firstHit(ServerLevel level, TheLayered layered, Vec3 from, Vec3 to, double radius) {
        AABB sweep = new AABB(from, to).inflate(radius);
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, sweep,
                other -> !(other instanceof Nightmare) && other.isAlive() && !layered.hasPassenger(other)
                        && !other.isSpectator());
        LivingEntity first = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity other : near) {
            AABB box = other.getBoundingBox().inflate(radius);
            Vec3 hit = box.contains(from) ? from : box.clip(from, to).orElse(null);
            if (hit != null && hit.distanceToSqr(from) < best) {
                best = hit.distanceToSqr(from);
                first = other;
            }
        }
        return first;
    }
}
