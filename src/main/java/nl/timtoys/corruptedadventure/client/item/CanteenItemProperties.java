package nl.timtoys.corruptedadventure.client.item;

import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import nl.timtoys.corruptedadventure.item.CanteenItem;
import nl.timtoys.corruptedadventure.registry.ModItems;

/** The canteen's model property: the sips left, read from the synced stack (the model overrides pick the texture). */
public final class CanteenItemProperties {
    private CanteenItemProperties() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(ModItems.CANTEEN.get(), CanteenItem.SIPS_PROPERTY,
                (stack, level, entity, seed) -> CanteenItem.sips(stack)));
    }
}
