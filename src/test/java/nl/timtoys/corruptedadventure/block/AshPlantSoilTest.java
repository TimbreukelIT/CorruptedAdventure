package nl.timtoys.corruptedadventure.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModTags;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ground rule of the fire flora. Unit tests have no datapack, so {@code #ash_plant_soil} is bound by hand with its
 * datagen contents; the real tag is checked on a server (test S).
 */
class AshPlantSoilTest {
    @BeforeEach
    void bindSoil() {
        BuiltInRegistries.BLOCK.bindTags(Map.of(ModTags.Blocks.ASH_PLANT_SOIL,
                holders(ModBlocks.ASH_BLOCK.get(), Blocks.COARSE_DIRT, ModBlocks.ASH_LAYER.get())));
    }

    @AfterEach
    void resetTags() {
        BuiltInRegistries.BLOCK.resetTags();
    }

    @Test
    void fireFloraGrowsOnAshCoarseDirtAndAFullAshLayerOnly() {
        BlockState fullLayer = layer(SnowLayerBlock.MAX_HEIGHT);
        for (BlockState ground : List.of(ModBlocks.ASH_BLOCK.get().defaultBlockState(), Blocks.COARSE_DIRT.defaultBlockState(), fullLayer)) {
            assertTrue(ModBlocks.FIRE_BLOOM.get().mayPlaceOn(ground, EmptyBlockGetter.INSTANCE, BlockPos.ZERO), ground.toString());
            assertTrue(ModBlocks.ASH_SPROUTS.get().mayPlaceOn(ground, EmptyBlockGetter.INSTANCE, BlockPos.ZERO), ground.toString());
            assertTrue(ModBlocks.EMBER_BERRY_BUSH.get().mayPlaceOn(ground, EmptyBlockGetter.INSTANCE, BlockPos.ZERO), ground.toString());
        }
        // A partial layer is no full face: the plant would float above the ash.
        for (BlockState ground : List.of(layer(1), layer(7), Blocks.STONE.defaultBlockState(), Blocks.GRASS_BLOCK.defaultBlockState(),
                Blocks.DIRT.defaultBlockState(), ModBlocks.ASH_CRUST.get().defaultBlockState(), Blocks.MOSS_BLOCK.defaultBlockState())) {
            assertFalse(ModBlocks.FIRE_BLOOM.get().mayPlaceOn(ground, EmptyBlockGetter.INSTANCE, BlockPos.ZERO), ground.toString());
            assertFalse(ModBlocks.ASH_SPROUTS.get().mayPlaceOn(ground, EmptyBlockGetter.INSTANCE, BlockPos.ZERO), ground.toString());
        }
    }

    /** The berry bush also grows on moss: the green floor of the dormant caldera (V7 = A). */
    @Test
    void berryBushAlsoGrowsOnMossButNotOnGrass() {
        assertTrue(ModBlocks.EMBER_BERRY_BUSH.get().mayPlaceOn(Blocks.MOSS_BLOCK.defaultBlockState(), EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
        assertFalse(ModBlocks.EMBER_BERRY_BUSH.get().mayPlaceOn(Blocks.GRASS_BLOCK.defaultBlockState(), EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
        assertFalse(ModBlocks.EMBER_BERRY_BUSH.get().mayPlaceOn(layer(3), EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
    }

    private static BlockState layer(int layers) {
        return ModBlocks.ASH_LAYER.get().defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers);
    }

    private static List<Holder<Block>> holders(Block... blocks) {
        return Arrays.stream(blocks).map(BuiltInRegistries.BLOCK::wrapAsHolder).toList();
    }
}
