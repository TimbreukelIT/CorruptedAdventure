package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.condition.ConditionDamageTypes;
import nl.timtoys.corruptedadventure.worldgen.biome.PlaceholderBiomes;
import nl.timtoys.corruptedadventure.worldgen.biome.Ring1BiomeModifiers;
import nl.timtoys.corruptedadventure.worldgen.biome.Ring2BiomeModifiers;
import nl.timtoys.corruptedadventure.worldgen.biome.Ring2Biomes;
import nl.timtoys.corruptedadventure.worldgen.carver.Ring2Carvers;
import nl.timtoys.corruptedadventure.worldgen.biome.TransitionBiomes;
import nl.timtoys.corruptedadventure.worldgen.biome.VanillaBiomeCopies;
import nl.timtoys.corruptedadventure.worldgen.feature.NetherRing1Features;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring1HorrorFeatures;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring2DressingFeatures;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring2Features;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring2FlatsFeatures;
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
        // A registry may be added only once, so all bootstraps of one registry share one lambda.
        event.createDatapackRegistryObjects(new RegistrySetBuilder()
                .add(Registries.CONFIGURED_FEATURE, context -> {
                    WitheredExpanseFeatures.configured(context);
                    Ring1HorrorFeatures.configured(context);
                    NetherRing1Features.configured(context);
                    Ring2Features.configured(context);
                    Ring2FlatsFeatures.configured(context);
                    Ring2DressingFeatures.configured(context);
                })
                .add(Registries.CONFIGURED_CARVER, Ring2Carvers::bootstrap)
                .add(Registries.PLACED_FEATURE, context -> {
                    WitheredExpanseFeatures.placed(context);
                    Ring1HorrorFeatures.placed(context);
                    NetherRing1Features.placed(context);
                    Ring2Features.placed(context);
                    Ring2FlatsFeatures.placed(context);
                    Ring2DressingFeatures.placed(context);
                })
                .add(Registries.BIOME, context -> {
                    PlaceholderBiomes.bootstrap(context);
                    VanillaBiomeCopies.bootstrap(context);
                    TransitionBiomes.bootstrap(context);
                    Ring2Biomes.bootstrap(context);
                })
                .add(Registries.WOLF_VARIANT, VanillaBiomeCopies::bootstrapWolfVariants)
                .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, context -> {
                    Ring1BiomeModifiers.bootstrap(context);
                    Ring2BiomeModifiers.bootstrap(context);
                })
                .add(Registries.DAMAGE_TYPE, ConditionDamageTypes::bootstrap),
                // "minecraft": the wolf variants override vanilla's entries.
                Set.of(CorruptedAdventure.MOD_ID, "minecraft"));
        // Created after the datapack registries, so their lookup contains our biomes.
        event.createProvider((output, lookup) -> new ModBiomeTagsProvider(output, lookup, event.getExistingFileHelper()));
        event.createProvider(ModDataMapProvider::new);
        event.createProvider((output, lookup) -> new ModDamageTypeTagsProvider(output, lookup, event.getExistingFileHelper()));

        ExistingFileHelper files = event.getExistingFileHelper();
        // Models check that their textures exist in src/main/resources: run tools/textures/Ring1Textures.java first.
        event.createProvider(output -> new ModBlockStateProvider(output, files));
        event.createProvider(output -> new ModItemModelProvider(output, files));
        event.createProvider(ModBlockLootProvider::create);
        ModBlockTagsProvider blockTags = event.createProvider((output, lookup) -> new ModBlockTagsProvider(output, lookup, files));
        event.createProvider((output, lookup) -> new ModItemTagsProvider(output, lookup, blockTags.contentsGetter(), files));
        event.createProvider(output -> new ModSoundDefinitionsProvider(output, files));
        event.createProvider(output -> new ModParticleDescriptionProvider(output, files));

        event.createProvider(EnglishLanguageProvider::new);
        event.createProvider(DutchLanguageProvider::new);
    }
}
