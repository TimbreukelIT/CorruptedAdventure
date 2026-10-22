package nl.timtoys.corruptedadventure.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.client.ambient.SingingSandAmbience;
import nl.timtoys.corruptedadventure.client.block.SignMaterials;
import nl.timtoys.corruptedadventure.client.fluid.ToxicPoolClientExtensions;
import nl.timtoys.corruptedadventure.client.hud.ConditionHudLayer;
import nl.timtoys.corruptedadventure.client.network.ConditionSyncHandler;
import nl.timtoys.corruptedadventure.client.network.RingLayoutHandler;
import nl.timtoys.corruptedadventure.client.network.RingWeatherHandler;
import nl.timtoys.corruptedadventure.client.network.VolcanoPlumesHandler;
import nl.timtoys.corruptedadventure.client.network.WindGustHandler;
import nl.timtoys.corruptedadventure.client.particle.AshFlakeParticle;
import nl.timtoys.corruptedadventure.client.particle.BlossomParticle;
import nl.timtoys.corruptedadventure.client.particle.DriftingHazeParticle;
import nl.timtoys.corruptedadventure.client.particle.EmberParticle;
import nl.timtoys.corruptedadventure.client.particle.GlitchSpeckParticle;
import nl.timtoys.corruptedadventure.client.particle.GlowMoteParticle;
import nl.timtoys.corruptedadventure.client.particle.IceGlintParticle;
import nl.timtoys.corruptedadventure.client.particle.RisingBubbleParticle;
import nl.timtoys.corruptedadventure.client.particle.SandGustParticle;
import nl.timtoys.corruptedadventure.client.particle.SteamPuffParticle;
import nl.timtoys.corruptedadventure.client.particle.WaterfallMistParticle;
import nl.timtoys.corruptedadventure.client.weather.ColdBreath;
import nl.timtoys.corruptedadventure.client.weather.NetherRing4Fog;
import nl.timtoys.corruptedadventure.client.weather.Ring3Ambience;
import nl.timtoys.corruptedadventure.client.weather.Ring4Ambience;
import nl.timtoys.corruptedadventure.client.weather.Ring4Fog;
import nl.timtoys.corruptedadventure.client.weather.Ring4SkyState;
import nl.timtoys.corruptedadventure.client.weather.RingWeatherClientHooks;
import nl.timtoys.corruptedadventure.client.weather.SunderedFade;
import nl.timtoys.corruptedadventure.client.weather.VolcanoPlumeRenderer;
import nl.timtoys.corruptedadventure.network.ClientboundHandlers;
import nl.timtoys.corruptedadventure.network.ConditionSyncPayload;
import nl.timtoys.corruptedadventure.network.RingLayoutPayload;
import nl.timtoys.corruptedadventure.network.RingWeatherPayload;
import nl.timtoys.corruptedadventure.network.VolcanoPlumesPayload;
import nl.timtoys.corruptedadventure.network.WindGustPayload;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;

/**
 * Client-only entrypoint. Never loaded on a dedicated server.
 * All client code (renderers, fog, sky, weather rendering, particles, screens) lives in this package.
 * Clientbound payload handlers are linked here through {@link ClientboundHandlers}, before any connection exists.
 */
