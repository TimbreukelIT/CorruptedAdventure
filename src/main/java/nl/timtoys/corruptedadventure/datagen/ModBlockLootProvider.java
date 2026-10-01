package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.registries.DeferredHolder;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Block loot tables. Every block in {@link ModBlocks} needs one; datagen fails otherwise. */
final class ModBlockLootProvider extends BlockLootSubProvider {
    private ModBlockLootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    static LootTableProvider create(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        return new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLootProvider::new, LootContextParamSets.BLOCK)), lookup);
    }

    @Override
    protected void generate() {
        add(ModBlocks.DEAD_GRASS_BLOCK.get(), block -> createSingleItemTableWithSilkTouch(block, ModBlocks.WITHERED_DIRT.get()));
        dropSelf(ModBlocks.WITHERED_DIRT.get());
        dropSelf(ModBlocks.DEAD_LOG.get());
        dropSelf(ModBlocks.DEAD_WOOD.get());
        dropSelf(ModBlocks.STRIPPED_DEAD_LOG.get());
        dropSelf(ModBlocks.STRIPPED_DEAD_WOOD.get());
        dropSelf(ModBlocks.DEAD_PLANKS.get());
        dropSelf(ModBlocks.DEAD_STAIRS.get());
        add(ModBlocks.DEAD_SLAB.get(), this::createSlabItemTable);
        dropSelf(ModBlocks.DEAD_FENCE.get());
        dropSelf(ModBlocks.DEAD_FENCE_GATE.get());
        add(ModBlocks.DEAD_DOOR.get(), this::createDoorTable);
        dropSelf(ModBlocks.DEAD_TRAPDOOR.get());
        dropSelf(ModBlocks.DEAD_PRESSURE_PLATE.get());
        dropSelf(ModBlocks.DEAD_BUTTON.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.entries().stream().<Block>map(DeferredHolder::get).toList();
    }
}
