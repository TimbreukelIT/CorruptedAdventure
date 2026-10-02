package nl.timtoys.corruptedadventure.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

/**
 * Server → one player: a wind gust of a sandstorm, a horizontal impulse in blocks per tick that the client adds to its
 * own movement (like vanilla's explosion knockback). Sent every {@code sandstormGustTicks} to each exposed player in
 * ring 2, only during a sandstorm.
 */
public record WindGustPayload(float x, float z) implements CustomPacketPayload {
    public static final Type<WindGustPayload> TYPE = new Type<>(CorruptedAdventure.id("wind_gust"));

    public static final StreamCodec<ByteBuf, WindGustPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, WindGustPayload::x,
            ByteBufCodecs.FLOAT, WindGustPayload::z,
            WindGustPayload::new);

    @Override
    public Type<WindGustPayload> type() {
        return TYPE;
    }
}
