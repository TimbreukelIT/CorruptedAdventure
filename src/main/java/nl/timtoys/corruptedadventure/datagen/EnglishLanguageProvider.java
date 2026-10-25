package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.common.data.LanguageProvider;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.datagen.lang.Golf1MaterialNames;
import nl.timtoys.corruptedadventure.datagen.lang.Golf1TierNames;
import nl.timtoys.corruptedadventure.datagen.lang.CoatingText;
import nl.timtoys.corruptedadventure.datagen.lang.LangOutput;
import nl.timtoys.corruptedadventure.datagen.lang.NetherRing3BiomeNames;
import nl.timtoys.corruptedadventure.datagen.lang.NetherRing4BiomeNames;
import nl.timtoys.corruptedadventure.datagen.lang.NetherRing5BiomeNames;
import nl.timtoys.corruptedadventure.datagen.lang.Ring3AmbienceText;
import nl.timtoys.corruptedadventure.datagen.lang.Ring3BiomeNames;
import nl.timtoys.corruptedadventure.datagen.lang.Ring3BlockNames;
import nl.timtoys.corruptedadventure.datagen.lang.Ring3ConditionText;
import nl.timtoys.corruptedadventure.datagen.lang.Ring3WeatherText;
import nl.timtoys.corruptedadventure.datagen.lang.Ring4AmbienceText;
import nl.timtoys.corruptedadventure.datagen.lang.Ring4BiomeNames;
import nl.timtoys.corruptedadventure.datagen.lang.Ring4BlockNames;
import nl.timtoys.corruptedadventure.datagen.lang.Ring4ConditionText;
import nl.timtoys.corruptedadventure.datagen.lang.Ring4WeatherText;
import nl.timtoys.corruptedadventure.datagen.lang.Ring5BiomeNames;
import nl.timtoys.corruptedadventure.datagen.lang.Ring5BlockNames;
import nl.timtoys.corruptedadventure.datagen.lang.Ring5ConditionText;
import nl.timtoys.corruptedadventure.datagen.lang.Ring5SignText;
import nl.timtoys.corruptedadventure.datagen.lang.Ring5WeatherText;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModItems;
import nl.timtoys.corruptedadventure.worldgen.biome.VanillaBiomeCopies;

/** {@code en_us}: mandatory for every key. */
final class EnglishLanguageProvider extends LanguageProvider implements LangOutput {
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

        add("subtitles.corruptedadventure.ambient.ring_1.strange_cave", "Something stirs in the dark");
        add("subtitles.corruptedadventure.weather.ring_2.sandstorm", "Sandstorm howls");
        add("subtitles.corruptedadventure.ambient.ring_2.singing_sand", "Sand hums");
        add("subtitles.corruptedadventure.ambient.ring_2.oasis", "Birds and insects chirp");
        add("subtitles.corruptedadventure.ambient.nether_ring_2.drips", "Water drips");

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

