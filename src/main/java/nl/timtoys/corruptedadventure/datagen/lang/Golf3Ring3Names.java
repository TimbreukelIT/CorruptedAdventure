package nl.timtoys.corruptedadventure.datagen.lang;

import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModItems;

/**
 * Names of the phase 2 golf 3 ring 3 materials and extras (stream R3): the dew crystal and sulfur blocks, the fire ruby
 * inlaid pumice set, the sulfur lamp, the pumice raft, the ember berries and the dripshroom stew. English is mandatory,
 * Dutch welcome.
 */
public final class Golf3Ring3Names {
    private Golf3Ring3Names() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.addBlock(ModBlocks.DEW_CRYSTAL_BLOCK, "Block of Dew Crystal");
        lang.addBlock(ModBlocks.SULFUR_BLOCK, "Block of Sulfur");
        lang.addBlock(ModBlocks.FIRE_RUBY_INLAID_PUMICE, "Fire Ruby Inlaid Pumice");
        lang.addBlock(ModBlocks.FIRE_RUBY_INLAID_PUMICE_STAIRS, "Fire Ruby Inlaid Pumice Stairs");
        lang.addBlock(ModBlocks.FIRE_RUBY_INLAID_PUMICE_SLAB, "Fire Ruby Inlaid Pumice Slab");
        lang.addBlock(ModBlocks.FIRE_RUBY_INLAID_PUMICE_WALL, "Fire Ruby Inlaid Pumice Wall");
        lang.addBlock(ModBlocks.SULFUR_LAMP, "Sulfur Lamp");
        lang.addBlock(ModBlocks.PUMICE_RAFT, "Pumice Raft");
        lang.addItem(ModItems.EMBER_BERRIES, "Ember Berries");
        lang.addItem(ModItems.DRIPSHROOM_STEW, "Dripshroom Stew");
    }

    public static void addDutch(LangOutput lang) {
        lang.addBlock(ModBlocks.DEW_CRYSTAL_BLOCK, "Dauwkristalblok");
        lang.addBlock(ModBlocks.SULFUR_BLOCK, "Zwavelblok");
        lang.addBlock(ModBlocks.FIRE_RUBY_INLAID_PUMICE, "Met vuurrobijn ingelegde puimsteen");
        lang.addBlock(ModBlocks.FIRE_RUBY_INLAID_PUMICE_STAIRS, "Met vuurrobijn ingelegde puimstenen trap");
        lang.addBlock(ModBlocks.FIRE_RUBY_INLAID_PUMICE_SLAB, "Met vuurrobijn ingelegde puimstenen plaat");
        lang.addBlock(ModBlocks.FIRE_RUBY_INLAID_PUMICE_WALL, "Met vuurrobijn ingelegde puimstenen muur");
        lang.addBlock(ModBlocks.SULFUR_LAMP, "Zwavellamp");
        lang.addBlock(ModBlocks.PUMICE_RAFT, "Puimsteenvlotter");
        lang.addItem(ModItems.EMBER_BERRIES, "Gloeibessen");
        lang.addItem(ModItems.DRIPSHROOM_STEW, "Druipzwamsoep");
    }
}
