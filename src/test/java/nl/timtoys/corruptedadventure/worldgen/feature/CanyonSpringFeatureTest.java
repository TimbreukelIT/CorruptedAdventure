package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The layout core of {@code canyon_spring} ({@link SpringLayout}) on 2,000 synthetic canyon walls and floors, checked
 * against a small re-implementation of vanilla's water rules ({@code FlowingFluid}, water: drop-off 1, slope distance
 * 4): the layout must be exactly where water from the source and the pool comes to rest, and stay put when every
 * block ticks again.
 */
class CanyonSpringFeatureTest {
    private static final int SCENES = 2000;
    private static final int MIN_WATER_Y = 66;
    private static final int WALL_HEIGHT = 8;
    private static final int MAX_FLOOR_Y = 100;

    @Test
    void layoutsAreVanillasSettledStateAndHeldIn() {
        int layouts = 0;
        int bent = 0;
        int deep = 0;
        int clefts = 0;
        for (int n = 0; n < SCENES; n++) {
            RandomSource random = RandomSource.create(n);
            Scene scene = Scene.random(random);
            int found = 0;
            for (int column = 0; column < 14 * 14 && found < 3; column++) {
                for (Direction away : Direction.Plane.HORIZONTAL) {
                    int x = 1 + column % 14;
                    int z = 1 + column / 14;
                    SpringLayout.Params params = params(random);
                    Optional<SpringLayout.Layout> layout = SpringLayout.build(scene, 0, 0, x, z, away, params, WALL_HEIGHT, MIN_WATER_Y, MAX_FLOOR_Y);
                    if (layout.isPresent() && found < 3) {
                        found++;
                        layouts++;
                        bent += params.bendAt() > 0 ? 1 : 0;
                        deep += params.poolDepth() > 1 ? 1 : 0;
                        BlockPos fallTop = layout.get().water().get(1).pos();
                        clefts += scene.surface(fallTop.getX(), fallTop.getZ()) > scene.surface(x, z) ? 1 : 0;
                        check(scene, layout.get(), params, "scene " + n + " at " + x + "," + z + " " + away);
                    }
                }
            }
        }
        assertTrue(layouts >= 300, "enough layouts to judge: " + layouts);
        assertTrue(bent >= 50 && deep >= 50 && clefts >= 50, "bends " + bent + ", deep pools " + deep + ", clefts " + clefts);
    }

    @Test
    void noLayoutWithoutWallFloorOrClosedTrench() {
        SpringLayout.Params params = new SpringLayout.Params(4, 4, 0, false, 2, 2, 0, 1);
        Scene wall = Scene.plain(70, 5, 30);
        assertTrue(build(wall, 5, params), "the reference case fits");
        assertFalse(build(Scene.plain(70, 5, 0), 5, params), "no wall");
        assertFalse(build(Scene.plain(70, 5, 6), 5, params), "wall too low");
        Scene sloped = Scene.plain(70, 5, 0);
        for (int z = -16; z < 32; z++) {
            sloped.column(4, z, 73, true);
            sloped.column(3, z, 76, true);
            sloped.column(2, z, 79, true);
        }
        Optional<SpringLayout.Layout> cleft = SpringLayout.build(sloped, 0, 0, 5, 7, Direction.EAST, params, WALL_HEIGHT, MIN_WATER_Y, MAX_FLOOR_Y);
        assertTrue(cleft.isPresent(), "a wall rising 3 per block: source two columns in, cleft cut");
        assertEquals(new BlockPos(3, 74, 7), cleft.get().water().get(0).pos(), "source two columns into the wall");
        assertEquals(new BlockPos(4, 74, 7), cleft.get().water().get(1).pos(), "fall in the cut column");
        assertTrue(cleft.get().carve().isEmpty(), "the cut column is all water");
        Scene gentle = Scene.plain(70, 5, 0);
        for (int z = -16; z < 32; z++) {
            gentle.column(4, z, 72, true);
            gentle.column(3, z, 74, true);
            gentle.column(2, z, 78, true);
            gentle.column(1, z, 82, true);
        }
        Optional<SpringLayout.Layout> deep = SpringLayout.build(gentle, 0, 0, 5, 7, Direction.EAST, params, WALL_HEIGHT, MIN_WATER_Y, MAX_FLOOR_Y);
        assertTrue(deep.isPresent(), "a wall rising 2 per block: source three columns in");
        assertEquals(new BlockPos(2, 74, 7), deep.get().water().get(0).pos());
        assertEquals(List.of(new BlockPos(4, 71, 7), new BlockPos(4, 72, 7)), deep.get().carve(), "open cut between fall and foot");
        assertFalse(build(Scene.plain(64, 5, 30), 5, params), "below the water line");
        Scene leaky = Scene.plain(70, 5, 30);
        leaky.blocks.put(new BlockPos(7, 70, 8), Cell.AIR);
        assertFalse(build(leaky, 5, params), "open trench side");
        Scene hollow = Scene.plain(70, 5, 30);
        hollow.blocks.put(new BlockPos(4, 74, 6), Cell.AIR);
        assertFalse(build(hollow, 5, params), "hole beside the source");
        Scene cave = Scene.plain(70, 5, 30);
        cave.blocks.put(new BlockPos(6, 68, 7), Cell.AIR);
        assertFalse(build(cave, 5, params), "sand over a cave under the trench");
        Scene overgrown = Scene.plain(70, 5, 30);
        overgrown.blocks.put(new BlockPos(9, 71, 8), Cell.PLANT);
        assertFalse(build(overgrown, 5, params), "something on the pool");
        SpringLayout.Params long_ = new SpringLayout.Params(4, 6, 0, false, 2, 3, 0, 1);
        assertTrue(build(Scene.plain(70, 5, 30), 5, long_), "path 5–10, pool 11–13");
        assertFalse(build(Scene.plain(70, 7, 30), 7, long_), "path 7–12, pool 13–15: past the chunk margin");
        Scene slot = Scene.plain(70, 5, 30);
        for (int z = -16; z < 32; z++) {
            slot.column(11, z, 100, true);
        }
        assertFalse(build(slot, 5, params), "a narrow slot");
    }

