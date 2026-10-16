package nl.timtoys.corruptedadventure.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;

/**
 * The two ways to light swamp gas that the block cannot see itself (package 72), server-side:
 * <ul>
 *   <li>an explosion: every gas block it reaches is lit (also when {@code mobGriefing} keeps the blocks, so a chain
 *   explosion still carries the fire on);</li>
 *   <li>flint and steel or a fire charge used on a face with gas in front of it: vanilla only lights fire in air, and
 *   the gas is not air.</li>
 * </ul>
 */
public final class SwampGasIgnition {
    private SwampGasIgnition() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(SwampGasIgnition::onDetonate);
        gameBus.addListener(SwampGasIgnition::onRightClickBlock);
    }

    private static void onDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        for (BlockPos pos : event.getAffectedBlocks()) {
            SwampGasBlock.ignite(level, pos, 1);
        }
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        ItemStack stack = event.getItemStack();
        if (level.isClientSide() || event.getFace() == null || !stack.canPerformAction(ItemAbilities.FIRESTARTER_LIGHT)) {
            return;
        }
        BlockPos target = event.getPos().relative(event.getFace());
        if (!SwampGasBlock.ignite(level, target, 1)) {
            return;
        }
        Player player = event.getEntity();
        level.playSound(null, target, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.playSound(null, target, ModSoundEvents.SWAMP_GAS_IGNITE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        // Used up like vanilla's: a durability point of flint and steel, or one fire charge.
        if (stack.isDamageableItem()) {
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(event.getHand()));
        } else if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
