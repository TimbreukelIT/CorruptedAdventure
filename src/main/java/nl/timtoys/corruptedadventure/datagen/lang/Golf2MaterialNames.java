package nl.timtoys.corruptedadventure.datagen.lang;

import nl.timtoys.corruptedadventure.registry.ModBlocks;

/**
 * Names of the phase 2 golf 2 material blocks (stream M): the soul crystal block, the turquoise inlaid sandstone set and
 * the salt lamp. English is mandatory, Dutch welcome.
 */
public final class Golf2MaterialNames {
    private Golf2MaterialNames() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.addBlock(ModBlocks.SOUL_CRYSTAL_BLOCK, "Block of Soul Crystal");
        lang.addBlock(ModBlocks.TURQUOISE_INLAID_SANDSTONE, "Turquoise Inlaid Sandstone");
        lang.addBlock(ModBlocks.TURQUOISE_INLAID_SANDSTONE_STAIRS, "Turquoise Inlaid Sandstone Stairs");
        lang.addBlock(ModBlocks.TURQUOISE_INLAID_SANDSTONE_SLAB, "Turquoise Inlaid Sandstone Slab");
        lang.addBlock(ModBlocks.TURQUOISE_INLAID_SANDSTONE_WALL, "Turquoise Inlaid Sandstone Wall");
        lang.addBlock(ModBlocks.SALT_LAMP, "Salt Lamp");
    }

    public static void addDutch(LangOutput lang) {
        lang.addBlock(ModBlocks.SOUL_CRYSTAL_BLOCK, "Zielkristalblok");
        lang.addBlock(ModBlocks.TURQUOISE_INLAID_SANDSTONE, "Met turkoois ingelegd zandsteen");
        lang.addBlock(ModBlocks.TURQUOISE_INLAID_SANDSTONE_STAIRS, "Met turkoois ingelegde zandstenen trap");
        lang.addBlock(ModBlocks.TURQUOISE_INLAID_SANDSTONE_SLAB, "Met turkoois ingelegde zandstenen plaat");
        lang.addBlock(ModBlocks.TURQUOISE_INLAID_SANDSTONE_WALL, "Met turkoois ingelegde zandstenen muur");
        lang.addBlock(ModBlocks.SALT_LAMP, "Zoutlamp");
    }
}
