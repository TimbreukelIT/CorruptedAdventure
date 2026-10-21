package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModFeatures;
import nl.timtoys.corruptedadventure.ring.Ring;

import java.util.List;
import java.util.Map;

import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.FLOOR;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.configuredKey;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.only;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.placedKey;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.register;

/**
 * The ores of ring 4 (package 77; Q14 = A, contract ring 4 §3–§4): vanilla's tapered further than in ring 3 (coal,
 * iron, copper ×0.2; gold, redstone, lapis ×0.15; diamond ×0.1), tidesilver and jade with a variant per host rock
 * (stone, tidestone, deepslate; jade only deep), and aquamarine geodes half open in the floor under deep water (the
 * lagoon's troughs and the fjords). The ring-wide ores come through the biome modifier {@code add_ring_4_ores}
 * ({@code Ring4BiomeModifiers}); the geodes are the two biomes' own.
 * <p>
 * The tunnel-bound parts (toxic crystal in the dry blighted tunnels and gas chambers, tidesilver in the tunnel walls,
 * jade deep in the flooded tunnels, aquamarine in flooded tunnels, the creatures' tracks) line the tunnel net of
 * package 73 ({@link TunnelLiningFeature}).
 */
public final class Ring4OreFeatures {
    static final ResourceKey<ConfiguredFeature<?, ?>> TIDESILVER_ORE_CONFIGURED = configuredKey("tidesilver_ore");
    static final ResourceKey<ConfiguredFeature<?, ?>> JADE_ORE_CONFIGURED = configuredKey("jade_ore");
    static final ResourceKey<ConfiguredFeature<?, ?>> AQUAMARINE_GEODE = configuredKey("aquamarine_geode");
    static final ResourceKey<ConfiguredFeature<?, ?>> TUNNEL_LINING = configuredKey("tunnel_lining");

    public static final ResourceKey<PlacedFeature> TIDESILVER_ORE = placedKey("tidesilver_ore");
    public static final ResourceKey<PlacedFeature> JADE_ORE = placedKey("jade_ore");
    /** LOCAL_MODIFICATIONS of {@code crystal_lagoon} and {@code mist_fjords}. */
    public static final ResourceKey<PlacedFeature> DEEP_WATER_AQUAMARINE_GEODES = placedKey("deep_water_aquamarine_geodes");
    /** UNDERGROUND_DECORATION of every ground biome: crystals, ores and tracks in the tunnel net (73). */
    public static final ResourceKey<PlacedFeature> TUNNEL_LININGS = placedKey("tunnel_linings");

    /** Vanilla's ores in ring 4 ({@link RingVanillaOres}; contract §4): ×0.2, ×0.15, ×0.1. */
    public static final Map<ResourceKey<PlacedFeature>, Double> RING_4_FACTORS = RingVanillaOres.factors(0.2, 0.15, 0.1);

    /** The ring 4 ores of the biome modifier, after vanilla's tapered ones, in this order. */
    public static final List<ResourceKey<PlacedFeature>> OWN = List.of(TIDESILVER_ORE, JADE_ORE);

    // Starting values, tuned at test stop 5b.
    static final int TIDESILVER_SIZE = 8;
    static final int TIDESILVER_COUNT = 5;
    static final int TIDESILVER_MIN_Y = -48;
    static final int TIDESILVER_MAX_Y = 48;
    static final int JADE_SIZE = 6;
    static final int JADE_COUNT = 3;
    static final int JADE_MIN_Y = -64;
    static final int JADE_MAX_Y = 0;
    /**
     * Backlog r. 420: a vanilla ore blob reaches at most 5 blocks sideways and 4 up and down from its origin (coal, size
     * 17, the largest of {@link RingVanillaOres#ORES}); with one more no blob touches a floating island, so the dressing of
     * the islands' undersides never depends on whether a neighbouring chunk's ore came first. The own ores of ring 4 lie
     * deep (at most Y 48) and need no filter.
     */
    static final int ORE_ISLAND_CLEARANCE = 6;
    /** Chance 1 in ring 4, 0 beyond: in {@code sundered_reach} the own ores stay out of the ring 5 half. */
    private static final RingChanceFilter IN_RING_4 = RingChanceFilter.progress(Ring.RING_4, 1.0, 1.0, 0.0, 1.0);
    /** A geode lies under at least this much water. */
    static final int GEODE_WATER = 8;

