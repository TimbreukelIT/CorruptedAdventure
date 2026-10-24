package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModFeatures;

import static nl.timtoys.corruptedadventure.worldgen.feature.Ring5Placements.SURFACE;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring5Placements.configuredKey;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring5Placements.placedKey;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring5Placements.register;

/**
 * The features of package 95 above ground (outer band and castle sites; besluiten gebruiker 2026-10-23, V9-B, V10-A): the
 * trees hanging upside down under the inverted hills, a few dead trees on their floes ({@code void_scars} is bare rock:
 * no soil for a tree), and the rubble on the ruined castle plateaus. The rims, walls and the ruined ground itself are
 * surface rules ({@code ring_5_ground}); the ores are {@link Ring5OreFeatures}. Numbers here are the tuning loop's.
 */
public final class Ring5OuterFeatures {
    static final ResourceKey<ConfiguredFeature<?, ?>> INVERTED_TREE = configuredKey("inverted_tree");
    static final ResourceKey<ConfiguredFeature<?, ?>> CASTLE_RUBBLE = configuredKey("castle_rubble");

    public static final ResourceKey<PlacedFeature> INVERTED_TREES = placedKey("inverted_trees");
    public static final ResourceKey<PlacedFeature> FLOE_DEAD_TREES = placedKey("floe_dead_trees");
    public static final ResourceKey<PlacedFeature> CASTLE_RUBBLES = placedKey("castle_rubble");

    private Ring5OuterFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        FeatureUtils.register(context, INVERTED_TREE, ModFeatures.INVERTED_TREE.get(), NoneFeatureConfiguration.INSTANCE);
        // Heaps of the ruins: mostly corrupted cobblestone, obsidian and a little crying obsidian and glitch.
        WeightedStateProvider rubble = new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                .add(ModBlocks.CORRUPTED_COBBLESTONE.get().defaultBlockState(), 6).add(Blocks.OBSIDIAN.defaultBlockState(), 2)
                .add(Blocks.CRYING_OBSIDIAN.defaultBlockState(), 1).add(ModBlocks.GLITCH_BLOCK.get().defaultBlockState(), 1));
        FeatureUtils.register(context, CASTLE_RUBBLE, ModFeatures.BOULDER.get(), new BoulderConfiguration(rubble, rubble, 0.0F,
                Blocks.AIR.defaultBlockState(), 0.0F, UniformFloat.of(1.0F, 2.5F), UniformFloat.of(1.0F, 2.5F)));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        // The undersides are found in the feature (the hill's pure underside), the grid only gives the column.
        register(context, INVERTED_TREES, INVERTED_TREE, JitteredGridPlacement.of(8, 2, 0.8F), Ring5Filter.of(3, 0, 0));
        register(context, FLOE_DEAD_TREES, Ring5InnerFeatures.WASTES_TREE, JitteredGridPlacement.of(16, 2, 0.35F), SURFACE,
                Ring5Filter.of(4, 3, Ring5Memories.OBJECT_CLEARANCE));
        register(context, CASTLE_RUBBLES, CASTLE_RUBBLE, JitteredGridPlacement.of(8, 2, 0.35F), SURFACE, Ring5Filter.onCastle(3));
    }
}
