package nl.timtoys.corruptedadventure.datagen.lang;

import nl.timtoys.corruptedadventure.registry.ModBlocks;

/**
 * Names of the phase 2 golf 4 material blocks (stream R4): the jade inlaid tidestone and the aquamarine inlaid lagoon
 * sandstone sets. English is mandatory, Dutch welcome.
 */
public final class Golf4MaterialNames {
    private Golf4MaterialNames() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.addBlock(ModBlocks.JADE_INLAID_TIDESTONE, "Jade Inlaid Tidestone");
        lang.addBlock(ModBlocks.JADE_INLAID_TIDESTONE_STAIRS, "Jade Inlaid Tidestone Stairs");
        lang.addBlock(ModBlocks.JADE_INLAID_TIDESTONE_SLAB, "Jade Inlaid Tidestone Slab");
        lang.addBlock(ModBlocks.JADE_INLAID_TIDESTONE_WALL, "Jade Inlaid Tidestone Wall");
        lang.addBlock(ModBlocks.AQUAMARINE_INLAID_LAGOON_SANDSTONE, "Aquamarine Inlaid Lagoon Sandstone");
        lang.addBlock(ModBlocks.AQUAMARINE_INLAID_LAGOON_SANDSTONE_STAIRS, "Aquamarine Inlaid Lagoon Sandstone Stairs");
        lang.addBlock(ModBlocks.AQUAMARINE_INLAID_LAGOON_SANDSTONE_SLAB, "Aquamarine Inlaid Lagoon Sandstone Slab");
        lang.addBlock(ModBlocks.AQUAMARINE_INLAID_LAGOON_SANDSTONE_WALL, "Aquamarine Inlaid Lagoon Sandstone Wall");
    }

    public static void addDutch(LangOutput lang) {
        lang.addBlock(ModBlocks.JADE_INLAID_TIDESTONE, "Met jade ingelegd getijdesteen");
        lang.addBlock(ModBlocks.JADE_INLAID_TIDESTONE_STAIRS, "Met jade ingelegde getijdestenen trap");
        lang.addBlock(ModBlocks.JADE_INLAID_TIDESTONE_SLAB, "Met jade ingelegde getijdestenen plaat");
        lang.addBlock(ModBlocks.JADE_INLAID_TIDESTONE_WALL, "Met jade ingelegde getijdestenen muur");
        lang.addBlock(ModBlocks.AQUAMARINE_INLAID_LAGOON_SANDSTONE, "Met aquamarijn ingelegd lagunezandsteen");
        lang.addBlock(ModBlocks.AQUAMARINE_INLAID_LAGOON_SANDSTONE_STAIRS, "Met aquamarijn ingelegde lagunezandstenen trap");
        lang.addBlock(ModBlocks.AQUAMARINE_INLAID_LAGOON_SANDSTONE_SLAB, "Met aquamarijn ingelegde lagunezandstenen plaat");
        lang.addBlock(ModBlocks.AQUAMARINE_INLAID_LAGOON_SANDSTONE_WALL, "Met aquamarijn ingelegde lagunezandstenen muur");
    }
}
