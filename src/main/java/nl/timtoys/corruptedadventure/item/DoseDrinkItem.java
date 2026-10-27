package nl.timtoys.corruptedadventure.item;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayer;
import nl.timtoys.corruptedadventure.condition.ConditionDose;

import java.util.function.Supplier;

/**
 * A single-use drink in a glass bottle that gives a {@link ConditionDose} (cactus juice). Drinkable at any time, like a
 * honey bottle (not food), and it gives the glass bottle back.
 */
public final class DoseDrinkItem extends Item {
    private final Supplier<ConditionDose> dose;

    public DoseDrinkItem(Properties properties, Supplier<ConditionDose> dose) {
        super(properties);
        this.dose = dose;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player && !(player instanceof FakePlayer)) {
            dose.get().apply(player);
            CriteriaTriggers.CONSUME_ITEM.trigger(player, stack);
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        if (entity instanceof Player player && player.hasInfiniteMaterials()) {
            return stack;
        }
        stack.shrink(1);
        if (stack.isEmpty()) {
            return new ItemStack(Items.GLASS_BOTTLE);
        }
        if (entity instanceof Player player && !player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) {
            player.drop(new ItemStack(Items.GLASS_BOTTLE), false);
        }
        return stack;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return CanteenItem.DRINK_TICKS;
    }
}
