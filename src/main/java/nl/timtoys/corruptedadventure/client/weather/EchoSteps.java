package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.util.RandomSource;

/**
 * When the echo footsteps of ring 5 come (package 98, V98-6 A): in bursts. After {@link #MIN_WALK}–{@link #MAX_WALK}
 * ticks of walking in ring 5 (1–3 min) a burst starts: each of the next {@link #MIN_STEPS}–{@link #MAX_STEPS} steps of
 * the player sounds again {@link #MIN_DELAY}–{@link #MAX_DELAY} ticks later (0.4–0.8 s, fixed per burst), from
 * {@link #MIN_BEHIND}–{@link #MAX_BEHIND} blocks behind (fixed per burst: the one following you stays at his distance).
 * If the player stops before the burst is over, exactly one more echo falls after the stop: one step too many. Pure state,
 * one instance per client ({@link EchoFootsteps}); client randomness, nothing synced.
 */
final class EchoSteps {
    static final int MIN_WALK = 1200;
    static final int MAX_WALK = 3600;
    static final int MIN_STEPS = 4;
    static final int MAX_STEPS = 10;
    static final int MIN_DELAY = 8;
    static final int MAX_DELAY = 16;
    static final double MIN_BEHIND = 6.0;
    static final double MAX_BEHIND = 12.0;
    /** How loud an echo sounds next to the player's own step (after the distance). */
    static final float MIN_LOUDNESS = 0.5F;
    static final float MAX_LOUDNESS = 0.7F;
    /** A little lower than the player's own step. */
    static final float PITCH = 0.95F;
    /**
     * Ticks without moving (and without a step) after which the player counts as standing still. Movement, not the gap
     * between steps: sneaking or soul sand spaces the steps further apart than this.
     */
    static final int STOPPED_AFTER = 12;
    /** The gap between the last echo and the extra one: the player's own pace, clamped to this range. */
    static final int MIN_GAP = 5;
    static final int MAX_GAP = 12;
    /** The gap when the pace is unknown (only one step in the burst): vanilla's walking pace. */
    static final int DEFAULT_GAP = 8;
    /** Vanilla's attenuation range of a sound up to volume 1 (blocks): its gain falls linearly to 0 there. */
    static final double SOUND_RANGE = 16.0;

    static final long NONE = Long.MIN_VALUE;

    private int walkNeeded = -1;
    private int walked;
    private boolean burst;
    private int stepsLeft;
    private int delay;
    private double behind;
    private float loudness;
    private long lastStep = NONE;
    private long previousStep = NONE;
    private long lastMove = NONE;

    /**
     * One client tick in ring 5.
     *
     * @param walking whether the player walks this tick (on the ground, moving)
     * @return the tick of the one extra echo after a stop, or {@link #NONE}
     */
    long tick(long now, boolean walking, RandomSource random) {
        if (walking) {
            lastMove = now;
        }
        if (!burst) {
            if (walking) {
                if (walkNeeded < 0) {
                    walkNeeded = MIN_WALK + random.nextInt(MAX_WALK - MIN_WALK + 1);
                }
                if (++walked >= walkNeeded) {
                    start(random);
                }
            }
            return NONE;
        }
        if (lastStep != NONE && now - Math.max(lastStep, lastMove) >= STOPPED_AFTER) {
            long gap = previousStep == NONE ? DEFAULT_GAP : Math.max(MIN_GAP, Math.min(MAX_GAP, lastStep - previousStep));
            long extra = lastStep + delay + gap;
            end();
            return extra;
        }
        return NONE;
    }

    /** A step of the player at {@code now}: the tick its echo is due, or {@link #NONE} outside a burst. */
    long step(long now) {
        if (!burst) {
            return NONE;
        }
        previousStep = lastStep;
        lastStep = now;
        long due = now + delay;
        if (--stepsLeft <= 0) {
            end();
        }
        return due;
    }

    /** Outside ring 5: no burst, and the walk counts again from 0. */
    void reset() {
        end();
    }

    private void start(RandomSource random) {
        burst = true;
        stepsLeft = MIN_STEPS + random.nextInt(MAX_STEPS - MIN_STEPS + 1);
        delay = MIN_DELAY + random.nextInt(MAX_DELAY - MIN_DELAY + 1);
        behind = MIN_BEHIND + random.nextDouble() * (MAX_BEHIND - MIN_BEHIND);
        loudness = MIN_LOUDNESS + random.nextFloat() * (MAX_LOUDNESS - MIN_LOUDNESS);
        lastStep = NONE;
        previousStep = NONE;
    }

    private void end() {
        burst = false;
        walked = 0;
        walkNeeded = -1;
        lastStep = NONE;
        previousStep = NONE;
    }

    boolean inBurst() {
        return burst;
    }

    /** How far behind the player this burst's echoes sound (blocks). */
    double behind() {
        return behind;
    }

    /**
     * The volume of an echo of a step of {@code stepVolume}: {@link #loudness} of it as heard, so raised against the
     * linear fall-off over {@link #SOUND_RANGE}; at most 1 (beyond that vanilla would widen the range).
     */
    float volume(float stepVolume) {
        return echoVolume(stepVolume, loudness, behind);
    }

    /** Pure, for tests. */
    static float echoVolume(float stepVolume, float loudness, double behind) {
        double heard = Math.max(0.25, 1.0 - behind / SOUND_RANGE);
        return (float) Math.min(1.0, stepVolume * loudness / heard);
    }
}
