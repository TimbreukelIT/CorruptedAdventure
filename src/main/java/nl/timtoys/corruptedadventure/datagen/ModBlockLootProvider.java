package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.LimitCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.registries.DeferredHolder;
import nl.timtoys.corruptedadventure.block.GiantLilyPadBlock;
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
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLootProvider::new, LootContextParamSets.BLOCK),
                        // 94: the abandoned camps' chests (one loot provider per pack: its name must be unique).
                        new LootTableProvider.SubProviderEntry(Ring5ChestLoot::new, LootContextParamSets.CHEST)), lookup);
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
        ring3();
        netherRing3();
        ring4();
        ring5();
        golf1Materials();
        golf2Materials();
        golf3Ring3();
    }

    /** Phase 2 golf 3 (stream R3): every block drops itself, a double slab two. */
    private void golf3Ring3() {
        dropSelf(ModBlocks.DEW_CRYSTAL_BLOCK.get());
        dropSelf(ModBlocks.SULFUR_BLOCK.get());
        dropSelf(ModBlocks.FIRE_RUBY_INLAID_PUMICE.get());
        dropSelf(ModBlocks.FIRE_RUBY_INLAID_PUMICE_STAIRS.get());
        add(ModBlocks.FIRE_RUBY_INLAID_PUMICE_SLAB.get(), this::createSlabItemTable);
        dropSelf(ModBlocks.FIRE_RUBY_INLAID_PUMICE_WALL.get());
        dropSelf(ModBlocks.SULFUR_LAMP.get());
        dropSelf(ModBlocks.PUMICE_RAFT.get());
    }

    /** Phase 2 golf 2 (stream M): every block drops itself (the amethyst block too), a double slab two. */
    private void golf2Materials() {
        dropSelf(ModBlocks.SOUL_CRYSTAL_BLOCK.get());
        dropSelf(ModBlocks.TURQUOISE_INLAID_SANDSTONE.get());
        dropSelf(ModBlocks.TURQUOISE_INLAID_SANDSTONE_STAIRS.get());
        add(ModBlocks.TURQUOISE_INLAID_SANDSTONE_SLAB.get(), this::createSlabItemTable);
        dropSelf(ModBlocks.TURQUOISE_INLAID_SANDSTONE_WALL.get());
        dropSelf(ModBlocks.SALT_LAMP.get());
    }

    /** Phase 2 golf 1 (stream D): the storage blocks drop themselves, like vanilla's. */
    private void golf1Materials() {
        dropSelf(ModBlocks.RAW_SUNSTONE_BLOCK.get());
        dropSelf(ModBlocks.RAW_TURQUOISE_BLOCK.get());
        dropSelf(ModBlocks.SUNSTONE_BLOCK.get());
        dropSelf(ModBlocks.TURQUOISE_BLOCK.get());
        dropSelf(ModBlocks.RAW_VOLCANITE_BLOCK.get());
        dropSelf(ModBlocks.VOLCANITE_BLOCK.get());
        dropSelf(ModBlocks.FIRE_RUBY_BLOCK.get());
        dropSelf(ModBlocks.RAW_TIDESILVER_BLOCK.get());
        dropSelf(ModBlocks.TIDESILVER_BLOCK.get());
        dropSelf(ModBlocks.JADE_BLOCK.get());
        dropSelf(ModBlocks.RAW_NULLITE_BLOCK.get());
        dropSelf(ModBlocks.NULLITE_BLOCK.get());
    }

    /**
     * Ring 5 (88): like the vanilla counterparts. The wall signs have no table of their own: they drop the standing
     * sign's ({@code lootFrom} in {@code ModBlocks}).
     */
    private void ring5() {
        add(ModBlocks.CORRUPTED_GRASS_BLOCK.get(), block -> createSingleItemTableWithSilkTouch(block, ModBlocks.CORRUPTED_DIRT.get()));
        add(ModBlocks.CORRUPTED_STONE.get(), block -> createSingleItemTableWithSilkTouch(block, ModBlocks.CORRUPTED_COBBLESTONE.get()));
        for (Block block : List.of(ModBlocks.CORRUPTED_DIRT.get(), ModBlocks.CORRUPTED_COBBLESTONE.get(), ModBlocks.CORRUPTED_SAND.get(),
                ModBlocks.CORRUPTED_LOG.get(), ModBlocks.CORRUPTED_WOOD.get(), ModBlocks.STRIPPED_CORRUPTED_LOG.get(),
                ModBlocks.STRIPPED_CORRUPTED_WOOD.get(), ModBlocks.CORRUPTED_PLANKS.get(), ModBlocks.CORRUPTED_STAIRS.get(),
                ModBlocks.CORRUPTED_FENCE.get(), ModBlocks.CORRUPTED_FENCE_GATE.get(), ModBlocks.CORRUPTED_TRAPDOOR.get(),
                ModBlocks.CORRUPTED_PRESSURE_PLATE.get(), ModBlocks.CORRUPTED_BUTTON.get(), ModBlocks.CORRUPTED_SIGN.get(),
                ModBlocks.CORRUPTED_HANGING_SIGN.get())) {
            dropSelf(block);
        }
        add(ModBlocks.CORRUPTED_SLAB.get(), this::createSlabItemTable);
        add(ModBlocks.CORRUPTED_DOOR.get(), this::createDoorTable);
        // No sapling and no apple: shears or Silk Touch -> the leaves, otherwise sticks like oak leaves (vanilla's
        // table for the sapling-less mangrove leaves).
        add(ModBlocks.CORRUPTED_LEAVES.get(), this::createMangroveLeavesDrops);

        // 89. Like glowstone's dust: 1-3 shards, Fortune adds, never more than 4; Silk Touch -> the block itself.
        add(ModBlocks.GLITCH_BLOCK.get(), block -> createSilkTouchDispatchTable(block, applyExplosionDecay(block,
                LootItem.lootTableItem(ModItems.GLITCH_SHARD.get())
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F)))
                        .apply(ApplyBonusCount.addUniformBonusCount(registries.lookupOrThrow(Registries.ENCHANTMENT)
                                .getOrThrow(Enchantments.FORTUNE)))
                        .apply(LimitCount.limitCount(IntRange.range(1, 4))))));
        dropSelf(ModBlocks.VOIDSTONE.get());
        for (Block ore : List.of(ModBlocks.NULLITE_ORE.get(), ModBlocks.VOIDSTONE_NULLITE_ORE.get())) {
            add(ore, block -> createOreDrop(block, ModItems.RAW_NULLITE.get()));
        }

        // 90: everything drops itself except the slabs (two from a double slab) and the white core ore (one white core,
        // Fortune adds; Silk Touch the ore).
        for (Block block : List.of(ModBlocks.CHALKRACK.get(), ModBlocks.WHITE_STONE.get(), ModBlocks.WHITE_STONE_STAIRS.get(),
                ModBlocks.WHITE_STONE_WALL.get(), ModBlocks.POLISHED_WHITE_STONE.get(), ModBlocks.POLISHED_WHITE_STONE_STAIRS.get(),
                ModBlocks.POLISHED_WHITE_STONE_WALL.get(), ModBlocks.WHITE_STONE_BRICKS.get(), ModBlocks.WHITE_STONE_BRICK_STAIRS.get(),
                ModBlocks.WHITE_STONE_BRICK_WALL.get(), ModBlocks.WHITE_STONE_PILLAR.get(), ModBlocks.MIRROR_STONE.get(),
                ModBlocks.STILL_LIGHT.get())) {
            dropSelf(block);
        }
        for (Block slab : List.of(ModBlocks.WHITE_STONE_SLAB.get(), ModBlocks.POLISHED_WHITE_STONE_SLAB.get(),
                ModBlocks.WHITE_STONE_BRICK_SLAB.get())) {
            add(slab, this::createSlabItemTable);
        }
        add(ModBlocks.WHITE_CORE_ORE.get(), block -> createOreDrop(block, ModItems.WHITE_CORE.get()));
    }

    private void ring4() {
        // Tidestone breaks into cobbled tidestone, like stone into cobblestone.
        add(ModBlocks.TIDESTONE.get(), block -> createSingleItemTableWithSilkTouch(block, ModBlocks.COBBLED_TIDESTONE.get()));
        for (Block block : List.of(ModBlocks.TIDESTONE_STAIRS.get(), ModBlocks.TIDESTONE_WALL.get(),
                ModBlocks.COBBLED_TIDESTONE.get(), ModBlocks.COBBLED_TIDESTONE_STAIRS.get(), ModBlocks.COBBLED_TIDESTONE_WALL.get(),
                ModBlocks.POLISHED_TIDESTONE.get(), ModBlocks.POLISHED_TIDESTONE_STAIRS.get(), ModBlocks.POLISHED_TIDESTONE_WALL.get(),
                ModBlocks.TIDESTONE_BRICKS.get(), ModBlocks.TIDESTONE_BRICK_STAIRS.get(), ModBlocks.TIDESTONE_BRICK_WALL.get())) {
            dropSelf(block);
        }
        for (Block slab : List.of(ModBlocks.TIDESTONE_SLAB.get(), ModBlocks.COBBLED_TIDESTONE_SLAB.get(),
                ModBlocks.POLISHED_TIDESTONE_SLAB.get(), ModBlocks.TIDESTONE_BRICK_SLAB.get())) {
            add(slab, this::createSlabItemTable);
        }

        for (Block block : List.of(ModBlocks.MOONPETAL_LOG.get(), ModBlocks.MOONPETAL_WOOD.get(),
                ModBlocks.STRIPPED_MOONPETAL_LOG.get(), ModBlocks.STRIPPED_MOONPETAL_WOOD.get(), ModBlocks.MOONPETAL_PLANKS.get(),
                ModBlocks.MOONPETAL_STAIRS.get(), ModBlocks.MOONPETAL_FENCE.get(), ModBlocks.MOONPETAL_FENCE_GATE.get(),
                ModBlocks.MOONPETAL_TRAPDOOR.get(), ModBlocks.MOONPETAL_PRESSURE_PLATE.get(), ModBlocks.MOONPETAL_BUTTON.get(),
                ModBlocks.MOONPETAL_SAPLING.get(), ModBlocks.WITHERED_MOONPETAL_LOG.get(), ModBlocks.WITHERED_MOONPETAL_WOOD.get(),
                ModBlocks.STRIPPED_WITHERED_MOONPETAL_LOG.get(), ModBlocks.STRIPPED_WITHERED_MOONPETAL_WOOD.get())) {
            dropSelf(block);
        }
        add(ModBlocks.MOONPETAL_SLAB.get(), this::createSlabItemTable);
        add(ModBlocks.MOONPETAL_DOOR.get(), this::createDoorTable);
        // Like oak leaves: shears/Silk Touch -> the leaves; otherwise sapling 1/20 (Fortune more) and sticks 1/50.
        add(ModBlocks.MOONPETAL_LEAVES.get(), block -> createLeavesDrops(block, ModBlocks.MOONPETAL_SAPLING.get(), NORMAL_LEAVES_SAPLING_CHANCES));
        dropPottedContents(ModBlocks.POTTED_MOONPETAL_SAPLING.get());

        dropSelf(ModBlocks.REEDS.get());
        dropSelf(ModBlocks.PEAT.get());
        dropSelf(ModBlocks.LOTUS.get());
        // Like vines and hanging roots: only with shears.
        add(ModBlocks.HANGING_SWAMP_MOSS.get(), BlockLootSubProvider::createShearsOnlyDrop);
        // One item per pad: only the north-west part drops (the others vanish with it, like the foot of a bed).
        add(ModBlocks.GIANT_LILY_PAD.get(), block -> createSinglePropConditionTable(block, GiantLilyPadBlock.PART,
                GiantLilyPadBlock.Part.NORTH_WEST));
        dropSelf(ModBlocks.GLOWCAP.get());
        dropSelf(ModBlocks.GLOW_BLOOM.get());
        add(ModBlocks.GLOWING_HANGING_ROOTS.get(), BlockLootSubProvider::createShearsOnlyDrop);
        dropSelf(ModBlocks.BLIGHTED_GLOWCAP.get());
        dropSelf(ModBlocks.BUBBLE_SPRING.get());

        // Like diamond ore -> diamond: one item (Fortune), Silk Touch -> the ore itself.
        for (Block ore : List.of(ModBlocks.TIDESILVER_ORE.get(), ModBlocks.TIDESTONE_TIDESILVER_ORE.get(),
                ModBlocks.DEEPSLATE_TIDESILVER_ORE.get())) {
            add(ore, block -> createOreDrop(block, ModItems.RAW_TIDESILVER.get()));
        }
        for (Block ore : List.of(ModBlocks.JADE_ORE.get(), ModBlocks.TIDESTONE_JADE_ORE.get(), ModBlocks.DEEPSLATE_JADE_ORE.get())) {
            add(ore, block -> createOreDrop(block, ModItems.JADE.get()));
        }
        // Like amethyst: the wall drops itself, the budding block nothing (not even with Silk Touch), buds only with
        // Silk Touch, the cluster gives shards.
        dropSelf(ModBlocks.TOXIC_CRYSTAL_BLOCK.get());
        add(ModBlocks.BUDDING_TOXIC_CRYSTAL.get(), noDrop());
        dropWhenSilkTouch(ModBlocks.SMALL_TOXIC_CRYSTAL_BUD.get());
        dropWhenSilkTouch(ModBlocks.MEDIUM_TOXIC_CRYSTAL_BUD.get());
        dropWhenSilkTouch(ModBlocks.LARGE_TOXIC_CRYSTAL_BUD.get());
        add(ModBlocks.TOXIC_CRYSTAL_CLUSTER.get(), block -> createCrystalClusterTable(block, ModItems.TOXIC_CRYSTAL_SHARD.get()));
        dropSelf(ModBlocks.AQUAMARINE_BLOCK.get());
        add(ModBlocks.BUDDING_AQUAMARINE.get(), noDrop());
        dropWhenSilkTouch(ModBlocks.SMALL_AQUAMARINE_BUD.get());
        dropWhenSilkTouch(ModBlocks.MEDIUM_AQUAMARINE_BUD.get());
        dropWhenSilkTouch(ModBlocks.LARGE_AQUAMARINE_BUD.get());
        add(ModBlocks.AQUAMARINE_CLUSTER.get(), block -> createCrystalClusterTable(block, ModItems.AQUAMARINE_SHARD.get()));

        // Gameplay blocks (72); gas, toxic pool and updraft have no loot table (like air, water and the bubble column).
        dropSelf(ModBlocks.SUCKING_MUD.get());
        add(ModBlocks.CLAWED_TIDESTONE.get(), block -> createSingleItemTableWithSilkTouch(block, ModBlocks.COBBLED_TIDESTONE.get()));
        // A smear of slime: only Silk Touch lifts it.
        dropWhenSilkTouch(ModBlocks.SLIME_TRAIL.get());
        dropSelf(ModBlocks.CREATURE_NEST.get());

        verdant();
        ring4Decor();
        lagoonSandAndClawMarks();
        lagoonSandstone();
        netherRing4();
    }

    /** The lagoon sandstone set (66f) drops like vanilla sandstone: each block itself, a double slab two slabs. */
    private void lagoonSandstone() {
        dropSelf(ModBlocks.LAGOON_SANDSTONE.get());
        dropSelf(ModBlocks.LAGOON_SANDSTONE_STAIRS.get());
        add(ModBlocks.LAGOON_SANDSTONE_SLAB.get(), this::createSlabItemTable);
        dropSelf(ModBlocks.LAGOON_SANDSTONE_WALL.get());
        dropSelf(ModBlocks.CUT_LAGOON_SANDSTONE.get());
        add(ModBlocks.CUT_LAGOON_SANDSTONE_SLAB.get(), this::createSlabItemTable);
        dropSelf(ModBlocks.CHISELED_LAGOON_SANDSTONE.get());
        dropSelf(ModBlocks.SMOOTH_LAGOON_SANDSTONE.get());
        dropSelf(ModBlocks.SMOOTH_LAGOON_SANDSTONE_STAIRS.get());
        add(ModBlocks.SMOOTH_LAGOON_SANDSTONE_SLAB.get(), this::createSlabItemTable);
    }

    /** The golf 3 blocks (66c): the sand drops itself, the claw marks break like their rock (Silk Touch: the block). */
    private void lagoonSandAndClawMarks() {
        dropSelf(ModBlocks.LAGOON_SAND.get());
        add(ModBlocks.CLAWED_STONE.get(), block -> createSingleItemTableWithSilkTouch(block, Items.COBBLESTONE));
        add(ModBlocks.CLAWED_DEEPSLATE.get(), block -> createSingleItemTableWithSilkTouch(block, Items.COBBLED_DEEPSLATE));
    }

    /**
     * The decor rollout blocks: the stones drop themselves (mossy tidestone like mossy cobblestone, also its shapes);
     * the moss and the glowgrass only with shears (like seagrass), the fern like vanilla's fern, the shrub itself.
     */
    private void ring4Decor() {
        for (Block block : List.of(ModBlocks.MOSSY_TIDESTONE.get(), ModBlocks.MOSSY_TIDESTONE_STAIRS.get(),
                ModBlocks.MOSSY_TIDESTONE_WALL.get(), ModBlocks.AQUAMARINE_STONE.get(), ModBlocks.MOONPETAL_SHRUB.get())) {
            dropSelf(block);
        }
        add(ModBlocks.MOSSY_TIDESTONE_SLAB.get(), this::createSlabItemTable);
        add(ModBlocks.FJORD_MOSS.get(), BlockLootSubProvider::createShearsOnlyDrop);
        add(ModBlocks.LAGOON_GLOWGRASS.get(), BlockLootSubProvider::createShearsOnlyDrop);
        add(ModBlocks.MIST_FERN.get(), this::createGrassDrops);
    }

    /** The verdant flora (72b), each like its vanilla model. */
    private void verdant() {
        for (Block block : List.of(ModBlocks.VERDANT_LOG.get(), ModBlocks.VERDANT_WOOD.get(), ModBlocks.STRIPPED_VERDANT_LOG.get(),
                ModBlocks.STRIPPED_VERDANT_WOOD.get(), ModBlocks.VERDANT_PLANKS.get(), ModBlocks.VERDANT_STAIRS.get(),
                ModBlocks.VERDANT_FENCE.get(), ModBlocks.VERDANT_FENCE_GATE.get(), ModBlocks.VERDANT_TRAPDOOR.get(),
                ModBlocks.VERDANT_PRESSURE_PLATE.get(), ModBlocks.VERDANT_BUTTON.get(), ModBlocks.VERDANT_SAPLING.get(),
                ModBlocks.VERDANT_BROMELIAD.get(), ModBlocks.VERDANT_ORCHID.get(), ModBlocks.VERDANT_HANGING_ORCHID.get(),
                ModBlocks.VERDANT_BLOSSOM_BUSH.get(), ModBlocks.VERDANT_WATER_BLOOM.get())) {
            dropSelf(block);
        }
        add(ModBlocks.VERDANT_SLAB.get(), this::createSlabItemTable);
        add(ModBlocks.VERDANT_DOOR.get(), this::createDoorTable);
        add(ModBlocks.VERDANT_LEAVES.get(), block -> createLeavesDrops(block, ModBlocks.VERDANT_SAPLING.get(), NORMAL_LEAVES_SAPLING_CHANCES));
        dropPottedContents(ModBlocks.POTTED_VERDANT_SAPLING.get());
        // Like the sunflower: the lower half drops the plant.
        add(ModBlocks.VERDANT_GIANT_LEAF.get(), block -> createSinglePropConditionTable(block, DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
        // Like fern and large fern: shears give the fern(s), otherwise now and then wheat seeds.
        add(ModBlocks.VERDANT_FERN.get(), this::createGrassDrops);
        add(ModBlocks.VERDANT_TALL_FERN.get(), block -> createDoublePlantWithSeedDrops(block, ModBlocks.VERDANT_FERN.get()));
        dropOther(ModBlocks.VERDANT_WALL_BROMELIAD.get(), ModBlocks.VERDANT_BROMELIAD.get());
        // Like weeping vines: shears or Silk Touch, otherwise a third of the time (Fortune more).
        addNetherVinesDropTable(ModBlocks.VERDANT_VINE.get(), ModBlocks.VERDANT_VINE_PLANT.get());
        // Like cocoa: three fruits when ripe, otherwise one.
        add(ModBlocks.VERDANT_FRUIT_POD.get(), block -> LootTable.lootTable().withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .add(applyExplosionDecay(block, LootItem.lootTableItem(ModItems.VERDANT_FRUIT.get())
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(3.0F))
                                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(CocoaBlock.AGE, 2))))))));
    }

    private void netherRing4() {
        for (Block block : List.of(ModBlocks.BONE_SAND.get(), ModBlocks.BLEACHED_NETHERRACK.get(),
                ModBlocks.FOSSIL_STONE.get(), ModBlocks.FOSSIL_STONE_STAIRS.get(), ModBlocks.FOSSIL_STONE_WALL.get(),
                ModBlocks.POLISHED_FOSSIL_STONE.get(), ModBlocks.POLISHED_FOSSIL_STONE_STAIRS.get(), ModBlocks.POLISHED_FOSSIL_STONE_WALL.get(),
                ModBlocks.FOSSIL_STONE_BRICKS.get(), ModBlocks.FOSSIL_STONE_BRICK_STAIRS.get(), ModBlocks.FOSSIL_STONE_BRICK_WALL.get(),
                ModBlocks.PURITY_CRYSTAL_BLOCK.get(), ModBlocks.PURITY_CRYSTAL_LAMP.get())) {
            dropSelf(block);
        }
        for (Block slab : List.of(ModBlocks.FOSSIL_STONE_SLAB.get(), ModBlocks.POLISHED_FOSSIL_STONE_SLAB.get(),
                ModBlocks.FOSSIL_STONE_BRICK_SLAB.get())) {
            add(slab, this::createSlabItemTable);
        }
        // Like the amethyst cluster: Silk Touch -> the cluster, a pickaxe -> 4 shards (Fortune), anything else 2.
        add(ModBlocks.PURITY_CRYSTAL_CLUSTER.get(), block -> createCrystalClusterTable(block, ModItems.PURITY_CRYSTAL_SHARD.get()));
    }

    private void netherRing3() {
        for (Block block : List.of(ModBlocks.COLD_LAVA.get(), ModBlocks.COLD_LAVA_STAIRS.get(), ModBlocks.COLD_LAVA_WALL.get(),
                ModBlocks.POLISHED_COLD_LAVA.get(), ModBlocks.POLISHED_COLD_LAVA_STAIRS.get(), ModBlocks.POLISHED_COLD_LAVA_WALL.get(),
                ModBlocks.COLD_LAVA_BRICKS.get(), ModBlocks.COLD_LAVA_BRICK_STAIRS.get(), ModBlocks.COLD_LAVA_BRICK_WALL.get())) {
            dropSelf(block);
        }
        for (Block slab : List.of(ModBlocks.COLD_LAVA_SLAB.get(), ModBlocks.POLISHED_COLD_LAVA_SLAB.get(), ModBlocks.COLD_LAVA_BRICK_SLAB.get())) {
            add(slab, this::createSlabItemTable);
        }
        // Like vanilla ice: only Silk Touch takes it along.
        dropWhenSilkTouch(ModBlocks.NETHER_ICE.get());
        dropWhenSilkTouch(ModBlocks.PACKED_NETHER_ICE.get());
        dropSelf(ModBlocks.RIME_NETHERRACK.get());

        dropSelf(ModBlocks.RIMESHROOM_STEM.get());
        dropSelf(ModBlocks.RIMESHROOM_HYPHAE.get());
        dropSelf(ModBlocks.STRIPPED_RIMESHROOM_STEM.get());
        dropSelf(ModBlocks.STRIPPED_RIMESHROOM_HYPHAE.get());
        dropSelf(ModBlocks.RIMESHROOM_PLANKS.get());
        dropSelf(ModBlocks.RIMESHROOM_STAIRS.get());
        add(ModBlocks.RIMESHROOM_SLAB.get(), this::createSlabItemTable);
        dropSelf(ModBlocks.RIMESHROOM_FENCE.get());
        dropSelf(ModBlocks.RIMESHROOM_FENCE_GATE.get());
        add(ModBlocks.RIMESHROOM_DOOR.get(), this::createDoorTable);
        dropSelf(ModBlocks.RIMESHROOM_TRAPDOOR.get());
        dropSelf(ModBlocks.RIMESHROOM_PRESSURE_PLATE.get());
        dropSelf(ModBlocks.RIMESHROOM_BUTTON.get());
        dropSelf(ModBlocks.RIMESHROOM_CAP.get());
        dropSelf(ModBlocks.ICE_STALACTITE.get());
        dropSelf(ModBlocks.GLOWING_FISSURE.get());
        dropWhenSilkTouch(ModBlocks.PRIMORDIAL_ICE.get());
    }

    private void ring3() {
        dropSelf(ModBlocks.ASH_BLOCK.get());
        add(ModBlocks.ASH_LAYER.get(), this::createAshLayerTable);
        // Like loose sand: plain ash without Silk Touch, no ready-made trap material.
        add(ModBlocks.ASH_CRUST.get(), block -> createSingleItemTableWithSilkTouch(block, ModBlocks.ASH_BLOCK.get()));
        dropSelf(ModBlocks.SMOLDERING_ASH.get());
        for (Block block : List.of(ModBlocks.PUMICE.get(), ModBlocks.PUMICE_STAIRS.get(), ModBlocks.PUMICE_WALL.get(),
                ModBlocks.POLISHED_PUMICE.get(), ModBlocks.POLISHED_PUMICE_STAIRS.get(), ModBlocks.POLISHED_PUMICE_WALL.get(),
                ModBlocks.PUMICE_BRICKS.get(), ModBlocks.PUMICE_BRICK_STAIRS.get(), ModBlocks.PUMICE_BRICK_WALL.get())) {
            dropSelf(block);
        }
        for (Block slab : List.of(ModBlocks.PUMICE_SLAB.get(), ModBlocks.POLISHED_PUMICE_SLAB.get(), ModBlocks.PUMICE_BRICK_SLAB.get())) {
            add(slab, this::createSlabItemTable);
        }
        dropSelf(ModBlocks.SMOLDERING_STONE.get());

        dropSelf(ModBlocks.CHARRED_LOG.get());
        dropSelf(ModBlocks.CHARRED_WOOD.get());
        dropSelf(ModBlocks.STRIPPED_CHARRED_LOG.get());
        dropSelf(ModBlocks.STRIPPED_CHARRED_WOOD.get());
        dropSelf(ModBlocks.CHARRED_PLANKS.get());
        dropSelf(ModBlocks.CHARRED_STAIRS.get());
        add(ModBlocks.CHARRED_SLAB.get(), this::createSlabItemTable);
        dropSelf(ModBlocks.CHARRED_FENCE.get());
        dropSelf(ModBlocks.CHARRED_FENCE_GATE.get());
        add(ModBlocks.CHARRED_DOOR.get(), this::createDoorTable);
        dropSelf(ModBlocks.CHARRED_TRAPDOOR.get());
        dropSelf(ModBlocks.CHARRED_PRESSURE_PLATE.get());
        dropSelf(ModBlocks.CHARRED_BUTTON.get());

        // No sapling: like mangrove leaves, shears/Silk Touch -> the leaves, otherwise sticks now and then.
        add(ModBlocks.EMBER_LEAVES.get(), this::createMangroveLeavesDrops);
        dropSelf(ModBlocks.FIRE_BLOOM.get());
        dropPottedContents(ModBlocks.POTTED_FIRE_BLOOM.get());
        // Like nether sprouts: only with shears.
        add(ModBlocks.ASH_SPROUTS.get(), BlockLootSubProvider::createShearsOnlyDrop);
        // Like vanilla's dead bush (and ring 2's dry shrub): itself with shears, otherwise 0-2 sticks.
        add(ModBlocks.CHARRED_SHRUB.get(), block -> createShearsDispatchTable(block, applyExplosionDecay(block,
                LootItem.lootTableItem(Items.STICK).apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 2.0F))))));
        add(ModBlocks.EMBER_BERRY_BUSH.get(), this::createBerryBushTable);

        // Like iron ore -> raw iron / diamond ore -> diamond: one item (Fortune), Silk Touch -> the ore itself.
        for (Block ore : List.of(ModBlocks.VOLCANITE_ORE.get(), ModBlocks.DEEPSLATE_VOLCANITE_ORE.get(), ModBlocks.BASALT_VOLCANITE_ORE.get())) {
            add(ore, block -> createOreDrop(block, ModItems.RAW_VOLCANITE.get()));
        }
        for (Block ore : List.of(ModBlocks.FIRE_RUBY_ORE.get(), ModBlocks.DEEPSLATE_FIRE_RUBY_ORE.get(), ModBlocks.BASALT_FIRE_RUBY_ORE.get())) {
            add(ore, block -> createOreDrop(block, ModItems.FIRE_RUBY.get()));
        }
        // Like copper ore: several at once (2–4, Fortune adds), Silk Touch -> the ore itself.
        for (Block ore : List.of(ModBlocks.SULFUR_ORE.get(), ModBlocks.BASALT_SULFUR_ORE.get())) {
            add(ore, block -> createSilkTouchDispatchTable(block, applyExplosionDecay(block,
                    LootItem.lootTableItem(ModItems.SULFUR.get())
                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F)))
                            .apply(ApplyBonusCount.addOreBonusCount(registries.lookupOrThrow(Registries.ENCHANTMENT)
                                    .getOrThrow(Enchantments.FORTUNE))))));
        }

        // Fire crystal like amethyst: the wall drops itself, buds only with Silk Touch, the cluster gives crystals.
        dropSelf(ModBlocks.FIRE_CRYSTAL_BLOCK.get());
        dropWhenSilkTouch(ModBlocks.SMALL_FIRE_CRYSTAL_BUD.get());
        dropWhenSilkTouch(ModBlocks.MEDIUM_FIRE_CRYSTAL_BUD.get());
        dropWhenSilkTouch(ModBlocks.LARGE_FIRE_CRYSTAL_BUD.get());
        add(ModBlocks.FIRE_CRYSTAL_CLUSTER.get(), block -> createCrystalClusterTable(block, ModItems.FIRE_CRYSTAL.get()));

        // The crust drops itself, not sulfur: sulfur comes from the ores.
        dropSelf(ModBlocks.SULFUR_CRUST.get());
        dropSelf(ModBlocks.FUMAROLE.get());
        dropSelf(ModBlocks.BOILING_MUD.get());
        dropWhenSilkTouch(ModBlocks.GEYSER.get());
        dropSelf(ModBlocks.YELLOW_SULFUR_MUD.get());
        dropSelf(ModBlocks.GREEN_SULFUR_MUD.get());
        dropSelf(ModBlocks.ORANGE_SULFUR_MUD.get());
        // Like amethyst: the buds only with Silk Touch, the crystal drops sulfur (balance in phase 2).
        dropWhenSilkTouch(ModBlocks.SMALL_SULFUR_CRYSTAL_BUD.get());
        dropWhenSilkTouch(ModBlocks.MEDIUM_SULFUR_CRYSTAL_BUD.get());
        dropWhenSilkTouch(ModBlocks.LARGE_SULFUR_CRYSTAL_BUD.get());
        add(ModBlocks.SULFUR_CRYSTAL.get(), block -> createCrystalClusterTable(block, ModItems.SULFUR.get()));
    }

    /**
     * Vanilla's sweet berry bush table on the ember berry bush: a ripe bush (age 3) drops 2–3 ember berries, age 2 drops
     * 1–2 (Fortune adds); younger bushes drop nothing.
     */
    private LootTable.Builder createBerryBushTable(Block block) {
        var fortune = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
        return applyExplosionDecay(block, LootTable.lootTable()
                .withPool(LootPool.lootPool().when(age(block, SweetBerryBushBlock.MAX_AGE))
                        .add(LootItem.lootTableItem(ModItems.EMBER_BERRIES.get()))
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 3.0F)))
                        .apply(ApplyBonusCount.addUniformBonusCount(fortune)))
                .withPool(LootPool.lootPool().when(age(block, 2))
                        .add(LootItem.lootTableItem(ModItems.EMBER_BERRIES.get()))
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F)))
                        .apply(ApplyBonusCount.addUniformBonusCount(fortune))));
    }

    private static LootItemBlockStatePropertyCondition.Builder age(Block block, int age) {
        return LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(SweetBerryBushBlock.AGE, age));
    }

    /**
     * One ash layer item per layer (8 for a full block). There is no loose ash item, so this is vanilla's snow layer
     * table with the layer itself as the drop; the block's tool rule (shovel) decides whether anything drops.
     */
    private LootTable.Builder createAshLayerTable(Block block) {
        var item = LootItem.lootTableItem(block);
        for (int layers = 2; layers <= SnowLayerBlock.MAX_HEIGHT; layers++) {
            item.apply(SetItemCountFunction.setCount(ConstantValue.exactly(layers))
                    .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                            .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(SnowLayerBlock.LAYERS, layers))));
        }
        return LootTable.lootTable().withPool(applyExplosionCondition(block, LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F)).add(item)));
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
        add(ModBlocks.DEW_CRYSTAL_CLUSTER.get(), block -> createCrystalClusterTable(block, ModItems.DEW_CRYSTAL.get()));
    }

    /** Like the amethyst cluster: Silk Touch -> the cluster; a pickaxe -> 4 crystals (Fortune), anything else 2. */
    private LootTable.Builder createCrystalClusterTable(Block block, Item crystal) {
        return createSilkTouchDispatchTable(block,
                LootItem.lootTableItem(crystal)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(4.0F)))
                        .apply(ApplyBonusCount.addOreBonusCount(registries.lookupOrThrow(Registries.ENCHANTMENT)
                                .getOrThrow(Enchantments.FORTUNE)))
                        .when(MatchTool.toolMatches(ItemPredicate.Builder.item().of(ItemTags.CLUSTER_MAX_HARVESTABLES)))
                        .otherwise(applyExplosionDecay(block, LootItem.lootTableItem(crystal)
                                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(2.0F))))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.entries().stream().<Block>map(DeferredHolder::get).toList();
    }
}
