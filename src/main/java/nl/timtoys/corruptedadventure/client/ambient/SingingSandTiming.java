package nl.timtoys.corruptedadventure.client.ambient;

/**
 * When the singing sand sounds: pure maths, no Minecraft classes, so a unit test can call it.
 */
final class SingingSandTiming {
    /** Average one hum per ~40 s at night (20 ticks per second). */
    static final float NIGHT_CHANCE = 1.0F / 800.0F;
    /** Average one hum per ~4 minutes by day. */
    static final float DAY_CHANCE = 1.0F / 4800.0F;
    /** No new hum within 12 s of the last, so they never overlap. */
    static final int COOLDOWN_TICKS = 240;

    private SingingSandTiming() {
    }

    /**
     * 0 by day, 1 at night, with a soft dusk and dawn. {@code timeOfDay} is vanilla's celestial angle: 0 at noon, 0.25 at
     * sunset, 0.5 at midnight. The sun counts as down from just before it touches the horizon (sun height -0.2).
     */
    static float nightFactor(float timeOfDay) {
        double sunHeight = Math.cos(timeOfDay * 2.0 * Math.PI);
        double x = Math.min(1.0, Math.max(0.0, (0.2 - sunHeight) / 0.4));
        return (float) (x * x * (3.0 - 2.0 * x));
    }

    /** Chance per tick of a hum, between the day and the night value. */
    static float chancePerTick(float nightFactor) {
        float factor = Math.min(1.0F, Math.max(0.0F, nightFactor));
        return DAY_CHANCE + (NIGHT_CHANCE - DAY_CHANCE) * factor;
    }
}
