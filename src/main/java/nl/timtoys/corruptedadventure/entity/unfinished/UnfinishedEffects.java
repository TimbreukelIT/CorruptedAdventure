package nl.timtoys.corruptedadventure.entity.unfinished;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;

/**
 * The Unfinished's sounds and particles, sent by the server to everyone near (vanilla events with a low pitch;
 * placeholders until the sound pass, backlog). His own voice is a mix of the four he is made of.
 */
final class UnfinishedEffects {
    private UnfinishedEffects() {
    }

    static void slamWindup(ServerLevel level, Entity boss) {
        play(level, boss.position(), SoundEvents.RAVAGER_ROAR, 3.0F, 0.4F);
    }

    static void slamHit(ServerLevel level, Vec3 at) {
        play(level, at, SoundEvents.GENERIC_EXPLODE.value(), 2.5F, 0.5F);
        play(level, at, SoundEvents.DEEPSLATE_BREAK, 3.0F, 0.4F);
        level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 4, 1.5, 0.3, 1.5, 0.0);
        level.sendParticles(ModParticleTypes.GLITCH_PIXEL.get(), at.x, at.y + 0.5, at.z, 40, 2.5, 0.5, 2.5, 0.0);
    }

    /** The warning of a rift step: glass and stone cracking, his void pool flaring. */
    static void riftCrack(ServerLevel level, Entity boss) {
        play(level, boss.position(), SoundEvents.GLASS_BREAK, 3.0F, 0.35F);
        play(level, boss.position(), SoundEvents.WARDEN_DIG, 3.0F, 0.6F);
        ground(level, boss, ParticleTypes.REVERSE_PORTAL, 120, 0.05);
    }

    static void riftBurst(ServerLevel level, Entity boss) {
        play(level, boss.position(), SoundEvents.ENDERMAN_TELEPORT, 3.0F, 0.3F);
        play(level, boss.position(), SoundEvents.WARDEN_EMERGE, 3.0F, 0.7F);
        ground(level, boss, ParticleTypes.REVERSE_PORTAL, 160, 0.08);
        ground(level, boss, ModParticleTypes.GLITCH_PIXEL.get(), 60, 0.0);
    }

    static void grabReach(ServerLevel level, Entity boss) {
        play(level, boss.position(), SoundEvents.SKELETON_HURT, 3.0F, 0.4F);
        play(level, boss.position(), SoundEvents.SPIDER_AMBIENT, 3.0F, 0.3F);
    }

    static void grabbed(ServerLevel level, Entity player) {
        play(level, player.position(), SoundEvents.BONE_BLOCK_BREAK, 2.0F, 0.5F);
    }

    static void released(ServerLevel level, Entity player) {
        play(level, player.position(), SoundEvents.SKELETON_DEATH, 2.0F, 0.5F);
    }

    static void ashRainStart(ServerLevel level, Entity boss) {
        play(level, boss.position(), SoundEvents.PHANTOM_FLAP, 3.0F, 0.3F);
        play(level, boss.position(), SoundEvents.BLAZE_AMBIENT, 3.0F, 0.4F);
    }

    /** One puff of an ash cloud: ash falling from above over the disc, embers between. */
    static void ashCloud(ServerLevel level, Vec3 centre, double radius) {
        level.sendParticles(ParticleTypes.ASH, centre.x, centre.y + 6.0, centre.z, 60, radius * 0.6, 2.0, radius * 0.6, 0.0);
        level.sendParticles(ParticleTypes.WHITE_ASH, centre.x, centre.y + 4.0, centre.z, 30, radius * 0.6, 2.0, radius * 0.6, 0.0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, centre.x, centre.y + 7.0, centre.z, 6, radius * 0.5, 0.5, radius * 0.5, 0.0);
    }

    static void lureSong(ServerLevel level, Entity boss) {
        play(level, boss.position(), SoundEvents.ELDER_GUARDIAN_AMBIENT, 3.5F, 0.55F);
        play(level, boss.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 3.0F, 0.5F);
    }

    static void luredPlayer(ServerLevel level, Entity player) {
        level.sendParticles(ParticleTypes.GLOW, player.getX(), player.getY() + 1.0, player.getZ(), 3, 0.4, 0.6, 0.4, 0.0);
    }

    static void voidPullStart(ServerLevel level, Entity boss) {
        play(level, boss.position(), SoundEvents.WARDEN_SONIC_CHARGE, 3.5F, 0.4F);
        play(level, boss.position(), SoundEvents.BEACON_DEACTIVATE, 3.0F, 0.3F);
    }

    static void voidPulled(ServerLevel level, Entity player) {
        level.sendParticles(ModParticleTypes.VOID_MOTE.get(), player.getX(), player.getY() + 1.0, player.getZ(), 4, 0.5, 0.8, 0.5, 0.0);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 4, 0.4, 0.8, 0.4, 0.02);
    }

    /** A weak spot hit while it is not loaded: the blow passes through something that is not there yet. */
    static void deflected(ServerLevel level, Vec3 at) {
        play(level, at, SoundEvents.AMETHYST_BLOCK_HIT, 1.5F, 0.4F);
        level.sendParticles(ModParticleTypes.GLITCH_PIXEL.get(), at.x, at.y, at.z, 8, 0.4, 0.4, 0.4, 0.0);
    }

    static void weakHit(ServerLevel level, Vec3 at) {
        play(level, at, SoundEvents.GLASS_BREAK, 1.5F, 0.8F);
    }

    /** The lens loads his weak spots (one sound when the state turns on). */
    static void loaded(ServerLevel level, Entity boss) {
        play(level, boss.position(), SoundEvents.BEACON_POWER_SELECT, 3.0F, 0.6F);
    }

    /** A graft tears off: chunks of a memory fall away. */
    static void graftLost(ServerLevel level, Vec3 at) {
        play(level, at, SoundEvents.WITHER_BREAK_BLOCK, 3.0F, 0.5F);
        play(level, at, SoundEvents.GLASS_BREAK, 3.0F, 0.3F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ModParticleTypes.GLITCH_PIXEL.get(), at.x, at.y, at.z, 80, 1.5, 1.5, 1.5, 0.0);
        level.sendParticles(ModParticleTypes.VOID_MOTE.get(), at.x, at.y, at.z, 40, 1.5, 1.5, 1.5, 0.0);
    }

    /** No player near for too long: he sinks back into the void (no drop). */
    static void retreat(ServerLevel level, Entity boss) {
        play(level, boss.position(), SoundEvents.WARDEN_DIG, 3.0F, 0.4F);
        ground(level, boss, ParticleTypes.REVERSE_PORTAL, 200, 0.08);
    }

    private static void play(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.HOSTILE, volume, pitch);
    }

    /** A burst over his void pool at his feet. */
    private static void ground(ServerLevel level, Entity boss, ParticleOptions particle, int count, double speed) {
        level.sendParticles(particle, boss.getX(), boss.getY() + 0.5, boss.getZ(), count, 4.0, 0.5, 4.0, speed);
    }
}
