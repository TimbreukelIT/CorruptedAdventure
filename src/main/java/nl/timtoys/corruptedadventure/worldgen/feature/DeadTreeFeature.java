package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntBinaryOperator;

/**
 * {@code corruptedadventure:dead_tree} (ring 4 decor B, 2026-10-19): a crooked dead tree of the blighted band. A bare
 * trunk that leans up to {@link #MAX_LEAN} out of line as it rises (from the floor of water up to {@code max_water}
 * deep, or from the ground), a few bare branches rising sideways, root knobs at the foot, vines hanging from the
 * branches and moss or mushrooms on the tops of the wood above the water.
 * <p>
 * A ground object ({@link GroundObjects}), so its shape does not depend on what generated before: every draw comes
 * from its own random (position and world seed), it reads only the frozen heightmaps and the pure terrain, and it stays
 * in its own chunk (the lean turns to a direction whose trunk fits the chunk; a branch ends at the chunk's edge). The
 * wood merges with the trees by their priority ({@link TreeOverlap#wins}); vines, moss and mushrooms only fill air.
 * It never grows into an island above it.
 */
public final class DeadTreeFeature extends Feature<DeadTreeConfiguration> {
    static final int MAX_LEAN = 3;
    static final int MAX_BRANCH = 5;
    /** Room kept under an island's bottom. */
    private static final int ISLAND_CLEARANCE = 4;
    private static final long SALT = 0xDEAD_7EE5L;
    /** The eight lean and branch directions (x, z). */
    private static final int[][] DIRECTIONS = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}, {1, 1}, {-1, 1}, {-1, -1}, {1, -1}};

    public DeadTreeFeature(Codec<DeadTreeConfiguration> codec) {
        super(codec);
    }

    /** The planned tree: its wood (trunk first) and its vines and growth, top first. */
    record Shape(Map<BlockPos, BlockState> wood, List<LandmarkPlan.Decor> decor) {
    }

    @Override
    public boolean place(FeaturePlaceContext<DeadTreeConfiguration> context) {
        WorldGenLevel level = context.level();
        DeadTreeConfiguration config = context.config();
        BlockPos origin = context.origin();
        RandomSource random = GroundObjects.random(level.getSeed(), origin, SALT);
        int height = config.height().sample(random);
        int lean = config.lean().sample(random);
        int direction = random.nextInt(DIRECTIONS.length);
        long shapeSeed = random.nextLong();

        int x = origin.getX();
        int z = origin.getZ();
        int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        if (surface - floor > config.maxWater() || !rooted(level.getBlockState(new BlockPos(x, floor - 1, z)))) {
            return false;
        }
        int trunk = surface - floor + height;
        if (!underIslands(level, x, z, floor + trunk + MAX_BRANCH)) {
            return false;
        }
        // The first direction (from the drawn one on) whose trunk stays in the chunk, else upright.
        for (int k = 0; k <= DIRECTIONS.length; k++) {
            int[] d = k < DIRECTIONS.length ? DIRECTIONS[(direction + k) % DIRECTIONS.length] : new int[]{0, 0};
            Shape shape = shape(config, new XoroshiroRandomSource(shapeSeed), x, floor, z, surface, trunk, lean, d,
                    (kx, kz) -> level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, kx, kz));
            if (shape != null) {
                GroundObjects.placeWood(level, IsolatedTreeFeature.terrain(level), shape.wood());
                // Whether or not a block of ours lost to another tree's wood, the support is wood (or a ground
                // object) either way: the growth goes where it survives, the same in either order.
                for (LandmarkPlan.Decor decor : shape.decor()) {
                    GroundObjects.placeDecor(level, decor.pos(), decor.state());
                }
                return true;
            }
        }
        return false;
    }

    /** Soil a tree stands on (not stone, bone, a puddle or sucking mud). */
    private static boolean rooted(BlockState below) {
        return below.is(BlockTags.DIRT) || SludgePuddle.soil(below) || below.is(BlockTags.SAND);
    }

    /** Whether no island hangs lower than {@code top} plus the clearance over the column (pure terrain). */
    private static boolean underIslands(WorldGenLevel level, int x, int z, int top) {
        IslandField field = Ring4Terrain.field(level);
        if (field == null) {
            return true;
        }
        for (IslandField.Island island : field.islandsAt(x, z)) {
            if (island.bottom() < top + ISLAND_CLEARANCE) {
                return false;
            }
        }
        return true;
    }

    /**
     * The tree leaning along {@code d} by {@code lean}, or null when its trunk leaves the chunk. Pure in its
     * arguments; every block of it lies in the chunk of (x, z).
     */
    static Shape shape(DeadTreeConfiguration config, RandomSource random, int x, int floor, int z, int surface, int trunk, int lean, int[] d,
                       IntBinaryOperator floorAt) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        Map<BlockPos, BlockState> wood = new LinkedHashMap<>();
        List<BlockPos> trunkBlocks = new ArrayList<>();
        BlockState log = config.log().trySetValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        int previous = 0;
        for (int i = 0; i < trunk; i++) {
            double f = trunk > 1 ? i / (double) (trunk - 1) : 0.0;
            int off = (int) Math.round(lean * Math.pow(f, 1.5));
            int tx = x + d[0] * off;
            int tz = z + d[1] * off;
            if (tx >> 4 != chunkX || tz >> 4 != chunkZ) {
                return null;
            }
            if (off != previous && d[0] != 0 && d[1] != 0) {
                // A diagonal step of the lean would join the trunk at a corner only: one block joins it along an edge.
                wood.put(new BlockPos(tx, floor + i, z + d[1] * previous), log);
            }
            previous = off;
            BlockPos pos = new BlockPos(tx, floor + i, tz);
            wood.put(pos, log);
            trunkBlocks.add(pos);
        }

        // Root knobs beside the foot, only where that column's floor is level with the trunk's (no knob floats or
        // sinks into a bank; the frozen heightmap of the own chunk).
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos knob = new BlockPos(x + side.getStepX(), floor, z + side.getStepZ());
            if (random.nextFloat() < 0.3F && inChunk(knob, chunkX, chunkZ) && floorAt.applyAsInt(knob.getX(), knob.getZ()) == floor) {
                wood.putIfAbsent(knob, config.wood());
            }
        }

        // Bare branches from the upper part above the water (a crown of dead limbs), rising one block every one or two.
        int lowest = Math.max(surface - floor + 1, 2);
        lowest = Math.min(trunk - 2, lowest + (trunk - lowest) * 2 / 5);
        int count = config.branches().sample(random);
        List<BlockPos> branchBlocks = new ArrayList<>();
        for (int b = 0; b < count; b++) {
            int from = lowest + random.nextInt(Math.max(1, trunk - lowest - 1));
            int[] dir = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
            int length = config.branchLength().sample(random);
            int riseEvery = 1 + random.nextInt(2);
            boolean twig = random.nextBoolean();
            BlockPos at = trunkBlocks.get(Math.min(from, trunkBlocks.size() - 1));
            // Every step joins the last face to face: a diagonal step through the block beside it, a rise straight up.
            for (int k = 1; k <= length; k++) {
                BlockPos side = at.offset(dir[0], 0, 0);
                BlockPos next = at.offset(dir[0], 0, dir[1]);
                if (!inChunk(next, chunkX, chunkZ) || !inChunk(side, chunkX, chunkZ)) {
                    break;
                }
                if (dir[0] != 0 && dir[1] != 0) {
                    wood.putIfAbsent(side, config.wood());
                    branchBlocks.add(side);
                }
                wood.putIfAbsent(next, config.wood());
                branchBlocks.add(next);
                if (k % riseEvery == 0) {
                    next = next.above();
                    wood.putIfAbsent(next, config.wood());
                    branchBlocks.add(next);
                }
                at = next;
            }
            if (twig && !at.equals(trunkBlocks.get(Math.min(from, trunkBlocks.size() - 1)))) {
                wood.putIfAbsent(at.above(), config.wood());
            }
        }

        List<LandmarkPlan.Decor> decor = new ArrayList<>();
        // Vines down from the branches, a few down the trunk above the water.
        for (BlockPos branch : branchBlocks) {
            vines(decor, wood, random, branch, config.vineChance(), config.vineLength().sample(random), surface, chunkX, chunkZ);
        }
        for (BlockPos pos : trunkBlocks) {
            vines(decor, wood, random, pos, config.vineChance() * 0.4F, config.vineLength().sample(random), surface, chunkX, chunkZ);
        }
        // Moss and mushrooms on the tops of the wood above the water.
        for (BlockPos pos : wood.keySet()) {
            boolean grows = random.nextFloat() < config.decorChance();
            BlockState state = config.decor().getState(random, pos);
            if (grows && pos.getY() >= surface && !wood.containsKey(pos.above())) {
                decor.add(new LandmarkPlan.Decor(pos.above(), state, pos));
            }
        }
        return new Shape(wood, decor);
    }

    /** With {@code chance}, a strand of {@code length} vines beside {@code support} on a drawn side (top first). */
    private static void vines(List<LandmarkPlan.Decor> decor, Map<BlockPos, BlockState> wood, RandomSource random, BlockPos support,
                              float chance, int length, int surface, int chunkX, int chunkZ) {
        boolean hangs = random.nextFloat() < chance;
        Direction side = Direction.from2DDataValue(random.nextInt(4));
        BlockPos at = support.relative(side);
        if (!hangs || support.getY() < surface || wood.containsKey(at) || !inChunk(at, chunkX, chunkZ)) {
            return;
        }
        BlockState vine = Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(side.getOpposite()), true);
        BlockPos above = support;
        for (int i = 0; i < length && !wood.containsKey(at) && at.getY() >= surface; i++) {
            decor.add(new LandmarkPlan.Decor(at, vine, above));
            above = at;
            at = at.below();
        }
    }

    private static boolean inChunk(BlockPos pos, int chunkX, int chunkZ) {
        return pos.getX() >> 4 == chunkX && pos.getZ() >> 4 == chunkZ;
    }
}
