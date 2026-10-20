package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModTags;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.WorldTreePart;

/**
 * How the ring 4 trees ({@link IsolatedTreeFeature}) treat each other's blocks, so that the end does not depend on
 * which tree (which chunk) comes first (tree determinism, option A, 2026-10-18):
 * <ul>
 *   <li>{@link #terrain}: what a tree sees where another tree already stands, namely the block from before any tree:
 *   wood is the sea below the sea level and air above it, leaves, loose roots and every non-solid block (plants,
 *   moss, glowcaps, propagules, the ground plants of the neighbours' features) are the fluid they hold, muddy roots
 *   are mud. The world tree (74) is terrain and stays what it is;</li>
 *   <li>{@link #wins}: who gets a block two trees want, a fixed priority trunk &gt; root &gt; leaf &gt; moss, glowcap
 *   or propagule, and a fixed order within one rank (fewer steps to a log, then the block state id), so the merge
 *   gives the same block in either order. Terrain and ground plants always give way to a planned tree block: the
 *   tree planned it against the terrain alone, and the ground features put their plants only in open air.</li>
 * </ul>
 * Wood holds no water, so where it stood comes from the terrain alone ({@link Terrain#water}): ring 4's open space
 * under Y 63 is the flat sea (73), above it the frozen worldgen heightmaps show the lakes.
 * <p>
 * Ground objects (ring 4 decor proef, 2026-10-19: boulders and rocks, {@link GroundObjects}) are the other way round:
 * a solid block that is no tree block at or above its column's frozen {@code WORLD_SURFACE_WG} height cannot be
 * terrain, so a tree sees the open space it took ({@link #object}) and never takes its place, as a lift column. The
 * objects write over the trees' blocks, so either order ends with the object and the tree's own shape. Fallen trunks
 * and stumps are wood and keep the trees' priority ({@link #wins}).
 */
final class TreeOverlap {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState MUD = Blocks.MUD.defaultBlockState();

    /**
     * The rank of a tree block in the priority; {@link #NONE} is terrain (always replaced by a planned block),
     * {@link #GROUND} the dirt a tree lays under its trunk (takes only terrain: existing dirt is terrain).
     */
    enum Rank {
        NONE,
        GROUND,
        DECOR,
        LEAF,
        ROOT,
        TRUNK
    }

    /** What the terrain alone says about a position (pure per seed: the world trees and the frozen heightmaps). */
    interface Terrain {
        /** The world tree's part ({@link WorldTreePart#NONE} away from the world trees or outside ring 4). */
        WorldTreePart worldTree(int x, int y, int z);

        /** Whether open space here held water: below the sea level, or between a column's floor and its water top. */
        boolean water(int x, int y, int z);

        /**
         * The column's frozen {@code WORLD_SURFACE_WG} height (the first open block over the terrain); a solid
         * non-tree block from here up is a ground object. Without a heightmap: none.
         */
        default int surface(int x, int z) {
            return Integer.MAX_VALUE;
        }
    }

    private TreeOverlap() {
    }

    /**
     * The block a tree sees at {@code pos} holding {@code state}: the terrain from before any tree. Pure in its
     * arguments, so a tree's view of the world is the same whether its neighbours grew before it or not.
     */
    static BlockState terrain(BlockState state, BlockPos pos, Terrain terrain) {
        if (state.isAir() || persistent(state) || state.getBlock() instanceof LiquidBlock) {
            return state;
        }
        boolean wood = state.is(BlockTags.LOGS);
        if (!wood && !state.is(BlockTags.LEAVES) && !root(state) && state.isSolid()) {
            return object(state, pos, terrain) ? open(pos, terrain) : state;
        }
        // The world tree (74) is terrain: its wood stays, a tree's block in its crown took a crown leaf.
        WorldTreePart part = terrain.worldTree(pos.getX(), pos.getY(), pos.getZ());
        if (part == WorldTreePart.LEAVES) {
            return ModBlocks.MOONPETAL_LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        }
        if (part == WorldTreePart.LOG || part == WorldTreePart.WOOD) {
            return state.is(ModTags.Blocks.MOONPETAL_LOGS) ? state
                    : (part == WorldTreePart.LOG ? ModBlocks.MOONPETAL_LOG.get() : ModBlocks.MOONPETAL_WOOD.get()).defaultBlockState();
        }
        if (state.is(Blocks.MUDDY_MANGROVE_ROOTS)) {
            return MUD;
        }
        if (wood || state.is(ModBlocks.UPDRAFT.get())) {
            // Wood holds no water, and a lift column (island_lift) fills the open space whatever was there.
            return open(pos, terrain);
        }
        return state.getFluidState().createLegacyBlock();
    }

    /** The open space at {@code pos}: the terrain says whether it was water (the sea, a lake) or air. */
    private static BlockState open(BlockPos pos, Terrain terrain) {
        return terrain.water(pos.getX(), pos.getY(), pos.getZ()) ? WATER : AIR;
    }

