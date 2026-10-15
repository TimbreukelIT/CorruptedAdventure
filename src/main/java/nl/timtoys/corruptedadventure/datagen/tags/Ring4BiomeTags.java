package nl.timtoys.corruptedadventure.datagen.tags;

import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.Tags;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.registry.ModTags;
import nl.timtoys.corruptedadventure.worldgen.biome.Ring4Biomes;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Biome tags of the ring 4 overworld biomes and the transitions {@code sundered_reach} and {@code verdant_falls}
 * ({@code is_ring_4}, {@code ring_4_blighted}, {@code heat_refuges}, {@code evaporation_refuges}, {@code c:} tags),
 * written by {@code ModBiomeTagsProvider}. Owned by stream W-biomes ({@code docs/plans/ring-4/waves.md}). No
 * {@code minecraft:} tags (they would switch on vanilla behaviour, and no {@code has_structure/*}: structures are phase 3).
 */
public final class Ring4BiomeTags {
    /** The {@code c:} tags of ring 4 (change 64): descriptive only, for other mods. */
    static final Map<TagKey<Biome>, List<ResourceKey<Biome>>> CONVENTION = Map.ofEntries(
            Map.entry(Tags.Biomes.IS_TEMPERATE_OVERWORLD, Ring4Biomes.WITH_TRANSITION),
            Map.entry(Tags.Biomes.IS_WET_OVERWORLD, List.of(ModBiomes.CASCADE_VALE, ModBiomes.CRYSTAL_LAGOON, ModBiomes.LOTUS_SHALLOWS,
                    ModBiomes.GLOWROOT_SWAMP, ModBiomes.MIST_FJORDS, ModBiomes.DROWNED_FOREST, ModBiomes.PEAT_MOORS, ModBiomes.REED_MARSH,
                    ModBiomes.SKY_GARDENS, ModBiomes.VERDANT_FALLS)),
            Map.entry(Tags.Biomes.IS_SWAMP, List.of(ModBiomes.GLOWROOT_SWAMP, ModBiomes.DROWNED_FOREST, ModBiomes.PEAT_MOORS, ModBiomes.REED_MARSH)),
            Map.entry(Tags.Biomes.IS_FOREST, List.of(ModBiomes.CASCADE_VALE)),
            Map.entry(Tags.Biomes.IS_AQUATIC, List.of(ModBiomes.CRYSTAL_LAGOON, ModBiomes.LOTUS_SHALLOWS)),
            Map.entry(Tags.Biomes.IS_MAGICAL, List.of(ModBiomes.CASCADE_VALE, ModBiomes.CRYSTAL_LAGOON, ModBiomes.LOTUS_SHALLOWS,
                    ModBiomes.GLOWROOT_SWAMP, ModBiomes.MIST_FJORDS, ModBiomes.SKY_GARDENS, ModBiomes.VERDANT_FALLS)),
            Map.entry(Tags.Biomes.IS_LUSH, List.of(ModBiomes.CASCADE_VALE, ModBiomes.GLOWROOT_SWAMP, ModBiomes.SKY_GARDENS,
                    ModBiomes.VERDANT_FALLS)),
            Map.entry(Tags.Biomes.IS_FLORAL, List.of(ModBiomes.LOTUS_SHALLOWS, ModBiomes.SKY_GARDENS)),
            Map.entry(Tags.Biomes.IS_SPOOKY, List.of(ModBiomes.DROWNED_FOREST, ModBiomes.PEAT_MOORS, ModBiomes.REED_MARSH,
                    ModBiomes.WITHERED_GARDENS, ModBiomes.SUNDERED_REACH)),
            Map.entry(Tags.Biomes.IS_DEAD, List.of(ModBiomes.DROWNED_FOREST, ModBiomes.WITHERED_GARDENS)),
            Map.entry(Tags.Biomes.IS_WASTELAND, List.of(ModBiomes.SUNDERED_REACH)));

    private Ring4BiomeTags() {
    }

    public static void addTags(Function<TagKey<Biome>, TagsProvider.TagAppender<Biome>> tag) {
        tag.apply(ModTags.Biomes.IS_RING_4).addAll(Ring4Biomes.WITH_TRANSITION);
        // Read by poison (C) and the weather (A): the blighted band and the islands above it.
        tag.apply(ModTags.Biomes.RING_4_BLIGHTED).addAll(Ring4Biomes.BLIGHTED_TAG);
        // verdant_falls (Q2 = A and its follow-up): its ring 3 part cools you down (heat_refuges, read by the heat rule)
        // and placed water stays (evaporation_refuges, read by the heat zone of C); the dormant caldera is only in the first.
        tag.apply(ModTags.Biomes.HEAT_REFUGES).add(ModBiomes.VERDANT_FALLS);
        tag.apply(ModTags.Biomes.EVAPORATION_REFUGES).add(ModBiomes.VERDANT_FALLS);
        CONVENTION.forEach((key, biomes) -> tag.apply(key).addAll(biomes));
    }
}
