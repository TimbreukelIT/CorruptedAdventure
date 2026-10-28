package nl.timtoys.corruptedadventure.condition;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathTypeCache;

import java.util.Optional;

/**
 * Where a void teleport may land ({@code docs/plans/ring-5/golf-1-conditions.md} §2): a short hop of
 * {@code [minBlocks, maxBlocks]} horizontally in a random direction, onto a spot where a player stands safely and that
 * never lies toward the void. Server-side, but behind {@link Terrain} so it is testable without a level.
 * <p>
 * A spot (feet block) is safe when
 * <ul>
 *     <li>its column is allowed ({@link Terrain#allowsColumn}: ring 5's inner band, loaded, inside the world border);</li>
 *     <li>feet and head have no collision shape and no fluid, and are open for vanilla's pathfinding (no fire, powder
 *     snow, berry bush, wither rose…);</li>
 *     <li>the floor has a sturdy top face and the feet are {@link PathType#WALKABLE} for vanilla's land pathfinding (so no
 *     magma, campfire, cactus or lava next to it);</li>
 *     <li>under the spot and under each of its 8 neighbours there is a block with a collision shape within
 *     {@link #MAX_DROP} blocks below the floor level and no fluid on the way down: no rift edge, no fall that hurts, no
 *     step into lava.</li>
 * </ul>
 * No safe spot after {@link #ATTEMPTS} directions: no teleport this time. Also the glitch arrow's hop (phase 2 golf 5,
 * {@code entity/GlitchHop}), with columns and a fit of its own.
 */
public final class VoidTeleportTarget {
    /** Directions tried per teleport. */
    static final int ATTEMPTS = 12;
    /** Feet levels tried per direction, from this far above the player's feet … */
    static final int UP = 2;
    /** … down to this far below. */
    static final int DOWN = 3;
    /** The deepest step next to the spot (blocks below its floor): vanilla's fall damage starts above 3. */
    static final int MAX_DROP = 3;

    private VoidTeleportTarget() {
    }

    /** What the search may look at. */
    public interface Terrain {
        /** The blocks; only read inside columns that {@link #allowsColumn} accepted and their neighbours. */
        BlockGetter blocks();

        /**
         * Whether a spot in this column may be a target: in ring 5's inner band, inside the world border, and the column
         * with its 8 neighbours loaded (so reading their blocks never loads or generates a chunk).
         */
        boolean allowsColumn(int x, int z);

        /** Whether what moves fits with its feet in {@code feet}, a spot already found safe for a player. */
        default boolean fits(BlockPos feet) {
            return true;
        }
    }

    /**
     * Whether column {@code x, z} of a real level lies inside the world border with its 8 neighbours loaded, so that
     * reading their blocks never loads or generates a chunk ({@link Terrain#allowsColumn} on a level).
     */
    public static boolean loadedInsideBorder(ServerLevel level, int x, int z) {
        return level.getWorldBorder().isWithinBounds(x + 0.5, z + 0.5)
                && isLoaded(level, x - 1, z - 1) && isLoaded(level, x + 1, z - 1)
                && isLoaded(level, x - 1, z + 1) && isLoaded(level, x + 1, z + 1);
    }

