package nl.timtoys.corruptedadventure.worldgen.feature;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModFeatures;

/**
 * Datagen bootstraps of the ring 2 rock landmarks (G2 ring 2, decision 1): the rock bridge of {@code canyonlands} and
 * the giant hoodoo of {@code hoodoo_fields} ({@link Ring2RockLandmarks}). One placement per chunk, no modifiers: the
 * feature finds the landmarks that reach its chunk itself and writes its part. Both run last in
 * {@code LOCAL_MODIFICATIONS} of their biome, so the vegetation that follows grows on and round them.
 */
public final class Ring2RockLandmarkFeatures {
    static final ResourceKey<ConfiguredFeature<?, ?>> ROCK_BRIDGE = configuredKey("rock_bridge");
    static final ResourceKey<ConfiguredFeature<?, ?>> GIANT_HOODOO = configuredKey("giant_hoodoo");
    public static final ResourceKey<PlacedFeature> CANYONLANDS_ROCK_BRIDGES = placedKey("canyonlands_rock_bridges");
    public static final ResourceKey<PlacedFeature> HOODOO_FIELDS_GIANT_HOODOOS = placedKey("hoodoo_fields_giant_hoodoos");

    private Ring2RockLandmarkFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        FeatureUtils.register(context, ROCK_BRIDGE, ModFeatures.ROCK_BRIDGE.get(), FeatureConfiguration.NONE);
        FeatureUtils.register(context, GIANT_HOODOO, ModFeatures.GIANT_HOODOO.get(), FeatureConfiguration.NONE);
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        var features = context.lookup(Registries.CONFIGURED_FEATURE);
        PlacementUtils.register(context, CANYONLANDS_ROCK_BRIDGES, features.getOrThrow(ROCK_BRIDGE), List.of());
        PlacementUtils.register(context, HOODOO_FIELDS_GIANT_HOODOOS, features.getOrThrow(GIANT_HOODOO), List.of());
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configuredKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, CorruptedAdventure.id("ring_2/" + name));
    }

    private static ResourceKey<PlacedFeature> placedKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, CorruptedAdventure.id("ring_2/" + name));
    }
}
