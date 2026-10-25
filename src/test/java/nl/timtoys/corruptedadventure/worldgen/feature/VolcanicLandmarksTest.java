package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntBinaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** G2 ring 3: the biome-own landmarks are pure per cell, spaced out, stay in their reach and biome, and keep their size. */
class VolcanicLandmarksTest {
    /** Level for the upright kinds, a steady slope down towards +x for the flow and the terraces. */
    private static IntBinaryOperator terrain(VolcanicLandmarks.Kind kind) {
        boolean slope = kind == VolcanicLandmarks.Kind.OBSIDIAN_FLOW || kind == VolcanicLandmarks.Kind.SINTER_TERRACES;
        return slope ? (x, z) -> 250 - Math.floorDiv(x * 2, 5) : (x, z) -> 70;
    }

    /** A world of the kind's own biome west of {@code edge} (and nothing east of it), plain wherever {@code plain} says. */
    private static VolcanicLandmarks.World world(long seed, VolcanicLandmarks.Kind kind, int edge, IntBinaryOperator plain) {
        IntBinaryOperator ground = terrain(kind);
        return new VolcanicLandmarks.World() {
            @Override
            public long seed() {
                return seed;
            }

            @Override
            public int ground(int x, int z) {
                return ground.applyAsInt(x, z);
            }

            @Override
            public boolean plain(int x, int z) {
                return plain.applyAsInt(x, z) != 0;
            }

            @Override
            public ResourceKey<Biome> biome(int x, int z) {
                return x < edge ? kind.biome() : ModBiomes.DORMANT_CALDERA;
            }
        };
    }

    private static VolcanicLandmarks.World world(long seed, VolcanicLandmarks.Kind kind) {
        return world(seed, kind, Integer.MAX_VALUE, (x, z) -> 1);
    }

    private static List<VolcanicLandmarks.Site> sites(VolcanicLandmarks.World world, VolcanicLandmarks.Kind kind,
                                                      Map<Long, Optional<VolcanicLandmarks.Site>> cache) {
        return VolcanicLandmarks.within(world, cache, kind, -4000, -4000, 4000, 4000, 0);
    }

    @Test
    void sitesArePureSpacedOutAndInAboutHalfTheCells() {
        for (VolcanicLandmarks.Kind kind : VolcanicLandmarks.Kind.values()) {
            List<VolcanicLandmarks.Site> first = sites(world(7L, kind), kind, new HashMap<>());
            // About half the cells of a world all of the kind's biome hold one (20 × 20 cells).
            double share = first.size() / 400.0;
            assertTrue(share > 0.35 && share < 0.62, kind + ": " + first.size() + " sites in 400 cells");
            // Asked in another order (cell by cell from the far corner) the same sites come out.
            Map<Long, Optional<VolcanicLandmarks.Site>> cache = new HashMap<>();
            for (int i = 10; i >= -11; i--) {
                for (int j = 10; j >= -11; j--) {
                    VolcanicLandmarks.site(world(7L, kind), cache, kind, i, j);
                }
            }
            assertEquals(Set.copyOf(first), Set.copyOf(sites(world(7L, kind), kind, cache)));
            for (VolcanicLandmarks.Site a : first) {
                for (VolcanicLandmarks.Site b : first) {
                    if (a != b) {
                        assertTrue(Math.max(Math.abs(a.x() - b.x()), Math.abs(a.z() - b.z())) > 2 * kind.reach(),
                                kind + ": two landmarks never meet");
                    }
                }
            }
            assertNotEquals(first, sites(world(8L, kind), kind, new HashMap<>()), "another seed, other sites");
        }
    }

    @Test
    void landmarksStayWithinTheirReachAndArePure() {
        for (VolcanicLandmarks.Kind kind : VolcanicLandmarks.Kind.values()) {
            VolcanicLandmarks.World world = world(11L, kind);
            List<VolcanicLandmarks.Site> sites = VolcanicLandmarks.within(world, new HashMap<>(), kind, -1200, -1200, 1200, 1200, 0);
            assertFalse(sites.isEmpty(), kind + " has sites");
            for (VolcanicLandmarks.Site site : sites) {
                VolcanicPlan plan = VolcanicLandmarks.build(world, site);
                assertNotNull(plan, kind + " accepts its own site");
                assertFalse(plan.isEmpty(), kind + " builds something");
                for (BlockPos column : plan.columns()) {
                    assertTrue(Math.abs(column.getX() - site.x()) <= kind.reach() && Math.abs(column.getZ() - site.z()) <= kind.reach(),
                            kind + " reaches " + column);
                }
                for (VolcanicPlan.Decor decor : plan.decor) {
                    assertEquals(1, decor.support().distManhattan(decor.pos()), "decor sits beside or on its support");
                }
                VolcanicPlan again = VolcanicLandmarks.build(world, site);
                assertEquals(plan.objects, again.objects);
                assertEquals(plan.covers, again.covers);
                assertEquals(plan.feet, again.feet);
                assertEquals(plan.groundDecor, again.groundDecor);
            }
        }
    }

