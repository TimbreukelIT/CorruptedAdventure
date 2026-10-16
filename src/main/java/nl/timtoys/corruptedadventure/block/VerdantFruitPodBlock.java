package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.timtoys.corruptedadventure.registry.ModItems;
import nl.timtoys.corruptedadventure.registry.ModTags;

/**
 * The fruit pod on the emergent verdant trunks (72b, H3 = A): vanilla's cocoa (three growth stages on random ticks and
 * bonemeal, {@link #FACING} towards the trunk), but it hangs on {@code verdant_logs} instead of jungle logs and gives
 * the placeholder {@code verdant_fruit}. No item of its own: only worldgen places it.
 */
public class VerdantFruitPodBlock extends CocoaBlock {
    public static final MapCodec<CocoaBlock> CODEC = simpleCodec(VerdantFruitPodBlock::new);

    public VerdantFruitPodBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<CocoaBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.relative(state.getValue(FACING))).is(ModTags.Blocks.VERDANT_LOGS);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(ModItems.VERDANT_FRUIT.get());
    }
}
