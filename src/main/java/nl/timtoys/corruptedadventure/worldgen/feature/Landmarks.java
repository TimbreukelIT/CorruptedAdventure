package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

/**
 * The rare eye-catchers of ring 4 (decor rollout group A, 2026-10-19: crystal spires, root arches, the ancient swamp
 * tree): at most one per cell of a world-fixed grid, its place and shape a pure function of the world seed and the cell.
 * A cell draws up to {@code tries} candidate anchors from its own random and keeps the first one the caller accepts
 * (biome, terrain: pure as well), so every chunk that asks gets the same landmark without reading another chunk's blocks.
 * <p>
 * Callers that write only into their own chunk ({@link CrystalSpireFeature}, {@link RootArchFeature}) ask for every
 * landmark that reaches the chunk ({@link #reaching}); a landmark that grows like a tree ({@link AncientSwampTreeFeature})
 * is written whole from the chunk that holds its anchor ({@link #anchoredIn}).
 */
public final class Landmarks {
    /**
     * A grid of landmark cells.
     *
     * @param cellSize blocks per cell (one landmark at most)
     * @param chance   the share of cells that try at all
     * @param tries    candidate anchors per cell, the first accepted one wins
     * @param margin   keeps an anchor this far inside its cell
     */
    public record Grid(int cellSize, float chance, int tries, int margin) {
        public static final Codec<Grid> CODEC = RecordCodecBuilder.<Grid>create(instance -> instance.group(
                Codec.intRange(32, 1024).fieldOf("cell_size").forGetter(Grid::cellSize),
                Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(Grid::chance),
                Codec.intRange(1, 16).fieldOf("tries").forGetter(Grid::tries),
                Codec.intRange(0, 64).fieldOf("margin").forGetter(Grid::margin)
        ).apply(instance, Grid::new)).validate(Grid::validate);

        private static DataResult<Grid> validate(Grid grid) {
            return 2 * grid.margin < grid.cellSize ? DataResult.success(grid)
                    : DataResult.error(() -> "landmark grid: margin " + grid.margin + " leaves no room in a cell of " + grid.cellSize);
        }
    }

    /** A candidate: the anchor column and the random that shapes the landmark there. */
    record Candidate(int x, int z, RandomSource random) {
    }

    private Landmarks() {
    }

    /**
     * The landmark of cell ({@code cellX}, {@code cellZ}): the first of its candidates {@code accept} takes, or null.
     * The candidate's random has drawn nothing yet when it reaches {@code accept} and the caller.
     */
    static Candidate inCell(long seed, long salt, Grid grid, int cellX, int cellZ, Predicate<Candidate> accept) {
        RandomSource cell = new XoroshiroRandomSource(RandomSupport.mixStafford13(seed ^ salt),
                RandomSupport.mixStafford13(ChunkPos.asLong(cellX, cellZ)));
        if (cell.nextFloat() >= grid.chance()) {
            return null;
        }
        int span = grid.cellSize() - 2 * grid.margin();
        for (int i = 0; i < grid.tries(); i++) {
            int x = cellX * grid.cellSize() + grid.margin() + cell.nextInt(span);
            int z = cellZ * grid.cellSize() + grid.margin() + cell.nextInt(span);
            long shape = cell.nextLong();
            Candidate candidate = new Candidate(x, z, new XoroshiroRandomSource(shape, RandomSupport.mixStafford13(shape)));
            if (accept.test(candidate)) {
                return new Candidate(x, z, new XoroshiroRandomSource(shape, RandomSupport.mixStafford13(shape)));
            }
        }
        return null;
    }

    /** The landmarks whose anchor lies within {@code reach} blocks (per axis) of the chunk {@code chunk}. */
    static List<Candidate> reaching(Object world, long seed, long salt, Grid grid, ChunkPos chunk, int reach, Predicate<Candidate> accept) {
        List<Candidate> found = new ArrayList<>();
        int minX = chunk.getMinBlockX() - reach;
        int maxX = chunk.getMaxBlockX() + reach;
        int minZ = chunk.getMinBlockZ() - reach;
        int maxZ = chunk.getMaxBlockZ() + reach;
        for (int cz = Math.floorDiv(minZ, grid.cellSize()); cz <= Math.floorDiv(maxZ, grid.cellSize()); cz++) {
            for (int cx = Math.floorDiv(minX, grid.cellSize()); cx <= Math.floorDiv(maxX, grid.cellSize()); cx++) {
                // Cheap first: is any candidate of the cell near the chunk at all?
                if (!anyCandidateWithin(seed, salt, grid, cx, cz, minX, minZ, maxX, maxZ)) {
                    continue;
                }
                Candidate landmark = cachedInCell(world, seed, salt, grid, cx, cz, accept);
                if (landmark != null && landmark.x() >= minX && landmark.x() <= maxX && landmark.z() >= minZ && landmark.z() <= maxZ) {
                    found.add(landmark);
                }
            }
        }
        return found;
    }

