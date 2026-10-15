package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * {@code corruptedadventure:ledge_strand} (verdant_falls, decision of the user on Q3): climbing routes on the walls.
 * Under an overhanging ledge a strand of {@code strand} (the climbable hanging swamp moss) hangs down to one block above
 * the next ledge or the floor, so a player climbs from ledge to ledge; now and then a single {@code accent} (the
 * glowing hanging roots, which cannot form a strand) hangs right under a ledge instead.
 * <p>
 * The origin is the column's {@code WORLD_SURFACE_WG} height; the scan ({@link LedgeStrands}) goes down to the sea level
 * through the origin's column only. A ledge counts only where open sky reaches down to its underside within
 * {@code reach} (the {@code WORLD_SURFACE_WG} heights around it), so no strands hang from cave ceilings. Determinism as
 * in ring 2 and 3 (23c): reads the pure terrain of its own column (strands and accents are never sturdy, so a second
 * placement in the same column finds no new ledge under them) and the generation heightmaps, which no feature changes;
 * writes only air in its own column. One random draw per overhang. Never water: strands are not waterlogged and stop
 * above water.
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
        int x = context.origin().getX();
        int z = context.origin().getZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (LedgeStrands.Overhang overhang : LedgeStrands.overhangs(context.origin().getY() - 1, level.getSeaLevel(),
                y -> level.getBlockState(pos.set(x, y, z)).isAir(),
                y -> level.getBlockState(pos.set(x, y, z)).isFaceSturdy(level, pos, Direction.DOWN))) {
            float roll = random.nextFloat();
            int gap = overhang.gap();
            if (gap < 2 || !openAround(level, x, z, overhang.underside(), config.reach())) {
                continue;
            }
            int under = overhang.underside() - 1;
            if (roll < config.strandChance()) {
                if (gap >= config.minGap() && gap <= config.maxGap()) {
                    // Down to one block above the floor: the strand ends within a jump of the ledge below.
                    for (int y = under; y > overhang.floor() + 1; y--) {
                        level.setBlock(pos.set(x, y, z), config.strand(), Block.UPDATE_CLIENTS);
                    }
                    placed = true;
                }
            } else if (roll < config.strandChance() + config.accentChance()) {
                level.setBlock(pos.set(x, under, z), config.accent(), Block.UPDATE_CLIENTS);
                placed = true;
            }
        }
        return placed;
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
