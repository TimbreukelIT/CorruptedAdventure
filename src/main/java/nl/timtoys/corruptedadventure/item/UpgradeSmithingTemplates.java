package nl.timtoys.corruptedadventure.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.SmithingTemplateItem;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

import java.util.List;

/**
 * The upgrade smithing template of a {@link GearTier}, built like vanilla's netherite upgrade (same slot icons and
 * tooltip layout, our own texts). Placeholder in wave 1: no smithing recipe and no source yet (waves 2–5).
 */
public final class UpgradeSmithingTemplates {
    private static final List<ResourceLocation> GEAR_ICONS = List.of(
            vanilla("item/empty_armor_slot_helmet"), vanilla("item/empty_slot_sword"),
            vanilla("item/empty_armor_slot_chestplate"), vanilla("item/empty_slot_pickaxe"),
            vanilla("item/empty_armor_slot_leggings"), vanilla("item/empty_slot_axe"),
            vanilla("item/empty_armor_slot_boots"), vanilla("item/empty_slot_hoe"),
            vanilla("item/empty_slot_shovel"));
    private static final List<ResourceLocation> INGOT_ICONS = List.of(vanilla("item/empty_slot_ingot"));

    private UpgradeSmithingTemplates() {
    }

    public static SmithingTemplateItem create(GearTier tier) {
        String upgrade = upgradeName(tier);
        return new SmithingTemplateItem(
                Component.translatable(appliesToKey(tier)).withStyle(ChatFormatting.BLUE),
                Component.translatable(ingredientsKey(tier)).withStyle(ChatFormatting.BLUE),
                Component.translatable(upgradeKey(tier)).withStyle(ChatFormatting.GRAY),
                Component.translatable(templateKey(upgrade, "base_slot_description")),
                Component.translatable(templateKey(upgrade, "additions_slot_description")),
                GEAR_ICONS, INGOT_ICONS);
    }

    /** {@code <tier>_upgrade_smithing_template}, like {@code netherite_upgrade_smithing_template}. */
    public static String itemName(GearTier tier) {
        return upgradeName(tier) + "_smithing_template";
    }

    public static String appliesToKey(GearTier tier) {
        return templateKey(upgradeName(tier), "applies_to");
    }

    public static String ingredientsKey(GearTier tier) {
        return templateKey(upgradeName(tier), "ingredients");
    }

    public static String baseSlotKey(GearTier tier) {
        return templateKey(upgradeName(tier), "base_slot_description");
    }

    public static String additionsSlotKey(GearTier tier) {
        return templateKey(upgradeName(tier), "additions_slot_description");
    }

    /** {@code upgrade.corruptedadventure.<tier>_upgrade}: the tooltip title, like vanilla's "Netherite Upgrade". */
    public static String upgradeKey(GearTier tier) {
        return "upgrade." + CorruptedAdventure.MOD_ID + "." + upgradeName(tier);
    }

    private static String upgradeName(GearTier tier) {
        return tier.getSerializedName() + "_upgrade";
    }

    private static String templateKey(String upgrade, String part) {
        return "item." + CorruptedAdventure.MOD_ID + ".smithing_template." + upgrade + "." + part;
    }

    private static ResourceLocation vanilla(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }
}
