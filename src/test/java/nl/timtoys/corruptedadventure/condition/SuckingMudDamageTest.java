package nl.timtoys.corruptedadventure.condition;

import net.minecraft.world.Difficulty;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The sucking mud damage (package 79): difficulty floor like the poison, own damage type, tags and death message. */
class SuckingMudDamageTest {
    private static final String SUCKING_MUD = "\"corruptedadventure:sucking_mud\"";
    private static final String DEATH = "\"death.attack.corruptedadventure.sucking_mud\"";

    @Test
    void likeThePoisonNoneOnPeacefulDownToTenOnEasyLethalFromNormal() {
        assertEquals(0.0F, SuckingMudDamage.damage(2.0F, 20.0F, Difficulty.PEACEFUL));
        assertEquals(2.0F, SuckingMudDamage.damage(2.0F, 11.0F, Difficulty.EASY));
        assertEquals(0.0F, SuckingMudDamage.damage(2.0F, 10.0F, Difficulty.EASY));
        assertEquals(2.0F, SuckingMudDamage.damage(2.0F, 1.0F, Difficulty.NORMAL));
        assertEquals(2.0F, SuckingMudDamage.damage(2.0F, 1.0F, Difficulty.HARD));
    }

    @Test
    void noAmountNoDamage() {
        assertEquals(0.0F, SuckingMudDamage.damage(0.0F, 20.0F, Difficulty.HARD));
        assertEquals(0.0F, SuckingMudDamage.damage(-1.0F, 20.0F, Difficulty.HARD));
    }

    @Test
    void ownDamageTypeTaggedLikeInWall() throws IOException {
        Path data = PoisonDamageTypeTagsTest.generated();
        assertTrue(Files.isRegularFile(data.resolve("corruptedadventure/damage_type/sucking_mud.json")));
        assertTagged(data, "minecraft/tags/damage_type/bypasses_armor.json");
        assertTagged(data, "minecraft/tags/damage_type/no_knockback.json");
        assertTagged(data, "neoforge/tags/damage_type/is_environment.json");
        assertFalse(Files.readString(data.resolve("minecraft/tags/damage_type/bypasses_cooldown.json"))
                .contains(SUCKING_MUD), "the mud respects the hurt cooldown, like vanilla suffocation");
    }

    @Test
    void ownDeathMessageInEnglishAndDutch() throws IOException {
        Path lang = PoisonDamageTypeTagsTest.generated().resolveSibling("assets").resolve("corruptedadventure/lang");
        for (String file : new String[]{"en_us.json", "nl_nl.json"}) {
            String text = Files.readString(lang.resolve(file));
            assertTrue(text.contains(DEATH), file);
            assertTrue(text.contains("\"death.attack.corruptedadventure.sucking_mud.player\""), file);
        }
    }

    private static void assertTagged(Path data, String tag) throws IOException {
        assertTrue(Files.readString(data.resolve(tag)).contains(SUCKING_MUD), tag + " lacks " + SUCKING_MUD);
    }
}
