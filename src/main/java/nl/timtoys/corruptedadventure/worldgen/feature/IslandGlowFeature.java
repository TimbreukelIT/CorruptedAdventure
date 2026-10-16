package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
 * column; this one runs before the island's hanging strands). The leaves a neighbouring chunk's tree may put here count
 * as open, so the scan and the spot do not depend on which chunk decorated first. Where the drawn spot fails (water,
 * no shadow), the cell's other columns are tried, nearest first. Every draw is made before the world is asked.
 * Placed once per chunk at its corner; writes only in its own chunk. Water stays: only dry ground takes a block.
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
                // Always the same draws per cell, before the world is asked, so a cell never depends on an earlier one.
                int x0 = minX + cx * config.spacing();
                int z0 = minZ + cz * config.spacing();
                int sx = offset(random, config.spacing());
                int sz = offset(random, config.spacing());
                float accentRoll = random.nextFloat();
                int ax = x0 + random.nextInt(config.spacing());
                int az = z0 + random.nextInt(config.spacing());
                BlockState mythicLight = config.mythic().light().getState(random, BlockPos.ZERO);
                BlockState mythicFallback = config.mythic().fallback().getState(random, BlockPos.ZERO);
                BlockState mythicAccent = config.mythic().accent().getState(random, BlockPos.ZERO);
                BlockState blightedLight = config.blighted().light().getState(random, BlockPos.ZERO);
                BlockState blightedFallback = config.blighted().fallback().getState(random, BlockPos.ZERO);
                BlockState blightedAccent = config.blighted().accent().getState(random, BlockPos.ZERO);
                // The light: the drawn spot, else the cell's other columns nearest to it first (a fixed order).
                for (int[] spot : spots(sx, sz, config.spacing())) {
                    int x = x0 + spot[0];
                    int z = z0 + spot[1];
                    int ground = shadowedGround(level, field, x, z, pos);
                    if (ground == Integer.MIN_VALUE) {
                        continue;
                    }
                    boolean blighted = Ring4Terrain.blighted(x, z);
                    if (put(level, pos.set(x, ground + 1, z), blighted ? blightedLight : mythicLight, blighted ? blightedFallback : mythicFallback)) {
                        placed = true;
                        break;
                    }
                }
                if (accentRoll < Math.max(config.mythic().accentChance(), config.blighted().accentChance())) {
                    int accentGround = shadowedGround(level, field, ax, az, pos);
                    boolean blighted = Ring4Terrain.blighted(ax, az);
                    IslandGlowConfiguration.Palette own = blighted ? config.blighted() : config.mythic();
                    BlockState accent = blighted ? blightedAccent : mythicAccent;
                    if (accentGround != Integer.MIN_VALUE && accentRoll < own.accentChance()) {
                        placed |= put(level, pos.set(ax, accentGround + 1, az), accent, accent);
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

    /**
     * The cell's columns to try for its light, the drawn spot (sx, sz) first, then the others by their distance to it
     * (then by z, then x): where the drawn spot is water or no shadow, the light still finds the cell's dry ground.
     */
    static List<int[]> spots(int sx, int sz, int spacing) {
        List<int[]> spots = new ArrayList<>();
        for (int z = 0; z < spacing; z++) {
            for (int x = 0; x < spacing; x++) {
                spots.add(new int[]{x, z});
            }
        }
        spots.sort(Comparator.<int[]>comparingInt(s -> Math.abs(s[0] - sx) + Math.abs(s[1] - sz)).thenComparingInt(s -> s[1])
                .thenComparingInt(s -> s[0]));
        return spots;
    }

    /**
     * {@code state}, or the fallback where it cannot stand; nothing if neither can. The spot must be open: air or the
     * leaves of a feature tree from a neighbouring chunk, which the plant replaces (a later tree's leaves skip the
     * plant), so both orders end the same.
     */
    private static boolean put(WorldGenLevel level, BlockPos pos, BlockState state, BlockState fallback) {
        if (!open(level.getBlockState(pos))) {
            return false;
        }
        BlockState chosen = state.canSurvive(level, pos) ? state : fallback;
        if (!chosen.canSurvive(level, pos)) {
            return false;
        }
        level.setBlock(pos, chosen, Block.UPDATE_CLIENTS);
        return true;
    }

    /** Air, or the (not persistent) leaves of a feature tree: what a neighbouring chunk's tree may have put here. */
    static boolean open(BlockState state) {
        return state.isAir() || state.is(BlockTags.LEAVES) && state.hasProperty(LeavesBlock.PERSISTENT) && !state.getValue(LeavesBlock.PERSISTENT);
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
        int ground = topBlock(surface + SCAN, surface - SCAN, y -> open(level.getBlockState(pos.set(x, y, z))));
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
