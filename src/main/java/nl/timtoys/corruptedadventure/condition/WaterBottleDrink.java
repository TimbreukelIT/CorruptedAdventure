package nl.timtoys.corruptedadventure.condition;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import nl.timtoys.corruptedadventure.config.ServerConfig;

/**
 * Drinking a vanilla water bottle lowers dehydration ({@code waterBottleAmount}, default 30). Works everywhere (also
 * outside ring 2 and outside ring worlds, where the meter is just 0), no cooldown. Server-side, real players only.
 */
public final class WaterBottleDrink {
    private WaterBottleDrink() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(LivingEntityUseItemEvent.Finish.class, WaterBottleDrink::onFinish);
    }

    private static void onFinish(LivingEntityUseItemEvent.Finish event) {
        // getItem() is a copy of the stack before drinking; getResultStack() is the empty bottle.
        if (event.getEntity() instanceof ServerPlayer player && !(player instanceof FakePlayer)
                && isWaterBottle(event.getItem())) {
            PlayerConditions.add(player, Condition.DEHYDRATION, -ServerConfig.DEHYDRATION.waterBottleAmount.get().floatValue());
        }
    }

    /**
     * A drinkable bottle of {@link Potions#WATER}. Splash and lingering bottles are other items (and never drunk);
     * awkward, mundane and thick potions are other potions.
     */
    static boolean isWaterBottle(ItemStack stack) {
        return stack.is(Items.POTION)
                && stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.WATER);
    }
}
