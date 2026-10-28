package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.common.data.LanguageProvider;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.datagen.lang.Golf1MaterialNames;
import nl.timtoys.corruptedadventure.datagen.lang.Golf1TierNames;
import nl.timtoys.corruptedadventure.datagen.lang.Golf2MaterialNames;
import nl.timtoys.corruptedadventure.datagen.lang.Golf3Ring3Names;
import nl.timtoys.corruptedadventure.datagen.lang.Golf4MaterialNames;
import nl.timtoys.corruptedadventure.datagen.lang.Golf5SkyGlassNames;
import nl.timtoys.corruptedadventure.datagen.lang.CoatingText;
import nl.timtoys.corruptedadventure.datagen.lang.ArrowText;
import nl.timtoys.corruptedadventure.datagen.lang.ConsumableText;
import nl.timtoys.corruptedadventure.datagen.lang.GearNames;
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

/** {@code nl_nl}: optional; missing keys fall back to {@code en_us}. */
final class DutchLanguageProvider extends LanguageProvider implements LangOutput {
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

        add("subtitles.corruptedadventure.ambient.ring_1.strange_cave", "Er beweegt iets in het donker");
        add("subtitles.corruptedadventure.weather.ring_2.sandstorm", "Zandstorm raast");
        add("subtitles.corruptedadventure.ambient.ring_2.singing_sand", "Het zand zingt");
        add("subtitles.corruptedadventure.ambient.ring_2.oasis", "Vogels en insecten tjilpen");
        add("subtitles.corruptedadventure.ambient.nether_ring_2.drips", "Water druppelt");

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
        biome(ModBiomes.PLACEHOLDER_RING_5, "Placeholder ring 5");
        biome(ModBiomes.VOID_EDGE, "Voidrand");
        biome(ModBiomes.DRIPSHROOM_FOREST, "Druipzwamwoud");
        biome(ModBiomes.SPORE_CAVES, "Sporengrotten");
        biome(ModBiomes.PLACEHOLDER_NETHER_RING_5, "Placeholder Nether-ring 5");
        biome(ModBiomes.NETHER_VOID_EDGE, "Nether-voidrand");

        // Ring 3 and Nether ring 3: one helper per stream, so the streams never edit this file.
        Ring3BlockNames.addDutch(this);
        Ring3BiomeNames.addDutch(this);
        NetherRing3BiomeNames.addDutch(this);
        Ring3ConditionText.addDutch(this);
        Ring3WeatherText.addDutch(this);
        Ring3AmbienceText.addDutch(this);

        // Ring 4 and Nether ring 4: likewise, one helper per stream.
        Ring4BlockNames.addDutch(this);
        Ring4BiomeNames.addDutch(this);
        NetherRing4BiomeNames.addDutch(this);
        Ring4ConditionText.addDutch(this);
        Ring4WeatherText.addDutch(this);
        Ring4AmbienceText.addDutch(this);

        // Ring 5 and Nether ring 5: likewise, one helper per stream.
        Ring5BlockNames.addDutch(this);
        Ring5BiomeNames.addDutch(this);
        NetherRing5BiomeNames.addDutch(this);
        Ring5ConditionText.addDutch(this);
        Ring5WeatherText.addDutch(this);
        Ring5SignText.addDutch(this);

        // Phase 2 golf 1: one helper per stream.
        Golf1MaterialNames.addDutch(this);
        Golf1TierNames.addDutch(this);
        CoatingText.addDutch(this);
        // Phase 2 golf 2: one helper per stream.
        Golf2MaterialNames.addDutch(this);
        Golf3Ring3Names.addDutch(this);
        ConsumableText.addDutch(this);
        GearNames.addDutch(this);
        // Phase 2 golf 4 (stream R4).
        Golf4MaterialNames.addDutch(this);
        // Phase 2 golf 3 + 4: one helper per stream.
        ArrowText.addDutch(this);
        // Phase 2 golf 5: one helper per stream.
        Golf5SkyGlassNames.addDutch(this);

        // Death messages of the conditions (damage types: ConditionDamageTypes).
        add("death.attack.corruptedadventure.dehydration", "%1$s is uitgedroogd");
        add("death.attack.corruptedadventure.dehydration.player", "%1$s droogde uit tijdens het gevecht met %2$s");
    }

    private void copy(ResourceKey<Biome> vanilla, String name) {
        biome(VanillaBiomeCopies.copyOf(vanilla), name);
    }
}
