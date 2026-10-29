package nl.timtoys.corruptedadventure.entity.layered;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

/**
 * The Layered's sounds and particles, sent by the server to everyone near (vanilla events with a low pitch;
 * placeholders until its own sounds, backlog). Bone rattles and canyon dust: it is a thing of bone and stone.
 */
final class LayeredEffects {
    // No coloured dust (DustParticleOptions): the client shifts each colour channel on its own, which turns sand and bone
    // green and pink. Block particles keep the colours of the texture; poof clouds are a neutral white.
    private LayeredEffects() {
    }

    static void biteWindup(ServerLevel level, Entity layered) {
        play(level, layered, SoundEvents.SPIDER_AMBIENT, 2.0F, 0.45F);
        play(level, layered, SoundEvents.SKELETON_AMBIENT, 1.5F, 0.5F);
    }

    static void bite(ServerLevel level, Entity layered) {
        play(level, layered, SoundEvents.EVOKER_FANGS_ATTACK, 1.5F, 0.6F);
    }

    static void grabbed(ServerLevel level, Entity layered) {
        play(level, layered, SoundEvents.EVOKER_FANGS_ATTACK, 2.0F, 0.5F);
        play(level, layered, SoundEvents.SKELETON_HURT, 1.5F, 0.4F);
    }

    static void shake(ServerLevel level, Entity layered) {
        play(level, layered, SoundEvents.SKELETON_STEP, 2.0F, 0.5F);
    }

    static void release(ServerLevel level, Entity layered) {
        play(level, layered, SoundEvents.SKELETON_AMBIENT, 1.5F, 0.4F);
    }

    /** It hammers the wall with its front legs: stone cracking and dust falling. */
    static void rockfallWindup(ServerLevel level, Entity layered) {
        play(level, layered, SoundEvents.STONE_HIT, 2.5F, 0.5F);
        play(level, layered, SoundEvents.DRIPSTONE_BLOCK_BREAK, 2.5F, 0.5F);
        dust(level, layered, 40);
    }

    /** Fire in its fur or the dazzle: it loses its grip, its bones rattling. */
    static void knockedOff(ServerLevel level, Entity layered) {
        play(level, layered, SoundEvents.SKELETON_DEATH, 2.0F, 0.5F);
        play(level, layered, SoundEvents.SPIDER_HURT, 2.0F, 0.45F);
    }

    /** It crashes down: a thud and a cloud of canyon dust. */
    static void land(ServerLevel level, Entity layered) {
        play(level, layered, SoundEvents.GENERIC_BIG_FALL, 3.0F, 0.6F);
        play(level, layered, SoundEvents.STONE_BREAK, 2.5F, 0.5F);
        dust(level, layered, 80);
    }

    static void flipped(ServerLevel level, Entity layered) {
        play(level, layered, SoundEvents.SPIDER_HURT, 2.0F, 0.4F);
        play(level, layered, SoundEvents.SKELETON_STEP, 2.0F, 0.4F);
    }

    static void getUp(ServerLevel level, Entity layered) {
        play(level, layered, SoundEvents.SPIDER_AMBIENT, 2.0F, 0.5F);
        dust(level, layered, 30);
    }

    /** No player near for too long: it crawls off into the rock (no drop). */
    static void retreat(ServerLevel level, Entity layered) {
        play(level, layered, SoundEvents.SKELETON_STEP, 2.0F, 0.4F);
        play(level, layered, SoundEvents.STONE_BREAK, 2.0F, 0.4F);
        dust(level, layered, 120);
    }

    /** The shared stagger: its bones clatter and the stone of its back grinds; dust shakes off it. */
    static void stagger(ServerLevel level, Entity layered) {
        play(level, layered, SoundEvents.SKELETON_HURT, 2.0F, 0.35F);
        play(level, layered, SoundEvents.RAVAGER_STUNNED, 1.5F, 0.7F);
        dust(level, layered, 50);
    }

    /** At the last known place of its target: a dry snuffle, the dead heads rattling. */
    static void sniff(ServerLevel level, Entity layered) {
        play(level, layered, SoundEvents.SNIFFER_SNIFFING, 2.0F, 0.5F);
        play(level, layered, SoundEvents.SKELETON_AMBIENT, 1.0F, 0.4F);
    }

    /** It crouches to leap: its bones rattle and dust and pebbles trickle off it. */
    static void pounceWindup(ServerLevel level, Entity layered) {
        play(level, layered, SoundEvents.SKELETON_AMBIENT, 2.5F, 0.4F);
        play(level, layered, SoundEvents.SPIDER_AMBIENT, 2.5F, 0.4F);
        play(level, layered, SoundEvents.GRAVEL_FALL, 2.0F, 0.6F);
    }

