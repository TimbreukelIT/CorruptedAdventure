package nl.timtoys.corruptedadventure.worldgen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vanilla's feature sorter needs one order of the placed features per generation step over all biomes of a dimension,
 * or a world fails to load ("Feature order cycle found"). This test rebuilds every biome's lists as the game does, from
 * the generated biome JSON plus the generated biome modifiers ({@code neoforge:add_features}, applied in the order of
 * their names), and looks for a cycle in the precedence graph of each dimension with a topological sort.
 * <p>
 * Overworld = every generated biome but the Nether ones ({@code is_nether_ring_*}, {@code nether_*},
 * {@code placeholder_nether_*}); the Nether's vanilla biomes ({@code soul_sand_valley}, {@code basalt_deltas}) are not
 * generated here and are left out.
 */
class FeatureOrderTest {
    private static final String GENERATED = "src/generated/resources/data/corruptedadventure";

    @Test
    void everyDimensionHasOneFeatureOrderPerStep() throws IOException {
        Path root = root();
        Map<String, List<List<String>>> biomes = biomes(root);
        applyModifiers(root, biomes);
        Set<String> nether = new HashSet<>();
        nether.addAll(tag(root, "is_nether_ring_1"));
        nether.addAll(tag(root, "is_nether_ring_2"));
        biomes.keySet().stream().filter(id -> id.contains(":nether_") || id.contains(":placeholder_nether_")).forEach(nether::add);
        Map<String, List<List<String>>> overworld = new LinkedHashMap<>();
        Map<String, List<List<String>>> netherBiomes = new LinkedHashMap<>();
        biomes.forEach((id, steps) -> (nether.contains(id) ? netherBiomes : overworld).put(id, steps));
        assertTrue(overworld.containsKey("corruptedadventure:sulfur_fields") && overworld.containsKey("corruptedadventure:dry_riverbeds")
                && overworld.containsKey("corruptedadventure:plains"), overworld.keySet().toString());
        assertNoCycle("overworld", overworld);
        assertNoCycle("nether", netherBiomes);
    }

    @Test
    void aCycleIsFound() {
        Map<String, List<List<String>>> biomes = Map.of(
                "a", List.of(List.of("x", "y")),
                "b", List.of(List.of("y", "z")),
                "c", List.of(List.of("z", "x")));
        assertFalse(cycle(biomes).isEmpty(), "a 3-cycle over three biomes, no pair in both orders");
        assertTrue(cycle(Map.of("a", List.of(List.of("x", "y")), "b", List.of(List.of("x", "z", "y")))).isEmpty());
    }

    private static void assertNoCycle(String dimension, Map<String, List<List<String>>> biomes) {
        List<String> cycle = cycle(biomes);
        assertTrue(cycle.isEmpty(), dimension + ": feature order cycle among " + cycle);
    }

    /** The features (as "step/feature") left in a cycle after a topological sort; empty if there is none. */
    static List<String> cycle(Map<String, List<List<String>>> biomes) {
        Map<String, Set<String>> next = new HashMap<>();
        Map<String, Integer> incoming = new HashMap<>();
        for (List<List<String>> steps : biomes.values()) {
            // As vanilla's FeatureSorter: one list per biome over all steps in step order.
            List<String> flat = new ArrayList<>();
            for (int step = 0; step < steps.size(); step++) {
                for (String feature : steps.get(step)) {
                    flat.add(step + "/" + feature);
                }
            }
            for (String node : flat) {
                next.computeIfAbsent(node, k -> new LinkedHashSet<>());
                incoming.putIfAbsent(node, 0);
            }
            for (int i = 1; i < flat.size(); i++) {
                if (next.get(flat.get(i - 1)).add(flat.get(i))) {
                    incoming.merge(flat.get(i), 1, Integer::sum);
                }
            }
        }
        Deque<String> ready = new ArrayDeque<>();
        incoming.forEach((node, count) -> {
            if (count == 0) {
                ready.add(node);
            }
        });
        while (!ready.isEmpty()) {
            String node = ready.poll();
            for (String after : next.get(node)) {
                if (incoming.merge(after, -1, Integer::sum) == 0) {
                    ready.add(after);
                }
            }
        }
        return incoming.entrySet().stream().filter(e -> e.getValue() > 0).map(Map.Entry::getKey).sorted().toList();
    }

