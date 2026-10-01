package nl.timtoys.corruptedadventure.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.worldgen.feature.PalmTrunkPlacer;

/** Trunk placer types with their own code, used by the tree features from datagen. */
public final class ModTrunkPlacerTypes {
    private static final DeferredRegister<TrunkPlacerType<?>> TRUNK_PLACER_TYPES =
            DeferredRegister.create(Registries.TRUNK_PLACER_TYPE, CorruptedAdventure.MOD_ID);

    /** {@code corruptedadventure:palm_trunk_placer} */
    public static final DeferredHolder<TrunkPlacerType<?>, TrunkPlacerType<PalmTrunkPlacer>> PALM_TRUNK_PLACER =
            TRUNK_PLACER_TYPES.register("palm_trunk_placer", () -> new TrunkPlacerType<>(PalmTrunkPlacer.CODEC));

    private ModTrunkPlacerTypes() {
    }

    public static void register(IEventBus modBus) {
        TRUNK_PLACER_TYPES.register(modBus);
    }
}
