package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModItems;
import nl.timtoys.corruptedadventure.registry.ModTags;

import java.util.concurrent.CompletableFuture;

/** Item tags: mostly copies of the block tags, so vanilla recipes (sticks, crafting table, charcoal) and fuels work. */
final class ModItemTagsProvider extends ItemTagsProvider {
    ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                        CompletableFuture<TagsProvider.TagLookup<Block>> blockTags, ExistingFileHelper existingFileHelper) {
        super(output, lookup, blockTags, CorruptedAdventure.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        copy(ModTags.Blocks.DEAD_LOGS, ModTags.Items.DEAD_LOGS);
        copy(ModTags.Blocks.PALM_LOGS, ModTags.Items.PALM_LOGS);
        copy(BlockTags.LEAVES, ItemTags.LEAVES);
        copy(BlockTags.SAPLINGS, ItemTags.SAPLINGS);
        copy(BlockTags.LOGS_THAT_BURN, ItemTags.LOGS_THAT_BURN);
        copy(BlockTags.PLANKS, ItemTags.PLANKS);
        copy(BlockTags.WOODEN_STAIRS, ItemTags.WOODEN_STAIRS);
        copy(BlockTags.WOODEN_SLABS, ItemTags.WOODEN_SLABS);
        copy(BlockTags.WOODEN_FENCES, ItemTags.WOODEN_FENCES);
        copy(BlockTags.FENCE_GATES, ItemTags.FENCE_GATES);
        copy(BlockTags.WOODEN_DOORS, ItemTags.WOODEN_DOORS);
        copy(BlockTags.WOODEN_TRAPDOORS, ItemTags.WOODEN_TRAPDOORS);
        copy(BlockTags.WOODEN_PRESSURE_PLATES, ItemTags.WOODEN_PRESSURE_PLATES);
        copy(BlockTags.WOODEN_BUTTONS, ItemTags.WOODEN_BUTTONS);
        copy(BlockTags.DIRT, ItemTags.DIRT);
        copy(Tags.Blocks.STRIPPED_LOGS, Tags.Items.STRIPPED_LOGS);
        copy(Tags.Blocks.STRIPPED_WOODS, Tags.Items.STRIPPED_WOODS);
        copy(Tags.Blocks.FENCE_GATES_WOODEN, Tags.Items.FENCE_GATES_WOODEN);

        copy(ModTags.Blocks.ORES_SOUL_CRYSTAL, ModTags.Items.ORES_SOUL_CRYSTAL);
        copy(Tags.Blocks.ORES, Tags.Items.ORES);
        copy(Tags.Blocks.ORE_RATES_SINGULAR, Tags.Items.ORE_RATES_SINGULAR);
        copy(Tags.Blocks.ORES_IN_GROUND_NETHERRACK, Tags.Items.ORES_IN_GROUND_NETHERRACK);
        tag(ModTags.Items.GEMS_SOUL_CRYSTAL).add(ModItems.SOUL_CRYSTAL.get());
        tag(Tags.Items.GEMS).addTag(ModTags.Items.GEMS_SOUL_CRYSTAL);
        copy(ModTags.Blocks.ORES_SUNSTONE, ModTags.Items.ORES_SUNSTONE);
        copy(ModTags.Blocks.ORES_TURQUOISE, ModTags.Items.ORES_TURQUOISE);
        tag(ModTags.Items.RAW_MATERIALS_SUNSTONE).add(ModItems.RAW_SUNSTONE.get());
        tag(ModTags.Items.RAW_MATERIALS_TURQUOISE).add(ModItems.RAW_TURQUOISE.get());
        tag(Tags.Items.RAW_MATERIALS).addTag(ModTags.Items.RAW_MATERIALS_SUNSTONE).addTag(ModTags.Items.RAW_MATERIALS_TURQUOISE);

        copy(ModTags.Blocks.CANYON_STONES, ModTags.Items.CANYON_STONES);
        copy(BlockTags.STAIRS, ItemTags.STAIRS);
        copy(BlockTags.SLABS, ItemTags.SLABS);
        copy(BlockTags.WALLS, ItemTags.WALLS);
        copy(BlockTags.SMALL_FLOWERS, ItemTags.SMALL_FLOWERS);
        copy(Tags.Blocks.COBBLESTONES, Tags.Items.COBBLESTONES);
        copy(Tags.Blocks.SANDSTONE_BLOCKS, Tags.Items.SANDSTONE_BLOCKS);
        copy(Tags.Blocks.SANDSTONE_SLABS, Tags.Items.SANDSTONE_SLABS);
        copy(Tags.Blocks.SANDSTONE_STAIRS, Tags.Items.SANDSTONE_STAIRS);
        // Canyon cobblestone makes stone tools, furnaces and the other cobblestone recipes, like vanilla cobblestone.
        tag(ItemTags.STONE_TOOL_MATERIALS).add(ModBlocks.CANYON_COBBLESTONE.asItem());
        tag(ItemTags.STONE_CRAFTING_MATERIALS).add(ModBlocks.CANYON_COBBLESTONE.asItem());
    }
}
