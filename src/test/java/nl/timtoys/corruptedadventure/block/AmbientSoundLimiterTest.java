package nl.timtoys.corruptedadventure.block;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The cap on block sounds from {@code animateTick}. */
class AmbientSoundLimiterTest {
    @Test
    void allowsOneSoundPerGap() {
        AmbientSoundLimiter limiter = new AmbientSoundLimiter(5);
        assertTrue(limiter.tryAcquire(100, 64.0));
        assertFalse(limiter.tryAcquire(100, 64.0));
        assertFalse(limiter.tryAcquire(104, 100.0));
        assertTrue(limiter.tryAcquire(105, 64.0));
        assertFalse(limiter.tryAcquire(106, 64.0));
    }

    /** A block at less than half the distance of the one that played may still play; that one then sets the bar. */
    @Test
    void aClearlyNearerBlockCutsIn() {
        AmbientSoundLimiter limiter = new AmbientSoundLimiter(10);
        assertTrue(limiter.tryAcquire(100, 400.0));
        assertFalse(limiter.tryAcquire(101, 100.0));
        assertTrue(limiter.tryAcquire(101, 99.0));
        assertFalse(limiter.tryAcquire(102, 25.0));
        assertTrue(limiter.tryAcquire(102, 4.0));
        assertFalse(limiter.tryAcquire(103, 1.0));
    }

    /** Joining another world resets the game time; the limiter must not stay shut until the old time comes back. */
    @Test
    void gameTimeJumpingBackFreesTheLimiter() {
        AmbientSoundLimiter limiter = new AmbientSoundLimiter(5);
        assertTrue(limiter.tryAcquire(1_000_000, 64.0));
        assertTrue(limiter.tryAcquire(20, 64.0));
        assertFalse(limiter.tryAcquire(21, 64.0));
    }
}
