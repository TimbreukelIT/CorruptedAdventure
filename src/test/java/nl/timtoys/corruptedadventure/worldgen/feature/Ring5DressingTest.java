package nl.timtoys.corruptedadventure.worldgen.feature;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.Ring5Field;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.Ring5FieldTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The pure plans of ring 5's dressing (94) on the test seed: the clean missing chunks (V1-A), the memories (V7-A), the
 * mirrored oak pairs (V4-A), the signs' facing and the torch arrow (V5-A). Measured numbers go into the plan
 * ({@code docs/plans/ring-5/golf-2-biomes.md}).
 */
public class Ring5DressingTest {
    static final RingLayout LAYOUT = Ring5FieldTest.LAYOUT;
    static final Ring5World WORLD = Ring5World.of(Ring5FieldTest.field(), LAYOUT, Ring5World.salt(Ring5FieldTest.SEED));
    /** Ring 5 with its void edge, in chunks: a square round the world's middle (the ring is a circle in it). */
    static final int EDGE_CHUNKS = 10200 / 16 + 1;

    /** Every clean missing chunk of the test world. */
    static List<ChunkPos> missingChunks(Ring5World world) {
        List<ChunkPos> found = new ArrayList<>();
        int cells = EDGE_CHUNKS / Ring5MissingChunks.CELL_CHUNKS + 1;
        for (int i = -cells; i <= cells; i++) {
            for (int j = -cells; j <= cells; j++) {
                long chosen = Ring5MissingChunks.decide(world, i, j);
                if (chosen != Long.MIN_VALUE) {
                    found.add(new ChunkPos((int) (chosen >> 32), (int) chosen));
                }
            }
        }
        return found;
    }

    @Test
    void missingChunksLieDeepInTheInnerBandOnSolidGround() {
        List<ChunkPos> chunks = missingChunks(WORLD);
        assertEquals(chunks, missingChunks(WORLD), "pure");
        // The inner band holds about (8850² − 8128²) π / (20 · 16)² ≈ 320 cells; most keep their chunk.
        System.out.println("missing chunks: " + chunks.size() + ", east: " + chunks.stream()
                .filter(c -> Math.abs(c.getMiddleBlockZ()) < 600 && c.getMiddleBlockX() > 0).toList());
        assertTrue(chunks.size() > 120 && chunks.size() < 360, "count " + chunks.size());
        for (ChunkPos chunk : chunks) {
            int x = chunk.getMiddleBlockX();
            int z = chunk.getMiddleBlockZ();
            assertTrue(WORLD.band().signedDistance(x, z) <= -Ring5MissingChunks.BAND_CLEARANCE, chunk + " near the band line");
            assertTrue(WORLD.ringDepth(x, z) >= Ring5MissingChunks.ZONE_CLEARANCE, chunk + " near the zone 4→5");
            assertTrue(Ring5MissingChunks.missing(WORLD, chunk.x, chunk.z));
            assertFalse(Ring5MissingChunks.missing(WORLD, chunk.x + 1, chunk.z), "one chunk per cell");
            for (int k = 0; k < 16; k += 5) {
                assertTrue(WORLD.isVoid(chunk.getMinBlockX() + k, chunk.getMinBlockZ() + 15 - k));
            }
        }
    }

