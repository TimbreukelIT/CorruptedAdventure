package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModFeatures;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static nl.timtoys.corruptedadventure.worldgen.feature.Ring5Placements.SURFACE;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring5Placements.configuredKey;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring5Placements.perChunk;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring5Placements.placedKey;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring5Placements.register;

/**
 * The features of package 94 (inner band, {@code false_meadow}, horror; besluiten gebruiker 2026-10-23, V1–V7):
 * {@code corrupted_wastes}' dead trees in groves (~30 % with tufts of persistent corrupted leaves), dead bushes and small
 * rocks; {@code false_meadow}'s vanilla grass and flowers and its mirrored oak pairs; the clean missing chunks; the
 * memories; the abandoned camps; Herobrine's signs and torch patterns. Numbers here are the tuning loop's.
 */
public final class Ring5InnerFeatures {
    // Configured.
    static final ResourceKey<ConfiguredFeature<?, ?>> MISSING_CHUNK = configuredKey("missing_chunk");
    static final ResourceKey<ConfiguredFeature<?, ?>> WASTES_DEAD_TREE = configuredKey("wastes_dead_tree");
    static final ResourceKey<ConfiguredFeature<?, ?>> WASTES_TUFTED_TREE = configuredKey("wastes_tufted_tree");
    static final ResourceKey<ConfiguredFeature<?, ?>> WASTES_TREE = configuredKey("wastes_tree");
    static final ResourceKey<ConfiguredFeature<?, ?>> DEAD_BUSH = configuredKey("dead_bush");
    static final ResourceKey<ConfiguredFeature<?, ?>> WASTES_ROCK = configuredKey("wastes_rock");
    static final ResourceKey<ConfiguredFeature<?, ?>> MEADOW_GRASS = configuredKey("meadow_grass");
    static final ResourceKey<ConfiguredFeature<?, ?>> MEADOW_FLOWER = configuredKey("meadow_flower");
    static final ResourceKey<ConfiguredFeature<?, ?>> MIRRORED_TREES = configuredKey("mirrored_trees");
    static final ResourceKey<ConfiguredFeature<?, ?>> ABANDONED_CAMP = configuredKey("abandoned_camp");
    static final ResourceKey<ConfiguredFeature<?, ?>> HEROBRINE_SIGN = configuredKey("herobrine_sign");
    static final ResourceKey<ConfiguredFeature<?, ?>> TORCH_PATTERN = configuredKey("torch_pattern");
    static final Map<Ring5Memories.Kind, ResourceKey<ConfiguredFeature<?, ?>>> MEMORY = configuredKeys();

    // Placed.
    public static final ResourceKey<PlacedFeature> MISSING_CHUNKS = placedKey("missing_chunks");
    public static final ResourceKey<PlacedFeature> WASTES_DEAD_TREES = placedKey("wastes_dead_trees");
    public static final ResourceKey<PlacedFeature> WASTES_DEAD_BUSHES = placedKey("wastes_dead_bushes");
    public static final ResourceKey<PlacedFeature> WASTES_ROCKS = placedKey("wastes_rocks");
    /** The same rocks, sparse, on {@code glitch_fields}' bare stone (plan 94.1: "spaarzaam glitch_fields"). */
    public static final ResourceKey<PlacedFeature> GLITCH_ROCKS = placedKey("glitch_rocks");
    public static final ResourceKey<PlacedFeature> MEADOW_GRASS_PATCHES = placedKey("meadow_grass");
    public static final ResourceKey<PlacedFeature> MEADOW_FLOWERS = placedKey("meadow_flowers");
    public static final ResourceKey<PlacedFeature> MEADOW_MIRRORED_TREES = placedKey("meadow_mirrored_trees");
    public static final ResourceKey<PlacedFeature> ABANDONED_CAMPS = placedKey("abandoned_camps");
    public static final ResourceKey<PlacedFeature> HEROBRINE_SIGNS = placedKey("herobrine_signs");
    public static final ResourceKey<PlacedFeature> TORCH_PATTERNS = placedKey("torch_patterns");
    public static final Map<Ring5Memories.Kind, ResourceKey<PlacedFeature>> MEMORIES = placedKeys();

