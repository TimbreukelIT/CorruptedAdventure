package nl.timtoys.corruptedadventure.condition;

import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;
import nl.timtoys.corruptedadventure.condition.VoidRule.Input;
import nl.timtoys.corruptedadventure.condition.VoidRule.Rates;
import nl.timtoys.corruptedadventure.condition.VoidRule.Result;
import nl.timtoys.corruptedadventure.config.CommonConfig;
import nl.timtoys.corruptedadventure.config.RingSettings;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.Ring5Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import org.junit.jupiter.api.Test;

import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The void rule (package 91): contract §11 numbers, the strongest wins, light keeps the void away, the zone. */
class VoidRuleTest {
    private static final ServerConfig.VoidCondition CONFIG = ServerConfig.VOID;
    /** The defaults of {@code [conditions.void]}, read from the spec. */
    private static final Rates DEFAULTS = new Rates(value(CONFIG.innerRise), value(CONFIG.outerRise),
            value(CONFIG.sourceRise), value(CONFIG.lightFall), value(CONFIG.outsideFall));
    /** Every rate different, so each label is decided by its own rate. */
    private static final Rates DISTINCT = new Rates(0.2f, 0.4f, 0.6f, 2.0f, 0.5f);
    private static final BooleanSupplier NEVER = () -> {
        throw new AssertionError("band asked outside ring 5");
    };

    @Test
    void contractStartValues() {
        assertEquals(new Rates(0.28f, 0.55f, 0.55f, 1.0f, 1.0f), DEFAULTS);
        assertEquals(4, CONFIG.sourceRadius.getDefault());
        assertEquals(12, CONFIG.lightLevel.getDefault());
        assertEquals(1.5, CONFIG.nightMultiplier.getDefault());
    }

    @Test
    void zeroToHundredTakesSixMinutesInsideAndThreeOutsideOrAtASource() {
        assertSeconds(360, VoidRule.evaluate(new Input(VoidZone.INNER, false, false), DEFAULTS));
        assertSeconds(180, VoidRule.evaluate(new Input(VoidZone.OUTER, false, false), DEFAULTS));
        assertSeconds(180, VoidRule.evaluate(new Input(VoidZone.INNER, true, false), DEFAULTS));
    }

    @Test
    void theStrongestWinsAndNothingStacks() {
        assertResult(0.2f, VoidExposure.INNER, VoidRule.evaluate(new Input(VoidZone.INNER, false, false), DISTINCT));
        assertResult(0.4f, VoidExposure.OUTER, VoidRule.evaluate(new Input(VoidZone.OUTER, false, false), DISTINCT));
        assertResult(0.6f, VoidExposure.SOURCE, VoidRule.evaluate(new Input(VoidZone.INNER, true, false), DISTINCT));
        assertResult(0.6f, VoidExposure.SOURCE, VoidRule.evaluate(new Input(VoidZone.OUTER, true, false), DISTINCT));
        // Defaults: the outer band and a source are equally strong; the band keeps the label, no sum.
        assertResult(0.55f, VoidExposure.OUTER, VoidRule.evaluate(new Input(VoidZone.OUTER, true, false), DEFAULTS));
        // A source weaker than the band never slows it down.
        Rates weakSource = new Rates(0.28f, 0.55f, 0.1f, 1.0f, 1.0f);
        assertResult(0.28f, VoidExposure.INNER, VoidRule.evaluate(new Input(VoidZone.INNER, true, false), weakSource));
    }

    @Test
    void blockLightMakesItFallEvenAtASourceAndInTheOuterBand() {
        assertResult(-2.0f, VoidExposure.LIGHT, VoidRule.evaluate(Input.LIT, DISTINCT));
        assertResult(-2.0f, VoidExposure.LIGHT, VoidRule.evaluate(new Input(VoidZone.OUTER, true, true), DISTINCT));
        assertResult(-1.0f, VoidExposure.LIGHT, VoidRule.evaluate(new Input(VoidZone.INNER, true, true), DEFAULTS));
    }

    @Test
    void outsideTheZoneItFallsEvenAtASource() {
        assertResult(-0.5f, VoidExposure.OUTSIDE, VoidRule.evaluate(Input.OUTSIDE, DISTINCT));
        assertResult(-0.5f, VoidExposure.OUTSIDE, VoidRule.evaluate(new Input(VoidZone.OUTSIDE, true, false), DISTINCT));
        assertResult(-1.0f, VoidExposure.OUTSIDE, VoidRule.evaluate(Input.OUTSIDE, DEFAULTS));
    }

