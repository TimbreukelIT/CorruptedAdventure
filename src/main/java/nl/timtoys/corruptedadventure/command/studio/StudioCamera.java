package nl.timtoys.corruptedadventure.command.studio;

import java.util.ArrayList;
import java.util.List;

/**
 * The camera poses of the photo studio (S0-2): pure math on the structure's shape. The camera is the player (spectator)
 * teleported to a pose; the client keeps the default window (854 x 480) and FOV (70°, vertical), so every pose is
 * framed for that view: the structure (every column of its blocks as a prism from its lowest to its highest block) fills
 * {@link #MARGIN} of the view in its tighter direction and sits in the middle of the picture.
 *
 * <p>Minecraft angles: yaw 0 looks south (+Z), 90 west, 180 north, -90 east; pitch 90 looks straight down.
 */
final class StudioCamera {
    static final double VERTICAL_FOV = 70.0;
    static final double ASPECT = 854.0 / 480.0;
    /** Share of the half view the structure may fill (a thin border stays empty). */
    static final double MARGIN = 0.9;
    /** A standing player's eye height: the pose is the feet position the teleport takes. */
    static final double EYE_HEIGHT = 1.62;
    static final float ISO_PITCH = 30.0F;
    static final float ELEVATION_PITCH = 10.0F;
    static final float CUT_PITCH = 50.0F;
    /** Rounds of centring: the projection of an uneven shape is off-centre until the aim point follows it. */
    private static final int CENTRING_ROUNDS = 4;
    /** Yaw of the iso views from the NE, SE, SW and NW corner (looking at the opposite corner). */
    private static final float[] ISO_YAWS = {45.0F, 135.0F, -135.0F, -45.0F};
    private static final String[] ISO_NAMES = {"ne", "se", "sw", "nw"};
    /** Yaw of the elevations seen from the N, E, S and W side. */
    private static final float[] ELEVATION_YAWS = {0.0F, 90.0F, 180.0F, -90.0F};
    private static final String[] ELEVATION_NAMES = {"n", "e", "s", "w"};

    private StudioCamera() {
    }

    /** One column of the structure: its blocks at x, z span minY..maxY (inclusive). */
    record Column(int x, int z, int minY, int maxY) {
    }

    /**
     * One camera pose: feet position and view angles, by day or night, with the cut-away Y (the highest kept block
     * layer) or null.
     */
    record Pose(String label, boolean night, double x, double y, double z, float yaw, float pitch, Integer cutY) {
        double eyeY() {
            return y + EYE_HEIGHT;
        }
    }

