package nl.timtoys.corruptedadventure.command.terrainmap;

/**
 * Finds the visible ground of one column from a solidity test (final density > 0, as in the noise chunk). Coarse steps
 * first, then block by block above the first solid sample, so a solid layer thinner than the step can be missed (like
 * vanilla's preliminary surface, which also steps by the cell height).
 */
final class ColumnScan {
    @FunctionalInterface
    interface Solid {
        boolean at(int y);
    }

    private ColumnScan() {
    }

    /** The highest solid Y in {@code [minY, maxY]}, scanning down from {@code maxY}; {@code minY - 1} if none. */
    static int top(Solid solid, int minY, int maxY, int step) {
        int previous = maxY + 1;
        for (int y = maxY; y >= minY; y = y > minY ? Math.max(y - step, minY) : minY - 1) {
            if (solid.at(y)) {
                return refineUp(solid, y, previous - 1);
            }
            previous = y;
        }
        return minY - 1;
    }

    /**
     * For a dimension with a ceiling: the floor of the tallest open run in the column (the main cavern rather than a
     * pocket in the roof). {@code maxY} if the column has no open sample; {@code minY - 1} if the run reaches the bottom.
     */
    static int floorOfTallestOpening(Solid solid, int minY, int maxY, int step) {
        int bestLength = 0;
        int bestBottom = 0;
        int runTop = Integer.MIN_VALUE;
        for (int y = maxY; y >= minY; y -= step) {
            if (solid.at(y)) {
                runTop = Integer.MIN_VALUE;
                continue;
            }
            if (runTop == Integer.MIN_VALUE) {
                runTop = y;
            }
            int length = runTop - y + 1;
            if (length > bestLength) {
                bestLength = length;
                bestBottom = y;
            }
        }
        if (bestLength == 0) {
            return maxY;
        }
        // The sample below the run's lowest open sample is solid (or below minY); the floor lies in between.
        int below = bestBottom - step;
        if (below < minY) {
            for (int y = bestBottom - 1; y >= minY; y--) {
                if (solid.at(y)) {
                    return y;
                }
            }
            return minY - 1;
        }
        return refineUp(solid, below, bestBottom - 1);
    }

    /** {@code solidY} is solid; the highest solid Y in {@code [solidY, upTo]}, assuming nothing above {@code upTo} counts. */
    private static int refineUp(Solid solid, int solidY, int upTo) {
        for (int y = upTo; y > solidY; y--) {
            if (solid.at(y)) {
                return y;
            }
        }
        return solidY;
    }
}
