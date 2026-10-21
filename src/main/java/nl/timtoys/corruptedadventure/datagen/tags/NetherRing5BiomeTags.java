package nl.timtoys.corruptedadventure.datagen.tags;

import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.Tags;
import nl.timtoys.corruptedadventure.registry.ModTags;
import nl.timtoys.corruptedadventure.worldgen.biome.NetherRing5Biomes;

import java.util.function.Function;

/**
 * Biome tags of the Nether ring 5 biomes ({@code is_nether_ring_5}, {@code silent}, {@code c:} tags), written by
 * {@code ModBiomeTagsProvider} (like {@link NetherRing4BiomeTags}). Owned by stream W-nether
 * ({@code docs/plans/ring-5/waves.md}). No {@code minecraft:} tags (no {@code is_nether}: no fortresses or ruined portals
 * until phase 3), no {@code minecraft:has_structure/*}.
 */
public final class NetherRing5BiomeTags {
    private NetherRing5BiomeTags() {
    }

    public static void addTags(Function<TagKey<Biome>, TagsProvider.TagAppender<Biome>> tag) {
        tag.apply(ModTags.Biomes.IS_NETHER_RING_5).addAll(NetherRing5Biomes.ALL);
        // silent (contract §2, read by stream A): no ambient sound and no music in all of Nether ring 5.
        tag.apply(ModTags.Biomes.SILENT).addAll(NetherRing5Biomes.ALL);
        tag.apply(Tags.Biomes.IS_NETHER).addAll(NetherRing5Biomes.ALL);
        // White stone and mirror stone, no lava and no water.
        tag.apply(Tags.Biomes.IS_DRY_NETHER).addAll(NetherRing5Biomes.ALL);
    }
}
