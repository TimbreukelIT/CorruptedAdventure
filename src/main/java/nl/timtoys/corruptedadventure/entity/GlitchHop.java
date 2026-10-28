package nl.timtoys.corruptedadventure.entity;

import net.minecraft.core.BlockPos;
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
import nl.timtoys.corruptedadventure.ring.RingWorld;
import nl.timtoys.corruptedadventure.weather.Ring5Glitches;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * The hop of a mob hit by a glitch arrow (fase-2-items.md §7.4): {@link #MIN_BLOCKS}–{@link #MAX_BLOCKS} blocks to a
 * spot the void teleport's search ({@link VoidTeleportTarget}) finds safe and the mob's whole box fits in, never into
 * the void and in ring 5 never outward, toward it (in a world of another preset: any safe spot). Server-side, a plain
 * teleport (vanilla's position sync) with the void teleport's glitch sound and pixels at both spots. No safe spot: the
 * mob stays.
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
        BlockPos from = mob.blockPosition();
        RingRule rule = RingWorld.isActive(level.getServer())
                ? RingRule.of(RingLayout.current(), level.dimension(), from.getX(), from.getZ()) : null;
        Optional<BlockPos> target = VoidTeleportTarget.find(new LevelTerrain(level, mob, rule), from, MIN_BLOCKS,
                MAX_BLOCKS, mob.getRandom());
        if (target.isEmpty()) {
            return false;
        }
        Vec3 start = mob.position();
        Vec3 to = Vec3.atBottomCenterOf(target.get());
        mob.stopRiding();
        Ring5Glitches.teleported(mob, start, to);
        mob.teleportTo(to.x, to.y, to.z);
        mob.resetFallDistance();
        mob.getNavigation().stop();
        level.gameEvent(GameEvent.TELEPORT, start, GameEvent.Context.of(mob));
        return true;
    }

    /**
     * Where a hop from block column {@code fromX, fromZ} of {@code dimension} may land in the ring layout: never in the
     * void (rim), and in ring 5 not farther out than it started (the void lies outward). The start's distance is
     * worked out once per hop, not per column tried.
     */
    record RingRule(RingLayout layout, int scale, double fromDistance) {
        static RingRule of(RingLayout layout, ResourceKey<Level> dimension, int fromX, int fromZ) {
            int scale = RingLayout.scale(dimension);
            return new RingRule(layout, scale, layout.effectiveDistance((fromX + 0.5) * scale, (fromZ + 0.5) * scale));
        }

        /** Whether column {@code x, z} may be a target. */
        boolean allows(int x, int z) {
            double targetX = (x + 0.5) * scale;
            double targetZ = (z + 0.5) * scale;
            Ring ring = layout.sampleOverworld(targetX, targetZ).ring();
            if (ring == Ring.VOID) {
                return false;
            }
            // Also in the Nether: its ring 5 borders the void rim (nether_void_edge) on its outside too.
            return ring != Ring.RING_5 || layout.effectiveDistance(targetX, targetZ) <= fromDistance;
        }
    }

    /**
     * The real level round the mob: loaded chunks only, inside the world border, by the {@link RingRule} (none: a world
     * of another preset).
     */
    private record LevelTerrain(ServerLevel level, Mob mob, @Nullable RingRule rule) implements VoidTeleportTarget.Terrain {
        @Override
        public BlockGetter blocks() {
            return level;
        }

        @Override
        public boolean allowsColumn(int x, int z) {
            return VoidTeleportTarget.loadedInsideBorder(level, x, z) && (rule == null || rule.allows(x, z));
        }

        /** The mob's box (it may be wider or taller than a player) hits no block and no fluid there. */
        @Override
        public boolean fits(BlockPos feet) {
            AABB box = mob.getDimensions(mob.getPose()).makeBoundingBox(Vec3.atBottomCenterOf(feet));
            return level.noCollision(mob, box) && !level.containsAnyLiquid(box);
        }
    }
}
