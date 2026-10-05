package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

/**
 * Nether ring 2's nylium: covered moss turns into soaked netherrack, and in the open it spreads to soaked netherrack
 * (never to vanilla netherrack, so it does not creep into Nether ring 1 or into builds). Not a {@code NyliumBlock}:
 * that hardcodes netherrack and the crimson/warped vegetation. Bonemeal plants spore sprouts.
 */
public class SporeMossBlock extends Block implements BonemealableBlock {
    public static final MapCodec<SporeMossBlock> CODEC = simpleCodec(SporeMossBlock::new);
    private static final int SPROUT_ATTEMPTS = 48;

    public SporeMossBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends SporeMossBlock> codec() {
        return CODEC;
    }

    /** Vanilla's {@code NyliumBlock.canBeNylium}: only a block that blocks all light covers it; the Nether is dark anyway. */
    static boolean canStayMoss(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        int lightBlocked = LightEngine.getLightBlockInto(
                level, state, pos, aboveState, above, Direction.UP, aboveState.getLightBlock(level, above));
        return lightBlocked < level.getMaxLightLevel();
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canStayMoss(state, level, pos)) {
            if (level.isAreaLoaded(pos, 1)) {
                level.setBlockAndUpdate(pos, ModBlocks.SOAKED_NETHERRACK.get().defaultBlockState());
            }
            return;
        }
        if (!level.isAreaLoaded(pos, 3)) {
            return;
        }
        BlockPos target = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
        if (level.getBlockState(target).is(ModBlocks.SOAKED_NETHERRACK) && canStayMoss(state, level, target)) {
            level.setBlockAndUpdate(target, defaultBlockState());
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return level.getBlockState(pos.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    /** Patches like vanilla's grass bonemeal: random walks from the spot above, planting where the ground allows. */
    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockState sprouts = ModBlocks.SPORE_SPROUTS.get().defaultBlockState();
        BlockPos above = pos.above();
        for (int i = 0; i < SPROUT_ATTEMPTS; i++) {
            BlockPos target = above;
            for (int step = 0; step < i / 12; step++) {
                target = target.offset(random.nextInt(3) - 1, (random.nextInt(3) - 1) * random.nextInt(3) / 2, random.nextInt(3) - 1);
                if (!level.getBlockState(target.below()).is(this) || level.getBlockState(target).isCollisionShapeFullBlock(level, target)) {
                    break;
                }
            }
            if (level.getBlockState(target).isAir() && sprouts.canSurvive(level, target)) {
                level.setBlock(target, sprouts, 3);
            }
        }
    }
}
