package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A ragged patch of withered ground: grass and dirt turn into {@code surface}/{@code subsurface}, standing trees lose
 * their leaves and their logs die, small plants disappear, and a few dead trees and bushes grow on it. Ground the
 * patch may not replace (water, sand, stone) is left alone, so patches follow the terrain.
 * <p>
 * Everything stays within {@code MAX_RADIUS + CROWN_MARGIN} blocks of the origin, plus the reach of a dead tree inside
 * the patch: well within the 3×3 chunks a feature may touch.
 */
public final class BarePatchFeature extends Feature<BarePatchConfiguration> {
    public static final int MAX_RADIUS = 9;
    /** Leaves this far beyond the edge still wither, with a falling chance: a ragged crown edge. */
    private static final int CROWN_MARGIN = 3;
    /** How far a column is followed down from the world surface; covers the tall jungle trees of ring 1. */
    private static final int MAX_SCAN = 32;
    /** Points around the edge that each get their own random cut-in, interpolated in between. */
    private static final int EDGE_POINTS = 8;
    /** Own dead trees stand within this fraction of the radius, so their branches stay on the patch. */
    private static final float TREE_SPREAD = 0.7F;
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS;

    public BarePatchFeature(Codec<BarePatchConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<BarePatchConfiguration> context) {
        BarePatchConfiguration config = context.config();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        int radius = config.radius().sample(random);
        float[] edge = new float[EDGE_POINTS];
        for (int i = 0; i < EDGE_POINTS; i++) {
            edge[i] = radius * (1.0F - config.edgeJitter() * random.nextFloat());
        }

        boolean changed = false;
        List<BlockPos> surfaces = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int reach = radius + CROWN_MARGIN;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                float distance = Mth.sqrt(dx * dx + dz * dz);
                float edgeRadius = edgeAt(edge, (float) Mth.atan2(dz, dx));
                if (distance > edgeRadius + CROWN_MARGIN) {
                    continue;
                }
                boolean inside = distance <= edgeRadius;
                float leafChance = inside ? 1.0F : 1.0F - (distance - edgeRadius) / (CROWN_MARGIN + 1);
                pos.set(origin.getX() + dx, 0, origin.getZ() + dz);
                changed |= witherColumn(level, random, config, pos, inside, leafChance, surfaces);
            }
        }

        List<BlockPos> treeSpots = new ArrayList<>();
        float treeReach = TREE_SPREAD * radius;
        for (BlockPos surface : surfaces) {
            if (surface.distToLowCornerSqr(origin.getX(), surface.getY(), origin.getZ()) <= treeReach * treeReach) {
                treeSpots.add(surface);
            }
        }
        int trees = config.treeCount().sample(random);
        for (int i = 0; i < trees && !treeSpots.isEmpty(); i++) {
            BlockPos ground = treeSpots.remove(random.nextInt(treeSpots.size()));
            if (level.isEmptyBlock(ground.above()) && level.isEmptyBlock(ground.above(2))) {
                changed |= config.trees().value().place(level, context.chunkGenerator(), random, ground.above());
            }
        }

        BlockState deadBush = Blocks.DEAD_BUSH.defaultBlockState();
        for (BlockPos ground : surfaces) {
            BlockPos above = ground.above();
            if (random.nextFloat() < config.deadBushChance() && level.isEmptyBlock(above) && deadBush.canSurvive(level, above)) {
                level.setBlock(above, deadBush, UPDATE_FLAGS);
            }
        }
        return changed;
    }

    /** Edge radius in the direction {@code angle} (radians, -π … π). */
    private static float edgeAt(float[] edge, float angle) {
        float index = (angle + Mth.PI) / Mth.TWO_PI * edge.length;
        int below = Mth.floor(index);
        return Mth.lerp(index - below, edge[Math.floorMod(below, edge.length)], edge[Math.floorMod(below + 1, edge.length)]);
    }

    /**
     * Follows one column down from the world surface. Leaves fall with {@code leafChance}; inside the patch logs die
     * and, once replaceable ground is found, plants go and the ground withers. Stops at water or other ground.
     *
     * @return whether anything changed
     */
    private static boolean witherColumn(WorldGenLevel level, RandomSource random, BarePatchConfiguration config,
                                        BlockPos.MutableBlockPos pos, boolean inside, float leafChance, List<BlockPos> surfaces) {
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ()) - 1;
        int bottom = Math.max(top - MAX_SCAN, level.getMinBuildHeight());
        boolean changed = false;
        List<BlockPos> plants = new ArrayList<>();
        for (int y = top; y >= bottom; y--) {
            pos.setY(y);
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.is(Blocks.SNOW)) {
                continue;
            }
            if (state.is(BlockTags.LEAVES)) {
                if (leafChance >= 1.0F || random.nextFloat() < leafChance) {
                    level.setBlock(pos, state.getFluidState().createLegacyBlock(), UPDATE_FLAGS);
                    // A neighbour that already froze its top layer may have put snow on these leaves.
                    BlockPos above = pos.above();
                    if (level.getBlockState(above).is(Blocks.SNOW)) {
                        level.setBlock(above, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
                    }
                    changed = true;
                }
                continue;
            }
            if (!state.getFluidState().isEmpty()) {
                return changed;
            }
            if (state.is(BlockTags.LOGS)) {
                if (inside) {
                    level.setBlock(pos, deadLog(config.deadLog(), state), UPDATE_FLAGS);
                    changed = true;
                }
                continue;
            }
            if (isPlant(state)) {
                if (inside) {
                    plants.add(pos.immutable());
                }
                continue;
            }
            if (inside && state.is(config.replaceable())) {
                for (BlockPos plant : plants) {
                    level.setBlock(plant, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
                }
                witherGround(level, random, config, pos.immutable(), surfaces);
                return true;
            }
            return changed;
        }
        return changed;
    }

    private static void witherGround(WorldGenLevel level, RandomSource random, BarePatchConfiguration config, BlockPos ground,
                                     List<BlockPos> surfaces) {
        BlockState above = level.getBlockState(ground.above());
        // Under a trunk the top stays covered, so it gets the subsurface block (dead grass would only turn to dirt).
        boolean open = above.isAir() || above.is(Blocks.SNOW);
        BlockState top = (open ? config.surface() : config.subsurface()).getState(random, ground);
        if (top.hasProperty(BlockStateProperties.SNOWY)) {
            top = top.setValue(BlockStateProperties.SNOWY, above.is(BlockTags.SNOW));
        }
        level.setBlock(ground, top, UPDATE_FLAGS);
        if (open) {
            surfaces.add(ground);
        }
        int depth = config.subsurfaceDepth().sample(random);
        for (int i = 1; i <= depth; i++) {
            BlockPos below = ground.below(i);
            if (!level.getBlockState(below).is(BlockTags.DIRT)) {
                break;
            }
            level.setBlock(below, config.subsurface().getState(random, below), UPDATE_FLAGS);
        }
    }

    private static BlockState deadLog(Block deadLog, BlockState log) {
        BlockState dead = deadLog.defaultBlockState();
        if (log.hasProperty(BlockStateProperties.AXIS) && dead.hasProperty(BlockStateProperties.AXIS)) {
            dead = dead.setValue(BlockStateProperties.AXIS, log.getValue(BlockStateProperties.AXIS));
        }
        return dead;
    }

    /** Small plants that wither away; anything with water in it never reaches this check. */
    private static boolean isPlant(BlockState state) {
        return state.is(BlockTags.REPLACEABLE_BY_TREES)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SAPLINGS)
                || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.COCOA)
                || state.is(Blocks.BAMBOO)
                || state.is(Blocks.BAMBOO_SAPLING)
                || state.is(Blocks.SUGAR_CANE)
                || state.is(Blocks.MOSS_CARPET)
                || state.is(Blocks.BROWN_MUSHROOM)
                || state.is(Blocks.RED_MUSHROOM);
    }
}