@Mod(value = CorruptedAdventure.MOD_ID, dist = Dist.CLIENT)
public final class CorruptedAdventureClient {
    public CorruptedAdventureClient(IEventBus modEventBus, ModContainer modContainer) {
        CorruptedAdventure.LOGGER.info("The Corrupted Adventure client is loading");
        ClientboundHandlers.set(RingLayoutPayload.TYPE, RingLayoutHandler::handle);
        ClientboundHandlers.set(ConditionSyncPayload.TYPE, ConditionSyncHandler::handle);
        ClientboundHandlers.set(RingWeatherPayload.TYPE, RingWeatherHandler::handle);
        ClientboundHandlers.set(WindGustPayload.TYPE, WindGustHandler::handle);
        ClientboundHandlers.set(VolcanoPlumesPayload.TYPE, VolcanoPlumesHandler::handle);
        modEventBus.addListener(ConditionHudLayer::onRegisterGuiLayers);
        modEventBus.addListener(CorruptedAdventureClient::onRegisterParticleProviders);
        modEventBus.addListener(ToxicPoolClientExtensions::onRegisterClientExtensions);
        // ring 5 — B (88): the corrupted signs' materials.
        modEventBus.addListener(SignMaterials::onClientSetup);
        NeoForge.EVENT_BUS.addListener(ClientRingLayout::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(RingDebugOverlay::onDebugText);
        NeoForge.EVENT_BUS.addListener(SingingSandAmbience::onClientTick);
        // Weather renderers (changes 28, 45, 66, 85): RingWeatherRenderers.register(type, renderer) here.
        RingWeatherClientHooks.registerRing2(modEventBus, NeoForge.EVENT_BUS);
        RingWeatherClientHooks.registerRing3();
        RingWeatherClientHooks.registerRing4();
        RingWeatherClientHooks.registerRing5(modEventBus, NeoForge.EVENT_BUS);
        RingWeatherClientHooks.register(NeoForge.EVENT_BUS);
        // Ring 3 ambience outside the weather (package 50): rumble with camera shake, sparks on the peaks.
        Ring3Ambience.register(NeoForge.EVENT_BUS);
        // Breath in the cold of Nether ring 3 (package 56).
        ColdBreath.register(NeoForge.EVENT_BUS);
        // Smoke plumes above the ring 3 volcanoes the server sends (package 50b).
        VolcanoPlumeRenderer.register(NeoForge.EVENT_BUS);
        // Ring 4 (change 80): day and night, reeds, the silhouette, glitch specks; zone 4→5 to grey after the weather's
        // fog colour; the pulsing fog of Nether ring 4.
        Ring4Ambience.register(NeoForge.EVENT_BUS);
        SunderedFade.register(NeoForge.EVENT_BUS);
        NetherRing4Fog.register(NeoForge.EVENT_BUS);
        // Ring 4 sky and mist (sparring 2026-10-20): camera state per tick, then the fog listeners; dusk, clouds and the
        // sky tint come in through the client mixins.
        Ring4SkyState.register(NeoForge.EVENT_BUS);
        Ring4Fog.register(NeoForge.EVENT_BUS);
    }

    private static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticleTypes.SAND_GUST.get(), SandGustParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.ASH_FLAKE.get(), sprites -> new AshFlakeParticle.Provider(sprites, false));
        event.registerSpriteSet(ModParticleTypes.EMBER.get(), sprites -> new EmberParticle.Provider(sprites, false));
        event.registerSpriteSet(ModParticleTypes.STEAM_PUFF.get(), sprites -> {
            // The volcano plumes (package 50b) share these sprites; the particle engine rebinds them on every reload.
            VolcanoPlumeRenderer.useSprites(sprites);
            return new SteamPuffParticle.Provider(sprites, SteamPuffParticle.STEAM);
        });
        event.registerSpriteSet(ModParticleTypes.DRIFTING_ASH.get(), sprites -> new AshFlakeParticle.Provider(sprites, true));
        event.registerSpriteSet(ModParticleTypes.RISING_EMBER.get(), sprites -> new EmberParticle.Provider(sprites, true));
        event.registerSpriteSet(ModParticleTypes.SULFUR_STEAM.get(),
                sprites -> new SteamPuffParticle.Provider(sprites, SteamPuffParticle.SULFUR_STEAM));
        event.registerSpriteSet(ModParticleTypes.ICE_GLINT.get(), IceGlintParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.WATERFALL_MIST.get(), WaterfallMistParticle.Provider::new);
        // Ring 4 and Nether ring 4 (package 80-0).
        event.registerSpriteSet(ModParticleTypes.WILL_O_WISP.get(),
                sprites -> new GlowMoteParticle.Provider(sprites, GlowMoteParticle.WILL_O_WISP));
        event.registerSpriteSet(ModParticleTypes.FIREFLY.get(),
                sprites -> new GlowMoteParticle.Provider(sprites, GlowMoteParticle.FIREFLY));
        event.registerSpriteSet(ModParticleTypes.BLOSSOM.get(), BlossomParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.SWAMP_GAS.get(),
                sprites -> new DriftingHazeParticle.Provider(sprites, DriftingHazeParticle.SWAMP_GAS));
        event.registerSpriteSet(ModParticleTypes.BONE_DUST.get(),
                sprites -> new DriftingHazeParticle.Provider(sprites, DriftingHazeParticle.BONE_DUST));
        event.registerSpriteSet(ModParticleTypes.TOXIC_BUBBLE.get(),
                sprites -> new RisingBubbleParticle.Provider(sprites, RisingBubbleParticle.TOXIC));
        event.registerSpriteSet(ModParticleTypes.UPDRAFT_BUBBLE.get(),
                sprites -> new RisingBubbleParticle.Provider(sprites, RisingBubbleParticle.UPDRAFT));
        event.registerSpriteSet(ModParticleTypes.GLITCH_SPECK.get(), GlitchSpeckParticle.Provider::new);
    }
}
