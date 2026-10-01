package nl.timtoys.corruptedadventure.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientboundHandlersTest {
    // Own test types, so the tests never touch the real handler table entries.
    private static final CustomPacketPayload.Type<TestPayload> HANDLED = new CustomPacketPayload.Type<>(CorruptedAdventure.id("test_handled"));
    private static final CustomPacketPayload.Type<TestPayload> UNHANDLED = new CustomPacketPayload.Type<>(CorruptedAdventure.id("test_unhandled"));

    @Test
    void forwardCallsTheHandlerThatWasSet() {
        List<TestPayload> received = new ArrayList<>();
        IPayloadHandler<TestPayload> forward = ClientboundHandlers.forward(HANDLED);
        ClientboundHandlers.set(HANDLED, (payload, context) -> received.add(payload));
        TestPayload payload = new TestPayload(HANDLED);
        forward.handle(payload, null);
        assertEquals(List.of(payload), received);
    }

    @Test
    void forwardWithoutHandlerIgnoresThePayload() {
        IPayloadHandler<TestPayload> forward = ClientboundHandlers.forward(UNHANDLED);
        assertDoesNotThrow(() -> forward.handle(new TestPayload(UNHANDLED), null));
        assertDoesNotThrow(() -> forward.handle(new TestPayload(UNHANDLED), null));
    }

    private record TestPayload(Type<TestPayload> type) implements CustomPacketPayload {
    }
}
