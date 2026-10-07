package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Ash sprouts: a small grass-like plant on ash ground ({@link AshPlantSoil}); short grass' outline (offset per position), no growth. */
public class AshSproutsBlock extends BushBlock {
    public static final MapCodec<AshSproutsBlock> CODEC = simpleCodec(AshSproutsBlock::new);
    /** Vanilla short grass' outline. */
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

    public AshSproutsBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<AshSproutsBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Vec3 offset = state.getOffset(level, pos);
        return SHAPE.move(offset.x, offset.y, offset.z);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return AshPlantSoil.supports(state);
    }
}
