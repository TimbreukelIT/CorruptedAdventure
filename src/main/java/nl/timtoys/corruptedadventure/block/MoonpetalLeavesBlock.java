package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;

/**
 * The moonpetal blossom crown: plain leaves that let blossom petals drift down where nothing is below, like vanilla's
 * cherry leaves (golf 2, package 72; A's {@code blossom} particle). Client level only ({@code animateTick}).
 */
public class MoonpetalLeavesBlock extends LeavesBlock {
    public static final MapCodec<MoonpetalLeavesBlock> CODEC = simpleCodec(MoonpetalLeavesBlock::new);
    /** One in this many animate ticks drops a petal (vanilla's cherry: 10; a moonpetal crown is large). */
    private static final int PETAL_CHANCE = 15;

    public MoonpetalLeavesBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends LeavesBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (random.nextInt(PETAL_CHANCE) != 0) {
            return;
        }
        BlockPos below = pos.below();
        BlockState stateBelow = level.getBlockState(below);
        if (!Block.isFaceFull(stateBelow.getCollisionShape(level, below), Direction.UP)) {
            ParticleUtils.spawnParticleBelow(level, pos, random, ModParticleTypes.BLOSSOM.get());
        }
    }
}
