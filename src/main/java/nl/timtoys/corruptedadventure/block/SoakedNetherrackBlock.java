package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

/**
 * Wet netherrack of Nether ring 2. Like vanilla netherrack, bonemeal turns it into the moss of a neighbouring block;
 * vanilla's {@code NetherrackBlock} only knows crimson and warped nylium, hence our own class.
 */
public class SoakedNetherrackBlock extends Block implements BonemealableBlock {
    public static final MapCodec<SoakedNetherrackBlock> CODEC = simpleCodec(SoakedNetherrackBlock::new);

    public SoakedNetherrackBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends SoakedNetherrackBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        if (!level.getBlockState(pos.above()).propagatesSkylightDown(level, pos)) {
            return false;
        }
        for (BlockPos neighbour : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (level.getBlockState(neighbour).is(ModBlocks.SPORE_MOSS)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        level.setBlock(pos, ModBlocks.SPORE_MOSS.get().defaultBlockState(), 3);
    }

    @Override
    public Type getType() {
        return Type.NEIGHBOR_SPREADER;
    }
}
