package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import nl.timtoys.corruptedadventure.registry.ModBiomes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code corruptedadventure:mirrored_trees} (94, V4-A), once per chunk: the oaks of {@code false_meadow} stand in pairs,
 * each with an exact mirrored twin {@link #MIN_GAP}–{@link #MAX_GAP} blocks away (the same oak, mirrored in the line
 * between them; the leaves' missing corners give it away on a second look). One pair at most per world cell of
 * {@link #CELL} blocks, a pure plan ({@link #pairs}); every chunk writes its own blocks of the pairs that reach into it, so
 * a pair across a chunk border is whole whatever generated first. Both oaks stand on the pure ground of the terrain
 * ({@link Ring5World#top}), both in {@code false_meadow} by the biome source, both clear of the void. No bee nests: the
 * meadow has no life. The leaves are persistent.
 */
public final class MirroredTreesFeature extends Feature<NoneFeatureConfiguration> {
    static final int CELL = 22;
    static final double CHANCE = 0.85;
    static final int MIN_GAP = 6;
    static final int MAX_GAP = 12;
    /** How far a tree's leaves reach from its trunk. */
    static final int CROWN = 2;
    static final int VOID_CLEARANCE = 4;
    private static final long SALT = 0x3177_0EE5L;
    private static final int FLAGS = Block.UPDATE_CLIENTS;
    /** The pairs of the last cells (a chunk asks up to nine; each cell is asked by the chunks round it). */
    private static final Ring5PlanCache<Long, Pair> PAIRS = new Ring5PlanCache<>(1024);

    public MirroredTreesFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /** One pair: its blocks (logs first). */
    record Pair(Map<BlockPos, BlockState> blocks) {
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        Ring5World world = Ring5Terrain.world(level);
        if (world == null) {
            return false;
        }
        Climate.Sampler sampler = level.getLevel().getChunkSource().randomState().sampler();
        BiomeSource biomes = level.getLevel().getChunkSource().getGenerator().getBiomeSource();
        BiomeAt biomeAt = (x, y, z) -> biomes.getNoiseBiome(x >> 2, y >> 2, z >> 2, sampler).is(ModBiomes.FALSE_MEADOW);
        ChunkPos chunk = new ChunkPos(context.origin());
        boolean placed = false;
        for (Pair pair : pairs(world, biomeAt, chunk)) {
            for (Map.Entry<BlockPos, BlockState> block : pair.blocks().entrySet()) {
                BlockPos pos = block.getKey();
                if (pos.getX() >> 4 == chunk.x && pos.getZ() >> 4 == chunk.z) {
                    BlockState existing = level.getBlockState(pos);
                    boolean log = block.getValue().is(Blocks.OAK_LOG);
                    if (Ring5Placements.free(existing) || (log && existing.is(Blocks.OAK_LEAVES))) {
                        level.setBlock(pos, block.getValue(), FLAGS);
                        placed = true;
                    }
                }
            }
        }
        return placed;
    }

    /** Whether the block lies in {@code false_meadow} (the biome source, pure). */
    @FunctionalInterface
    interface BiomeAt {
        boolean meadow(int x, int y, int z);
    }

    /** The pairs whose blocks may reach into {@code chunk} (remembered per world: {@code biomeAt} is the world's own). */
    static List<Pair> pairs(Ring5World world, BiomeAt biomeAt, ChunkPos chunk) {
        int reach = MAX_GAP + CROWN;
        List<Pair> pairs = new ArrayList<>();
        for (int i = Math.floorDiv(chunk.getMinBlockX() - reach, CELL); i <= Math.floorDiv(chunk.getMaxBlockX() + reach, CELL); i++) {
            for (int j = Math.floorDiv(chunk.getMinBlockZ() - reach, CELL); j <= Math.floorDiv(chunk.getMaxBlockZ() + reach, CELL); j++) {
                int ci = i;
                int cj = j;
                Pair pair = PAIRS.get(world, ChunkPos.asLong(i, j), k -> pair(world, biomeAt, ci, cj));
                if (pair != null) {
                    pairs.add(pair);
                }
            }
        }
        return pairs;
    }

    /** The pair of cell (i, j), or null. Pure in the world and the cell. */
    static Pair pair(Ring5World world, BiomeAt biomeAt, int i, int j) {
        RandomSource r = new XoroshiroRandomSource(RandomSupport.mixStafford13(world.salt() ^ SALT), RandomSupport.mixStafford13(((long) i << 32) ^ (j & 0xFFFFFFFFL)));
        if (r.nextDouble() >= CHANCE) {
            return null;
        }
        boolean alongX = r.nextBoolean();
        int gap = MIN_GAP + r.nextInt(MAX_GAP - MIN_GAP + 1);
        int ax = i * CELL + CROWN + r.nextInt(CELL - 2 * CROWN);
        int az = j * CELL + CROWN + r.nextInt(CELL - 2 * CROWN);
        int bx = alongX ? ax + gap : ax;
        int bz = alongX ? az : az + gap;
        long shapeSeed = r.nextLong();
        int ay = groundAbove(world, biomeAt, ax, az);
        int by = groundAbove(world, biomeAt, bx, bz);
        if (ay == Integer.MIN_VALUE || by == Integer.MIN_VALUE) {
            return null;
        }
        Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        List<int[]> oak = oak(new XoroshiroRandomSource(shapeSeed));
        BlockState log = Blocks.OAK_LOG.defaultBlockState();
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (int[] b : oak) {
            BlockState state = b[3] == 1 ? log : leaves;
            blocks.put(new BlockPos(ax + b[0], ay + b[1], az + b[2]), state);
            // The twin: mirrored across the line between the trunks.
            int mx = alongX ? bx - b[0] : bx + b[0];
            int mz = alongX ? bz + b[2] : bz - b[2];
            blocks.put(new BlockPos(mx, by + b[1], mz), state);
        }
        return new Pair(blocks);
    }

    /** The first block over the pure ground at (x, z) for a tree of the meadow, or {@link Integer#MIN_VALUE}. */
    private static int groundAbove(Ring5World world, BiomeAt biomeAt, int x, int z) {
        if (!world.core(x, z) || world.voidDistance(x, z, VOID_CLEARANCE) <= VOID_CLEARANCE || world.openTop(x, z)) {
            return Integer.MIN_VALUE;
        }
        int top = world.top(x, z);
        return biomeAt.meadow(x, top + 1, z) ? top + 1 : Integer.MIN_VALUE;
    }

    /**
     * A vanilla-like oak round its trunk at (0, 0, 0): {@code {dx, dy, dz, isLog}}, the trunk 4–6 high, two wide leaf layers
     * with corners dropped at random and two narrow ones on top. Logs first.
     */
    static List<int[]> oak(RandomSource random) {
        int height = 4 + random.nextInt(3);
        List<int[]> blocks = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            blocks.add(new int[]{0, y, 0, 1});
        }
        for (int y = height - 3; y <= height; y++) {
            int radius = y >= height - 1 ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    boolean corner = Math.abs(dx) == radius && Math.abs(dz) == radius;
                    boolean trunk = dx == 0 && dz == 0 && y < height;
                    if (trunk || (corner && (y == height || random.nextInt(2) == 0))) {
                        continue;
                    }
                    blocks.add(new int[]{dx, y, dz, 0});
                }
            }
        }
        return blocks;
    }
}
