package nl.timtoys.corruptedadventure.datagen.lang;

/**
 * Texts of the ring 3 weather and ambience: subtitles. Owned by stream A ({@code docs/plans/ring-3/waves.md});
 * English is mandatory, Dutch welcome.
 */
public final class Ring3WeatherText {
    private static final String SUBTITLES = "subtitles.corruptedadventure.weather.ring_3.";

    private Ring3WeatherText() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.add(SUBTITLES + "ash_rain", "Ash rain crackles");
        lang.add(SUBTITLES + "eruption", "Volcano rumbles");
        lang.add(SUBTITLES + "ash_cloud", "Ash cloud roars");
    }

    public static void addDutch(LangOutput lang) {
        lang.add(SUBTITLES + "ash_rain", "Asregen knispert");
        lang.add(SUBTITLES + "eruption", "Vulkaan rommelt");
        lang.add(SUBTITLES + "ash_cloud", "Aswolk raast");
    }
}
