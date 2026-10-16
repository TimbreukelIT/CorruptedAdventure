package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModFeatures;

import java.util.List;

/**
 * Datagen bootstraps (configured + placed features) of Nether ring 4. Owned by stream W-nether ({@code docs/plans/ring-4/waves.md}).
 * The shapes (dunes, columns, geodes, the sea-creature fossils) are terrain; the features dress them (78): the lining of
 * the giant geodes (N3 = A), the cladding of the crystal columns (N2 = A) and loose bones and boulders in the dunes
 * (N4 = A). Starting values, tuned at the test stop.
 */
public final class NetherRing4Features {
    /**
     * Region noise ({@code ring_4/nether/region}) from which the crystal spires start; the bone dunes lie between
     * {@link #HOLLOWS_THRESHOLD} and this, the geode hollows below. The terrain's region weights turn over here, and so do
     * the surface rule and the preset's {@code multi_noise}; {@code NetherRing4JsonTest} compares them.
     */
    public static final double SPIRES_THRESHOLD = 0.18;
    /** Region noise below which the geode hollows lie. */
    public static final double HOLLOWS_THRESHOLD = -0.18;
    /** The surface paints the columns' crystal from this region value (nether_ring4_surface.py, COLUMNS_REGION_FROM). */
    public static final double COLUMNS_REGION_FROM = 0.20;
    /** The placement puts the rubble's origin here; it finds the dune floor (Y ~40–47) by scanning down. */
    static final int RUBBLE_Y = 47;

    static final ResourceKey<ConfiguredFeature<?, ?>> GEODE_LINING = configuredKey("geode_lining");
    static final ResourceKey<ConfiguredFeature<?, ?>> PILLAR_CLADDING = configuredKey("pillar_cladding");
    static final ResourceKey<ConfiguredFeature<?, ?>> BONE_RUBBLE = configuredKey("bone_rubble");
    static final ResourceKey<ConfiguredFeature<?, ?>> FOSSIL_BOULDER = configuredKey("fossil_boulder");

    /** UNDERGROUND_DECORATION in all three biomes, once per chunk: a geode or a column may cross into a chunk of another biome. */
    public static final ResourceKey<PlacedFeature> GEODE_LININGS = placedKey("geode_linings");
    public static final ResourceKey<PlacedFeature> PILLAR_CLADDINGS = placedKey("pillar_claddings");
    /** LOCAL_MODIFICATIONS in bone_dunes: loose bones about once per 2 chunks, a fossil stone boulder about once per 6. */
    public static final ResourceKey<PlacedFeature> DUNE_BONES = placedKey("dune_bones");
    public static final ResourceKey<PlacedFeature> DUNE_BOULDERS = placedKey("dune_boulders");

    private NetherRing4Features() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        BlockState crystal = ModBlocks.PURITY_CRYSTAL_BLOCK.get().defaultBlockState();
        BlockState lamp = ModBlocks.PURITY_CRYSTAL_LAMP.get().defaultBlockState();
        BlockState cluster = ModBlocks.PURITY_CRYSTAL_CLUSTER.get().defaultBlockState();
        FeatureUtils.register(context, GEODE_LINING, ModFeatures.GIANT_GEODE_LINING.get(), new GiantGeodeLiningConfiguration(
                crystal, Blocks.CALCITE.defaultBlockState(), ModBlocks.BLEACHED_NETHERRACK.get().defaultBlockState(), lamp, cluster,
                0.25F, 0.02F));
        FeatureUtils.register(context, PILLAR_CLADDING, ModFeatures.PILLAR_CLADDING.get(), new PillarCladdingConfiguration(
                crystal, lamp, cluster, 0.18F, 1.0F / 150.0F, COLUMNS_REGION_FROM));
        BlockState bone = Blocks.BONE_BLOCK.defaultBlockState();
        FeatureUtils.register(context, BONE_RUBBLE, ModFeatures.DUNE_RUBBLE.get(), new RubbleConfiguration(weighted(
                SimpleWeightedRandomList.<BlockState>builder()
                        .add(bone.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X), 2)
                        .add(bone.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z), 2)
                        .add(bone.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y), 1)), UniformInt.of(1, 2)));
        FeatureUtils.register(context, FOSSIL_BOULDER, ModFeatures.DUNE_RUBBLE.get(), new RubbleConfiguration(
                BlockStateProvider.simple(ModBlocks.FOSSIL_STONE.get()), UniformInt.of(2, 3)));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        PlacementUtils.register(context, GEODE_LININGS, features.getOrThrow(GEODE_LINING));
        PlacementUtils.register(context, PILLAR_CLADDINGS, features.getOrThrow(PILLAR_CLADDING));
        HeightRangePlacement rubbleY = HeightRangePlacement.of(ConstantHeight.of(VerticalAnchor.absolute(RUBBLE_Y)));
        PlacementUtils.register(context, DUNE_BONES, features.getOrThrow(BONE_RUBBLE),
                RarityFilter.onAverageOnceEvery(2), InSquarePlacement.spread(), rubbleY, BiomeFilter.biome());
        PlacementUtils.register(context, DUNE_BOULDERS, features.getOrThrow(FOSSIL_BOULDER),
                RarityFilter.onAverageOnceEvery(6), InSquarePlacement.spread(), rubbleY, BiomeFilter.biome());
    }

    /**
     * The placed features of a Nether ring 4 biome, per decoration step (one list for all three, so the feature order is
     * the same in every biome).
     */
    public static List<ResourceKey<PlacedFeature>> features(ResourceKey<Biome> biome, GenerationStep.Decoration step) {
        return switch (step) {
            case LOCAL_MODIFICATIONS -> biome == ModBiomes.BONE_DUNES ? List.of(DUNE_BONES, DUNE_BOULDERS) : List.of();
            case UNDERGROUND_DECORATION -> List.of(GEODE_LININGS, PILLAR_CLADDINGS);
            default -> List.of();
        };
    }

    private static WeightedStateProvider weighted(SimpleWeightedRandomList.Builder<BlockState> builder) {
        return new WeightedStateProvider(builder.build());
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configuredKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, CorruptedAdventure.id("nether_ring_4/" + name));
    }

    private static ResourceKey<PlacedFeature> placedKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, CorruptedAdventure.id("nether_ring_4/" + name));
    }
}
