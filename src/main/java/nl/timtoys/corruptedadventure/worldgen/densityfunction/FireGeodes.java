package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.util.Mth;
import nl.timtoys.corruptedadventure.worldgen.feature.FireGeodeShape;

import javax.annotation.Nullable;

/**
 * Where the fire crystal geodes of ring 3 lie (package 42; the shape is {@link FireGeodeShape}). A cell takes the first
 * of its attempts that lies deep in the ring 3 core (well away from the ring's borders), clear of the abyss and its lava
 * sea and of every magma chamber, with ground well above it. Lava tubes may cut through one (G2-WB2 = A); vanilla caves
 * and carvers stay out (a guard band around it). Pure.
 */
final class FireGeodes {
    private static final int HASH_SALT = 4203;

    private FireGeodes() {
    }

    /** The geode of geode cell (i, j), or null. */
    @Nullable
    static FireGeodeShape.Geode place(VolcanicField f, int i, int j) {
        long salt = f.salt();
        FireGeodeShape.CellHash hash = (x, z, s) -> CellShapes.cell(salt, x, z, HASH_SALT + s);
        for (int attempt = 0; attempt < FireGeodeShape.ATTEMPTS; attempt++) {
            FireGeodeShape.Geode geode = FireGeodeShape.candidate(hash, i, j, attempt);
            if (geode == null) {
                return null;
            }
            if (fits(f, geode)) {
                return geode;
            }
        }
        return null;
    }

    private static boolean fits(VolcanicField f, FireGeodeShape.Geode g) {
        int reach = g.reach();
        if (f.regions(g.x(), g.z()).core() < 0.99 || f.nearAbyss(g.x(), g.z(), reach + 12.0)) {
            return false;
        }
        int ci = Math.floorDiv(g.x(), LavaTubes.CELL);
        int cj = Math.floorDiv(g.z(), LavaTubes.CELL);
        for (int di = -1; di <= 1; di++) {
            for (int dj = -1; dj <= 1; dj++) {
                LavaTubes.Chamber chamber = f.tubeSystem(ci + di, cj + dj).chamber();
                if (chamber != null && Math.hypot(g.x() - chamber.x(), g.z() - chamber.z()) < chamber.reach() + reach + 4.0) {
                    return false;
                }
            }
        }
        // Ground well above it, at its centre and around its rim (a rift or the seabed may lie lower).
        for (int k = 0; k <= 8; k++) {
            double r = k == 8 ? 0.0 : reach + 4.0;
            double x = g.x() + r * Math.cos(k * Math.PI / 4.0);
            double z = g.z() + r * Math.sin(k * Math.PI / 4.0);
            if (f.traceGround(x, z) < g.y() + reach + 12.0) {
                return false;
            }
        }
        return true;
    }

    /** The cavity of {@code g} in column (x, z) as an inclusive block range, or null if the column misses it. */
    @Nullable
    static int[] cavity(FireGeodeShape.Geode g, int x, int z) {
        double dx = (x - g.x()) / g.stretchX();
        double dz = (z - g.z()) / g.stretchZ();
        double rest = g.radius() * g.radius() - dx * dx - dz * dz;
        if (rest < 0.0) {
            return null;
        }
        // Exactly the blocks of FireGeodeShape.Geode#inCavity in this column: the closed form, its ends checked against it
        // (rounding at the very rim).
        double span = g.stretchY() * Math.sqrt(rest);
        int bottom = Mth.ceil(g.y() - span) - 1;
        int top = Mth.floor(g.y() + span) + 1;
        while (bottom <= top && !g.inCavity(x, bottom, z)) {
            bottom++;
        }
        while (top >= bottom && !g.inCavity(x, top, z)) {
            top--;
        }
        return bottom > top ? null : new int[]{bottom, top};
    }
}
