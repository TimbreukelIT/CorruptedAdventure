package nl.timtoys.corruptedadventure.network;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * All payloads of the mod, registered in one place. Common code: loaded on the dedicated server too, so it never names
 * a class from {@code client/}; clientbound payloads get their handler through {@link ClientboundHandlers}.
 */
public final class ModNetwork {
    /**
     * Network version, required on both sides: a client with another version (or without the mod) gets NeoForge's
     * disconnect message. <b>Bump this on every change to a payload or its codec, and for every new payload</b>
     * (an older client does not know it).
     */
    public static final String PROTOCOL_VERSION = "11";

    private ModNetwork() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModNetwork::onRegisterPayloadHandlers);
    }

    private static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        register(event.registrar(PROTOCOL_VERSION));
    }

    /** One line per payload. Handlers run on the main thread (the registrar's default). */
    private static void register(PayloadRegistrar registrar) {
        registrar.playToClient(RingLayoutPayload.TYPE, RingLayoutPayload.STREAM_CODEC,
                ClientboundHandlers.forward(RingLayoutPayload.TYPE));
        registrar.playToClient(ConditionSyncPayload.TYPE, ConditionSyncPayload.STREAM_CODEC,
                ClientboundHandlers.forward(ConditionSyncPayload.TYPE));
        registrar.playToClient(RingWeatherPayload.TYPE, RingWeatherPayload.STREAM_CODEC,
                ClientboundHandlers.forward(RingWeatherPayload.TYPE));
        registrar.playToClient(WindGustPayload.TYPE, WindGustPayload.STREAM_CODEC,
                ClientboundHandlers.forward(WindGustPayload.TYPE));
        registrar.playToClient(VolcanoPlumesPayload.TYPE, VolcanoPlumesPayload.STREAM_CODEC,
                ClientboundHandlers.forward(VolcanoPlumesPayload.TYPE));
        registrar.playToClient(LensScanPayload.TYPE, LensScanPayload.STREAM_CODEC,
                ClientboundHandlers.forward(LensScanPayload.TYPE));
        // Phase 4: The Lost Keeper's mist bank.
        registrar.playToClient(KeeperMistPayload.TYPE, KeeperMistPayload.STREAM_CODEC,
                ClientboundHandlers.forward(KeeperMistPayload.TYPE));
    }
}
