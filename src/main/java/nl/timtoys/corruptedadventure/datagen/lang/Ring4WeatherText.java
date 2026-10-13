package nl.timtoys.corruptedadventure.datagen.lang;

/**
 * Texts of the ring 4 weather: subtitles. Owned by stream A
 * ({@code docs/plans/ring-4/waves.md}); English is mandatory, Dutch welcome.
 */
public final class Ring4WeatherText {
    private static final String SUBTITLES = "subtitles.corruptedadventure.weather.ring_4.";

    private Ring4WeatherText() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.add(SUBTITLES + "dense_fog", "Fog muffles the world");
        lang.add(SUBTITLES + "storm_wind", "Storm wind howls");
        lang.add(SUBTITLES + "toxic_rain", "Toxic rain hisses");
    }

    public static void addDutch(LangOutput lang) {
        lang.add(SUBTITLES + "dense_fog", "Mist dempt de wereld");
        lang.add(SUBTITLES + "storm_wind", "Stormwind huilt");
        lang.add(SUBTITLES + "toxic_rain", "Gifregen sist");
    }
}