    @Test
    void everyColumnLiesInTheBiomeOnPlainGround() {
        for (VolcanicLandmarks.Kind kind : VolcanicLandmarks.Kind.values()) {
            // The biome ends at x = 0 (on the 8-block grid the check samples), a strip at z 0..3 is not plain.
            VolcanicLandmarks.World world = world(5L, kind, 0, (x, z) -> z >= 0 && z < 4 ? 0 : 1);
            List<VolcanicLandmarks.Site> sites = VolcanicLandmarks.within(world, new HashMap<>(), kind, -2400, -2400, 2400, 2400, 0);
            assertFalse(sites.isEmpty(), kind + " has sites west of the edge");
            for (VolcanicLandmarks.Site site : sites) {
                for (BlockPos column : VolcanicLandmarks.build(world, site).columns()) {
                    assertTrue(column.getX() < 0, kind + " crosses out of its biome at " + column);
                    assertFalse(column.getZ() >= 0 && column.getZ() < 4 && (column.getX() & 3) == 0 && (column.getZ() & 3) == 0,
                            kind + " lies over ground that is not plain at " + column);
                }
            }
        }
    }

    @Test
    void theUprightKindsNeedLevelGroundAndTheFlowASlope() {
        VolcanicLandmarks.Site flat = new VolcanicLandmarks.Site(VolcanicLandmarks.Kind.OBSIDIAN_FLOW, 200, 200, 70, 3L);
        assertNull(ObsidianFlowShape.build(flat, (x, z) -> 70), "no flow on level ground");
        assertNull(SinterTerraceShape.build(new VolcanicLandmarks.Site(VolcanicLandmarks.Kind.SINTER_TERRACES, 200, 200, 70, 3L),
                (x, z) -> 70), "no terraces on level ground");
        IntBinaryOperator steep = (x, z) -> 200 - 2 * x;
        assertNull(CharredGiantShape.build(new VolcanicLandmarks.Site(VolcanicLandmarks.Kind.CHARRED_GIANT, 50, 50, 100, 3L), steep));
        assertNull(BasaltOrganShape.build(new VolcanicLandmarks.Site(VolcanicLandmarks.Kind.BASALT_ORGAN, 50, 50, 100, 3L), steep));
        assertNull(TitanWeaponShape.build(new VolcanicLandmarks.Site(VolcanicLandmarks.Kind.TITAN_WEAPON, 50, 50, 100, 3L), steep));
    }

