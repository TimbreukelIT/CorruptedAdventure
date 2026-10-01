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
        /** Ground for the desert flora (and the palm sapling): sand, terracotta, dirt and cracked clay. */
        public static final TagKey<Block> DESERT_PLANT_SOIL = TagKey.create(Registries.BLOCK, CorruptedAdventure.id("desert_plant_soil"));

        private Blocks() {
        }
    }

    public static final class Items {
        public static final TagKey<Item> DEAD_LOGS = TagKey.create(Registries.ITEM, CorruptedAdventure.id("dead_logs"));
        public static final TagKey<Item> CANYON_STONES = TagKey.create(Registries.ITEM, CorruptedAdventure.id("canyon_stones"));
        public static final TagKey<Item> ORES_SOUL_CRYSTAL = TagKey.create(Registries.ITEM, common("ores/soul_crystal"));
        /** Convention tag {@code c:gems/soul_crystal}; part of {@code c:gems}. */
        public static final TagKey<Item> GEMS_SOUL_CRYSTAL = TagKey.create(Registries.ITEM, common("gems/soul_crystal"));

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
