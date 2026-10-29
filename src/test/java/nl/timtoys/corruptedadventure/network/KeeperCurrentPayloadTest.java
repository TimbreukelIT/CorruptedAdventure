package nl.timtoys.corruptedadventure.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeeperCurrentPayloadTest {
    @Test
    void roundTripKeepsEveryField() {
        KeeperCurrentPayload payload = new KeeperCurrentPayload(0.02F, -0.08F, -0.015F);
        ByteBuf buffer = Unpooled.buffer();
        KeeperCurrentPayload.STREAM_CODEC.encode(buffer, payload);
        assertEquals(payload, KeeperCurrentPayload.STREAM_CODEC.decode(buffer));
        assertEquals(0, buffer.readableBytes());
    }
}
