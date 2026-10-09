package nl.timtoys.corruptedadventure.worldgen.feature;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    /** The default size is the old free-standing fungus draw for draw (Nether ring 2 unchanged by 53b). */
    @Test
    void defaultFreeSizeDrawsAsBefore() {
        for (long seed = 0; seed < 50; seed++) {
            assertEquals(GiantFungusShape.build(RandomSource.create(seed), 0, 64, 0, HALL, false),
                    GiantFungusShape.build(RandomSource.create(seed), 0, 64, 0, HALL, false, GiantFungusShape.FreeSize.DEFAULT));
        }
        assertEquals(new GiantFungusShape.FreeSize(8, 20, 3, 6, GiantFungusShape.MIN_FREE_GAP), GiantFungusShape.FreeSize.DEFAULT);
    }

    /** Nether ring 3's rimeshrooms (G2-WN1 = A): stems 12–22 and small ones 3–6, caps within the reach. */
    @Test
    void freeSizesBoundStemAndCap() {
        for (GiantFungusShape.FreeSize size : List.of(NetherRing3Features.RIMESHROOM_SIZE, NetherRing3Features.SMALL_RIMESHROOM_SIZE)) {
            Set<Integer> heights = new HashSet<>();
            for (long seed = 0; seed < 300; seed++) {
                int top = Integer.MIN_VALUE;
                for (GiantFungusShape.Part part : GiantFungusShape.build(RandomSource.create(seed * 0x9E3779B97F4A7C15L), 0, 64, 0, HALL, false, size)) {
                    assertTrue(Math.abs(part.x()) <= size.maxCapRadius() && Math.abs(part.z()) <= size.maxCapRadius(), part.toString());
                    if (part.kind() == GiantFungusShape.Kind.STEM && part.x() == 0 && part.z() == 0) {
                        top = Math.max(top, part.y());
                    }
                }
                heights.add(top - HALL.floor(0, 64, 0));
            }
            assertEquals(size.minHeight(), heights.stream().mapToInt(Integer::intValue).min().orElseThrow());
            assertEquals(size.maxHeight(), heights.stream().mapToInt(Integer::intValue).max().orElseThrow());
        }
        assertEquals(12, NetherRing3Features.RIMESHROOM_SIZE.minHeight());
        assertEquals(22, NetherRing3Features.RIMESHROOM_SIZE.maxHeight());
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

    /** A hall with a flat floor at {@code floor} and a flat ceiling at {@code ceiling}. */
    private static GiantFungusShape.Ground flatHall(int floor, int ceiling) {
        return new GiantFungusShape.Ground() {
            @Override
            public int floor(int x, int fromY, int z) {
                return fromY <= floor ? FungalTerrainScan.NONE : floor;
            }

            @Override
            public int ceiling(int x, int fromY, int z) {
                return fromY >= ceiling ? FungalTerrainScan.NONE : ceiling;
            }

            @Override
            public boolean lava(int x, int y, int z) {
                return false;
            }
        };
    }

    /** Nether ring 3's sizes need room for their largest fungus: one block lower and nothing grows, at the room no cap meets the ceiling. */
    @Test
    void ring3SizesNeedRoomForTheirLargestFungus() {
        for (GiantFungusShape.FreeSize size : List.of(NetherRing3Features.RIMESHROOM_SIZE, NetherRing3Features.SMALL_RIMESHROOM_SIZE)) {
            assertEquals(size.maxHeight() + size.maxCapRadius() + 1, size.minGap());
            GiantFungusShape.Ground tight = flatHall(40, 40 + size.minGap());
            GiantFungusShape.Ground tooLow = flatHall(40, 40 + size.minGap() - 1);
            for (long seed = 0; seed < 200; seed++) {
                assertTrue(GiantFungusShape.build(RandomSource.create(seed), 0, 42, 0, tooLow, false, size).isEmpty());
                List<GiantFungusShape.Part> parts = GiantFungusShape.build(RandomSource.create(seed), 0, 42, 0, tight, false, size);
                assertTrue(!parts.isEmpty(), "seed " + seed);
                for (GiantFungusShape.Part part : parts) {
                    assertTrue(part.y() < 40 + size.minGap(), "seed " + seed + " " + part);
                }
            }
        }
        assertEquals(10, NetherRing3Features.SMALL_RIMESHROOM_SIZE.minGap());
    }

    /** Under the rimeshroom caps the stalactites hang free: never in a stem column, always right under a cap block. */
    @Test
    void ring3StalactitesHangUnderTheCapClearOfTheStem() {
        for (GiantFungusShape.FreeSize size : List.of(NetherRing3Features.RIMESHROOM_SIZE, NetherRing3Features.SMALL_RIMESHROOM_SIZE)) {
            for (long seed = 0; seed < 300; seed++) {
                List<GiantFungusShape.Part> parts = GiantFungusShape.build(RandomSource.create(seed), 0, 64, 0, HALL, false, size);
                Set<List<Integer>> stems = new HashSet<>();
                Set<List<Integer>> caps = new HashSet<>();
                for (GiantFungusShape.Part part : parts) {
                    if (part.kind() == GiantFungusShape.Kind.STEM) {
                        stems.add(List.of(part.x(), part.z()));
                    } else if (part.kind() != GiantFungusShape.Kind.STALACTITE) {
                        caps.add(List.of(part.x(), part.y(), part.z()));
                    }
                }
                if (size.minCapRadius() < 3) {
                    assertEquals(Set.of(List.of(0, 0)), stems, "small caps get the thin stem, seed " + seed);
                }
                for (GiantFungusShape.Part part : parts) {
                    if (part.kind() != GiantFungusShape.Kind.STALACTITE) {
                        continue;
                    }
                    assertTrue(!stems.contains(List.of(part.x(), part.z())), "seed " + seed + " " + part);
                    // The top block of each stalactite hangs from a cap block.
                    if (parts.stream().noneMatch(other ->
                            other.kind() == GiantFungusShape.Kind.STALACTITE && other.x() == part.x() && other.z() == part.z() && other.y() == part.y() + 1)) {
                        assertTrue(caps.contains(List.of(part.x(), part.y() + 1, part.z())), "seed " + seed + " " + part);
                    }
                }
            }
        }
    }

    @Test
    void freeSizeRejectsEmptyOrTooLargeRanges() {
        assertThrows(IllegalArgumentException.class, () -> new GiantFungusShape.FreeSize(10, 8, 3, 6, 30));
        assertThrows(IllegalArgumentException.class, () -> new GiantFungusShape.FreeSize(8, 20, 5, 4, 30));
        assertThrows(IllegalArgumentException.class, () -> new GiantFungusShape.FreeSize(8, 20, 3, 7, 30));
        assertThrows(IllegalArgumentException.class, () -> new GiantFungusShape.FreeSize(8, 20, 3, 6, 11));
        String bad = "{\"min_height\":10,\"max_height\":8,\"min_cap_radius\":3,\"max_cap_radius\":6,\"min_gap\":30}";
        assertTrue(GiantFungusShape.FreeSize.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(bad)).error().isPresent());
        GiantFungusShape.FreeSize size = NetherRing3Features.RIMESHROOM_SIZE;
        assertEquals(size, GiantFungusShape.FreeSize.CODEC.parse(JsonOps.INSTANCE,
                GiantFungusShape.FreeSize.CODEC.encodeStart(JsonOps.INSTANCE, size).getOrThrow()).getOrThrow());
    }
}
