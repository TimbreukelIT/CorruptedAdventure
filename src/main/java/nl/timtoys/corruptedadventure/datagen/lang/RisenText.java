package nl.timtoys.corruptedadventure.datagen.lang;

import nl.timtoys.corruptedadventure.registry.ModEntityTypes;
import nl.timtoys.corruptedadventure.registry.ModMobEffects;

/**
 * The Risen, the ring 3 Nightmare (phase 4, stream R3): its name (also on the boss bar), the ash walkers it lets fall
 * and its ash effect. English is mandatory, Dutch welcome.
 */
public final class RisenText {
    private RisenText() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.add(ModEntityTypes.THE_RISEN.get().getDescriptionId(), "The Risen");
        lang.add(ModEntityTypes.ASH_WALKER.get().getDescriptionId(), "Ash Walker");
        lang.add(ModMobEffects.ASHEN.get().getDescriptionId(), "Ashen");
    }

    public static void addDutch(LangOutput lang) {
        lang.add(ModEntityTypes.THE_RISEN.get().getDescriptionId(), "De Herrijzende");
        lang.add(ModEntityTypes.ASH_WALKER.get().getDescriptionId(), "Aswandelaar");
        lang.add(ModMobEffects.ASHEN.get().getDescriptionId(), "Onder de as");
    }
}
