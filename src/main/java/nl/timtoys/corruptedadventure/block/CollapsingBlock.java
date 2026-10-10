package nl.timtoys.corruptedadventure.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block of {@code #corruptedadventure:collapsing_blocks} with its own way to cave in once {@link LooseSandTrigger}
 * has warned about it (ash crust plans its chain itself). Blocks in the tag without it (loose sand, a vanilla class)
 * get a plain scheduled tick and vanilla's update chain.
 */
public interface CollapsingBlock {
    /** Starts the collapse at {@code pos}; the block itself falls after {@code delay} ticks. Server-side. */
    void startCollapse(ServerLevel level, BlockPos pos, int delay);

    /** Starts the collapse of the block at {@code pos}: its own way, or a plain scheduled tick. */
    static void start(ServerLevel level, BlockPos pos, BlockState state, int delay) {
        if (state.getBlock() instanceof CollapsingBlock block) {
            block.startCollapse(level, pos, delay);
        } else {
            level.scheduleTick(pos, state.getBlock(), delay);
        }
    }
}
