package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.NoiseProvider;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RandomOffsetPlacement;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModFeatures;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandOutput;

import java.util.List;
import java.util.Optional;

import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4MythicFeatures.weighted;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.ON_GROUND;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.PATCHES_NOISE;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.SURFACE;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.configuredKey;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.only;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.placedKey;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.register;

/**
 * {@code glowroot_swamp} (ring 4 decor rollout group A, 2026-10-19; sparring 2026-10-19): dim, overgrown and glowing.
 * <ul>
 *   <li>the giant mangroves of package 75 carry long strands of hanging moss and glowing roots
 *   ({@link HangingStrandDecorator} in {@code ring_4/swamp_tree});</li>
 *   <li>tangles of mangrove roots over the shallow water ({@link RootTangleFeature}), a moss floor on the land, dense
 *   ferns and, on the dry hummocks, glowcaps; lily pads on the open water;</li>
 *   <li>the eye-catchers: great root arches from bank to bank ({@link RootArchFeature}) and now and then an ancient
 *   mangrove with a 3 × 3 trunk ({@link AncientSwampTreeFeature}).</li>
 * </ul>
 * Everything merges with the trees by their priority or fills open air only. Amounts are starting values for the
 * tuning loop.
 */
public final class GlowrootSwampFeatures {
    static final ResourceKey<ConfiguredFeature<?, ?>> FLOOR = configuredKey("swamp_moss_floor");
    static final ResourceKey<ConfiguredFeature<?, ?>> FERNS = configuredKey("swamp_ferns");
    static final ResourceKey<ConfiguredFeature<?, ?>> MUSHROOMS = configuredKey("swamp_mushrooms");
    static final ResourceKey<ConfiguredFeature<?, ?>> LILY_PADS = configuredKey("swamp_lily_pads");
    static final ResourceKey<ConfiguredFeature<?, ?>> ROOT_TANGLE = configuredKey("swamp_root_tangle");
    static final ResourceKey<ConfiguredFeature<?, ?>> ROOT_ARCH = configuredKey("swamp_root_arch");
    static final ResourceKey<ConfiguredFeature<?, ?>> ANCIENT_TREE = configuredKey("ancient_swamp_tree");

    public static final ResourceKey<PlacedFeature> GLOWROOT_SWAMP_MOSS_FLOOR = placedKey("glowroot_swamp_moss_floor");
    public static final ResourceKey<PlacedFeature> GLOWROOT_SWAMP_ANCIENT_TREES = placedKey("glowroot_swamp_ancient_trees");
    public static final ResourceKey<PlacedFeature> GLOWROOT_SWAMP_ROOT_ARCHES = placedKey("glowroot_swamp_root_arches");
    public static final ResourceKey<PlacedFeature> GLOWROOT_SWAMP_ROOT_TANGLES = placedKey("glowroot_swamp_root_tangles");
    public static final ResourceKey<PlacedFeature> GLOWROOT_SWAMP_LILY_PADS = placedKey("glowroot_swamp_lily_pads");
    public static final ResourceKey<PlacedFeature> GLOWROOT_SWAMP_FERNS = placedKey("glowroot_swamp_ferns");
    public static final ResourceKey<PlacedFeature> GLOWROOT_SWAMP_MUSHROOMS = placedKey("glowroot_swamp_mushrooms");

    /** The fixed seed of the moss floor's block noise (pure by position). */
    private static final long FLOOR_SEED = 0x6_100F_5EEDL;

    private GlowrootSwampFeatures() {
    }

