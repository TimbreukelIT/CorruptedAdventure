package nl.timtoys.corruptedadventure.condition;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;

/**
 * Package 55b: a wet sponge dries at once where water evaporates (under the open sky in ring 3,
 * {@link WaterEvaporation#evaporatesAt}), with the Nether's effect. Asked by {@code WetSpongeBlockDryMixin} from
 * {@code WetSpongeBlock#onPlace}, the method vanilla dries it in for the Nether, so every route counts: placed by a
 * player or dispenser, {@code /setblock}, and a dry sponge that just soaked up water from a covered neighbour (set
 * without neighbour updates, which {@code NeighborNotifyEvent} would miss). Underground a wet sponge stays wet, like
 * water there.
 */
public final class SpongeDrying {
    private SpongeDrying() {
    }

    /** Dries the wet sponge just placed at {@code pos} when it lies exposed; true if it did. */
    public static boolean dryIfExposed(ServerLevel level, BlockPos pos) {
        if (!WaterEvaporation.evaporatesAt(level, pos)) {
            return false;
        }
        // Vanilla's Nether drying (WetSpongeBlock#onPlace in an ultra-warm dimension).
        level.setBlock(pos, Blocks.SPONGE.defaultBlockState(), Block.UPDATE_ALL);
        level.levelEvent(LevelEvent.PARTICLES_WATER_EVAPORATING, pos, 0);
        level.playSound(null, pos, SoundEvents.WET_SPONGE_DRIES, SoundSource.BLOCKS, 1.0F,
                (1.0F + level.getRandom().nextFloat() * 0.2F) * 0.7F);
        return true;
    }
}
