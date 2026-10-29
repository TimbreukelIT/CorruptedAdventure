package nl.timtoys.corruptedadventure.entity.risen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * The Risen's sounds and particles, sent by the server to everyone near (vanilla events, low pitches; several at once
 * for the choir). Placeholders for step 1: its own sounds are in the backlog.
 */
final class RisenEffects {
    private RisenEffects() {
    }

    /** The ground glows and cracks where it will rise. */
    static void groundGlow(ServerLevel level, BlockPos centre, int radius) {
        level.playSound(null, centre, SoundEvents.LAVA_AMBIENT, SoundSource.HOSTILE, 2.0F, 0.5F);
        level.sendParticles(ParticleTypes.LAVA, centre.getX() + 0.5, centre.getY() + 1.1, centre.getZ() + 0.5, 6,
                radius * 0.6, 0.1, radius * 0.6, 0.0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, centre.getX() + 0.5, centre.getY() + 1.2, centre.getZ() + 0.5, 8,
                radius * 0.6, 0.2, radius * 0.6, 0.02);
    }

    /** The pool opens: the choir rises with it. */
    static void poolOpens(ServerLevel level, BlockPos centre) {
        level.playSound(null, centre, SoundEvents.LAVA_POP, SoundSource.HOSTILE, 3.0F, 0.5F);
        choir(level, centre.getX() + 0.5, centre.getY() + 1.0, centre.getZ() + 0.5, 2.0F, 0.6F);
    }

    /** Wings torn open at the top of the rise. */
    static void risen(ServerLevel level, Entity risen) {
        level.playSound(null, risen.getX(), risen.getY(), risen.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 4.0F, 0.5F);
        burst(level, risen, ParticleTypes.FLAME, 60, 0.08);
        burst(level, risen, ParticleTypes.LARGE_SMOKE, 30, 0.04);
    }

