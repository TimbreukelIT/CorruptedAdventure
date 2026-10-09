package nl.timtoys.corruptedadventure.datagen.tags;

import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.Tags;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.registry.ModTags;
import nl.timtoys.corruptedadventure.worldgen.biome.NetherRing3Biomes;

import java.util.function.Function;

/**
 * Biome tags of the Nether ring 3 biomes ({@code is_nether_ring_3}, {@code cold_exposed}, {@code c:} tags), written by
 * {@code ModBiomeTagsProvider}. Owned by stream W-nether ({@code docs/plans/ring-3/waves.md}). Like Nether ring 2 no
 * {@code minecraft:} tags (no {@code is_nether}: no fortresses or ruined portals until phase 3).
 */
public final class NetherRing3BiomeTags {
    private NetherRing3BiomeTags() {
    }

    public static void addTags(Function<TagKey<Biome>, TagsProvider.TagAppender<Biome>> tag) {
        tag.apply(ModTags.Biomes.IS_NETHER_RING_3).addAll(NetherRing3Biomes.ALL);
        tag.apply(Tags.Biomes.IS_NETHER).addAll(NetherRing3Biomes.ALL);
        tag.apply(Tags.Biomes.IS_COLD_NETHER).addAll(NetherRing3Biomes.ALL);
        // cold_exposed (contract §2): the open plain where the cold rises ×1.5 (ColdRule).
        tag.apply(ModTags.Biomes.COLD_EXPOSED).add(ModBiomes.FROZEN_LAVA_SEA);
    }
}
