package nl.timtoys.corruptedadventure.datagen.lang;

import net.minecraft.world.item.Item;
import nl.timtoys.corruptedadventure.item.GearTier;
import nl.timtoys.corruptedadventure.item.UpgradeSmithingTemplates;
import nl.timtoys.corruptedadventure.registry.ModItems;

import java.util.Map;
import java.util.function.Supplier;

/**
 * The texts of the gear tiers (phase 2 wave 1, stream B): the upgrade smithing templates, laid out like vanilla's
 * netherite upgrade (the item is called "Smithing Template", the tooltip names the upgrade). Which gear a template
 * upgrades is provisional (fase-2-items.md §4.3): sunstone takes diamond gear, every next tier the one before.
 */
public final class Golf1TierNames {
    private static final Map<GearTier, Supplier<? extends Item>> TEMPLATES = Map.of(
            GearTier.SUNSTONE, ModItems.SUNSTONE_UPGRADE_SMITHING_TEMPLATE,
            GearTier.VOLCANITE, ModItems.VOLCANITE_UPGRADE_SMITHING_TEMPLATE,
            GearTier.TIDESILVER, ModItems.TIDESILVER_UPGRADE_SMITHING_TEMPLATE,
            GearTier.NULLITE, ModItems.NULLITE_UPGRADE_SMITHING_TEMPLATE);

    private Golf1TierNames() {
    }

    public static void addEnglish(LangOutput lang) {
        add(lang, "Smithing Template", "%s Upgrade", "%s Ingot", "%s Equipment", "Add %s armor, weapon, or tool",
                "Add %s Ingot", Map.of(GearTier.SUNSTONE, "Sunstone", GearTier.VOLCANITE, "Volcanite",
                        GearTier.TIDESILVER, "Tidesilver", GearTier.NULLITE, "Nullite"), "Diamond", false);
    }

    public static void addDutch(LangOutput lang) {
        add(lang, "Smeedsjabloon", "%s-upgrade", "%sstaaf", "%suitrusting", "Voeg %swapen, -gereedschap of -harnas toe",
                "Voeg %sstaaf toe", Map.of(GearTier.SUNSTONE, "Zonnesteen", GearTier.VOLCANITE, "Vulkaniet",
                        GearTier.TIDESILVER, "Getijdezilver", GearTier.NULLITE, "Nulliet"), "Diamant", true);
    }

    /**
     * {@code previous} is the material of the gear a template upgrades: diamond for the first tier, then the tier
     * before. The base slot text uses the lower-case material, like vanilla's "Add diamond armor, weapon, or tool"; Dutch
     * also writes the ingot in lower case mid-sentence ("Voeg netherietstaaf toe").
     */
    private static void add(LangOutput lang, String item, String upgrade, String ingot, String equipment, String baseSlot,
                            String additionsSlot, Map<GearTier, String> names, String firstPrevious,
                            boolean lowerCaseIngot) {
        for (GearTier tier : GearTier.values()) {
            String name = names.get(tier);
            String previous = tier.ordinal() == 0 ? firstPrevious : names.get(GearTier.values()[tier.ordinal() - 1]);
            lang.addItem(TEMPLATES.get(tier), item);
            lang.add(UpgradeSmithingTemplates.upgradeKey(tier), upgrade.formatted(name));
            lang.add(UpgradeSmithingTemplates.appliesToKey(tier), equipment.formatted(previous));
            lang.add(UpgradeSmithingTemplates.ingredientsKey(tier), ingot.formatted(name));
            lang.add(UpgradeSmithingTemplates.baseSlotKey(tier), baseSlot.formatted(lowerFirst(previous)));
            lang.add(UpgradeSmithingTemplates.additionsSlotKey(tier), additionsSlot.formatted(lowerCaseIngot ? lowerFirst(name) : name));
        }
    }

    private static String lowerFirst(String text) {
        return Character.toLowerCase(text.charAt(0)) + text.substring(1);
    }
}
