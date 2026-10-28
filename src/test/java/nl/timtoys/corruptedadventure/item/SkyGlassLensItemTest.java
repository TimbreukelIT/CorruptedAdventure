package nl.timtoys.corruptedadventure.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.neoforged.neoforge.common.ItemAbilities;
import nl.timtoys.corruptedadventure.registry.ModCreativeTabs;
import nl.timtoys.corruptedadventure.registry.ModItems;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkyGlassLensItemTest {
    @Test
    void theLensIsASpyglassOfOneInTheTabOfRing4() {
        ItemStack lens = new ItemStack(ModItems.SKY_GLASS_LENS.get());
        assertEquals(1, lens.getMaxStackSize());
        // NeoForge's Player.isScoping asks this: vanilla's zoom, scope, hand and mouse follow from it.
        assertTrue(lens.canPerformAction(ItemAbilities.SPYGLASS_SCOPE));
        assertEquals(UseAnim.SPYGLASS, lens.getUseAnimation());
        assertTrue(ModCreativeTabs.contents(4).contains(ModItems.SKY_GLASS_LENS.get()), "Nether ring 4 sits in tab 4");
    }
}
