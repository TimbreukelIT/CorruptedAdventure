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
 * A ground plant of the mythical band (decor rollout): fjord moss, mist fern and moonpetal shrub. A bush on
 * {@code #corruptedadventure:ring_4_plant_soil} (dirt-like ground and sand) that pops a copy with bonemeal
 * ({@link PoppingBonemealable}), like the verdant plants.
 */
public class MythicPlantBlock extends BushBlock implements PoppingBonemealable {
    public static final MapCodec<MythicPlantBlock> CODEC = simpleCodec(MythicPlantBlock::new);
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

    public MythicPlantBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends MythicPlantBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModTags.Blocks.RING_4_PLANT_SOIL);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Follows the random offset of the model, like vanilla's flowers.
        Vec3 offset = state.getOffset(level, pos);
        return SHAPE.move(offset.x, offset.y, offset.z);
    }
}
