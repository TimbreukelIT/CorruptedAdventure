package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import java.util.ArrayList;
import java.util.List;

/**
 * The fantasy landforms of ring 3 (change 39d, decision 6): rare landmarks (a floating volcano spewing lava, an abyss to
 * a lava sea with natural bridges) and small, regular ones (obsidian needles, natural arches over lava rivers and rift
 * valleys). Each lives in its own grid cell (kept inside it), so a column only looks at its own cell. Pure; the decision
 * per cell is remembered per world.
 */
final class Landmarks {
    static final int FLOATING_CELL = 2000;
    static final int ABYSS_CELL = 1600;
    static final int NEEDLE_CELL = 192;
    static final int RIFT_ARCH_CELL = 384;
    private static final int FLOATING = 11;
    private static final int ABYSS = 12;
    private static final int NEEDLE = 13;
    private static final int RIFT_ARCH = 14;

    /** Kinds of thin solids, for the surface ({@code solid_kind}). */
    static final int KIND_BONE = 1;
    static final int KIND_OBSIDIAN = 2;
    static final int KIND_BASALT = 3;

    private Landmarks() {
    }

    // ---------------------------------------------------------------- arches (shared by rivers and rifts)

    /**
     * A natural arch: in the vertical plane through (x, z) along (cos, sin), half span {@code span}, crown {@code height}
     * above {@code base} (absolute Y), {@code thickness} thick in its plane and {@code width} across it.
     */
    record Arch(double x, double z, double cos, double sin, double span, double height, double base, double thickness,
                double width) {
        /** Writes the solid interval (absolute Y) of this arch in a column into {@code out}; false if none. */
        boolean interval(double px, double pz, double[] out) {
            double dx = px - x;
            double dz = pz - z;
            double across = dx * cos + dz * sin;
            double normal = -dx * sin + dz * cos;
            if (Math.abs(normal) > width * 0.5 || Math.abs(across) > span) {
                return false;
            }
            double c = across / span;
            double top = height * Math.sqrt(Math.max(0.0, 1.0 - c * c));
            double innerSpan = span - thickness;
            double ci = across / innerSpan;
            double bottom = Math.abs(ci) >= 1.0 ? -6.0 : (height - thickness) * Math.sqrt(1.0 - ci * ci);
            out[0] = base + bottom;
            out[1] = base + top;
            return top > bottom;
        }

        double reach() {
            return span + width;
        }
    }

    // ---------------------------------------------------------------- needles

    /** An obsidian needle: centre, base radius, height. */
    record Needle(double x, double z, double radius, double height) {
        /** Height above the ground at (px, pz), or a negative value outside. */
        double at(double px, double pz) {
            return at(px, pz, height);
        }

        /** The same for a needle shortened to {@code tall} (the same point and taper, scaled down). */
        double at(double px, double pz, double tall) {
            double d = Math.sqrt((px - x) * (px - x) + (pz - z) * (pz - z)) / radius;
            if (d >= 1.0 || tall <= 0.0) {
                return -1.0;
            }
            return tall * (1.0 - Math.pow(d, 1.0 / 0.7));
        }
    }

    static Needle needle(VolcanicField field, int i, int j) {
        long h = CellShapes.cell(field.salt(), i, j, NEEDLE);
        if (CellShapes.unit(h, 0) >= 0.3) {
            return null;
        }
        double radius = 3.5 + 2.5 * CellShapes.unit(h, 1);
        double x = (i + 0.2 + 0.6 * CellShapes.unit(h, 2)) * NEEDLE_CELL;
        double z = (j + 0.2 + 0.6 * CellShapes.unit(h, 3)) * NEEDLE_CELL;
        VolcanicField.Regions r = field.regions(x, z);
        if (r.fade() < 0.9 || r.seabed() > 0.1 || r.titan() > 0.1 || !field.clearOfVolcanoes(x, z, radius + 30.0)
                || field.nearAbyss(x, z, radius)
                || field.riverWithin(x, z, radius + 20.0)) {
            return null;
        }
        return new Needle(x, z, radius, 60.0 + 60.0 * CellShapes.unit(h, 4));
    }

