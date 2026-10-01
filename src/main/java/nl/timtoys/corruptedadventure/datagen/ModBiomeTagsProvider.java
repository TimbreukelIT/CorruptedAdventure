package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModTags;
import nl.timtoys.corruptedadventure.worldgen.biome.VanillaBiomeCopies;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Adds the ring 1 and Nether ring 1 copies to the vanilla biome tags their originals are in ({@code replace: false}). Membership comes
 * from running vanilla's own tag provider and keeping its direct entries for copied biomes; {@code #tag} entries need
 * no copy, they resolve once the referenced tag is mirrored too.
 * <p>
 * Never mirrored: {@code is_overworld} (vanilla's {@code has_structure/stronghold} is {@code #is_overworld}),
 * {@code has_structure/stronghold}, {@code stronghold_biased_to} and {@code has_structure/ancient_city}.
 * <p>
 * Also the NeoForge {@code c:} Nether tags for the Nether copies (written out: vanilla's provider does not know them),
 * our own {@code has_feature/*} tags, which the biome modifiers of {@code Ring1BiomeModifiers} use, and
 * {@code is_nether_ring_1}.
 */
final class ModBiomeTagsProvider extends BiomeTagsProvider {
    static final List<TagKey<Biome>> MIRRORED_TAGS = List.of(
            // Biome classes
            BiomeTags.IS_OCEAN, BiomeTags.IS_DEEP_OCEAN, BiomeTags.IS_BEACH, BiomeTags.IS_RIVER, BiomeTags.IS_MOUNTAIN,
            BiomeTags.IS_HILL, BiomeTags.IS_BADLANDS, BiomeTags.IS_JUNGLE, BiomeTags.IS_FOREST, BiomeTags.IS_SAVANNA,
            BiomeTags.IS_TAIGA,
            // Behaviour
            BiomeTags.ALLOWS_SURFACE_SLIME_SPAWNS, BiomeTags.SPAWNS_COLD_VARIANT_FROGS, BiomeTags.SPAWNS_WARM_VARIANT_FROGS,
            BiomeTags.SPAWNS_GOLD_RABBITS, BiomeTags.SPAWNS_WHITE_RABBITS, BiomeTags.SPAWNS_SNOW_FOXES,
            BiomeTags.MORE_FREQUENT_DROWNED_SPAWNS, BiomeTags.HAS_CLOSER_WATER_FOG, BiomeTags.WATER_ON_MAP_OUTLINES,
            BiomeTags.PLAYS_UNDERWATER_MUSIC, BiomeTags.PRODUCES_CORALS_FROM_BONEMEAL, BiomeTags.SNOW_GOLEM_MELTS,
            BiomeTags.POLAR_BEARS_SPAWN_ON_ALTERNATE_BLOCKS, BiomeTags.INCREASED_FIRE_BURNOUT,
            BiomeTags.REDUCED_WATER_AMBIENT_SPAWNS, BiomeTags.ALLOWS_TROPICAL_FISH_SPAWNS_AT_ANY_HEIGHT,
            BiomeTags.WITHOUT_ZOMBIE_SIEGES, BiomeTags.WITHOUT_PATROL_SPAWNS, BiomeTags.WITHOUT_WANDERING_TRADER_SPAWNS,
            BiomeTags.MINESHAFT_BLOCKING, BiomeTags.REQUIRED_OCEAN_MONUMENT_SURROUNDING,
            // Overworld structures, except stronghold and ancient city
            BiomeTags.HAS_VILLAGE_DESERT, BiomeTags.HAS_VILLAGE_PLAINS, BiomeTags.HAS_VILLAGE_SAVANNA,
            BiomeTags.HAS_VILLAGE_SNOWY, BiomeTags.HAS_VILLAGE_TAIGA, BiomeTags.HAS_DESERT_PYRAMID,
            BiomeTags.HAS_JUNGLE_TEMPLE, BiomeTags.HAS_SWAMP_HUT, BiomeTags.HAS_IGLOO, BiomeTags.HAS_PILLAGER_OUTPOST,
            BiomeTags.HAS_WOODLAND_MANSION, BiomeTags.HAS_OCEAN_MONUMENT, BiomeTags.HAS_SHIPWRECK,
            BiomeTags.HAS_SHIPWRECK_BEACHED, BiomeTags.HAS_OCEAN_RUIN_COLD, BiomeTags.HAS_OCEAN_RUIN_WARM,
            BiomeTags.HAS_TRAIL_RUINS, BiomeTags.HAS_MINESHAFT, BiomeTags.HAS_MINESHAFT_MESA,
            BiomeTags.HAS_RUINED_PORTAL_STANDARD, BiomeTags.HAS_RUINED_PORTAL_DESERT, BiomeTags.HAS_RUINED_PORTAL_JUNGLE,
            BiomeTags.HAS_RUINED_PORTAL_MOUNTAIN, BiomeTags.HAS_RUINED_PORTAL_OCEAN, BiomeTags.HAS_RUINED_PORTAL_SWAMP,
            BiomeTags.HAS_BURIED_TREASURE, BiomeTags.HAS_TRIAL_CHAMBERS,
            // Nether: is_nether also brings fortresses and Nether ruined portals (#is_nether); bastions are listed apart
            BiomeTags.IS_NETHER, BiomeTags.HAS_BASTION_REMNANT);

    /**
     * Vanilla biomes whose ring 1 copies get bare patches: vegetated land. Not oceans, rivers, beaches, deserts,
     * badlands, snow and ice, peaks, mangroves or caves.
     */
    static final List<ResourceKey<Biome>> BARE_PATCH_BIOMES = List.of(
            Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW, Biomes.FOREST, Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST,
            Biomes.OLD_GROWTH_BIRCH_FOREST, Biomes.DARK_FOREST, Biomes.TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA,
            Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA,
            Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_FOREST, Biomes.JUNGLE, Biomes.SPARSE_JUNGLE, Biomes.BAMBOO_JUNGLE,
            Biomes.SWAMP, Biomes.CHERRY_GROVE);

    ModBiomeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFileHelper) {
        super(output, lookup, CorruptedAdventure.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        super.addTags(provider);
        Map<ResourceLocation, TagBuilder> vanilla = new HashMap<>(builders);
        builders.clear();

        for (TagKey<Biome> tag : MIRRORED_TAGS) {
            TagBuilder original = vanilla.get(tag.location());
            if (original == null) {
                throw new IllegalStateException("Vanilla has no biome tag " + tag.location());
            }
            List<ResourceKey<Biome>> copies = original.build().stream()
                    .filter(entry -> !entry.isTag())
                    .map(entry -> ResourceKey.create(Registries.BIOME, entry.getId()))
                    .filter(VanillaBiomeCopies.COPIES::contains)
                    .map(VanillaBiomeCopies::copyOf)
                    .toList();
            if (!copies.isEmpty()) {
                tag(tag).addAll(copies);
            }
        }

        List<ResourceKey<Biome>> netherCopies = copies(VanillaBiomeCopies.NETHER_COPIES);
        tag(Tags.Biomes.IS_NETHER).addAll(netherCopies);
        tag(Tags.Biomes.IS_HOT_NETHER).addAll(netherCopies);
        tag(Tags.Biomes.IS_DRY_NETHER).addAll(netherCopies);
        tag(Tags.Biomes.IS_NETHER_FOREST).addAll(copies(List.of(Biomes.CRIMSON_FOREST, Biomes.WARPED_FOREST)));
        tag(ModTags.Biomes.IS_NETHER_RING_1).addAll(netherCopies);

        tag(ModTags.Biomes.HAS_BARE_PATCH).addAll(BARE_PATCH_BIOMES.stream().map(VanillaBiomeCopies::copyOf).toList());
        // Every copy, cave biomes included: the biome filter checks the biome at the tunnel's own height.
        tag(ModTags.Biomes.HAS_UNNATURAL_TUNNEL).addAll(copies(VanillaBiomeCopies.OVERWORLD_COPIES));
    }

    /** The copies of {@code vanilla}, sorted for a stable file. */
    private static List<ResourceKey<Biome>> copies(Collection<ResourceKey<Biome>> vanilla) {
        return vanilla.stream()
                .sorted(Comparator.comparing(ResourceKey::location))
                .map(VanillaBiomeCopies::copyOf)
                .toList();
    }
}
