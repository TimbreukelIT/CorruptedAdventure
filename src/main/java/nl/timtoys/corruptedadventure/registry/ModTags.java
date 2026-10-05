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
         * sprouts. Worldgen of that ring keeps its determinism rules on this tag; later changes add the dew crystal.
         */
        public static final TagKey<Block> FUNGAL_GROWTHS = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("fungal_growths"));

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

        private Biomes() {
        }
    }

    /** A tag in the shared {@code c} namespace (conventions shared with other mods). */
    private static ResourceLocation common(String path) {
        return ResourceLocation.fromNamespaceAndPath("c", path);
    }
}
