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
import nl.timtoys.corruptedadventure.registry.ModTags;

/**
 * Charred shrub (G2 ring 3, decision 4 = A): the fire flora of the bare ring 3 plains, a burnt-out bush with glowing
 * tips. Dead bush outline (offset per position); stands on ash ground ({@link AshPlantSoil}) and on the bare rock of
 * {@code #corruptedadventure:charred_shrub_soil} (pumice, basalt). Extends {@link BushBlock}, as the dry shrub of ring 2.
 */
public class CharredShrubBlock extends BushBlock {
    public static final MapCodec<CharredShrubBlock> CODEC = simpleCodec(CharredShrubBlock::new);
    /** Vanilla's dead bush outline. */
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

    public CharredShrubBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<CharredShrubBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Vec3 offset = state.getOffset(level, pos);
        return SHAPE.move(offset.x, offset.y, offset.z);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return AshPlantSoil.supports(state) || state.is(ModTags.Blocks.CHARRED_SHRUB_SOIL);
    }
}
