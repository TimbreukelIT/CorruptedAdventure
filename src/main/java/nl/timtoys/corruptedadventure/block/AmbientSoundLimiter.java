package nl.timtoys.corruptedadventure.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Caps how often a client plays one kind of block sound from {@code animateTick}: a field of hundreds of boiling mud
 * blocks or tens of fumaroles would otherwise drone. One instance per sound, shared by every block of that kind; only
 * used on the client thread ({@code animateTick}), and holds no client classes.
 *
 * <p>The slot goes to whichever block rolls first, so a block clearly nearer to the player (less than half the distance
 * of the one that played) may still play within the gap: the vent at your feet is not drowned out by far ones. Each
 * such cut-in halves the distance, so a gap holds only a few sounds.
 */
final class AmbientSoundLimiter {
    private final int minGapTicks;
    private long nextAllowed = Long.MIN_VALUE;
    private double playedDistanceSqr = Double.MAX_VALUE;

    AmbientSoundLimiter(int minGapTicks) {
        this.minGapTicks = minGapTicks;
    }

    /** Whether the block at {@code pos} may play the sound now; measured to the nearest player (on a client mostly you). */
    boolean tryAcquire(Level level, BlockPos pos) {
        Player player = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, -1.0, false);
        return player != null && tryAcquire(level.getGameTime(), player.distanceToSqr(pos.getCenter()));
    }

    /**
     * Whether a sound at {@code distanceSqr} from the player may play at {@code gameTime}; if so, the gap starts again.
     * A game time that jumped back (another world or server) frees the limiter at once.
     */
    boolean tryAcquire(long gameTime, double distanceSqr) {
        boolean inGap = gameTime < nextAllowed && gameTime >= nextAllowed - minGapTicks;
        if (inGap && distanceSqr * 4 >= playedDistanceSqr) {
            return false;
        }
        nextAllowed = gameTime + minGapTicks;
        playedDistanceSqr = distanceSqr;
        return true;
    }
}
