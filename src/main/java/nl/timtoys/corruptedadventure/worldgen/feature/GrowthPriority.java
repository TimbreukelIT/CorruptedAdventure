package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

/**
 * The write rule of the Nether ring 2 features (change 35, the determinism rule). Growths (stem, cap, lamp, stalactite,
 * dew crystal, mould, sprouts) have a fixed rank, high to low. A feature writes a growth only into air or into a growth
 * of lower rank, never into terrain and never somewhere else when the place is taken. So the final block at a position is
 * the highest-ranked growth any feature wanted there, whatever the order in which chunks were decorated: stronger
 * growths win, equal ones merge by a fixed total order (mould faces add up, otherwise the larger block state id wins).
 */
public final class GrowthPriority {
    /** Not a growth. */
    public static final int NONE = -1;
    public static final int STEM = 6;
    public static final int CAP = 5;
    public static final int LAMP = 4;
    public static final int STALACTITE = 3;
    public static final int CRYSTAL = 2;
    public static final int MOLD = 1;
    public static final int SPROUTS = 0;

    private GrowthPriority() {
    }

    public static int rank(BlockState state) {
        Block block = state.getBlock();
        if (block == ModBlocks.DRIPSHROOM_STEM.get() || block == ModBlocks.DRIPSHROOM_HYPHAE.get()
                || block == ModBlocks.STRIPPED_DRIPSHROOM_STEM.get() || block == ModBlocks.STRIPPED_DRIPSHROOM_HYPHAE.get()) {
            return STEM;
        }
        if (block == ModBlocks.DRIPSHROOM_CAP.get()) {
            return CAP;
        }
        if (block == ModBlocks.DRIPSHROOM_LAMP.get()) {
            return LAMP;
        }
        if (block == ModBlocks.SLIME_STALACTITE.get()) {
            return STALACTITE;
        }
        if (block == ModBlocks.SMALL_DEW_CRYSTAL_BUD.get() || block == ModBlocks.MEDIUM_DEW_CRYSTAL_BUD.get()
                || block == ModBlocks.LARGE_DEW_CRYSTAL_BUD.get() || block == ModBlocks.DEW_CRYSTAL_CLUSTER.get()) {
            return CRYSTAL;
        }
        if (block == ModBlocks.GLOW_MOLD.get()) {
            return MOLD;
        }
        if (block == ModBlocks.SPORE_SPROUTS.get()) {
            return SPROUTS;
        }
        return NONE;
    }

    public static boolean isGrowth(BlockState state) {
        return rank(state) != NONE;
    }

    /**
     * What stands at a position after {@code candidate} was written over {@code existing}: terrain stays, air takes the
     * candidate, a growth keeps the higher rank; equal ranks merge (a total order, so any write order ends the same).
     */
    public static BlockState merge(BlockState existing, BlockState candidate) {
        if (existing.isAir()) {
            return candidate;
        }
        int existingRank = rank(existing);
        if (existingRank == NONE) {
            return existing;
        }
        int candidateRank = rank(candidate);
        if (candidateRank != existingRank) {
            return candidateRank > existingRank ? candidate : existing;
        }
        if (existingRank == MOLD) {
            BlockState merged = existing;
            for (Direction direction : Direction.values()) {
                if (MultifaceBlock.hasFace(candidate, direction)) {
                    merged = merged.setValue(MultifaceBlock.getFaceProperty(direction), true);
                }
            }
            return merged;
        }
        return Block.BLOCK_STATE_REGISTRY.getId(candidate) > Block.BLOCK_STATE_REGISTRY.getId(existing) ? candidate : existing;
    }

    /** Writes {@code candidate} at {@code pos} by {@link #merge}; returns what stands there now. */
    public static BlockState write(FungalWorld world, BlockPos pos, BlockState candidate) {
        BlockState existing = world.get(pos);
        BlockState merged = merge(existing, candidate);
        if (merged != existing) {
            world.set(pos, merged);
        }
        return merged;
    }
}
