package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.worldgen.biome.Ring4Biomes;

import java.util.List;
import java.util.Set;

/**
 * Datagen bootstraps (configured + placed features) of ring 4 and its one global feature order. Owned by stream
 * W-biomes ({@code docs/plans/ring-4/waves.md}); the feature classes per biome ({@link VerdantFallsFeatures}, …) are
 * called from here, so {@code DataGenerators} stays untouched.
 * <p>
 * {@link #ORDER} lists every biome-own ring 4 feature per generation step in one order, as {@code Ring3Features.ORDER}
 * does for ring 3; each biome gets the subset that names it, in that order ({@code Ring4Biomes.generation}), which keeps
 * vanilla's feature sorter free of cycles.
 */
public final class Ring4Features {
    /** A biome-own feature: its step and the ring 4 biomes that place it. */
    public record Entry(ResourceKey<PlacedFeature> feature, GenerationStep.Decoration step, Set<ResourceKey<Biome>> biomes) {
        public Entry {
            biomes = Set.copyOf(biomes);
        }
    }

    /** Every biome-own ring 4 feature in the global order (within a step, list order = placement order). */
    public static final List<Entry> ORDER = List.of(
            // 75: under the islands of every ground biome, the glow on the ground first, then what hangs above it.
            ground(Ring4MythicFeatures.ISLAND_GLOW_GROUND),
            ground(Ring4MythicFeatures.ISLAND_UNDERSIDES),
            // verdant_falls: the moss carpets on the treads, the climbing strands under the ledges.
            vegetal(VerdantFallsFeatures.VERDANT_FALLS_MOSS_CARPETS, ModBiomes.VERDANT_FALLS),
            vegetal(VerdantFallsFeatures.VERDANT_FALLS_LEDGE_STRANDS, ModBiomes.VERDANT_FALLS),
            // 75b: the tropical flora (after the strands: no plant ever shortens a climbing route).
            vegetal(VerdantFallsFeatures.VERDANT_FALLS_BANK_TREES, ModBiomes.VERDANT_FALLS),
            vegetal(VerdantFallsFeatures.VERDANT_FALLS_RIM_TREES, ModBiomes.VERDANT_FALLS),
            vegetal(VerdantFallsFeatures.VERDANT_FALLS_TREAD_TREE_FERNS, ModBiomes.VERDANT_FALLS),
            vegetal(VerdantFallsFeatures.VERDANT_FALLS_RISER_TREE_FERNS, ModBiomes.VERDANT_FALLS),
            vegetal(VerdantFallsFeatures.VERDANT_FALLS_WALL_PLANTS, ModBiomes.VERDANT_FALLS),
            vegetal(VerdantFallsFeatures.VERDANT_FALLS_FERNS, ModBiomes.VERDANT_FALLS),
            vegetal(VerdantFallsFeatures.VERDANT_FALLS_TALL_FERNS, ModBiomes.VERDANT_FALLS),
            vegetal(VerdantFallsFeatures.VERDANT_FALLS_GIANT_LEAVES, ModBiomes.VERDANT_FALLS),
            vegetal(VerdantFallsFeatures.VERDANT_FALLS_BANK_GIANT_LEAVES, ModBiomes.VERDANT_FALLS),
            vegetal(VerdantFallsFeatures.VERDANT_FALLS_BROMELIADS, ModBiomes.VERDANT_FALLS),
            vegetal(VerdantFallsFeatures.VERDANT_FALLS_BLOSSOM_BUSHES, ModBiomes.VERDANT_FALLS),
            vegetal(VerdantFallsFeatures.VERDANT_FALLS_LOTUS, ModBiomes.VERDANT_FALLS),
            vegetal(VerdantFallsFeatures.VERDANT_FALLS_WATER_BLOOMS, ModBiomes.VERDANT_FALLS),
            // 75: the trees of the mythical band and the sky gardens.
            vegetal(Ring4MythicFeatures.CASCADE_VALE_TREES, ModBiomes.CASCADE_VALE),
            vegetal(Ring4MythicFeatures.SKY_GARDENS_TREES, ModBiomes.SKY_GARDENS),
            vegetal(Ring4MythicFeatures.GLOWROOT_SWAMP_TREES, ModBiomes.GLOWROOT_SWAMP),
            vegetal(Ring4MythicFeatures.MIST_FJORDS_TREES, ModBiomes.MIST_FJORDS),
            // 75: the water (the springs change the floor before anything grows on it).
            vegetal(Ring4MythicFeatures.DEEP_POOL_BUBBLE_SPRINGS, ModBiomes.CRYSTAL_LAGOON, ModBiomes.LOTUS_SHALLOWS,
                    ModBiomes.GLOWROOT_SWAMP, ModBiomes.MIST_FJORDS),
            vegetal(Ring4MythicFeatures.LAGOON_CORAL, ModBiomes.CRYSTAL_LAGOON),
            vegetal(Ring4MythicFeatures.LAGOON_SEAGRASS, ModBiomes.CRYSTAL_LAGOON),
            vegetal(Ring4MythicFeatures.LAGOON_SEA_PICKLES, ModBiomes.CRYSTAL_LAGOON),
            vegetal(Ring4MythicFeatures.SHALLOWS_SEAGRASS, ModBiomes.CASCADE_VALE, ModBiomes.LOTUS_SHALLOWS, ModBiomes.GLOWROOT_SWAMP,
                    ModBiomes.MIST_FJORDS),
            vegetal(Ring4MythicFeatures.LOTUS_SHALLOWS_GIANT_LILY_PADS, ModBiomes.LOTUS_SHALLOWS),
            vegetal(Ring4MythicFeatures.LOTUS_SHALLOWS_LOTUS, ModBiomes.LOTUS_SHALLOWS),
            vegetal(Ring4MythicFeatures.LOTUS_SHALLOWS_REEDS, ModBiomes.LOTUS_SHALLOWS),
            // 75: the land.
            vegetal(Ring4MythicFeatures.MYTHIC_GRASS, ModBiomes.CASCADE_VALE, ModBiomes.LOTUS_SHALLOWS, ModBiomes.GLOWROOT_SWAMP,
                    ModBiomes.SKY_GARDENS),
            vegetal(Ring4MythicFeatures.LAKESIDE_GLOW_BLOOMS, ModBiomes.CASCADE_VALE, ModBiomes.LOTUS_SHALLOWS, ModBiomes.SKY_GARDENS),
            vegetal(Ring4MythicFeatures.GLOWROOT_SWAMP_GLOWCAPS, ModBiomes.GLOWROOT_SWAMP),
            vegetal(Ring4MythicFeatures.GLOWROOT_SWAMP_MOSS, ModBiomes.GLOWROOT_SWAMP),
            vegetal(Ring4MythicFeatures.MIST_FJORDS_FERNS, ModBiomes.MIST_FJORDS),
            vegetal(Ring4MythicFeatures.MIST_FJORDS_MOSS, ModBiomes.MIST_FJORDS),
            vegetal(Ring4MythicFeatures.SKY_GARDENS_FLOWERS, ModBiomes.SKY_GARDENS),
            // 76: the hazards shape the ground before anything grows on it.
            local(Ring4BlightedFeatures.PEAT_MOORS_GAS_POCKETS, ModBiomes.PEAT_MOORS),
            local(Ring4BlightedFeatures.PEAT_MOORS_SINKHOLE_GAS, ModBiomes.PEAT_MOORS),
            local(Ring4BlightedFeatures.PEAT_MOORS_TOXIC_POOLS, ModBiomes.PEAT_MOORS),
            local(Ring4BlightedFeatures.BLIGHTED_TOXIC_POOLS, ModBiomes.REED_MARSH, ModBiomes.DROWNED_FOREST),
            local(Ring4BlightedFeatures.BANK_SUCKING_MUD, ModBiomes.PEAT_MOORS, ModBiomes.DROWNED_FOREST),
            // 77: aquamarine geodes half open under deep water (the lagoon's troughs, the fjords).
            local(Ring4OreFeatures.DEEP_WATER_AQUAMARINE_GEODES, ModBiomes.CRYSTAL_LAGOON, ModBiomes.MIST_FJORDS),
            // 76: the dead trees, the reeds (dense in the marsh), the blighted glowcaps and dead bushes.
            vegetal(Ring4BlightedFeatures.DROWNED_FOREST_TREES, ModBiomes.DROWNED_FOREST),
            vegetal(Ring4BlightedFeatures.WITHERED_GARDENS_TREES, ModBiomes.WITHERED_GARDENS),
            vegetal(Ring4BlightedFeatures.REED_MARSH_REEDS, ModBiomes.REED_MARSH),
            vegetal(Ring4BlightedFeatures.BLIGHTED_BANK_REEDS, ModBiomes.PEAT_MOORS, ModBiomes.DROWNED_FOREST),
            vegetal(Ring4BlightedFeatures.REED_MARSH_GLOWCAPS, ModBiomes.REED_MARSH),
            vegetal(Ring4BlightedFeatures.BLIGHTED_GLOWCAPS, ModBiomes.PEAT_MOORS, ModBiomes.DROWNED_FOREST, ModBiomes.WITHERED_GARDENS),
            vegetal(Ring4BlightedFeatures.BLIGHTED_DEAD_BUSHES, ModBiomes.PEAT_MOORS, ModBiomes.WITHERED_GARDENS));

