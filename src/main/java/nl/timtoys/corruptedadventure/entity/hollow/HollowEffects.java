package nl.timtoys.corruptedadventure.entity.hollow;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;

/**
 * The Hollow's sounds and particles, sent by the server to everyone near (vanilla events with a low pitch; own sounds
 * are in the backlog).
 */
final class HollowEffects {
    private HollowEffects() {
    }

    /** The warning before a rift step: stone and glass cracking, ~1 s before it goes. */
    static void riftCrack(ServerLevel level, Entity hollow) {
        level.playSound(null, hollow.getX(), hollow.getY(), hollow.getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 0.4F);
        level.playSound(null, hollow.getX(), hollow.getY(), hollow.getZ(), SoundEvents.DEEPSLATE_BREAK, SoundSource.HOSTILE, 2.5F, 0.5F);
        burst(level, hollow, ModParticleTypes.GLITCH_PIXEL.get(), 24, 0.0);
    }

    /** Where a rift step leaves and where it comes out (the rifts themselves are {@link HollowRift}s). */
    static void riftBurst(ServerLevel level, Entity hollow) {
        level.playSound(null, hollow.getX(), hollow.getY(), hollow.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2.0F, 0.45F);
        burst(level, hollow, ModParticleTypes.GLITCH_PIXEL.get(), 20, 0.0);
    }

    static void phaseOut(ServerLevel level, Entity hollow) {
        level.playSound(null, hollow.getX(), hollow.getY(), hollow.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.5F, 0.6F);
        burst(level, hollow, ModParticleTypes.VOID_MOTE.get(), 20, 0.0);
    }

    static void phaseIn(ServerLevel level, Entity hollow) {
        level.playSound(null, hollow.getX(), hollow.getY(), hollow.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.5F, 0.6F);
    }

    /** A soul crystal pins it in phase: the crystal shatters into soul light. */
    static void pinned(ServerLevel level, Entity hollow) {
        level.playSound(null, hollow.getX(), hollow.getY(), hollow.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 2.0F, 0.6F);
        level.playSound(null, hollow.getX(), hollow.getY(), hollow.getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 2.0F, 0.8F);
        burst(level, hollow, ParticleTypes.SOUL_FIRE_FLAME, 40, 0.02);
    }

    static void slamWindup(ServerLevel level, Entity hollow) {
        level.playSound(null, hollow.getX(), hollow.getY(), hollow.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.5F, 0.5F);
    }

    static void slamHit(ServerLevel level, Entity hollow) {
        level.playSound(null, hollow.getX(), hollow.getY(), hollow.getZ(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 2.0F, 0.4F);
    }

    /** The warning on both players before a swap. */
    static void swapMark(ServerLevel level, Entity player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.HOSTILE, 1.5F, 0.5F);
        level.sendParticles(ModParticleTypes.GLITCH_PIXEL.get(), player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.9, 0.4, 0.0);
    }

    static void swapped(ServerLevel level, Entity player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.6F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.3, 0.8, 0.3, 0.05);
    }

    /** No player near for too long: it sinks back into nothing (no drop). */
    static void retreat(ServerLevel level, Entity hollow) {
        level.playSound(null, hollow.getX(), hollow.getY(), hollow.getZ(), SoundEvents.WARDEN_DIG, SoundSource.HOSTILE, 2.0F, 0.5F);
        burst(level, hollow, ParticleTypes.REVERSE_PORTAL, 120, 0.05);
        burst(level, hollow, ModParticleTypes.VOID_MOTE.get(), 40, 0.0);
    }

    private static void burst(ServerLevel level, Entity hollow, ParticleOptions particle, int count, double speed) {
        level.sendParticles(particle, hollow.getX(), hollow.getY() + hollow.getBbHeight() * 0.5, hollow.getZ(), count,
                hollow.getBbWidth() * 0.5, hollow.getBbHeight() * 0.4, hollow.getBbWidth() * 0.5, speed);
    }
}
