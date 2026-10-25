package nl.timtoys.corruptedadventure.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Tiers;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The provisional tier curve (fase-2-items.md §4.3): sunstone at least netherite on every stat, every next tier strictly
 * above on uses, speed and damage and at least equal on the rest; and the generated gating tags (after {@code runData}).
 */
class GearTierTest {
    private static final String GENERATED = "src/generated/resources/data";
    private static final int NETHERITE_DURABILITY_FACTOR = 37;

    @Test
    void sunstoneIsAtLeastNetherite() {
        GearTier sunstone = GearTier.SUNSTONE;
        assertTrue(sunstone.getUses() >= Tiers.NETHERITE.getUses());
        assertTrue(sunstone.getSpeed() >= Tiers.NETHERITE.getSpeed());
        assertTrue(sunstone.getAttackDamageBonus() >= Tiers.NETHERITE.getAttackDamageBonus());
        assertTrue(sunstone.getEnchantmentValue() >= Tiers.NETHERITE.getEnchantmentValue());
        ArmorMaterial netherite = ArmorMaterials.NETHERITE.value();
        for (ArmorItem.Type type : ArmorItem.Type.values()) {
            assertTrue(sunstone.defense().get(type) >= netherite.getDefense(type), type.getName());
        }
        assertTrue(sunstone.toughness() >= netherite.toughness());
        assertTrue(sunstone.knockbackResistance() >= netherite.knockbackResistance());
        assertTrue(sunstone.durabilityFactor() >= NETHERITE_DURABILITY_FACTOR);
    }

    @Test
    void everyTierIsAStepAboveTheOneBefore() {
        GearTier[] tiers = GearTier.values();
        for (int i = 1; i < tiers.length; i++) {
            GearTier lower = tiers[i - 1];
            GearTier upper = tiers[i];
            String step = lower + " -> " + upper;
            assertTrue(upper.getUses() > lower.getUses(), step);
            assertTrue(upper.getSpeed() > lower.getSpeed(), step);
            assertTrue(upper.getAttackDamageBonus() > lower.getAttackDamageBonus(), step);
            assertTrue(upper.getEnchantmentValue() >= lower.getEnchantmentValue(), step);
            for (ArmorItem.Type type : ArmorItem.Type.values()) {
                assertTrue(upper.defense().get(type) >= lower.defense().get(type), step + " " + type.getName());
            }
            assertTrue(upper.toughness() >= lower.toughness(), step);
            assertTrue(upper.knockbackResistance() >= lower.knockbackResistance(), step);
            assertTrue(upper.durabilityFactor() > lower.durabilityFactor(), step);
            assertEquals(lower.ring() + 1, upper.ring(), step);
        }
    }

    /** Full armour stays within the vanilla attribute caps (armor 30, toughness 20). */
    @Test
    void fullArmourStaysWithinTheAttributeCaps() {
        for (GearTier tier : GearTier.values()) {
            int armor = tier.defense().get(ArmorItem.Type.HELMET) + tier.defense().get(ArmorItem.Type.CHESTPLATE)
                    + tier.defense().get(ArmorItem.Type.LEGGINGS) + tier.defense().get(ArmorItem.Type.BOOTS);
            assertTrue(armor <= 30, tier.toString());
            assertTrue(4 * tier.toughness() <= 20, tier.toString());
        }
    }

    @Test
    void everyVanillaTierFailsOnAllOurNeedsTags() throws IOException {
        Set<String> ours = new HashSet<>();
        for (GearTier tier : GearTier.values()) {
            ours.add("#corruptedadventure:needs_" + tier.getSerializedName() + "_tool");
        }
        for (String vanilla : new String[]{"wooden", "stone", "gold", "iron", "diamond", "netherite"}) {
            assertTrue(values("minecraft/tags/block/incorrect_for_" + vanilla + "_tool.json").containsAll(ours), vanilla);
        }
    }

