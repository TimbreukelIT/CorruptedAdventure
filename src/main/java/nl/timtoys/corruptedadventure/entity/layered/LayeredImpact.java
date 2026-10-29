package nl.timtoys.corruptedadventure.entity.layered;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.entity.nightmare.Nightmare;

import java.util.List;

/**
 * A blow where The Layered stands: the landing of its pounce ({@link LayeredPounceGoal}) and bursting up out of the sand
 * ({@link LayeredBurrowGoal}). Everything living within {@code radius} of its feet (sideways, up to
 * {@link #REACH_UP} blocks above or below) takes the damage (times its strength) and is thrown {@code outward} away from
 * it and {@code lift} up; not a held player, and no Nightmare (itself included: Nightmares do not hit each other).
 * Server-side.
 */
public final class LayeredImpact {
    private static final double REACH_UP = 2.0;

    private LayeredImpact() {
    }

    /** Hits around its feet; returns who was hurt. */
    public static List<LivingEntity> hit(ServerLevel level, TheLayered layered, double radius, double damage,
                                         double outward, double lift) {
        Vec3 centre = layered.position();
        AABB area = new AABB(centre.x - radius, centre.y - REACH_UP, centre.z - radius,
                centre.x + radius, centre.y + REACH_UP, centre.z + radius);
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, area, other -> !(other instanceof Nightmare)
                && other.isAlive() && !layered.hasPassenger(other) && flatDistanceSqr(other, centre) <= radius * radius);
        float amount = (float) damage * layered.strength();
        return near.stream().filter(other -> {
            if (!other.hurt(layered.damageSources().mobAttack(layered), amount)) {
                return false;
            }
            Vec3 away = new Vec3(other.getX() - centre.x, 0.0, other.getZ() - centre.z);
            away = away.lengthSqr() > 1.0E-4 ? away.normalize() : Vec3.ZERO;
            other.push(away.x * outward, lift, away.z * outward);
            other.hurtMarked = true;
            return true;
        }).toList();
    }

    /** The block it stands on (for the particles), or the one its feet touch. */
    static BlockState ground(ServerLevel level, TheLayered layered) {
        BlockState below = level.getBlockState(BlockPos.containing(layered.getX(), layered.getY() - 0.5, layered.getZ()));
        return below.isAir() ? layered.getBlockStateOn() : below;
    }

    private static double flatDistanceSqr(LivingEntity other, Vec3 centre) {
        double dx = other.getX() - centre.x;
        double dz = other.getZ() - centre.z;
        return dx * dx + dz * dz;
    }
}
