package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandFieldTest;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.ZoneFracture;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.ZoneFractureTest;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The landmarks of decor D on the test seed: the giant verdant trees beside the gorges, the split rocks on the landmark
 * floes of the fracture zone 4→5 (86c).
 */
class DecorDLandmarksTest {
    private static final IslandField FIELD = IslandFieldTest.field();
    private static final long SEED = IslandFieldTest.SEED;
    private static final ZoneFracture FRACTURE = ZoneFractureTest.fracture(SEED);
    private static final ZoneFracture AGAIN = ZoneFractureTest.fracture(SEED);

    /** Every gorge round the zone 3→4, by index. */
    private static Map<Integer, IslandField.GorgeSite> gorges() {
        Map<Integer, IslandField.GorgeSite> found = new LinkedHashMap<>();
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += 0.02) {
            for (IslandField.GorgeSite site : FIELD.gorgesNear((int) (6000 * Math.cos(angle)), (int) (6000 * Math.sin(angle)))) {
                found.putIfAbsent(site.index(), site);
            }
        }
        return found;
    }

    @Test
    void mostGorgesGetAGiantTreeOnFlatDryGroundClearOfTheFalls() {
        Map<Integer, IslandField.GorgeSite> gorges = gorges();
        assertTrue(gorges.size() > 30, "gorges found: " + gorges.size());
        int trees = 0;
        for (IslandField.GorgeSite gorge : gorges.values()) {
            Optional<VerdantGiantTrees.Spot> spot = VerdantGiantTrees.spot(FIELD, gorge);
            System.out.printf("giant tree of gorge %d (centre %.0f %.0f): %s%n", gorge.index(), gorge.centreX(), gorge.centreZ(),
                    spot.map(s -> s.x() + " " + s.ground() + " " + s.z()).orElse("none"));
            if (spot.isEmpty()) {
                continue;
            }
            trees++;
            VerdantGiantTrees.Spot s = spot.get();
            int r = VerdantGiantTrees.FALL_CLEARANCE;
            assertTrue(FIELD.waterfallsWithin(s.x() - r, s.z() - r, s.x() + r, s.z() + r).isEmpty());
            // Found the same from any chunk near it (the placement asks per chunk).
            assertEquals(List.of(s), VerdantGiantTrees.near(FIELD, s.x() & ~15, s.z() & ~15, (s.x() & ~15) + 15, (s.z() & ~15) + 15, 0));
            assertTrue(Math.hypot(s.x() - gorge.centreX(), s.z() - gorge.centreZ()) <= VerdantGiantTrees.MAX_SPOT_DISTANCE);
        }
        System.out.printf("giant trees: %d of %d gorges%n", trees, gorges.size());
        assertTrue(trees * 2 >= gorges.size(), "at least half the gorges get their tree: " + trees + " of " + gorges.size());
    }

    /** The giant tree's reach stays inside the 3 × 3 chunks round its own, also with the edge updates. */
    @Test
    void theGiantTreeReachesNoFurtherThanTheNeighbouringChunks() {
        int branch = 7 + 1 + (9 - VerdantGiantTrunkPlacer.BRANCH_CROWN);
        int crown = 9 + 1;
        int buttress = 1 + 6 + 1;
        int reach = Math.max(branch, Math.max(crown, buttress));
        assertTrue(reach <= VerdantGiantTrunkPlacer.MAX_REACH, "reach " + reach);
        // The origin anywhere in its chunk, plus one for the edge updates: still in the neighbouring chunk.
        assertTrue(VerdantGiantTrunkPlacer.MAX_REACH + 1 <= 16);
    }

    private static final SplitRockConfiguration CONFIG = new SplitRockConfiguration(BlockStateProvider.simple(Blocks.STONE),
            BlockStateProvider.simple(Blocks.STONE), BlockStateProvider.simple(Blocks.STONE), ConstantInt.of(30),
            ConstantFloat.of(0.8F), UniformFloat.of(4.0F, 5.5F), ConstantFloat.of(2.5F));

    /**
     * 86c (V8-A): every landmark floe of the fracture zone carries exactly one split rock, in its middle on its flat top
     * (base = {@code topY} − 1), split along the ray; the chunks it reaches all find it and only it, from the landmark
     * alone (pure). Its foot (the outline at the top, the faces' roughness at its widest) stays on the floe.
     */
    @Test
    void everyLandmarkCarriesOneSplitRockOnItsFloe() {
        List<ZoneFracture.Landmark> landmarks = FRACTURE.landmarks();
        assertTrue(landmarks.size() > 50, "landmarks " + landmarks.size());
        Map<SplitRockFeature.Rock, Integer> found = new LinkedHashMap<>();
        for (ZoneFracture.Landmark landmark : landmarks) {
            SplitRockFeature.Rock rock = SplitRockFeature.plan(SEED, CONFIG, landmark);
            System.out.printf("split rock of landmark %d %d %d (radius %d): half width %.1f, length %.2f%n", landmark.x(),
                    landmark.topY(), landmark.z(), landmark.radius(), rock.halfWidth(), rock.length());
            assertEquals(landmark.x() + 0.5, rock.x());
            assertEquals(landmark.z() + 0.5, rock.z());
            assertEquals(landmark.topY() - 1, rock.base());
            // The base is the floe's own top block (the terrain's extra rock ends there), not just topY by construction.
            assertTrue(FRACTURE.floeAt(landmark.x(), rock.base(), landmark.z()));
            assertFalse(FRACTURE.floeAt(landmark.x(), rock.base() + 1, landmark.z()));
            double distance = Math.hypot(rock.x(), rock.z());
            assertEquals(rock.x() / distance, rock.ux(), 1e-12);
            assertEquals(rock.z() / distance, rock.uz(), 1e-12);
            assertTrue(rock.halfWidth() >= SplitRockFeature.MIN_HALF_WIDTH && rock.halfWidth() <= SplitRockFeature.MAX_HALF_WIDTH);
            for (int dx = -SplitRockFeature.REACH; dx <= SplitRockFeature.REACH; dx++) {
                for (int dz = -SplitRockFeature.REACH; dz <= SplitRockFeature.REACH; dz++) {
                    double along = dx * rock.ux() + dz * rock.uz();
                    double across = Math.abs(-dx * rock.uz() + dz * rock.ux());
                    if (SplitRockFeature.inside(rock, along, across, rock.highHeight(), 0, SplitRockFeature.RAG) >= 0.0) {
                        assertTrue(dx * dx + dz * dz <= landmark.radius() * landmark.radius(), "foot off the floe at " + dx + ", " + dz);
                    }
                }
            }
            // Every chunk the rock reaches finds it, and no other rock; the same from another copy of the zone (pure).
            int x0 = (int) Math.floor(rock.x()) - SplitRockFeature.REACH;
            int z0 = (int) Math.floor(rock.z()) - SplitRockFeature.REACH;
            for (int cx = x0 & ~15; cx <= x0 + 2 * SplitRockFeature.REACH; cx += 16) {
                for (int cz = z0 & ~15; cz <= z0 + 2 * SplitRockFeature.REACH; cz += 16) {
                    assertEquals(List.of(rock), SplitRockFeature.rocksNear(FRACTURE, SEED, CONFIG, cx, cz));
                    assertEquals(List.of(rock), SplitRockFeature.rocksNear(AGAIN, SEED, CONFIG, cx, cz));
                }
            }
            found.merge(rock, 1, Integer::sum);
        }
        assertEquals(landmarks.size(), found.size(), "one rock per landmark");
    }

    /**
     * 86c: no rock outside the zone 4→5: the whole rock of every landmark lies where the zone fractures, and chunks round
     * the zone on either side (past its noisy edges: 8000 ± 128, noise ± 96) find none.
     */
    @Test
    void noSplitRockOutsideTheZone() {
        RingLayout layout = IslandFieldTest.LAYOUT;
        for (ZoneFracture.Landmark landmark : FRACTURE.landmarks()) {
            for (int dx = -SplitRockFeature.REACH; dx <= SplitRockFeature.REACH; dx += SplitRockFeature.REACH) {
                for (int dz = -SplitRockFeature.REACH; dz <= SplitRockFeature.REACH; dz += SplitRockFeature.REACH) {
                    double effective = layout.effectiveDistance(landmark.x() + 0.5 + dx, landmark.z() + 0.5 + dz);
                    assertTrue(ZoneFracture.strength(layout, effective) > 0.0, "landmark at " + effective);
                }
            }
        }
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += 0.005) {
            for (double r : new double[]{8000.0 - 128.0 - 96.0 - 16.0, 8000.0 + 128.0 + 96.0 + 16.0}) {
                int x = (int) Math.floor(r * Math.cos(angle));
                int z = (int) Math.floor(r * Math.sin(angle));
                assertTrue(SplitRockFeature.rocksNear(FRACTURE, SEED, CONFIG, x & ~15, z & ~15).isEmpty(), "rock near " + x + ", " + z);
            }
        }
    }

    /** The split stays open at the foot (within 1.1 of its middle line), the halves within the rock's reach. */
    @Test
    void theSplitIsWalkableAndTheRockStaysWithinItsReach() {
        SplitRockFeature.Rock rock = new SplitRockFeature.Rock(0.5, 0.5, 1.0, 0.0, 80, SplitRockFeature.MAX_HALF_WIDTH, 24, 34, 1,
                SplitRockFeature.MAX_LENGTH, SplitRockFeature.MAX_LEAN);
        RandomSource random = RandomSource.create(7);
        for (int i = 0; i < 2000; i++) {
            double across = 1.1 * random.nextDouble();
            assertTrue(SplitRockFeature.inside(rock, 0.0, across, 34, random.nextInt(3), rag(random)) < 0.0, "the split's floor stays open");
        }
        // Every direction of the bridge, the diagonal ones included (review: their corners reach furthest along x and z).
        for (double angle = 0.0; angle < Math.PI; angle += Math.PI / 16.0) {
            double ux = Math.cos(angle);
            double uz = Math.sin(angle);
            for (int dx = -20; dx <= 20; dx++) {
                for (int dz = -20; dz <= 20; dz++) {
                    double along = dx * ux + dz * uz;
                    double across = Math.abs(-dx * uz + dz * ux);
                    for (int dy = 0; dy <= 34; dy++) {
                        if (SplitRockFeature.inside(rock, along, across, 34, dy, SplitRockFeature.RAG) >= 0.0) {
                            assertTrue(Math.max(Math.abs(dx), Math.abs(dz)) <= SplitRockFeature.REACH, dx + " " + dz + " at " + angle);
                        }
                    }
                }
            }
        }
        // The anchor's frozen heightmap is read from every chunk the rock reaches: within one chunk of the anchor's.
        assertTrue(SplitRockFeature.REACH < 16);
    }

    private static double rag(RandomSource random) {
        return SplitRockFeature.RAG * (2.0 * random.nextDouble() - 1.0);
    }
}