    @Test
    void memoriesAreRareWholeAndHalfGrey() {
        Map<Ring5Memories.Kind, Integer> kinds = new EnumMap<>(Ring5Memories.Kind.class);
        int floating = 0;
        int tilted = 0;
        List<Ring5Memories.Memory> all = new ArrayList<>();
        int cells = 10200 / Ring5Memories.CELL + 1;
        for (int i = -cells; i <= cells; i++) {
            for (int j = -cells; j <= cells; j++) {
                Ring5Memories.Memory memory = Ring5Memories.memory(WORLD, i, j);
                if (memory == null) {
                    continue;
                }
                assertEquals(memory, Ring5Memories.memory(WORLD, i, j), "pure");
                all.add(memory);
                kinds.merge(memory.kind(), 1, Integer::sum);
                floating += memory.floating() ? 1 : 0;
                tilted += memory.tilt() > 0.0 ? 1 : 0;
                assertTrue(memory.radius() >= Ring5Memories.MIN_RADIUS && memory.radius() <= Ring5Memories.MAX_RADIUS);
                assertTrue(WORLD.core(memory.x(), memory.z()));
                Ring5Field.CastleSite site = WORLD.nearestCastle(memory.x(), memory.z());
                assertTrue(site.distance(memory.x(), memory.z()) >= site.radius() + site.belt() + memory.radius());
            }
        }
        System.out.println("memories: " + all.size() + " " + kinds + ", floating " + floating + ", tilted " + tilted + "; east: "
                + all.stream().filter(m -> m.x() > 0 && Math.abs(m.z()) < 1500).toList());
        assertEquals(4, kinds.size(), "every kind");
        assertTrue(all.size() >= 40, "count " + all.size());
        double share = floating / (double) all.size();
        assertTrue(share > 0.15 && share < 0.6, "floating share " + share);

        // One memory's blocks: none is an ore's target (stone), about half of its ground grey, all within its reach.
        Ring5Memories.Memory memory = all.get(0);
        Map<BlockPos, BlockState> blocks = Ring5Memories.blocks(memory, WORLD);
        assertFalse(blocks.isEmpty());
        int grey = 0;
        for (Map.Entry<BlockPos, BlockState> block : blocks.entrySet()) {
            BlockState state = block.getValue();
            assertFalse(state.is(net.minecraft.world.level.block.Blocks.STONE), "no stone in a memory");
            assertFalse(state.is(nl.timtoys.corruptedadventure.registry.ModBlocks.CORRUPTED_STONE.get()), "no corrupted stone");
            grey += BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().startsWith("corrupted_") ? 1 : 0;
            BlockPos pos = block.getKey();
            assertTrue(Math.hypot(pos.getX() - memory.x(), pos.getZ() - memory.z()) <= memory.radius() * 1.3 + Ring5Memories.REACH + 1);
        }
        assertTrue(grey > blocks.size() / 5 && grey < blocks.size() * 4 / 5, "grey " + grey + " of " + blocks.size());
    }

    @Test
    void mirroredPairsAreExactMirrorImages() {
        MirroredTreesFeature.BiomeAt everywhere = (x, y, z) -> true;
        int pairs = 0;
        // Cells in the inner band east (x 8400 on).
        int first = 8400 / MirroredTreesFeature.CELL;
        for (int i = first; i < first + 30; i++) {
            for (int j = -10; j < 10; j++) {
                MirroredTreesFeature.Pair pair = MirroredTreesFeature.pair(WORLD, everywhere, i, j);
                if (pair == null) {
                    continue;
                }
                pairs++;
                // The logs of both trees: the two trunks; their shapes mirror each other block for block.
                Set<BlockPos> blocks = new HashSet<>(pair.blocks().keySet());
                assertEquals(pair.blocks().size(), blocks.size(), "no shared block");
                assertTrue(pair.blocks().size() % 2 == 0, "two halves");
            }
        }
        assertTrue(pairs > 100, "pairs " + pairs);
    }

    @Test
    void signsFaceTheWorldsMiddle() {
        // East of the middle the text faces west (rotation 4), north faces south (0), west east (12), south north (8).
        assertEquals(4, HerobrineSignFeature.facingTheMiddle(8500, 0));
        assertEquals(0, HerobrineSignFeature.facingTheMiddle(0, -8500));
        assertEquals(12, HerobrineSignFeature.facingTheMiddle(-8500, 0));
        assertEquals(8, HerobrineSignFeature.facingTheMiddle(0, 8500));
    }

