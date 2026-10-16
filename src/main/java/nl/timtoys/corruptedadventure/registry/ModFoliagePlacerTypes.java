package nl.timtoys.corruptedadventure.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.worldgen.feature.PalmFoliagePlacer;
import nl.timtoys.corruptedadventure.worldgen.feature.VerdantFoliagePlacer;

/** Foliage placer types with their own code, used by the tree features from datagen. */
public final class ModFoliagePlacerTypes {
    private static final DeferredRegister<FoliagePlacerType<?>> FOLIAGE_PLACER_TYPES =
            DeferredRegister.create(Registries.FOLIAGE_PLACER_TYPE, CorruptedAdventure.MOD_ID);

    /** {@code corruptedadventure:palm_foliage_placer} */
    public static final DeferredHolder<FoliagePlacerType<?>, FoliagePlacerType<PalmFoliagePlacer>> PALM_FOLIAGE_PLACER =
            FOLIAGE_PLACER_TYPES.register("palm_foliage_placer", () -> new FoliagePlacerType<>(PalmFoliagePlacer.CODEC));

    // ring 4 — W-biomes
    /** {@code corruptedadventure:verdant_foliage_placer} (75b: umbrella crown and fern fronds of verdant_falls) */
    public static final DeferredHolder<FoliagePlacerType<?>, FoliagePlacerType<VerdantFoliagePlacer>> VERDANT_FOLIAGE_PLACER =
            FOLIAGE_PLACER_TYPES.register("verdant_foliage_placer", () -> new FoliagePlacerType<>(VerdantFoliagePlacer.CODEC));

    private ModFoliagePlacerTypes() {
    }

    public static void register(IEventBus modBus) {
        FOLIAGE_PLACER_TYPES.register(modBus);
    }
}
