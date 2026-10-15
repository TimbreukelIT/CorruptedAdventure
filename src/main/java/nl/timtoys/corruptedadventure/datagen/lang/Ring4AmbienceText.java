package nl.timtoys.corruptedadventure.datagen.lang;

/**
 * Subtitles of the ring 4 ambience (stream A): for now the wildlife of {@code verdant_falls}; its waterfall loop has
 * none, like vanilla's loops. English is mandatory, Dutch welcome.
 */
public final class Ring4AmbienceText {
    private static final String AMBIENT = "subtitles.corruptedadventure.ambient.";

    private Ring4AmbienceText() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.add(AMBIENT + "verdant_falls.wildlife", "Tropical wildlife calls");
    }

    public static void addDutch(LangOutput lang) {
        lang.add(AMBIENT + "verdant_falls.wildlife", "Tropische dieren roepen");
    }
}
