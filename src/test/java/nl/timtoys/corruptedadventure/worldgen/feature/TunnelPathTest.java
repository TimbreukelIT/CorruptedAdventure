package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TunnelPathTest {
    private static final UniformInt FIRST = UniformInt.of(12, 28);
    private static final UniformInt SECOND = UniformInt.of(6, 14);

    @Test
    void sameSeedGivesTheSamePath() {
        for (long seed = 0; seed < 50; seed++) {
            TunnelPath path = TunnelPath.random(new XoroshiroRandomSource(seed), FIRST, SECOND, 0.5F);
            TunnelPath again = TunnelPath.random(new XoroshiroRandomSource(seed), FIRST, SECOND, 0.5F);
            assertEquals(path, again);
            BlockPos origin = new BlockPos(37, 20, -91);
            assertEquals(path.mouthCandidates(origin), again.mouthCandidates(origin));
        }
    }

    @Test
    void lengthsStayWithinTheProvidersAndTurnsAreRightAngles() {
        int turns = 0;
        for (long seed = 0; seed < 500; seed++) {
            TunnelPath path = TunnelPath.random(new XoroshiroRandomSource(seed), FIRST, SECOND, 0.5F);
            assertTrue(path.direction().getAxis().isHorizontal());
            assertTrue(path.firstLength() >= 12 && path.firstLength() <= 28, "first " + path.firstLength());
            if (path.turn().isPresent()) {
                turns++;
                Direction turn = path.turn().get();
                assertTrue(turn == path.direction().getClockWise() || turn == path.direction().getCounterClockWise());
                assertTrue(path.secondLength() >= 6 && path.secondLength() <= 14, "second " + path.secondLength());
            } else {
                assertEquals(0, path.secondLength());
            }
        }
        assertTrue(turns > 150 && turns < 350, "turns " + turns);
    }

    @Test
    void segmentsAreStraightLevelAndConnected() {
        for (long seed = 0; seed < 200; seed++) {
            TunnelPath path = TunnelPath.random(new XoroshiroRandomSource(seed), FIRST, SECOND, 0.5F);
            BlockPos mouth = new BlockPos(5, 40, 9);
            List<BlockPos> cells = path.cells(mouth);
            assertEquals(path.firstLength() + path.secondLength(), cells.size());
            assertEquals(mouth, cells.getFirst());
            for (int i = 1; i < cells.size(); i++) {
                BlockPos step = cells.get(i).subtract(cells.get(i - 1));
                Direction expected = i < path.firstLength() ? path.direction() : path.turn().orElseThrow();
                assertEquals(expected.getNormal(), step, "step " + i + " of seed " + seed);
                assertEquals(mouth.getY(), cells.get(i).getY());
            }
        }
    }

    @Test
    void everyCandidateKeepsTheTunnelInsideTheFeatureArea() {
        RandomSource origins = new XoroshiroRandomSource(42L);
        for (long seed = 0; seed < 300; seed++) {
            TunnelPath path = TunnelPath.random(new XoroshiroRandomSource(seed), UniformInt.of(1, TunnelPath.MAX_FIRST_LENGTH),
                    UniformInt.of(1, TunnelPath.MAX_SECOND_LENGTH), 0.5F);
            BlockPos origin = new BlockPos(origins.nextInt(2000) - 1000, 0, origins.nextInt(2000) - 1000);
            List<BlockPos> candidates = path.mouthCandidates(origin);
            assertFalse(candidates.isEmpty(), "no mouth for seed " + seed);
            for (BlockPos mouth : candidates) {
                assertEquals(0, mouth.subtract(origin).get(path.direction().getClockWise().getAxis()), "mouth off the line");
                assertInsideWithNeighbours(origin, mouth.relative(path.direction().getOpposite()), false);
                for (BlockPos cell : path.cells(mouth)) {
                    assertInsideWithNeighbours(origin, cell, true);
                }
            }
            // Ordered along the heading.
            for (int i = 1; i < candidates.size(); i++) {
                assertEquals(path.direction().getNormal(), candidates.get(i).subtract(candidates.get(i - 1)));
            }
        }
    }

    /** A feature may touch the 3×3 chunks around its origin's chunk; cells are also read one block around them. */
    private static void assertInsideWithNeighbours(BlockPos origin, BlockPos pos, boolean neighbours) {
        int reach = neighbours ? 1 : 0;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                int chunkX = SectionPos.blockToSectionCoord(pos.getX() + dx);
                int chunkZ = SectionPos.blockToSectionCoord(pos.getZ() + dz);
                assertTrue(Math.abs(chunkX - SectionPos.blockToSectionCoord(origin.getX())) <= 1
                        && Math.abs(chunkZ - SectionPos.blockToSectionCoord(origin.getZ())) <= 1, pos + " from origin " + origin);
            }
        }
    }
}
