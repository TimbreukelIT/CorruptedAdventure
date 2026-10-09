package nl.timtoys.corruptedadventure.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.VarInt;
import nl.timtoys.corruptedadventure.network.VolcanoPlumesPayload.Plume;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VolcanoPlumesPayloadTest {
    @Test
    void roundTripKeepsEveryPlumeInOrder() {
        for (List<Plume> plumes : List.of(
                List.<Plume>of(),
                List.of(new Plume(4800, -12, 212, 18, true)),
                List.of(new Plume(-5120, 3300, 140, 9, false), new Plume(0, 0, -64, 0, true), new Plume(4110, 672, 225, 30, false)))) {
            VolcanoPlumesPayload payload = new VolcanoPlumesPayload(plumes);
            ByteBuf buffer = Unpooled.buffer();
            VolcanoPlumesPayload.STREAM_CODEC.encode(buffer, payload);
            assertEquals(payload, VolcanoPlumesPayload.STREAM_CODEC.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        }
    }

    @Test
    void fullListRoundTrips() {
        VolcanoPlumesPayload payload = new VolcanoPlumesPayload(plumes(VolcanoPlumesPayload.MAX_PLUMES));
        ByteBuf buffer = Unpooled.buffer();
        VolcanoPlumesPayload.STREAM_CODEC.encode(buffer, payload);
        assertEquals(payload, VolcanoPlumesPayload.STREAM_CODEC.decode(buffer));
    }

    @Test
    void longerListIsRefused() {
        ByteBuf buffer = Unpooled.buffer();
        VarInt.write(buffer, VolcanoPlumesPayload.MAX_PLUMES + 1);
        assertThrows(DecoderException.class, () -> VolcanoPlumesPayload.STREAM_CODEC.decode(buffer));
        assertThrows(EncoderException.class, () -> VolcanoPlumesPayload.STREAM_CODEC.encode(Unpooled.buffer(),
                new VolcanoPlumesPayload(plumes(VolcanoPlumesPayload.MAX_PLUMES + 1))));
    }

    @Test
    void payloadKeepsItsOwnCopy() {
        List<Plume> plumes = new ArrayList<>(plumes(2));
        VolcanoPlumesPayload payload = new VolcanoPlumesPayload(plumes);
        plumes.clear();
        assertEquals(2, payload.plumes().size());
    }

    private static List<Plume> plumes(int count) {
        return IntStream.range(0, count).mapToObj(i -> new Plume(4000 + i * 100, -i * 50, 180 + i, 10 + i, i % 3 == 0)).toList();
    }
}
