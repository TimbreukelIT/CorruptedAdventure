package nl.timtoys.corruptedadventure.item.coating;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.SmithingTemplateItem;

import java.util.List;

/**
 * The coating templates: vanilla armor trim templates, except that the tooltip and the empty addition slot name the one
 * material of the coating (vanilla's text says "Ingots & Crystals").
 */
public final class CoatingTemplates {
    /** {@code item.corruptedadventure.smithing_template.coating.additions_slot_description}, with the material name. */
    public static final String ADDITIONS_SLOT_KEY = "item.corruptedadventure.smithing_template.coating.additions_slot_description";

    private static final String VANILLA = "item.minecraft.smithing_template.armor_trim.";
    private static final List<ResourceLocation> ARMOR_ICONS = List.of(
            vanillaIcon("empty_armor_slot_helmet"), vanillaIcon("empty_armor_slot_chestplate"),
            vanillaIcon("empty_armor_slot_leggings"), vanillaIcon("empty_armor_slot_boots"));
    private static final List<ResourceLocation> MATERIAL_ICONS = List.of(
            vanillaIcon("empty_slot_amethyst_shard"), vanillaIcon("empty_slot_diamond"), vanillaIcon("empty_slot_quartz"));

    private CoatingTemplates() {
    }

    public static SmithingTemplateItem create(Coating coating) {
        Component material = Component.translatable(coating.material().descriptionId());
        return new SmithingTemplateItem(
                Component.translatable(VANILLA + "applies_to").withStyle(ChatFormatting.BLUE),
                material.copy().withStyle(ChatFormatting.BLUE),
                ModTrimPatterns.description(coating).withStyle(ChatFormatting.GRAY),
                Component.translatable(VANILLA + "base_slot_description"),
                Component.translatable(ADDITIONS_SLOT_KEY, material),
                ARMOR_ICONS, MATERIAL_ICONS);
    }

    private static ResourceLocation vanillaIcon(String name) {
        return ResourceLocation.withDefaultNamespace("item/" + name);
    }
}
