package nl.timtoys.corruptedadventure.condition;

import net.minecraft.world.effect.MobEffectCategory;
import nl.timtoys.corruptedadventure.registry.ModMobEffects;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateEffectsTest {
    private static float modify(Condition condition, float rate, RateEffect active) {
        return RateEffects.modify(condition, rate, effect -> effect == active, (effect, slowed) -> 0.5);
    }

    @Test
    void onlyARisingRateOfASlowedConditionIsMultiplied() {
        assertEquals(0.2F, modify(Condition.DEHYDRATION, 0.4F, RateEffect.COOLED), 1.0E-6F);
        assertEquals(0.275F, modify(Condition.HEAT, 0.55F, RateEffect.COOLED), 1.0E-6F, "cooled also slows heat");
        assertEquals(-0.5F, modify(Condition.HEAT, -0.5F, RateEffect.COOLED), "falling stays");
        assertEquals(0.0F, modify(Condition.DEHYDRATION, 0.0F, RateEffect.COOLED));
        assertEquals(0.42F, modify(Condition.COLD, 0.42F, RateEffect.COOLED), "another condition");
        assertEquals(0.4F, modify(Condition.DEHYDRATION, 0.4F, null), "not cooled");
        assertEquals(1.5F, modify(Condition.POISON, 3.0F, RateEffect.CLEANSED), 1.0E-6F);
        assertEquals(3.0F, modify(Condition.POISON, 3.0F, RateEffect.WARMED), "warmed leaves poison alone");
        assertEquals(0.21F, modify(Condition.COLD, 0.42F, RateEffect.WARMED), 1.0E-6F);
        assertEquals(0.14F, modify(Condition.VOID, 0.28F, RateEffect.ANCHORED), 1.0E-6F);
        assertEquals(-1.0F, modify(Condition.VOID, -1.0F, RateEffect.ANCHORED), "a lit void meter keeps falling");
        assertEquals(0.33F, modify(Condition.SILENCE, 0.33F, RateEffect.ANCHORED), "anchored leaves the silence alone");
        assertEquals(0.165F, modify(Condition.SILENCE, 0.33F, RateEffect.RESOUNDING), 1.0E-6F);
        assertEquals(0.28F, modify(Condition.VOID, 0.28F, RateEffect.RESOUNDING), "resounding leaves the void alone");
    }

    @Test
    void everyEffectSlowsItsOwnConditions() {
        assertSlows(RateEffect.COOLED, EnumSet.of(Condition.DEHYDRATION, Condition.HEAT));
        assertSlows(RateEffect.CLEANSED, EnumSet.of(Condition.POISON));
        assertSlows(RateEffect.WARMED, EnumSet.of(Condition.COLD));
        assertSlows(RateEffect.ANCHORED, EnumSet.of(Condition.VOID));
        assertSlows(RateEffect.RESOUNDING, EnumSet.of(Condition.SILENCE));
    }

    private static void assertSlows(RateEffect effect, Set<Condition> slowed) {
        for (Condition condition : Condition.values()) {
            assertEquals(slowed.contains(condition), effect.slows(condition), effect + " on " + condition);
        }
        assertEquals(1.0F, effect.factor(Condition.DEBUG), "no factor off its conditions");
    }

    @Test
    void everyEffectIsItsOwnBeneficialStatusEffect() {
        assertEquals(ModMobEffects.COOLED.get(), RateEffect.COOLED.mobEffect().value());
        assertEquals(ModMobEffects.CLEANSED.get(), RateEffect.CLEANSED.mobEffect().value());
        assertEquals(ModMobEffects.WARMED.get(), RateEffect.WARMED.mobEffect().value());
        assertEquals(ModMobEffects.ANCHORED.get(), RateEffect.ANCHORED.mobEffect().value());
        assertEquals(ModMobEffects.RESOUNDING.get(), RateEffect.RESOUNDING.mobEffect().value());
        for (RateEffect effect : RateEffect.values()) {
            assertEquals(MobEffectCategory.BENEFICIAL, effect.mobEffect().value().getCategory());
            for (RateEffect other : RateEffect.values()) {
                assertTrue(effect == other || effect.mobEffect() != other.mobEffect(), "one status effect each");
            }
        }
    }
}
