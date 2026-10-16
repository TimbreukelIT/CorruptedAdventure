package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.data.worldgen.placement.OrePlacements;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.worldgen.biome.Ring4BiomeModifiers;
import nl.timtoys.corruptedadventure.worldgen.biome.Ring4Biomes;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Package 77: the ore list of ring 4 and the shape of the sunken aquamarine geodes. */
class Ring4OreFeaturesTest {
    /** Contract §4: coal, iron, copper ×0.2; gold, redstone, lapis ×0.15; diamond ×0.1. */
    @Test
    void vanillasOresAreTaperedAsDecided() {
        assertEquals(0.2, Ring4OreFeatures.RING_4_FACTORS.get(OrePlacements.ORE_COAL_UPPER));
        assertEquals(0.2, Ring4OreFeatures.RING_4_FACTORS.get(OrePlacements.ORE_COPPER));
        assertEquals(0.15, Ring4OreFeatures.RING_4_FACTORS.get(OrePlacements.ORE_GOLD));
        assertEquals(0.15, Ring4OreFeatures.RING_4_FACTORS.get(OrePlacements.ORE_LAPIS_BURIED));
        assertEquals(0.1, Ring4OreFeatures.RING_4_FACTORS.get(OrePlacements.ORE_DIAMOND_LARGE));
        assertEquals(RingVanillaOres.ORES.size(), Ring4OreFeatures.RING_4_FACTORS.size());
    }

    /** One modifier: vanilla's tapered ores in vanilla's order, then tidesilver and jade; not in verdant_falls. */
    @Test
    void theModifierAddsTheRingsOresOutsideVerdantFalls() {
        List<?> ores = Ring4BiomeModifiers.ores();
        assertEquals(RingVanillaOres.keys(Ring.RING_4), ores.subList(0, RingVanillaOres.ORES.size()));
        assertEquals(Ring4OreFeatures.OWN, ores.subList(RingVanillaOres.ORES.size(), ores.size()));
        assertTrue(Ring4BiomeModifiers.oreBiomes().containsAll(Ring4Biomes.ALL));
        assertTrue(Ring4BiomeModifiers.oreBiomes().contains(ModBiomes.SUNDERED_REACH));
        assertFalse(Ring4BiomeModifiers.oreBiomes().contains(ModBiomes.VERDANT_FALLS), "no ores open the exact walls");
    }

    /** A hollow, then the crystal, middle and outer layers, one block each; nothing beyond. */
    @Test
    void aGeodeHasAHollowAndThreeLayers() {
        int hollow = 3;
        assertEquals(SunkenGeodeFeature.Part.HOLLOW, SunkenGeodeFeature.part(0, hollow));
        assertEquals(SunkenGeodeFeature.Part.HOLLOW, SunkenGeodeFeature.part(9, hollow));
        assertEquals(SunkenGeodeFeature.Part.CRYSTAL, SunkenGeodeFeature.part(16, hollow));
        assertEquals(SunkenGeodeFeature.Part.MIDDLE, SunkenGeodeFeature.part(25, hollow));
        assertEquals(SunkenGeodeFeature.Part.OUTER, SunkenGeodeFeature.part(36, hollow));
        assertNull(SunkenGeodeFeature.part(49, hollow));
        // Every hollow block is wrapped: its neighbours inside the sphere are hollow or crystal, never a later layer.
        for (int x = -6; x <= 6; x++) {
            for (int y = -6; y <= 6; y++) {
                for (int z = -6; z <= 6; z++) {
                    if (SunkenGeodeFeature.part(x * x + y * y + z * z, hollow) != SunkenGeodeFeature.Part.HOLLOW) {
                        continue;
                    }
                    for (int[] d : new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}}) {
                        int nx = x + d[0];
                        int ny = y + d[1];
                        int nz = z + d[2];
                        SunkenGeodeFeature.Part next = SunkenGeodeFeature.part(nx * nx + ny * ny + nz * nz, hollow);
                        assertTrue(Set.of(SunkenGeodeFeature.Part.HOLLOW, SunkenGeodeFeature.Part.CRYSTAL).contains(next), x + "," + y + "," + z);
                    }
                }
            }
        }
        assertTrue(SunkenGeodeFeature.MAX_HOLLOW + SunkenGeodeFeature.LAYERS <= 7, "fits the jittered grid's margin");
    }
}
