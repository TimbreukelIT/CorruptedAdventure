package nl.timtoys.corruptedadventure.registry;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Direction;
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
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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

    private static String experience(DropExperienceBlock ore) {
        JsonObject json = DropExperienceBlock.CODEC.codec().encodeStart(JsonOps.INSTANCE, ore).getOrThrow().getAsJsonObject();
        return IntProvider.CODEC.parse(JsonOps.INSTANCE, json.get("experience")).getOrThrow().toString();
    }
}
