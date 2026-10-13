package nl.timtoys.corruptedadventure.block;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuddingCrystalBlockTest {
    private static final BuddingCrystalBlock TOXIC = ModBlocks.BUDDING_TOXIC_CRYSTAL.get();
    private static final BuddingCrystalBlock AQUAMARINE = ModBlocks.BUDDING_AQUAMARINE.get();

    private static BlockState stage(BuddingCrystalBlock budding, int index, Direction facing, boolean waterlogged) {
        return budding.stages().get(index).defaultBlockState().setValue(AmethystClusterBlock.FACING, facing)
                .setValue(AmethystClusterBlock.WATERLOGGED, waterlogged);
    }

    @Test
    void eachBuddingBlockGrowsItsOwnFourStages() {
        assertEquals(List.of(ModBlocks.SMALL_TOXIC_CRYSTAL_BUD.get(), ModBlocks.MEDIUM_TOXIC_CRYSTAL_BUD.get(),
                ModBlocks.LARGE_TOXIC_CRYSTAL_BUD.get(), ModBlocks.TOXIC_CRYSTAL_CLUSTER.get()), TOXIC.stages());
        assertEquals(List.of(ModBlocks.SMALL_AQUAMARINE_BUD.get(), ModBlocks.MEDIUM_AQUAMARINE_BUD.get(),
                ModBlocks.LARGE_AQUAMARINE_BUD.get(), ModBlocks.AQUAMARINE_CLUSTER.get()), AQUAMARINE.stages());
    }

    @Test
    void airGrowsASmallBudFacingAwayAndAWaterSourceAWaterloggedOne() {
        for (Direction direction : Direction.values()) {
            assertEquals(stage(TOXIC, 0, direction, false), TOXIC.grownState(Blocks.AIR.defaultBlockState(), direction));
            assertEquals(stage(AQUAMARINE, 0, direction, true), AQUAMARINE.grownState(Blocks.WATER.defaultBlockState(), direction));
        }
        assertNull(AQUAMARINE.grownState(Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 3), Direction.UP));
    }

    @Test
    void eachStageBecomesTheNextKeepingFacingAndWater() {
        for (boolean waterlogged : new boolean[] {false, true}) {
            for (int i = 0; i < 3; i++) {
                assertEquals(stage(AQUAMARINE, i + 1, Direction.SOUTH, waterlogged),
                        AQUAMARINE.grownState(stage(AQUAMARINE, i, Direction.SOUTH, waterlogged), Direction.SOUTH));
            }
        }
    }

    @Test
    void theClusterBudsFacingElsewhereAndOtherCrystalsDoNotGrow() {
        assertNull(TOXIC.grownState(stage(TOXIC, 3, Direction.UP, false), Direction.UP));
        assertNull(TOXIC.grownState(stage(TOXIC, 1, Direction.UP, false), Direction.DOWN));
        // Aquamarine buds next to a toxic budding block stay aquamarine.
        assertNull(TOXIC.grownState(stage(AQUAMARINE, 0, Direction.UP, false), Direction.UP));
        for (Block other : List.of(Blocks.STONE, Blocks.SMALL_AMETHYST_BUD, Blocks.LAVA)) {
            assertNull(TOXIC.grownState(other.defaultBlockState(), Direction.UP), other.toString());
        }
    }

    @Test
    void buddingBlocksTickAreBrokenByPistonsAndKeepTheirStagesInTheCodec() {
        for (BuddingCrystalBlock budding : List.of(TOXIC, AQUAMARINE)) {
            BlockState state = budding.defaultBlockState();
            assertTrue(state.isRandomlyTicking());
            assertEquals(PushReaction.DESTROY, state.getPistonPushReaction());
            JsonElement json = BuddingCrystalBlock.CODEC.codec().encodeStart(JsonOps.INSTANCE, budding).getOrThrow();
            List<String> stages = new ArrayList<>();
            json.getAsJsonObject().getAsJsonArray("stages").forEach(stage -> stages.add(stage.getAsString()));
            assertEquals(budding.stages().stream().map(stage -> BuiltInRegistries.BLOCK.getKey(stage).toString()).toList(), stages);
        }
    }
}
