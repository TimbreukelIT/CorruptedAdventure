package nl.timtoys.corruptedadventure.worldgen.feature;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JitteredGridPlacementTest {
    private static final int CHUNKS = 1000;

    @Test
    void candidatesStayInTheirChunkAndApartAcrossChunks() {
        RandomSource pick = RandomSource.create(23);
        for (int cell : new int[]{1, 2, 4, 8, 16}) {
            for (int margin = 0; 2 * margin < cell; margin++) {
                JitteredGridPlacement grid = JitteredGridPlacement.of(cell, margin, 1.0F);
                // Fine grids have many candidates per chunk: fewer chunks keep the pairwise check quick.
                for (int n = 0; n < (cell < 4 ? 5 : CHUNKS / 10); n++) {
                    int cx = pick.nextInt(2000) - 1000;
                    int cz = pick.nextInt(2000) - 1000;
                    List<BlockPos> own = candidates(grid, cx, cz);
                    assertEquals((16 / cell) * (16 / cell), own.size());
                    List<BlockPos> around = new ArrayList<>();
                    for (int dz = -1; dz <= 1; dz++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            around.addAll(candidates(grid, cx + dx, cz + dz));
                        }
                    }
                    for (BlockPos a : own) {
                        assertEquals(cx, a.getX() >> 4);
                        assertEquals(cz, a.getZ() >> 4);
                        for (BlockPos b : around) {
                            if (!a.equals(b)) {
                                int apart = Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getZ() - b.getZ()));
                                assertTrue(apart >= 2 * margin + 1, "cell " + cell + " margin " + margin + ": " + a + " " + b);
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void marginKeepsTheCandidatesOffTheCellBorders() {
        JitteredGridPlacement grid = JitteredGridPlacement.of(8, 2, 1.0F);
        for (int n = 0; n < CHUNKS; n++) {
            for (BlockPos pos : candidates(grid, n, -n)) {
                int x = Math.floorMod(pos.getX(), 8);
                int z = Math.floorMod(pos.getZ(), 8);
                assertTrue(x >= 2 && x <= 5 && z >= 2 && z <= 5, pos.toString());
            }
        }
    }

    @Test
    void sameRandomGivesTheSamePositionsAndChanceThinsThemOut() {
        JitteredGridPlacement grid = JitteredGridPlacement.of(4, 1, 0.25F);
        int kept = 0;
        for (int n = 0; n < CHUNKS; n++) {
            List<BlockPos> first = candidates(grid, n, 7);
            assertEquals(first, candidates(grid, n, 7));
            kept += first.size();
        }
        double share = kept / (CHUNKS * 16.0);
        assertTrue(share > 0.22 && share < 0.28, "kept " + share);
    }

    @Test
    void codecRoundTripAndValidation() {
        JitteredGridPlacement grid = JitteredGridPlacement.of(8, 2, 0.6F);
        JsonElement json = JitteredGridPlacement.CODEC.codec().encodeStart(JsonOps.INSTANCE, grid).getOrThrow();
        JitteredGridPlacement back = JitteredGridPlacement.CODEC.codec().parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(candidates(grid, 3, 4), candidates(back, 3, 4));
        assertTrue(parse("{\"cell_size\": 3}").isError(), "cell size must divide 16");
        assertTrue(parse("{\"cell_size\": 8, \"margin\": 4}").isError(), "no room left");
        assertTrue(parse("{\"cell_size\": 8, \"margin\": 3}").isSuccess());
    }

    private static com.mojang.serialization.DataResult<JitteredGridPlacement> parse(String json) {
        return JitteredGridPlacement.CODEC.codec().parse(JsonOps.INSTANCE, com.google.gson.JsonParser.parseString(json));
    }

    /** As the decoration step: one random per chunk (and feature), origin at the chunk's minimum corner. */
    private static List<BlockPos> candidates(JitteredGridPlacement grid, int cx, int cz) {
        RandomSource random = RandomSource.create(cx * 341873128712L + cz * 132897987541L);
        return grid.candidates(cx * 16, 64, cz * 16, random);
    }
}
