package nl.timtoys.corruptedadventure.block;

/**
 * The budget of a burning swamp gas chain in one level (decision Q9 = A, golf 2): at most {@link #MAX_BURNS_PER_TICK}
 * gas blocks burn per game tick (the rest waits a tick), every {@link #BLOCKS_PER_EXPLOSION}th burning block is the
 * start of a cluster and explodes, but at most {@link #MAX_EXPLOSIONS_PER_TICK} per tick; the other cluster starts only
 * flare up with the ignite sound, all other blocks burn off as flames. The count of a fire starts again after a quiet
 * gap, so a fresh fire always starts with a bang (even a bubble of a few blocks). Pure bookkeeping, server thread only.
 */
final class SwampGasBurn {
    static final int MAX_BURNS_PER_TICK = 64;
    static final int BLOCKS_PER_EXPLOSION = 8;
    static final int MAX_EXPLOSIONS_PER_TICK = 4;
    /** Game ticks without any burning after which the next burn starts a new fire. */
    static final int QUIET_GAP = 40;

    /** What a gas block that is due to burn does now. */
    enum Outcome {
        /** The budget of this tick is spent: stay lit, try again next tick. */
        WAIT,
        /** Burn with a real explosion. */
        EXPLODE,
        /** Burn as flames with the ignite sound (a cluster start beyond the explosion cap). */
        FLARE,
        /** Burn as flames only. */
        FLAME
    }

    private long tick = Long.MIN_VALUE;
    private long lastBurn = Long.MIN_VALUE;
    private int burnsThisTick;
    private int explosionsThisTick;
    private long burnsThisFire;

    Outcome next(long gameTime) {
        if (gameTime != tick) {
            tick = gameTime;
            burnsThisTick = 0;
            explosionsThisTick = 0;
        }
        if (burnsThisTick >= MAX_BURNS_PER_TICK) {
            return Outcome.WAIT;
        }
        // A game time that jumped back (another world) also starts a new fire.
        if (lastBurn == Long.MIN_VALUE || gameTime - lastBurn > QUIET_GAP || gameTime < lastBurn) {
            burnsThisFire = 0;
        }
        lastBurn = gameTime;
        burnsThisTick++;
        boolean clusterStart = burnsThisFire++ % BLOCKS_PER_EXPLOSION == 0;
        if (!clusterStart) {
            return Outcome.FLAME;
        }
        if (explosionsThisTick < MAX_EXPLOSIONS_PER_TICK) {
            explosionsThisTick++;
            return Outcome.EXPLODE;
        }
        return Outcome.FLARE;
    }
}
