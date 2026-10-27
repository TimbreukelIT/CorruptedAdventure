package nl.timtoys.corruptedadventure.worldgen.surface;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YBandsRuleTest {
    private static final List<BlockState> BLOCKS = List.of(Blocks.STONE.defaultBlockState(), Blocks.SAND.defaultBlockState(),
            Blocks.TERRACOTTA.defaultBlockState(), Blocks.RED_SANDSTONE.defaultBlockState());
    private static final String BLOCK = "{\"type\":\"minecraft:block\",\"result_state\":{\"Name\":\"minecraft:stone\"}}";

    /** The table gives the first band in list order, at every height: unsorted, repeated and shadowed bands too. */
    @Test
    void theTableIsTheFirstBandReached() {
        Random random = new Random(20260930L);
        for (int round = 0; round < 200; round++) {
            List<YBandsRule.Band> bands = new ArrayList<>();
            int count = 1 + random.nextInt(12);
            for (int i = 0; i < count; i++) {
                bands.add(new YBandsRule.Band(random.nextInt(120) - 40, BLOCKS.get(random.nextInt(BLOCKS.size()))));
            }
            YBandsRule rule = new YBandsRule(1, bands, Blocks.DEEPSLATE.defaultBlockState());
            YBandsRule.Table table = rule.table();
            for (int y = -80; y < 120; y++) {
                assertSame(firstReached(bands, y), table.get(y), "round " + round + ", y " + y);
            }
        }
    }

    private static BlockState firstReached(List<YBandsRule.Band> bands, int y) {
        for (YBandsRule.Band band : bands) {
            if (y >= band.from()) {
                return band.block();
            }
        }
        return Blocks.DEEPSLATE.defaultBlockState();
    }

    /**
     * No chain that tools/worldgen/surface_bands.py would convert is left in the surface rules (the script's own test,
     * {@link #chainBands}, and its threshold {@code MIN_BANDS}), and every y_bands has descending bands (the script drops
     * the bands that can never be reached).
     */
    @Test
    void theSurfaceRulesHoldNoConvertibleYChains() {
        int minBands = scriptMinBands();
        for (String name : List.of("overworld", "nether")) {
            JsonObject rule = WorldgenData.read("noise_settings/" + name + ".json").getAsJsonObject("surface_rule");
            int[] found = new int[1];
            walk(rule, found, minBands);
            assertEquals(name.equals("overworld") ? 13 : 0, found[0], name + ": y_bands rules (the ring 2 strata)");
        }
    }

    /** The chain test reads as the script does: nested tests count, a second multiplier, stone depth or no end do not. */
    @Test
    void theChainTestIsTheScripts() {
        String chain = seq(yAbove(80, 1, false, BLOCK), yAbove(70, 1, false, seq(yAbove(75, 1, false, BLOCK), BLOCK)), BLOCK);
        assertEquals(3, chainBands(JsonParser.parseString(chain).getAsJsonObject()));
        String mixed = seq(yAbove(80, 2, false, BLOCK), yAbove(70, 1, false, BLOCK), BLOCK);
        assertEquals(-1, chainBands(JsonParser.parseString(mixed).getAsJsonObject()), "two multipliers");
        String depth = seq(yAbove(80, 1, true, BLOCK), BLOCK);
        assertEquals(-1, chainBands(JsonParser.parseString(depth).getAsJsonObject()), "add_stone_depth");
        String open = seq(yAbove(80, 1, false, BLOCK), yAbove(70, 1, false, BLOCK));
        assertEquals(-1, chainBands(JsonParser.parseString(open).getAsJsonObject()), "gives no block below Y 70");
        // What follows a rule that always gives a block never runs and does not count.
        String shadowed = seq(yAbove(80, 1, false, BLOCK), BLOCK, "{\"type\":\"minecraft:condition\",\"if_true\":{\"type\":\"minecraft:steep\"},\"then_run\":" + BLOCK + "}");
        assertEquals(1, chainBands(JsonParser.parseString(shadowed).getAsJsonObject()));
        assertTrue(scriptMinBands() > 3);
    }

    private static String seq(String... rules) {
        return "{\"type\":\"minecraft:sequence\",\"sequence\":[" + String.join(",", rules) + "]}";
    }

    private static String yAbove(int y, int multiplier, boolean addStoneDepth, String then) {
        return "{\"type\":\"minecraft:condition\",\"if_true\":{\"type\":\"minecraft:y_above\",\"anchor\":{\"absolute\":" + y
                + "},\"surface_depth_multiplier\":" + multiplier + ",\"add_stone_depth\":" + addStoneDepth + "},\"then_run\":" + then + "}";
    }

    /** {@code MIN_BANDS} of tools/worldgen/surface_bands.py. */
    private static int scriptMinBands() {
        Path script = WorldgenData.root().resolve("../../../../../../tools/worldgen/surface_bands.py").normalize();
        try {
            Matcher matcher = Pattern.compile("(?m)^MIN_BANDS = (\\d+)").matcher(Files.readString(script));
            assertTrue(matcher.find(), "MIN_BANDS in " + script);
            return Integer.parseInt(matcher.group(1));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The number of bands surface_bands.py makes of {@code rule} (its {@code flatten}, before it drops the unreachable
     * ones), or -1 if the script calls it no chain: a sequence of y_above tests (absolute anchor, no stone depth, one
     * surface depth multiplier) and plain blocks, nested to any depth, that always gives a block.
     */
    static int chainBands(JsonObject rule) {
        if (!type(rule).equals("minecraft:sequence")) {
            return -1;
        }
        int[] flat = flatten(rule, new Integer[1]);
        return flat != null && flat[1] == 1 ? flat[0] : -1;
    }

    /** {bands, 1 if the rule always gives a block}, or null for no chain. */
    private static int[] flatten(JsonObject rule, Integer[] multiplier) {
        switch (type(rule)) {
            case "minecraft:block" -> {
                return new int[]{0, 1};
            }
            case "minecraft:sequence" -> {
                int bands = 0;
                for (JsonElement child : rule.getAsJsonArray("sequence")) {
                    int[] sub = flatten(child.getAsJsonObject(), multiplier);
                    if (sub == null) {
                        return null;
                    }
                    bands += sub[0];
                    if (sub[1] == 1) {
                        return new int[]{bands, 1};
                    }
                }
                return new int[]{bands, 0};
            }
            case "minecraft:condition" -> {
                JsonObject test = rule.getAsJsonObject("if_true");
                JsonObject anchor = test.has("anchor") ? test.getAsJsonObject("anchor") : new JsonObject();
                if (!type(test).equals("minecraft:y_above") || (test.has("add_stone_depth") && test.get("add_stone_depth").getAsBoolean())
                        || !anchor.keySet().equals(Set.of("absolute"))) {
                    return null;
                }
                int m = test.get("surface_depth_multiplier").getAsInt();
                if (multiplier[0] == null) {
                    multiplier[0] = m;
                } else if (multiplier[0] != m) {
                    return null;
                }
                int[] sub = flatten(rule.getAsJsonObject("then_run"), multiplier);
                return sub == null ? null : new int[]{sub[0] + sub[1], 0};
            }
            default -> {
                return null;
            }
        }
    }

    private static String type(JsonObject rule) {
        return rule.has("type") ? rule.get("type").getAsString() : "";
    }

    private static void walk(JsonElement element, int[] found, int minBands) {
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> walk(child, found, minBands));
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject object = element.getAsJsonObject();
        int bands = chainBands(object);
        assertTrue(bands < minBands, "a chain of " + bands + " y_above bands: run tools/worldgen/surface_bands.py");
        if (type(object).equals("corruptedadventure:y_bands")) {
            found[0]++;
            JsonArray bandList = object.getAsJsonArray("bands");
            assertFalse(bandList.isEmpty());
            for (int i = 1; i < bandList.size(); i++) {
                assertTrue(bandList.get(i).getAsJsonObject().get("from").getAsInt() < bandList.get(i - 1).getAsJsonObject().get("from").getAsInt());
            }
        }
        object.entrySet().forEach(entry -> walk(entry.getValue(), found, minBands));
    }
}
