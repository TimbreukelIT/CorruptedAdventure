package nl.timtoys.corruptedadventure.datagen.lang;

import nl.timtoys.corruptedadventure.registry.ModItems;
import nl.timtoys.corruptedadventure.registry.ModMobEffects;

/** The consumables against dehydration and their status effect (phase 2 golf 2, stream V). English is mandatory, Dutch welcome. */
public final class ConsumableText {
    private ConsumableText() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.addItem(ModItems.CANTEEN, "Canteen");
        lang.add(ModItems.CANTEEN.get().sipsKey(), "Sips: %s/%s");
        lang.addItem(ModItems.CACTUS_JUICE, "Cactus Juice");
        lang.addItem(ModItems.SALTED_JERKY, "Salted Jerky");
        lang.add(ModMobEffects.COOLED.get().getDescriptionId(), "Cooled");
    }

    public static void addDutch(LangOutput lang) {
        lang.addItem(ModItems.CANTEEN, "Veldfles");
        lang.add(ModItems.CANTEEN.get().sipsKey(), "Slokken: %s/%s");
        lang.addItem(ModItems.CACTUS_JUICE, "Cactussap");
        lang.addItem(ModItems.SALTED_JERKY, "Gezouten gedroogd vlees");
        lang.add(ModMobEffects.COOLED.get().getDescriptionId(), "Verkoeld");
    }
}
