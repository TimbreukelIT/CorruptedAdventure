package nl.timtoys.corruptedadventure.datagen.model;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import nl.timtoys.corruptedadventure.item.CanteenItem;
import nl.timtoys.corruptedadventure.item.HandWarmerItem;
import nl.timtoys.corruptedadventure.registry.ModItems;

/**
 * Item models of the consumables (phase 2 golf 2 stream V, golf 3 + 4 stream C), written by
 * {@code ModItemModelProvider}. The canteen's own model is the empty one (texture {@code canteen_empty}); from one sip on,
 * the override {@code canteen_filled} shows texture {@code canteen}. The hand warmer works the same with its charges.
 */
public final class ConsumableItemModels {
    private ConsumableItemModels() {
    }

    public static void build(ItemModelProvider models) {
        emptyOrFilled(models, ModItems.CANTEEN.getId().getPath(), CanteenItem.SIPS_PROPERTY);
        emptyOrFilled(models, ModItems.HAND_WARMER.getId().getPath(), HandWarmerItem.CHARGES_PROPERTY);
        models.basicItem(ModItems.CACTUS_JUICE.get());
        models.basicItem(ModItems.SALTED_JERKY.get());
        models.basicItem(ModItems.DEW_DRAUGHT.get());
        models.basicItem(ModItems.PURIFIED_WATER.get());
    }

    /** Model {@code name} shows texture {@code <name>_empty}; from {@code property} 1 on, {@code <name>_filled} shows
     * {@code <name>}. */
    private static void emptyOrFilled(ItemModelProvider models, String name, ResourceLocation property) {
        ItemModelBuilder filled = models.withExistingParent(name + "_filled", models.mcLoc("item/generated"))
                .texture("layer0", models.modLoc("item/" + name));
        models.withExistingParent(name, models.mcLoc("item/generated"))
                .texture("layer0", models.modLoc("item/" + name + "_empty"))
                .override().predicate(property, 1.0F).model(filled).end();
    }
}
