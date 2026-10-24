package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.worldgen.biome.Ring5Biomes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Datagen bootstrap of the ring 5 features (keys {@code corruptedadventure:ring_5/<name>}, like {@link Ring4Features}) and
 * their one global order. Owned by stream W-biomes ({@code docs/plans/ring-5/waves.md}); the feature classes per package
 * ({@link Ring5InnerFeatures} for 94, {@link Ring5OuterFeatures} and {@link Ring5OreFeatures} for 95) are called from here, so {@code DataGenerators} stays untouched.
 * <p>
 * {@link #ORDER} lists every ring 5 feature per generation step in one order; each biome gets the subset that names it
 * ({@code Ring5Biomes.bootstrap}), which keeps vanilla's feature sorter free of cycles. Every ring 5 feature writes only
 * into its own chunk ({@link Ring5Placements}), so the order within a chunk is the whole story.
 */
public final class Ring5Features {
    /** A ring 5 feature: its step and the ring 5 biomes that place it. */
    public record Entry(ResourceKey<PlacedFeature> feature, GenerationStep.Decoration step, Set<ResourceKey<Biome>> biomes) {
        public Entry {
            biomes = Set.copyOf(biomes);
        }
    }

    /** The inner band's biomes where camps stand: not the false meadow (nobody camps in the silence). */
    private static final Set<ResourceKey<Biome>> CAMP_BIOMES = Set.of(ModBiomes.CORRUPTED_WASTES, ModBiomes.GLITCH_FIELDS);

    /** Every ring 5 feature in the global order (within a step, list order = placement order). */
    public static final List<Entry> ORDER = order();

    private Ring5Features() {
    }

    private static List<Entry> order() {
        List<Entry> order = new ArrayList<>();
        // 94: the clean missing chunks first (their floor and the lava dam round them), before anything stands in them.
        order.add(new Entry(Ring5InnerFeatures.MISSING_CHUNKS, GenerationStep.Decoration.RAW_GENERATION, Set.copyOf(Ring5Biomes.INNER)));
        // 95: the ores (nullite on rock facing the void, deep nullite, glitch clusters); vanilla's come after them through
        // the biome modifier add_ring_5_ores.
        order.add(new Entry(Ring5OreFeatures.NULLITE_FACES, GenerationStep.Decoration.UNDERGROUND_ORES, Set.copyOf(Ring5Biomes.OUTER)));
        order.add(new Entry(Ring5OreFeatures.NULLITE_DEEPS, GenerationStep.Decoration.UNDERGROUND_ORES, Set.copyOf(Ring5Biomes.INNER)));
        order.add(new Entry(Ring5OreFeatures.GLITCH_CLUSTERS, GenerationStep.Decoration.UNDERGROUND_ORES, Set.copyOf(Ring5Biomes.ALL)));
        // 94: the memories are terrain-like: before the camps and every plant and tree, which keep clear of them.
        for (Ring5Memories.Kind kind : Ring5Memories.Kind.values()) {
            order.add(new Entry(Ring5InnerFeatures.MEMORIES.get(kind), GenerationStep.Decoration.LOCAL_MODIFICATIONS, Set.copyOf(Ring5Biomes.ALL)));
        }
        order.add(new Entry(Ring5InnerFeatures.ABANDONED_CAMPS, GenerationStep.Decoration.LOCAL_MODIFICATIONS, CAMP_BIOMES));
        // 95: the rubble of the ruined castle plateaus (the pinnacles are void_scars).
        order.add(new Entry(Ring5OuterFeatures.CASTLE_RUBBLES, GenerationStep.Decoration.LOCAL_MODIFICATIONS, Set.of(ModBiomes.VOID_SCARS)));
        // 94: the plants of the ground first, then the objects that go over them (rocks, trees).
        order.add(vegetal(Ring5InnerFeatures.MEADOW_GRASS_PATCHES, ModBiomes.FALSE_MEADOW));
        order.add(vegetal(Ring5InnerFeatures.MEADOW_FLOWERS, ModBiomes.FALSE_MEADOW));
        order.add(vegetal(Ring5InnerFeatures.WASTES_DEAD_BUSHES, ModBiomes.CORRUPTED_WASTES));
        order.add(vegetal(Ring5InnerFeatures.FLOE_DEAD_BUSHES, ModBiomes.INVERTED_HILLS));
        order.add(vegetal(Ring5InnerFeatures.WASTES_ROCKS, ModBiomes.CORRUPTED_WASTES));
        order.add(vegetal(Ring5InnerFeatures.GLITCH_ROCKS, ModBiomes.GLITCH_FIELDS));
        order.add(vegetal(Ring5InnerFeatures.WASTES_DEAD_TREES, ModBiomes.CORRUPTED_WASTES));
        order.add(vegetal(Ring5InnerFeatures.MEADOW_MIRRORED_TREES, ModBiomes.FALSE_MEADOW));
        // 95: the trees hanging under the inverted hills, a few dead trees on their floes.
        order.add(vegetal(Ring5OuterFeatures.INVERTED_TREES, ModBiomes.INVERTED_HILLS));
        order.add(vegetal(Ring5OuterFeatures.FLOE_DEAD_TREES, ModBiomes.INVERTED_HILLS));
        // 94: Herobrine's signs and torches last, on whatever ground is left open.
        order.add(new Entry(Ring5InnerFeatures.HEROBRINE_SIGNS, GenerationStep.Decoration.TOP_LAYER_MODIFICATION, Set.copyOf(Ring5Biomes.INNER)));
        order.add(new Entry(Ring5InnerFeatures.TORCH_PATTERNS, GenerationStep.Decoration.TOP_LAYER_MODIFICATION, Set.copyOf(Ring5Biomes.INNER)));
        return List.copyOf(order);
    }

    @SafeVarargs
    private static Entry vegetal(ResourceKey<PlacedFeature> feature, ResourceKey<Biome>... biomes) {
        return new Entry(feature, GenerationStep.Decoration.VEGETAL_DECORATION, Set.of(biomes));
    }

    /** The ring 5 features of {@code biome} in {@code step}, in the global order. */
    public static List<ResourceKey<PlacedFeature>> features(ResourceKey<Biome> biome, GenerationStep.Decoration step) {
        return ORDER.stream().filter(entry -> entry.step() == step && entry.biomes().contains(biome)).map(Entry::feature).toList();
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        Ring5InnerFeatures.configured(context);
        Ring5OuterFeatures.configured(context);
        Ring5OreFeatures.configured(context);
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        Ring5InnerFeatures.placed(context);
        Ring5OuterFeatures.placed(context);
        Ring5OreFeatures.placed(context);
    }
}
