package nl.timtoys.corruptedadventure.worldgen.surface;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * {@code polygon_edge}: the share of edge columns, hexagonal cells, a pure function of (seed, x, z) and the codec; the
 * settings are read from the salt flats surface rule in {@code noise_settings/overworld.json}.
 */
class PolygonEdgeConditionTest {
    private static final ResourceKey<NormalNoise.NoiseParameters> HASH =
            ResourceKey.create(Registries.NOISE, ResourceLocation.fromNamespaceAndPath("corruptedadventure", "ring_2_salt_polygons"));
    private static final int AREA = 512;

    @Test
    void edgeShareOfTheSaltFlats() throws IOException {
        PolygonEdgeCondition condition = fromSurfaceRules();
        for (long seed : new long[]{20260930L, 1L, -77L}) {
            PolygonGrid grid = condition.grid(noise(seed));
            int edges = 0;
            for (int x = 0; x < AREA; x++) {
                for (int z = 0; z < AREA; z++) {
                    edges += grid.isEdge(x, z) ? 1 : 0;
                }
            }
            double share = edges / (double) (AREA * AREA);
            assertTrue(share >= 0.15 && share <= 0.25, "edge share " + share + " for seed " + seed);
        }
    }

    @Test
    void cellsAreHexagonsOfTheCellSize() throws IOException {
        PolygonEdgeCondition condition = fromSurfaceRules();
        PolygonGrid grid = condition.grid(noise(20260930L));
        Map<Long, Set<Long>> neighbours = new HashMap<>();
        Map<Long, Integer> sizes = new HashMap<>();
        long[][] owners = new long[AREA][AREA];
        for (int x = 0; x < AREA; x++) {
            for (int z = 0; z < AREA; z++) {
                owners[x][z] = grid.owner(x, z);
                sizes.merge(owners[x][z], 1, Integer::sum);
            }
        }
        for (int x = 0; x < AREA - 1; x++) {
            for (int z = 0; z < AREA - 1; z++) {
                long a = owners[x][z];
                for (long b : new long[]{owners[x + 1][z], owners[x][z + 1]}) {
                    if (a != b) {
                        neighbours.computeIfAbsent(a, k -> new HashSet<>()).add(b);
                        neighbours.computeIfAbsent(b, k -> new HashSet<>()).add(a);
                    }
                }
            }
        }
        // Only cells away from the border of the sampled area.
        double total = 0;
        int cells = 0;
        double area = 0;
        int margin = (int) Math.ceil(2 * condition.cellSize());
        for (int x = margin; x < AREA - margin; x += 3) {
            for (int z = margin; z < AREA - margin; z += 3) {
                long owner = owners[x][z];
                if (neighbours.containsKey(owner) && neighbours.get(owner).size() > 0 && sizes.containsKey(owner)) {
                    total += neighbours.get(owner).size();
                    area += sizes.get(owner);
                    cells++;
                    sizes.remove(owner);
                }
            }
        }
        double mean = total / cells;
        assertTrue(mean >= 5.0 && mean <= 7.0, "mean neighbours " + mean);
        // A hexagon of the triangular lattice covers √3/2 · cell².
        double expected = Math.sqrt(3.0) / 2.0 * condition.cellSize() * condition.cellSize();
        assertEquals(expected, area / cells, expected * 0.1, "mean cell area");
    }

