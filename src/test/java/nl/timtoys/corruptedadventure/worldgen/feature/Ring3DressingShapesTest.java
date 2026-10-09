package nl.timtoys.corruptedadventure.worldgen.feature;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.level.block.SnowLayerBlock;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicOutput;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The pure parts of the ring 3 dressing (change 47): filters, drifts and shapes. */
class Ring3DressingShapesTest {
    @Test
    void groundOnlyKeepsTheSurfaceOnTheTerrain() {
        // The surface is the first open block; the terrain height lies between the top block and that block.
        assertTrue(VolcanicFilter.onGround(101, 100.4, 3));
        assertTrue(VolcanicFilter.onGround(104, 100.4, 3));
        assertFalse(VolcanicFilter.onGround(160, 100.4, 3), "a needle or island top far above the terrain");
        assertFalse(VolcanicFilter.onGround(80, 100.4, 3), "a bridge's underside, an overhang");
    }

    @Test
    void rangesNeedPerColumnOutputs() {
        assertTrue(VolcanicFilter.Range.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"output\":\"lava_bank\",\"max\":0.1}")).result().isPresent());
        assertFalse(VolcanicFilter.Range.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"output\":\"thin\",\"max\":0.1}")).result().isPresent(), "3D output");
        assertFalse(VolcanicFilter.Range.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"output\":\"cliff\",\"min\":0.5,\"max\":0.5}")).result().isPresent(), "empty range");
        VolcanicFilter ground = VolcanicFilter.builder().ground().build();
        String json = VolcanicFilter.CODEC.codec().encodeStart(JsonOps.INSTANCE, ground).getOrThrow().toString();
        for (VolcanicOutput.Output output : new VolcanicOutput.Output[]{VolcanicOutput.Output.LAVA_BANK, VolcanicOutput.Output.CLIFF,
                VolcanicOutput.Output.VOLCANO_RISE, VolcanicOutput.Output.SOLID_KIND, VolcanicOutput.Output.CALDERA_MASK}) {
            assertTrue(json.contains(output.getSerializedName()), json);
        }
        assertTrue(json.contains("\"ground_only\":true"), json);
    }

    @Test
    void stepsGetARampOfLayersAgainstTheirRiser() {
        // A plain at 100 with a riser to 101 at x = 3 and beyond.
        AshDriftFeature.Heights step = (x, z) -> x >= 3 ? 101 : 100;
        assertEquals(6, AshDriftFeature.step(2, 0, 100, step));
        assertEquals(4, AshDriftFeature.step(1, 0, 100, step));
        assertEquals(2, AshDriftFeature.step(0, 0, 100, step));
        assertEquals(0, AshDriftFeature.step(-1, 0, 100, step));
        // A wall two high is no step; a riser beyond a dip is none either.
        assertEquals(0, AshDriftFeature.step(2, 0, 100, (x, z) -> x >= 3 ? 102 : 100));
        assertEquals(0, AshDriftFeature.step(0, 0, 100, (x, z) -> x == 1 ? 99 : x >= 2 ? 101 : 100));
    }

    @Test
    void driftsPileUpInTheLeeAndInGullies() {
        AshDriftConfiguration.BiomeAmount full = new AshDriftConfiguration.BiomeAmount(ModBiomes.ASHEN_WASTES, 1.0F, false, true);
        AshDriftConfiguration.BiomeAmount none = new AshDriftConfiguration.BiomeAmount(ModBiomes.ASHEN_WASTES, 0.0F, false, false);
        AshDriftFeature.Heights flat = (x, z) -> 100;
        // Wind along +x: the lee lies downwind of higher ground.
        AshDriftFeature.Heights lee = (x, z) -> x < 0 ? 104 : 100;
        AshDriftFeature.Heights gully = (x, z) -> Math.abs(x) >= 3 ? 104 : 100;
        int open = AshDriftFeature.layers(full, 0.0, 0, 0, 1.0, 0.0, flat);
        assertTrue(AshDriftFeature.layers(full, 0.0, 0, 0, 1.0, 0.0, lee) > open);
        assertTrue(AshDriftFeature.layers(full, 0.0, 0, 0, 1.0, 0.0, gully) > open);
        assertEquals(0, AshDriftFeature.layers(none, 0.0, 0, 0, 1.0, 0.0, lee));
        assertTrue(AshDriftFeature.layers(full, 1.0, 0, 0, 1.0, 0.0, gully) <= SnowLayerBlock.MAX_HEIGHT);
        assertEquals(0, AshDriftFeature.layers(full, -1.0, 0, 0, 1.0, 0.0, flat), "bare where the noise is low");
    }

    @Test
    void patchesAreStretchedAndRagged() {
        float[] round = {1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F};
        assertTrue(GroundPatchFeature.distance(0, 0, 3, 1.0F, 0.0F, round) <= 1.0F);
        assertTrue(GroundPatchFeature.distance(3, 0, 3, 1.0F, 0.0F, round) <= 1.0F);
        assertTrue(GroundPatchFeature.distance(4, 0, 3, 1.0F, 0.0F, round) > 1.0F);
        // Stretched 2.5 along x: 7 blocks out along the length, still 3 across.
        assertTrue(GroundPatchFeature.distance(7, 0, 3, 2.5F, 0.0F, round) <= 1.0F);
        assertTrue(GroundPatchFeature.distance(0, 4, 3, 2.5F, 0.0F, round) > 1.0F);
    }

    @Test
    void hornitosNarrowUpwards() {
        for (int height = 2; height <= HornitoFeature.MAX_HEIGHT; height++) {
            assertEquals(height, HornitoFeature.layers(0, 0, height, 0.0F), "the throat reaches the top");
            int previous = height;
            for (int d = 1; d <= HornitoFeature.MAX_REACH + 1; d++) {
                int layers = HornitoFeature.layers(d, 0, height, 0.6F);
                assertTrue(layers <= previous, "narrower further out");
                previous = layers;
            }
            assertEquals(0, HornitoFeature.layers(HornitoFeature.MAX_REACH + 1, 0, height, 0.6F));
        }
    }

    @Test
    void impactCratersAreShallowBowls() {
        assertEquals(2, ImpactCraterFeature.depth(0, 0, 3));
        assertEquals(1, ImpactCraterFeature.depth(2, 1, 3));
        assertEquals(0, ImpactCraterFeature.depth(4, 0, 3));
    }
}