    @Test
    void aFallRateOfZeroIsPositiveZero() {
        Result result = VoidRule.evaluate(Input.LIT, new Rates(0.28f, 0.55f, 0.55f, 0.0f, 0.0f));
        assertEquals(0, Float.compare(0.0f, result.rate()));
    }

    @Test
    void theZoneIsOverworldRingFiveAndTheVoidRimSwitchingAtTheMiddle() {
        assertTrue(VoidZone.inZone(Level.OVERWORLD, RingPosition.within(Ring.RING_5)));
        assertTrue(VoidZone.inZone(Level.OVERWORLD, RingPosition.within(Ring.VOID)));
        assertFalse(VoidZone.inZone(Level.NETHER, RingPosition.within(Ring.RING_5)));
        assertFalse(VoidZone.inZone(Level.OVERWORLD, RingPosition.within(Ring.RING_4)));
        // Zone 4→5: the ring 4 side falls.
        assertFalse(VoidZone.inZone(Level.OVERWORLD, new RingPosition(Ring.RING_4, Ring.RING_5, 0.49)));
        assertTrue(VoidZone.inZone(Level.OVERWORLD, new RingPosition(Ring.RING_4, Ring.RING_5, 0.5)));
        assertTrue(VoidZone.inZone(Level.OVERWORLD, new RingPosition(Ring.RING_5, Ring.VOID, 0.49)));
        assertTrue(VoidZone.inZone(Level.OVERWORLD, new RingPosition(Ring.RING_5, Ring.VOID, 0.5)));
    }

    @Test
    void theVoidRimIsOuterBandAndTheBandIsOnlyAskedInRingFive() {
        assertEquals(VoidZone.OUTER, VoidZone.of(Level.OVERWORLD, RingPosition.within(Ring.VOID), NEVER));
        assertEquals(VoidZone.OUTER, VoidZone.of(Level.OVERWORLD, new RingPosition(Ring.RING_5, Ring.VOID, 0.5), NEVER));
        assertEquals(VoidZone.OUTSIDE, VoidZone.of(Level.OVERWORLD, RingPosition.within(Ring.RING_4), NEVER));
        assertEquals(VoidZone.OUTSIDE, VoidZone.of(Level.NETHER, RingPosition.within(Ring.RING_5), NEVER));
        assertEquals(VoidZone.INNER, VoidZone.of(Level.OVERWORLD, RingPosition.within(Ring.RING_5), () -> false));
        assertEquals(VoidZone.OUTER, VoidZone.of(Level.OVERWORLD, RingPosition.within(Ring.RING_5), () -> true));
    }

    @Test
    void theRealBandSplitsRingFive() {
        RingLayout layout = new RingLayout(new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 10200, 96, 128,
                CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES), RingLayout.noiseSeed(20260930L));
        Ring5Band band = Ring5Band.of(layout);
        // Ring 5 runs 8000 - 9700: its inner edge is always inner, its outer edge always outer.
        assertEquals(VoidZone.INNER, zone(layout, band, 8250, 0));
        assertEquals(VoidZone.INNER, zone(layout, band, 0, -8250));
        assertEquals(VoidZone.OUTER, zone(layout, band, 9500, 0));
        assertEquals(VoidZone.OUTER, zone(layout, band, -9500, 0));
        assertEquals(VoidZone.OUTER, zone(layout, band, 10000, 0), "the void rim");
        assertEquals(VoidZone.OUTSIDE, zone(layout, band, 7500, 0), "ring 4");
    }

    private static VoidZone zone(RingLayout layout, Ring5Band band, double x, double z) {
        return VoidZone.of(Level.OVERWORLD, layout.sample(Level.OVERWORLD, x, z), () -> band.outer(x, z));
    }

    private static void assertSeconds(double expected, Result result) {
        double seconds = 100 / result.rate();
        assertTrue(Math.abs(seconds - expected) <= expected * 0.1, "0 -> 100 in " + seconds + " s, expected ~" + expected);
    }

    private static void assertResult(float rate, VoidExposure exposure, Result result) {
        assertEquals(rate, result.rate(), 1.0E-6f);
        assertEquals(exposure, result.exposure());
    }

    private static float value(ModConfigSpec.DoubleValue value) {
        return value.getDefault().floatValue();
    }
}