    @Test
    void theArrowPointsToTheNearestCastle() {
        Ring5Field.CastleSite site = WORLD.field().castleSites().get(0);
        int[] d = TorchPatternFeature.towardsCastle(WORLD, site.x() - 200, site.z());
        assertNotNull(d);
        assertEquals(1, d[0]);
        assertEquals(0, d[1]);
    }
    /**
     * The surface (94/95): one top-level ring 5 rule right after the bedrock floor and ring 3's cave walls, behind the
     * guard {@code core}: missing chunks to air first (before rims and walls), then rims, upper walls, veins, walls, the
     * inverted hills' undersides, the same edge round the holes of 93's void caves (INT3), castle ground, then the biomes'
     * surfaces near the column's top. Written by
     * {@code tools/worldgen/ring5_surface.py}.
     */
    @Test
    void theRing5GroundRuleStandsAfterTheBedrockAndTheCaveWalls() {
        JsonArray rules = WorldgenData.read("noise_settings/overworld.json").getAsJsonObject("surface_rule").getAsJsonArray("sequence");
        assertEquals("minecraft:bedrock_floor", rules.get(0).getAsJsonObject().getAsJsonObject("if_true").get("random_name").getAsString());
        assertEquals("ring_3", rules.get(1).getAsJsonObject().getAsJsonObject("if_true").get("ring").getAsString());
        JsonObject rule = rules.get(2).getAsJsonObject();
        assertEquals(part("core"), rule.getAsJsonObject("if_true"));
        List<String> parts = new ArrayList<>();
        List<String> results = new ArrayList<>();
        for (JsonElement step : rule.getAsJsonObject("then_run").getAsJsonArray("sequence")) {
            parts.add(step.getAsJsonObject().getAsJsonObject("if_true").get("part").getAsString());
            results.add(step.getAsJsonObject().getAsJsonObject("then_run").toString());
        }
        assertEquals(List.of("missing_chunk", "rim", "wall_top", "vein", "wall", "inverted_hill", "hole_rim", "hole_wall_top",
                "hole_vein", "hole_wall", "castle", "upper"), parts);
        assertTrue(results.get(0).contains("minecraft:air"));
        for (int k : new int[]{1, 2, 3, 6, 7, 8}) {
            assertTrue(results.get(k).contains("corruptedadventure:voidstone"), parts.get(k));
        }
        for (int k : new int[]{4, 9}) {
            assertTrue(results.get(k).contains("corruptedadventure:corrupted_stone") && !results.get(k).contains("deepslate"), parts.get(k));
        }
        assertTrue(results.get(5).contains("corruptedadventure:corrupted_grass_block") && results.get(5).contains("corruptedadventure:corrupted_dirt"));
        assertTrue(results.get(10).contains("minecraft:obsidian") && results.get(10).contains("corruptedadventure:glitch_block"));
        // The biomes' surfaces once more near the column's pure top (no bare rectangles beside void holes).
        assertTrue(results.get(11).contains("corruptedadventure:corrupted_wastes") && results.get(11).contains("minecraft:grass_block"));
        long count = rules.asList().stream().filter(r -> part("core").equals(r.getAsJsonObject().getAsJsonObject("if_true"))).count();
        assertEquals(1, count, "one ring 5 ground rule");
    }

    private static JsonObject part(String part) {
        JsonObject condition = new JsonObject();
        condition.addProperty("type", "corruptedadventure:ring_5_ground");
        condition.addProperty("part", part);
        return condition;
    }

    /** The salt from the seed is the one the random state gives the surface pass (the same formula as RandomState). */
    @Test
    void theSaltIsPureInTheSeed() {
        assertEquals(Ring5World.salt(Ring5FieldTest.SEED), Ring5World.salt(Ring5FieldTest.SEED));
        assertFalse(Ring5World.salt(Ring5FieldTest.SEED) == Ring5World.salt(Ring5FieldTest.SEED + 1));
    }
}
