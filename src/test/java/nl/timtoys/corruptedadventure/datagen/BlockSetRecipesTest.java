package nl.timtoys.corruptedadventure.datagen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The generated recipes of the block and wood sets (phase 2 golf 1, stream A; after {@code runData}): ids, types and
 * output counts as vanilla's for the set they copy, every shape block craftable, and everything in our namespace with
 * its unlock advancement.
 */
class BlockSetRecipesTest {
    private static final String GENERATED = "src/generated/resources/data/corruptedadventure";

    /** Wood sets: planks prefix, log name, wood name (hyphae for the stems). */
    private static final String[][] WOOD_SETS = {
            {"dead", "dead_log", "dead_wood"},
            {"palm", "palm_log", "palm_wood"},
            {"dripshroom", "dripshroom_stem", "dripshroom_hyphae"},
            {"charred", "charred_log", "charred_wood"},
            {"rimeshroom", "rimeshroom_stem", "rimeshroom_hyphae"},
            {"moonpetal", "moonpetal_log", "moonpetal_wood"},
            {"verdant", "verdant_log", "verdant_wood"},
            {"corrupted", "corrupted_log", "corrupted_wood"}};

    /** Stone ladders like tuff's: per tier the full block and the prefix of its shapes. */
    private static final String[][][] LADDERS = {
            {{"pumice", "pumice"}, {"polished_pumice", "polished_pumice"}, {"pumice_bricks", "pumice_brick"}},
            {{"cold_lava", "cold_lava"}, {"polished_cold_lava", "polished_cold_lava"}, {"cold_lava_bricks", "cold_lava_brick"}},
            {{"tidestone", "tidestone"}, {"polished_tidestone", "polished_tidestone"}, {"tidestone_bricks", "tidestone_brick"}},
            {{"fossil_stone", "fossil_stone"}, {"polished_fossil_stone", "polished_fossil_stone"},
                    {"fossil_stone_bricks", "fossil_stone_brick"}},
            {{"white_stone", "white_stone"}, {"polished_white_stone", "polished_white_stone"},
                    {"white_stone_bricks", "white_stone_brick"}},
            {{"salt_block", null}, {"polished_salt", "polished_salt"}}};

    /** Single-tier sets: only their shapes (crafting and stonecutter). */
    private static final String[][] SINGLE = {
            {"canyon_cobblestone", "canyon_cobblestone"}, {"canyon_stone_bricks", "canyon_stone_brick"},
            {"cobbled_tidestone", "cobbled_tidestone"}, {"mossy_tidestone", "mossy_tidestone"}};

    @Test
    void theWoodSetsHaveOaksRecipesWithOaksCounts() {
        for (String[] set : WOOD_SETS) {
            String p = set[0];
            assertCrafted(p + "_planks", 4);
            assertEquals("corruptedadventure:" + p + (p.endsWith("shroom") ? "_stems" : "_logs"),
                    read("recipe/" + p + "_planks.json").getAsJsonArray("ingredients").get(0).getAsJsonObject().get("tag").getAsString(), p);
            assertCrafted(set[2], 3);
            assertCrafted("stripped_" + set[2], 3);
            assertEquals("corruptedadventure:" + set[1],
                    read("recipe/" + set[2] + ".json").getAsJsonObject("key").getAsJsonObject("#").get("item").getAsString(), p);
            assertCrafted(p + "_stairs", 4);
            assertCrafted(p + "_slab", 6);
            assertCrafted(p + "_fence", 3);
            assertCrafted(p + "_fence_gate", 1);
            assertCrafted(p + "_door", 3);
            assertCrafted(p + "_trapdoor", 2);
            assertCrafted(p + "_button", 1);
            assertCrafted(p + "_pressure_plate", 1);
            assertEquals("wooden_door", read("recipe/" + p + "_door.json").get("group").getAsString(), p);
        }
    }

