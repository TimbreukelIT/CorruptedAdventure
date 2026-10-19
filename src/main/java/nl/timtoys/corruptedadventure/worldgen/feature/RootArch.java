package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One bow of mangrove roots between two feet (ring 4 decor rollout group A, 2026-10-19): the landmark arches over the
 * water of {@code glowroot_swamp} ({@link RootArchFeature}) and the small tangles above it ({@link RootTangleFeature}).
 * Pure geometry: the feet, the height of the crown, a sideways bow and the thickness (more at the feet).
 * <p>
 * {@link #write} puts the part of some arches in one chunk: roots only into open space (never into the ground), with the
 * trees' own priority ({@link GroundObjects#placeWood}: a trunk beats a root, a root beats leaves, in either order), and
 * waterlogged where the terrain held water ({@link TreeOverlap.Terrain#water}, what a tree sees there). The moss on top
 * and the strands underneath come from each block's own random and only fill open air where they survive, so they do
 * not depend on whether a tree's block or the arch's root ended up above them.
 */
record RootArch(double x1, double y1, double z1, double x2, double y2, double z2, double crown, double bow, double radius) {
    private static final double STEP = 0.3;

    /** Every block within the arch's thickness that lies in {@code chunk}, in a fixed order. */
    void collect(Set<BlockPos> blocks, ChunkPos chunk) {
        double length = Math.hypot(x2 - x1, z2 - z1) + 2.0 * (crown - Math.min(y1, y2)) + 2.0;
        int steps = Mth.ceil(length / STEP);
        double dx = x2 - x1;
        double dz = z2 - z1;
        double flat = Math.max(1.0e-6, Math.hypot(dx, dz));
        double px = -dz / flat;
        double pz = dx / flat;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double lift = Math.sin(Math.PI * t);
            double cx = Mth.lerp(t, x1, x2) + px * bow * lift;
            double cz = Mth.lerp(t, z1, z2) + pz * bow * lift;
            double base = Mth.lerp(t, y1, y2);
            double cy = base + (crown - base) * Math.pow(lift, 0.7);
            double r = radius * (1.0 + 0.8 * Math.pow(1.0 - lift, 3.0));
            int reach = Mth.ceil(r);
            for (int by = Mth.floor(cy) - reach; by <= Mth.floor(cy) + reach; by++) {
                for (int bz = Mth.floor(cz) - reach; bz <= Mth.floor(cz) + reach; bz++) {
                    for (int bx = Mth.floor(cx) - reach; bx <= Mth.floor(cx) + reach; bx++) {
                        if (bx >> 4 != chunk.x || bz >> 4 != chunk.z) {
                            continue;
                        }
                        double ox = bx + 0.5 - cx;
                        double oy = by + 0.5 - cy;
                        double oz = bz + 0.5 - cz;
                        if (ox * ox + oy * oy + oz * oz <= r * r + 0.15) {
                            blocks.add(new BlockPos(bx, by, bz));
                        }
                    }
                }
            }
        }
    }

    /**
     * Writes the part of {@code arches} in {@code chunk}: the roots, then moss on top and strands under them. The randoms
     * of the decoration are per block ({@code seed}, {@code salt}).
     */
    static boolean write(WorldGenLevel level, RootArchConfiguration config, ChunkPos chunk, List<RootArch> arches, long salt) {
        Set<BlockPos> blocks = new LinkedHashSet<>();
        arches.forEach(arch -> arch.collect(blocks, chunk));
        if (blocks.isEmpty()) {
            return false;
        }
        TreeOverlap.Terrain terrain = IsolatedTreeFeature.terrain(level);
        Map<BlockPos, BlockState> wood = new LinkedHashMap<>();
        for (BlockPos pos : blocks) {
            if (ground(level, terrain, pos)) {
                continue;
            }
            BlockState roots = config.roots();
            if (roots.hasProperty(BlockStateProperties.WATERLOGGED)) {
                roots = roots.setValue(BlockStateProperties.WATERLOGGED, terrain.water(pos.getX(), pos.getY(), pos.getZ()));
            }
            wood.put(pos, roots);
        }
        if (wood.isEmpty()) {
            return false;
        }
        GroundObjects.placeWood(level, terrain, wood);
        long seed = level.getSeed();
        for (BlockPos pos : wood.keySet()) {
            BlockPos above = pos.above();
            if (!blocks.contains(above) && Landmarks.inChunk(above, chunk)) {
                RandomSource random = GroundObjects.random(seed, above, salt ^ 0x70B);
                if (random.nextFloat() < config.topChance()) {
                    decor(level, above, config.topDecor().getState(random, above));
                }
            }
            BlockPos below = pos.below();
            if (!blocks.contains(below) && !terrain.water(below.getX(), below.getY(), below.getZ())) {
                hang(level, config, GroundObjects.random(seed, below, salt ^ 0x5D2), below);
            }
        }
        return true;
    }

    /**
     * Whether the terrain alone holds ground at {@code pos}: what a tree sees there ({@link TreeOverlap#terrain}) is
     * neither air nor a fluid. Not the {@code OCEAN_FLOOR_WG} heightmap: under a floating island that is the island's top.
     */
    static boolean ground(WorldGenLevel level, TreeOverlap.Terrain terrain, BlockPos pos) {
        BlockState seen = TreeOverlap.terrain(level.getBlockState(pos), pos, terrain);
        return !seen.isAir() && !(seen.getBlock() instanceof LiquidBlock);
    }

    /**
     * The first open block over the ground of column x, z ({@link #ground}), searched from {@code top} down to
     * {@code bottom}, or {@link Integer#MIN_VALUE} if the column is open all the way.
     */
    static int groundTop(WorldGenLevel level, TreeOverlap.Terrain terrain, int x, int z, int top, int bottom) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = top; y >= bottom; y--) {
            if (ground(level, terrain, pos.set(x, y, z))) {
                return y + 1;
            }
        }
        return Integer.MIN_VALUE;
    }

    /** A strand of moss under an arch's block, or one glowing root. */
    private static void hang(WorldGenLevel level, RootArchConfiguration config, RandomSource random, BlockPos from) {
        float roll = random.nextFloat();
        boolean tip = random.nextFloat() < config.tipChance();
        int length = 1 + random.nextInt(config.maxStrand());
        if (roll >= config.strandChance()) {
            return;
        }
        if (tip) {
            decor(level, from, config.tip());
            return;
        }
        // Every block on its own (support, tree rule): a tree's leaf in the strand, before or after it, ends the same.
        for (int i = 0; i < length; i++) {
            decor(level, from.below(i), config.strand());
        }
    }

    /**
     * The arch's moss, glowcaps and strands are placed as a tree places its decoration ({@link TreeOverlap#wins}): into
     * open air or over a replaceable plant or strand, and over a tree's moss, propagule or strand only where the tree's
     * rule lets it win, so a swamp tree's decoration and the arch's end the same in either order. Only where it survives.
     */
    private static void decor(WorldGenLevel level, BlockPos pos, BlockState state) {
        BlockState existing = level.getBlockState(pos);
        if (existing == state || !existing.isAir() && !(existing.canBeReplaced() || TreeOverlap.existing(existing,
                level.getBlockState(pos.below())) != TreeOverlap.Rank.NONE)) {
            return;
        }
        if (!existing.isAir() && (!existing.getFluidState().isEmpty()
                || !TreeOverlap.wins(state, existing, level.getBlockState(pos.below())))) {
            return;
        }
        if (!state.canSurvive(level, pos)) {
            return;
        }
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        EdgeUpdates.apply(level, Set.of(pos));
    }
}
