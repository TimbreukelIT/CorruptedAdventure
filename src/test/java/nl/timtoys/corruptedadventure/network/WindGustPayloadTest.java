package nl.timtoys.corruptedadventure.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WindGustPayloadTest {
    @Test
    void roundTripTwoFloats() {
        for (WindGustPayload payload : new WindGustPayload[]{
                new WindGustPayload(0.0F, 0.0F),
                new WindGustPayload(0.056568F, -0.056568F),
                new WindGustPayload(-0.5F, 0.5F)}) {
            ByteBuf buffer = Unpooled.buffer();
            WindGustPayload.STREAM_CODEC.encode(buffer, payload);
            assertEquals(8, buffer.readableBytes());
            assertEquals(payload, WindGustPayload.STREAM_CODEC.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        }
    }
}
