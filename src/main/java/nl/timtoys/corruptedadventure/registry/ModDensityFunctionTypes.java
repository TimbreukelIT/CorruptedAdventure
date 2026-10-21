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
import nl.timtoys.corruptedadventure.worldgen.densityfunction.GiantGeodes;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.SeaFossils;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandOutput;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.RadialFalloff;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.Ring4BandFunction;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.Ring5BandFunction;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.Ring5Field;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.Ring5Output;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.RingBlend;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.RingEdge;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.RingFillGate;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.RingGate;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.RockPillars;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VoidEdge;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicField;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicOutput;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.ZoneFracture;

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

    // ring 3 — W-terrein

    /** {@code corruptedadventure:volcanic_field} (ring 3 terrain) */
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<VolcanicField>> VOLCANIC_FIELD =
            DENSITY_FUNCTION_TYPES.register("volcanic_field", () -> VolcanicField.MAP_CODEC);

    /** {@code corruptedadventure:volcanic_output} (one output of a volcanic field) */
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<VolcanicOutput>> VOLCANIC_OUTPUT =
            DENSITY_FUNCTION_TYPES.register("volcanic_output", () -> VolcanicOutput.MAP_CODEC);

    // 39e — worldgen performance
    /** {@code corruptedadventure:ring_fill_gate} (only set by {@code RingBlend.mapAll}, not in JSON) */
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<RingFillGate>> RING_FILL_GATE =
            DENSITY_FUNCTION_TYPES.register("ring_fill_gate", () -> RingFillGate.MAP_CODEC);

    // ring 4 — W-terrein

    /** {@code corruptedadventure:ring_4_band} (the band axis of ring 4, package 59) */
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<Ring4BandFunction>> RING_4_BAND =
            DENSITY_FUNCTION_TYPES.register("ring_4_band", () -> Ring4BandFunction.MAP_CODEC);

    /** {@code corruptedadventure:island_field} (the floating islands of ring 4, package 59) */
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<IslandField>> ISLAND_FIELD =
            DENSITY_FUNCTION_TYPES.register("island_field", () -> IslandField.MAP_CODEC);

    // ring 4 — W-terrein

    /** {@code corruptedadventure:island_output} (one output of the ring 4 terrain field, golf 1) */
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<IslandOutput>> ISLAND_OUTPUT =
            DENSITY_FUNCTION_TYPES.register("island_output", () -> IslandOutput.MAP_CODEC);

    // ring 4 — W-nether

    /** {@code corruptedadventure:giant_geodes} (the giant geodes of Nether ring 4's geode hollows, package 65) */
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<GiantGeodes>> GIANT_GEODES =
            DENSITY_FUNCTION_TYPES.register("giant_geodes", () -> GiantGeodes.MAP_CODEC);

    /** {@code corruptedadventure:sea_fossils} (the sea-creature fossils of Nether ring 4's bone dunes as terrain, package 78) */
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<SeaFossils>> SEA_FOSSILS =
            DENSITY_FUNCTION_TYPES.register("sea_fossils", () -> SeaFossils.MAP_CODEC);

    // ring 5 — W-terrein

    /** {@code corruptedadventure:ring_5_band} (the band axis of ring 5, package 82) */
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<Ring5BandFunction>> RING_5_BAND =
            DENSITY_FUNCTION_TYPES.register("ring_5_band", () -> Ring5BandFunction.MAP_CODEC);

    /** {@code corruptedadventure:ring_5_field} (the terrain field of ring 5 with the castle sites, package 82) */
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<Ring5Field>> RING_5_FIELD =
            DENSITY_FUNCTION_TYPES.register("ring_5_field", () -> Ring5Field.MAP_CODEC);

    /** {@code corruptedadventure:ring_5_output} (one output of the ring 5 terrain field, package 82) */
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<Ring5Output>> RING_5_OUTPUT =
            DENSITY_FUNCTION_TYPES.register("ring_5_output", () -> Ring5Output.MAP_CODEC);

    /** {@code corruptedadventure:zone_fracture} (the fracture zone 4→5 around the overworld's final density; 82 skeleton, 85) */
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<ZoneFracture>> ZONE_FRACTURE =
            DENSITY_FUNCTION_TYPES.register("zone_fracture", () -> ZoneFracture.MAP_CODEC);

    private ModDensityFunctionTypes() {
    }

    public static void register(IEventBus modBus) {
        DENSITY_FUNCTION_TYPES.register(modBus);
    }
}
