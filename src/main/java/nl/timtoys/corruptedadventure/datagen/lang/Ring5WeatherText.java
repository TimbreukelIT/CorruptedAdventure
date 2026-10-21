package nl.timtoys.corruptedadventure.datagen.lang;

/**
 * Texts of the ring 5 weather: subtitles (package 92). Owned by stream A ({@code docs/plans/ring-5/waves.md});
 * English is mandatory, Dutch welcome.
 */
public final class Ring5WeatherText {
    private static final String SUBTITLES = "subtitles.corruptedadventure.weather.ring_5.";

    private Ring5WeatherText() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.add(SUBTITLES + "glitch_storm", "Glitch storm stutters");
        lang.add(SUBTITLES + "reverse_rain", "Rain rises");
        lang.add(SUBTITLES + "standstill", "The world stands still");
    }

    public static void addDutch(LangOutput lang) {
        lang.add(SUBTITLES + "glitch_storm", "Glitchstorm hapert");
        lang.add(SUBTITLES + "reverse_rain", "Regen stijgt op");
        lang.add(SUBTITLES + "standstill", "De wereld staat stil");
    }
}
