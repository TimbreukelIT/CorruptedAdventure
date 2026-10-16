package nl.timtoys.corruptedadventure.datagen.lang;

/**
 * Texts of the ring 4 conditions (poison, purifying, sucking mud): meters and death messages. Owned by stream C
 * ({@code docs/plans/ring-4/waves.md}); English is mandatory, Dutch welcome. The meters have no text of their own
 * (the HUD shows a bar, {@code /ca condition} the serialized name).
 */
public final class Ring4ConditionText {
    private static final String POISON_DEATH = "death.attack.corruptedadventure.poison";
    private static final String SUCKING_MUD_DEATH = "death.attack.corruptedadventure.sucking_mud";

    private Ring4ConditionText() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.add(POISON_DEATH, "%1$s succumbed to the poison of the blighted marsh");
        lang.add(POISON_DEATH + ".player", "%1$s succumbed to the poison of the blighted marsh whilst fighting %2$s");
        lang.add(SUCKING_MUD_DEATH, "%1$s was swallowed by the sucking mud");
        lang.add(SUCKING_MUD_DEATH + ".player", "%1$s was swallowed by the sucking mud whilst fighting %2$s");
    }

    public static void addDutch(LangOutput lang) {
        lang.add(POISON_DEATH, "%1$s bezweek aan het gif van het bedorven moeras");
        lang.add(POISON_DEATH + ".player", "%1$s bezweek aan het gif van het bedorven moeras tijdens het gevecht met %2$s");
        lang.add(SUCKING_MUD_DEATH, "%1$s werd opgeslokt door de zuigmodder");
        lang.add(SUCKING_MUD_DEATH + ".player", "%1$s werd opgeslokt door de zuigmodder tijdens het gevecht met %2$s");
    }
}
