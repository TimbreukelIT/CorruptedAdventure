package nl.timtoys.corruptedadventure.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

/**
 * Server → one player: a push of The Lost Keeper's water (phase 4, ring 4) in blocks per tick, that the client adds to its
 * own movement (like vanilla's explosion knockback and {@link WindGustPayload}, but with a vertical part): sent every tick
 * to a player in the current of its bubble trap, which pulls them down, so swimming sideways still gets them out.
 */
public record KeeperCurrentPayload(float x, float y, float z) implements CustomPacketPayload {
    public static final Type<KeeperCurrentPayload> TYPE = new Type<>(CorruptedAdventure.id("keeper_current"));

    public static final StreamCodec<ByteBuf, KeeperCurrentPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, KeeperCurrentPayload::x,
            ByteBufCodecs.FLOAT, KeeperCurrentPayload::y,
            ByteBufCodecs.FLOAT, KeeperCurrentPayload::z,
            KeeperCurrentPayload::new);

    @Override
    public Type<KeeperCurrentPayload> type() {
        return TYPE;
    }
}
