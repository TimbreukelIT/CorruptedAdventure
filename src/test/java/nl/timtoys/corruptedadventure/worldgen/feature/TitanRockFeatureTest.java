package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.HolderSet;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The pure part of the titan rocks (change 47b): one rock per cell at most, a function of seed and cell only. */
class TitanRockFeatureTest {
    private static final TitanRockConfiguration CONFIG = new TitanRockConfiguration(
            List.of(CorruptedAdventure.id("a"), CorruptedAdventure.id("b"), CorruptedAdventure.id("c")), 256, 0.4F,
            ModBiomes.TITAN_FIELDS, 2, HolderSet.empty());

    @Test
    void rocksArePureAndStayInsideTheirCell() {
        int rocks = 0;
        for (int i = -20; i < 20; i++) {
            for (int j = -20; j < 20; j++) {
                TitanRockFeature.Rock rock = TitanRockFeature.rock(20260930L, i, j, CONFIG);
                assertEquals(rock, TitanRockFeature.rock(20260930L, i, j, CONFIG));
                if (rock == null) {
                    continue;
                }
                rocks++;
                // Its whole reach stays inside the cell, so two rocks never overlap.
                assertTrue(rock.x() - TitanRockFeature.MAX_REACH > i * 256 && rock.x() + TitanRockFeature.MAX_REACH < (i + 1) * 256, "x " + rock);
                assertTrue(rock.z() - TitanRockFeature.MAX_REACH > j * 256 && rock.z() + TitanRockFeature.MAX_REACH < (j + 1) * 256, "z " + rock);
                assertTrue(rock.template() >= 0 && rock.template() < 3);
            }
        }
        assertTrue(rocks > 560 && rocks < 720, "about 4 in 10 cells: " + rocks);
        TitanRockConfiguration always = new TitanRockConfiguration(CONFIG.templates(), 256, 1.0F, ModBiomes.TITAN_FIELDS, 2, HolderSet.empty());
        assertNotEquals(TitanRockFeature.rock(1L, 0, 0, always), TitanRockFeature.rock(2L, 0, 0, always), "another seed, another rock");
    }
}
