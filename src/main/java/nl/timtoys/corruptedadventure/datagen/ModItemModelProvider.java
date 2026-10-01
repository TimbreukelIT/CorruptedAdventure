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
    }

    private void blockItem(DeferredBlock<? extends Block> block) {
        withExistingParent(block.getId().getPath(), modLoc("block/" + block.getId().getPath()));
    }
}
