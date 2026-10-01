package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.timtoys.corruptedadventure.registry.ModTags;

/**
 * Dry shrub: a dead bush that grows on {@code #corruptedadventure:desert_plant_soil}. Extends {@link BushBlock}
 * instead of {@code DeadBushBlock}, whose {@code codec()} return type cannot be narrowed to this class.
 */
public class DryShrubBlock extends BushBlock {
    public static final MapCodec<DryShrubBlock> CODEC = simpleCodec(DryShrubBlock::new);
    /** Same outline as vanilla's dead bush. */
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

    public DryShrubBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<DryShrubBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModTags.Blocks.DESERT_PLANT_SOIL);
    }
}
