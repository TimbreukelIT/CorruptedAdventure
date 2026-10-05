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

        // Nether ring 1: copies of three vanilla Nether biomes, with vanilla's names.
        copy(Biomes.NETHER_WASTES, "Netherwoestenij");
        copy(Biomes.CRIMSON_FOREST, "Helrood bos");
        copy(Biomes.WARPED_FOREST, "Spookachtig bos");

        add("itemGroup.corruptedadventure.main", "The Corrupted Adventure");
        add("subtitles.corruptedadventure.ambient.ring_1.strange_cave", "Er beweegt iets in het donker");
        add("subtitles.corruptedadventure.weather.ring_2.sandstorm", "Zandstorm raast");
        add("subtitles.corruptedadventure.ambient.ring_2.singing_sand", "Het zand zingt");
        add("subtitles.corruptedadventure.ambient.ring_2.oasis", "Vogels en insecten tjilpen");

        // Ring 1 blocks.
        addBlock(ModBlocks.DEAD_GRASS_BLOCK, "Dood grasblok");
        addBlock(ModBlocks.WITHERED_DIRT, "Verdorde aarde");
        addBlock(ModBlocks.DEAD_LOG, "Doodhouten stam");
        addBlock(ModBlocks.DEAD_WOOD, "Doodhout");
        addBlock(ModBlocks.STRIPPED_DEAD_LOG, "Gestripte doodhouten stam");
        addBlock(ModBlocks.STRIPPED_DEAD_WOOD, "Gestript doodhout");
        addBlock(ModBlocks.DEAD_PLANKS, "Doodhouten planken");
        addBlock(ModBlocks.DEAD_STAIRS, "Doodhouten trap");
        addBlock(ModBlocks.DEAD_SLAB, "Doodhouten plaat");
        addBlock(ModBlocks.DEAD_FENCE, "Doodhouten hek");
        addBlock(ModBlocks.DEAD_FENCE_GATE, "Doodhouten poort");
        addBlock(ModBlocks.DEAD_DOOR, "Doodhouten deur");
        addBlock(ModBlocks.DEAD_TRAPDOOR, "Doodhouten valluik");
        addBlock(ModBlocks.DEAD_PRESSURE_PLATE, "Doodhouten drukplaat");
        addBlock(ModBlocks.DEAD_BUTTON, "Doodhouten knop");

        // Nether ring 1.
        addBlock(ModBlocks.SOUL_CRYSTAL_ORE, "Zielkristalerts");
        addBlock(ModBlocks.CRIMSON_SOUL_CRYSTAL_ORE, "Karmozijnrood zielkristalerts");
        addItem(ModItems.SOUL_CRYSTAL, "Zielkristal");

        // Ring 2 blocks (vanilla nl_nl terms: keisteen, blokstenen, zandsteen, opgepot).
        addBlock(ModBlocks.RED_CANYON_STONE, "Rode kloofsteen");
        addBlock(ModBlocks.ORANGE_CANYON_STONE, "Oranje kloofsteen");
        addBlock(ModBlocks.CREAM_CANYON_STONE, "Crèmekleurige kloofsteen");
        addBlock(ModBlocks.CANYON_COBBLESTONE, "Kloofkeisteen");
        addBlock(ModBlocks.CANYON_COBBLESTONE_STAIRS, "Kloofkeistenen trap");
        addBlock(ModBlocks.CANYON_COBBLESTONE_SLAB, "Kloofkeistenen plaat");
        addBlock(ModBlocks.CANYON_COBBLESTONE_WALL, "Kloofkeistenen muur");
        addBlock(ModBlocks.CANYON_STONE_BRICKS, "Kloofblokstenen");
        addBlock(ModBlocks.CANYON_STONE_BRICK_STAIRS, "Kloofblokstenen trap");
        addBlock(ModBlocks.CANYON_STONE_BRICK_SLAB, "Kloofblokstenen plaat");
        addBlock(ModBlocks.CANYON_STONE_BRICK_WALL, "Kloofblokstenen muur");
        addBlock(ModBlocks.SCORCHED_SANDSTONE, "Geschroeid zandsteen");
        addBlock(ModBlocks.SCORCHED_SANDSTONE_STAIRS, "Geschroeide zandstenen trap");
        addBlock(ModBlocks.SCORCHED_SANDSTONE_SLAB, "Geschroeide zandstenen plaat");
        addBlock(ModBlocks.SCORCHED_SANDSTONE_WALL, "Geschroeide zandstenen muur");
        addBlock(ModBlocks.CUT_SCORCHED_SANDSTONE, "Gesneden geschroeid zandsteen");
        addBlock(ModBlocks.CUT_SCORCHED_SANDSTONE_SLAB, "Gesneden geschroeide zandstenen plaat");
        addBlock(ModBlocks.CHISELED_SCORCHED_SANDSTONE, "Gebeiteld geschroeid zandsteen");
        addBlock(ModBlocks.SMOOTH_SCORCHED_SANDSTONE, "Glad geschroeid zandsteen");
        addBlock(ModBlocks.SMOOTH_SCORCHED_SANDSTONE_STAIRS, "Gladde geschroeide zandstenen trap");
        addBlock(ModBlocks.SMOOTH_SCORCHED_SANDSTONE_SLAB, "Gladde geschroeide zandstenen plaat");
        addBlock(ModBlocks.LOOSE_SAND, "Los zand");
        addBlock(ModBlocks.CRACKED_CLAY, "Gebarsten klei");
        addBlock(ModBlocks.DRY_SHRUB, "Dorre struik");
        addBlock(ModBlocks.BARREL_CACTUS, "Tonnetjescactus");
        addBlock(ModBlocks.DESERT_FLOWER, "Woestijnbloem");
        addBlock(ModBlocks.POTTED_DRY_SHRUB, "Opgepotte dorre struik");
        addBlock(ModBlocks.POTTED_BARREL_CACTUS, "Opgepotte tonnetjescactus");
        addBlock(ModBlocks.POTTED_DESERT_FLOWER, "Opgepotte woestijnbloem");
        addBlock(ModBlocks.PALM_LOG, "Palmstam");
        addBlock(ModBlocks.PALM_WOOD, "Palmhout");
        addBlock(ModBlocks.STRIPPED_PALM_LOG, "Gestripte palmstam");
        addBlock(ModBlocks.STRIPPED_PALM_WOOD, "Gestript palmhout");
        addBlock(ModBlocks.PALM_PLANKS, "Palmhouten planken");
        addBlock(ModBlocks.PALM_STAIRS, "Palmhouten trap");
        addBlock(ModBlocks.PALM_SLAB, "Palmhouten plaat");
        addBlock(ModBlocks.PALM_FENCE, "Palmhouten hek");
        addBlock(ModBlocks.PALM_FENCE_GATE, "Palmhouten poort");
        addBlock(ModBlocks.PALM_DOOR, "Palmhouten deur");
        addBlock(ModBlocks.PALM_TRAPDOOR, "Palmhouten valluik");
        addBlock(ModBlocks.PALM_PRESSURE_PLATE, "Palmhouten drukplaat");
        addBlock(ModBlocks.PALM_BUTTON, "Palmhouten knop");
        addBlock(ModBlocks.PALM_LEAVES, "Palmbladeren");
        addBlock(ModBlocks.PALM_SAPLING, "Palmzaailing");
        addBlock(ModBlocks.POTTED_PALM_SAPLING, "Opgepotte palmzaailing");

        // Ring 2 ores (vanilla pattern: "Ruw ijzer").
        addBlock(ModBlocks.RED_CANYON_SUNSTONE_ORE, "Zonnesteenerts in rode kloofsteen");
        addBlock(ModBlocks.ORANGE_CANYON_SUNSTONE_ORE, "Zonnesteenerts in oranje kloofsteen");
        addBlock(ModBlocks.CREAM_CANYON_SUNSTONE_ORE, "Zonnesteenerts in crèmekleurige kloofsteen");
        addBlock(ModBlocks.SCORCHED_SANDSTONE_SUNSTONE_ORE, "Zonnesteenerts in geschroeid zandsteen");
        addBlock(ModBlocks.RED_CANYON_TURQUOISE_ORE, "Turkooiserts in rode kloofsteen");
        addBlock(ModBlocks.ORANGE_CANYON_TURQUOISE_ORE, "Turkooiserts in oranje kloofsteen");
        addBlock(ModBlocks.CREAM_CANYON_TURQUOISE_ORE, "Turkooiserts in crèmekleurige kloofsteen");
        addItem(ModItems.RAW_SUNSTONE, "Ruwe zonnesteen");
        addItem(ModItems.RAW_TURQUOISE, "Ruw turkoois");

        // Ring 2 salt (vanilla pattern: "Gepolijst andesiet", "Gepolijste andesiettrap", "Sneeuwblok").
        addBlock(ModBlocks.SALT_CRUST, "Zoutkorst");
        addBlock(ModBlocks.SALT_BLOCK, "Zoutblok");
        addBlock(ModBlocks.POLISHED_SALT, "Gepolijst zout");
        addBlock(ModBlocks.POLISHED_SALT_STAIRS, "Gepolijste zouttrap");
        addBlock(ModBlocks.POLISHED_SALT_SLAB, "Gepolijste zoutplaat");
        addBlock(ModBlocks.POLISHED_SALT_WALL, "Gepolijste zoutmuur");
        addBlock(ModBlocks.SALT_CRYSTAL, "Zoutkristal");
        addItem(ModItems.SALT, "Zout");

        // Nether ring 2 blocks.
        addBlock(ModBlocks.SOAKED_NETHERRACK, "Doorweekt netherrack");
        addBlock(ModBlocks.SPORE_MOSS, "Sporenmos");
        addBlock(ModBlocks.DRIPSHROOM_STEM, "Druipzwamsteel");
        addBlock(ModBlocks.DRIPSHROOM_HYPHAE, "Druipzwamhyfen");
        addBlock(ModBlocks.STRIPPED_DRIPSHROOM_STEM, "Gestripte druipzwamsteel");
        addBlock(ModBlocks.STRIPPED_DRIPSHROOM_HYPHAE, "Gestripte druipzwamhyfen");
        addBlock(ModBlocks.DRIPSHROOM_PLANKS, "Druipzwamplanken");
        addBlock(ModBlocks.DRIPSHROOM_STAIRS, "Druipzwamtrap");
        addBlock(ModBlocks.DRIPSHROOM_SLAB, "Druipzwamplaat");
        addBlock(ModBlocks.DRIPSHROOM_FENCE, "Druipzwamhek");
        addBlock(ModBlocks.DRIPSHROOM_FENCE_GATE, "Druipzwampoort");
        addBlock(ModBlocks.DRIPSHROOM_DOOR, "Druipzwamdeur");
        addBlock(ModBlocks.DRIPSHROOM_TRAPDOOR, "Druipzwamvalluik");
        addBlock(ModBlocks.DRIPSHROOM_PRESSURE_PLATE, "Druipzwamdrukplaat");
        addBlock(ModBlocks.DRIPSHROOM_BUTTON, "Druipzwamknop");
        addBlock(ModBlocks.DRIPSHROOM_CAP, "Druipzwamhoed");
        addBlock(ModBlocks.DRIPSHROOM_LAMP, "Zwamlamp");
        addBlock(ModBlocks.GLOW_MOLD, "Gloeischimmel");
        addBlock(ModBlocks.SLIME_STALACTITE, "Slijmstalactiet");
        addBlock(ModBlocks.SPORE_SPROUTS, "Sporenspruiten");
        addBlock(ModBlocks.DEW_SOURCE, "Dauwbron");
        addBlock(ModBlocks.SMALL_DEW_CRYSTAL_BUD, "Kleine dauwkristalknop");
        addBlock(ModBlocks.MEDIUM_DEW_CRYSTAL_BUD, "Middelgrote dauwkristalknop");
        addBlock(ModBlocks.LARGE_DEW_CRYSTAL_BUD, "Grote dauwkristalknop");
        addBlock(ModBlocks.DEW_CRYSTAL_CLUSTER, "Dauwkristalcluster");
        addItem(ModItems.DEW_CRYSTAL, "Dauwkristal");

        biome(ModBiomes.WITHERED_EXPANSE, "Verdorde vlakte");
        biome(ModBiomes.DRY_RIVERBEDS, "Drooggevallen rivierbeddingen");
        biome(ModBiomes.CANYONLANDS, "Kloofland");
        biome(ModBiomes.OASIS_FLATS, "Oasevlakte");
        biome(ModBiomes.HOODOO_FIELDS, "Hoodoovelden");
        biome(ModBiomes.SALT_FLATS, "Zoutvlakte");
        biome(ModBiomes.DUNE_SEA, "Duinenzee");
        biome(ModBiomes.PLACEHOLDER_RING_3, "Placeholder ring 3");
        biome(ModBiomes.PLACEHOLDER_RING_4, "Placeholder ring 4");
        biome(ModBiomes.PLACEHOLDER_RING_5, "Placeholder ring 5");
        biome(ModBiomes.VOID_EDGE, "Voidrand");
        biome(ModBiomes.PLACEHOLDER_NETHER_RING_2, "Placeholder Nether-ring 2");
        biome(ModBiomes.PLACEHOLDER_NETHER_RING_3, "Placeholder Nether-ring 3");
        biome(ModBiomes.PLACEHOLDER_NETHER_RING_4, "Placeholder Nether-ring 4");
        biome(ModBiomes.PLACEHOLDER_NETHER_RING_5, "Placeholder Nether-ring 5");
        biome(ModBiomes.NETHER_VOID_EDGE, "Nether-voidrand");

        // Death messages of the conditions (damage types: ConditionDamageTypes).
        add("death.attack.corruptedadventure.dehydration", "%1$s is uitgedroogd");
        add("death.attack.corruptedadventure.dehydration.player", "%1$s droogde uit tijdens het gevecht met %2$s");
    }

    private void biome(ResourceKey<Biome> biome, String name) {
        add(biome.location().toLanguageKey("biome"), name);
    }

    private void copy(ResourceKey<Biome> vanilla, String name) {
        biome(VanillaBiomeCopies.copyOf(vanilla), name);
    }
}
