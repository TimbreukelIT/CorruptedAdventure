package nl.timtoys.corruptedadventure.registry;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
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

    private static String experience(DropExperienceBlock ore) {
        JsonObject json = DropExperienceBlock.CODEC.codec().encodeStart(JsonOps.INSTANCE, ore).getOrThrow().getAsJsonObject();
        return IntProvider.CODEC.parse(JsonOps.INSTANCE, json.get("experience")).getOrThrow().toString();
    }
}
