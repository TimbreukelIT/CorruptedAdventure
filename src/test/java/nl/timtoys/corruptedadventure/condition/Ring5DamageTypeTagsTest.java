package nl.timtoys.corruptedadventure.condition;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The generated damage type {@code corruptedadventure:void} and its tags ({@code Ring5DamageTypeTags}, after
 * {@code runData}): through armour, no knockback, past the hurt cooldown, environment; like the poison's wither, so not
 * {@code is_poison}.
 */
class Ring5DamageTypeTagsTest {
    private static final String VOID = "\"corruptedadventure:void\"";
    private static final String SILENCE = "\"corruptedadventure:silence\"";

    @Test
    void voidDamageBypassesArmourAndTheHurtCooldown() throws IOException {
        Path generated = PoisonDamageTypeTagsTest.generated();
        assertTagged(generated, "minecraft/tags/damage_type/bypasses_armor.json");
        assertTagged(generated, "minecraft/tags/damage_type/no_knockback.json");
        assertTagged(generated, "minecraft/tags/damage_type/bypasses_cooldown.json");
        assertTagged(generated, "neoforge/tags/damage_type/is_environment.json");
        assertFalse(Files.readString(generated.resolve("neoforge/tags/damage_type/is_poison.json")).contains(VOID));
        String type = Files.readString(generated.resolve("corruptedadventure/damage_type/void.json"));
        assertTrue(type.contains("\"corruptedadventure.void\""), type);
    }

    /** The silence's heartbeat (package 97) is tagged like the void damage. */
    @Test
    void theHeartbeatIsTaggedLikeTheVoid() throws IOException {
        Path generated = PoisonDamageTypeTagsTest.generated();
        for (String tag : new String[]{"minecraft/tags/damage_type/bypasses_armor.json",
                "minecraft/tags/damage_type/no_knockback.json", "minecraft/tags/damage_type/bypasses_cooldown.json",
                "neoforge/tags/damage_type/is_environment.json"}) {
            assertTagged(generated, tag, SILENCE);
        }
        assertFalse(Files.readString(generated.resolve("neoforge/tags/damage_type/is_poison.json")).contains(SILENCE));
        String type = Files.readString(generated.resolve("corruptedadventure/damage_type/silence.json"));
        assertTrue(type.contains("\"corruptedadventure.silence\""), type);
    }

    private static void assertTagged(Path generated, String tag) throws IOException {
        assertTagged(generated, tag, VOID);
    }

    private static void assertTagged(Path generated, String tag, String entry) throws IOException {
        assertTrue(Files.readString(generated.resolve(tag)).contains(entry), tag + " lacks " + entry);
    }
}
