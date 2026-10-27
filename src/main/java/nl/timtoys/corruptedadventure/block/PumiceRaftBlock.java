package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A thin plate of pumice that floats on lava (fase-2-items.md §6.4): vanilla's lily pad rule with lava for water, so you
 * can stand on it above a lava lake. No physics: it stays where it is placed and breaks when its lava source goes. A
 * {@link BushBlock} like {@link GiantLilyPadBlock}, because {@code WaterlilyBlock} fixes its codec type.
 */
public class PumiceRaftBlock extends BushBlock {
    public static final MapCodec<PumiceRaftBlock> CODEC = simpleCodec(PumiceRaftBlock::new);
    /** The whole block, so a row of rafts makes a closed path. */
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 1.5, 16.0);

    public PumiceRaftBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<PumiceRaftBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** On a lava source, with nothing liquid in its own spot. */
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return level.getFluidState(pos).getType() == Fluids.LAVA && level.getFluidState(pos.above()).getType() == Fluids.EMPTY;
    }
}
