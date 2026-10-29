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

    // --- the life round: bubble trap, dive and burst, the ring --------------------------------------------------------

    /** It lowers its head and calls up its breath from the deep: a deep rumble and a gurgle. */
    static void bubbleCall(ServerLevel level, TheLostKeeper keeper) {
        Vec3 head = keeper.headCentre();
        sound(level, head, SoundEvents.ELDER_GUARDIAN_AMBIENT, 2.5F, 0.45F);
        sound(level, head, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 2.0F, 0.6F);
        level.sendParticles(GLOW, head.x, head.y, head.z, 12, 1.0, 0.6, 1.0, 0.0);
    }

    /** The warning on a trap's spot: bubbles rise there (on land they burst out of the ground as small gusts). */
    static void bubbleWarn(ServerLevel level, Vec3 spot, boolean water, double radius, int tick) {
        double spread = radius * 0.6;
        if (water) {
            level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, spot.x, spot.y - 1.5, spot.z, 6, spread, 0.8, spread, 0.05);
            level.sendParticles(ParticleTypes.BUBBLE_POP, spot.x, spot.y + 0.1, spot.z, 4, spread, 0.05, spread, 0.02);
            level.sendParticles(ParticleTypes.SPLASH, spot.x, spot.y + 0.1, spot.z, 3, spread, 0.05, spread, 0.0);
        } else {
            level.sendParticles(ParticleTypes.BUBBLE_POP, spot.x, spot.y + 0.1, spot.z, 5, spread, 0.05, spread, 0.03);
            level.sendParticles(GLOW, spot.x, spot.y + 0.2, spot.z, 2, spread, 0.1, spread, 0.0);
            if (tick % 4 == 0) {
                level.sendParticles(ParticleTypes.SMALL_GUST, spot.x, spot.y + 0.3, spot.z, 1, spread, 0.1, spread, 0.0);
            }
        }
        if (tick % 10 == 1) {
            sound(level, spot, water ? SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT : SoundEvents.BREEZE_INHALE, 1.5F,
                    0.6F + tick * 0.01F);
        }
    }

    /** The current takes hold: a pull of water down into the deep. */
    static void currentStart(ServerLevel level, Vec3 spot) {
        sound(level, spot, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 2.0F, 0.5F);
        sound(level, spot, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.5F, 0.6F);
        level.sendParticles(ParticleTypes.SPLASH, spot.x, spot.y + 0.1, spot.z, 30, 1.0, 0.1, 1.0, 0.1);
    }

    /** One tick of the current: the downward stream of the column. */
    static void current(ServerLevel level, Vec3 spot, double bottom, double radius) {
        double height = Math.max(1.0, spot.y - bottom);
        double middle = (spot.y + bottom) / 2.0;
        level.sendParticles(ParticleTypes.CURRENT_DOWN, spot.x, middle, spot.z, 8, radius * 0.5, height * 0.4, radius * 0.5, 0.0);
        level.sendParticles(ParticleTypes.BUBBLE, spot.x, spot.y - 0.5, spot.z, 3, radius * 0.5, 0.3, radius * 0.5, 0.0);
    }

    /** On land: a gust bursts out of the ground. */
    static void gust(ServerLevel level, Vec3 spot) {
        sound(level, spot, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 2.0F, 0.6F);
        sound(level, spot, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 2.0F, 0.5F);
        level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, spot.x, spot.y + 0.5, spot.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.POOF, spot.x, spot.y + 0.3, spot.z, 20, 0.8, 0.2, 0.8, 0.08);
    }

    /** It plunges into the water. */
    static void diveSplash(ServerLevel level, Vec3 at) {
        sound(level, at, SoundEvents.GENERIC_SPLASH, 3.0F, 0.5F);
        sound(level, at, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 2.0F, 0.5F);
        level.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 1.0, at.z, 50, 1.5, 0.3, 1.5, 0.2);
    }

    /** The surface churns where it will burst: ripples, rising bubbles and a growing rumble. */
    static void burstWarn(ServerLevel level, Vec3 spot, double radius, int tick) {
        double spread = radius * 0.5;
        level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, spot.x, spot.y - 1.5, spot.z, 8, spread, 0.8, spread, 0.08);
        level.sendParticles(ParticleTypes.FISHING, spot.x, spot.y + 0.05, spot.z, 6, radius * 0.7, 0.0, radius * 0.7, 0.02);
        level.sendParticles(ParticleTypes.SPLASH, spot.x, spot.y + 0.1, spot.z, 4, spread, 0.05, spread, 0.0);
        if (tick % 8 == 1) {
            sound(level, spot, SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 2.0F, 0.5F + tick * 0.02F);
            sound(level, spot, SoundEvents.ELDER_GUARDIAN_AMBIENT, 1.5F, 0.4F);
        }
    }

    /** It bursts out of the water: a column of spray. */
    static void burst(ServerLevel level, Vec3 spot, double radius) {
        sound(level, spot, SoundEvents.GENERIC_SPLASH, 3.0F, 0.4F);
        sound(level, spot, SoundEvents.ELDER_GUARDIAN_CURSE, 2.0F, 0.7F);
        sound(level, spot, SoundEvents.TRIDENT_RIPTIDE_3.value(), 2.5F, 0.6F);
        level.sendParticles(ParticleTypes.SPLASH, spot.x, spot.y + 0.5, spot.z, 120, radius * 0.6, 1.5, radius * 0.6, 0.4);
        level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, spot.x, spot.y - 1.0, spot.z, 40, radius * 0.5, 1.0, radius * 0.5, 0.2);
        level.sendParticles(ParticleTypes.CLOUD, spot.x, spot.y + 1.0, spot.z, 20, radius * 0.5, 1.0, radius * 0.5, 0.05);
    }

    /** It starts the ring: a long hiss and a pastel ring of glow where its body will lie. */
    static void ringCall(ServerLevel level, TheLostKeeper keeper, Vec3 centre, double radius) {
        Vec3 head = keeper.headCentre();
        sound(level, head, SoundEvents.ELDER_GUARDIAN_AMBIENT, 3.0F, 0.7F);
        sound(level, head, SoundEvents.CONDUIT_ATTACK_TARGET, 2.0F, 0.5F);
        for (int k = 0; k < 32; k++) {
            double a = Math.PI * 2.0 * k / 32;
            level.sendParticles(GLOW, centre.x + Math.cos(a) * radius, centre.y, centre.z + Math.sin(a) * radius, 2, 0.1, 0.2, 0.1, 0.0);
        }
    }

    /** While it coils: a hiss now and then, scales on scales. */
    static void ringHiss(ServerLevel level, TheLostKeeper keeper) {
        Vec3 head = keeper.headCentre();
        sound(level, head, SoundEvents.BREEZE_SLIDE, 1.5F, 0.5F);
        sound(level, keeper.body().node(KeeperBody.SEGMENTS / 2), SoundEvents.ELDER_GUARDIAN_FLOP, 1.0F, 0.6F);
    }

    /** Hurt enough, it breaks off the ring. */
    static void ringBreaks(ServerLevel level, TheLostKeeper keeper) {
        Vec3 head = keeper.headCentre();
        sound(level, head, SoundEvents.ELDER_GUARDIAN_HURT, 2.5F, 0.5F);
        level.sendParticles(GLOW, head.x, head.y, head.z, 20, 1.0, 0.6, 1.0, 0.0);
    }

    /** The ring closes on whoever is still inside. */
    static void squeeze(ServerLevel level, Vec3 centre, double radius) {
        sound(level, centre, SoundEvents.EVOKER_FANGS_ATTACK, 2.0F, 0.5F);
        sound(level, centre, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 2.0F, 0.5F);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, centre.x, centre.y, centre.z, 6, radius * 0.4, 0.3, radius * 0.4, 0.0);
        level.sendParticles(ParticleTypes.SPLASH, centre.x, centre.y, centre.z, 40, radius * 0.5, 0.3, radius * 0.5, 0.2);
    }

    private static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.HOSTILE, volume, pitch);
    }
}
