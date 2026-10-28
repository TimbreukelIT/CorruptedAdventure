package nl.timtoys.corruptedadventure.client.item;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LensOutlineTest {
    @Test
    void oneBlockIsItsTwelveEdges() {
        int[] lines = LensOutline.union(List.of(new BlockPos(3, -4, 5)));
        assertEquals(12, lines.length / 6);
        assertEquals(segments(LensOutline.box(3, -4, 5, 4, -3, 6)), segments(lines));
    }

    @Test
    void neighboursShareNoFaceLines() {
        // 2 × 1 × 1: the long edges as two pieces each (8), the two ends (4 + 4), nothing on the shared face.
        assertEquals(16, LensOutline.union(List.of(new BlockPos(0, 0, 0), new BlockPos(1, 0, 0))).length / 6);
        // A full 2 × 2 × 2 cube: 12 edges of length 2, no inner lines.
        List<BlockPos> cube = BlockPos.betweenClosedStream(0, 0, 0, 1, 1, 1).map(BlockPos::immutable).toList();
        assertEquals(24, LensOutline.union(cube).length / 6);
    }

    @Test
    void blocksTouchingAlongAnEdgeKeepThatEdgeOnce() {
        int[] lines = LensOutline.union(List.of(new BlockPos(0, 0, 0), new BlockPos(1, 1, 0)));
        assertEquals(23, lines.length / 6);
        assertTrue(segments(lines).contains(List.of(1, 1, 0, 1, 1, 1)), "the shared edge along z");
    }

    @Test
    void theDampingFadesInAndOutInFiveTicks() {
        float scale = 1.0F;
        for (int tick = 0; tick < 5; tick++) {
            scale = LensView.nextScale(scale, true);
        }
        assertEquals(LensView.DAMPED, scale, 1e-6F);
        assertEquals(LensView.DAMPED, LensView.nextScale(scale, true), 1e-6F);
        for (int tick = 0; tick < 5; tick++) {
            scale = LensView.nextScale(scale, false);
        }
        assertEquals(1.0F, scale, 1e-6F);
    }

    private static Set<List<Integer>> segments(int[] lines) {
        Set<List<Integer>> set = new HashSet<>();
        for (int i = 0; i < lines.length; i += 6) {
            set.add(List.of(lines[i], lines[i + 1], lines[i + 2], lines[i + 3], lines[i + 4], lines[i + 5]));
        }
        return set;
    }
}
