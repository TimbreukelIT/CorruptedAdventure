package nl.timtoys.corruptedadventure.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.condition.Condition;

import java.util.Arrays;

/**
 * Server → client, only to the player it belongs to: all of that player's meters, rounded to 0–100, one byte each in
 * {@link Condition#values()} order. Always a full snapshot (no deltas), so a missed packet cannot leave the HUD wrong.
 */
public record ConditionSyncPayload(int[] values) implements CustomPacketPayload {
    public static final Type<ConditionSyncPayload> TYPE = new Type<>(CorruptedAdventure.id("condition_sync"));

    private static final int COUNT = Condition.values().length;

    public static final StreamCodec<ByteBuf, ConditionSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                for (int value : payload.values) {
                    buffer.writeByte(value);
                }
            },
            buffer -> {
                int[] values = new int[COUNT];
                for (int i = 0; i < COUNT; i++) {
                    values[i] = buffer.readUnsignedByte();
                }
                return new ConditionSyncPayload(values);
            });

    public ConditionSyncPayload {
        if (values.length != COUNT || Arrays.stream(values).anyMatch(value -> value < 0 || value > 100)) {
            throw new IllegalArgumentException("Expected " + COUNT + " meters of 0-100: " + Arrays.toString(values));
        }
        values = values.clone();
    }

    @Override
    public int[] values() {
        return values.clone();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ConditionSyncPayload payload && Arrays.equals(values, payload.values);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }

    @Override
    public String toString() {
        return "ConditionSyncPayload" + Arrays.toString(values);
    }

    @Override
    public Type<ConditionSyncPayload> type() {
        return TYPE;
    }
}
