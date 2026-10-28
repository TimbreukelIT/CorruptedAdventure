package nl.timtoys.corruptedadventure.entity.unfinished;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.config.UnfinishedConfig;
import nl.timtoys.corruptedadventure.entity.nightmare.Nightmare;
import org.jetbrains.annotations.Nullable;

/**
 * Grijpen en meeklimmen, the memory of The Layered (its legs): he turns his grafted legs toward a player, the front leg
 * swings out and the claw closes after {@code grabReachTicks} on whoever is still near the claw. The held player is
 * dragged along with the claw as it climbs to {@code grabLift} blocks above his feet, then let go: the fall is the
 * damage. Hitting him hard enough during the hold ({@code grabBreakDamage}, from anyone) breaks the grip early, and so
 * does losing the legs. Only players in the fight are grabbed.
 */
final class UnfinishedGrabGoal extends UnfinishedAttackGoal {
    /** The claw on the rest pose of the grab: front-left of him. */
    private static final double CLAW_LEFT = 8.0;
    private static final double CLAW_FORWARD = 6.0;
    private static final double CLAW_START_UP = 3.0;
    /** Blocks from the closing claw within which the player is caught: the leg sweeps a wide arc. */
    private static final double CATCH_RADIUS = 8.0;
    /** Share of the gap to the claw closed per tick: the player is dragged, not teleported. */
    private static final double DRAG = 0.45;

    @Nullable
    private Player held;
    private float damageMark;

    UnfinishedGrabGoal(TheUnfinished boss) {
        super(boss, Graft.GELAAGDE);
    }

    @Override
    protected int cooldown() {
        UnfinishedConfig config = ServerConfig.UNFINISHED;
        return UnfinishedConfig.cooldown(config.grabCooldownMin, config.grabCooldownMax, roll());
    }

    @Override
    protected int duration() {
        UnfinishedConfig config = ServerConfig.UNFINISHED;
        return config.grabReachTicks.get() + config.grabHoldTicks.get() + UnfinishedAnimations.GRAB_RECOVER_TICKS;
    }

    @Override
    protected boolean wants(LivingEntity target) {
        double range = ServerConfig.UNFINISHED.grabRange.get();
        return target instanceof Player player && Nightmare.isFighter(player) && boss.distanceToSqr(target) <= range * range;
    }

    @Override
    protected void begin(ServerLevel level, LivingEntity target) {
        held = null;
        boss.faceWithLocalPoint(target.position(), CLAW_LEFT, CLAW_FORWARD);
        boss.triggerAnim(UnfinishedAnimations.ACTION, UnfinishedAnimations.GRAB);
        UnfinishedEffects.grabReach(level, boss);
    }

    @Override
    protected void act(ServerLevel level, LivingEntity target, int tick) {
        UnfinishedConfig config = ServerConfig.UNFINISHED;
        int reach = config.grabReachTicks.get();
        int hold = config.grabHoldTicks.get();
        if (tick < reach) {
            boss.faceWithLocalPoint(target.position(), CLAW_LEFT, CLAW_FORWARD);
            return;
        }
        if (tick == reach) {
            Vec3 claw = boss.localToWorld(CLAW_LEFT, CLAW_START_UP, CLAW_FORWARD);
            if (target instanceof Player player && Nightmare.isFighter(player)
                    && player.position().distanceToSqr(claw) <= CATCH_RADIUS * CATCH_RADIUS) {
                held = player;
                damageMark = boss.damageTaken();
                UnfinishedEffects.grabbed(level, player);
            }
            return;
        }
        if (held == null) {
            return;
        }
        boolean broken = !Nightmare.isFighter(held) || held.level() != boss.level()
                || boss.damageTaken() - damageMark >= config.grabBreakDamage.get();
        if (broken) {
            boss.triggerAnim(UnfinishedAnimations.ACTION, UnfinishedAnimations.FLINCH);
            release(level);
            return;
        }
        if (tick >= reach + hold) {
            release(level);
            return;
        }
        double progress = (tick - reach) / (double) hold;
        double up = CLAW_START_UP + (config.grabLift.get() - CLAW_START_UP) * Math.min(1.0, progress * 1.6);
        Vec3 claw = boss.localToWorld(CLAW_LEFT, up, CLAW_FORWARD);
        held.setDeltaMovement(claw.subtract(held.position()).scale(DRAG));
        held.hurtMarked = true;
        // Only the fall after the release counts.
        held.resetFallDistance();
    }

    @Override
    protected void end(ServerLevel level) {
        release(level);
    }

    private void release(ServerLevel level) {
        if (held != null) {
            held.setDeltaMovement(Vec3.ZERO);
            held.hurtMarked = true;
            UnfinishedEffects.released(level, held);
            held = null;
        }
    }
}
