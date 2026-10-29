package nl.timtoys.corruptedadventure.entity.hollow;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.entity.nightmare.Nightmare;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * How The Hollow's area attacks (the shock wave, the void claws, the sweep) hurt: as its own attack, times its strength,
 * with a throw away from where the blow came from. Without an owner (a {@code /summon}ed wave or claw) it is plain magic
 * damage. Nightmares, creative players and spectators are never hit.
 */
final class HollowStrike {
    private HollowStrike() {
    }

    /** Who the attack can hit inside {@code box}. */
    static List<LivingEntity> victims(ServerLevel level, AABB box) {
        return level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity.isAlive() && !(entity instanceof Nightmare) && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(entity));
    }

    /**
     * Hurts {@code victim} for {@code damage} and throws it away from {@code from}: {@code push} blocks/tick
     * horizontally and {@code lift} up, less by its knockback resistance.
     *
     * @return whether the hit landed
     */
    static boolean hit(ServerLevel level, @Nullable TheHollow owner, LivingEntity victim, float damage, Vec3 from,
                       double push, double lift) {
        DamageSource source = owner != null ? level.damageSources().mobAttack(owner) : level.damageSources().magic();
        float amount = owner != null ? damage * owner.strength() : damage;
        if (!victim.hurt(source, amount)) {
            return false;
        }
        double resist = 1.0 - Math.min(1.0, victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        Vec3 away = new Vec3(victim.getX() - from.x, 0.0, victim.getZ() - from.z);
        away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize();
        victim.push(away.x * push * resist, lift * resist, away.z * push * resist);
        victim.hurtMarked = true;
        return true;
    }
}
