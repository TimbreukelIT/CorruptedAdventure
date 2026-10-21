package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@code corruptedadventure:fallen_tree} (ring 4 decor proef, 2026-10-19): a stump, a fallen trunk lying beside it, or
 * one of the two, with moss and mushrooms on the wood. The trunk lies on level dry ground only (it ends where the ground
 * steps or turns wet) and stays in its own chunk. A ground object ({@link GroundObjects}): its own random and the frozen
 * heightmaps decide; the wood merges with the trees by their own priority ({@link TreeOverlap#wins}), the growth on it
 * only fills open air.
 */
public final class FallenTreeFeature extends Feature<FallenTreeConfiguration> {
    static final int MAX_LENGTH = 7;
    /** A trunk shorter than this (the ground stepped right away) is left out. */
    static final int MIN_LENGTH = 2;
    private static final long SALT = 0xFA11_E7L;

    public FallenTreeFeature(Codec<FallenTreeConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<FallenTreeConfiguration> context) {
        WorldGenLevel level = context.level();
        FallenTreeConfiguration config = context.config();
        RandomSource random = GroundObjects.random(level.getSeed(), context.origin(), SALT);
        float kind = random.nextFloat();
        boolean stump = kind < config.stumpOnly() || kind >= config.stumpOnly() + config.trunkOnly();
        boolean trunk = kind >= config.stumpOnly();
        int stumpHeight = config.stumpHeight().sample(random);
        int length = config.length().sample(random);
        int gap = stump ? 1 + random.nextInt(2) : 0;
        Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        BlockPos origin = inChunk(context.origin(), direction, trunk ? gap + length : 0);
        int foot = GroundObjects.surface(level, origin.getX(), origin.getZ());
        if (!GroundObjects.dry(level, origin.getX(), origin.getZ()) || Ring4Terrain.onDebris(level, origin.getX(), foot, origin.getZ())) {
            return false;
        }
        Map<BlockPos, BlockState> wood = new LinkedHashMap<>();
        List<BlockPos> tops = new ArrayList<>();
        if (stump) {
            BlockState upright = config.log().trySetValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            for (int y = 0; y < stumpHeight; y++) {
                wood.put(origin.above(foot - origin.getY() + y), upright);
            }
            tops.add(origin.above(foot - origin.getY() + stumpHeight - 1));
        }
        if (trunk) {
            BlockState lying = config.log().trySetValue(RotatedPillarBlock.AXIS, direction.getAxis());
            List<BlockPos> logs = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                BlockPos at = origin.relative(direction, gap + i);
                if (GroundObjects.surface(level, at.getX(), at.getZ()) != foot || !GroundObjects.dry(level, at.getX(), at.getZ())) {
                    break;
                }
                logs.add(new BlockPos(at.getX(), foot, at.getZ()));
            }
            if (logs.size() >= MIN_LENGTH) {
                logs.forEach(pos -> wood.put(pos, lying));
                tops.addAll(logs);
            }
        }
        if (wood.isEmpty()) {
            return false;
        }
        // Draw the growth for every planned top, whether its log won or not.
        List<BlockState> growth = new ArrayList<>(tops.size());
        for (BlockPos top : tops) {
            boolean grows = random.nextFloat() < config.decorChance();
            BlockState state = config.decor().getState(random, top);
            growth.add(grows ? state : null);
        }
        Set<BlockPos> placed = GroundObjects.placeWood(level, IsolatedTreeFeature.terrain(level), wood);
        for (int i = 0; i < tops.size(); i++) {
            if (growth.get(i) != null && placed.contains(tops.get(i))) {
                GroundObjects.placeDecor(level, tops.get(i).above(), growth.get(i));
            }
        }
        return true;
    }

    /** {@code origin} moved along {@code direction}'s axis so that {@code reach} blocks that way stay in its chunk. */
    static BlockPos inChunk(BlockPos origin, Direction direction, int reach) {
        int min = (direction.getAxis() == Direction.Axis.X ? origin.getX() : origin.getZ()) & ~15;
        int at = direction.getAxis() == Direction.Axis.X ? origin.getX() : origin.getZ();
        int moved = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? Math.min(at, min + 15 - reach) : Math.max(at, min + reach);
        return direction.getAxis() == Direction.Axis.X ? new BlockPos(moved, origin.getY(), origin.getZ())
                : new BlockPos(origin.getX(), origin.getY(), moved);
    }
}
