package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * {@code corruptedadventure:island_vines} (package 75, design ring 4 "Verbindingen": hanging roots and vines between
 * nearby islands): a sagging vine of roots from the facing edge of one island to the other's, for every island pair of
 * package 74 without a rock arch ({@link IslandField#islandPairs}); now and then a second one beside it. Moss hangs
 * under the vine in places, a glowing root now and then.
 * <p>
 * Every chunk draws its own part of each vine ({@link #rope}: a pure function of the pair, so the parts meet across
 * chunk borders) and writes only in its own chunk. A vine block replaces only air and the leaves of a feature tree
 * (never the world tree's persistent leaves, logs, water or terrain), so whichever decorates first, a neighbour's
 * tree or this chunk, the result is the same; what hangs under it only takes air. Decisions per pair and per block
 * come from a hash of their position, not from the placement random.
 */
public final class IslandVinesFeature extends Feature<IslandVinesConfiguration> {
    /** How far below an edge's top block a vine leaves the island. */
    static final int ATTACH_BELOW_TOP = 2;
    /** The second vine lies this far beside the first and one lower. */
    static final int SECOND_OFFSET = 2;
    private static final long PAIR_SALT = 7501L;
    private static final long STRAND_SALT = 7502L;

    public IslandVinesFeature(Codec<IslandVinesConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<IslandVinesConfiguration> context) {
        WorldGenLevel level = context.level();
        IslandField field = Ring4Terrain.field(level);
        if (field == null) {
            return false;
        }
        IslandVinesConfiguration config = context.config();
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        List<IslandField.IslandPair> pairs = new ArrayList<>(field.islandPairs(minX, minZ, minX + 15, minZ + 15));
        pairs.sort(Comparator.comparingInt(IslandField.IslandPair::firstX).thenComparingInt(IslandField.IslandPair::firstZ)
                .thenComparingInt(IslandField.IslandPair::secondX).thenComparingInt(IslandField.IslandPair::secondZ));
        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (IslandField.IslandPair pair : pairs) {
            if (pair.arch()) {
                continue;
            }
            placed |= draw(level, rope(pair, config.sag(), 0), minX, minZ, config, pos);
            if (unit(hash(pair.firstX(), pair.firstTop(), pair.firstZ(), PAIR_SALT + pair.secondX() * 31L + pair.secondZ())) < config.secondChance()) {
                placed |= draw(level, rope(pair, config.sag(), SECOND_OFFSET), minX, minZ, config, pos);
            }
        }
        return placed;
    }

    /** The part of a vine inside the chunk at (minX, minZ), with what hangs under it. */
    private static boolean draw(WorldGenLevel level, List<BlockPos> rope, int minX, int minZ, IslandVinesConfiguration config,
                                BlockPos.MutableBlockPos pos) {
        boolean placed = false;
        for (BlockPos block : rope) {
            if ((block.getX() & ~15) != minX || (block.getZ() & ~15) != minZ || !takes(level.getBlockState(block))) {
                continue;
            }
            level.setBlock(block, config.rope(), Block.UPDATE_CLIENTS);
            placed = true;
            float roll = unit(hash(block.getX(), block.getY(), block.getZ(), STRAND_SALT));
            if (roll < config.strandChance()) {
                int length = 1 + (int) (roll / config.strandChance() * 3.0F);
                for (int dy = 1; dy <= length && level.getBlockState(pos.set(block.getX(), block.getY() - dy, block.getZ())).isAir(); dy++) {
                    level.setBlock(pos, config.strand(), Block.UPDATE_CLIENTS);
                }
            } else if (roll < config.strandChance() + config.accentChance()
                    && level.getBlockState(pos.set(block.getX(), block.getY() - 1, block.getZ())).isAir()) {
                level.setBlock(pos, config.accent(), Block.UPDATE_CLIENTS);
            }
        }
        return placed;
    }

    /** Air, or the leaves of a feature tree (not persistent: the world tree's leaves are). Never logs, water or terrain. */
    static boolean takes(BlockState state) {
        if (state.isAir()) {
            return true;
        }
        return state.is(BlockTags.LEAVES) && state.hasProperty(LeavesBlock.PERSISTENT) && !state.getValue(LeavesBlock.PERSISTENT);
    }

    /**
     * The blocks of a vine of a pair, face to face from the first edge to the second: it leaves each island
     * {@link #ATTACH_BELOW_TOP} under the edge's top (never under its bottom), sags {@code 1 + sag · span} in the middle
     * (a parabola), and lies {@code offset} blocks to the side (and as many halves lower) for a second vine. Pure.
     */
    static List<BlockPos> rope(IslandField.IslandPair pair, float sag, int offset) {
        double ax = pair.firstX() + 0.5;
        double az = pair.firstZ() + 0.5;
        double bx = pair.secondX() + 0.5;
        double bz = pair.secondZ() + 0.5;
        double span = Math.hypot(bx - ax, bz - az);
        if (span < 1.0) {
            return List.of();
        }
        // Sideways: the horizontal normal of the line.
        double nx = -(bz - az) / span;
        double nz = (bx - ax) / span;
        double ay = Math.max(pair.firstBottom(), pair.firstTop() - ATTACH_BELOW_TOP) - offset / 2;
        double by = Math.max(pair.secondBottom(), pair.secondTop() - ATTACH_BELOW_TOP) - offset / 2;
        double dip = 1.0 + sag * span;
        int steps = Mth.ceil(span * 3.0) + 1;
        Set<BlockPos> blocks = new LinkedHashSet<>();
        BlockPos last = null;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double x = Mth.lerp(t, ax, bx) + nx * offset;
            double z = Mth.lerp(t, az, bz) + nz * offset;
            double y = Mth.lerp(t, ay, by) - dip * 4.0 * t * (1.0 - t);
            BlockPos next = BlockPos.containing(x, y, z);
            if (last != null) {
                // Face to face: one axis at a time.
                BlockPos step = last;
                while (!step.equals(next)) {
                    if (step.getX() != next.getX()) {
                        step = step.offset(Integer.signum(next.getX() - step.getX()), 0, 0);
                    } else if (step.getZ() != next.getZ()) {
                        step = step.offset(0, 0, Integer.signum(next.getZ() - step.getZ()));
                    } else {
                        step = step.offset(0, Integer.signum(next.getY() - step.getY()), 0);
                    }
                    blocks.add(step);
                }
            } else {
                blocks.add(next);
            }
            last = next;
        }
        return List.copyOf(blocks);
    }

    private static long hash(int x, int y, int z, long salt) {
        return Mth.getSeed(x, y, z) ^ salt * 0x9E3779B97F4A7C15L;
    }

    /** A hash in [0, 1). */
    private static float unit(long hash) {
        return ((hash >>> 24) & 0xFFFFFF) / (float) (1 << 24);
    }
}
