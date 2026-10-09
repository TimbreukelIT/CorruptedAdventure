package nl.timtoys.corruptedadventure.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.worldgen.feature.BarePatchConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.BarePatchFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.CanyonSpringConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.CanyonSpringFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.DewGeodeConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.DewGeodeFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.GiantFungusConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.GiantFungusFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.MoldPatchConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.MoldPatchFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.SlimeStalactiteConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.SlimeStalactiteFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.SporeSproutsConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.SporeSproutsFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.OutcropConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.OutcropFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.RubbleConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.RubbleFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.SurfaceFossilConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.SurfaceFossilFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.SurfacePoolConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.SurfacePoolFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.UnnaturalTunnelConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.UnnaturalTunnelFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.AshCastConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.AshCastFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.AshDriftConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.AshDriftFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.AshFootprintsConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.AshFootprintsFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.GroundPatchConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.GroundPatchFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.GroundVentConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.GroundVentFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.HornitoConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.HornitoFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.ImpactCraterConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.ImpactCraterFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.TitanRockConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.TitanRockFeature;

/** Feature types with their own code; the configured and placed features come from datagen. */
public final class ModFeatures {
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, CorruptedAdventure.MOD_ID);

    /** {@code corruptedadventure:bare_patch} */
    public static final DeferredHolder<Feature<?>, Feature<BarePatchConfiguration>> BARE_PATCH =
            FEATURES.register("bare_patch", () -> new BarePatchFeature(BarePatchConfiguration.CODEC));
    /** {@code corruptedadventure:unnatural_tunnel} */
    public static final DeferredHolder<Feature<?>, Feature<UnnaturalTunnelConfiguration>> UNNATURAL_TUNNEL =
            FEATURES.register("unnatural_tunnel", () -> new UnnaturalTunnelFeature(UnnaturalTunnelConfiguration.CODEC));
    /** {@code corruptedadventure:rubble} */
    public static final DeferredHolder<Feature<?>, Feature<RubbleConfiguration>> RUBBLE =
            FEATURES.register("rubble", () -> new RubbleFeature(RubbleConfiguration.CODEC));
    /** {@code corruptedadventure:canyon_spring} */
    public static final DeferredHolder<Feature<?>, Feature<CanyonSpringConfiguration>> CANYON_SPRING =
            FEATURES.register("canyon_spring", () -> new CanyonSpringFeature(CanyonSpringConfiguration.CODEC));
    /** {@code corruptedadventure:surface_pool} (change 30b) */
    public static final DeferredHolder<Feature<?>, Feature<SurfacePoolConfiguration>> SURFACE_POOL =
            FEATURES.register("surface_pool", () -> new SurfacePoolFeature(SurfacePoolConfiguration.CODEC));
    /** {@code corruptedadventure:outcrop} (change 30b) */
    public static final DeferredHolder<Feature<?>, Feature<OutcropConfiguration>> OUTCROP =
            FEATURES.register("outcrop", () -> new OutcropFeature(OutcropConfiguration.CODEC));
    /** {@code corruptedadventure:surface_fossil} (change 30c) */
    public static final DeferredHolder<Feature<?>, Feature<SurfaceFossilConfiguration>> SURFACE_FOSSIL =
            FEATURES.register("surface_fossil", () -> new SurfaceFossilFeature(SurfaceFossilConfiguration.CODEC));

    /** {@code corruptedadventure:giant_fungus} (change 35) */
    public static final DeferredHolder<Feature<?>, Feature<GiantFungusConfiguration>> GIANT_FUNGUS =
            FEATURES.register("giant_fungus", () -> new GiantFungusFeature(GiantFungusConfiguration.CODEC));
    /** {@code corruptedadventure:slime_stalactite} (change 35) */
    public static final DeferredHolder<Feature<?>, Feature<SlimeStalactiteConfiguration>> SLIME_STALACTITE =
            FEATURES.register("slime_stalactite", () -> new SlimeStalactiteFeature(SlimeStalactiteConfiguration.CODEC));
    /** {@code corruptedadventure:mold_patch} (change 35) */
    public static final DeferredHolder<Feature<?>, Feature<MoldPatchConfiguration>> MOLD_PATCH =
            FEATURES.register("mold_patch", () -> new MoldPatchFeature(MoldPatchConfiguration.CODEC));
    /** {@code corruptedadventure:spore_sprouts_patch} (change 35) */
    public static final DeferredHolder<Feature<?>, Feature<SporeSproutsConfiguration>> SPORE_SPROUTS_PATCH =
            FEATURES.register("spore_sprouts_patch", () -> new SporeSproutsFeature(SporeSproutsConfiguration.CODEC));
    /** {@code corruptedadventure:dew_geode} (change 35) */
    public static final DeferredHolder<Feature<?>, Feature<DewGeodeConfiguration>> DEW_GEODE =
            FEATURES.register("dew_geode", () -> new DewGeodeFeature(DewGeodeConfiguration.CODEC));

    // ring 3 — W-biomes g2
    /** {@code corruptedadventure:ash_drift} (change 47) */
    public static final DeferredHolder<Feature<?>, Feature<AshDriftConfiguration>> ASH_DRIFT =
            FEATURES.register("ash_drift", () -> new AshDriftFeature(AshDriftConfiguration.CODEC));
    /** {@code corruptedadventure:ground_patch} (change 47) */
    public static final DeferredHolder<Feature<?>, Feature<GroundPatchConfiguration>> GROUND_PATCH =
            FEATURES.register("ground_patch", () -> new GroundPatchFeature(GroundPatchConfiguration.CODEC));
    /** {@code corruptedadventure:ground_vent} (change 47) */
    public static final DeferredHolder<Feature<?>, Feature<GroundVentConfiguration>> GROUND_VENT =
            FEATURES.register("ground_vent", () -> new GroundVentFeature(GroundVentConfiguration.CODEC));
    /** {@code corruptedadventure:hornito} (change 47) */
    public static final DeferredHolder<Feature<?>, Feature<HornitoConfiguration>> HORNITO =
            FEATURES.register("hornito", () -> new HornitoFeature(HornitoConfiguration.CODEC));
    /** {@code corruptedadventure:impact_crater} (change 47) */
    public static final DeferredHolder<Feature<?>, Feature<ImpactCraterConfiguration>> IMPACT_CRATER =
            FEATURES.register("impact_crater", () -> new ImpactCraterFeature(ImpactCraterConfiguration.CODEC));
    /** {@code corruptedadventure:titan_rock} (change 47b) */
    public static final DeferredHolder<Feature<?>, Feature<TitanRockConfiguration>> TITAN_ROCK =
            FEATURES.register("titan_rock", () -> new TitanRockFeature(TitanRockConfiguration.CODEC));
    /** {@code corruptedadventure:ash_cast} (change 49) */
    public static final DeferredHolder<Feature<?>, Feature<AshCastConfiguration>> ASH_CAST =
            FEATURES.register("ash_cast", () -> new AshCastFeature(AshCastConfiguration.CODEC));
    /** {@code corruptedadventure:ash_footprints} (change 49) */
    public static final DeferredHolder<Feature<?>, Feature<AshFootprintsConfiguration>> ASH_FOOTPRINTS =
            FEATURES.register("ash_footprints", () -> new AshFootprintsFeature(AshFootprintsConfiguration.CODEC));

    private ModFeatures() {
    }

    public static void register(IEventBus modBus) {
        FEATURES.register(modBus);
    }
}
