package nl.timtoys.corruptedadventure.condition;

import nl.timtoys.corruptedadventure.config.ServerConfig;

/**
 * The doses of the consumables (fase-2-items.md §5.3–§5.4, §6.3, §6.6, §7.3), from the section of their condition
 * ({@code [conditions.dehydration]}, {@code heat}, {@code poison}, {@code cold}, {@code void}, {@code silence}), read
 * live.
 */
public final class ConsumableDoses {
    private ConsumableDoses() {
    }

    /** One sip from the canteen: −25 and 45 s cooled. */
    public static ConditionDose canteenSip() {
        ServerConfig.DehydrationCondition config = ServerConfig.DEHYDRATION;
        return ConditionDose.withEffect(Condition.DEHYDRATION, -config.canteenSipAmount.get().floatValue(),
                RateEffect.COOLED, config.canteenCooledSeconds.get());
    }

    /** One cactus juice: −15 and 20 s cooled. */
    public static ConditionDose cactusJuice() {
        ServerConfig.DehydrationCondition config = ServerConfig.DEHYDRATION;
        return ConditionDose.withEffect(Condition.DEHYDRATION, -config.cactusJuiceAmount.get().floatValue(),
                RateEffect.COOLED, config.cactusJuiceCooledSeconds.get());
    }

    /** One salted jerky: +8, good food that makes you thirsty. */
    public static ConditionDose saltedJerky() {
        return ConditionDose.of(Condition.DEHYDRATION, ServerConfig.DEHYDRATION.saltedJerkyAmount.get().floatValue());
    }

    /** One dew draught: −20 heat and 45 s cooled. */
    public static ConditionDose dewDraught() {
        ServerConfig.HeatCondition config = ServerConfig.HEAT;
        return ConditionDose.withEffect(Condition.HEAT, -config.dewDraughtAmount.get().floatValue(),
                RateEffect.COOLED, config.dewDraughtCooledSeconds.get());
    }

    /** One purified water: −20 poison and 45 s cleansed. */
    public static ConditionDose purifiedWater() {
        ServerConfig.PoisonCondition config = ServerConfig.POISON;
        return ConditionDose.withEffect(Condition.POISON, -config.purifiedWaterAmount.get().floatValue(),
                RateEffect.CLEANSED, config.purifiedWaterCleansedSeconds.get());
    }

    /** One hand warmer charge: −20 cold and 45 s warmed. */
    public static ConditionDose handWarmerCharge() {
        ServerConfig.ColdCondition config = ServerConfig.COLD;
        return ConditionDose.withEffect(Condition.COLD, -config.handWarmerAmount.get().floatValue(),
                RateEffect.WARMED, config.handWarmerWarmedSeconds.get());
    }

    /** One sky light: −20 void and 45 s anchored. */
    public static ConditionDose skyLight() {
        ServerConfig.VoidCondition config = ServerConfig.VOID;
        return ConditionDose.withEffect(Condition.VOID, -config.skyLightAmount.get().floatValue(),
                RateEffect.ANCHORED, config.skyLightAnchoredSeconds.get());
    }

    /** One blow on the echo shell, for the one who blows it: −20 silence and 45 s resounding. */
    public static ConditionDose echoShellBlow() {
        ServerConfig.SilenceCondition config = ServerConfig.SILENCE;
        return ConditionDose.withEffect(Condition.SILENCE, -config.echoShellAmount.get().floatValue(),
                RateEffect.RESOUNDING, config.echoShellResoundingSeconds.get());
    }
}
