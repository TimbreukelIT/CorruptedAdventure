package nl.timtoys.corruptedadventure.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Bonemeal on a plant that does not grow pops a copy of it, like vanilla's tall flowers (decision F11 = A, golf 2:
 * "plants work with bonemeal"). For blocks whose item is their own ({@code asItem()}; a wall variant gives its standing
 * item).
 */
public interface PoppingBonemealable extends BonemealableBlock {
    @Override
    default boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    default boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    default void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        Block.popResource(level, pos, new ItemStack(state.getBlock()));
    }
}
