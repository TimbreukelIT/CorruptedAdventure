package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import nl.timtoys.corruptedadventure.worldgen.chunk.IslandFields;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;

import javax.annotation.Nullable;
import java.util.List;

/**
 * {@code corruptedadventure:ledge_strand} (verdant_falls, decision of the user on Q3): climbing routes on the walls.
 * Under an overhanging ledge a strand of {@code strand} (the climbable hanging swamp moss) hangs down to one block above
 * the next ledge or the floor, so a player climbs from ledge to ledge; now and then a single {@code accent} (the
 * glowing hanging roots, which cannot form a strand) hangs right under a ledge instead.
 * <p>
 * Under a climbing knob of a gorge's route ({@link IslandField#climbKnob}, a pure terrain query) a strand always hangs
 * (decision 2026-10-15): the knobs of the origin's chunk get theirs on every placement, whichever columns the
 * placement picks, so every route stays climbable; a second placement finds the strand there and adds nothing.
 * <p>
 * The origin is the column's {@code WORLD_SURFACE_WG} height; the scan ({@link LedgeStrands}) goes down to the sea level
 * through the origin's column only. A ledge counts only where open sky reaches down to its underside within
 * {@code reach} (the {@code WORLD_SURFACE_WG} heights around it), so no strands hang from cave ceilings. Determinism as
 * in ring 2 and 3 (23c): reads the pure terrain of its own chunk's columns (strands and accents are never sturdy, so a
 * second placement in the same column finds no new ledge under them), the terrain query and the generation
 * heightmaps, which no feature changes; writes only air in its own chunk. One random draw per overhang. Never water:
 * strands are not waterlogged and stop above water.
 */
public final class LedgeStrandFeature extends Feature<LedgeStrandConfiguration> {
    public LedgeStrandFeature(Codec<LedgeStrandConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<LedgeStrandConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        LedgeStrandConfiguration config = context.config();
        IslandField field = IslandFields.current(level.getLevel().dimension());
        int x = context.origin().getX();
        int z = context.origin().getZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (LedgeStrands.Overhang overhang : overhangs(level, x, z, context.origin().getY() - 1, pos)) {
            float roll = random.nextFloat();
            int gap = overhang.gap();
            boolean knob = field != null && field.climbKnob(x, overhang.underside(), z);
            if (gap < 2 || !knob && !openAround(level, x, z, overhang.underside(), config.reach())) {
                continue;
            }
            switch (LedgeStrands.hang(gap, knob, roll, config.strandChance(), config.accentChance(), config.minGap(), config.maxGap())) {
                case STRAND -> {
                    strand(level, x, z, overhang, config, pos);
                    placed = true;
                }
                case ACCENT -> {
                    level.setBlock(pos.set(x, overhang.underside() - 1, z), config.accent(), Block.UPDATE_CLIENTS);
                    placed = true;
                }
                case NONE -> {
                }
            }
        }
        if (field != null) {
            ChunkPos chunk = new ChunkPos(context.origin());
            placed |= knobs(level, field, chunk, config, pos);
        }
        return placed;
    }

    /** The strands under the climbing knobs of a chunk that have none yet. */
    private static boolean knobs(WorldGenLevel level, IslandField field, ChunkPos chunk, LedgeStrandConfiguration config,
                                 BlockPos.MutableBlockPos pos) {
        List<BlockPos> knobs = field.climbKnobs(chunk.getMinBlockX(), chunk.getMinBlockZ(), chunk.getMaxBlockX(), chunk.getMaxBlockZ());
        boolean placed = false;
        for (BlockPos knob : knobs) {
            if (!level.getBlockState(pos.set(knob.getX(), knob.getY() - 1, knob.getZ())).isAir()) {
                continue;
            }
            LedgeStrands.Overhang overhang = first(overhangs(level, knob.getX(), knob.getZ(), knob.getY(), pos), knob.getY());
            if (overhang != null && LedgeStrands.hang(overhang.gap(), true, 0.0F, config.strandChance(), config.accentChance(),
                    config.minGap(), config.maxGap()) == LedgeStrands.Hang.STRAND) {
                strand(level, knob.getX(), knob.getZ(), overhang, config, pos);
                placed = true;
            }
        }
        return placed;
    }

    @Nullable
    private static LedgeStrands.Overhang first(List<LedgeStrands.Overhang> overhangs, int underside) {
        return overhangs.isEmpty() || overhangs.get(0).underside() != underside ? null : overhangs.get(0);
    }

    private static List<LedgeStrands.Overhang> overhangs(WorldGenLevel level, int x, int z, int top, BlockPos.MutableBlockPos pos) {
        return LedgeStrands.overhangs(top, level.getSeaLevel(), y -> level.getBlockState(pos.set(x, y, z)).isAir(),
                y -> level.getBlockState(pos.set(x, y, z)).isFaceSturdy(level, pos, Direction.DOWN));
    }

    /**
     * Down to one block above the floor: the strand ends within a jump of the ledge below. Its lowest block is the
     * configured tip (a vine's full-grown tip, so it never grows on); the variant (the flowering vine) is chosen by the
     * strand's position, not by a draw, so the knobs' strands and the random ones agree whichever comes first.
     */
    private static void strand(WorldGenLevel level, int x, int z, LedgeStrands.Overhang overhang, LedgeStrandConfiguration config,
                               BlockPos.MutableBlockPos pos) {
        int bottom = overhang.floor() + 2;
        boolean variant = LedgeStrands.inVariant(x, overhang.underside(), z, config.variantChance());
        for (int y = overhang.underside() - 1; y >= bottom; y--) {
            level.setBlock(pos.set(x, y, z), config.strandAt(y, bottom, variant), Block.UPDATE_CLIENTS);
        }
    }

    /** Whether a column within {@code reach} is open to the sky down to {@code underside}: the ledge hangs over open air. */
    private static boolean openAround(WorldGenLevel level, int x, int z, int underside, int reach) {
        for (int dz = -reach; dz <= reach; dz++) {
            for (int dx = -reach; dx <= reach; dx++) {
                if (level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x + dx, z + dz) <= underside) {
                    return true;
                }
            }
        }
        return false;
    }
}
