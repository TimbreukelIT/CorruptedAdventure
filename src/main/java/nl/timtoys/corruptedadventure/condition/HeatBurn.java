package nl.timtoys.corruptedadventure.condition;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The top heat threshold: the player burns. Every second the threshold deals {@link #DAMAGE} as
 * {@link Ring3DamageTypes#HEAT} (a fire type: fire resistance blocks it, fire protection reduces it; own death
 * message), and every tick a short flame keeps the player visibly on fire. Water, rain, bubble columns and powder
 * snow put it out, as they do vanilla fire.
 * <p>
 * The flame is kept between 1 and {@link #FLAME_TICKS} remaining fire ticks, so vanilla's own fire damage (dealt when
 * the remaining ticks are a multiple of 20) never fires from it; real fire (lava, a fire block, Fire Aspect) sets far
 * more ticks and then burns, and is credited, the vanilla way. Below the threshold the flame dies within half a second.
 */
public final class HeatBurn implements ThresholdEffect {
    /** The threshold effect of the fourth heat threshold ({@link ConditionDefinitions}). */
    public static final HeatBurn IGNITE = new HeatBurn();
    /** How many heat thresholds are reached when this one is: it is the fourth. */
    static final int THRESHOLD_COUNT = 4;
    /** Half a heart per second, like vanilla burning. */
    static final float DAMAGE = 1.0F;
    /** Below 20, so vanilla's fire damage never triggers from the flame alone; refreshed every tick. */
    static final int FLAME_TICKS = 10;

    private HeatBurn() {
    }

    static void register(IEventBus gameBus) {
        gameBus.addListener(PlayerTickEvent.Pre.class, HeatBurn::onPlayerTick);
    }

    @Override
    public void apply(ServerPlayer player) {
        if (!isPutOut(player)) {
            player.hurt(player.damageSources().source(Ring3DamageTypes.HEAT), DAMAGE);
        }
    }

    @Override
    public void clear(ServerPlayer player) {
    }

    private static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player && isReached(player) && !isPutOut(player)) {
            player.igniteForTicks(FLAME_TICKS);
        }
    }

    /** The burn threshold was reached on the last condition tick. */
    private static boolean isReached(ServerPlayer player) {
        return PlayerConditions.meters(player).appliedThresholds(Condition.HEAT) >= THRESHOLD_COUNT;
    }

    /** Where vanilla puts fire out. */
    private static boolean isPutOut(ServerPlayer player) {
        return player.isInWaterRainOrBubble() || player.isInPowderSnow;
    }
}
