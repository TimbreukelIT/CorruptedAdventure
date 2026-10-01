package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModItems;

/** Item models. Block items use their block model, except where vanilla uses a separate inventory model. */
final class ModItemModelProvider extends ItemModelProvider {
    ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, CorruptedAdventure.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        ResourceLocation planks = modLoc("block/dead_planks");
        blockItem(ModBlocks.DEAD_GRASS_BLOCK);
        blockItem(ModBlocks.WITHERED_DIRT);
        blockItem(ModBlocks.DEAD_LOG);
        blockItem(ModBlocks.DEAD_WOOD);
        blockItem(ModBlocks.STRIPPED_DEAD_LOG);
        blockItem(ModBlocks.STRIPPED_DEAD_WOOD);
        blockItem(ModBlocks.DEAD_PLANKS);
        blockItem(ModBlocks.DEAD_STAIRS);
        blockItem(ModBlocks.DEAD_SLAB);
        fenceInventory(ModBlocks.DEAD_FENCE.getId().getPath(), planks);
        blockItem(ModBlocks.DEAD_FENCE_GATE);
        basicItem(ModBlocks.DEAD_DOOR.asItem());
        withExistingParent(ModBlocks.DEAD_TRAPDOOR.getId().getPath(), modLoc("block/dead_trapdoor_bottom"));
        blockItem(ModBlocks.DEAD_PRESSURE_PLATE);
        buttonInventory(ModBlocks.DEAD_BUTTON.getId().getPath(), planks);

        blockItem(ModBlocks.SOUL_CRYSTAL_ORE);
        blockItem(ModBlocks.CRIMSON_SOUL_CRYSTAL_ORE);
        basicItem(ModItems.SOUL_CRYSTAL.get());

        blockItem(ModBlocks.RED_CANYON_STONE);
        blockItem(ModBlocks.ORANGE_CANYON_STONE);
        blockItem(ModBlocks.CREAM_CANYON_STONE);
        blockItem(ModBlocks.CANYON_COBBLESTONE);
        blockItem(ModBlocks.CANYON_COBBLESTONE_STAIRS);
        blockItem(ModBlocks.CANYON_COBBLESTONE_SLAB);
        wallInventory(ModBlocks.CANYON_COBBLESTONE_WALL.getId().getPath(), modLoc("block/canyon_cobblestone"));
        blockItem(ModBlocks.CANYON_STONE_BRICKS);
        blockItem(ModBlocks.CANYON_STONE_BRICK_STAIRS);
        blockItem(ModBlocks.CANYON_STONE_BRICK_SLAB);
        wallInventory(ModBlocks.CANYON_STONE_BRICK_WALL.getId().getPath(), modLoc("block/canyon_stone_bricks"));
        blockItem(ModBlocks.SCORCHED_SANDSTONE);
        blockItem(ModBlocks.SCORCHED_SANDSTONE_STAIRS);
        blockItem(ModBlocks.SCORCHED_SANDSTONE_SLAB);
        wallInventory(ModBlocks.SCORCHED_SANDSTONE_WALL.getId().getPath(), modLoc("block/scorched_sandstone"));
        blockItem(ModBlocks.CUT_SCORCHED_SANDSTONE);
        blockItem(ModBlocks.CUT_SCORCHED_SANDSTONE_SLAB);
        blockItem(ModBlocks.CHISELED_SCORCHED_SANDSTONE);
        blockItem(ModBlocks.SMOOTH_SCORCHED_SANDSTONE);
        blockItem(ModBlocks.SMOOTH_SCORCHED_SANDSTONE_STAIRS);
        blockItem(ModBlocks.SMOOTH_SCORCHED_SANDSTONE_SLAB);
        blockItem(ModBlocks.LOOSE_SAND);
        blockItem(ModBlocks.CRACKED_CLAY);
        flatBlockItem(ModBlocks.DRY_SHRUB);
        blockItem(ModBlocks.BARREL_CACTUS);
        flatBlockItem(ModBlocks.DESERT_FLOWER);

        ResourceLocation palmPlanks = modLoc("block/palm_planks");
        blockItem(ModBlocks.PALM_LOG);
        blockItem(ModBlocks.PALM_WOOD);
        blockItem(ModBlocks.STRIPPED_PALM_LOG);
        blockItem(ModBlocks.STRIPPED_PALM_WOOD);
        blockItem(ModBlocks.PALM_PLANKS);
        blockItem(ModBlocks.PALM_STAIRS);
        blockItem(ModBlocks.PALM_SLAB);
        fenceInventory(ModBlocks.PALM_FENCE.getId().getPath(), palmPlanks);
        blockItem(ModBlocks.PALM_FENCE_GATE);
        basicItem(ModBlocks.PALM_DOOR.asItem());
        withExistingParent(ModBlocks.PALM_TRAPDOOR.getId().getPath(), modLoc("block/palm_trapdoor_bottom"));
        blockItem(ModBlocks.PALM_PRESSURE_PLATE);
        buttonInventory(ModBlocks.PALM_BUTTON.getId().getPath(), palmPlanks);
        blockItem(ModBlocks.PALM_LEAVES);
        flatBlockItem(ModBlocks.PALM_SAPLING);
        // Potted plants have no item.

        blockItem(ModBlocks.RED_CANYON_SUNSTONE_ORE);
        blockItem(ModBlocks.ORANGE_CANYON_SUNSTONE_ORE);
        blockItem(ModBlocks.CREAM_CANYON_SUNSTONE_ORE);
        blockItem(ModBlocks.SCORCHED_SANDSTONE_SUNSTONE_ORE);
        blockItem(ModBlocks.RED_CANYON_TURQUOISE_ORE);
        blockItem(ModBlocks.ORANGE_CANYON_TURQUOISE_ORE);
        blockItem(ModBlocks.CREAM_CANYON_TURQUOISE_ORE);
        basicItem(ModItems.RAW_SUNSTONE.get());
        basicItem(ModItems.RAW_TURQUOISE.get());
    }

    /** A flat item showing the block texture, like vanilla's plants. */
    private void flatBlockItem(DeferredBlock<? extends Block> block) {
        String name = block.getId().getPath();
        withExistingParent(name, mcLoc("item/generated")).texture("layer0", modLoc("block/" + name));
    }

    private void blockItem(DeferredBlock<? extends Block> block) {
        withExistingParent(block.getId().getPath(), modLoc("block/" + block.getId().getPath()));
    }
}
