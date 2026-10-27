package nl.timtoys.corruptedadventure.condition;

import nl.timtoys.corruptedadventure.config.ServerConfig;

/** The doses of the consumables (fase-2-items.md §5.3–§5.4), from {@code [conditions.dehydration]}, read live. */
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
}
