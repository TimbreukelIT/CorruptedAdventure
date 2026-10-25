package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModFeatures;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Datagen bootstrap of the ring 3 landmarks (G2, decision 3 = A; {@link VolcanicLandmarks}): one feature
 * {@code ring_3/landmarks}, placed once per chunk without a biome filter in the seven biomes that have a landmark (a
 * landmark is a function of its cell, and every chunk it covers writes its part).
 */
public final class VolcanicLandmarkFeatures {
    static final ResourceKey<ConfiguredFeature<?, ?>> LANDMARKS_CONFIGURED = ResourceKey.create(Registries.CONFIGURED_FEATURE,
            CorruptedAdventure.id("ring_3/landmarks"));
    public static final ResourceKey<PlacedFeature> LANDMARKS = ResourceKey.create(Registries.PLACED_FEATURE,
            CorruptedAdventure.id("ring_3/landmarks"));
    /** The biomes with a landmark of their own. */
    public static final Set<ResourceKey<Biome>> BIOMES = EnumSet.allOf(VolcanicLandmarks.Kind.class).stream()
            .map(VolcanicLandmarks.Kind::biome).collect(Collectors.toUnmodifiableSet());
    /** How far the ember woods' trees and fallen logs keep outside an ember giant's reach. */
    static final int TREE_CLEARANCE = 2;

    private VolcanicLandmarkFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        FeatureUtils.register(context, LANDMARKS_CONFIGURED, ModFeatures.VOLCANIC_LANDMARK.get(), NoneFeatureConfiguration.INSTANCE);
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        PlacementUtils.register(context, LANDMARKS, features.getOrThrow(LANDMARKS_CONFIGURED));
    }
}
