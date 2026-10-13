package nl.timtoys.corruptedadventure.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import nl.timtoys.corruptedadventure.block.GiantLilyPadBlock.Part;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GiantLilyPadBlockTest {
    private static final BlockPos ORIGIN = new BlockPos(10, 63, -20);

    private static BlockState part(Part part) {
        return ModBlocks.GIANT_LILY_PAD.get().defaultBlockState().setValue(GiantLilyPadBlock.PART, part);
    }

    @Test
    void thePartsCoverTwoByTwoBlocksFromTheNorthWestCorner() {
        Set<BlockPos> covered = new HashSet<>();
        for (Part part : Part.values()) {
            BlockPos pos = part.from(ORIGIN);
            covered.add(pos);
            assertEquals(ORIGIN, part.northWest(pos), part.toString());
        }
        assertEquals(Set.of(ORIGIN, ORIGIN.east(), ORIGIN.south(), ORIGIN.south().east()), covered);
        assertEquals(ORIGIN.east(), Part.NORTH_EAST.from(ORIGIN));
        assertEquals(ORIGIN.south(), Part.SOUTH_WEST.from(ORIGIN));
    }

    /** The clicked spot is the corner by the player; the pad runs in the facing and to the right of it. */
    @Test
    void thePadExtendsInTheFacingAndToTheRightOfThePlayer() {
        BlockPos clicked = ORIGIN;
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockPos northWest = GiantLilyPadBlock.northWestCorner(clicked, facing);
            Set<BlockPos> pad = new HashSet<>();
            for (Part part : Part.values()) {
                pad.add(part.from(northWest));
            }
            assertTrue(pad.contains(clicked), facing.toString());
            assertTrue(pad.contains(clicked.relative(facing)), facing.toString());
            assertTrue(pad.contains(clicked.relative(facing.getClockWise())), facing.toString());
            assertFalse(pad.contains(clicked.relative(facing.getOpposite())), facing.toString());
        }
        // Facing north: the clicked spot is the south-west corner.
        assertEquals(ORIGIN.north(), GiantLilyPadBlock.northWestCorner(ORIGIN, Direction.NORTH));
        assertEquals(Part.SOUTH_WEST, Part.at(ORIGIN.north(), ORIGIN));
    }

    @Test
    void eachPartKnowsItsTwoNeighbours() {
        for (Part part : Part.values()) {
            int siblings = 0;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                if (part.hasSibling(direction)) {
                    siblings++;
                    Part sibling = part.sibling(direction);
                    assertEquals(part.from(ORIGIN).relative(direction), sibling.from(ORIGIN), part + " " + direction);
                    assertSame(part, sibling.sibling(direction.getOpposite()));
                }
            }
            assertEquals(2, siblings, part.toString());
            assertFalse(part.hasSibling(Direction.UP));
            assertFalse(part.hasSibling(Direction.DOWN));
        }
    }

    /** A part whose neighbouring part is gone (or replaced by anything else) disappears; the update chain takes the rest. */
    @Test
    void aPartWithoutItsNeighbourDisappears() {
        BlockState northWest = part(Part.NORTH_WEST);
        for (BlockState gone : new BlockState[] {Blocks.AIR.defaultBlockState(), Blocks.WATER.defaultBlockState(),
                Blocks.LILY_PAD.defaultBlockState(), part(Part.SOUTH_EAST)}) {
            assertTrue(northWest.updateShape(Direction.EAST, gone, null, ORIGIN, ORIGIN.east()).isAir(), gone.toString());
        }
        assertTrue(part(Part.SOUTH_EAST).updateShape(Direction.NORTH, Blocks.AIR.defaultBlockState(), null,
                ORIGIN, ORIGIN.north()).isAir());
    }

    /** Turning the pad clockwise moves each quarter one corner on; the parts stay a whole pad. */
    @Test
    void rotatingTurnsEachPartToTheNextCorner() {
        assertEquals(part(Part.NORTH_EAST), part(Part.NORTH_WEST).rotate(Rotation.CLOCKWISE_90));
        assertEquals(part(Part.SOUTH_EAST), part(Part.NORTH_EAST).rotate(Rotation.CLOCKWISE_90));
        assertEquals(part(Part.SOUTH_WEST), part(Part.SOUTH_EAST).rotate(Rotation.CLOCKWISE_90));
        assertEquals(part(Part.NORTH_WEST), part(Part.SOUTH_WEST).rotate(Rotation.CLOCKWISE_90));
        for (Part part : Part.values()) {
            assertSame(part, part.rotate(Rotation.NONE));
            assertSame(part.rotate(Rotation.CLOCKWISE_180), part.rotate(Rotation.CLOCKWISE_90).rotate(Rotation.CLOCKWISE_90));
            assertSame(part, part.rotate(Rotation.CLOCKWISE_90).rotate(Rotation.COUNTERCLOCKWISE_90));
        }
        Set<Part> turned = new HashSet<>();
        for (Part part : Part.values()) {
            turned.add(part.rotate(Rotation.CLOCKWISE_90));
        }
        assertEquals(Set.of(Part.values()), turned);
    }

    /** Left-right swaps north and south, front-back swaps east and west (vanilla's mirror of directions). */
    @Test
    void mirroringReflectsEachPart() {
        assertEquals(part(Part.SOUTH_WEST), part(Part.NORTH_WEST).mirror(Mirror.LEFT_RIGHT));
        assertEquals(part(Part.NORTH_EAST), part(Part.NORTH_WEST).mirror(Mirror.FRONT_BACK));
        assertEquals(part(Part.NORTH_EAST), part(Part.SOUTH_EAST).mirror(Mirror.LEFT_RIGHT));
        for (Part part : Part.values()) {
            assertSame(part, part.mirror(Mirror.NONE));
            assertSame(part, part.mirror(Mirror.LEFT_RIGHT).mirror(Mirror.LEFT_RIGHT));
        }
    }

    @Test
    void thePadActsLikeALilyPad() {
        BlockState state = part(Part.NORTH_WEST);
        assertSame(Part.NORTH_WEST, ModBlocks.GIANT_LILY_PAD.get().defaultBlockState().getValue(GiantLilyPadBlock.PART));
        assertEquals(PushReaction.DESTROY, state.getPistonPushReaction());
        assertFalse(state.isRandomlyTicking());
        assertSame(GiantLilyPadBlock.CODEC, ((GiantLilyPadBlock) state.getBlock()).codec());
    }
}
