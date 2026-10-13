package nl.timtoys.corruptedadventure.datagen.lang;

/**
 * Texts of the ring 4 conditions (poison, purifying): meters and death messages. Owned by stream C
 * ({@code docs/plans/ring-4/waves.md}); English is mandatory, Dutch welcome. The meter name is not shown yet (the HUD
 * draws a bar, {@code /ca condition} the serialized name); it is there for a labelled HUD later.
 */
public final class Ring4ConditionText {
    private static final String POISON_METER = "condition.corruptedadventure.poison";
    private static final String POISON_DEATH = "death.attack.corruptedadventure.poison";

    private Ring4ConditionText() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.add(POISON_METER, "Poison");
        lang.add(POISON_DEATH, "%1$s succumbed to the poison of the blighted marsh");
        lang.add(POISON_DEATH + ".player", "%1$s succumbed to the poison of the blighted marsh whilst fighting %2$s");
    }

    public static void addDutch(LangOutput lang) {
        lang.add(POISON_METER, "Gif");
        lang.add(POISON_DEATH, "%1$s bezweek aan het gif van het bedorven moeras");
        lang.add(POISON_DEATH + ".player", "%1$s bezweek aan het gif van het bedorven moeras tijdens het gevecht met %2$s");
    }
}
