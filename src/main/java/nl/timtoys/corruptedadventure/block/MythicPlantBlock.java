package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import nl.timtoys.corruptedadventure.registry.ModTags;

/**
 * A ground plant of the mythical band (decor rollout): fjord moss, mist fern and moonpetal shrub. The verdant plant's
 * shape and bonemeal ({@link PoppingBonemealable}), but on {@code #corruptedadventure:ring_4_plant_soil} (dirt-like
 * ground and sand).
 */
public class MythicPlantBlock extends VerdantPlantBlock {
    public static final MapCodec<MythicPlantBlock> CODEC = simpleCodec(MythicPlantBlock::new);

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
}
