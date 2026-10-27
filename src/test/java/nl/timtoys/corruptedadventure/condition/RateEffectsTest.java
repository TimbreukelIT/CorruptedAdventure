package nl.timtoys.corruptedadventure.condition;

import nl.timtoys.corruptedadventure.registry.ModMobEffects;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateEffectsTest {
    private static float modify(Condition condition, float rate, boolean cooled) {
        return RateEffects.modify(condition, rate, effect -> cooled, effect -> 0.5);
    }

    @Test
    void onlyARisingRateOfItsOwnConditionIsMultiplied() {
        assertEquals(0.2F, modify(Condition.DEHYDRATION, 0.4F, true), 1.0E-6F);
        assertEquals(-0.5F, modify(Condition.DEHYDRATION, -0.5F, true), "falling stays");
        assertEquals(0.0F, modify(Condition.DEHYDRATION, 0.0F, true));
        assertEquals(0.55F, modify(Condition.HEAT, 0.55F, true), "another condition");
        assertEquals(0.4F, modify(Condition.DEHYDRATION, 0.4F, false), "not cooled");
    }

    @Test
    void cooledIsTheStatusEffectOfDehydration() {
        assertEquals(Condition.DEHYDRATION, RateEffect.COOLED.condition());
        assertEquals(ModMobEffects.COOLED.get(), RateEffect.COOLED.mobEffect().value());
        assertEquals(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, ModMobEffects.COOLED.get().getCategory());
    }
}
