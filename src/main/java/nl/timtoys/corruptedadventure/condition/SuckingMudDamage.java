package nl.timtoys.corruptedadventure.condition;

import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;

/**
 * The damage of the sucking mud (ring 4, {@code docs/plans/ring-4/golf-2.md} G2–G3), owned by stream C and called by
 * the block (stream B, package 72) while an entity's head is under the mud. Deals {@link Ring4DamageTypes#SUCKING_MUD}
 * (own death message), limited like the poison by {@link DifficultyFloor#LETHAL_FROM_NORMAL}: none on peaceful, down
 * to 10 health on easy, lethal on normal and hard. That floor holds for players and mobs alike (decision V1 = B), so
 * on peaceful and easy no mob dies in the mud either. Server-side only.
 */
public final class SuckingMudDamage {
    private SuckingMudDamage() {
    }

    /**
     * Hurts {@code entity} by {@code amount} as sucking mud, after the difficulty floor. The caller decides when (head
     * under the mud) and how much. Does nothing on the client.
     *
     * @return whether damage was dealt (false on the client, when the floor leaves nothing, or when the entity is
     * invulnerable or inside its hurt cooldown)
     */
    public static boolean hurt(LivingEntity entity, float amount) {
        if (entity.level().isClientSide()) {
            return false;
        }
        float damage = damage(amount, entity.getHealth(), entity.level().getDifficulty());
        return damage > 0 && entity.hurt(entity.damageSources().source(Ring4DamageTypes.SUCKING_MUD), damage);
    }

    /** The damage to deal after {@link DifficultyFloor#LETHAL_FROM_NORMAL}; 0 = none. */
    static float damage(float amount, float health, Difficulty difficulty) {
        return amount <= 0 ? 0 : DifficultyFloor.LETHAL_FROM_NORMAL.damage(amount, health, difficulty);
    }
}
