package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingPosition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test helper: the blocks of ring 4's terrain and water near the surface as the stack and the aquifer make them, from the
 * field alone (the waterscan's rule offline: water never against air, but in the recorded falls). Solid: the per-block
 * {@code thin} where it decides, else below the surface (exact on the descent of verdant_falls, the 4-block grid's
 * elsewhere; vanilla's caves left out: the descent closes them). Water: a spring, a fall's column, a lake above the sea
 * from the exact floor up, the sea (Y ≤ 62) where ring 4's half has it; ring 3's half is sealed below Y 63.
 */
final class WaterScan {
    static final int AIR = 0;
    static final int SOLID = 1;
    static final int WATER = 2;
    static final int FALL = 3;

    private final IslandField field;
    private final RingLayout layout;
    final Set<Long> exempt = new HashSet<>();
    int water;

    WaterScan(IslandField field, RingLayout layout) {
        this.field = field;
        this.layout = layout;
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    /** The block at (x, y, z). */
    int block(int x, int y, int z) {
        IslandField.Column c = field.column(x, z);
        double surface = c.verdant ? c.surface() : field.gridSurface(x, z);
        c = field.column(x, z);
        int springY = c.springY();
        int fallTop = c.fallTop();
        int fallBottom = c.fallBottom();
        int lakeTop = c.lakeTop();
        double exactFloor = c.exactFloor();
        int guard = c.caveLiftFrom();
        double thin = field.thin(x, y, z);
        boolean solid = thin > 0.0 || thin == 0.0 && y < surface;
        if (solid) {
            return SOLID;
        }
        RingPosition position = layout.sampleOverworld(x, z);
        boolean ring3Half = position.ring() == Ring.RING_3;
        if (ring3Half && position.inTransition() && y < 63) {
            return SOLID;
        }
        if (y <= fallTop && y >= fallBottom) {
            return FALL;
        }
        if (y == springY) {
            return WATER;
        }
        if (lakeTop != Integer.MIN_VALUE) {
            int floor = (int) Math.floor(Double.isNaN(exactFloor) ? field.gridSurface(x, z) : exactFloor) - 1;
            if (y < floor && y >= guard) {
                return SOLID;
            }
            if (y < lakeTop && y >= floor) {
                return WATER;
            }
        }
        if (!ring3Half && y <= 62) {
            return WATER;
        }
        return AIR;
    }

    /**
     * The water blocks in the box (columns, Y 40–140) with air beside or below them, outside the falls' columns and the
     * four neighbours of each fall's top block (the spring among them), as "x y z".
     */
    List<String> scan(int minX, int minZ, int maxX, int maxZ) {
        List<String> over = new ArrayList<>();
        for (IslandField.Waterfall fall : field.waterfallsWithin(minX - 16, minZ - 16, maxX + 16, maxZ + 16)) {
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                exempt.add(key(fall.x() + d[0], fall.top(), fall.z() + d[1]));
            }
        }
        for (int cx = minX; cx <= maxX; cx += 16) {
            for (int cz = minZ; cz <= maxZ; cz += 16) {
                for (int x = cx; x < Math.min(maxX + 1, cx + 16); x++) {
                    for (int z = cz; z < Math.min(maxZ + 1, cz + 16); z++) {
                        IslandField.Column c = field.column(x, z);
                        int hi = Math.max(c.lakeTop(), Math.max(c.springY(), 63));
                        int lo = (int) Math.floor(c.lowestSurface()) - 2;
                        if (c.lakeTop() == Integer.MIN_VALUE && c.springY() == Integer.MIN_VALUE && c.surface() >= 63.0) {
                            continue;
                        }
                        for (int y = Math.max(40, lo); y <= Math.min(140, hi); y++) {
                            int here = block(x, y, z);
                            if (here != WATER) {
                                continue;
                            }
                            water++;
                            if (exempt.contains(key(x, y, z))) {
                                continue;
                            }
                            boolean exposed = block(x, y - 1, z) == AIR || block(x + 1, y, z) == AIR || block(x - 1, y, z) == AIR
                                    || block(x, y, z + 1) == AIR || block(x, y, z - 1) == AIR;
                            if (exposed) {
                                over.add(x + " " + y + " " + z);
                            }
                        }
                    }
                }
            }
        }
        return over;
    }
}
