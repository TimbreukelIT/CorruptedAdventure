package nl.timtoys.corruptedadventure.registry;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.worldgen.surface.PolygonEdgeCondition;
import nl.timtoys.corruptedadventure.worldgen.surface.Ring4ThresholdCondition;
import nl.timtoys.corruptedadventure.worldgen.surface.RingDepthCondition;
import nl.timtoys.corruptedadventure.worldgen.surface.SeaFossilCondition;
import nl.timtoys.corruptedadventure.worldgen.surface.RingReachCondition;
import nl.timtoys.corruptedadventure.worldgen.surface.VolcanicThresholdCondition;
import nl.timtoys.corruptedadventure.worldgen.surface.WorldTreeCondition;
import nl.timtoys.corruptedadventure.worldgen.surface.NetherRing5DressingCondition;
import nl.timtoys.corruptedadventure.worldgen.surface.NetherRing5PillarCondition;
import nl.timtoys.corruptedadventure.worldgen.surface.Ring5GroundCondition;
import nl.timtoys.corruptedadventure.worldgen.surface.Ring5SpeckleCondition;

/** Surface rule condition types (the {@code if_true} of a surface rule); the rules themselves are hand-written JSON. */
public final class ModSurfaceConditions {
    private static final DeferredRegister<MapCodec<? extends SurfaceRules.ConditionSource>> CONDITIONS =
            DeferredRegister.create(Registries.MATERIAL_CONDITION, CorruptedAdventure.MOD_ID);

    /** {@code corruptedadventure:polygon_edge} */
    public static final DeferredHolder<MapCodec<? extends SurfaceRules.ConditionSource>, MapCodec<PolygonEdgeCondition>> POLYGON_EDGE =
            CONDITIONS.register("polygon_edge", () -> PolygonEdgeCondition.MAP_CODEC);

    // ring 3 — W-biomes
    /** {@code corruptedadventure:volcanic_threshold} */
    public static final DeferredHolder<MapCodec<? extends SurfaceRules.ConditionSource>, MapCodec<VolcanicThresholdCondition>> VOLCANIC_THRESHOLD =
            CONDITIONS.register("volcanic_threshold", () -> VolcanicThresholdCondition.MAP_CODEC);

    // 39e — worldgen performance
    /** {@code corruptedadventure:ring_reach} */
    public static final DeferredHolder<MapCodec<? extends SurfaceRules.ConditionSource>, MapCodec<RingReachCondition>> RING_REACH =
            CONDITIONS.register("ring_reach", () -> RingReachCondition.MAP_CODEC);

    // ring 4 — W-nether
    /** {@code corruptedadventure:ring_depth} (65: the crystal columns of Nether ring 4 only where the terrain has them) */
    public static final DeferredHolder<MapCodec<? extends SurfaceRules.ConditionSource>, MapCodec<RingDepthCondition>> RING_DEPTH =
            CONDITIONS.register("ring_depth", () -> RingDepthCondition.MAP_CODEC);
    /** {@code corruptedadventure:sea_fossil} (78: the bones of the sea-creature fossils, painted bone_block) */
    public static final DeferredHolder<MapCodec<? extends SurfaceRules.ConditionSource>, MapCodec<SeaFossilCondition>> SEA_FOSSIL =
            CONDITIONS.register("sea_fossil", () -> SeaFossilCondition.MAP_CODEC);

    // ring 4 — W-biomes (verdant_falls)
    /** {@code corruptedadventure:ring4_threshold} (Q7 = A: calcite on the rims and lips, stone on the exact walls) */
    public static final DeferredHolder<MapCodec<? extends SurfaceRules.ConditionSource>, MapCodec<Ring4ThresholdCondition>> RING4_THRESHOLD =
            CONDITIONS.register("ring4_threshold", () -> Ring4ThresholdCondition.MAP_CODEC);

    // ring 4 — W-terrein-b
    /** {@code corruptedadventure:world_tree} (74, Q7 = A: the world tree's trunk, wood and leaves) */
    public static final DeferredHolder<MapCodec<? extends SurfaceRules.ConditionSource>, MapCodec<WorldTreeCondition>> WORLD_TREE =
            CONDITIONS.register("world_tree", () -> WorldTreeCondition.MAP_CODEC);

    // ring 5 — W-nether
    /** {@code corruptedadventure:nether_ring_5_pillar} (87: the columns of Nether ring 5, grid and giant, painted as columns) */
    public static final DeferredHolder<MapCodec<? extends SurfaceRules.ConditionSource>, MapCodec<NetherRing5PillarCondition>> NETHER_RING_5_PILLAR =
            CONDITIONS.register("nether_ring_5_pillar", () -> NetherRing5PillarCondition.MAP_CODEC);
    /** {@code corruptedadventure:nether_ring_5_dressing} (96: light band and ore in the columns, the giant column's vein and floor imprint) */
    public static final DeferredHolder<MapCodec<? extends SurfaceRules.ConditionSource>, MapCodec<NetherRing5DressingCondition>> NETHER_RING_5_DRESSING =
            CONDITIONS.register("nether_ring_5_dressing", () -> NetherRing5DressingCondition.MAP_CODEC);

    // ring 5 — W-biomes
    /** {@code corruptedadventure:ring_5_ground} (94: ring 5's own ground and its clean missing chunks) */
    public static final DeferredHolder<MapCodec<? extends SurfaceRules.ConditionSource>, MapCodec<Ring5GroundCondition>> RING_5_GROUND =
            CONDITIONS.register("ring_5_ground", () -> Ring5GroundCondition.MAP_CODEC);
    /** {@code corruptedadventure:ring_5_speckle} (94: pure random specks and patches, the glitch blocks of glitch_fields) */
    public static final DeferredHolder<MapCodec<? extends SurfaceRules.ConditionSource>, MapCodec<Ring5SpeckleCondition>> RING_5_SPECKLE =
            CONDITIONS.register("ring_5_speckle", () -> Ring5SpeckleCondition.MAP_CODEC);

    private ModSurfaceConditions() {
    }

    public static void register(IEventBus modBus) {
        CONDITIONS.register(modBus);
    }
}
