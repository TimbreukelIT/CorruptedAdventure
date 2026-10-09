package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicField;

/**
 * {@code corruptedadventure:fire_geode} (change 41b, G2-WB2 = A): lines the fire crystal geodes whose cavities the
 * terrain dug (package 42, {@code ring_3/overworld/fire_geodes}; the geode of a cell from {@link VolcanicField#fireGeode},
 * its shape {@link FireGeodeShape}). From the cavity out: an inner wall of fire crystal, blackstone, a shell of smooth
 * basalt; on the inner wall buds and clusters that glow. A lava tube may have cut the geode open: its lava and air stay.
 * <p>
 * Only the open blocks are lined: a block of the shell is lined where the cavity just inside it (towards the centre) is
 * really open, not where another shape kept the cavity rock (a thin solid). Only terrain is replaced (never air, fluid
 * or bedrock); buds go only into air.
 * <p>
 * Determinism: placed once per chunk, each chunk lines its own part of the geode of its cell (a chunk lies in one
 * cell). Every decision reads the pure shape and whether the cavity is open, and the cavity is terrain that no other
 * feature fills (a bud in it still counts as open), so the neighbouring chunks may read it at any time; the lining
 * blocks are no target of any ore, so a later ore blob from a neighbour leaves them. The buds draw from the placement
 * random in a fixed order.
 */
public final class FireGeodeFeature extends Feature<FireGeodeConfiguration> {
    /** The order in which a cavity block looks for the wall a bud grows from. */
    private static final Direction[] WALLS = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    public FireGeodeFeature(Codec<FireGeodeConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<FireGeodeConfiguration> context) {
        WorldGenLevel level = context.level();
        VolcanicField field = Ring3Terrain.field(level);
        if (field == null) {
            return false;
        }
        ChunkPos chunk = new ChunkPos(context.origin());
        FireGeodeShape.Geode geode = field.fireGeode(chunk.getMinBlockX(), chunk.getMinBlockZ());
        if (geode == null) {
            return false;
        }
        int reach = geode.reach();
        int fromX = Math.max(chunk.getMinBlockX(), geode.x() - reach);
        int toX = Math.min(chunk.getMaxBlockX(), geode.x() + reach);
        int fromZ = Math.max(chunk.getMinBlockZ(), geode.z() - reach);
        int toZ = Math.min(chunk.getMaxBlockZ(), geode.z() + reach);
        if (fromX > toX || fromZ > toZ) {
            return false;
        }
        FireGeodeConfiguration config = context.config();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        // The lining.
        for (int x = fromX; x <= toX; x++) {
            for (int z = fromZ; z <= toZ; z++) {
                for (int y = geode.y() - reach; y <= geode.y() + reach; y++) {
                    double depth = geode.distance(x, y, z) - geode.radius();
                    if (depth <= 0.0 || depth > FireGeodeShape.LINING || !lined(level, geode, x, y, z)
                            || !terrain(level.getBlockState(pos.set(x, y, z)))) {
                        continue;
                    }
                    level.setBlock(pos, depth <= 1.0 ? config.inner() : depth <= 2.0 ? config.middle() : config.outer(), Block.UPDATE_CLIENTS);
                    placed = true;
                }
            }
        }
        // Buds and clusters on the inner wall, pointing into the cavity.
        RandomSource random = context.random();
        for (int x = fromX; x <= toX; x++) {
            for (int z = fromZ; z <= toZ; z++) {
                for (int y = geode.y() - reach; y <= geode.y() + reach; y++) {
                    if (!geode.inCavity(x, y, z)) {
                        continue;
                    }
                    // Two draws per cavity block, used or not, so the stream does not depend on what stands there.
                    float roll = random.nextFloat();
                    float kind = random.nextFloat();
                    if (roll >= config.budChance() || !level.getBlockState(pos.set(x, y, z)).isAir()) {
                        continue;
                    }
                    Direction wall = wall(level, geode, x, y, z);
                    if (wall != null) {
                        level.setBlock(pos, bud(kind).setValue(AmethystClusterBlock.FACING, wall.getOpposite()), Block.UPDATE_CLIENTS);
                        placed = true;
                    }
                }
            }
        }
        return placed;
    }

    /**
     * Whether the shell block (x, y, z) is lined: the cavity just inside it, on the line to the centre, is open. Reads
     * cavity blocks only (within the 3 × 3 chunks, their openness fixed by the terrain).
     */
    static boolean lined(WorldGenLevel level, FireGeodeShape.Geode geode, int x, int y, int z) {
        double distance = geode.distance(x, y, z);
        for (double inside : new double[]{0.5, 1.5}) {
            double scale = (geode.radius() - inside) / distance;
            int px = Mth.floor(geode.x() + (x - geode.x()) * scale + 0.5);
            int py = Mth.floor(geode.y() + (y - geode.y()) * scale + 0.5);
            int pz = Mth.floor(geode.z() + (z - geode.z()) * scale + 0.5);
            if (geode.inCavity(px, py, pz)) {
                return open(level.getBlockState(new BlockPos(px, py, pz)));
            }
        }
        return false;
    }

    /** The first side of a cavity block that is the lined inner wall (terrain), or null. */
    private static Direction wall(WorldGenLevel level, FireGeodeShape.Geode geode, int x, int y, int z) {
        for (Direction side : WALLS) {
            int nx = x + side.getStepX();
            int ny = y + side.getStepY();
            int nz = z + side.getStepZ();
            double depth = geode.distance(nx, ny, nz) - geode.radius();
            if (depth > 0.0 && depth <= 1.0 && lined(level, geode, nx, ny, nz) && terrain(level.getBlockState(new BlockPos(nx, ny, nz)))) {
                return side;
            }
        }
        return null;
    }

    /** An open cavity block: air, a fluid (a lava tube cut through), or a bud already grown into it. */
    private static boolean open(BlockState state) {
        return state.isAir() || !state.getFluidState().isEmpty() || state.getBlock() instanceof AmethystClusterBlock;
    }

    /** Rock the lining may replace: not air, fluid or bedrock. */
    private static boolean terrain(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && !state.is(Blocks.BEDROCK);
    }

    /** Mostly small and medium buds, large ones now and then, a full cluster rarely. */
    static BlockState bud(float roll) {
        Block block = roll < 0.4F ? ModBlocks.SMALL_FIRE_CRYSTAL_BUD.get() : roll < 0.7F ? ModBlocks.MEDIUM_FIRE_CRYSTAL_BUD.get()
                : roll < 0.9F ? ModBlocks.LARGE_FIRE_CRYSTAL_BUD.get() : ModBlocks.FIRE_CRYSTAL_CLUSTER.get();
        return block.defaultBlockState();
    }
}
