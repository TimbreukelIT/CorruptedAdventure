package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.datagen.model.ArrowItemModels;
import nl.timtoys.corruptedadventure.datagen.model.ConsumableItemModels;
import nl.timtoys.corruptedadventure.item.coating.Coating;
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

        blockItem(ModBlocks.SALT_CRUST);
        blockItem(ModBlocks.SALT_BLOCK);
        blockItem(ModBlocks.POLISHED_SALT);
        blockItem(ModBlocks.POLISHED_SALT_STAIRS);
        blockItem(ModBlocks.POLISHED_SALT_SLAB);
        wallInventory(ModBlocks.POLISHED_SALT_WALL.getId().getPath(), modLoc("block/polished_salt"));
        // salt_crystal: ModBlockStateProvider.crystalCluster writes its item model.
        basicItem(ModItems.SALT.get());

        netherRing2();
        golf1Materials();
        coatings();
        golf2Materials();
        golf3Ring3();
        ConsumableItemModels.build(this);
        golf4Materials();
        ArrowItemModels.build(this);
    }

    /** Phase 2 golf 3 (stream R3): the blocks, the raft as a flat item, the ember berries and the dripshroom stew. */
    private void golf3Ring3() {
        blockItem(ModBlocks.DEW_CRYSTAL_BLOCK);
        blockItem(ModBlocks.SULFUR_BLOCK);
        blockItem(ModBlocks.FIRE_RUBY_INLAID_PUMICE);
        blockItem(ModBlocks.FIRE_RUBY_INLAID_PUMICE_STAIRS);
        blockItem(ModBlocks.FIRE_RUBY_INLAID_PUMICE_SLAB);
        wallInventory(ModBlocks.FIRE_RUBY_INLAID_PUMICE_WALL.getId().getPath(), modLoc("block/fire_ruby_inlaid_pumice"));
        blockItem(ModBlocks.SULFUR_LAMP);
        basicItem(ModBlocks.PUMICE_RAFT.asItem());
        basicItem(ModItems.EMBER_BERRIES.get());
        basicItem(ModItems.DRIPSHROOM_STEW.get());
    }

    /** Phase 2 golf 4 (stream R4): the two inlaid sets. */
    private void golf4Materials() {
        blockItem(ModBlocks.JADE_INLAID_TIDESTONE);
        blockItem(ModBlocks.JADE_INLAID_TIDESTONE_STAIRS);
        blockItem(ModBlocks.JADE_INLAID_TIDESTONE_SLAB);
        wallInventory(ModBlocks.JADE_INLAID_TIDESTONE_WALL.getId().getPath(), modLoc("block/jade_inlaid_tidestone"));
        blockItem(ModBlocks.AQUAMARINE_INLAID_LAGOON_SANDSTONE);
        blockItem(ModBlocks.AQUAMARINE_INLAID_LAGOON_SANDSTONE_STAIRS);
        blockItem(ModBlocks.AQUAMARINE_INLAID_LAGOON_SANDSTONE_SLAB);
        wallInventory(ModBlocks.AQUAMARINE_INLAID_LAGOON_SANDSTONE_WALL.getId().getPath(),
                modLoc("block/aquamarine_inlaid_lagoon_sandstone"));
    }

    /** Phase 2 golf 2 (stream M): the soul crystal block, the inlaid sandstone set and the salt lamp. */
    private void golf2Materials() {
        blockItem(ModBlocks.SOUL_CRYSTAL_BLOCK);
        blockItem(ModBlocks.TURQUOISE_INLAID_SANDSTONE);
        blockItem(ModBlocks.TURQUOISE_INLAID_SANDSTONE_STAIRS);
        blockItem(ModBlocks.TURQUOISE_INLAID_SANDSTONE_SLAB);
        wallInventory(ModBlocks.TURQUOISE_INLAID_SANDSTONE_WALL.getId().getPath(),
                modLoc("block/turquoise_inlaid_sandstone"));
        blockItem(ModBlocks.SALT_LAMP);
    }

    /** Phase 2 golf 1 (stream D): the storage blocks and the ingots and turquoise gem. */
    private void golf1Materials() {
        blockItem(ModBlocks.RAW_SUNSTONE_BLOCK);
        blockItem(ModBlocks.RAW_TURQUOISE_BLOCK);
        blockItem(ModBlocks.SUNSTONE_BLOCK);
        blockItem(ModBlocks.TURQUOISE_BLOCK);
        blockItem(ModBlocks.RAW_VOLCANITE_BLOCK);
        blockItem(ModBlocks.VOLCANITE_BLOCK);
        blockItem(ModBlocks.FIRE_RUBY_BLOCK);
        blockItem(ModBlocks.RAW_TIDESILVER_BLOCK);
        blockItem(ModBlocks.TIDESILVER_BLOCK);
        blockItem(ModBlocks.JADE_BLOCK);
        blockItem(ModBlocks.RAW_NULLITE_BLOCK);
        blockItem(ModBlocks.NULLITE_BLOCK);
        basicItem(ModItems.SUNSTONE_INGOT.get());
        basicItem(ModItems.TURQUOISE.get());
        basicItem(ModItems.VOLCANITE_INGOT.get());
        basicItem(ModItems.TIDESILVER_INGOT.get());
        basicItem(ModItems.NULLITE_INGOT.get());
    }

    /** Phase 2 golf 1 (stream C): the coating templates, flat items like vanilla's trim templates. */
    private void coatings() {
        for (Coating coating : Coating.values()) {
            basicItem(coating.template());
        }
    }

    private void netherRing2() {
        ResourceLocation planks = modLoc("block/dripshroom_planks");
        blockItem(ModBlocks.SOAKED_NETHERRACK);
        blockItem(ModBlocks.SPORE_MOSS);
        blockItem(ModBlocks.DRIPSHROOM_STEM);
        blockItem(ModBlocks.DRIPSHROOM_HYPHAE);
        blockItem(ModBlocks.STRIPPED_DRIPSHROOM_STEM);
        blockItem(ModBlocks.STRIPPED_DRIPSHROOM_HYPHAE);
        blockItem(ModBlocks.DRIPSHROOM_PLANKS);
        blockItem(ModBlocks.DRIPSHROOM_STAIRS);
        blockItem(ModBlocks.DRIPSHROOM_SLAB);
        fenceInventory(ModBlocks.DRIPSHROOM_FENCE.getId().getPath(), planks);
        blockItem(ModBlocks.DRIPSHROOM_FENCE_GATE);
        basicItem(ModBlocks.DRIPSHROOM_DOOR.asItem());
        withExistingParent(ModBlocks.DRIPSHROOM_TRAPDOOR.getId().getPath(), modLoc("block/dripshroom_trapdoor_bottom"));
        blockItem(ModBlocks.DRIPSHROOM_PRESSURE_PLATE);
        buttonInventory(ModBlocks.DRIPSHROOM_BUTTON.getId().getPath(), planks);
        blockItem(ModBlocks.DRIPSHROOM_CAP);
        blockItem(ModBlocks.DRIPSHROOM_LAMP);
        flatBlockItem(ModBlocks.GLOW_MOLD);
        // The stalactite's item is flat on its own texture, like vanilla's pointed dripstone.
        withExistingParent(ModBlocks.SLIME_STALACTITE.getId().getPath(), mcLoc("item/generated"))
                .texture("layer0", modLoc("item/slime_stalactite"));
        flatBlockItem(ModBlocks.SPORE_SPROUTS);
        blockItem(ModBlocks.DEW_SOURCE);
        // The dew crystal stages: ModBlockStateProvider.crystalCluster writes their item models.
        basicItem(ModItems.DEW_CRYSTAL.get());

        ring3();
    }

    private void ring3() {
        blockItem(ModBlocks.ASH_BLOCK);
        // Like vanilla's snow item: the one-layer model.
        withExistingParent(ModBlocks.ASH_LAYER.getId().getPath(), modLoc("block/ash_layer_height2"));
        blockItem(ModBlocks.ASH_CRUST);
        blockItem(ModBlocks.SMOLDERING_ASH);
        blockItem(ModBlocks.PUMICE);
        blockItem(ModBlocks.PUMICE_STAIRS);
        blockItem(ModBlocks.PUMICE_SLAB);
        wallInventory(ModBlocks.PUMICE_WALL.getId().getPath(), modLoc("block/pumice"));
        blockItem(ModBlocks.POLISHED_PUMICE);
        blockItem(ModBlocks.POLISHED_PUMICE_STAIRS);
        blockItem(ModBlocks.POLISHED_PUMICE_SLAB);
        wallInventory(ModBlocks.POLISHED_PUMICE_WALL.getId().getPath(), modLoc("block/polished_pumice"));
        blockItem(ModBlocks.PUMICE_BRICKS);
        blockItem(ModBlocks.PUMICE_BRICK_STAIRS);
        blockItem(ModBlocks.PUMICE_BRICK_SLAB);
        wallInventory(ModBlocks.PUMICE_BRICK_WALL.getId().getPath(), modLoc("block/pumice_bricks"));
        blockItem(ModBlocks.SMOLDERING_STONE);

        ResourceLocation planks = modLoc("block/charred_planks");
        blockItem(ModBlocks.CHARRED_LOG);
        blockItem(ModBlocks.CHARRED_WOOD);
        blockItem(ModBlocks.STRIPPED_CHARRED_LOG);
        blockItem(ModBlocks.STRIPPED_CHARRED_WOOD);
        blockItem(ModBlocks.CHARRED_PLANKS);
        blockItem(ModBlocks.CHARRED_STAIRS);
        blockItem(ModBlocks.CHARRED_SLAB);
        fenceInventory(ModBlocks.CHARRED_FENCE.getId().getPath(), planks);
        blockItem(ModBlocks.CHARRED_FENCE_GATE);
        basicItem(ModBlocks.CHARRED_DOOR.asItem());
        withExistingParent(ModBlocks.CHARRED_TRAPDOOR.getId().getPath(), modLoc("block/charred_trapdoor_bottom"));
        blockItem(ModBlocks.CHARRED_PRESSURE_PLATE);
        buttonInventory(ModBlocks.CHARRED_BUTTON.getId().getPath(), planks);

        blockItem(ModBlocks.EMBER_LEAVES);
        flatBlockItem(ModBlocks.FIRE_BLOOM);
        flatBlockItem(ModBlocks.ASH_SPROUTS);
        flatBlockItem(ModBlocks.CHARRED_SHRUB);
        // ember_berry_bush: no item of its own (its item is the ember berries, golf3Ring3).

        blockItem(ModBlocks.VOLCANITE_ORE);
        blockItem(ModBlocks.DEEPSLATE_VOLCANITE_ORE);
        blockItem(ModBlocks.BASALT_VOLCANITE_ORE);
        blockItem(ModBlocks.SULFUR_ORE);
        blockItem(ModBlocks.BASALT_SULFUR_ORE);
        blockItem(ModBlocks.FIRE_RUBY_ORE);
        blockItem(ModBlocks.DEEPSLATE_FIRE_RUBY_ORE);
        blockItem(ModBlocks.BASALT_FIRE_RUBY_ORE);
        basicItem(ModItems.RAW_VOLCANITE.get());
        basicItem(ModItems.SULFUR.get());
        basicItem(ModItems.FIRE_RUBY.get());
        blockItem(ModBlocks.FIRE_CRYSTAL_BLOCK);
        // The fire crystal stages: ModBlockStateProvider.crystalCluster writes their item models.
        basicItem(ModItems.FIRE_CRYSTAL.get());

        blockItem(ModBlocks.SULFUR_CRUST);
        blockItem(ModBlocks.FUMAROLE);
        blockItem(ModBlocks.BOILING_MUD);
        blockItem(ModBlocks.GEYSER);
        blockItem(ModBlocks.YELLOW_SULFUR_MUD);
        blockItem(ModBlocks.GREEN_SULFUR_MUD);
        blockItem(ModBlocks.ORANGE_SULFUR_MUD);
        // The sulfur crystal stages: ModBlockStateProvider.crystalCluster writes their item models.

        netherRing3();
    }

    private void netherRing3() {
        blockItem(ModBlocks.COLD_LAVA);
        blockItem(ModBlocks.COLD_LAVA_STAIRS);
        blockItem(ModBlocks.COLD_LAVA_SLAB);
        wallInventory(ModBlocks.COLD_LAVA_WALL.getId().getPath(), modLoc("block/cold_lava"));
        blockItem(ModBlocks.POLISHED_COLD_LAVA);
        blockItem(ModBlocks.POLISHED_COLD_LAVA_STAIRS);
        blockItem(ModBlocks.POLISHED_COLD_LAVA_SLAB);
        wallInventory(ModBlocks.POLISHED_COLD_LAVA_WALL.getId().getPath(), modLoc("block/polished_cold_lava"));
        blockItem(ModBlocks.COLD_LAVA_BRICKS);
        blockItem(ModBlocks.COLD_LAVA_BRICK_STAIRS);
        blockItem(ModBlocks.COLD_LAVA_BRICK_SLAB);
        wallInventory(ModBlocks.COLD_LAVA_BRICK_WALL.getId().getPath(), modLoc("block/cold_lava_bricks"));
        blockItem(ModBlocks.NETHER_ICE);
        blockItem(ModBlocks.PACKED_NETHER_ICE);
        blockItem(ModBlocks.RIME_NETHERRACK);

        ResourceLocation planks = modLoc("block/rimeshroom_planks");
        blockItem(ModBlocks.RIMESHROOM_STEM);
        blockItem(ModBlocks.RIMESHROOM_HYPHAE);
        blockItem(ModBlocks.STRIPPED_RIMESHROOM_STEM);
        blockItem(ModBlocks.STRIPPED_RIMESHROOM_HYPHAE);
        blockItem(ModBlocks.RIMESHROOM_PLANKS);
        blockItem(ModBlocks.RIMESHROOM_STAIRS);
        blockItem(ModBlocks.RIMESHROOM_SLAB);
        fenceInventory(ModBlocks.RIMESHROOM_FENCE.getId().getPath(), planks);
        blockItem(ModBlocks.RIMESHROOM_FENCE_GATE);
        basicItem(ModBlocks.RIMESHROOM_DOOR.asItem());
        withExistingParent(ModBlocks.RIMESHROOM_TRAPDOOR.getId().getPath(), modLoc("block/rimeshroom_trapdoor_bottom"));
        blockItem(ModBlocks.RIMESHROOM_PRESSURE_PLATE);
        buttonInventory(ModBlocks.RIMESHROOM_BUTTON.getId().getPath(), planks);
        blockItem(ModBlocks.RIMESHROOM_CAP);
        // Flat on its own texture, like the slime stalactite.
        withExistingParent(ModBlocks.ICE_STALACTITE.getId().getPath(), mcLoc("item/generated"))
                .texture("layer0", modLoc("item/ice_stalactite"));
        blockItem(ModBlocks.GLOWING_FISSURE);
        blockItem(ModBlocks.PRIMORDIAL_ICE);

        ring4();
    }

    private void ring4() {
        blockItem(ModBlocks.TIDESTONE);
        blockItem(ModBlocks.TIDESTONE_STAIRS);
        blockItem(ModBlocks.TIDESTONE_SLAB);
        wallInventory(ModBlocks.TIDESTONE_WALL.getId().getPath(), modLoc("block/tidestone"));
        blockItem(ModBlocks.COBBLED_TIDESTONE);
        blockItem(ModBlocks.COBBLED_TIDESTONE_STAIRS);
        blockItem(ModBlocks.COBBLED_TIDESTONE_SLAB);
        wallInventory(ModBlocks.COBBLED_TIDESTONE_WALL.getId().getPath(), modLoc("block/cobbled_tidestone"));
        blockItem(ModBlocks.POLISHED_TIDESTONE);
        blockItem(ModBlocks.POLISHED_TIDESTONE_STAIRS);
        blockItem(ModBlocks.POLISHED_TIDESTONE_SLAB);
        wallInventory(ModBlocks.POLISHED_TIDESTONE_WALL.getId().getPath(), modLoc("block/polished_tidestone"));
        blockItem(ModBlocks.TIDESTONE_BRICKS);
        blockItem(ModBlocks.TIDESTONE_BRICK_STAIRS);
        blockItem(ModBlocks.TIDESTONE_BRICK_SLAB);
        wallInventory(ModBlocks.TIDESTONE_BRICK_WALL.getId().getPath(), modLoc("block/tidestone_bricks"));

        ResourceLocation planks = modLoc("block/moonpetal_planks");
        blockItem(ModBlocks.MOONPETAL_LOG);
        blockItem(ModBlocks.MOONPETAL_WOOD);
        blockItem(ModBlocks.STRIPPED_MOONPETAL_LOG);
        blockItem(ModBlocks.STRIPPED_MOONPETAL_WOOD);
        blockItem(ModBlocks.MOONPETAL_PLANKS);
        blockItem(ModBlocks.MOONPETAL_STAIRS);
        blockItem(ModBlocks.MOONPETAL_SLAB);
        fenceInventory(ModBlocks.MOONPETAL_FENCE.getId().getPath(), planks);
        blockItem(ModBlocks.MOONPETAL_FENCE_GATE);
        basicItem(ModBlocks.MOONPETAL_DOOR.asItem());
        withExistingParent(ModBlocks.MOONPETAL_TRAPDOOR.getId().getPath(), modLoc("block/moonpetal_trapdoor_bottom"));
        blockItem(ModBlocks.MOONPETAL_PRESSURE_PLATE);
        buttonInventory(ModBlocks.MOONPETAL_BUTTON.getId().getPath(), planks);
        blockItem(ModBlocks.MOONPETAL_LEAVES);
        flatBlockItem(ModBlocks.MOONPETAL_SAPLING);
        blockItem(ModBlocks.WITHERED_MOONPETAL_LOG);
        blockItem(ModBlocks.WITHERED_MOONPETAL_WOOD);
        blockItem(ModBlocks.STRIPPED_WITHERED_MOONPETAL_LOG);
        blockItem(ModBlocks.STRIPPED_WITHERED_MOONPETAL_WOOD);

        // The reed item shows the plume, like vanilla's sugar cane item shows the plant.
        withExistingParent(ModBlocks.REEDS.getId().getPath(), mcLoc("item/generated"))
                .texture("layer0", modLoc("block/reeds_top"));
        blockItem(ModBlocks.PEAT);
        flatBlockItem(ModBlocks.HANGING_SWAMP_MOSS);
        // The whole pad as one flat item (the block only has quarter textures).
        withExistingParent(ModBlocks.GIANT_LILY_PAD.getId().getPath(), mcLoc("item/generated"))
                .texture("layer0", modLoc("item/giant_lily_pad"));
        flatBlockItem(ModBlocks.LOTUS);
        flatBlockItem(ModBlocks.GLOWCAP);
        flatBlockItem(ModBlocks.GLOW_BLOOM);
        flatBlockItem(ModBlocks.GLOWING_HANGING_ROOTS);
        flatBlockItem(ModBlocks.BLIGHTED_GLOWCAP);
        blockItem(ModBlocks.BUBBLE_SPRING);

        blockItem(ModBlocks.TIDESILVER_ORE);
        blockItem(ModBlocks.TIDESTONE_TIDESILVER_ORE);
        blockItem(ModBlocks.DEEPSLATE_TIDESILVER_ORE);
        blockItem(ModBlocks.JADE_ORE);
        blockItem(ModBlocks.TIDESTONE_JADE_ORE);
        blockItem(ModBlocks.DEEPSLATE_JADE_ORE);
        blockItem(ModBlocks.TOXIC_CRYSTAL_BLOCK);
        blockItem(ModBlocks.BUDDING_TOXIC_CRYSTAL);
        blockItem(ModBlocks.AQUAMARINE_BLOCK);
        blockItem(ModBlocks.BUDDING_AQUAMARINE);
        // The crystal stages: ModBlockStateProvider.crystalCluster writes their item models.
        basicItem(ModItems.RAW_TIDESILVER.get());
        basicItem(ModItems.JADE.get());
        basicItem(ModItems.TOXIC_CRYSTAL_SHARD.get());
        basicItem(ModItems.AQUAMARINE_SHARD.get());

        // Gameplay blocks (72); the swamp gas and the toxic pool have no block item.
        blockItem(ModBlocks.SUCKING_MUD);
        basicItem(ModItems.TOXIC_POOL_BUCKET.get());
        flatBlockItem(ModBlocks.UPDRAFT);
        blockItem(ModBlocks.CLAWED_TIDESTONE);
        flatBlockItem(ModBlocks.SLIME_TRAIL);
        blockItem(ModBlocks.CREATURE_NEST);

        verdant();
        netherRing4();
        ring5();
        tiers();
        GearItemModels.build(this, existingFileHelper);
    }

    /** Phase 2 wave 1 (stream B): the upgrade smithing templates, flat items like vanilla's. */
    private void tiers() {
        basicItem(ModItems.SUNSTONE_UPGRADE_SMITHING_TEMPLATE.get());
        basicItem(ModItems.VOLCANITE_UPGRADE_SMITHING_TEMPLATE.get());
        basicItem(ModItems.TIDESILVER_UPGRADE_SMITHING_TEMPLATE.get());
        basicItem(ModItems.NULLITE_UPGRADE_SMITHING_TEMPLATE.get());
    }

    /** The verdant flora (72b); the wall bromeliad, the vine stem and the fruit pod have no item of their own. */
    private void verdant() {
        ResourceLocation planks = modLoc("block/verdant_planks");
        blockItem(ModBlocks.VERDANT_LOG);
        blockItem(ModBlocks.VERDANT_WOOD);
        blockItem(ModBlocks.STRIPPED_VERDANT_LOG);
        blockItem(ModBlocks.STRIPPED_VERDANT_WOOD);
        blockItem(ModBlocks.VERDANT_PLANKS);
        blockItem(ModBlocks.VERDANT_STAIRS);
        blockItem(ModBlocks.VERDANT_SLAB);
        fenceInventory(ModBlocks.VERDANT_FENCE.getId().getPath(), planks);
        blockItem(ModBlocks.VERDANT_FENCE_GATE);
        basicItem(ModBlocks.VERDANT_DOOR.asItem());
        withExistingParent(ModBlocks.VERDANT_TRAPDOOR.getId().getPath(), modLoc("block/verdant_trapdoor_bottom"));
        blockItem(ModBlocks.VERDANT_PRESSURE_PLATE);
        buttonInventory(ModBlocks.VERDANT_BUTTON.getId().getPath(), planks);
        blockItem(ModBlocks.VERDANT_LEAVES);
        flatBlockItem(ModBlocks.VERDANT_SAPLING);
        // Double plants show their top half, like vanilla's large fern.
        flatItem(ModBlocks.VERDANT_GIANT_LEAF, "verdant_giant_leaf_top");
        flatBlockItem(ModBlocks.VERDANT_FERN);
        flatItem(ModBlocks.VERDANT_TALL_FERN, "verdant_tall_fern_top");
        flatBlockItem(ModBlocks.VERDANT_BROMELIAD);
        flatBlockItem(ModBlocks.VERDANT_ORCHID);
        flatBlockItem(ModBlocks.VERDANT_HANGING_ORCHID);
        flatBlockItem(ModBlocks.VERDANT_BLOSSOM_BUSH);
        flatBlockItem(ModBlocks.VERDANT_VINE);
        flatBlockItem(ModBlocks.VERDANT_WATER_BLOOM);
        basicItem(ModItems.VERDANT_FRUIT.get());

        // Decor rollout blocks: the stones on their block models, the plants flat on their texture.
        blockItem(ModBlocks.MOSSY_TIDESTONE);
        blockItem(ModBlocks.MOSSY_TIDESTONE_STAIRS);
        blockItem(ModBlocks.MOSSY_TIDESTONE_SLAB);
        wallInventory(ModBlocks.MOSSY_TIDESTONE_WALL.getId().getPath(), modLoc("block/mossy_tidestone"));
        blockItem(ModBlocks.AQUAMARINE_STONE);
        flatBlockItem(ModBlocks.FJORD_MOSS);
        flatBlockItem(ModBlocks.MIST_FERN);
        flatBlockItem(ModBlocks.MOONPETAL_SHRUB);
        flatBlockItem(ModBlocks.LAGOON_GLOWGRASS);

        // Golf 3 blocks (66c): cubes on their block models.
        blockItem(ModBlocks.LAGOON_SAND);
        // Lagoon sandstone (66f): like the scorched sandstone set.
        blockItem(ModBlocks.LAGOON_SANDSTONE);
        blockItem(ModBlocks.LAGOON_SANDSTONE_STAIRS);
        blockItem(ModBlocks.LAGOON_SANDSTONE_SLAB);
        wallInventory(ModBlocks.LAGOON_SANDSTONE_WALL.getId().getPath(), modLoc("block/lagoon_sandstone"));
        blockItem(ModBlocks.CUT_LAGOON_SANDSTONE);
        blockItem(ModBlocks.CUT_LAGOON_SANDSTONE_SLAB);
        blockItem(ModBlocks.CHISELED_LAGOON_SANDSTONE);
        blockItem(ModBlocks.SMOOTH_LAGOON_SANDSTONE);
        blockItem(ModBlocks.SMOOTH_LAGOON_SANDSTONE_STAIRS);
        blockItem(ModBlocks.SMOOTH_LAGOON_SANDSTONE_SLAB);
        blockItem(ModBlocks.CLAWED_STONE);
        blockItem(ModBlocks.CLAWED_DEEPSLATE);
    }

    private void netherRing4() {
        blockItem(ModBlocks.BONE_SAND);
        blockItem(ModBlocks.BLEACHED_NETHERRACK);
        blockItem(ModBlocks.FOSSIL_STONE);
        blockItem(ModBlocks.FOSSIL_STONE_STAIRS);
        blockItem(ModBlocks.FOSSIL_STONE_SLAB);
        wallInventory(ModBlocks.FOSSIL_STONE_WALL.getId().getPath(), modLoc("block/fossil_stone"));
        blockItem(ModBlocks.POLISHED_FOSSIL_STONE);
        blockItem(ModBlocks.POLISHED_FOSSIL_STONE_STAIRS);
        blockItem(ModBlocks.POLISHED_FOSSIL_STONE_SLAB);
        wallInventory(ModBlocks.POLISHED_FOSSIL_STONE_WALL.getId().getPath(), modLoc("block/polished_fossil_stone"));
        blockItem(ModBlocks.FOSSIL_STONE_BRICKS);
        blockItem(ModBlocks.FOSSIL_STONE_BRICK_STAIRS);
        blockItem(ModBlocks.FOSSIL_STONE_BRICK_SLAB);
        wallInventory(ModBlocks.FOSSIL_STONE_BRICK_WALL.getId().getPath(), modLoc("block/fossil_stone_bricks"));
        blockItem(ModBlocks.PURITY_CRYSTAL_BLOCK);
        // purity_crystal_cluster: ModBlockStateProvider.crystalCluster writes its item model.
        blockItem(ModBlocks.PURITY_CRYSTAL_LAMP);
        basicItem(ModItems.PURITY_CRYSTAL_SHARD.get());
    }

    /**
     * Ring 5 (88): cubes and the wood set like the verdant set; the door and the two signs are flat items on T's
     * {@code item/} textures. The wall signs have no item.
     */
    private void ring5() {
        blockItem(ModBlocks.CORRUPTED_GRASS_BLOCK);
        blockItem(ModBlocks.CORRUPTED_DIRT);
        blockItem(ModBlocks.CORRUPTED_STONE);
        blockItem(ModBlocks.CORRUPTED_COBBLESTONE);
        blockItem(ModBlocks.CORRUPTED_SAND);
        ResourceLocation planks = modLoc("block/corrupted_planks");
        blockItem(ModBlocks.CORRUPTED_LOG);
        blockItem(ModBlocks.CORRUPTED_WOOD);
        blockItem(ModBlocks.STRIPPED_CORRUPTED_LOG);
        blockItem(ModBlocks.STRIPPED_CORRUPTED_WOOD);
        blockItem(ModBlocks.CORRUPTED_PLANKS);
        blockItem(ModBlocks.CORRUPTED_STAIRS);
        blockItem(ModBlocks.CORRUPTED_SLAB);
        fenceInventory(ModBlocks.CORRUPTED_FENCE.getId().getPath(), planks);
        blockItem(ModBlocks.CORRUPTED_FENCE_GATE);
        basicItem(ModBlocks.CORRUPTED_DOOR.asItem());
        withExistingParent(ModBlocks.CORRUPTED_TRAPDOOR.getId().getPath(), modLoc("block/corrupted_trapdoor_bottom"));
        blockItem(ModBlocks.CORRUPTED_PRESSURE_PLATE);
        buttonInventory(ModBlocks.CORRUPTED_BUTTON.getId().getPath(), planks);
        basicItem(ModBlocks.CORRUPTED_SIGN.asItem());
        basicItem(ModBlocks.CORRUPTED_HANGING_SIGN.asItem());
        blockItem(ModBlocks.CORRUPTED_LEAVES);
        // 89.
        blockItem(ModBlocks.GLITCH_BLOCK);
        blockItem(ModBlocks.VOIDSTONE);
        blockItem(ModBlocks.NULLITE_ORE);
        blockItem(ModBlocks.VOIDSTONE_NULLITE_ORE);
        basicItem(ModItems.GLITCH_SHARD.get());
        basicItem(ModItems.RAW_NULLITE.get());
        // 90.
        blockItem(ModBlocks.CHALKRACK);
        blockItem(ModBlocks.WHITE_STONE);
        blockItem(ModBlocks.WHITE_STONE_STAIRS);
        blockItem(ModBlocks.WHITE_STONE_SLAB);
        wallInventory(ModBlocks.WHITE_STONE_WALL.getId().getPath(), modLoc("block/white_stone"));
        blockItem(ModBlocks.POLISHED_WHITE_STONE);
        blockItem(ModBlocks.POLISHED_WHITE_STONE_STAIRS);
        blockItem(ModBlocks.POLISHED_WHITE_STONE_SLAB);
        wallInventory(ModBlocks.POLISHED_WHITE_STONE_WALL.getId().getPath(), modLoc("block/polished_white_stone"));
        blockItem(ModBlocks.WHITE_STONE_BRICKS);
        blockItem(ModBlocks.WHITE_STONE_BRICK_STAIRS);
        blockItem(ModBlocks.WHITE_STONE_BRICK_SLAB);
        wallInventory(ModBlocks.WHITE_STONE_BRICK_WALL.getId().getPath(), modLoc("block/white_stone_bricks"));
        blockItem(ModBlocks.WHITE_STONE_PILLAR);
        blockItem(ModBlocks.MIRROR_STONE);
        blockItem(ModBlocks.STILL_LIGHT);
        blockItem(ModBlocks.WHITE_CORE_ORE);
        basicItem(ModItems.WHITE_CORE.get());
    }

    /** A flat item showing the block texture, like vanilla's plants. */
    private void flatBlockItem(DeferredBlock<? extends Block> block) {
        String name = block.getId().getPath();
        withExistingParent(name, mcLoc("item/generated")).texture("layer0", modLoc("block/" + name));
    }

    /** A flat item on the block texture {@code texture}. */
    private void flatItem(DeferredBlock<? extends Block> block, String texture) {
        withExistingParent(block.getId().getPath(), mcLoc("item/generated")).texture("layer0", modLoc("block/" + texture));
    }

    private void blockItem(DeferredBlock<? extends Block> block) {
        withExistingParent(block.getId().getPath(), modLoc("block/" + block.getId().getPath()));
    }
}
