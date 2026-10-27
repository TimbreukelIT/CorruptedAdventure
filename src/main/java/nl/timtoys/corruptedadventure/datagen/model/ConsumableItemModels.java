package nl.timtoys.corruptedadventure.datagen.model;

import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import nl.timtoys.corruptedadventure.item.CanteenItem;
import nl.timtoys.corruptedadventure.registry.ModItems;

/**
 * Item models of the consumables (phase 2 golf 2, stream V), written by {@code ModItemModelProvider}. The canteen's own
 * model is the empty one (texture {@code canteen_empty}); from one sip on, the override {@code canteen_filled} shows
 * texture {@code canteen}.
 */
public final class ConsumableItemModels {
    private ConsumableItemModels() {
    }

    public static void build(ItemModelProvider models) {
        String canteen = ModItems.CANTEEN.getId().getPath();
        ItemModelBuilder filled = models.withExistingParent(canteen + "_filled", models.mcLoc("item/generated"))
                .texture("layer0", models.modLoc("item/" + canteen));
        models.withExistingParent(canteen, models.mcLoc("item/generated"))
                .texture("layer0", models.modLoc("item/" + canteen + "_empty"))
                .override().predicate(CanteenItem.SIPS_PROPERTY, 1.0F).model(filled).end();
        models.basicItem(ModItems.CACTUS_JUICE.get());
        models.basicItem(ModItems.SALTED_JERKY.get());
    }
}
