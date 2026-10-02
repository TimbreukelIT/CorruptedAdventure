package nl.timtoys.corruptedadventure.condition;

import net.minecraft.world.level.dimension.DimensionType;

/**
 * Whether the sun is above the horizon, from the time of day only. Unlike vanilla's {@code Level.isDay()} rain and
 * thunder do not count: vanilla rain is global, so it would also darken ring 2, which has no precipitation.
 */
public final class Sun {
    private Sun() {
    }

    /**
     * Uses vanilla's sun angle ({@link DimensionType#timeOfDay}: 0 = noon, 0.25 = setting, 0.5 = midnight, 0.75 =
     * rising), so the switch matches the sun on screen: up from day time 23,215 to 12,785. A dimension with a fixed
     * time (the Nether: 18.000) never has the sun up.
     */
    public static boolean isUp(DimensionType type, long dayTime) {
        float angle = type.timeOfDay(dayTime);
        return angle < 0.25f || angle > 0.75f;
    }
}
