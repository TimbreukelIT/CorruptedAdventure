package nl.timtoys.corruptedadventure.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

/**
 * The generated {@code #corruptedadventure:corrupted} block tag and {@code corruptedadventure:healthy_block} data map
 * (after {@code runData}) cover exactly the same nine blocks (contract ring 5 §3/§11): every tagged block has a healthy
 * counterpart, no mapping without the tag, and the rest of the corrupted wood set is in neither.
 */
class Ring5HealthyBlockTest {
    private static final String GENERATED = "src/generated/resources/data";
    private static final Map<String, String> EXPECTED = Map.of(
            "corruptedadventure:corrupted_grass_block", "minecraft:grass_block",
            "corruptedadventure:corrupted_dirt", "minecraft:dirt",
            "corruptedadventure:corrupted_stone", "minecraft:stone",
            "corruptedadventure:corrupted_cobblestone", "minecraft:cobblestone",
            "corruptedadventure:corrupted_sand", "minecraft:sand",
            "corruptedadventure:corrupted_log", "minecraft:oak_log",
            "corruptedadventure:corrupted_wood", "minecraft:oak_wood",
            "corruptedadventure:corrupted_planks", "minecraft:oak_planks",
            "corruptedadventure:corrupted_leaves", "minecraft:oak_leaves");

    @Test
    void theCorruptedTagAndTheHealthyBlockMapCoverExactlyTheNineVariants() throws IOException {
        Set<String> tagged = new HashSet<>();
        read("corruptedadventure/tags/block/corrupted.json").getAsJsonArray("values")
                .forEach(value -> tagged.add(value.getAsString()));
        Map<String, String> healthy = new HashMap<>();
        read("corruptedadventure/data_maps/block/healthy_block.json").getAsJsonObject("values").entrySet()
                .forEach(entry -> healthy.put(entry.getKey(), valueOf(entry.getValue())));

        assertEquals(EXPECTED.keySet(), tagged);
        assertEquals(EXPECTED, healthy);
    }

    @Test
    void theRestOfTheWoodSetIsNeitherCorruptedNorMapped() throws IOException {
        String tag = Files.readString(generated().resolve("corruptedadventure/tags/block/corrupted.json"));
        String map = Files.readString(generated().resolve("corruptedadventure/data_maps/block/healthy_block.json"));
        for (String name : new String[] {"stripped_corrupted_log", "stripped_corrupted_wood", "corrupted_stairs", "corrupted_slab",
                "corrupted_fence", "corrupted_fence_gate", "corrupted_door", "corrupted_trapdoor", "corrupted_button",
                "corrupted_pressure_plate", "corrupted_sign", "corrupted_wall_sign", "corrupted_hanging_sign",
                "corrupted_wall_hanging_sign"}) {
            String id = "\"corruptedadventure:" + name + "\"";
            assertFalse(tag.contains(id), name);
            assertFalse(map.contains(id), name);
        }
    }

    /** A data map value is written plain, or as {@code {"value": ...}} when it carries a replace flag or conditions. */
    private static String valueOf(JsonElement element) {
        return element.isJsonObject() ? element.getAsJsonObject().get("value").getAsString() : element.getAsString();
    }

    /** {@code src/generated/resources/data}, found upwards from the working directory (as in {@code PoisonDamageTypeTagsTest}). */
    private static Path generated() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            if (Files.isDirectory(dir.resolve(GENERATED))) {
                return dir.resolve(GENERATED);
            }
        }
        throw new IllegalStateException("No " + GENERATED + " above " + Path.of("").toAbsolutePath());
    }

    private static JsonObject read(String file) throws IOException {
        Path path = generated().resolve(file);
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
