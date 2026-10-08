package nl.timtoys.corruptedadventure.worldgen.densityfunction;

/**
 * Fields of small cinder cones (change 39d): cones 10–40 high with a mini crater, on a grid of {@link #CELL}, clustered
 * by a regional noise. Never on or next to a volcano, in a rift or near a lava river, never in the seabed or the titan
 * fields. Pure; the decision per cell is remembered per world ({@code Decisions}).
 */
final class CinderCones {
    static final int CELL = 112;
    private static final double SLOPE = 0.62;

    private CinderCones() {
    }

    /** One cinder cone: centre, height, foot radius, crater radius and crater depth. */
    record Cone(double x, double z, double height, double radius, double crater, double craterDepth) {
        /** Height above the ground at distance {@code d}, 0 beyond the foot. */
        double at(double d) {
            if (d >= radius) {
                return 0.0;
            }
            if (d >= crater) {
                return height * Math.pow((radius - d) / (radius - crater), 1.15);
            }
            double k = d / crater;
            return height - craterDepth * (1.0 - k * k);
        }

        double reach() {
            return radius;
        }
    }

    /** The cone of cell (i, j), or null. */
    static Cone place(VolcanicField field, int i, int j) {
        long h = CellShapes.cell(field.salt(), i, j, CellShapes.CINDER);
        double cx = (i + 0.15 + 0.7 * CellShapes.unit(h, 1)) * CELL;
        double cz = (j + 0.15 + 0.7 * CellShapes.unit(h, 2)) * CELL;
        VolcanicField.Regions r = field.regions(cx, cz);
        if (r.core() < 0.99) {
            return null;
        }
        double regional = 0.9 * r.peaks() + 0.7 * r.flows() + 0.45 * r.ember() + 0.35 * r.ashen() + 0.15 * r.sulfur();
        regional *= (1.0 - r.seabed()) * (1.0 - r.titan());
        double chance = regional * field.cinderFieldAt(cx, cz);
        if (CellShapes.unit(h, 0) >= chance) {
            return null;
        }
        double height = 10.0 + 30.0 * Math.pow(CellShapes.unit(h, 3), 1.5);
        double crater = 3.0 + 0.15 * height;
        double radius = crater + height / SLOPE;
        if (!field.cinderMayStand(cx, cz, radius) || field.nearAbyss(cx, cz, radius)) {
            return null;
        }
        return new Cone(cx, cz, height, radius, crater, Math.min(10.0, 0.35 * height));
    }
}
