package nl.timtoys.corruptedadventure.condition;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;

/**
 * Damage every {@code intervalSeconds} seconds while the threshold is reached, limited by a {@link DifficultyFloor}.
 * The interval counts on the player's own tick counter, which the handler only checks on whole seconds.
 */
public record DamageThreshold(ResourceKey<DamageType> damageType, float amount, int intervalSeconds,
                              DifficultyFloor floor) implements ThresholdEffect {
    @Override
    public void apply(ServerPlayer player) {
        if (!isDue(player.tickCount, intervalSeconds)) {
            return;
        }
        float damage = floor.damage(amount, player.getHealth(), player.level().getDifficulty());
        if (damage > 0) {
            player.hurt(player.damageSources().source(damageType), damage);
        }
    }

    @Override
    public void clear(ServerPlayer player) {
    }

    static boolean isDue(int tickCount, int intervalSeconds) {
        return intervalSeconds <= 1 || (tickCount / 20) % intervalSeconds == 0;
    }
}
