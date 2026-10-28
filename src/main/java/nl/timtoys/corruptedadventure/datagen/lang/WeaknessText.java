package nl.timtoys.corruptedadventure.datagen.lang;

import nl.timtoys.corruptedadventure.registry.ModItems;
import nl.timtoys.corruptedadventure.registry.ModMobEffects;

/**
 * The items against the Nightmares' weaknesses and their status effects (phase 4, stream I). English is mandatory, Dutch
 * welcome.
 */
public final class WeaknessText {
    private static final String FLASH_SUBTITLE = "subtitles.corruptedadventure.item.sunstone_flash.flash";
    private static final String HORN_SUBTITLE = "subtitles.corruptedadventure.item.volcanite_horn.blow";

    private WeaknessText() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.addItem(ModItems.SUNSTONE_FLASH, "Sunstone Flash");
        lang.add(FLASH_SUBTITLE, "Sunstone flashes");
        lang.addItem(ModItems.VOLCANITE_HORN, "Volcanite Horn");
        lang.add(HORN_SUBTITLE, "Volcanite horn sounds");
        lang.add(ModMobEffects.DAZZLED.get().getDescriptionId(), "Dazzled");
        lang.add(ModMobEffects.STUNNED.get().getDescriptionId(), "Stunned");
    }

    public static void addDutch(LangOutput lang) {
        lang.addItem(ModItems.SUNSTONE_FLASH, "Zonnesteenflits");
        lang.add(FLASH_SUBTITLE, "Zonnesteen flitst");
        lang.addItem(ModItems.VOLCANITE_HORN, "Vulkanieten hoorn");
        lang.add(HORN_SUBTITLE, "Vulkanieten hoorn schalt");
        lang.add(ModMobEffects.DAZZLED.get().getDescriptionId(), "Verblind");
        lang.add(ModMobEffects.STUNNED.get().getDescriptionId(), "Verstijfd");
    }
}
