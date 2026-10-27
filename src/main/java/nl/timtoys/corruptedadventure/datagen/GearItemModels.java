package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredItem;
import nl.timtoys.corruptedadventure.registry.GearSet;
import nl.timtoys.corruptedadventure.registry.ModItems;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Item models of the gear tier sets ({@link ModItems#GEAR_SETS}). Tools are handheld; armour is flat with vanilla's
 * trim overrides ({@code ItemModelGenerators.generateArmorTrims}), so a trimmed or coated piece shows the trim in the
 * inventory too (the coatings borrow a vanilla material's {@code item_model_index}, fase-2-items.md §4.4).
 */
final class GearItemModels {
    /** Vanilla's trim materials and their {@code item_model_index}, ascending (the last matching override wins). */
    private static final Map<String, Float> TRIM_MATERIALS = new LinkedHashMap<>();
    private static final ExistingFileHelper.ResourceType TEXTURE =
            new ExistingFileHelper.ResourceType(PackType.CLIENT_RESOURCES, ".png", "textures");

    static {
        TRIM_MATERIALS.put("quartz", 0.1F);
        TRIM_MATERIALS.put("iron", 0.2F);
        TRIM_MATERIALS.put("netherite", 0.3F);
        TRIM_MATERIALS.put("redstone", 0.4F);
        TRIM_MATERIALS.put("copper", 0.5F);
        TRIM_MATERIALS.put("gold", 0.6F);
        TRIM_MATERIALS.put("emerald", 0.7F);
        TRIM_MATERIALS.put("diamond", 0.8F);
        TRIM_MATERIALS.put("lapis", 0.9F);
        TRIM_MATERIALS.put("amethyst", 1.0F);
    }

    private GearItemModels() {
    }

    static void build(ItemModelProvider models, ExistingFileHelper files) {
        for (GearSet set : ModItems.GEAR_SETS) {
            for (DeferredItem<? extends Item> tool : set.tools()) {
                models.handheldItem(tool.get());
            }
            for (DeferredItem<ArmorItem> armor : set.armor()) {
                trimmedArmor(models, files, armor.get());
            }
        }
    }

    private static void trimmedArmor(ItemModelProvider models, ExistingFileHelper files, ArmorItem armor) {
        String name = BuiltInRegistries.ITEM.getKey(armor).getPath();
        ResourceLocation texture = models.modLoc("item/" + name);
        ItemModelBuilder base = models.basicItem(armor);
        TRIM_MATERIALS.forEach((material, index) -> {
            // The trim overlays are not files: vanilla's armor_trims atlas makes them from the palettes.
            ResourceLocation overlay = ResourceLocation.withDefaultNamespace(
                    "trims/items/" + armor.getType().getName() + "_trim_" + material);
            files.trackGenerated(overlay, TEXTURE);
            String trimmed = name + "_" + material + "_trim";
            models.withExistingParent(trimmed, "item/generated").texture("layer0", texture).texture("layer1", overlay);
            base.override().predicate(ResourceLocation.withDefaultNamespace("trim_type"), index)
                    .model(new ModelFile.UncheckedModelFile(models.modLoc("item/" + trimmed))).end();
        });
    }
}
