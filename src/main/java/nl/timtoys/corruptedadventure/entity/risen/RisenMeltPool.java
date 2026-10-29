package nl.timtoys.corruptedadventure.entity.risen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import nl.timtoys.corruptedadventure.registry.ModTags;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * The ground The Risen melts to rise from (design doc ring 3, "Omhoog smelten"): a disc of surface blocks around where it
 * appears first glows and cracks (magma), then its middle opens to lava, and after the rise it sets to a permanent scar
 * of obsidian, basalt and magma. Only natural blocks ({@link ModTags.Blocks#RISEN_MELTABLE}) with open air (or a plant)
 * above, never a player's blocks; lava only where the pool holds it on every side and below, so it cannot run off.
 * The temporary lava always goes again: the scar replaces it when the pool sets, also when the Risen dies or leaves first.
 * Saved with the Risen. Server-side.
 */
public final class RisenMeltPool {
    /** Most a column's surface may lie above or below the centre's to belong to the pool (it stays a pool, not a slope). */
    private static final int MAX_STEP = 1;

    private final BlockPos centre;
    private final List<BlockPos> ground;
    private final Set<BlockPos> lava = new HashSet<>();
    private boolean set;

    private RisenMeltPool(BlockPos centre, List<BlockPos> ground) {
        this.centre = centre;
        this.ground = ground;
    }

    /**
     * The surface blocks within {@code radius} of {@code centre} (the block it stands on) that may melt: per column the
     * highest meltable block with open air above, at most {@link #MAX_STEP} above or below the centre.
     */
    public static RisenMeltPool plan(ServerLevel level, BlockPos centre, int radius) {
        List<BlockPos> ground = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius + radius) {
                    continue;
                }
                // The highest meltable block with air above, near the centre's height (also under an overhang).
                for (int dy = MAX_STEP; dy >= -MAX_STEP; dy--) {
                    BlockPos pos = centre.offset(dx, dy, dz);
                    if (meltable(level, pos)) {
                        ground.add(pos);
                        break;
                    }
                }
            }
        }
        return new RisenMeltPool(centre, ground);
    }

    private static boolean meltable(ServerLevel level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return level.getBlockState(pos).is(ModTags.Blocks.RISEN_MELTABLE)
                && (above.isAir() || above.canBeReplaced() && above.getFluidState().isEmpty());
    }

    public BlockPos centre() {
        return centre;
    }

    /** The ground glows and cracks: every pool block turns to magma, plants on it burn away. */
    public void glow(ServerLevel level) {
        for (BlockPos pos : ground) {
            if (!meltable(level, pos)) {
                continue;
            }
            if (!level.getBlockState(pos.above()).isAir()) {
                level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            level.setBlock(pos, Blocks.MAGMA_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /** The middle opens: lava wherever the pool holds it, so none runs out over the edge or down a hole. */
    public void open(ServerLevel level) {
        Set<BlockPos> glowing = new HashSet<>();
        for (BlockPos pos : ground) {
            if (level.getBlockState(pos).is(Blocks.MAGMA_BLOCK)) {
                glowing.add(pos);
            }
        }
        for (BlockPos pos : glowing) {
            if (holds(level, pos, glowing)) {
                lava.add(pos);
            }
        }
        for (BlockPos pos : lava) {
            level.setBlock(pos, Blocks.LAVA.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /** Every side at the same height is pool or solid ground, and so is the block below. */
    private static boolean holds(ServerLevel level, BlockPos pos, Set<BlockPos> pool) {
        if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
            return false;
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos next = pos.relative(side);
            if (!pool.contains(next) && !level.getBlockState(next).isFaceSturdy(level, next, side.getOpposite())) {
                return false;
            }
        }
        return true;
    }

    public boolean isSet() {
        return set;
    }

    /**
     * The pool sets to its scar: lava still there becomes obsidian, basalt or magma; the glowing rim basalt or magma.
     * With {@code loadChunks} false only in loaded chunks (no hitch on the tick); the rest stays pending
     * ({@link #isSet()} false) for a next call.
     */
    public void set(ServerLevel level, boolean loadChunks) {
        if (set) {
            return;
        }
        RandomSource random = level.getRandom();
        for (Iterator<BlockPos> it = ground.iterator(); it.hasNext(); ) {
            BlockPos pos = it.next();
            if (!loadChunks && !level.isLoaded(pos)) {
                continue;
            }
            it.remove();
            BlockState state = level.getBlockState(pos);
            if (lava.contains(pos) && state.is(Blocks.LAVA)) {
                float roll = random.nextFloat();
                Block scar = roll < 0.3F ? Blocks.OBSIDIAN : roll < 0.75F ? Blocks.BASALT : Blocks.MAGMA_BLOCK;
                level.setBlock(pos, scar.defaultBlockState(), Block.UPDATE_ALL);
            } else if (state.is(Blocks.MAGMA_BLOCK) && random.nextFloat() < 0.6F) {
                level.setBlock(pos, Blocks.BASALT.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        if (ground.isEmpty()) {
            set = true;
            RisenEffects.poolSets(level, centre);
        }
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("centre", centre.asLong());
        tag.put("ground", new LongArrayTag(ground.stream().mapToLong(BlockPos::asLong).toArray()));
        tag.put("lava", new LongArrayTag(lava.stream().mapToLong(BlockPos::asLong).toArray()));
        tag.putBoolean("set", set);
        return tag;
    }

    static RisenMeltPool load(CompoundTag tag) {
        List<BlockPos> ground = new ArrayList<>();
        for (long pos : tag.getLongArray("ground")) {
            ground.add(BlockPos.of(pos));
        }
        RisenMeltPool pool = new RisenMeltPool(BlockPos.of(tag.getLong("centre")), ground);
        if (tag.contains("lava", Tag.TAG_LONG_ARRAY)) {
            for (long pos : tag.getLongArray("lava")) {
                pool.lava.add(BlockPos.of(pos));
            }
        }
        pool.set = tag.getBoolean("set");
        return pool;
    }
}
