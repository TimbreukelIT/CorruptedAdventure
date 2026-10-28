package nl.timtoys.corruptedadventure.datagen.lang;

import nl.timtoys.corruptedadventure.registry.ModItems;

/** The sky glass lens (phase 2 golf 5, stream L). English is mandatory, Dutch welcome. */
public final class LensText {
    private LensText() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.addItem(ModItems.SKY_GLASS_LENS, "Sky Glass Lens");
    }

    public static void addDutch(LangOutput lang) {
        lang.addItem(ModItems.SKY_GLASS_LENS, "Hemelglaslens");
    }
}
