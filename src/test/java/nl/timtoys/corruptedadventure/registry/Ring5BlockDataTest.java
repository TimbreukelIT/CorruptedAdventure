package nl.timtoys.corruptedadventure.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The generated ring 5 and Nether ring 5 tags and loot tables of 89 and 90 (after {@code runData}). */
class Ring5BlockDataTest {
    private static final String GENERATED = "src/generated/resources/data";

    @Test
    void voidSourcesAreExactlyTheGlitchBlockAndVoidstone() throws IOException {
        assertEquals(Set.of("corruptedadventure:glitch_block", "corruptedadventure:voidstone"),
                values("corruptedadventure/tags/block/void_sources.json"));
    }

    @Test
    void ring5StonesAreExactlyCorruptedStoneAndVoidstone() throws IOException {
        assertEquals(Set.of("corruptedadventure:corrupted_stone", "corruptedadventure:voidstone"),
                values("corruptedadventure/tags/block/ring_5_stones.json"));
    }

    /** OV2 = A: the diamond tier, not netherite. */
    @Test
    void nulliteNeedsADiamondPickaxe() throws IOException {
        assertTrue(values("minecraft/tags/block/needs_diamond_tool.json").contains("#c:ores/nullite"));
        assertEquals(Set.of("corruptedadventure:nullite_ore", "corruptedadventure:voidstone_nullite_ore"),
                values("c/tags/block/ores/nullite.json"));
        assertTrue(values("c/tags/block/ores.json").contains("#c:ores/nullite"));
        assertTrue(values("minecraft/tags/block/mineable/pickaxe.json").contains("#c:ores/nullite"));
    }

    /** 90: white stone and chalkrack host the Nether ring 5 ores; only chalkrack is Nether base stone (like fossil stone, 68). */
    @Test
    void netherRing5StonesAreWhiteStoneAndChalkrackAndOnlyChalkrackIsBaseStone() throws IOException {
        assertEquals(Set.of("corruptedadventure:white_stone", "corruptedadventure:chalkrack"),
                values("corruptedadventure/tags/block/nether_ring_5_stones.json"));
        Set<String> baseStone = values("minecraft/tags/block/base_stone_nether.json");
        assertTrue(baseStone.contains("corruptedadventure:chalkrack"));
        assertFalse(baseStone.contains("corruptedadventure:white_stone"));
        assertTrue(values("minecraft/tags/block/nether_carver_replaceables.json").contains("corruptedadventure:white_stone"));
        assertTrue(values("minecraft/tags/block/needs_diamond_tool.json").contains("#c:ores/white_core"));
        assertEquals(Set.of("corruptedadventure:white_core_ore"), values("c/tags/block/ores_in_ground/white_stone.json"));
        assertEquals(Set.of("corruptedadventure:white_core"), values("c/tags/item/gems/white_core.json"));
    }

    /** Like glowstone's dust: 1-3 shards, Fortune adds a uniform bonus, never more than 4; Silk Touch the block. */
    @Test
    void theGlitchBlockDropsOneToThreeShardsAtMostFour() throws IOException {
        JsonArray children = read("corruptedadventure/loot_table/blocks/glitch_block.json").getAsJsonArray("pools").get(0)
                .getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonArray("children");
        assertEquals("corruptedadventure:glitch_block", children.get(0).getAsJsonObject().get("name").getAsString());
        JsonObject shards = children.get(1).getAsJsonObject();
        assertEquals("corruptedadventure:glitch_shard", shards.get("name").getAsString());
        JsonObject count = function(shards, "minecraft:set_count").getAsJsonObject("count");
        assertEquals(1.0, count.get("min").getAsDouble());
        assertEquals(3.0, count.get("max").getAsDouble());
        assertEquals("minecraft:uniform_bonus_count", function(shards, "minecraft:apply_bonus").get("formula").getAsString());
        assertEquals(4, function(shards, "minecraft:limit_count").getAsJsonObject("limit").get("max").getAsInt());
    }

    private static JsonObject function(JsonObject entry, String type) {
        for (JsonElement function : entry.getAsJsonArray("functions")) {
            if (function.getAsJsonObject().get("function").getAsString().equals(type)) {
                return function.getAsJsonObject();
            }
        }
        throw new AssertionError("no " + type + " in " + entry);
    }

    private static Set<String> values(String file) throws IOException {
        Set<String> values = new HashSet<>();
        read(file).getAsJsonArray("values").forEach(value -> values.add(value.getAsString()));
        return values;
    }

    private static JsonObject read(String file) throws IOException {
        return JsonParser.parseString(Files.readString(generated().resolve(file))).getAsJsonObject();
    }

    /** {@code src/generated/resources/data}, found upwards from the working directory. */
    private static Path generated() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            if (Files.isDirectory(dir.resolve(GENERATED))) {
                return dir.resolve(GENERATED);
            }
        }
        throw new IllegalStateException("No " + GENERATED + " above " + Path.of("").toAbsolutePath());
    }
}
