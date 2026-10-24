package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModTags;
import nl.timtoys.corruptedadventure.ring.Ring;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static nl.timtoys.corruptedadventure.worldgen.feature.Ring5Placements.configuredKey;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring5Placements.placedKey;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring5Placements.register;

/**
 * The ores of ring 5 (95, V3-A, V11-A): nullite where the risk is, in rock that faces the void in the outer band (scar
 * walls, floe edges, the undersides of floes and floating chunks; {@link Ring5VoidFacePlacement}) and a little deep in
 * the inner band; glitch blocks in clusters everywhere in the ring (their shards are the glitches' harvest); vanilla's ores
 * nearly gone ({@link #FACTORS}: ×0,05 common, ×0,03 middle, no diamond), also in corrupted stone. Deep in the inner band
 * the rock is deepslate: nullite there is {@code nullite_ore} for now (placeholder; a deepslate variant is backlog).
 */
public final class Ring5OreFeatures {
    /** Vanilla's ores in ring 5 (contract §4, V11-A). */
    public static final Map<ResourceKey<PlacedFeature>, Double> FACTORS = RingVanillaOres.factors(0.05, 0.03, 0.0);

    static final ResourceKey<ConfiguredFeature<?, ?>> NULLITE_FACE = configuredKey("nullite_face");
    static final ResourceKey<ConfiguredFeature<?, ?>> NULLITE_DEEP = configuredKey("nullite_deep");
    static final ResourceKey<ConfiguredFeature<?, ?>> GLITCH_CLUSTER = configuredKey("glitch_cluster");

    public static final ResourceKey<PlacedFeature> NULLITE_FACES = placedKey("nullite_faces");
    public static final ResourceKey<PlacedFeature> NULLITE_DEEPS = placedKey("nullite_deep");
    public static final ResourceKey<PlacedFeature> GLITCH_CLUSTERS = placedKey("glitch_clusters");

    /** Vein sizes (vanilla's ore size: about this many blocks at most) and attempts per chunk. */
    static final int FACE_SIZE = 6;
    static final int FACE_COUNT = 4;
    static final int DEEP_SIZE = 4;
    static final int DEEP_COUNT = 1;
    static final int DEEP_MIN_Y = -64;
    static final int DEEP_MAX_Y = -16;
    static final int GLITCH_SIZE = 7;
    static final int GLITCH_COUNT = 6;
    static final int GLITCH_MIN_Y = -64;
    static final int GLITCH_MAX_Y = 160;
    /** The vanilla ores ring 5 places ({@link #FACTORS}: no diamond). */
    static final List<TagKey<Block>> VANILLA_ORES = List.of(BlockTags.COAL_ORES, BlockTags.IRON_ORES, BlockTags.COPPER_ORES,
            BlockTags.GOLD_ORES, BlockTags.REDSTONE_ORES, BlockTags.LAPIS_ORES);

    private Ring5OreFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        OreConfiguration.TargetBlockState inStone = OreConfiguration.target(new BlockMatchTest(ModBlocks.CORRUPTED_STONE.get()),
                ModBlocks.NULLITE_ORE.get().defaultBlockState());
        OreConfiguration.TargetBlockState inVoidstone = OreConfiguration.target(new BlockMatchTest(ModBlocks.VOIDSTONE.get()),
                ModBlocks.VOIDSTONE_NULLITE_ORE.get().defaultBlockState());
        OreConfiguration.TargetBlockState inDeepslate = OreConfiguration.target(new BlockMatchTest(Blocks.DEEPSLATE),
                ModBlocks.NULLITE_ORE.get().defaultBlockState());
        BlockState nullite = ModBlocks.NULLITE_ORE.get().defaultBlockState();
        BlockState glitch = ModBlocks.GLITCH_BLOCK.get().defaultBlockState();
        // Veins of one chunk cross those of the next in either order: glitch wins over nullite and vanilla's ores, nullite
        // over vanilla's, so where they meet the block is the same whatever generated first. (Not nullite over glitch: the
        // castle rubble's glitch blocks are a feature, which a neighbour's vein would reach before or after.)
        List<OreConfiguration.TargetBlockState> overGlitch = overVanilla(glitch);
        overGlitch.add(OreConfiguration.target(new BlockMatchTest(ModBlocks.NULLITE_ORE.get()), glitch));
        overGlitch.add(OreConfiguration.target(new BlockMatchTest(ModBlocks.VOIDSTONE_NULLITE_ORE.get()), glitch));
        FeatureUtils.register(context, NULLITE_FACE, Feature.ORE, new OreConfiguration(with(overVanilla(nullite), inStone, inVoidstone),
                FACE_SIZE));
        FeatureUtils.register(context, NULLITE_DEEP, Feature.ORE, new OreConfiguration(with(overVanilla(nullite), inStone, inDeepslate),
                DEEP_SIZE));
        FeatureUtils.register(context, GLITCH_CLUSTER, Feature.ORE, new OreConfiguration(with(overGlitch,
                OreConfiguration.target(new TagMatchTest(ModTags.Blocks.RING_5_STONES), glitch),
                OreConfiguration.target(new BlockMatchTest(Blocks.DEEPSLATE), glitch)), GLITCH_SIZE));
    }

    /** Targets that turn every vanilla ore of ring 5 (stone and deepslate variants) into {@code state}. */
    private static List<OreConfiguration.TargetBlockState> overVanilla(BlockState state) {
        List<OreConfiguration.TargetBlockState> targets = new ArrayList<>();
        for (TagKey<Block> ores : VANILLA_ORES) {
            targets.add(OreConfiguration.target(new TagMatchTest(ores), state));
        }
        return targets;
    }

    /** {@code first}, then {@code extra}, as one list (the first matching target of a block wins). */
    private static List<OreConfiguration.TargetBlockState> with(List<OreConfiguration.TargetBlockState> extra,
                                                                OreConfiguration.TargetBlockState... first) {
        List<OreConfiguration.TargetBlockState> targets = new ArrayList<>(List.of(first));
        targets.addAll(extra);
        return List.copyOf(targets);
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        register(context, NULLITE_FACES, NULLITE_FACE, CountPlacement.of(FACE_COUNT), InSquarePlacement.spread(),
                Ring5VoidFacePlacement.INSTANCE);
        register(context, NULLITE_DEEPS, NULLITE_DEEP, CountPlacement.of(DEEP_COUNT), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(DEEP_MIN_Y), VerticalAnchor.absolute(DEEP_MAX_Y)));
        register(context, GLITCH_CLUSTERS, GLITCH_CLUSTER, CountPlacement.of(GLITCH_COUNT), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(GLITCH_MIN_Y), VerticalAnchor.absolute(GLITCH_MAX_Y)));
        RingVanillaOres.placed(context, Ring.RING_5, FACTORS);
    }
}
