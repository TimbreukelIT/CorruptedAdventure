package nl.timtoys.corruptedadventure.entity.unfinished;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.config.UnfinishedConfig;
import org.jetbrains.annotations.Nullable;

/**
 * Brokken gooien, his own: he tears a memory off his chest ({@link ShardVolley}), it rises above his crown while he turns
 * to his target and its ground glitches under it ({@code throwRiseTicks}: the warning), then he hurls it to where the
 * target stands at that moment; the spot is marked while it flies. Broken off before the throw, the memory falls apart
 * where it hangs. Not a graft's memory: he keeps it to the end.
 */
final class UnfinishedThrowGoal extends UnfinishedAttackGoal {
    private static final double CHEST_UP = 8.0;
    private static final double CHEST_FORWARD = 4.0;
    private static final double HOVER_UP = 18.0;
    private static final double HOVER_FORWARD = 2.0;
    /** Blocks per tick the memory flies (its flight time comes from this, within the bounds). */
    private static final double FLIGHT_SPEED = 1.2;
    private static final int MIN_FLIGHT = 14;
    private static final int MAX_FLIGHT = 36;
    /** Ticks after the throw before his next attack may start (the lump is still in the air). */
    private static final int FOLLOW_THROUGH = 12;

    @Nullable
    private ShardVolley volley;

    UnfinishedThrowGoal(TheUnfinished boss) {
        super(boss, null);
    }

    @Override
    protected int cooldown() {
        UnfinishedConfig config = ServerConfig.UNFINISHED;
        return UnfinishedConfig.cooldown(config.throwCooldownMin, config.throwCooldownMax, roll());
    }

    @Override
    protected int duration() {
        return ServerConfig.UNFINISHED.throwRiseTicks.get() + FOLLOW_THROUGH;
    }

    @Override
    protected boolean wants(LivingEntity target) {
        UnfinishedConfig config = ServerConfig.UNFINISHED;
        double distance = boss.distanceTo(target);
        return distance >= config.throwMinRange.get() && distance <= config.throwRange.get();
    }

    @Override
    protected void begin(ServerLevel level, LivingEntity target) {
        boss.faceToward(target.position());
        volley = ShardVolley.tearOff(level, boss, boss.localToWorld(0.0, CHEST_UP, CHEST_FORWARD),
                ServerConfig.UNFINISHED.throwPieces.get());
        boss.triggerAnim(UnfinishedAnimations.ACTION, UnfinishedAnimations.THROW);
        UnfinishedEffects.throwWindup(level, boss);
    }

    @Override
    protected void act(ServerLevel level, LivingEntity target, int tick) {
        if (volley == null) {
            return;
        }
        int rise = ServerConfig.UNFINISHED.throwRiseTicks.get();
        if (tick < rise) {
            boss.faceToward(target.position());
            volley.hover(boss.localToWorld(0.0, HOVER_UP, HOVER_FORWARD));
            if (tick % 4 == 0) {
                // Who is aimed at sees it: the ground under him glitches.
                UnfinishedEffects.shardMark(level, target.position(), 1.5);
            }
            return;
        }
        if (tick == rise) {
            Vec3 to = target.position();
            int flight = Mth.clamp((int) Math.round(volley.centre().distanceTo(to) / FLIGHT_SPEED), MIN_FLIGHT, MAX_FLIGHT);
            volley.launch(to, flight);
            UnfinishedEffects.throwRelease(level, boss);
        }
    }

    @Override
    protected void end(ServerLevel level) {
        if (volley != null && !volley.isThrown()) {
            volley.drop();
        }
        volley = null;
    }
}
