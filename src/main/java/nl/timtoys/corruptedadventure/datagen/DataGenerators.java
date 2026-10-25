package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.condition.ConditionDamageTypes;
import nl.timtoys.corruptedadventure.condition.Ring3DamageTypes;
import nl.timtoys.corruptedadventure.condition.Ring4DamageTypes;
import nl.timtoys.corruptedadventure.condition.Ring5DamageTypes;
import nl.timtoys.corruptedadventure.item.coating.ModTrimMaterials;
import nl.timtoys.corruptedadventure.item.coating.ModTrimPatterns;
import nl.timtoys.corruptedadventure.worldgen.biome.NetherRing2Biomes;
import nl.timtoys.corruptedadventure.worldgen.biome.NetherRing3BiomeModifiers;
import nl.timtoys.corruptedadventure.worldgen.biome.NetherRing3Biomes;
import nl.timtoys.corruptedadventure.worldgen.biome.NetherRing4BiomeModifiers;
import nl.timtoys.corruptedadventure.worldgen.biome.NetherRing4Biomes;
import nl.timtoys.corruptedadventure.worldgen.biome.NetherRing5BiomeModifiers;
import nl.timtoys.corruptedadventure.worldgen.biome.NetherRing5Biomes;
import nl.timtoys.corruptedadventure.worldgen.biome.PlaceholderBiomes;
import nl.timtoys.corruptedadventure.worldgen.biome.Ring1BiomeModifiers;
import nl.timtoys.corruptedadventure.worldgen.biome.Ring2BiomeModifiers;
import nl.timtoys.corruptedadventure.worldgen.biome.Ring2Biomes;
import nl.timtoys.corruptedadventure.worldgen.biome.Ring3BiomeModifiers;
import nl.timtoys.corruptedadventure.worldgen.biome.Ring3Biomes;
import nl.timtoys.corruptedadventure.worldgen.biome.Ring4BiomeModifiers;
import nl.timtoys.corruptedadventure.worldgen.biome.Ring4Biomes;
import nl.timtoys.corruptedadventure.worldgen.biome.Ring5BiomeModifiers;
import nl.timtoys.corruptedadventure.worldgen.biome.Ring5Biomes;
import nl.timtoys.corruptedadventure.worldgen.biome.TransitionBiomes;
import nl.timtoys.corruptedadventure.worldgen.biome.VanillaBiomeCopies;
import nl.timtoys.corruptedadventure.worldgen.carver.NetherRing2Carvers;
import nl.timtoys.corruptedadventure.worldgen.carver.NetherRing3Carvers;
import nl.timtoys.corruptedadventure.worldgen.carver.NetherRing4Carvers;
import nl.timtoys.corruptedadventure.worldgen.carver.NetherRing5Carvers;
import nl.timtoys.corruptedadventure.worldgen.carver.Ring2Carvers;
import nl.timtoys.corruptedadventure.worldgen.carver.Ring3Carvers;
import nl.timtoys.corruptedadventure.worldgen.carver.Ring4Carvers;
import nl.timtoys.corruptedadventure.worldgen.carver.Ring5Carvers;
import nl.timtoys.corruptedadventure.worldgen.feature.DryRiverbedsFeatures;
import nl.timtoys.corruptedadventure.worldgen.feature.NetherRing1Features;
import nl.timtoys.corruptedadventure.worldgen.feature.NetherRing2Features;
import nl.timtoys.corruptedadventure.worldgen.feature.NetherRing3Features;
import nl.timtoys.corruptedadventure.worldgen.feature.NetherRing4Features;
import nl.timtoys.corruptedadventure.worldgen.feature.NetherRing5Features;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring1HorrorFeatures;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring2DressingFeatures;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring2RockFeatures;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring2FlatLandmarkFeatures;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring2Features;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring2FlatsFeatures;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring2FlatsLifeFeatures;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring2RockLandmarkFeatures;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring3Features;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring4Features;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring5Features;
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
        // A registry may be added only once, so all bootstraps of one registry share one lambda. Rings 3, 4 and 5 and their
        // Nether rings have one entry class per stream (and registry); a stream calls its further classes from there.
        event.createDatapackRegistryObjects(new RegistrySetBuilder()
                .add(Registries.CONFIGURED_FEATURE, context -> {
                    WitheredExpanseFeatures.configured(context);
                    Ring1HorrorFeatures.configured(context);
                    NetherRing1Features.configured(context);
                    NetherRing2Features.configured(context);
                    Ring2Features.configured(context);
                    Ring2FlatsFeatures.configured(context);
                    Ring2DressingFeatures.configured(context);
                    Ring2FlatsLifeFeatures.configured(context);
                    Ring2RockLandmarkFeatures.configured(context);
                    Ring2FlatLandmarkFeatures.configured(context);
                    DryRiverbedsFeatures.configured(context);
                    Ring2RockFeatures.configured(context);
                    Ring3Features.configured(context);
                    NetherRing3Features.configured(context);
                    Ring4Features.configured(context);
                    NetherRing4Features.configured(context);
                    Ring5Features.configured(context);
                    NetherRing5Features.configured(context);
                })
                .add(Registries.CONFIGURED_CARVER, context -> {
                    Ring2Carvers.bootstrap(context);
                    NetherRing2Carvers.bootstrap(context);
                    Ring3Carvers.bootstrap(context);
                    NetherRing3Carvers.bootstrap(context);
                    Ring4Carvers.bootstrap(context);
                    NetherRing4Carvers.bootstrap(context);
                    Ring5Carvers.bootstrap(context);
                    NetherRing5Carvers.bootstrap(context);
                })
                .add(Registries.PLACED_FEATURE, context -> {
                    WitheredExpanseFeatures.placed(context);
                    Ring1HorrorFeatures.placed(context);
                    NetherRing1Features.placed(context);
                    NetherRing2Features.placed(context);
                    Ring2Features.placed(context);
                    Ring2FlatsFeatures.placed(context);
                    Ring2DressingFeatures.placed(context);
                    Ring2FlatsLifeFeatures.placed(context);
                    Ring2RockLandmarkFeatures.placed(context);
                    Ring2FlatLandmarkFeatures.placed(context);
                    DryRiverbedsFeatures.placed(context);
                    Ring2RockFeatures.placed(context);
                    Ring3Features.placed(context);
                    NetherRing3Features.placed(context);
                    Ring4Features.placed(context);
                    NetherRing4Features.placed(context);
                    Ring5Features.placed(context);
                    NetherRing5Features.placed(context);
                })
                .add(Registries.BIOME, context -> {
                    PlaceholderBiomes.bootstrap(context);
                    VanillaBiomeCopies.bootstrap(context);
                    TransitionBiomes.bootstrap(context);
                    Ring2Biomes.bootstrap(context);
                    NetherRing2Biomes.bootstrap(context);
                    Ring3Biomes.bootstrap(context);
                    NetherRing3Biomes.bootstrap(context);
                    Ring4Biomes.bootstrap(context);
                    NetherRing4Biomes.bootstrap(context);
                    Ring5Biomes.bootstrap(context);
                    NetherRing5Biomes.bootstrap(context);
                })
                .add(Registries.WOLF_VARIANT, VanillaBiomeCopies::bootstrapWolfVariants)
                .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, context -> {
                    Ring1BiomeModifiers.bootstrap(context);
                    Ring2BiomeModifiers.bootstrap(context);
                    Ring3BiomeModifiers.bootstrap(context);
                    NetherRing3BiomeModifiers.bootstrap(context);
                    Ring4BiomeModifiers.bootstrap(context);
                    NetherRing4BiomeModifiers.bootstrap(context);
                    Ring5BiomeModifiers.bootstrap(context);
                    NetherRing5BiomeModifiers.bootstrap(context);
                })
                .add(Registries.DAMAGE_TYPE, context -> {
                    ConditionDamageTypes.bootstrap(context);
                    Ring3DamageTypes.bootstrap(context);
                    Ring4DamageTypes.bootstrap(context);
                    Ring5DamageTypes.bootstrap(context);
                })
                .add(Registries.TRIM_PATTERN, ModTrimPatterns::bootstrap)
                .add(Registries.TRIM_MATERIAL, ModTrimMaterials::bootstrap),
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
        event.createProvider(ModRecipeProvider::new);
        event.createProvider((output, lookup) -> new CoatingAtlasProvider(output, lookup, files));

        event.createProvider(EnglishLanguageProvider::new);
        event.createProvider(DutchLanguageProvider::new);
    }
}
