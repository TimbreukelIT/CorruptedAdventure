package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.worldgen.biome.PlaceholderBiomes;
import nl.timtoys.corruptedadventure.worldgen.biome.VanillaBiomeCopies;

import java.util.Set;

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
        // A registry may be added only once, so all biome bootstraps share one lambda.
        event.createDatapackRegistryObjects(new RegistrySetBuilder()
                .add(Registries.BIOME, context -> {
                    PlaceholderBiomes.bootstrap(context);
                    VanillaBiomeCopies.bootstrap(context);
                })
                .add(Registries.WOLF_VARIANT, VanillaBiomeCopies::bootstrapWolfVariants),
                // "minecraft": the wolf variants override vanilla's entries.
                Set.of(CorruptedAdventure.MOD_ID, "minecraft"));
        // Created after the datapack registries, so their lookup contains our biomes.
        event.createProvider((output, lookup) -> new ModBiomeTagsProvider(output, lookup, event.getExistingFileHelper()));
        event.createProvider(ModDataMapProvider::new);
        event.createProvider(EnglishLanguageProvider::new);
        event.createProvider(DutchLanguageProvider::new);
    }
}
