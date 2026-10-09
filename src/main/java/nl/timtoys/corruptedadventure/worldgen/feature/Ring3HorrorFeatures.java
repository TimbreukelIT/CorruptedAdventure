package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModFeatures;

import java.util.List;

/**
 * The horror of ring 3 (change 49, G2-WB10 = A): casts of ash in human poses, about one group of 1–3 per 3–4 chunks on
 * the ash plain and rarely in the ember woods and titan fields, glowing at night in their cracks; and trails of
 * footprints in the ash, about one per 20 chunks, that stop abruptly (at a cast, heading for a rim, or nowhere).
 * Amounts are starting values, tuned at test stop 4b.
 * <p>
 * Determinism as in {@link Ring3DressingFeatures}: a cast group lies in its own chunk; a trail is a pure function of
 * seed and cell ({@link FootprintPath}) and every chunk writes its own prints.
 */
public final class Ring3HorrorFeatures {
    // Configured.
    static final ResourceKey<ConfiguredFeature<?, ?>> ASH_CAST = configured("ash_cast");
    static final ResourceKey<ConfiguredFeature<?, ?>> ASH_FOOTPRINTS = configured("ash_footprints");

    // Placed, in the global order of their step (Ring3Features.ORDER).
    public static final ResourceKey<PlacedFeature> ASHEN_WASTES_ASH_CASTS = placed("ashen_wastes_ash_casts");
    public static final ResourceKey<PlacedFeature> RARE_ASH_CASTS = placed("rare_ash_casts");
    /** After the ash drifts: a print dents the drift. */
    public static final ResourceKey<PlacedFeature> ASH_FOOTPRINT_TRAILS = placed("ash_footprint_trails");

    /** The grid of the trails (blocks): with a chance of 0.9 about one trail per 20 chunks. */
    static final int TRAIL_CELL = 72;
    /** The others of a group stand up to this far from the first. */
    private static final int CAST_SPREAD = 4;

    private static final PlacementModifier SURFACE = HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG);

    private Ring3HorrorFeatures() {
    }

    static AshCastConfiguration cast() {
        return new AshCastConfiguration(
                List.of(template("ash_cast_crawling"), template("ash_cast_kneeling"), template("ash_cast_reaching"), template("ash_cast_lying"),
                        template("ash_cast_curled")),
                UniformInt.of(1, 3), CAST_SPREAD,
                HolderSet.direct(Block::builtInRegistryHolder, ModBlocks.ASH_BLOCK.get(), ModBlocks.PUMICE.get(), Blocks.SOUL_SOIL, Blocks.GRAVEL),
                HolderSet.direct(Block::builtInRegistryHolder, ModBlocks.ASH_BLOCK.get(), ModBlocks.SMOLDERING_ASH.get()));
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        FeatureUtils.register(context, ASH_CAST, ModFeatures.ASH_CAST.get(), cast());
        FeatureUtils.register(context, ASH_FOOTPRINTS, ModFeatures.ASH_FOOTPRINTS.get(), new AshFootprintsConfiguration(
                TRAIL_CELL, 0.9F, ModBlocks.ASH_BLOCK.get(), ModBlocks.ASH_LAYER.get(), cast()));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        PlacementModifier ground = VolcanicFilter.builder().ground().build();
        // The margin keeps most of a group (spread 4, figures up to 5 long) in the chunk; a figure that does not fit stays out.
        PlacementUtils.register(context, ASHEN_WASTES_ASH_CASTS, features.getOrThrow(ASH_CAST),
                JitteredGridPlacement.of(16, CAST_SPREAD, 0.3F), SURFACE, SlopeFilter.flat(3, 1), ground, BiomeFilter.biome());
        PlacementUtils.register(context, RARE_ASH_CASTS, features.getOrThrow(ASH_CAST),
                JitteredGridPlacement.of(16, CAST_SPREAD, 0.05F), SURFACE, SlopeFilter.flat(3, 1), ground, BiomeFilter.biome());
        // Once per chunk without a biome filter: every chunk writes its part of each trail that crosses it.
        PlacementUtils.register(context, ASH_FOOTPRINT_TRAILS, features.getOrThrow(ASH_FOOTPRINTS));
    }

    private static ResourceLocation template(String name) {
        return CorruptedAdventure.id("ring_3/" + name);
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configured(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, CorruptedAdventure.id("ring_3/" + name));
    }

    private static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, CorruptedAdventure.id("ring_3/" + name));
    }
}
