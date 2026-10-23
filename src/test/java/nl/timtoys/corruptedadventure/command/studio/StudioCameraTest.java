package nl.timtoys.corruptedadventure.command.studio;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudioCameraTest {
    /** A 9 x 11 house on Y 64 with walls to 68 and a pyramid roof up to 72 (narrower towards the top). */
    private static final List<StudioCamera.Column> HOUSE = house();

    private static List<StudioCamera.Column> house() {
        List<StudioCamera.Column> columns = new ArrayList<>();
        for (int x = -4; x <= 4; x++) {
            for (int z = -5; z <= 5; z++) {
                int edge = Math.min(Math.min(x + 4, 4 - x), Math.min(z + 5, 5 - z));
                columns.add(new StudioCamera.Column(x, z, 64, 68 + Math.min(edge, 4)));
            }
        }
        return columns;
    }

    @Test
    void theFullSetHasIsoElevationsTopCutsAndNight() {
        List<StudioCamera.Pose> poses = StudioCamera.poses(HOUSE, List.of(66, 70));
        assertEquals(4 + 4 + 1 + 2 + 4, poses.size());
        assertEquals(List.of("iso_ne_day", "iso_se_day", "iso_sw_day", "iso_nw_day", "elev_n_day", "elev_e_day", "elev_s_day",
                        "elev_w_day", "top_day", "cut_f1_y66_day", "cut_f2_y70_day", "iso_ne_night", "iso_se_night", "iso_sw_night",
                        "iso_nw_night"),
                poses.stream().map(StudioCamera.Pose::label).toList());
        assertEquals(4, poses.stream().filter(StudioCamera.Pose::night).count());
        assertEquals(Integer.valueOf(66), poses.get(9).cutY());
        assertNull(poses.getFirst().cutY());
    }

    @Test
    void camerasStandOnTheSideTheyAreNamedAfter() {
        List<StudioCamera.Pose> poses = StudioCamera.poses(HOUSE, List.of());
        StudioCamera.Pose ne = poses.get(0);
        assertTrue(ne.x() > 0.5 && ne.z() < 0.5, "NE corner: east (+x) and north (-z)");
        StudioCamera.Pose sw = poses.get(2);
        assertTrue(sw.x() < 0.5 && sw.z() > 0.5);
        StudioCamera.Pose north = poses.get(4);
        assertTrue(north.z() < -5 && Math.abs(north.x() - 0.5) < 1e-3);
        StudioCamera.Pose east = poses.get(5);
        assertTrue(east.x() > 5 && Math.abs(east.z() - 0.5) < 1e-3);
        StudioCamera.Pose top = poses.get(8);
        assertTrue(top.eyeY() > 73);
        assertEquals(0.5, top.x(), 1e-3);
        assertEquals(0.5, top.z(), 1e-3);
    }

    @Test
    void theStructureFillsTheViewUpToTheMarginAndIsCentred() {
        for (StudioCamera.Pose pose : StudioCamera.poses(HOUSE, List.of(66))) {
            double[][] points = StudioCamera.corners(HOUSE, pose.cutY() == null ? Integer.MAX_VALUE : pose.cutY());
            double[] f = StudioCamera.forward(pose.yaw(), pose.pitch());
            double[] right = StudioCamera.right(pose.yaw());
            double[] up = StudioCamera.cross(right, f);
            double tanV = Math.tan(Math.toRadians(StudioCamera.VERTICAL_FOV / 2.0));
            double tanH = tanV * StudioCamera.ASPECT;
            double minX = 9, maxX = -9, minY = 9, maxY = -9;
            for (double[] p : points) {
                double vx = p[0] - pose.x(), vy = p[1] - pose.eyeY(), vz = p[2] - pose.z();
                double depth = vx * f[0] + vy * f[1] + vz * f[2];
                assertTrue(depth > 0.0, pose.label() + ": point behind the camera");
                double sx = (vx * right[0] + vy * right[1] + vz * right[2]) / (depth * tanH);
                double sy = (vx * up[0] + vy * up[1] + vz * up[2]) / (depth * tanV);
                minX = Math.min(minX, sx);
                maxX = Math.max(maxX, sx);
                minY = Math.min(minY, sy);
                maxY = Math.max(maxY, sy);
            }
            double fill = Math.max(Math.max(-minX, maxX), Math.max(-minY, maxY));
            assertEquals(StudioCamera.MARGIN, fill, 1e-6, pose.label() + ": fills the view up to the margin");
            assertEquals(0.0, (minX + maxX) / 2.0, 0.02, pose.label() + ": centred left-right");
            assertEquals(0.0, (minY + maxY) / 2.0, 0.02, pose.label() + ": centred up-down");
        }
    }

    @Test
    void aCutDropsWhatLiesAboveIt() {
        assertEquals(HOUSE.size() * 8, StudioCamera.corners(HOUSE, Integer.MAX_VALUE).length);
        for (double[] p : StudioCamera.corners(HOUSE, 66)) {
            assertTrue(p[1] <= 67.0);
        }
        assertEquals(0, StudioCamera.corners(HOUSE, 63).length);
    }
}
