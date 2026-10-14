package nl.timtoys.corruptedadventure.condition;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import nl.timtoys.corruptedadventure.config.ServerConfig;

/**
 * The top poison threshold: the player withers, and this can kill. Vanilla's wither effect shows the black hearts; the
 * damage is ours: every {@code damageInterval} seconds {@code damage} as {@link Ring4DamageTypes#POISON} (own death
 * message), limited by {@link DifficultyFloor#LETHAL_FROM_NORMAL}. That type bypasses the hurt cooldown
 * ({@code #minecraft:bypasses_cooldown}), so the vanilla poison of the second threshold, ticking just before, never
 * swallows it.
 * <p>
 * <b>Our wither</b> is a short instance ({@link #DURATION_TICKS}, refreshed every second) that is ambient, without
 * particles and without an icon ({@link #isOurs}); its vanilla damage ({@code minecraft:wither}) is cancelled, also
 * right after login before the first condition tick. <b>A foreign wither</b> (wither skeleton, wither rose, a potion)
 * is respected: vanilla merges it into ours and the merged instance loses one of those flags, so from then on it is
 * foreign. We do not refresh over it, its vanilla damage counts (on top of ours: two causes), and {@link #clear} leaves
 * it alone. Ours is short, so a merge keeps at most {@link #DURATION_TICKS} of our duration; once the foreign one has
 * expired, the next tick puts ours back.
 */
final class PoisonWither implements ThresholdEffect {
    /** The threshold effect of the third poison threshold ({@link ConditionDefinitions}). */
    static final PoisonWither WITHER = new PoisonWither();
    /** Two seconds: refreshed every second, and no icon, so vanilla's blink below 200 ticks does not matter. */
    static final int DURATION_TICKS = 40;

    private PoisonWither() {
    }

    static void register(IEventBus gameBus) {
        gameBus.addListener(LivingIncomingDamageEvent.class, PoisonWither::onIncomingDamage);
    }

    @Override
    public void apply(ServerPlayer player) {
        ServerConfig.PoisonCondition config = ServerConfig.POISON;
        MobEffectInstance active = player.getEffect(MobEffects.WITHER);
        if (active == null || isOurs(active)) {
            player.addEffect(ours(config.effect3Amplifier.get()));
        }
        if (DamageThreshold.isDue(player.tickCount, config.damageInterval.get())) {
            float damage = DifficultyFloor.LETHAL_FROM_NORMAL.damage(config.damage.get().floatValue(),
                    player.getHealth(), player.level().getDifficulty());
            if (damage > 0) {
                player.hurt(player.damageSources().source(Ring4DamageTypes.POISON), damage);
            }
        }
    }

    @Override
    public void clear(ServerPlayer player) {
        if (isOurs(player.getEffect(MobEffects.WITHER))) {
            player.removeEffect(MobEffects.WITHER);
        }
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getSource().is(DamageTypes.WITHER)
                && isOurs(player.getEffect(MobEffects.WITHER))) {
            event.setCanceled(true);
        }
    }

    /** Our instance: ambient, no particles, no icon. */
    static MobEffectInstance ours(int amplifier) {
        return new MobEffectInstance(MobEffects.WITHER, DURATION_TICKS, amplifier, true, false, false);
    }

    /**
     * Whether the active wither is ours: ambient, without particles and without an icon. Vanilla merges a foreign
     * wither into ours by taking over its flags, so a merged instance is no longer ours. Also true for a saved
     * instance right after login.
     */
    static boolean isOurs(MobEffectInstance active) {
        return active != null && active.isAmbient() && !active.isVisible() && !active.showIcon();
    }
}
