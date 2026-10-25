package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntBinaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** G2 ring 2: the flat biomes' landmarks are pure, stay within their reach, never float, and only suit their ground. */
class Ring2FlatLandmarksTest {
    /** A terrain with the given ground (water nowhere) and every biome everywhere. */
    private static Ring2FlatLandmarks.Terrain terrain(IntBinaryOperator ground) {
        return new Ring2FlatLandmarks.Terrain() {
            @Override
            public int surface(int x, int z) {
                return ground.applyAsInt(x, z);
            }

            @Override
            public int floor(int x, int z) {
                return ground.applyAsInt(x, z);
            }

            @Override
            public boolean biomeAround(int x, int z, int radius, ResourceKey<Biome> biome) {
                return true;
            }
        };
    }

    /** Rolling dunes: valleys at 68, crests 20 higher, ~105 apart. */
    private static int dunes(int x, int z) {
        return 68 + (int) Math.round(10.0 * (1.0 - Math.cos(x * 0.06 + Math.sin(z * 0.03) * 2.0)));
    }

    @Test
    void theDomeWantsBareCrustTheSkeletonAValleyThePalmDryPlain() {
        Ring2FlatLandmarks.Terrain crust = terrain((x, z) -> SaltDomeShape.CRUST_TOP);
        assertTrue(Ring2FlatLandmarks.accepts(crust, Ring2FlatLandmarks.Kind.SALT_DOME, 100, 100));
        // A sand island (+2) within the footprint: no dome.
        Ring2FlatLandmarks.Terrain island = terrain((x, z) -> x > 110 ? SaltDomeShape.CRUST_TOP + 2 : SaltDomeShape.CRUST_TOP);
        assertFalse(Ring2FlatLandmarks.accepts(island, Ring2FlatLandmarks.Kind.SALT_DOME, 100, 100));

        Ring2FlatLandmarks.Terrain dunes = terrain(Ring2FlatLandmarksTest::dunes);
        assertTrue(Ring2FlatLandmarks.accepts(dunes, Ring2FlatLandmarks.Kind.DUNE_SKELETON, 0, 0), "a valley floor");
        assertFalse(Ring2FlatLandmarks.accepts(dunes, Ring2FlatLandmarks.Kind.DUNE_SKELETON, 52, 0), "a crest");

        Ring2FlatLandmarks.Terrain plain = terrain((x, z) -> 68);
        assertTrue(Ring2FlatLandmarks.accepts(plain, Ring2FlatLandmarks.Kind.GIANT_PALM, 8, 8));
        Ring2FlatLandmarks.Terrain high = terrain((x, z) -> 90);
        assertFalse(Ring2FlatLandmarks.accepts(high, Ring2FlatLandmarks.Kind.GIANT_PALM, 8, 8));
    }

    @Test
    void thePalmsAnchorSitsInTheMiddleOfItsChunk() {
        for (int x = -40; x <= 40; x++) {
            int snapped = Ring2FlatLandmarks.anchorX(Ring2FlatLandmarks.Kind.GIANT_PALM, x);
            assertEquals(x >> 4, snapped >> 4);
            assertTrue((snapped & 15) >= 7 && (snapped & 15) <= 8);
            assertTrue(Math.abs(snapped - x) <= Ring2FlatLandmarks.SNAP);
            assertEquals(x, Ring2FlatLandmarks.anchorX(Ring2FlatLandmarks.Kind.SALT_DOME, x));
        }
    }

    @Test
    void theDomeIsAHollowShellWithAWayInAndCrystalsInside() {
        for (long seed = 0; seed < 40; seed++) {
            Ring2FlatLandmarks.Site site = new Ring2FlatLandmarks.Site(Ring2FlatLandmarks.Kind.SALT_DOME, 100, -50, SaltDomeShape.CRUST_TOP, seed);
            LandmarkPlan plan = Ring2FlatLandmarks.build(site, terrain((x, z) -> SaltDomeShape.CRUST_TOP));
            withinReach(site, plan);
            assertTrue(plan.objects.keySet().stream().allMatch(pos -> pos.getY() >= SaltDomeShape.CRUST_TOP));
            assertTrue(plan.objects.keySet().stream().mapToInt(BlockPos::getY).max().orElseThrow() >= SaltDomeShape.CRUST_TOP + 6);
            int width = plan.objects.keySet().stream().mapToInt(BlockPos::getX).max().orElseThrow()
                    - plan.objects.keySet().stream().mapToInt(BlockPos::getX).min().orElseThrow() + 1;
            assertTrue(width >= 16 && width <= 33, "width " + width);
            assertFalse(plan.objects.containsKey(new BlockPos(100, SaltDomeShape.CRUST_TOP + 2, -50)), "hollow");
            assertFalse(plan.decor.isEmpty(), "crystals inside");
            for (LandmarkPlan.Decor decor : plan.decor) {
                assertTrue(plan.objects.containsKey(decor.support()));
                assertFalse(plan.objects.containsKey(decor.pos()));
            }
            assertTrue(hasWayIn(site, plan), "a way in at the foot");
            noFloating(plan, pos -> pos.getY() == SaltDomeShape.CRUST_TOP);
        }
    }

