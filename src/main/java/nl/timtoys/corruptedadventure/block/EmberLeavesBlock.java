package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Smouldering leaves on charred trunks. The trunk is dead, so they never decay: no random ticks and no decay, whatever
 * the distance to a log. Everything else (distance updates, waterlogging, shears) is vanilla leaves.
 */
public class EmberLeavesBlock extends LeavesBlock {
    public static final MapCodec<EmberLeavesBlock> CODEC = simpleCodec(EmberLeavesBlock::new);

    public EmberLeavesBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends LeavesBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return false;
    }

    @Override
    protected boolean decaying(BlockState state) {
        return false;
    }
}
