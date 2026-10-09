package nl.timtoys.corruptedadventure.datagen.lang;

/**
 * Subtitles of the ring 3 and Nether ring 3 ambience and of the ring 3 block sounds (package 50-0). Owned by stream A;
 * the loops (embers, titan hum, caldera, wind) have none, like vanilla's loops. English is mandatory, Dutch welcome.
 */
public final class Ring3AmbienceText {
    private static final String AMBIENT = "subtitles.corruptedadventure.ambient.";
    private static final String BLOCK = "subtitles.corruptedadventure.block.";

    private Ring3AmbienceText() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.add(AMBIENT + "ring_3.rumble", "Ground rumbles");
        lang.add(AMBIENT + "ring_3.lava", "Lava bubbles");
        lang.add(AMBIENT + "nether_ring_3.ice_creak", "Ice creaks");
        lang.add(BLOCK + "fumarole.puff", "Fumarole hisses");
        lang.add(BLOCK + "geyser.burst", "Geyser erupts");
        lang.add(BLOCK + "geyser.warning", "Geyser gurgles");
        lang.add(BLOCK + "boiling_mud.bubble", "Mud bubbles");
    }

    public static void addDutch(LangOutput lang) {
        lang.add(AMBIENT + "ring_3.rumble", "Grond rommelt");
        lang.add(AMBIENT + "ring_3.lava", "Lava borrelt");
        lang.add(AMBIENT + "nether_ring_3.ice_creak", "IJs kraakt");
        lang.add(BLOCK + "fumarole.puff", "Fumarole sist");
        lang.add(BLOCK + "geyser.burst", "Geiser spuit");
        lang.add(BLOCK + "geyser.warning", "Geiser gorgelt");
        lang.add(BLOCK + "boiling_mud.bubble", "Modder borrelt");
    }
}