    /**
     * The moss and glowing roots under the giant mangroves (added to {@code ring_4/swamp_tree}): moss from an aerial root
     * under the leaves, a glowing root under the roots.
     */
    static HangingStrandDecorator strands() {
        return new HangingStrandDecorator(Blocks.MANGROVE_ROOTS.defaultBlockState(), ModBlocks.HANGING_SWAMP_MOSS.get().defaultBlockState(),
                0.2F, 4,
                ModBlocks.GLOWING_HANGING_ROOTS.get().defaultBlockState(), 0.3F);
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        // The moss floor: most grass turns to moss, in patches by a fixed noise (pure per position).
        FeatureUtils.register(context, FLOOR, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new NoiseProvider(FLOOR_SEED,
                new NormalNoise.NoiseParameters(-3, 1.0, 1.0), 1.0F, List.of(Blocks.MOSS_BLOCK.defaultBlockState(),
                Blocks.MOSS_BLOCK.defaultBlockState(), Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.MOSS_BLOCK.defaultBlockState(),
                Blocks.MOSS_BLOCK.defaultBlockState(), Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.MOSS_BLOCK.defaultBlockState()))));
        FeatureUtils.register(context, FERNS, ModFeatures.GROUND_PLANT.get(), new SimpleBlockConfiguration(weighted(
                Blocks.FERN.defaultBlockState(), 5, Blocks.LARGE_FERN.defaultBlockState(), 3, Blocks.SHORT_GRASS.defaultBlockState(), 1)));
        // Only glowcaps: a vanilla mushroom survives by the light, which world generation does not know yet, so whether
        // it stayed depended on the load order (det 2026-10-20).
        FeatureUtils.register(context, MUSHROOMS, ModFeatures.GROUND_PLANT.get(), new SimpleBlockConfiguration(
                BlockStateProvider.simple(ModBlocks.GLOWCAP.get())));
        FeatureUtils.register(context, LILY_PADS, ModFeatures.GROUND_PLANT.get(), new SimpleBlockConfiguration(weighted(
                Blocks.LILY_PAD.defaultBlockState(), 4, ModBlocks.LOTUS.get().defaultBlockState(), 1)));
        RootArchConfiguration tangle = new RootArchConfiguration(Optional.empty(), Optional.empty(), Blocks.MANGROVE_ROOTS.defaultBlockState(),
                weighted(Blocks.MOSS_CARPET.defaultBlockState(), 4, ModBlocks.GLOWCAP.get().defaultBlockState(), 1), 0.45F,
                ModBlocks.HANGING_SWAMP_MOSS.get().defaultBlockState(), 0.2F, 2, ModBlocks.GLOWING_HANGING_ROOTS.get().defaultBlockState(), 0.35F);
        FeatureUtils.register(context, ROOT_TANGLE, ModFeatures.ROOT_TANGLE.get(), tangle);
        // The eye-catchers: an arch per cell of 160 at most (where banks face each other over water), an ancient tree per
        // cell of 288.
        FeatureUtils.register(context, ROOT_ARCH, ModFeatures.ROOT_ARCH.get(), new RootArchConfiguration(
                Optional.of(new Landmarks.Grid(160, 0.85F, 16, 20)), Optional.of(ModBiomes.GLOWROOT_SWAMP),
                Blocks.MANGROVE_ROOTS.defaultBlockState(),
                weighted(Blocks.MOSS_CARPET.defaultBlockState(), 5, ModBlocks.GLOWCAP.get().defaultBlockState(), 1), 0.5F,
                ModBlocks.HANGING_SWAMP_MOSS.get().defaultBlockState(), 0.3F, 6, ModBlocks.GLOWING_HANGING_ROOTS.get().defaultBlockState(), 0.3F));
        FeatureUtils.register(context, ANCIENT_TREE, ModFeatures.ANCIENT_SWAMP_TREE.get(), new AncientSwampTreeConfiguration(
                new Landmarks.Grid(288, 0.85F, 8, 24), ModBiomes.GLOWROOT_SWAMP, UniformInt.of(26, 34), UniformInt.of(5, 7),
                Blocks.MANGROVE_LOG.defaultBlockState(), Blocks.MANGROVE_ROOTS.defaultBlockState(),
                Blocks.MUDDY_MANGROVE_ROOTS.defaultBlockState(), Blocks.MANGROVE_LEAVES.defaultBlockState(),
                ModBlocks.HANGING_SWAMP_MOSS.get().defaultBlockState(), ModBlocks.GLOWING_HANGING_ROOTS.get().defaultBlockState(),
                weighted(Blocks.MOSS_CARPET.defaultBlockState(), 3, ModBlocks.GLOWCAP.get().defaultBlockState(), 1),
                Blocks.MANGROVE_PROPAGULE.defaultBlockState().setValue(MangrovePropaguleBlock.HANGING, true)));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        register(context, GLOWROOT_SWAMP_MOSS_FLOOR, FLOOR, JitteredGridPlacement.of(1, 0, 0.8F), SURFACE,
                RandomOffsetPlacement.vertical(ConstantInt.of(-1)), only(BlockPredicate.matchesBlocks(Blocks.GRASS_BLOCK)));
        Ring4Placements.perChunk(context, GLOWROOT_SWAMP_ANCIENT_TREES, ANCIENT_TREE);
        Ring4Placements.perChunk(context, GLOWROOT_SWAMP_ROOT_ARCHES, ROOT_ARCH);
        register(context, GLOWROOT_SWAMP_ROOT_TANGLES, ROOT_TANGLE, JitteredGridPlacement.of(16, 0, 0.3F), SURFACE);
        register(context, GLOWROOT_SWAMP_LILY_PADS, LILY_PADS, JitteredGridPlacement.of(2, 0, 0.12F, ClusterDensity.of(
                PATCHES_NOISE, -0.1, 0.3).withSalt(8)), SURFACE, only(LotusShallowsFeatures.WATER_BELOW));
        register(context, GLOWROOT_SWAMP_FERNS, FERNS, JitteredGridPlacement.of(2, 0, 0.45F), SURFACE, only(ON_GROUND),
                Ring4Filter.builder().ground().build());
        // Glowcaps on the dry hummocks (ground at least a block over the water).
        register(context, GLOWROOT_SWAMP_MUSHROOMS, MUSHROOMS, JitteredGridPlacement.of(2, 0, 0.3F, ClusterDensity.of(
                        PATCHES_NOISE, 0.0, 0.35).withSalt(9).withFloor(0.2)), SURFACE, only(ON_GROUND),
                Ring4Filter.builder().ground().above(IslandOutput.Output.SURFACE, 63.5).build());
    }
}
