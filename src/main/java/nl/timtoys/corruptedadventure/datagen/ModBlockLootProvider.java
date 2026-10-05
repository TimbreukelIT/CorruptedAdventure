package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.registries.DeferredHolder;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModItems;

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

        // Silk Touch -> the ore itself; otherwise 1 soul crystal (Fortune), the crimson variant leaves no netherrack.
        // XP comes from DropExperienceBlock, not from the loot table.
        add(ModBlocks.SOUL_CRYSTAL_ORE.get(), block -> createOreDrop(block, ModItems.SOUL_CRYSTAL.get()));
        add(ModBlocks.CRIMSON_SOUL_CRYSTAL_ORE.get(), block -> createOreDrop(block, ModItems.SOUL_CRYSTAL.get()));

        // Canyon stone tints break into canyon cobblestone, like stone into cobblestone.
        for (Block tint : new Block[] {
                ModBlocks.RED_CANYON_STONE.get(), ModBlocks.ORANGE_CANYON_STONE.get(), ModBlocks.CREAM_CANYON_STONE.get()}) {
            add(tint, block -> createSingleItemTableWithSilkTouch(block, ModBlocks.CANYON_COBBLESTONE.get()));
        }
        dropSelf(ModBlocks.CANYON_COBBLESTONE.get());
        dropSelf(ModBlocks.CANYON_COBBLESTONE_STAIRS.get());
        add(ModBlocks.CANYON_COBBLESTONE_SLAB.get(), this::createSlabItemTable);
        dropSelf(ModBlocks.CANYON_COBBLESTONE_WALL.get());
        dropSelf(ModBlocks.CANYON_STONE_BRICKS.get());
        dropSelf(ModBlocks.CANYON_STONE_BRICK_STAIRS.get());
        add(ModBlocks.CANYON_STONE_BRICK_SLAB.get(), this::createSlabItemTable);
        dropSelf(ModBlocks.CANYON_STONE_BRICK_WALL.get());

        dropSelf(ModBlocks.SCORCHED_SANDSTONE.get());
        dropSelf(ModBlocks.SCORCHED_SANDSTONE_STAIRS.get());
        add(ModBlocks.SCORCHED_SANDSTONE_SLAB.get(), this::createSlabItemTable);
        dropSelf(ModBlocks.SCORCHED_SANDSTONE_WALL.get());
        dropSelf(ModBlocks.CUT_SCORCHED_SANDSTONE.get());
        add(ModBlocks.CUT_SCORCHED_SANDSTONE_SLAB.get(), this::createSlabItemTable);
        dropSelf(ModBlocks.CHISELED_SCORCHED_SANDSTONE.get());
        dropSelf(ModBlocks.SMOOTH_SCORCHED_SANDSTONE.get());
        dropSelf(ModBlocks.SMOOTH_SCORCHED_SANDSTONE_STAIRS.get());
        add(ModBlocks.SMOOTH_SCORCHED_SANDSTONE_SLAB.get(), this::createSlabItemTable);

        // Plain sand without Silk Touch: no ready-made trap material.
        add(ModBlocks.LOOSE_SAND.get(), block -> createSingleItemTableWithSilkTouch(block, Items.SAND));

        dropSelf(ModBlocks.CRACKED_CLAY.get());
        // Like vanilla's dead bush: itself with shears, otherwise 0-2 sticks.
        add(ModBlocks.DRY_SHRUB.get(), block -> createShearsDispatchTable(block, applyExplosionDecay(block,
                LootItem.lootTableItem(Items.STICK).apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 2.0F))))));
        dropSelf(ModBlocks.BARREL_CACTUS.get());
        dropSelf(ModBlocks.DESERT_FLOWER.get());
        dropPottedContents(ModBlocks.POTTED_DRY_SHRUB.get());
        dropPottedContents(ModBlocks.POTTED_BARREL_CACTUS.get());
        dropPottedContents(ModBlocks.POTTED_DESERT_FLOWER.get());

        dropSelf(ModBlocks.PALM_LOG.get());
        dropSelf(ModBlocks.PALM_WOOD.get());
        dropSelf(ModBlocks.STRIPPED_PALM_LOG.get());
        dropSelf(ModBlocks.STRIPPED_PALM_WOOD.get());
        dropSelf(ModBlocks.PALM_PLANKS.get());
        dropSelf(ModBlocks.PALM_STAIRS.get());
        add(ModBlocks.PALM_SLAB.get(), this::createSlabItemTable);
        dropSelf(ModBlocks.PALM_FENCE.get());
        dropSelf(ModBlocks.PALM_FENCE_GATE.get());
        add(ModBlocks.PALM_DOOR.get(), this::createDoorTable);
        dropSelf(ModBlocks.PALM_TRAPDOOR.get());
        dropSelf(ModBlocks.PALM_PRESSURE_PLATE.get());
        dropSelf(ModBlocks.PALM_BUTTON.get());
        // Like oak leaves: shears/Silk Touch -> the leaves; otherwise sapling 1/20 (Fortune more) and sticks 1/50.
        add(ModBlocks.PALM_LEAVES.get(), block -> createLeavesDrops(block, ModBlocks.PALM_SAPLING.get(), NORMAL_LEAVES_SAPLING_CHANCES));
        dropSelf(ModBlocks.PALM_SAPLING.get());
        dropPottedContents(ModBlocks.POTTED_PALM_SAPLING.get());

        // Like iron ore -> raw iron: one raw item (Fortune), Silk Touch -> the ore variant itself.
        for (Block ore : List.of(ModBlocks.RED_CANYON_SUNSTONE_ORE.get(), ModBlocks.ORANGE_CANYON_SUNSTONE_ORE.get(),
                ModBlocks.CREAM_CANYON_SUNSTONE_ORE.get(), ModBlocks.SCORCHED_SANDSTONE_SUNSTONE_ORE.get())) {
            add(ore, block -> createOreDrop(block, ModItems.RAW_SUNSTONE.get()));
        }
        for (Block ore : List.of(ModBlocks.RED_CANYON_TURQUOISE_ORE.get(), ModBlocks.ORANGE_CANYON_TURQUOISE_ORE.get(),
                ModBlocks.CREAM_CANYON_TURQUOISE_ORE.get())) {
            add(ore, block -> createOreDrop(block, ModItems.RAW_TURQUOISE.get()));
        }

        // Like clay -> 4 clay balls (no Fortune); Silk Touch -> the crust itself.
        add(ModBlocks.SALT_CRUST.get(), block -> createSingleItemTableWithSilkTouch(block, ModItems.SALT.get(), ConstantValue.exactly(4.0F)));
        dropSelf(ModBlocks.SALT_BLOCK.get());
        dropSelf(ModBlocks.POLISHED_SALT.get());
        dropSelf(ModBlocks.POLISHED_SALT_STAIRS.get());
        add(ModBlocks.POLISHED_SALT_SLAB.get(), this::createSlabItemTable);
        dropSelf(ModBlocks.POLISHED_SALT_WALL.get());
        // Decoration you want to take along: itself, with any tool.
        dropSelf(ModBlocks.SALT_CRYSTAL.get());

        netherRing2();
    }

    private void netherRing2() {
        dropSelf(ModBlocks.SOAKED_NETHERRACK.get());
        // Like nylium -> netherrack: Silk Touch keeps the moss, otherwise soaked netherrack.
        add(ModBlocks.SPORE_MOSS.get(), block -> createSingleItemTableWithSilkTouch(block, ModBlocks.SOAKED_NETHERRACK.get()));
        dropSelf(ModBlocks.DRIPSHROOM_STEM.get());
        dropSelf(ModBlocks.DRIPSHROOM_HYPHAE.get());
        dropSelf(ModBlocks.STRIPPED_DRIPSHROOM_STEM.get());
        dropSelf(ModBlocks.STRIPPED_DRIPSHROOM_HYPHAE.get());
        dropSelf(ModBlocks.DRIPSHROOM_PLANKS.get());
        dropSelf(ModBlocks.DRIPSHROOM_STAIRS.get());
        add(ModBlocks.DRIPSHROOM_SLAB.get(), this::createSlabItemTable);
        dropSelf(ModBlocks.DRIPSHROOM_FENCE.get());
        dropSelf(ModBlocks.DRIPSHROOM_FENCE_GATE.get());
        add(ModBlocks.DRIPSHROOM_DOOR.get(), this::createDoorTable);
        dropSelf(ModBlocks.DRIPSHROOM_TRAPDOOR.get());
        dropSelf(ModBlocks.DRIPSHROOM_PRESSURE_PLATE.get());
        dropSelf(ModBlocks.DRIPSHROOM_BUTTON.get());
        dropSelf(ModBlocks.DRIPSHROOM_CAP.get());
        dropSelf(ModBlocks.DRIPSHROOM_LAMP.get());
        // Like glow lichen: only with shears, one item per face.
        add(ModBlocks.GLOW_MOLD.get(), block -> createMultifaceBlockDrops(block, HAS_SHEARS));
        dropSelf(ModBlocks.SLIME_STALACTITE.get());
        dropSelf(ModBlocks.SPORE_SPROUTS.get());

        // Like budding amethyst: the source drops nothing, not even with Silk Touch; buds only with Silk Touch.
        add(ModBlocks.DEW_SOURCE.get(), noDrop());
        dropWhenSilkTouch(ModBlocks.SMALL_DEW_CRYSTAL_BUD.get());
        dropWhenSilkTouch(ModBlocks.MEDIUM_DEW_CRYSTAL_BUD.get());
        dropWhenSilkTouch(ModBlocks.LARGE_DEW_CRYSTAL_BUD.get());
        // Like the amethyst cluster: Silk Touch -> the cluster; a pickaxe -> 4 crystals (Fortune), anything else 2.
        add(ModBlocks.DEW_CRYSTAL_CLUSTER.get(), block -> createSilkTouchDispatchTable(block,
                LootItem.lootTableItem(ModItems.DEW_CRYSTAL.get())
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(4.0F)))
                        .apply(ApplyBonusCount.addOreBonusCount(registries.lookupOrThrow(Registries.ENCHANTMENT)
                                .getOrThrow(Enchantments.FORTUNE)))
                        .when(MatchTool.toolMatches(ItemPredicate.Builder.item().of(ItemTags.CLUSTER_MAX_HARVESTABLES)))
                        .otherwise(applyExplosionDecay(block, LootItem.lootTableItem(ModItems.DEW_CRYSTAL.get())
                                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(2.0F)))))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.entries().stream().<Block>map(DeferredHolder::get).toList();
    }
}
