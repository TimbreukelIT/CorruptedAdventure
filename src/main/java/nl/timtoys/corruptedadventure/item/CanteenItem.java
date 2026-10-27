package nl.timtoys.corruptedadventure.item;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.util.FakePlayer;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.condition.ConsumableDoses;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModDataComponents;

import java.util.List;

/**
 * The canteen (fase-2-items.md §5.3): {@link #MAX_SIPS} sips per filling, each one a {@link ConsumableDoses#canteenSip}.
 * Filled to the top by a water source (which stays), a water cauldron (one level lower, like a bottle) or a dew source
 * (the way to fill it in the Nether); drinking works anywhere. The sips live in {@link ModDataComponents#SIPS}; only the
 * server changes them, the client only shows them.
 */
public final class CanteenItem extends Item {
    public static final int MAX_SIPS = 4;
    /** Item property for the model overrides: the sips left (0 to {@link #MAX_SIPS}); registered in {@code client/}. */
    public static final ResourceLocation SIPS_PROPERTY = CorruptedAdventure.id("sips");
    static final int DRINK_TICKS = 32;
    private static final int BAR_COLOR = 0x3F76E4;

    public CanteenItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static int sips(ItemStack stack) {
        return Mth.clamp(stack.getOrDefault(ModDataComponents.SIPS, 0), 0, MAX_SIPS);
    }

    /** A water cauldron or a dew source, clicked directly; water sources are found by {@link #use}. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        boolean cauldron = state.is(Blocks.WATER_CAULDRON);
        if (sips(stack) >= MAX_SIPS || !(cauldron || state.is(ModBlocks.DEW_SOURCE)) || context.getPlayer() == null) {
            return InteractionResult.PASS; // A full canteen drinks instead (use).
        }
        if (!level.isClientSide) {
            if (cauldron) {
                LayeredCauldronBlock.lowerFillLevel(state, level, pos);
                context.getPlayer().awardStat(Stats.USE_CAULDRON);
            }
            fill(stack, context.getPlayer(), level, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (sips(stack) < MAX_SIPS) {
            BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
            BlockPos pos = hit.getBlockPos();
            if (hit.getType() == HitResult.Type.BLOCK && level.mayInteract(player, pos)
                    && level.getFluidState(pos).is(FluidTags.WATER)) {
                if (!level.isClientSide) {
                    fill(stack, player, level, pos);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
        }
        return sips(stack) > 0 ? ItemUtils.startUsingInstantly(level, player, hand) : InteractionResultHolder.pass(stack);
    }

    private void fill(ItemStack stack, Player player, Level level, BlockPos pos) {
        stack.set(ModDataComponents.SIPS, MAX_SIPS);
        player.awardStat(Stats.ITEM_USED.get(this));
        level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player && !(player instanceof FakePlayer) && sips(stack) > 0) {
            ConsumableDoses.canteenSip().apply(player);
            CriteriaTriggers.CONSUME_ITEM.trigger(player, stack);
            player.awardStat(Stats.ITEM_USED.get(this));
            if (!player.hasInfiniteMaterials()) {
                stack.set(ModDataComponents.SIPS, afterSip(sips(stack)));
            }
        }
        return stack;
    }

    static int afterSip(int sips) {
        return Math.max(0, sips - 1);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return DRINK_TICKS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(sipsKey(), sips(stack), MAX_SIPS).withStyle(ChatFormatting.GRAY));
    }

    /** {@code item.corruptedadventure.canteen.sips}: "Sips: %s/%s". */
    public String sipsKey() {
        return getDescriptionId() + ".sips";
    }

    /** Like the bundle's fullness bar: the sips left, in water blue; hidden when empty. */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return sips(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return barWidth(sips(stack));
    }

    static int barWidth(int sips) {
        return Math.round(13.0F * sips / MAX_SIPS);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BAR_COLOR;
    }
}
