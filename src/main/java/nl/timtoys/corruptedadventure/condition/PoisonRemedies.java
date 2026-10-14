package nl.timtoys.corruptedadventure.condition;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import nl.timtoys.corruptedadventure.config.ServerConfig;

/**
 * The emergency remedies against poison (vanilla items): a milk bucket lowers the meter by {@code milkAmount} (40), a
 * honey bottle by {@code honeyAmount} (20). Works everywhere, no cooldown, like {@link WaterBottleDrink}. Milk also
 * clears every effect (vanilla), ours included; if the meter is still above a threshold the next condition tick
 * (within a second) puts that threshold's effect back. Server-side, real players only.
 */
final class PoisonRemedies {
    private PoisonRemedies() {
    }

    static void register(IEventBus gameBus) {
        gameBus.addListener(LivingEntityUseItemEvent.Finish.class, PoisonRemedies::onFinish);
    }

    private static void onFinish(LivingEntityUseItemEvent.Finish event) {
        // getItem() is a copy of the stack before drinking.
        if (event.getEntity() instanceof ServerPlayer player && !(player instanceof FakePlayer)) {
            float amount = amount(event.getItem(), ServerConfig.POISON.milkAmount.get().floatValue(),
                    ServerConfig.POISON.honeyAmount.get().floatValue());
            if (amount > 0) {
                PlayerConditions.add(player, Condition.POISON, -amount);
            }
        }
    }

    /** Points a finished drink takes off the meter: {@code milk} for a milk bucket, {@code honey} for a honey bottle. */
    static float amount(ItemStack stack, float milk, float honey) {
        if (stack.is(Items.MILK_BUCKET)) {
            return milk;
        }
        if (stack.is(Items.HONEY_BOTTLE)) {
            return honey;
        }
        return 0;
    }
}
