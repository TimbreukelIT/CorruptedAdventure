package nl.timtoys.corruptedadventure.entity.unfinished;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.condition.Condition;
import nl.timtoys.corruptedadventure.condition.PlayerConditions;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.config.UnfinishedConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Naar de void trekken, his own attack (no graft): the void core in his crown swells and every player in the fight within
 * {@code voidPullRadius} is dragged toward the nearest void edge round them ({@link VoidEdge}; none within
 * {@code voidEdgeSearch}: toward him) for {@code voidPullTicks}, while their void meter (the ring 5 condition) rises by
 * {@code voidPullMeterPerSecond}. Sneaking resists the pull, not the meter; light does not help here.
 */
final class UnfinishedVoidPullGoal extends UnfinishedAttackGoal {
    private final Map<UUID, Vec3> edges = new HashMap<>();

    UnfinishedVoidPullGoal(TheUnfinished boss) {
        super(boss, null);
    }

    @Override
    protected int cooldown() {
        UnfinishedConfig config = ServerConfig.UNFINISHED;
        return UnfinishedConfig.cooldown(config.voidPullCooldownMin, config.voidPullCooldownMax, roll());
    }

    @Override
    protected int duration() {
        return ServerConfig.UNFINISHED.voidPullTicks.get();
    }

    @Override
    protected boolean wants(LivingEntity target) {
        return !boss.fighters(ServerConfig.UNFINISHED.voidPullRadius.get()).isEmpty();
    }

    @Override
    protected void begin(ServerLevel level, LivingEntity target) {
        edges.clear();
        int search = ServerConfig.UNFINISHED.voidEdgeSearch.get();
        for (Player player : boss.fighters(ServerConfig.UNFINISHED.voidPullRadius.get())) {
            edges.put(player.getUUID(), VoidEdge.nearest(level, player.position(), search).orElse(boss.position()));
        }
        boss.triggerAnim(UnfinishedAnimations.ACTION, UnfinishedAnimations.VOID_PULL);
        UnfinishedEffects.voidPullStart(level, boss);
    }

    @Override
    protected void act(ServerLevel level, LivingEntity target, int tick) {
        UnfinishedConfig config = ServerConfig.UNFINISHED;
        float meter = (float) (config.voidPullMeterPerSecond.get() / 20.0);
        for (Player player : boss.fighters(config.voidPullRadius.get())) {
            Vec3 edge = edges.get(player.getUUID());
            if (edge == null) {
                continue;
            }
            UnfinishedPull.toward(player, edge, config.voidPullStrength.get());
            if (player instanceof ServerPlayer serverPlayer) {
                PlayerConditions.add(serverPlayer, Condition.VOID, meter);
            }
            if (tick % 4 == 0) {
                UnfinishedEffects.voidPulled(level, player);
            }
        }
    }

    @Override
    protected void end(ServerLevel level) {
        edges.clear();
    }
}
