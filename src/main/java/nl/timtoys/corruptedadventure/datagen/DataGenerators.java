package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import nl.timtoys.corruptedadventure.worldgen.biome.PlaceholderBiomes;

/**
 * Hooks all data providers into {@code ./gradlew runData}; output goes to {@code src/generated/resources}.
 */
public final class DataGenerators {
    private DataGenerators() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(DataGenerators::gatherData);
    }

    private static void gatherData(GatherDataEvent event) {
        event.createDatapackRegistryObjects(new RegistrySetBuilder()
                .add(Registries.BIOME, PlaceholderBiomes::bootstrap));
        event.createProvider(EnglishLanguageProvider::new);
        event.createProvider(DutchLanguageProvider::new);
    }
}
