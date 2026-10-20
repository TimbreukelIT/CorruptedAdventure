package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Floating debris round the big islands of ring 4 (sparring 2026-10-20, B2-3 a: "more filling in the sky"): small
 * pieces of 1–4 blocks a few blocks off the edge of every giant and every middle island of at least
 * {@link #MIN_DIAMETER}, most of them level with the torn edge or a little below it, the farther out the lower, as if
 * they crumbled off. Terrain like the islands (the surface rules dress them), but no island: no hole, no fall, no lift.
 * <p>
 * A piece keeps free space round it ({@link #KEEP_CLEAR}): from every island (its own too, so it floats free), from the
 * rock arches, from the columns of every fall, spring and lift, from the world trees and the sky lakes, and from the
 * ground. Pure, decided once per island and world ({@link IslandLandmarks#debris}).
 */
final class IslandDebris {
    /** Middle islands at least this wide (diameter) get debris; giants always. */
    static final double MIN_DIAMETER = 40.0;
    /** Pieces per giant: 6–12; per middle island: 2–5. */
    static final int GIANT_MIN = 6;
    static final int GIANT_SPREAD = 7;
    static final int MIDDLE_MIN = 2;
    static final int MIDDLE_SPREAD = 4;
    /** How far (blocks) past the island's edge a piece lies: {@code MIN_OUT + SPREAD_OUT · u²}. */
    static final double MIN_OUT = 3.0;
    static final double SPREAD_OUT = 13.0;
    /** The farthest a piece's blocks reach past its island's {@link IslandShape#reach}. */
    static final double MAX_OUT = MIN_OUT + SPREAD_OUT + 3.0;
    /** The free space (blocks) a piece keeps sideways, and above and below. */
    static final int KEEP_CLEAR = 2;
    static final int KEEP_CLEAR_Y = 3;
    /** A piece's lowest block stays this far above the ground under it. */
    static final double GROUND_CLEARANCE = 8.0;
    private static final long SALT = 7417;

    private IslandDebris() {
    }

    /**
     * One piece: its top block {@code y} over column (x, z), {@code size} 1–4 (a single block up to a clump of 13
     * columns, {@code size} high in the middle); its bounding box.
     */
    record Piece(int x, int y, int z, int size, int minX, int minZ, int maxX, int maxZ) {
        static Piece of(int x, int y, int z, int size) {
            int r = size / 2;
            return new Piece(x, y, z, size, x - r, z - r, x + r, z + r);
        }

        /** The footprint's radius (column centres within it belong to the piece). */
        double radius() {
            return 0.5 * size;
        }

        /** The piece's blocks in column (px, pz): {lowest, highest}, or null. */
        @Nullable
        int[] interval(int px, int pz) {
            if (px < minX || px > maxX || pz < minZ || pz > maxZ) {
                return null;
            }
            double d = Math.hypot(px - x, pz - z);
            double r = radius();
            if (d > r) {
                return null;
            }
            // A clump that hangs a little: deepest in the middle.
            return new int[]{y - (int) Math.round((size - 1) * (1.0 - d / (r + 0.5))), y};
        }
    }

    /** The debris of an island (none for clods, small middle islands and the world trees' islands). */
    static List<Piece> of(IslandField field, IslandShape island) {
        double diameter = 2.0 * island.a() * Math.sqrt(island.stretch());
        int wanted;
        long h = SeededHash.hash(field.hashNoise(), Mth.floor(island.x()), Mth.floor(island.z()), SALT);
        if (island.kind() == IslandShape.GIANT) {
            wanted = GIANT_MIN + (int) (SeededHash.unit(h, 0) * GIANT_SPREAD);
        } else if (island.kind() == IslandShape.MIDDLE && diameter >= MIN_DIAMETER) {
            wanted = MIDDLE_MIN + (int) (SeededHash.unit(h, 0) * MIDDLE_SPREAD);
        } else {
            return List.of();
        }
        List<Piece> pieces = new ArrayList<>();
        for (int k = 0; k < wanted * 3 && pieces.size() < wanted; k++) {
            Piece piece = candidate(island, SeededHash.mix(h + (k + 1) * 0x9E3779B97F4A7C15L));
            if (piece != null && apart(piece, pieces) && clear(field, island, piece)) {
                pieces.add(piece);
            }
        }
        return List.copyOf(pieces);
    }

    /** A piece off the island's edge along a seeded angle, or null if the island has no edge there. */
    @Nullable
    private static Piece candidate(IslandShape island, long h) {
        double t = SeededHash.unit(h, 1) * Math.PI * 2.0;
        double cos = Math.cos(t);
        double sin = Math.sin(t);
        double edge = -1.0;
        for (double r = 0.0; r <= island.reach(); r += 0.5) {
            if (island.distance(island.x() + r * cos, island.z() + r * sin) < 1.0) {
                edge = r;
            }
        }
        if (edge < 0.0) {
            return null;
        }
        double u = SeededHash.unit(h, 2);
        double out = MIN_OUT + SPREAD_OUT * u * u;
        double roll = SeededHash.unit(h, 3);
        int size = roll < 0.3 ? 1 : roll < 0.65 ? 2 : roll < 0.9 ? 3 : 4;
        // Level with the torn edge or below it; the farther out, the lower it may have sunk.
        double drop = (SeededHash.unit(h, 4) * 1.3 - 0.2) * (island.edgeDepth() + out);
        int y = Mth.floor(island.top() - 1.0 - drop);
        return Piece.of(Mth.floor(island.x() + (edge + out) * cos), y, Mth.floor(island.z() + (edge + out) * sin), size);
    }

    /** Whether the piece keeps a block of air from the pieces placed before. */
    private static boolean apart(Piece piece, List<Piece> pieces) {
        for (Piece other : pieces) {
            if (piece.minX() <= other.maxX() + 1 && piece.maxX() >= other.minX() - 1 && piece.minZ() <= other.maxZ() + 1
                    && piece.maxZ() >= other.minZ() - 1 && piece.y() - piece.size() <= other.y() + 1
                    && other.y() - other.size() <= piece.y() + 1) {
                return false;
            }
        }
        return true;
    }

    /** Whether the piece keeps its free space (class comment). */
    private static boolean clear(IslandField field, IslandShape island, Piece piece) {
        int lo = piece.y() - piece.size() + 1;
        if (piece.y() > IslandPlacement.MAX_TOP
                || lo < Math.max(field.baseHeight(piece.x() + 0.5, piece.z() + 0.5), IslandShape.SEA_LEVEL) + GROUND_CLEARANCE) {
            return false;
        }
        int minX = piece.minX() - KEEP_CLEAR;
        int minZ = piece.minZ() - KEEP_CLEAR;
        int maxX = piece.maxX() + KEEP_CLEAR;
        int maxZ = piece.maxZ() + KEEP_CLEAR;
        int bottom = lo - KEEP_CLEAR_Y;
        int top = piece.y() + KEEP_CLEAR_Y;
        for (WorldTreeShape tree : field.landmarks().trees()) {
            if (tree.reaches(minX, minZ, maxX, maxZ)) {
                return false;
            }
        }
        // Islands, their falls, springs, lifts and sky lakes (an island's details lie within its reach and a lift's).
        for (IslandShape other : field.shapesWithin(minX, minZ, maxX, maxZ)) {
            double near = other.reach() + IslandField.LIFT_REACH + KEEP_CLEAR + piece.size() + 2.0;
            if (Math.abs(other.x() - piece.x()) > near || Math.abs(other.z() - piece.z()) > near) {
                continue;
            }
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    int[] solid = other.interval(x + 0.5, z + 0.5);
                    if (solid != null && solid[0] <= top && solid[1] >= bottom) {
                        return false;
                    }
                }
            }
            IslandField.Details details = field.details(other);
            for (IslandField.Fall fall : details.falls()) {
                if (inBox(fall.fallX(), fall.fallZ(), minX, minZ, maxX, maxZ) || inBox(fall.springX(), fall.springZ(), minX, minZ, maxX, maxZ)) {
                    return false;
                }
            }
            if (details.lift() != null && inBox(details.lift().x(), details.lift().z(), minX, minZ, maxX, maxZ)) {
                return false;
            }
            SkyLakes.Lake lake = field.landmarks().lake(other);
            if (lake != null && lake.maxX() + 4 >= minX && lake.minX() - 4 <= maxX && lake.maxZ() + 4 >= minZ && lake.minZ() - 4 <= maxZ) {
                return false;
            }
        }
        // The rock arches: an arch reaches from its first island's edge past the second's.
        int margin = Mth.ceil(IslandField.MAX_REACH + IslandArches.MAX_GAP);
        for (IslandShape other : field.shapesWithin(minX - margin, minZ - margin, maxX + margin, maxZ + margin)) {
            for (IslandArches.Pair pair : field.landmarks().pairs(other)) {
                IslandArches.Arch arch = pair.arch();
                if (arch == null || arch.maxX() < minX || arch.minX() > maxX || arch.maxZ() < minZ || arch.minZ() > maxZ) {
                    continue;
                }
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        int[] rock = arch.interval(x, z);
                        if (rock != null && rock[0] <= top && rock[1] >= bottom) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private static boolean inBox(int x, int z, int minX, int minZ, int maxX, int maxZ) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }
}
