package nl.timtoys.corruptedadventure.datagen.tags;

import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.util.function.Function;

/**
 * Biome tags of the Nether ring 3 biomes ({@code is_nether_ring_3}, {@code c:} tags), written by {@code ModBiomeTagsProvider}. Owned by stream W-nether
 * ({@code docs/plans/ring-3/waves.md}); empty until the biomes exist.
 */
public final class NetherRing3BiomeTags {
    private NetherRing3BiomeTags() {
    }

    public static void addTags(Function<TagKey<Biome>, TagsProvider.TagAppender<Biome>> tag) {
    }
}
