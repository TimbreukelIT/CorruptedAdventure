package nl.timtoys.corruptedadventure.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.timtoys.corruptedadventure.registry.ModTags;

import java.util.ArrayList;
import java.util.List;

/**
 * Collapsing blocks ({@code #corruptedadventure:collapsing_blocks}: loose sand, ash crust) cave in when a player walks
 * under them: a hanging one (left by worldgen, which places blocks without updates) just above a player gets a short
 * warning (dust, a creak) and then a scheduled tick, after which vanilla's falling-block logic drops it and lets the
 * neighbours follow. A {@link CollapsingBlock} starts its own collapse instead (ash crust ignores updates and plans
 * its chain, {@link AshCrustCollapse}; this is the only way it ever falls). Only real players trigger it, never
 * mobs. Only falling blocks in the tag count (a block that does not fall would be warned about forever). Server-side only; particles and sound reach the clients through vanilla
 * packets. The name stays from ring 2 (the class is registered from the mod entry point).
 */
public final class LooseSandTrigger {
    /** Each player is checked every this many ticks, spread over the players by entity id. */
    static final int CHECK_INTERVAL = 10;
    /** Delay between the warning and the fall (tuning value). */
    static final int WARNING_TICKS = 15;
    /** Blocks around the player's footprint that are checked (tuning value). */
    static final int HORIZONTAL_REACH = 1;
    /** Blocks above the top of the player's box that are checked (tuning value). */
    static final int VERTICAL_REACH = 2;

    private LooseSandTrigger() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(LooseSandTrigger::onPlayerTick);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer || player.isSpectator()) {
            return;
        }
        if ((player.tickCount + player.getId()) % CHECK_INTERVAL != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        boolean warned = false;
        for (BlockPos pos : scanBox(player.getBoundingBox())) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(ModTags.Blocks.COLLAPSING_BLOCKS)) {
                continue;
            }
            if (shouldTrigger(false, state, level.getBlockState(pos.below()),
                    level.getBlockTicks().hasScheduledTick(pos, state.getBlock()))) {
                warn(level, pos, state, !warned);
                warned = true;
                CollapsingBlock.start(level, pos, state, WARNING_TICKS);
            }
        }
    }

    /** Dust under every block that is about to fall; one creak per check, so a whole ceiling does not stack sounds. */
    private static void warn(ServerLevel level, BlockPos pos, BlockState state, boolean withSound) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, state),
                pos.getX() + 0.5, pos.getY() - 0.05, pos.getZ() + 0.5, 6, 0.3, 0.0, 0.3, 0.0);
        if (withSound) {
            level.playSound(null, pos, SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 0.6F, 0.5F);
        }
    }

    /**
     * The block positions checked above an entity: its footprint widened by {@link #HORIZONTAL_REACH}, from the block
     * that holds the top of the box up to {@link #VERTICAL_REACH} blocks above that top.
     */
    static List<BlockPos> scanBox(AABB box) {
        int minX = Mth.floor(box.minX - HORIZONTAL_REACH);
        int maxX = Mth.floor(box.maxX + HORIZONTAL_REACH);
        int minZ = Mth.floor(box.minZ - HORIZONTAL_REACH);
        int maxZ = Mth.floor(box.maxZ + HORIZONTAL_REACH);
        int minY = Mth.floor(box.maxY);
        int maxY = Mth.floor(box.maxY + VERTICAL_REACH);
        List<BlockPos> positions = new ArrayList<>((maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1));
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    positions.add(new BlockPos(x, y, z));
                }
            }
        }
        return positions;
    }

    /**
     * Whether a block above a player should start to cave in: a collapsing falling block with room to fall that is not
     * already about to (a scheduled tick means a warning or a vanilla update is already under way). Spectators never
     * trigger.
     */
    static boolean shouldTrigger(boolean spectator, BlockState state, BlockState below, boolean alreadyScheduled) {
        return !spectator && state.is(ModTags.Blocks.COLLAPSING_BLOCKS) && state.getBlock() instanceof FallingBlock
                && FallingBlock.isFree(below) && !alreadyScheduled;
    }
}
