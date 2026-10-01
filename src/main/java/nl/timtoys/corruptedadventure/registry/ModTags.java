package nl.timtoys.corruptedadventure.registry;

import net.minecraft.core.registries.Registries;
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

        private Blocks() {
        }
    }

    public static final class Items {
        public static final TagKey<Item> DEAD_LOGS = TagKey.create(Registries.ITEM, CorruptedAdventure.id("dead_logs"));

        private Items() {
        }
    }

    public static final class Biomes {
        /** Biomes that get bare patches with dead trees (ring 1: the vegetated land copies). */
        public static final TagKey<Biome> HAS_BARE_PATCH = TagKey.create(Registries.BIOME, CorruptedAdventure.id("has_feature/bare_patch"));
        /** Biomes that get unnatural tunnels (ring 1: every copy, cave biomes included). */
        public static final TagKey<Biome> HAS_UNNATURAL_TUNNEL = TagKey.create(Registries.BIOME, CorruptedAdventure.id("has_feature/unnatural_tunnel"));

        private Biomes() {
        }
    }
}