    @Test
    void theSkeletonLiesInTheSandAndNothingHangs() {
        IntBinaryOperator ground = Ring2FlatLandmarksTest::dunes;
        for (long seed = 0; seed < 40; seed++) {
            Ring2FlatLandmarks.Site site = new Ring2FlatLandmarks.Site(Ring2FlatLandmarks.Kind.DUNE_SKELETON, 0, 0, ground.applyAsInt(0, 0), seed);
            LandmarkPlan plan = Ring2FlatLandmarks.build(site, terrain(ground));
            withinReach(site, plan);
            assertTrue(plan.objects.size() > 60, "a big skeleton: " + plan.objects.size());
            assertTrue(plan.objects.values().stream().allMatch(state -> state.is(Blocks.BONE_BLOCK)));
            assertTrue(plan.objects.keySet().stream().anyMatch(pos -> pos.getY() >= ground.applyAsInt(pos.getX(), pos.getZ()) + 5),
                    "ribs rise out of the sand");
            // Everything hangs on a block in or on the sand.
            noFloating(plan, pos -> pos.getY() <= ground.applyAsInt(pos.getX(), pos.getZ()));
        }
    }

    @Test
    void theGiantPalmStandsBesideItsPoolWithAConnectedCrown() {
        IntBinaryOperator ground = (x, z) -> 68;
        for (long seed = 0; seed < 40; seed++) {
            Ring2FlatLandmarks.Site site = new Ring2FlatLandmarks.Site(Ring2FlatLandmarks.Kind.GIANT_PALM, 8, 8, 68, seed);
            LandmarkPlan plan = Ring2FlatLandmarks.build(site, terrain(ground));
            withinReach(site, plan);
            BlockState log = ModBlocks.PALM_LOG.get().defaultBlockState();
            long logs = plan.objects.values().stream().filter(state -> state == log).count();
            assertTrue(logs >= 4 * 20, "a 2 x 2 trunk of 20 or more: " + logs);
            int top = plan.objects.keySet().stream().mapToInt(BlockPos::getY).max().orElseThrow();
            assertTrue(top >= 68 + 20 && top <= 68 + 28 + 3, "top " + top);
            // Nothing of the palm or its rock in the pool (radius + rim round the anchor).
            for (BlockPos pos : plan.objects.keySet()) {
                double d = Math.hypot(pos.getX() + 0.5 - 8.5, pos.getZ() + 0.5 - 8.5);
                assertTrue(d > 2.5 || pos.getY() > 72, "in the pool: " + pos);
            }
            noFloating(plan, pos -> pos.getY() <= 68);
        }
    }

    /** Every block, foot and plant of the plan lies within the kind's reach of the anchor (so every chunk it touches asks). */
    private static void withinReach(Ring2FlatLandmarks.Site site, LandmarkPlan plan) {
        int reach = site.kind().reach();
        for (BlockPos pos : plan.objects.keySet()) {
            assertTrue(Math.abs(pos.getX() - site.x()) <= reach && Math.abs(pos.getZ() - site.z()) <= reach, "out of reach: " + pos);
        }
        for (LandmarkPlan.Foot foot : plan.feet) {
            assertTrue(Math.abs(foot.x() - site.x()) <= reach && Math.abs(foot.z() - site.z()) <= reach);
        }
        for (LandmarkPlan.Decor decor : plan.decor) {
            assertTrue(Math.abs(decor.pos().getX() - site.x()) <= reach && Math.abs(decor.pos().getZ() - site.z()) <= reach);
        }
    }

    /** Every object is connected (26 neighbours) to one that {@code grounded} holds. */
    private static void noFloating(LandmarkPlan plan, java.util.function.Predicate<BlockPos> grounded) {
        Set<BlockPos> reached = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos pos : plan.objects.keySet()) {
            if (grounded.test(pos)) {
                reached.add(pos);
                queue.add(pos);
            }
        }
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            for (BlockPos next : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                if (plan.objects.containsKey(next) && reached.add(next.immutable())) {
                    queue.add(next.immutable());
                }
            }
        }
        for (BlockPos pos : plan.objects.keySet()) {
            assertTrue(reached.contains(pos), "floating: " + pos);
        }
    }

    /** Whether some column at the foot of the shell, between shell blocks round it, is open up to 3 high. */
    private static boolean hasWayIn(Ring2FlatLandmarks.Site site, LandmarkPlan plan) {
        int base = site.ground();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int r = 1; r <= 17; r++) {
                for (int side = -12; side <= 12; side++) {
                    int x = site.x() + direction.getStepX() * r + direction.getStepZ() * side;
                    int z = site.z() + direction.getStepZ() * r + direction.getStepX() * side;
                    BlockPos foot = new BlockPos(x, base, z);
                    if (!plan.objects.containsKey(foot) && !plan.objects.containsKey(foot.above())
                            && plan.objects.containsKey(foot.above(4))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
