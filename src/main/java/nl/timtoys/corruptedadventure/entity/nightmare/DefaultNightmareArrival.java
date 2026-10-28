package nl.timtoys.corruptedadventure.entity.nightmare;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * The arrival of a Nightmare without its own (a placeholder until its ring stream builds one): short omens of low cave
 * sounds, a heartbeat and ash round the target, then it steps out of smoke behind the target. Vanilla sounds and
 * particles, sent by the server.
 */
public final class DefaultNightmareArrival implements NightmareArrival {
    public static final DefaultNightmareArrival INSTANCE = new DefaultNightmareArrival();

    private static final int SOUND_EVERY = 60;
    private static final int HEARTBEAT_TICKS = 100;

    private DefaultNightmareArrival() {
    }

    @Override
    public int omenTicks() {
        return ServerConfig.NIGHTMARE_DIRECTOR.defaultOmenSeconds.get() * 20;
    }

    @Override
    public void omen(ServerLevel level, ServerPlayer target, BlockPos place, int tick) {
        int left = omenTicks() - tick;
        if (tick % SOUND_EVERY == 0) {
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.AMBIENT_CAVE.value(),
                    SoundSource.HOSTILE, 1.0F, 0.5F);
        }
        if (left <= HEARTBEAT_TICKS && left % 20 == 0) {
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.WARDEN_HEARTBEAT,
                    SoundSource.HOSTILE, 1.5F, 0.6F);
        }
        if (tick % 10 == 0) {
            level.sendParticles(ParticleTypes.ASH, target.getX(), target.getY() + 1.5, target.getZ(),
                    left <= HEARTBEAT_TICKS ? 30 : 8, 4.0, 2.0, 4.0, 0.0);
        }
    }

    @Override
    @Nullable
    public Nightmare appear(ServerLevel level, EntityType<? extends Nightmare> type, ServerPlayer target, BlockPos place,
                            float strength) {
        Optional<Vec3> spot = NightmareSpots.behind(level, type, target,
                ServerConfig.NIGHTMARE_DIRECTOR.defaultArrivalDistance.get());
        if (spot.isEmpty()) {
            return null;
        }
        Entity created = type.create(level);
        if (!(created instanceof Nightmare nightmare)) {
            return null;
        }
        Vec3 feet = spot.get();
        float yaw = (float) Math.toDegrees(Math.atan2(target.getZ() - feet.z, target.getX() - feet.x)) - 90.0F;
        nightmare.moveTo(feet.x, feet.y, feet.z, yaw, 0.0F);
        nightmare.setYHeadRot(yaw);
        if (!level.addFreshEntity(nightmare)) {
            return null;
        }
        nightmare.setTarget(target);
        level.playSound(null, feet.x, feet.y, feet.z, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 2.0F, 0.6F);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, feet.x, feet.y + nightmare.getBbHeight() * 0.5, feet.z, 80,
                nightmare.getBbWidth() * 0.6, nightmare.getBbHeight() * 0.4, nightmare.getBbWidth() * 0.6, 0.02);
        return nightmare;
    }
}
