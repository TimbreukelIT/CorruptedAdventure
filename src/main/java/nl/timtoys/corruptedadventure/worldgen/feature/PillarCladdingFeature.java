package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.SeaFossils;

/**
 * {@code corruptedadventure:pillar_cladding} (78, N2 = A): clads the crystal columns of Nether ring 4's
 * {@code crystal_spires}, whose purity crystal skin the surface paints: clusters on the open sides and under the flared
 * heads (never on top of anything: the block below a cluster is open; and never in the 2 blocks above a walkway or floor, so the ledges and bridges stay
 * free), and now and then a lamp set into the skin (the pulsing crystal light).
 * <p>
 * Determinism: once per chunk, writes only its own chunk, reads one block around it. A lamp is skin and a cluster is
 * open for every test, so what a neighbouring chunk already placed changes nothing. The region axis (the terrain's own
 * wired function, through {@link NetherRing4Terrain}) and two probe layers skip the chunks without columns before the
 * full read. The random draws go in a fixed order: one
 * per block of the columns' zone, used or not.
 */
public final class PillarCladdingFeature extends Feature<PillarCladdingConfiguration> {
    /** The columns rise from the lava sea's bed (the surface paints them from Y 4) to the ceiling (at most 120). */
    static final int BOTTOM = 4;
    static final int TOP = 124;
    /** Where a cluster looks for the skin: under a head first, then the sides (never below: nothing grows on a top). */
    private static final Direction[] SKIN = {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    /** The heights where every column shows skin: its shaft (lava sea to ceiling) and its flared head (Y 94–112). */
    private static final int[] PROBES = {60, 100};
    /** How far a chunk's region samples lie apart, and the margin of the skip test for the noise between them. */
    private static final int SAMPLE = 4;
    private static final double SAMPLE_MARGIN = 0.04;

    public PillarCladdingFeature(Codec<PillarCladdingConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<PillarCladdingConfiguration> context) {
        WorldGenLevel level = context.level();
        PillarCladdingConfiguration config = context.config();
        ChunkPos chunk = new ChunkPos(context.origin());
        // the region as the terrain and the surface see it (the wired ring_4/nether/region)
        SeaFossils terrain = NetherRing4Terrain.fossils(level);
        if (terrain == null) {
            return false;
        }
        int x0 = chunk.getMinBlockX();
        int z0 = chunk.getMinBlockZ();
        boolean any = false;
        for (int dx = 0; dx <= 16 && !any; dx += SAMPLE) {
            for (int dz = 0; dz <= 16 && !any; dz += SAMPLE) {
                any = terrain.regionAt(x0 + dx, z0 + dz) >= config.regionMin() - SAMPLE_MARGIN;
            }
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        if (!any || !skinAtProbeHeights(level, config, x0, z0, pos)) {
            return false;
        }
        // 0 other, 1 skin (crystal or lamp), 2 open (air or a cluster); own chunk plus one block round it
        int ny = TOP - BOTTOM + 3;
        byte[] kind = new byte[18 * 18 * ny];
        for (int x = -1; x <= 16; x++) {
            for (int z = -1; z <= 16; z++) {
                for (int y = BOTTOM - 1; y <= TOP + 1; y++) {
                    BlockState state = level.getBlockState(pos.set(x0 + x, y, z0 + z));
                    kind[index(x, y, z, ny)] = skin(state, config) ? (byte) 1 : open(state) ? (byte) 2 : (byte) 0;
                }
            }
        }
        RandomSource random = context.random();
        boolean placed = false;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (terrain.regionAt(x0 + x, z0 + z) < config.regionMin()) {
                    continue;
                }
                for (int y = BOTTOM; y <= TOP; y++) {
                    float roll = random.nextFloat();
                    byte k = kind[index(x, y, z, ny)];
                    if (k == 1 && roll < config.lampChance() && exposed(kind, x, y, z, ny)) {
                        level.setBlock(pos.set(x0 + x, y, z0 + z), config.lamp(), Block.UPDATE_CLIENTS);
                        placed = true;
                    } else if (k == 2 && roll < config.clusterChance() && kind[index(x, y - 1, z, ny)] == 2
                            && !aboveWalkway(kind, x, y, z, ny)) {
                        // open below: a cluster hangs on a side or under a head, never stands on top of anything
                        Direction side = skinSide(kind, x, y, z, ny);
                        if (side != null && level.getBlockState(pos.set(x0 + x, y, z0 + z)).isAir()) {
                            BlockState cluster = config.cluster();
                            if (cluster.hasProperty(AmethystClusterBlock.FACING)) {
                                cluster = cluster.setValue(AmethystClusterBlock.FACING, side.getOpposite());
                            }
                            level.setBlock(pos, cluster, Block.UPDATE_CLIENTS);
                            placed = true;
                        }
                    }
                }
            }
        }
        return placed;
    }

    /**
     * Whether the chunk (and one block round it) shows skin at the probe heights: every column rises from the lava sea to
     * the ceiling through {@link #PROBES} (shaft and flared head), so a chunk without skin there has no column to clad.
     */
    private static boolean skinAtProbeHeights(WorldGenLevel level, PillarCladdingConfiguration config, int x0, int z0, BlockPos.MutableBlockPos pos) {
        for (int y : PROBES) {
            for (int x = -1; x <= 16; x++) {
                for (int z = -1; z <= 16; z++) {
                    if (skin(level.getBlockState(pos.set(x0 + x, y, z0 + z)), config)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static int index(int x, int y, int z, int ny) {
        return ((x + 1) * 18 + (z + 1)) * ny + (y - BOTTOM + 1);
    }

    private static boolean exposed(byte[] kind, int x, int y, int z, int ny) {
        for (Direction side : Direction.values()) {
            if (kind[index(x + side.getStepX(), y + side.getStepY(), z + side.getStepZ(), ny)] == 2) {
                return true;
            }
        }
        return false;
    }

    /** The first side of an open block that is skin (a cluster grows from it), or null. */
    private static Direction skinSide(byte[] kind, int x, int y, int z, int ny) {
        for (Direction side : SKIN) {
            if (kind[index(x + side.getStepX(), y + side.getStepY(), z + side.getStepZ(), ny)] == 1) {
                return side;
            }
        }
        return null;
    }

    /** Within 2 above a solid block that is not skin: a ledge, a bridge or a floor, where one walks. */
    private static boolean aboveWalkway(byte[] kind, int x, int y, int z, int ny) {
        return kind[index(x, y - 1, z, ny)] == 0 || y - 2 >= BOTTOM - 1 && kind[index(x, y - 2, z, ny)] == 0;
    }

    private static boolean skin(BlockState state, PillarCladdingConfiguration config) {
        return state.is(config.crystal().getBlock()) || state.is(config.lamp().getBlock());
    }

    private static boolean open(BlockState state) {
        return state.isAir() || state.getBlock() instanceof AmethystClusterBlock;
    }
}
