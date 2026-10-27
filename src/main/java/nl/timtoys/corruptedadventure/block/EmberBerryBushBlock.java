package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import nl.timtoys.corruptedadventure.registry.ModItems;

/**
 * The rare edible fire plant of ring 3: vanilla's sweet berry bush (ages, growth, thorns, picking, bonemeal) on ash
 * ground ({@link AshPlantSoil}) or moss (the dormant caldera). Picking and the loot give {@link ModItems#EMBER_BERRIES},
 * which plant the bush again (phase 2 golf 3); like vanilla's bush it has no item of its own.
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
        return new ItemStack(ModItems.EMBER_BERRIES.get());
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return AshPlantSoil.supports(state) || state.is(Blocks.MOSS_BLOCK);
    }

    /** Vanilla's picking (same counts, sound and age reset) with ember berries instead of sweet berries. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        int age = state.getValue(AGE);
        if (age <= 1) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }
        int count = 1 + level.random.nextInt(2) + (age == MAX_AGE ? 1 : 0);
        popResource(level, pos, new ItemStack(ModItems.EMBER_BERRIES.get(), count));
        level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F,
                0.8F + level.random.nextFloat() * 0.4F);
        BlockState picked = state.setValue(AGE, 1);
        level.setBlock(pos, picked, UPDATE_CLIENTS);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, picked));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
