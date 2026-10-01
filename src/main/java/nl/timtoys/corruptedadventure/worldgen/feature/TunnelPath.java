package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shape of an unnatural tunnel, without a level: a straight, perfectly level first segment and at most one right-angle
 * turn into a second segment. The tunnel is two blocks high; its cells are the floor blocks.
 *
 * @param direction    heading of the first segment, from the mouth into the rock
 * @param turn         heading of the second segment, if the tunnel turns
 * @param firstLength  cells in the first segment, the mouth included
 * @param secondLength cells in the second segment after the corner; 0 without a turn
 */
public record TunnelPath(Direction direction, Optional<Direction> turn, int firstLength, int secondLength) {
    public static final int MAX_FIRST_LENGTH = 32;
    public static final int MAX_SECOND_LENGTH = 15;
    /**
     * A feature may touch the 3×3 chunks around its origin's chunk. Cells keep this distance from that edge so their
     * neighbours can be checked as well.
     */
    private static final int EDGE_MARGIN = 1;
    /** Further than any mouth can lie from the origin inside the 3×3 chunks. */
    private static final int SEARCH_REACH = 48;

    public static TunnelPath random(RandomSource random, IntProvider firstLength, IntProvider secondLength, float turnChance) {
        Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int first = firstLength.sample(random);
        if (random.nextFloat() < turnChance) {
            Direction turn = random.nextBoolean() ? direction.getClockWise() : direction.getCounterClockWise();
            return new TunnelPath(direction, Optional.of(turn), first, secondLength.sample(random));
        }
        return new TunnelPath(direction, Optional.empty(), first, 0);
    }

    /** Floor cells from the mouth inwards. */
    public List<BlockPos> cells(BlockPos mouth) {
        List<BlockPos> cells = new ArrayList<>(firstLength + secondLength);
        for (int i = 0; i < firstLength; i++) {
            cells.add(mouth.relative(direction, i));
        }
        BlockPos corner = cells.getLast();
        turn.ifPresent(side -> {
            for (int i = 1; i <= secondLength; i++) {
                cells.add(corner.relative(side, i));
            }
        });
        return cells;
    }

    /**
     * Mouths on the line through {@code origin} along the direction, in that order, for which the open block in front
     * of the mouth and every cell stay inside the 3×3 chunks around the origin's chunk, one block from their edge.
     */
    public List<BlockPos> mouthCandidates(BlockPos origin) {
        List<BlockPos> candidates = new ArrayList<>();
        for (int offset = -SEARCH_REACH; offset <= SEARCH_REACH; offset++) {
            BlockPos mouth = origin.relative(direction, offset);
            // Both segments are straight, so their ends bound the whole tunnel.
            BlockPos corner = mouth.relative(direction, firstLength - 1);
            BlockPos end = turn.map(side -> corner.relative(side, secondLength)).orElse(corner);
            if (fits(origin, mouth.relative(direction.getOpposite())) && fits(origin, corner) && fits(origin, end)) {
                candidates.add(mouth);
            }
        }
        return candidates;
    }

    /** Whether {@code pos} lies inside the 3×3 chunks around the origin's chunk, at least {@link #EDGE_MARGIN} from the edge. */
    static boolean fits(BlockPos origin, BlockPos pos) {
        return fits(origin.getX(), pos.getX()) && fits(origin.getZ(), pos.getZ());
    }

    private static boolean fits(int origin, int value) {
        int chunkStart = SectionPos.blockToSectionCoord(origin) * SectionPos.SECTION_SIZE;
        return value >= chunkStart - SectionPos.SECTION_SIZE + EDGE_MARGIN
                && value <= chunkStart + 2 * SectionPos.SECTION_SIZE - 1 - EDGE_MARGIN;
    }
}
