package nl.timtoys.corruptedadventure.datagen.model;

import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import nl.timtoys.corruptedadventure.registry.ModItems;

/** Item models of the fire, poison and glitch arrows (phase 2 golf 3 + 4 and 5, stream A), written by {@code ModItemModelProvider}. */
public final class ArrowItemModels {
    private ArrowItemModels() {
    }

    public static void build(ItemModelProvider models) {
        models.basicItem(ModItems.FIRE_ARROW.get());
        models.basicItem(ModItems.POISON_ARROW.get());
        models.basicItem(ModItems.GLITCH_ARROW.get());
    }
}
