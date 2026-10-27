package nl.timtoys.corruptedadventure.datagen.lang;

import nl.timtoys.corruptedadventure.registry.ModEntityTypes;
import nl.timtoys.corruptedadventure.registry.ModItems;

/** The fire and poison arrows, item and entity (phase 2 golf 3 + 4, stream A). English is mandatory, Dutch welcome. */
public final class ArrowText {
    private ArrowText() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.addItem(ModItems.FIRE_ARROW, "Fire Arrow");
        lang.add(ModEntityTypes.FIRE_ARROW.get().getDescriptionId(), "Fire Arrow");
        lang.addItem(ModItems.POISON_ARROW, "Poison Arrow");
        lang.add(ModEntityTypes.POISON_ARROW.get().getDescriptionId(), "Poison Arrow");
    }

    public static void addDutch(LangOutput lang) {
        lang.addItem(ModItems.FIRE_ARROW, "Brandpijl");
        lang.add(ModEntityTypes.FIRE_ARROW.get().getDescriptionId(), "Brandpijl");
        lang.addItem(ModItems.POISON_ARROW, "Gifpijl");
        lang.add(ModEntityTypes.POISON_ARROW.get().getDescriptionId(), "Gifpijl");
    }
}
