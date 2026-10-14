package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A small glowing mushroom of ring 4 (the glowcap and its blighted variant, same class): a plant on any block with a
 * sturdy top (ground, logs, the roots of the swamp trees). Unlike vanilla mushrooms it ignores light, never spreads and
 * never grows into a huge mushroom: no random ticks, no bonemeal. Its light comes from the block properties.
 */
public class GlowcapBlock extends BushBlock {
    public static final MapCodec<GlowcapBlock> CODEC = simpleCodec(GlowcapBlock::new);
    /** Vanilla mushroom's outline. */
    private static final VoxelShape SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 6.0, 11.0);

    public GlowcapBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<GlowcapBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.isFaceSturdy(level, pos, Direction.UP);
    }
}
