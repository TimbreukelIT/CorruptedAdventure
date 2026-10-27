package nl.timtoys.corruptedadventure.worldgen.chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurfaceBiomeManagerTest {
    private static final List<Holder<Biome>> BIOMES = List.of(Holder.direct(biome()), Holder.direct(biome()), Holder.direct(biome()));
    private static final int MIN_Y = -64;
    private static final int MAX_Y = 320;

    /** Blobs of a few quarts with borders on every axis, a layer change in y and single-quart specks. */
    private static Holder<Biome> pattern(int x, int y, int z) {
        if (Math.floorMod(x * 31 + y * 17 + z * 13, 97) == 0) {
            return BIOMES.get(2);
        }
        int blob = Math.floorDiv(x + 3, 5) + Math.floorDiv(z - 1, 3) + (y > 12 ? 1 : 0);
        return BIOMES.get(Math.floorMod(blob, 3));
    }

    @Test
    void everyBlockOfAChunkGetsVanillasBiome() {
        BiomeManager vanilla = new BiomeManager(SurfaceBiomeManagerTest::pattern, BiomeManager.obfuscateSeed(20260930L));
        int fast = 0;
        int total = 0;
        for (int[] chunk : new int[][]{{0, 0}, {-1, -1}, {7, -12}, {-33, 20}}) {
            int minX = chunk[0] * 16;
            int minZ = chunk[1] * 16;
            SurfaceBiomeManager surface = new SurfaceBiomeManager(vanilla, minX, minZ, MIN_Y, MAX_Y);
            for (int x = minX; x < minX + 16; x++) {
                for (int z = minZ; z < minZ + 16; z++) {
                    for (int y = MAX_Y; y >= MIN_Y; y--) {
                        BlockPos pos = new BlockPos(x, y, z);
                        Holder<Biome> expected = vanilla.getBiome(pos);
                        assertSame(expected, surface.getBiome(pos), pos.toString());
                        total++;
                        if (uniform(x, y, z)) {
                            fast++;
                        }
                    }
                }
            }
        }
        // The pattern exercises both paths.
        assertTrue(fast > total / 3 && fast < total, fast + " of " + total);
    }

    @Test
    void blocksOutsideTheChunkStillGetVanillasBiome() {
        BiomeManager vanilla = new BiomeManager(SurfaceBiomeManagerTest::pattern, BiomeManager.obfuscateSeed(1L));
        SurfaceBiomeManager surface = new SurfaceBiomeManager(vanilla, 32, -48, MIN_Y, MAX_Y);
        for (int x = 32 - 24; x < 32 + 40; x += 3) {
            for (int z = -48 - 24; z < -48 + 40; z += 3) {
                for (int y : new int[]{MIN_Y - 9, MIN_Y - 1, MIN_Y, 0, 13, MAX_Y, MAX_Y + 1, MAX_Y + 7}) {
                    BlockPos pos = new BlockPos(x, y, z);
                    assertSame(vanilla.getBiome(pos), surface.getBiome(pos), pos.toString());
                }
            }
        }
    }

    private static boolean uniform(int x, int y, int z) {
        int l = (x - 2) >> 2;
        int m = (y - 2) >> 2;
        int n = (z - 2) >> 2;
        Holder<Biome> biome = pattern(l, m, n);
        for (int corner = 1; corner < 8; corner++) {
            if (pattern(l + (corner >> 2 & 1), m + (corner >> 1 & 1), n + (corner & 1)) != biome) {
                return false;
            }
        }
        return true;
    }

    private static Biome biome() {
        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.5F)
                .downfall(0.0F)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .skyColor(0).fogColor(0).waterColor(0).waterFogColor(0).build())
                .mobSpawnSettings(MobSpawnSettings.EMPTY)
                .generationSettings(BiomeGenerationSettings.EMPTY)
                .build();
    }
}
