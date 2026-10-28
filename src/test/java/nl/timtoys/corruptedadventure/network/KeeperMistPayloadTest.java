package nl.timtoys.corruptedadventure.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeeperMistPayloadTest {
    @Test
    void roundTripKeepsEveryField() {
        KeeperMistPayload payload = new KeeperMistPayload(123456, -4123.25, 87.5, 3999.75, 40.0F, 24.0F, 14.0F, 0.6F);
        ByteBuf buffer = Unpooled.buffer();
        KeeperMistPayload.STREAM_CODEC.encode(buffer, payload);
        assertEquals(payload, KeeperMistPayload.STREAM_CODEC.decode(buffer));
        assertEquals(0, buffer.readableBytes());
    }
}
