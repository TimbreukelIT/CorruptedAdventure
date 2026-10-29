package nl.timtoys.corruptedadventure.entity.unfinished;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;
import org.jetbrains.annotations.Nullable;

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

    // ------------------------------------------------------------------------------------------------ throw

    /** A memory tears off his chest: stone cracking, a burst of pixels where it comes loose. */
    static void shardTear(ServerLevel level, Vec3 at) {
        play(level, at, SoundEvents.DEEPSLATE_BREAK, 3.0F, 0.5F);
        play(level, at, SoundEvents.ANCIENT_DEBRIS_BREAK, 3.0F, 0.4F);
        level.sendParticles(ModParticleTypes.GLITCH_PIXEL.get(), at.x, at.y, at.z, 40, 1.5, 1.5, 1.5, 0.0);
    }

    /** The warning while it rises: a deep grinding hum from him. */
    static void throwWindup(ServerLevel level, Entity boss) {
        play(level, boss.position(), SoundEvents.WARDEN_ROAR, 3.0F, 0.5F);
        play(level, boss.position(), SoundEvents.RESPAWN_ANCHOR_CHARGE, 3.0F, 0.4F);
    }

    static void throwRelease(ServerLevel level, Entity boss) {
        play(level, boss.position(), SoundEvents.RAVAGER_ATTACK, 3.0F, 0.4F);
        play(level, boss.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 3.0F, 0.4F);
    }

    /** The spot a memory will come down on: a ring of glitch pixels on the ground. */
    static void shardMark(ServerLevel level, Vec3 at, double radius) {
        int points = Math.max(8, (int) (radius * 8));
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points;
            level.sendParticles(ModParticleTypes.GLITCH_PIXEL.get(), at.x + Math.cos(angle) * radius, at.y + 0.2,
                    at.z + Math.sin(angle) * radius, 1, 0.05, 0.05, 0.05, 0.0);
        }
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 0.2, at.z, 4, radius * 0.4, 0.1, radius * 0.4, 0.0);
    }

    static void shardImpact(ServerLevel level, Vec3 at) {
        play(level, at, SoundEvents.GENERIC_EXPLODE.value(), 2.5F, 0.6F);
        play(level, at, SoundEvents.STONE_BREAK, 3.0F, 0.5F);
        level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 2, 1.0, 0.3, 1.0, 0.0);
        level.sendParticles(ModParticleTypes.GLITCH_PIXEL.get(), at.x, at.y + 0.5, at.z, 50, 2.0, 0.6, 2.0, 0.0);
    }

    /** A shard with nowhere to lie bursts into pixels. */
    static void shardShatter(ServerLevel level, Vec3 at) {
        play(level, at, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0F, 0.5F);
        level.sendParticles(ModParticleTypes.GLITCH_PIXEL.get(), at.x, at.y + 0.5, at.z, 10, 0.3, 0.3, 0.3, 0.0);
    }

    /** A lying shard (or ground that came back) flickers out. */
    static void shardGone(ServerLevel level, BlockPos pos) {
        play(level, Vec3.atCenterOf(pos), SoundEvents.AMETHYST_BLOCK_HIT, 0.6F, 0.5F);
        level.sendParticles(ModParticleTypes.GLITCH_PIXEL.get(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3,
                0.3, 0.3, 0.0);
    }

    // ------------------------------------------------------------------------------------------------ glitch step

    /** The warning: he freezes mid-move, a stuttering hum and pixels leaking from his seams. */
    static void glitchFreeze(ServerLevel level, Entity boss) {
        play(level, boss.position(), ModSoundEvents.RING_5_GLITCH.get(), 3.0F, 0.5F);
        play(level, boss.position(), SoundEvents.BEACON_DEACTIVATE, 3.0F, 1.6F);
        silhouette(level, boss.position(), 60);
    }

    /**
     * Where the next frame will be: a pillar of glitch pixels his height. Before the last one, also a ring on the ground
     * round where its frame blow lands.
     */
    static void glitchMark(ServerLevel level, Vec3 spot, @Nullable Vec3 strike, double radius) {
        level.sendParticles(ModParticleTypes.GLITCH_PIXEL.get(), spot.x, spot.y + 7.0, spot.z, 24, 1.2, 4.0, 1.2, 0.0);
        if (strike != null) {
            shardMark(level, strike, radius);
        }
    }

    /** A frame skipped: an after-image of pixels where he was, the glitch sound where he is. */
    static void glitchJump(ServerLevel level, Vec3 from, Vec3 to) {
        silhouette(level, from, 50);
        play(level, to, ModSoundEvents.RING_5_GLITCH.get(), 2.5F, 0.6F + level.random.nextFloat() * 0.3F);
        play(level, to, SoundEvents.ENDERMAN_TELEPORT, 2.0F, 0.4F);
    }

    static void glitchStrike(ServerLevel level, Vec3 at) {
        play(level, at, SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 0.8F);
        play(level, at, SoundEvents.GLASS_BREAK, 3.0F, 0.5F);
        level.sendParticles(ModParticleTypes.GLITCH_PIXEL.get(), at.x, at.y + 0.5, at.z, 60, 2.5, 0.5, 2.5, 0.0);
        level.sendParticles(ParticleTypes.SONIC_BOOM, at.x, at.y + 1.0, at.z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    // ------------------------------------------------------------------------------------------------ unloading

    /** He presses his void into the ground: a low drone from him and from the patch. */
    static void unloadWarn(ServerLevel level, Entity boss, Vec3 at) {
        play(level, boss.position(), SoundEvents.WARDEN_SONIC_CHARGE, 3.0F, 0.5F);
        play(level, at, ModSoundEvents.RING_5_GLITCH.get(), 2.5F, 0.4F);
        play(level, at, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), 2.5F, 0.5F);
    }

    /** Pixels rising off the flickering patch. */
    static void unloadFlicker(ServerLevel level, Vec3 at, double radius) {
        level.sendParticles(ModParticleTypes.GLITCH_PIXEL.get(), at.x, at.y + 0.3, at.z, 16, radius * 0.6, 0.2, radius * 0.6, 0.0);
        level.sendParticles(ModParticleTypes.VOID_MOTE.get(), at.x, at.y + 0.3, at.z, 4, radius * 0.6, 0.2, radius * 0.6, 0.0);
    }

    /** The patch unloads: glass and stone giving way at once, the void breathing up out of the hole. */
    static void unloadOpen(ServerLevel level, Vec3 at, double radius) {
        play(level, at, SoundEvents.GLASS_BREAK, 3.0F, 0.3F);
        play(level, at, SoundEvents.BEACON_DEACTIVATE, 3.0F, 0.4F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z, 120, radius * 0.6, 0.5, radius * 0.6, 0.05);
        level.sendParticles(ModParticleTypes.VOID_MOTE.get(), at.x, at.y - 2.0, at.z, 40, radius * 0.6, 2.0, radius * 0.6, 0.0);
    }

    private static void play(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.HOSTILE, volume, pitch);
    }

    /** His outline in pixels: a column his width and height. */
    private static void silhouette(ServerLevel level, Vec3 at, int count) {
        level.sendParticles(ModParticleTypes.GLITCH_PIXEL.get(), at.x, at.y + 7.0, at.z, count, 2.0, 4.5, 2.0, 0.0);
    }

    /** A burst over his void pool at his feet. */
    private static void ground(ServerLevel level, Entity boss, ParticleOptions particle, int count, double speed) {
        level.sendParticles(particle, boss.getX(), boss.getY() + 0.5, boss.getZ(), count, 4.0, 0.5, 4.0, speed);
    }
}
