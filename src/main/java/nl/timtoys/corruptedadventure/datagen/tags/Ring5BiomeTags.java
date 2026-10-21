package nl.timtoys.corruptedadventure.datagen.tags;

import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.Tags;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.registry.ModTags;
import nl.timtoys.corruptedadventure.worldgen.biome.Ring5Biomes;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Biome tags of the ring 5 overworld biomes ({@code is_ring_5}, {@code ring_5_inner}, {@code ring_5_outer},
 * {@code silent}, {@code c:} tags), written by {@code ModBiomeTagsProvider} (like {@link Ring4BiomeTags}). Owned by
 * stream W-biomes ({@code docs/plans/ring-5/waves.md}); W-nether adds the Nether-5 biomes to {@code silent}. No
 * {@code minecraft:} tags (they would switch on vanilla behaviour, and no {@code has_structure/*}: structures are phase 3).
 */
public final class Ring5BiomeTags {
    /**
     * The {@code c:} tags of ring 5 (package 86): descriptive only, for other mods. NeoForge itself attaches no
     * behaviour to {@code c:is_void} or {@code c:no_default_monsters}.
     */
    static final Map<TagKey<Biome>, List<ResourceKey<Biome>>> CONVENTION = Map.ofEntries(
            Map.entry(Tags.Biomes.IS_DRY_OVERWORLD, Ring5Biomes.ALL),
            Map.entry(Tags.Biomes.IS_SPOOKY, Ring5Biomes.ALL),
            Map.entry(Tags.Biomes.IS_WASTELAND, List.of(ModBiomes.CORRUPTED_WASTES, ModBiomes.VOID_SCARS, ModBiomes.SHATTERED_EXPANSE)),
            Map.entry(Tags.Biomes.IS_DEAD, List.of(ModBiomes.CORRUPTED_WASTES)),
            Map.entry(Tags.Biomes.IS_PLAINS, List.of(ModBiomes.FALSE_MEADOW)),
            Map.entry(Tags.Biomes.IS_VOID, List.of(ModBiomes.SHATTERED_EXPANSE)),
            Map.entry(Tags.Biomes.NO_DEFAULT_MONSTERS, Ring5Biomes.SILENT));

    private Ring5BiomeTags() {
    }

    public static void addTags(Function<TagKey<Biome>, TagsProvider.TagAppender<Biome>> tag) {
        tag.apply(ModTags.Biomes.IS_RING_5).addAll(Ring5Biomes.ALL);
        // Fallbacks for the atmosphere (sky, stars) and conditions; the band itself is Ring5Band.
        tag.apply(ModTags.Biomes.RING_5_INNER).addAll(Ring5Biomes.INNER);
        tag.apply(ModTags.Biomes.RING_5_OUTER).addAll(Ring5Biomes.OUTER);
        tag.apply(ModTags.Biomes.SILENT).addAll(Ring5Biomes.SILENT);
        CONVENTION.forEach((key, biomes) -> tag.apply(key).addAll(biomes));
    }
}
