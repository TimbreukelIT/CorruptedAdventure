package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;
import net.neoforged.neoforge.common.ItemAbility;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import org.jetbrains.annotations.Nullable;

/**
 * Dead grass behaves like grass, but within its own family: covered or dark → withered dirt; in the light it
 * spreads to withered dirt only. Vanilla grass and dead grass never affect each other.
 * The random tick follows vanilla's {@code SpreadingSnowyDirtBlock}, which hardcodes dirt and grass.
 */
public class DeadGrassBlock extends SnowyDirtBlock {
    public static final MapCodec<DeadGrassBlock> CODEC = simpleCodec(DeadGrassBlock::new);

    public DeadGrassBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends DeadGrassBlock> codec() {
        return CODEC;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canStayGrass(state, level, pos)) {
            // Prevent loading unloaded chunks when checking the neighbours' light (as NeoForge does for grass).
            if (!level.isAreaLoaded(pos, 1)) {
                return;
            }
            level.setBlockAndUpdate(pos, ModBlocks.WITHERED_DIRT.get().defaultBlockState());
            return;
        }
        if (!level.isAreaLoaded(pos, 3) || level.getMaxLocalRawBrightness(pos.above()) < 9) {
            return;
        }
        BlockState grass = defaultBlockState();
        for (int i = 0; i < 4; i++) {
            BlockPos target = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
            if (level.getBlockState(target).is(ModBlocks.WITHERED_DIRT) && canSpreadTo(grass, level, target)) {
                level.setBlockAndUpdate(target, grass.setValue(SNOWY, level.getBlockState(target.above()).is(Blocks.SNOW)));
            }
        }
    }

    @Override
    public @Nullable BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility itemAbility, boolean simulate) {
        BlockState modified = WitheredDirtBlock.dirtToolState(context, itemAbility);
        return modified != null ? modified : super.getToolModifiedState(state, context, itemAbility, simulate);
    }

    private static boolean canStayGrass(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        if (aboveState.is(Blocks.SNOW) && aboveState.getValue(SnowLayerBlock.LAYERS) == 1) {
            return true;
        }
        if (aboveState.getFluidState().getAmount() == 8) {
            return false;
        }
        int lightBlocked = LightEngine.getLightBlockInto(
                level, state, pos, aboveState, above, Direction.UP, aboveState.getLightBlock(level, above));
        return lightBlocked < level.getMaxLightLevel();
    }

    private static boolean canSpreadTo(BlockState grass, LevelReader level, BlockPos pos) {
        return canStayGrass(grass, level, pos) && !level.getFluidState(pos.above()).is(FluidTags.WATER);
    }
}
