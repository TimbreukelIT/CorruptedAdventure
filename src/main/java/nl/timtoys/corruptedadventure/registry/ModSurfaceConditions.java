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

    private ModSurfaceConditions() {
    }

    public static void register(IEventBus modBus) {
        CONDITIONS.register(modBus);
    }
}
