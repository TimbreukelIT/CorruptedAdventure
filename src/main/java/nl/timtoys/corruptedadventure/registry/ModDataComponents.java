package nl.timtoys.corruptedadventure.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.item.CanteenItem;

/** Item data components. */
public final class ModDataComponents {
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CorruptedAdventure.MOD_ID);

    /**
     * Sips left in a canteen (0 to {@link CanteenItem#MAX_SIPS}). Saved and synced to the client, which only reads it
     * for the tooltip, the bar and the model. Absent = empty, so a crafted canteen needs no default.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SIPS =
            COMPONENTS.registerComponentType("sips", builder -> builder
                    .persistent(Codec.intRange(0, CanteenItem.MAX_SIPS))
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    private ModDataComponents() {
    }

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }
}
