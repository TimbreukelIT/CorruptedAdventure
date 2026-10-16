package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.SeaFossilShape;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.SeaFossils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The loose bones and boulders of the dunes (78, N4 = A) keep clear of the fossils' bones, floors and ramps. */
class DuneRubbleFeatureTest {
    @Test
    void clearOfTheFossils() {
        SeaFossilShape.Settings settings = SeaFossilShape.Settings.CODEC.parse(JsonOps.INSTANCE,
                WorldgenData.read("density_function/ring_4/nether/fossils.json").getAsJsonObject("settings")).getOrThrow();
        NormalNoise.NoiseParameters parameters = WorldgenData.noiseParameters("nether_ring_4_fossil_cells");
        SeaFossils fossils = new SeaFossils(DensityFunctions.zero(), DensityFunctions.constant(43.0), DensityFunctions.zero(),
                DensityFunctions.zero(), new DensityFunction.NoiseHolder(Holder.direct(parameters),
                NormalNoise.create(new LegacyRandomSource(3L), parameters)), settings);
        SeaFossilShape.Fossil fossil = fossils.fossilOfCell(2, 2);
        assertNotNull(fossil);
        int x = (int) Math.floor(fossil.x());
        int z = (int) Math.floor(fossil.z());
        assertTrue(DuneRubbleFeature.nearFossil(fossils, x, z), "on the floor inside");
        // just past the ramp at the side of the ribcage: within the clearance of the ramp
        double l = fossil.floorHalfWidth(0.0) + fossil.ramp() + 2.0;
        assertTrue(DuneRubbleFeature.nearFossil(fossils, (int) Math.floor(fossil.x() - l * fossil.sin()),
                (int) Math.floor(fossil.z() + l * fossil.cos())), "next to the ramp");
        double far = fossil.lMax() + fossil.ramp() + 12.0;
        assertFalse(DuneRubbleFeature.nearFossil(fossils, (int) Math.floor(fossil.x() - far * fossil.sin()),
                (int) Math.floor(fossil.z() + far * fossil.cos())), "away from it");
        assertFalse(DuneRubbleFeature.nearFossil(null, x, z), "no fossils in this dimension");
    }
}
