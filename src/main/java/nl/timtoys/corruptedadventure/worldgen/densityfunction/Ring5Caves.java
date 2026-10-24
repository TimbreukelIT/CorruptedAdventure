package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The caves of ring 5 of its own (package 93, V93-2 … V93-5 and V93-8 = A), exact per block like the rest of the field;
 * vanilla's damped noise caves (V93-1) come on top in the JSON and are not copied.
 * <ul>
 *     <li><b>Impossible corridors</b> ({@link Net}): loose nets, at most one per cell of {@link #NET_CELL} with chance
 *     {@link #NET_CHANCE}, each on one fixed level ({@link #LEVEL_TOP} down in steps of {@link #LEVEL_STEP}): a grid of
 *     straight, axis-aligned corridors of exactly 3 × 3 (every {@link #WIDE_SPACING}) or 2 high × 1 wide (every
 *     {@link #NARROW_SPACING}); every grid edge with chance {@link #EDGE_CHANCE}, so dead ends and loops come by
 *     themselves. Sometimes a second level {@link #LEVEL_STEP} lower, joined by a straight shaft; per net one straight
 *     3 × 3 shaft up to the surface (the entrance; where a corridor runs into a slope it opens there as well).</li>
 *     <li><b>Copied corridors</b> (V93-5): about one net in {@link #COPY_CHANCE}⁻¹ is one straight corridor with a few
 *     dead-end stubs, laid down 2–3 times side by side, {@link #MIN_COPY_GAP}–{@link #MAX_COPY_GAP} apart, block for block
 *     the same (each copy with its own shaft).</li>
 *     <li><b>Void caves</b> ({@link Chamber}, V93-4): ragged chambers Ø 12–30, 6–14 high, deep in the inner band, about one
 *     per {@link #CHAMBER_CELL} × {@link #CHAMBER_CELL}, with 1–4 holes Ø 2–6 in the floor down into the void (below the
 *     world's bottom, no bedrock). About one in four mirrors itself about an axis through its middle. In the outer band
 *     (V93-8) only rare chambers in thick floes near the band line ({@link #OUTER_CELL}), their holes through the floe's
 *     underside.</li>
 * </ul>
 * Only the shapes, in world coordinates; the field ({@link Ring5Field}) keeps those that lie well inside their band and
 * turns them into open intervals per column. Every element lies inside its own cell, so a column asks its own cell only.
 * Pure: a function of the per-world salt and the cell.
 */
final class Ring5Caves {
    // ---------------------------------------------------------------- impossible corridors

    static final int NET_CELL = 128;
    static final double NET_CHANCE = 0.4;
    /** The floor of the highest level; the levels lie every {@link #LEVEL_STEP} below it, {@link #LEVELS} of them. */
    static final int LEVEL_TOP = 48;
    static final int LEVEL_STEP = 16;
    static final int LEVELS = 6;
    static final int WIDE_SPACING = 16;
    static final int NARROW_SPACING = 8;
    /** A wide corridor is 3 × 3 (centre ± 1), a narrow one 1 wide and 2 high. */
    static final int WIDE_HALF = 1;
    static final int WIDE_HEIGHT = 3;
    static final int NARROW_HALF = 0;
    static final int NARROW_HEIGHT = 2;
    /** Grid nodes per side: wide 4–6, narrow 6–10. */
    static final int WIDE_MIN_NODES = 4;
    static final int WIDE_MAX_NODES = 6;
    static final int NARROW_MIN_NODES = 6;
    static final int NARROW_MAX_NODES = 10;
    static final double EDGE_CHANCE = 0.6;
    static final double SECOND_LEVEL_CHANCE = 0.35;
    /** A net keeps this far inside its cell (blocks, its corridors and shafts included). */
    static final int NET_MARGIN = 12;
    /** A shaft is 3 × 3 (centre ± 1); the entrance shaft runs up to the world's top. */
    static final int SHAFT_HALF = 1;
    static final int SKY = 320;

    static final double COPY_CHANCE = 0.25;
    static final int MIN_COPY_LENGTH = 48;
    static final int MAX_COPY_LENGTH = 96;
    static final int MIN_COPY_GAP = 8;
    static final int MAX_COPY_GAP = 24;
    static final int MIN_COPIES = 2;
    static final int MAX_COPIES = 3;
    static final int MAX_STUBS = 3;
    static final int MAX_STUB = 6;
    /** At least this much rock between a copy's stub and the next copy. */
    static final int STUB_ROCK = 2;

    // ---------------------------------------------------------------- void caves

    static final int CHAMBER_CELL = 256;
    static final double CHAMBER_CHANCE = 0.9;
    /** Radius (Ø 12–30), height and the floor's level (V93-4: Y −40 … 10). */
    static final double MIN_RADIUS = 6.0;
    static final double MAX_RADIUS = 15.0;
    static final int MIN_HEIGHT = 6;
    static final int MAX_HEIGHT = 14;
    static final int MIN_FLOOR = -40;
    static final int MAX_FLOOR = 10;
    /** A chamber keeps this far inside its cell: wider than the lava dam (6) and the noise caves' closure. */
    static final int CHAMBER_MARGIN = 40;
    /** The outline swings at most this share of the radius (three harmonics). */
    static final double RAGGED = 0.2;
    /** The floor rises this much from the middle to the wall. */
    static final double FLOOR_RISE = 3.0;
    static final double MIRROR_CHANCE = 0.25;
    static final int MIN_HOLES = 1;
    static final int MAX_HOLES = 4;
    /** Hole radius (Ø 2–6). */
    static final double MIN_HOLE = 1.0;
    static final double MAX_HOLE = 3.0;
    /** The floor round a hole that the code marks as its rim (for W-biomes' voidstone). */
    static final double RIM = 2.0;
    static final int RIM_DEPTH = 2;
    /** The bottom of a hole: far below the world's bottom (open into the void). */
    static final int HOLE_BOTTOM = -1024;

    /** Outer band (V93-8): one chamber at most per cell of this size, in a floe at least this thick. */
    static final int OUTER_CELL = 512;
    static final double OUTER_CHANCE = 0.9;
    static final double OUTER_MAX_RADIUS = 12.0;
    static final int OUTER_MAX_HEIGHT = 10;
    static final int OUTER_MIN_THICKNESS = 40;
    /** Rock kept under the floor and over the ceiling of an outer chamber. */
    static final int OUTER_FLOOR_ROCK = 4;
    static final int OUTER_ROOF_ROCK = 6;

    // ---------------------------------------------------------------- the code of the DF ring_5/overworld/caves

    /** What a block of ring 5's own caves holds; {@link #code} is the value of {@code ring_5/overworld/caves}. */
    enum Kind {
        /** Not in a cave of its own (rock, open ground, vanilla's caves). */
        NONE,
        /** An impossible corridor or one of its shafts. */
        CORRIDOR,
        /** A void cave's chamber. */
        CHAMBER,
        /** A hole down into the void. */
        HOLE,
        /** Rock: the floor round a hole (its rim; for W-biomes' voidstone). */
        RIM;

        private static final Kind[] BY_CODE = values();

        int code() {
            return ordinal();
        }

        static Kind of(int code) {
            return BY_CODE[code];
        }
    }

    private static final int NET_SALT = 9301;
    private static final int EDGE_SALT = 9302;
    private static final int COPY_SALT = 9303;
    private static final int CHAMBER_SALT = 9304;
    private static final int HOLE_SALT = 9305;
    private static final int OUTER_SALT = 9306;

    private final long salt;

    Ring5Caves(long salt) {
        this.salt = salt;
    }

    static int netCellOf(int block) {
        return Math.floorDiv(block, NET_CELL);
    }

    static int chamberCellOf(int block) {
        return Math.floorDiv(block, CHAMBER_CELL);
    }

    static int outerCellOf(int block) {
        return Math.floorDiv(block, OUTER_CELL);
    }

    // ---------------------------------------------------------------- nets

    /** The net of net cell (i, j) before the field's check, or null. */
    @Nullable
    Net net(int i, int j) {
        long h = CellShapes.cell(salt, i, j, NET_SALT);
        if (CellShapes.unit(h, 0) >= NET_CHANCE) {
            return null;
        }
        boolean wide = CellShapes.unit(h, 1) < 0.5;
        int level = (int) Math.floor(LEVELS * CellShapes.unit(h, 2));
        int floor = LEVEL_TOP - LEVEL_STEP * level;
        Net net = CellShapes.unit(h, 3) < COPY_CHANCE ? copied(i, j, h, wide, floor) : grid(i, j, h, wide, floor, level);
        return net.boxes.isEmpty() ? null : net;
    }

    /** A grid net: nodes every spacing, each edge with its chance, sometimes a second level; one entrance shaft. */
    private Net grid(int i, int j, long h, boolean wide, int floor, int level) {
        int spacing = wide ? WIDE_SPACING : NARROW_SPACING;
        int min = wide ? WIDE_MIN_NODES : NARROW_MIN_NODES;
        int max = wide ? WIDE_MAX_NODES : NARROW_MAX_NODES;
        int nx = min + (int) Math.floor((max - min + 1) * CellShapes.unit(h, 4));
        int nz = min + (int) Math.floor((max - min + 1) * CellShapes.unit(h, 5));
        // The outermost nodes' shafts and corridors reach SHAFT_HALF past them (WIDE_HALF is no wider).
        int room = NET_CELL - 2 * NET_MARGIN - 2 * SHAFT_HALF;
        int x0 = i * NET_CELL + NET_MARGIN + SHAFT_HALF + (int) Math.floor((room - (nx - 1) * spacing) * CellShapes.unit(h, 6));
        int z0 = j * NET_CELL + NET_MARGIN + SHAFT_HALF + (int) Math.floor((room - (nz - 1) * spacing) * CellShapes.unit(h, 7));
        boolean two = level < LEVELS - 1 && CellShapes.unit(h, 8) < SECOND_LEVEL_CHANCE;
        int half = wide ? WIDE_HALF : NARROW_HALF;
        int height = wide ? WIDE_HEIGHT : NARROW_HEIGHT;
        Net net = new Net(wide, floor, two ? floor - LEVEL_STEP : floor);
        boolean[][] top = edges(i, j, 0, nx, nz);
        boolean[][] low = two ? edges(i, j, 1, nx, nz) : null;
        int entrance = pickNode(h, 9, top, null, nx, nz);
        if (entrance < 0) {
            return net;
        }
        addEdges(net, top, nx, nz, x0, z0, spacing, half, floor, height);
        int ex = x0 + (entrance % nx) * spacing;
        int ez = z0 + (entrance / nx) * spacing;
        net.add(ex - SHAFT_HALF, ez - SHAFT_HALF, ex + SHAFT_HALF, ez + SHAFT_HALF, floor, SKY, true);
        if (low != null) {
            // The shaft between the levels stands on a node with a corridor on both; if none has, the lower level gets one.
            int joint = pickNode(h, 10, top, low, nx, nz);
            if (joint < 0) {
                joint = entrance;
                int a = joint % nx;
                int b = joint / nx;
                if (a < nx - 1) {
                    low[0][joint] = true;
                } else {
                    low[0][b * nx + a - 1] = true;
                }
            }
            int lowFloor = floor - LEVEL_STEP;
            addEdges(net, low, nx, nz, x0, z0, spacing, half, lowFloor, height);
            int jx = x0 + (joint % nx) * spacing;
            int jz = z0 + (joint / nx) * spacing;
            net.add(jx - SHAFT_HALF, jz - SHAFT_HALF, jx + SHAFT_HALF, jz + SHAFT_HALF, lowFloor, floor + height, true);
        }
        return net;
    }

    /** The edges of one level: [0][node] to the east neighbour, [1][node] to the south one, each with its chance. */
    private boolean[][] edges(int i, int j, int level, int nx, int nz) {
        long g = CellShapes.cell(salt, i, j, EDGE_SALT + 16 * level);
        boolean[][] edges = new boolean[2][nx * nz];
        for (int b = 0; b < nz; b++) {
            for (int a = 0; a < nx; a++) {
                int node = b * nx + a;
                edges[0][node] = a < nx - 1 && CellShapes.unit(g, 2 * node) < EDGE_CHANCE;
                edges[1][node] = b < nz - 1 && CellShapes.unit(g, 2 * node + 1) < EDGE_CHANCE;
            }
        }
        return edges;
    }

    private static boolean touched(boolean[][] edges, int node, int nx) {
        int a = node % nx;
        return edges[0][node] || edges[1][node] || a > 0 && edges[0][node - 1] || node >= nx && edges[1][node - nx];
    }

    /** A node with a corridor on {@code first} (and on {@code second}, if given), from a hashed start; −1 if none. */
    private static int pickNode(long h, int n, boolean[][] first, @Nullable boolean[][] second, int nx, int nz) {
        int nodes = nx * nz;
        int start = (int) Math.floor(nodes * CellShapes.unit(h, n));
        for (int k = 0; k < nodes; k++) {
            int node = (start + k) % nodes;
            if (touched(first, node, nx) && (second == null || touched(second, node, nx))) {
                return node;
            }
        }
        return -1;
    }

    private static void addEdges(Net net, boolean[][] edges, int nx, int nz, int x0, int z0, int spacing, int half,
                                 int floor, int height) {
        for (int b = 0; b < nz; b++) {
            for (int a = 0; a < nx; a++) {
                int node = b * nx + a;
                int cx = x0 + a * spacing;
                int cz = z0 + b * spacing;
                if (edges[0][node]) {
                    net.add(cx - half, cz - half, cx + spacing + half, cz + half, floor, floor + height, false);
                }
                if (edges[1][node]) {
                    net.add(cx - half, cz - half, cx + half, cz + spacing + half, floor, floor + height, false);
                }
            }
        }
    }

    /**
     * A copied net (V93-5): one straight corridor with up to {@link #MAX_STUBS} dead-end stubs on one side, laid down
     * {@link #MIN_COPIES}–{@link #MAX_COPIES} times side by side; each copy with a shaft at its start.
     */
    private Net copied(int i, int j, long h, boolean wide, int floor) {
        long g = CellShapes.cell(salt, i, j, COPY_SALT);
        boolean alongX = CellShapes.unit(g, 0) < 0.5;
        int length = MIN_COPY_LENGTH + (int) Math.floor((MAX_COPY_LENGTH - MIN_COPY_LENGTH + 1) * CellShapes.unit(g, 1));
        int gap = MIN_COPY_GAP + (int) Math.floor((MAX_COPY_GAP - MIN_COPY_GAP + 1) * CellShapes.unit(g, 2));
        int copies = MIN_COPIES + (int) Math.floor((MAX_COPIES - MIN_COPIES + 1) * CellShapes.unit(g, 3));
        int half = wide ? WIDE_HALF : NARROW_HALF;
        int height = wide ? WIDE_HEIGHT : NARROW_HEIGHT;
        // The stubs end STUB_ROCK before the next copy's corridor.
        int stub = Math.min(MAX_STUB, gap - 2 * half - 1 - STUB_ROCK);
        int stubs = stub < 2 ? 0 : (int) Math.floor((MAX_STUBS + 1) * CellShapes.unit(g, 4));
        // The extent across: the first copy's shaft, then the last copy's shaft or, wider, its stubs on the far side.
        int far = stubs > 0 ? Math.max(SHAFT_HALF, half + stub) : SHAFT_HALF;
        int across = (copies - 1) * gap + SHAFT_HALF + far;
        // Along: the shaft at the start reaches SHAFT_HALF before the corridor.
        int along0 = NET_MARGIN + SHAFT_HALF + (int) Math.floor((NET_CELL - 2 * NET_MARGIN - SHAFT_HALF - length) * CellShapes.unit(g, 5));
        int across0 = NET_MARGIN + SHAFT_HALF + (int) Math.floor((NET_CELL - 2 * NET_MARGIN - across) * CellShapes.unit(g, 6));
        int[] stubAt = new int[stubs];
        for (int s = 0; s < stubs; s++) {
            stubAt[s] = 2 + (int) Math.floor((length - 4) * CellShapes.unit(g, 7 + s));
        }
        int baseAlong = (alongX ? i : j) * NET_CELL + along0;
        int baseAcross = (alongX ? j : i) * NET_CELL + across0;
        Net net = new Net(wide, floor, floor);
        net.copies = copies;
        net.copyGap = gap;
        net.copyAlongX = alongX;
        for (int k = 0; k < copies; k++) {
            int c = baseAcross + k * gap;
            addAligned(net, alongX, baseAlong, c - half, baseAlong + length, c + half, floor, floor + height, false);
            for (int at : stubAt) {
                addAligned(net, alongX, baseAlong + at - half, c + half + 1, baseAlong + at + half, c + half + stub, floor,
                        floor + height, false);
            }
            addAligned(net, alongX, baseAlong - SHAFT_HALF, c - SHAFT_HALF, baseAlong + SHAFT_HALF, c + SHAFT_HALF, floor, SKY, true);
        }
        return net;
    }

    /** A box given along and across the copies' axis. */
    private static void addAligned(Net net, boolean alongX, int along0, int across0, int along1, int across1, int y0, int y1,
                                   boolean shaft) {
        if (alongX) {
            net.add(along0, across0, along1, across1, y0, y1, shaft);
        } else {
            net.add(across0, along0, across1, along1, y0, y1, shaft);
        }
    }

    // ---------------------------------------------------------------- chambers

    /** The void cave of chamber cell (i, j) in the inner band, before the field's check, or null. */
    @Nullable
    Chamber chamber(int i, int j) {
        long h = CellShapes.cell(salt, i, j, CHAMBER_SALT);
        if (CellShapes.unit(h, 0) >= CHAMBER_CHANCE) {
            return null;
        }
        double radius = MIN_RADIUS + (MAX_RADIUS - MIN_RADIUS) * CellShapes.unit(h, 1);
        int height = MIN_HEIGHT + (int) Math.floor((MAX_HEIGHT - MIN_HEIGHT + 1) * CellShapes.unit(h, 2));
        int floor = MIN_FLOOR + (int) Math.floor((MAX_FLOOR - MIN_FLOOR + 1) * CellShapes.unit(h, 3));
        return shape(h, i * CHAMBER_CELL, j * CHAMBER_CELL, CHAMBER_CELL, radius, height, floor);
    }

    /**
     * The candidate of an outer chamber (V93-8) in outer cell (i, j): its middle, radius and height; the field finds the
     * floe round it and its floor ({@link #outerChamber}). Null if the cell has none.
     */
    @Nullable
    Chamber outerCandidate(int i, int j) {
        long h = CellShapes.cell(salt, i, j, OUTER_SALT);
        if (CellShapes.unit(h, 0) >= OUTER_CHANCE) {
            return null;
        }
        double radius = MIN_RADIUS + (OUTER_MAX_RADIUS - MIN_RADIUS) * CellShapes.unit(h, 1);
        int height = MIN_HEIGHT + (int) Math.floor((OUTER_MAX_HEIGHT - MIN_HEIGHT + 1) * CellShapes.unit(h, 2));
        return shape(h, i * OUTER_CELL, j * OUTER_CELL, OUTER_CELL, radius, height, 0);
    }

    /** The outer candidate on its floor, which the field chose from the floe round it (between {@code low} and {@code high}). */
    static Chamber outerChamber(Chamber candidate, int low, int high, long h) {
        int floor = low + (int) Math.floor((high - low + 1) * SeededHash.unit(h, 0));
        return candidate.at(floor);
    }

    private Chamber shape(long h, int originX, int originZ, int cell, double radius, int height, int floor) {
        boolean mirrored = CellShapes.unit(h, 4) < MIRROR_CHANCE;
        // A mirrored chamber has its axis on a block edge (an integer middle), so the mirror maps blocks onto blocks.
        double cx = originX + CHAMBER_MARGIN + Math.floor((cell - 2.0 * CHAMBER_MARGIN) * CellShapes.unit(h, 5));
        double cz = originZ + CHAMBER_MARGIN + Math.floor((cell - 2.0 * CHAMBER_MARGIN) * CellShapes.unit(h, 6));
        if (!mirrored) {
            cx += CellShapes.unit(h, 7);
            cz += CellShapes.unit(h, 8);
        }
        boolean axisX = CellShapes.unit(h, 9) < 0.5;
        double axis = axisX ? 0.0 : Math.PI / 2.0;
        double[] amplitude = new double[3];
        double[] phase = new double[3];
        double total = 0.0;
        for (int k = 0; k < 3; k++) {
            amplitude[k] = CellShapes.unit(h, 10 + k);
            total += amplitude[k];
            phase[k] = mirrored ? 0.0 : 2.0 * Math.PI * CellShapes.unit(h, 13 + k);
        }
        for (int k = 0; k < 3; k++) {
            amplitude[k] *= RAGGED / Math.max(total, 1e-9);
        }
        // Holes stay inside the chamber's smallest outline (radius × (1 − RAGGED)), with room round them.
        long g = CellShapes.cell(salt, (long) Math.floor(cx), (long) Math.floor(cz), HOLE_SALT);
        int count = MIN_HOLES + (int) Math.floor((MAX_HOLES - MIN_HOLES + 1) * SeededHash.unit(g, 0));
        List<double[]> holes = new ArrayList<>();
        double inside = radius * (1.0 - RAGGED);
        for (int k = 0; holes.size() < count && k < MAX_HOLES; k++) {
            double r = MIN_HOLE + (MAX_HOLE - MIN_HOLE) * SeededHash.unit(g, 1 + 4 * k);
            double reach = Math.max(0.0, 0.8 * inside - r);
            double d = reach * Math.sqrt(SeededHash.unit(g, 2 + 4 * k));
            double a = 2.0 * Math.PI * SeededHash.unit(g, 3 + 4 * k);
            double hx = cx + d * Math.cos(a);
            double hz = cz + d * Math.sin(a);
            if (mirrored && count - holes.size() == 1) {
                // The last, unpaired hole of a mirrored chamber lies on its axis (its own mirror image).
                if (axisX) {
                    hz = cz;
                } else {
                    hx = cx;
                }
            }
            holes.add(new double[]{hx, hz, r});
            if (mirrored && holes.size() < count) {
                // The mirror image about the chamber's axis (the line through the middle along x or z).
                holes.add(axisX ? new double[]{hx, 2.0 * cz - hz, r} : new double[]{2.0 * cx - hx, hz, r});
            }
        }
        return new Chamber(cx, cz, radius, height, floor, mirrored, axis, amplitude, phase, List.copyOf(holes));
    }

    // ---------------------------------------------------------------- records

    /** An axis-aligned box of open blocks: x0..x1, z0..z1 inclusive, y0 (floor) up to y1 (exclusive). */
    record Box(int x0, int z0, int x1, int z1, int y0, int y1, boolean shaft) {
        boolean covers(int x, int z) {
            return x >= x0 && x <= x1 && z >= z0 && z <= z1;
        }
    }

    /** A net: its boxes (corridors and shafts) and the box round them all (2D). */
    static final class Net {
        final boolean wide;
        /** The floor of the net's top and lowest level. */
        final int top;
        final int low;
        final List<Box> boxes = new ArrayList<>();
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        /** A copied net (V93-5): how many copies, how far apart and along which axis; 1 for a grid net. */
        int copies = 1;
        int copyGap;
        boolean copyAlongX;

        Net(boolean wide, int top, int low) {
            this.wide = wide;
            this.top = top;
            this.low = low;
        }

        void add(int x0, int z0, int x1, int z1, int y0, int y1, boolean shaft) {
            boxes.add(new Box(x0, z0, x1, z1, y0, y1, shaft));
            minX = Math.min(minX, x0);
            minZ = Math.min(minZ, z0);
            maxX = Math.max(maxX, x1);
            maxZ = Math.max(maxZ, z1);
        }

        boolean near(int x, int z, int margin) {
            return x >= minX - margin && x <= maxX + margin && z >= minZ - margin && z <= maxZ + margin;
        }
    }

    /**
     * A void cave's chamber: middle, radius, height and floor; the outline swings with three harmonics (mirrored: all
     * phases 0 about the axis, so the outline is symmetric); holes {x, z, r} in the floor down into the void.
     */
    record Chamber(double x, double z, double radius, int height, int floor, boolean mirrored, double axis,
                   double[] amplitude, double[] phase, List<double[]> holes) {
        Chamber at(int newFloor) {
            return new Chamber(x, z, radius, height, newFloor, mirrored, axis, amplitude, phase, holes);
        }

        /** The outline's radius towards the angle {@code a} (from the middle). */
        double outline(double a) {
            double r = 1.0;
            for (int k = 0; k < amplitude.length; k++) {
                r += amplitude[k] * Math.cos((k + 2) * (a - axis) + phase[k]);
            }
            return radius * r;
        }

        boolean near(int bx, int bz, double margin) {
            double reach = radius * (1.0 + RAGGED) + margin;
            return Math.abs(bx + 0.5 - x) <= reach && Math.abs(bz + 0.5 - z) <= reach;
        }

        /** How far (blocks) the column lies from the nearest hole's edge: at most 0 on a hole; never more than the truth. */
        double holeDistance(int bx, int bz) {
            double best = Double.POSITIVE_INFINITY;
            for (double[] hole : holes) {
                best = Math.min(best, Math.hypot(bx + 0.5 - hole[0], bz + 0.5 - hole[1]) - hole[2]);
            }
            return best;
        }

        /**
         * Writes the chamber's open interval at the column into {@code out} ({floor, ceiling, kind}) and returns true, or
         * false where the column is outside. A hole's column is open from {@link #HOLE_BOTTOM}; the rim's rock is {@code
         * out[3]} (the rim's top, else {@link Integer#MIN_VALUE}).
         */
        boolean column(int bx, int bz, int[] out) {
            double dx = bx + 0.5 - x;
            double dz = bz + 0.5 - z;
            double d = Math.hypot(dx, dz);
            double r = outline(Math.atan2(dz, dx));
            if (d >= r) {
                return false;
            }
            double t = d / r;
            int bottom = floor + (int) Math.floor(FLOOR_RISE * t * t);
            int ceiling = floor + (int) Math.round(height * Math.sqrt(1.0 - t * t));
            if (ceiling - bottom < 2) {
                return false;
            }
            double hole = holeDistance(bx, bz);
            out[0] = hole <= 0.0 ? HOLE_BOTTOM : bottom;
            out[1] = ceiling;
            out[2] = hole <= 0.0 ? Kind.HOLE.code() : Kind.CHAMBER.code();
            out[3] = hole > 0.0 && hole <= RIM ? bottom : Integer.MIN_VALUE;
            return true;
        }
    }
}
