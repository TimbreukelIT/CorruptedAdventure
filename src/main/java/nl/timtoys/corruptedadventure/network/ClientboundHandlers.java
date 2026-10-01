package nl.timtoys.corruptedadventure.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dispatch table for clientbound payloads, so common code never names a client class (not even in a lambda or method
 * reference: a dedicated server must never load one). {@link ModNetwork} registers {@link #forward}; the client
 * entrypoint fills the table with {@link #set} during mod construction, before any connection exists.
 * <p>
 * Pattern for every clientbound payload: payload + codec in {@code network/}, handler in {@code client/network/},
 * {@code set} in {@code CorruptedAdventureClient}.
 */
public final class ClientboundHandlers {
    private static final Map<CustomPacketPayload.Type<?>, IPayloadHandler<?>> HANDLERS = new ConcurrentHashMap<>();
    private static final Set<CustomPacketPayload.Type<?>> WARNED = ConcurrentHashMap.newKeySet();

    private ClientboundHandlers() {
    }

    public static <T extends CustomPacketPayload> void set(CustomPacketPayload.Type<T> type, IPayloadHandler<T> handler) {
        HANDLERS.put(type, handler);
    }

    /** Handler that looks up the client's handler on receipt; without one the payload is ignored (warned once). */
    static <T extends CustomPacketPayload> IPayloadHandler<T> forward(CustomPacketPayload.Type<T> type) {
        return (payload, context) -> {
            @SuppressWarnings("unchecked") // set() only pairs a type with a handler of that type
            IPayloadHandler<T> handler = (IPayloadHandler<T>) HANDLERS.get(type);
            if (handler != null) {
                handler.handle(payload, context);
            } else if (WARNED.add(type)) {
                CorruptedAdventure.LOGGER.warn("No client handler for payload {}; ignoring it", type.id());
            }
        };
    }
}
