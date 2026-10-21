package nl.timtoys.corruptedadventure.datagen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The generated recipes of the corrupted wood set (88r, after {@code runData}): exactly oak's thirteen, with oak's
 * output counts and a recipe advancement each, and no other ring 5 recipe (contract ring 5 §11).
 */
class Ring5RecipesTest {
    private static final String GENERATED = "src/generated/resources/data/corruptedadventure";

    /** Recipe id (= result item) and its output count, as vanilla's oak recipes. */
    private static final Map<String, Integer> EXPECTED = new LinkedHashMap<>();

    static {
        EXPECTED.put("corrupted_planks", 4);
        EXPECTED.put("corrupted_wood", 3);
        EXPECTED.put("stripped_corrupted_wood", 3);
        EXPECTED.put("corrupted_stairs", 4);
        EXPECTED.put("corrupted_slab", 6);
        EXPECTED.put("corrupted_fence", 3);
        EXPECTED.put("corrupted_fence_gate", 1);
        EXPECTED.put("corrupted_door", 3);
        EXPECTED.put("corrupted_trapdoor", 2);
        EXPECTED.put("corrupted_button", 1);
        EXPECTED.put("corrupted_pressure_plate", 1);
        EXPECTED.put("corrupted_sign", 3);
        EXPECTED.put("corrupted_hanging_sign", 6);
    }

    @Test
    void theWoodSetHasOaksThirteenRecipesWithOaksCounts() throws IOException {
        for (Map.Entry<String, Integer> recipe : EXPECTED.entrySet()) {
            JsonObject json = read("recipe/" + recipe.getKey() + ".json");
            JsonObject result = json.getAsJsonObject("result");
            assertEquals("corruptedadventure:" + recipe.getKey(), result.get("id").getAsString(), recipe.getKey());
            int count = result.has("count") ? result.get("count").getAsInt() : 1;
            assertEquals(recipe.getValue(), count, recipe.getKey());
        }
        assertEquals("corruptedadventure:corrupted_logs",
                read("recipe/corrupted_planks.json").getAsJsonArray("ingredients").get(0).getAsJsonObject().get("tag").getAsString());
        assertEquals("minecraft:chain", read("recipe/corrupted_hanging_sign.json").getAsJsonObject("key")
                .getAsJsonObject("X").get("item").getAsString());
    }

    @Test
    void everyRecipeHasItsUnlockAdvancement() throws IOException {
        Path advancements = generated().resolve("advancement/recipes");
        List<String> files;
        try (Stream<Path> walk = Files.walk(advancements)) {
            files = walk.map(path -> path.getFileName().toString()).toList();
        }
        for (String recipe : EXPECTED.keySet()) {
            assertTrue(files.contains(recipe + ".json"), "no advancement for " + recipe);
        }
    }

    @Test
    void noOtherRing5RecipesExist() throws IOException {
        List<String> ring5;
        try (Stream<Path> walk = Files.list(generated().resolve("recipe"))) {
            ring5 = walk.map(path -> path.getFileName().toString().replace(".json", ""))
                    .filter(name -> name.contains("corrupted")).sorted().toList();
        }
        assertEquals(EXPECTED.keySet().stream().sorted().toList(), ring5);
    }

    private static JsonObject read(String file) throws IOException {
        return JsonParser.parseString(Files.readString(generated().resolve(file))).getAsJsonObject();
    }

    /** {@code src/generated/resources/data/corruptedadventure}, found upwards from the working directory. */
    private static Path generated() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            if (Files.isDirectory(dir.resolve(GENERATED))) {
                return dir.resolve(GENERATED);
            }
        }
        throw new IllegalStateException("No " + GENERATED + " above " + Path.of("").toAbsolutePath());
    }
}
