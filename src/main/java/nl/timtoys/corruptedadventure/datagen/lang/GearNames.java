package nl.timtoys.corruptedadventure.datagen.lang;

import net.minecraft.world.item.Item;
import nl.timtoys.corruptedadventure.registry.ModItems;

import java.util.List;
import java.util.function.Supplier;

/**
 * The names of the gear tier sets (golf 2: sunstone), like vanilla's netherite set: English "Sunstone Sword", Dutch the
 * material as an adjective, like vanilla's "Netherieten zwaard" and "Stenen zwaard" ("Zonnestenen zwaard").
 */
public final class GearNames {
    private static final List<Supplier<? extends Item>> SUNSTONE = List.of(ModItems.SUNSTONE_SWORD, ModItems.SUNSTONE_AXE,
            ModItems.SUNSTONE_PICKAXE, ModItems.SUNSTONE_SHOVEL, ModItems.SUNSTONE_HOE, ModItems.SUNSTONE_HELMET,
            ModItems.SUNSTONE_CHESTPLATE, ModItems.SUNSTONE_LEGGINGS, ModItems.SUNSTONE_BOOTS);

    private GearNames() {
    }

    public static void addEnglish(LangOutput lang) {
        add(lang, "Sunstone ", List.of("Sword", "Axe", "Pickaxe", "Shovel", "Hoe", "Helmet", "Chestplate", "Leggings", "Boots"));
    }

    public static void addDutch(LangOutput lang) {
        add(lang, "Zonnestenen ", List.of("zwaard", "bijl", "houweel", "schep", "schoffel", "helm", "kuras", "beenstukken",
                "laarzen"));
    }

    /** {@code pieces} in the order of {@link #SUNSTONE}. */
    private static void add(LangOutput lang, String material, List<String> pieces) {
        for (int i = 0; i < SUNSTONE.size(); i++) {
            lang.addItem(SUNSTONE.get(i), material + pieces.get(i));
        }
    }
}
