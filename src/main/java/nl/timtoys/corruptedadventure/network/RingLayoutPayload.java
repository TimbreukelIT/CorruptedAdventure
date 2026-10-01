package nl.timtoys.corruptedadventure.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.config.RingSettings;
import nl.timtoys.corruptedadventure.ring.RingLayout;

/**
 * Server → client on login (ring worlds only): everything a client needs to build the same {@link RingLayout} as the
 * server. Carries the noise seed, never the world seed.
 */
public record RingLayoutPayload(RingSettings settings, long noiseSeed) implements CustomPacketPayload {
    public static final Type<RingLayoutPayload> TYPE = new Type<>(CorruptedAdventure.id("ring_layout"));

    // A hash is uniformly random, so a var-long would mostly be longer than 8 bytes; 1.21.1 has no fixed-long codec.
    private static final StreamCodec<ByteBuf, Long> LONG = StreamCodec.of(ByteBuf::writeLong, ByteBuf::readLong);

    public static final StreamCodec<ByteBuf, RingLayoutPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(RingSettings.RING_COUNT)), p -> p.settings().ringOuterRadii(),
            ByteBufCodecs.VAR_INT, p -> p.settings().voidOuterRadius(),
            ByteBufCodecs.VAR_INT, p -> p.settings().boundaryNoise(),
            ByteBufCodecs.VAR_INT, p -> p.settings().transitionWidth(),
            LONG, RingLayoutPayload::noiseSeed,
            (radii, voidOuterRadius, boundaryNoise, transitionWidth, noiseSeed) -> new RingLayoutPayload(
                    new RingSettings(radii, voidOuterRadius, boundaryNoise, transitionWidth), noiseSeed));

    public static RingLayoutPayload of(RingLayout layout) {
        return new RingLayoutPayload(layout.settings(), layout.noiseSeed());
    }

    @Override
    public Type<RingLayoutPayload> type() {
        return TYPE;
    }
}
