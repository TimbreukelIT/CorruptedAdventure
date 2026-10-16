package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

/**
 * The low verdant fern (72b): like vanilla's fern, bonemeal grows it into the tall verdant fern where there is room
 * above, otherwise it pops a copy.
 */
public class VerdantFernBlock extends VerdantPlantBlock {
    public static final MapCodec<VerdantFernBlock> CODEC = simpleCodec(VerdantFernBlock::new);

    public VerdantFernBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<VerdantFernBlock> codec() {
        return CODEC;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockState tall = ModBlocks.VERDANT_TALL_FERN.get().defaultBlockState();
        if (roomToGrow(level, pos) && tall.canSurvive(level, pos)) {
            DoublePlantBlock.placeAt(level, tall, pos, UPDATE_CLIENTS);
        } else {
            super.performBonemeal(level, random, pos, state);
        }
    }

    private static boolean roomToGrow(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.above()).isAir();
    }
}