    /** The dead trees' groves (the ring 4 grove noise with a salt of its own): none below −0,1, every cell from 0,25. */
    static final ClusterDensity GROVES = ClusterDensity.of(Ring4Placements.GROVES_NOISE, -0.1, 0.25).withSalt(51);
    /** The share of the dead trees with tufts of leaves (V2-B). */
    static final float TUFTED_SHARE = 0.3F;

    private Ring5InnerFeatures() {
    }

    private static Map<Ring5Memories.Kind, ResourceKey<ConfiguredFeature<?, ?>>> configuredKeys() {
        Map<Ring5Memories.Kind, ResourceKey<ConfiguredFeature<?, ?>>> keys = new EnumMap<>(Ring5Memories.Kind.class);
        for (Ring5Memories.Kind kind : Ring5Memories.Kind.values()) {
            keys.put(kind, configuredKey("memory_" + kind.getSerializedName()));
        }
        return Map.copyOf(keys);
    }

    private static Map<Ring5Memories.Kind, ResourceKey<PlacedFeature>> placedKeys() {
        Map<Ring5Memories.Kind, ResourceKey<PlacedFeature>> keys = new EnumMap<>(Ring5Memories.Kind.class);
        for (Ring5Memories.Kind kind : Ring5Memories.Kind.values()) {
            keys.put(kind, placedKey("memory_" + kind.getSerializedName()));
        }
        return Map.copyOf(keys);
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        FeatureUtils.register(context, MISSING_CHUNK, ModFeatures.MISSING_CHUNK.get(), NoneFeatureConfiguration.INSTANCE);

        BlockState log = ModBlocks.CORRUPTED_LOG.get().defaultBlockState();
        BlockState wood = ModBlocks.CORRUPTED_WOOD.get().defaultBlockState();
        BlockState leaves = ModBlocks.CORRUPTED_LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        // Bare, 4–9 high, a little crooked, a few short dead limbs; no water, no vines.
        FeatureUtils.register(context, WASTES_DEAD_TREE, ModFeatures.DEAD_TREE.get(), new DeadTreeConfiguration(log, wood,
                UniformInt.of(4, 9), UniformInt.of(0, 2), UniformInt.of(1, 3), UniformInt.of(1, 3), 0, 0.0F, ConstantInt.of(1),
                BlockStateProvider.simple(leaves), 0.0F));
        // The same with grey tufts on the tops of its wood.
        FeatureUtils.register(context, WASTES_TUFTED_TREE, ModFeatures.DEAD_TREE.get(), new DeadTreeConfiguration(log, wood,
                UniformInt.of(4, 9), UniformInt.of(0, 2), UniformInt.of(1, 3), UniformInt.of(1, 3), 0, 0.0F, ConstantInt.of(1),
                BlockStateProvider.simple(leaves), 0.35F));
        FeatureUtils.register(context, WASTES_TREE, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(
                List.of(new WeightedPlacedFeature(PlacementUtils.inlinePlaced(features.getOrThrow(WASTES_TUFTED_TREE)), TUFTED_SHARE)),
                PlacementUtils.inlinePlaced(features.getOrThrow(WASTES_DEAD_TREE))));
        FeatureUtils.register(context, DEAD_BUSH, Feature.SIMPLE_BLOCK,
                new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.DEAD_BUSH)));
        BlockStateProvider cobble = BlockStateProvider.simple(ModBlocks.CORRUPTED_COBBLESTONE.get());
        FeatureUtils.register(context, WASTES_ROCK, ModFeatures.BOULDER.get(), new BoulderConfiguration(cobble, cobble, 0.0F,
                Blocks.AIR.defaultBlockState(), 0.0F, UniformFloat.of(1.0F, 2.0F), UniformFloat.of(1.0F, 2.0F)));

        FeatureUtils.register(context, MEADOW_GRASS, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new WeightedStateProvider(
                SimpleWeightedRandomList.<BlockState>builder().add(Blocks.SHORT_GRASS.defaultBlockState(), 9)
                        .add(Blocks.TALL_GRASS.defaultBlockState(), 1))));
        // Vanilla plains' flowers.
        FeatureUtils.register(context, MEADOW_FLOWER, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new WeightedStateProvider(
                SimpleWeightedRandomList.<BlockState>builder().add(Blocks.DANDELION.defaultBlockState(), 4).add(Blocks.POPPY.defaultBlockState(), 4)
                        .add(Blocks.AZURE_BLUET.defaultBlockState(), 2).add(Blocks.OXEYE_DAISY.defaultBlockState(), 2)
                        .add(Blocks.CORNFLOWER.defaultBlockState(), 2).add(Blocks.WHITE_TULIP.defaultBlockState(), 1)
                        .add(Blocks.ORANGE_TULIP.defaultBlockState(), 1).add(Blocks.RED_TULIP.defaultBlockState(), 1)
                        .add(Blocks.PINK_TULIP.defaultBlockState(), 1))));
        none(context, MIRRORED_TREES, ModFeatures.MIRRORED_TREES.get());
        none(context, ABANDONED_CAMP, ModFeatures.ABANDONED_CAMP.get());
        none(context, HEROBRINE_SIGN, ModFeatures.HEROBRINE_SIGN.get());
        none(context, TORCH_PATTERN, ModFeatures.TORCH_PATTERN.get());
        for (Ring5Memories.Kind kind : Ring5Memories.Kind.values()) {
            FeatureUtils.register(context, MEMORY.get(kind), ModFeatures.RING_5_MEMORY.get(), new MemoryFeature.Config(kind));
        }
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        perChunk(context, MISSING_CHUNKS, MISSING_CHUNK);
        for (Ring5Memories.Kind kind : Ring5Memories.Kind.values()) {
            perChunk(context, MEMORIES.get(kind), MEMORY.get(kind));
        }
        register(context, WASTES_DEAD_TREES, WASTES_TREE, JitteredGridPlacement.of(8, 2, 0.45F, GROVES), SURFACE,
                Ring5Filter.of(4, 3, Ring5Memories.OBJECT_CLEARANCE));
        register(context, WASTES_DEAD_BUSHES, DEAD_BUSH, CountPlacement.of(3), InSquarePlacement.spread(), SURFACE);
        register(context, WASTES_ROCKS, WASTES_ROCK, JitteredGridPlacement.of(16, 3, 0.35F), SURFACE,
                Ring5Filter.of(4, 2, Ring5Memories.OBJECT_CLEARANCE));
        register(context, GLITCH_ROCKS, WASTES_ROCK, JitteredGridPlacement.of(16, 3, 0.15F), SURFACE,
                Ring5Filter.of(4, 2, Ring5Memories.OBJECT_CLEARANCE));
        // A meadow as thick as vanilla's plains (about a third of the ground grown over), its flowers plain to see.
        register(context, MEADOW_GRASS_PATCHES, MEADOW_GRASS, CountPlacement.of(96), InSquarePlacement.spread(), SURFACE);
        register(context, MEADOW_FLOWERS, MEADOW_FLOWER, CountPlacement.of(10), InSquarePlacement.spread(), SURFACE);
        perChunk(context, MEADOW_MIRRORED_TREES, MIRRORED_TREES);
        register(context, ABANDONED_CAMPS, ABANDONED_CAMP, JitteredGridPlacement.of(16, 4, 0.1F), SURFACE, Ring5Filter.of(6, 0, 4));
        register(context, HEROBRINE_SIGNS, HEROBRINE_SIGN, JitteredGridPlacement.of(16, 1, 0.2F), SURFACE, Ring5Filter.of(3, 0, 3));
        register(context, TORCH_PATTERNS, TORCH_PATTERN, JitteredGridPlacement.of(16, 3, 0.075F), SURFACE,
                Ring5Filter.of(4, 0, Ring5Memories.OBJECT_CLEARANCE));
    }

    private static void none(BootstrapContext<ConfiguredFeature<?, ?>> context, ResourceKey<ConfiguredFeature<?, ?>> key,
                             Feature<NoneFeatureConfiguration> feature) {
        FeatureUtils.register(context, key, feature, NoneFeatureConfiguration.INSTANCE);
    }
}
