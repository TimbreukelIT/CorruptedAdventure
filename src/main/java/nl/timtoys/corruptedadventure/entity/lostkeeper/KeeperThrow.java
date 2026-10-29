package nl.timtoys.corruptedadventure.entity.lostkeeper;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;

/**
 * Throws a fighter of The Lost Keeper (the gust of the bubble trap, the burst, the squeeze of the ring): the server sets
 * the speed and marks it, so the client takes it over (as for the tail whip). A fighter in a boat falls out and the boat
 * goes up as well; nothing breaks.
 */
public final class KeeperThrow {
    private KeeperThrow() {
    }

    /** {@code fighter} gets {@code up} blocks/tick upwards and {@code outward} (horizontal) on top of it. */
    public static void launch(Entity fighter, double up, Vec3 outward) {
        Entity vehicle = fighter.getVehicle();
        if (vehicle instanceof Boat boat) {
            fighter.stopRiding();
            set(boat, up * 0.8, outward.scale(0.5));
        }
        set(fighter, up, outward);
    }

    private static void set(Entity entity, double up, Vec3 outward) {
        entity.setDeltaMovement(outward.x, up, outward.z);
        entity.hurtMarked = true;
        entity.hasImpulse = true;
    }

    /** The horizontal unit vector from {@code centre} to {@code at} times {@code strength}; zero on top of it. */
    static Vec3 away(Vec3 centre, Vec3 at, double strength) {
        Vec3 flat = new Vec3(at.x - centre.x, 0.0, at.z - centre.z);
        return flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize().scale(strength);
    }
}
