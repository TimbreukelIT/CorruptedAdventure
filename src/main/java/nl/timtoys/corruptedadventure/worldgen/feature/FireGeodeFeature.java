package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
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
import net.neoforged.neoforge.common.Tags;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModTags;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicField;

/**
 * {@code corruptedadventure:fire_geode} (change 41b, G2-WB2 = A): lines the fire crystal geodes whose cavities the
 * terrain dug (package 42, {@code ring_3/overworld/fire_geodes}; the geode of a cell from {@link VolcanicField#fireGeode},
 * its shape {@link FireGeodeShape}). From the cavity out: an inner wall of fire crystal, blackstone, a shell of smooth
 * basalt; on the inner wall buds and clusters that glow. A lava tube may have cut the geode open: its lava and air stay.
 * <p>
 * The layers follow the blocks, not the stretched distance (a squashed axis would skip a band): a cavity block is open
 * where the pure shape has its cavity and the block really is open (air, fluid, or a bud already grown there); layer 1
 * is the rock with a face on such a block, layer 2 the rock with a face on layer 1, layer 3 the same on layer 2. So
 * only the open cavity is lined, never where another shape kept it rock. Only rock is replaced ({@link #rock}: the
 * stones of the overworld and ring 3, ores, gravel and dirt, the lining itself; never a block entity); buds go only
 * into air, on open blocks with a face on layer 1.
 * <p>
 * Determinism: placed once per chunk, each chunk lines its own part of the geode of its cell (a chunk lies in one
 * cell), reading the blocks up to 3 around it. It assumes that <b>no feature fills the cavity</b> (a bud in it still
 * counts as open) and that every later change to the rock around it keeps it {@link #rock} (ores, the lining): then
 * every chunk sees the same open blocks and the same rock whatever the order of generation. The buds draw from the
 * placement random in a fixed order.
 */
