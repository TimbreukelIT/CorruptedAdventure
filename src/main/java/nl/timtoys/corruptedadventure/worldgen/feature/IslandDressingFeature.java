package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;

import java.util.List;

/**
 * {@code corruptedadventure:island_dressing} (decor rollout C, 2026-10-19; backlog "kale donkere onderkant"): the living
 * underside of the floating islands of the mythical band, seen from below. Per column of an island's underside:
 * <ul>
 *   <li>the underside block turns to moss in patches (the ring 4 patch noise), now and then to a crystal with a bud
 *   hanging under it;</li>
 *   <li>single blocks hang under it (glow lichen, spore blossoms, roots, aquamarine buds), or a strand of moss;</li>
 * </ul>
 * and on the islands' outer walls curtains of vines that hang on a little below the edge. The blighted band keeps the
 * plain undersides of {@link IslandUndersideFeature} (its height biome {@code withered_gardens} stays as it is).
 * <p>
 * Order-independent as {@link IslandUndersideFeature}: every column draws the same values before the world is asked;
 * the overhangs are found in the own column with every non-solid block counted as open ({@link IslandUndersideFeature#open}:
 * a neighbouring chunk's leaves, the strands and plants of this chunk's features); the underside block is terrain, which
 * only this chunk's features change; the rest goes only into air, each block only where it survives on what stands
 * now, so a tree's leaf that takes a spot (a tree wins over every block of this feature) cuts a hanging strand or a
 * curtain below the wall exactly as the shape update cuts it when the leaf comes later. Writes only in its own chunk;
 * the vines read the wall beside them, which may lie in a neighbouring chunk (terrain, sturdy whatever grows on it).
 */
public final class IslandDressingFeature extends Feature<IslandDressingConfiguration> {
    /** The smallest open gap under an underside that gets anything hanging (low overhangs near the ground stay bare). */
    static final int MIN_GAP = 3;
    /** Blocks between the moss field and the other users of the patch noise. */
    private static final double MOSS_SALT = 37013.0;
    private static final double MOSS_SCALE = 1.6;

    public IslandDressingFeature(Codec<IslandDressingConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<IslandDressingConfiguration> context) {
        WorldGenLevel level = context.level();
        IslandField field = Ring4Terrain.field(level);
        if (field == null) {
            return false;
        }
        IslandDressingConfiguration config = context.config();
        RandomSource random = context.random();
        NormalNoise patches = level.getLevel().getChunkSource().randomState().getOrCreateNoise(Ring4Placements.PATCHES_NOISE);
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        float[] vineRolls = new float[4];
        int[] vineStarts = new int[4];
        int[] vineBelows = new int[4];
        for (int dz = 0; dz < 16; dz++) {
            for (int dx = 0; dx < 16; dx++) {
                // Always the same draws, before the world is asked, so a column's outcome never depends on an earlier one.
                float roll = random.nextFloat();
                int length = config.minLength() + random.nextInt(config.maxLength() - config.minLength() + 1);
                BlockState hanging = config.hanging().getState(random, BlockPos.ZERO);
                BlockState bud = config.crystalBud().getState(random, BlockPos.ZERO);
                for (int side = 0; side < 4; side++) {
                    vineRolls[side] = random.nextFloat();
                    vineStarts[side] = 1 + random.nextInt(3);
                    vineBelows[side] = random.nextInt(config.vineBelow() + 1);
                }
                int x = minX + dx;
                int z = minZ + dz;
                if (Ring4Terrain.blighted(x, z)) {
                    continue;
                }
                boolean island = field.islandColumn(x, z).islandMask() > 0.0;
                if (island) {
                    boolean moss = patches.getValue((x + MOSS_SALT) * MOSS_SCALE, 0.0, z * MOSS_SCALE) > config.mossCover();
                    placed |= underside(level, field, config, x, z, roll, length, hanging, bud, moss, pos);
                } else {
                    for (int side = 0; side < 4; side++) {
                        if (vineRolls[side] < config.vineChance()) {
                            placed |= curtain(level, field, config, x, z, Direction.from2DDataValue(side), vineStarts[side],
                                    vineBelows[side], pos);
                        }
                    }
                }
            }
        }
        return placed;
    }

