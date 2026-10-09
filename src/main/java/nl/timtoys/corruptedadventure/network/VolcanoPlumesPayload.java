package nl.timtoys.corruptedadventure.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

import java.util.List;

/**
 * Server → one player: the active volcanoes near them (package 50b), so the client can draw their smoke plumes. The
 * client knows neither the seed nor the volcano positions; the server reads them from the pure worldgen decisions and
 * sends a new list only when it changes (an empty list clears the plumes). At most {@link #MAX_PLUMES}: the nearest,
 * sorted by x and z (a stable order, so walking past the same volcanoes sends nothing new).
 */
public record VolcanoPlumesPayload(List<Plume> plumes) implements CustomPacketPayload {
    public static final Type<VolcanoPlumesPayload> TYPE = new Type<>(CorruptedAdventure.id("volcano_plumes"));
    /** Cap of one payload; the decoder refuses longer lists. */
    public static final int MAX_PLUMES = 24;

    public static final StreamCodec<ByteBuf, VolcanoPlumesPayload> STREAM_CODEC = Plume.STREAM_CODEC
            .apply(ByteBufCodecs.list(MAX_PLUMES))
            .map(VolcanoPlumesPayload::new, VolcanoPlumesPayload::plumes);

    public VolcanoPlumesPayload {
        plumes = List.copyOf(plumes);
    }

    @Override
    public Type<VolcanoPlumesPayload> type() {
        return TYPE;
    }

    /**
     * One plume: the crater centre ({@code x}, {@code z}), the height it rises from ({@code y}, the crater rim), the
     * crater radius (width of the plume's foot) and whether it is a great horizon volcano (a taller, thicker plume).
     */
    public record Plume(int x, int z, int y, int radius, boolean great) {
        public static final StreamCodec<ByteBuf, Plume> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Plume::x,
                ByteBufCodecs.VAR_INT, Plume::z,
                ByteBufCodecs.VAR_INT, Plume::y,
                ByteBufCodecs.VAR_INT, Plume::radius,
                ByteBufCodecs.BOOL, Plume::great,
                Plume::new);
    }
}
