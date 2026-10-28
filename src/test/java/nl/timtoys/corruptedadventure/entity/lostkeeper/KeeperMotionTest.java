package nl.timtoys.corruptedadventure.entity.lostkeeper;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeeperMotionTest {
    @Test
    void aSmallTurnArrivesAndALargeOneIsLimited() {
        Vec3 east = new Vec3(1.0, 0.0, 0.0);
        Vec3 slightly = new Vec3(Math.cos(0.05), 0.0, Math.sin(0.05));
        assertEquals(slightly, KeeperMotion.turn(east, slightly, 0.1));
        Vec3 north = new Vec3(0.0, 0.0, -1.0);
        Vec3 turned = KeeperMotion.turn(east, north, 0.1);
        assertEquals(1.0, turned.length(), 1.0E-9);
        assertEquals(0.1, Math.acos(turned.dot(east)), 1.0E-9);
        assertTrue(turned.z < 0.0, "towards the target");
    }

    @Test
    void turningRightRoundDoesNotGetStuck() {
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        Vec3 turned = KeeperMotion.turn(up, new Vec3(0.0, -1.0, 0.0), 0.2);
        assertEquals(0.2, Math.acos(turned.dot(up)), 1.0E-9);
        Vec3 east = new Vec3(1.0, 0.0, 0.0);
        assertFalse(KeeperMotion.turn(east, east.scale(-1.0), 0.2).equals(east));
    }

    @Test
    void yawAndPitchFollowMinecraftsConventions() {
        assertEquals(0.0F, KeeperMotion.yaw(new Vec3(0.0, 0.0, 1.0)), 1.0E-4F);
        assertEquals(90.0F, KeeperMotion.yaw(new Vec3(-1.0, 0.0, 0.0)), 1.0E-4F);
        assertEquals(90.0F, KeeperMotion.pitch(new Vec3(0.0, -1.0, 0.0)), 1.0E-4F);
        Vec3 look = Vec3.directionFromRotation(30.0F, 120.0F);
        assertEquals(120.0F, KeeperMotion.yaw(look), 1.0E-3F);
        assertEquals(30.0F, KeeperMotion.pitch(look), 1.0E-3F);
        Vec3 up = KeeperMotion.up(0.0F, 0.0F);
        assertEquals(1.0, up.y, 1.0E-6);
    }

    @Test
    void theBreathConeHoldsWhatIsAheadWithinItsAngleAndRange() {
        Vec3 mouth = Vec3.ZERO;
        Vec3 axis = new Vec3(0.0, 0.0, 1.0);
        double cos = Math.cos(Math.toRadians(25.0));
        assertTrue(KeeperBreathGoal.inCone(mouth, axis, new Vec3(0.0, 0.0, 8.0), 10.0, cos));
        assertTrue(KeeperBreathGoal.inCone(mouth, axis, new Vec3(2.0, 0.0, 8.0), 10.0, cos));
        assertFalse(KeeperBreathGoal.inCone(mouth, axis, new Vec3(6.0, 0.0, 8.0), 10.0, cos), "outside the angle");
        assertFalse(KeeperBreathGoal.inCone(mouth, axis, new Vec3(0.0, 0.0, 11.0), 10.0, cos), "beyond the range");
        assertFalse(KeeperBreathGoal.inCone(mouth, axis, new Vec3(0.0, 0.0, -4.0), 10.0, cos), "behind");
        assertTrue(KeeperBreathGoal.inCone(mouth, axis, new Vec3(0.5, 0.0, -0.5), 10.0, cos), "at the mouth");
    }
}
