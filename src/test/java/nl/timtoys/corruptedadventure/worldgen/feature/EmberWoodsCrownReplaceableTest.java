package nl.timtoys.corruptedadventure.worldgen.feature;

import com.google.gson.JsonObject;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 47e (INT3, determinism read in reverse order): a crown on higher ground reaches the ground level of a neighbouring
 * chunk. For the end to be the same whichever chunk decorates first, the generated data must let a trunk or a fallen
 * log start in ember leaves (a log replaces a leaf), and make the ash layer and fire flora replaceable by trees (they in
 * turn keep out of leaves already there). This checks that data (tag and feature JSON), not a decoration run.
 */
class EmberWoodsCrownReplaceableTest {
    private static final String GENERATED = "src/generated/resources/data";

    @Test
    void groundFeaturesUnderACrownAreReplaceableAndLogsMayStartInLeaves() {
        Path data = generated();
        String replaceable = WorldgenData.read(data.resolve("minecraft/tags/block/replaceable_by_trees.json")).toString();
        for (String block : new String[]{"ash_layer", "fire_bloom", "ember_berry_bush", "ash_sprouts"}) {
            assertTrue(replaceable.contains("\"corruptedadventure:" + block + "\""), block + " replaceable by trees");
        }
        String leaves = "\"corruptedadventure:ember_leaves\"";
        for (String placed : new String[]{"ember_woods_trees", "ember_woods_fallen_logs"}) {
            JsonObject json = WorldgenData.read(data.resolve("corruptedadventure/worldgen/placed_feature/ring_3/" + placed + ".json"));
            assertTrue(json.toString().contains(leaves), placed + " may start in a crown");
        }
        for (String log : new String[]{"fallen_charred_log_x", "fallen_charred_log_z"}) {
            JsonObject json = WorldgenData.read(data.resolve("corruptedadventure/worldgen/configured_feature/ring_3/" + log + ".json"));
            assertTrue(json.toString().contains(leaves), log + " grows through a crown");
        }
    }

    /** {@code src/generated/resources/data}, found upwards from the working directory (like {@link WorldgenData#root()}). */
    private static Path generated() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            if (Files.isDirectory(dir.resolve(GENERATED))) {
                return dir.resolve(GENERATED);
            }
        }
        throw new IllegalStateException("Could not find " + GENERATED);
    }
}
