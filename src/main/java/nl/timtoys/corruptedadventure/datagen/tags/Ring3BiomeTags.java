package nl.timtoys.corruptedadventure.datagen.tags;

import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.Tags;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.registry.ModTags;
import nl.timtoys.corruptedadventure.worldgen.biome.Ring3Biomes;

import java.util.function.Function;

/**
 * Biome tags of the ring 3 overworld biomes and the transition {@code steam_fens} ({@code is_ring_3}, {@code heat_peaks}, {@code heat_refuges}, {@code c:} tags),
 * written by {@code ModBiomeTagsProvider}. Owned by stream W-biomes ({@code docs/plans/ring-3/waves.md}). No
 * {@code minecraft:has_structure/*} tags: no vanilla structures in ring 3 (structures are phase 3).
 */
public final class Ring3BiomeTags {
    private Ring3BiomeTags() {
    }

    public static void addTags(Function<TagKey<Biome>, TagsProvider.TagAppender<Biome>> tag) {
        tag.apply(ModTags.Biomes.IS_RING_3).addAll(Ring3Biomes.WITH_TRANSITION);
        tag.apply(Tags.Biomes.IS_HOT_OVERWORLD).addAll(Ring3Biomes.WITH_TRANSITION);
        tag.apply(Tags.Biomes.IS_DRY_OVERWORLD).addAll(Ring3Biomes.ALL);
        tag.apply(Tags.Biomes.IS_WET_OVERWORLD).add(Ring3Biomes.TRANSITION);
        // Read by the heat condition (contract §5): heat ×2 on the peaks, falling in the dormant crater.
        tag.apply(ModTags.Biomes.HEAT_PEAKS).add(ModBiomes.VOLCANIC_PEAKS);
        tag.apply(ModTags.Biomes.HEAT_REFUGES).add(ModBiomes.DORMANT_CALDERA);
    }
}
