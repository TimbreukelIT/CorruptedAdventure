package nl.timtoys.corruptedadventure.entity.layered;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import nl.timtoys.corruptedadventure.config.LayeredConfig;
import nl.timtoys.corruptedadventure.entity.nightmare.Nightmare;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * The rock fall (design: "steenslag en los zand"): from a wall it hammers the stone with its front legs and breaks rock
 * and loose sand loose above its target ({@link LayeredRockfall}). On the ground with a wall within
 * {@code wallSearchRadius} it first climbs {@code perchHeight} blocks up that wall. Holds still on the wall while it
 * hammers ({@link TheLayered#requestCling}). Only with {@code rockfallEnabled}, while its lifetime budget
 * ({@code rockfallBlocksPerFight}) lasts, once per {@code rockfallCooldownMin..Max} seconds (the first half a cooldown
 * after it first could), at a fighter within {@link #RANGE} blocks.
 */
final class LayeredRockfallGoal extends Goal {
    private static final double RANGE = 20.0;
    private static final int CLIMB_TIMEOUT_TICKS = 120;
    private static final int HOLD_TICKS = 16;

    private enum Phase { CLIMB, HAMMER, HOLD }

    private final TheLayered layered;
    private int nextTick = -1;
    private Phase phase = Phase.CLIMB;
    private int ticks;
    private boolean done;
    private double startY;
    @Nullable
    private Direction wall;

    LayeredRockfallGoal(TheLayered layered) {
        this.layered = layered;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        LayeredConfig settings = TheLayered.settings();
        if (!settings.rockfallEnabled.get() || !layered.canAct() || layered.isVehicle() || layered.rockfallLeft() <= 0
                || !(layered.getTarget() instanceof Player player) || !Nightmare.isFighter(player)
                || layered.distanceToSqr(player) > RANGE * RANGE) {
            return false;
        }
        if (nextTick < 0) {
            nextTick = layered.tickCount + cooldown() / 2;
            return false;
        }
        if (layered.tickCount < nextTick) {
            return false;
        }
        wall = layered.isOnWall() ? layered.getSurface() : layered.findWall(settings.wallSearchRadius.get());
        return wall != null;
    }

    @Override
    public boolean canContinueToUse() {
        return !done && layered.canAct() && !layered.isVehicle() && layered.getTarget() != null;
    }

    @Override
    public boolean isInterruptable() {
        return false;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        done = false;
        ticks = 0;
        startY = layered.getY();
        layered.getNavigation().stop();
        if (layered.isOnWall()) {
            startHammer();
        } else {
            phase = Phase.CLIMB;
        }
    }

    @Override
    public void stop() {
        layered.requestCling(false);
        nextTick = layered.tickCount + cooldown();
        wall = null;
    }

    @Override
    public void tick() {
        LivingEntity target = layered.getTarget();
        if (target == null) {
            done = true;
            return;
        }
        switch (phase) {
            case CLIMB -> climb();
            case HAMMER -> {
                layered.getLookControl().setLookAt(target, 30.0F, 30.0F);
                if (--ticks <= 0) {
                    int dropped = LayeredRockfall.drop((ServerLevel) layered.level(), layered, target,
                            Math.min(TheLayered.settings().rockfallBlocksPerAttack.get(), layered.rockfallLeft()));
                    layered.useRockfall(dropped);
                    phase = Phase.HOLD;
                    ticks = HOLD_TICKS;
                }
            }
            case HOLD -> {
                if (--ticks <= 0) {
                    done = true;
                }
            }
        }
    }

    private void climb() {
        ticks++;
        Direction toward = layered.isOnWall() ? layered.getSurface() : wall;
        layered.getMoveControl().setWantedPosition(layered.getX() + toward.getStepX() * 3.0, layered.getY() + 3.0,
                layered.getZ() + toward.getStepZ() * 3.0, 1.0);
        boolean high = layered.getY() - startY >= TheLayered.settings().perchHeight.get();
        if (layered.isOnWall() && high) {
            startHammer();
        } else if (ticks >= CLIMB_TIMEOUT_TICKS) {
            // Did not get up there (no climbable wall after all): hammer from where it is if it clings, else give up.
            if (layered.isOnWall()) {
                startHammer();
            } else {
                done = true;
            }
        }
    }

    private void startHammer() {
        phase = Phase.HAMMER;
        ticks = TheLayered.settings().rockfallWindupTicks.get();
        layered.getNavigation().stop();
        layered.requestCling(true);
        layered.triggerAnim(LayeredAnimations.ACTION, LayeredAnimations.ROCKFALL);
        LayeredEffects.rockfallWindup((ServerLevel) layered.level(), layered);
    }

    private int cooldown() {
        return TheLayered.settings().rockfallCooldownTicks(layered.getRandom().nextInt(1 << 16));
    }
}
