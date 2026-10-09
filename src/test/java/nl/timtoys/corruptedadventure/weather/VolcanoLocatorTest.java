package nl.timtoys.corruptedadventure.weather;

import nl.timtoys.corruptedadventure.network.VolcanoPlumesPayload.Plume;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicField;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicField.VolcanoInfo;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicFieldTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The plumes of the ring 3 terrain on the test seed, read from the same field the overworld generates with. */
class VolcanoLocatorTest {
    private static VolcanicField field;

    @BeforeAll
    static void setUp() {
        field = VolcanicFieldTest.field();
    }

    @Test
    void everyActiveVolcanoGetsAPlumeAndNoDormantOneDoes() {
        // A wide stretch of ring 3 east of the centre, so it holds great, ordinary and dormant volcanoes.
        List<VolcanoInfo> volcanoes = field.volcanoesWithin(4800, 0, 2000);
        List<Plume> plumes = VolcanoLocator.plumes(field, 4800, 0, 2000);
        List<VolcanoInfo> active = volcanoes.stream().filter(volcano -> !volcano.dormant()).toList();

        assertTrue(volcanoes.stream().anyMatch(VolcanoInfo::dormant), "the test area should hold a dormant volcano");
        assertFalse(active.isEmpty());
        assertEquals(active.size(), plumes.size());
        for (int k = 0; k < active.size(); k++) {
            VolcanoInfo volcano = active.get(k);
            assertEquals(new Plume(volcano.x(), volcano.z(), volcano.craterY(), volcano.craterRadius(), volcano.great()),
                    plumes.get(k));
        }
    }

    @Test
    void plumesStayWithinTheRadius() {
        int radius = 512;
        for (Plume plume : VolcanoLocator.plumes(field, 4500, 700, radius)) {
            double dx = plume.x() - 4500;
            double dz = plume.z() - 700;
            assertTrue(dx * dx + dz * dz <= (radius + 1.0) * (radius + 1.0));
        }
    }

    @Test
    void theCentreOfTheWorldHasNoVolcanoes() {
        assertEquals(List.of(), VolcanoLocator.plumes(field, 0, 0, 512));
    }
}
