package nl.timtoys.corruptedadventure.condition;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.dimension.DimensionType;
import org.junit.jupiter.api.Test;

import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SunTest {
    /** As vanilla's overworld type ({@code DimensionTypes.bootstrap}); only the fixed time matters here. */
    private static final DimensionType OVERWORLD = type(OptionalLong.empty());

    @Test
    void sunUpFollowsTheTimeOfDay() {
        assertSun(true, 0);
        assertSun(true, 6000);
        assertSun(true, 12000);
        assertSun(false, 13000);
        assertSun(false, 18000);
        assertSun(true, 23500);
    }

    @Test
    void switchesAtVanillaSunsetAndSunrise() {
        // Sun angle 0.25 (setting) and 0.75 (rising) lie between these ticks.
        assertSun(true, 12780);
        assertSun(false, 12830);
        assertSun(false, 23190);
        assertSun(true, 23240);
    }

    @Test
    void laterDaysRepeat() {
        for (long day = 1; day <= 3; day++) {
            for (long time : new long[]{0, 6000, 12000, 13000, 18000, 23500}) {
                assertEquals(Sun.isUp(OVERWORLD, time), Sun.isUp(OVERWORLD, day * 24000 + time),
                        "day " + day + ", time " + time);
            }
        }
    }

    @Test
    void fixedNightNeverHasSun() {
        DimensionType nether = type(OptionalLong.of(18000));
        for (long time = 0; time < 24000; time += 500) {
            assertFalse(Sun.isUp(nether, time));
        }
    }

    private static void assertSun(boolean up, long dayTime) {
        assertEquals(up, Sun.isUp(OVERWORLD, dayTime), "day time " + dayTime);
    }

    private static DimensionType type(OptionalLong fixedTime) {
        return new DimensionType(fixedTime, true, false, false, true, 1.0, true, false, -64, 384, 384,
                BlockTags.INFINIBURN_OVERWORLD, ResourceLocation.withDefaultNamespace("overworld"), 0.0f,
                new DimensionType.MonsterSettings(false, true, UniformInt.of(0, 7), 0));
    }
}
