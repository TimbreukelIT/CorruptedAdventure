package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.material.Fluids;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicField;

/**
 * {@code corruptedadventure:lava_fall} (change 47c, G2-WB3 = A): sparse lava falls that end in lava the terrain already
 * has: from a crater wall into its lake, from the wall of a river gorge into the river, from an abyss wall into the
 * lava sea. A source is a lava block set into the wall a few blocks below its top, open only towards the lava, with a
 * scheduled tick: it runs out and falls (plan §0.6, the same kind of flowing source as the terrain's springs; the
 * deliberate exception to "no lava against air").
 * <p>
 * Placed once per chunk. A column tries with {@code chance} (a hash of the seed and the column, so nothing depends on
 * the order of generation); it qualifies where it holds no lava itself, a neighbouring column has lava at least
 * {@code min_drop} below the source (the pure terrain), both lie in one of {@code biomes} (the ring 3 core: in the
 * transitions the real surface is a blend, the pure terrain only half of it) and the fall really lands in lava (the
 * first block below the open side). Only columns one block inside the chunk try, so the source, the wall around it
 * and the fall's column are all the chunk's own blocks; at most {@code max_per_chunk} sources.
 */
public final class LavaFallFeature extends Feature<LavaFallConfiguration> {
    /** How far below the wall's top the source sits, so the lava does not spill over the rim. */
    static final int SOURCE_DEPTH = 3;
    private static final long SALT = 0x2C6FE96EE78B6955L;

    public LavaFallFeature(Codec<LavaFallConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<LavaFallConfiguration> context) {
        VolcanicField field = Ring3Terrain.field(context.level());
        if (field == null) {
            return false;
        }
        LavaFallConfiguration config = context.config();
        WorldGenLevel level = context.level();
        ChunkPos chunk = new ChunkPos(context.origin());
        long seed = level.getSeed();
        int sources = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int z = chunk.getMinBlockZ() + 1; z < chunk.getMinBlockZ() + 15; z++) {
            for (int x = chunk.getMinBlockX() + 1; x < chunk.getMinBlockX() + 15; x++) {
                if (!tries(seed, x, z, config.chance())) {
                    continue;
                }
                VolcanicField.Column wall = field.column(x, z);
                if (wall.lavaTop() != Integer.MIN_VALUE) {
                    continue;
                }
                int y = Mth.floor(wall.height()) - SOURCE_DEPTH;
                for (Direction side : Direction.Plane.HORIZONTAL) {
                    if (opensOnto(field, x + side.getStepX(), z + side.getStepZ(), y, config.minDrop()) && enclosed(level, pos.set(x, y, z), side)
                            && config.biomes().contains(level.getBiome(pos)) && landsInLava(level, pos.relative(side), config)) {
                        level.setBlock(pos, Blocks.LAVA.defaultBlockState(), Block.UPDATE_CLIENTS);
                        level.scheduleTick(pos, Fluids.LAVA, Fluids.LAVA.getTickDelay(level));
                        sources++;
                        break;
                    }
                }
                if (sources >= config.maxPerChunk()) {
                    return true;
                }
            }
        }
        return sources > 0;
    }

    /** Whether column (x, z) tries to hold a source: a hash of the seed and the column. */
    static boolean tries(long seed, int x, int z, float chance) {
        long h = mix(seed ^ SALT ^ mix(x * 0x9E3779B97F4A7C15L + z * 0xC2B2AE3D27D4EB4FL));
        return (h >>> 11) * 0x1.0p-53 < chance;
    }

    /** Whether column (x, z) has lava at least {@code minDrop} below {@code y}, open above it (pure terrain). */
    private static boolean opensOnto(VolcanicField field, int x, int z, int y, int minDrop) {
        VolcanicField.Column below = field.column(x, z);
        int top = below.lavaTop();
        return top != Integer.MIN_VALUE && y - top >= minDrop && below.height() <= top + 1 && !below.islandAt(y);
    }

    /** The source block and all its neighbours but the open side (above too) are solid (this chunk's), the open side air. */
    private static boolean enclosed(WorldGenLevel level, BlockPos source, Direction open) {
        if (!solid(level.getBlockState(source)) || !level.getBlockState(source.relative(open)).isAir()) {
            return false;
        }
        for (Direction direction : Direction.values()) {
            if (direction != open && !solid(level.getBlockState(source.relative(direction)))) {
                return false;
            }
        }
        return true;
    }

    /** Whether the fall from {@code top} (air) goes down through air into lava, in one of the configured biomes. */
    private static boolean landsInLava(WorldGenLevel level, BlockPos top, LavaFallConfiguration config) {
        if (!config.biomes().contains(level.getBiome(top))) {
            return false;
        }
        BlockPos.MutableBlockPos pos = top.mutable();
        while (pos.getY() > level.getMinBuildHeight() && level.getBlockState(pos).isAir()) {
            pos.move(Direction.DOWN);
        }
        return level.getFluidState(pos).is(FluidTags.LAVA);
    }

    private static boolean solid(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty();
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
