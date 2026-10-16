package nl.timtoys.corruptedadventure.registry;

import net.minecraft.sounds.SoundEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

/**
 * Our fluid types (NeoForge). The client look (textures, fog) is a client extension in {@code client/}
 * ({@code ToxicPoolClientExtensions}); nothing here is client-only.
 */
public final class ModFluidTypes {
    private static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, CorruptedAdventure.MOD_ID);

    /**
     * The toxic pool of ring 4 (contract ring 4 §3): a thick, sluggish liquid. You swim in it like in lava (half the
     * speed of water, NeoForge's generic fluid movement), you can drown in it, boats do not float on it and it never
     * makes new sources. The poison comes from the conditions ({@code #poison_sources}), not from the fluid.
     */
    public static final DeferredHolder<FluidType, FluidType> TOXIC_POOL = FLUID_TYPES.register("toxic_pool",
            () -> new FluidType(FluidType.Properties.create()
                    .canSwim(true)
                    .canDrown(true)
                    .canExtinguish(true)
                    .supportsBoating(false)
                    .canHydrate(false)
                    .canConvertToSource(false)
                    .fallDistanceModifier(0.0F)
                    .motionScale(0.007)
                    .density(1500)
                    .viscosity(3000)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

    private ModFluidTypes() {
    }

    public static void register(IEventBus modBus) {
        FLUID_TYPES.register(modBus);
    }
}
