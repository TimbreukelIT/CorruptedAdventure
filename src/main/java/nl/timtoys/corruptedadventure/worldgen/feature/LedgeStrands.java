package nl.timtoys.corruptedadventure.worldgen.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

/**
 * The pure column scan of {@link LedgeStrandFeature}: the overhangs in one column, top down. An overhang is a block
 * with a sturdy underside and air right below it; its gap is the air below, down to the next block that is not air
 * (the next ledge, the floor or water).
 */
public final class LedgeStrands {
    /** {@link Overhang#floor} when the gap runs below the lowest Y scanned: no floor known. */
    public static final int NO_FLOOR = Integer.MIN_VALUE;

    private LedgeStrands() {
    }

    /**
     * One overhang.
     *
     * @param underside the block with the sturdy underside (the ledge)
     * @param floor     the first block below the gap that is not air, or {@link #NO_FLOOR}
     */
    public record Overhang(int underside, int floor) {
        /** The air blocks between the ledge and the floor; 0 without a floor. */
        public int gap() {
            return floor == NO_FLOOR ? 0 : underside - floor - 1;
        }
    }

    /**
     * The overhangs of a column from {@code top} (a terrain block) down to {@code minY}.
     *
     * @param air        whether the block at a Y is air
     * @param sturdyDown whether the block at a Y (not air) has a sturdy face downwards
     */
    public static List<Overhang> overhangs(int top, int minY, IntPredicate air, IntPredicate sturdyDown) {
        List<Overhang> found = new ArrayList<>();
        boolean solidAbove = !air.test(top);
        for (int y = top - 1; y >= minY; y--) {
            boolean isAir = air.test(y);
            if (isAir && solidAbove && sturdyDown.test(y + 1)) {
                int bottom = y;
                while (bottom - 1 >= minY && air.test(bottom - 1)) {
                    bottom--;
                }
                found.add(new Overhang(y + 1, bottom - 1 >= minY ? bottom - 1 : NO_FLOOR));
                // The loop goes on at the floor, under air.
                y = bottom;
                solidAbove = false;
                continue;
            }
            solidAbove = !isAir;
        }
        return found;
    }
}
