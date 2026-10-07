package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The rare edible fire plant of ring 3: vanilla's sweet berry bush (ages, growth, thorns, picking, bonemeal) on ash
 * ground ({@link AshPlantSoil}) or moss (the dormant caldera). Picking drops vanilla sweet berries for now; its own
 * food follows in phase 2. Pick-block gives the bush itself, not the berries.
 */
public class EmberBerryBushBlock extends SweetBerryBushBlock {
    public static final MapCodec<SweetBerryBushBlock> CODEC = simpleCodec(EmberBerryBushBlock::new);

    public EmberBerryBushBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<SweetBerryBushBlock> codec() {
        return CODEC;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(this);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return AshPlantSoil.supports(state) || state.is(Blocks.MOSS_BLOCK);
    }
}
