package nl.timtoys.corruptedadventure.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.worldgen.feature.RingChanceFilter;

public final class ModPlacementModifierTypes {
    private static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_MODIFIER_TYPES =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, CorruptedAdventure.MOD_ID);

    /** {@code corruptedadventure:ring_chance} */
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<RingChanceFilter>> RING_CHANCE =
            PLACEMENT_MODIFIER_TYPES.register("ring_chance", () -> () -> RingChanceFilter.CODEC);

    private ModPlacementModifierTypes() {
    }

    public static void register(IEventBus modBus) {
        PLACEMENT_MODIFIER_TYPES.register(modBus);
    }
}
