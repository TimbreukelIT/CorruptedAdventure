package nl.timtoys.corruptedadventure.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

/**
 * Server → one player near the place of The Unfinished's arrival (phase 4 step 2): the standstill falls there for a while,
 * only for the players it is sent to; the ring weather itself does not change. Sent about once a second during the omens
 * (so a player who walks in feels it too); {@code ticks} 0 lifts it (the omens were cancelled). The client draws it with the
 * ring 5 standstill's own renderer ({@code client/weather/LocalStandstill}).
 *
 * @param x      the centre of the standstill (the place of the arrival)
 * @param radius blocks round the centre where it is felt in full; it thins out over the last quarter
 * @param ticks  how long from now it holds before it fades
 * @param seed   fixes where the frozen rain streaks hang (the same on every client)
 */
public record UnfinishedStandstillPayload(double x, double z, float radius, int ticks, int seed) implements CustomPacketPayload {
    public static final Type<UnfinishedStandstillPayload> TYPE = new Type<>(CorruptedAdventure.id("unfinished_standstill"));

    public static final StreamCodec<ByteBuf, UnfinishedStandstillPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeDouble(payload.x);
                buffer.writeDouble(payload.z);
                buffer.writeFloat(payload.radius);
                VarInt.write(buffer, payload.ticks);
                buffer.writeInt(payload.seed);
            },
            buffer -> new UnfinishedStandstillPayload(buffer.readDouble(), buffer.readDouble(), buffer.readFloat(),
                    VarInt.read(buffer), buffer.readInt()));

    @Override
    public Type<UnfinishedStandstillPayload> type() {
        return TYPE;
    }
}
