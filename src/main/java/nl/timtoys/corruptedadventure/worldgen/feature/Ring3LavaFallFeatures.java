package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModFeatures;
import nl.timtoys.corruptedadventure.worldgen.biome.Ring3Biomes;

/**
 * The sparse lava falls of ring 3 (change 47c, G2-WB3 = A; {@link LavaFallFeature}): about one per 2–4 active craters
 * (their walls run ~150 columns around the lake), plus a few from gorge and abyss walls; at most 2 sources per chunk
 * (decision 16). Starting values, tuned at test stop 4b.
 */
public final class Ring3LavaFallFeatures {
    static final ResourceKey<ConfiguredFeature<?, ?>> LAVA_FALL = ResourceKey.create(Registries.CONFIGURED_FEATURE,
            CorruptedAdventure.id("ring_3/lava_fall"));
    /**
     * LOCAL_MODIFICATIONS, in every biome of the ring 3 core (lava runs through all of them; not the transitions, review
     * W-biomes); once per chunk, no biome filter: the feature checks the biome of the wall and of the fall.
     */
    public static final ResourceKey<PlacedFeature> LAVA_FALLS = ResourceKey.create(Registries.PLACED_FEATURE,
            CorruptedAdventure.id("ring_3/lava_falls"));

    private Ring3LavaFallFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        FeatureUtils.register(context, LAVA_FALL, ModFeatures.LAVA_FALL.get(), new LavaFallConfiguration(1.0F / 400.0F, 10, 2,
                HolderSet.direct(Ring3Biomes.ALL.stream().map(biomes::getOrThrow).toList())));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        PlacementUtils.register(context, LAVA_FALLS, context.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(LAVA_FALL));
    }
}