    /** Each tick of the crouch: dust falls off it. */
    static void pounceCrouch(ServerLevel level, Entity layered) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, canyonStone()), layered.getX(),
                layered.getY() + 0.5, layered.getZ(), 4, layered.getBbWidth() * 0.6, 0.6, layered.getBbWidth() * 0.6, 0.0);
    }

    static void leap(ServerLevel level, Entity layered) {
        play(level, layered, SoundEvents.RAVAGER_ROAR, 2.0F, 0.6F);
        play(level, layered, SoundEvents.STONE_BREAK, 2.0F, 0.6F);
        dust(level, layered, 30);
    }

    /** It lands: a heavy thud, a ring of dust and the ground it hit, thrown up. */
    static void pounceImpact(ServerLevel level, Entity layered, BlockState ground, double radius) {
        play(level, layered, SoundEvents.GENERIC_BIG_FALL, 3.0F, 0.5F);
        play(level, layered, SoundEvents.ANVIL_LAND, 1.5F, 0.4F);
        ring(level, layered.position(), ground, radius, 48);
        level.sendParticles(ParticleTypes.EXPLOSION, layered.getX(), layered.getY() + 0.3, layered.getZ(), 2, 0.6, 0.1, 0.6,
                0.0);
    }

    /** Each tick of digging in: the sand sprays up around it. */
    static void dig(ServerLevel level, Entity layered, BlockState ground, int tick) {
        if (tick % 5 == 0) {
            play(level, layered, ground.getSoundType().getHitSound(), 2.0F, 0.6F);
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), layered.getX(), layered.getY() + 0.2,
                layered.getZ(), 12, layered.getBbWidth() * 0.6, 0.2, layered.getBbWidth() * 0.6, 0.25);
    }

    /** Gone under the sand. */
    static void submerge(ServerLevel level, Entity layered, BlockState ground) {
        play(level, layered, ground.getSoundType().getBreakSound(), 2.5F, 0.5F);
        play(level, layered, SoundEvents.SKELETON_STEP, 2.0F, 0.4F);
    }

    /**
     * The sand moving above it while it travels under the ground: a bulge as wide as its body (a dense mound of grains
     * and dust of the ground block), with grains spraying up in front of it, sliding sand behind it and a hiss of sand.
     */
    static void trail(ServerLevel level, Vec3 at, Vec3 heading, BlockState ground, int tick) {
        BlockParticleOption grains = new BlockParticleOption(ParticleTypes.BLOCK, ground);
        BlockParticleOption dust = new BlockParticleOption(ParticleTypes.FALLING_DUST, ground);
        // The bulge itself: a low, dense mound of grains of the ground over its back.
        level.sendParticles(grains, at.x, at.y + 0.3, at.z, 40, 0.6, 0.15, 0.6, 0.0);
        level.sendParticles(grains, at.x, at.y + 0.15, at.z, 30, 0.8, 0.12, 0.8, 0.05);
        level.sendParticles(dust, at.x, at.y + 0.5, at.z, 10, 0.9, 0.3, 0.9, 0.0);
        Vec3 bow = at.add(heading.scale(1.3));
        level.sendParticles(grains, bow.x, bow.y + 0.3, bow.z, 14, 0.4, 0.1, 0.4, 0.35);
        Vec3 wake = at.subtract(heading.scale(1.2));
        level.sendParticles(dust, wake.x, wake.y + 0.2, wake.z, 6, 0.6, 0.1, 0.6, 0.0);
        if (tick % 4 == 0) {
            level.playSound(null, at.x, at.y, at.z, SoundEvents.SAND_BREAK, SoundSource.HOSTILE, 1.5F, 0.5F);
        }
        if (tick % 8 == 0) {
            level.playSound(null, at.x, at.y, at.z, SoundEvents.WARDEN_DIG, SoundSource.HOSTILE, 1.0F, 1.2F);
        }
    }

    /**
     * The sand heaves where it will burst up: a ring of churning sand that grows over the warning, grains spurting up
     * higher and higher from the middle, and a rumble.
     */
    static void heave(ServerLevel level, Vec3 at, BlockState ground, int tick, int ticks) {
        BlockParticleOption grains = new BlockParticleOption(ParticleTypes.BLOCK, ground);
        double grow = Math.min(1.0, (tick + 1.0) / Math.max(1, ticks));
        double radius = 0.8 + grow * 1.4;
        for (int i = 0; i < 16; i++) {
            double angle = i * Math.PI / 8.0 + tick * 0.25;
            level.sendParticles(grains, at.x + Math.cos(angle) * radius, at.y + 0.15, at.z + Math.sin(angle) * radius, 2,
                    0.15, 0.05, 0.15, 0.1);
        }
        level.sendParticles(grains, at.x, at.y + 0.2, at.z, 20, 0.5, 0.1, 0.5, 0.2 + grow * 0.5);
        level.sendParticles(grains, at.x, at.y + 0.3 + grow * 0.3, at.z, 24, radius * 0.5, 0.1 + grow * 0.2,
                radius * 0.5, 0.0);
        level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, ground), at.x, at.y + 0.8, at.z, 8,
                radius * 0.6, 0.4, radius * 0.6, 0.0);
        if (tick % 5 == 0) {
            level.playSound(null, at.x, at.y, at.z, SoundEvents.WARDEN_DIG, SoundSource.HOSTILE, 2.0F, 0.7F);
        }
    }

    /** It bursts up out of the sand. */
    static void erupt(ServerLevel level, Entity layered, BlockState ground, double radius) {
        play(level, layered, SoundEvents.WARDEN_EMERGE, 2.0F, 0.9F);
        play(level, layered, SoundEvents.SKELETON_HURT, 2.0F, 0.4F);
        ring(level, layered.position(), ground, radius, 60);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), layered.getX(), layered.getY() + 1.0,
                layered.getZ(), 80, 0.8, 1.2, 0.8, 0.4);
    }

    /** The dead heads rear back: their jaws creak open. */
    static void spitWindup(ServerLevel level, Entity layered) {
        play(level, layered, SoundEvents.SKELETON_AMBIENT, 2.5F, 0.6F);
        play(level, layered, SoundEvents.SKELETON_STEP, 2.0F, 0.3F);
    }

    /** Each tick of rearing back: dust gathering in a dead head's jaws. */
    static void spitGather(ServerLevel level, Vec3 mouth) {
        level.sendParticles(ParticleTypes.WHITE_ASH, mouth.x, mouth.y, mouth.z, 6, 0.4, 0.3, 0.4, 0.0);
        level.sendParticles(ParticleTypes.POOF, mouth.x, mouth.y, mouth.z, 1, 0.15, 0.15, 0.15, 0.0);
    }

    static void spit(ServerLevel level, Vec3 mouth) {
        level.playSound(null, mouth.x, mouth.y, mouth.z, SoundEvents.LLAMA_SPIT, SoundSource.HOSTILE, 2.0F, 0.4F);
        level.playSound(null, mouth.x, mouth.y, mouth.z, SoundEvents.SKELETON_HURT, SoundSource.HOSTILE, 1.0F, 0.6F);
    }

    /** One step of a flying cloud of dust and bone splinters. */
    static void spitCloud(ServerLevel level, Vec3 at, double radius) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState()), at.x, at.y,
                at.z, 5, radius * 0.3, radius * 0.3, radius * 0.3, 0.05);
        level.sendParticles(ParticleTypes.POOF, at.x, at.y, at.z, 4, radius * 0.3, radius * 0.3, radius * 0.3, 0.0);
        level.sendParticles(ParticleTypes.WHITE_ASH, at.x, at.y, at.z, 8, radius * 0.4, radius * 0.4, radius * 0.4, 0.02);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, canyonStone()), at.x, at.y, at.z, 3,
                radius * 0.3, radius * 0.3, radius * 0.3, 0.05);
    }

    /** A cloud bursts on someone or on stone. */
    static void spitBurst(ServerLevel level, Vec3 at) {
        level.playSound(null, at.x, at.y, at.z, SoundEvents.SAND_BREAK, SoundSource.HOSTILE, 1.5F, 0.6F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState()), at.x, at.y,
                at.z, 16, 0.4, 0.4, 0.4, 0.15);
        level.sendParticles(ParticleTypes.WHITE_ASH, at.x, at.y, at.z, 30, 0.6, 0.6, 0.6, 0.05);
        level.sendParticles(ParticleTypes.POOF, at.x, at.y, at.z, 8, 0.4, 0.4, 0.4, 0.02);
    }

    private static void ring(ServerLevel level, Vec3 centre, BlockState ground, double radius, int count) {
        BlockParticleOption particle = new BlockParticleOption(ParticleTypes.BLOCK, ground);
        for (int i = 0; i < count; i++) {
            double angle = i * Math.PI * 2.0 / count;
            level.sendParticles(particle, centre.x + Math.cos(angle) * radius, centre.y + 0.1,
                    centre.z + Math.sin(angle) * radius, 2, 0.1, 0.1, 0.1, 0.2);
        }
    }

    private static BlockState canyonStone() {
        return ModBlocks.ORANGE_CANYON_STONE.get().defaultBlockState();
    }

    private static void play(ServerLevel level, Entity layered, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, layered.getX(), layered.getY(), layered.getZ(), sound, SoundSource.HOSTILE, volume, pitch);
    }

    private static void dust(ServerLevel level, Entity layered, int count) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, canyonStone()), layered.getX(),
                layered.getY() + layered.getBbHeight() * 0.5, layered.getZ(), count, layered.getBbWidth() * 0.8,
                layered.getBbHeight() * 0.5, layered.getBbWidth() * 0.8, 0.1);
    }
}
