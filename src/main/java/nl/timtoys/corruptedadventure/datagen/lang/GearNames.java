package nl.timtoys.corruptedadventure.datagen.lang;

import nl.timtoys.corruptedadventure.item.GearTier;
import nl.timtoys.corruptedadventure.registry.GearSet;
import nl.timtoys.corruptedadventure.registry.ModItems;

import java.util.List;
import java.util.Map;

/**
 * The names of the gear tier sets, like vanilla's netherite set: English "Sunstone Sword", Dutch the material as an
 * adjective, like vanilla's "Netherieten zwaard" and "Stenen zwaard" ("Zonnestenen zwaard", "Getijdezilveren zwaard").
 */
public final class GearNames {
    private GearNames() {
    }

    public static void addEnglish(LangOutput lang) {
        add(lang, Map.of(GearTier.SUNSTONE, "Sunstone ", GearTier.VOLCANITE, "Volcanite ", GearTier.TIDESILVER, "Tidesilver "),
                List.of("Sword", "Axe", "Pickaxe", "Shovel", "Hoe", "Helmet", "Chestplate", "Leggings", "Boots"));
    }

    public static void addDutch(LangOutput lang) {
        add(lang, Map.of(GearTier.SUNSTONE, "Zonnestenen ", GearTier.VOLCANITE, "Vulkanieten ",
                        GearTier.TIDESILVER, "Getijdezilveren "),
                List.of("zwaard", "bijl", "houweel", "schep", "schoffel", "helm", "kuras", "beenstukken", "laarzen"));
    }

    /** {@code pieces} in the order of {@link GearSet#PIECES}. */
    private static void add(LangOutput lang, Map<GearTier, String> materials, List<String> pieces) {
        for (GearSet set : ModItems.GEAR_SETS) {
            for (int i = 0; i < pieces.size(); i++) {
                lang.addItem(set.pieces().get(i), materials.get(set.tier()) + pieces.get(i));
            }
        }
    }
}
