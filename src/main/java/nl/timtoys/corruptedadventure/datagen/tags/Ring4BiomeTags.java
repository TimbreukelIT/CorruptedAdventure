package nl.timtoys.corruptedadventure.datagen.tags;

import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.util.function.Function;

/**
 * Biome tags of the ring 4 overworld biomes and the transition {@code sundered_reach} ({@code is_ring_4},
 * {@code ring_4_blighted}, {@code c:} tags), written by {@code ModBiomeTagsProvider}. Owned by stream W-biomes
 * ({@code docs/plans/ring-4/waves.md}); empty until the biomes exist.
 */
public final class Ring4BiomeTags {
    private Ring4BiomeTags() {
    }

    public static void addTags(Function<TagKey<Biome>, TagsProvider.TagAppender<Biome>> tag) {
    }
}
