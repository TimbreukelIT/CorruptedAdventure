package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModFeatures;

import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4MythicFeatures.weighted;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.configuredKey;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.perChunk;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.placedKey;

/**
 * {@code sundered_reach} (ring 4 decor D, 2026-10-19; the rollout sparring's group D): only its landmark this round, the
 * split rock on every land bridge ({@link SplitRockFeature}), where the ground has fully broken up: dark deepslate and
 * tuff flecked with ring 4's tidestone, cobbled faces in the split, a mossy foot. No lore (phase 5); the rest of the
 * zone's dressing is not part of this round.
 */
public final class SunderedReachFeatures {
    static final ResourceKey<ConfiguredFeature<?, ?>> SPLIT_ROCK = configuredKey("sundered_split_rock");

    public static final ResourceKey<PlacedFeature> SUNDERED_REACH_SPLIT_ROCKS = placedKey("sundered_reach_split_rocks");

    /** Where along the zone the rock stands: past its full break-up (0.55), well before ring 5. */
    static final float POSITION = 0.7F;

    private SunderedReachFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        FeatureUtils.register(context, SPLIT_ROCK, ModFeatures.SPLIT_ROCK.get(), new SplitRockConfiguration(
                weighted(Blocks.DEEPSLATE.defaultBlockState(), 6, Blocks.TUFF.defaultBlockState(), 2,
                        ModBlocks.TIDESTONE.get().defaultBlockState(), 2, Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 1),
                weighted(Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 3, Blocks.DEEPSLATE.defaultBlockState(), 2,
                        Blocks.TUFF.defaultBlockState(), 1),
                weighted(Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 2, ModBlocks.MOSSY_TIDESTONE.get().defaultBlockState(), 2,
                        Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 2, Blocks.MOSS_BLOCK.defaultBlockState(), 1),
                UniformInt.of(24, 34), UniformFloat.of(0.7F, 0.9F), UniformFloat.of(4.0F, 5.5F), UniformFloat.of(1.5F, 3.5F),
                POSITION));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        // Once per chunk: the feature finds the rocks that reach into the chunk itself (pure per bridge).
        perChunk(context, SUNDERED_REACH_SPLIT_ROCKS, SPLIT_ROCK);
    }
}
