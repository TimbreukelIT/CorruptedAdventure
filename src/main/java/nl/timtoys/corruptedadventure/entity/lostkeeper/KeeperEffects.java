package nl.timtoys.corruptedadventure.entity.lostkeeper;

import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;

/**
 * The Lost Keeper's sounds and particles, sent by the server to everyone near (vanilla events with an odd pitch, as for
 * De Holle). Placeholders until it gets its own sounds (backlog).
 */
final class KeeperEffects {
    /** The breath's green (ring 4 "gifgloed"). */
    private static final ParticleOptions POISON = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.53F, 0.63F, 0.25F);
    /** The pastel glow of the mythical half. */
    private static final ParticleOptions GLOW = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.9F, 0.71F, 0.87F);

    private KeeperEffects() {
    }

    /** The siren's call: a soft, high chime round its head and a pastel shimmer. */
    static void lureSong(ServerLevel level, TheLostKeeper keeper, int tick) {
        Vec3 head = keeper.headCentre();
        if (tick % 10 == 0) {
            float pitch = 0.8F + 0.25F * (float) Math.sin(tick * 0.37);
            sound(level, head, SoundEvents.AMETHYST_BLOCK_RESONATE, 2.5F, pitch);
            sound(level, head, SoundEvents.CONDUIT_AMBIENT_SHORT, 2.0F, 1.4F);
        }
        level.sendParticles(GLOW, head.x, head.y + 1.0, head.z, 3, 1.2, 1.0, 1.2, 0.0);
        level.sendParticles(ModParticleTypes.FIREFLY.get(), head.x, head.y + 1.5, head.z, 1, 1.5, 1.0, 1.5, 0.0);
    }

    /** On each lured player: a thread of glow drifting towards the keeper. */
    static void lureThread(ServerLevel level, Vec3 from, Vec3 towards) {
        Vec3 mid = from.lerp(towards, 0.3);
        level.sendParticles(GLOW, mid.x, mid.y + 1.0, mid.z, 1, 0.2, 0.2, 0.2, 0.0);
    }

    /** It rears back and draws in its breath. */
    static void breathWindup(ServerLevel level, TheLostKeeper keeper) {
        Vec3 mouth = keeper.mouth();
        sound(level, mouth, SoundEvents.ELDER_GUARDIAN_AMBIENT, 2.5F, 0.6F);
        sound(level, mouth, SoundEvents.BREEZE_INHALE, 2.0F, 0.5F);
    }

    /** One tick of breath: a green stream from its mouth along {@code direction}. */
    static void breath(ServerLevel level, TheLostKeeper keeper, Vec3 direction, double range, int tick) {
        Vec3 mouth = keeper.mouth();
        if (tick % 8 == 0) {
            sound(level, mouth, SoundEvents.ENDER_DRAGON_SHOOT, 2.0F, 0.5F);
            sound(level, mouth, SoundEvents.LAVA_EXTINGUISH, 1.0F, 0.6F);
        }
        for (int k = 0; k < 6; k++) {
            double along = range * (k + level.random.nextDouble()) / 6.0;
            Vec3 at = mouth.add(direction.scale(along));
            double spread = 0.25 + along * 0.3;
            level.sendParticles(ParticleTypes.SNEEZE, at.x, at.y, at.z, 2, spread, spread, spread, 0.02);
            level.sendParticles(POISON, at.x, at.y, at.z, 2, spread, spread, spread, 0.0);
        }
    }

    /** A lingering poison cloud. */
    static void cloud(ServerLevel level, Vec3 centre, double radius) {
        level.sendParticles(POISON, centre.x, centre.y, centre.z, 8, radius * 0.5, radius * 0.4, radius * 0.5, 0.0);
        level.sendParticles(ModParticleTypes.SWAMP_GAS.get(), centre.x, centre.y, centre.z, 2, radius * 0.5, radius * 0.3, radius * 0.5, 0.0);
    }

    /** The lunge before the snap. */
    static void lunge(ServerLevel level, TheLostKeeper keeper) {
        sound(level, keeper.headCentre(), SoundEvents.ELDER_GUARDIAN_HURT, 2.5F, 0.5F);
    }

    /** It snaps its jaws: a bite (nothing caught) or the coil round a fighter. */
    static void snap(ServerLevel level, TheLostKeeper keeper, boolean caught) {
        Vec3 mouth = keeper.mouth();
        sound(level, mouth, SoundEvents.EVOKER_FANGS_ATTACK, 2.0F, 0.6F);
        if (caught) {
            sound(level, mouth, SoundEvents.ELDER_GUARDIAN_CURSE, 1.5F, 1.2F);
            level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, mouth.x, mouth.y, mouth.z, 30, 1.0, 1.0, 1.0, 0.1);
        }
    }

    /** The tail rattles before the whip. */
    static void whipWindup(ServerLevel level, Vec3 tail) {
        sound(level, tail, SoundEvents.TRIDENT_RIPTIDE_1.value(), 2.0F, 0.5F);
    }

    static void whipHit(ServerLevel level, Vec3 at) {
        sound(level, at, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 2.0F, 0.5F);
        sound(level, at, SoundEvents.GENERIC_SPLASH, 1.5F, 0.6F);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y + 1.0, at.z, 3, 0.6, 0.4, 0.6, 0.0);
    }

    /** A branch of the crown breaks off: pearl and coral splinter, a light goes out. */
    static void branchBreaks(ServerLevel level, TheLostKeeper keeper, Vec3 crown) {
        sound(level, crown, SoundEvents.AMETHYST_CLUSTER_BREAK, 2.5F, 0.7F);
        sound(level, crown, SoundEvents.GLASS_BREAK, 2.0F, 0.6F);
        sound(level, crown, SoundEvents.ELDER_GUARDIAN_HURT, 2.0F, 0.8F);
        level.sendParticles(GLOW, crown.x, crown.y, crown.z, 30, 0.6, 0.6, 0.6, 0.0);
        level.sendParticles(ParticleTypes.END_ROD, crown.x, crown.y, crown.z, 12, 0.4, 0.4, 0.4, 0.08);
    }

    /** The last branch is gone: the crown goes dark. */
    static void crownDark(ServerLevel level, TheLostKeeper keeper) {
        Vec3 head = keeper.headCentre();
        sound(level, head, SoundEvents.BEACON_DEACTIVATE, 3.0F, 0.5F);
        sound(level, head, SoundEvents.ELDER_GUARDIAN_DEATH, 2.0F, 1.3F);
    }

    /** It comes down after losing its crown: on the ground hard, in water with a great splash. */
    static void landing(ServerLevel level, TheLostKeeper keeper, boolean water) {
        Vec3 head = keeper.headCentre();
        if (water) {
            sound(level, head, SoundEvents.GENERIC_SPLASH, 3.0F, 0.5F);
            level.sendParticles(ParticleTypes.SPLASH, head.x, head.y, head.z, 80, 3.0, 0.5, 3.0, 0.2);
        } else {
            sound(level, head, SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 0.6F);
            level.sendParticles(ParticleTypes.CLOUD, head.x, head.y - 1.0, head.z, 40, 3.0, 0.3, 3.0, 0.05);
        }
    }

    /** No player near for too long: it sinks into its mist (no drop). */
    static void retreat(ServerLevel level, TheLostKeeper keeper) {
        Vec3 head = keeper.headCentre();
        sound(level, head, SoundEvents.ELDER_GUARDIAN_AMBIENT, 3.0F, 0.5F);
        level.sendParticles(ParticleTypes.CLOUD, head.x, head.y, head.z, 60, 2.0, 2.0, 2.0, 0.02);
    }

    private static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.HOSTILE, volume, pitch);
    }
}