    static void poolSets(ServerLevel level, BlockPos centre) {
        level.playSound(null, centre, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 2.0F, 0.6F);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, centre.getX() + 0.5, centre.getY() + 1.2, centre.getZ() + 0.5, 30,
                2.0, 0.3, 2.0, 0.02);
    }

    /** A beat of the wings, now and then while it flies. */
    static void flap(ServerLevel level, Entity risen) {
        level.playSound(null, risen.getX(), risen.getY(), risen.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 3.0F, 0.55F);
    }

    /** Embers trailing from the feathers. */
    static void embers(ServerLevel level, Entity risen) {
        level.sendParticles(ParticleTypes.SMALL_FLAME, risen.getX(), risen.getY() + risen.getBbHeight() * 0.6, risen.getZ(), 3,
                3.0, 1.0, 3.0, 0.01);
    }

    /** Ash falling from its wings in a column down to the ground. */
    static void ashRain(ServerLevel level, double x, double top, double bottom, double z, double radius) {
        double middle = (top + bottom) * 0.5;
        double half = Math.max(0.5, (top - bottom) * 0.5);
        level.sendParticles(ParticleTypes.ASH, x, middle, z, 60, radius * 0.6, half, radius * 0.6, 0.0);
        level.sendParticles(ParticleTypes.WHITE_ASH, x, middle, z, 25, radius * 0.6, half, radius * 0.6, 0.0);
    }

    static void diveScreech(ServerLevel level, Entity risen) {
        level.playSound(null, risen.getX(), risen.getY(), risen.getZ(), SoundEvents.PHANTOM_SWOOP, SoundSource.HOSTILE, 4.0F, 0.4F);
    }

    static void talonHit(ServerLevel level, Entity victim) {
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.5F, 0.6F);
        level.sendParticles(ParticleTypes.FLAME, victim.getX(), victim.getY() + 1.0, victim.getZ(), 15, 0.4, 0.6, 0.4, 0.02);
    }

    static void land(ServerLevel level, Entity risen) {
        level.playSound(null, risen.getX(), risen.getY(), risen.getZ(), SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 3.0F, 0.4F);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, risen.getX(), risen.getY() + 0.2, risen.getZ(), 20, 3.0, 0.1, 3.0, 0.01);
    }

    static void peck(ServerLevel level, Entity risen) {
        level.playSound(null, risen.getX(), risen.getY(), risen.getZ(), SoundEvents.PHANTOM_BITE, SoundSource.HOSTILE, 2.5F, 0.4F);
    }

    static void buffet(ServerLevel level, Entity risen, double radius) {
        level.playSound(null, risen.getX(), risen.getY(), risen.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 4.0F, 0.35F);
        level.sendParticles(ParticleTypes.CLOUD, risen.getX(), risen.getY() + 0.5, risen.getZ(), 40, radius * 0.5, 0.2, radius * 0.5, 0.15);
        level.sendParticles(ParticleTypes.ASH, risen.getX(), risen.getY() + 1.0, risen.getZ(), 80, radius * 0.5, 1.0, radius * 0.5, 0.05);
    }

    /** The choir draws breath: the furnace in its chest flares. */
    static void screamWindup(ServerLevel level, Entity risen) {
        level.playSound(null, risen.getX(), risen.getY(), risen.getZ(), SoundEvents.BLAZE_AMBIENT, SoundSource.HOSTILE, 3.0F, 0.4F);
        burst(level, risen, ParticleTypes.FLAME, 30, 0.02);
    }

    /** Every voice at once. */
    static void scream(ServerLevel level, Entity risen) {
        choir(level, risen.getX(), risen.getY() + risen.getBbHeight() * 0.7, risen.getZ(), 5.0F, 1.0F);
        burst(level, risen, ParticleTypes.SONIC_BOOM, 1, 0.0);
        burst(level, risen, ParticleTypes.FLAME, 80, 0.15);
    }

    /** Figures tear loose from its feathers. */
    static void release(ServerLevel level, Entity walker) {
        level.playSound(null, walker.getX(), walker.getY(), walker.getZ(), SoundEvents.HUSK_AMBIENT, SoundSource.HOSTILE, 2.0F, 0.5F);
        level.sendParticles(ParticleTypes.ASH, walker.getX(), walker.getY() + 1.0, walker.getZ(), 40, 0.4, 0.8, 0.4, 0.02);
        level.sendParticles(ParticleTypes.SMALL_FLAME, walker.getX(), walker.getY() + 1.0, walker.getZ(), 10, 0.3, 0.6, 0.3, 0.01);
    }

    /** The horn drowned out the choir. */
    static void stunned(ServerLevel level, Entity risen) {
        level.playSound(null, risen.getX(), risen.getY(), risen.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 4.0F, 0.5F);
        level.playSound(null, risen.getX(), risen.getY(), risen.getZ(), SoundEvents.BLAZE_HURT, SoundSource.HOSTILE, 3.0F, 0.4F);
        burst(level, risen, ParticleTypes.LARGE_SMOKE, 60, 0.03);
    }

    /** No player near for too long: it sinks into the ground as smoke and embers (no drop). */
    static void retreat(ServerLevel level, Entity risen) {
        choir(level, risen.getX(), risen.getY(), risen.getZ(), 2.0F, 0.5F);
        burst(level, risen, ParticleTypes.LARGE_SMOKE, 120, 0.05);
        burst(level, risen, ParticleTypes.LAVA, 20, 0.0);
    }

    // --- leven-ronde: swoop, ember rain, burning ground -----------------------------------------------------------------

    /** It beats up into the sky for the swoop: heavy flaps and a trail of smoke. */
    static void swoopClimb(ServerLevel level, Entity risen) {
        level.playSound(null, risen.getX(), risen.getY(), risen.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 4.0F, 0.45F);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, risen.getX(), risen.getY(), risen.getZ(), 12, 2.0, 0.5, 2.0, 0.01);
    }

    /** At the top, before it drops: the screech, the choir drawing breath, its wings flaring. */
    static void swoopWindup(ServerLevel level, Entity risen) {
        level.playSound(null, risen.getX(), risen.getY(), risen.getZ(), SoundEvents.PHANTOM_SWOOP, SoundSource.HOSTILE, 6.0F, 0.3F);
        choir(level, risen.getX(), risen.getY(), risen.getZ(), 3.0F, 0.7F);
        burst(level, risen, ParticleTypes.FLAME, 50, 0.05);
    }

    /** Flames streaming from it during the windup, so it shows against the sky. */
    static void swoopGlow(ServerLevel level, Entity risen) {
        burst(level, risen, ParticleTypes.FLAME, 12, 0.03);
        burst(level, risen, ParticleTypes.LAVA, 2, 0.0);
    }

    /** It hits the bottom of its swoop. */
    static void swoopImpact(ServerLevel level, Entity risen) {
        level.playSound(null, risen.getX(), risen.getY(), risen.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 4.0F, 0.5F);
        level.playSound(null, risen.getX(), risen.getY(), risen.getZ(), SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 4.0F, 0.4F);
        level.sendParticles(ParticleTypes.FLAME, risen.getX(), risen.getY() + 0.5, risen.getZ(), 60, 2.5, 0.3, 2.5, 0.08);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, risen.getX(), risen.getY() + 0.5, risen.getZ(), 30, 2.5, 0.3, 2.5, 0.03);
    }

    static void swoopHit(ServerLevel level, Entity victim) {
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 2.0F, 0.5F);
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.5F, 0.7F);
        level.sendParticles(ParticleTypes.FLAME, victim.getX(), victim.getY() + 1.0, victim.getZ(), 25, 0.5, 0.7, 0.5, 0.04);
    }

    /** It rears up on one leg: a growl and cracks glowing in the ground around it, out to where the stomp reaches. */
    static void stompWindup(ServerLevel level, Entity risen, double radius) {
        level.playSound(null, risen.getX(), risen.getY(), risen.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 3.0F, 0.5F);
        ring(level, risen.getX(), risen.getY() + 0.1, risen.getZ(), radius, ParticleTypes.SMALL_FLAME);
    }

    /** The stomp lands: a shock of smoke and flame through the ground around it. */
    static void stomp(ServerLevel level, Entity risen, double radius) {
        level.playSound(null, risen.getX(), risen.getY(), risen.getZ(), SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 5.0F, 0.3F);
        level.playSound(null, risen.getX(), risen.getY(), risen.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.5F, 0.6F);
        for (double r = 1.5; r <= radius; r += 1.5) {
            ring(level, risen.getX(), risen.getY() + 0.2, risen.getZ(), r, ParticleTypes.CAMPFIRE_COSY_SMOKE);
        }
        ring(level, risen.getX(), risen.getY() + 0.2, risen.getZ(), radius, ParticleTypes.FLAME);
    }

    /** The rim of the ember rain's area smoulders before the embers fall. */
    static void emberRainRim(ServerLevel level, Vec3 centre, double radius) {
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.FIRE_AMBIENT, SoundSource.HOSTILE, 3.0F, 0.6F);
        int points = Math.max(12, (int) (radius * 4));
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points;
            double x = centre.x + Math.cos(angle) * radius;
            double z = centre.z + Math.sin(angle) * radius;
            double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z)) + 0.1;
            level.sendParticles(ParticleTypes.SMALL_FLAME, x, y, z, 1, 0.1, 0.0, 0.1, 0.005);
            level.sendParticles(ParticleTypes.SMOKE, x, y + 0.2, z, 1, 0.1, 0.1, 0.1, 0.01);
        }
    }

    /** The wings shed the embers. */
    static void emberRainShed(ServerLevel level, Entity risen) {
        level.playSound(null, risen.getX(), risen.getY(), risen.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 3.0F, 0.5F);
        level.sendParticles(ParticleTypes.LAVA, risen.getX(), risen.getY() + risen.getBbHeight() * 0.5, risen.getZ(), 8,
                5.0, 0.5, 5.0, 0.0);
    }

    /** An ember on its way: a glowing drop above its spot and smoke on the spot itself. */
    static void emberFalling(ServerLevel level, BlockPos spot, long ticksLeft) {
        double x = spot.getX() + 0.5;
        double z = spot.getZ() + 0.5;
        level.sendParticles(ParticleTypes.FLAME, x, spot.getY() + 0.1, z, 2, 0.25, 0.0, 0.25, 0.005);
        level.sendParticles(ParticleTypes.SMOKE, x, spot.getY() + 0.3, z, 2, 0.2, 0.1, 0.2, 0.01);
        level.sendParticles(ParticleTypes.FALLING_LAVA, x, spot.getY() + 1.0 + ticksLeft * 0.6, z, 2, 0.15, 0.3, 0.15, 0.0);
    }

    static void emberLands(ServerLevel level, BlockPos spot) {
        double x = spot.getX() + 0.5;
        double z = spot.getZ() + 0.5;
        level.playSound(null, x, spot.getY(), z, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.5F, 0.8F + level.random.nextFloat() * 0.3F);
        level.sendParticles(ParticleTypes.LAVA, x, spot.getY() + 0.2, z, 4, 0.3, 0.1, 0.3, 0.0);
        level.sendParticles(ParticleTypes.FLAME, x, spot.getY() + 0.2, z, 10, 0.4, 0.2, 0.4, 0.04);
    }

    /** A tile of its fire catches. */
    static void fireCatches(ServerLevel level, BlockPos tile) {
        level.sendParticles(ParticleTypes.FLAME, tile.getX() + 0.5, tile.getY() + 0.2, tile.getZ() + 0.5, 4, 0.35, 0.1, 0.35, 0.02);
    }

    /** A burning tile, on its beat; dying down, it only smokes. */
    static void fireFlicker(ServerLevel level, BlockPos tile, boolean dying) {
        double x = tile.getX() + 0.5;
        double z = tile.getZ() + 0.5;
        if (!dying) {
            level.sendParticles(ParticleTypes.FLAME, x, tile.getY() + 0.25, z, 3, 0.3, 0.15, 0.3, 0.01);
        }
        level.sendParticles(dying ? ParticleTypes.SMOKE : ParticleTypes.LARGE_SMOKE, x, tile.getY() + 0.6, z, 1, 0.3, 0.2, 0.3, 0.01);
        if (level.random.nextInt(12) == 0) {
            level.playSound(null, x, tile.getY(), z, SoundEvents.FIRE_AMBIENT, SoundSource.HOSTILE, 0.8F, 0.8F + level.random.nextFloat() * 0.4F);
        }
    }

    /** Its choir: three low voices stacked. */
    private static void choir(ServerLevel level, double x, double y, double z, float volume, float pitch) {
        for (SoundEvent voice : new SoundEvent[]{SoundEvents.GHAST_SCREAM, SoundEvents.WARDEN_ROAR, SoundEvents.ELDER_GUARDIAN_CURSE}) {
            level.playSound(null, x, y, z, voice, SoundSource.HOSTILE, volume, pitch * (0.5F + level.random.nextFloat() * 0.2F));
        }
    }

    private static void ring(ServerLevel level, double x, double y, double z, double radius, ParticleOptions particle) {
        int points = Math.max(10, (int) (radius * 5));
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points;
            level.sendParticles(particle, x + Math.cos(angle) * radius, y, z + Math.sin(angle) * radius, 1, 0.1, 0.05, 0.1, 0.01);
        }
    }

    private static void burst(ServerLevel level, Entity risen, ParticleOptions particle, int count, double speed) {
        level.sendParticles(particle, risen.getX(), risen.getY() + risen.getBbHeight() * 0.6, risen.getZ(), count,
                risen.getBbWidth() * 0.6, risen.getBbHeight() * 0.3, risen.getBbWidth() * 0.6, speed);
    }
}
