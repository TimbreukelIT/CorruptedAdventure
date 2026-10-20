package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;

/**
 * {@code corruptedadventure:island_underside} (package 75; design ring 4 "Uitgerukt", "Omhoog"): what hangs under the
 * floating islands. Strands of hanging moss of a few blocks, single glowing roots right under the island, and under
 * the low islands (gap up to {@code max_climb_gap}) now and then a strand down to one block above the ground or the
 * lake, so a player can climb up (the moss is climbable in all of ring 4).
 * <p>
 * Per chunk, per column one roll; only a hit pays for the scan. A column counts where an island stands over it
 * ({@link IslandField#islandColumn}, pure); the overhangs are found in the own column from the frozen
 * {@code WORLD_SURFACE_WG} down to just below the terrain's surface ({@link LedgeStrands#overhangs}), so the scan reads
 * the island and the open space under it, which only this chunk's features change (this one after the glow on the
 * ground). The leaves a neighbouring chunk's tree may put here and every non-solid plant count as open ({@link #open}:
 * the scan does not depend on which chunk decorated first). Writes only into air in its own chunk, a strand down to the
 * first block that is taken (decor rollout C: a tree's leaf wins in either order); never water (a strand stops above
 * it).
 */
public final class IslandUndersideFeature extends Feature<IslandUndersideConfiguration> {
    /** How far below the terrain's surface the scan still looks for the floor under an island. */
    static final int FLOOR_DEPTH = 4;

    public IslandUndersideFeature(Codec<IslandUndersideConfiguration> codec) {
        super(codec);
    }

    /** What one column gets. */
    enum Kind {
        NONE, CLIMB, DANGLE, ACCENT
    }

    /** The kind for a roll in [0, 1): climbing strands first, then dangling ones, then accents. */
    static Kind kind(float roll, IslandUndersideConfiguration config) {
        if (roll < config.climbChance()) {
            return Kind.CLIMB;
        }
        if (roll < config.climbChance() + config.strandChance()) {
            return Kind.DANGLE;
        }
        return roll < config.climbChance() + config.strandChance() + config.accentChance() ? Kind.ACCENT : Kind.NONE;
    }

    /**
     * How many blocks hang under an overhang with {@code gap} air: a climbing strand to one block above the floor (if the
     * gap allows climbing, else it dangles), a dangling one {@code length} long but never closer than two blocks to the
     * floor; 0 for none.
     */
    static int length(Kind kind, int gap, int length, int maxClimbGap) {
        if (kind == Kind.CLIMB && gap <= maxClimbGap) {
            return Math.max(0, gap - 1);
        }
        if (kind == Kind.CLIMB || kind == Kind.DANGLE) {
            return Math.max(0, Math.min(length, gap - 2));
        }
        return kind == Kind.ACCENT && gap >= 2 ? 1 : 0;
    }

    @Override
    public boolean place(FeaturePlaceContext<IslandUndersideConfiguration> context) {
        WorldGenLevel level = context.level();
        IslandField field = Ring4Terrain.field(level);
        if (field == null) {
            return false;
        }
        IslandUndersideConfiguration config = context.config();
        RandomSource random = context.random();
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int dz = 0; dz < 16; dz++) {
            for (int dx = 0; dx < 16; dx++) {
                // Always the same draws, before the world is asked, so a column's outcome never depends on an earlier one.
                float roll = random.nextFloat();
                int dangle = config.minLength() + random.nextInt(config.maxLength() - config.minLength() + 1);
                BlockState accent = config.accent().getState(random, BlockPos.ZERO);
                Kind kind = kind(roll, config);
                int x = minX + dx;
                int z = minZ + dz;
                if (kind == Kind.NONE || field.islandColumn(x, z).islandMask() <= 0.0) {
                    continue;
                }
                int bottom = Mth.floor(field.column(x, z).surface()) - FLOOR_DEPTH;
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                for (LedgeStrands.Overhang overhang : LedgeStrands.overhangs(top, bottom,
                        y -> open(level.getBlockState(pos.set(x, y, z))),
                        y -> level.getBlockState(pos.set(x, y, z)).isFaceSturdy(level, pos, Direction.DOWN))) {
                    int hang = length(kind, overhang.gap(), dangle, config.maxClimbGap());
                    // Nothing hangs from the world tree (74: its wood is terrain, but no island's underside).
                    if (hang <= 0 || level.getBlockState(pos.set(x, overhang.underside(), z)).is(BlockTags.LOGS)) {
                        continue;
                    }
                    if (kind == Kind.ACCENT) {
                        placed |= fill(level, pos.set(x, overhang.underside() - 1, z), accent);
                        continue;
                    }
                    // Decor rollout C: only into air, and the strand ends at the first block that is not (a neighbour's
                    // leaf): the shape update cuts it there the same way when the leaf comes later.
                    for (int y = overhang.underside() - 1; y >= overhang.underside() - hang; y--) {
                        if (!fill(level, pos.set(x, y, z), config.strand())) {
                            break;
                        }
                        placed = true;
                    }
                }
            }
        }
        return placed;
    }

    /**
     * What the scan for overhangs counts as open (decor rollout C, backlog "boom-determinisme, rest"): air, the leaves of
     * a feature tree ({@link IslandGlowFeature#open}) and every other block that is neither solid nor fluid (the glow
     * plants on the ground, the strands and plants of this chunk's features), so an overhang and its gap are the
     * terrain's alone, whether a neighbour's leaf or this chunk's glow plant came first. Water stays a floor.
     */
    static boolean open(BlockState state) {
        return IslandGlowFeature.open(state) || !state.isSolid() && state.getFluidState().isEmpty();
    }

    /** {@code state} into air where it holds; false if the spot is taken or it cannot stand there. */
    private static boolean fill(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (!level.getBlockState(pos).isAir() || !state.canSurvive(level, pos)) {
            return false;
        }
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        return true;
    }
}
