package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModFeatures;

/**
 * The lining of the fire crystal geodes (change 41b, {@link FireGeodeFeature}): fire crystal, blackstone and smooth
 * basalt from the cavity out, buds and clusters on about a third of the inner wall. Starting values, tuned at test
 * stop 4b.
 */
public final class Ring3GeodeFeatures {
    static final ResourceKey<ConfiguredFeature<?, ?>> FIRE_GEODE = ResourceKey.create(Registries.CONFIGURED_FEATURE,
            CorruptedAdventure.id("ring_3/fire_geode"));
    /** UNDERGROUND_DECORATION in the ring 3 core; once per chunk, no biome filter: the geode of the cell is the terrain's. */
    public static final ResourceKey<PlacedFeature> FIRE_GEODES = ResourceKey.create(Registries.PLACED_FEATURE,
            CorruptedAdventure.id("ring_3/fire_geodes"));

    private Ring3GeodeFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        FeatureUtils.register(context, FIRE_GEODE, ModFeatures.FIRE_GEODE.get(), new FireGeodeConfiguration(
                ModBlocks.FIRE_CRYSTAL_BLOCK.get().defaultBlockState(), Blocks.BLACKSTONE.defaultBlockState(),
                Blocks.SMOOTH_BASALT.defaultBlockState(), 0.35F));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        PlacementUtils.register(context, FIRE_GEODES, context.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(FIRE_GEODE));
    }
}
