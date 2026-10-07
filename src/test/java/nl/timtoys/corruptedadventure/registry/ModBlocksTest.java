package nl.timtoys.corruptedadventure.registry;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModBlocksTest {
    @Test
    void everyBlockExceptPottedPlantsHasABlockItemWithTheSameName() {
        assertFalse(ModBlocks.entries().isEmpty());
        for (DeferredHolder<Block, ? extends Block> block : ModBlocks.entries()) {
            Item item = block.get().asItem();
            if (block.get() instanceof FlowerPotBlock) {
                assertSame(Items.AIR, item, block.getId().toString());
                continue;
            }
            assertInstanceOf(BlockItem.class, item, block.getId().toString());
            assertEquals(block.getId(), BuiltInRegistries.ITEM.getKey(item));
        }
    }

    @Test
    void pottedPlantsHoldTheirPlant() {
        assertSame(ModBlocks.DRY_SHRUB.get(), ModBlocks.POTTED_DRY_SHRUB.get().getPotted());
        assertSame(ModBlocks.BARREL_CACTUS.get(), ModBlocks.POTTED_BARREL_CACTUS.get().getPotted());
        assertSame(ModBlocks.DESERT_FLOWER.get(), ModBlocks.POTTED_DESERT_FLOWER.get().getPotted());
        assertSame(ModBlocks.PALM_SAPLING.get(), ModBlocks.POTTED_PALM_SAPLING.get().getPotted());
        assertSame(ModBlocks.FIRE_BLOOM.get(), ModBlocks.POTTED_FIRE_BLOOM.get().getPotted());
    }

    @Test
    void creativeTabShowsBlocksThenStandaloneItemsInRegistrationOrder() {
        CreativeModeTab tab = ModCreativeTabs.MAIN.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(
                FeatureFlags.DEFAULT_FLAGS, false, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)));
        List<Item> shown = tab.getDisplayItems().stream().map(ItemStack::getItem).toList();

        List<Item> expected = new ArrayList<>();
        ModBlocks.entries().stream().map(block -> block.get().asItem()).filter(item -> item != Items.AIR).forEach(expected::add);
        ModItems.entries().stream().<Item>map(DeferredHolder::get).filter(item -> !(item instanceof BlockItem)).forEach(expected::add);
        assertEquals(expected, shown);
        // Every item exactly once.
        assertEquals(ModItems.entries().size(), shown.size());
        assertEquals(new HashSet<>(ModItems.entries().stream().<Item>map(DeferredHolder::get).toList()), new HashSet<>(shown));
    }

    @Test
    void looseSandSitsBetweenTheScorchedSandstoneSetAndTheFlora() {
        List<Item> shown = new ArrayList<>();
        ModBlocks.entries().stream().map(block -> block.get().asItem()).filter(item -> item != Items.AIR).forEach(shown::add);
        int looseSand = shown.indexOf(ModBlocks.LOOSE_SAND.asItem());
        assertTrue(looseSand > 0);
        assertEquals(ModBlocks.SMOOTH_SCORCHED_SANDSTONE_SLAB.asItem(), shown.get(looseSand - 1));
        assertEquals(ModBlocks.CRACKED_CLAY.asItem(), shown.get(looseSand + 1));
    }

    @Test
    void palmSetFollowsTheDesertFloraFromLogToSapling() {
        List<Item> shown = new ArrayList<>();
        ModBlocks.entries().stream().map(block -> block.get().asItem()).filter(item -> item != Items.AIR).forEach(shown::add);
        int log = shown.indexOf(ModBlocks.PALM_LOG.asItem());
        assertEquals(ModBlocks.DESERT_FLOWER.asItem(), shown.get(log - 1));
        assertEquals(List.of(ModBlocks.PALM_LOG.asItem(), ModBlocks.PALM_WOOD.asItem(), ModBlocks.STRIPPED_PALM_LOG.asItem(),
                ModBlocks.STRIPPED_PALM_WOOD.asItem(), ModBlocks.PALM_PLANKS.asItem(), ModBlocks.PALM_STAIRS.asItem(),
                ModBlocks.PALM_SLAB.asItem(), ModBlocks.PALM_FENCE.asItem(), ModBlocks.PALM_FENCE_GATE.asItem(),
                ModBlocks.PALM_DOOR.asItem(), ModBlocks.PALM_TRAPDOOR.asItem(), ModBlocks.PALM_PRESSURE_PLATE.asItem(),
                ModBlocks.PALM_BUTTON.asItem(), ModBlocks.PALM_LEAVES.asItem(), ModBlocks.PALM_SAPLING.asItem()),
                shown.subList(log, log + 15));
    }

    @Test
    void ring2OresFollowThePalmSetSunstoneFirst() {
        List<Item> shown = new ArrayList<>();
        ModBlocks.entries().stream().map(block -> block.get().asItem()).filter(item -> item != Items.AIR).forEach(shown::add);
        int sapling = shown.indexOf(ModBlocks.PALM_SAPLING.asItem());
        assertEquals(List.of(ModBlocks.RED_CANYON_SUNSTONE_ORE.asItem(), ModBlocks.ORANGE_CANYON_SUNSTONE_ORE.asItem(),
                ModBlocks.CREAM_CANYON_SUNSTONE_ORE.asItem(), ModBlocks.SCORCHED_SANDSTONE_SUNSTONE_ORE.asItem(),
                ModBlocks.RED_CANYON_TURQUOISE_ORE.asItem(), ModBlocks.ORANGE_CANYON_TURQUOISE_ORE.asItem(),
                ModBlocks.CREAM_CANYON_TURQUOISE_ORE.asItem()), shown.subList(sapling + 1, sapling + 8));
        List<Item> standalone = ModItems.entries().stream().<Item>map(DeferredHolder::get)
                .filter(item -> !(item instanceof BlockItem)).toList();
        int soulCrystal = standalone.indexOf(ModItems.SOUL_CRYSTAL.get());
        assertEquals(List.of(ModItems.RAW_SUNSTONE.get(), ModItems.RAW_TURQUOISE.get()),
                standalone.subList(soulCrystal + 1, soulCrystal + 3));
    }

    @Test
    void saltSetFollowsTheRing2OresAndSaltFollowsRawTurquoise() {
        List<Item> shown = new ArrayList<>();
        ModBlocks.entries().stream().map(block -> block.get().asItem()).filter(item -> item != Items.AIR).forEach(shown::add);
        int ore = shown.indexOf(ModBlocks.CREAM_CANYON_TURQUOISE_ORE.asItem());
        assertEquals(List.of(ModBlocks.SALT_CRUST.asItem(), ModBlocks.SALT_BLOCK.asItem(), ModBlocks.POLISHED_SALT.asItem(),
                ModBlocks.POLISHED_SALT_STAIRS.asItem(), ModBlocks.POLISHED_SALT_SLAB.asItem(),
                ModBlocks.POLISHED_SALT_WALL.asItem(), ModBlocks.SALT_CRYSTAL.asItem()), shown.subList(ore + 1, ore + 8));
        List<Item> standalone = ModItems.entries().stream().<Item>map(DeferredHolder::get)
                .filter(item -> !(item instanceof BlockItem)).toList();
        assertEquals(standalone.indexOf(ModItems.RAW_TURQUOISE.get()) + 1, standalone.indexOf(ModItems.SALT.get()));
    }

    /** Worldgen (change 30) places the default state on the crust, so it must face up and glow faintly. */
    @Test
    void saltCrystalStandsUpDryAndGlowsFaintly() {
        BlockState crystal = ModBlocks.SALT_CRYSTAL.get().defaultBlockState();
        assertEquals(Direction.UP, crystal.getValue(AmethystClusterBlock.FACING));
        assertFalse(crystal.getValue(AmethystClusterBlock.WATERLOGGED));
        assertEquals(3, crystal.getLightEmission());
    }

    @Test
    void saltCrustDoesNotFall() {
        assertFalse(ModBlocks.SALT_CRUST.get() instanceof FallingBlock);
    }

    @Test
    void sunstoneOresGlowAndTurquoiseOresDoNot() {
        for (Block ore : List.of(ModBlocks.RED_CANYON_SUNSTONE_ORE.get(), ModBlocks.ORANGE_CANYON_SUNSTONE_ORE.get(),
                ModBlocks.CREAM_CANYON_SUNSTONE_ORE.get(), ModBlocks.SCORCHED_SANDSTONE_SUNSTONE_ORE.get())) {
            assertEquals(3, ore.defaultBlockState().getLightEmission(), ore.toString());
        }
        for (Block ore : List.of(ModBlocks.RED_CANYON_TURQUOISE_ORE.get(), ModBlocks.ORANGE_CANYON_TURQUOISE_ORE.get(),
                ModBlocks.CREAM_CANYON_TURQUOISE_ORE.get())) {
            assertEquals(0, ore.defaultBlockState().getLightEmission(), ore.toString());
        }
    }

    /** XP only drops when a player breaks the block (not testable over RCON); the codec exposes the range. */
    @Test
    void ring2OresDropSunstoneTwoToFiveAndTurquoiseThreeToSevenXp() {
        for (DeferredBlock<DropExperienceBlock> ore : List.of(ModBlocks.RED_CANYON_SUNSTONE_ORE, ModBlocks.ORANGE_CANYON_SUNSTONE_ORE,
                ModBlocks.CREAM_CANYON_SUNSTONE_ORE, ModBlocks.SCORCHED_SANDSTONE_SUNSTONE_ORE)) {
            assertEquals(UniformInt.of(2, 5).toString(), experience(ore.get()), ore.getId().toString());
        }
        for (DeferredBlock<DropExperienceBlock> ore : List.of(ModBlocks.RED_CANYON_TURQUOISE_ORE,
                ModBlocks.ORANGE_CANYON_TURQUOISE_ORE, ModBlocks.CREAM_CANYON_TURQUOISE_ORE)) {
            assertEquals(UniformInt.of(3, 7).toString(), experience(ore.get()), ore.getId().toString());
        }
    }

    @Test
    void netherRing2FollowsTheSaltSetInOrderAndEndsWithTheSprouts() {
        List<Item> shown = new ArrayList<>();
        ModBlocks.entries().stream().map(block -> block.get().asItem()).filter(item -> item != Items.AIR).forEach(shown::add);
        int soaked = shown.indexOf(ModBlocks.SOAKED_NETHERRACK.asItem());
        assertEquals(ModBlocks.SALT_CRYSTAL.asItem(), shown.get(soaked - 1));
        assertEquals(List.of(ModBlocks.SOAKED_NETHERRACK.asItem(), ModBlocks.SPORE_MOSS.asItem(),
                ModBlocks.DRIPSHROOM_STEM.asItem(), ModBlocks.DRIPSHROOM_HYPHAE.asItem(), ModBlocks.STRIPPED_DRIPSHROOM_STEM.asItem(),
                ModBlocks.STRIPPED_DRIPSHROOM_HYPHAE.asItem(), ModBlocks.DRIPSHROOM_PLANKS.asItem(), ModBlocks.DRIPSHROOM_STAIRS.asItem(),
                ModBlocks.DRIPSHROOM_SLAB.asItem(), ModBlocks.DRIPSHROOM_FENCE.asItem(), ModBlocks.DRIPSHROOM_FENCE_GATE.asItem(),
                ModBlocks.DRIPSHROOM_DOOR.asItem(), ModBlocks.DRIPSHROOM_TRAPDOOR.asItem(), ModBlocks.DRIPSHROOM_PRESSURE_PLATE.asItem(),
                ModBlocks.DRIPSHROOM_BUTTON.asItem(), ModBlocks.DRIPSHROOM_CAP.asItem(), ModBlocks.DRIPSHROOM_LAMP.asItem(),
                ModBlocks.GLOW_MOLD.asItem(), ModBlocks.SLIME_STALACTITE.asItem(), ModBlocks.SPORE_SPROUTS.asItem()),
                shown.subList(soaked, soaked + 20));
    }

    @Test
    void dewBlocksEndTheNetherRing2SectionAndTheDewCrystalEndsTheLooseItems() {
        List<Item> shown = new ArrayList<>();
        ModBlocks.entries().stream().map(block -> block.get().asItem()).filter(item -> item != Items.AIR).forEach(shown::add);
        int sprouts = shown.indexOf(ModBlocks.SPORE_SPROUTS.asItem());
        assertEquals(List.of(ModBlocks.DEW_SOURCE.asItem(), ModBlocks.SMALL_DEW_CRYSTAL_BUD.asItem(),
                ModBlocks.MEDIUM_DEW_CRYSTAL_BUD.asItem(), ModBlocks.LARGE_DEW_CRYSTAL_BUD.asItem(),
                ModBlocks.DEW_CRYSTAL_CLUSTER.asItem()), shown.subList(sprouts + 1, sprouts + 6));
        List<Item> standalone = ModItems.entries().stream().<Item>map(DeferredHolder::get)
                .filter(item -> !(item instanceof BlockItem)).toList();
        assertEquals(standalone.indexOf(ModItems.SALT.get()) + 1, standalone.indexOf(ModItems.DEW_CRYSTAL.get()));
    }

    @Test
    void netherRing2BlocksTickLightAndSoundAsPlanned() {
        assertTrue(ModBlocks.SPORE_MOSS.get().defaultBlockState().isRandomlyTicking());
        assertFalse(ModBlocks.SOAKED_NETHERRACK.get().defaultBlockState().isRandomlyTicking());
        assertFalse(ModBlocks.SPORE_SPROUTS.get().defaultBlockState().isRandomlyTicking());
        assertFalse(ModBlocks.SLIME_STALACTITE.get().defaultBlockState().isRandomlyTicking());
        assertEquals(SoundType.MUD, ModBlocks.SOAKED_NETHERRACK.get().defaultBlockState().getSoundType());
        assertEquals(15, ModBlocks.DRIPSHROOM_LAMP.get().defaultBlockState().getLightEmission());
        // Glow mould has no face by default (dark); with a face it glows like glow lichen.
        assertEquals(0, ModBlocks.GLOW_MOLD.get().defaultBlockState().getLightEmission());
        assertEquals(7, ModBlocks.GLOW_MOLD.get().defaultBlockState()
                .setValue(MultifaceBlock.getFaceProperty(Direction.UP), true).getLightEmission());
        assertEquals(0, ModBlocks.SPORE_SPROUTS.get().defaultBlockState().getLightEmission());
    }

    /** Ring 3 follows Nether ring 2 directly (contract §3), in this order. */
    @Test
    void ring3FollowsTheDewBlocksInOrder() {
        List<Item> shown = blockItems();
        int cluster = shown.indexOf(ModBlocks.DEW_CRYSTAL_CLUSTER.asItem());
        List<Item> ring3 = Stream.of(ModBlocks.ASH_BLOCK, ModBlocks.ASH_LAYER, ModBlocks.ASH_CRUST, ModBlocks.SMOLDERING_ASH,
                ModBlocks.PUMICE, ModBlocks.PUMICE_STAIRS, ModBlocks.PUMICE_SLAB, ModBlocks.PUMICE_WALL,
                ModBlocks.POLISHED_PUMICE, ModBlocks.POLISHED_PUMICE_STAIRS, ModBlocks.POLISHED_PUMICE_SLAB,
                ModBlocks.POLISHED_PUMICE_WALL, ModBlocks.PUMICE_BRICKS, ModBlocks.PUMICE_BRICK_STAIRS,
                ModBlocks.PUMICE_BRICK_SLAB, ModBlocks.PUMICE_BRICK_WALL, ModBlocks.SMOLDERING_STONE,
                ModBlocks.CHARRED_LOG, ModBlocks.CHARRED_WOOD, ModBlocks.STRIPPED_CHARRED_LOG, ModBlocks.STRIPPED_CHARRED_WOOD,
                ModBlocks.CHARRED_PLANKS, ModBlocks.CHARRED_STAIRS, ModBlocks.CHARRED_SLAB, ModBlocks.CHARRED_FENCE,
                ModBlocks.CHARRED_FENCE_GATE, ModBlocks.CHARRED_DOOR, ModBlocks.CHARRED_TRAPDOOR,
                ModBlocks.CHARRED_PRESSURE_PLATE, ModBlocks.CHARRED_BUTTON,
                ModBlocks.EMBER_LEAVES, ModBlocks.FIRE_BLOOM, ModBlocks.ASH_SPROUTS, ModBlocks.EMBER_BERRY_BUSH,
                ModBlocks.VOLCANITE_ORE, ModBlocks.DEEPSLATE_VOLCANITE_ORE, ModBlocks.BASALT_VOLCANITE_ORE,
                ModBlocks.SULFUR_ORE, ModBlocks.BASALT_SULFUR_ORE, ModBlocks.FIRE_RUBY_ORE,
                ModBlocks.DEEPSLATE_FIRE_RUBY_ORE, ModBlocks.BASALT_FIRE_RUBY_ORE,
                ModBlocks.FIRE_CRYSTAL_BLOCK, ModBlocks.SMALL_FIRE_CRYSTAL_BUD, ModBlocks.MEDIUM_FIRE_CRYSTAL_BUD,
                ModBlocks.LARGE_FIRE_CRYSTAL_BUD, ModBlocks.FIRE_CRYSTAL_CLUSTER,
                ModBlocks.SULFUR_CRUST, ModBlocks.FUMAROLE, ModBlocks.BOILING_MUD, ModBlocks.GEYSER).<Item>map(DeferredBlock::asItem).toList();
        assertEquals(ring3, shown.subList(cluster + 1, cluster + 1 + ring3.size()));
    }

    @Test
    void smolderingStoneGlowsAtSevenOnlyThroughItsOverlay() {
        BlockState stone = ModBlocks.SMOLDERING_STONE.get().defaultBlockState();
        assertEquals(7, stone.getLightEmission());
        // Two layers (decision 2026-10-06): the block is lit normally, only the model's glow overlay is full-bright.
        assertFalse(stone.emissiveRendering(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
        assertFalse(stone.isRandomlyTicking());
    }

    /** Decision 2026-10-06: charred wood does not burn, so lava never sets it alight (no fire odds either: S). */
    @Test
    void charredWoodIsNotIgnitedByLava() {
        for (DeferredBlock<?> block : List.of(ModBlocks.CHARRED_LOG, ModBlocks.CHARRED_WOOD, ModBlocks.STRIPPED_CHARRED_LOG,
                ModBlocks.STRIPPED_CHARRED_WOOD, ModBlocks.CHARRED_PLANKS, ModBlocks.CHARRED_STAIRS, ModBlocks.CHARRED_SLAB,
                ModBlocks.CHARRED_FENCE, ModBlocks.CHARRED_FENCE_GATE, ModBlocks.CHARRED_DOOR, ModBlocks.CHARRED_TRAPDOOR,
                ModBlocks.CHARRED_PRESSURE_PLATE, ModBlocks.CHARRED_BUTTON)) {
            assertFalse(block.get().defaultBlockState().ignitedByLava(), block.getId().toString());
        }
        assertTrue(Blocks.OAK_PLANKS.defaultBlockState().ignitedByLava());
    }

    @Test
    void ashBlockDoesNotFallButTheCrustDoes() {
        assertFalse(ModBlocks.ASH_BLOCK.get() instanceof FallingBlock);
        assertInstanceOf(FallingBlock.class, ModBlocks.ASH_CRUST.get());
    }

    /** V3 = A: the ember leaves only glow in their texture; the smouldering ash glows faintly (light 2). */
    @Test
    void emberLeavesNeverTickOrDecayAndGiveNoLight() {
        for (BlockState state : ModBlocks.EMBER_LEAVES.get().getStateDefinition().getPossibleStates()) {
            assertFalse(state.isRandomlyTicking(), state.toString());
            assertEquals(0, state.getLightEmission(), state.toString());
            assertFalse(state.ignitedByLava(), state.toString());
        }
        assertEquals(2, ModBlocks.SMOLDERING_ASH.get().defaultBlockState().getLightEmission());
    }

    @Test
    void fireFloraDoesNotBurnButTheBerryBushStillGrows() {
        for (DeferredBlock<?> plant : List.of(ModBlocks.FIRE_BLOOM, ModBlocks.ASH_SPROUTS, ModBlocks.EMBER_BERRY_BUSH)) {
            assertFalse(plant.get().defaultBlockState().ignitedByLava(), plant.getId().toString());
        }
        assertTrue(ModBlocks.EMBER_BERRY_BUSH.get().defaultBlockState().isRandomlyTicking());
    }

    /** One step above ring 2: XP volcanite 3–6, sulfur 1–3, fire ruby 4–8; half a point harder than the host's vanilla ore. */
    @Test
    void ring3OresHaveTheirXpLightAndHardness() {
        Map<UniformInt, List<DeferredBlock<DropExperienceBlock>>> xp = Map.of(
                UniformInt.of(3, 6), List.of(ModBlocks.VOLCANITE_ORE, ModBlocks.DEEPSLATE_VOLCANITE_ORE, ModBlocks.BASALT_VOLCANITE_ORE),
                UniformInt.of(1, 3), List.of(ModBlocks.SULFUR_ORE, ModBlocks.BASALT_SULFUR_ORE),
                UniformInt.of(4, 8), List.of(ModBlocks.FIRE_RUBY_ORE, ModBlocks.DEEPSLATE_FIRE_RUBY_ORE, ModBlocks.BASALT_FIRE_RUBY_ORE));
        xp.forEach((range, ores) -> ores.forEach(ore -> assertEquals(range.toString(), experience(ore.get()), ore.getId().toString())));
        assertEquals(3, ModBlocks.FIRE_RUBY_ORE.get().defaultBlockState().getLightEmission());
        assertEquals(3, ModBlocks.DEEPSLATE_FIRE_RUBY_ORE.get().defaultBlockState().getLightEmission());
        assertEquals(3, ModBlocks.BASALT_FIRE_RUBY_ORE.get().defaultBlockState().getLightEmission());
        assertEquals(0, ModBlocks.VOLCANITE_ORE.get().defaultBlockState().getLightEmission());
        assertEquals(0, ModBlocks.SULFUR_ORE.get().defaultBlockState().getLightEmission());
        float extra = ModBlocks.RING_3_ORE_EXTRA_HARDNESS;
        assertEquals(Blocks.IRON_ORE.defaultDestroyTime() + extra, ModBlocks.VOLCANITE_ORE.get().defaultDestroyTime());
        assertEquals(Blocks.DEEPSLATE_IRON_ORE.defaultDestroyTime() + extra, ModBlocks.DEEPSLATE_FIRE_RUBY_ORE.get().defaultDestroyTime());
        assertEquals(Blocks.COAL_ORE.defaultDestroyTime() + extra, ModBlocks.SULFUR_ORE.get().defaultDestroyTime());
        assertEquals(Blocks.COAL_ORE.defaultDestroyTime() + extra, ModBlocks.BASALT_SULFUR_ORE.get().defaultDestroyTime());
        assertEquals(Blocks.IRON_ORE.getExplosionResistance(), ModBlocks.VOLCANITE_ORE.get().getExplosionResistance());
        assertEquals(SoundType.BASALT, ModBlocks.BASALT_SULFUR_ORE.get().defaultBlockState().getSoundType());
    }

    @Test
    void ring3RawMaterialsFollowTheDewCrystal() {
        List<Item> standalone = ModItems.entries().stream().<Item>map(DeferredHolder::get)
                .filter(item -> !(item instanceof BlockItem)).toList();
        int dew = standalone.indexOf(ModItems.DEW_CRYSTAL.get());
        assertEquals(List.of(ModItems.RAW_VOLCANITE.get(), ModItems.SULFUR.get(), ModItems.FIRE_RUBY.get(), ModItems.FIRE_CRYSTAL.get()),
                standalone.subList(dew + 1, dew + 5));
    }

    /** Like the dew crystal stages: light 1, 2, 4, 5, facing up and dry by default; the wall gives no light and never ticks. */
    @Test
    void fireCrystalStagesGlowAndTheWallDoesNot() {
        List<DeferredBlock<AmethystClusterBlock>> stages = List.of(ModBlocks.SMALL_FIRE_CRYSTAL_BUD, ModBlocks.MEDIUM_FIRE_CRYSTAL_BUD,
                ModBlocks.LARGE_FIRE_CRYSTAL_BUD, ModBlocks.FIRE_CRYSTAL_CLUSTER);
        int[] light = {1, 2, 4, 5};
        for (int i = 0; i < stages.size(); i++) {
            BlockState stage = stages.get(i).get().defaultBlockState();
            assertEquals(light[i], stage.getLightEmission(), stages.get(i).getId().toString());
            assertEquals(Direction.UP, stage.getValue(AmethystClusterBlock.FACING));
            assertFalse(stage.getValue(AmethystClusterBlock.WATERLOGGED));
            assertFalse(stage.isRandomlyTicking());
        }
        assertEquals(0, ModBlocks.FIRE_CRYSTAL_BLOCK.get().defaultBlockState().getLightEmission());
        assertFalse(ModBlocks.FIRE_CRYSTAL_BLOCK.get().defaultBlockState().isRandomlyTicking());
    }

    /** Only the geyser ticks (to start its cycle); the mud slows like honey; neither crust nor mud falls or glows. */
    @Test
    void sulfurAndSteamBlocksBehaveAsPlanned() {
        assertTrue(ModBlocks.GEYSER.get().defaultBlockState().isRandomlyTicking());
        for (DeferredBlock<?> block : List.of(ModBlocks.SULFUR_CRUST, ModBlocks.FUMAROLE, ModBlocks.BOILING_MUD)) {
            assertFalse(block.get().defaultBlockState().isRandomlyTicking(), block.getId().toString());
        }
        for (DeferredBlock<?> block : List.of(ModBlocks.SULFUR_CRUST, ModBlocks.FUMAROLE, ModBlocks.BOILING_MUD, ModBlocks.GEYSER)) {
            assertEquals(0, block.get().defaultBlockState().getLightEmission(), block.getId().toString());
            assertFalse(block.get() instanceof FallingBlock, block.getId().toString());
        }
        assertEquals(Blocks.HONEY_BLOCK.getSpeedFactor(), ModBlocks.BOILING_MUD.get().getSpeedFactor());
        assertEquals(Blocks.HONEY_BLOCK.getJumpFactor(), ModBlocks.BOILING_MUD.get().getJumpFactor());
        assertEquals(Blocks.MUD.defaultBlockState().getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO),
                ModBlocks.BOILING_MUD.get().defaultBlockState().getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
    }

    /** Nether ring 3 follows ring 3 directly (contract §3), in this order. */
    @Test
    void netherRing3FollowsRing3InOrder() {
        List<Item> shown = blockItems();
        int geyser = shown.indexOf(ModBlocks.GEYSER.asItem());
        List<Item> netherRing3 = Stream.of(ModBlocks.COLD_LAVA, ModBlocks.COLD_LAVA_STAIRS, ModBlocks.COLD_LAVA_SLAB,
                ModBlocks.COLD_LAVA_WALL, ModBlocks.POLISHED_COLD_LAVA, ModBlocks.POLISHED_COLD_LAVA_STAIRS,
                ModBlocks.POLISHED_COLD_LAVA_SLAB, ModBlocks.POLISHED_COLD_LAVA_WALL, ModBlocks.COLD_LAVA_BRICKS,
                ModBlocks.COLD_LAVA_BRICK_STAIRS, ModBlocks.COLD_LAVA_BRICK_SLAB, ModBlocks.COLD_LAVA_BRICK_WALL,
                ModBlocks.NETHER_ICE, ModBlocks.PACKED_NETHER_ICE, ModBlocks.RIME_NETHERRACK,
                ModBlocks.RIMESHROOM_STEM, ModBlocks.RIMESHROOM_HYPHAE, ModBlocks.STRIPPED_RIMESHROOM_STEM,
                ModBlocks.STRIPPED_RIMESHROOM_HYPHAE, ModBlocks.RIMESHROOM_PLANKS, ModBlocks.RIMESHROOM_STAIRS,
                ModBlocks.RIMESHROOM_SLAB, ModBlocks.RIMESHROOM_FENCE, ModBlocks.RIMESHROOM_FENCE_GATE,
                ModBlocks.RIMESHROOM_DOOR, ModBlocks.RIMESHROOM_TRAPDOOR, ModBlocks.RIMESHROOM_PRESSURE_PLATE,
                ModBlocks.RIMESHROOM_BUTTON, ModBlocks.RIMESHROOM_CAP, ModBlocks.ICE_STALACTITE,
                ModBlocks.GLOWING_FISSURE).<Item>map(DeferredBlock::asItem).toList();
        assertEquals(netherRing3, shown.subList(geyser + 1, geyser + 1 + netherRing3.size()));
    }

    /** Decision 2026-10-06: Nether ice is opaque and solid, slippery like ice, and never melts (no random ticks). */
    @Test
    void netherIceIsOpaqueSlipperyAndNeverMelts() {
        for (DeferredBlock<?> ice : List.of(ModBlocks.NETHER_ICE, ModBlocks.PACKED_NETHER_ICE)) {
            BlockState state = ice.get().defaultBlockState();
            assertTrue(state.canOcclude(), ice.getId().toString());
            assertFalse(state.isRandomlyTicking(), ice.getId().toString());
            assertEquals(Blocks.ICE.getFriction(), ice.get().getFriction(), ice.getId().toString());
        }
    }

    @Test
    void netherRing3WoodDoesNotBurnAndTheFissureGlowsAtSevenOnlyThroughItsOverlay() {
        for (DeferredBlock<?> block : List.of(ModBlocks.RIMESHROOM_STEM, ModBlocks.RIMESHROOM_HYPHAE,
                ModBlocks.STRIPPED_RIMESHROOM_STEM, ModBlocks.STRIPPED_RIMESHROOM_HYPHAE, ModBlocks.RIMESHROOM_PLANKS,
                ModBlocks.RIMESHROOM_STAIRS, ModBlocks.RIMESHROOM_SLAB, ModBlocks.RIMESHROOM_FENCE,
                ModBlocks.RIMESHROOM_FENCE_GATE, ModBlocks.RIMESHROOM_DOOR, ModBlocks.RIMESHROOM_TRAPDOOR,
                ModBlocks.RIMESHROOM_PRESSURE_PLATE, ModBlocks.RIMESHROOM_BUTTON)) {
            assertFalse(block.get().defaultBlockState().ignitedByLava(), block.getId().toString());
        }
        BlockState fissure = ModBlocks.GLOWING_FISSURE.get().defaultBlockState();
        assertEquals(7, fissure.getLightEmission());
        assertFalse(fissure.emissiveRendering(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
        assertFalse(ModBlocks.ICE_STALACTITE.get().defaultBlockState().isRandomlyTicking());
        assertEquals(SoundType.GLASS, ModBlocks.ICE_STALACTITE.get().defaultBlockState().getSoundType());
        assertEquals(NoteBlockInstrument.CHIME, ModBlocks.ICE_STALACTITE.get().defaultBlockState().instrument());
    }

    /** Nether wood is no furnace fuel: every rimeshroom item is in the generated {@code #non_flammable_wood}. */
    @Test
    void everyRimeshroomItemIsNonFlammableWood() {
        String relative = "src/generated/resources/data/minecraft/tags/item/non_flammable_wood.json";
        Path dir = Path.of("").toAbsolutePath();
        while (!Files.exists(dir.resolve(relative))) {
            dir = dir.getParent();
        }
        Set<String> values = new HashSet<>();
        WorldgenData.read(dir.resolve(relative)).getAsJsonArray("values").forEach(value -> values.add(value.getAsString()));
        List<String> rimeshroomWood = ModBlocks.entries().stream()
                .filter(block -> block.get().asItem() != Items.AIR && !block.getId().equals(ModBlocks.RIMESHROOM_CAP.getId()))
                .map(block -> block.getId().toString()).filter(id -> id.contains("rimeshroom")).toList();
        assertEquals(13, rimeshroomWood.size());
        rimeshroomWood.forEach(id -> assertTrue(values.contains(id), id));
    }

    private static List<Item> blockItems() {
        List<Item> shown = new ArrayList<>();
        ModBlocks.entries().stream().map(block -> block.get().asItem()).filter(item -> item != Items.AIR).forEach(shown::add);
        return shown;
    }

    private static String experience(DropExperienceBlock ore) {
        JsonObject json = DropExperienceBlock.CODEC.codec().encodeStart(JsonOps.INSTANCE, ore).getOrThrow().getAsJsonObject();
        return IntProvider.CODEC.parse(JsonOps.INSTANCE, json.get("experience")).getOrThrow().toString();
    }
}
