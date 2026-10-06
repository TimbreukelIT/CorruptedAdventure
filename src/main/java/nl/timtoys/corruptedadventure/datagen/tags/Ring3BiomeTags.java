package nl.timtoys.corruptedadventure.datagen.tags;

import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.util.function.Function;

/**
 * Biome tags of the ring 3 overworld biomes ({@code is_ring_3}, {@code heat_peaks}, {@code heat_refuges}, {@code c:} tags), written by {@code ModBiomeTagsProvider}. Owned by stream W-biomes
 * ({@code docs/plans/ring-3/waves.md}); empty until the biomes exist.
 */
public final class Ring3BiomeTags {
    private Ring3BiomeTags() {
    }

    public static void addTags(Function<TagKey<Biome>, TagsProvider.TagAppender<Biome>> tag) {
    }
}
