package nl.timtoys.corruptedadventure.fluid;

import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry.InteractionInformation;
import nl.timtoys.corruptedadventure.registry.ModFluidTypes;

/**
 * How the toxic pool mixes (contract ring 4 §3): where it touches water it turns into mud, where it touches lava into
 * stone. Only the toxic fluid changes, so water stays water and never turns poisonous. Server-side, through NeoForge's
 * {@link FluidInteractionRegistry} (checked whenever the toxic fluid is placed or a neighbour changes).
 */
public final class ToxicPoolInteractions {
    private ToxicPoolInteractions() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ToxicPoolInteractions::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            FluidInteractionRegistry.addInteraction(ModFluidTypes.TOXIC_POOL.get(),
                    new InteractionInformation(NeoForgeMod.WATER_TYPE.value(), Blocks.MUD.defaultBlockState()));
            FluidInteractionRegistry.addInteraction(ModFluidTypes.TOXIC_POOL.get(),
                    new InteractionInformation(NeoForgeMod.LAVA_TYPE.value(), Blocks.STONE.defaultBlockState()));
        });
    }
}
