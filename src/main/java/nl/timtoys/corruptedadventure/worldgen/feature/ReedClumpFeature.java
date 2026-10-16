package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.material.Fluids;
import nl.timtoys.corruptedadventure.block.ReedsBlock;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

/**
 * {@code corruptedadventure:reed_clump} (packages 75, 76): a clump of reeds, 2–3 high, on the ground and in water one
 * deep (the bottom block waterlogged), as {@link ReedsBlock} allows. Every column of the disc around the origin that
 * lies in the origin's chunk gets a stem with {@code density}; each block must survive on the one below, and the top
 * one is marked {@link ReedsBlock#TOP}. The ground comes from the frozen {@code WORLD_SURFACE_WG}; writes only in its
 * own chunk, replacing only air and the water of the bottom block.
 */
public final class ReedClumpFeature extends Feature<ReedClumpConfiguration> {
    public ReedClumpFeature(Codec<ReedClumpConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<ReedClumpConfiguration> context) {
        WorldGenLevel level = context.level();
        ReedClumpConfiguration config = context.config();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int r = config.radius();
        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dz = -r; dz <= r; dz++) {
            for (int dx = -r; dx <= r; dx++) {
                // Always both draws, so a column never depends on an earlier one.
                float roll = random.nextFloat();
                int height = config.minHeight() + random.nextInt(config.maxHeight() - config.minHeight() + 1);
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                if (dx * dx + dz * dz > r * r || roll >= config.density() || x >> 4 != origin.getX() >> 4 || z >> 4 != origin.getZ() >> 4) {
                    continue;
                }
                placed |= stem(level, pos.set(x, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z), z), height);
            }
        }
        return placed;
    }

    /** One stem from {@code top} (the first open block above the ground or water); in water one deep from the water. */
    static boolean stem(WorldGenLevel level, BlockPos.MutableBlockPos top, int height) {
        int y = top.getY();
        boolean wet = level.getFluidState(top.below()).is(Fluids.WATER) && level.getFluidState(top.below()).isSource();
        if (wet) {
            if (!level.getFluidState(top.below(2)).isEmpty()) {
                return false;
            }
            y--;
        }
        BlockState reeds = ModBlocks.REEDS.get().defaultBlockState();
        BlockPos last = null;
        for (int i = 0; i < height; i++) {
            BlockPos at = new BlockPos(top.getX(), y + i, top.getZ());
            boolean water = i == 0 && wet;
            if (!water && !level.getBlockState(at).isAir()) {
                break;
            }
            // Placed as the top first: it survives alone; the one below loses the mark when this one stands.
            BlockState state = reeds.setValue(ReedsBlock.WATERLOGGED, water).setValue(ReedsBlock.TOP, true);
            if (!state.canSurvive(level, at)) {
                break;
            }
            level.setBlock(at, state, Block.UPDATE_CLIENTS);
            if (last != null) {
                level.setBlock(last, level.getBlockState(last).setValue(ReedsBlock.TOP, false), Block.UPDATE_CLIENTS);
            }
            last = at;
        }
        return last != null;
    }
}