    /** A spring at the foot of the plain wall, streaming east. */
    private static boolean build(Scene scene, int x, SpringLayout.Params params) {
        return SpringLayout.build(scene, 0, 0, x, 7, Direction.EAST, params, WALL_HEIGHT, MIN_WATER_Y, MAX_FLOOR_Y).isPresent();
    }

    private static SpringLayout.Params params(RandomSource random) {
        int length = 3 + random.nextInt(4);
        int bendAt = random.nextBoolean() ? 1 + random.nextInt(length - 2) : 0;
        int width = 2 + random.nextInt(2);
        return new SpringLayout.Params(3 + random.nextInt(6), length, bendAt, random.nextBoolean(), width, 2 + random.nextInt(2),
                random.nextInt(width), 1 + random.nextInt(2));
    }

    private static void check(Scene scene, SpringLayout.Layout layout, SpringLayout.Params params, String where) {
        Map<BlockPos, SpringLayout.Water> water = new HashMap<>();
        for (SpringLayout.Water w : layout.water()) {
            water.put(w.pos(), w);
        }
        assertEquals(SpringLayout.Kind.SOURCE, layout.water().get(0).kind(), where);
        for (BlockPos pos : water.keySet()) {
            assertTrue(inChunk(pos), where + " water outside the chunk margin " + pos);
            assertTrue(pos.getY() >= MIN_WATER_Y - 1, where + " water line " + pos);
        }
        for (BlockPos pos : layout.rim()) {
            assertTrue(inChunk(pos), where + " rim " + pos);
            assertTrue(scene.floor(pos.getX(), pos.getY(), pos.getZ()), where + " rim on floor " + pos);
        }
        long pool = layout.water().stream().filter(w -> w.kind() == SpringLayout.Kind.POOL).count();
        assertEquals((long) params.poolWidth() * params.poolLength() * params.poolDepth(), pool, where);
        long fall = layout.water().stream().filter(w -> w.kind() == SpringLayout.Kind.FALLING).count();
        assertEquals(params.springHeight(), fall, where + " the fall plus its landing in the trench");

        // Held in: something solid or water below every block, and beside every block but the fall.
        int fallX = 0;
        int fallZ = 0;
        int floorY = Integer.MAX_VALUE;
        for (SpringLayout.Water w : layout.water()) {
            if (w.kind() == SpringLayout.Kind.FALLING) {
                fallX = w.pos().getX();
                fallZ = w.pos().getZ();
                floorY = Math.min(floorY, w.pos().getY());
            }
        }
        for (SpringLayout.Water w : layout.water()) {
            BlockPos pos = w.pos();
            assertTrue(water.containsKey(pos.below()) || scene.solid(pos.getX(), pos.getY() - 1, pos.getZ()), where + " nothing below " + pos);
            boolean inFall = pos.getX() == fallX && pos.getZ() == fallZ && pos.getY() > floorY;
            if (!inFall) {
                for (Direction side : Direction.Plane.HORIZONTAL) {
                    BlockPos next = pos.relative(side);
                    assertTrue(water.containsKey(next) || scene.solid(next.getX(), next.getY(), next.getZ()), where + " leak beside " + pos);
                }
            }
        }

        // The layout is where vanilla's water comes to rest, starting from the source and the pool alone...
        Set<BlockPos> open = new HashSet<>(water.keySet());
        open.addAll(layout.carve());
        for (BlockPos pos : layout.carve()) {
            assertTrue(inChunk(pos), where + " cleft outside the chunk margin " + pos);
            assertFalse(water.containsKey(pos), where + " cleft and water overlap");
            assertTrue(scene.solid(pos.getX(), pos.getY(), pos.getZ()) || scene.air(pos.getX(), pos.getY(), pos.getZ()), where);
        }
        Sim fromSources = new Sim(scene, open);
        for (SpringLayout.Water w : layout.water()) {
            if (w.kind() == SpringLayout.Kind.SOURCE || w.kind() == SpringLayout.Kind.POOL) {
                fromSources.place(w.pos(), Fluid.still());
            }
        }
        fromSources.run(water.size() + 40);
        assertEquals(expected(layout), fromSources.water, where + " settled state");
        // ...and nothing moves when every block of it ticks again (a block update next to it).
        Sim settled = new Sim(scene, open);
        for (SpringLayout.Water w : layout.water()) {
            settled.place(w.pos(), fluid(w));
        }
        settled.run(water.size() + 40);
        assertEquals(expected(layout), settled.water, where + " stable");
    }