    private static Map<String, List<List<String>>> biomes(Path root) throws IOException {
        Map<String, List<List<String>>> biomes = new TreeMap<>();
        try (Stream<Path> files = Files.list(root.resolve("worldgen/biome"))) {
            for (Path file : files.sorted().toList()) {
                String id = "corruptedadventure:" + file.getFileName().toString().replace(".json", "");
                List<List<String>> steps = new ArrayList<>();
                for (JsonElement step : WorldgenData.read(file).getAsJsonArray("features")) {
                    List<String> features = new ArrayList<>();
                    for (JsonElement feature : step.getAsJsonArray()) {
                        features.add(feature.getAsString());
                    }
                    steps.add(features);
                }
                biomes.put(id, steps);
            }
        }
        return biomes;
    }

    /** NeoForge's add_features modifiers, in the order of their names, appended to their step. */
    private static void applyModifiers(Path root, Map<String, List<List<String>>> biomes) throws IOException {
        try (Stream<Path> files = Files.list(root.resolve("neoforge/biome_modifier"))) {
            for (Path file : files.sorted().toList()) {
                JsonObject modifier = WorldgenData.read(file);
                if (!modifier.get("type").getAsString().equals("neoforge:add_features")) {
                    continue;
                }
                int step = STEPS.indexOf(modifier.get("step").getAsString());
                assertTrue(step >= 0, file.toString());
                List<String> features = new ArrayList<>();
                JsonElement listed = modifier.get("features");
                if (listed.isJsonArray()) {
                    listed.getAsJsonArray().forEach(feature -> features.add(feature.getAsString()));
                } else {
                    features.add(listed.getAsString());
                }
                for (String biome : targets(root, modifier.get("biomes"))) {
                    List<List<String>> steps = biomes.get(biome);
                    if (steps == null) {
                        continue;
                    }
                    while (steps.size() <= step) {
                        steps.add(new ArrayList<>());
                    }
                    List<String> list = new ArrayList<>(steps.get(step));
                    list.addAll(features);
                    steps.set(step, list);
                }
            }
        }
    }

    private static Set<String> targets(Path root, JsonElement biomes) throws IOException {
        Set<String> out = new LinkedHashSet<>();
        if (biomes.isJsonArray()) {
            for (JsonElement element : biomes.getAsJsonArray()) {
                out.addAll(targets(root, element));
            }
        } else if (biomes.isJsonObject()) {
            JsonObject set = biomes.getAsJsonObject();
            assertTrue(set.get("type").getAsString().equals("neoforge:or"), set.toString());
            out.addAll(targets(root, set.get("values")));
        } else {
            String id = biomes.getAsString();
            if (id.startsWith("#")) {
                String tag = id.substring(1);
                if (tag.startsWith("corruptedadventure:")) {
                    for (String value : tag(root, tag.substring("corruptedadventure:".length()))) {
                        out.addAll(targets(root, new com.google.gson.JsonPrimitive(value)));
                    }
                }
            } else {
                out.add(id);
            }
        }
        return out;
    }

    private static List<String> tag(Path root, String name) {
        Path file = root.resolve("tags/worldgen/biome/" + name + ".json");
        List<String> values = new ArrayList<>();
        if (Files.exists(file)) {
            JsonArray array = WorldgenData.read(file).getAsJsonArray("values");
            array.forEach(value -> values.add(value.isJsonObject() ? value.getAsJsonObject().get("id").getAsString() : value.getAsString()));
        }
        return values;
    }

    private static final List<String> STEPS = List.of("raw_generation", "lakes", "local_modifications", "underground_structures",
            "surface_structures", "strongholds", "underground_ores", "underground_decoration", "fluid_springs", "vegetal_decoration",
            "top_layer_modification");

    private static Path root() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            if (Files.isDirectory(dir.resolve(GENERATED))) {
                return dir.resolve(GENERATED);
            }
        }
        throw new IllegalStateException("no " + GENERATED);
    }
}
