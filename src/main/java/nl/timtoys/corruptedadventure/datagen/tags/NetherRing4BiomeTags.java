package nl.timtoys.corruptedadventure.datagen.tags;

import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.Tags;
import nl.timtoys.corruptedadventure.registry.ModTags;
import nl.timtoys.corruptedadventure.worldgen.biome.NetherRing4Biomes;

import java.util.function.Function;

/**
 * Biome tags of the Nether ring 4 biomes ({@code is_nether_ring_4}, {@code purifying}, {@code c:} tags), written by
 * {@code ModBiomeTagsProvider}. Owned by stream W-nether ({@code docs/plans/ring-4/waves.md}). Like Nether ring 2 and 3
 * no {@code minecraft:} tags (no {@code is_nether}: no fortresses or ruined portals until phase 3).
 */
public final class NetherRing4BiomeTags {
    private NetherRing4BiomeTags() {
    }

    public static void addTags(Function<TagKey<Biome>, TagsProvider.TagAppender<Biome>> tag) {
        tag.apply(ModTags.Biomes.IS_NETHER_RING_4).addAll(NetherRing4Biomes.ALL);
        // purifying (contract §2, read by stream C in 79): every condition meter falls faster in all of Nether ring 4.
        tag.apply(ModTags.Biomes.PURIFYING).addAll(NetherRing4Biomes.ALL);
        tag.apply(Tags.Biomes.IS_NETHER).addAll(NetherRing4Biomes.ALL);
        // A dry desert of bone and crystal above a lava sea.
        tag.apply(Tags.Biomes.IS_HOT_NETHER).addAll(NetherRing4Biomes.ALL);
        tag.apply(Tags.Biomes.IS_DRY_NETHER).addAll(NetherRing4Biomes.ALL);
    }
}
