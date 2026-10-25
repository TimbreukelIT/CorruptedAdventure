package nl.timtoys.corruptedadventure.worldgen.feature;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

/**
 * The pure ring 2 terrain for the rock landmarks (G2 ring 2): the 2D surface height {@code ring_2/overworld/height}
 * (plateau, mesas, canyon floors, flats; without the per-block fine forms of {@code canyon_details}), wired to the
 * world's own noises, and the canyon strata of the surface rules. Ring 2 has no Java terrain field like ring 3 or 4, so
 * this reads the same density function the terrain is built from, at any column, without a chunk.
 */
final class Ring2PureTerrain {
    static final ResourceKey<DensityFunction> HEIGHT = ResourceKey.create(Registries.DENSITY_FUNCTION,
            CorruptedAdventure.id("ring_2/overworld/height"));

    /**
     * The strata of the canyon walls (change 23b), as the surface rules lay them: a period of 48 from Y 8, each band's
     * lower edge raised by the column's surface depth. 0 red, 1 orange, 2 cream.
     */
    private static final byte[] STRATA = strata();
    private static final int STRATA_BASE = 8;

    private static final Map<RandomState, Ring2PureTerrain> WORLDS = Collections.synchronizedMap(new WeakHashMap<>());

    private final DensityFunction height;
    private final NormalNoise surface;

    private Ring2PureTerrain(DensityFunction height, NormalNoise surface) {
        this.height = height;
        this.surface = surface;
    }

    /** The pure terrain of the level's world (wired once per random state). */
    static Ring2PureTerrain of(WorldGenLevel level) {
        ServerLevel server = level.getLevel();
        RandomState state = server.getChunkSource().randomState();
        return WORLDS.computeIfAbsent(state, s -> {
            DensityFunction raw = server.registryAccess().registryOrThrow(Registries.DENSITY_FUNCTION).getHolderOrThrow(HEIGHT).value();
            return new Ring2PureTerrain(raw.mapAll(new Wiring(s)), s.getOrCreateNoise(Noises.SURFACE));
        });
    }

    /** The ring 2 surface height at the block column holding (x, z) (absolute Y, before the fine forms). */
    double height(double x, double z) {
        return height.compute(new DensityFunction.SinglePointContext(Mth.floor(x), 0, Mth.floor(z)));
    }

    /** The surface depth of the column as vanilla's surface system computes it, without its tiny random part. */
    int surfaceDepth(int x, int z) {
        return (int) (surface.getValue(x, 0.0, z) * 2.75 + 3.125);
    }

    /** The canyon stone of the strata at y in this column. */
    BlockState stratum(int x, int y, int z) {
        return stratumBlock(stratumIndex(y - surfaceDepth(x, z)));
    }

    /** 0 red, 1 orange, 2 cream: the band of the strata table at y (the surface depth already taken off). */
    static int stratumIndex(int y) {
        return STRATA[Math.floorMod(y - STRATA_BASE, STRATA.length)];
    }

    static BlockState stratumBlock(int index) {
        return switch (index) {
            case 1 -> ModBlocks.ORANGE_CANYON_STONE.get().defaultBlockState();
            case 2 -> ModBlocks.CREAM_CANYON_STONE.get().defaultBlockState();
            default -> ModBlocks.RED_CANYON_STONE.get().defaultBlockState();
        };
    }

    /** Red 22, orange 12, cream 14 per 48 (the bands of {@code noise_settings/overworld.json}, from their lower edges). */
    private static byte[] strata() {
        int[][] bands = {{0, 0}, {5, 2}, {8, 0}, {12, 1}, {15, 0}, {19, 2}, {23, 0}, {26, 1}, {31, 2}, {34, 0}, {40, 1}, {44, 2}};
        byte[] table = new byte[48];
        for (int i = 0; i < bands.length; i++) {
            int end = i + 1 < bands.length ? bands[i + 1][0] : 48;
            for (int o = bands[i][0]; o < end; o++) {
                table[o] = (byte) bands[i][1];
            }
        }
        return table;
    }

    /** Gives every noise its instance of the world (the same instances the terrain uses: the random state caches them). */
    private record Wiring(RandomState state) implements DensityFunction.Visitor {
        @Override
        public DensityFunction apply(DensityFunction function) {
            return function;
        }

        @Override
        public DensityFunction.NoiseHolder visitNoise(DensityFunction.NoiseHolder noise) {
            return new DensityFunction.NoiseHolder(noise.noiseData(), state.getOrCreateNoise(noise.noiseData().unwrapKey().orElseThrow()));
        }
    }
}