        // Ring 2 blocks.
        addBlock(ModBlocks.RED_CANYON_STONE, "Red Canyon Stone");
        addBlock(ModBlocks.ORANGE_CANYON_STONE, "Orange Canyon Stone");
        addBlock(ModBlocks.CREAM_CANYON_STONE, "Cream Canyon Stone");
        addBlock(ModBlocks.CANYON_COBBLESTONE, "Canyon Cobblestone");
        addBlock(ModBlocks.CANYON_COBBLESTONE_STAIRS, "Canyon Cobblestone Stairs");
        addBlock(ModBlocks.CANYON_COBBLESTONE_SLAB, "Canyon Cobblestone Slab");
        addBlock(ModBlocks.CANYON_COBBLESTONE_WALL, "Canyon Cobblestone Wall");
        addBlock(ModBlocks.CANYON_STONE_BRICKS, "Canyon Stone Bricks");
        addBlock(ModBlocks.CANYON_STONE_BRICK_STAIRS, "Canyon Stone Brick Stairs");
        addBlock(ModBlocks.CANYON_STONE_BRICK_SLAB, "Canyon Stone Brick Slab");
        addBlock(ModBlocks.CANYON_STONE_BRICK_WALL, "Canyon Stone Brick Wall");
        addBlock(ModBlocks.SCORCHED_SANDSTONE, "Scorched Sandstone");
        addBlock(ModBlocks.SCORCHED_SANDSTONE_STAIRS, "Scorched Sandstone Stairs");
        addBlock(ModBlocks.SCORCHED_SANDSTONE_SLAB, "Scorched Sandstone Slab");
        addBlock(ModBlocks.SCORCHED_SANDSTONE_WALL, "Scorched Sandstone Wall");
        addBlock(ModBlocks.CUT_SCORCHED_SANDSTONE, "Cut Scorched Sandstone");
        addBlock(ModBlocks.CUT_SCORCHED_SANDSTONE_SLAB, "Cut Scorched Sandstone Slab");
        addBlock(ModBlocks.CHISELED_SCORCHED_SANDSTONE, "Chiseled Scorched Sandstone");
        addBlock(ModBlocks.SMOOTH_SCORCHED_SANDSTONE, "Smooth Scorched Sandstone");
        addBlock(ModBlocks.SMOOTH_SCORCHED_SANDSTONE_STAIRS, "Smooth Scorched Sandstone Stairs");
        addBlock(ModBlocks.SMOOTH_SCORCHED_SANDSTONE_SLAB, "Smooth Scorched Sandstone Slab");
        addBlock(ModBlocks.LOOSE_SAND, "Loose Sand");
        addBlock(ModBlocks.CRACKED_CLAY, "Cracked Clay");
        addBlock(ModBlocks.DRY_SHRUB, "Dry Shrub");
        addBlock(ModBlocks.BARREL_CACTUS, "Barrel Cactus");
        addBlock(ModBlocks.DESERT_FLOWER, "Desert Flower");
        addBlock(ModBlocks.POTTED_DRY_SHRUB, "Potted Dry Shrub");
        addBlock(ModBlocks.POTTED_BARREL_CACTUS, "Potted Barrel Cactus");
        addBlock(ModBlocks.POTTED_DESERT_FLOWER, "Potted Desert Flower");
        addBlock(ModBlocks.PALM_LOG, "Palm Log");
        addBlock(ModBlocks.PALM_WOOD, "Palm Wood");
        addBlock(ModBlocks.STRIPPED_PALM_LOG, "Stripped Palm Log");
        addBlock(ModBlocks.STRIPPED_PALM_WOOD, "Stripped Palm Wood");
        addBlock(ModBlocks.PALM_PLANKS, "Palm Planks");
        addBlock(ModBlocks.PALM_STAIRS, "Palm Stairs");
        addBlock(ModBlocks.PALM_SLAB, "Palm Slab");
        addBlock(ModBlocks.PALM_FENCE, "Palm Fence");
        addBlock(ModBlocks.PALM_FENCE_GATE, "Palm Fence Gate");
        addBlock(ModBlocks.PALM_DOOR, "Palm Door");
        addBlock(ModBlocks.PALM_TRAPDOOR, "Palm Trapdoor");
        addBlock(ModBlocks.PALM_PRESSURE_PLATE, "Palm Pressure Plate");
        addBlock(ModBlocks.PALM_BUTTON, "Palm Button");
        addBlock(ModBlocks.PALM_LEAVES, "Palm Leaves");
        addBlock(ModBlocks.PALM_SAPLING, "Palm Sapling");
        addBlock(ModBlocks.POTTED_PALM_SAPLING, "Potted Palm Sapling");

        // Ring 2 ores.
        addBlock(ModBlocks.RED_CANYON_SUNSTONE_ORE, "Red Canyon Sunstone Ore");
        addBlock(ModBlocks.ORANGE_CANYON_SUNSTONE_ORE, "Orange Canyon Sunstone Ore");
        addBlock(ModBlocks.CREAM_CANYON_SUNSTONE_ORE, "Cream Canyon Sunstone Ore");
        addBlock(ModBlocks.SCORCHED_SANDSTONE_SUNSTONE_ORE, "Scorched Sandstone Sunstone Ore");
        addBlock(ModBlocks.RED_CANYON_TURQUOISE_ORE, "Red Canyon Turquoise Ore");
        addBlock(ModBlocks.ORANGE_CANYON_TURQUOISE_ORE, "Orange Canyon Turquoise Ore");
        addBlock(ModBlocks.CREAM_CANYON_TURQUOISE_ORE, "Cream Canyon Turquoise Ore");
        addItem(ModItems.RAW_SUNSTONE, "Raw Sunstone");
        addItem(ModItems.RAW_TURQUOISE, "Raw Turquoise");

        // Ring 2 salt.
        addBlock(ModBlocks.SALT_CRUST, "Salt Crust");
        addBlock(ModBlocks.SALT_BLOCK, "Salt Block");
        addBlock(ModBlocks.POLISHED_SALT, "Polished Salt");
        addBlock(ModBlocks.POLISHED_SALT_STAIRS, "Polished Salt Stairs");
        addBlock(ModBlocks.POLISHED_SALT_SLAB, "Polished Salt Slab");
        addBlock(ModBlocks.POLISHED_SALT_WALL, "Polished Salt Wall");
        addBlock(ModBlocks.SALT_CRYSTAL, "Salt Crystal");
        addItem(ModItems.SALT, "Salt");

