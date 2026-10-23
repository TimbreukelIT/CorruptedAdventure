package nl.timtoys.corruptedadventure.client.weather;

import net.neoforged.bus.api.IEventBus;

/**
 * Registers the client side of ring 5's and Nether ring 5's atmosphere (package 98) from {@code CorruptedAdventureClient}
 * ({@code Dist.CLIENT} only): the sky per band ({@link Ring5Sky}, before the fog that reads it), the fog and glitch fog
 * ({@link Ring5Fog}), the echo footsteps ({@link EchoFootsteps}), Nether ring 5's white fog ({@link NetherRing5Fog}), the
 * silence ({@link SilenceEffects}) and the void meter's glitch visuals ({@link VoidGlitchEffects}). The biome sounds and
 * particles are biome data ({@code worldgen/biome/Ring5Atmosphere}); the void teleport's glitch is the server's
 * ({@code weather/Ring5Glitches}).
 */
public final class Ring5ClientAtmosphere {
    private Ring5ClientAtmosphere() {
    }

    public static void register(IEventBus gameBus) {
        Ring5Sky.register(gameBus);
        Ring5Fog.register(gameBus);
        EchoFootsteps.register(gameBus);
        NetherRing5Fog.register(gameBus);
        SilenceEffects.register(gameBus);
        VoidGlitchEffects.register(gameBus);
    }
}
