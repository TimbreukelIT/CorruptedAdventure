package nl.timtoys.corruptedadventure.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The ash layer is vanilla's snow layer in shape and stacking, but never melts (no random ticks). */
class AshLayerTest {
    @Test
    void everyLayerHasTheSnowLayerShapes() {
        for (int layers = 1; layers <= SnowLayerBlock.MAX_HEIGHT; layers++) {
            BlockState ash = ModBlocks.ASH_LAYER.get().defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers);
            BlockState snow = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers);
            assertEquals(snow.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO),
                    ash.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO), "shape, layers " + layers);
            assertEquals(snow.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty()),
                    ash.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty()), "collision, layers " + layers);
            assertEquals(snow.getBlockSupportShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO),
                    ash.getBlockSupportShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO), "support, layers " + layers);
        }
    }

    @Test
    void startsAsOneLayerAndHasOnlyTheLayersProperty() {
        BlockState ash = ModBlocks.ASH_LAYER.get().defaultBlockState();
        assertEquals(1, ash.getValue(SnowLayerBlock.LAYERS));
        assertEquals(Blocks.SNOW.getStateDefinition().getProperties(), ModBlocks.ASH_LAYER.get().getStateDefinition().getProperties());
        assertEquals(SnowLayerBlock.MAX_HEIGHT, ModBlocks.ASH_LAYER.get().getStateDefinition().getPossibleStates().size());
    }

    /** Vanilla's snow layer only melts from a random tick; without them the ash stays, whatever the light. */
    @Test
    void neverTicksSoItNeverMelts() {
        assertTrue(Blocks.SNOW.defaultBlockState().isRandomlyTicking());
        for (BlockState state : ModBlocks.ASH_LAYER.get().getStateDefinition().getPossibleStates()) {
            assertFalse(state.isRandomlyTicking(), state.toString());
        }
    }

    @Test
    void aFullStackBlocksTheViewLikeSnow() {
        BlockState full = ModBlocks.ASH_LAYER.get().defaultBlockState().setValue(SnowLayerBlock.LAYERS, SnowLayerBlock.MAX_HEIGHT);
        assertTrue(full.isViewBlocking(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
        assertFalse(ModBlocks.ASH_LAYER.get().defaultBlockState().isViewBlocking(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
    }
}
