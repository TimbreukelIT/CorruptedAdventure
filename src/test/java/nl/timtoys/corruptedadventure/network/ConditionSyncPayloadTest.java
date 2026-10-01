package nl.timtoys.corruptedadventure.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import nl.timtoys.corruptedadventure.condition.Condition;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConditionSyncPayloadTest {
    private static final int COUNT = Condition.values().length;

    @Test
    void roundTripOneBytePerCondition() {
        for (int value : new int[]{0, 1, 50, 99, 100}) {
            int[] values = new int[COUNT];
            Arrays.fill(values, value);
            ByteBuf buffer = Unpooled.buffer();
            ConditionSyncPayload.STREAM_CODEC.encode(buffer, new ConditionSyncPayload(values));
            assertEquals(COUNT, buffer.readableBytes());
            ConditionSyncPayload received = ConditionSyncPayload.STREAM_CODEC.decode(buffer);
            assertEquals(0, buffer.readableBytes());
            assertArrayEquals(values, received.values());
            assertEquals(new ConditionSyncPayload(values), received);
        }
    }

    @Test
    void rejectsWrongLengthOrRange() {
        assertThrows(IllegalArgumentException.class, () -> new ConditionSyncPayload(new int[COUNT + 1]));
        int[] tooHigh = new int[COUNT];
        tooHigh[0] = 101;
        assertThrows(IllegalArgumentException.class, () -> new ConditionSyncPayload(tooHigh));
    }

    @Test
    void valuesAreCopied() {
        int[] values = new int[COUNT];
        ConditionSyncPayload payload = new ConditionSyncPayload(values);
        values[0] = 50;
        payload.values()[0] = 60;
        assertEquals(0, payload.values()[0]);
    }
}