    private Ring4Features() {
    }

    @SafeVarargs
    private static Entry vegetal(ResourceKey<PlacedFeature> feature, ResourceKey<Biome>... biomes) {
        return new Entry(feature, GenerationStep.Decoration.VEGETAL_DECORATION, Set.of(biomes));
    }

    @SafeVarargs
    private static Entry local(ResourceKey<PlacedFeature> feature, ResourceKey<Biome>... biomes) {
        return new Entry(feature, GenerationStep.Decoration.LOCAL_MODIFICATIONS, Set.of(biomes));
    }

    /** In every ground biome (the islands lie over all of them; the feature finds them through the terrain). */
    private static Entry ground(ResourceKey<PlacedFeature> feature) {
        return new Entry(feature, GenerationStep.Decoration.VEGETAL_DECORATION, Set.copyOf(Ring4Biomes.GROUND));
    }

    /** The biome-own features of {@code biome} in {@code step}, in the global order. */
    public static List<ResourceKey<PlacedFeature>> features(ResourceKey<Biome> biome, GenerationStep.Decoration step) {
        return ORDER.stream().filter(entry -> entry.step() == step && entry.biomes().contains(biome)).map(Entry::feature).toList();
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        VerdantFallsFeatures.configured(context);
        Ring4MythicFeatures.configured(context);
        Ring4BlightedFeatures.configured(context);
        Ring4OreFeatures.configured(context);
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        VerdantFallsFeatures.placed(context);
        Ring4MythicFeatures.placed(context);
        Ring4BlightedFeatures.placed(context);
        Ring4OreFeatures.placed(context);
    }
}
