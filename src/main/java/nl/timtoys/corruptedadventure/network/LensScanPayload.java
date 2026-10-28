package nl.timtoys.corruptedadventure.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

import java.util.List;

/**
 * Server → the one player looking through a sky glass lens (phase 2 golf 5, §7.2): the hidden glitches round them, about
 * once a second while they look; an empty payload when they stop ({@link #NONE}). The server decides what is shown; the
 * client only draws it. Each list is capped (the decoder refuses longer ones); the server keeps the nearest.
 *
 * @param glitches      the glitch blocks
 * @param holes         one block per column of a void hole under the ground, at the floor's height next to it
 * @param missingChunks the clean missing chunks
 */
public record LensScanPayload(List<BlockPos> glitches, List<BlockPos> holes, List<MissingChunk> missingChunks)
        implements CustomPacketPayload {
    public static final Type<LensScanPayload> TYPE = new Type<>(CorruptedAdventure.id("lens_scan"));
    public static final int MAX_GLITCHES = 512;
    public static final int MAX_HOLES = 256;
    public static final int MAX_CHUNKS = 16;
    public static final LensScanPayload NONE = new LensScanPayload(List.of(), List.of(), List.of());

    public static final StreamCodec<ByteBuf, LensScanPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_GLITCHES)), LensScanPayload::glitches,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_HOLES)), LensScanPayload::holes,
            MissingChunk.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_CHUNKS)), LensScanPayload::missingChunks,
            LensScanPayload::new);

    public LensScanPayload {
        glitches = List.copyOf(glitches);
        holes = List.copyOf(holes);
        missingChunks = List.copyOf(missingChunks);
    }

    public boolean isEmpty() {
        return glitches.isEmpty() && holes.isEmpty() && missingChunks.isEmpty();
    }

    @Override
    public Type<LensScanPayload> type() {
        return TYPE;
    }

    /** A missing chunk (chunk coordinates) from the world's bottom up to the ground round it ({@code top}, exclusive). */
    public record MissingChunk(int chunkX, int chunkZ, int bottom, int top) {
        public static final StreamCodec<ByteBuf, MissingChunk> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, MissingChunk::chunkX,
                ByteBufCodecs.VAR_INT, MissingChunk::chunkZ,
                ByteBufCodecs.VAR_INT, MissingChunk::bottom,
                ByteBufCodecs.VAR_INT, MissingChunk::top,
                MissingChunk::new);
    }
}
