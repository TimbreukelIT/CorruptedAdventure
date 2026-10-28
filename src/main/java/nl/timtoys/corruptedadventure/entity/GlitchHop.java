package nl.timtoys.corruptedadventure.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import nl.timtoys.corruptedadventure.condition.VoidTeleportTarget;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.weather.Ring5Glitches;

import java.util.Optional;

/**
 * The hop of a mob hit by a glitch arrow (fase-2-items.md §7.4): {@link #MIN_BLOCKS}–{@link #MAX_BLOCKS} blocks to a
 * spot the void teleport's search ({@link VoidTeleportTarget}) finds safe and the mob's whole box fits in, never into
 * the void and in ring 5 never outward, toward it. Server-side, a plain teleport (vanilla's position sync) with the void
 * teleport's glitch sound and pixels at both spots. No safe spot: the mob stays.
 * <p>
 * Bosses ({@code #c:bosses}) and a mob that carries a rider stay too (the rider could be a player, who does not hop for
 * now); a mob that rides gets off and hops.
 */
public final class GlitchHop {
    static final int MIN_BLOCKS = 1;
    static final int MAX_BLOCKS = 5;

    private GlitchHop() {
    }

    /** Moves {@code mob} if it may hop and there is a safe spot; returns whether it moved. */
    static boolean tryHop(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level) || !mob.isAlive() || mob.isVehicle()
                || mob.getType().is(Tags.EntityTypes.BOSSES)) {
            return false;
        }
        Optional<BlockPos> target = VoidTeleportTarget.find(new LevelTerrain(level, mob, mob.blockPosition()),
                mob.blockPosition(), MIN_BLOCKS, MAX_BLOCKS, mob.getRandom());
        if (target.isEmpty()) {
            return false;
        }
        Vec3 from = mob.position();
        Vec3 to = Vec3.atBottomCenterOf(target.get());
        mob.stopRiding();
        Ring5Glitches.teleported(mob, from, to);
        mob.teleportTo(to.x, to.y, to.z);
        mob.resetFallDistance();
        mob.getNavigation().stop();
        level.gameEvent(GameEvent.TELEPORT, from, GameEvent.Context.of(mob));
        return true;
    }

    /**
     * Whether a hop from block column {@code fromX, fromZ} may land in column {@code x, z} of {@code dimension}: never in
     * the void (rim), and in ring 5 not farther out than it started (the void lies outward).
     */
    static boolean allowsRing(RingLayout layout, ResourceKey<Level> dimension, int fromX, int fromZ, int x, int z) {
        int scale = RingLayout.scale(dimension);
        double targetX = (x + 0.5) * scale;
        double targetZ = (z + 0.5) * scale;
        Ring ring = layout.sampleOverworld(targetX, targetZ).ring();
        if (ring == Ring.VOID) {
            return false;
        }
        return ring != Ring.RING_5
                || layout.effectiveDistance(targetX, targetZ) <= layout.effectiveDistance((fromX + 0.5) * scale, (fromZ + 0.5) * scale);
    }

    /** The real level round the mob: loaded chunks only, inside the world border, by {@link #allowsRing}. */
    private record LevelTerrain(ServerLevel level, Mob mob, BlockPos from) implements VoidTeleportTarget.Terrain {
        @Override
        public BlockGetter blocks() {
            return level;
        }

        @Override
        public boolean allowsColumn(int x, int z) {
            return level.getWorldBorder().isWithinBounds(x + 0.5, z + 0.5)
                    && isLoaded(x - 1, z - 1) && isLoaded(x + 1, z - 1) && isLoaded(x - 1, z + 1) && isLoaded(x + 1, z + 1)
                    && allowsRing(RingLayout.current(), level.dimension(), from.getX(), from.getZ(), x, z);
        }

        /** The mob's box (it may be wider or taller than a player) hits no block and no fluid there. */
        @Override
        public boolean fits(BlockPos feet) {
            AABB box = mob.getDimensions(mob.getPose()).makeBoundingBox(Vec3.atBottomCenterOf(feet));
            return level.noCollision(mob, box) && !level.containsAnyLiquid(box);
        }

        /** The chunk of this column is loaded (checking the 4 corners of a 3 × 3 covers every chunk it touches). */
        private boolean isLoaded(int x, int z) {
            return level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(x),
                    SectionPos.blockToSectionCoord(z)) != null;
        }
    }
}
