package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.block.StalactiteBlock;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

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
        glowingCube(ModBlocks.SMOLDERING_STONE.get());

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
        slimeStalactite();
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
     * texture; the waterlogged property does not change the model.
     */
    private void slimeStalactite() {
        getVariantBuilder(ModBlocks.SLIME_STALACTITE.get()).forAllStatesExcept(state -> {
            String direction = state.getValue(StalactiteBlock.TIP_DIRECTION).getSerializedName();
            String thickness = state.getValue(StalactiteBlock.THICKNESS).getSerializedName();
            String name = "slime_stalactite_" + direction + "_" + thickness;
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
     * amethyst bud transforms. Shared by every crystal block (salt crystal, the dew crystal stages). An {@code emissive}
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
        model.element().from(0.8f, 0, 8).to(15.2f, 16, 8)
                .rotation().origin(8, 8, 8).axis(Direction.Axis.Y).angle(45).rescale(true).end().shade(false)
                .emissivity(15, 15).ao(false)
                .face(Direction.NORTH).uvs(0, 0, 16, 16).texture("#cross").end()
                .face(Direction.SOUTH).uvs(0, 0, 16, 16).texture("#cross").end().end();
        model.element().from(8, 0, 0.8f).to(8, 16, 15.2f)
                .rotation().origin(8, 8, 8).axis(Direction.Axis.Y).angle(45).rescale(true).end().shade(false)
                .emissivity(15, 15).ao(false)
                .face(Direction.WEST).uvs(0, 0, 16, 16).texture("#cross").end()
                .face(Direction.EAST).uvs(0, 0, 16, 16).texture("#cross").end().end();
        return model;
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
        BlockModelBuilder model = models().withExistingParent(BuiltInRegistries.BLOCK.getKey(block).getPath(), mcLoc("block/block"))
                .renderType(renderType)
                .texture("particle", particle);
        base.apply(model, "base");
        overlay.apply(model, "overlay");
        model.element().allFaces((direction, face) -> face.texture("#base_" + CubeTextures.face(direction)).cullface(direction)).end();
        var overlayElement = model.element()
                .allFaces((direction, face) -> face.texture("#overlay_" + CubeTextures.face(direction)).cullface(direction));
        if (emissive) {
            overlayElement.emissivity(15, 15).ao(false);
        }
        overlayElement.end();
        simpleBlock(block, model);
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
