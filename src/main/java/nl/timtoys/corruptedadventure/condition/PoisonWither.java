package nl.timtoys.corruptedadventure.condition;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import nl.timtoys.corruptedadventure.config.ServerConfig;

/**
 * The top poison threshold: the player withers, and this can kill. Vanilla's wither effect (ambient, refreshed every
 * second like a {@link MobEffectThreshold}) shows the black hearts on the player's own client; the damage is ours:
 * every {@code damageInterval} seconds {@code damage} as {@link Ring4DamageTypes#POISON} (own death message, settable).
 * The wither effect's own damage ({@code minecraft:wither}) is cancelled while our ambient instance is the active one,
 * so it does not hit twice. A stronger wither from elsewhere (a wither rose) hides ours; then it is not ambient and
 * deals its vanilla damage as usual.
 */
final class PoisonWither implements ThresholdEffect {
    /** The threshold effect of the third poison threshold ({@link ConditionDefinitions}). */
    static final PoisonWither WITHER = new PoisonWither();
    /** How many poison thresholds are reached when this one is: it is the third. */
    static final int THRESHOLD_COUNT = 3;

    private PoisonWither() {
    }

    static void register(IEventBus gameBus) {
        gameBus.addListener(LivingIncomingDamageEvent.class, PoisonWither::onIncomingDamage);
    }

    @Override
    public void apply(ServerPlayer player) {
        ServerConfig.PoisonCondition config = ServerConfig.POISON;
        new MobEffectThreshold(MobEffects.WITHER, config.effect3Amplifier.get()).apply(player);
        float damage = config.damage.get().floatValue();
        if (damage > 0 && DamageThreshold.isDue(player.tickCount, config.damageInterval.get())) {
            player.hurt(player.damageSources().source(Ring4DamageTypes.POISON), damage);
        }
    }

    @Override
    public void clear(ServerPlayer player) {
        new MobEffectThreshold(MobEffects.WITHER, 0).clear(player);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getSource().is(DamageTypes.WITHER)
                && isOurs(player.getEffect(MobEffects.WITHER), isReached(player))) {
            event.setCanceled(true);
        }
    }

    /** The active wither instance is the one this threshold keeps up: ambient, while the threshold is reached. */
    static boolean isOurs(MobEffectInstance active, boolean reached) {
        return reached && active != null && active.isAmbient();
    }

    /** The wither threshold was reached on the last condition tick. */
    private static boolean isReached(ServerPlayer player) {
        return PlayerConditions.meters(player).appliedThresholds(Condition.POISON) >= THRESHOLD_COUNT;
    }
}
