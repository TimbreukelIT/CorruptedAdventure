package nl.timtoys.corruptedadventure.datagen.lang;

/**
 * Texts of the ring 5 conditions (void and silence: death messages). Owned by stream C
 * ({@code docs/plans/ring-5/waves.md}); English is mandatory, Dutch welcome. The meters have no text of their own
 * (the HUD shows a bar, {@code /ca condition} the serialized name).
 */
public final class Ring5ConditionText {
    private static final String VOID_DEATH = "death.attack.corruptedadventure.void";

    private Ring5ConditionText() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.add(VOID_DEATH, "%1$s was unmade by the void");
        lang.add(VOID_DEATH + ".player", "%1$s was unmade by the void whilst fighting %2$s");
    }

    public static void addDutch(LangOutput lang) {
        lang.add(VOID_DEATH, "%1$s werd door de void uitgewist");
        lang.add(VOID_DEATH + ".player", "%1$s werd door de void uitgewist tijdens het gevecht met %2$s");
    }
}
