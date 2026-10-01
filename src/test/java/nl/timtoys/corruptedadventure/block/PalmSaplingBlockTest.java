package nl.timtoys.corruptedadventure.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModTags;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ground rule: vanilla sapling ground ({@code #dirt}, farmland) or {@code #desert_plant_soil}. Unit tests have no
 * datapack, so the two tags are bound here by hand with a few members each; the real tag contents are checked on a
 * server (test S).
 */
class PalmSaplingBlockTest {
    @AfterEach
    void resetTags() {
        BuiltInRegistries.BLOCK.resetTags();
    }

    @Test
    void growsOnVanillaSaplingGroundAndOnDesertPlantSoil() {
        BuiltInRegistries.BLOCK.bindTags(Map.of(
                BlockTags.DIRT, holders(Blocks.DIRT, ModBlocks.DEAD_GRASS_BLOCK.get()),
                ModTags.Blocks.DESERT_PLANT_SOIL, holders(Blocks.SAND, Blocks.RED_SAND, Blocks.TERRACOTTA, ModBlocks.CRACKED_CLAY.get())));

        for (Block ground : List.of(Blocks.DIRT, ModBlocks.DEAD_GRASS_BLOCK.get(), Blocks.FARMLAND,
                Blocks.SAND, Blocks.RED_SAND, Blocks.TERRACOTTA, ModBlocks.CRACKED_CLAY.get())) {
            assertTrue(mayPlaceOn(ground), ground.toString());
        }
        for (Block ground : List.of(Blocks.STONE, ModBlocks.RED_CANYON_STONE.get(), ModBlocks.LOOSE_SAND.get(), Blocks.GRAVEL)) {
            assertFalse(mayPlaceOn(ground), ground.toString());
        }
    }

    @Test
    void withoutTheSoilTagOnlyVanillaGroundCounts() {
        BuiltInRegistries.BLOCK.bindTags(Map.of(BlockTags.DIRT, holders(Blocks.DIRT)));

        assertTrue(mayPlaceOn(Blocks.DIRT));
        assertFalse(mayPlaceOn(Blocks.SAND));
    }

    private static boolean mayPlaceOn(Block ground) {
        return ModBlocks.PALM_SAPLING.get().mayPlaceOn(ground.defaultBlockState(), EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    private static List<Holder<Block>> holders(Block... blocks) {
        return Arrays.stream(blocks).map(BuiltInRegistries.BLOCK::wrapAsHolder).toList();
    }
}
