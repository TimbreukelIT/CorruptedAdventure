package nl.timtoys.corruptedadventure.entity.nightmare;

import java.util.ArrayDeque;

/**
 * The bookkeeping of a stagger (fase-4-nightmares-concept.md "Leven-ronde"): the damage of the last few seconds, the
 * stagger's ticks left and the cooldown after it. Pure; {@link Nightmare} applies it.
 */
final class NightmareStagger {
    private record Hit(int tick, float amount) {
    }

    private final ArrayDeque<Hit> hits = new ArrayDeque<>();
    private float windowDamage;
    private int ticksLeft;
    /** The tick from which a new stagger may start. */
    private int readyTick;

    boolean isStaggered() {
        return ticksLeft > 0;
    }

    boolean isReady(int tick) {
        return ticksLeft == 0 && tick >= readyTick;
    }

    /**
     * Adds a hit; true when the damage within {@code windowTicks} reaches {@code threshold} and a stagger may start.
     * While staggered or cooling down hits do not add up, so the next stagger needs fresh damage.
     */
    boolean addDamage(int tick, float amount, float threshold, int windowTicks) {
        if (!isReady(tick)) {
            return false;
        }
        while (!hits.isEmpty() && hits.peekFirst().tick() <= tick - windowTicks) {
            windowDamage -= hits.removeFirst().amount();
        }
        if (hits.isEmpty()) {
            windowDamage = 0.0F;
        }
        hits.addLast(new Hit(tick, amount));
        windowDamage += amount;
        return threshold > 0.0F && windowDamage >= threshold;
    }

    /** Starts a stagger of {@code ticks}; after it the next one waits {@code cooldownTicks}. */
    void start(int tick, int ticks, int cooldownTicks) {
        ticksLeft = ticks;
        readyTick = tick + ticks + cooldownTicks;
        hits.clear();
        windowDamage = 0.0F;
    }

    /** One tick down; true at the tick the stagger ends. */
    boolean tick() {
        return ticksLeft > 0 && --ticksLeft == 0;
    }

    /** Ends it at once (the cooldown stays). */
    void end() {
        ticksLeft = 0;
    }
}
