package nl.timtoys.corruptedadventure.item.weakness;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.List;

/**
 * Who a weakness item reaches (server): never the user, never spectators. A fake player (a machine using the item)
 * reaches nobody: automation must not keep a Nightmare blinded or stunned, and the director does not count fake players
 * as fighters either.
 */
final class WeaknessReach {
    private WeaknessReach() {
    }

    /**
     * Living entities of which a sight point lies within {@code range} of {@code user}'s eyes, inside the cone of
     * {@code coneDegrees} (half angle) round its look direction, with only light-passing blocks between
     * ({@link LightClip}). The sight points are where the look direction meets the hitbox, its centre and its eyes, so
     * a large mob counts where it is aimed at, not only by its eyes. A player only where {@code user} can harm it (the
     * server's pvp setting and teams).
     */
    static List<LivingEntity> inCone(ServerLevel level, Player user, double range, double coneDegrees) {
        if (user instanceof FakePlayer) {
            return List.of();
        }
        Vec3 eyes = user.getEyePosition();
        Vec3 look = user.getLookAngle();
        double minCos = Math.cos(Math.toRadians(coneDegrees));
        AABB box = new AABB(eyes, eyes).inflate(range);
        return level.getEntitiesOfClass(LivingEntity.class, box, target -> {
            if (!reachable(user, target) || target instanceof Player player && !user.canHarmPlayer(player)) {
                return false;
            }
            AABB hitbox = target.getBoundingBox();
            for (Vec3 point : List.of(aimedPoint(hitbox, eyes, look, range), hitbox.getCenter(), target.getEyePosition())) {
                if (inCone(eyes, look, point, range, minCos) && lit(level, user, eyes, point)) {
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Living entities other than players whose hitbox comes within {@code radius} of {@code user}'s eyes, through walls.
     * The hitbox rather than the feet, so a large flyer counts by its nearest part.
     */
    static List<LivingEntity> inRadius(ServerLevel level, Player user, double radius) {
        if (user instanceof FakePlayer) {
            return List.of();
        }
        Vec3 eyes = user.getEyePosition();
        AABB box = new AABB(eyes, eyes).inflate(radius);
        double radiusSqr = radius * radius;
        return level.getEntitiesOfClass(LivingEntity.class, box, target -> reachable(user, target)
                && !(target instanceof Player) && nearestPoint(target.getBoundingBox(), eyes).distanceToSqr(eyes) <= radiusSqr);
    }

    /** Where the look ray enters {@code hitbox}; if it passes by, the hitbox point nearest to it. */
    private static Vec3 aimedPoint(AABB hitbox, Vec3 eyes, Vec3 look, double range) {
        return hitbox.clip(eyes, eyes.add(look.scale(range))).orElseGet(() -> {
            double along = Math.max(0.0, hitbox.getCenter().subtract(eyes).dot(look));
            return nearestPoint(hitbox, eyes.add(look.scale(along)));
        });
    }

    private static boolean inCone(Vec3 eyes, Vec3 look, Vec3 point, double range, double minCos) {
        Vec3 toPoint = point.subtract(eyes);
        double distance = toPoint.length();
        return distance <= range && (distance < 1.0E-4 || toPoint.dot(look) / distance >= minCos);
    }

    private static boolean lit(ServerLevel level, Player user, Vec3 eyes, Vec3 point) {
        return level.clip(new LightClip(eyes, point, user)).getType() == HitResult.Type.MISS;
    }

    private static Vec3 nearestPoint(AABB box, Vec3 point) {
        return new Vec3(Math.clamp(point.x, box.minX, box.maxX), Math.clamp(point.y, box.minY, box.maxY),
                Math.clamp(point.z, box.minZ, box.maxZ));
    }

    private static boolean reachable(Player user, LivingEntity target) {
        return target != user && target.isAlive() && !target.isSpectator();
    }

    /**
     * A sight line that light passes: through glass, panes and bars (their visual shape is empty) and through ice, slime
     * and honey (half transparent, but with a full visual shape); fluids do not stop it.
     */
    private static final class LightClip extends ClipContext {
        LightClip(Vec3 from, Vec3 to, Entity entity) {
            super(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, entity);
        }

        @Override
        public VoxelShape getBlockShape(BlockState state, BlockGetter level, BlockPos pos) {
            return state.getBlock() instanceof HalfTransparentBlock ? Shapes.empty() : super.getBlockShape(state, level, pos);
        }
    }
}