    @Test
    void theShapesKeepTheirSizes() {
        for (long seed = 1; seed <= 24; seed++) {
            int g = 70;
            IntBinaryOperator level = (x, z) -> g;
            // Charred giant: a trunk 15–25 over the ground (and a splinter).
            VolcanicPlan giant = CharredGiantShape.build(site(VolcanicLandmarks.Kind.CHARRED_GIANT, seed), level);
            int trunk = top(giant, 100, 100, ModBlocks.CHARRED_LOG.get().defaultBlockState()) - g;
            assertTrue(trunk >= 13 && trunk <= 27, "charred trunk " + trunk);
            // Ember giant: 20–28, a hollow of air in its foot, persistent leaves.
            VolcanicPlan ember = EmberGiantShape.build(site(VolcanicLandmarks.Kind.EMBER_GIANT, seed), level);
            assertTrue(ember.objects.get(new BlockPos(100, g, 100)).isAir(), "the hollow");
            assertEquals(ModBlocks.SMOLDERING_ASH.get().defaultBlockState(), ember.objects.get(new BlockPos(100, g - 1, 100)));
            int crownTop = ember.objects.entrySet().stream().filter(e -> e.getValue().getBlock() instanceof LeavesBlock)
                    .peek(e -> assertTrue(e.getValue().getValue(LeavesBlock.PERSISTENT)))
                    .mapToInt(e -> e.getKey().getY()).max().orElseThrow() - g;
            assertTrue(crownTop >= 19 && crownTop <= 32, "ember giant " + crownTop);
            // Basalt organ: 10–30 columns' worth of heads, the tallest 8–20 over the ground.
            VolcanicPlan organ = BasaltOrganShape.build(site(VolcanicLandmarks.Kind.BASALT_ORGAN, seed), level);
            int organTop = organ.objects.keySet().stream().mapToInt(BlockPos::getY).max().orElseThrow() - g;
            assertTrue(organTop >= 8 && organTop <= 21, "organ " + organTop);
            // Titan weapon: its top 20–30 over the ground.
            VolcanicPlan weapon = TitanWeaponShape.build(site(VolcanicLandmarks.Kind.TITAN_WEAPON, seed), level);
            int weaponTop = weapon.objects.keySet().stream().mapToInt(BlockPos::getY).max().orElseThrow() - g;
            assertTrue(weaponTop >= 19 && weaponTop <= 31, "weapon " + weaponTop);
            // Whale: 25–35 long (bone blocks along its axis), its arches over the ground.
            VolcanicPlan whale = WhaleSkeletonShape.build(site(VolcanicLandmarks.Kind.WHALE_SKELETON, seed), level);
            assertTrue(whale.objects.values().stream().allMatch(state -> state.is(Blocks.BONE_BLOCK)), "bone only");
            int span = span(whale);
            assertTrue(span >= 18 && span <= 41, "whale " + span);
            assertTrue(whale.objects.keySet().stream().mapToInt(BlockPos::getY).max().orElseThrow() - g >= 5, "the ribs arch");
            // Flow: on a slope, 30–60 long, its covers obsidian or blackstone.
            IntBinaryOperator slope = (x, z) -> 250 - Math.floorDiv(x * 2, 5);
            VolcanicPlan flow = ObsidianFlowShape.build(site(VolcanicLandmarks.Kind.OBSIDIAN_FLOW, seed), slope);
            assertNotNull(flow, "a flow down the slope");
            int length = flow.covers.keySet().stream().mapToInt(BlockPos::getX).max().orElseThrow() - 100;
            assertTrue(length >= 25 && length <= 64, "flow " + length);
            assertTrue(flow.covers.values().stream().allMatch(c -> c.fill().is(Blocks.OBSIDIAN) || c.fill().is(Blocks.CRYING_OBSIDIAN)
                    || c.fill().is(Blocks.BLACKSTONE)));
            // Terraces: 4–6 levels stepping down the slope.
            VolcanicPlan terraces = SinterTerraceShape.build(site(VolcanicLandmarks.Kind.SINTER_TERRACES, seed), slope);
            assertNotNull(terraces, "terraces on the slope");
            long levels = terraces.objects.entrySet().stream().filter(e -> isMud(e.getValue())).mapToInt(e -> e.getKey().getY()).distinct().count();
            assertTrue(levels >= 4 && levels <= 6, "terraces " + levels);
        }
    }

    private static VolcanicLandmarks.Site site(VolcanicLandmarks.Kind kind, long seed) {
        return new VolcanicLandmarks.Site(kind, 100, 100, kind == VolcanicLandmarks.Kind.OBSIDIAN_FLOW
                || kind == VolcanicLandmarks.Kind.SINTER_TERRACES ? 250 - 40 : 70, seed * 0x9E3779B97F4A7C15L);
    }

    private static int top(VolcanicPlan plan, int x, int z, BlockState log) {
        return plan.objects.entrySet().stream().filter(e -> e.getValue().getBlock() == log.getBlock())
                .mapToInt(e -> e.getKey().getY()).max().orElseThrow();
    }

    /** The longest horizontal extent of the plan's blocks (x or z). */
    private static int span(VolcanicPlan plan) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : plan.objects.keySet()) {
            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX());
            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        return Math.max(maxX - minX, maxZ - minZ) + 1;
    }

    private static boolean isMud(BlockState state) {
        return state.is(ModBlocks.YELLOW_SULFUR_MUD.get()) || state.is(ModBlocks.ORANGE_SULFUR_MUD.get())
                || state.is(ModBlocks.GREEN_SULFUR_MUD.get()) || state.is(ModBlocks.BOILING_MUD.get());
    }
}
