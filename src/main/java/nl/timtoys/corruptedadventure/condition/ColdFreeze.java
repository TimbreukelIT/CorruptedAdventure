package nl.timtoys.corruptedadventure.condition;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.timtoys.corruptedadventure.config.ServerConfig;

/**
 * The top cold threshold: the player freezes. Every tick, after vanilla's {@code aiStep}, the player's
 * {@code ticksFrozen} is set to fully frozen, so vanilla shows its frost overlay, frozen hearts and shaking (on every
 * client: the value is synced entity data) and slows the player like powder snow. Every {@code damageInterval} seconds
 * the threshold deals {@code damage} as {@link Ring3DamageTypes#COLD} (own death message). A burning player neither
 * freezes nor takes cold damage. Not a {@link DamageThreshold}: that one cannot skip a burning player.
 * <p>
 * Why not vanilla's own freezing: vanilla thaws {@code ticksFrozen} by 2 every tick outside powder snow, and deals its
 * damage only while {@code canFreeze()}, which a single leather piece makes false; leather should slow the cold, not
 * make immune. Because {@code aiStep} thaws before it checks, vanilla's freeze damage never fires from this alone
 * (only in real powder snow, the vanilla way). Below the threshold vanilla thaws the overlay within 3.5 s; fire thaws
 * it at once (vanilla), so a burning player is not kept frozen.
 * <p>
 * Known cost (review 55, kept on purpose): {@code aiStep} lowers {@code ticksFrozen} by 2 and this sets it back every
 * tick, so the frozen player's entity data is sent every tick (one int, to the player and its trackers), as vanilla
 * does for the air supply of every player under water. Avoiding it needs a mixin into {@code aiStep} plus suppressing
 * vanilla's own freeze damage at 140.
 */
final class ColdFreeze implements ThresholdEffect {
    /** The threshold effect of the third cold threshold ({@link ConditionDefinitions}). */
    static final ColdFreeze FREEZE = new ColdFreeze();
    /** How many cold thresholds are reached when this one is: it is the third. */
    static final int THRESHOLD_COUNT = 3;

    private ColdFreeze() {
    }

    static void register(IEventBus gameBus) {
        gameBus.addListener(PlayerTickEvent.Post.class, ColdFreeze::onPlayerTick);
    }

    @Override
    public void apply(ServerPlayer player) {
        ServerConfig.ColdCondition config = ServerConfig.COLD;
        float damage = config.damage.get().floatValue();
        if (damage > 0 && !player.isOnFire() && DamageThreshold.isDue(player.tickCount, config.damageInterval.get())) {
            player.hurt(player.damageSources().source(Ring3DamageTypes.COLD), damage);
        }
    }

    @Override
    public void clear(ServerPlayer player) {
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.isAlive() && !player.isOnFire()
                && isReached(player)) {
            player.setTicksFrozen(frozenTicks(player.getTicksFrozen(), player.getTicksRequiredToFreeze()));
        }
    }

    /** Fully frozen, but never lower than a value vanilla set (powder snow keeps its own count). */
    static int frozenTicks(int current, int required) {
        return Math.max(current, required);
    }

    /** The freeze threshold was reached on the last condition tick. */
    private static boolean isReached(ServerPlayer player) {
        return PlayerConditions.meters(player).appliedThresholds(Condition.COLD) >= THRESHOLD_COUNT;
    }
}
