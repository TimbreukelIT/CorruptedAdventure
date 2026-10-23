package nl.timtoys.corruptedadventure.datagen.lang;

/**
 * Texts of the ring 5 weather and ambience: subtitles (packages 92 and 98). Owned by stream A
 * ({@code docs/plans/ring-5/waves.md}); English is mandatory, Dutch welcome. Loops (the void drone, the silent music)
 * carry no subtitle, like vanilla's.
 */
public final class Ring5WeatherText {
    private static final String SUBTITLES = "subtitles.corruptedadventure.weather.ring_5.";
    private static final String AMBIENT = "subtitles.corruptedadventure.ambient.ring_5.";
    private static final String NETHER_AMBIENT = "subtitles.corruptedadventure.ambient.nether_ring_5.";

    private Ring5WeatherText() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.add(SUBTITLES + "glitch_storm", "Glitch storm stutters");
        lang.add(SUBTITLES + "reverse_rain", "Rain rises");
        lang.add(SUBTITLES + "standstill", "The world stands still");
        lang.add(AMBIENT + "glitch", "Something glitches");
        lang.add(AMBIENT + "echo_step", "Footsteps behind you");
        lang.add(NETHER_AMBIENT + "heartbeat", "Heart beats");
    }

    public static void addDutch(LangOutput lang) {
        lang.add(SUBTITLES + "glitch_storm", "Glitchstorm hapert");
        lang.add(SUBTITLES + "reverse_rain", "Regen stijgt op");
        lang.add(SUBTITLES + "standstill", "De wereld staat stil");
        lang.add(AMBIENT + "glitch", "Iets hapert");
        lang.add(AMBIENT + "echo_step", "Voetstappen achter je");
        lang.add(NETHER_AMBIENT + "heartbeat", "Hart klopt");
    }
}
