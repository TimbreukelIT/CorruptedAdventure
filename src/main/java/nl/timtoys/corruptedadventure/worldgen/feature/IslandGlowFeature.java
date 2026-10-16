package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;

import java.util.function.IntPredicate;

/**
 * {@code corruptedadventure:island_glow} (teststop 5a decision, package 75; Q13 = B, G4 = B): glowing ground cover on
 * the dry land in the shadow of the floating islands, so hostile mobs do not spawn there by day (block light ≥ 1). The
 * chunk is split into cells of {@code spacing}; each cell gets one light source near its middle (offset 1 to
 * {@code spacing} − 2), so with a source of light ≥ 5 every block of the cell lies within its reach on level ground.
 * The palette follows the band of ring 4 per column ({@link Ring4Terrain#blighted}): mythical glow blooms and glowcaps
 * with aquamarine buds; blighted glowcaps, toxic crystal clusters and buds, no budding blocks (H5 = A).
 * <p>
 * A cell counts only where an island stands over the column ({@link IslandField#islandColumn}, pure) and the frozen
 * {@code WORLD_SURFACE_WG} lies well above the ground found near the terrain's surface (a short scan in the own
 * column, only through terrain: features of other chunks never write here, this one runs before the island's hanging
 * strands). Placed once per chunk at its corner; writes only in its own chunk. Water stays: only dry ground with air
 * above takes a block.
 */
public final class IslandGlowFeature extends Feature<IslandGlowConfiguration> {
    /** How far above and below the terrain's surface the scan looks for the real top block (interpolation, holes). */
    static final int SCAN = 4;
    /** The open space above the ground that marks an island's shadow (the island's underside lies higher). */
    static final int MIN_SHADOW = 4;

    public IslandGlowFeature(Codec<IslandGlowConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<IslandGlowConfiguration> context) {
        WorldGenLevel level = context.level();
        IslandField field = Ring4Terrain.field(level);
        if (field == null) {
            return false;
        }
        IslandGlowConfiguration config = context.config();
        RandomSource random = context.random();
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        int cells = 16 / config.spacing();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int cz = 0; cz < cells; cz++) {
            for (int cx = 0; cx < cells; cx++) {
                // Always the same draws per cell, so a cell's spots never depend on an earlier cell.
                int x = minX + cx * config.spacing() + offset(random, config.spacing());
                int z = minZ + cz * config.spacing() + offset(random, config.spacing());
                float accentRoll = random.nextFloat();
                int ax = minX + cx * config.spacing() + random.nextInt(config.spacing());
                int az = minZ + cz * config.spacing() + random.nextInt(config.spacing());
                IslandGlowConfiguration.Palette palette = null;
                int ground = shadowedGround(level, field, x, z, pos);
                if (ground != Integer.MIN_VALUE) {
                    palette = Ring4Terrain.blighted(x, z) ? config.blighted() : config.mythic();
                    placed |= put(level, pos.set(x, ground + 1, z), random, palette.light(), palette.fallback());
                }
                if (accentRoll < config.mythic().accentChance() || accentRoll < config.blighted().accentChance()) {
                    int accentGround = shadowedGround(level, field, ax, az, pos);
                    IslandGlowConfiguration.Palette own = Ring4Terrain.blighted(ax, az) ? config.blighted() : config.mythic();
                    if (accentGround != Integer.MIN_VALUE && accentRoll < own.accentChance()) {
                        placed |= put(level, pos.set(ax, accentGround + 1, az), random, own.accent(), own.accent());
                    }
                }
            }
        }
        return placed;
    }

    /** The cell offset of the light source: 1 to spacing − 2 (the middle of the cell), or 0–1 for a spacing of 2–3. */
    static int offset(RandomSource random, int spacing) {
        return spacing >= 4 ? 1 + random.nextInt(spacing - 2) : random.nextInt(Math.min(2, spacing));
    }

    /** {@code state}, or the fallback's where it cannot stand; nothing if neither can. */
    private static boolean put(WorldGenLevel level, BlockPos pos, RandomSource random, BlockStateProvider state,
                               BlockStateProvider fallback) {
        if (!level.getBlockState(pos).isAir()) {
            return false;
        }
        BlockState first = state.getState(random, pos);
        BlockState chosen = first.canSurvive(level, pos) ? first : fallback.getState(random, pos);
        if (!chosen.canSurvive(level, pos)) {
            return false;
        }
        level.setBlock(pos, chosen, Block.UPDATE_CLIENTS);
        return true;
    }

    /**
     * The Y of the dry ground in an island's shadow at (x, z), or {@link Integer#MIN_VALUE}: an island over the column,
     * the top block near the terrain's surface solid and sturdy on top with air above, and the frozen surface (the
     * island's top) well above it.
     */
    private static int shadowedGround(WorldGenLevel level, IslandField field, int x, int z, BlockPos.MutableBlockPos pos) {
        if (field.islandColumn(x, z).islandMask() <= 0.0) {
            return Integer.MIN_VALUE;
        }
        int surface = Mth.ceil(field.column(x, z).surface());
        int ground = topBlock(surface + SCAN, surface - SCAN, y -> level.getBlockState(pos.set(x, y, z)).isAir());
        if (ground == Integer.MIN_VALUE || level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) <= ground + MIN_SHADOW) {
            return Integer.MIN_VALUE;
        }
        BlockState below = level.getBlockState(pos.set(x, ground, z));
        if (!below.getFluidState().isEmpty() || !below.isFaceSturdy(level, pos, Direction.UP)) {
            return Integer.MIN_VALUE;
        }
        return ground;
    }

    /**
     * The first block from {@code top} down to {@code bottom} that is not air with air right above it, or
     * {@link Integer#MIN_VALUE}. A scan that starts inside a solid (a low island's underside) first passes its air.
     */
    static int topBlock(int top, int bottom, IntPredicate air) {
        boolean airAbove = false;
        for (int y = top; y >= bottom; y--) {
            boolean isAir = air.test(y);
            if (!isAir && airAbove) {
                return y;
            }
            airAbove = isAir;
        }
        return Integer.MIN_VALUE;
    }
}
