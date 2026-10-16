package nl.timtoys.corruptedadventure.datagen.lang;

/**
 * Subtitles of the ring 4 and Nether ring 4 ambience and of the ring 4 block sounds (stream A). The loops (water,
 * sundered drone, Nether wind, the {@code verdant_falls} falls) have none, like vanilla's loops. English is mandatory,
 * Dutch welcome.
 */
public final class Ring4AmbienceText {
    private static final String AMBIENT = "subtitles.corruptedadventure.ambient.";
    private static final String BLOCK = "subtitles.corruptedadventure.block.";

    private Ring4AmbienceText() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.add(AMBIENT + "verdant_falls.wildlife", "Tropical wildlife calls");
        lang.add(AMBIENT + "ring_4.night_choir", "Distant voices hum");
        lang.add(AMBIENT + "ring_4.reeds", "Reeds rustle");
        lang.add(AMBIENT + "ring_4.bubbles", "Bubbles rise from the deep");
        lang.add(AMBIENT + "nether_ring_4.crystal_hum", "Crystals hum");
        lang.add(BLOCK + "swamp_gas.ignite", "Swamp gas ignites");
        lang.add(BLOCK + "bubble_spring.bubble", "Bubble spring bubbles");
        lang.add(BLOCK + "updraft.flow", "Updraft rushes");
        lang.add(BLOCK + "sucking_mud.slurp", "Mud slurps");
    }

    public static void addDutch(LangOutput lang) {
        lang.add(AMBIENT + "verdant_falls.wildlife", "Tropische dieren roepen");
        lang.add(AMBIENT + "ring_4.night_choir", "Verre stemmen neuriën");
        lang.add(AMBIENT + "ring_4.reeds", "Riet ritselt");
        lang.add(AMBIENT + "ring_4.bubbles", "Bellen stijgen uit de diepte");
        lang.add(AMBIENT + "nether_ring_4.crystal_hum", "Kristallen zoemen");
        lang.add(BLOCK + "swamp_gas.ignite", "Moerasgas ontbrandt");
        lang.add(BLOCK + "bubble_spring.bubble", "Bellenbron borrelt");
        lang.add(BLOCK + "updraft.flow", "Opwaartse stroom ruist");
        lang.add(BLOCK + "sucking_mud.slurp", "Modder slurpt");
    }
}
