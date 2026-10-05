package nl.timtoys.corruptedadventure.block;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The stalactite is a copy of vanilla pointed dripstone: same properties (the blockstate JSON and worldgen rely on the
 * names and values) and the same shape per direction and thickness. The behaviour that reads a world (merging,
 * thickness, falling) is tested over RCON on a server (test S).
 */
class StalactiteBlockTest {
    private static final Block STALACTITE = ModBlocks.SLIME_STALACTITE.get();

    @Test
    void hasTheSamePropertiesAsPointedDripstone() {
        assertEquals(propertyNames(Blocks.POINTED_DRIPSTONE), propertyNames(STALACTITE));
        assertEquals(List.of("thickness", "vertical_direction", "waterlogged"), propertyNames(STALACTITE));
        assertEquals(Blocks.POINTED_DRIPSTONE.getStateDefinition().getPossibleStates().size(),
                STALACTITE.getStateDefinition().getPossibleStates().size());
    }

    @Test
    void hasTheSameShapePerDirectionAndThickness() {
        for (BlockState vanilla : Blocks.POINTED_DRIPSTONE.getStateDefinition().getPossibleStates()) {
            BlockState ours = STALACTITE.defaultBlockState()
                    .setValue(StalactiteBlock.TIP_DIRECTION, vanilla.getValue(StalactiteBlock.TIP_DIRECTION))
                    .setValue(StalactiteBlock.THICKNESS, vanilla.getValue(StalactiteBlock.THICKNESS))
                    .setValue(StalactiteBlock.WATERLOGGED, vanilla.getValue(StalactiteBlock.WATERLOGGED));
            assertFalse(Shapes.joinIsNotEmpty(
                    vanilla.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO), ours.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO),
                    BooleanOp.NOT_SAME), ours.toString());
        }
    }

    @Test
    void usesTheVanillaThicknessValuesAndDoesNotRandomlyTick() {
        assertEquals(List.of(DripstoneThickness.values()), List.copyOf(StalactiteBlock.THICKNESS.getPossibleValues()));
        assertFalse(STALACTITE.defaultBlockState().isRandomlyTicking());
        assertEquals(DripstoneThickness.TIP, STALACTITE.defaultBlockState().getValue(StalactiteBlock.THICKNESS));
    }

    @Test
    void codecEncodesAndIsItsOwn() {
        StalactiteBlock block = (StalactiteBlock) STALACTITE;
        assertSame(StalactiteBlock.CODEC, block.codec());
        // Decoding would construct a block, which the frozen registry refuses in a unit test.
        JsonElement json = StalactiteBlock.CODEC.codec().encodeStart(JsonOps.INSTANCE, block).getOrThrow();
        assertTrue(json.isJsonObject());
    }

    private static List<String> propertyNames(Block block) {
        return block.getStateDefinition().getProperties().stream().map(Property::getName).toList();
    }
}