    @Test
    void everyTierFailsExactlyOnTheHigherTiers() throws IOException {
        GearTier[] tiers = GearTier.values();
        for (int i = 0; i < tiers.length; i++) {
            Set<String> expected = new HashSet<>();
            for (int j = i + 1; j < tiers.length; j++) {
                expected.add("#corruptedadventure:needs_" + tiers[j].getSerializedName() + "_tool");
            }
            assertEquals(expected, values("corruptedadventure/tags/block/incorrect_for_" + tiers[i].getSerializedName()
                    + "_tool.json"), tiers[i].toString());
        }
    }

    @Test
    void ringOresAreInTheNeedsTagOfTheRingBefore() throws IOException {
        assertTrue(values("minecraft/tags/block/needs_diamond_tool.json").containsAll(ours("red_canyon_sunstone_ore",
                "orange_canyon_sunstone_ore", "cream_canyon_sunstone_ore", "scorched_sandstone_sunstone_ore",
                "red_canyon_turquoise_ore", "orange_canyon_turquoise_ore", "cream_canyon_turquoise_ore")));
        // The tier tags hold exactly the ring's ores and storage blocks (the latter like vanilla: what their ore needs).
        assertEquals(ours("volcanite_ore", "deepslate_volcanite_ore", "basalt_volcanite_ore", "sulfur_ore", "basalt_sulfur_ore",
                        "fire_ruby_ore", "deepslate_fire_ruby_ore", "basalt_fire_ruby_ore",
                        "raw_volcanite_block", "volcanite_block", "fire_ruby_block"),
                values("corruptedadventure/tags/block/needs_sunstone_tool.json"));
        assertEquals(ours("tidesilver_ore", "tidestone_tidesilver_ore", "deepslate_tidesilver_ore", "jade_ore",
                        "tidestone_jade_ore", "deepslate_jade_ore", "raw_tidesilver_block", "tidesilver_block", "jade_block"),
                values("corruptedadventure/tags/block/needs_volcanite_tool.json"));
        assertEquals(ours("nullite_ore", "voidstone_nullite_ore", "white_core_ore", "raw_nullite_block", "nullite_block"),
                values("corruptedadventure/tags/block/needs_tidesilver_tool.json"));
        assertEquals(Set.of(), values("corruptedadventure/tags/block/needs_nullite_tool.json"));
        Set<String> iron = values("minecraft/tags/block/needs_iron_tool.json");
        for (String moved : ours("red_canyon_sunstone_ore", "red_canyon_turquoise_ore", "volcanite_ore", "fire_ruby_ore")) {
            assertFalse(iron.contains(moved), moved);
        }
    }

    /**
     * Review B (F1): a needs tag that took a public {@code c:} tag would gate other mods' ores in it too (a modpack's
     * sulfur ore), since every vanilla tier fails on our needs tags.
     */
    @Test
    void needsTagsNeverTakeAConventionTag() throws IOException {
        for (String path : List.of("minecraft/tags/block/needs_iron_tool.json", "minecraft/tags/block/needs_diamond_tool.json",
                "corruptedadventure/tags/block/needs_sunstone_tool.json", "corruptedadventure/tags/block/needs_volcanite_tool.json",
                "corruptedadventure/tags/block/needs_tidesilver_tool.json", "corruptedadventure/tags/block/needs_nullite_tool.json")) {
            for (String value : values(path)) {
                assertFalse(value.startsWith("#c:"), path + " takes " + value);
            }
        }
    }

    private static Set<String> ours(String... names) {
        Set<String> ids = new HashSet<>();
        for (String name : names) {
            ids.add(CorruptedAdventure.MOD_ID + ":" + name);
        }
        return ids;
    }

    private static Set<String> values(String path) throws IOException {
        Set<String> values = new HashSet<>();
        for (JsonElement element : JsonParser.parseString(Files.readString(generated().resolve(path))).getAsJsonObject()
                .getAsJsonArray("values")) {
            values.add(element.getAsString());
        }
        return values;
    }

    private static Path generated() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            if (Files.isDirectory(dir.resolve(GENERATED))) {
                return dir.resolve(GENERATED);
            }
        }
        throw new IllegalStateException("No " + GENERATED + " above " + Path.of("").toAbsolutePath());
    }
}
