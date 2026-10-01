package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.Nullable;

/** Withered dirt: the ground under dead grass. Shovel and hoe work as on vanilla dirt. */
public class WitheredDirtBlock extends Block {
    public static final MapCodec<WitheredDirtBlock> CODEC = simpleCodec(WitheredDirtBlock::new);

    public WitheredDirtBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends WitheredDirtBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility itemAbility, boolean simulate) {
        BlockState modified = dirtToolState(context, itemAbility);
        return modified != null ? modified : super.getToolModifiedState(state, context, itemAbility, simulate);
    }

    /**
     * Shovel → dirt path, hoe → farmland (only with air above), like vanilla dirt and grass;
     * vanilla only knows its own blocks here. Shared with {@link DeadGrassBlock}.
     */
    static @Nullable BlockState dirtToolState(UseOnContext context, ItemAbility itemAbility) {
        if (!context.getItemInHand().canPerformAction(itemAbility)) {
            return null;
        }
        if (itemAbility == ItemAbilities.SHOVEL_FLATTEN) {
            return Blocks.DIRT_PATH.defaultBlockState();
        }
        if (itemAbility == ItemAbilities.HOE_TILL && HoeItem.onlyIfAirAbove(context)) {
            return Blocks.FARMLAND.defaultBlockState();
        }
        return null;
    }
}
