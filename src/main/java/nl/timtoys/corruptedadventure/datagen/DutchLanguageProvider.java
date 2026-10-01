package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.common.data.LanguageProvider;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.worldgen.biome.VanillaBiomeCopies;

/** {@code nl_nl}: optional; missing keys fall back to {@code en_us}. */
final class DutchLanguageProvider extends LanguageProvider {
    DutchLanguageProvider(PackOutput output) {
        super(output, CorruptedAdventure.MOD_ID, "nl_nl");
    }

    @Override
    protected void addTranslations() {
        add("generator.corruptedadventure.corrupted_world", "Corrupte wereld");

        // Ring 1: copies of the vanilla overworld biomes, with vanilla's names.
        copy(Biomes.PLAINS, "Velden");
        copy(Biomes.SUNFLOWER_PLAINS, "Zonnebloemvelden");
        copy(Biomes.SNOWY_PLAINS, "Besneeuwde velden");
        copy(Biomes.ICE_SPIKES, "IJspijlers");
        copy(Biomes.DESERT, "Woestijn");
        copy(Biomes.SWAMP, "Moeras");
        copy(Biomes.MANGROVE_SWAMP, "Mangrovemoeras");
        copy(Biomes.FOREST, "Bos");
        copy(Biomes.FLOWER_FOREST, "Bloemenbos");
        copy(Biomes.BIRCH_FOREST, "Berkenbos");
        copy(Biomes.DARK_FOREST, "Dicht bos");
        copy(Biomes.OLD_GROWTH_BIRCH_FOREST, "Oerberkenbos");
        copy(Biomes.OLD_GROWTH_PINE_TAIGA, "Oernaaldbomentaiga");
        copy(Biomes.OLD_GROWTH_SPRUCE_TAIGA, "Oersparrentaiga");
        copy(Biomes.TAIGA, "Taiga");
        copy(Biomes.SNOWY_TAIGA, "Besneeuwde taiga");
        copy(Biomes.SAVANNA, "Savanne");
        copy(Biomes.SAVANNA_PLATEAU, "Savanneplateau");
        copy(Biomes.WINDSWEPT_HILLS, "Onstuimige heuvels");
        copy(Biomes.WINDSWEPT_GRAVELLY_HILLS, "Onstuimige grindheuvels");
        copy(Biomes.WINDSWEPT_FOREST, "Onstuimig bos");
        copy(Biomes.WINDSWEPT_SAVANNA, "Onstuimige savanne");
        copy(Biomes.JUNGLE, "Oerwoud");
        copy(Biomes.SPARSE_JUNGLE, "Dunbegroeid oerwoud");
        copy(Biomes.BAMBOO_JUNGLE, "Bamboe-oerwoud");
        copy(Biomes.BADLANDS, "Badlands");
        copy(Biomes.ERODED_BADLANDS, "Geërodeerde badlands");
        copy(Biomes.WOODED_BADLANDS, "Beboste badlands");
        copy(Biomes.MEADOW, "Weide");
        copy(Biomes.CHERRY_GROVE, "Kersenboomgaard");
        copy(Biomes.GROVE, "Bosje");
        copy(Biomes.SNOWY_SLOPES, "Besneeuwde pistes");
        copy(Biomes.FROZEN_PEAKS, "Bevroren pieken");
        copy(Biomes.JAGGED_PEAKS, "Scherpe pieken");
        copy(Biomes.STONY_PEAKS, "Steenpieken");
        copy(Biomes.RIVER, "Rivier");
        copy(Biomes.FROZEN_RIVER, "Bevroren rivier");
        copy(Biomes.BEACH, "Strand");
        copy(Biomes.SNOWY_BEACH, "Besneeuwd strand");
        copy(Biomes.STONY_SHORE, "Steenkust");
        copy(Biomes.WARM_OCEAN, "Warme oceaan");
        copy(Biomes.LUKEWARM_OCEAN, "Lauwe oceaan");
        copy(Biomes.DEEP_LUKEWARM_OCEAN, "Lauwe diepe oceaan");
        copy(Biomes.OCEAN, "Oceaan");
        copy(Biomes.DEEP_OCEAN, "Diepe oceaan");
        copy(Biomes.COLD_OCEAN, "Koude oceaan");
        copy(Biomes.DEEP_COLD_OCEAN, "Koude diepe oceaan");
        copy(Biomes.FROZEN_OCEAN, "Bevroren oceaan");
        copy(Biomes.DEEP_FROZEN_OCEAN, "Bevroren diepe oceaan");
        copy(Biomes.DRIPSTONE_CAVES, "Druipsteengrotten");
        copy(Biomes.LUSH_CAVES, "Weelderige grotten");
        copy(Biomes.DEEP_DARK, "Diep duister");

        biome(ModBiomes.PLACEHOLDER_RING_2, "Placeholder ring 2");
        biome(ModBiomes.PLACEHOLDER_RING_3, "Placeholder ring 3");
        biome(ModBiomes.PLACEHOLDER_RING_4, "Placeholder ring 4");
        biome(ModBiomes.PLACEHOLDER_RING_5, "Placeholder ring 5");
        biome(ModBiomes.VOID_EDGE, "Voidrand");
        biome(ModBiomes.PLACEHOLDER_NETHER_RING_1, "Placeholder Nether-ring 1");
        biome(ModBiomes.PLACEHOLDER_NETHER_RING_2, "Placeholder Nether-ring 2");
        biome(ModBiomes.PLACEHOLDER_NETHER_RING_3, "Placeholder Nether-ring 3");
        biome(ModBiomes.PLACEHOLDER_NETHER_RING_4, "Placeholder Nether-ring 4");
        biome(ModBiomes.PLACEHOLDER_NETHER_RING_5, "Placeholder Nether-ring 5");
        biome(ModBiomes.NETHER_VOID_EDGE, "Nether-voidrand");
    }

    private void biome(ResourceKey<Biome> biome, String name) {
        add(biome.location().toLanguageKey("biome"), name);
    }

    private void copy(ResourceKey<Biome> vanilla, String name) {
        biome(VanillaBiomeCopies.copyOf(vanilla), name);
    }
}
