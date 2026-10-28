package nl.timtoys.corruptedadventure.datagen.lang;

import nl.timtoys.corruptedadventure.registry.ModMobEffects;

/**
 * The items against the Nightmares' weaknesses and their status effects (phase 4, stream I). English is mandatory, Dutch
 * welcome.
 */
public final class WeaknessText {
    private WeaknessText() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.add(ModMobEffects.DAZZLED.get().getDescriptionId(), "Dazzled");
        lang.add(ModMobEffects.STUNNED.get().getDescriptionId(), "Stunned");
    }

    public static void addDutch(LangOutput lang) {
        lang.add(ModMobEffects.DAZZLED.get().getDescriptionId(), "Verblind");
        lang.add(ModMobEffects.STUNNED.get().getDescriptionId(), "Verstijfd");
    }
}