    @Test
    void witheredMoonpetalLogsGiveMoonpetalPlanksAndTheirOwnWood() {
        JsonObject planks = assertCrafted("moonpetal_planks_from_withered_moonpetal_logs", 4, "moonpetal_planks");
        assertEquals("corruptedadventure:withered_moonpetal_logs",
                planks.getAsJsonArray("ingredients").get(0).getAsJsonObject().get("tag").getAsString());
        assertCrafted("withered_moonpetal_wood", 3);
        assertCrafted("stripped_withered_moonpetal_wood", 3);
    }

    @Test
    void theStoneLaddersHaveTuffsRecipes() {
        for (String[][] ladder : LADDERS) {
            for (int i = 0; i < ladder.length; i++) {
                assertShapesCrafted(ladder[i][1]);
                if (i + 1 < ladder.length) {
                    assertCrafted(ladder[i + 1][0], 4);
                }
                for (int j = i; j < ladder.length; j++) {
                    if (j > i) {
                        assertCut(ladder[j][0], ladder[i][0], 1);
                    }
                    assertShapesCut(ladder[j][1], ladder[i][0]);
                }
            }
        }
        for (String[] single : SINGLE) {
            assertShapesCrafted(single[1]);
            assertShapesCut(single[1], single[0]);
        }
    }

    @Test
    void theCanyonStoneSetHasStoneAndStoneBricksRecipes() {
        JsonObject bricks = assertCrafted("canyon_stone_bricks", 4);
        assertEquals("corruptedadventure:canyon_stones", bricks.getAsJsonObject("key").getAsJsonObject("S").get("tag").getAsString());
        for (String tint : List.of("red_canyon_stone", "orange_canyon_stone", "cream_canyon_stone")) {
            assertCut("canyon_stone_bricks", tint, 1);
            assertShapesCut("canyon_stone_brick", tint);
        }
        assertSmelted("orange_canyon_stone", "canyon_cobblestone");
        assertSmelted("tidestone", "cobbled_tidestone");
        assertSmelted("corrupted_stone", "corrupted_cobblestone");
    }

    @Test
    void scorchedSandstoneHasSandstonesRecipesWithoutAPackingRecipe() {
        assertCrafted("scorched_sandstone_stairs", 4);
        assertCrafted("scorched_sandstone_slab", 6);
        assertCrafted("scorched_sandstone_wall", 6);
        assertCrafted("cut_scorched_sandstone", 4);
        assertCrafted("cut_scorched_sandstone_slab", 6);
        assertCrafted("chiseled_scorched_sandstone", 1);
        assertCrafted("smooth_scorched_sandstone_slab", 6);
        assertCrafted("smooth_scorched_sandstone_stairs", 4);
        assertSmelted("smooth_scorched_sandstone", "scorched_sandstone");
        assertCut("cut_scorched_sandstone_slab", "cut_scorched_sandstone", 2);
        assertCut("chiseled_scorched_sandstone", "scorched_sandstone", 1);
        assertFalse(Files.exists(generated().resolve("recipe/scorched_sandstone.json")));
        // The lagoon set keeps its packing recipe and its glass.
        assertCrafted("lagoon_sandstone", 1);
        assertEquals("minecraft:glass", read("recipe/glass_from_smelting_lagoon_sand.json")
                .getAsJsonObject("result").get("id").getAsString());
    }

    @Test
    void theSingleRecipesAreLikeVanillas() {
        assertCrafted("mossy_tidestone_from_vine", 1, "mossy_tidestone");
        assertCrafted("mossy_tidestone_from_moss_block", 1, "mossy_tidestone");
        assertCrafted("white_stone_pillar", 2);
        assertCut("white_stone_pillar", "white_stone", 1);
        assertCrafted("packed_nether_ice", 1);
        assertEquals(9, read("recipe/packed_nether_ice.json").getAsJsonArray("ingredients").size());
        JsonObject dye = read("recipe/orange_dye_from_desert_flower.json");
        assertEquals("minecraft:orange_dye", dye.getAsJsonObject("result").get("id").getAsString());
        assertEquals("orange_dye", dye.get("group").getAsString());
    }

