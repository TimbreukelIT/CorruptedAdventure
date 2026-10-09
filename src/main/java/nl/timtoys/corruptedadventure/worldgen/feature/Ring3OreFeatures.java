package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.heightproviders.TrapezoidHeight;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RandomOffsetPlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.ring.Ring;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The ores of ring 3 (change 41; G2-WB1 = A): vanilla's tapered further than in ring 2, plus volcanite, sulfur and
 * fire ruby, each with a variant per host rock (stone, deepslate, basalt; sulfur only stone and basalt, never
 * blackstone). All in {@code UNDERGROUND_ORES}; the ring-wide ones come through the biome modifier
 * {@code add_ring_3_ores} ({@code Ring3BiomeModifiers}), the extra sulfur of {@code sulfur_fields} is that biome's own.
 * <p>
 * Every own ore carries a {@link RingChanceFilter} {@code progress} on ring 3 with chance 1: in a ring 3 biome it
 * always passes, in the transition {@code dry_riverbeds} it keeps the ring 3 set in the ring 3 half (backlog r. 123).
 * Besides the placement random, placements read only the pure ring 3 terrain ({@link Ring3Terrain}), so they do not
 * depend on the order in which chunks decorate.
 */
public final class Ring3OreFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> VOLCANITE_ORE_CONFIGURED = configuredKey("volcanite_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SULFUR_ORE_CONFIGURED = configuredKey("sulfur_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FIRE_RUBY_ORE_CONFIGURED = configuredKey("fire_ruby_ore");

    public static final ResourceKey<PlacedFeature> VOLCANITE_ORE = placedKey("volcanite_ore");
    /** Extra volcanite tries that only take where lava lies within {@link #NEAR_LAVA_RADIUS} blocks. */
    public static final ResourceKey<PlacedFeature> VOLCANITE_ORE_NEAR_LAVA = placedKey("volcanite_ore_near_lava");
    public static final ResourceKey<PlacedFeature> SULFUR_ORE = placedKey("sulfur_ore");
    /** The extra sulfur of {@code sulfur_fields} (with {@link #SULFUR_ORE}: about three times the ring-wide amount). */
    public static final ResourceKey<PlacedFeature> SULFUR_ORE_RICH = placedKey("sulfur_ore_rich");
    public static final ResourceKey<PlacedFeature> FIRE_RUBY_ORE = placedKey("fire_ruby_ore");

    /** Vanilla's ores in ring 3 ({@link RingVanillaOres}): coal, iron, copper ×0.35; gold, redstone, lapis ×0.3; diamond ×0.1. */
    public static final Map<ResourceKey<PlacedFeature>, Double> RING_3_FACTORS = RingVanillaOres.factors(0.35, 0.3, 0.1);

    /** The ring 3 ores of the biome modifier, after vanilla's tapered ones, in this order. */
    public static final List<ResourceKey<PlacedFeature>> OWN = List.of(VOLCANITE_ORE, VOLCANITE_ORE_NEAR_LAVA, SULFUR_ORE, FIRE_RUBY_ORE);

    // Starting values, tuned at test stop 4b.
    static final int VOLCANITE_SIZE = 9;
    static final int VOLCANITE_COUNT = 6;
    static final int VOLCANITE_MIN_Y = -48;
    static final int VOLCANITE_MAX_Y = 40;
    static final int VOLCANITE_NEAR_LAVA_COUNT = 10;
    static final int NEAR_LAVA_RADIUS = 4;
    /** Every other column within the radius is asked (25 per try). */
    static final int NEAR_LAVA_STEP = 2;
    static final int SULFUR_SIZE = 7;
    static final int SULFUR_COUNT = 3;
    static final int SULFUR_RICH_COUNT = 6;
    /** Sulfur lies 4–30 blocks below the surface: the terrain spans Y ~70–250, so an absolute band would miss it. */
    static final int SULFUR_MIN_DEPTH = 4;
    static final int SULFUR_MAX_DEPTH = 30;
    static final int FIRE_RUBY_SIZE = 4;
    static final int FIRE_RUBY_MIN_COUNT = 1;
    static final int FIRE_RUBY_MAX_COUNT = 2;
    static final int FIRE_RUBY_MIN_Y = -40;
    static final int FIRE_RUBY_MAX_Y = 30;

    private Ring3OreFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        RuleTest stone = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        RuleTest deepslate = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
        RuleTest basalt = new BlockMatchTest(Blocks.BASALT);
        // Exposed blocks stay: fire ruby in an abyss wall above the lava sea is risk and reward (plan 41).
        FeatureUtils.register(context, VOLCANITE_ORE_CONFIGURED, Feature.ORE, new OreConfiguration(List.of(
                target(stone, ModBlocks.VOLCANITE_ORE.get()),
                target(deepslate, ModBlocks.DEEPSLATE_VOLCANITE_ORE.get()),
                target(basalt, ModBlocks.BASALT_VOLCANITE_ORE.get())), VOLCANITE_SIZE, 0.0F));
        FeatureUtils.register(context, SULFUR_ORE_CONFIGURED, Feature.ORE, new OreConfiguration(List.of(
                target(stone, ModBlocks.SULFUR_ORE.get()),
                target(basalt, ModBlocks.BASALT_SULFUR_ORE.get())), SULFUR_SIZE, 0.0F));
        FeatureUtils.register(context, FIRE_RUBY_ORE_CONFIGURED, Feature.ORE, new OreConfiguration(List.of(
                target(stone, ModBlocks.FIRE_RUBY_ORE.get()),
                target(deepslate, ModBlocks.DEEPSLATE_FIRE_RUBY_ORE.get()),
                target(basalt, ModBlocks.BASALT_FIRE_RUBY_ORE.get())), FIRE_RUBY_SIZE, 0.0F));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        RingVanillaOres.placed(context, Ring.RING_3, RING_3_FACTORS);

        HeightRangePlacement volcaniteBand = HeightRangePlacement.of(TrapezoidHeight.of(
                VerticalAnchor.absolute(VOLCANITE_MIN_Y), VerticalAnchor.absolute(VOLCANITE_MAX_Y)));
        PlacementUtils.register(context, VOLCANITE_ORE, features.getOrThrow(VOLCANITE_ORE_CONFIGURED),
                ore(CountPlacement.of(VOLCANITE_COUNT), volcaniteBand));
        // "Near lava" is the terrain's own lava (a pure decision, never world blocks), checked last: after the cheap
        // filters, so only tries in ring 3 biomes pay for the column lookups.
        List<PlacementModifier> nearLava = new ArrayList<>(ore(CountPlacement.of(VOLCANITE_NEAR_LAVA_COUNT),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(VOLCANITE_MIN_Y), VerticalAnchor.absolute(VOLCANITE_MAX_Y))));
        nearLava.add(TerrainLavaFilter.within(NEAR_LAVA_RADIUS, NEAR_LAVA_STEP));
        PlacementUtils.register(context, VOLCANITE_ORE_NEAR_LAVA, features.getOrThrow(VOLCANITE_ORE_CONFIGURED), List.copyOf(nearLava));
        PlacementUtils.register(context, SULFUR_ORE, features.getOrThrow(SULFUR_ORE_CONFIGURED),
                ore(CountPlacement.of(SULFUR_COUNT), sulfurDepth()));
        PlacementUtils.register(context, SULFUR_ORE_RICH, features.getOrThrow(SULFUR_ORE_CONFIGURED),
                ore(CountPlacement.of(SULFUR_RICH_COUNT), sulfurDepth()));
        PlacementUtils.register(context, FIRE_RUBY_ORE, features.getOrThrow(FIRE_RUBY_ORE_CONFIGURED),
                ore(CountPlacement.of(UniformInt.of(FIRE_RUBY_MIN_COUNT, FIRE_RUBY_MAX_COUNT)),
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(FIRE_RUBY_MIN_Y), VerticalAnchor.absolute(FIRE_RUBY_MAX_Y))));
    }

    /**
     * 4–30 blocks below the terrain's own surface ({@link TerrainHeightPlacement}: never the top of an island, arch,
     * needle or the lava of a river), uniform. Vanilla's vertical offset is limited to ±16 per modifier, so a fixed
     * step of 16 down and a uniform one from there.
     */
    private static PlacementModifier[] sulfurDepth() {
        int step = 16;
        return new PlacementModifier[]{
                TerrainHeightPlacement.INSTANCE,
                RandomOffsetPlacement.vertical(ConstantInt.of(-step)),
                RandomOffsetPlacement.vertical(UniformInt.of(step - SULFUR_MAX_DEPTH, step - SULFUR_MIN_DEPTH))};
    }

    /** {@code count}, spread in the chunk, the height modifiers, the ring 3 chance and the biome filter. */
    private static List<PlacementModifier> ore(PlacementModifier count, PlacementModifier... height) {
        List<PlacementModifier> modifiers = new ArrayList<>();
        modifiers.add(count);
        modifiers.add(InSquarePlacement.spread());
        modifiers.addAll(List.of(height));
        modifiers.add(RingChanceFilter.progress(Ring.RING_3, 1.0, 1.0, 0.0, 1.0));
        modifiers.add(BiomeFilter.biome());
        return List.copyOf(modifiers);
    }

    private static OreConfiguration.TargetBlockState target(RuleTest host, Block ore) {
        return OreConfiguration.target(host, ore.defaultBlockState());
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configuredKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, CorruptedAdventure.id("ring_3/" + name));
    }

    static ResourceKey<PlacedFeature> placedKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, CorruptedAdventure.id("ring_3/" + name));
    }
}
