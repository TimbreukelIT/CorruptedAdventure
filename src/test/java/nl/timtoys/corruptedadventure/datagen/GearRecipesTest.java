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

/** The generated sunstone recipes (fase-2-items.md §5.1, after {@code runData}). */
class GearRecipesTest {
    private static final String GENERATED = "src/generated/resources/data";
    private static final Path DATA = generated();
    private static final Path RECIPES = DATA.resolve("corruptedadventure/recipe");
    private static final String TEMPLATE = "corruptedadventure:sunstone_upgrade_smithing_template";

    @Test
    void everyDiamondPieceUpgradesToSunstone() throws IOException {
        for (String piece : List.of("sword", "axe", "pickaxe", "shovel", "hoe", "helmet", "chestplate", "leggings", "boots")) {
            JsonObject recipe = read(RECIPES.resolve("sunstone_" + piece + "_smithing.json"));
            assertEquals("minecraft:smithing_transform", recipe.get("type").getAsString(), piece);
            assertEquals(TEMPLATE, recipe.getAsJsonObject("template").get("item").getAsString(), piece);
            assertEquals("minecraft:diamond_" + piece, recipe.getAsJsonObject("base").get("item").getAsString(), piece);
            assertEquals("corruptedadventure:sunstone_ingot", recipe.getAsJsonObject("addition").get("item").getAsString(), piece);
            assertEquals("corruptedadventure:sunstone_" + piece, recipe.getAsJsonObject("result").get("id").getAsString(), piece);
        }
    }

    /** 6 soul crystal + 2 diamond + 1 scorched sandstone, one template; no copy recipe (crafting is the copy). */
    @Test
    void theTemplateComesFromSoulCrystalOnly() throws IOException {
        JsonObject recipe = read(RECIPES.resolve("sunstone_upgrade_smithing_template.json"));
        Map<String, String> key = new HashMap<>();
        recipe.getAsJsonObject("key").entrySet()
                .forEach(entry -> key.put(entry.getKey(), entry.getValue().getAsJsonObject().get("item").getAsString()));
        Map<String, Integer> counts = new HashMap<>();
        for (var row : recipe.getAsJsonArray("pattern")) {
            for (char symbol : row.getAsString().toCharArray()) {
                counts.merge(key.get(String.valueOf(symbol)), 1, Integer::sum);
            }
        }
        assertEquals(Map.of("corruptedadventure:soul_crystal", 6, "minecraft:diamond", 2,
                "corruptedadventure:scorched_sandstone", 1), counts);
        assertEquals(1, recipe.getAsJsonObject("result").get("count").getAsInt());
        try (var files = Files.list(RECIPES)) {
            assertFalse(files.anyMatch(file -> !file.getFileName().toString().equals("sunstone_upgrade_smithing_template.json")
                    && file.getFileName().toString().contains("sunstone_upgrade_smithing_template")));
        }
    }

    /** The coatings ({@code smithing_trim} on {@code #trimmable_armor}) take sunstone armour too. */
    @Test
    void sunstoneArmourTakesCoatings() throws IOException {
        String trimmable = Files.readString(DATA.resolve("minecraft/tags/item/trimmable_armor.json"));
        for (String piece : List.of("helmet", "chestplate", "leggings", "boots")) {
            assertTrue(trimmable.contains("\"corruptedadventure:sunstone_" + piece + "\""), piece);
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
