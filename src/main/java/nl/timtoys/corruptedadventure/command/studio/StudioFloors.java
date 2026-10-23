package nl.timtoys.corruptedadventure.command.studio;

import java.util.ArrayList;
import java.util.List;

/**
 * Floors for the cut-aways (S0-2), from the structure's own blocks. Per layer only the columns that go on above it
 * count (structure blocks both at or below and above the layer): a layer is floor-like when at least
 * {@link #SOLID_SHARE} of those columns is solid there, and they cover at least {@link #MIN_SPAN_SHARE} of the
 * footprint. So low side pieces (tents, cages) do not dilute a tower's floors, and a roof's eaves, which end at their
 * layer, do not count. Consecutive floor-like layers are one floor (a thick slab); its walking surface is the group's
 * top layer. A group with fewer than {@link #HEADROOM} layers of the structure above it is the roof, not a floor.
 * Without any floor the structure's lowest layer counts as the ground floor, so every structure gets at least
 * one cut-away.
 */
final class StudioFloors {
    static final double SOLID_SHARE = 0.8;
    /** A floor carries at least this share of the footprint (no "floor" in a lone pillar or spire). */
    static final double MIN_SPAN_SHARE = 0.1;
    static final int HEADROOM = 3;
    /** A cut-away keeps the floor and this many layers above it (furniture, the lower half of the walls). */
    static final int KEEP_ABOVE = 2;

    private StudioFloors() {
    }

    /**
     * @param minY      Y of index 0
     * @param solid     per layer, bottom up: columns that go on above the layer and are solid at it
     * @param spanning  per layer: columns that go on above the layer (a structure block at or below it and one above)
     * @param footprint number of columns with any structure block
     * @return the walking surface Y of every floor, bottom up
     */
    static List<Integer> detect(int minY, int[] solid, int[] spanning, int footprint) {
        List<Integer> floors = new ArrayList<>();
        if (footprint <= 0 || solid.length == 0) {
            return floors;
        }
        int maxY = minY + solid.length - 1;
        int top = Integer.MIN_VALUE;
        for (int i = 0; i <= solid.length; i++) {
            boolean floorLike = i < solid.length && spanning[i] >= MIN_SPAN_SHARE * footprint
                    && solid[i] >= SOLID_SHARE * spanning[i];
            if (floorLike) {
                top = minY + i;
            } else if (top != Integer.MIN_VALUE) {
                if (top + HEADROOM <= maxY) {
                    floors.add(top);
                }
                top = Integer.MIN_VALUE;
            }
        }
        if (floors.isEmpty()) {
            floors.add(minY);
        }
        return floors;
    }

    /** The highest kept layer of each floor's cut-away. */
    static List<Integer> cuts(List<Integer> floors) {
        return floors.stream().map(y -> y + KEEP_ABOVE).toList();
    }
}
