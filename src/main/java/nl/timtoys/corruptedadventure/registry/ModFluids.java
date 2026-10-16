package nl.timtoys.corruptedadventure.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.fluid.ToxicPoolFluid;

/** Our fluids: a source and a flowing variant per fluid type ({@link ModFluidTypes}). */
public final class ModFluids {
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, CorruptedAdventure.MOD_ID);

    public static final DeferredHolder<Fluid, ToxicPoolFluid.Source> TOXIC_POOL = FLUIDS.register("toxic_pool",
            () -> new ToxicPoolFluid.Source(toxicPool()));
    public static final DeferredHolder<Fluid, ToxicPoolFluid.Flowing> FLOWING_TOXIC_POOL = FLUIDS.register("flowing_toxic_pool",
            () -> new ToxicPoolFluid.Flowing(toxicPool()));

    private ModFluids() {
    }

    public static void register(IEventBus modBus) {
        FLUIDS.register(modBus);
    }

    /**
     * Slow and short like lava in the overworld (contract ring 4 §3): it spreads about three blocks (level drops by 2
     * per block), a tick rate of 30 and a short search for slopes.
     */
    private static BaseFlowingFluid.Properties toxicPool() {
        return new BaseFlowingFluid.Properties(ModFluidTypes.TOXIC_POOL, TOXIC_POOL, FLOWING_TOXIC_POOL)
                .block(ModBlocks.TOXIC_POOL)
                .bucket(ModItems.TOXIC_POOL_BUCKET)
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2)
                .tickRate(30)
                .explosionResistance(100.0F);
    }
}
