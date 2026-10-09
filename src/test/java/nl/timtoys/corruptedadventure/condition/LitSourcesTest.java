package nl.timtoys.corruptedadventure.condition;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
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
 * Heat and warmth sources only count while lit (package 55b). Unit tests have no datapack, so the tags are bound by hand
 * with part of their datagen contents.
 */
class LitSourcesTest {
    @BeforeEach
    void bindSources() {
        BuiltInRegistries.BLOCK.bindTags(Map.of(
                ModTags.Blocks.HEAT_SOURCES, holders(Blocks.LAVA, Blocks.MAGMA_BLOCK, Blocks.CAMPFIRE, ModBlocks.SMOLDERING_STONE.get()),
                ModTags.Blocks.WARMTH_SOURCES, holders(Blocks.LAVA, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE)));
    }

    @AfterEach
    void resetTags() {
        BuiltInRegistries.BLOCK.resetTags();
    }

    @Test
    void aPutOutCampfireIsNoHeatSource() {
        assertTrue(ConditionContext.isHeatSource(campfire(Blocks.CAMPFIRE, true)));
        assertFalse(ConditionContext.isHeatSource(campfire(Blocks.CAMPFIRE, false)));
    }

    @Test
    void blocksWithoutALitStateAlwaysCount() {
        for (BlockState state : List.of(Blocks.LAVA.defaultBlockState(), Blocks.MAGMA_BLOCK.defaultBlockState(),
                ModBlocks.SMOLDERING_STONE.get().defaultBlockState())) {
            assertTrue(ConditionContext.isHeatSource(state), state.toString());
        }
        assertFalse(ConditionContext.isHeatSource(Blocks.STONE.defaultBlockState()));
        // The tag decides, not the lit state: a lit soul campfire is warmth, but no heat.
        assertFalse(ConditionContext.isHeatSource(campfire(Blocks.SOUL_CAMPFIRE, true)));
    }

    @Test
    void warmthKeepsTheSameRule() {
        assertTrue(ConditionContext.isWarmthSource(campfire(Blocks.SOUL_CAMPFIRE, true)));
        assertFalse(ConditionContext.isWarmthSource(campfire(Blocks.SOUL_CAMPFIRE, false)));
        assertFalse(ConditionContext.isWarmthSource(campfire(Blocks.CAMPFIRE, false)));
        assertTrue(ConditionContext.isWarmthSource(Blocks.LAVA.defaultBlockState()));
    }

    private static BlockState campfire(Block block, boolean lit) {
        return block.defaultBlockState().setValue(CampfireBlock.LIT, lit);
    }

    private static List<Holder<Block>> holders(Block... blocks) {
        return Arrays.stream(blocks).map(BuiltInRegistries.BLOCK::wrapAsHolder).toList();
    }
}
