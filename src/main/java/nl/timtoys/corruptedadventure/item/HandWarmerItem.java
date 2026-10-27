package nl.timtoys.corruptedadventure.item;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayer;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.condition.ConsumableDoses;
import nl.timtoys.corruptedadventure.registry.ModDataComponents;
import nl.timtoys.corruptedadventure.registry.ModItems;

import java.util.List;

/**
 * The hand warmer (fase-2-items.md §6.6): {@link #MAX_CHARGES} charges, each one a {@link ConsumableDoses#handWarmerCharge},
 * used by holding the use button like a sip. A crafted one is empty, like the canteen; a fire crystal clicked onto it in
 * an inventory, like into a bundle, fills it up. The charges live in
 * {@link ModDataComponents#CHARGES}; only the server's click and use count, the client only shows them.
 */
public final class HandWarmerItem extends Item {
    public static final int MAX_CHARGES = 4;
    /** Item property for the model overrides: the charges left (0 to {@link #MAX_CHARGES}); registered in {@code client/}. */
    public static final ResourceLocation CHARGES_PROPERTY = CorruptedAdventure.id("charges");
    private static final int BAR_COLOR = 0xFF8A2A;

    public HandWarmerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static int charges(ItemStack stack) {
        return Mth.clamp(stack.getOrDefault(ModDataComponents.CHARGES, 0), 0, MAX_CHARGES);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        return charges(stack) > 0 ? ItemUtils.startUsingInstantly(level, player, hand) : InteractionResultHolder.pass(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player && !(player instanceof FakePlayer) && charges(stack) > 0) {
            ConsumableDoses.handWarmerCharge().apply(player);
            CriteriaTriggers.CONSUME_ITEM.trigger(player, stack);
            player.awardStat(Stats.ITEM_USED.get(this));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_AMBIENT,
                    SoundSource.PLAYERS, 0.8F, 1.2F);
            if (!player.hasInfiniteMaterials()) {
                stack.set(ModDataComponents.CHARGES, afterCharge(charges(stack)));
            }
        }
        return stack;
    }

    static int afterCharge(int charges) {
        return Math.max(0, charges - 1);
    }

    /** A fire crystal on the cursor, right-clicked onto a hand warmer that is not full: one crystal fills it up. */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot, ClickAction action, Player player,
                                            SlotAccess access) {
        if (action != ClickAction.SECONDARY || !other.is(ModItems.FIRE_CRYSTAL) || charges(stack) >= MAX_CHARGES
                || !slot.allowModification(player)) {
            return false;
        }
        other.consume(1, player);
        stack.set(ModDataComponents.CHARGES, MAX_CHARGES);
        player.playSound(SoundEvents.FLINTANDSTEEL_USE, 0.8F, 1.0F);
        return true;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return CanteenItem.DRINK_TICKS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(chargesKey(), charges(stack), MAX_CHARGES).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(refillKey()).withStyle(ChatFormatting.DARK_GRAY));
    }

    /** {@code item.corruptedadventure.hand_warmer.charges}: "Charges: %s/%s". */
    public String chargesKey() {
        return getDescriptionId() + ".charges";
    }

    /** {@code item.corruptedadventure.hand_warmer.refill}: how to fill it up again. */
    public String refillKey() {
        return getDescriptionId() + ".refill";
    }

    /** Like the canteen's bar: the charges left, in ember orange; hidden when empty. */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return charges(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return barWidth(charges(stack));
    }

    static int barWidth(int charges) {
        return Math.round(13.0F * charges / MAX_CHARGES);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BAR_COLOR;
    }
}
