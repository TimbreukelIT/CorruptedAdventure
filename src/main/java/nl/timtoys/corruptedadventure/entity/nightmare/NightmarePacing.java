package nl.timtoys.corruptedadventure.entity.nightmare;

import net.minecraft.util.RandomSource;

/**
 * The tempo of a fight (fase-4-nightmares-concept.md "Leven-ronde"): a burst of attacks, then a rest window in which it
 * starts no attack and backs off or circles its target. Pure; {@link Nightmare} counts and ticks it.
 */
public final class NightmarePacing {
    /** How a rest window moves: away from the target to the rest distance, or around it. */
    public enum Style {
        BACK_OFF,
        CIRCLE
    }

    /** Attacks left in this burst; 0: a new burst is drawn at the next attack. */
    private int attacksLeft;
    private int restTicks;
    private Style style = Style.CIRCLE;
    private boolean clockwise;

    boolean isResting() {
        return restTicks > 0;
    }

    Style style() {
        return style;
    }

    boolean clockwise() {
        return clockwise;
    }

    /**
     * One attack started. The last of a burst (drawn between {@code burstMin} and {@code burstMax}) starts a rest window
     * of {@code restTicks} with a drawn style; true then.
     */
    boolean count(RandomSource random, int burstMin, int burstMax, int restTicks, double backOffChance) {
        if (attacksLeft <= 0) {
            int lo = Math.max(1, burstMin);
            attacksLeft = lo + random.nextInt(Math.max(lo, burstMax) - lo + 1);
        }
        if (--attacksLeft > 0 || restTicks <= 0) {
            return false;
        }
        this.restTicks = restTicks;
        style = random.nextDouble() < backOffChance ? Style.BACK_OFF : Style.CIRCLE;
        clockwise = random.nextBoolean();
        return true;
    }

    /** One tick of the rest window down; true at the tick it ends. */
    boolean tick() {
        return restTicks > 0 && --restTicks == 0;
    }

    /** No fight (no target): a fresh burst next time, no rest. */
    void reset() {
        attacksLeft = 0;
        restTicks = 0;
    }
}
