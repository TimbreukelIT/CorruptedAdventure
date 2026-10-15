package nl.timtoys.corruptedadventure.condition;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.CreateFluidSourceEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Change 44: water placed in ring 3 of the overworld under the open sky evaporates at once, with vanilla's Nether
 * effect (fizz and large smoke). Underground and under a roof water behaves normally; outside ring 3 nothing changes,
 * and neither does it in an evaporation refuge ({@code #evaporation_refuges}, the ring 3 part of {@code verdant_falls};
 * {@link HeatZone#exposed}).
 * <p>
 * One hook for every route: NeoForge's {@link BlockEvent.NeighborNotifyEvent}, which fires on the server whenever a
 * block is set with neighbour updates. A new water source or a newly waterlogged block there is dried before its
 * neighbours hear of it, so lava next to it never sees the water (no obsidian or cobblestone under the open sky, as in
 * the Nether; user decision 2026-10-07). That covers a bucket (player and dispenser, also into a waterloggable block),
 * ice broken or melting into water (also frosted ice), and {@code /setblock}/{@code /fill}. The same event also fires
 * when a neighbour (redstone, an observer) updates the block at an existing water source, so water that is exposed
 * later (a roof mined away) evaporates once something touches it. Worldgen sets blocks without neighbour updates, so
 * natural water is left alone until then (ring 3 has none above Y ~0).
 * <p>
 * Two flowing waters beside covered sources would turn an open gap into a new source every few ticks, which would dry
 * again and again; {@link CreateFluidSourceEvent} therefore stops a source from counting towards a new one
 * when it borders an evaporating cell. Flowing water itself never evaporates.
 * <p>
 * Drying inside the notification is deliberate (a later tick would let lava turn to obsidian first); the outer update
 * then finishes with the stale water state (neighbour shape updates), which the blocks next to it treat like air, and
 * the client only receives the final state. Known limits: the client predicts a player's bucket for a moment (the
 * server corrects it the same tick), the bucket's own empty sound plays next to the fizz, and a roof mined a moment
 * ago may still dim the sky light for a few ticks (water placed then stays).
 */
public final class WaterEvaporation {
    private WaterEvaporation() {
    }

    static void register(IEventBus gameBus) {
        gameBus.addListener(BlockEvent.NeighborNotifyEvent.class, WaterEvaporation::onNeighborNotify);
        gameBus.addListener(CreateFluidSourceEvent.class, WaterEvaporation::onCreateFluidSource);
    }

    private static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        BlockState state = event.getState();
        // Cheapest test first: this event fires for every block update on the server.
        if (!isWaterSource(state.getFluidState()) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState dried = dried(state);
        if (dried == null || !HeatZone.exposed(level, pos)) {
            return;
        }
        level.setBlock(pos, dried, Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F,
                2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                8, 0.25, 0.25, 0.25, 0.0);
    }

    /** {@code pos} is a water source asked whether it counts towards a new source beside it. */
    private static void onCreateFluidSource(CreateFluidSourceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !event.canConvert()
                || !event.getFluidState().is(FluidTags.WATER)) {
            return;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighbour = event.getPos().relative(direction);
            if (!level.getFluidState(neighbour).isSource() && HeatZone.exposed(level, neighbour)) {
                event.setCanConvert(false);
                return;
            }
        }
    }

    static boolean isWaterSource(FluidState fluid) {
        return fluid.isSource() && fluid.is(FluidTags.WATER);
    }

    /**
     * The block without its water: air for a water block, the dry variant of a waterlogged block; null for blocks
     * that only exist in water (seagrass, kelp, bubble columns), which are left alone.
     */
    static @Nullable BlockState dried(BlockState state) {
        if (state.is(Blocks.WATER)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            return state.setValue(BlockStateProperties.WATERLOGGED, false);
        }
        return null;
    }
}
