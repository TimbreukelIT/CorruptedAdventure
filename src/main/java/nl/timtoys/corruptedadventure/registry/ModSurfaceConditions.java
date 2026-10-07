package nl.timtoys.corruptedadventure.registry;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.worldgen.surface.PolygonEdgeCondition;
import nl.timtoys.corruptedadventure.worldgen.surface.VolcanicThresholdCondition;

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

    private ModSurfaceConditions() {
    }

    public static void register(IEventBus modBus) {
        CONDITIONS.register(modBus);
    }
}
