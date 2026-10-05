package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GiantFungusShapeTest {
    private static final int FLOOR = 28;
    private static final int CEILING = 108;

    /** A hall with a slightly uneven floor and a flat ceiling. */
    private static final GiantFungusShape.Ground HALL = new GiantFungusShape.Ground() {
        @Override
        public int floor(int x, int fromY, int z) {
            return fromY <= FLOOR - 2 ? FungalTerrainScan.NONE : FLOOR + Math.floorMod(x * 31 + z * 17, 3) - 1;
        }

        @Override
        public int ceiling(int x, int fromY, int z) {
            return fromY >= CEILING ? FungalTerrainScan.NONE : CEILING;
        }

        @Override
        public boolean lava(int x, int y, int z) {
            return false;
        }
    };

    @Test
    void pillarStemRunsUnbrokenFromFloorToCeiling() {
        for (long seed = 0; seed < 200; seed++) {
            List<GiantFungusShape.Part> parts = GiantFungusShape.build(RandomSource.create(seed), 0, 64, 0, HALL, true);
            assertTrue(!parts.isEmpty(), "seed " + seed);
            Set<Integer> stemAtCentre = new HashSet<>();
            for (GiantFungusShape.Part part : parts) {
                if (part.kind() == GiantFungusShape.Kind.STEM && part.x() == 0 && part.z() == 0) {
                    stemAtCentre.add(part.y());
                }
            }
            int floor = HALL.floor(0, 64, 0);
            for (int y = floor + 1; y < CEILING; y++) {
                assertTrue(stemAtCentre.contains(y), "seed " + seed + " missing stem at Y " + y);
            }
        }
    }

    @Test
    void everyPartStaysWithinTheReachOfTheOrigin() {
        // An origin at the very edge of its chunk keeps all parts inside the 3 x 3 chunks around it.
        assertTrue(GiantFungusShape.MAX_REACH < 16);
        for (boolean pillars : new boolean[]{true, false}) {
            for (long seed = 0; seed < 200; seed++) {
                for (GiantFungusShape.Part part : GiantFungusShape.build(RandomSource.create(seed), 5, 64, -3, HALL, pillars)) {
                    assertTrue(Math.abs(part.x() - 5) <= GiantFungusShape.MAX_REACH && Math.abs(part.z() + 3) <= GiantFungusShape.MAX_REACH,
                            "seed " + seed + " " + part);
                }
            }
        }
    }

    @Test
    void freeStandingFungusIsAStemWithACap() {
        for (long seed = 0; seed < 200; seed++) {
            List<GiantFungusShape.Part> parts = GiantFungusShape.build(RandomSource.create(seed), 0, 64, 0, HALL, false);
            assertTrue(!parts.isEmpty(), "seed " + seed);
            int stems = 0;
            int caps = 0;
            for (GiantFungusShape.Part part : parts) {
                if (part.kind() == GiantFungusShape.Kind.STEM) {
                    stems++;
                    assertTrue(part.y() >= FLOOR - 2 && part.y() <= FLOOR + 21, "stem height " + part);
                } else if (part.kind() == GiantFungusShape.Kind.CAP || part.kind() == GiantFungusShape.Kind.LAMP) {
                    caps++;
                }
            }
            assertTrue(stems >= 8 && caps > 0, "seed " + seed);
        }
    }

    @Test
    void lampShareStaysNearItsChance() {
        int lamps = 0;
        int cap = 0;
        for (long seed = 0; seed < 200; seed++) {
            for (GiantFungusShape.Part part : GiantFungusShape.build(RandomSource.create(seed), 0, 64, 0, HALL, true)) {
                if (part.kind() == GiantFungusShape.Kind.LAMP) {
                    lamps++;
                    cap++;
                } else if (part.kind() == GiantFungusShape.Kind.CAP) {
                    cap++;
                }
            }
        }
        double share = (double) lamps / cap;
        assertTrue(Math.abs(share - GiantFungusShape.LAMP_CHANCE) < 0.02, "lamp share " + share);
    }

    @Test
    void sameSeedSameFungusAndLowHallsTakeNoPillar() {
        assertEquals(GiantFungusShape.build(RandomSource.create(7), 0, 64, 0, HALL, true), GiantFungusShape.build(RandomSource.create(7), 0, 64, 0, HALL, true));
        GiantFungusShape.Ground low = new GiantFungusShape.Ground() {
            @Override
            public int floor(int x, int fromY, int z) {
                return 40;
            }

            @Override
            public int ceiling(int x, int fromY, int z) {
                return 52;
            }

            @Override
            public boolean lava(int x, int y, int z) {
                return false;
            }
        };
        assertTrue(GiantFungusShape.build(RandomSource.create(1), 0, 45, 0, low, true).isEmpty());
    }
}
