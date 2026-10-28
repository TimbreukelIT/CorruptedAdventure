package nl.timtoys.corruptedadventure.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

/**
 * Server → one player: the mist bank The Lost Keeper brings along (phase 4, ring 4), about once a second while the player
 * is within {@code mistSyncRange}; {@code strength} 0 when it leaves, dies or retreats. The server decides where and how
 * thick; the client only draws the fog ({@code client/entity/lostkeeper/KeeperMistFog}).
 *
 * @param keeperId   the keeper's entity id (one bank per keeper)
 * @param x          the bank's centre (the keeper's head)
 * @param radius     blocks of thickest mist round the centre
 * @param fade       blocks beyond the radius over which it thins out
 * @param visibility blocks of view in the thickest mist
 * @param strength   0–1: how far the mist has risen (it rises and sinks smoothly on the server)
 */
public record KeeperMistPayload(int keeperId, double x, double y, double z, float radius, float fade, float visibility,
                                float strength) implements CustomPacketPayload {
    public static final Type<KeeperMistPayload> TYPE = new Type<>(CorruptedAdventure.id("keeper_mist"));

    public static final StreamCodec<ByteBuf, KeeperMistPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                VarInt.write(buffer, payload.keeperId);
                buffer.writeDouble(payload.x);
                buffer.writeDouble(payload.y);
                buffer.writeDouble(payload.z);
                buffer.writeFloat(payload.radius);
                buffer.writeFloat(payload.fade);
                buffer.writeFloat(payload.visibility);
                buffer.writeFloat(payload.strength);
            },
            buffer -> new KeeperMistPayload(VarInt.read(buffer), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                    buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat()));

    @Override
    public Type<KeeperMistPayload> type() {
        return TYPE;
    }
}
