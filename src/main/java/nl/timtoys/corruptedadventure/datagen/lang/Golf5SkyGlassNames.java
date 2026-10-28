package nl.timtoys.corruptedadventure.datagen.lang;

import nl.timtoys.corruptedadventure.registry.ModBlocks;

/**
 * Names of the phase 2 golf 5 blocks of stream H: the sky sand, sky glass and its pane, the glitch shard block. English
 * is mandatory, Dutch welcome.
 */
public final class Golf5SkyGlassNames {
    private Golf5SkyGlassNames() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.addBlock(ModBlocks.SKY_SAND, "Sky Sand");
        lang.addBlock(ModBlocks.SKY_GLASS, "Sky Glass");
        lang.addBlock(ModBlocks.SKY_GLASS_PANE, "Sky Glass Pane");
        lang.addBlock(ModBlocks.GLITCH_SHARD_BLOCK, "Glitch Shard Block");
    }

    public static void addDutch(LangOutput lang) {
        lang.addBlock(ModBlocks.SKY_SAND, "Hemelzand");
        lang.addBlock(ModBlocks.SKY_GLASS, "Hemelglas");
        lang.addBlock(ModBlocks.SKY_GLASS_PANE, "Hemelglasruit");
        lang.addBlock(ModBlocks.GLITCH_SHARD_BLOCK, "Glitchscherfblok");
    }
}