    /**
     * {@link #inCell}, remembering which try won per cell: the chunks around a landmark all ask for it, and the test of
     * a try (terrain and biome far from the chunk) is the costly part. The answer is pure, so remembering it changes
     * nothing but the time. A caller passes one {@code accept} per {@code salt}, and its world's random state as
     * {@code world}.
     */
    static Candidate cachedInCell(Object world, long seed, long salt, Grid grid, int cellX, int cellZ, Predicate<Candidate> accept) {
        CellKey key = new CellKey(seed, salt, grid, cellX, cellZ);
        Map<CellKey, Integer> winners = WINNERS.computeIfAbsent(world, w -> Collections.synchronizedMap(new LinkedHashMap<>(256, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<CellKey, Integer> eldest) {
                return size() > 4096;
            }
        }));
        Integer known = winners.get(key);
        if (known == null) {
            int[] tried = {-1};
            int[] won = {-1};
            inCell(seed, salt, grid, cellX, cellZ, candidate -> {
                tried[0]++;
                if (accept.test(candidate)) {
                    won[0] = tried[0];
                    return true;
                }
                return false;
            });
            known = won[0];
            winners.put(key, known);
        }
        if (known < 0) {
            return null;
        }
        int index = known;
        int[] at = {-1};
        return inCell(seed, salt, grid, cellX, cellZ, candidate -> ++at[0] == index);
    }

    private record CellKey(long seed, long salt, Grid grid, int cellX, int cellZ) {
    }

    /**
     * The winning try per cell, per world (the level's random state, held weakly: a new world with the same seed but other
     * ring settings asks again), the most recent few thousand cells (all threads).
     */
    private static final Map<Object, Map<CellKey, Integer>> WINNERS = Collections.synchronizedMap(new WeakHashMap<>());

    /** The landmark whose anchor lies in the chunk {@code chunk}, or null. */
    static Candidate anchoredIn(Object world, long seed, long salt, Grid grid, ChunkPos chunk, Predicate<Candidate> accept) {
        List<Candidate> found = reaching(world, seed, salt, grid, chunk, 0, accept);
        return found.isEmpty() ? null : found.getFirst();
    }

    private static boolean anyCandidateWithin(long seed, long salt, Grid grid, int cellX, int cellZ, int minX, int minZ, int maxX, int maxZ) {
        boolean[] near = new boolean[1];
        inCell(seed, salt, grid, cellX, cellZ, candidate -> {
            near[0] |= candidate.x() >= minX && candidate.x() <= maxX && candidate.z() >= minZ && candidate.z() <= maxZ;
            return false;
        });
        return near[0];
    }

    /**
     * The biome of column x, z at the sea level, straight from the biome source (the call that fills the chunks'
     * biomes): pure, also for columns outside the region a feature may read.
     */
    static Holder<Biome> biome(WorldGenLevel level, int x, int z) {
        ServerChunkCache chunks = level.getLevel().getChunkSource();
        return chunks.getGenerator().getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(level.getSeaLevel()),
                QuartPos.fromBlock(z), chunks.randomState().sampler());
    }

    /** Whether the column and eight columns {@code radius} around it all lie in {@code biome}. */
    static boolean biomeAround(WorldGenLevel level, int x, int z, int radius, ResourceKey<Biome> biome) {
        if (!biome(level, x, z).is(biome)) {
            return false;
        }
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0;
            if (!biome(level, x + (int) Math.round(Math.cos(angle) * radius), z + (int) Math.round(Math.sin(angle) * radius)).is(biome)) {
                return false;
            }
        }
        return true;
    }

    /**
     * The biome of column x, z at the sea level as the chunk stores it (the same value as {@link #biome}, but cheap): only
     * for columns in the region a feature may read (its chunk and the eight around it).
     */
    static Holder<Biome> storedBiome(WorldGenLevel level, int x, int z) {
        return level.getChunk(x >> 4, z >> 4).getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(level.getSeaLevel()),
                QuartPos.fromBlock(z));
    }

    /** The cache owner of {@link #cachedInCell}: the level's random state (one per world and dimension). */
    static Object world(WorldGenLevel level) {
        return level.getLevel().getChunkSource().randomState();
    }

    /** Whether the four columns {@code margin} away (and the column itself) lie in {@code biome}: the stored biomes. */
    static boolean storedBiomeAround(WorldGenLevel level, int x, int z, int margin, Predicate<Holder<Biome>> biome) {
        return biome.test(storedBiome(level, x, z)) && biome.test(storedBiome(level, x + margin, z))
                && biome.test(storedBiome(level, x - margin, z)) && biome.test(storedBiome(level, x, z + margin))
                && biome.test(storedBiome(level, x, z - margin));
    }

    /** Whether {@code pos} lies in the chunk {@code chunk}. */
    static boolean inChunk(BlockPos pos, ChunkPos chunk) {
        return pos.getX() >> 4 == chunk.x && pos.getZ() >> 4 == chunk.z;
    }
}
