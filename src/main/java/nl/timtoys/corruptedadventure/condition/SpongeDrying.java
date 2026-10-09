package nl.timtoys.corruptedadventure.condition;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Package 55b: a wet sponge placed where water evaporates (under the open sky in ring 3, {@link HeatZone#exposed})
 * dries at once, with the Nether's effect. Asked by {@code WetSpongeBlockDryMixin} from {@code WetSpongeBlock#onPlace},
 * the method vanilla dries it in for the Nether, so every route of placing counts: a player, a dispenser,
 * {@code /setblock}.
 * <p>
 * Except a sponge that just soaked up water (it replaces a dry sponge): drying it would place a dry sponge, which
 * soaks up the covered water next to it again, and so on, a bottomless pump (the Nether never meets this: it has no
 * water). So it stays wet, like underground; placed again, it dries. Placing a wet sponge beside covered water thus
 * dries it, it soaks up once (vanilla's limit) and stays wet.
 */
public final class SpongeDrying {
    private SpongeDrying() {
    }

    /** Dries the wet sponge just placed at {@code pos} over {@code oldState} when it lies exposed; true if it did. */
    public static boolean dryIfExposed(ServerLevel level, BlockPos pos, BlockState oldState) {
        if (!driesOnPlace(oldState) || !HeatZone.exposed(level, pos)) {
            return false;
        }
        // Vanilla's Nether drying (WetSpongeBlock#onPlace in an ultra-warm dimension).
        level.setBlock(pos, Blocks.SPONGE.defaultBlockState(), Block.UPDATE_ALL);
        level.levelEvent(LevelEvent.PARTICLES_WATER_EVAPORATING, pos, 0);
        level.playSound(null, pos, SoundEvents.WET_SPONGE_DRIES, SoundSource.BLOCKS, 1.0F,
                (1.0F + level.getRandom().nextFloat() * 0.2F) * 0.7F);
        return true;
    }

    /** Not when it replaces a dry sponge: then it just soaked up water (see the class comment). */
    static boolean driesOnPlace(BlockState oldState) {
        return !oldState.is(Blocks.SPONGE);
    }
}
