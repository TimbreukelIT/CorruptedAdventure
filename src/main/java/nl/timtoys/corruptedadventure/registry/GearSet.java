package nl.timtoys.corruptedadventure.registry;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.neoforged.neoforge.registries.DeferredItem;
import nl.timtoys.corruptedadventure.item.GearTier;

import java.util.List;

/**
 * The nine pieces of a {@link GearTier}'s set as registered in {@link ModItems}, so datagen and tests walk every set the
 * same way. {@link #PIECES} names the pieces in the order of {@link #pieces()}.
 */
public record GearSet(GearTier tier, DeferredItem<SwordItem> sword, DeferredItem<AxeItem> axe,
                      DeferredItem<PickaxeItem> pickaxe, DeferredItem<ShovelItem> shovel, DeferredItem<HoeItem> hoe,
                      DeferredItem<ArmorItem> helmet, DeferredItem<ArmorItem> chestplate,
                      DeferredItem<ArmorItem> leggings, DeferredItem<ArmorItem> boots) {
    /** The piece names, the suffix of every item id ({@code <tier>_<piece>}) and of vanilla's netherite set. */
    public static final List<String> PIECES = List.of("sword", "axe", "pickaxe", "shovel", "hoe", "helmet", "chestplate",
            "leggings", "boots");

    /** Sword, axe, pickaxe, shovel, hoe. */
    public List<DeferredItem<? extends Item>> tools() {
        return List.of(sword, axe, pickaxe, shovel, hoe);
    }

    /** Helmet, chestplate, leggings, boots. */
    public List<DeferredItem<ArmorItem>> armor() {
        return List.of(helmet, chestplate, leggings, boots);
    }

    /** All nine, in the order of {@link #PIECES}. */
    public List<DeferredItem<? extends Item>> pieces() {
        return List.of(sword, axe, pickaxe, shovel, hoe, helmet, chestplate, leggings, boots);
    }
}
