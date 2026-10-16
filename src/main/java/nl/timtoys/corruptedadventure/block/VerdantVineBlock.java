package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.NetherVines;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

/**
 * The tip of the verdant vine (72b, F6 = C): vanilla's weeping vines (hangs from a sturdy underside, grows down on random
 * ticks and with bonemeal), climbable through {@code #climbable}. {@link #FLOWERING} is the flowering variant (one block
 * pair with a state instead of four blocks); a vine keeps its flowering when it grows and when tip and stem turn into
 * each other. Worldgen places the tip at the maximum age (25) where a strand must not grow longer.
 */
public class VerdantVineBlock extends GrowingPlantHeadBlock {
    public static final MapCodec<VerdantVineBlock> CODEC = simpleCodec(VerdantVineBlock::new);
    public static final BooleanProperty FLOWERING = BooleanProperty.create("flowering");
    private static final VoxelShape SHAPE = Block.box(4.0, 9.0, 4.0, 12.0, 16.0, 12.0);
    /** Weeping vines' chance to grow per random tick. */
    private static final double GROW_CHANCE = 0.1;

    public VerdantVineBlock(Properties properties) {
        super(properties, Direction.DOWN, SHAPE, false, GROW_CHANCE);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0).setValue(FLOWERING, false));
    }

    @Override
    protected MapCodec<VerdantVineBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FLOWERING);
    }

    @Override
    protected BlockState updateBodyAfterConvertedFromHead(BlockState head, BlockState body) {
        return body.setValue(FLOWERING, head.getValue(FLOWERING));
    }

    @Override
    protected int getBlocksToGrowWhenBonemealed(RandomSource random) {
        return NetherVines.getBlocksToGrowWhenBonemealed(random);
    }

    @Override
    protected boolean canGrowInto(BlockState state) {
        return NetherVines.isValidGrowthState(state);
    }

    @Override
    protected Block getBodyBlock() {
        return ModBlocks.VERDANT_VINE_PLANT.get();
    }
}
