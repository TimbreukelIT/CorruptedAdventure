package nl.timtoys.corruptedadventure.condition;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import nl.timtoys.corruptedadventure.config.CommonConfig;
import nl.timtoys.corruptedadventure.config.RingSettings;
import nl.timtoys.corruptedadventure.ring.Ring4Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.weather.RingWeatherState;
import nl.timtoys.corruptedadventure.weather.RingWeatherType;
import org.junit.jupiter.api.Test;

import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Toxic rain on poison, the remedies (milk, honey) and the withering at the top threshold (package 69). */
class PoisonModifiersTest {
    private static final int DURATION = 6000;
    private static final float RAIN_RISE = 0.5F;
    private static final BooleanSupplier YES = () -> true;
    private static final BooleanSupplier NO = () -> false;
    private static final BooleanSupplier NEVER = () -> {
        throw new AssertionError("asked although an earlier check already failed");
    };

    /** A weather at {@code intensity} (fade-in part of a long weather). */
    private static RingWeatherState weather(RingWeatherType type, float intensity) {
        int elapsed = Math.round(intensity * type.fadeTicks());
        return new RingWeatherState(type, DURATION - elapsed, DURATION, 7);
    }

    private static float rained(float rate, RingWeatherState state, BooleanSupplier openSky, BooleanSupplier blighted) {
        return ToxicRainPoisonModifier.rained(rate, state, openSky, blighted, RAIN_RISE,
                ToxicRainPoisonModifier.DEFAULT_RAIN_MULTIPLIER);
    }

    @Test
    void fullToxicRainUnderTheOpenSkyInTheBlightedBandRises() {
        assertEquals(0.5F, rained(-1.0F, weather(RingWeatherType.TOXIC_RAIN, 1.0F), YES, YES), 1.0E-6F);
        float seconds = 100 / rained(-1.0F, weather(RingWeatherType.TOXIC_RAIN, 1.0F), YES, YES);
        assertTrue(seconds > 190 && seconds < 230, "0 -> 100 in " + seconds + " s (~3.5 min)");
    }

    @Test
    void theRainFollowsTheFadeAndTheMultiplier() {
        assertEquals(0.25F, rained(-1.0F, weather(RingWeatherType.TOXIC_RAIN, 0.5F), YES, YES), 1.0E-6F);
        RingWeatherState fadingOut = new RingWeatherState(RingWeatherType.TOXIC_RAIN, 150, DURATION, 7);
        assertEquals(0.125F, rained(-1.0F, fadingOut, YES, YES), 1.0E-6F);
        assertEquals(1.0F, ToxicRainPoisonModifier.rained(-1.0F, weather(RingWeatherType.TOXIC_RAIN, 1.0F), YES, YES,
                RAIN_RISE, 2.0F), 1.0E-6F);
        assertEquals(1.0F, ToxicRainPoisonModifier.DEFAULT_RAIN_MULTIPLIER);
    }

    @Test
    void aStrongerSourceWinsAndTheRainDoesNotStack() {
        assertEquals(3.0F, rained(3.0F, weather(RingWeatherType.TOXIC_RAIN, 1.0F), YES, YES), 1.0E-6F);
        assertEquals(2.0F, rained(2.0F, weather(RingWeatherType.TOXIC_RAIN, 1.0F), YES, YES), 1.0E-6F);
    }

    @Test
    void noRainNoChange() {
        // Other weather, clear, a ring without weather: the rate stays and neither sky nor band is asked.
        assertEquals(-1.0F, rained(-1.0F, weather(RingWeatherType.DENSE_FOG, 1.0F), NEVER, NEVER));
        assertEquals(-1.0F, rained(-1.0F, weather(RingWeatherType.ASH_RAIN, 1.0F), NEVER, NEVER));
        assertEquals(-1.0F, rained(-1.0F, RingWeatherState.STILL, NEVER, NEVER));
        // The first tick of the rain: intensity 0.
        assertEquals(-1.0F, rained(-1.0F, weather(RingWeatherType.TOXIC_RAIN, 0.0F), NEVER, NEVER));
    }

    @Test
    void onlyUnderTheOpenSkyAndInTheBlightedBand() {
        RingWeatherState rain = weather(RingWeatherType.TOXIC_RAIN, 1.0F);
        assertEquals(-1.0F, rained(-1.0F, rain, NO, NEVER), "under a roof or with the eyes under water");
        assertEquals(-1.0F, rained(-1.0F, rain, YES, NO), "in the mythical band");
    }

    @Test
    void theBandDecidesWithTheRealBandAxis() {
        RingLayout layout = new RingLayout(new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 10200, 96, 128,
                CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES), RingLayout.noiseSeed(20260930L));
        Ring4Band band = Ring4Band.of(layout);
        RingWeatherState rain = weather(RingWeatherType.TOXIC_RAIN, 1.0F);
        // Ring 4 runs 6000 - 8000: its inner edge is always mythical, its outer edge always blighted.
        assertEquals(-1.0F, rained(-1.0F, rain, YES, () -> band.blighted(6100, 0)));
        assertEquals(0.5F, rained(-1.0F, rain, YES, () -> band.blighted(7900, 0)), 1.0E-6F);
        assertEquals(0.5F, rained(-1.0F, rain, YES, () -> band.blighted(0, -7900)), 1.0E-6F);
    }

    @Test
    void onlyMilkAndHoneyAreRemedies() {
        assertEquals(40.0F, PoisonRemedies.amount(new ItemStack(Items.MILK_BUCKET), 40.0F, 20.0F));
        assertEquals(20.0F, PoisonRemedies.amount(new ItemStack(Items.HONEY_BOTTLE), 40.0F, 20.0F));
        assertEquals(0.0F, PoisonRemedies.amount(new ItemStack(Items.BUCKET), 40.0F, 20.0F));
        assertEquals(0.0F, PoisonRemedies.amount(new ItemStack(Items.WATER_BUCKET), 40.0F, 20.0F));
        assertEquals(0.0F, PoisonRemedies.amount(new ItemStack(Items.GLASS_BOTTLE), 40.0F, 20.0F));
        assertEquals(0.0F, PoisonRemedies.amount(new ItemStack(Items.HONEY_BLOCK), 40.0F, 20.0F));
        assertEquals(0.0F, PoisonRemedies.amount(PotionContents.createItemStack(Items.POTION, Potions.WATER), 40.0F, 20.0F));
        assertEquals(0.0F, PoisonRemedies.amount(ItemStack.EMPTY, 40.0F, 20.0F));
    }

    @Test
    void witherDamageIsCancelledOnlyForOurAmbientWither() {
        MobEffectInstance ours = new MobEffectInstance(MobEffects.WITHER, 240, 0, true, false, true);
        MobEffectInstance rose = new MobEffectInstance(MobEffects.WITHER, 40, 1);
        assertTrue(PoisonWither.isOurs(ours, true));
        assertFalse(PoisonWither.isOurs(ours, false), "threshold no longer reached");
        assertFalse(PoisonWither.isOurs(rose, true), "a wither rose deals its vanilla damage");
        assertFalse(PoisonWither.isOurs(null, true));
    }

    @Test
    void thePoisonModifierLeavesOtherConditionsAlone() {
        ToxicRainPoisonModifier modifier = new ToxicRainPoisonModifier();
        for (Condition condition : Condition.values()) {
            if (condition != Condition.POISON) {
                assertEquals(0.55F, modifier.modify(condition, null, 0.55F));
            }
        }
    }
}
