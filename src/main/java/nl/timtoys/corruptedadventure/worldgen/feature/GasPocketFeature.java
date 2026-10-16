package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * {@code corruptedadventure:gas_pocket} (package 76, Q10 = A): a closed bubble of swamp gas just under the peat crust.
 * The pocket is a small box ({@code width} × {@code width} × {@code height}) whose top lies {@code crust} blocks under
 * the ground at the origin (the top block under {@code WORLD_SURFACE_WG}); it lies in the origin's chunk at least one
 * block from its edges (every block {@link #closed} reads is the own chunk's, so two pockets of neighbouring chunks
 * never judge each other), and is only placed when all of it is solid ground now, and every block around it is solid and dry ({@link #closed}): the
 * gas never touches air, water or a cave, so it stays hidden until someone digs (or lights) it.
 */
public final class GasPocketFeature extends Feature<GasPocketConfiguration> {
    public static final int MAX_WIDTH = 5;

    public GasPocketFeature(Codec<GasPocketConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<GasPocketConfiguration> context) {
        WorldGenLevel level = context.level();
        GasPocketConfiguration config = context.config();
        RandomSource random = context.random();
        int sizeX = config.width().sample(random);
        int sizeZ = config.width().sample(random);
        int height = config.height().sample(random);
        int crust = config.crust().sample(random);
        BlockPos origin = context.origin();
        int top = origin.getY() - 1 - crust;
        int x0 = SuckingMudPatchFeature.start(origin.getX(), sizeX, 1);
        int z0 = SuckingMudPatchFeature.start(origin.getZ(), sizeZ, 1);
        Set<BlockPos> pocket = box(x0, top - height + 1, z0, sizeX, height, sizeZ);
        Predicate<BlockPos> solid = pos -> solid(level.getBlockState(pos));
        for (BlockPos pos : pocket) {
            if (!solid.test(pos)) {
                return false;
            }
        }
        if (!closed(pocket, solid)) {
            return false;
        }
        for (BlockPos pos : pocket) {
            level.setBlock(pos, config.gas(), Block.UPDATE_CLIENTS);
        }
        return true;
    }

    private static boolean solid(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && !state.canBeReplaced();
    }

    /** The blocks of a box with its lowest corner at (x0, y0, z0). */
    static Set<BlockPos> box(int x0, int y0, int z0, int sizeX, int sizeY, int sizeZ) {
        Set<BlockPos> box = new LinkedHashSet<>();
        for (int x = x0; x < x0 + sizeX; x++) {
            for (int y = y0; y < y0 + sizeY; y++) {
                for (int z = z0; z < z0 + sizeZ; z++) {
                    box.add(new BlockPos(x, y, z));
                }
            }
        }
        return box;
    }

    /** Whether every block around the pocket (all six sides) is {@code solid} or more pocket. */
    static boolean closed(Set<BlockPos> pocket, Predicate<BlockPos> solid) {
        for (BlockPos pos : pocket) {
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!pocket.contains(next) && !solid.test(next)) {
                    return false;
                }
            }
        }
        return true;
    }
}
