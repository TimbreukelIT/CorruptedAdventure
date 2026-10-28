package nl.timtoys.corruptedadventure.condition;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.Ring5Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.weather.Ring5Glitches;

import java.util.Optional;

/**
 * The short void teleport of the second threshold in ring 5's inner band (package 91): server-side, to a spot that
 * {@link VoidTeleportTarget} found safe. A normal server teleport (vanilla's position sync), so other players see a
 * plain jump, with a glitch sound and pixels at both spots ({@code Ring5Glitches}, stream A, package 98).
 */
final class VoidTeleport {
    private VoidTeleport() {
    }

    /** What decides whether the player may be moved now, already read (testable without a server). */
    record Stance(boolean exposed, boolean onGround, boolean riding, boolean carrying, boolean sleeping,
                  boolean fallFlying, boolean inFluid) {
        static Stance of(ServerPlayer player) {
            return new Stance(!player.isCreative() && !player.isSpectator(), player.onGround(), player.isPassenger(),
                    player.isVehicle(), player.isSleeping(), player.isFallFlying(),
                    player.isInLiquid() || player.isInPowderSnow);
        }

        /** Survival or adventure, standing on the ground, not riding or carrying, not sleeping, gliding or swimming. */
        boolean mayTeleport() {
            return exposed && onGround && !riding && !carrying && !sleeping && !fallFlying && !inFluid;
        }
    }

    /**
     * Teleports the player to a safe spot {@code minBlocks}–{@code maxBlocks} away, if there is one; the rotation stays,
     * the fall distance is reset. Returns whether it moved the player.
     */
    static boolean tryTeleport(ServerPlayer player, int minBlocks, int maxBlocks) {
        ServerLevel level = player.serverLevel();
        Optional<BlockPos> target = VoidTeleportTarget.find(new LevelTerrain(level), player.blockPosition(), minBlocks,
                maxBlocks, player.getRandom());
        if (target.isEmpty()) {
            return false;
        }
        BlockPos spot = target.get();
        // Sound and pixels at both spots (stream A, package 98; INT2 merge point with 97).
        Ring5Glitches.teleported(player, player.position(), Vec3.atBottomCenterOf(spot));
        player.teleportTo(level, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, player.getYRot(), player.getXRot());
        player.resetFallDistance();
        return true;
    }

    /** The real level: ring 5's inner band, loaded chunks only, inside the world border. */
    private record LevelTerrain(ServerLevel level) implements VoidTeleportTarget.Terrain {
        @Override
        public BlockGetter blocks() {
            return level;
        }

        @Override
        public boolean allowsColumn(int x, int z) {
            return level.dimension() == Level.OVERWORLD && VoidTeleportTarget.loadedInsideBorder(level, x, z)
                    && isInnerBand(x + 0.5, z + 0.5);
        }

        /** Ring 5 (not the ring 4 side of the boundary, not the void rim) and its inner band. */
        private static boolean isInnerBand(double x, double z) {
            RingLayout layout = RingLayout.current();
            return layout.sampleOverworld(x, z).ring() == Ring.RING_5 && Ring5Band.of(layout).inner(x, z);
        }
    }
}
