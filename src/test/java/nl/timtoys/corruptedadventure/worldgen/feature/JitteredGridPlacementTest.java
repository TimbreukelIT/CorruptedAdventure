package nl.timtoys.corruptedadventure.worldgen.feature;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

    @Test
    void densityIsPureInSeedAndPosition() {
        ClusterDensity density = ClusterDensity.of(GROVES, 0.0, 0.3);
        JitteredGridPlacement grid = JitteredGridPlacement.of(8, 2, 0.9F, density);
        NormalNoise a = groves(20260930L);
        NormalNoise b = groves(20260930L);
        List<BlockPos> all = new ArrayList<>();
        for (int n = 0; n < CHUNKS; n++) {
            int cx = n % 40 - 20;
            int cz = n / 40 - 12;
            List<BlockPos> first = candidates(grid, cx, cz, share(density, a));
            assertEquals(first, candidates(grid, cx, cz, share(density, b)), "same seed, same chunk");
            // Every kept candidate is one of the grid's own (the density only thins them out).
            assertTrue(candidates(JitteredGridPlacement.of(8, 2, 0.9F), cx, cz).containsAll(first));
            all.addAll(first);
        }
        assertTrue(!all.isEmpty());
        List<BlockPos> salted = new ArrayList<>();
        ClusterDensity other = density.withSalt(1);
        for (int n = 0; n < CHUNKS; n++) {
            salted.addAll(candidates(JitteredGridPlacement.of(8, 2, 0.9F, other), n % 40 - 20, n / 40 - 12, share(other, a)));
        }
        assertNotEquals(all, salted, "another salt reads another field");
    }

    @Test
    void densityGroupsTheCellsIntoGrovesAndOpenGround() {
        ClusterDensity density = ClusterDensity.of(GROVES, 0.0, 0.3);
        NormalNoise noise = groves(20260930L);
        double[] values = new double[64 * 64];
        // Cells of 8 over 32 × 32 chunks: kept with chance 1 · share.
        JitteredGridPlacement grid = JitteredGridPlacement.of(8, 2, 1.0F, density);
        Set<Long> kept = new HashSet<>();
        for (int cz = 0; cz < 32; cz++) {
            for (int cx = 0; cx < 32; cx++) {
                for (BlockPos pos : candidates(grid, cx, cz, share(density, noise))) {
                    kept.add(cell(pos.getX() >> 3, pos.getZ() >> 3));
                }
            }
        }
        for (int i = 0; i < values.length; i++) {
            values[i] = density.value(noise, (i % 64) * 8 + 4, (i / 64) * 8 + 4);
        }
        Arrays.sort(values);
        System.out.printf("ring_4_groves at cell centres: p10 %.2f p25 %.2f p50 %.2f p75 %.2f p90 %.2f%n",
                values[values.length / 10], values[values.length / 4], values[values.length / 2], values[values.length * 3 / 4],
                values[values.length * 9 / 10]);
        double share = kept.size() / (64.0 * 64.0);
        int pairs = 0;
        int both = 0;
        for (int z = 0; z < 64; z++) {
            for (int x = 0; x < 63; x++) {
                if (kept.contains(cell(x, z))) {
                    pairs++;
                    both += kept.contains(cell(x + 1, z)) ? 1 : 0;
                }
            }
        }
        double together = both / (double) pairs;
        System.out.printf("kept %.2f of the cells, a kept cell's neighbour kept %.2f%n", share, together);
        assertTrue(share > 0.15 && share < 0.65, "kept " + share);
        // Groves: a kept cell's neighbour is kept far more often than any cell; open ground: whole empty stretches.
        assertTrue(together > share * 1.3, "neighbour " + together + " against " + share);
        int empty = 0;
        for (int z = 0; z < 64; z += 4) {
            for (int x = 0; x < 64; x += 4) {
                boolean none = true;
                for (int dz = 0; dz < 4 && none; dz++) {
                    for (int dx = 0; dx < 4 && none; dx++) {
                        none = !kept.contains(cell(x + dx, z + dz));
                    }
                }
                empty += none ? 1 : 0;
            }
        }
        assertTrue(empty >= 10, "open stretches of 32 × 32 blocks: " + empty);
    }

    @Test
    void densityCodecRoundTrip() {
        JitteredGridPlacement grid = JitteredGridPlacement.of(4, 1, 0.5F, ClusterDensity.of(GROVES, 0.2, -0.1).withSalt(3).withFloor(0.1));
        JsonElement json = JitteredGridPlacement.CODEC.codec().encodeStart(JsonOps.INSTANCE, grid).getOrThrow();
        JitteredGridPlacement back = JitteredGridPlacement.CODEC.codec().parse(JsonOps.INSTANCE, json).getOrThrow();
        NormalNoise noise = groves(5L);
        ClusterDensity density = ClusterDensity.of(GROVES, 0.2, -0.1).withSalt(3).withFloor(0.1);
        assertEquals(candidates(grid, 3, 4, share(density, noise)), candidates(back, 3, 4, share(density, noise)));
        assertTrue(parse("{\"cell_size\": 8, \"density\": {\"noise\": \"corruptedadventure:ring_4_groves\", \"low\": 0.1, \"high\": 0.1}}").isError(),
                "low and high must differ");
        // A falling ramp: the open ground of the same noise.
        assertEquals(1.0, density.share(-0.5), 1e-9);
        assertEquals(0.1, density.share(0.5), 1e-9);
    }

    private static final ResourceKey<NormalNoise.NoiseParameters> GROVES = ResourceKey.create(Registries.NOISE,
            CorruptedAdventure.id("ring_4_groves"));

    private static NormalNoise groves(long seed) {
        return NormalNoise.create(new LegacyRandomSource(seed), WorldgenData.noiseParameters("ring_4_groves"));
    }

    private static JitteredGridPlacement.CellShare share(ClusterDensity density, NormalNoise noise) {
        return (x, z) -> density.share(noise, x, z);
    }

    private static long cell(int x, int z) {
        return (long) x << 32 | (z & 0xFFFFFFFFL);
    }

    private static List<BlockPos> candidates(JitteredGridPlacement grid, int cx, int cz, JitteredGridPlacement.CellShare share) {
        RandomSource random = RandomSource.create(cx * 341873128712L + cz * 132897987541L);
        return grid.candidates(cx * 16, 64, cz * 16, random, share);
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
