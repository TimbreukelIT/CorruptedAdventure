package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandFieldTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The landmarks of decor D on the test seed: the giant verdant trees beside the gorges, the split rocks on the bridges. */
class DecorDLandmarksTest {
    private static final IslandField FIELD = IslandFieldTest.field();
    private static final long SEED = IslandFieldTest.SEED;

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

    @Test
    @Disabled("85: land bridges removed; split rock moves in 86c")
    void everyLandBridgeCarriesOneSplitRockInTheBrokenZone() {
        SplitRockConfiguration config = new SplitRockConfiguration(BlockStateProvider.simple(Blocks.STONE), BlockStateProvider.simple(Blocks.STONE),
                BlockStateProvider.simple(Blocks.STONE), ConstantInt.of(30), ConstantFloat.of(0.8F), ConstantFloat.of(3.5F),
                ConstantFloat.of(2.5F), SunderedReachFeatures.POSITION);
        Map<Integer, IslandField.LandBridge> bridges = new LinkedHashMap<>();
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += 0.02) {
            for (IslandField.LandBridge bridge : FIELD.landBridgesNear((int) (8000 * Math.cos(angle)), (int) (8000 * Math.sin(angle)))) {
                bridges.putIfAbsent(bridge.index(), bridge);
            }
        }
        assertTrue(bridges.size() > 50, "bridges " + bridges.size());
        for (IslandField.LandBridge bridge : bridges.values()) {
            SplitRockFeature.Rock rock = SplitRockFeature.plan(FIELD, SEED, config, bridge);
            System.out.printf("split rock of bridge %d: %d %d (half width %.1f)%n", bridge.index(), (int) Math.floor(rock.x()),
                    (int) Math.floor(rock.z()), rock.halfWidth());
            double target = bridge.zoneStart() + SunderedReachFeatures.POSITION * (bridge.zoneEnd() - bridge.zoneStart());
            assertEquals(target, FIELD.effectiveDistance(rock.x(), rock.z()), 3.0);
            // Pure: a chunk it reaches finds it.
            int cx = (int) Math.floor(rock.x()) & ~15;
            int cz = (int) Math.floor(rock.z()) & ~15;
            assertEquals(1, SplitRockFeature.rocksNear(FIELD, SEED, config, cx, cz).size());
        }
    }

    /** The split stays open at the foot (within 1.1 of its middle line), the halves within the rock's reach. */
    @Test
    void theSplitIsWalkableAndTheRockStaysWithinItsReach() {
        SplitRockFeature.Rock rock = new SplitRockFeature.Rock(0.5, 0.5, 1.0, 0.0, SplitRockFeature.MAX_HALF_WIDTH, 24, 34, 1,
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
