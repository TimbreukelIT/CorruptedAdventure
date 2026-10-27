package nl.timtoys.corruptedadventure.condition;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.timtoys.corruptedadventure.ring.RingWorld;

import java.util.ArrayList;
import java.util.List;

/**
 * The server-side condition tick: once per second per player (on the player's own tick counter, which spreads the
 * load), only in ring worlds. Per condition: rule → modifiers in order → clamp 0–100 → cumulative thresholds → sync
 * if a rounded value changed. Skips dead players and fake players. In creative and spectator meters only fall and no
 * threshold is active.
 */
public final class ConditionTicker {
    static final int INTERVAL_TICKS = 20;

    private ConditionTicker() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(PlayerTickEvent.Post.class, ConditionTicker::onPlayerTick);
        // Coatings (phase 2) work on every condition, so they sit here and not in a ring's hooks.
        ConditionDefinitions.addModifier(RateModifier.Stage.GEAR, new CoatingModifier());
        // So do the timed effects of consumables (phase 2 golf 2): canteen, cactus juice.
        ConditionDefinitions.addModifier(RateModifier.Stage.GEAR, new RateEffects());
        Ring3Conditions.register(gameBus);
        Ring4Conditions.register(gameBus);
        Ring5Conditions.register(gameBus);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && !(player instanceof FakePlayer)
                && player.tickCount % INTERVAL_TICKS == 0 && player.isAlive() && RingWorld.isActive(player.server)) {
            tick(player);
        }
    }

    private static void tick(ServerPlayer player) {
        ConditionMeters meters = PlayerConditions.meters(player);
        ConditionContext context = ConditionContext.of(player);
        boolean exposed = isExposed(player);
        List<ThresholdEffect> toClear = new ArrayList<>();
        List<ThresholdEffect> toApply = new ArrayList<>();

        for (Condition condition : Condition.values()) {
            meters.add(condition, rate(player, condition, context)); // one tick = one second

            List<Threshold> thresholds = ConditionDefinitions.thresholds(condition);
            ThresholdUpdate update = ThresholdUpdate.of(
                    meters.get(condition), exposed, meters.appliedThresholds(condition), thresholds);
            toClear.addAll(update.clear());
            toApply.addAll(update.apply());
            // Before any effect runs: VoidDarkness reads the reached count of the void.
            meters.setAppliedThresholds(condition, update.reached());
        }

        // All clears before all applies: if two conditions share an effect, one clearing it must not undo the other.
        toClear.forEach(effect -> effect.clear(player));
        toApply.forEach(effect -> effect.apply(player));
        PlayerConditions.syncIfChanged(player, meters);
    }

    /**
     * The rate the next tick applies to this player (rule and all modifiers, points per second), without applying it;
     * also for {@code /ca condition get}.
     */
    public static float rate(ServerPlayer player, Condition condition, ConditionContext context) {
        return rate(condition, context, ConditionDefinitions.rule(condition).ratePerSecond(context),
                ConditionDefinitions.modifiers(), isExposed(player));
    }

    private static boolean isExposed(ServerPlayer player) {
        return !player.isCreative() && !player.isSpectator();
    }

    /** The rule's rate through all modifiers in order; unexposed players (creative, spectator) never rise. */
    static float rate(Condition condition, ConditionContext context, float ruleRate, List<RateModifier> modifiers,
                      boolean exposed) {
        float rate = ruleRate;
        for (RateModifier modifier : modifiers) {
            rate = modifier.modify(condition, context, rate);
        }
        return exposed ? rate : Math.min(rate, 0);
    }

    /**
     * Which threshold effects to apply and clear for one condition. Cumulative: every threshold at or below the meter
     * is reached. Cleared are the thresholds reached last time but not now; after loading ({@code previous} unknown)
     * every unreached threshold, to clean up effects saved with the player.
     */
    record ThresholdUpdate(int reached, List<ThresholdEffect> apply, List<ThresholdEffect> clear) {
        static ThresholdUpdate of(float value, boolean exposed, int previous, List<Threshold> thresholds) {
            int reached = 0;
            if (exposed) {
                while (reached < thresholds.size() && value >= thresholds.get(reached).level()) {
                    reached++;
                }
            }
            int clearTo = previous == ConditionMeters.UNKNOWN ? thresholds.size() : Math.min(previous, thresholds.size());
            List<ThresholdEffect> apply = new ArrayList<>();
            List<ThresholdEffect> clear = new ArrayList<>();
            for (int i = 0; i < reached; i++) {
                apply.add(thresholds.get(i).effect());
            }
            for (int i = reached; i < clearTo; i++) {
                clear.add(thresholds.get(i).effect());
            }
            return new ThresholdUpdate(reached, apply, clear);
        }
    }
}
