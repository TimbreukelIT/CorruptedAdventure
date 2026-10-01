package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.common.data.LanguageProvider;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModItems;
import nl.timtoys.corruptedadventure.worldgen.biome.VanillaBiomeCopies;

/** {@code en_us}: mandatory for every key. */
final class EnglishLanguageProvider extends LanguageProvider {
    EnglishLanguageProvider(PackOutput output) {
        super(output, CorruptedAdventure.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("generator.corruptedadventure.corrupted_world", "Corrupted World");

        // Ring 1: copies of the vanilla overworld biomes, with vanilla's names.
        copy(Biomes.PLAINS, "Plains");
        copy(Biomes.SUNFLOWER_PLAINS, "Sunflower Plains");
        copy(Biomes.SNOWY_PLAINS, "Snowy Plains");
        copy(Biomes.ICE_SPIKES, "Ice Spikes");
        copy(Biomes.DESERT, "Desert");
        copy(Biomes.SWAMP, "Swamp");
        copy(Biomes.MANGROVE_SWAMP, "Mangrove Swamp");
        copy(Biomes.FOREST, "Forest");
        copy(Biomes.FLOWER_FOREST, "Flower Forest");
        copy(Biomes.BIRCH_FOREST, "Birch Forest");
        copy(Biomes.DARK_FOREST, "Dark Forest");
        copy(Biomes.OLD_GROWTH_BIRCH_FOREST, "Old Growth Birch Forest");
        copy(Biomes.OLD_GROWTH_PINE_TAIGA, "Old Growth Pine Taiga");
        copy(Biomes.OLD_GROWTH_SPRUCE_TAIGA, "Old Growth Spruce Taiga");
        copy(Biomes.TAIGA, "Taiga");
        copy(Biomes.SNOWY_TAIGA, "Snowy Taiga");
        copy(Biomes.SAVANNA, "Savanna");
        copy(Biomes.SAVANNA_PLATEAU, "Savanna Plateau");
        copy(Biomes.WINDSWEPT_HILLS, "Windswept Hills");
        copy(Biomes.WINDSWEPT_GRAVELLY_HILLS, "Windswept Gravelly Hills");
        copy(Biomes.WINDSWEPT_FOREST, "Windswept Forest");
        copy(Biomes.WINDSWEPT_SAVANNA, "Windswept Savanna");
        copy(Biomes.JUNGLE, "Jungle");
        copy(Biomes.SPARSE_JUNGLE, "Sparse Jungle");
        copy(Biomes.BAMBOO_JUNGLE, "Bamboo Jungle");
        copy(Biomes.BADLANDS, "Badlands");
        copy(Biomes.ERODED_BADLANDS, "Eroded Badlands");
        copy(Biomes.WOODED_BADLANDS, "Wooded Badlands");
        copy(Biomes.MEADOW, "Meadow");
        copy(Biomes.CHERRY_GROVE, "Cherry Grove");
        copy(Biomes.GROVE, "Grove");
        copy(Biomes.SNOWY_SLOPES, "Snowy Slopes");
        copy(Biomes.FROZEN_PEAKS, "Frozen Peaks");
        copy(Biomes.JAGGED_PEAKS, "Jagged Peaks");
        copy(Biomes.STONY_PEAKS, "Stony Peaks");
        copy(Biomes.RIVER, "River");
        copy(Biomes.FROZEN_RIVER, "Frozen River");
        copy(Biomes.BEACH, "Beach");
        copy(Biomes.SNOWY_BEACH, "Snowy Beach");
        copy(Biomes.STONY_SHORE, "Stony Shore");
        copy(Biomes.WARM_OCEAN, "Warm Ocean");
        copy(Biomes.LUKEWARM_OCEAN, "Lukewarm Ocean");
        copy(Biomes.DEEP_LUKEWARM_OCEAN, "Deep Lukewarm Ocean");
        copy(Biomes.OCEAN, "Ocean");
        copy(Biomes.DEEP_OCEAN, "Deep Ocean");
        copy(Biomes.COLD_OCEAN, "Cold Ocean");
        copy(Biomes.DEEP_COLD_OCEAN, "Deep Cold Ocean");
        copy(Biomes.FROZEN_OCEAN, "Frozen Ocean");
        copy(Biomes.DEEP_FROZEN_OCEAN, "Deep Frozen Ocean");
        copy(Biomes.DRIPSTONE_CAVES, "Dripstone Caves");
        copy(Biomes.LUSH_CAVES, "Lush Caves");
        copy(Biomes.DEEP_DARK, "Deep Dark");

        // Nether ring 1: copies of three vanilla Nether biomes, with vanilla's names.
        copy(Biomes.NETHER_WASTES, "Nether Wastes");
        copy(Biomes.CRIMSON_FOREST, "Crimson Forest");
        copy(Biomes.WARPED_FOREST, "Warped Forest");

        add("itemGroup.corruptedadventure.main", "The Corrupted Adventure");
        add("subtitles.corruptedadventure.ambient.ring_1.strange_cave", "Something stirs in the dark");

        // Ring 1 blocks.
        addBlock(ModBlocks.DEAD_GRASS_BLOCK, "Dead Grass Block");
        addBlock(ModBlocks.WITHERED_DIRT, "Withered Dirt");
        addBlock(ModBlocks.DEAD_LOG, "Dead Log");
        addBlock(ModBlocks.DEAD_WOOD, "Dead Wood");
        addBlock(ModBlocks.STRIPPED_DEAD_LOG, "Stripped Dead Log");
        addBlock(ModBlocks.STRIPPED_DEAD_WOOD, "Stripped Dead Wood");
        addBlock(ModBlocks.DEAD_PLANKS, "Dead Planks");
        addBlock(ModBlocks.DEAD_STAIRS, "Dead Stairs");
        addBlock(ModBlocks.DEAD_SLAB, "Dead Slab");
        addBlock(ModBlocks.DEAD_FENCE, "Dead Fence");
        addBlock(ModBlocks.DEAD_FENCE_GATE, "Dead Fence Gate");
        addBlock(ModBlocks.DEAD_DOOR, "Dead Door");
        addBlock(ModBlocks.DEAD_TRAPDOOR, "Dead Trapdoor");
        addBlock(ModBlocks.DEAD_PRESSURE_PLATE, "Dead Pressure Plate");
        addBlock(ModBlocks.DEAD_BUTTON, "Dead Button");

        // Nether ring 1.
        addBlock(ModBlocks.SOUL_CRYSTAL_ORE, "Soul Crystal Ore");
        addBlock(ModBlocks.CRIMSON_SOUL_CRYSTAL_ORE, "Crimson Soul Crystal Ore");
        addItem(ModItems.SOUL_CRYSTAL, "Soul Crystal");

        biome(ModBiomes.WITHERED_EXPANSE, "Withered Expanse");
        biome(ModBiomes.PLACEHOLDER_RING_2, "Placeholder Ring 2");
        biome(ModBiomes.PLACEHOLDER_RING_3, "Placeholder Ring 3");
        biome(ModBiomes.PLACEHOLDER_RING_4, "Placeholder Ring 4");
        biome(ModBiomes.PLACEHOLDER_RING_5, "Placeholder Ring 5");
        biome(ModBiomes.VOID_EDGE, "Void Edge");
        biome(ModBiomes.PLACEHOLDER_NETHER_RING_2, "Placeholder Nether Ring 2");
        biome(ModBiomes.PLACEHOLDER_NETHER_RING_3, "Placeholder Nether Ring 3");
        biome(ModBiomes.PLACEHOLDER_NETHER_RING_4, "Placeholder Nether Ring 4");
        biome(ModBiomes.PLACEHOLDER_NETHER_RING_5, "Placeholder Nether Ring 5");
        biome(ModBiomes.NETHER_VOID_EDGE, "Nether Void Edge");
    }

    private void biome(ResourceKey<Biome> biome, String name) {
        add(biome.location().toLanguageKey("biome"), name);
    }

    private void copy(ResourceKey<Biome> vanilla, String name) {
        biome(VanillaBiomeCopies.copyOf(vanilla), name);
    }
}
