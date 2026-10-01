package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadialFalloffTest {
    private final RadialFalloff falloff = new RadialFalloff(300.0, 450.0);

    @Test
    void oneUpToTheInnerRadius() {
        assertEquals(1.0, falloff.valueAt(0.0));
        assertEquals(1.0, falloff.valueAt(299.0));
        assertEquals(1.0, falloff.valueAt(300.0));
    }

    @Test
    void zeroFromTheOuterRadius() {
        assertEquals(0.0, falloff.valueAt(450.0));
        assertEquals(0.0, falloff.valueAt(10_000.0));
    }

    @Test
    void halfwayIsHalf() {
        assertEquals(0.5, falloff.valueAt(375.0), 1e-9);
    }

    @Test
    void decreasesMonotonically() {
        for (double d = 300.0; d < 450.0; d += 1.0) {
            assertTrue(falloff.valueAt(d + 1.0) < falloff.valueAt(d), "not decreasing at " + d);
        }
    }

    @Test
    void computeUsesTheHorizontalDistanceToTheCentre() {
        // 3-4-5 triangle: (225, 300) lies 375 blocks from 0,0; y does not matter.
        assertEquals(0.5, falloff.compute(new DensityFunction.SinglePointContext(225, -64, -300)), 1e-9);
        assertEquals(1.0, falloff.compute(new DensityFunction.SinglePointContext(0, 300, 0)));
    }

    @Test
    void codecReadsBothRadii() {
        assertEquals(falloff, decode(300.0, 450.0).getOrThrow());
    }

    @Test
    void codecRejectsInnerNotBelowOuter() {
        assertTrue(decode(450.0, 450.0).isError());
        assertTrue(decode(500.0, 450.0).isError());
    }

    @Test
    void codecRejectsNegativeRadii() {
        assertTrue(decode(-1.0, 450.0).isError());
        assertTrue(decode(0.0, -1.0).isError());
    }

    private static DataResult<RadialFalloff> decode(double inner, double outer) {
        JsonObject json = new JsonObject();
        json.addProperty("inner_radius", inner);
        json.addProperty("outer_radius", outer);
        return RadialFalloff.MAP_CODEC.codec().parse(JsonOps.INSTANCE, json);
    }
}
