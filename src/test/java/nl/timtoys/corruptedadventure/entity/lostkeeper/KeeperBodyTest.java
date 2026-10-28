package nl.timtoys.corruptedadventure.entity.lostkeeper;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeeperBodyTest {
    private static final double EPSILON = 1.0E-9;

    @Test
    void resetLaysItStraightBehindTheNeck() {
        KeeperBody body = new KeeperBody();
        // Yaw 0 looks to +z, so the body lies towards -z.
        body.reset(new Vec3(10.0, 70.0, 5.0), 0.0F);
        for (int i = 0; i <= KeeperBody.SEGMENTS; i++) {
            Vec3 node = body.node(i);
            assertEquals(10.0, node.x, EPSILON);
            assertEquals(70.0, node.y, EPSILON);
            assertEquals(5.0 - i * KeeperBody.SEGMENT_LENGTH, node.z, EPSILON);
        }
    }

    @Test
    void followingKeepsEverySegmentAtItsLength() {
        KeeperBody body = new KeeperBody();
        body.reset(Vec3.ZERO, 90.0F);
        // A swim in a rising curve.
        for (int t = 1; t <= 200; t++) {
            body.follow(new Vec3(Math.sin(t * 0.05) * 20.0, t * 0.1, Math.cos(t * 0.07) * 15.0));
            for (int i = 1; i <= KeeperBody.SEGMENTS; i++) {
                assertEquals(KeeperBody.SEGMENT_LENGTH, body.node(i).distanceTo(body.node(i - 1)), 1.0E-6);
            }
        }
    }

    @Test
    void theTailTrailsThePathOfTheHead() {
        KeeperBody body = new KeeperBody();
        body.reset(Vec3.ZERO, -90.0F);
        // Swim straight along +x for a long way: the body ends up on the line behind the head.
        for (int t = 1; t <= 100; t++) {
            body.follow(new Vec3(t * 0.5, 0.0, 0.0));
        }
        Vec3 tail = body.node(KeeperBody.SEGMENTS);
        assertEquals(50.0 - KeeperBody.SEGMENTS * KeeperBody.SEGMENT_LENGTH, tail.x, 1.0E-6);
        assertEquals(0.0, tail.z, 1.0E-6);
    }

    @Test
    void theWhipSwingsTheRearRoundThePivotAndKeepsTheLengths() {
        KeeperBody body = new KeeperBody();
        body.reset(Vec3.ZERO, 0.0F);
        Vec3 pivot = body.node(TheLostKeeper.WHIP_PIVOT);
        Vec3 before = body.node(KeeperBody.SEGMENTS).subtract(pivot);
        body.swing(TheLostKeeper.WHIP_PIVOT, Math.PI / 2.0);
        Vec3 after = body.node(KeeperBody.SEGMENTS).subtract(pivot);
        assertEquals(before.length(), after.length(), 1.0E-9);
        assertEquals(0.0, before.dot(after), 1.0E-9);
        // Positive turns +x towards +z: the tail lay along -z, so it now lies along +x.
        assertTrue(after.x > 0.0);
        for (int i = 0; i <= TheLostKeeper.WHIP_PIVOT; i++) {
            assertEquals(0.0, body.node(i).x, EPSILON);
        }
    }

    @Test
    void theNeckSitsBehindTheHeadCentre() {
        Vec3 neck = KeeperBody.neck(new Vec3(0.0, 64.0, 0.0), new Vec3(0.0, 0.0, 1.0));
        assertEquals(64.0 + KeeperBody.AXIS, neck.y, EPSILON);
        assertEquals(-KeeperBody.NECK, neck.z, EPSILON);
    }
}
