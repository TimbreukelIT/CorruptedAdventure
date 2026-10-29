package nl.timtoys.corruptedadventure.entity.unfinished;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModTags;

/**
 * What The Unfinished may touch for a while: natural ground (stone, soil, sand, gravel, ores, the ring 5 rock and
 * bedrock), never something with a block entity or a fluid, and nothing a player builds with (planks, cobblestone,
 * bricks, glass: they are not in these tags). The ground he unloads and the ground his memory shards may lie on.
 */
final class UnfinishedNature {
    private UnfinishedNature() {
    }

    /** A solid natural block. */
    static boolean isGround(BlockState state) {
        if (state.hasBlockEntity() || !state.getFluidState().isEmpty()) {
            return false;
        }
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.DIRT) || state.is(BlockTags.SAND)
                || state.is(Tags.Blocks.GRAVELS) || state.is(Tags.Blocks.ORES) || state.is(ModTags.Blocks.RING_5_STONES)
                || state.is(ModTags.Blocks.VOID_SOURCES) || state.is(Blocks.BEDROCK) || state.is(Blocks.CLAY)
                || state.is(Blocks.CALCITE) || state.is(Blocks.SMOOTH_BASALT)
                || state.is(ModBlocks.CORRUPTED_GRASS_BLOCK.get()) || state.is(ModBlocks.CORRUPTED_DIRT.get());
    }

    /** Air, or a plant or snow layer on the ground (replaceable, no fluid, no block entity). */
    static boolean isOpen(BlockState state) {
        return state.isAir() || state.canBeReplaced() && state.getFluidState().isEmpty() && !state.hasBlockEntity();
    }
}
