package nl.timtoys.corruptedadventure.worldgen.feature;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicField;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicOutput;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The pure parts of the ring 3 dressing (41b, 47, 47c, 49): filters, landmarks, drifts, shapes, trails, falls, buds. */
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
                VolcanicOutput.Output.VOLCANO_RISE, VolcanicOutput.Output.CALDERA_MASK, VolcanicOutput.Output.COLLAPSE}) {
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

    @Test
    void footprintTrailsArePureAndStopAbruptly() {
        Map<String, Integer> ends = new HashMap<>();
        int trails = 0;
        for (int i = -20; i < 20; i++) {
            for (int j = -20; j < 20; j++) {
                FootprintPath.Trail trail = FootprintPath.trail(20260930L, i, j, 72, 0.9F, FootprintPath.Rims.NONE);
                FootprintPath.Trail again = FootprintPath.trail(20260930L, i, j, 72, 0.9F, FootprintPath.Rims.NONE);
                assertEquals(trail, again);
                if (trail == null) {
                    continue;
                }
                trails++;
                ends.merge(trail.end().name(), 1, Integer::sum);
                assertTrue(trail.steps().size() >= FootprintPath.MIN_STEPS && trail.steps().size() <= FootprintPath.MAX_STEPS);
                for (int k = 1; k < trail.steps().size(); k++) {
                    FootprintPath.Step a = trail.steps().get(k - 1);
                    FootprintPath.Step b = trail.steps().get(k);
                    assertTrue(Math.abs(a.x() - b.x()) <= 3 && Math.abs(a.z() - b.z()) <= 3, "steps follow each other");
                    assertTrue(a.left() != b.left(), "the feet alternate");
                }
                for (FootprintPath.Step step : trail.steps()) {
                    assertTrue(step.x() >= i * 72 - FootprintPath.MAX_LENGTH && step.x() < (i + 1) * 72 + FootprintPath.MAX_LENGTH);
                    assertTrue(step.z() >= j * 72 - FootprintPath.MAX_LENGTH && step.z() < (j + 1) * 72 + FootprintPath.MAX_LENGTH);
                }
            }
        }
        assertTrue(trails > 1200 && trails < 1700, "about 9 in 10 cells: " + trails);
        assertEquals(2, ends.size(), "without a rim a rim trail ends in nothing: " + ends);
        // Another seed, other trails.
        assertFalse(FootprintPath.trail(1L, 0, 0, 72, 1.0F, FootprintPath.Rims.NONE).equals(FootprintPath.trail(2L, 0, 0, 72, 1.0F, FootprintPath.Rims.NONE)));
    }

    @Test
    void landmarkPointsArePlacedInsideTheChunkMargin() {
        List<VolcanicField.Point> points = List.of(new VolcanicField.Point(8.3, 8.7), new VolcanicField.Point(1.0, 1.0),
                new VolcanicField.Point(20.0, 8.0));
        assertEquals(List.of(new BlockPos(8, 64, 8)),
                TerrainLandmarkPlacement.of(TerrainLandmarkPlacement.Landmark.MOUND, 1.0F, 0, 2, 0).place(points, 0, 64, 0, 1L));
        assertEquals(List.of(), TerrainLandmarkPlacement.of(TerrainLandmarkPlacement.Landmark.MOUND, 0.0F, 0, 0, 0)
                .place(points, 0, 64, 0, 1L));
    }

    @Test
    void aMovedLandmarkPointLandsInExactlyOneChunk() {
        TerrainLandmarkPlacement spread = TerrainLandmarkPlacement.of(TerrainLandmarkPlacement.Landmark.RIBCAGE, 1.0F, 12, 0, 3);
        for (int k = 0; k < 200; k++) {
            List<VolcanicField.Point> point = List.of(new VolcanicField.Point(k * 7.3 - 500.0, k * 3.1 - 200.0));
            int placed = 0;
            int px = (int) Math.floor(point.getFirst().x());
            int pz = (int) Math.floor(point.getFirst().z());
            for (int cx = Math.floorDiv(px - 13, 16); cx <= Math.floorDiv(px + 13, 16); cx++) {
                for (int cz = Math.floorDiv(pz - 13, 16); cz <= Math.floorDiv(pz + 13, 16); cz++) {
                    for (BlockPos pos : spread.place(point, cx * 16, 64, cz * 16, 20260930L)) {
                        placed++;
                        assertTrue(Math.abs(pos.getX() - Math.floor(point.getFirst().x())) <= 12, pos.toString());
                    }
                }
            }
            assertEquals(1, placed, "point " + k);
        }
        // Another salt moves the same point elsewhere (two features on one landmark do not stack).
        List<VolcanicField.Point> one = List.of(new VolcanicField.Point(100.0, 100.0));
        List<BlockPos> a = new java.util.ArrayList<>();
        List<BlockPos> b = new java.util.ArrayList<>();
        for (int cx = 4; cx <= 8; cx++) {
            for (int cz = 4; cz <= 8; cz++) {
                a.addAll(spread.place(one, cx * 16, 64, cz * 16, 1L));
                b.addAll(TerrainLandmarkPlacement.of(TerrainLandmarkPlacement.Landmark.RIBCAGE, 1.0F, 12, 0, 4).place(one, cx * 16, 64, cz * 16, 1L));
            }
        }
        assertFalse(a.equals(b));
    }

    @Test
    void fireGeodeBudsAreMostlySmallAndTheLiningFitsTheMargin() {
        assertEquals(ModBlocks.SMALL_FIRE_CRYSTAL_BUD.get(), FireGeodeFeature.bud(0.1F).getBlock());
        assertEquals(ModBlocks.MEDIUM_FIRE_CRYSTAL_BUD.get(), FireGeodeFeature.bud(0.5F).getBlock());
        assertEquals(ModBlocks.LARGE_FIRE_CRYSTAL_BUD.get(), FireGeodeFeature.bud(0.8F).getBlock());
        assertEquals(ModBlocks.FIRE_CRYSTAL_CLUSTER.get(), FireGeodeFeature.bud(0.95F).getBlock());
        // The three lining layers of one block each make up the shape's lining, which stays inside the cell.
        assertEquals(3.0, FireGeodeShape.LINING);
        FireGeodeShape.Geode largest = new FireGeodeShape.Geode(0, 0, 0, FireGeodeShape.RADIUS_MAX, 1.15, 1.15, 1.15);
        assertTrue(largest.reach() < FireGeodeShape.MARGIN, "reach " + largest.reach());
    }

    @Test
    void everyFireGeodeLayerFollowsTheOpenCavityAlsoOnASquashedAxis() {
        for (int radius = FireGeodeShape.RADIUS_MIN; radius <= FireGeodeShape.RADIUS_MAX; radius++) {
            FireGeodeShape.Geode geode = new FireGeodeShape.Geode(0, 0, 0, radius, 0.85, 0.85, 1.15);
            int half = radius + 6;
            int n = 2 * half + 1;
            byte[] kind = new byte[n * n * n];
            for (int x = 0; x < n; x++) {
                for (int y = 0; y < n; y++) {
                    for (int z = 0; z < n; z++) {
                        kind[(x * n + y) * n + z] = geode.inCavity(x - half, y - half, z - half) ? FireGeodeFeature.OPEN : FireGeodeFeature.ROCK;
                    }
                }
            }
            byte[] layer = FireGeodeFeature.layers(kind, n, n, n);
            // Along each axis out of the centre: the cavity, then the crystal wall, blackstone and basalt, one block each.
            for (int[] axis : new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}}) {
                int step = 0;
                while (kind[index(half, n, axis, step)] == FireGeodeFeature.OPEN) {
                    step++;
                }
                for (int k = 1; k <= FireGeodeFeature.LAYERS; k++) {
                    assertEquals(k, layer[index(half, n, axis, step + k - 1)], "radius " + radius + " axis " + java.util.Arrays.toString(axis));
                }
                assertEquals(0, layer[index(half, n, axis, step + FireGeodeFeature.LAYERS)]);
            }
            // Every open block with a face on rock faces the crystal wall there.
            for (int x = 1; x < n - 1; x++) {
                for (int y = 1; y < n - 1; y++) {
                    for (int z = 1; z < n - 1; z++) {
                        if (kind[(x * n + y) * n + z] != FireGeodeFeature.OPEN) {
                            continue;
                        }
                        for (int[] d : new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}}) {
                            int j = ((x + d[0]) * n + y + d[1]) * n + z + d[2];
                            if (kind[j] == FireGeodeFeature.ROCK) {
                                assertEquals(1, layer[j]);
                            }
                        }
                    }
                }
            }
        }
    }

    private static int index(int half, int n, int[] axis, int step) {
        return ((half + axis[0] * step) * n + half + axis[1] * step) * n + half + axis[2] * step;
    }

    @Test
    void lavaFallTriesArePureAndRare() {
        int tries = 0;
        for (int x = 0; x < 200; x++) {
            for (int z = 0; z < 200; z++) {
                boolean once = LavaFallFeature.tries(20260930L, x, z, 1.0F / 400.0F);
                assertEquals(once, LavaFallFeature.tries(20260930L, x, z, 1.0F / 400.0F));
                tries += once ? 1 : 0;
            }
        }
        assertTrue(tries > 60 && tries < 140, "about 1 in 400 columns: " + tries);
    }

    @Test
    void rimTrailsEndAtTheRimWalkingIn() {
        // A rim right where the trail would start, the hole to the east.
        FootprintPath.Rims rims = (x, z) -> new FootprintPath.Rim(x, z, 0.0);
        int rimTrails = 0;
        for (int i = -10; i < 10; i++) {
            for (int j = -10; j < 10; j++) {
                FootprintPath.Trail trail = FootprintPath.trail(20260930L, i, j, 72, 1.0F, rims);
                if (trail.end() != FootprintPath.End.RIM) {
                    continue;
                }
                rimTrails++;
                FootprintPath.Step last = trail.steps().get(trail.steps().size() - 1);
                assertTrue(Math.hypot(last.x() + 0.5 - trail.endX(), last.z() + 0.5 - trail.endZ()) <= FootprintPath.STRIDE + 1.5, "last step at the rim");
                assertEquals(0.0, trail.heading(), 1.0E-9);
                assertEquals(trail, FootprintPath.trail(20260930L, i, j, 72, 1.0F, rims));
            }
        }
        assertTrue(rimTrails > 80, "about a third of the cells: " + rimTrails);
    }

    @Test
    void rotationsTurnPlusXTowardsADirection() {
        assertEquals(Rotation.NONE, Ring3Terrain.towards(5, 1));
        assertEquals(Rotation.CLOCKWISE_180, Ring3Terrain.towards(-5, 1));
        assertEquals(Rotation.CLOCKWISE_90, Ring3Terrain.towards(1, 5));
        assertEquals(Rotation.COUNTERCLOCKWISE_90, Ring3Terrain.towards(1, -5));
        // Clockwise 90 turns +x into +z, as the template transform does.
        BlockPos turned = StructureTemplate.transform(new BlockPos(1, 0, 0), Mirror.NONE, Rotation.CLOCKWISE_90, BlockPos.ZERO);
        assertEquals(new BlockPos(0, 0, 1), turned);
    }
}
