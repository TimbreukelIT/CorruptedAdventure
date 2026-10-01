package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

/**
 * Blockstates and block models. Textures must exist before {@code runData}
 * (generate them with {@code tools/textures/Ring1Textures.java}). Render types live in the model JSON.
 */
final class ModBlockStateProvider extends BlockStateProvider {
    ModBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, CorruptedAdventure.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        deadGrassBlock();
        randomlyRotated(ModBlocks.WITHERED_DIRT.get());

        logBlock(ModBlocks.DEAD_LOG.get());
        axisBlock(ModBlocks.DEAD_WOOD.get(), blockTexture(ModBlocks.DEAD_LOG.get()), blockTexture(ModBlocks.DEAD_LOG.get()));
        logBlock(ModBlocks.STRIPPED_DEAD_LOG.get());
        axisBlock(ModBlocks.STRIPPED_DEAD_WOOD.get(),
                blockTexture(ModBlocks.STRIPPED_DEAD_LOG.get()), blockTexture(ModBlocks.STRIPPED_DEAD_LOG.get()));

        ResourceLocation planks = blockTexture(ModBlocks.DEAD_PLANKS.get());
        simpleBlock(ModBlocks.DEAD_PLANKS.get());
        stairsBlock(ModBlocks.DEAD_STAIRS.get(), planks);
        slabBlock(ModBlocks.DEAD_SLAB.get(), planks, planks);
        fenceBlock(ModBlocks.DEAD_FENCE.get(), planks);
        fenceGateBlock(ModBlocks.DEAD_FENCE_GATE.get(), planks);
        doorBlockWithRenderType(ModBlocks.DEAD_DOOR.get(),
                modLoc("block/dead_door_bottom"), modLoc("block/dead_door_top"), "cutout");
        trapdoorBlockWithRenderType(ModBlocks.DEAD_TRAPDOOR.get(), modLoc("block/dead_trapdoor"), true, "cutout");
        pressurePlateBlock(ModBlocks.DEAD_PRESSURE_PLATE.get(), planks);
        buttonBlock(ModBlocks.DEAD_BUTTON.get(), planks);
    }

    /** Own textures for every face (no tint, no vanilla textures); bottom and particle are withered dirt. */
    private void deadGrassBlock() {
        ResourceLocation dirt = blockTexture(ModBlocks.WITHERED_DIRT.get());
        ResourceLocation top = modLoc("block/dead_grass_block_top");
        ModelFile grass = models().cubeBottomTop("dead_grass_block", modLoc("block/dead_grass_block_side"), dirt, top)
                .texture("particle", dirt);
        ModelFile snowy = models().cubeBottomTop("dead_grass_block_snow", modLoc("block/dead_grass_block_snow"), dirt, top)
                .texture("particle", dirt);
        getVariantBuilder(ModBlocks.DEAD_GRASS_BLOCK.get()).forAllStates(state -> state.getValue(SnowyDirtBlock.SNOWY)
                ? ConfiguredModel.builder().modelFile(snowy).build()
                : ConfiguredModel.allYRotations(grass, 0, false));
    }

    /** Like vanilla dirt and grass: four random Y rotations against visible tiling. */
    private void randomlyRotated(Block block) {
        simpleBlock(block, ConfiguredModel.allYRotations(cubeAll(block), 0, false));
    }
}