    @Test
    void edgesAreClosedLinesAcrossChunkBorders() throws IOException {
        PolygonEdgeCondition condition = fromSurfaceRules();
        NormalNoise noise = noise(20260930L);
        // A fresh grid per chunk, columns in another order: the same answer (a function of world coordinates only).
        PolygonGrid whole = condition.grid(noise);
        for (int chunkX = -2; chunkX < 2; chunkX++) {
            for (int chunkZ = -2; chunkZ < 2; chunkZ++) {
                PolygonGrid chunk = condition.grid(noise);
                for (int dz = 15; dz >= 0; dz--) {
                    for (int dx = 15; dx >= 0; dx--) {
                        int x = chunkX * 16 + dx;
                        int z = chunkZ * 16 + dz;
                        assertEquals(whole.isEdge(x, z), chunk.isEdge(x, z), x + " " + z);
                    }
                }
            }
        }
        // Two columns of different polygons that touch along an axis are never both inside: an edge separates them.
        for (int x = -64; x < 64; x++) {
            for (int z = -64; z < 64; z++) {
                if (whole.isEdge(x, z)) {
                    continue;
                }
                for (int[] d : new int[][]{{1, 0}, {0, 1}}) {
                    if (!whole.isEdge(x + d[0], z + d[1])) {
                        assertEquals(whole.owner(x, z), whole.owner(x + d[0], z + d[1]), "open edge at " + x + " " + z);
                    }
                }
            }
        }
    }

    @Test
    void sameSeedSamePatternOtherSeedOther() throws IOException {
        PolygonEdgeCondition condition = fromSurfaceRules();
        PolygonGrid a = condition.grid(noise(5L));
        PolygonGrid b = condition.grid(noise(5L));
        PolygonGrid c = condition.grid(noise(6L));
        int different = 0;
        for (int x = 0; x < 128; x++) {
            for (int z = 0; z < 128; z++) {
                assertEquals(a.isEdge(x, z), b.isEdge(x, z));
                different += a.isEdge(x, z) != c.isEdge(x, z) ? 1 : 0;
            }
        }
        assertNotEquals(0, different);
    }

    @Test
    void codecRoundTripAndRanges() throws IOException {
        PolygonEdgeCondition condition = fromSurfaceRules();
        JsonElement json = PolygonEdgeCondition.MAP_CODEC.codec().encodeStart(JsonOps.INSTANCE, condition).getOrThrow();
        assertEquals(condition, PolygonEdgeCondition.MAP_CODEC.codec().parse(JsonOps.INSTANCE, json).getOrThrow());
        for (String bad : new String[]{"\"cell_size\": 3.0", "\"jitter\": 0.5", "\"width\": 0.0"}) {
            JsonObject broken = json.getAsJsonObject().deepCopy();
            JsonObject override = JsonParser.parseString("{" + bad + "}").getAsJsonObject();
            override.entrySet().forEach(e -> broken.add(e.getKey(), e.getValue()));
            assertTrue(PolygonEdgeCondition.MAP_CODEC.codec().parse(JsonOps.INSTANCE, broken).error().isPresent(), bad);
        }
        assertFalse(PolygonEdgeCondition.MAP_CODEC.codec().parse(JsonOps.INSTANCE, json).error().isPresent());
    }

    private static NormalNoise noise(long seed) {
        return NormalNoise.create(new XoroshiroRandomSource(seed), new NormalNoise.NoiseParameters(0, 1.0));
    }

    /** The condition of the salt flats surface rule, decoded from the JSON. */
    static PolygonEdgeCondition fromSurfaceRules() throws IOException {
        JsonElement rules = JsonParser.parseString(Files.readString(findSettings()));
        JsonObject found = find(rules);
        if (found == null) {
            return fail("no polygon_edge in the surface rules");
        }
        PolygonEdgeCondition condition = PolygonEdgeCondition.MAP_CODEC.codec().parse(JsonOps.INSTANCE, found).getOrThrow();
        assertEquals(HASH, condition.hash());
        return condition;
    }

    private static JsonObject find(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("type") && object.get("type").getAsString().equals("corruptedadventure:polygon_edge")) {
                return object;
            }
            for (var entry : object.entrySet()) {
                JsonObject found = find(entry.getValue());
                if (found != null) {
                    return found;
                }
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                JsonObject found = find(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static Path findSettings() {
        String path = "src/main/resources/data/corruptedadventure/worldgen/noise_settings/overworld.json";
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            if (Files.isRegularFile(dir.resolve(path))) {
                return dir.resolve(path);
            }
        }
        return fail("Could not find " + path);
    }
}