        // Nether ring 2 blocks.
        addBlock(ModBlocks.SOAKED_NETHERRACK, "Soaked Netherrack");
        addBlock(ModBlocks.SPORE_MOSS, "Spore Moss");
        addBlock(ModBlocks.DRIPSHROOM_STEM, "Dripshroom Stem");
        addBlock(ModBlocks.DRIPSHROOM_HYPHAE, "Dripshroom Hyphae");
        addBlock(ModBlocks.STRIPPED_DRIPSHROOM_STEM, "Stripped Dripshroom Stem");
        addBlock(ModBlocks.STRIPPED_DRIPSHROOM_HYPHAE, "Stripped Dripshroom Hyphae");
        addBlock(ModBlocks.DRIPSHROOM_PLANKS, "Dripshroom Planks");
        addBlock(ModBlocks.DRIPSHROOM_STAIRS, "Dripshroom Stairs");
        addBlock(ModBlocks.DRIPSHROOM_SLAB, "Dripshroom Slab");
        addBlock(ModBlocks.DRIPSHROOM_FENCE, "Dripshroom Fence");
        addBlock(ModBlocks.DRIPSHROOM_FENCE_GATE, "Dripshroom Fence Gate");
        addBlock(ModBlocks.DRIPSHROOM_DOOR, "Dripshroom Door");
        addBlock(ModBlocks.DRIPSHROOM_TRAPDOOR, "Dripshroom Trapdoor");
        addBlock(ModBlocks.DRIPSHROOM_PRESSURE_PLATE, "Dripshroom Pressure Plate");
        addBlock(ModBlocks.DRIPSHROOM_BUTTON, "Dripshroom Button");
        addBlock(ModBlocks.DRIPSHROOM_CAP, "Dripshroom Cap");
        addBlock(ModBlocks.DRIPSHROOM_LAMP, "Dripshroom Lamp");
        addBlock(ModBlocks.GLOW_MOLD, "Glow Mold");
        addBlock(ModBlocks.SLIME_STALACTITE, "Slime Stalactite");
        addBlock(ModBlocks.SPORE_SPROUTS, "Spore Sprouts");
        addBlock(ModBlocks.DEW_SOURCE, "Dew Source");
        addBlock(ModBlocks.SMALL_DEW_CRYSTAL_BUD, "Small Dew Crystal Bud");
        addBlock(ModBlocks.MEDIUM_DEW_CRYSTAL_BUD, "Medium Dew Crystal Bud");
        addBlock(ModBlocks.LARGE_DEW_CRYSTAL_BUD, "Large Dew Crystal Bud");
        addBlock(ModBlocks.DEW_CRYSTAL_CLUSTER, "Dew Crystal Cluster");
        addItem(ModItems.DEW_CRYSTAL, "Dew Crystal");

        biome(ModBiomes.WITHERED_EXPANSE, "Withered Expanse");
        biome(ModBiomes.DRY_RIVERBEDS, "Dry Riverbeds");
        biome(ModBiomes.CANYONLANDS, "Canyonlands");
        biome(ModBiomes.OASIS_FLATS, "Oasis Flats");
        biome(ModBiomes.HOODOO_FIELDS, "Hoodoo Fields");
        biome(ModBiomes.SALT_FLATS, "Salt Flats");
        biome(ModBiomes.DUNE_SEA, "Dune Sea");
        biome(ModBiomes.PLACEHOLDER_RING_5, "Placeholder Ring 5");
        biome(ModBiomes.VOID_EDGE, "Void Edge");
        biome(ModBiomes.DRIPSHROOM_FOREST, "Dripshroom Forest");
        biome(ModBiomes.SPORE_CAVES, "Spore Caves");
        biome(ModBiomes.PLACEHOLDER_NETHER_RING_5, "Placeholder Nether Ring 5");
        biome(ModBiomes.NETHER_VOID_EDGE, "Nether Void Edge");

        // Ring 3 and Nether ring 3: one helper per stream, so the streams never edit this file.
        Ring3BlockNames.addEnglish(this);
        Ring3BiomeNames.addEnglish(this);
        NetherRing3BiomeNames.addEnglish(this);
        Ring3ConditionText.addEnglish(this);
        Ring3WeatherText.addEnglish(this);
        Ring3AmbienceText.addEnglish(this);

        // Ring 4 and Nether ring 4: likewise, one helper per stream.
        Ring4BlockNames.addEnglish(this);
        Ring4BiomeNames.addEnglish(this);
        NetherRing4BiomeNames.addEnglish(this);
        Ring4ConditionText.addEnglish(this);
        Ring4WeatherText.addEnglish(this);
        Ring4AmbienceText.addEnglish(this);

        // Ring 5 and Nether ring 5: likewise, one helper per stream.
        Ring5BlockNames.addEnglish(this);
        Ring5BiomeNames.addEnglish(this);
        NetherRing5BiomeNames.addEnglish(this);
        Ring5ConditionText.addEnglish(this);
        Ring5WeatherText.addEnglish(this);
        Ring5SignText.addEnglish(this);

        // Phase 2 golf 1: one helper per stream.
        Golf1MaterialNames.addEnglish(this);
        Golf1TierNames.addEnglish(this);
        CoatingText.addEnglish(this);

        // Death messages of the conditions (damage types: ConditionDamageTypes).
        add("death.attack.corruptedadventure.dehydration", "%1$s dried out");
        add("death.attack.corruptedadventure.dehydration.player", "%1$s dried out whilst fighting %2$s");
    }

    private void copy(ResourceKey<Biome> vanilla, String name) {
        biome(VanillaBiomeCopies.copyOf(vanilla), name);
    }
}