public final class FireGeodeFeature extends Feature<FireGeodeConfiguration> {
    /** The order in which a cavity block looks for the wall a bud grows from. */
    private static final Direction[] WALLS = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    /** How far the layers reach from the open cavity, in face steps. */
    static final int LAYERS = 3;
    static final byte OTHER = 0;
    static final byte ROCK = 1;
    static final byte OPEN = 2;

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
        Box box = Box.of(geode, chunk);
        if (box == null) {
            return false;
        }
        // What every block of the box is (the own chunk and 3 around it, inside the geode's outer ellipsoid).
        byte[] kind = new byte[box.size()];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = box.x0; x <= box.x1; x++) {
            for (int z = box.z0; z <= box.z1; z++) {
                int[] span = box.span(geode, x, z);
                for (int y = span[0]; y <= span[1]; y++) {
                    BlockState state = level.getBlockState(pos.set(x, y, z));
                    kind[box.index(x, y, z)] = geode.inCavity(x, y, z) && open(state) ? OPEN : rock(state) ? ROCK : OTHER;
                }
            }
        }
        byte[] layer = layers(kind, box.nx, box.ny, box.nz);

        FireGeodeConfiguration config = context.config();
        BlockState[] states = {null, config.inner(), config.middle(), config.outer()};
        boolean placed = false;
        RandomSource random = context.random();
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                if (!box.contains(x, z)) {
                    continue;
                }
                int[] span = box.span(geode, x, z);
                for (int y = span[0]; y <= span[1]; y++) {
                    int i = box.index(x, y, z);
                    if (layer[i] > 0) {
                        level.setBlock(pos.set(x, y, z), states[layer[i]], Block.UPDATE_CLIENTS);
                        placed = true;
                    } else if (geode.inCavity(x, y, z)) {
                        // Two draws per cavity block, used or not, so the stream does not depend on what stands there.
                        float roll = random.nextFloat();
                        float budKind = random.nextFloat();
                        Direction wall = kind[i] == OPEN && roll < config.budChance() ? wall(layer, box, x, y, z) : null;
                        if (wall != null && level.getBlockState(pos.set(x, y, z)).isAir()) {
                            level.setBlock(pos, bud(budKind).setValue(AmethystClusterBlock.FACING, wall.getOpposite()), Block.UPDATE_CLIENTS);
                            placed = true;
                        }
                    }
                }
            }
        }
        return placed;
    }

    /**
     * The layer (1–{@link #LAYERS}, 0 = none) of every block of a box of {@code kind}s ({@link #OPEN}, {@link #ROCK},
     * {@link #OTHER}; index {@code (x · ny + y) · nz + z}): rock with a face on an open block is layer 1, rock with a face
     * on layer k is layer k + 1. Pure.
     */
    static byte[] layers(byte[] kind, int nx, int ny, int nz) {
        byte[] layer = new byte[kind.length];
        for (byte k = 1; k <= LAYERS; k++) {
            for (int x = 0; x < nx; x++) {
                for (int y = 0; y < ny; y++) {
                    for (int z = 0; z < nz; z++) {
                        int i = (x * ny + y) * nz + z;
                        if (kind[i] != ROCK || layer[i] != 0) {
                            continue;
                        }
                        for (Direction side : WALLS) {
                            int ax = x + side.getStepX();
                            int ay = y + side.getStepY();
                            int az = z + side.getStepZ();
                            if (ax < 0 || ay < 0 || az < 0 || ax >= nx || ay >= ny || az >= nz) {
                                continue;
                            }
                            int j = (ax * ny + ay) * nz + az;
                            if (k == 1 ? kind[j] == OPEN : layer[j] == k - 1) {
                                layer[i] = k;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return layer;
    }

    /** The first side of a cavity block on layer 1, or null. */
    private static Direction wall(byte[] layer, Box box, int x, int y, int z) {
        for (Direction side : WALLS) {
            int j = box.indexOrNone(x + side.getStepX(), y + side.getStepY(), z + side.getStepZ());
            if (j >= 0 && layer[j] == 1) {
                return side;
            }
        }
        return null;
    }

    /** An open cavity block: air, a fluid (a lava tube cut through), or a bud already grown into it. */
    private static boolean open(BlockState state) {
        return state.isAir() || !state.getFluidState().isEmpty() || state.getBlock() instanceof AmethystClusterBlock;
    }

    /** Rock the lining may replace or pass through: stone, ores and what ore blobs make, and the lining. */
    static boolean rock(BlockState state) {
        if (state.hasBlockEntity() || !state.getFluidState().isEmpty()) {
            return false;
        }
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(ModTags.Blocks.RING_3_STONES) || state.is(Tags.Blocks.ORES)
                || state.is(Blocks.SMOOTH_BASALT) || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.GRAVEL) || state.is(Blocks.DIRT)
                || state.is(ModBlocks.FIRE_CRYSTAL_BLOCK.get());
    }

    /** Mostly small and medium buds, large ones now and then, a full cluster rarely. */
    static BlockState bud(float roll) {
        Block block = roll < 0.4F ? ModBlocks.SMALL_FIRE_CRYSTAL_BUD.get() : roll < 0.7F ? ModBlocks.MEDIUM_FIRE_CRYSTAL_BUD.get()
                : roll < 0.9F ? ModBlocks.LARGE_FIRE_CRYSTAL_BUD.get() : ModBlocks.FIRE_CRYSTAL_CLUSTER.get();
        return block.defaultBlockState();
    }

    /**
     * The blocks a chunk reads: its own and {@link #LAYERS} around it, within the geode's outer ellipsoid (the cavity
     * grown by the layers, at most {@link #LAYERS} face steps on the most squashed axis).
     */
    static final class Box {
        final int x0;
        final int x1;
        final int y0;
        final int y1;
        final int z0;
        final int z1;
        final int nx;
        final int ny;
        final int nz;
        private final double outer;
        private final int[] span = new int[2];

        private Box(int x0, int x1, int y0, int y1, int z0, int z1, double outer) {
            this.x0 = x0;
            this.x1 = x1;
            this.y0 = y0;
            this.y1 = y1;
            this.z0 = z0;
            this.z1 = z1;
            this.nx = x1 - x0 + 1;
            this.ny = y1 - y0 + 1;
            this.nz = z1 - z0 + 1;
            this.outer = outer;
        }

        /** The box of {@code chunk} for {@code geode}, or null where the geode does not reach the chunk. */
        static Box of(FireGeodeShape.Geode geode, ChunkPos chunk) {
            double squash = Math.min(geode.stretchX(), Math.min(geode.stretchY(), geode.stretchZ()));
            double outer = geode.radius() + (LAYERS + 1) / squash;
            int rx = Mth.ceil(outer * geode.stretchX());
            int ry = Mth.ceil(outer * geode.stretchY());
            int rz = Mth.ceil(outer * geode.stretchZ());
            int x0 = Math.max(chunk.getMinBlockX() - LAYERS, geode.x() - rx);
            int x1 = Math.min(chunk.getMaxBlockX() + LAYERS, geode.x() + rx);
            int z0 = Math.max(chunk.getMinBlockZ() - LAYERS, geode.z() - rz);
            int z1 = Math.min(chunk.getMaxBlockZ() + LAYERS, geode.z() + rz);
            if (x0 > x1 || z0 > z1 || geode.x() + rx < chunk.getMinBlockX() || geode.x() - rx > chunk.getMaxBlockX()
                    || geode.z() + rz < chunk.getMinBlockZ() || geode.z() - rz > chunk.getMaxBlockZ()) {
                return null;
            }
            return new Box(x0, x1, geode.y() - ry, geode.y() + ry, z0, z1, outer);
        }

        int size() {
            return nx * ny * nz;
        }

        boolean contains(int x, int z) {
            return x >= x0 && x <= x1 && z >= z0 && z <= z1;
        }

        int index(int x, int y, int z) {
            return ((x - x0) * ny + (y - y0)) * nz + (z - z0);
        }

        int indexOrNone(int x, int y, int z) {
            return x < x0 || x > x1 || y < y0 || y > y1 || z < z0 || z > z1 ? -1 : index(x, y, z);
        }

        /** The Y range of column (x, z) inside the outer ellipsoid (empty: first > second); reuses one array. */
        int[] span(FireGeodeShape.Geode geode, int x, int z) {
            double dx = (x - geode.x()) / geode.stretchX();
            double dz = (z - geode.z()) / geode.stretchZ();
            double rest = outer * outer - dx * dx - dz * dz;
            if (rest < 0.0) {
                span[0] = 1;
                span[1] = 0;
                return span;
            }
            double half = Math.sqrt(rest) * geode.stretchY();
            span[0] = Math.max(y0, Mth.floor(geode.y() - half));
            span[1] = Math.min(y1, Mth.ceil(geode.y() + half));
            return span;
        }
    }
}
