package nl.timtoys.corruptedadventure.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DewSourceBlockTest {
    private static final List<Block> STAGES = List.of(ModBlocks.SMALL_DEW_CRYSTAL_BUD.get(),
            ModBlocks.MEDIUM_DEW_CRYSTAL_BUD.get(), ModBlocks.LARGE_DEW_CRYSTAL_BUD.get(), ModBlocks.DEW_CRYSTAL_CLUSTER.get());

    private static BlockState stage(int index, Direction facing, boolean waterlogged) {
        return STAGES.get(index).defaultBlockState().setValue(AmethystClusterBlock.FACING, facing)
                .setValue(AmethystClusterBlock.WATERLOGGED, waterlogged);
    }

    @Test
    void airGrowsASmallBudFacingAwayFromTheSource() {
        for (Direction direction : Direction.values()) {
            assertEquals(stage(0, direction, false), DewSourceBlock.grownState(Blocks.AIR.defaultBlockState(), direction));
        }
    }

    @Test
    void aFullWaterSourceGrowsAWaterloggedBudButFlowingWaterDoesNot() {
        assertEquals(stage(0, Direction.NORTH, true), DewSourceBlock.grownState(Blocks.WATER.defaultBlockState(), Direction.NORTH));
        BlockState flowing = Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 3);
        assertNull(DewSourceBlock.grownState(flowing, Direction.NORTH));
    }

    @Test
    void eachStageBecomesTheNextKeepingFacingAndWaterlogging() {
        for (boolean waterlogged : new boolean[] {false, true}) {
            for (int i = 0; i < 3; i++) {
                assertEquals(stage(i + 1, Direction.WEST, waterlogged),
                        DewSourceBlock.grownState(stage(i, Direction.WEST, waterlogged), Direction.WEST));
            }
        }
    }

    @Test
    void theClusterAndBudsFacingElsewhereDoNotGrow() {
        assertNull(DewSourceBlock.grownState(stage(3, Direction.UP, false), Direction.UP));
        assertNull(DewSourceBlock.grownState(stage(0, Direction.UP, false), Direction.DOWN));
        assertNull(DewSourceBlock.grownState(stage(2, Direction.EAST, false), Direction.NORTH));
    }

    @Test
    void otherBlocksDoNotGrow() {
        for (BlockState other : List.of(Blocks.LAVA.defaultBlockState(), Blocks.STONE.defaultBlockState(),
                ModBlocks.DEW_SOURCE.get().defaultBlockState(), Blocks.SMALL_AMETHYST_BUD.defaultBlockState())) {
            assertNull(DewSourceBlock.grownState(other, Direction.UP), other.toString());
        }
    }

    @Test
    void sourceTicksRandomlyAndIsPushedAway() {
        BlockState source = ModBlocks.DEW_SOURCE.get().defaultBlockState();
        assertTrue(source.isRandomlyTicking());
        assertEquals(PushReaction.DESTROY, source.getPistonPushReaction());
        assertEquals(0, source.getLightEmission());
        assertSame(DewSourceBlock.CODEC, ((DewSourceBlock) source.getBlock()).codec());
    }

    @Test
    void stagesStandUpDryAndGlowLikeAmethyst() {
        int[] light = {1, 2, 4, 5};
        for (int i = 0; i < STAGES.size(); i++) {
            BlockState state = STAGES.get(i).defaultBlockState();
            assertNotNull(state);
            assertEquals(Direction.UP, state.getValue(AmethystClusterBlock.FACING));
            assertFalse(state.getValue(AmethystClusterBlock.WATERLOGGED));
            assertFalse(state.isRandomlyTicking());
            assertEquals(light[i], state.getLightEmission(), STAGES.get(i).toString());
        }
    }
}
