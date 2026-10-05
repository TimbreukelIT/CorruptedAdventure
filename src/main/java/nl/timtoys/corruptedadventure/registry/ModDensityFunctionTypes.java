package nl.timtoys.corruptedadventure.registry;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.CanyonDetails;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.CellEdges;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.DewGeodes;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.RadialFalloff;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.RingBlend;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.RingEdge;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.RingGate;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.RockPillars;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VoidEdge;

public final class ModDensityFunctionTypes {
    private static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, CorruptedAdventure.MOD_ID);

    /** {@code corruptedadventure:ring_blend} */
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<RingBlend>> RING_BLEND =
            DENSITY_FUNCTION_TYPES.register("ring_blend", () -> RingBlend.MAP_CODEC);

    /** {@code corruptedadventure:ring_gate} */
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<RingGate>> RING_GATE =
            DENSITY_FUNCTION_TYPES.register("ring_gate", () -> RingGate.MAP_CODEC);

    /** {@code corruptedadventure:void_edge} */
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<VoidEdge>> VOID_EDGE =
            DENSITY_FUNCTION_TYPES.register("void_edge", () -> VoidEdge.MAP_CODEC);

    /** {@code corruptedadventure:ring_edge} */
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<RingEdge>> RING_EDGE =
            DENSITY_FUNCTION_TYPES.register("ring_edge", () -> RingEdge.MAP_CODEC);

    /** {@code corruptedadventure:dew_geodes} (change 35) */
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<DewGeodes>> DEW_GEODES =
            DENSITY_FUNCTION_TYPES.register("dew_geodes", () -> DewGeodes.MAP_CODEC);

    /** {@code corruptedadventure:radial_falloff} */
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<RadialFalloff>> RADIAL_FALLOFF =
            DENSITY_FUNCTION_TYPES.register("radial_falloff", () -> RadialFalloff.MAP_CODEC);

    /** {@code corruptedadventure:cell_edges} */
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<CellEdges>> CELL_EDGES =
            DENSITY_FUNCTION_TYPES.register("cell_edges", () -> CellEdges.MAP_CODEC);

    /** {@code corruptedadventure:rock_pillars} */
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<RockPillars>> ROCK_PILLARS =
            DENSITY_FUNCTION_TYPES.register("rock_pillars", () -> RockPillars.MAP_CODEC);

    /** {@code corruptedadventure:canyon_details} */
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<CanyonDetails>> CANYON_DETAILS =
            DENSITY_FUNCTION_TYPES.register("canyon_details", () -> CanyonDetails.MAP_CODEC);

    private ModDensityFunctionTypes() {
    }

    public static void register(IEventBus modBus) {
        DENSITY_FUNCTION_TYPES.register(modBus);
    }
}
