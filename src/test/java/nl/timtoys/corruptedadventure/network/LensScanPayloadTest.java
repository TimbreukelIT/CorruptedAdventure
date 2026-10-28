package nl.timtoys.corruptedadventure.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.VarInt;
import nl.timtoys.corruptedadventure.network.LensScanPayload.MissingChunk;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LensScanPayloadTest {
    @Test
    void roundTripKeepsEveryMarker() {
        for (LensScanPayload payload : List.of(
                LensScanPayload.NONE,
                new LensScanPayload(List.of(new BlockPos(4100, -12, -3300)), List.of(), List.of()),
                new LensScanPayload(List.of(new BlockPos(1, 2, 3), new BlockPos(-5000, 150, 4800)),
                        List.of(new BlockPos(4800, -31, 12), new BlockPos(4801, -31, 12)),
                        List.of(new MissingChunk(300, -7, -64, 71), new MissingChunk(-301, 0, -64, -60))))) {
            ByteBuf buffer = Unpooled.buffer();
            LensScanPayload.STREAM_CODEC.encode(buffer, payload);
            assertEquals(payload, LensScanPayload.STREAM_CODEC.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        }
    }

    @Test
    void fullListsRoundTripAndLongerOnesAreRefused() {
        LensScanPayload full = new LensScanPayload(blocks(LensScanPayload.MAX_GLITCHES), blocks(LensScanPayload.MAX_HOLES),
                IntStream.range(0, LensScanPayload.MAX_CHUNKS).mapToObj(i -> new MissingChunk(i, -i, -64, 80)).toList());
        ByteBuf buffer = Unpooled.buffer();
        LensScanPayload.STREAM_CODEC.encode(buffer, full);
        assertEquals(full, LensScanPayload.STREAM_CODEC.decode(buffer));

        ByteBuf longer = Unpooled.buffer();
        VarInt.write(longer, LensScanPayload.MAX_GLITCHES + 1);
        assertThrows(DecoderException.class, () -> LensScanPayload.STREAM_CODEC.decode(longer));
    }

    @Test
    void onlyTheEmptyPayloadIsEmptyAndItKeepsItsOwnCopy() {
        assertTrue(LensScanPayload.NONE.isEmpty());
        List<MissingChunk> chunks = new ArrayList<>(List.of(new MissingChunk(1, 1, -64, 70)));
        LensScanPayload payload = new LensScanPayload(List.of(), List.of(), chunks);
        chunks.clear();
        assertFalse(payload.isEmpty());
        assertEquals(1, payload.missingChunks().size());
    }

    private static List<BlockPos> blocks(int count) {
        return IntStream.range(0, count).mapToObj(i -> new BlockPos(4000 + i % 64, -64 + i / 64, -i)).toList();
    }
}
