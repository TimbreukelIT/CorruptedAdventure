package nl.timtoys.corruptedadventure.client.item;

import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import nl.timtoys.corruptedadventure.item.CanteenItem;
import nl.timtoys.corruptedadventure.item.HandWarmerItem;
import nl.timtoys.corruptedadventure.registry.ModItems;

/**
 * The model properties of the consumables with uses left: the canteen's sips and the hand warmer's charges, read from the
 * synced stack (the model overrides pick the texture).
 */
public final class ConsumableItemProperties {
    private ConsumableItemProperties() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(ModItems.CANTEEN.get(), CanteenItem.SIPS_PROPERTY,
                    (stack, level, entity, seed) -> CanteenItem.sips(stack));
            ItemProperties.register(ModItems.HAND_WARMER.get(), HandWarmerItem.CHARGES_PROPERTY,
                    (stack, level, entity, seed) -> HandWarmerItem.charges(stack));
        });
    }
}
