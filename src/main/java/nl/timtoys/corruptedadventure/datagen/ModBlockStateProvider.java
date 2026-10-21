package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import nl.timtoys.corruptedadventure.block.VerdantVineBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.block.GiantLilyPadBlock;
import nl.timtoys.corruptedadventure.block.ReedsBlock;
import nl.timtoys.corruptedadventure.block.SlimeTrailBlock;
import nl.timtoys.corruptedadventure.block.StalactiteBlock;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Blockstates and block models. Textures must exist before {@code runData}
 * (generate them with {@code tools/textures/Ring1Textures.java} and {@code Ring2Textures.java}). Render types live in the
 * model JSON.
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

        ResourceLocation netherrack = mcLoc("block/netherrack");
        oreWithOverlay(ModBlocks.SOUL_CRYSTAL_ORE.get(), netherrack,
                CubeTextures.all(netherrack), CubeTextures.all(modLoc("block/soul_crystal_ore_overlay")), true);
        oreWithOverlay(ModBlocks.CRIMSON_SOUL_CRYSTAL_ORE.get(), mcLoc("block/crimson_nylium"),
                new CubeTextures(mcLoc("block/crimson_nylium"), mcLoc("block/crimson_nylium_side"), netherrack),
                new CubeTextures(modLoc("block/crimson_soul_crystal_ore_overlay_top"),
                        modLoc("block/crimson_soul_crystal_ore_overlay_side"), modLoc("block/soul_crystal_ore_overlay")), true);

        ring2();
        ring3();
        netherRing3();
        ring4();
        netherRing4();
        ring5();
    }

    /**
     * Ring 5 (88) on T's paths ({@code golf-1-textures.md} §2): the corrupted variants laid out like their vanilla
     * counterparts, without tint (grass and leaves carry their grey in the texture); the wood set like the verdant set.
     * The signs' block models only carry the particle (the planks; the stripped log for the hanging sign).
     */
    private void ring5() {
        ResourceLocation dirt = blockTexture(ModBlocks.CORRUPTED_DIRT.get());
        ModelFile grass = models().cubeBottomTop("corrupted_grass_block", modLoc("block/corrupted_grass_block_side"), dirt,
                modLoc("block/corrupted_grass_block_top")).texture("particle", dirt);
        simpleBlock(ModBlocks.CORRUPTED_GRASS_BLOCK.get(), ConfiguredModel.allYRotations(grass, 0, false));
        randomlyRotated(ModBlocks.CORRUPTED_DIRT.get());
        mirroredAndRotated(ModBlocks.CORRUPTED_STONE.get());
        simpleBlock(ModBlocks.CORRUPTED_COBBLESTONE.get());
        randomlyRotated(ModBlocks.CORRUPTED_SAND.get());

        logBlock(ModBlocks.CORRUPTED_LOG.get());
        axisBlock(ModBlocks.CORRUPTED_WOOD.get(), blockTexture(ModBlocks.CORRUPTED_LOG.get()), blockTexture(ModBlocks.CORRUPTED_LOG.get()));
        ResourceLocation stripped = blockTexture(ModBlocks.STRIPPED_CORRUPTED_LOG.get());
        logBlock(ModBlocks.STRIPPED_CORRUPTED_LOG.get());
        axisBlock(ModBlocks.STRIPPED_CORRUPTED_WOOD.get(), stripped, stripped);
        ResourceLocation planks = blockTexture(ModBlocks.CORRUPTED_PLANKS.get());
        simpleBlock(ModBlocks.CORRUPTED_PLANKS.get());
        stairsBlock(ModBlocks.CORRUPTED_STAIRS.get(), planks);
        slabBlock(ModBlocks.CORRUPTED_SLAB.get(), planks, planks);
        fenceBlock(ModBlocks.CORRUPTED_FENCE.get(), planks);
        fenceGateBlock(ModBlocks.CORRUPTED_FENCE_GATE.get(), planks);
        doorBlockWithRenderType(ModBlocks.CORRUPTED_DOOR.get(),
                modLoc("block/corrupted_door_bottom"), modLoc("block/corrupted_door_top"), "cutout");
        trapdoorBlockWithRenderType(ModBlocks.CORRUPTED_TRAPDOOR.get(), modLoc("block/corrupted_trapdoor"), true, "cutout");
        pressurePlateBlock(ModBlocks.CORRUPTED_PRESSURE_PLATE.get(), planks);
        buttonBlock(ModBlocks.CORRUPTED_BUTTON.get(), planks);
        signBlock(ModBlocks.CORRUPTED_SIGN.get(), ModBlocks.CORRUPTED_WALL_SIGN.get(), planks);
        hangingSignBlock(ModBlocks.CORRUPTED_HANGING_SIGN.get(), ModBlocks.CORRUPTED_WALL_HANGING_SIGN.get(), stripped);
        simpleBlock(ModBlocks.CORRUPTED_LEAVES.get(), models().cubeAll("corrupted_leaves",
                blockTexture(ModBlocks.CORRUPTED_LEAVES.get())).renderType("cutout_mipped"));

        // 89: the glitch block animates through its .mcmeta (T); voidstone's stars and the nullite crystals are emissive
        // overlays (no block light). The voidstone ore shows plain voidstone under the crystals.
        simpleBlock(ModBlocks.GLITCH_BLOCK.get());
        ResourceLocation voidstone = blockTexture(ModBlocks.VOIDSTONE.get());
        oreWithOverlay(ModBlocks.VOIDSTONE.get(), voidstone, CubeTextures.all(voidstone),
                CubeTextures.all(modLoc("block/voidstone_glow")), true);
        CubeTextures nullite = CubeTextures.all(modLoc("block/nullite_ore_overlay"));
        ResourceLocation corruptedStone = blockTexture(ModBlocks.CORRUPTED_STONE.get());
        oreWithOverlay(ModBlocks.NULLITE_ORE.get(), corruptedStone, CubeTextures.all(corruptedStone), nullite, true);
        oreWithOverlay(ModBlocks.VOIDSTONE_NULLITE_ORE.get(), voidstone, CubeTextures.all(voidstone), nullite, true);
    }

    /** Nether ring 4: bone sand, bleached netherrack, the fossil stone set (like tidestone) and the purity crystal. */
    private void netherRing4() {
        simpleBlock(ModBlocks.BONE_SAND.get());
        simpleBlock(ModBlocks.BLEACHED_NETHERRACK.get());
        stoneFamily(ModBlocks.FOSSIL_STONE.get(), ModBlocks.FOSSIL_STONE_STAIRS.get(), ModBlocks.FOSSIL_STONE_SLAB.get(),
                ModBlocks.FOSSIL_STONE_WALL.get());
        stoneFamily(ModBlocks.POLISHED_FOSSIL_STONE.get(), ModBlocks.POLISHED_FOSSIL_STONE_STAIRS.get(),
                ModBlocks.POLISHED_FOSSIL_STONE_SLAB.get(), ModBlocks.POLISHED_FOSSIL_STONE_WALL.get());
        stoneFamily(ModBlocks.FOSSIL_STONE_BRICKS.get(), ModBlocks.FOSSIL_STONE_BRICK_STAIRS.get(),
                ModBlocks.FOSSIL_STONE_BRICK_SLAB.get(), ModBlocks.FOSSIL_STONE_BRICK_WALL.get());
        simpleBlock(ModBlocks.PURITY_CRYSTAL_BLOCK.get());
        // The cluster pulses and glows evenly like the fire crystal stages (T, 71a).
        crystalCluster(ModBlocks.PURITY_CRYSTAL_CLUSTER.get(), true);
        simpleBlock(ModBlocks.PURITY_CRYSTAL_LAMP.get());
    }

    /** Ring 4: tidestone, the moonpetal set (laid out like the palm set), the swamp and water flora, the glowing flora. */
    private void ring4() {
        // The layered texture repeats visibly on large cliffs (golf 3): mirrored and rotated variants, like vanilla stone.
        ResourceLocation tidestone = blockTexture(ModBlocks.TIDESTONE.get());
        mirroredAndRotated(ModBlocks.TIDESTONE.get());
        stairsBlock(ModBlocks.TIDESTONE_STAIRS.get(), tidestone);
        slabBlock(ModBlocks.TIDESTONE_SLAB.get(), tidestone, tidestone);
        wallBlock(ModBlocks.TIDESTONE_WALL.get(), tidestone);
        stoneFamily(ModBlocks.COBBLED_TIDESTONE.get(), ModBlocks.COBBLED_TIDESTONE_STAIRS.get(),
                ModBlocks.COBBLED_TIDESTONE_SLAB.get(), ModBlocks.COBBLED_TIDESTONE_WALL.get());
        stoneFamily(ModBlocks.POLISHED_TIDESTONE.get(), ModBlocks.POLISHED_TIDESTONE_STAIRS.get(),
                ModBlocks.POLISHED_TIDESTONE_SLAB.get(), ModBlocks.POLISHED_TIDESTONE_WALL.get());
        stoneFamily(ModBlocks.TIDESTONE_BRICKS.get(), ModBlocks.TIDESTONE_BRICK_STAIRS.get(),
                ModBlocks.TIDESTONE_BRICK_SLAB.get(), ModBlocks.TIDESTONE_BRICK_WALL.get());

        logBlock(ModBlocks.MOONPETAL_LOG.get());
        axisBlock(ModBlocks.MOONPETAL_WOOD.get(), blockTexture(ModBlocks.MOONPETAL_LOG.get()), blockTexture(ModBlocks.MOONPETAL_LOG.get()));
        logBlock(ModBlocks.STRIPPED_MOONPETAL_LOG.get());
        axisBlock(ModBlocks.STRIPPED_MOONPETAL_WOOD.get(),
                blockTexture(ModBlocks.STRIPPED_MOONPETAL_LOG.get()), blockTexture(ModBlocks.STRIPPED_MOONPETAL_LOG.get()));
        ResourceLocation planks = blockTexture(ModBlocks.MOONPETAL_PLANKS.get());
        simpleBlock(ModBlocks.MOONPETAL_PLANKS.get());
        stairsBlock(ModBlocks.MOONPETAL_STAIRS.get(), planks);
        slabBlock(ModBlocks.MOONPETAL_SLAB.get(), planks, planks);
        fenceBlock(ModBlocks.MOONPETAL_FENCE.get(), planks);
        fenceGateBlock(ModBlocks.MOONPETAL_FENCE_GATE.get(), planks);
        doorBlockWithRenderType(ModBlocks.MOONPETAL_DOOR.get(),
                modLoc("block/moonpetal_door_bottom"), modLoc("block/moonpetal_door_top"), "cutout");
        trapdoorBlockWithRenderType(ModBlocks.MOONPETAL_TRAPDOOR.get(), modLoc("block/moonpetal_trapdoor"), true, "cutout");
        pressurePlateBlock(ModBlocks.MOONPETAL_PRESSURE_PLATE.get(), planks);
        buttonBlock(ModBlocks.MOONPETAL_BUTTON.get(), planks);
        // Fixed blossom colour in the texture (no tint), like the palm leaves.
        simpleBlock(ModBlocks.MOONPETAL_LEAVES.get(), models().cubeAll("moonpetal_leaves",
                blockTexture(ModBlocks.MOONPETAL_LEAVES.get())).renderType("cutout_mipped"));
        cross(ModBlocks.MOONPETAL_SAPLING.get());
        simpleBlock(ModBlocks.POTTED_MOONPETAL_SAPLING.get(), models().flowerPotCross("potted_moonpetal_sapling",
                blockTexture(ModBlocks.MOONPETAL_SAPLING.get())).renderType("cutout"));

        logBlock(ModBlocks.WITHERED_MOONPETAL_LOG.get());
        axisBlock(ModBlocks.WITHERED_MOONPETAL_WOOD.get(),
                blockTexture(ModBlocks.WITHERED_MOONPETAL_LOG.get()), blockTexture(ModBlocks.WITHERED_MOONPETAL_LOG.get()));
        logBlock(ModBlocks.STRIPPED_WITHERED_MOONPETAL_LOG.get());
        axisBlock(ModBlocks.STRIPPED_WITHERED_MOONPETAL_WOOD.get(), blockTexture(ModBlocks.STRIPPED_WITHERED_MOONPETAL_LOG.get()),
                blockTexture(ModBlocks.STRIPPED_WITHERED_MOONPETAL_LOG.get()));

        // The stem on every block, the plume on the top one; age and water do not change the model.
        ModelFile reedStem = crossModel("reeds", blockTexture(ModBlocks.REEDS.get()));
        ModelFile reedTop = crossModel("reeds_top", modLoc("block/reeds_top"));
        getVariantBuilder(ModBlocks.REEDS.get()).forAllStatesExcept(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(ReedsBlock.TOP) ? reedTop : reedStem).build(), ReedsBlock.AGE, ReedsBlock.WATERLOGGED);
        randomlyRotated(ModBlocks.PEAT.get());
        // The waterlogged property does not change the model.
        getVariantBuilder(ModBlocks.HANGING_SWAMP_MOSS.get()).forAllStatesExcept(state -> ConfiguredModel.builder()
                .modelFile(crossModel("hanging_swamp_moss", blockTexture(ModBlocks.HANGING_SWAMP_MOSS.get()))).build(),
                BlockStateProperties.WATERLOGGED);
        giantLilyPad();
        simpleBlock(ModBlocks.LOTUS.get(), flatPad("lotus", blockTexture(ModBlocks.LOTUS.get())));

        glowingCross(ModBlocks.GLOWCAP.get());
        glowingCross(ModBlocks.GLOW_BLOOM.get());
        glowingCross(ModBlocks.GLOWING_HANGING_ROOTS.get());
        glowingCross(ModBlocks.BLIGHTED_GLOWCAP.get());
        vent(ModBlocks.BUBBLE_SPRING.get());

        ring4Ores();
        ring4Gameplay();
        verdant();
        ring4Decor();
        lagoonSandAndClawMarks();
        lagoonSandstone();
    }

    /**
     * The lagoon sandstone set (66f), modelled like vanilla sandstone: the block, its stairs and slab on side/bottom/top,
     * the wall on the side; cut and chiseled are columns with the sandstone top as end; smooth is the top on all faces.
     */
    private void lagoonSandstone() {
        ResourceLocation top = modLoc("block/lagoon_sandstone_top");
        ResourceLocation side = modLoc("block/lagoon_sandstone");
        ResourceLocation bottom = modLoc("block/lagoon_sandstone_bottom");
        simpleBlock(ModBlocks.LAGOON_SANDSTONE.get(), models().cubeBottomTop("lagoon_sandstone", side, bottom, top));
        stairsBlock(ModBlocks.LAGOON_SANDSTONE_STAIRS.get(), side, bottom, top);
        slabBlock(ModBlocks.LAGOON_SANDSTONE_SLAB.get(), modLoc("block/lagoon_sandstone"), side, bottom, top);
        wallBlock(ModBlocks.LAGOON_SANDSTONE_WALL.get(), side);
        ResourceLocation cutSide = blockTexture(ModBlocks.CUT_LAGOON_SANDSTONE.get());
        simpleBlock(ModBlocks.CUT_LAGOON_SANDSTONE.get(), models().cubeColumn("cut_lagoon_sandstone", cutSide, top));
        slabBlock(ModBlocks.CUT_LAGOON_SANDSTONE_SLAB.get(), modLoc("block/cut_lagoon_sandstone"), cutSide, top, top);
        simpleBlock(ModBlocks.CHISELED_LAGOON_SANDSTONE.get(), models().cubeColumn("chiseled_lagoon_sandstone",
                blockTexture(ModBlocks.CHISELED_LAGOON_SANDSTONE.get()), top));
        simpleBlock(ModBlocks.SMOOTH_LAGOON_SANDSTONE.get(), models().cubeAll("smooth_lagoon_sandstone", top));
        stairsBlock(ModBlocks.SMOOTH_LAGOON_SANDSTONE_STAIRS.get(), top);
        slabBlock(ModBlocks.SMOOTH_LAGOON_SANDSTONE_SLAB.get(), modLoc("block/smooth_lagoon_sandstone"), top);
    }

    /** The golf 3 blocks (66c): plain cubes on T's textures (the clawed deepslate is no pillar, like clawed tidestone). */
    private void lagoonSandAndClawMarks() {
        simpleBlock(ModBlocks.LAGOON_SAND.get());
        simpleBlock(ModBlocks.CLAWED_STONE.get());
        simpleBlock(ModBlocks.CLAWED_DEEPSLATE.get());
    }

    /**
     * The decor rollout blocks: mossy tidestone as a stone family, the aquamarine stone as a cube with its emissive
     * {@code _glow} veins, the plants as crosses and the glowgrass with its glow layer (waterlogged or not, one model).
     */
    private void ring4Decor() {
        stoneFamily(ModBlocks.MOSSY_TIDESTONE.get(), ModBlocks.MOSSY_TIDESTONE_STAIRS.get(),
                ModBlocks.MOSSY_TIDESTONE_SLAB.get(), ModBlocks.MOSSY_TIDESTONE_WALL.get());
        glowingCube(ModBlocks.AQUAMARINE_STONE.get());
        cross(ModBlocks.FJORD_MOSS.get());
        cross(ModBlocks.MIST_FERN.get());
        cross(ModBlocks.MOONPETAL_SHRUB.get());
        glowingCross(ModBlocks.LAGOON_GLOWGRASS.get());
    }

    /**
     * The verdant flora (72b) on T's paths ({@code golf-2-textures.md}): the wood set laid out like the moonpetal set,
     * fixed-colour leaves; the plants as crosses, double plants on {@code _bottom}/{@code _top}, the glowing ones with
     * their emissive {@code _glow} layer, the wall plants as a plane on the wall, the vine per {@code flowering}, the
     * water bloom flat like the lily pad and the fruit pod on vanilla's cocoa models.
     */
    private void verdant() {
        logBlock(ModBlocks.VERDANT_LOG.get());
        axisBlock(ModBlocks.VERDANT_WOOD.get(), blockTexture(ModBlocks.VERDANT_LOG.get()), blockTexture(ModBlocks.VERDANT_LOG.get()));
        logBlock(ModBlocks.STRIPPED_VERDANT_LOG.get());
        axisBlock(ModBlocks.STRIPPED_VERDANT_WOOD.get(),
                blockTexture(ModBlocks.STRIPPED_VERDANT_LOG.get()), blockTexture(ModBlocks.STRIPPED_VERDANT_LOG.get()));
        ResourceLocation planks = blockTexture(ModBlocks.VERDANT_PLANKS.get());
        simpleBlock(ModBlocks.VERDANT_PLANKS.get());
        stairsBlock(ModBlocks.VERDANT_STAIRS.get(), planks);
        slabBlock(ModBlocks.VERDANT_SLAB.get(), planks, planks);
        fenceBlock(ModBlocks.VERDANT_FENCE.get(), planks);
        fenceGateBlock(ModBlocks.VERDANT_FENCE_GATE.get(), planks);
        doorBlockWithRenderType(ModBlocks.VERDANT_DOOR.get(),
                modLoc("block/verdant_door_bottom"), modLoc("block/verdant_door_top"), "cutout");
        trapdoorBlockWithRenderType(ModBlocks.VERDANT_TRAPDOOR.get(), modLoc("block/verdant_trapdoor"), true, "cutout");
        pressurePlateBlock(ModBlocks.VERDANT_PRESSURE_PLATE.get(), planks);
        buttonBlock(ModBlocks.VERDANT_BUTTON.get(), planks);
        simpleBlock(ModBlocks.VERDANT_LEAVES.get(), models().cubeAll("verdant_leaves",
                blockTexture(ModBlocks.VERDANT_LEAVES.get())).renderType("cutout_mipped"));
        cross(ModBlocks.VERDANT_SAPLING.get());
        simpleBlock(ModBlocks.POTTED_VERDANT_SAPLING.get(), models().flowerPotCross("potted_verdant_sapling",
                blockTexture(ModBlocks.VERDANT_SAPLING.get())).renderType("cutout"));

        doublePlant(ModBlocks.VERDANT_GIANT_LEAF.get());
        cross(ModBlocks.VERDANT_FERN.get());
        doublePlant(ModBlocks.VERDANT_TALL_FERN.get());
        glowingCross(ModBlocks.VERDANT_BROMELIAD.get());
        ResourceLocation bromeliad = blockTexture(ModBlocks.VERDANT_BROMELIAD.get());
        horizontalBlock(ModBlocks.VERDANT_WALL_BROMELIAD.get(), glowingWallPlant("verdant_wall_bromeliad", bromeliad), 180);
        horizontalBlock(ModBlocks.VERDANT_ORCHID.get(),
                glowingWallPlant("verdant_orchid", blockTexture(ModBlocks.VERDANT_ORCHID.get())), 180);
        // The waterlogged property does not change the model.
        getVariantBuilder(ModBlocks.VERDANT_HANGING_ORCHID.get()).forAllStatesExcept(state -> ConfiguredModel.builder()
                .modelFile(crossModel("verdant_hanging_orchid", blockTexture(ModBlocks.VERDANT_HANGING_ORCHID.get()))).build(),
                BlockStateProperties.WATERLOGGED);
        cross(ModBlocks.VERDANT_BLOSSOM_BUSH.get());
        // Age does not change the model; flowering picks the flowering texture.
        vine(ModBlocks.VERDANT_VINE.get(), "verdant_vine", GrowingPlantHeadBlock.AGE);
        vine(ModBlocks.VERDANT_VINE_PLANT.get(), "verdant_vine_plant");
        simpleBlock(ModBlocks.VERDANT_WATER_BLOOM.get(),
                flatPad("verdant_water_bloom", blockTexture(ModBlocks.VERDANT_WATER_BLOOM.get())));
        fruitPod();
    }

    /** A double plant: a cross per half on {@code <name>_bottom} and {@code <name>_top}. */
    private void doublePlant(Block block) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        ModelFile lower = crossModel(name + "_bottom", modLoc("block/" + name + "_bottom"));
        ModelFile upper = crossModel(name + "_top", modLoc("block/" + name + "_top"));
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER ? lower : upper).build());
    }

    /**
     * A plant on a wall, drawn for {@code facing=north} (the wall to the south) like vanilla's ladder: one double-sided
     * plane a pixel off the wall with the texture, and the same plane with the emissive {@code _glow} overlay.
     */
    private ModelFile glowingWallPlant(String name, ResourceLocation texture) {
        BlockModelBuilder model = models().getBuilder(name).renderType("cutout").ao(false)
                .texture("particle", texture).texture("plant", texture).texture("glow", texture.withSuffix("_glow"));
        wallPlane(model, "#plant", false);
        wallPlane(model, "#glow", true);
        return model;
    }

    private static void wallPlane(BlockModelBuilder model, String texture, boolean emissive) {
        var plane = model.element().from(0, 0, 15).to(16, 16, 15).shade(false);
        if (emissive) {
            plane.emissivity(15, 15).ao(false);
        }
        plane.face(Direction.NORTH).uvs(0, 0, 16, 16).texture(texture).end()
                .face(Direction.SOUTH).uvs(16, 0, 0, 16).texture(texture).end().end();
    }

    /** A vine block: a cross on {@code <name>} or, flowering, on {@code <name>_flowering}; {@code ignored} properties do not count. */
    private void vine(Block block, String name, Property<?>... ignored) {
        ModelFile plain = crossModel(name, modLoc("block/" + name));
        ModelFile flowering = crossModel(name + "_flowering", modLoc("block/" + name + "_flowering"));
        getVariantBuilder(block).forAllStatesExcept(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(VerdantVineBlock.FLOWERING) ? flowering : plain).build(), ignored);
    }

    /** The fruit pod on vanilla's cocoa models (T's cocoa UV layout), rotated like vanilla's cocoa blockstate. */
    private void fruitPod() {
        List<ModelFile> stages = new ArrayList<>();
        for (int age = 0; age <= 2; age++) {
            String name = "verdant_fruit_pod_stage" + age;
            stages.add(models().withExistingParent(name, mcLoc("block/cocoa_stage" + age))
                    .texture("cocoa", modLoc("block/" + name)).texture("particle", modLoc("block/" + name)));
        }
        getVariantBuilder(ModBlocks.VERDANT_FRUIT_POD.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(stages.get(state.getValue(CocoaBlock.AGE)))
                .rotationY((int) state.getValue(CocoaBlock.FACING).toYRot()).build());
    }

    /**
     * The gameplay blocks of package 72 (texture paths confirmed with T, backlog r. 277): the invisible gas on vanilla's
     * empty model, the mud and the nest as cubes, the toxic pool as a liquid (only its particle; the fluid renderer
     * draws it through the client extension), the updraft as a see-through column of four sides, the clawed tidestone
     * as a cube and the slime trail as a flat decal along its axis.
     */
    private void ring4Gameplay() {
        simpleBlock(ModBlocks.SWAMP_GAS.get(), models().getExistingFile(mcLoc("block/air")));
        simpleBlock(ModBlocks.SUCKING_MUD.get());
        simpleBlock(ModBlocks.TOXIC_POOL.get(), models().getBuilder("toxic_pool")
                .texture("particle", modLoc("block/toxic_pool_still")));
        updraft();
        simpleBlock(ModBlocks.CLAWED_TIDESTONE.get());
        ModelFile trail = flatPad("slime_trail", blockTexture(ModBlocks.SLIME_TRAIL.get()));
        getVariantBuilder(ModBlocks.SLIME_TRAIL.get()).forAllStates(state -> ConfiguredModel.builder().modelFile(trail)
                .rotationY(state.getValue(SlimeTrailBlock.AXIS) == Direction.Axis.X ? 0 : 90).build());
        simpleBlock(ModBlocks.CREATURE_NEST.get());
    }

    /**
     * The updraft: a full cube with only its four sides, each drawn from both sides (you see the streaks around you
     * when you stand in it), cutout; faces towards another updraft block are skipped by the block itself.
     */
    private void updraft() {
        ResourceLocation texture = blockTexture(ModBlocks.UPDRAFT.get());
        BlockModelBuilder model = models().getBuilder("updraft").renderType("cutout").ao(false)
                .texture("particle", texture).texture("updraft", texture);
        model.element().from(0, 0, 0).to(16, 16, 16).allFacesExcept((direction, face) -> face.texture("#updraft")
                .cullface(direction), Set.of(Direction.UP, Direction.DOWN)).end();
        // The same sides seen from inside: four flat planes on the sides, each facing inwards (no cull face).
        model.element().from(0, 0, 0).to(16, 16, 0).face(Direction.SOUTH).texture("#updraft").end().end();
        model.element().from(0, 0, 16).to(16, 16, 16).face(Direction.NORTH).texture("#updraft").end().end();
        model.element().from(0, 0, 0).to(0, 16, 16).face(Direction.EAST).texture("#updraft").end().end();
        model.element().from(16, 0, 0).to(16, 16, 16).face(Direction.WEST).texture("#updraft").end().end();
        simpleBlock(ModBlocks.UPDRAFT.get(), model);
    }

    /**
     * Ring 4 ores over stone, tidestone and deepslate (column texture), one overlay per ore shared by all hosts; only
     * tidesilver glows. The crystals in the amethyst pattern: wall and budding block as cubes, the stages glowing.
     */
    private void ring4Ores() {
        CubeTextures stone = CubeTextures.all(mcLoc("block/stone"));
        CubeTextures tidestone = CubeTextures.all(blockTexture(ModBlocks.TIDESTONE.get()));
        CubeTextures deepslate = new CubeTextures(mcLoc("block/deepslate_top"), mcLoc("block/deepslate"), mcLoc("block/deepslate_top"));
        CubeTextures tidesilver = CubeTextures.all(modLoc("block/tidesilver_ore_overlay"));
        CubeTextures jade = CubeTextures.all(modLoc("block/jade_ore_overlay"));
        oreWithOverlay(ModBlocks.TIDESILVER_ORE.get(), stone.side(), stone, tidesilver, true);
        oreWithOverlay(ModBlocks.TIDESTONE_TIDESILVER_ORE.get(), tidestone.side(), tidestone, tidesilver, true);
        oreWithOverlay(ModBlocks.DEEPSLATE_TIDESILVER_ORE.get(), deepslate.side(), deepslate, tidesilver, true);
        oreWithOverlay(ModBlocks.JADE_ORE.get(), stone.side(), stone, jade, false);
        oreWithOverlay(ModBlocks.TIDESTONE_JADE_ORE.get(), tidestone.side(), tidestone, jade, false);
        oreWithOverlay(ModBlocks.DEEPSLATE_JADE_ORE.get(), deepslate.side(), deepslate, jade, false);

        for (Block block : List.of(ModBlocks.TOXIC_CRYSTAL_BLOCK.get(), ModBlocks.BUDDING_TOXIC_CRYSTAL.get(),
                ModBlocks.AQUAMARINE_BLOCK.get(), ModBlocks.BUDDING_AQUAMARINE.get())) {
            simpleBlock(block);
        }
        // The toxic crystal pulses and glows evenly (like the fire crystal); aquamarine is a still, matte gem (T, 71a).
        for (Block stage : List.of(ModBlocks.SMALL_TOXIC_CRYSTAL_BUD.get(), ModBlocks.MEDIUM_TOXIC_CRYSTAL_BUD.get(),
                ModBlocks.LARGE_TOXIC_CRYSTAL_BUD.get(), ModBlocks.TOXIC_CRYSTAL_CLUSTER.get())) {
            crystalCluster(stage, true);
        }
        for (Block stage : List.of(ModBlocks.SMALL_AQUAMARINE_BUD.get(), ModBlocks.MEDIUM_AQUAMARINE_BUD.get(),
                ModBlocks.LARGE_AQUAMARINE_BUD.get(), ModBlocks.AQUAMARINE_CLUSTER.get())) {
            crystalCluster(stage, false);
        }
    }

    /**
     * One flat model per part on its own quarter texture {@code giant_lily_pad_<part>} (seen from above, north up), at
     * vanilla's lily pad height. Fixed colour in the texture, no tint.
     */
    private void giantLilyPad() {
        getVariantBuilder(ModBlocks.GIANT_LILY_PAD.get()).forAllStates(state -> {
            String name = "giant_lily_pad_" + state.getValue(GiantLilyPadBlock.PART).getSerializedName();
            return ConfiguredModel.builder().modelFile(flatPad(name, modLoc("block/" + name))).build();
        });
    }

    /** A flat cutout plane on the water like vanilla's lily pad (same height and UVs), fixed colour, no tint. */
    private ModelFile flatPad(String name, ResourceLocation texture) {
        BlockModelBuilder model = models().getBuilder(name).renderType("cutout").ao(false)
                .texture("particle", texture).texture("texture", texture);
        model.element().from(0, 0.25f, 0).to(16, 0.25f, 16)
                .face(Direction.UP).uvs(0, 0, 16, 16).texture("#texture").end()
                .face(Direction.DOWN).uvs(0, 16, 16, 0).texture("#texture").end().end();
        return model;
    }

    /**
     * A plant that glows in the dark (contract §7: two layers): vanilla's cross with the texture {@code <name>}, lit
     * normally, plus the same two planes with the emissive cutout overlay {@code <name>_glow}. Properties such as
     * waterlogged do not change the model.
     */
    private void glowingCross(Block block) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        ResourceLocation base = blockTexture(block);
        BlockModelBuilder model = models().getBuilder(name).renderType("cutout").ao(false)
                .texture("particle", base).texture("cross", base).texture("glow", base.withSuffix("_glow"));
        crossPlanes(model, "#cross", false);
        crossPlanes(model, "#glow", true);
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder().modelFile(model).build());
    }

    /** Nether ring 3: cold lava, Nether ice, rime netherrack, the rimeshroom set (laid out like the dripshroom set). */
    private void netherRing3() {
        stoneFamily(ModBlocks.COLD_LAVA.get(), ModBlocks.COLD_LAVA_STAIRS.get(), ModBlocks.COLD_LAVA_SLAB.get(),
                ModBlocks.COLD_LAVA_WALL.get());
        stoneFamily(ModBlocks.POLISHED_COLD_LAVA.get(), ModBlocks.POLISHED_COLD_LAVA_STAIRS.get(),
                ModBlocks.POLISHED_COLD_LAVA_SLAB.get(), ModBlocks.POLISHED_COLD_LAVA_WALL.get());
        stoneFamily(ModBlocks.COLD_LAVA_BRICKS.get(), ModBlocks.COLD_LAVA_BRICK_STAIRS.get(),
                ModBlocks.COLD_LAVA_BRICK_SLAB.get(), ModBlocks.COLD_LAVA_BRICK_WALL.get());
        // Opaque, solid render type (decision 2026-10-06), unlike vanilla's translucent ice.
        simpleBlock(ModBlocks.NETHER_ICE.get());
        simpleBlock(ModBlocks.PACKED_NETHER_ICE.get());
        simpleBlock(ModBlocks.RIME_NETHERRACK.get());

        logBlock(ModBlocks.RIMESHROOM_STEM.get());
        axisBlock(ModBlocks.RIMESHROOM_HYPHAE.get(),
                blockTexture(ModBlocks.RIMESHROOM_STEM.get()), blockTexture(ModBlocks.RIMESHROOM_STEM.get()));
        logBlock(ModBlocks.STRIPPED_RIMESHROOM_STEM.get());
        axisBlock(ModBlocks.STRIPPED_RIMESHROOM_HYPHAE.get(),
                blockTexture(ModBlocks.STRIPPED_RIMESHROOM_STEM.get()), blockTexture(ModBlocks.STRIPPED_RIMESHROOM_STEM.get()));
        ResourceLocation planks = blockTexture(ModBlocks.RIMESHROOM_PLANKS.get());
        simpleBlock(ModBlocks.RIMESHROOM_PLANKS.get());
        stairsBlock(ModBlocks.RIMESHROOM_STAIRS.get(), planks);
        slabBlock(ModBlocks.RIMESHROOM_SLAB.get(), planks, planks);
        fenceBlock(ModBlocks.RIMESHROOM_FENCE.get(), planks);
        fenceGateBlock(ModBlocks.RIMESHROOM_FENCE_GATE.get(), planks);
        doorBlockWithRenderType(ModBlocks.RIMESHROOM_DOOR.get(),
                modLoc("block/rimeshroom_door_bottom"), modLoc("block/rimeshroom_door_top"), "cutout");
        trapdoorBlockWithRenderType(ModBlocks.RIMESHROOM_TRAPDOOR.get(), modLoc("block/rimeshroom_trapdoor"), true, "cutout");
        pressurePlateBlock(ModBlocks.RIMESHROOM_PRESSURE_PLATE.get(), planks);
        buttonBlock(ModBlocks.RIMESHROOM_BUTTON.get(), planks);
        simpleBlock(ModBlocks.RIMESHROOM_CAP.get());

        stalactite(ModBlocks.ICE_STALACTITE.get());
        rotatedGlowingCube(ModBlocks.GLOWING_FISSURE.get());
        simpleBlock(ModBlocks.PRIMORDIAL_ICE.get());
    }

    /** Ring 3: ash, pumice, smouldering stone and the charred set (laid out like the dripshroom set). */
    private void ring3() {
        ModelFile ash = cubeAll(ModBlocks.ASH_BLOCK.get());
        simpleBlock(ModBlocks.ASH_BLOCK.get(), ash);
        ashLayer(ash);
        simpleBlock(ModBlocks.ASH_CRUST.get());
        glowingCube(ModBlocks.SMOLDERING_ASH.get());
        stoneFamily(ModBlocks.PUMICE.get(), ModBlocks.PUMICE_STAIRS.get(), ModBlocks.PUMICE_SLAB.get(), ModBlocks.PUMICE_WALL.get());
        stoneFamily(ModBlocks.POLISHED_PUMICE.get(), ModBlocks.POLISHED_PUMICE_STAIRS.get(),
                ModBlocks.POLISHED_PUMICE_SLAB.get(), ModBlocks.POLISHED_PUMICE_WALL.get());
        stoneFamily(ModBlocks.PUMICE_BRICKS.get(), ModBlocks.PUMICE_BRICK_STAIRS.get(),
                ModBlocks.PUMICE_BRICK_SLAB.get(), ModBlocks.PUMICE_BRICK_WALL.get());
        rotatedGlowingCube(ModBlocks.SMOLDERING_STONE.get());

        logBlock(ModBlocks.CHARRED_LOG.get());
        axisBlock(ModBlocks.CHARRED_WOOD.get(), blockTexture(ModBlocks.CHARRED_LOG.get()), blockTexture(ModBlocks.CHARRED_LOG.get()));
        logBlock(ModBlocks.STRIPPED_CHARRED_LOG.get());
        axisBlock(ModBlocks.STRIPPED_CHARRED_WOOD.get(),
                blockTexture(ModBlocks.STRIPPED_CHARRED_LOG.get()), blockTexture(ModBlocks.STRIPPED_CHARRED_LOG.get()));
        ResourceLocation planks = blockTexture(ModBlocks.CHARRED_PLANKS.get());
        simpleBlock(ModBlocks.CHARRED_PLANKS.get());
        stairsBlock(ModBlocks.CHARRED_STAIRS.get(), planks);
        slabBlock(ModBlocks.CHARRED_SLAB.get(), planks, planks);
        fenceBlock(ModBlocks.CHARRED_FENCE.get(), planks);
        fenceGateBlock(ModBlocks.CHARRED_FENCE_GATE.get(), planks);
        doorBlockWithRenderType(ModBlocks.CHARRED_DOOR.get(),
                modLoc("block/charred_door_bottom"), modLoc("block/charred_door_top"), "cutout");
        trapdoorBlockWithRenderType(ModBlocks.CHARRED_TRAPDOOR.get(), modLoc("block/charred_trapdoor"), true, "cutout");
        pressurePlateBlock(ModBlocks.CHARRED_PRESSURE_PLATE.get(), planks);
        buttonBlock(ModBlocks.CHARRED_BUTTON.get(), planks);

        // Fixed colour (no tint); the glowing specks are the emissive overlay.
        glowingCube(ModBlocks.EMBER_LEAVES.get(), "cutout_mipped");
        cross(ModBlocks.FIRE_BLOOM.get());
        simpleBlock(ModBlocks.POTTED_FIRE_BLOOM.get(), models().flowerPotCross("potted_fire_bloom",
                blockTexture(ModBlocks.FIRE_BLOOM.get())).renderType("cutout"));
        cross(ModBlocks.ASH_SPROUTS.get());
        // Like vanilla's sweet berry bush: one cross model per age.
        getVariantBuilder(ModBlocks.EMBER_BERRY_BUSH.get()).forAllStates(state -> {
            String name = "ember_berry_bush_stage" + state.getValue(SweetBerryBushBlock.AGE);
            return ConfiguredModel.builder().modelFile(crossModel(name, modLoc("block/" + name))).build();
        });

        ring3Ores();

        // Sulfur fields and steam fens.
        simpleBlock(ModBlocks.SULFUR_CRUST.get());
        vent(ModBlocks.FUMAROLE.get());
        simpleBlock(ModBlocks.BOILING_MUD.get());
        vent(ModBlocks.GEYSER.get());
        simpleBlock(ModBlocks.YELLOW_SULFUR_MUD.get());
        simpleBlock(ModBlocks.GREEN_SULFUR_MUD.get());
        simpleBlock(ModBlocks.ORANGE_SULFUR_MUD.get());
        crystalCluster(ModBlocks.SMALL_SULFUR_CRYSTAL_BUD.get(), false);
        crystalCluster(ModBlocks.MEDIUM_SULFUR_CRYSTAL_BUD.get(), false);
        crystalCluster(ModBlocks.LARGE_SULFUR_CRYSTAL_BUD.get(), false);
        crystalCluster(ModBlocks.SULFUR_CRYSTAL.get(), false);
    }

    /**
     * Ring 3 ores over vanilla stone, deepslate (column texture) and basalt (column texture), one overlay per ore shared
     * by all hosts. Only the fire ruby glows; volcanite and sulfur are matte.
     */
    private void ring3Ores() {
        CubeTextures stone = CubeTextures.all(mcLoc("block/stone"));
        CubeTextures deepslate = new CubeTextures(mcLoc("block/deepslate_top"), mcLoc("block/deepslate"), mcLoc("block/deepslate_top"));
        CubeTextures basalt = new CubeTextures(mcLoc("block/basalt_top"), mcLoc("block/basalt_side"), mcLoc("block/basalt_top"));
        CubeTextures volcanite = CubeTextures.all(modLoc("block/volcanite_ore_overlay"));
        CubeTextures sulfur = CubeTextures.all(modLoc("block/sulfur_ore_overlay"));
        CubeTextures fireRuby = CubeTextures.all(modLoc("block/fire_ruby_ore_overlay"));
        oreWithOverlay(ModBlocks.VOLCANITE_ORE.get(), stone.side(), stone, volcanite, false);
        oreWithOverlay(ModBlocks.DEEPSLATE_VOLCANITE_ORE.get(), deepslate.side(), deepslate, volcanite, false);
        oreWithOverlay(ModBlocks.BASALT_VOLCANITE_ORE.get(), basalt.side(), basalt, volcanite, false);
        oreWithOverlay(ModBlocks.SULFUR_ORE.get(), stone.side(), stone, sulfur, false);
        oreWithOverlay(ModBlocks.BASALT_SULFUR_ORE.get(), basalt.side(), basalt, sulfur, false);
        oreWithOverlay(ModBlocks.FIRE_RUBY_ORE.get(), stone.side(), stone, fireRuby, true);
        oreWithOverlay(ModBlocks.DEEPSLATE_FIRE_RUBY_ORE.get(), deepslate.side(), deepslate, fireRuby, true);
        oreWithOverlay(ModBlocks.BASALT_FIRE_RUBY_ORE.get(), basalt.side(), basalt, fireRuby, true);

        // Fire crystal: the wall is a plain cube; the stages glow like the dew crystal stages.
        simpleBlock(ModBlocks.FIRE_CRYSTAL_BLOCK.get());
        crystalCluster(ModBlocks.SMALL_FIRE_CRYSTAL_BUD.get(), true);
        crystalCluster(ModBlocks.MEDIUM_FIRE_CRYSTAL_BUD.get(), true);
        crystalCluster(ModBlocks.LARGE_FIRE_CRYSTAL_BUD.get(), true);
        crystalCluster(ModBlocks.FIRE_CRYSTAL_CLUSTER.get(), true);
    }

    /** A vent: its opening {@code <name>_top} on top, the rock {@code <name>_side} on the sides and the bottom. */
    private void vent(Block block) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        ResourceLocation side = modLoc("block/" + name + "_side");
        simpleBlock(block, models().cubeBottomTop(name, side, side, modLoc("block/" + name + "_top")));
    }

    /**
     * A block whose glowing parts glow in the dark (decision 2026-10-06: two layers): the base texture {@code <name>}
     * lit normally, with the emissive cutout overlay {@code <name>_glow} (only the glowing pixels) on top, the ore model.
     */
    private void glowingCube(Block block) {
        glowingCube(block, "cutout");
    }

    private void glowingCube(Block block, String renderType) {
        ResourceLocation base = blockTexture(block);
        overlayCube(block, base, CubeTextures.all(base), CubeTextures.all(base.withSuffix("_glow")), true, renderType);
    }

    /**
     * {@link #glowingCube} in 32 orientations (package 46b): netherrack's 16 x/y rotations of the model and of a
     * mirrored copy {@code <name>_mirrored} (like stone's {@code cube_mirrored_all}), so every face shows the texture in
     * all 8 orientations and a floor patch shows no tile grid. A variant rotates the whole model, so the emissive layer
     * stays on its pixels.
     */
    private void rotatedGlowingCube(Block block) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        ResourceLocation base = blockTexture(block);
        CubeTextures glow = CubeTextures.all(base.withSuffix("_glow"));
        List<ConfiguredModel> variants = new ArrayList<>();
        for (ModelFile model : List.of(
                overlayModel(name, base, CubeTextures.all(base), glow, true, "cutout", false),
                overlayModel(name + "_mirrored", base, CubeTextures.all(base), glow, true, "cutout", true))) {
            for (int x = 0; x < 360; x += 90) {
                for (int y = 0; y < 360; y += 90) {
                    variants.add(new ConfiguredModel(model, x, y, false));
                }
            }
        }
        simpleBlock(block, variants.toArray(ConfiguredModel[]::new));
    }

    /**
     * Like vanilla's snow layer: layers 1–7 reuse vanilla's {@code snow_height2..14} shapes with the ash texture, 8 layers
     * is the ash block's model. The item uses the one-layer model.
     */
    private void ashLayer(ModelFile full) {
        ResourceLocation ash = blockTexture(ModBlocks.ASH_BLOCK.get());
        getVariantBuilder(ModBlocks.ASH_LAYER.get()).forAllStates(state -> {
            int layers = state.getValue(SnowLayerBlock.LAYERS);
            ModelFile model = layers == SnowLayerBlock.MAX_HEIGHT ? full
                    : models().withExistingParent("ash_layer_height" + 2 * layers, mcLoc("block/snow_height" + 2 * layers))
                            .texture("particle", ash).texture("texture", ash);
            return ConfiguredModel.builder().modelFile(model).build();
        });
    }

    private void ring2() {
        simpleBlock(ModBlocks.RED_CANYON_STONE.get());
        simpleBlock(ModBlocks.ORANGE_CANYON_STONE.get());
        simpleBlock(ModBlocks.CREAM_CANYON_STONE.get());
        stoneFamily(ModBlocks.CANYON_COBBLESTONE.get(), ModBlocks.CANYON_COBBLESTONE_STAIRS.get(),
                ModBlocks.CANYON_COBBLESTONE_SLAB.get(), ModBlocks.CANYON_COBBLESTONE_WALL.get());
        stoneFamily(ModBlocks.CANYON_STONE_BRICKS.get(), ModBlocks.CANYON_STONE_BRICK_STAIRS.get(),
                ModBlocks.CANYON_STONE_BRICK_SLAB.get(), ModBlocks.CANYON_STONE_BRICK_WALL.get());

        // Scorched sandstone in vanilla sandstone's layout: cut and chiseled are columns, smooth is the top texture.
        ResourceLocation top = modLoc("block/scorched_sandstone_top");
        ResourceLocation side = modLoc("block/scorched_sandstone");
        ResourceLocation bottom = modLoc("block/scorched_sandstone_bottom");
        simpleBlock(ModBlocks.SCORCHED_SANDSTONE.get(), models().cubeBottomTop("scorched_sandstone", side, bottom, top));
        stairsBlock(ModBlocks.SCORCHED_SANDSTONE_STAIRS.get(), side, bottom, top);
        slabBlock(ModBlocks.SCORCHED_SANDSTONE_SLAB.get(), modLoc("block/scorched_sandstone"), side, bottom, top);
        wallBlock(ModBlocks.SCORCHED_SANDSTONE_WALL.get(), side);
        ResourceLocation cutSide = blockTexture(ModBlocks.CUT_SCORCHED_SANDSTONE.get());
        simpleBlock(ModBlocks.CUT_SCORCHED_SANDSTONE.get(), models().cubeColumn("cut_scorched_sandstone", cutSide, top));
        slabBlock(ModBlocks.CUT_SCORCHED_SANDSTONE_SLAB.get(), modLoc("block/cut_scorched_sandstone"), cutSide, top, top);
        simpleBlock(ModBlocks.CHISELED_SCORCHED_SANDSTONE.get(), models().cubeColumn("chiseled_scorched_sandstone",
                blockTexture(ModBlocks.CHISELED_SCORCHED_SANDSTONE.get()), top));
        simpleBlock(ModBlocks.SMOOTH_SCORCHED_SANDSTONE.get(), models().cubeAll("smooth_scorched_sandstone", top));
        stairsBlock(ModBlocks.SMOOTH_SCORCHED_SANDSTONE_STAIRS.get(), top);
        slabBlock(ModBlocks.SMOOTH_SCORCHED_SANDSTONE_SLAB.get(), modLoc("block/smooth_scorched_sandstone"), top);

        simpleBlock(ModBlocks.LOOSE_SAND.get());

        randomlyRotated(ModBlocks.CRACKED_CLAY.get());
        cross(ModBlocks.DRY_SHRUB.get());
        barrelCactus();
        cross(ModBlocks.DESERT_FLOWER.get());
        simpleBlock(ModBlocks.POTTED_DRY_SHRUB.get(), models().flowerPotCross("potted_dry_shrub",
                blockTexture(ModBlocks.DRY_SHRUB.get())).renderType("cutout"));
        simpleBlock(ModBlocks.POTTED_DESERT_FLOWER.get(), models().flowerPotCross("potted_desert_flower",
                blockTexture(ModBlocks.DESERT_FLOWER.get())).renderType("cutout"));
        pottedBarrelCactus();

        palm();
        ring2Ores();
        salt();

        netherRing2();
    }

    /** Nether ring 2: soaked netherrack with spore moss, the dripshroom set (laid out like the palm set), growths. */
    private void netherRing2() {
        simpleBlock(ModBlocks.SOAKED_NETHERRACK.get());
        // Like nylium: moss on top and as a rim on the sides, the bottom is the ground it grows on.
        simpleBlock(ModBlocks.SPORE_MOSS.get(), models().cubeBottomTop("spore_moss", modLoc("block/spore_moss_side"),
                blockTexture(ModBlocks.SOAKED_NETHERRACK.get()), modLoc("block/spore_moss")));

        logBlock(ModBlocks.DRIPSHROOM_STEM.get());
        axisBlock(ModBlocks.DRIPSHROOM_HYPHAE.get(),
                blockTexture(ModBlocks.DRIPSHROOM_STEM.get()), blockTexture(ModBlocks.DRIPSHROOM_STEM.get()));
        logBlock(ModBlocks.STRIPPED_DRIPSHROOM_STEM.get());
        axisBlock(ModBlocks.STRIPPED_DRIPSHROOM_HYPHAE.get(),
                blockTexture(ModBlocks.STRIPPED_DRIPSHROOM_STEM.get()), blockTexture(ModBlocks.STRIPPED_DRIPSHROOM_STEM.get()));

        ResourceLocation planks = blockTexture(ModBlocks.DRIPSHROOM_PLANKS.get());
        simpleBlock(ModBlocks.DRIPSHROOM_PLANKS.get());
        stairsBlock(ModBlocks.DRIPSHROOM_STAIRS.get(), planks);
        slabBlock(ModBlocks.DRIPSHROOM_SLAB.get(), planks, planks);
        fenceBlock(ModBlocks.DRIPSHROOM_FENCE.get(), planks);
        fenceGateBlock(ModBlocks.DRIPSHROOM_FENCE_GATE.get(), planks);
        doorBlockWithRenderType(ModBlocks.DRIPSHROOM_DOOR.get(),
                modLoc("block/dripshroom_door_bottom"), modLoc("block/dripshroom_door_top"), "cutout");
        trapdoorBlockWithRenderType(ModBlocks.DRIPSHROOM_TRAPDOOR.get(), modLoc("block/dripshroom_trapdoor"), true, "cutout");
        pressurePlateBlock(ModBlocks.DRIPSHROOM_PRESSURE_PLATE.get(), planks);
        buttonBlock(ModBlocks.DRIPSHROOM_BUTTON.get(), planks);

        simpleBlock(ModBlocks.DRIPSHROOM_CAP.get());
        simpleBlock(ModBlocks.DRIPSHROOM_LAMP.get());
        glowMold();
        stalactite(ModBlocks.SLIME_STALACTITE.get());
        cross(ModBlocks.SPORE_SPROUTS.get());
        simpleBlock(ModBlocks.DEW_SOURCE.get());
        crystalCluster(ModBlocks.SMALL_DEW_CRYSTAL_BUD.get(), true);
        crystalCluster(ModBlocks.MEDIUM_DEW_CRYSTAL_BUD.get(), true);
        crystalCluster(ModBlocks.LARGE_DEW_CRYSTAL_BUD.get(), true);
        crystalCluster(ModBlocks.DEW_CRYSTAL_CLUSTER.get(), true);
    }

    /**
     * Vanilla's glow lichen model (full-bright, decision 37-4 A: the mold glows in the texture, not only through its light): one double-sided plane just inside the north face, rotated per face exactly like
     * vanilla's multipart blockstate (a block without faces is never shown, so vanilla's all-false fallback is left out).
     */
    private void glowMold() {
        ResourceLocation texture = blockTexture(ModBlocks.GLOW_MOLD.get());
        BlockModelBuilder model = models().getBuilder("glow_mold").renderType("cutout").ao(false)
                .texture("particle", texture).texture("glow_mold", texture);
        model.element().from(0, 0, 0.1f).to(16, 16, 0.1f).emissivity(15, 15).ao(false)
                .face(Direction.NORTH).uvs(16, 0, 0, 16).texture("#glow_mold").end()
                .face(Direction.SOUTH).uvs(0, 0, 16, 16).texture("#glow_mold").end().end();
        MultiPartBlockStateBuilder states = getMultipartBuilder(ModBlocks.GLOW_MOLD.get());
        glowMoldFace(states, model, MultifaceBlock.getFaceProperty(Direction.NORTH), 0, 0);
        glowMoldFace(states, model, MultifaceBlock.getFaceProperty(Direction.EAST), 0, 90);
        glowMoldFace(states, model, MultifaceBlock.getFaceProperty(Direction.SOUTH), 0, 180);
        glowMoldFace(states, model, MultifaceBlock.getFaceProperty(Direction.WEST), 0, 270);
        glowMoldFace(states, model, MultifaceBlock.getFaceProperty(Direction.UP), 270, 0);
        glowMoldFace(states, model, MultifaceBlock.getFaceProperty(Direction.DOWN), 90, 0);
    }

    private static void glowMoldFace(MultiPartBlockStateBuilder states, ModelFile model, BooleanProperty face, int x, int y) {
        states.part().modelFile(model).rotationX(x).rotationY(y).uvLock(true).addModel().condition(face, true).end();
    }

    /**
     * Ten cutout models like vanilla's pointed dripstone (one per direction and thickness, same parent), each on its own
     * texture {@code <name>_<direction>_<thickness>}; the waterlogged property does not change the model. Slime and ice.
     */
    private void stalactite(StalactiteBlock block) {
        String blockName = BuiltInRegistries.BLOCK.getKey(block).getPath();
        getVariantBuilder(block).forAllStatesExcept(state -> {
            String direction = state.getValue(StalactiteBlock.TIP_DIRECTION).getSerializedName();
            String thickness = state.getValue(StalactiteBlock.THICKNESS).getSerializedName();
            String name = blockName + "_" + direction + "_" + thickness;
            ModelFile model = models().withExistingParent(name, mcLoc("block/pointed_dripstone"))
                    .texture("cross", modLoc("block/" + name)).renderType("cutout");
            return ConfiguredModel.builder().modelFile(model).build();
        }, StalactiteBlock.WATERLOGGED);
    }

    /** The salt set: a crust over salt block (its bottom is the salt block), polished salt with its shapes, a crystal. */
    private void salt() {
        ResourceLocation saltBlock = blockTexture(ModBlocks.SALT_BLOCK.get());
        simpleBlock(ModBlocks.SALT_CRUST.get(), models().cubeBottomTop("salt_crust",
                modLoc("block/salt_crust_side"), saltBlock, modLoc("block/salt_crust_top")));
        simpleBlock(ModBlocks.SALT_BLOCK.get());
        ResourceLocation polished = blockTexture(ModBlocks.POLISHED_SALT.get());
        simpleBlock(ModBlocks.POLISHED_SALT.get());
        stairsBlock(ModBlocks.POLISHED_SALT_STAIRS.get(), polished);
        slabBlock(ModBlocks.POLISHED_SALT_SLAB.get(), polished, polished);
        wallBlock(ModBlocks.POLISHED_SALT_WALL.get(), polished);
        crystalCluster(ModBlocks.SALT_CRYSTAL.get(), false);
    }

    /**
     * A crystal on any face, like vanilla's amethyst cluster: one cutout cross model, rotated per {@code facing} exactly
     * as vanilla's blockstate (the same model when waterlogged), and a flat item with the block texture on vanilla's
     * amethyst bud transforms. Shared by every crystal block (salt crystal, the dew and fire crystal stages). An {@code emissive}
     * crystal glows evenly in the dark (NeoForge {@code neoforge_data}, rendering only); that needs its own elements, a
     * copy of vanilla's {@code block/cross}, because the vanilla parent allows no extra data on its elements.
     */
    private void crystalCluster(Block block, boolean emissive) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        ResourceLocation texture = blockTexture(block);
        directionalBlock(block, emissive ? emissiveCross(name, texture) : models().cross(name, texture).renderType("cutout"));
        itemModels().withExistingParent(name, mcLoc("item/amethyst_bud")).texture("layer0", texture);
    }

    /** Vanilla's {@code block/cross} (two diagonal planes, no shading) with full-bright elements and no ambient occlusion. */
    private BlockModelBuilder emissiveCross(String name, ResourceLocation texture) {
        BlockModelBuilder model = models().getBuilder(name).renderType("cutout").ao(false)
                .texture("particle", texture).texture("cross", texture);
        crossPlanes(model, "#cross", true);
        return model;
    }

    /** Adds vanilla's two diagonal cross planes on {@code texture}; {@code emissive} planes are full-bright. */
    private static void crossPlanes(BlockModelBuilder model, String texture, boolean emissive) {
        var first = model.element().from(0.8f, 0, 8).to(15.2f, 16, 8)
                .rotation().origin(8, 8, 8).axis(Direction.Axis.Y).angle(45).rescale(true).end().shade(false);
        if (emissive) {
            first.emissivity(15, 15).ao(false);
        }
        first.face(Direction.NORTH).uvs(0, 0, 16, 16).texture(texture).end()
                .face(Direction.SOUTH).uvs(0, 0, 16, 16).texture(texture).end().end();
        var second = model.element().from(8, 0, 0.8f).to(8, 16, 15.2f)
                .rotation().origin(8, 8, 8).axis(Direction.Axis.Y).angle(45).rescale(true).end().shade(false);
        if (emissive) {
            second.emissivity(15, 15).ao(false);
        }
        second.face(Direction.WEST).uvs(0, 0, 16, 16).texture(texture).end()
                .face(Direction.EAST).uvs(0, 0, 16, 16).texture(texture).end().end();
    }

    /**
     * Ring 2 ores: per host rock its own texture as the base, with one overlay per ore shared by all host rocks. Only
     * sunstone glows; turquoise is a matte gem.
     */
    private void ring2Ores() {
        CubeTextures scorchedSandstone = new CubeTextures(modLoc("block/scorched_sandstone_top"),
                modLoc("block/scorched_sandstone"), modLoc("block/scorched_sandstone_bottom"));
        CubeTextures sunstone = CubeTextures.all(modLoc("block/sunstone_ore_overlay"));
        CubeTextures turquoise = CubeTextures.all(modLoc("block/turquoise_ore_overlay"));
        canyonOre(ModBlocks.RED_CANYON_SUNSTONE_ORE.get(), ModBlocks.RED_CANYON_STONE.get(), sunstone, true);
        canyonOre(ModBlocks.ORANGE_CANYON_SUNSTONE_ORE.get(), ModBlocks.ORANGE_CANYON_STONE.get(), sunstone, true);
        canyonOre(ModBlocks.CREAM_CANYON_SUNSTONE_ORE.get(), ModBlocks.CREAM_CANYON_STONE.get(), sunstone, true);
        oreWithOverlay(ModBlocks.SCORCHED_SANDSTONE_SUNSTONE_ORE.get(), scorchedSandstone.side(), scorchedSandstone, sunstone, true);
        canyonOre(ModBlocks.RED_CANYON_TURQUOISE_ORE.get(), ModBlocks.RED_CANYON_STONE.get(), turquoise, false);
        canyonOre(ModBlocks.ORANGE_CANYON_TURQUOISE_ORE.get(), ModBlocks.ORANGE_CANYON_STONE.get(), turquoise, false);
        canyonOre(ModBlocks.CREAM_CANYON_TURQUOISE_ORE.get(), ModBlocks.CREAM_CANYON_STONE.get(), turquoise, false);
    }

    /** An ore over a canyon stone tint (the same texture on every face). */
    private void canyonOre(Block ore, Block canyonStone, CubeTextures overlay, boolean emissive) {
        ResourceLocation stone = blockTexture(canyonStone);
        oreWithOverlay(ore, stone, CubeTextures.all(stone), overlay, emissive);
    }

    /** The palm set, laid out like the dead wood set, plus leaves, sapling and its pot. */
    private void palm() {
        logBlock(ModBlocks.PALM_LOG.get());
        axisBlock(ModBlocks.PALM_WOOD.get(), blockTexture(ModBlocks.PALM_LOG.get()), blockTexture(ModBlocks.PALM_LOG.get()));
        logBlock(ModBlocks.STRIPPED_PALM_LOG.get());
        axisBlock(ModBlocks.STRIPPED_PALM_WOOD.get(),
                blockTexture(ModBlocks.STRIPPED_PALM_LOG.get()), blockTexture(ModBlocks.STRIPPED_PALM_LOG.get()));

        ResourceLocation planks = blockTexture(ModBlocks.PALM_PLANKS.get());
        simpleBlock(ModBlocks.PALM_PLANKS.get());
        stairsBlock(ModBlocks.PALM_STAIRS.get(), planks);
        slabBlock(ModBlocks.PALM_SLAB.get(), planks, planks);
        fenceBlock(ModBlocks.PALM_FENCE.get(), planks);
        fenceGateBlock(ModBlocks.PALM_FENCE_GATE.get(), planks);
        doorBlockWithRenderType(ModBlocks.PALM_DOOR.get(),
                modLoc("block/palm_door_bottom"), modLoc("block/palm_door_top"), "cutout");
        trapdoorBlockWithRenderType(ModBlocks.PALM_TRAPDOOR.get(), modLoc("block/palm_trapdoor"), true, "cutout");
        pressurePlateBlock(ModBlocks.PALM_PRESSURE_PLATE.get(), planks);
        buttonBlock(ModBlocks.PALM_BUTTON.get(), planks);

        // Fixed colour in the texture (no biome tint, so no tintindex), like the dead grass.
        simpleBlock(ModBlocks.PALM_LEAVES.get(), models().cubeAll("palm_leaves", blockTexture(ModBlocks.PALM_LEAVES.get()))
                .renderType("cutout_mipped"));
        // The stage property does not change the model.
        cross(ModBlocks.PALM_SAPLING.get());
        simpleBlock(ModBlocks.POTTED_PALM_SAPLING.get(), models().flowerPotCross("potted_palm_sapling",
                blockTexture(ModBlocks.PALM_SAPLING.get())).renderType("cutout"));
    }

    /** A full block with its stairs, slab and wall, all on the block's own texture. */
    private void stoneFamily(Block block, StairBlock stairs, SlabBlock slab, WallBlock wall) {
        ResourceLocation texture = blockTexture(block);
        simpleBlock(block);
        stairsBlock(stairs, texture);
        slabBlock(slab, texture, texture);
        wallBlock(wall, texture);
    }

    /**
     * Like vanilla stone: the plain and a mirrored cube, each in four Y rotations, so a large face of the block does not
     * repeat the same texture tile after tile.
     */
    private void mirroredAndRotated(Block block) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        ResourceLocation texture = blockTexture(block);
        ModelFile plain = models().cubeAll(name, texture);
        ModelFile mirrored = models().singleTexture(name + "_mirrored", mcLoc("block/cube_mirrored_all"), "all", texture);
        List<ConfiguredModel> variants = new ArrayList<>();
        for (int y = 0; y < 360; y += 90) {
            variants.add(new ConfiguredModel(plain, 0, y, false));
            variants.add(new ConfiguredModel(mirrored, 0, y, false));
        }
        getVariantBuilder(block).partialState().setModels(variants.toArray(ConfiguredModel[]::new));
    }

    /** Cross model like vanilla plants; cutout, so the texture only has alpha 0 or 255. */
    private void cross(Block block) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        simpleBlock(block, crossModel(name, blockTexture(block)));
    }

    private ModelFile crossModel(String name, ResourceLocation texture) {
        return models().cross(name, texture).renderType("cutout");
    }

    /** One box with the same bounds as {@code BarrelCactusBlock.SHAPE} (3–13 px wide, 10 px high). */
    private void barrelCactus() {
        BlockModelBuilder model = models().withExistingParent("barrel_cactus", mcLoc("block/block"))
                .renderType("cutout")
                .texture("particle", modLoc("block/barrel_cactus_side"))
                .texture("top", modLoc("block/barrel_cactus_top"))
                .texture("side", modLoc("block/barrel_cactus_side"));
        model.element().from(3, 0, 3).to(13, 10, 13).allFaces((direction, face) -> {
            switch (direction) {
                case UP -> face.texture("#top").uvs(3, 3, 13, 13);
                case DOWN -> face.texture("#top").uvs(3, 3, 13, 13).cullface(Direction.DOWN);
                default -> face.texture("#side").uvs(3, 6, 13, 16);
            }
        }).end();
        simpleBlock(ModBlocks.BARREL_CACTUS.get(), model);
    }

    /** Like vanilla's potted cactus: a one-box pot and a smaller cactus in it (a child model cannot add elements). */
    private void pottedBarrelCactus() {
        BlockModelBuilder model = models().withExistingParent("potted_barrel_cactus", mcLoc("block/block"))
                .renderType("cutout")
                .ao(false)
                .texture("particle", mcLoc("block/flower_pot"))
                .texture("flowerpot", mcLoc("block/flower_pot"))
                .texture("top", modLoc("block/barrel_cactus_top"))
                .texture("side", modLoc("block/barrel_cactus_side"));
        model.element().from(5, 0, 5).to(11, 6, 11).allFaces((direction, face) -> {
            switch (direction) {
                case DOWN -> face.texture("#flowerpot").uvs(5, 5, 11, 11).cullface(Direction.DOWN);
                default -> face.texture("#flowerpot");
            }
        }).end();
        model.element().from(5.5f, 6, 5.5f).to(10.5f, 11, 10.5f).allFacesExcept((direction, face) -> {
            if (direction == Direction.UP) {
                face.texture("#top").uvs(3, 3, 13, 13);
            } else {
                face.texture("#side").uvs(3, 6, 13, 16);
            }
        }, Set.of(Direction.DOWN)).end();
        simpleBlock(ModBlocks.POTTED_BARREL_CACTUS.get(), model);
    }

    /**
     * Ore as two full cubes: a base (a reference to the host rock's texture, vanilla or ours, never a copy) with our own
     * transparent overlay on top, like vanilla's grass block overlay. Cutout, so overlay pixels must have alpha 0 or
     * 255. An {@code emissive} overlay gets NeoForge {@code neoforge_data} (full block and sky light) without ambient
     * occlusion, so it glows evenly in the dark; this only affects rendering, not the light the block emits.
     */
    private void oreWithOverlay(Block block, ResourceLocation particle, CubeTextures base, CubeTextures overlay,
                                boolean emissive) {
        overlayCube(block, particle, base, overlay, emissive, "cutout");
    }

    /** {@link #oreWithOverlay} with a chosen render type (leaves: {@code cutout_mipped}). */
    private void overlayCube(Block block, ResourceLocation particle, CubeTextures base, CubeTextures overlay,
                             boolean emissive, String renderType) {
        simpleBlock(block, overlayModel(BuiltInRegistries.BLOCK.getKey(block).getPath(), particle, base, overlay, emissive,
                renderType, false));
    }

    /** The model of {@link #overlayCube}; {@code mirrored} flips every face of both layers horizontally. */
    private BlockModelBuilder overlayModel(String name, ResourceLocation particle, CubeTextures base, CubeTextures overlay,
                                           boolean emissive, String renderType, boolean mirrored) {
        BlockModelBuilder model = models().withExistingParent(name, mcLoc("block/block"))
                .renderType(renderType)
                .texture("particle", particle);
        base.apply(model, "base");
        overlay.apply(model, "overlay");
        model.element().allFaces((direction, face) -> {
            face.texture("#base_" + CubeTextures.face(direction)).cullface(direction);
            if (mirrored) {
                face.uvs(16, 0, 0, 16);
            }
        }).end();
        var overlayElement = model.element().allFaces((direction, face) -> {
            face.texture("#overlay_" + CubeTextures.face(direction)).cullface(direction);
            if (mirrored) {
                face.uvs(16, 0, 0, 16);
            }
        });
        if (emissive) {
            overlayElement.emissivity(15, 15).ao(false);
        }
        overlayElement.end();
        return model;
    }

    /** Textures for the top, the four sides and the bottom of a cube. */
    private record CubeTextures(ResourceLocation top, ResourceLocation side, ResourceLocation bottom) {
        static CubeTextures all(ResourceLocation texture) {
            return new CubeTextures(texture, texture, texture);
        }

        static String face(Direction direction) {
            return switch (direction) {
                case UP -> "top";
                case DOWN -> "bottom";
                default -> "side";
            };
        }

        void apply(BlockModelBuilder model, String prefix) {
            model.texture(prefix + "_top", top).texture(prefix + "_side", side).texture(prefix + "_bottom", bottom);
        }
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
