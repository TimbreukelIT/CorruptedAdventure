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
        copy(ModTags.Blocks.DRIPSHROOM_STEMS, ModTags.Items.DRIPSHROOM_STEMS);
        tag(ItemTags.LOGS).addTag(ModTags.Items.DRIPSHROOM_STEMS);
        // Nether wood: no furnace fuel, like crimson and warped.
        tag(ItemTags.NON_FLAMMABLE_WOOD).add(ModBlocks.DRIPSHROOM_STEM.asItem(), ModBlocks.DRIPSHROOM_HYPHAE.asItem(),
                ModBlocks.STRIPPED_DRIPSHROOM_STEM.asItem(), ModBlocks.STRIPPED_DRIPSHROOM_HYPHAE.asItem(),
                ModBlocks.DRIPSHROOM_PLANKS.asItem(), ModBlocks.DRIPSHROOM_STAIRS.asItem(), ModBlocks.DRIPSHROOM_SLAB.asItem(),
                ModBlocks.DRIPSHROOM_FENCE.asItem(), ModBlocks.DRIPSHROOM_FENCE_GATE.asItem(), ModBlocks.DRIPSHROOM_DOOR.asItem(),
                ModBlocks.DRIPSHROOM_TRAPDOOR.asItem(), ModBlocks.DRIPSHROOM_BUTTON.asItem(),
                ModBlocks.DRIPSHROOM_PRESSURE_PLATE.asItem());
        copy(ModTags.Blocks.CHARRED_LOGS, ModTags.Items.CHARRED_LOGS);
        tag(ItemTags.LOGS).addTag(ModTags.Items.CHARRED_LOGS);
        // Charred wood does not burn (decision 2026-10-06): no furnace fuel either, like the Nether wood sets.
        tag(ItemTags.NON_FLAMMABLE_WOOD).add(ModBlocks.CHARRED_LOG.asItem(), ModBlocks.CHARRED_WOOD.asItem(),
                ModBlocks.STRIPPED_CHARRED_LOG.asItem(), ModBlocks.STRIPPED_CHARRED_WOOD.asItem(),
                ModBlocks.CHARRED_PLANKS.asItem(), ModBlocks.CHARRED_STAIRS.asItem(), ModBlocks.CHARRED_SLAB.asItem(),
                ModBlocks.CHARRED_FENCE.asItem(), ModBlocks.CHARRED_FENCE_GATE.asItem(), ModBlocks.CHARRED_DOOR.asItem(),
                ModBlocks.CHARRED_TRAPDOOR.asItem(), ModBlocks.CHARRED_BUTTON.asItem(),
                ModBlocks.CHARRED_PRESSURE_PLATE.asItem());
        copy(ModTags.Blocks.RIMESHROOM_STEMS, ModTags.Items.RIMESHROOM_STEMS);
        tag(ItemTags.LOGS).addTag(ModTags.Items.RIMESHROOM_STEMS);
        tag(ItemTags.NON_FLAMMABLE_WOOD).add(ModBlocks.RIMESHROOM_STEM.asItem(), ModBlocks.RIMESHROOM_HYPHAE.asItem(),
                ModBlocks.STRIPPED_RIMESHROOM_STEM.asItem(), ModBlocks.STRIPPED_RIMESHROOM_HYPHAE.asItem(),
                ModBlocks.RIMESHROOM_PLANKS.asItem(), ModBlocks.RIMESHROOM_STAIRS.asItem(), ModBlocks.RIMESHROOM_SLAB.asItem(),
                ModBlocks.RIMESHROOM_FENCE.asItem(), ModBlocks.RIMESHROOM_FENCE_GATE.asItem(), ModBlocks.RIMESHROOM_DOOR.asItem(),
                ModBlocks.RIMESHROOM_TRAPDOOR.asItem(), ModBlocks.RIMESHROOM_BUTTON.asItem(),
                ModBlocks.RIMESHROOM_PRESSURE_PLATE.asItem());
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
        tag(ModTags.Items.DUSTS_SALT).add(ModItems.SALT.get());
        tag(Tags.Items.DUSTS).addTag(ModTags.Items.DUSTS_SALT);
        tag(ModTags.Items.GEMS_DEW_CRYSTAL).add(ModItems.DEW_CRYSTAL.get());
        tag(Tags.Items.GEMS).addTag(ModTags.Items.GEMS_DEW_CRYSTAL);
        copy(ModTags.Blocks.ORES_VOLCANITE, ModTags.Items.ORES_VOLCANITE);
        copy(ModTags.Blocks.ORES_SULFUR, ModTags.Items.ORES_SULFUR);
        copy(ModTags.Blocks.ORES_FIRE_RUBY, ModTags.Items.ORES_FIRE_RUBY);
        copy(Tags.Blocks.ORE_RATES_DENSE, Tags.Items.ORE_RATES_DENSE);
        copy(Tags.Blocks.ORES_IN_GROUND_STONE, Tags.Items.ORES_IN_GROUND_STONE);
        copy(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE, Tags.Items.ORES_IN_GROUND_DEEPSLATE);
        tag(ModTags.Items.RAW_MATERIALS_VOLCANITE).add(ModItems.RAW_VOLCANITE.get());
        tag(Tags.Items.RAW_MATERIALS).addTag(ModTags.Items.RAW_MATERIALS_VOLCANITE);
        tag(ModTags.Items.DUSTS_SULFUR).add(ModItems.SULFUR.get());
        tag(Tags.Items.DUSTS).addTag(ModTags.Items.DUSTS_SULFUR);
        tag(ModTags.Items.GEMS_FIRE_RUBY).add(ModItems.FIRE_RUBY.get());
        tag(Tags.Items.GEMS).addTag(ModTags.Items.GEMS_FIRE_RUBY);
        tag(ModTags.Items.GEMS_FIRE_CRYSTAL).add(ModItems.FIRE_CRYSTAL.get());
        tag(Tags.Items.GEMS).addTag(ModTags.Items.GEMS_FIRE_CRYSTAL);
        copy(Tags.Blocks.BUDDING_BLOCKS, Tags.Items.BUDDING_BLOCKS);
        copy(Tags.Blocks.BUDS, Tags.Items.BUDS);
        copy(Tags.Blocks.CLUSTERS, Tags.Items.CLUSTERS);

        copy(ModTags.Blocks.CANYON_STONES, ModTags.Items.CANYON_STONES);
        copy(BlockTags.STAIRS, ItemTags.STAIRS);
        copy(BlockTags.SLABS, ItemTags.SLABS);
        copy(BlockTags.WALLS, ItemTags.WALLS);
        copy(BlockTags.SMALL_FLOWERS, ItemTags.SMALL_FLOWERS);
        copy(Tags.Blocks.COBBLESTONES, Tags.Items.COBBLESTONES);
        copy(Tags.Blocks.NETHERRACKS, Tags.Items.NETHERRACKS);
        copy(BlockTags.WART_BLOCKS, ItemTags.WART_BLOCKS);
        // The lagoon sand (66c) is sand as an item too (vanilla's item tag holds sand and red sand).
        copy(BlockTags.SAND, ItemTags.SAND);
        copy(Tags.Blocks.SANDS, Tags.Items.SANDS);
        copy(Tags.Blocks.SANDSTONE_BLOCKS, Tags.Items.SANDSTONE_BLOCKS);
        copy(Tags.Blocks.SANDSTONE_SLABS, Tags.Items.SANDSTONE_SLABS);
        copy(Tags.Blocks.SANDSTONE_STAIRS, Tags.Items.SANDSTONE_STAIRS);
        // Canyon cobblestone makes stone tools, furnaces and the other cobblestone recipes, like vanilla cobblestone.
        tag(ItemTags.STONE_TOOL_MATERIALS).add(ModBlocks.CANYON_COBBLESTONE.asItem());
        tag(ItemTags.STONE_CRAFTING_MATERIALS).add(ModBlocks.CANYON_COBBLESTONE.asItem());

        ring4();
    }

    /** Ring 4: the burnable wood sets reach #logs_that_burn through the block tag copy above. */
    private void ring4() {
        copy(ModTags.Blocks.MOONPETAL_LOGS, ModTags.Items.MOONPETAL_LOGS);
        copy(ModTags.Blocks.WITHERED_MOONPETAL_LOGS, ModTags.Items.WITHERED_MOONPETAL_LOGS);
        copy(ModTags.Blocks.VERDANT_LOGS, ModTags.Items.VERDANT_LOGS);
        // Cobbled tidestone makes stone tools and the cobblestone recipes, like canyon cobblestone.
        tag(ItemTags.STONE_TOOL_MATERIALS).add(ModBlocks.COBBLED_TIDESTONE.asItem());
        tag(ItemTags.STONE_CRAFTING_MATERIALS).add(ModBlocks.COBBLED_TIDESTONE.asItem());

        copy(ModTags.Blocks.ORES_TIDESILVER, ModTags.Items.ORES_TIDESILVER);
        copy(ModTags.Blocks.ORES_JADE, ModTags.Items.ORES_JADE);
        copy(ModTags.Blocks.ORES_IN_GROUND_TIDESTONE, ModTags.Items.ORES_IN_GROUND_TIDESTONE);
        tag(ModTags.Items.RAW_MATERIALS_TIDESILVER).add(ModItems.RAW_TIDESILVER.get());
        tag(Tags.Items.RAW_MATERIALS).addTag(ModTags.Items.RAW_MATERIALS_TIDESILVER);
        tag(ModTags.Items.GEMS_JADE).add(ModItems.JADE.get());
        tag(ModTags.Items.GEMS_TOXIC_CRYSTAL).add(ModItems.TOXIC_CRYSTAL_SHARD.get());
        tag(ModTags.Items.GEMS_AQUAMARINE).add(ModItems.AQUAMARINE_SHARD.get());
        tag(Tags.Items.GEMS).addTag(ModTags.Items.GEMS_JADE).addTag(ModTags.Items.GEMS_TOXIC_CRYSTAL)
                .addTag(ModTags.Items.GEMS_AQUAMARINE);
        // Nether ring 4.
        tag(ModTags.Items.GEMS_PURITY_CRYSTAL).add(ModItems.PURITY_CRYSTAL_SHARD.get());
        tag(Tags.Items.GEMS).addTag(ModTags.Items.GEMS_PURITY_CRYSTAL);
    }
}
