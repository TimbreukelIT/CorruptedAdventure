package nl.timtoys.corruptedadventure.datagen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModItems;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The generated material recipes of phase 2 golf 1 (stream D, after {@code runData}). */
class MaterialRecipesTest {
    private static final String GENERATED = "src/generated/resources/data/corruptedadventure/recipe";
    private static final Path RECIPES = generated();

    private static final List<String> STORAGE = List.of("raw_sunstone_block:raw_sunstone", "raw_turquoise_block:raw_turquoise",
            "sunstone_block:sunstone_ingot", "turquoise_block:turquoise", "raw_volcanite_block:raw_volcanite",
            "volcanite_block:volcanite_ingot", "fire_ruby_block:fire_ruby", "raw_tidesilver_block:raw_tidesilver",
            "tidesilver_block:tidesilver_ingot", "jade_block:jade", "raw_nullite_block:raw_nullite",
            "nullite_block:nullite_ingot");

    @Test
    void everyOreAndRawMaterialSmeltsAndBlasts() throws IOException {
        List<String> inputs = new ArrayList<>();
        ModBlocks.entries().stream().map(block -> block.getId().getPath()).filter(name -> name.endsWith("_ore"))
                .forEach(inputs::add);
        ModItems.entries().stream().map(item -> item.getId().getPath()).filter(name -> name.startsWith("raw_")
                && !name.endsWith("_block")).forEach(inputs::add);
        assertEquals(31, inputs.size(), inputs.toString());
        for (String input : inputs) {
            for (String kind : List.of("smelting", "blasting")) {
                try (var files = Files.list(RECIPES)) {
                    List<Path> found = files.filter(file -> file.getFileName().toString()
                            .endsWith("_from_" + kind + "_" + input + ".json")).toList();
                    assertEquals(1, found.size(), kind + " " + input);
                    JsonObject json = read(found.get(0));
                    assertEquals("minecraft:" + kind, json.get("type").getAsString());
                    assertEquals(kind.equals("smelting") ? 200 : 100, json.get("cookingtime").getAsInt());
                }
            }
        }
    }

    @Test
    void storageBlocksPackNineAndUnpackNine() throws IOException {
        for (String pair : STORAGE) {
            String block = pair.split(":")[0];
            String item = pair.split(":")[1];
            JsonObject pack = read(RECIPES.resolve(block + ".json"));
            assertEquals("minecraft:crafting_shaped", pack.get("type").getAsString(), block);
            assertEquals(List.of("###", "###", "###"), pack.getAsJsonArray("pattern").asList().stream()
                    .map(element -> element.getAsString()).toList(), block);
            assertEquals("corruptedadventure:" + item, pack.getAsJsonObject("key").getAsJsonObject("#").get("item").getAsString());
            JsonObject unpack = read(RECIPES.resolve(item + "_from_" + block + ".json"));
            assertEquals(9, unpack.getAsJsonObject("result").get("count").getAsInt(), block);
        }
    }

    @Test
    void saltPacksFourBothWaysAndCrystalShardsOnlyIntoTheBlock() throws IOException {
        assertEquals(List.of("##", "##"), read(RECIPES.resolve("salt_block.json")).getAsJsonArray("pattern").asList()
                .stream().map(element -> element.getAsString()).toList());
        assertEquals(4, read(RECIPES.resolve("salt_from_salt_block.json")).getAsJsonObject("result").get("count").getAsInt());
        for (String crystal : List.of("fire_crystal_block", "toxic_crystal_block", "aquamarine_block", "purity_crystal_block")) {
            assertTrue(Files.exists(RECIPES.resolve(crystal + ".json")), crystal);
            try (var files = Files.list(RECIPES)) {
                assertFalse(files.anyMatch(file -> file.getFileName().toString().endsWith("_from_" + crystal + ".json")), crystal);
            }
        }
    }

    /** The tests run in {@code run/}; walk up to the checkout (as in {@code Ring5RecipesTest}). */
    private static Path generated() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            if (Files.isDirectory(dir.resolve(GENERATED))) {
                return dir.resolve(GENERATED);
            }
        }
        throw new IllegalStateException("No " + GENERATED + " above " + Path.of("").toAbsolutePath());
    }

    private static JsonObject read(Path file) throws IOException {
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    }
}
