package nl.timtoys.corruptedadventure.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.worldgen.feature.InvertedTreeFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.MemoryFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.MirroredTreesFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.AbandonedCampFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.TorchPatternFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.HerobrineSignFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.MissingChunkFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.BarePatchConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.BlightedLandmarkFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.BoulderConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.BoulderFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.DeadTreeConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.DeadTreeFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.FallenTreeConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.SplitRockConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.SplitRockFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.FallenTreeFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.FungusRingFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.TerraceDressingConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.TerraceDressingFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.GiantMoonpetalConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.GiantMoonpetalFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.IslandDressingConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.IslandDressingFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.SkyRuinConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.SkyRuinFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.AncientSwampTreeConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.AncientSwampTreeFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.CrystalSpireConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.CrystalSpireFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.RootArchConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.RootArchFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.RootTangleFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.ShoalConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.ShoalFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.DuneRubbleFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.GiantGeodeLiningConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.GiantGeodeLiningFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.PillarCladdingConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.PillarCladdingFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.BarePatchFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.CanyonSpringConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.CanyonSpringFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.DewGeodeConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.DewGeodeFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.FireGeodeConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.FireGeodeFeature;
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
import nl.timtoys.corruptedadventure.worldgen.feature.TerracePoolConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.TerracePoolFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.GiantLilyPadFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.IslandGlowConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.IslandGlowFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.IslandUndersideConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.IslandUndersideFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.LocalCoralFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.ReedClumpConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.ReedClumpFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.GasPocketConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.GasPocketFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.SinkholeGasConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.SinkholeGasFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.SuckingMudPatchConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.SuckingMudPatchFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.ToxicPoolConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.ToxicPoolFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.SunkenGeodeConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.SunkenGeodeFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.ToxicPuddleFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.WallPlantConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.WallPlantFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.IslandVinesConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.IslandVinesFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.GroundPlantFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.IsolatedTreeFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.IslandLiftFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.TunnelLiningFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.UnnaturalTunnelConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.UnnaturalTunnelFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.WallDrapeConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.WallDrapeFeature;
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
import nl.timtoys.corruptedadventure.worldgen.feature.HangingClusterConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.HangingClusterFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.HornitoConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.HornitoFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.ImpactCraterConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.ImpactCraterFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.LavaFallConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.LavaFallFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.LedgeStrandConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.LedgeStrandFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.TitanRockConfiguration;
import nl.timtoys.corruptedadventure.worldgen.feature.TitanRockFeature;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring2FlatLandmarkFeature;

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
    /** {@code corruptedadventure:lava_fall} (change 47c) */
    public static final DeferredHolder<Feature<?>, Feature<LavaFallConfiguration>> LAVA_FALL =
            FEATURES.register("lava_fall", () -> new LavaFallFeature(LavaFallConfiguration.CODEC));
    /** {@code corruptedadventure:hanging_cluster} (change 47c) */
    public static final DeferredHolder<Feature<?>, Feature<HangingClusterConfiguration>> HANGING_CLUSTER =
            FEATURES.register("hanging_cluster", () -> new HangingClusterFeature(HangingClusterConfiguration.CODEC));
    /** {@code corruptedadventure:fire_geode} (change 41b) */
    public static final DeferredHolder<Feature<?>, Feature<FireGeodeConfiguration>> FIRE_GEODE =
            FEATURES.register("fire_geode", () -> new FireGeodeFeature(FireGeodeConfiguration.CODEC));
    /** {@code corruptedadventure:wall_drape} (change 47d) */
    public static final DeferredHolder<Feature<?>, Feature<WallDrapeConfiguration>> WALL_DRAPE =
            FEATURES.register("wall_drape", () -> new WallDrapeFeature(WallDrapeConfiguration.CODEC));
    /** {@code corruptedadventure:terrace_pool} (change 47d) */
    public static final DeferredHolder<Feature<?>, Feature<TerracePoolConfiguration>> TERRACE_POOL =
            FEATURES.register("terrace_pool", () -> new TerracePoolFeature(TerracePoolConfiguration.CODEC));

    // ring 4 — W-biomes (verdant_falls)
    /** {@code corruptedadventure:ledge_strand} (verdant_falls: climbing strands under the ledges) */
    public static final DeferredHolder<Feature<?>, Feature<LedgeStrandConfiguration>> LEDGE_STRAND =
            FEATURES.register("ledge_strand", () -> new LedgeStrandFeature(LedgeStrandConfiguration.CODEC));

    // ring 4 — W-nether
    /** {@code corruptedadventure:giant_geode_lining} (78: the lining of the giant geodes of geode_hollows) */
    public static final DeferredHolder<Feature<?>, Feature<GiantGeodeLiningConfiguration>> GIANT_GEODE_LINING =
            FEATURES.register("giant_geode_lining", () -> new GiantGeodeLiningFeature(GiantGeodeLiningConfiguration.CODEC));
    /** {@code corruptedadventure:pillar_cladding} (78: clusters and lamps on the crystal columns of crystal_spires) */
    public static final DeferredHolder<Feature<?>, Feature<PillarCladdingConfiguration>> PILLAR_CLADDING =
            FEATURES.register("pillar_cladding", () -> new PillarCladdingFeature(PillarCladdingConfiguration.CODEC));
    /** {@code corruptedadventure:dune_rubble} (78: loose bones and boulders in the bone dunes, clear of the fossils) */
    public static final DeferredHolder<Feature<?>, Feature<RubbleConfiguration>> DUNE_RUBBLE =
            FEATURES.register("dune_rubble", () -> new DuneRubbleFeature(RubbleConfiguration.CODEC));

    // ring 4 — W-biomes g2
    /** {@code corruptedadventure:island_glow} (75: glowing ground cover in the shadow of the islands) */
    public static final DeferredHolder<Feature<?>, Feature<IslandGlowConfiguration>> ISLAND_GLOW =
            FEATURES.register("island_glow", () -> new IslandGlowFeature(IslandGlowConfiguration.CODEC));
    /** {@code corruptedadventure:island_underside} (75: moss strands and glowing roots under the islands) */
    public static final DeferredHolder<Feature<?>, Feature<IslandUndersideConfiguration>> ISLAND_UNDERSIDE =
            FEATURES.register("island_underside", () -> new IslandUndersideFeature(IslandUndersideConfiguration.CODEC));
    /** {@code corruptedadventure:giant_lily_pad} (75: the whole 2 × 2 pad on still water) */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> GIANT_LILY_PAD =
            FEATURES.register("giant_lily_pad", () -> new GiantLilyPadFeature(NoneFeatureConfiguration.CODEC));
    /** {@code corruptedadventure:reed_clump} (75, 76: reeds 2–3 high on banks and in shallow water) */
    public static final DeferredHolder<Feature<?>, Feature<ReedClumpConfiguration>> REED_CLUMP =
            FEATURES.register("reed_clump", () -> new ReedClumpFeature(ReedClumpConfiguration.CODEC));
    /** {@code corruptedadventure:local_coral} (75: vanilla's warm-ocean coral kept in its chunk) */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> LOCAL_CORAL =
            FEATURES.register("local_coral", () -> new LocalCoralFeature(NoneFeatureConfiguration.CODEC));
    /** {@code corruptedadventure:sucking_mud_patch} (76: small patches of sucking mud on the blighted banks, G1 = A) */
    public static final DeferredHolder<Feature<?>, Feature<SuckingMudPatchConfiguration>> SUCKING_MUD_PATCH =
            FEATURES.register("sucking_mud_patch", () -> new SuckingMudPatchFeature(SuckingMudPatchConfiguration.CODEC));
    /** {@code corruptedadventure:toxic_pool} (76: closed bowls of toxic sludge) */
    public static final DeferredHolder<Feature<?>, Feature<ToxicPoolConfiguration>> TOXIC_POOL =
            FEATURES.register("toxic_pool", () -> new ToxicPoolFeature(ToxicPoolConfiguration.CODEC));
    /** {@code corruptedadventure:gas_pocket} (76: closed swamp gas bubbles under the peat crust, Q10 = A) */
    public static final DeferredHolder<Feature<?>, Feature<GasPocketConfiguration>> GAS_POCKET =
            FEATURES.register("gas_pocket", () -> new GasPocketFeature(GasPocketConfiguration.CODEC));
    /** {@code corruptedadventure:sinkhole_gas} (76: layers of swamp gas on the floor of the peat sinkholes, Q10 = A) */
    public static final DeferredHolder<Feature<?>, Feature<SinkholeGasConfiguration>> SINKHOLE_GAS =
            FEATURES.register("sinkhole_gas", () -> new SinkholeGasFeature(SinkholeGasConfiguration.CODEC));
    /** {@code corruptedadventure:sunken_geode} (77: aquamarine geodes half open in the floor under deep water, Q14 = A) */
    public static final DeferredHolder<Feature<?>, Feature<SunkenGeodeConfiguration>> SUNKEN_GEODE =
            FEATURES.register("sunken_geode", () -> new SunkenGeodeFeature(SunkenGeodeConfiguration.CODEC));
    /** {@code corruptedadventure:wall_plant} (75b: orchids and wall bromeliads on the gorge walls of verdant_falls) */
    public static final DeferredHolder<Feature<?>, Feature<WallPlantConfiguration>> WALL_PLANT =
            FEATURES.register("wall_plant", () -> new WallPlantFeature(WallPlantConfiguration.CODEC));
    /** {@code corruptedadventure:island_vines} (75: sagging root vines between the island pairs of 74) */
    public static final DeferredHolder<Feature<?>, Feature<IslandVinesConfiguration>> ISLAND_VINES =
            FEATURES.register("island_vines", () -> new IslandVinesFeature(IslandVinesConfiguration.CODEC));
    /** {@code corruptedadventure:island_lift} (lift package before teststop 5b: the updraft columns of package 61) */
    public static final DeferredHolder<Feature<?>, Feature<BlockStateConfiguration>> ISLAND_LIFT =
            FEATURES.register("island_lift", () -> new IslandLiftFeature(BlockStateConfiguration.CODEC));
    /** {@code corruptedadventure:tunnel_lining} (77: crystals, ores and creature tracks in the tunnel net of 73) */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> TUNNEL_LINING =
            FEATURES.register("tunnel_lining", () -> new TunnelLiningFeature(NoneFeatureConfiguration.CODEC));
    /**
     * {@code corruptedadventure:isolated_tree} (tree determinism, option A): vanilla's tree for ring 4, grown against the
     * terrain without the other trees, with its own random and a fixed priority where trees overlap
     */
    public static final DeferredHolder<Feature<?>, Feature<TreeConfiguration>> ISOLATED_TREE =
            FEATURES.register("isolated_tree", () -> new IsolatedTreeFeature(TreeConfiguration.CODEC));
    /**
     * {@code corruptedadventure:ground_plant} (tree determinism): {@code simple_block} that always draws its plant and
     * then only fills open air, for the weighted ground plants of ring 4
     */
    public static final DeferredHolder<Feature<?>, Feature<SimpleBlockConfiguration>> GROUND_PLANT =
            FEATURES.register("ground_plant", () -> new GroundPlantFeature(SimpleBlockConfiguration.CODEC));
    /** {@code corruptedadventure:boulder} (ring 4 decor proef): a mossy rock, a ground object trees never take */
    public static final DeferredHolder<Feature<?>, Feature<BoulderConfiguration>> BOULDER =
            FEATURES.register("boulder", () -> new BoulderFeature(BoulderConfiguration.CODEC));
    /** {@code corruptedadventure:fallen_tree} (ring 4 decor proef): a stump and/or a fallen trunk with moss and mushrooms */
    public static final DeferredHolder<Feature<?>, Feature<FallenTreeConfiguration>> FALLEN_TREE =
            FEATURES.register("fallen_tree", () -> new FallenTreeFeature(FallenTreeConfiguration.CODEC));
    /** {@code corruptedadventure:terrace_dressing} (ring 4 decor proef): vines down the terrace walls, moss on the rims */
    public static final DeferredHolder<Feature<?>, Feature<TerraceDressingConfiguration>> TERRACE_DRESSING =
            FEATURES.register("terrace_dressing", () -> new TerraceDressingFeature(TerraceDressingConfiguration.CODEC));
    /** {@code corruptedadventure:split_rock} (decor D; 86c): the split rock on each landmark floe of the zone 4→5 (sundered_reach) */
    public static final DeferredHolder<Feature<?>, Feature<SplitRockConfiguration>> SPLIT_ROCK =
            FEATURES.register("split_rock", () -> new SplitRockFeature(SplitRockConfiguration.CODEC));

    // ring 4 — decor B (drowned_forest, peat_moors, reed_marsh)
    /** {@code corruptedadventure:dead_tree} (decor B): a crooked dead tree with bare branches and vines, in its own chunk */
    public static final DeferredHolder<Feature<?>, Feature<DeadTreeConfiguration>> DEAD_TREE =
            FEATURES.register("dead_tree", () -> new DeadTreeFeature(DeadTreeConfiguration.CODEC));
    /** {@code corruptedadventure:toxic_puddle} (decor B): a small closed puddle of toxic sludge in a rim of mud */
    public static final DeferredHolder<Feature<?>, Feature<ToxicPuddleFeature.Config>> TOXIC_PUDDLE =
            FEATURES.register("toxic_puddle", () -> new ToxicPuddleFeature(ToxicPuddleFeature.Config.CODEC));
    /** {@code corruptedadventure:fungus_ring} (decor B): a ring of mushrooms on dry ground */
    public static final DeferredHolder<Feature<?>, Feature<FungusRingFeature.Config>> FUNGUS_RING =
            FEATURES.register("fungus_ring", () -> new FungusRingFeature(FungusRingFeature.Config.CODEC));
    /** {@code corruptedadventure:blighted_landmark} (decor B): this chunk's part of the rare landmarks of the blighted band */
    public static final DeferredHolder<Feature<?>, Feature<BlightedLandmarkFeature.Config>> BLIGHTED_LANDMARK =
            FEATURES.register("blighted_landmark", () -> new BlightedLandmarkFeature(BlightedLandmarkFeature.Config.CODEC));
    /** {@code corruptedadventure:island_dressing} (decor rollout C): moss, crystals, hanging plants and vines on the islands' undersides */
    public static final DeferredHolder<Feature<?>, Feature<IslandDressingConfiguration>> ISLAND_DRESSING =
            FEATURES.register("island_dressing", () -> new IslandDressingFeature(IslandDressingConfiguration.CODEC));
    /** {@code corruptedadventure:giant_moonpetal} (decor rollout C): the giant moonpetal whose crown covers a sky island */
    public static final DeferredHolder<Feature<?>, Feature<GiantMoonpetalConfiguration>> GIANT_MOONPETAL =
            FEATURES.register("giant_moonpetal", () -> new GiantMoonpetalFeature(GiantMoonpetalConfiguration.CODEC));
    /** {@code corruptedadventure:sky_ruin} (decor rollout C): a crumbling ring of pillars with an arch on a sky island */
    public static final DeferredHolder<Feature<?>, Feature<SkyRuinConfiguration>> SKY_RUIN =
            FEATURES.register("sky_ruin", () -> new SkyRuinFeature(SkyRuinConfiguration.CODEC));

    // ring 4 — decor rollout group A (crystal_lagoon, lotus_shallows, glowroot_swamp)
    /** {@code corruptedadventure:shoal} (decor A): sand banks, atolls and islets raised out of shallow sea water */
    public static final DeferredHolder<Feature<?>, Feature<ShoalConfiguration>> SHOAL =
            FEATURES.register("shoal", () -> new ShoalFeature(ShoalConfiguration.CODEC));
    /** {@code corruptedadventure:crystal_spire} (decor A): the lagoon's landmark, a group of glowing crystal spires */
    public static final DeferredHolder<Feature<?>, Feature<CrystalSpireConfiguration>> CRYSTAL_SPIRE =
            FEATURES.register("crystal_spire", () -> new CrystalSpireFeature(CrystalSpireConfiguration.CODEC));
    /** {@code corruptedadventure:root_arch} (decor A): the swamp's landmark arch of mangrove roots over the water */
    public static final DeferredHolder<Feature<?>, Feature<RootArchConfiguration>> ROOT_ARCH =
            FEATURES.register("root_arch", () -> new RootArchFeature(RootArchConfiguration.CODEC));
    /** {@code corruptedadventure:root_tangle} (decor A): a knot of small root bows over shallow swamp water */
    public static final DeferredHolder<Feature<?>, Feature<RootArchConfiguration>> ROOT_TANGLE =
            FEATURES.register("root_tangle", () -> new RootTangleFeature(RootArchConfiguration.CODEC));
    /** {@code corruptedadventure:ancient_swamp_tree} (decor A): the swamp's landmark tree, a 3 × 3 mangrove full of moss */
    public static final DeferredHolder<Feature<?>, Feature<AncientSwampTreeConfiguration>> ANCIENT_SWAMP_TREE =
            FEATURES.register("ancient_swamp_tree", () -> new AncientSwampTreeFeature(AncientSwampTreeConfiguration.CODEC));

    // ring 5 — W-biomes
    /** {@code corruptedadventure:missing_chunk} (94): the clean missing chunks (their floor) and the lava dam round them */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> MISSING_CHUNK =
            FEATURES.register("missing_chunk", () -> new MissingChunkFeature(NoneFeatureConfiguration.CODEC));
    /** {@code corruptedadventure:herobrine_sign} (94): a waxed corrupted sign with one translatable line, facing inwards */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> HEROBRINE_SIGN =
            FEATURES.register("herobrine_sign", () -> new HerobrineSignFeature(NoneFeatureConfiguration.CODEC));
    /** {@code corruptedadventure:torch_pattern} (94): redstone torches in a cross, a ring, a row or an arrow to a castle */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> TORCH_PATTERN =
            FEATURES.register("torch_pattern", () -> new TorchPatternFeature(NoneFeatureConfiguration.CODEC));
    /** {@code corruptedadventure:abandoned_camp} (94): a player's camp left behind (tent, bed, chest, dead campfire) */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> ABANDONED_CAMP =
            FEATURES.register("abandoned_camp", () -> new AbandonedCampFeature(NoneFeatureConfiguration.CODEC));
    /** {@code corruptedadventure:mirrored_trees} (94): this chunk's part of false_meadow's mirrored oak pairs */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> MIRRORED_TREES =
            FEATURES.register("mirrored_trees", () -> new MirroredTreesFeature(NoneFeatureConfiguration.CODEC));
    /** {@code corruptedadventure:ring_5_memory} (94): this chunk's part of ring 5's memories of one kind */
    public static final DeferredHolder<Feature<?>, Feature<MemoryFeature.Config>> RING_5_MEMORY =
            FEATURES.register("ring_5_memory", () -> new MemoryFeature(MemoryFeature.Config.CODEC));
    /** {@code corruptedadventure:inverted_tree} (95): a tree hanging upside down under an inverted hill */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> INVERTED_TREE =
            FEATURES.register("inverted_tree", () -> new InvertedTreeFeature(NoneFeatureConfiguration.CODEC));

    // ring 2 — G2 landmarks of the flat biomes (salt dome, dune skeleton, giant palm)
    /** {@code corruptedadventure:ring_2_flat_landmark} (G2 ring 2): this chunk's part of the flat biomes' landmarks of one kind */
    public static final DeferredHolder<Feature<?>, Feature<Ring2FlatLandmarkFeature.Config>> RING_2_FLAT_LANDMARK =
            FEATURES.register("ring_2_flat_landmark", () -> new Ring2FlatLandmarkFeature(Ring2FlatLandmarkFeature.Config.CODEC));

    private ModFeatures() {
    }

    public static void register(IEventBus modBus) {
        FEATURES.register(modBus);
    }
}
