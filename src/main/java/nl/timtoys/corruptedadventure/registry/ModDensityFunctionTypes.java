package nl.timtoys.corruptedadventure.registry;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.RingBlend;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VoidEdge;

public final class ModDensityFunctionTypes {
    private static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, CorruptedAdventure.MOD_ID);

    /** {@code corruptedadventure:ring_blend} */
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<RingBlend>> RING_BLEND =
            DENSITY_FUNCTION_TYPES.register("ring_blend", () -> RingBlend.MAP_CODEC);

    /** {@code corruptedadventure:void_edge} */
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<VoidEdge>> VOID_EDGE =
            DENSITY_FUNCTION_TYPES.register("void_edge", () -> VoidEdge.MAP_CODEC);

    private ModDensityFunctionTypes() {
    }

    public static void register(IEventBus modBus) {
        DENSITY_FUNCTION_TYPES.register(modBus);
    }
}
