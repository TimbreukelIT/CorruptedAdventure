package nl.timtoys.corruptedadventure.datagen.lang;

/**
 * Texts of the ring 3 conditions (heat, cold): meters and death messages. Owned by stream C
 * ({@code docs/plans/ring-3/waves.md}); English is mandatory, Dutch welcome. The meters have no text of their own
 * (the HUD shows a bar, {@code /ca condition} the serialized name).
 */
public final class Ring3ConditionText {
    private static final String HEAT_DEATH = "death.attack.corruptedadventure.heat";
    private static final String COLD_DEATH = "death.attack.corruptedadventure.cold";

    private Ring3ConditionText() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.add(HEAT_DEATH, "%1$s succumbed to the heat");
        lang.add(HEAT_DEATH + ".player", "%1$s succumbed to the heat whilst fighting %2$s");
        lang.add(COLD_DEATH, "%1$s froze in the cold of the Nether");
        lang.add(COLD_DEATH + ".player", "%1$s froze in the cold of the Nether whilst fighting %2$s");
    }

    public static void addDutch(LangOutput lang) {
        lang.add(HEAT_DEATH, "%1$s bezweek aan de hitte");
        lang.add(HEAT_DEATH + ".player", "%1$s bezweek aan de hitte tijdens het gevecht met %2$s");
        lang.add(COLD_DEATH, "%1$s bevroor in de kou van de Nether");
        lang.add(COLD_DEATH + ".player", "%1$s bevroor in de kou van de Nether tijdens het gevecht met %2$s");
    }
}