    private Ring4OreFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        RuleTest stone = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        // Plain tidestone only: not the tunnels' claw marks (in ring_4_stones), which a later vein would turn to ore.
        RuleTest tidestone = new BlockMatchTest(ModBlocks.TIDESTONE.get());
        RuleTest deepslate = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
        FeatureUtils.register(context, TIDESILVER_ORE_CONFIGURED, Feature.ORE, new OreConfiguration(List.of(
                target(stone, ModBlocks.TIDESILVER_ORE.get()),
                target(tidestone, ModBlocks.TIDESTONE_TIDESILVER_ORE.get()),
                target(deepslate, ModBlocks.DEEPSLATE_TIDESILVER_ORE.get())), TIDESILVER_SIZE, 0.0F));
        FeatureUtils.register(context, JADE_ORE_CONFIGURED, Feature.ORE, new OreConfiguration(List.of(
                target(stone, ModBlocks.JADE_ORE.get()),
                target(tidestone, ModBlocks.TIDESTONE_JADE_ORE.get()),
                target(deepslate, ModBlocks.DEEPSLATE_JADE_ORE.get())), JADE_SIZE, 0.0F));
        FeatureUtils.register(context, TUNNEL_LINING, ModFeatures.TUNNEL_LINING.get(),
                net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE);
        FeatureUtils.register(context, AQUAMARINE_GEODE, ModFeatures.SUNKEN_GEODE.get(), new SunkenGeodeConfiguration(
                ModBlocks.AQUAMARINE_BLOCK.get().defaultBlockState(), ModBlocks.BUDDING_AQUAMARINE.get().defaultBlockState(),
                Ring4MythicFeatures.weighted(ModBlocks.SMALL_AQUAMARINE_BUD.get().defaultBlockState(), 2,
                        ModBlocks.MEDIUM_AQUAMARINE_BUD.get().defaultBlockState(), 2, ModBlocks.LARGE_AQUAMARINE_BUD.get().defaultBlockState(), 2,
                        ModBlocks.AQUAMARINE_CLUSTER.get().defaultBlockState(), 1),
                Blocks.CALCITE.defaultBlockState(), ModBlocks.TIDESTONE.get().defaultBlockState(), UniformInt.of(2, 3), 0.1F, 0.45F));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        RingVanillaOres.placed(context, Ring.RING_4, RING_4_FACTORS, Ring4Filter.builder().clearOfIslands(ORE_ISLAND_CLEARANCE).build());
        PlacementUtils.register(context, TIDESILVER_ORE, features.getOrThrow(TIDESILVER_ORE_CONFIGURED), List.of(
                CountPlacement.of(TIDESILVER_COUNT), InSquarePlacement.spread(),
                HeightRangePlacement.triangle(VerticalAnchor.absolute(TIDESILVER_MIN_Y), VerticalAnchor.absolute(TIDESILVER_MAX_Y)),
                IN_RING_4, BiomeFilter.biome()));
        PlacementUtils.register(context, JADE_ORE, features.getOrThrow(JADE_ORE_CONFIGURED), List.of(
                CountPlacement.of(JADE_COUNT), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(JADE_MIN_Y), VerticalAnchor.absolute(JADE_MAX_Y)),
                IN_RING_4, BiomeFilter.biome()));
        Ring4Placements.perChunk(context, TUNNEL_LININGS, TUNNEL_LINING);
        // One try in a few chunks, kept whole in its chunk (a geode reaches 6 blocks).
        register(context, DEEP_WATER_AQUAMARINE_GEODES, AQUAMARINE_GEODE, JitteredGridPlacement.of(16, 6, 0.12F), FLOOR,
                only(Ring4Placements.waterAbove(GEODE_WATER)));
    }

    private static OreConfiguration.TargetBlockState target(RuleTest host, Block ore) {
        return OreConfiguration.target(host, ore.defaultBlockState());
    }
}
