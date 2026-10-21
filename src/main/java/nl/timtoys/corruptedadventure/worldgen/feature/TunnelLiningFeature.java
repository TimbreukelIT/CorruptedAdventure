package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.Tags;
import nl.timtoys.corruptedadventure.block.SlimeTrailBlock;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModTags;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.Ring4Tunnels;

import javax.annotation.Nullable;

/**
 * {@code corruptedadventure:tunnel_lining} (package 77, Q14 = A; the tunnel net of 73): what lines the creatures'
 * roads. In the rock of the walls, floors and ceilings ({@link IslandField#tunnelWall}):
 * <ul>
 *   <li>the dry blighted net (dry tunnels, gas chambers, poison pools): toxic crystal patches with budding blocks (the
 *   poison caves; budding only underground, H5 = A), a bud or cluster in front where the tunnel is air;</li>
 *   <li>the flooded mythical net: aquamarine patches with budding blocks, a waterlogged bud in front in the water; jade
 *   deep down (below Y 0);</li>
 *   <li>both: tidesilver ore, claw marks in the side walls ({@code clawed_tidestone}, {@code clawed_stone},
 *   {@code clawed_deepslate} by the rock around them, 77b);</li>
 *   <li>on the dry net's floors: slime trails, now and then a nest or a few bones; bones on the flooded floors.</li>
 * </ul>
 * Only solid, dry blocks of the wall turn into other solid blocks, and what stands in front of a wall only takes air
 * of the dry net or water of the flooded net (buds waterlogged there): no water or poison ever meets air through this
 * feature, nothing goes into a gas chamber's gas, and nothing it places can light gas. An ore of another chunk's vein
 * that this feature finds becomes the lining in the ore's own host variant, and the ring 4 veins target plain
 * tidestone (not the claw marks), so the order of chunks does not matter; every
 * decision is a hash of the world seed and the block's position (no draws), and the feature writes only in its own
 * chunk. Budding blocks only where rock covers them.
 */
public final class TunnelLiningFeature extends Feature<NoneFeatureConfiguration> {
    // Starting values per wall or floor block, tuned at test stop 5b.
    static final float TOXIC_CRYSTAL = 0.02F;
    static final float TOXIC_CRYSTAL_IN_GAS = 0.05F;
    static final float AQUAMARINE = 0.015F;
    static final float BUDDING_SHARE = 0.3F;
    static final float BUD_IN_FRONT = 0.5F;
    static final float TIDESILVER = 0.012F;
    static final float JADE = 0.02F;
    static final int JADE_BELOW = 0;
    static final float CLAW_MARKS = 0.03F;
    static final float SLIME_TRAIL = 0.06F;
    static final float BONES = 0.006F;
    static final float NEST = 0.004F;
    private static final long SALT = 7701L;
    /** Budding blocks only this far under the floor of the open world above (water or sky). */
    static final int COVER = 3;

    /** What a wall block becomes. */
    enum Lining {
        NONE, TOXIC_CRYSTAL, AQUAMARINE, TIDESILVER, JADE, CLAW_MARKS
    }

