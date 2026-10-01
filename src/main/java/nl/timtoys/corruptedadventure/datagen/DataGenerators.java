package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.worldgen.biome.PlaceholderBiomes;
import nl.timtoys.corruptedadventure.worldgen.biome.TransitionBiomes;
import nl.timtoys.corruptedadventure.worldgen.biome.VanillaBiomeCopies;
import nl.timtoys.corruptedadventure.worldgen.feature.WitheredExpanseFeatures;

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
                .add(Registries.CONFIGURED_FEATURE, WitheredExpanseFeatures::configured)
                .add(Registries.PLACED_FEATURE, WitheredExpanseFeatures::placed)
                .add(Registries.BIOME, context -> {
                    PlaceholderBiomes.bootstrap(context);
                    VanillaBiomeCopies.bootstrap(context);
                    TransitionBiomes.bootstrap(context);
                })
                .add(Registries.WOLF_VARIANT, VanillaBiomeCopies::bootstrapWolfVariants),
                // "minecraft": the wolf variants override vanilla's entries.
                Set.of(CorruptedAdventure.MOD_ID, "minecraft"));
        // Created after the datapack registries, so their lookup contains our biomes.
        event.createProvider((output, lookup) -> new ModBiomeTagsProvider(output, lookup, event.getExistingFileHelper()));
        event.createProvider(ModDataMapProvider::new);

        ExistingFileHelper files = event.getExistingFileHelper();
        // Models check that their textures exist in src/main/resources: run tools/textures/Ring1Textures.java first.
        event.createProvider(output -> new ModBlockStateProvider(output, files));
        event.createProvider(output -> new ModItemModelProvider(output, files));
        event.createProvider(ModBlockLootProvider::create);
        ModBlockTagsProvider blockTags = event.createProvider((output, lookup) -> new ModBlockTagsProvider(output, lookup, files));
        event.createProvider((output, lookup) -> new ModItemTagsProvider(output, lookup, blockTags.contentsGetter(), files));

        event.createProvider(EnglishLanguageProvider::new);
        event.createProvider(DutchLanguageProvider::new);
    }
}
