package nl.timtoys.corruptedadventure.entity.layered;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

/**
 * The Layered's sounds and particles, sent by the server to everyone near (vanilla events with a low pitch;
 * placeholders until its own sounds, backlog). Bone rattles and canyon dust: it is a thing of bone and stone.
 */
final class LayeredEffects {
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

    private static void play(ServerLevel level, Entity layered, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, layered.getX(), layered.getY(), layered.getZ(), sound, SoundSource.HOSTILE, volume, pitch);
    }

    private static void dust(ServerLevel level, Entity layered, int count) {
        BlockState stone = ModBlocks.ORANGE_CANYON_STONE.get().defaultBlockState();
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, stone), layered.getX(),
                layered.getY() + layered.getBbHeight() * 0.5, layered.getZ(), count, layered.getBbWidth() * 0.8,
                layered.getBbHeight() * 0.5, layered.getBbWidth() * 0.8, 0.1);
    }
}