    /**
     * The full set: 4 iso corners, 4 elevations and the top view by day, one steep iso view per floor with the layers
     * above it removed ({@code floorCuts}: highest kept Y per floor, bottom up), and the 4 iso corners by night.
     */
    static List<Pose> poses(List<Column> columns, List<Integer> floorCuts) {
        double[][] whole = corners(columns, Integer.MAX_VALUE);
        List<Pose> poses = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            poses.add(frame("iso_" + ISO_NAMES[i] + "_day", false, whole, ISO_YAWS[i], ISO_PITCH, null));
        }
        for (int i = 0; i < 4; i++) {
            poses.add(frame("elev_" + ELEVATION_NAMES[i] + "_day", false, whole, ELEVATION_YAWS[i], ELEVATION_PITCH, null));
        }
        // Yaw 180 looks north: north is the top of the image.
        poses.add(frame("top_day", false, whole, 180.0F, 90.0F, null));
        for (int i = 0; i < floorCuts.size(); i++) {
            int cut = floorCuts.get(i);
            double[][] kept = corners(columns, cut);
            poses.add(frame("cut_f" + (i + 1) + "_y" + cut + "_day", false, kept.length > 0 ? kept : whole, ISO_YAWS[1],
                    CUT_PITCH, cut));
        }
        for (int i = 0; i < 4; i++) {
            poses.add(frame("iso_" + ISO_NAMES[i] + "_night", true, whole, ISO_YAWS[i], ISO_PITCH, null));
        }
        return poses;
    }

    /** The 8 corners of every column's prism, cut off above layer {@code keepY} (columns entirely above it drop out). */
    static double[][] corners(List<Column> columns, int keepY) {
        List<double[]> points = new ArrayList<>();
        for (Column column : columns) {
            if (column.minY() > keepY) {
                continue;
            }
            double top = Math.min(column.maxY(), keepY) + 1.0;
            for (int corner = 0; corner < 8; corner++) {
                points.add(new double[]{column.x() + (corner & 1), (corner & 2) == 0 ? column.minY() : top, column.z() + ((corner >> 2) & 1)});
            }
        }
        return points.toArray(new double[0][]);
    }

    /**
     * The pose looking along yaw/pitch at the middle of the points' picture, as close as they all still fit the view.
     */
    static Pose frame(String label, boolean night, double[][] points, float yaw, float pitch, Integer cutY) {
        double[] f = forward(yaw, pitch);
        double[] right = right(yaw);
        double[] up = cross(right, f);
        double tanV = Math.tan(Math.toRadians(VERTICAL_FOV / 2.0));
        double tanH = tanV * ASPECT;
        double[] target = centre(points);
        double distance = 1.0;
        for (int round = 0; round < CENTRING_ROUNDS; round++) {
            distance = fit(points, target, f, right, up, tanH * MARGIN, tanV * MARGIN);
            // Shift the aim point by the middle of the picture's extents (in the plane through the target).
            double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            for (double[] p : points) {
                double vx = p[0] - target[0], vy = p[1] - target[1], vz = p[2] - target[2];
                double depth = distance + vx * f[0] + vy * f[1] + vz * f[2];
                double sx = (vx * right[0] + vy * right[1] + vz * right[2]) / depth;
                double sy = (vx * up[0] + vy * up[1] + vz * up[2]) / depth;
                minX = Math.min(minX, sx);
                maxX = Math.max(maxX, sx);
                minY = Math.min(minY, sy);
                maxY = Math.max(maxY, sy);
            }
            double shiftX = (minX + maxX) / 2.0 * distance;
            double shiftY = (minY + maxY) / 2.0 * distance;
            for (int a = 0; a < 3; a++) {
                target[a] += right[a] * shiftX + up[a] * shiftY;
            }
        }
        distance = fit(points, target, f, right, up, tanH * MARGIN, tanV * MARGIN);
        double eyeX = target[0] - f[0] * distance;
        double eyeY = target[1] - f[1] * distance;
        double eyeZ = target[2] - f[2] * distance;
        return new Pose(label, night, eyeX, eyeY - EYE_HEIGHT, eyeZ, yaw, pitch, cutY);
    }

    /** The smallest distance back from the target along the view at which every point fits the given tangents. */
    private static double fit(double[][] points, double[] target, double[] f, double[] right, double[] up, double tanH, double tanV) {
        double distance = 1.0;
        for (double[] p : points) {
            double vx = p[0] - target[0], vy = p[1] - target[1], vz = p[2] - target[2];
            double along = vx * f[0] + vy * f[1] + vz * f[2];
            double sideways = Math.abs(vx * right[0] + vy * right[1] + vz * right[2]);
            double upwards = Math.abs(vx * up[0] + vy * up[1] + vz * up[2]);
            // The point is at depth distance + along in front of the camera: it fits when its offsets stay within the
            // view's tangents at that depth.
            distance = Math.max(distance, sideways / tanH - along);
            distance = Math.max(distance, upwards / tanV - along);
            distance = Math.max(distance, 1.0 - along);
        }
        return distance;
    }

    private static double[] centre(double[][] points) {
        double[] min = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        double[] max = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        for (double[] p : points) {
            for (int a = 0; a < 3; a++) {
                min[a] = Math.min(min[a], p[a]);
                max[a] = Math.max(max[a], p[a]);
            }
        }
        return new double[]{(min[0] + max[0]) / 2.0, (min[1] + max[1]) / 2.0, (min[2] + max[2]) / 2.0};
    }

    /** The farthest horizontal distance from the camera to a point (what the view distance must reach). */
    static double reach(Pose pose, double[][] points) {
        double far = 0.0;
        for (double[] p : points) {
            far = Math.max(far, Math.hypot(p[0] - pose.x(), p[2] - pose.z()));
        }
        return far;
    }

    static double[] forward(float yaw, float pitch) {
        double y = Math.toRadians(yaw);
        double p = Math.toRadians(pitch);
        return new double[]{-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p)};
    }

    /** Horizontal unit vector at right angles to the view's yaw (also defined when looking straight down). */
    static double[] right(float yaw) {
        double y = Math.toRadians(yaw);
        return new double[]{Math.cos(y), 0.0, Math.sin(y)};
    }

    static double[] cross(double[] a, double[] b) {
        return new double[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }
}