    /**
     * Whether {@code state} at {@code pos} is a ground object: solid, no log, leaves or root, at or above the column's
     * frozen surface ({@link Terrain#surface}), and no part of a tree (a fruit pod). Nothing of the terrain stands
     * there; the world trees (74) are logs and leaves and lie under their own surface.
     */
    static boolean object(BlockState state, BlockPos pos, Terrain terrain) {
        return state.isSolid() && !state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES) && !root(state)
                && !state.is(ModBlocks.VERDANT_FRUIT_POD.get()) && pos.getY() >= terrain.surface(pos.getX(), pos.getZ());
    }

    /**
     * The rank of a block a tree planned (by its block: the trunk setter also lays dirt under the trunk; a hanging strand
     * ranks with that dirt, {@link #strand}).
     */
    static Rank planned(BlockState state) {
        if (root(state)) {
            return Rank.ROOT;
        }
        if (state.is(BlockTags.LOGS)) {
            return Rank.TRUNK;
        }
        if (state.is(BlockTags.DIRT) || strand(state)) {
            return Rank.GROUND;
        }
        return state.is(BlockTags.LEAVES) ? Rank.LEAF : Rank.DECOR;
    }

    /**
     * The rank of a block already in the level ({@code below} is the block under it): another tree's trunk, root or
     * leaves, or its moss, glowcap, propagule or fruit pod (a hanging propagule; moss or a glowcap on a mangrove root;
     * pods grow only on the emergent tree), or a hanging strand ({@link #strand}, {@link Rank#GROUND}). Everything else
     * (terrain and dirt, the world tree's leaves, ground plants, which the ground features only put in open air) is
     * {@link Rank#NONE}.
     */
    static Rank existing(BlockState state, BlockState below) {
        if (root(state)) {
            return Rank.ROOT;
        }
        if (state.is(BlockTags.LOGS)) {
            return Rank.TRUNK;
        }
        if (state.is(BlockTags.LEAVES)) {
            return persistent(state) ? Rank.NONE : Rank.LEAF;
        }
        if (strand(state)) {
            return Rank.GROUND;
        }
        boolean decor = state.is(Blocks.MANGROVE_PROPAGULE) && state.getValue(MangrovePropaguleBlock.HANGING)
                || (state.is(Blocks.MOSS_CARPET) || state.is(ModBlocks.GLOWCAP.get())) && root(below)
                || state.is(ModBlocks.VERDANT_FRUIT_POD.get());
        return decor ? Rank.DECOR : Rank.NONE;
    }

    /**
     * Whether the planned {@code ours} takes the place of {@code existing}, which stands on {@code below}. A lift column
     * ({@code updraft}, filled after the trees of its chunk and over anything there) always stays.
     */
    static boolean wins(BlockState ours, BlockState existing, BlockState below) {
        if (existing.is(ModBlocks.UPDRAFT.get())) {
            return false;
        }
        Rank theirs = existing(existing, below);
        if (theirs == Rank.NONE) {
            return true;
        }
        int rank = Integer.compare(planned(ours).ordinal(), theirs.ordinal());
        if (rank != 0) {
            return rank > 0;
        }
        return compareWithinRank(ours, existing) > 0;
    }

    /**
     * A total order within one rank (positive: {@code a} goes first): leaves closer to a log first (they keep their
     * crown alive); at {@link Rank#GROUND} a tree's dirt before a strand and a glowing root before moss (a moss strand cut
     * by a root falls whole, {@link EdgeUpdates}); then the higher block state id. Symmetric, so the merge is the same in
     * either order.
     */
    static int compareWithinRank(BlockState a, BlockState b) {
        if (strand(a) != strand(b)) {
            return strand(a) ? -1 : 1;
        }
        boolean rootA = a.is(ModBlocks.GLOWING_HANGING_ROOTS.get());
        if (strand(a) && rootA != b.is(ModBlocks.GLOWING_HANGING_ROOTS.get())) {
            return rootA ? 1 : -1;
        }
        if (a.hasProperty(BlockStateProperties.DISTANCE) && b.hasProperty(BlockStateProperties.DISTANCE)) {
            int distance = Integer.compare(b.getValue(BlockStateProperties.DISTANCE), a.getValue(BlockStateProperties.DISTANCE));
            if (distance != 0) {
                return distance;
            }
        }
        return Integer.compare(Block.getId(a), Block.getId(b));
    }

    /**
     * Hanging swamp moss and glowing roots (decor A: the strands of {@code hanging_strand}, the arches and the ancient
     * tree): {@link Rank#GROUND}, planned and found in the level alike, so they take only open space, never another tree's
     * moss, propagule or leaf; two trees' strands on one block meet by a fixed order ({@link #compareWithinRank}: a
     * glowing root before moss), which gives the same block in either order (det 2026-10-20: before, a strand in the level
     * ranked as terrain and the later tree's strand always won).
     */
    static boolean strand(BlockState state) {
        return state.is(ModBlocks.HANGING_SWAMP_MOSS.get()) || state.is(ModBlocks.GLOWING_HANGING_ROOTS.get());
    }

    private static boolean root(BlockState state) {
        return state.is(Blocks.MANGROVE_ROOTS) || state.is(Blocks.MUDDY_MANGROVE_ROOTS);
    }

    private static boolean persistent(BlockState state) {
        return state.hasProperty(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.PERSISTENT);
    }
}