    /** The chunk of this column is loaded (checking the 4 corners of a 3 × 3 covers every chunk it touches). */
    private static boolean isLoaded(ServerLevel level, int x, int z) {
        return level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(x),
                SectionPos.blockToSectionCoord(z)) != null;
    }

    /**
     * A safe feet block {@code minBlocks}–{@code maxBlocks} blocks from the player's feet block (horizontally, between
     * block centres), or empty.
     */
    public static Optional<BlockPos> find(Terrain terrain, BlockPos feet, int minBlocks, int maxBlocks, RandomSource random) {
        PathTypeCache pathTypes = new PathTypeCache(); // fresh per search: never stale after a block change
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double distance = minBlocks + random.nextDouble() * (maxBlocks - minBlocks);
            int dx = (int) Math.round(Math.cos(angle) * distance);
            int dz = (int) Math.round(Math.sin(angle) * distance);
            int squared = dx * dx + dz * dz;
            int targetX = feet.getX() + dx;
            int targetZ = feet.getZ() + dz;
            if (squared < minBlocks * minBlocks || squared > maxBlocks * maxBlocks
                    || !terrain.allowsColumn(targetX, targetZ)) {
                continue;
            }
            for (int y = feet.getY() + UP; y >= feet.getY() - DOWN; y--) {
                pos.set(targetX, y, targetZ);
                if (isSafe(terrain.blocks(), pathTypes, pos) && terrain.fits(pos)) {
                    return Optional.of(pos.immutable());
                }
            }
        }
        return Optional.empty();
    }

    /** Whether a player may stand with the feet in {@code feet}; see the class comment. */
    static boolean isSafe(BlockGetter blocks, PathTypeCache pathTypes, BlockPos feet) {
        BlockPos head = feet.above();
        BlockPos floor = feet.below();
        return isPassable(blocks, pathTypes, feet) && isPassable(blocks, pathTypes, head)
                && blocks.getBlockState(floor).isFaceSturdy(blocks, floor, Direction.UP)
                && isWalkable(blocks, pathTypes, feet)
                && hasNoDropAround(blocks, floor);
    }

    /** No collision shape, no fluid, and open for vanilla's pathfinding (excludes fire, powder snow, berry bush, …). */
    private static boolean isPassable(BlockGetter blocks, PathTypeCache pathTypes, BlockPos pos) {
        BlockState state = blocks.getBlockState(pos);
        return state.getCollisionShape(blocks, pos).isEmpty() && state.getFluidState().isEmpty()
                && pathTypes.getOrCompute(blocks, pos) == PathType.OPEN;
    }

    /**
     * Vanilla's {@code WalkNodeEvaluator.getPathTypeStatic} is {@link PathType#WALKABLE} at the feet: the floor is no
     * danger itself and no neighbour of the feet (3 × 3 × 3, the feet column excluded) is a danger. Rebuilt here because
     * vanilla's version needs a mob; the NeoForge hooks for neighbouring blocks and fluids are asked like vanilla does.
     */
    static boolean isWalkable(BlockGetter blocks, PathTypeCache pathTypes, BlockPos feet) {
        switch (pathTypes.getOrCompute(blocks, feet.below())) {
            case OPEN, WATER, LAVA, WALKABLE, DAMAGE_FIRE, DAMAGE_OTHER, STICKY_HONEY, POWDER_SNOW, DAMAGE_CAUTIOUS,
                 TRAPDOOR -> {
                return false;
            }
            default -> {
            }
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) {
                        continue;
                    }
                    pos.set(feet.getX() + dx, feet.getY() + dy, feet.getZ() + dz);
                    PathType type = pathTypes.getOrCompute(blocks, pos);
                    BlockState state = blocks.getBlockState(pos);
                    PathType hooked = state.getAdjacentBlockPathType(blocks, pos, null, type);
                    if (hooked == null) {
                        hooked = state.getFluidState().getAdjacentBlockPathType(blocks, pos, null, type);
                    }
                    if (hooked != null) {
                        return hooked == PathType.WALKABLE;
                    }
                    switch (type) {
                        case DAMAGE_OTHER, DAMAGE_FIRE, LAVA, WATER, DAMAGE_CAUTIOUS -> {
                            return false;
                        }
                        default -> {
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Under the floor and under each of its 8 neighbours a block with a collision shape within {@link #MAX_DROP}, with
     * only air (or other blocks without collision and fluid) above it: no fluid on the way down, so a step to the side
     * never lands in lava or water.
     */
    static boolean hasNoDropAround(BlockGetter blocks, BlockPos floor) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!hasFloorBelow(blocks, pos, floor.getX() + dx, floor.getY(), floor.getZ() + dz)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean hasFloorBelow(BlockGetter blocks, BlockPos.MutableBlockPos pos, int x, int top, int z) {
        for (int down = 0; down <= MAX_DROP; down++) {
            pos.set(x, top - down, z);
            BlockState state = blocks.getBlockState(pos);
            if (!state.getFluidState().isEmpty()) {
                return false;
            }
            if (!state.getCollisionShape(blocks, pos).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
