package nl.timtoys.corruptedadventure.client.entity.hollow;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.timtoys.corruptedadventure.entity.hollow.HollowRift;
import nl.timtoys.corruptedadventure.entity.hollow.RiftPhase;

import java.util.Set;

/**
 * An omen of The Hollow's arrival, on the client only: torches and lanterns near an arrival rift in its omen (or opening) phase
 * gutter, with smoke and now and then the hiss of a flame going out. The blocks themselves stay as they are.
 */
public final class RiftOmenFlicker {
    private static final Set<Block> FLAMES = Set.of(Blocks.TORCH, Blocks.WALL_TORCH, Blocks.SOUL_TORCH,
            Blocks.SOUL_WALL_TORCH, Blocks.LANTERN, Blocks.SOUL_LANTERN, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE);
    private static final int RADIUS = 10;
    private static final int INTERVAL = 8;
    private static final double SEEN_FROM = 64.0;

    private RiftOmenFlicker() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(RiftOmenFlicker::onClientTick);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || minecraft.isPaused() || level.getGameTime() % INTERVAL != 0) {
            return;
        }
        for (HollowRift rift : level.getEntitiesOfClass(HollowRift.class, minecraft.player.getBoundingBox().inflate(SEEN_FROM))) {
            if (rift.isArrival() && (rift.phase() == RiftPhase.OMEN || rift.phase() == RiftPhase.OPENING)) {
                gutter(level, rift.blockPosition(), level.getRandom());
            }
        }
    }

    private static void gutter(ClientLevel level, BlockPos centre, RandomSource random) {
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-RADIUS, -4, -RADIUS), centre.offset(RADIUS, 8, RADIUS))) {
            if (!FLAMES.contains(level.getBlockState(pos).getBlock()) || random.nextFloat() > 0.5F) {
                continue;
            }
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.7;
            double z = pos.getZ() + 0.5;
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.03, 0.0);
            level.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0.0, 0.02, 0.0);
            if (random.nextFloat() < 0.08F) {
                level.playLocalSound(x, y, z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.3F, 0.6F, false);
            }
        }
    }
}
