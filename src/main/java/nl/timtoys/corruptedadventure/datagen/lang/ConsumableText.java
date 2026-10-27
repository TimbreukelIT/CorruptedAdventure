package nl.timtoys.corruptedadventure.datagen.lang;

import nl.timtoys.corruptedadventure.registry.ModItems;
import nl.timtoys.corruptedadventure.registry.ModMobEffects;

/**
 * The consumables against the conditions and their status effects (phase 2 golf 2 stream V, golf 3 + 4 stream C).
 * English is mandatory, Dutch welcome.
 */
public final class ConsumableText {
    private ConsumableText() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.addItem(ModItems.CANTEEN, "Canteen");
        lang.add(ModItems.CANTEEN.get().sipsKey(), "Sips: %s/%s");
        lang.addItem(ModItems.CACTUS_JUICE, "Cactus Juice");
        lang.addItem(ModItems.SALTED_JERKY, "Salted Jerky");
        lang.addItem(ModItems.DEW_DRAUGHT, "Dew Draught");
        lang.addItem(ModItems.PURIFIED_WATER, "Purified Water");
        lang.addItem(ModItems.HAND_WARMER, "Hand Warmer");
        lang.add(ModItems.HAND_WARMER.get().chargesKey(), "Charges: %s/%s");
        lang.add(ModItems.HAND_WARMER.get().refillKey(), "Refill: click a fire crystal onto it");
        lang.add(ModMobEffects.COOLED.get().getDescriptionId(), "Cooled");
        lang.add(ModMobEffects.CLEANSED.get().getDescriptionId(), "Cleansed");
        lang.add(ModMobEffects.WARMED.get().getDescriptionId(), "Warmed");
    }

    public static void addDutch(LangOutput lang) {
        lang.addItem(ModItems.CANTEEN, "Veldfles");
        lang.add(ModItems.CANTEEN.get().sipsKey(), "Slokken: %s/%s");
        lang.addItem(ModItems.CACTUS_JUICE, "Cactussap");
        lang.addItem(ModItems.SALTED_JERKY, "Gezouten gedroogd vlees");
        lang.addItem(ModItems.DEW_DRAUGHT, "Dauwdrank");
        lang.addItem(ModItems.PURIFIED_WATER, "Zuiver water");
        lang.addItem(ModItems.HAND_WARMER, "Handwarmer");
        lang.add(ModItems.HAND_WARMER.get().chargesKey(), "Ladingen: %s/%s");
        lang.add(ModItems.HAND_WARMER.get().refillKey(), "Bijvullen: klik er een vuurkristal op");
        lang.add(ModMobEffects.COOLED.get().getDescriptionId(), "Verkoeld");
        lang.add(ModMobEffects.CLEANSED.get().getDescriptionId(), "Gezuiverd");
        lang.add(ModMobEffects.WARMED.get().getDescriptionId(), "Verwarmd");
    }
}
