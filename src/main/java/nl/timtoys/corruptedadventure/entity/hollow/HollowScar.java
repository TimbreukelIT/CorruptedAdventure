package nl.timtoys.corruptedadventure.entity.hollow;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

import java.util.Map;
import java.util.function.Supplier;

/**
 * The scar an arrival's rift leaves (fase-4-nightmares-concept.md, De Holle "Litteken"): a small, permanent patch of
 * ring 5 corruption on the ground where the rift stood, ragged at its edge. Only natural ground turns (soil, stone,
 * sand, gravel; {@link #CORRUPTED}); everything else, so anything a player built with, stays as it is. Plants on a
 * turned block go. Bounded by {@code [nightmares.hollow] scarRadius} and off with {@code scarEnabled}. Healing or decay
 * follows in phase 5.
 */
public final class HollowScar {
    private static final Map<Block, Supplier<? extends Block>> CORRUPTED = Map.ofEntries(
            Map.entry(Blocks.GRASS_BLOCK, ModBlocks.CORRUPTED_GRASS_BLOCK),
            Map.entry(Blocks.DIRT, ModBlocks.CORRUPTED_DIRT),
            Map.entry(Blocks.COARSE_DIRT, ModBlocks.CORRUPTED_DIRT),
            Map.entry(Blocks.PODZOL, ModBlocks.CORRUPTED_GRASS_BLOCK),
            Map.entry(Blocks.ROOTED_DIRT, ModBlocks.CORRUPTED_DIRT),
            Map.entry(Blocks.MYCELIUM, ModBlocks.CORRUPTED_GRASS_BLOCK),
            Map.entry(Blocks.STONE, ModBlocks.CORRUPTED_STONE),
            Map.entry(Blocks.ANDESITE, ModBlocks.CORRUPTED_STONE),
            Map.entry(Blocks.DIORITE, ModBlocks.CORRUPTED_STONE),
            Map.entry(Blocks.GRANITE, ModBlocks.CORRUPTED_STONE),
            Map.entry(Blocks.TUFF, ModBlocks.CORRUPTED_STONE),
            Map.entry(Blocks.DEEPSLATE, ModBlocks.CORRUPTED_STONE),
            Map.entry(Blocks.SAND, ModBlocks.CORRUPTED_SAND),
            Map.entry(Blocks.RED_SAND, ModBlocks.CORRUPTED_SAND),
            Map.entry(Blocks.GRAVEL, ModBlocks.CORRUPTED_COBBLESTONE),
            Map.entry(ModBlocks.DEAD_GRASS_BLOCK.get(), ModBlocks.CORRUPTED_GRASS_BLOCK),
            Map.entry(ModBlocks.WITHERED_DIRT.get(), ModBlocks.CORRUPTED_DIRT));
    /** How far up and down from the rift's foot the ground is looked for. */
    private static final int REACH = 2;

    private HollowScar() {
    }

    /** Turns the ground round {@code foot}; returns how many blocks changed. */
    public static int place(ServerLevel level, BlockPos foot, double radius, RandomSource random) {
        int changed = 0;
        int r = (int) Math.ceil(radius);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                // Solid in the middle, ragged toward the edge.
                if (distance > radius || random.nextDouble() > 1.15 - distance / radius) {
                    continue;
                }
                BlockPos ground = findGround(level, foot.offset(dx, 0, dz));
                if (ground != null && turn(level, ground, distance < 1.0 && random.nextInt(3) == 0)) {
                    changed++;
                }
            }
        }
        return changed;
    }

    /** The highest natural ground block within {@link #REACH} of the foot's height, or null. */
    private static BlockPos findGround(ServerLevel level, BlockPos column) {
        for (int dy = REACH; dy >= -REACH; dy--) {
            BlockPos pos = column.above(dy);
            if (!level.isLoaded(pos)) {
                return null;
            }
            BlockState state = level.getBlockState(pos);
            if (CORRUPTED.containsKey(state.getBlock())) {
                return pos;
            }
            if (!state.isAir() && !state.canBeReplaced()) {
                // Something else is in the way (a tree, a wall): leave this column.
                return null;
            }
        }
        return null;
    }

    private static boolean turn(ServerLevel level, BlockPos ground, boolean glitch) {
        Supplier<? extends Block> into = CORRUPTED.get(level.getBlockState(ground).getBlock());
        if (into == null) {
            return false;
        }
        BlockPos above = ground.above();
        BlockState plant = level.getBlockState(above);
        if (!plant.isAir() && plant.canBeReplaced() && plant.getFluidState().isEmpty()) {
            level.removeBlock(above, false);
        }
        Block block = glitch ? ModBlocks.GLITCH_BLOCK.get() : into.get();
        return level.setBlock(ground, block.defaultBlockState(), Block.UPDATE_ALL);
    }
}
