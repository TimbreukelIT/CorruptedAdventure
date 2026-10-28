package nl.timtoys.corruptedadventure.datagen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The generated gear tier recipes (fase-2-items.md §5.1, §6.2, §6.5, §7.1, after {@code runData}). */
class GearRecipesTest {
    private static final String GENERATED = "src/generated/resources/data";
    private static final Path DATA = generated();
    private static final Path RECIPES = DATA.resolve("corruptedadventure/recipe");
    private static final List<String> PIECES = List.of("sword", "axe", "pickaxe", "shovel", "hoe", "helmet", "chestplate",
            "leggings", "boots");
    /** Each tier with the material whose gear it upgrades, lowest first. */
    private static final List<List<String>> LADDER = List.of(List.of("sunstone", "minecraft:diamond"),
            List.of("volcanite", "corruptedadventure:sunstone"), List.of("tidesilver", "corruptedadventure:volcanite"),
            List.of("nullite", "corruptedadventure:tidesilver"));

    @Test
    void everyTierUpgradesThePiecesOfTheOneBefore() throws IOException {
        for (List<String> step : LADDER) {
            String tier = step.get(0);
            for (String piece : PIECES) {
                String name = tier + "_" + piece;
                JsonObject recipe = read(RECIPES.resolve(name + "_smithing.json"));
                assertEquals("minecraft:smithing_transform", recipe.get("type").getAsString(), name);
                assertEquals("corruptedadventure:" + tier + "_upgrade_smithing_template",
                        recipe.getAsJsonObject("template").get("item").getAsString(), name);
                assertEquals(step.get(1) + "_" + piece, recipe.getAsJsonObject("base").get("item").getAsString(), name);
                assertEquals("corruptedadventure:" + tier + "_ingot",
                        recipe.getAsJsonObject("addition").get("item").getAsString(), name);
                assertEquals("corruptedadventure:" + name, recipe.getAsJsonObject("result").get("id").getAsString(), name);
            }
        }
    }

    /** Sunstone from soul crystal (§5.1), volcanite from dew crystal (§6.2), tidesilver from primordial ice (§6.5). */
    @Test
    void theTemplatesComeFromTheNetherMaterialTheIngotBeforeAndARingStone() throws IOException {
        assertTemplate("sunstone", Map.of("corruptedadventure:soul_crystal", 6, "minecraft:diamond", 2,
                "corruptedadventure:scorched_sandstone", 1));
        assertTemplate("volcanite", Map.of("corruptedadventure:dew_crystal", 6, "corruptedadventure:sunstone_ingot", 2,
                "corruptedadventure:pumice", 1));
        assertTemplate("tidesilver", Map.of("corruptedadventure:primordial_ice", 6,
                "corruptedadventure:volcanite_ingot", 2, "corruptedadventure:tidestone", 1));
    }

    /** One template per craft and no copy recipe: crafting already is the copy. */
    private static void assertTemplate(String tier, Map<String, Integer> expected) throws IOException {
        String template = tier + "_upgrade_smithing_template";
        JsonObject recipe = read(RECIPES.resolve(template + ".json"));
        Map<String, String> key = new HashMap<>();
        recipe.getAsJsonObject("key").entrySet()
                .forEach(entry -> key.put(entry.getKey(), entry.getValue().getAsJsonObject().get("item").getAsString()));
        Map<String, Integer> counts = new HashMap<>();
        for (var row : recipe.getAsJsonArray("pattern")) {
            for (char symbol : row.getAsString().toCharArray()) {
                counts.merge(key.get(String.valueOf(symbol)), 1, Integer::sum);
            }
        }
        assertEquals(expected, counts, tier);
        assertEquals(1, recipe.getAsJsonObject("result").get("count").getAsInt(), tier);
        try (var files = Files.list(RECIPES)) {
            assertFalse(files.anyMatch(file -> !file.getFileName().toString().equals(template + ".json")
                    && file.getFileName().toString().contains(template)), tier);
        }
    }

    /** The coatings ({@code smithing_trim} on {@code #trimmable_armor}) take the armour of every tier. */
    @Test
    void tierArmourTakesCoatings() throws IOException {
        String trimmable = Files.readString(DATA.resolve("minecraft/tags/item/trimmable_armor.json"));
        for (List<String> step : LADDER) {
            for (String piece : List.of("helmet", "chestplate", "leggings", "boots")) {
                assertTrue(trimmable.contains("\"corruptedadventure:" + step.get(0) + "_" + piece + "\""),
                        step.get(0) + " " + piece);
            }
        }
    }

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
