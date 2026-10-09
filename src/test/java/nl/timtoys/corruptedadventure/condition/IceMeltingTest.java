package nl.timtoys.corruptedadventure.condition;

import nl.timtoys.corruptedadventure.condition.HeatRule.Input;
import nl.timtoys.corruptedadventure.condition.HeatRule.Rates;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The ice rule of package 55b: the heat rule's verdict for a block, with its exceptions. */
class IceMeltingTest {
    private static final Rates DEFAULTS = new Rates(0.55f, 1.5f, 2.0f, 0.5f, 0.25f, 0.5f, 2.0f, 2.0f, 0.5f);

    @Test
    void meltsInTheSunOrNearALitSource() {
        assertTrue(IceMelting.melts(new Input(true, true, true, false, false, false, false, false), DEFAULTS));
        // At night or under a roof, a source still melts it.
        assertTrue(IceMelting.melts(new Input(true, false, false, true, true, false, false, false), DEFAULTS));
    }

    @Test
    void staysAtNightInTheShadeAndUnderARoofWithoutASource() {
        assertFalse(IceMelting.melts(new Input(true, false, false, false, false, false, false, false), DEFAULTS));
        assertFalse(IceMelting.melts(new Input(true, true, false, false, false, false, false, false), DEFAULTS));
        assertFalse(IceMelting.melts(new Input(true, true, false, true, false, false, false, false), DEFAULTS));
    }

    /** The heat meter's exceptions hold for ice too: a refuge biome or water cools, also in the sun and near a source. */
    @Test
    void refugeAndWaterKeepTheIce() {
        assertFalse(IceMelting.melts(new Input(true, true, true, false, true, false, false, true), DEFAULTS));
        assertFalse(IceMelting.melts(new Input(true, true, true, false, true, false, true, false), DEFAULTS));
    }

    @Test
    void outsideTheZoneNothingMelts() {
        assertFalse(IceMelting.melts(new Input(false, true, true, false, true, false, false, false), DEFAULTS));
    }

    /** Where the heat meter cannot rise (a rise of 0 in the config), ice does not melt by heat either. */
    @Test
    void noRiseNoMelting() {
        Rates noRise = new Rates(0.0f, 1.5f, 2.0f, 0.5f, 0.25f, 0.5f, 2.0f, 2.0f, 0.5f);
        assertFalse(IceMelting.melts(new Input(true, true, true, false, true, false, false, false), noRise));
    }
}
