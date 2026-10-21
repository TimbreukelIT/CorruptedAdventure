package nl.timtoys.corruptedadventure.datagen.tags;

import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.util.function.Function;

/**
 * Biome tags of the Nether ring 5 biomes, written by {@code ModBiomeTagsProvider} (like {@link NetherRing4BiomeTags}).
 * Owned by stream W-nether ({@code docs/plans/ring-5/waves.md}); empty until the biomes exist. No {@code minecraft:has_structure/*} (phase 3).
 */
public final class NetherRing5BiomeTags {
    private NetherRing5BiomeTags() {
    }

    public static void addTags(Function<TagKey<Biome>, TagsProvider.TagAppender<Biome>> tag) {
    }
}
