package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModFeatures;

import java.util.List;

/**
 * Datagen bootstraps (configured + placed features) of Nether ring 3 (53b, 54). Owned by stream W-nether; keys
 * {@code corruptedadventure:nether_ring_3/<name>} (contract §4), kept here instead of the shared registry classes.
 * <p>
 * The features follow the Nether determinism rule of {@link GrowthPriority} (change 35): they read terrain only (air and
 * the growths are see-through), write growths only (rimeshroom, ice stalactite) and never move aside; origins from
 * {@code count} → {@code in_square} → absolute Y → {@code biome}. Everything that is terrain — the Nether-ice plates,
 * the warm rings around the cracks, the frozen lavafalls, the ice falls and the ice bridges — is surface rule or density
 * function, so no feature ever writes terrain that a feature of a neighbouring chunk could read. The one exception, the
 * blue-ice veins of the glacier (54), swaps one solid ice for another: no scan sees the difference, overlapping veins
 * write the same block, and with no air-exposure discard a vein never reads the air the growths fill.
 */
public final class NetherRing3Features {
    /**
     * Region noise ({@code ring_3/nether/region}) from which the rime forest's halls start; the frozen lava sea lies
     * between {@link #RIFTS_THRESHOLD} and this, the glacier rifts below. The terrain's region weights turn over here, and
     * so do the surface rule and the preset's {@code multi_noise}; {@code NetherRing3JsonTest} compares them.
     */
    public static final double RIME_THRESHOLD = 0.18;
    /** Region noise below which the glacier rifts lie. */
    public static final double RIFTS_THRESHOLD = -0.18;

    /** Frozen giant rimeshrooms (G2-WN1 = A): 12–22 high, no lamp, ice stalactites under the cap; need 29 blocks of room. */
    public static final GiantFungusShape.FreeSize RIMESHROOM_SIZE = GiantFungusShape.FreeSize.of(12, 22, 4, 6);
    /** The small rimeshrooms on the forest floor (thin stem, room 10, so they also grow in low halls). */
    public static final GiantFungusShape.FreeSize SMALL_RIMESHROOM_SIZE = GiantFungusShape.FreeSize.of(3, 6, 2, 3);

    public static final ResourceKey<ConfiguredFeature<?, ?>> RIMESHROOM = configuredKey("rimeshroom");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SMALL_RIMESHROOM = configuredKey("small_rimeshroom");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ICE_STALACTITE = configuredKey("ice_stalactite");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BLUE_ICE_VEIN = configuredKey("blue_ice_vein");

    public static final ResourceKey<PlacedFeature> FROZEN_LAVA_SEA_STALACTITES = placedKey("frozen_lava_sea_stalactites");
    public static final ResourceKey<PlacedFeature> RIME_FOREST_RIMESHROOMS = placedKey("rime_forest_rimeshrooms");
    public static final ResourceKey<PlacedFeature> RIME_FOREST_SMALL_RIMESHROOMS = placedKey("rime_forest_small_rimeshrooms");
    public static final ResourceKey<PlacedFeature> RIME_FOREST_STALACTITES = placedKey("rime_forest_stalactites");
    public static final ResourceKey<PlacedFeature> GLACIER_RIFTS_STALACTITES = placedKey("glacier_rifts_stalactites");
    public static final ResourceKey<PlacedFeature> GLACIER_RIFTS_BLUE_ICE_VEINS = placedKey("glacier_rifts_blue_ice_veins");

    /**
     * Every vegetal feature of Nether ring 3 in its one fixed order; each biome takes its own in this order
     * ({@code NetherRing3Biomes}), so no feature order cycle can arise between them.
     */
    public static final List<ResourceKey<PlacedFeature>> VEGETATION = List.of(FROZEN_LAVA_SEA_STALACTITES,
            RIME_FOREST_RIMESHROOMS, RIME_FOREST_SMALL_RIMESHROOMS, RIME_FOREST_STALACTITES, GLACIER_RIFTS_STALACTITES);
    /** The underground decoration of Nether ring 3 in its one order (so far only the glacier's blue-ice veins). */
    public static final List<ResourceKey<PlacedFeature>> ORES = List.of(GLACIER_RIFTS_BLUE_ICE_VEINS);

