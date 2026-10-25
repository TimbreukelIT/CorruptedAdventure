package nl.timtoys.corruptedadventure.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

/** Our own tag keys; their contents come from datagen. */
public final class ModTags {
    private ModTags() {
    }

    public static final class Blocks {
        /** Dead log, wood and their stripped variants; part of {@code #minecraft:logs_that_burn}. */
        public static final TagKey<Block> DEAD_LOGS = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("dead_logs"));
        /** Convention tag {@code c:ores/soul_crystal}; part of {@code c:ores}. */
        public static final TagKey<Block> ORES_SOUL_CRYSTAL = TagKey.create(Registries.BLOCK, common("ores/soul_crystal"));
        /** The three natural canyon stone tints (red, orange, cream): surface rules, ore targets. */
        public static final TagKey<Block> CANYON_STONES = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("canyon_stones"));
        /** Palm log, wood and their stripped variants; part of {@code #minecraft:logs_that_burn}. */
        public static final TagKey<Block> PALM_LOGS = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("palm_logs"));
        /** Convention tag {@code c:ores/sunstone} (all host rocks); part of {@code c:ores}. */
        public static final TagKey<Block> ORES_SUNSTONE = TagKey.create(Registries.BLOCK, common("ores/sunstone"));
        /** Convention tag {@code c:ores/turquoise} (all host rocks); part of {@code c:ores}. */
        public static final TagKey<Block> ORES_TURQUOISE = TagKey.create(Registries.BLOCK, common("ores/turquoise"));
        /** Ground for the desert flora (and the palm sapling): sand, terracotta, dirt and cracked clay. */
        public static final TagKey<Block> DESERT_PLANT_SOIL = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("desert_plant_soil"));
        /** Dripshroom stem, hyphae and their stripped variants; part of {@code #minecraft:logs}, deliberately not of {@code #logs_that_burn}. */
        public static final TagKey<Block> DRIPSHROOM_STEMS = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("dripshroom_stems"));
        /**
         * Everything that grows in the Nether ring 2 fungal forest and caves: stems, caps, lamp, stalactites, mould,
         * sprouts. Worldgen of that ring keeps its determinism rules on this tag; also the dew crystal stages.
         */
        public static final TagKey<Block> FUNGAL_GROWTHS = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("fungal_growths"));

        // Ring 3 and Nether ring 3 (docs/plans/ring-3/contract.md §3); contents from the blocks stream.
        /** Blocks that raise the heat meter of players within a few blocks (ring 3). */
        public static final TagKey<Block> HEAT_SOURCES = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("heat_sources"));
        /** Blocks that lower the cold meter of players within a few blocks (Nether ring 3). */
        public static final TagKey<Block> WARMTH_SOURCES = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("warmth_sources"));
        /** Host rocks of the ring 3 ores. */
        public static final TagKey<Block> RING_3_STONES = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("ring_3_stones"));
        /** Charred log, wood and their stripped variants; part of {@code #minecraft:logs}, deliberately not of {@code #logs_that_burn}. */
        public static final TagKey<Block> CHARRED_LOGS = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("charred_logs"));
        /** Rimeshroom stem, hyphae and their stripped variants; part of {@code #minecraft:logs}. */
        public static final TagKey<Block> RIMESHROOM_STEMS = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("rimeshroom_stems"));
        /** Convention tag {@code c:ores/volcanite} (all host rocks); part of {@code c:ores}. */
        public static final TagKey<Block> ORES_VOLCANITE = TagKey.create(Registries.BLOCK, common("ores/volcanite"));
        /** Convention tag {@code c:ores/sulfur} (all host rocks); part of {@code c:ores}. */
        public static final TagKey<Block> ORES_SULFUR = TagKey.create(Registries.BLOCK, common("ores/sulfur"));
        /** Convention tag {@code c:ores/fire_ruby} (all host rocks); part of {@code c:ores}. */
        public static final TagKey<Block> ORES_FIRE_RUBY = TagKey.create(Registries.BLOCK, common("ores/fire_ruby"));
        /** Ground of the ring 3 fire flora: ash, coarse dirt and a full ash layer ({@code AshPlantSoil}). Stream B. */
        public static final TagKey<Block> ASH_PLANT_SOIL = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("ash_plant_soil"));
        /** G2 ring 3: the bare rock a charred shrub stands on besides {@link #ASH_PLANT_SOIL} (pumice, basalt). */
        public static final TagKey<Block> CHARRED_SHRUB_SOIL = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("charred_shrub_soil"));
        /**
         * Falling blocks that cave in when a player walks under them ({@code LooseSandTrigger}): loose sand, ash crust.
         * Stream B. Only falling blocks belong here: the trigger relies on vanilla's falling-block tick.
         */
        public static final TagKey<Block> COLLAPSING_BLOCKS = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("collapsing_blocks"));

        // Ring 4 and Nether ring 4 (docs/plans/ring-4/contract.md §3); contents from stream B.
        /** Blocks that raise the poison meter of players nearby (the toxic pool); swamp gas counts separately. */
        public static final TagKey<Block> POISON_SOURCES = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("poison_sources"));
        /** Blocks that raise the poison meter while the head (eye height) is in them: swamp gas (package 72). */
        public static final TagKey<Block> POISON_GASES = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("poison_gases"));
        /** Host rocks of the ring 4 ores. */
        public static final TagKey<Block> RING_4_STONES = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("ring_4_stones"));
        /** Moonpetal log, wood and their stripped variants. */
        public static final TagKey<Block> MOONPETAL_LOGS = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("moonpetal_logs"));
        /** Withered moonpetal log, wood and their stripped variants. */
        public static final TagKey<Block> WITHERED_MOONPETAL_LOGS = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("withered_moonpetal_logs"));
        /** Verdant log, wood and their stripped variants (72b); the fruit pod hangs on them. */
        public static final TagKey<Block> VERDANT_LOGS = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("verdant_logs"));
        /** Ground of the mythical decor plants (fjord moss, mist fern, moonpetal shrub): {@code #dirt} and {@code #sand}. */
        public static final TagKey<Block> RING_4_PLANT_SOIL = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("ring_4_plant_soil"));
        /** Ground of the lagoon glowgrass: {@link #RING_4_PLANT_SOIL} plus the lagoon floor's gravel and clay. */
        public static final TagKey<Block> LAGOON_PLANT_SOIL = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("lagoon_plant_soil"));
        public static final TagKey<Block> ORES_TIDESILVER = TagKey.create(Registries.BLOCK, common("ores/tidesilver"));
        public static final TagKey<Block> ORES_JADE = TagKey.create(Registries.BLOCK, common("ores/jade"));
        /** Convention tag {@code c:ores_in_ground/tidestone}: the ring 4 ores in tidestone. */
        public static final TagKey<Block> ORES_IN_GROUND_TIDESTONE = TagKey.create(Registries.BLOCK, common("ores_in_ground/tidestone"));

        // Ring 5 and Nether ring 5 (docs/plans/ring-5/contract.md §3); contents from stream B.
        /** The corrupted variants of vanilla blocks (88); each has a {@code healthy_block} data map entry ({@code ModDataMaps}). */
        public static final TagKey<Block> CORRUPTED = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("corrupted"));
        /** Host rocks of the ring 5 ores. */
        public static final TagKey<Block> RING_5_STONES = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("ring_5_stones"));
        /** Blocks that raise the void meter of players nearby (glitch block, voidstone). */
        public static final TagKey<Block> VOID_SOURCES = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("void_sources"));
        /** Host rocks of the Nether ring 5 ores. */
        public static final TagKey<Block> NETHER_RING_5_STONES = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("nether_ring_5_stones"));
        /** Corrupted log, wood and their stripped variants (88): the planks recipe takes them. */
        public static final TagKey<Block> CORRUPTED_LOGS = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("corrupted_logs"));
        public static final TagKey<Block> ORES_NULLITE = TagKey.create(Registries.BLOCK, common("ores/nullite"));
        public static final TagKey<Block> ORES_IN_GROUND_CORRUPTED_STONE = TagKey.create(Registries.BLOCK, common("ores_in_ground/corrupted_stone"));
        public static final TagKey<Block> ORES_IN_GROUND_VOIDSTONE = TagKey.create(Registries.BLOCK, common("ores_in_ground/voidstone"));
        public static final TagKey<Block> ORES_WHITE_CORE = TagKey.create(Registries.BLOCK, common("ores/white_core"));
        public static final TagKey<Block> ORES_IN_GROUND_WHITE_STONE = TagKey.create(Registries.BLOCK, common("ores_in_ground/white_stone"));

        // Gear tiers (phase 2, GearTier): needs_<tier>_tool = blocks that need at least that tier; incorrect_for_<tier>_tool
        // = the needs tags of all higher tiers (vanilla's incorrect_for_* tags get all of ours).
        public static final TagKey<Block> NEEDS_SUNSTONE_TOOL = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("needs_sunstone_tool"));
        public static final TagKey<Block> NEEDS_VOLCANITE_TOOL = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("needs_volcanite_tool"));
        public static final TagKey<Block> NEEDS_TIDESILVER_TOOL = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("needs_tidesilver_tool"));
        public static final TagKey<Block> NEEDS_NULLITE_TOOL = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("needs_nullite_tool"));
        public static final TagKey<Block> INCORRECT_FOR_SUNSTONE_TOOL = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("incorrect_for_sunstone_tool"));
        public static final TagKey<Block> INCORRECT_FOR_VOLCANITE_TOOL = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("incorrect_for_volcanite_tool"));
        public static final TagKey<Block> INCORRECT_FOR_TIDESILVER_TOOL = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("incorrect_for_tidesilver_tool"));
        public static final TagKey<Block> INCORRECT_FOR_NULLITE_TOOL = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("incorrect_for_nullite_tool"));

        private Blocks() {
        }
    }

    public static final class Items {
        public static final TagKey<Item> DEAD_LOGS = TagKey.create(Registries.ITEM, CorruptedAdventure.id("dead_logs"));
        public static final TagKey<Item> CANYON_STONES = TagKey.create(Registries.ITEM, CorruptedAdventure.id("canyon_stones"));
        public static final TagKey<Item> PALM_LOGS = TagKey.create(Registries.ITEM, CorruptedAdventure.id("palm_logs"));
        public static final TagKey<Item> DRIPSHROOM_STEMS = TagKey.create(Registries.ITEM, CorruptedAdventure.id("dripshroom_stems"));
        public static final TagKey<Item> ORES_SOUL_CRYSTAL = TagKey.create(Registries.ITEM, common("ores/soul_crystal"));
        /** Convention tag {@code c:gems/soul_crystal}; part of {@code c:gems}. */
        public static final TagKey<Item> GEMS_SOUL_CRYSTAL = TagKey.create(Registries.ITEM, common("gems/soul_crystal"));
        public static final TagKey<Item> ORES_SUNSTONE = TagKey.create(Registries.ITEM, common("ores/sunstone"));
        public static final TagKey<Item> ORES_TURQUOISE = TagKey.create(Registries.ITEM, common("ores/turquoise"));
        /** Convention tag {@code c:raw_materials/sunstone}; part of {@code c:raw_materials}. */
        public static final TagKey<Item> RAW_MATERIALS_SUNSTONE = TagKey.create(Registries.ITEM, common("raw_materials/sunstone"));
        /** Convention tag {@code c:raw_materials/turquoise}; part of {@code c:raw_materials}. */
        public static final TagKey<Item> RAW_MATERIALS_TURQUOISE = TagKey.create(Registries.ITEM, common("raw_materials/turquoise"));
        /** Convention tag {@code c:dusts/salt}; part of {@code c:dusts}. */
        public static final TagKey<Item> DUSTS_SALT = TagKey.create(Registries.ITEM, common("dusts/salt"));
        /** Convention tag {@code c:gems/dew_crystal}; part of {@code c:gems}. */
        public static final TagKey<Item> GEMS_DEW_CRYSTAL = TagKey.create(Registries.ITEM, common("gems/dew_crystal"));

        // Ring 3 and Nether ring 3 (docs/plans/ring-3/contract.md §3); contents from the blocks stream.
        public static final TagKey<Item> CHARRED_LOGS = TagKey.create(Registries.ITEM, CorruptedAdventure.id("charred_logs"));
        public static final TagKey<Item> RIMESHROOM_STEMS = TagKey.create(Registries.ITEM, CorruptedAdventure.id("rimeshroom_stems"));
        public static final TagKey<Item> ORES_VOLCANITE = TagKey.create(Registries.ITEM, common("ores/volcanite"));
        public static final TagKey<Item> ORES_SULFUR = TagKey.create(Registries.ITEM, common("ores/sulfur"));
        public static final TagKey<Item> ORES_FIRE_RUBY = TagKey.create(Registries.ITEM, common("ores/fire_ruby"));
        /** Convention tag {@code c:raw_materials/volcanite}; part of {@code c:raw_materials}. */
        public static final TagKey<Item> RAW_MATERIALS_VOLCANITE = TagKey.create(Registries.ITEM, common("raw_materials/volcanite"));
        /** Convention tag {@code c:dusts/sulfur}; part of {@code c:dusts}. */
        public static final TagKey<Item> DUSTS_SULFUR = TagKey.create(Registries.ITEM, common("dusts/sulfur"));
        /** Convention tag {@code c:gems/fire_ruby}; part of {@code c:gems}. */
        public static final TagKey<Item> GEMS_FIRE_RUBY = TagKey.create(Registries.ITEM, common("gems/fire_ruby"));
        /** Convention tag {@code c:gems/fire_crystal}; part of {@code c:gems}. */
        public static final TagKey<Item> GEMS_FIRE_CRYSTAL = TagKey.create(Registries.ITEM, common("gems/fire_crystal"));

        // Ring 4 and Nether ring 4 (docs/plans/ring-4/contract.md §3); contents from stream B.
        public static final TagKey<Item> MOONPETAL_LOGS = TagKey.create(Registries.ITEM, CorruptedAdventure.id("moonpetal_logs"));
        public static final TagKey<Item> WITHERED_MOONPETAL_LOGS = TagKey.create(Registries.ITEM, CorruptedAdventure.id("withered_moonpetal_logs"));
        public static final TagKey<Item> VERDANT_LOGS = TagKey.create(Registries.ITEM, CorruptedAdventure.id("verdant_logs"));
        public static final TagKey<Item> ORES_TIDESILVER = TagKey.create(Registries.ITEM, common("ores/tidesilver"));
        public static final TagKey<Item> ORES_JADE = TagKey.create(Registries.ITEM, common("ores/jade"));
        public static final TagKey<Item> ORES_IN_GROUND_TIDESTONE = TagKey.create(Registries.ITEM, common("ores_in_ground/tidestone"));
        /** Convention tag {@code c:raw_materials/tidesilver}; part of {@code c:raw_materials}. */
        public static final TagKey<Item> RAW_MATERIALS_TIDESILVER = TagKey.create(Registries.ITEM, common("raw_materials/tidesilver"));
        /** Convention tag {@code c:gems/jade}; part of {@code c:gems}. */
        public static final TagKey<Item> GEMS_JADE = TagKey.create(Registries.ITEM, common("gems/jade"));
        /** Convention tag {@code c:gems/toxic_crystal}; part of {@code c:gems}. */
        public static final TagKey<Item> GEMS_TOXIC_CRYSTAL = TagKey.create(Registries.ITEM, common("gems/toxic_crystal"));
        /** Convention tag {@code c:gems/aquamarine}; part of {@code c:gems}. */
        public static final TagKey<Item> GEMS_AQUAMARINE = TagKey.create(Registries.ITEM, common("gems/aquamarine"));
        /** Convention tag {@code c:gems/purity_crystal}; part of {@code c:gems}. */
        public static final TagKey<Item> GEMS_PURITY_CRYSTAL = TagKey.create(Registries.ITEM, common("gems/purity_crystal"));

        // Ring 5 and Nether ring 5 (docs/plans/ring-5/contract.md §3); contents from stream B.
        public static final TagKey<Item> CORRUPTED_LOGS = TagKey.create(Registries.ITEM, CorruptedAdventure.id("corrupted_logs"));
        public static final TagKey<Item> ORES_NULLITE = TagKey.create(Registries.ITEM, common("ores/nullite"));
        public static final TagKey<Item> ORES_IN_GROUND_CORRUPTED_STONE = TagKey.create(Registries.ITEM, common("ores_in_ground/corrupted_stone"));
        public static final TagKey<Item> ORES_IN_GROUND_VOIDSTONE = TagKey.create(Registries.ITEM, common("ores_in_ground/voidstone"));
        /** Convention tag {@code c:raw_materials/nullite}; part of {@code c:raw_materials}. */
        public static final TagKey<Item> RAW_MATERIALS_NULLITE = TagKey.create(Registries.ITEM, common("raw_materials/nullite"));
        public static final TagKey<Item> ORES_WHITE_CORE = TagKey.create(Registries.ITEM, common("ores/white_core"));
        public static final TagKey<Item> ORES_IN_GROUND_WHITE_STONE = TagKey.create(Registries.ITEM, common("ores_in_ground/white_stone"));
        /** Convention tag {@code c:gems/white_core}; part of {@code c:gems}. */
        public static final TagKey<Item> GEMS_WHITE_CORE = TagKey.create(Registries.ITEM, common("gems/white_core"));

        // Gear tiers: the repair material (c:ingots/<tier>, part of c:ingots); stream D fills them with the ingots.
        public static final TagKey<Item> INGOTS_SUNSTONE = TagKey.create(Registries.ITEM, common("ingots/sunstone"));
        public static final TagKey<Item> INGOTS_VOLCANITE = TagKey.create(Registries.ITEM, common("ingots/volcanite"));
        public static final TagKey<Item> INGOTS_TIDESILVER = TagKey.create(Registries.ITEM, common("ingots/tidesilver"));
        public static final TagKey<Item> INGOTS_NULLITE = TagKey.create(Registries.ITEM, common("ingots/nullite"));

        private Items() {
        }
    }

    public static final class Biomes {
        /** Biomes that get bare patches with dead trees (ring 1: the vegetated land copies). */
        public static final TagKey<Biome> HAS_BARE_PATCH = TagKey.create(Registries.BIOME, CorruptedAdventure.id("has_feature/bare_patch"));
        /** Biomes that get unnatural tunnels (ring 1: every copy, cave biomes included). */
        public static final TagKey<Biome> HAS_UNNATURAL_TUNNEL = TagKey.create(Registries.BIOME, CorruptedAdventure.id("has_feature/unnatural_tunnel"));
        /** Biomes that get the crimson floor soul crystal ore (Nether ring 1: the crimson forest copy). */
        public static final TagKey<Biome> HAS_CRIMSON_SOUL_CRYSTAL_ORE = TagKey.create(Registries.BIOME, CorruptedAdventure.id("has_feature/crimson_soul_crystal_ore"));
        /**
         * Every biome with ring 2 worldgen ({@code Ring2Biomes.ALL}); for biome modifiers and filters of the later ring 2
         * changes (ores, caves).
         */
        public static final TagKey<Biome> IS_RING_2 = TagKey.create(Registries.BIOME, CorruptedAdventure.id("is_ring_2"));
        /** The biomes of Nether ring 1 (the vanilla Nether copies). */
        public static final TagKey<Biome> IS_NETHER_RING_1 = TagKey.create(Registries.BIOME, CorruptedAdventure.id("is_nether_ring_1"));

        /** The biomes of Nether ring 2 ({@code NetherRing2Biomes.ALL}). */
        public static final TagKey<Biome> IS_NETHER_RING_2 = TagKey.create(Registries.BIOME, CorruptedAdventure.id("is_nether_ring_2"));

        // Ring 3 and Nether ring 3 (docs/plans/ring-3/contract.md §2); contents from the worldgen streams.
        /** Every biome with ring 3 worldgen, the transition biome {@code steam_fens} included. */
        public static final TagKey<Biome> IS_RING_3 = TagKey.create(Registries.BIOME, CorruptedAdventure.id("is_ring_3"));
        /** The biomes of Nether ring 3. */
        public static final TagKey<Biome> IS_NETHER_RING_3 = TagKey.create(Registries.BIOME, CorruptedAdventure.id("is_nether_ring_3"));
        /** Biomes where the heat meter rises faster ({@code volcanic_peaks}). */
        public static final TagKey<Biome> HEAT_PEAKS = TagKey.create(Registries.BIOME, CorruptedAdventure.id("heat_peaks"));
        /** Biomes where the heat meter falls ({@code dormant_caldera}). */
        public static final TagKey<Biome> HEAT_REFUGES = TagKey.create(Registries.BIOME, CorruptedAdventure.id("heat_refuges"));
        /**
         * Biomes in the heat zone where placed water does not evaporate and a wet sponge does not dry ({@code HeatZone};
         * the ring 3 part of {@code verdant_falls}, filled by W-biomes). Separate from {@link #HEAT_REFUGES}: the dormant
         * caldera cools but stays dry (user decision 2026-10-15).
         */
        public static final TagKey<Biome> EVAPORATION_REFUGES = TagKey.create(Registries.BIOME, CorruptedAdventure.id("evaporation_refuges"));

        // ring 3 — conditions g2
        /**
         * Nether ring 3 biomes where the cold rises faster: the open plain ({@code frozen_lava_sea}; contents by
         * W-nether in {@code NetherRing3BiomeTags}).
         */
        public static final TagKey<Biome> COLD_EXPOSED = TagKey.create(Registries.BIOME, CorruptedAdventure.id("cold_exposed"));

        // Ring 4 and Nether ring 4 (docs/plans/ring-4/contract.md §2); contents from the worldgen streams.
        /** Every biome with ring 4 worldgen, the transition biome {@code sundered_reach} included. */
        public static final TagKey<Biome> IS_RING_4 = TagKey.create(Registries.BIOME, CorruptedAdventure.id("is_ring_4"));
        /** The biomes of Nether ring 4. */
        public static final TagKey<Biome> IS_NETHER_RING_4 = TagKey.create(Registries.BIOME, CorruptedAdventure.id("is_nether_ring_4"));
        /** The blighted band of ring 4 and {@code withered_gardens} (poison sources, poison rain). */
        public static final TagKey<Biome> RING_4_BLIGHTED = TagKey.create(Registries.BIOME, CorruptedAdventure.id("ring_4_blighted"));
        /** Biomes that purify (every falling condition meter falls faster, {@code PurifyingModifier}): the Nether ring 4 biomes. */
        public static final TagKey<Biome> PURIFYING = TagKey.create(Registries.BIOME, CorruptedAdventure.id("purifying"));

        // Ring 5 and Nether ring 5 (docs/plans/ring-5/contract.md §2); contents from the worldgen streams.
        /** The six ring 5 biomes (not {@code sundered_reach}: it stays in {@link #IS_RING_4}). */
        public static final TagKey<Biome> IS_RING_5 = TagKey.create(Registries.BIOME, CorruptedAdventure.id("is_ring_5"));
        /** The biomes of Nether ring 5. */
        public static final TagKey<Biome> IS_NETHER_RING_5 = TagKey.create(Registries.BIOME, CorruptedAdventure.id("is_nether_ring_5"));
        /** The biomes of ring 5's inner (dead) band; a fallback only, the band itself is {@code Ring5Band}. */
        public static final TagKey<Biome> RING_5_INNER = TagKey.create(Registries.BIOME, CorruptedAdventure.id("ring_5_inner"));
        /** The biomes of ring 5's outer band (falling away into the void); a fallback only, like {@link #RING_5_INNER}. */
        public static final TagKey<Biome> RING_5_OUTER = TagKey.create(Registries.BIOME, CorruptedAdventure.id("ring_5_outer"));
        /** Biomes without ambience or music ({@code false_meadow}, Nether ring 5). */
        public static final TagKey<Biome> SILENT = TagKey.create(Registries.BIOME, CorruptedAdventure.id("silent"));

        private Biomes() {
        }
    }

    /** A tag in the shared {@code c} namespace (conventions shared with other mods). */
    private static ResourceLocation common(String path) {
        return ResourceLocation.fromNamespaceAndPath("c", path);
    }
}
