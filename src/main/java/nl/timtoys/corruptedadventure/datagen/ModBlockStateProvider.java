package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
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
                CubeTextures.all(netherrack), CubeTextures.all(modLoc("block/soul_crystal_ore_overlay")));
        oreWithOverlay(ModBlocks.CRIMSON_SOUL_CRYSTAL_ORE.get(), mcLoc("block/crimson_nylium"),
                new CubeTextures(mcLoc("block/crimson_nylium"), mcLoc("block/crimson_nylium_side"), netherrack),
                new CubeTextures(modLoc("block/crimson_soul_crystal_ore_overlay_top"),
                        modLoc("block/crimson_soul_crystal_ore_overlay_side"), modLoc("block/soul_crystal_ore_overlay")));

        ring2();
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
        simpleBlock(block, models().cross(name, blockTexture(block)).renderType("cutout"));
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
     * Ore as two full cubes: a base (a reference to the vanilla texture, no copy) with our own transparent overlay on
     * top, like vanilla's grass block overlay. Cutout, so overlay pixels must have alpha 0 or 255. The overlay is
     * emissive (NeoForge {@code neoforge_data}: full block and sky light) without ambient occlusion, so it glows evenly
     * in the dark; this only affects rendering, not the light the block emits.
     */
    private void oreWithOverlay(Block block, ResourceLocation particle, CubeTextures base, CubeTextures overlay) {
        BlockModelBuilder model = models().withExistingParent(BuiltInRegistries.BLOCK.getKey(block).getPath(), mcLoc("block/block"))
                .renderType("cutout")
                .texture("particle", particle);
        base.apply(model, "base");
        overlay.apply(model, "overlay");
        model.element().allFaces((direction, face) -> face.texture("#base_" + CubeTextures.face(direction)).cullface(direction)).end();
        model.element().allFaces((direction, face) -> face.texture("#overlay_" + CubeTextures.face(direction)).cullface(direction))
                .emissivity(15, 15)
                .ao(false)
                .end();
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
