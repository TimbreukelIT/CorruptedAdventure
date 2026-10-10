package nl.timtoys.corruptedadventure.worldgen.feature;

import com.google.gson.JsonObject;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 47e (INT3, determinism read in reverse order): a crown on higher ground reaches the ground level of a neighbouring
 * chunk. Whichever chunk decorates first, the end is the same: a trunk or a fallen log may start in its leaves (and a
 * log replaces a leaf), and the crown takes an ash layer or fire flora there (replaceable by trees), which in turn keep
 * out of leaves already there.
 */
class EmberWoodsCrownOrderTest {
    /** {@code src/generated/resources/data}. */
    private static final Path DATA = WorldgenData.root().getParent().getParent().getParent().getParent().getParent()
            .resolve("generated/resources/data");

    @Test
    void groundFeaturesUnderACrownEndTheSameWhateverTheOrder() {
        String replaceable = WorldgenData.read(DATA.resolve("minecraft/tags/block/replaceable_by_trees.json")).toString();
        for (String block : new String[]{"ash_layer", "fire_bloom", "ember_berry_bush", "ash_sprouts"}) {
            assertTrue(replaceable.contains("\"corruptedadventure:" + block + "\""), block + " replaceable by trees");
        }
        String leaves = "\"corruptedadventure:ember_leaves\"";
        for (String placed : new String[]{"ember_woods_trees", "ember_woods_fallen_logs"}) {
            JsonObject json = WorldgenData.read(DATA.resolve("corruptedadventure/worldgen/placed_feature/ring_3/" + placed + ".json"));
            assertTrue(json.toString().contains(leaves), placed + " may start in a crown");
        }
        for (String log : new String[]{"fallen_charred_log_x", "fallen_charred_log_z"}) {
            JsonObject json = WorldgenData.read(DATA.resolve("corruptedadventure/worldgen/configured_feature/ring_3/" + log + ".json"));
            assertTrue(json.toString().contains(leaves), log + " grows through a crown");
        }
    }
}
