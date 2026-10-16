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
import nl.timtoys.corruptedadventure.worldgen.densityfunction.GiantGeodes;

/**
 * {@code corruptedadventure:giant_geode_lining} (78, N3 = A): lines the giant geodes of Nether ring 4's
 * {@code geode_hollows}, whose hollows the terrain dug ({@link GiantGeodes}, geometry {@link GiantGeodeShape}; the geode
 * of a cell from {@link GiantGeodes#geodeAt}, backlog r. 284). From the hollow out: purity crystal, calcite, bleached
 * netherrack, with a lamp here and there in the crystal of the dome and clusters on the inner wall. The floor stays
 * bone sand and walkable: no layer below the geode's floor, so no cluster stands on it.
 * <p>
 * Same method as {@link FireGeodeFeature} (and its {@link FireGeodeFeature#layers}): a hollow block is open where the
 * pure shape has its hollow (the dome, and the doorways only where they meet the wall) and the block really is open;
 * layer 1 is the rock with a face on such a block, layers 2 and 3 grow from it. Only rock is replaced ({@link #rock}).
 * <p>
 * Determinism: once per chunk, each chunk lines its own part of the geode of its cell (a chunk lies in one cell), reading
 * the blocks up to 3 around it. No other feature reaches into the hollows; the lining's own blocks count as rock and its
 * clusters as open, so every chunk sees the same open blocks and rock whatever the order of generation. The random
 * draws go in a fixed order, used or not.
 */
public final class GiantGeodeLiningFeature extends Feature<GiantGeodeLiningConfiguration> {
    /** Where a cluster looks for the inner wall: hanging from the dome first, then the sides, then a step of the wall. */
    private static final Direction[] WALLS = {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.DOWN};
    private static final int LAYERS = FireGeodeFeature.LAYERS;
    /** Doorway blocks this far past the dome's surface still count as the hollow (the mouth of the doorway is lined). */
    private static final double MOUTH = 1.5;

    public GiantGeodeLiningFeature(Codec<GiantGeodeLiningConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<GiantGeodeLiningConfiguration> context) {
        WorldGenLevel level = context.level();
        GiantGeodes geodes = NetherRing4Terrain.geodes(level);
        if (geodes == null) {
            return false;
        }
        ChunkPos chunk = new ChunkPos(context.origin());
        GiantGeodeShape.Geode geode = geodes.geodeAt(chunk.getMinBlockX(), chunk.getMinBlockZ());
        if (geode == null) {
            return false;
        }
        int reach = Mth.ceil(geode.radius() * Math.max(geode.stretchX(), geode.stretchZ()) + MOUTH) + LAYERS + 1;
        int x0 = Math.max(chunk.getMinBlockX() - LAYERS, geode.x() - reach);
        int x1 = Math.min(chunk.getMaxBlockX() + LAYERS, geode.x() + reach);
        int z0 = Math.max(chunk.getMinBlockZ() - LAYERS, geode.z() - reach);
        int z1 = Math.min(chunk.getMaxBlockZ() + LAYERS, geode.z() + reach);
        if (x0 > x1 || z0 > z1) {
            return false;
        }
        int y0 = geode.floor() - 1;
        int y1 = geode.y() + Mth.ceil(geode.radius() * geode.stretchY() + MOUTH) + LAYERS + 1;
        int nx = x1 - x0 + 1;
        int ny = y1 - y0 + 1;
        int nz = z1 - z0 + 1;
        byte[] kind = new byte[nx * ny * nz];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    BlockState state = level.getBlockState(pos.set(x, y, z));
                    kind[((x - x0) * ny + (y - y0)) * nz + (z - z0)] = classify(geode, x, y, z, open(state), rock(state));
                }
            }
        }
        byte[] layer = FireGeodeFeature.layers(kind, nx, ny, nz);

        GiantGeodeLiningConfiguration config = context.config();
        BlockState[] states = {null, config.inner(), config.middle(), config.outer()};
        RandomSource random = context.random();
        boolean placed = false;
        for (int x = Math.max(x0, chunk.getMinBlockX()); x <= Math.min(x1, chunk.getMaxBlockX()); x++) {
            for (int z = Math.max(z0, chunk.getMinBlockZ()); z <= Math.min(z1, chunk.getMaxBlockZ()); z++) {
                for (int y = y0; y <= y1; y++) {
                    int i = ((x - x0) * ny + (y - y0)) * nz + (z - z0);
                    // One draw per block, used or not, so the stream does not depend on what stands there.
                    float roll = random.nextFloat();
                    if (layer[i] > 0) {
                        BlockState state = layer[i] == 1 && y > geode.y() && roll < config.lampChance() ? config.lamp() : states[layer[i]];
                        level.setBlock(pos.set(x, y, z), state, Block.UPDATE_CLIENTS);
                        placed = true;
                    } else if (kind[i] == FireGeodeFeature.OPEN && roll < config.clusterChance()) {
                        Direction wall = wall(layer, x - x0, y - y0, z - z0, nx, ny, nz);
                        if (wall != null && level.getBlockState(pos.set(x, y, z)).isAir()) {
                            level.setBlock(pos, cluster(config.cluster(), wall), Block.UPDATE_CLIENTS);
                            placed = true;
                        }
                    }
                }
            }
        }
        return placed;
    }

    /** What a block of the box is for the lining: open hollow, rock that may be lined, or neither. */
    static byte classify(GiantGeodeShape.Geode geode, int x, int y, int z, boolean open, boolean rock) {
        if (y < geode.floor()) {
            return FireGeodeFeature.OTHER; // the floor stays bone sand
        }
        if (geode.hollow(x, y, z) && geode.distance(x, y, z) <= geode.radius() + MOUTH) {
            return open ? FireGeodeFeature.OPEN : FireGeodeFeature.OTHER;
        }
        return rock ? FireGeodeFeature.ROCK : FireGeodeFeature.OTHER;
    }

    /** The first side of an open block that is inner wall (layer 1), or null. */
    static Direction wall(byte[] layer, int x, int y, int z, int nx, int ny, int nz) {
        for (Direction side : WALLS) {
            int ax = x + side.getStepX();
            int ay = y + side.getStepY();
            int az = z + side.getStepZ();
            if (ax >= 0 && ay >= 0 && az >= 0 && ax < nx && ay < ny && az < nz && layer[(ax * ny + ay) * nz + az] == 1) {
                return side;
            }
        }
        return null;
    }

    private static BlockState cluster(BlockState cluster, Direction wall) {
        return cluster.hasProperty(AmethystClusterBlock.FACING) ? cluster.setValue(AmethystClusterBlock.FACING, wall.getOpposite()) : cluster;
    }

    /** An open hollow block: air, or a cluster already grown into it. */
    private static boolean open(BlockState state) {
        return state.isAir() || state.getBlock() instanceof AmethystClusterBlock;
    }

    /** Rock the lining may replace: the rock and the bone sand of the steps in the wall, and the lining itself. */
    static boolean rock(BlockState state) {
        if (state.hasBlockEntity() || !state.getFluidState().isEmpty()) {
            return false;
        }
        return state.is(ModBlocks.BLEACHED_NETHERRACK.get()) || state.is(ModBlocks.BONE_SAND.get()) || state.is(Blocks.NETHERRACK)
                || state.is(ModBlocks.PURITY_CRYSTAL_BLOCK.get()) || state.is(ModBlocks.PURITY_CRYSTAL_LAMP.get()) || state.is(Blocks.CALCITE);
    }
}