    public TunnelLiningFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        IslandField field = Ring4Terrain.field(level);
        if (field == null) {
            return false;
        }
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        // Most chunks have no net: one look at its pieces (memoized per world) before any column's terrain.
        if (field.tunnelsWithin(minX - 1, minZ - 1, minX + 16, minZ + 16).isEmpty()
                && field.tunnelRoomsWithin(minX - 1, minZ - 1, minX + 16, minZ + 16).isEmpty()) {
            return false;
        }
        long seed = level.getSeed();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                // The column's own open blocks and one beyond, and the neighbours' open blocks beside it: the side walls
                // of an elliptical tunnel lie in columns past its open ones, or at heights the own column does not open.
                int lo = Integer.MAX_VALUE;
                int hi = Integer.MIN_VALUE;
                Ring4Tunnels.Span own = field.tunnelSpan(x, z);
                if (own != null && own.openLo() <= own.openHi()) {
                    lo = own.openLo() - 1;
                    hi = own.openHi() + 1;
                }
                for (int[] d : SIDES) {
                    Ring4Tunnels.Span side = field.tunnelSpan(x + d[0], z + d[1]);
                    if (side != null && side.openLo() <= side.openHi()) {
                        lo = Math.min(lo, side.openLo());
                        hi = Math.max(hi, side.openHi());
                    }
                }
                for (int y = lo; y <= hi; y++) {
                    placed |= line(level, field, seed, x, y, z, pos);
                }
            }
        }
        return placed;
    }

    private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /** One block of a column near the net: a wall block's lining, or a floor's track. */
    private static boolean line(WorldGenLevel level, IslandField field, long seed, int x, int y, int z, BlockPos.MutableBlockPos pos) {
        Ring4Tunnels.Fill own = field.tunnelFill(x, y, z);
        if (own == Ring4Tunnels.Fill.DRY_AIR || own == Ring4Tunnels.Fill.WATER) {
            return track(level, field, seed, own, x, y, z, pos);
        }
        if (own != Ring4Tunnels.Fill.NONE) {
            return false;
        }
        Ring4Tunnels.Fill wall = field.tunnelWall(x, y, z);
        if (wall == Ring4Tunnels.Fill.NONE) {
            return false;
        }
        BlockState state = level.getBlockState(pos.set(x, y, z));
        if (!rock(state)) {
            return false;
        }
        boolean side = field.tunnelFill(x, y - 1, z) == Ring4Tunnels.Fill.NONE && field.tunnelFill(x, y + 1, z) == Ring4Tunnels.Fill.NONE;
        Lining lining = lining(wall, side, y, unit(seed, x, y, z, 0));
        if (lining == Lining.NONE) {
            return false;
        }
        // Budding only where rock covers it (H5 = A): not in a wall of an entrance's mouth under open water or sky.
        boolean covered = y < level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - COVER;
        level.setBlock(pos, liningState(lining, state, covered ? unit(seed, x, y, z, 1) : 1.0F), Block.UPDATE_CLIENTS);
        if ((lining == Lining.TOXIC_CRYSTAL || lining == Lining.AQUAMARINE) && unit(seed, x, y, z, 2) < BUD_IN_FRONT) {
            bud(level, field, lining, x, y, z, unit(seed, x, y, z, 3), pos);
        }
        return true;
    }

    /**
     * What a wall block of the net beside {@code wall} becomes, by its roll in [0, 1): crystal patches by band, ores,
     * claw marks only in a side wall (not a floor or ceiling), jade only deep in the flooded net. Pure.
     */
    static Lining lining(Ring4Tunnels.Fill wall, boolean side, int y, float roll) {
        boolean flooded = wall == Ring4Tunnels.Fill.WATER || wall == Ring4Tunnels.Fill.DOME_AIR;
        float crystal = flooded ? AQUAMARINE : wall == Ring4Tunnels.Fill.GAS ? TOXIC_CRYSTAL_IN_GAS : TOXIC_CRYSTAL;
        float at = crystal;
        if (roll < at) {
            return flooded ? Lining.AQUAMARINE : Lining.TOXIC_CRYSTAL;
        }
        at += TIDESILVER;
        if (roll < at) {
            return Lining.TIDESILVER;
        }
        if (flooded && y < JADE_BELOW) {
            at += JADE;
            if (roll < at) {
                return Lining.JADE;
            }
        }
        at += CLAW_MARKS;
        return side && roll < at ? Lining.CLAW_MARKS : Lining.NONE;
    }

    /**
     * The block for a lining by its host: an ore's variant follows the host rock (an ore of another feature by its
     * convention tag), so the lining is the same whether a neighbour's vein came before or after it.
     */
    static BlockState liningState(Lining lining, BlockState host, float roll) {
        boolean deepslate = host.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES) || host.is(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE);
        boolean tidestone = host.is(ModTags.Blocks.RING_4_STONES) || host.is(ModTags.Blocks.ORES_IN_GROUND_TIDESTONE);
        return switch (lining) {
            case TOXIC_CRYSTAL -> (roll < BUDDING_SHARE ? ModBlocks.BUDDING_TOXIC_CRYSTAL : ModBlocks.TOXIC_CRYSTAL_BLOCK).get().defaultBlockState();
            case AQUAMARINE -> (roll < BUDDING_SHARE ? ModBlocks.BUDDING_AQUAMARINE : ModBlocks.AQUAMARINE_BLOCK).get().defaultBlockState();
            case TIDESILVER -> (deepslate ? ModBlocks.DEEPSLATE_TIDESILVER_ORE : tidestone ? ModBlocks.TIDESTONE_TIDESILVER_ORE
                    : ModBlocks.TIDESILVER_ORE).get().defaultBlockState();
            case JADE -> (deepslate ? ModBlocks.DEEPSLATE_JADE_ORE : tidestone ? ModBlocks.TIDESTONE_JADE_ORE : ModBlocks.JADE_ORE).get().defaultBlockState();
            // 77b (golf 3): the claw marks in the rock around them.
            case CLAW_MARKS -> (deepslate ? ModBlocks.CLAWED_DEEPSLATE : tidestone ? ModBlocks.CLAWED_TIDESTONE : ModBlocks.CLAWED_STONE)
                    .get().defaultBlockState();
            case NONE -> host;
        };
    }

    /**
     * A bud or cluster on the crystal, in the first neighbour that is the net's open space in this chunk: air of the dry
     * net, or water of the flooded net (waterlogged there). Never into gas, a pool or a dome's air over water.
     */
    private static void bud(WorldGenLevel level, IslandField field, Lining lining, int x, int y, int z, float roll,
                            BlockPos.MutableBlockPos pos) {
        for (Direction direction : Direction.values()) {
            int nx = x + direction.getStepX();
            int ny = y + direction.getStepY();
            int nz = z + direction.getStepZ();
            if (nx >> 4 != x >> 4 || nz >> 4 != z >> 4) {
                continue;
            }
            Ring4Tunnels.Fill fill = field.tunnelFill(nx, ny, nz);
            BlockState there = level.getBlockState(pos.set(nx, ny, nz));
            boolean dry = lining == Lining.TOXIC_CRYSTAL && fill == Ring4Tunnels.Fill.DRY_AIR && there.isAir();
            boolean wet = lining == Lining.AQUAMARINE && fill == Ring4Tunnels.Fill.WATER && there.is(Blocks.WATER)
                    && there.getFluidState().isSource();
            if (!dry && !wet) {
                continue;
            }
            BlockState bud = budState(lining, roll).setValue(AmethystClusterBlock.FACING, direction)
                    .setValue(BlockStateProperties.WATERLOGGED, wet);
            level.setBlock(pos, bud, Block.UPDATE_CLIENTS);
            return;
        }
    }

    private static BlockState budState(Lining lining, float roll) {
        boolean toxic = lining == Lining.TOXIC_CRYSTAL;
        if (roll < 0.35F) {
            return (toxic ? ModBlocks.SMALL_TOXIC_CRYSTAL_BUD : ModBlocks.SMALL_AQUAMARINE_BUD).get().defaultBlockState();
        }
        if (roll < 0.65F) {
            return (toxic ? ModBlocks.MEDIUM_TOXIC_CRYSTAL_BUD : ModBlocks.MEDIUM_AQUAMARINE_BUD).get().defaultBlockState();
        }
        if (roll < 0.88F) {
            return (toxic ? ModBlocks.LARGE_TOXIC_CRYSTAL_BUD : ModBlocks.LARGE_AQUAMARINE_BUD).get().defaultBlockState();
        }
        return (toxic ? ModBlocks.TOXIC_CRYSTAL_CLUSTER : ModBlocks.AQUAMARINE_CLUSTER).get().defaultBlockState();
    }

    /**
     * A floor's track: an open block of the net right above a rock floor. The dry net: slime trails, a nest, bones; the
     * flooded net: bones (in the water: a bone block holds no water, so nothing opens).
     */
    private static boolean track(WorldGenLevel level, IslandField field, long seed, Ring4Tunnels.Fill fill, int x, int y, int z,
                                 BlockPos.MutableBlockPos pos) {
        if (field.tunnelFill(x, y - 1, z) != Ring4Tunnels.Fill.NONE || !rock(level.getBlockState(pos.set(x, y - 1, z)))) {
            return false;
        }
        BlockState here = level.getBlockState(pos.set(x, y, z));
        boolean dry = fill == Ring4Tunnels.Fill.DRY_AIR && here.isAir();
        boolean wet = fill == Ring4Tunnels.Fill.WATER && here.is(Blocks.WATER);
        @Nullable BlockState track = track(dry, wet, unit(seed, x, y, z, 4), unit(seed, x, y, z, 5));
        if (track == null || !track.canSurvive(level, pos)) {
            return false;
        }
        level.setBlock(pos, track, Block.UPDATE_CLIENTS);
        return true;
    }

    /** The track for a floor block by its rolls, or null. Pure. */
    @Nullable
    static BlockState track(boolean dry, boolean wet, float roll, float turn) {
        Direction.Axis axis = turn < 0.5F ? Direction.Axis.X : Direction.Axis.Z;
        if (dry) {
            if (roll < SLIME_TRAIL) {
                return ModBlocks.SLIME_TRAIL.get().defaultBlockState().setValue(SlimeTrailBlock.AXIS, axis);
            }
            if (roll < SLIME_TRAIL + BONES) {
                return Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
            }
            return roll < SLIME_TRAIL + BONES + NEST ? ModBlocks.CREATURE_NEST.get().defaultBlockState() : null;
        }
        return wet && roll < BONES ? Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis) : null;
    }

    /**
     * A wall block that may turn: solid and dry rock or ore (also another feature's ore), never a budding block. The claw
     * marks in stone and deepslate count as rock too (77b: they are in neither stone tag on purpose), so a floor with
     * claw marks carries a track as the rock did.
     */
    static boolean rock(BlockState state) {
        return (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(ModTags.Blocks.RING_4_STONES) || state.is(Tags.Blocks.ORES)
                || state.is(ModBlocks.CLAWED_STONE.get()) || state.is(ModBlocks.CLAWED_DEEPSLATE.get()))
                && state.getFluidState().isEmpty();
    }

    /** A hash of the world seed and the position for decision {@code k}, in [0, 1). */
    static float unit(long seed, int x, int y, int z, int k) {
        long h = Mth.getSeed(x, y, z) ^ seed ^ (SALT + k) * 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return ((h >>> 40) & 0xFFFFFF) / (float) (1 << 24);
    }
}
