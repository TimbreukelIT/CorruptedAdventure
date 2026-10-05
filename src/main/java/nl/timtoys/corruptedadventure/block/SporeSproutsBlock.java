package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

/** Small plant of Nether ring 2: stands on spore moss and soaked netherrack only (not on nylium, dirt or soul soil). */
public class SporeSproutsBlock extends BushBlock {
    public static final MapCodec<SporeSproutsBlock> CODEC = simpleCodec(SporeSproutsBlock::new);
    private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 7.0, 13.0);

    public SporeSproutsBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<SporeSproutsBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModBlocks.SPORE_MOSS) || state.is(ModBlocks.SOAKED_NETHERRACK);
    }
}