    private static Map<BlockPos, Fluid> expected(SpringLayout.Layout layout) {
        Map<BlockPos, Fluid> map = new HashMap<>();
        for (SpringLayout.Water w : layout.water()) {
            map.put(w.pos(), fluid(w));
        }
        return map;
    }

    private static Fluid fluid(SpringLayout.Water w) {
        return switch (w.kind()) {
            case SOURCE, POOL -> Fluid.still();
            case FALLING -> new Fluid(8, true, false);
            case FLOWING -> new Fluid(w.amount(), false, false);
        };
    }

    private static boolean inChunk(BlockPos pos) {
        return pos.getX() >= 1 && pos.getX() <= 14 && pos.getZ() >= 1 && pos.getZ() <= 14;
    }

    enum Cell { AIR, STONE, FLOOR, PLANT }

    /** Water as vanilla's {@code FluidState}: amount 1–8, falling, source. */
    record Fluid(int amount, boolean falling, boolean source) {
        static Fluid still() {
            return new Fluid(8, false, true);
        }
    }

    /**
     * A synthetic canyon around chunk 0, 0: floor at a random height (bumps, dips, a cave here and there), a wall of
     * random height on one side with a ragged face and holes, sometimes a second wall close by (a slot).
     */
    static final class Scene implements SpringLayout.Terrain {
        final Map<BlockPos, Cell> blocks = new HashMap<>();
        private final Map<Long, Integer> heights = new HashMap<>();
        private final int floorY;

        private Scene(int floorY) {
            this.floorY = floorY;
        }

        /** Flat floor; a straight wall of {@code wallHeight} (0: none) west of {@code wallLine}. */
        static Scene plain(int floorY, int wallLine, int wallHeight) {
            Scene scene = new Scene(floorY);
            for (int x = -16; x < 32; x++) {
                for (int z = -16; z < 32; z++) {
                    boolean wall = wallHeight > 0 && x < wallLine;
                    scene.column(x, z, wall ? floorY + wallHeight : floorY, wall);
                }
            }
            return scene;
        }

