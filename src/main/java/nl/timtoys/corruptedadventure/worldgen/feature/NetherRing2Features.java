package nl.timtoys.corruptedadventure.worldgen.feature;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.OrePlacements;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.block.state.BlockState;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures;
import nl.timtoys.corruptedadventure.registry.ModFeatures;
import nl.timtoys.corruptedadventure.registry.ModPlacedFeatures;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Datagen bootstraps (configured + placed features) of Nether ring 2 (change 35): the giant fungi, slime stalactites,
 * glow mould, spore sprouts, the dew geode lining and the ore copies. Every placement follows the determinism rule of
 * {@link GrowthPriority}: no {@code count_on_every_layer}, {@code random_patch}, {@code multiface_growth}, {@code block_column},
 * vanilla {@code geode} or {@code huge_fungus}; origins from {@code count}/{@code rarity_filter} → {@code in_square} (or
 * the jittered grid) → absolute Y → {@code biome}; the features scan for floor and ceiling themselves. All Y anchors are
 * absolute (the Nether's {@code below_top} lies above the roof).
 */
public final class NetherRing2Features {
    public static final ResourceKey<NormalNoise.NoiseParameters> REGION_NOISE = noise("nether_ring_2_region");
    public static final ResourceKey<NormalNoise.NoiseParameters> GEODE_CELLS = noise("nether_ring_2_geode_cells");
    static final ResourceKey<NormalNoise.NoiseParameters> SPROUTS_NOISE = noise("nether_ring_2_sprouts");

    /** Region noise value from which the dripshroom forest starts (the spore caves lie below); the same value the preset's {@code multi_noise} uses. */
    public static final double REGION_THRESHOLD = -0.10;

    /**
     * The geometry of the dew geodes, shared by the density function {@code ring_2/nether/dew_geodes} (hand-written JSON,
     * its {@code settings}) and the lining feature; {@code NetherRing2JsonTest} compares them.
     */
    public static final DewGeodeShape.Settings GEODE_SETTINGS = new DewGeodeShape.Settings(32, REGION_THRESHOLD, 0.9, 0.95,
            3, 5, 3, 4, 30, 112, 26, 110, 23, 2);

    /** Vanilla's Nether top: {@code below_top} anchors become {@code absolute(NETHER_TOP − n)}. */
    private static final int NETHER_TOP = 127;

    /** The placed ores in the order of the biomes' lists (vanilla's order: addNetherDefaultOres, then magma). */
    public static final List<ResourceKey<PlacedFeature>> ORES = List.of(ModPlacedFeatures.NETHER_RING_2_ORE_GRAVEL,
            ModPlacedFeatures.NETHER_RING_2_ORE_BLACKSTONE, ModPlacedFeatures.NETHER_RING_2_ORE_GOLD,
            ModPlacedFeatures.NETHER_RING_2_ORE_QUARTZ, ModPlacedFeatures.NETHER_RING_2_ORE_DEBRIS_LARGE,
            ModPlacedFeatures.NETHER_RING_2_ORE_DEBRIS_SMALL, ModPlacedFeatures.NETHER_RING_2_ORE_MAGMA);

    private NetherRing2Features() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        BlockState stalactite = ModBlocks.SLIME_STALACTITE.get().defaultBlockState();
        BlockState stem = ModBlocks.DRIPSHROOM_STEM.get().defaultBlockState();
        BlockState cap = ModBlocks.DRIPSHROOM_CAP.get().defaultBlockState();
        BlockState lamp = ModBlocks.DRIPSHROOM_LAMP.get().defaultBlockState();
        FeatureUtils.register(context, ModConfiguredFeatures.GIANT_FUNGUS, ModFeatures.GIANT_FUNGUS.get(),
                new GiantFungusConfiguration(stem, cap, lamp, stalactite, true));
        FeatureUtils.register(context, ModConfiguredFeatures.GIANT_FUNGUS_FREE, ModFeatures.GIANT_FUNGUS.get(),
                new GiantFungusConfiguration(stem, cap, lamp, stalactite, false));
        FeatureUtils.register(context, ModConfiguredFeatures.SLIME_STALACTITE, ModFeatures.SLIME_STALACTITE.get(),
                new SlimeStalactiteConfiguration(stalactite, UniformInt.of(1, 5)));
        FeatureUtils.register(context, ModConfiguredFeatures.MOLD_PATCH, ModFeatures.MOLD_PATCH.get(),
                new MoldPatchConfiguration(ModBlocks.GLOW_MOLD.get().defaultBlockState(), UniformInt.of(2, 4), 0.3F));
        FeatureUtils.register(context, ModConfiguredFeatures.SPORE_SPROUTS_PATCH, ModFeatures.SPORE_SPROUTS_PATCH.get(),
                new SporeSproutsConfiguration(ModBlocks.SPORE_SPROUTS.get().defaultBlockState(), UniformInt.of(2, 3), 0.5F));
        FeatureUtils.register(context, ModConfiguredFeatures.DEW_GEODE, ModFeatures.DEW_GEODE.get(),
                new DewGeodeConfiguration(GEODE_SETTINGS, REGION_NOISE, GEODE_CELLS, 0.05, 0.25, 0.04));

        // Vanilla's sizes (OreFeatures); the target is soaked netherrack, the rock of the whole ring.
        BlockMatchTest soaked = new BlockMatchTest(ModBlocks.SOAKED_NETHERRACK.get());
        ore(context, ModConfiguredFeatures.NETHER_RING_2_ORE_GRAVEL, soaked, Blocks.GRAVEL.defaultBlockState(), 33, 0.0F);
        ore(context, ModConfiguredFeatures.NETHER_RING_2_ORE_BLACKSTONE, soaked, Blocks.BLACKSTONE.defaultBlockState(), 33, 0.0F);
        ore(context, ModConfiguredFeatures.NETHER_RING_2_ORE_GOLD, soaked, Blocks.NETHER_GOLD_ORE.defaultBlockState(), 10, 0.0F);
        ore(context, ModConfiguredFeatures.NETHER_RING_2_ORE_QUARTZ, soaked, Blocks.NETHER_QUARTZ_ORE.defaultBlockState(), 14, 0.0F);
        ore(context, ModConfiguredFeatures.NETHER_RING_2_ORE_DEBRIS_LARGE, soaked, Blocks.ANCIENT_DEBRIS.defaultBlockState(), 3, 1.0F);
        ore(context, ModConfiguredFeatures.NETHER_RING_2_ORE_DEBRIS_SMALL, soaked, Blocks.ANCIENT_DEBRIS.defaultBlockState(), 2, 1.0F);
        ore(context, ModConfiguredFeatures.NETHER_RING_2_ORE_MAGMA, soaked, Blocks.MAGMA_BLOCK.defaultBlockState(), 33, 0.0F);
    }

    private static void ore(BootstrapContext<ConfiguredFeature<?, ?>> context, ResourceKey<ConfiguredFeature<?, ?>> key,
                            BlockMatchTest target, BlockState state, int size, float discard) {
        FeatureUtils.register(context, key, Feature.ORE, new OreConfiguration(target, state, size, discard));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        Holder<ConfiguredFeature<?, ?>> fungus = features.getOrThrow(ModConfiguredFeatures.GIANT_FUNGUS);
        Holder<ConfiguredFeature<?, ?>> freeFungus = features.getOrThrow(ModConfiguredFeatures.GIANT_FUNGUS_FREE);
        Holder<ConfiguredFeature<?, ?>> stalactite = features.getOrThrow(ModConfiguredFeatures.SLIME_STALACTITE);
        Holder<ConfiguredFeature<?, ?>> mold = features.getOrThrow(ModConfiguredFeatures.MOLD_PATCH);
        Holder<ConfiguredFeature<?, ?>> sprouts = features.getOrThrow(ModConfiguredFeatures.SPORE_SPROUTS_PATCH);

        // The geode lining: once per chunk, origin = the chunk's corner; the feature finds the cell centres itself. The
        // cavities lie ≥ 48 blocks inside ring 2, so the corner (≤ 16 from a centre) is always in one of the two biomes,
        // which both have this feature; the filter is only there because vanilla's validation wants one.
        PlacementUtils.register(context, ModPlacedFeatures.DEW_GEODE, features.getOrThrow(ModConfiguredFeatures.DEW_GEODE),
                CountPlacement.of(1), BiomeFilter.biome());

        // Dripshroom forest. Pillars scan from Y 64 (a hall's mid height), free fungi from a random Y in the hall.
        PlacementUtils.register(context, ModPlacedFeatures.DRIPSHROOM_FOREST_PILLARS, fungus,
                RarityFilter.onAverageOnceEvery(3), InSquarePlacement.spread(), fixedY(64), BiomeFilter.biome());
        PlacementUtils.register(context, ModPlacedFeatures.DRIPSHROOM_FOREST_FUNGI, freeFungus,
                CountPlacement.of(2), InSquarePlacement.spread(), uniformY(30, 90), BiomeFilter.biome());
        PlacementUtils.register(context, ModPlacedFeatures.DRIPSHROOM_FOREST_STALACTITES, stalactite,
                CountPlacement.of(3), InSquarePlacement.spread(), uniformY(40, 100), BiomeFilter.biome());
        PlacementUtils.register(context, ModPlacedFeatures.DRIPSHROOM_FOREST_MOLD, mold,
                RarityFilter.onAverageOnceEvery(2), InSquarePlacement.spread(), uniformY(24, 110), BiomeFilter.biome());
        // Patches of sprouts: one candidate per cell of 8 where the sprout noise is high, a floor found from a random Y.
        PlacementUtils.register(context, ModPlacedFeatures.DRIPSHROOM_FOREST_SPROUTS, sprouts,
                JitteredGridPlacement.of(8, 1, 0.5F), NoiseBandFilter.between(SPROUTS_NOISE, 0.1, Double.MAX_VALUE),
                uniformY(26, 100), BiomeFilter.biome());

        // Spore caves: lower and denser, more mould and sprouts, no pillars.
        PlacementUtils.register(context, ModPlacedFeatures.SPORE_CAVES_FUNGI, freeFungus,
                CountPlacement.of(1), InSquarePlacement.spread(), uniformY(24, 70), BiomeFilter.biome());
        PlacementUtils.register(context, ModPlacedFeatures.SPORE_CAVES_STALACTITES, stalactite,
                CountPlacement.of(6), InSquarePlacement.spread(), uniformY(24, 70), BiomeFilter.biome());
        PlacementUtils.register(context, ModPlacedFeatures.SPORE_CAVES_MOLD, mold,
                CountPlacement.of(4), InSquarePlacement.spread(), uniformY(24, 70), BiomeFilter.biome());
        PlacementUtils.register(context, ModPlacedFeatures.SPORE_CAVES_SPROUTS, sprouts,
                JitteredGridPlacement.of(8, 1, 0.8F), NoiseBandFilter.between(SPROUTS_NOISE, -0.1, Double.MAX_VALUE),
                uniformY(24, 70), BiomeFilter.biome());

        // The ore copies: vanilla's modifiers (read from vanilla, not copied) with the below_top anchors made absolute.
        Map<ResourceKey<PlacedFeature>, PlacedFeature> vanilla = RingVanillaOres.vanillaPlacements(context);
        oreCopy(context, features, vanilla, OrePlacements.ORE_GRAVEL_NETHER, ModConfiguredFeatures.NETHER_RING_2_ORE_GRAVEL, ModPlacedFeatures.NETHER_RING_2_ORE_GRAVEL);
        oreCopy(context, features, vanilla, OrePlacements.ORE_BLACKSTONE, ModConfiguredFeatures.NETHER_RING_2_ORE_BLACKSTONE, ModPlacedFeatures.NETHER_RING_2_ORE_BLACKSTONE);
        oreCopy(context, features, vanilla, OrePlacements.ORE_GOLD_NETHER, ModConfiguredFeatures.NETHER_RING_2_ORE_GOLD, ModPlacedFeatures.NETHER_RING_2_ORE_GOLD);
        oreCopy(context, features, vanilla, OrePlacements.ORE_QUARTZ_NETHER, ModConfiguredFeatures.NETHER_RING_2_ORE_QUARTZ, ModPlacedFeatures.NETHER_RING_2_ORE_QUARTZ);
        oreCopy(context, features, vanilla, OrePlacements.ORE_ANCIENT_DEBRIS_LARGE, ModConfiguredFeatures.NETHER_RING_2_ORE_DEBRIS_LARGE, ModPlacedFeatures.NETHER_RING_2_ORE_DEBRIS_LARGE);
        oreCopy(context, features, vanilla, vanillaKey("ore_debris_small"), ModConfiguredFeatures.NETHER_RING_2_ORE_DEBRIS_SMALL, ModPlacedFeatures.NETHER_RING_2_ORE_DEBRIS_SMALL);
        oreCopy(context, features, vanilla, OrePlacements.ORE_MAGMA, ModConfiguredFeatures.NETHER_RING_2_ORE_MAGMA, ModPlacedFeatures.NETHER_RING_2_ORE_MAGMA);
    }

    private static void oreCopy(BootstrapContext<PlacedFeature> context, HolderGetter<ConfiguredFeature<?, ?>> features,
                                Map<ResourceKey<PlacedFeature>, PlacedFeature> vanilla, ResourceKey<PlacedFeature> vanillaKey,
                                ResourceKey<ConfiguredFeature<?, ?>> configured, ResourceKey<PlacedFeature> key) {
        PlacedFeature original = vanilla.get(vanillaKey);
        if (original == null) {
            throw new IllegalStateException("vanilla no longer registers " + vanillaKey.location());
        }
        context.register(key, new PlacedFeature(features.getOrThrow(configured), absoluteAnchors(original.placement())));
    }

    /** The modifiers with every {@code below_top} anchor turned into the absolute Y it means in vanilla's Nether. */
    static List<PlacementModifier> absoluteAnchors(List<PlacementModifier> modifiers) {
        List<PlacementModifier> result = new ArrayList<>();
        for (PlacementModifier modifier : modifiers) {
            JsonElement json = PlacementModifier.CODEC.encodeStart(JsonOps.INSTANCE, modifier).getOrThrow();
            result.add(PlacementModifier.CODEC.parse(JsonOps.INSTANCE, absolute(json)).getOrThrow());
        }
        return List.copyOf(result);
    }

    private static JsonElement absolute(JsonElement json) {
        if (json.isJsonObject()) {
            JsonObject object = json.getAsJsonObject();
            JsonObject copy = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if (entry.getKey().equals("below_top") && entry.getValue().isJsonPrimitive()) {
                    copy.addProperty("absolute", NETHER_TOP - entry.getValue().getAsInt());
                } else if (entry.getKey().equals("above_bottom") && entry.getValue().isJsonPrimitive()) {
                    copy.addProperty("absolute", entry.getValue().getAsInt());
                } else {
                    copy.add(entry.getKey(), absolute(entry.getValue()));
                }
            }
            return copy;
        }
        if (json.isJsonArray()) {
            com.google.gson.JsonArray copy = new com.google.gson.JsonArray();
            json.getAsJsonArray().forEach(element -> copy.add(absolute(element)));
            return copy;
        }
        return json;
    }

    private static ResourceKey<PlacedFeature> vanillaKey(String path) {
        return ResourceKey.create(Registries.PLACED_FEATURE, net.minecraft.resources.ResourceLocation.withDefaultNamespace(path));
    }

    private static PlacementModifier fixedY(int y) {
        return HeightRangePlacement.of(ConstantHeight.of(VerticalAnchor.absolute(y)));
    }

    private static PlacementModifier uniformY(int min, int max) {
        return HeightRangePlacement.uniform(VerticalAnchor.absolute(min), VerticalAnchor.absolute(max));
    }

    private static ResourceKey<NormalNoise.NoiseParameters> noise(String path) {
        return ResourceKey.create(Registries.NOISE, CorruptedAdventure.id(path));
    }
}
