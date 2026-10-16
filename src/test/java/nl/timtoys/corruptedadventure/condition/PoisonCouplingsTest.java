package nl.timtoys.corruptedadventure.condition;

import net.minecraft.core.registries.BuiltInRegistries;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The couplings of the poison rule with the ring 4 blocks (package 79, {@code docs/plans/ring-4/contract.md} §3, §5),
 * on the generated block tags the rule reads. The toxic pool and the swamp gas must be sources once stream B registers
 * them (package 72); until then their check is skipped, and an empty tag (no JSON) is no error. Sucking mud, slime
 * trails, the glowcaps and the toxic crystal are never a source. The tags list blocks only (no nested tags), so the
 * text checks see everything the rule reads.
 */
class PoisonCouplingsTest {
    private static final String SOURCES = "corruptedadventure/tags/block/poison_sources.json";
    private static final String GASES = "corruptedadventure/tags/block/poison_gases.json";
    private static final List<String> NEVER_POISON = List.of("sucking_mud", "slime_trail", "glowcap",
            "blighted_glowcap", "toxic_crystal_block", "budding_toxic_crystal", "small_toxic_crystal_bud",
            "medium_toxic_crystal_bud", "large_toxic_crystal_bud", "toxic_crystal_cluster");

    @Test
    void theToxicPoolIsAPoolSource() throws IOException {
        assertCoupled("toxic_pool", SOURCES);
    }

    @Test
    void theSwampGasIsAGas() throws IOException {
        assertCoupled("swamp_gas", GASES);
    }

    @Test
    void mudTrailsGlowcapsAndCrystalAreNoSource() throws IOException {
        for (String tag : List.of(SOURCES, GASES)) {
            String values = read(tag);
            for (String name : NEVER_POISON) {
                assertFalse(values.contains(quoted(name)), tag + " must not hold " + name);
            }
            assertFalse(values.contains("\"minecraft:water\""), tag + " must not hold water");
            // Only direct entries, so the checks above see everything the rule reads.
            assertFalse(values.contains("\"#"), tag + " must list blocks, not other tags");
        }
    }

    /** Once {@code name} is a registered block, it is in {@code tag}. */
    private static void assertCoupled(String name, String tag) throws IOException {
        if (BuiltInRegistries.BLOCK.containsKey(CorruptedAdventure.id(name))) {
            assertTrue(read(tag).contains(quoted(name)), tag + " lacks " + name);
        }
    }

    /** The tag's JSON, or empty if the tag is empty (datagen writes no file then). */
    private static String read(String tag) throws IOException {
        Path file = PoisonDamageTypeTagsTest.generated().resolve(tag);
        return Files.isRegularFile(file) ? Files.readString(file) : "";
    }

    private static String quoted(String name) {
        return "\"" + CorruptedAdventure.id(name) + "\"";
    }
}