    private NetherRing3Features() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        BlockState stem = ModBlocks.RIMESHROOM_STEM.get().defaultBlockState();
        BlockState cap = ModBlocks.RIMESHROOM_CAP.get().defaultBlockState();
        BlockState stalactite = ModBlocks.ICE_STALACTITE.get().defaultBlockState();
        // No lamp: the lamp is the cap itself (cold and dark). Free-standing only: the halls get a wood, not pillars.
        FeatureUtils.register(context, RIMESHROOM, ModFeatures.GIANT_FUNGUS.get(),
                new GiantFungusConfiguration(stem, cap, cap, stalactite, false, RIMESHROOM_SIZE));
        FeatureUtils.register(context, SMALL_RIMESHROOM, ModFeatures.GIANT_FUNGUS.get(),
                new GiantFungusConfiguration(stem, cap, cap, stalactite, false, SMALL_RIMESHROOM_SIZE));
        FeatureUtils.register(context, ICE_STALACTITE, ModFeatures.SLIME_STALACTITE.get(),
                new SlimeStalactiteConfiguration(stalactite, UniformInt.of(1, 6)));
        FeatureUtils.register(context, BLUE_ICE_VEIN, Feature.ORE, new OreConfiguration(
                new BlockMatchTest(ModBlocks.PACKED_NETHER_ICE.get()), Blocks.BLUE_ICE.defaultBlockState(), 24, 0.0F));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        Holder<ConfiguredFeature<?, ?>> rimeshroom = features.getOrThrow(RIMESHROOM);
        Holder<ConfiguredFeature<?, ?>> smallRimeshroom = features.getOrThrow(SMALL_RIMESHROOM);
        Holder<ConfiguredFeature<?, ?>> stalactite = features.getOrThrow(ICE_STALACTITE);

        // The sea: icicles from the ceiling (Y ~108), the pillar heads and the frozen drapes. The stalactite scans up
        // at most 40 blocks, so only origins high in the hall (or under a pillar head) find a ceiling.
        PlacementUtils.register(context, FROZEN_LAVA_SEA_STALACTITES, stalactite,
                CountPlacement.of(8), InSquarePlacement.spread(), uniformY(64, 106), BiomeFilter.biome());

        // The rime forest: the frozen wood from a random Y in the hall (the scan finds the floor), small rimeshrooms
        // between them, icicles from the hall ceilings and walls.
        PlacementUtils.register(context, RIME_FOREST_RIMESHROOMS, rimeshroom,
                CountPlacement.of(3), InSquarePlacement.spread(), uniformY(30, 90), BiomeFilter.biome());
        PlacementUtils.register(context, RIME_FOREST_SMALL_RIMESHROOMS, smallRimeshroom,
                CountPlacement.of(4), InSquarePlacement.spread(), uniformY(28, 70), BiomeFilter.biome());
        PlacementUtils.register(context, RIME_FOREST_STALACTITES, stalactite,
                CountPlacement.of(5), InSquarePlacement.spread(), uniformY(50, 106), BiomeFilter.biome());

        // The glacier rifts (54): icicles under the tunnel roofs, the ice bridges and the overhangs of the rift walls;
        // blue-ice veins in the packed Nether ice of the mass.
        PlacementUtils.register(context, GLACIER_RIFTS_STALACTITES, stalactite,
                CountPlacement.of(8), InSquarePlacement.spread(), uniformY(36, 100), BiomeFilter.biome());
        PlacementUtils.register(context, GLACIER_RIFTS_BLUE_ICE_VEINS, features.getOrThrow(BLUE_ICE_VEIN),
                CountPlacement.of(6), InSquarePlacement.spread(), uniformY(30, 100), BiomeFilter.biome());
    }

    private static PlacementModifier uniformY(int min, int max) {
        return HeightRangePlacement.uniform(VerticalAnchor.absolute(min), VerticalAnchor.absolute(max));
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configuredKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, CorruptedAdventure.id("nether_ring_3/" + name));
    }

    private static ResourceKey<PlacedFeature> placedKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, CorruptedAdventure.id("nether_ring_3/" + name));
    }
}
