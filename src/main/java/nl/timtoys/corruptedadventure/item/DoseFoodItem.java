package nl.timtoys.corruptedadventure.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayer;
import nl.timtoys.corruptedadventure.condition.ConditionDose;

import java.util.function.Supplier;

/** Food that also gives a {@link ConditionDose} when eaten (salted jerky: good food, more thirst). */
public final class DoseFoodItem extends Item {
    private final Supplier<ConditionDose> dose;

    public DoseFoodItem(Properties properties, Supplier<ConditionDose> dose) {
        super(properties);
        this.dose = dose;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player && !(player instanceof FakePlayer)) {
            dose.get().apply(player);
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