        static Scene random(RandomSource random) {
            int floorY = 63 + random.nextInt(26);
            Scene scene = new Scene(floorY);
            Direction wallSide = Direction.from2DDataValue(random.nextInt(4));
            int line = 1 + random.nextInt(10);
            int wallHeight = 4 + random.nextInt(36);
            int slot = random.nextFloat() < 0.2F ? 3 + random.nextInt(6) : -1;
            // Blocks the wall rises per block away from its foot: sheer, or a slope as real canyon walls have.
            int slope = new int[]{2, 3, 4, 6, 40}[random.nextInt(5)];
            Map<Integer, Integer> jag = new HashMap<>();
            for (int x = -16; x < 32; x++) {
                for (int z = -16; z < 32; z++) {
                    // Distance from the wall line, measured away from the wall side.
                    int along = wallSide.getAxis() == Direction.Axis.X ? z : x;
                    int across = wallSide.getAxis() == Direction.Axis.X ? x : z;
                    int out = wallSide.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? across - line : line - across;
                    int shift = jag.computeIfAbsent(along, a -> random.nextInt(3) - 1);
                    boolean wall = out + shift < 0 || slot > 0 && out + shift > slot;
                    int into = slot > 0 && out + shift > slot ? out + shift - slot : -(out + shift);
                    int height = wall ? floorY + Math.min(wallHeight, slope * into) + random.nextInt(3) : floorY;
                    if (!wall) {
                        float bump = random.nextFloat();
                        height += bump < 0.06F ? 1 : bump < 0.09F ? -1 : 0;
                    }
                    scene.column(x, z, height, wall);
                    if (wall && random.nextFloat() < 0.04F) {
                        scene.blocks.put(new BlockPos(x, floorY + 1 + random.nextInt(Math.max(1, wallHeight)), z), Cell.AIR);
                    }
                    if (!wall && random.nextFloat() < 0.02F) {
                        scene.blocks.put(new BlockPos(x, height - 2, z), Cell.AIR);
                    }
                    if (!wall && random.nextFloat() < 0.03F) {
                        scene.blocks.put(new BlockPos(x, height + 1, z), Cell.PLANT);
                    }
                }
            }
            return scene;
        }

        void column(int x, int z, int height, boolean wall) {
            heights.put(key(x, z), height);
            for (int y = floorY - 6; y <= height; y++) {
                blocks.put(new BlockPos(x, y, z), !wall && y >= height - 1 ? Cell.FLOOR : Cell.STONE);
            }
        }

        Cell cell(int x, int y, int z) {
            Cell cell = blocks.get(new BlockPos(x, y, z));
            if (cell != null) {
                return cell;
            }
            return y < floorY - 6 ? Cell.STONE : Cell.AIR;
        }

        @Override
        public int surface(int x, int z) {
            Integer height = heights.get(key(x, z));
            return height == null ? floorY : height;
        }

        @Override
        public boolean solid(int x, int y, int z) {
            Cell cell = cell(x, y, z);
            return cell == Cell.STONE || cell == Cell.FLOOR;
        }

        @Override
        public boolean floor(int x, int y, int z) {
            return cell(x, y, z) == Cell.FLOOR;
        }

        @Override
        public boolean air(int x, int y, int z) {
            return cell(x, y, z) == Cell.AIR;
        }

        @Override
        public boolean gravity(int x, int y, int z) {
            return cell(x, y, z) == Cell.FLOOR;
        }

        private static long key(int x, int z) {
            return (long) x << 32 | z & 0xFFFFFFFFL;
        }
    }

    /**
     * Vanilla's water ({@code FlowingFluid.tick}, {@code spread}, {@code getNewLiquid}, {@code getSpread},
     * {@code getSlopeDistance}) on the scene, the given blocks dug out (air). Ticks run in schedule order with one delay.
     */
    static final class Sim {
        final Map<BlockPos, Fluid> water = new HashMap<>();
        private final Scene scene;
        private final Set<BlockPos> dug;
        private LinkedHashSet<BlockPos> scheduled = new LinkedHashSet<>();

        Sim(Scene scene, Set<BlockPos> dug) {
            this.scene = scene;
            this.dug = dug;
        }

        void place(BlockPos pos, Fluid fluid) {
            water.put(pos, fluid);
            scheduled.add(pos);
        }

        void run(int maxWater) {
            for (int round = 0; !scheduled.isEmpty(); round++) {
                if (round > 2000 || water.size() > maxWater) {
                    fail("water does not come to rest (leak?), " + water.size() + " blocks");
                }
                LinkedHashSet<BlockPos> now = scheduled;
                scheduled = new LinkedHashSet<>();
                for (BlockPos pos : now) {
                    Fluid fluid = water.get(pos);
                    if (fluid != null) {
                        tick(pos, fluid);
                    }
                }
            }
        }

        private boolean solid(BlockPos pos) {
            return !dug.contains(pos) && scene.solid(pos.getX(), pos.getY(), pos.getZ());
        }

        private boolean isSource(BlockPos pos) {
            Fluid fluid = water.get(pos);
            return fluid != null && fluid.source();
        }

