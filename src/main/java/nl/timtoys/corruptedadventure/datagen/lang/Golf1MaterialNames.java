package nl.timtoys.corruptedadventure.datagen.lang;

import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModCreativeTabs;
import nl.timtoys.corruptedadventure.registry.ModItems;

/**
 * Names of the creative tabs per ring and of the phase 2 golf 1 materials: the ingots, the turquoise gem and the storage
 * blocks (stream D). English is mandatory, Dutch welcome.
 */
public final class Golf1MaterialNames {
    private static final int RINGS = 5;

    private Golf1MaterialNames() {
    }

    public static void addEnglish(LangOutput lang) {
        for (int ring = 1; ring <= RINGS; ring++) {
            lang.add(ModCreativeTabs.titleKey(ring), "Corrupted Adventure: Ring " + ring);
        }
        lang.addItem(ModItems.SUNSTONE_INGOT, "Sunstone Ingot");
        lang.addItem(ModItems.TURQUOISE, "Turquoise");
        lang.addItem(ModItems.VOLCANITE_INGOT, "Volcanite Ingot");
        lang.addItem(ModItems.TIDESILVER_INGOT, "Tidesilver Ingot");
        lang.addItem(ModItems.NULLITE_INGOT, "Nullite Ingot");
        lang.addBlock(ModBlocks.RAW_SUNSTONE_BLOCK, "Block of Raw Sunstone");
        lang.addBlock(ModBlocks.RAW_TURQUOISE_BLOCK, "Block of Raw Turquoise");
        lang.addBlock(ModBlocks.SUNSTONE_BLOCK, "Block of Sunstone");
        lang.addBlock(ModBlocks.TURQUOISE_BLOCK, "Block of Turquoise");
        lang.addBlock(ModBlocks.RAW_VOLCANITE_BLOCK, "Block of Raw Volcanite");
        lang.addBlock(ModBlocks.VOLCANITE_BLOCK, "Block of Volcanite");
        lang.addBlock(ModBlocks.FIRE_RUBY_BLOCK, "Block of Fire Ruby");
        lang.addBlock(ModBlocks.RAW_TIDESILVER_BLOCK, "Block of Raw Tidesilver");
        lang.addBlock(ModBlocks.TIDESILVER_BLOCK, "Block of Tidesilver");
        lang.addBlock(ModBlocks.JADE_BLOCK, "Block of Jade");
        lang.addBlock(ModBlocks.RAW_NULLITE_BLOCK, "Block of Raw Nullite");
        lang.addBlock(ModBlocks.NULLITE_BLOCK, "Block of Nullite");
    }

    public static void addDutch(LangOutput lang) {
        for (int ring = 1; ring <= RINGS; ring++) {
            lang.add(ModCreativeTabs.titleKey(ring), "Corrupted Adventure: ring " + ring);
        }
        lang.addItem(ModItems.SUNSTONE_INGOT, "Zonnesteenstaaf");
        lang.addItem(ModItems.TURQUOISE, "Turkoois");
        lang.addItem(ModItems.VOLCANITE_INGOT, "Vulkanietstaaf");
        lang.addItem(ModItems.TIDESILVER_INGOT, "Getijdezilverstaaf");
        lang.addItem(ModItems.NULLITE_INGOT, "Nullietstaaf");
        lang.addBlock(ModBlocks.RAW_SUNSTONE_BLOCK, "Blok ruwe zonnesteen");
        lang.addBlock(ModBlocks.RAW_TURQUOISE_BLOCK, "Blok ruw turkoois");
        lang.addBlock(ModBlocks.SUNSTONE_BLOCK, "Zonnesteenblok");
        lang.addBlock(ModBlocks.TURQUOISE_BLOCK, "Turkooisblok");
        lang.addBlock(ModBlocks.RAW_VOLCANITE_BLOCK, "Blok ruw vulkaniet");
        lang.addBlock(ModBlocks.VOLCANITE_BLOCK, "Vulkanietblok");
        lang.addBlock(ModBlocks.FIRE_RUBY_BLOCK, "Vuurrobijnblok");
        lang.addBlock(ModBlocks.RAW_TIDESILVER_BLOCK, "Blok ruw getijdezilver");
        lang.addBlock(ModBlocks.TIDESILVER_BLOCK, "Getijdezilverblok");
        lang.addBlock(ModBlocks.JADE_BLOCK, "Jadeblok");
        lang.addBlock(ModBlocks.RAW_NULLITE_BLOCK, "Blok ruw nulliet");
        lang.addBlock(ModBlocks.NULLITE_BLOCK, "Nullietblok");
    }
}
