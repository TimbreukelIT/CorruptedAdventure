package nl.timtoys.corruptedadventure.datagen.model;

import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import nl.timtoys.corruptedadventure.registry.ModItems;

/**
 * Item models of the weakness items (phase 4, stream I), written by {@code ModItemModelProvider}. Flat like the echo
 * shell; the horn pose comes from the use animation.
 */
public final class WeaknessItemModels {
    private WeaknessItemModels() {
    }

    public static void build(ItemModelProvider models) {
        models.basicItem(ModItems.SUNSTONE_FLASH.get());
        models.basicItem(ModItems.VOLCANITE_HORN.get());
    }
}