        private void set(BlockPos pos, Fluid fluid) {
            if (fluid == null) {
                water.remove(pos);
            } else {
                water.put(pos, fluid);
            }
            scheduled.add(pos);
            for (Direction side : Direction.values()) {
                if (water.containsKey(pos.relative(side))) {
                    scheduled.add(pos.relative(side));
                }
            }
        }

        private void tick(BlockPos pos, Fluid state) {
            if (!state.source()) {
                Fluid next = newLiquid(pos);
                if (next == null) {
                    set(pos, null);
                    return;
                }
                if (!next.equals(state)) {
                    state = next;
                    set(pos, next);
                }
            }
            spread(pos, state);
        }

        private Fluid newLiquid(BlockPos pos) {
            int highest = 0;
            int sources = 0;
            for (Direction side : Direction.Plane.HORIZONTAL) {
                Fluid fluid = water.get(pos.relative(side));
                if (fluid != null) {
                    sources += fluid.source() ? 1 : 0;
                    highest = Math.max(highest, fluid.amount());
                }
            }
            if (sources >= 2 && (solid(pos.below()) || isSource(pos.below()))) {
                return Fluid.still();
            }
            if (water.containsKey(pos.above())) {
                return new Fluid(8, true, false);
            }
            int amount = highest - 1;
            return amount <= 0 ? null : new Fluid(amount, false, false);
        }

        /** Water can go into a block that is neither solid nor water already (vanilla never replaces water by spreading). */
        private boolean canSpreadTo(BlockPos to) {
            return !water.containsKey(to) && !solid(to);
        }

        private void spread(BlockPos pos, Fluid state) {
            BlockPos below = pos.below();
            if (canSpreadTo(below)) {
                set(below, newLiquid(below));
                if (sourceNeighbours(pos) >= 3) {
                    spreadToSides(pos, state);
                }
            } else if (state.source() || !isWaterHole(below)) {
                spreadToSides(pos, state);
            }
        }

        private int sourceNeighbours(BlockPos pos) {
            int count = 0;
            for (Direction side : Direction.Plane.HORIZONTAL) {
                count += isSource(pos.relative(side)) ? 1 : 0;
            }
            return count;
        }

        private boolean isWaterHole(BlockPos below) {
            return !solid(below);
        }

        private boolean canPassThrough(BlockPos to) {
            return !isSource(to) && !solid(to);
        }

        private void spreadToSides(BlockPos pos, Fluid state) {
            int amount = state.falling() ? 7 : state.amount() - 1;
            if (amount <= 0) {
                return;
            }
            int best = 1000;
            List<Direction> chosen = new ArrayList<>();
            for (Direction side : Direction.Plane.HORIZONTAL) {
                BlockPos next = pos.relative(side);
                if (canPassThrough(next)) {
                    int distance = isWaterHole(next.below()) ? 0 : slopeDistance(next, 1, side.getOpposite());
                    if (distance < best) {
                        chosen.clear();
                    }
                    if (distance <= best) {
                        chosen.add(side);
                        best = distance;
                    }
                }
            }
            for (Direction side : chosen) {
                BlockPos next = pos.relative(side);
                if (canSpreadTo(next)) {
                    Fluid fluid = newLiquid(next);
                    if (fluid != null) {
                        set(next, fluid);
                    }
                }
            }
        }

        private int slopeDistance(BlockPos pos, int distance, Direction from) {
            int best = 1000;
            for (Direction side : Direction.Plane.HORIZONTAL) {
                if (side == from) {
                    continue;
                }
                BlockPos next = pos.relative(side);
                if (canPassThrough(next)) {
                    if (isWaterHole(next.below())) {
                        return distance;
                    }
                    if (distance < 4) {
                        best = Math.min(best, slopeDistance(next, distance + 1, side.getOpposite()));
                    }
                }
            }
            return best;
        }
    }

    @Test
    void simSpreadsLikeVanillaOnOpenGround() {
        // Sanity check of the reference: a source on flat ground spreads 7 out, amount dropping by one per block.
        Scene flat = Scene.plain(70, 0, 0);
        Sim sim = new Sim(flat, new HashSet<>());
        sim.place(new BlockPos(8, 71, 8), Fluid.still());
        sim.run(400);
        assertEquals(new Fluid(1, false, false), sim.water.get(new BlockPos(15, 71, 8)));
        assertEquals(new Fluid(4, false, false), sim.water.get(new BlockPos(10, 71, 10)));
        assertFalse(sim.water.containsKey(new BlockPos(16, 71, 8)));
        assertEquals(1 + 4 * (7 + 6 + 5 + 4 + 3 + 2 + 1), sim.water.size());
    }
}
