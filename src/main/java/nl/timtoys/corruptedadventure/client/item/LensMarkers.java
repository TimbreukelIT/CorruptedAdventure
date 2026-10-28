package nl.timtoys.corruptedadventure.client.item;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import nl.timtoys.corruptedadventure.network.LensScanPayload;
import nl.timtoys.corruptedadventure.network.LensScanPayload.MissingChunk;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The outlines of the last sky glass lens scan the server sent ({@link LensScanPayload}), as line segments per kind
 * ({@link LensOutline}), for the level they came in. An empty payload, another level or logging out clears them.
 */
final class LensMarkers {
    private static final int[] NONE = new int[0];

    private static int[] glitches = NONE;
    private static int[] holes = NONE;
    private static int[] chunks = NONE;
    @Nullable
    private static ClientLevel level;

    private LensMarkers() {
    }

    static void set(LensScanPayload payload) {
        if (payload.isEmpty()) {
            clear();
            return;
        }
        glitches = LensOutline.union(payload.glitches());
        holes = LensOutline.union(payload.holes());
        List<int[]> boxes = new ArrayList<>();
        int size = 0;
        for (MissingChunk chunk : payload.missingChunks()) {
            int x0 = chunk.chunkX() << 4;
            int z0 = chunk.chunkZ() << 4;
            int[] box = LensOutline.box(x0, chunk.bottom(), z0, x0 + 16, chunk.top(), z0 + 16);
            boxes.add(box);
            size += box.length;
        }
        int[] all = new int[size];
        int at = 0;
        for (int[] box : boxes) {
            System.arraycopy(box, 0, all, at, box.length);
            at += box.length;
        }
        chunks = all;
        level = Minecraft.getInstance().level;
    }

    static void clear() {
        glitches = NONE;
        holes = NONE;
        chunks = NONE;
        level = null;
    }

    /** Whether there is anything to draw in {@code current} (markers of another level count as none). */
    static boolean any(@Nullable ClientLevel current) {
        return current != null && current == level && (glitches.length > 0 || holes.length > 0 || chunks.length > 0);
    }

    static int[] glitches() {
        return glitches;
    }

    static int[] holes() {
        return holes;
    }

    static int[] chunks() {
        return chunks;
    }
}