    /** The dressing of every overhang of an island column: moss or a crystal in the underside, what hangs under it. */
    private static boolean underside(WorldGenLevel level, IslandField field, IslandDressingConfiguration config, int x, int z,
                                     float roll, int length, BlockState hanging, BlockState bud, boolean moss,
                                     BlockPos.MutableBlockPos pos) {
        int bottom = Mth.floor(field.column(x, z).surface()) - IslandUndersideFeature.FLOOR_DEPTH;
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
        List<LedgeStrands.Overhang> overhangs = LedgeStrands.overhangs(top, bottom,
                y -> IslandUndersideFeature.open(level.getBlockState(pos.set(x, y, z))),
                y -> level.getBlockState(pos.set(x, y, z)).isFaceSturdy(level, pos, Direction.DOWN));
        boolean placed = false;
        for (LedgeStrands.Overhang overhang : overhangs) {
            int gap = overhang.floor() == LedgeStrands.NO_FLOOR ? Integer.MAX_VALUE : overhang.underside() - overhang.floor() - 1;
            int u = overhang.underside();
            if (gap < MIN_GAP || !terrain(level.getBlockState(pos.set(x, u, z)))) {
                continue;
            }
            boolean crystal = roll >= config.hangingChance() + config.strandChance()
                    && roll < config.hangingChance() + config.strandChance() + config.crystalChance();
            if (crystal || moss) {
                level.setBlock(pos.set(x, u, z), crystal ? config.crystal() : config.moss(), Block.UPDATE_CLIENTS);
                placed = true;
            }
            if (crystal) {
                placed |= hang(level, pos.set(x, u - 1, z), bud);
            } else if (roll < config.hangingChance()) {
                placed |= hang(level, pos.set(x, u - 1, z), hanging);
            } else if (roll < config.hangingChance() + config.strandChance()) {
                int hang = Math.min(length, gap - 2);
                for (int y = u - 1; y >= u - hang; y--) {
                    if (!hang(level, pos.set(x, y, z), config.strand())) {
                        break;
                    }
                    placed = true;
                }
            }
        }
        return placed;
    }

    /**
     * A curtain of vines in the open column (x, z) down the wall of the island column beside it on {@code side}: from
     * {@code start} blocks under the wall column's top down the sturdy wall, then up to {@code below} blocks on below
     * it (each held by the vine above). Each vine only into air and only where it holds now.
     */
    private static boolean curtain(WorldGenLevel level, IslandField field, IslandDressingConfiguration config, int x, int z,
                                   Direction side, int start, int below, BlockPos.MutableBlockPos pos) {
        int wx = x + side.getStepX();
        int wz = z + side.getStepZ();
        if (field.islandColumn(wx, wz).islandMask() <= 0.0 || Ring4Terrain.blighted(wx, wz)) {
            return false;
        }
        BlockState vine = config.vine().setValue(VineBlock.getPropertyForFace(side), true);
        int floor = Mth.floor(field.column(wx, wz).surface()) + IslandUndersideFeature.FLOOR_DEPTH;
        BlockPos.MutableBlockPos wall = new BlockPos.MutableBlockPos();
        boolean placed = false;
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, wx, wz) - 1 - start;
        // Down the wall: it must be the island (sturdy towards the curtain), well above the ground.
        for (; y > floor; y--) {
            wall.set(wx, y, wz);
            if (!level.getBlockState(wall).isFaceSturdy(level, wall, side.getOpposite())) {
                break;
            }
            placed |= hang(level, pos.set(x, y, z), vine);
        }
        if (!placed) {
            return false;
        }
        for (int n = 0; n < below && y > floor; n++, y--) {
            if (!hang(level, pos.set(x, y, z), vine)) {
                break;
            }
        }
        return true;
    }

    /** {@code state} into air where it holds; false if it went nowhere. */
    private static boolean hang(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (!level.getBlockState(pos).isAir() || !state.canSurvive(level, pos)) {
            return false;
        }
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        return true;
    }

    /** An island's own block: earth or rock (not a tree's wood, the world tree, a vine's rope or a crystal). */
    static boolean terrain(BlockState state) {
        return state.is(BlockTags.MOSS_REPLACEABLE) || state.is(BlockTags.DIRT) || state.is(ModBlocks.TIDESTONE.get())
                || state.is(ModBlocks.COBBLED_TIDESTONE.get()) || state.is(BlockTags.SAND);
    }
}
