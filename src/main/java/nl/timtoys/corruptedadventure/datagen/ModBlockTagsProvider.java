package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModTags;

import java.util.concurrent.CompletableFuture;

/**
 * Block tags. The wood set relies on vanilla's tag chains: {@code #minecraft:wooden_*}, {@code #planks} and
 * {@code #logs} are already in {@code #mineable/axe}, NeoForge's furnace fuels and {@code c:fences/wooden}.
 * Dead grass and withered dirt are deliberately not in {@code #animals_spawnable_on} (no animals on dead ground).
 */
final class ModBlockTagsProvider extends BlockTagsProvider {
    ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFileHelper) {
        super(output, lookup, CorruptedAdventure.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ModTags.Blocks.DEAD_LOGS).add(
                ModBlocks.DEAD_LOG.get(), ModBlocks.DEAD_WOOD.get(), ModBlocks.STRIPPED_DEAD_LOG.get(), ModBlocks.STRIPPED_DEAD_WOOD.get());
        tag(BlockTags.LOGS_THAT_BURN).addTag(ModTags.Blocks.DEAD_LOGS);
        tag(BlockTags.PLANKS).add(ModBlocks.DEAD_PLANKS.get());
        tag(BlockTags.WOODEN_STAIRS).add(ModBlocks.DEAD_STAIRS.get());
        tag(BlockTags.WOODEN_SLABS).add(ModBlocks.DEAD_SLAB.get());
        tag(BlockTags.WOODEN_FENCES).add(ModBlocks.DEAD_FENCE.get());
        tag(BlockTags.FENCE_GATES).add(ModBlocks.DEAD_FENCE_GATE.get());
        tag(BlockTags.WOODEN_DOORS).add(ModBlocks.DEAD_DOOR.get());
        tag(BlockTags.WOODEN_TRAPDOORS).add(ModBlocks.DEAD_TRAPDOOR.get());
        tag(BlockTags.WOODEN_PRESSURE_PLATES).add(ModBlocks.DEAD_PRESSURE_PLATE.get());
        tag(BlockTags.WOODEN_BUTTONS).add(ModBlocks.DEAD_BUTTON.get());

        // #dirt: plants grow on it, carvers cut through it (#overworld_carver_replaceables), endermen pick it up.
        tag(BlockTags.DIRT).add(ModBlocks.DEAD_GRASS_BLOCK.get(), ModBlocks.WITHERED_DIRT.get());
        tag(BlockTags.MINEABLE_WITH_SHOVEL).add(ModBlocks.DEAD_GRASS_BLOCK.get(), ModBlocks.WITHERED_DIRT.get());

        tag(Tags.Blocks.STRIPPED_LOGS).add(ModBlocks.STRIPPED_DEAD_LOG.get());
        tag(Tags.Blocks.STRIPPED_WOODS).add(ModBlocks.STRIPPED_DEAD_WOOD.get());
        tag(Tags.Blocks.FENCE_GATES_WOODEN).add(ModBlocks.DEAD_FENCE_GATE.get());
    }
}
