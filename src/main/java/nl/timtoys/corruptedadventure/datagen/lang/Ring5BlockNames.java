package nl.timtoys.corruptedadventure.datagen.lang;

import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModItems;

/**
 * Names of the ring 5 and Nether ring 5 blocks and items. Owned by stream B
 * ({@code docs/plans/ring-5/waves.md}); English is mandatory, Dutch welcome. Dutch: "verdorven" for corrupted (the ring 4
 * "verdord" is withered). The wall signs need no name: like vanilla's they use their item's.
 */
public final class Ring5BlockNames {
    private Ring5BlockNames() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.addBlock(ModBlocks.CORRUPTED_GRASS_BLOCK, "Corrupted Grass Block");
        lang.addBlock(ModBlocks.CORRUPTED_DIRT, "Corrupted Dirt");
        lang.addBlock(ModBlocks.CORRUPTED_STONE, "Corrupted Stone");
        lang.addBlock(ModBlocks.CORRUPTED_COBBLESTONE, "Corrupted Cobblestone");
        lang.addBlock(ModBlocks.CORRUPTED_SAND, "Corrupted Sand");
        lang.addBlock(ModBlocks.CORRUPTED_LOG, "Corrupted Log");
        lang.addBlock(ModBlocks.CORRUPTED_WOOD, "Corrupted Wood");
        lang.addBlock(ModBlocks.STRIPPED_CORRUPTED_LOG, "Stripped Corrupted Log");
        lang.addBlock(ModBlocks.STRIPPED_CORRUPTED_WOOD, "Stripped Corrupted Wood");
        lang.addBlock(ModBlocks.CORRUPTED_PLANKS, "Corrupted Planks");
        lang.addBlock(ModBlocks.CORRUPTED_STAIRS, "Corrupted Stairs");
        lang.addBlock(ModBlocks.CORRUPTED_SLAB, "Corrupted Slab");
        lang.addBlock(ModBlocks.CORRUPTED_FENCE, "Corrupted Fence");
        lang.addBlock(ModBlocks.CORRUPTED_FENCE_GATE, "Corrupted Fence Gate");
        lang.addBlock(ModBlocks.CORRUPTED_DOOR, "Corrupted Door");
        lang.addBlock(ModBlocks.CORRUPTED_TRAPDOOR, "Corrupted Trapdoor");
        lang.addBlock(ModBlocks.CORRUPTED_PRESSURE_PLATE, "Corrupted Pressure Plate");
        lang.addBlock(ModBlocks.CORRUPTED_BUTTON, "Corrupted Button");
        lang.addBlock(ModBlocks.CORRUPTED_SIGN, "Corrupted Sign");
        lang.addBlock(ModBlocks.CORRUPTED_HANGING_SIGN, "Corrupted Hanging Sign");
        lang.addBlock(ModBlocks.CORRUPTED_LEAVES, "Corrupted Leaves");
        lang.addBlock(ModBlocks.GLITCH_BLOCK, "Glitch Block");
        lang.addBlock(ModBlocks.VOIDSTONE, "Voidstone");
        lang.addBlock(ModBlocks.NULLITE_ORE, "Nullite Ore");
        lang.addBlock(ModBlocks.VOIDSTONE_NULLITE_ORE, "Voidstone Nullite Ore");
        lang.addItem(ModItems.GLITCH_SHARD, "Glitch Shard");
        lang.addItem(ModItems.RAW_NULLITE, "Raw Nullite");
    }

    public static void addDutch(LangOutput lang) {
        lang.addBlock(ModBlocks.CORRUPTED_GRASS_BLOCK, "Verdorven grasblok");
        lang.addBlock(ModBlocks.CORRUPTED_DIRT, "Verdorven aarde");
        lang.addBlock(ModBlocks.CORRUPTED_STONE, "Verdorven steen");
        lang.addBlock(ModBlocks.CORRUPTED_COBBLESTONE, "Verdorven keisteen");
        lang.addBlock(ModBlocks.CORRUPTED_SAND, "Verdorven zand");
        lang.addBlock(ModBlocks.CORRUPTED_LOG, "Verdorven stam");
        lang.addBlock(ModBlocks.CORRUPTED_WOOD, "Verdorven hout");
        lang.addBlock(ModBlocks.STRIPPED_CORRUPTED_LOG, "Gestripte verdorven stam");
        lang.addBlock(ModBlocks.STRIPPED_CORRUPTED_WOOD, "Gestript verdorven hout");
        lang.addBlock(ModBlocks.CORRUPTED_PLANKS, "Verdorven planken");
        lang.addBlock(ModBlocks.CORRUPTED_STAIRS, "Verdorven trap");
        lang.addBlock(ModBlocks.CORRUPTED_SLAB, "Verdorven plaat");
        lang.addBlock(ModBlocks.CORRUPTED_FENCE, "Verdorven hek");
        lang.addBlock(ModBlocks.CORRUPTED_FENCE_GATE, "Verdorven poort");
        lang.addBlock(ModBlocks.CORRUPTED_DOOR, "Verdorven deur");
        lang.addBlock(ModBlocks.CORRUPTED_TRAPDOOR, "Verdorven valluik");
        lang.addBlock(ModBlocks.CORRUPTED_PRESSURE_PLATE, "Verdorven drukplaat");
        lang.addBlock(ModBlocks.CORRUPTED_BUTTON, "Verdorven knop");
        lang.addBlock(ModBlocks.CORRUPTED_SIGN, "Verdorven bord");
        lang.addBlock(ModBlocks.CORRUPTED_HANGING_SIGN, "Verdorven hangbord");
        lang.addBlock(ModBlocks.CORRUPTED_LEAVES, "Verdorven bladeren");
        lang.addBlock(ModBlocks.GLITCH_BLOCK, "Glitchblok");
        lang.addBlock(ModBlocks.VOIDSTONE, "Voidsteen");
        lang.addBlock(ModBlocks.NULLITE_ORE, "Nullieterts");
        lang.addBlock(ModBlocks.VOIDSTONE_NULLITE_ORE, "Voidsteen-nullieterts");
        lang.addItem(ModItems.GLITCH_SHARD, "Glitchscherf");
        lang.addItem(ModItems.RAW_NULLITE, "Ruw nulliet");
    }
}
