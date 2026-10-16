package nl.timtoys.corruptedadventure.condition;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The generated damage-type tags of {@code corruptedadventure:poison} ({@code Ring4DamageTypeTags}, after
 * {@code runData}). {@code bypasses_cooldown} matters: the vanilla poison of the second threshold (1.0 every 25 ticks)
 * often hits a few ticks before the wither damage, and inside the 10-tick hurt cooldown a hit of equal size would be
 * swallowed.
 */
class PoisonDamageTypeTagsTest {
    private static final String GENERATED = "src/generated/resources/data";
    private static final String POISON = "\"corruptedadventure:poison\"";

    @Test
    void poisonDamageBypassesTheHurtCooldownAndArmour() throws IOException {
        assertTagged("minecraft/tags/damage_type/bypasses_cooldown.json");
        assertTagged("minecraft/tags/damage_type/bypasses_armor.json");
        assertTagged("minecraft/tags/damage_type/no_knockback.json");
        assertTagged("neoforge/tags/damage_type/is_poison.json");
        assertTagged("neoforge/tags/damage_type/is_environment.json");
        assertTrue(Files.isRegularFile(generated().resolve("corruptedadventure/damage_type/poison.json")));
    }

    private static void assertTagged(String tag) throws IOException {
        assertTrue(Files.readString(generated().resolve(tag)).contains(POISON), tag + " lacks " + POISON);
    }

    /** {@code src/generated/resources/data}, found upwards from the working directory. */
    static Path generated() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            if (Files.isDirectory(dir.resolve(GENERATED))) {
                return dir.resolve(GENERATED);
            }
        }
        throw new IllegalStateException("No " + GENERATED + " above " + Path.of("").toAbsolutePath());
    }
}
