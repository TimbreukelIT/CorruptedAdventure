package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import java.util.ArrayList;
import java.util.List;

/**
 * Giant stone bones in {@code titan_fields} (change 39d): single ribs standing as arches out of the ground (20–60 high)
 * and, rarely, the whole ribcage of a fallen titan with its spine. Thin shapes: solid intervals per column, outside the
 * terrain interpolation. Pure; the decision per cell is remembered per world.
 */
final class TitanBones {
    static final int RIB_CELL = 112;
    static final int CAGE_CELL = 448;
    /** Thickness of a rib (in its plane and across it). */
    private static final double THICKNESS = 6.0;

    private TitanBones() {
    }

    /**
     * One rib: an arch in the vertical plane through (x, z) along direction {@code angle}, half span {@code span}, height
     * {@code height}; {@code from}..{@code to} (−1..1 of the span) is the part that still stands (broken ribs).
     */
    record Rib(double x, double z, double cos, double sin, double span, double height, double from, double to) {
        /** Writes the solid interval of this rib in a column (relative to the ground) into {@code out}; false if none. */
        boolean interval(double px, double pz, double[] out) {
            double dx = px - x;
            double dz = pz - z;
            double across = dx * cos + dz * sin;
            double normal = -dx * sin + dz * cos;
            if (Math.abs(normal) > THICKNESS * 0.5 || Math.abs(across) > span) {
                return false;
            }
            double c = across / span;
            if (c < from || c > to) {
                return false;
            }
            double top = height * Math.sqrt(Math.max(0.0, 1.0 - c * c));
            double innerSpan = span - THICKNESS;
            double innerHeight = height - THICKNESS;
            double ci = across / innerSpan;
            double bottom = Math.abs(ci) >= 1.0 ? -3.0 : innerHeight * Math.sqrt(1.0 - ci * ci);
            out[0] = bottom;
            out[1] = top;
            return top > bottom;
        }

        double reach() {
            return span + THICKNESS;
        }
    }

    /** A ribcage: its ribs and the vertebrae of its spine (boxes along the spine, relative to the ground). */
    record Cage(List<Rib> ribs, double[] vx, double[] vz, double[] vHeight, double reach, double x, double z) {
    }

    /** The single rib of cell (i, j), or null. */
    static Rib rib(VolcanicField field, int i, int j) {
        long h = CellShapes.cell(field.salt(), i, j, CellShapes.RIB);
        if (CellShapes.unit(h, 0) >= 0.5) {
            return null;
        }
        double span = 18.0 + 17.0 * CellShapes.unit(h, 1);
        double margin = span + THICKNESS + 1.0;
        double x = i * RIB_CELL + margin + CellShapes.unit(h, 2) * (RIB_CELL - 2.0 * margin);
        double z = j * RIB_CELL + margin + CellShapes.unit(h, 3) * (RIB_CELL - 2.0 * margin);
        if (field.regions(x, z).titanS() < 0.6 || !field.clearOfVolcanoes(x, z, span + THICKNESS)) {
            return null;
        }
        double angle = CellShapes.unit(h, 4) * Math.PI;
        double height = 20.0 + 40.0 * CellShapes.unit(h, 5);
        // A third of the ribs is broken off on one side.
        double broken = CellShapes.unit(h, 6);
        double from = broken < 0.17 ? -1.0 : broken < 0.33 ? -0.2 : -1.0;
        double to = broken < 0.17 ? 0.25 : 1.0;
        return new Rib(x, z, Math.cos(angle), Math.sin(angle), span, height, from, to);
    }

    /** The ribcage of cell (i, j), or null (rare: deep inside the titan fields only). */
    static Cage cage(VolcanicField field, int i, int j) {
        long h = CellShapes.cell(field.salt(), i, j, CellShapes.TITAN);
        if (CellShapes.unit(h, 0) >= 0.25) {
            return null;
        }
        double length = 120.0 + 60.0 * CellShapes.unit(h, 1);
        double ribSpan = 30.0 + 15.0 * CellShapes.unit(h, 2);
        double reach = length * 0.5 + ribSpan + THICKNESS;
        double x = i * CAGE_CELL + reach + CellShapes.unit(h, 3) * (CAGE_CELL - 2.0 * reach);
        double z = j * CAGE_CELL + reach + CellShapes.unit(h, 4) * (CAGE_CELL - 2.0 * reach);
        if (field.regions(x, z).titanS() < 0.85 || !field.clearOfVolcanoes(x, z, reach)) {
            return null;
        }
        double angle = CellShapes.unit(h, 5) * Math.PI;
        double sx = Math.cos(angle);
        double sz = Math.sin(angle);
        // Ribs cross the spine: their plane is perpendicular to it, so they run along (−sz, sx).
        List<Rib> ribs = new ArrayList<>();
        int count = (int) (length * 0.6 / 14.0);
        double maxHeight = 42.0 + 18.0 * CellShapes.unit(h, 6);
        for (int k = 0; k < count; k++) {
            double t = (k + 0.5) / count;
            double s = (t - 0.5) * length * 0.6 + length * 0.08;
            double shape = Math.sin(Math.PI * (0.15 + 0.7 * t));
            double u = CellShapes.unit(h, 10 + k);
            if (u < 0.12) {
                continue; // a missing rib
            }
            double from = u < 0.3 ? -0.3 : -1.0;
            double to = u > 0.9 ? 0.35 : 1.0;
            ribs.add(new Rib(x + sx * s, z + sz * s, -sz, sx, ribSpan * (0.75 + 0.25 * shape), maxHeight * shape, from, to));
        }
        int vertebrae = (int) (length / 9.0);
        double[] vx = new double[vertebrae];
        double[] vz = new double[vertebrae];
        double[] vHeight = new double[vertebrae];
        for (int k = 0; k < vertebrae; k++) {
            double s = (k + 0.5) / vertebrae * length - length * 0.5;
            vx[k] = x + sx * s;
            vz[k] = z + sz * s;
            vHeight[k] = 5.0 + 3.0 * CellShapes.unit(h, 40 + k) - 3.0 * Math.abs(s) / length;
        }
        return new Cage(List.copyOf(ribs), vx, vz, vHeight, reach, x, z);
    }

    /** The vertebra interval of a cage in a column (relative to the ground), or false. */
    static boolean vertebra(Cage cage, double px, double pz, double[] out) {
        for (int k = 0; k < cage.vx().length; k++) {
            double dx = px - cage.vx()[k];
            double dz = pz - cage.vz()[k];
            if (dx * dx + dz * dz <= 3.2 * 3.2) {
                out[0] = -2.0;
                out[1] = cage.vHeight()[k];
                return true;
            }
        }
        return false;
    }
}