    /** Every stairs, slab, wall, fence, fence gate, door, trapdoor, button and pressure plate of ours can be made. */
    @Test
    void everyShapeBlockHasARecipe() {
        Set<String> results = new HashSet<>();
        for (Path recipe : recipes()) {
            JsonObject json = read(generated().relativize(recipe).toString().replace('\\', '/'));
            if (json.has("result") && json.get("result").isJsonObject()) {
                results.add(json.getAsJsonObject("result").get("id").getAsString());
            }
        }
        List<String> missing = ModBlocks.entries().stream().map(holder -> holder.get())
                .filter(block -> block instanceof StairBlock || block instanceof SlabBlock || block instanceof WallBlock
                        || block instanceof FenceBlock || block instanceof FenceGateBlock || block instanceof DoorBlock
                        || block instanceof TrapDoorBlock || block instanceof ButtonBlock || block instanceof PressurePlateBlock)
                .map(block -> BuiltInRegistries.BLOCK.getKey(block).toString())
                .filter(id -> !results.contains(id))
                .toList();
        assertTrue(missing.isEmpty(), "shape blocks without a recipe: " + missing);
    }

    @Test
    void everyRecipeIsOursAndHasItsUnlockAdvancement() throws IOException {
        Path minecraft = generated().resolveSibling("minecraft").resolve("recipe");
        assertFalse(Files.exists(minecraft), "recipes in minecraft: " + minecraft);
        Set<String> advancements;
        try (Stream<Path> walk = Files.walk(generated().resolve("advancement/recipes"))) {
            advancements = new HashSet<>(walk.map(path -> path.getFileName().toString()).toList());
        }
        for (Path recipe : recipes()) {
            assertTrue(advancements.contains(recipe.getFileName().toString()), "no advancement for " + recipe.getFileName());
        }
    }

    private static void assertShapesCrafted(String prefix) {
        if (prefix == null) {
            return;
        }
        assertCrafted(prefix + "_stairs", 4);
        assertCrafted(prefix + "_slab", 6);
        assertCrafted(prefix + "_wall", 6);
    }

    private static void assertShapesCut(String prefix, String material) {
        if (prefix == null) {
            return;
        }
        assertCut(prefix + "_stairs", material, 1);
        assertCut(prefix + "_slab", material, 2);
        assertCut(prefix + "_wall", material, 1);
    }

    private static JsonObject assertCrafted(String id, int count) {
        return assertCrafted(id, count, id);
    }

    private static JsonObject assertCrafted(String id, int count, String result) {
        JsonObject json = read("recipe/" + id + ".json");
        assertTrue(json.get("type").getAsString().startsWith("minecraft:crafting_"), id);
        assertResult(json, result, count, id);
        return json;
    }

    private static void assertCut(String result, String material, int count) {
        String id = result + "_from_" + material + "_stonecutting";
        JsonObject json = read("recipe/" + id + ".json");
        assertEquals("minecraft:stonecutting", json.get("type").getAsString(), id);
        assertEquals("corruptedadventure:" + material, json.getAsJsonObject("ingredient").get("item").getAsString(), id);
        assertResult(json, result, count, id);
    }

    private static void assertSmelted(String result, String input) {
        JsonObject json = read("recipe/" + result + ".json");
        assertEquals("minecraft:smelting", json.get("type").getAsString(), result);
        assertEquals("corruptedadventure:" + input, json.getAsJsonObject("ingredient").get("item").getAsString(), result);
        assertEquals(0.1, json.get("experience").getAsDouble(), 1e-6, result);
        assertResult(json, result, 1, result);
    }

    private static void assertResult(JsonObject json, String result, int count, String id) {
        JsonObject out = json.getAsJsonObject("result");
        assertEquals("corruptedadventure:" + result, out.get("id").getAsString(), id);
        assertEquals(count, out.has("count") ? out.get("count").getAsInt() : 1, id);
    }

    private static List<Path> recipes() {
        try (Stream<Path> list = Files.list(generated().resolve("recipe"))) {
            return list.sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static JsonObject read(String file) {
        try {
            return JsonParser.parseString(Files.readString(generated().resolve(file))).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(file, e);
        }
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
