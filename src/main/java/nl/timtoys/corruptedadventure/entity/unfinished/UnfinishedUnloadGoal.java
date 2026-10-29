package nl.timtoys.corruptedadventure.entity.unfinished;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.config.UnfinishedConfig;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ontladen grond, his own: he presses his void into the ground under his target. For {@code unloadWarnTicks} the patch's
 * top blocks flicker to glitch blocks and back (the warning, and the sound of it), then it unloads downward,
 * {@code unloadLayersPerTick} layers a tick, to the bottom of the world: void under your feet. After
 * {@code unloadOpenTicks} it loads back in exactly as it was, bottom first ({@link UnfinishedTempBlocks}); who fell in
 * is caught by the void ({@link UnfinishedUnload#catchFallen}). Broken off during the warning, the patch settles back at
 * once; once it unloads, it runs its course. Off with {@code unloadEnabled}.
 */
final class UnfinishedUnloadGoal extends UnfinishedAttackGoal {
    /** He never unloads under his own feet: columns this close to him stay. */
    private static final double OWN_CLEARANCE = 5.0;
    /** Extra ticks a flickering glitch block is booked for, should nothing else put it back. */
    private static final int FLICKER_SAFETY = 200;

    private final List<UnfinishedUnload.Column> columns = new ArrayList<>();
    private final Set<BlockPos> flickering = new HashSet<>();
    private BlockPos centre = BlockPos.ZERO;
    @Nullable
    private UnfinishedUnload.Dig dig;
    private boolean done;

    UnfinishedUnloadGoal(TheUnfinished boss) {
        super(boss, null);
    }

    @Override
    protected int cooldown() {
        UnfinishedConfig config = ServerConfig.UNFINISHED;
        return UnfinishedConfig.cooldown(config.unloadCooldownMin, config.unloadCooldownMax, roll());
    }

    @Override
    protected int duration() {
        UnfinishedConfig config = ServerConfig.UNFINISHED;
        int height = boss.level().getHeight();
        return config.unloadWarnTicks.get() + height / config.unloadLayersPerTick.get() + 2;
    }

    @Override
    protected boolean finished() {
        return done;
    }

    @Override
    protected boolean wants(LivingEntity target) {
        UnfinishedConfig config = ServerConfig.UNFINISHED;
        double range = config.unloadRange.get();
        double clear = config.unloadRadius.get() + OWN_CLEARANCE;
        double distance = boss.distanceToSqr(target.getX(), boss.getY(), target.getZ());
        return config.unloadEnabled.get() && target.onGround() && distance <= range * range && distance > clear * clear;
    }

    @Override
    protected void begin(ServerLevel level, LivingEntity target) {
        UnfinishedConfig config = ServerConfig.UNFINISHED;
        done = false;
        flickering.clear();
        centre = target.blockPosition();
        dig = null;
        columns.clear();
        columns.addAll(UnfinishedUnload.plan(level, centre, config.unloadRadius.get(), config.unloadMaxBlocks.get(),
                boss.position(), OWN_CLEARANCE));
        if (columns.isEmpty()) {
            done = true;
            return;
        }
        boss.faceToward(Vec3.atCenterOf(centre));
        boss.triggerAnim(UnfinishedAnimations.ACTION, UnfinishedAnimations.UNLOAD);
        UnfinishedEffects.unloadWarn(level, boss, Vec3.atBottomCenterOf(centre.above()));
    }

    @Override
    protected void act(ServerLevel level, LivingEntity target, int tick) {
        if (done) {
            return;
        }
        UnfinishedConfig config = ServerConfig.UNFINISHED;
        int warn = config.unloadWarnTicks.get();
        if (tick < warn) {
            if (tick % 2 == 0) {
                flicker(level, tick, warn);
            }
            return;
        }
        if (tick == warn) {
            settleFlicker(level);
            long openUntil = level.getGameTime() + config.unloadOpenTicks.get();
            int surface = columns.stream().mapToInt(UnfinishedUnload.Column::top).max().orElse(centre.getY()) + 1;
            UnfinishedTempBlocks.of(level).addHole(new UnfinishedTempBlocks.Hole(centre, config.unloadRadius.get(), surface,
                    openUntil + 20));
            UnfinishedEffects.unloadOpen(level, Vec3.atBottomCenterOf(centre.above()), config.unloadRadius.get());
            dig = new UnfinishedUnload.Dig(columns, openUntil);
        }
        if (dig != null && dig.step(level, config.unloadLayersPerTick.get())) {
            dig = null;
            done = true;
        }
    }

    @Override
    protected void end(ServerLevel level) {
        settleFlicker(level);
        if (dig != null) {
            // Broken off while unloading (a stagger, a lost target): the hole is open already, so it runs its course.
            UnfinishedTempBlocks.of(level).addDig(dig);
            dig = null;
        }
        columns.clear();
        done = false;
    }

    /** The warning: each top block now and then shows as a glitch block, more often toward the end. */
    private void flicker(ServerLevel level, int tick, int warn) {
        UnfinishedTempBlocks data = UnfinishedTempBlocks.of(level);
        RandomSource random = boss.getRandom();
        float share = 0.25F + 0.6F * tick / warn;
        BlockState glitch = ModBlocks.GLITCH_BLOCK.get().defaultBlockState();
        long until = level.getGameTime() + warn + FLICKER_SAFETY;
        for (UnfinishedUnload.Column column : columns) {
            BlockPos top = new BlockPos(column.x(), column.top(), column.z());
            if (random.nextFloat() < share) {
                if (!flickering.contains(top) && level.getBlockState(top).isSolidRender(level, top)
                        && data.swap(level, top, glitch, until, UnfinishedTempBlocks.Kind.GROUND)) {
                    flickering.add(top);
                }
            } else if (flickering.remove(top)) {
                data.restore(level, top);
            }
        }
        UnfinishedEffects.unloadFlicker(level, Vec3.atBottomCenterOf(centre.above()), ServerConfig.UNFINISHED.unloadRadius.get());
    }

    private void settleFlicker(ServerLevel level) {
        if (flickering.isEmpty()) {
            return;
        }
        UnfinishedTempBlocks data = UnfinishedTempBlocks.of(level);
        for (BlockPos pos : flickering) {
            data.restore(level, pos);
        }
        flickering.clear();
    }
}
