package nl.timtoys.corruptedadventure.condition;

import net.minecraft.server.level.ServerPlayer;
import nl.timtoys.corruptedadventure.config.ServerConfig;

/**
 * The third silence threshold (package 97): all sound gone (client, stream A), darkness and a heartbeat. The heartbeat
 * is a {@link DamageThreshold} of {@link Ring5DamageTypes#SILENCE} with {@link DifficultyFloor#NEVER_LETHAL}: it hurts
 * down to 1 health on every difficulty and never kills (user, 2026-10-23). It works wherever the meter is (like the
 * void's third threshold), so it stops only once the meter has fallen below it. The darkness goes through
 * {@link ConditionDarkness}, together with the void's.
 */
record SilenceHeartbeat(DamageThreshold heartbeat) implements ThresholdEffect {
    /** The threshold count (cumulative) from which darkness and the heartbeat hold: the third. */
    static final int FROM = 3;
    /** Never lethal, on every difficulty. */
    static final DifficultyFloor FLOOR = DifficultyFloor.NEVER_LETHAL;

    /** The live heartbeat from {@code [conditions.silence]}. */
    static SilenceHeartbeat fromConfig() {
        ServerConfig.SilenceCondition config = ServerConfig.SILENCE;
        return new SilenceHeartbeat(new DamageThreshold(Ring5DamageTypes.SILENCE, config.damage.get().floatValue(),
                config.damageInterval.get(), FLOOR));
    }

    @Override
    public void apply(ServerPlayer player) {
        ConditionDarkness.update(player, ConditionDarkness.wanted(player));
        heartbeat.apply(player);
    }

    @Override
    public void clear(ServerPlayer player) {
        ConditionDarkness.update(player, ConditionDarkness.wanted(player));
    }

    /**
     * The darkness the silence wants now, from the reached count ({@code ConditionMeters.appliedThresholds}, set by
     * {@link ConditionTicker} before any effect runs), or {@link ConditionDarkness#NONE}.
     */
    static int darknessAmplifier(ServerPlayer player) {
        return wanted(PlayerConditions.meters(player).appliedThresholds(Condition.SILENCE))
                ? ServerConfig.SILENCE.darknessAmplifier.get() : ConditionDarkness.NONE;
    }

    static boolean wanted(int reachedThresholds) {
        return reachedThresholds >= FROM;
    }
}
