package nl.timtoys.corruptedadventure.weather;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;

/**
 * What a void teleport sounds and looks like (package 98, V98-8 A; the teleport itself is stream C's {@code VoidTeleport},
 * package 91): a short glitch at the spot the player leaves and at the spot he lands on, heard by everyone near (also the
 * player), and a burst of glitch pixels at both, so other players see him skip. Server-side; the sound and particles
 * reach the clients through vanilla's packets. Since phase 2 golf 5 also the hop of a mob hit by a glitch arrow
 * ({@code entity/GlitchHop}).
 */
public final class Ring5Glitches {
    static final float VOLUME = 0.8F;
    static final int PIXELS = 12;

    private Ring5Glitches() {
    }

    /**
     * Called by {@code VoidTeleport} (and {@code GlitchHop}) once it moves {@code entity} from {@code from} to {@code to}
     * (feet positions), on the server.
     */
    public static void teleported(Entity entity, Vec3 from, Vec3 to) {
        ServerLevel level = (ServerLevel) entity.level();
        glitch(level, from);
        glitch(level, to);
    }

    private static void glitch(ServerLevel level, Vec3 at) {
        level.playSound(null, at.x, at.y, at.z, ModSoundEvents.RING_5_GLITCH.get(), SoundSource.PLAYERS, VOLUME,
                0.9F + level.getRandom().nextFloat() * 0.2F);
        level.sendParticles(ModParticleTypes.GLITCH_PIXEL.get(), at.x, at.y + 1.0, at.z, PIXELS, 0.4, 0.8, 0.4, 0.0);
    }
}
