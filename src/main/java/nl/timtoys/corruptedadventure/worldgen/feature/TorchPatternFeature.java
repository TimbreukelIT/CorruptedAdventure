package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.Ring5Field;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code corruptedadventure:torch_pattern} (94, V5-A): redstone torches standing on the dead ground in a pattern no one
 * would make by chance: a cross, a ring, a row, or an arrow that points to the nearest castle site (the player can follow
 * it). Their light (7) does not hold the void back (that needs 12). All or nothing: on ground within one block of the
 * middle's height, else the pattern is not placed. A ground object in its own chunk (reach {@link #REACH}).
 */
public final class TorchPatternFeature extends Feature<NoneFeatureConfiguration> {
    static final int REACH = 3;
    private static final long SALT = 0x70_7C4E5L;
    private static final int FLAGS = Block.UPDATE_CLIENTS;
    /** The eight arrow directions (x, z), east first, clockwise on a north-up map. */
    private static final int[][] DIRECTIONS = {{1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}};

    public TorchPatternFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    enum Pattern { CROSS, RING, ROW, ARROW }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = GroundObjects.fit(context.origin(), REACH);
        RandomSource random = GroundObjects.random(level.getSeed(), origin, SALT);
        Ring5World world = Ring5Terrain.world(level);
        if (world == null) {
            return false;
        }
        Pattern pattern = Pattern.values()[random.nextInt(Pattern.values().length)];
        List<int[]> offsets = offsets(pattern, pattern == Pattern.ARROW ? towardsCastle(world, origin.getX(), origin.getZ())
                : DIRECTIONS[random.nextInt(2) * 2]);
        int middle = GroundObjects.surface(level, origin.getX(), origin.getZ());
        List<BlockPos> torches = new ArrayList<>();
        for (int[] offset : offsets) {
            int x = origin.getX() + offset[0];
            int z = origin.getZ() + offset[1];
            int y = GroundObjects.surface(level, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            BlockPos below = pos.below();
            if (Math.abs(y - middle) > 1 || !Ring5Placements.free(level.getBlockState(pos))
                    || !level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                return false;
            }
            torches.add(pos);
        }
        for (BlockPos pos : torches) {
            level.setBlock(pos, Blocks.REDSTONE_TORCH.defaultBlockState(), FLAGS);
        }
        return true;
    }

    /** The 8-way direction from (x, z) to the nearest castle site. */
    static int[] towardsCastle(Ring5World world, int x, int z) {
        Ring5Field.CastleSite nearest = world.nearestCastle(x, z);
        if (nearest == null) {
            return DIRECTIONS[0];
        }
        double angle = Math.atan2(nearest.z() - z, nearest.x() - x);
        int index = Math.floorMod((int) Math.round(angle / (Math.PI / 4.0)), DIRECTIONS.length);
        return DIRECTIONS[index];
    }

    /** The torches of a pattern round its middle; {@code d} is the arrow's (or the row's) direction. */
    static List<int[]> offsets(Pattern pattern, int[] d) {
        List<int[]> offsets = new ArrayList<>();
        switch (pattern) {
            case CROSS -> {
                offsets.add(new int[]{0, 0});
                for (int k = 1; k <= 2; k++) {
                    offsets.add(new int[]{k, 0});
                    offsets.add(new int[]{-k, 0});
                    offsets.add(new int[]{0, k});
                    offsets.add(new int[]{0, -k});
                }
            }
            case RING -> {
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        double r = Math.hypot(dx, dz);
                        if (r > 1.5 && r < 2.5) {
                            offsets.add(new int[]{dx, dz});
                        }
                    }
                }
            }
            case ROW -> {
                for (int k = -2; k <= 2; k++) {
                    offsets.add(new int[]{d[0] * k, d[1] * k});
                }
            }
            case ARROW -> {
                // The shaft from behind the middle to the tip, and the two barbs one step back from the tip.
                for (int k = -2; k <= 2; k++) {
                    offsets.add(new int[]{d[0] * k, d[1] * k});
                }
                int[] perpendicular = {-d[1], d[0]};
                offsets.add(new int[]{d[0] + perpendicular[0], d[1] + perpendicular[1]});
                offsets.add(new int[]{d[0] - perpendicular[0], d[1] - perpendicular[1]});
            }
        }
        return offsets;
    }
}