    // ---------------------------------------------------------------- arches over rifts

    static Arch riftArch(VolcanicField field, int i, int j) {
        long h = CellShapes.cell(field.salt(), i, j, RIFT_ARCH);
        if (CellShapes.unit(h, 0) >= 0.5) {
            return null;
        }
        // Look for a rift floor in the middle of the cell (a few jittered candidates, the first one wins).
        Ring3Relief.Detail detail = new Ring3Relief.Detail();
        double x = 0.0;
        double z = 0.0;
        double depth = 0.0;
        for (int k = 0; k < 16 && depth < 15.0; k++) {
            x = (i + 0.3 + 0.1 * (k % 4) + 0.1 * CellShapes.unit(h, 10 + k)) * RIFT_ARCH_CELL;
            z = (j + 0.3 + 0.1 * (k / 4) + 0.1 * CellShapes.unit(h, 30 + k)) * RIFT_ARCH_CELL;
            depth = field.regions(x, z).riftOpen() < 0.9 ? 0.0 : field.riftCut(x, z, detail);
        }
        if (depth < 15.0) {
            return null;
        }
        // Across the rift: the direction (of eight) with the shortest way from rim to rim, marching both ways.
        double nx = 0.0;
        double nz = 0.0;
        double reachA = -1.0;
        double reachB = -1.0;
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 8.0;
            double cos = Math.cos(a);
            double sin = Math.sin(a);
            double ra = rim(field, x, z, cos, sin, detail);
            double rb = ra < 0.0 ? -1.0 : rim(field, x, z, -cos, -sin, detail);
            if (rb >= 0.0 && (reachA < 0.0 || ra + rb < reachA + reachB)) {
                nx = cos;
                nz = sin;
                reachA = ra;
                reachB = rb;
            }
        }
        if (reachA < 0.0) {
            return null;
        }
        // Centre the arch between the rims.
        double shift = (reachA - reachB) * 0.5;
        double cx = x + nx * shift;
        double cz = z + nz * shift;
        double span = (reachA + reachB) * 0.5 + 6.0;
        if (span > 0.4 * RIFT_ARCH_CELL) {
            return null;
        }
        double floor = field.traceGround(cx, cz);
        if (!field.clearOfVolcanoes(cx, cz, 0.0)) {
            return null;
        }
        return new Arch(cx, cz, nx, nz, span, depth + 6.0 + 6.0 * CellShapes.unit(h, 3), floor, 5.0, 5.0 + 3.0 * CellShapes.unit(h, 4));
    }

    private static double rim(VolcanicField field, double x, double z, double nx, double nz, Ring3Relief.Detail detail) {
        for (double s = 4.0; s <= 150.0; s += 4.0) {
            if (field.riftCut(x + nx * s, z + nz * s, detail) < 2.0) {
                return s;
            }
        }
        return -1.0;
    }

    // ---------------------------------------------------------------- arches over rivers

    /** Arches across a traced river (a few per river, never at its source or end pool). */
    static List<Arch> riverArches(LavaRivers.River river, long riverHash) {
        List<Arch> arches = new ArrayList<>();
        int n = river.points();
        for (int k = 12; k + 8 < n; k += 6) {
            double u = CellShapes.unit(riverHash, 100 + k);
            if (u >= 0.12) {
                continue;
            }
            double dx = river.x()[k + 1] - river.x()[k - 1];
            double dz = river.z()[k + 1] - river.z()[k - 1];
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len == 0.0) {
                continue;
            }
            // The arch spans the river: its plane runs across the flow.
            double cos = -dz / len;
            double sin = dx / len;
            double span = river.half()[k] + 7.0 + 5.0 * CellShapes.unit(riverHash, 200 + k);
            double height = 9.0 + 10.0 * CellShapes.unit(riverHash, 300 + k);
            arches.add(new Arch(river.x()[k], river.z()[k], cos, sin, span, height, river.level()[k] + 2.0, 4.0,
                    4.0 + 2.0 * CellShapes.unit(riverHash, 400 + k)));
            k += 12;
        }
        return List.copyOf(arches);
    }

    // ---------------------------------------------------------------- floating volcano

    /**
     * A floating volcano: an island of radius {@code radius} with its plateau at {@code base}, a cone rising
     * {@code cone} to a crater with a lava lake, an underside hanging {@code depth} below; lava springs on the rim.
     */
    record FloatingVolcano(double x, double z, double radius, double base, double cone, double depth, double crater,
                           double lakeTop, double craterFloor, int[] springX, int[] springZ, int[] springY) {
        /** Top of the island at normalised distance {@code d} (0..1). */
        double topAt(double d) {
            double rimD = crater / radius;
            if (d <= rimD) {
                double k = d / rimD;
                return craterFloor + (base + cone - craterFloor) * k * k;
            }
            double u = (d - rimD) / (1.0 - rimD);
            return base + cone * Math.pow(1.0 - u, 1.6) - 3.0 * Math.pow(d, 8.0);
        }

        double bottomAt(double d) {
            return base - 2.0 - depth * Math.pow(Math.max(0.0, 1.0 - d), 1.2);
        }
    }

    static FloatingVolcano floating(VolcanicField field, int i, int j) {
        long h = CellShapes.cell(field.salt(), i, j, FLOATING);
        if (CellShapes.unit(h, 0) >= 0.45) {
            return null;
        }
        double radius = 40.0 + 15.0 * CellShapes.unit(h, 1);
        double margin = radius + 40.0;
        double x = i * FLOATING_CELL + margin + CellShapes.unit(h, 2) * (FLOATING_CELL - 2.0 * margin);
        double z = j * FLOATING_CELL + margin + CellShapes.unit(h, 3) * (FLOATING_CELL - 2.0 * margin);
        VolcanicField.Regions r = field.regions(x, z);
        if (r.core() < 0.99 || r.seabed() > 0.1 || !field.clearOfVolcanoes(x, z, radius + 60.0)) {
            return null;
        }
        double base = 200.0 + 12.0 * CellShapes.unit(h, 4);
        double cone = 38.0 + 12.0 * CellShapes.unit(h, 5);
        double depth = 55.0 + 15.0 * CellShapes.unit(h, 6);
        // Clear of the ground under it, also where a hill rises.
        for (int k = 0; k <= 8; k++) {
            double a = k * Math.PI / 4.0;
            double s = k == 8 ? 0.0 : radius * 0.6;
            double g = field.traceGround(x + Math.cos(a) * s, z + Math.sin(a) * s);
            if (base - 2.0 - depth * (k == 8 ? 1.0 : 0.4) < g + 15.0) {
                return null;
            }
        }
        double crater = 10.0 + 4.0 * CellShapes.unit(h, 7);
        double rimTop = base + cone;
        double craterFloor = rimTop - 12.0;
        double lakeTop = craterFloor + 6.0;
        int springs = 3 + (int) (2.0 * CellShapes.unit(h, 8));
        int[] sx = new int[springs];
        int[] sz = new int[springs];
        int[] sy = new int[springs];
        double angle0 = CellShapes.unit(h, 9) * Math.PI * 2.0;
        FloatingVolcano shape = new FloatingVolcano(x, z, radius, base, cone, depth, crater, lakeTop, craterFloor, sx, sz, sy);
        for (int k = 0; k < springs; k++) {
            // Springs just outside the crater rim: the lava runs down the cone and off the island's edge.
            double a = angle0 + k * Math.PI * 2.0 / springs;
            double s = crater + 3.0;
            sx[k] = (int) Math.floor(x + Math.cos(a) * s);
            sz[k] = (int) Math.floor(z + Math.sin(a) * s);
            double d = Math.sqrt(Math.pow(sx[k] + 0.5 - x, 2) + Math.pow(sz[k] + 0.5 - z, 2)) / radius
                    + 0.05 * field.warpAt(sx[k] + 0.5, sz[k] + 0.5);
            sy[k] = (int) Math.floor(shape.topAt(d));
        }
        return shape;
    }

    // ---------------------------------------------------------------- abyss

    /**
     * An abyss: a chasm along (cos, sin) through (x, z), {@code length} long, {@code width} half wide, down to a lava
     * sea at {@link #ABYSS_LAVA}; natural bridges cross it at {@code bridges} (along-positions) at {@code rim} height.
     */
    record Abyss(double x, double z, double cos, double sin, double length, double width, double rim, double[] bridges,
                 double[] bridgeWidth, double[] bridgeY) {
        /** Normalised distance across (0 on the axis, 1 at the rim) at a position, or a value ≥ 1 outside. */
        double across(double px, double pz) {
            double dx = px - x;
            double dz = pz - z;
            double along = (dx * cos + dz * sin) / (length * 0.5);
            if (Math.abs(along) >= 1.0) {
                return 2.0;
            }
            double normal = -dx * sin + dz * cos;
            double w = width * Math.sqrt(1.0 - along * along) * (0.85 + 0.15 * Math.cos(along * 7.0));
            return Math.abs(normal) / Math.max(1.0, w);
        }

        double along(double px, double pz) {
            return (px - x) * cos + (pz - z) * sin;
        }

        double normal(double px, double pz) {
            return -(px - x) * sin + (pz - z) * cos;
        }

        double reach() {
            return length * 0.5 + width + 16.0;
        }
    }

    static final int ABYSS_FLOOR = 2;
    static final int ABYSS_LAVA = 10;

    static Abyss abyss(VolcanicField field, int i, int j) {
        long h = CellShapes.cell(field.salt(), i, j, ABYSS);
        if (CellShapes.unit(h, 0) >= 0.4) {
            return null;
        }
        double length = 300.0 + 200.0 * CellShapes.unit(h, 1);
        double width = 18.0 + 12.0 * CellShapes.unit(h, 2);
        double margin = length * 0.5 + width + 30.0;
        double x = i * ABYSS_CELL + margin + CellShapes.unit(h, 3) * (ABYSS_CELL - 2.0 * margin);
        double z = j * ABYSS_CELL + margin + CellShapes.unit(h, 4) * (ABYSS_CELL - 2.0 * margin);
        double angle = CellShapes.unit(h, 5) * Math.PI;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        for (int k = -2; k <= 2; k++) {
            double px = x + cos * k * length * 0.25;
            double pz = z + sin * k * length * 0.25;
            VolcanicField.Regions r = field.regions(px, pz);
            if (r.core() < 0.99 || r.seabed() > 0.05 || r.titan() > 0.3 || !field.clearOfVolcanoes(px, pz, length * 0.25 + width + 40.0)) {
                return null;
            }
        }
        double rim = field.plainAt(x, z);
        int count = 1 + (int) (2.99 * CellShapes.unit(h, 6));
        double[] bridges = new double[count];
        double[] bridgeWidth = new double[count];
        double[] bridgeY = new double[count];
        for (int k = 0; k < count; k++) {
            bridges[k] = (-0.35 + 0.7 * (k + 0.5) / count + 0.08 * (CellShapes.unit(h, 10 + k) - 0.5)) * length;
            bridgeWidth[k] = 3.0 + 2.0 * CellShapes.unit(h, 20 + k);
            // The deck sits just below the lower of its two rims (the smooth plain there; the abyss itself is not asked).
            double bx = x + cos * bridges[k];
            double bz = z + sin * bridges[k];
            double reach = width + 8.0;
            bridgeY[k] = Math.min(field.plainAt(bx - sin * reach, bz + cos * reach), field.plainAt(bx + sin * reach, bz - cos * reach));
        }
        return new Abyss(x, z, cos, sin, length, width, rim, bridges, bridgeWidth, bridgeY);
    }
}
