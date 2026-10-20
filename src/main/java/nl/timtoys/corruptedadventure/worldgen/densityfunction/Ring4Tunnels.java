package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.util.Mth;
import nl.timtoys.corruptedadventure.ring.Ring4Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * The tunnel net of overworld ring 4 (package 73): the creatures' roads, 5–12 wide, under the whole ring outside its
 * zones. Per band (Q5 = A): in the mythical band flooded to the sea level, with air domes at the high points (a flat
 * water level, sealed); in the blighted band dry, with swamp gas chambers and poison pools (only sources in closed
 * bowls). The bands' nets never meet: an edge between a flooded and a dry node is cut in the middle by a plug of rock
 * (the closed transition). Entrances (Q6 = A) rise into the islands' holes that hold the sea (Y 62) and, dry, into the
 * peat sinkholes; never under a lake above the sea. Nothing lies within {@link #ZONE_GAP} blocks of the zones 3→4 and
 * 4→5.
 * <p>
 * <b>Shape.</b> A node per cell of {@link #CELL} (chance {@link #NODE_CHANCE}) with a hall, an edge to the east and
 * the south neighbour (chance {@link #EDGE_CHANCE} each). An edge is a centreline (a bent curve with a lateral wobble,
 * points every {@link #STEP} blocks) with a centre height and a half width at each point and one half height; its
 * cross-section is an ellipse in the vertical plane, so a column's open blocks are one interval per tunnel, found from
 * the nearest centreline piece in 2D. Halls and domes are ellipsoids with a ragged outline. Every value is a pure
 * function of the seed and the cell, decided once per world ({@link Memo}).
 * <p>
 * <b>Water.</b> A flooded edge runs from each node down to a valley, then over crests (local maxima, an air dome on
 * each) and valleys to the other node; every flooded node is a high point with a dome hall. So within a dome's outline
 * no tunnel block lies above its water level, and the level is flat. A dry edge rises from both nodes to a crest, so a
 * dry node is a low point and its pool's level lies under every tunnel's floor there. Sealed spaces (domes, the dry net,
 * the dry sinkholes) close vanilla's caves around them ({@link IslandField#caveLift}, padded by the 4 × 8 × 4 grid the
 * stack interpolates on) and keep carvers out ({@link Span#lifted}). Everything else under the sea level in ring 4 is
 * water ({@code RingAquifer}), so vanilla caves meeting a flooded tunnel meet water.
 * <p>
 * <b>Cost.</b> Per column: one lookup of the chunk's element list (empty almost everywhere, and without any work in
 * the zones) and, near a tunnel, a pass over its centreline pieces; per block, two comparisons against the column's
 * envelope. The decisions sample the surface (on the 4-block grid) along each edge once per world.
 */
public final class Ring4Tunnels {
    // ---------------------------------------------------------------- what a block of the net holds

    /** What fills a block of the net; {@link #code} is the value of the DF {@code ring_4/overworld/tunnels}. */
    public enum Fill {
        /** Not in the net. */
        NONE,
        /** Water: the flooded net (mythical band) up to its level. */
        WATER,
        /** Air in the flooded net: an air dome over its level, or a part above the sea level. */
        DOME_AIR,
        /** Air in the dry net (blighted band). */
        DRY_AIR,
        /** A swamp gas chamber. */
        GAS,
        /** A poison pool (toxic sources in a closed bowl). */
        POOL;

        public int code() {
            return ordinal();
        }
    }

    // ---------------------------------------------------------------- the numbers

    /** The cell of the node grid. */
    static final int CELL = 128;
    /** A node lies at least this far inside its cell. */
    private static final double NODE_MARGIN = 28.0;
    private static final double NODE_CHANCE = 0.8;
    private static final double EDGE_CHANCE = 0.62;
    /** Open space keeps at least this far (effective distance) from the zones 3→4 and 4→5 (Q6 = A). */
    static final double ZONE_GAP = 32.0;
    /** Rock over a tunnel's ceiling, under the lowest corner of the 4-block grid around it. */
    static final double COVER = 6.0;
    /**
     * How far (blocks, sideways and up/down) vanilla's caves are closed around sealed open space: the stack
     * interpolates on a 4 × 8 × 4 grid, so a block's cell corners lie up to 4·√2 sideways and 8 up or down; 2 more
     * blocks of rock and a margin for a slope.
     */
    static final double LIFT_SIDE = 8.0;
    static final int LIFT_UP = 12;
    /** Half widths (5–12 wide) and half heights (5–7 high) of a tunnel. */
    static final double W_MIN = 2.5;
    static final double W_MAX = 6.0;
    static final double HH_MIN = 2.5;
    static final double HH_MAX = 3.5;
    /** The sea's top block: water stands up to and including it. */
    public static final int SEA_TOP = 62;
    static final double Y_MIN = -40.0;
    /** Node levels: a smooth base (the warp noise, ~500 blocks) plus a jitter, per band. */
    private static final double FLOODED_MID = 4.0;
    private static final double FLOODED_AMP = 16.0;
    private static final double FLOODED_JITTER = 8.0;
    private static final double DRY_MID = 18.0;
    private static final double DRY_AMP = 12.0;
    private static final double DRY_JITTER = 6.0;
    /** Halls at the nodes. */
    private static final double HALL_R_MIN = 7.0;
    private static final double HALL_R_MAX = 11.0;
    /** The outline of a hall or dome swings out at most this share of its radius. */
    private static final double RAGGED = 0.2;
    /** The highest a hall reaches above its node's centre (the clearance a node needs under the ground). */
    private static final double HALL_UP_MAX = 11.0;
    /** Crest to crest along a flooded edge, and at least this from a valley to a crest. */
    private static final double CREST_SPACING = 44.0;
    private static final double CREST_STEP_MIN = 20.0;
    private static final double DROP_MIN = 4.0;
    private static final double DROP_MAX = 8.0;
    /** A crest's dome is this much wider than the tunnel there; with the outline's swing it reaches {@link #DOME_REACH}. */
    private static final double DOME_WIDER = 1.5;
    private static final double DOME_REACH = (W_MAX + DOME_WIDER) * 1.2 + 1.0;
    /** A dome's air over its water level. */
    private static final double DOME_MIN = 3.0;
    private static final double DOME_MAX = 5.0;
    private static final double ENTRANCE_MIN = 48.0;
    private static final double ENTRANCE_MAX = 110.0;
    private static final double ENTRANCE_CHANCE = 0.75;
    /** A hole's floor at its centre must lie at least this low for an entrance (water over it). */
    private static final double ENTRANCE_HOLE_FLOOR = 56.0;
    /**
     * The steepest a ramp to an entrance rises on average (blocks up per block along; the smootherstep is up to 1.9
     * times as steep in its middle): swum in a flooded one, walked (with jumps) in a dry one.
     */
    private static final double RAMP_SLOPE = 0.75;
    private static final double DRY_RAMP_SLOPE = 0.5;
    /**
     * Around a dry sinkhole's bowl the ground is checked by its rings ({@link #decideSink}) out to this far past its
     * radius; a ramp's cover is not checked there (the 4-block grid's corners in the bowl would forbid any ramp).
     */
    private static final double SINK_RINGS = 7.0;
    private static final double SINK_RAMP_FREE = 6.0;
    /** The steepest an edge between two nodes may be on average. */
    private static final double EDGE_SLOPE = 0.4;
    /** Rock at least this thick between the open spaces of a flooded and a dry element, and around a dome or pool. */
    private static final double GAP = 4.0;
    /** Centreline points along a tunnel. */
    private static final double STEP = 3.0;
    /** A dry sinkhole needs ground at least this high around its bowl. */
    private static final double SINK_RIM = 64.5;
    /** Shares of a dry hall's kinds: a poison pool, a gas chamber, else plain. */
    private static final double POOL_CHANCE = 0.35;
    private static final double GAS_CHANCE = 0.3;

    private static final int KIND_NODE = 7301;
    private static final int KIND_EDGE = 7302;
    private static final int KIND_ENTRANCE = 7304;
    private static final int KIND_HALL = 7305;
    private static final int KIND_DRY_ENTRANCE = 7306;

    // Interval kinds of a column's span.
    private static final byte K_FLOOD = 1;
    private static final byte K_DRY = 2;
    private static final byte K_GAS = 3;
    private static final byte K_POOL = 4;

    private final IslandField field;

    Ring4Tunnels(IslandField field) {
        this.field = field;
    }

    /**
     * The world's decisions. Kept per world here, not in the field's {@code Decisions}: in-game a noise chunk's copy of
     * the field does not share those (most likely its ring 3 plateau input gets fresh caches from the noise chunk, so
     * the copy is not {@code equals} and gets decisions of its own); the net's decisions reach some hundred cells around a chunk and must not be made again per chunk (perf,
     * 2026-10-16). The key is everything they depend on: the seed (through the salt and the layout's noise seed) and
     * the ring settings.
     */
    private Memo memo() {
        Memo known = memo;
        if (known == null) {
            RingLayout layout = field.ringLayout();
            WorldKey key = new WorldKey(field.salt(), layout.noiseSeed(), layout.settings(), field.settings(), field.relief());
            known = WORLDS.get(key);
            if (known == null) {
                synchronized (WORLDS) {
                    known = WORLDS.get(key);
                    if (known == null) {
                        if (WORLDS.size() >= MAX_WORLDS) {
                            // A new world (seed or radii): the old ones' decisions are no longer asked for.
                            WORLDS.clear();
                        }
                        known = new Memo();
                        WORLDS.put(key, known);
                    }
                }
            }
            memo = known;
        }
        return known;
    }

    /**
     * What the decisions depend on: the seed (the hash noise's salt, the layout's noise seed), the ring settings, the
     * field's settings and its relief (its noises: one world's instances). Not the ring 3 plateau's own settings (one
     * {@code islands.json} per world, read only in the zones the net keeps away from).
     */
    private record WorldKey(long salt, long noiseSeed, nl.timtoys.corruptedadventure.config.RingSettings ringSettings,
                            IslandField.Settings settings, Ring4Relief relief) {
    }

    /**
     * The decisions of the worlds generated in this JVM (a server has one overworld; tests make a few); at most
     * {@link #MAX_WORLDS}, then forgotten (a reopened world has new noises: a new key).
     */
    private static final Map<WorldKey, Memo> WORLDS = new ConcurrentHashMap<>();
    private static final int MAX_WORLDS = 4;
    private volatile Memo memo;

    /** Forgets every world's decisions (tests: decide a world again from scratch); fields already asked keep theirs. */
    static void forgetWorlds() {
        synchronized (WORLDS) {
            WORLDS.clear();
        }
    }

    // ---------------------------------------------------------------- per column

    /**
     * The open intervals and the closed caves of the net in a column (the column's values filled: its sinkhole),
     * or null where the net has nothing there.
     */
    @Nullable
    Span span(IslandField.Column c) {
        List<Element> elements = elementsOfChunk(c.x >> 4, c.z >> 4);
        if (elements.isEmpty()) {
            return null;
        }
        double px = c.x + 0.5;
        double pz = c.z + 0.5;
        Span span = null;
        for (Element e : elements) {
            if (px < e.minX || px > e.maxX || pz < e.minZ || pz > e.maxZ) {
                continue;
            }
            if (span == null) {
                span = new Span();
            }
            e.addTo(span, px, pz, c);
        }
        return span == null || span.n == 0 && span.nLift == 0 ? null : span;
    }

    /**
     * The elements (halls, tunnels, domes, dry sinkholes) whose reach (with the closed caves around them) covers the
     * chunk, chambers first; none in a chunk the net cannot reach (the zones and beyond).
     */
    List<Element> elementsOfChunk(int cx, int cz) {
        return memo().chunks.get(KeyedSlots.key(cx, cz), key -> {
            int minX = cx << 4;
            int minZ = cz << 4;
            RingLayout layout = field.ringLayout();
            double e = layout.effectiveDistance(minX + 8.0, minZ + 8.0);
            // The farthest an element (an entrance's dry sinkhole with its closed caves) reaches past the net's band.
            double reach = 12.0 + 32.0;
            if (e < innerEdge(layout) - reach || e > outerEdge(layout) + reach) {
                return List.of();
            }
            List<Element> chambers = new ArrayList<>();
            List<Element> tunnels = new ArrayList<>();
            // A cell's elements stay inside the cells around it (withinNeighbours).
            int i0 = Math.floorDiv(minX, CELL);
            int j0 = Math.floorDiv(minZ, CELL);
            for (int i = i0 - 1; i <= i0 + 1; i++) {
                for (int j = j0 - 1; j <= j0 + 1; j++) {
                    for (Element el : finalCell(i, j)) {
                        if (el.maxX >= minX && el.minX <= minX + 16 && el.maxZ >= minZ && el.minZ <= minZ + 16) {
                            (el instanceof Room ? chambers : tunnels).add(el);
                        }
                    }
                }
            }
            int pad = (int) Math.ceil(Ring4Relief.SINKHOLE_MAX_RADIUS + LIFT_SIDE + 1.0);
            int cell = Ring4Relief.SINKHOLE_CELL;
            for (int si = Math.floorDiv(minX - pad, cell); si <= Math.floorDiv(minX + 15 + pad, cell); si++) {
                for (int sj = Math.floorDiv(minZ - pad, cell); sj <= Math.floorDiv(minZ + 15 + pad, cell); sj++) {
                    Sink sink = drySink(si, sj);
                    if (sink != null && sink.maxX >= minX && sink.minX <= minX + 16 && sink.maxZ >= minZ && sink.minZ <= minZ + 16) {
                        tunnels.add(sink);
                    }
                }
            }
            chambers.addAll(tunnels);
            return List.copyOf(chambers);
        });
    }

    // ---------------------------------------------------------------- the band the net lies in

    /** The effective distance from which open space may lie (the zone 3→4's outer edge and the gap). */
    private static double innerEdge(RingLayout layout) {
        return VerdantDescent.zoneStart(layout) + VerdantDescent.width(layout) + ZONE_GAP;
    }

    /** The effective distance up to which open space may lie (the zone 4→5's inner edge less the gap). */
    private static double outerEdge(RingLayout layout) {
        return Ring4Sundered.zoneStart(layout) - ZONE_GAP;
    }

    /** Whether a disc of {@code radius} around (x, z) lies in the net's band (open space at least the gap from the zones). */
    private boolean inBand(double x, double z, double radius) {
        RingLayout layout = field.ringLayout();
        double e = layout.effectiveDistance(x, z);
        return e - radius >= innerEdge(layout) && e + radius <= outerEdge(layout);
    }

    // ---------------------------------------------------------------- the ground over the net

    /**
     * The highest a ceiling may reach at (x, z): {@link #COVER} under the lowest corner of the 4-block grid around it
     * (the stack interpolates between them; holes and sinkholes counted) and under the closed band of a lake above the
     * sea (its caves are rock there, the aquifer would plug a tunnel). {@code ground} remembers corners for one decision.
     */
    private double ceilingLimit(Map<Long, Double> ground, double x, double z) {
        int qx = Mth.floor(x) & ~3;
        int qz = Mth.floor(z) & ~3;
        double lowest = Double.POSITIVE_INFINITY;
        for (int dx = 0; dx <= 4; dx += 4) {
            for (int dz = 0; dz <= 4; dz += 4) {
                int gx = qx + dx;
                int gz = qz + dz;
                Double known = ground.get(KeyedSlots.key(gx, gz));
                if (known == null) {
                    // The face of a rough fjord wall may lie its roughness under the surface (relief 2026-10-20).
                    known = field.faceUncached(gx, gz);
                    ground.put(KeyedSlots.key(gx, gz), known);
                }
                lowest = Math.min(lowest, known);
            }
        }
        // Peat sinkholes are cut per block, not on the grid: the deepest bowl whose disc comes near the point's cell.
        lowest = Math.min(lowest, sinkFloorNear(x, z, 4.0 * Math.sqrt(2.0)));
        double limit = lowest - COVER;
        for (IslandShape shape : field.shapesNear(Mth.floor(x), Mth.floor(z))) {
            double reach = shape.groundReach();
            if (Math.abs(x - shape.x()) > reach || Math.abs(z - shape.z()) > reach) {
                continue;
            }
            if (shape.holeDistance(x, z) < 1.0 + IslandShape.CAVE_BAND / shape.holeRadius()) {
                IslandField.Details details = field.details(shape);
                if (details.lakeTop() != Integer.MIN_VALUE) {
                    limit = Math.min(limit, details.lakeBottom() - IslandField.CAVE_FLOOR_MARGIN - 2.0);
                }
            }
        }
        return limit;
    }

    /** The deepest floor of a sinkhole (the peat cutting it) whose bowl comes within {@code ring} of (x, z), else infinity. */
    private double sinkFloorNear(double x, double z, double ring) {
        double reach = ring + Ring4Relief.SINKHOLE_MAX_RADIUS;
        int cell = Ring4Relief.SINKHOLE_CELL;
        double floor = Double.POSITIVE_INFINITY;
        for (int si = Math.floorDiv(Mth.floor(x - reach), cell); si <= Math.floorDiv(Mth.floor(x + reach), cell); si++) {
            for (int sj = Math.floorDiv(Mth.floor(z - reach), cell); sj <= Math.floorDiv(Mth.floor(z + reach), cell); sj++) {
                double[] hole = Ring4Relief.sinkhole(si, sj, field.salt());
                if (hole != null && Math.hypot(hole[0] - x, hole[1] - z) - hole[2] <= ring) {
                    // The deepest floor, not the centre's: on a slope the bowl is deepest on its low side.
                    floor = Math.min(floor, field.deepestSinkholeFloor(si, sj, hole));
                }
            }
        }
        return floor;
    }

    // ---------------------------------------------------------------- stage 1: nodes

    /** A node: a hall's centre ({@code y} its middle), its band, its hall's radius. */
    record Node(int i, int j, double x, double y, double z, boolean flooded, double hallR, long hash) {
        long key() {
            return KeyedSlots.key(i, j);
        }
    }

    @Nullable
    Node node(int i, int j) {
        return memo().once(memo().nodes, KeyedSlots.key(i, j), () -> Optional.ofNullable(decideNode(i, j))).orElse(null);
    }

    @Nullable
    private Node decideNode(int i, int j) {
        long h = CellShapes.cell(field.salt(), i, j, KIND_NODE);
        if (CellShapes.unit(h, 0) >= NODE_CHANCE) {
            return null;
        }
        double x = i * CELL + NODE_MARGIN + CellShapes.unit(h, 1) * (CELL - 2.0 * NODE_MARGIN);
        double z = j * CELL + NODE_MARGIN + CellShapes.unit(h, 2) * (CELL - 2.0 * NODE_MARGIN);
        double hallR = HALL_R_MIN + (HALL_R_MAX - HALL_R_MIN) * CellShapes.unit(h, 3);
        if (!inBand(x, z, hallR * (1.0 + RAGGED) + LIFT_SIDE + 2.0)) {
            return null;
        }
        boolean flooded = Ring4Band.of(field.ringLayout()).value(x, z) < 0.0;
        double base = field.relief().warpAt(x * 0.25, 1000.5, z * 0.25);
        double jitter = 2.0 * CellShapes.unit(h, 4) - 1.0;
        double y = flooded ? FLOODED_MID + FLOODED_AMP * base + FLOODED_JITTER * jitter : DRY_MID + DRY_AMP * base + DRY_JITTER * jitter;
        Map<Long, Double> ground = new HashMap<>();
        // The hall's whole footprint (its outline swung out, a block more), every 4 blocks: each sample reads its cell's
        // corners, so every corner under the hall counts.
        double limit = Double.POSITIVE_INFINITY;
        double ring = hallR * (1.0 + RAGGED) + 1.0;
        for (double dx = -ring; dx <= ring + 0.01; dx += 4.0) {
            for (double dz = -ring; dz <= ring + 0.01; dz += 4.0) {
                if (dx * dx + dz * dz <= (ring + 3.0) * (ring + 3.0)) {
                    limit = Math.min(limit, ceilingLimit(ground, x + dx, z + dz));
                }
            }
        }
        y = Math.min(y, limit - HALL_UP_MAX);
        if (y < Y_MIN + 10.0) {
            return null;
        }
        return new Node(i, j, x, y, z, flooded, hallR, h);
    }

    // ---------------------------------------------------------------- stage 2/3: edges and entrances

    /** The edge from node (i, j) to its east ({@code dir} 0) or south (1) neighbour, or null. */
    @Nullable
    Edge edge(int i, int j, int dir) {
        long key = KeyedSlots.key(i, j) * 2 + dir;
        return memo().once(memo().edges, key, () -> Optional.ofNullable(decideEdge(i, j, dir))).orElse(null);
    }

    @Nullable
    private Edge decideEdge(int i, int j, int dir) {
        long h = CellShapes.cell(field.salt(), i, j, KIND_EDGE + dir * 7);
        if (CellShapes.unit(h, 0) >= EDGE_CHANCE) {
            return null;
        }
        Node a = node(i, j);
        Node b = dir == 0 ? node(i + 1, j) : node(i, j + 1);
        if (a == null || b == null) {
            return null;
        }
        Line line = Line.between(a.x, a.z, b.x, b.z, h, 0.2, 2.0 + 6.0 * CellShapes.unit(h, 3));
        double length = line.length();
        if (Math.abs(b.y - a.y) > EDGE_SLOPE * length) {
            return null;
        }
        double hh = HH_MIN + (HH_MAX - HH_MIN) * CellShapes.unit(h, 4);
        double[] w = line.widths(h);
        Edge edge = new Edge(a.key(), b.key());
        boolean made;
        if (a.flooded && b.flooded) {
            made = floodedEdge(edge, line, w, hh, a, b, h);
        } else if (!a.flooded && !b.flooded) {
            made = dryEdge(edge, line, w, hh, a, b, h);
        } else {
            made = mixedEdge(edge, line, w, hh, a, b, h);
        }
        return made && withinNeighbours(edge, i, j) ? edge : null;
    }

    /**
     * Whether every part of an edge or entrance of cell (i, j), with its closed caves, stays inside the cells around it
     * (i ± 1, j ± 1): so a chunk needs only the elements of the 3 × 3 cells around its own, and two elements can meet
     * only if their cells lie at most 2 apart.
     */
    private static boolean withinNeighbours(Edge edge, int i, int j) {
        double minX = (i - 1) * (double) CELL;
        double maxX = (i + 2) * (double) CELL;
        double minZ = (j - 1) * (double) CELL;
        double maxZ = (j + 2) * (double) CELL;
        for (Element e : edge.parts) {
            if (e.minX < minX || e.maxX > maxX || e.minZ < minZ || e.maxZ > maxZ) {
                return false;
            }
        }
        return true;
    }

    /** The distance from a node to its first valley: past its hall and a tunnel's width. */
    private static double valleyDistance(Node n) {
        // A tunnel block within a block of the hall's outline lies on a point at most a width further along the line.
        return (n.hallR * (1.0 + RAGGED) + 1.0 + W_MAX) * 1.05 + 2.0;
    }

    private boolean floodedEdge(Edge edge, Line line, double[] w, double hh, Node a, Node b, long h) {
        double length = line.length();
        double dA = valleyDistance(a);
        double dB = valleyDistance(b);
        double span = length - dA - dB;
        if (span < 0.0) {
            return false;
        }
        int crests = span < 2.0 * CREST_STEP_MIN ? 0
                : Mth.clamp((int) Math.round(span / CREST_SPACING), 1, (int) Math.floor(span / (2.0 * CREST_STEP_MIN)));
        List<double[]> keys = new ArrayList<>();
        keys.add(new double[]{0.0, a.y});
        if (crests == 0) {
            double s = dA + span / 2.0;
            keys.add(new double[]{s, Math.min(a.y, b.y) - drop(h, 10)});
        } else {
            int marks = 2 * crests;
            double[] ys = new double[marks + 1];
            double[] ss = new double[marks + 1];
            for (int m = 0; m <= marks; m++) {
                ss[m] = dA + m * span / marks;
                if (m % 2 == 1) {
                    ys[m] = Mth.lerp(ss[m] / length, a.y, b.y) - 2.0 + 4.0 * SeededHash.unit(h, 20 + m);
                }
            }
            for (int m = 0; m <= marks; m += 2) {
                double before = m == 0 ? a.y : ys[m - 1];
                double after = m == marks ? b.y : ys[m + 1];
                ys[m] = Math.min(before, after) - drop(h, 10 + m);
            }
            for (int m = 0; m <= marks; m++) {
                keys.add(new double[]{ss[m], ys[m]});
            }
        }
        keys.add(new double[]{length, b.y});
        Profile profile = new Profile(keys);
        // The crests' domes add their air to the ceiling the ground must cover.
        double[] domeH = new double[crests];
        for (int k = 0; k < crests; k++) {
            domeH[k] = DOME_MIN + (DOME_MAX - DOME_MIN) * SeededHash.unit(h, 40 + k);
        }
        if (!fit(line, w, hh, profile, dA, dB, k -> crestPeak(profile, crests, k), domeH, null)) {
            return false;
        }
        Seg seg = new Seg(edge, line, w, hh, profile, 0, line.size() - 1, K_FLOOD, false, a.key(), b.key());
        edge.parts.add(seg);
        for (int k = 0; k < crests; k++) {
            double s = profile.keyS(2 + 2 * k);
            double yc = profile.at(s);
            int level = Mth.floor(yc + hh);
            if (level >= SEA_TOP) {
                continue;
            }
            int p = line.indexAt(s);
            double ww = w[p];
            // Only the edge's own: the domes keep clear of every other tunnel and hall (stage 4).
            edge.parts.add(new Room(edge, line.x[p], line.z[p], level, ww + DOME_WIDER, 2.0, domeH[k], K_FLOOD, level, true,
                    SeededHash.unit(h, 60 + k), Long.MIN_VALUE, Long.MIN_VALUE));
        }
        return true;
    }

    /** The crest s of the k-th crest of a flooded profile (keys: A, V0, C1, V1, …). */
    private static double crestPeak(Profile profile, int crests, int k) {
        return k < crests ? profile.keyS(2 + 2 * k) : Double.NaN;
    }

    private static double drop(long h, int n) {
        return DROP_MIN + (DROP_MAX - DROP_MIN) * SeededHash.unit(h, n);
    }

    private boolean dryEdge(Edge edge, Line line, double[] w, double hh, Node a, Node b, long h) {
        double length = line.length();
        double crest = Math.max(a.y, b.y) + 3.0 + 5.0 * SeededHash.unit(h, 11);
        List<double[]> keys = List.of(new double[]{0.0, a.y}, new double[]{length / 2.0, crest}, new double[]{length, b.y});
        Profile profile = new Profile(keys);
        double floor = Math.max(a.y, b.y) + 2.0;
        if (!fit(line, w, hh, profile, 0.0, 0.0, k -> Double.NaN, new double[0], new double[]{floor})) {
            return false;
        }
        edge.parts.add(new Seg(edge, line, w, hh, profile, 0, line.size() - 1, K_DRY, true, a.key(), b.key()));
        return true;
    }

    /** A flooded node and a dry one: down from both to a valley in the middle, cut there by a plug of rock. */
    private boolean mixedEdge(Edge edge, Line line, double[] w, double hh, Node a, Node b, long h) {
        double length = line.length();
        double valley = Math.min(a.y, b.y) - drop(h, 12);
        Profile profile = new Profile(List.of(new double[]{0.0, a.y}, new double[]{length / 2.0, valley}, new double[]{length, b.y}));
        if (!fit(line, w, hh, profile, 0.0, 0.0, k -> Double.NaN, new double[0], null)) {
            return false;
        }
        // The plug: the two dead ends' caps (a half width each past the last point) keep 2 · 3 blocks of rock apart.
        double cut = W_MAX + 3.0;
        int lastA = line.indexAt(length / 2.0 - cut);
        int firstB = line.indexAt(length / 2.0 + cut) + 1;
        if (lastA < 1 || firstB > line.size() - 2) {
            return false;
        }
        edge.parts.add(new Seg(edge, line, w, hh, profile, 0, lastA, a.flooded ? K_FLOOD : K_DRY, !a.flooded,
                a.key(), Long.MIN_VALUE));
        edge.parts.add(new Seg(edge, line, w, hh, profile, firstB, line.size() - 1, b.flooded ? K_FLOOD : K_DRY, !b.flooded,
                Long.MIN_VALUE, b.key()));
        edge.mixed = true;
        return true;
    }

    /**
     * Whether the profile fits under the ground and inside the band: every point's ceiling (a dome's air near a crest)
     * under {@link #ceilingLimit} at the centre and both sides, and the tunnel with its closed caves inside the band. If
     * not, the interior key points (from the second to the second last) are lowered once by the excess, but no lower
     * than {@code floors[0]} (if given) for the middle key; null-safe. {@code skip} (if non-null) leaves points out of
     * the check (an entrance's end inside its hole or bowl).
     */
    private boolean fit(Line line, double[] w, double hh, Profile profile, double endA, double endB,
                        java.util.function.IntToDoubleFunction crest, double[] domeH, @Nullable double[] floors) {
        return fit(line, w, hh, profile, endA, endB, crest, domeH, floors, null, true);
    }

    private boolean fit(Line line, double[] w, double hh, Profile profile, double endA, double endB,
                        java.util.function.IntToDoubleFunction crest, double[] domeH, @Nullable double[] floors,
                        @Nullable java.util.function.BiPredicate<Double, Double> skip, boolean sealedLift) {
        double length = line.length();
        Map<Long, Double> ground = new HashMap<>();
        double[] limit = new double[line.size()];
        for (int p = 0; p < line.size(); p++) {
            double x = line.x[p];
            double z = line.z[p];
            double reach = w[p];
            for (int k = 0; k < domeH.length; k++) {
                double c = crest.applyAsDouble(k);
                if (!Double.isNaN(c) && Math.abs(line.s[p] - c) <= DOME_REACH) {
                    // A crest's dome is wider than the tunnel, and its closed caves reach past it.
                    reach = Math.max(reach, DOME_REACH);
                }
            }
            if (!inBand(x, z, reach + (sealedLift ? LIFT_SIDE : 0.0))) {
                return false;
            }
            double nx = -line.dz(p);
            double nz = line.dx(p);
            double side = w[p] + 1.0;
            double lim = Double.POSITIVE_INFINITY;
            for (int k = -1; k <= 1; k++) {
                double sx = x + k * side * nx;
                double sz = z + k * side * nz;
                if (skip != null && skip.test(sx, sz)) {
                    continue;
                }
                lim = Math.min(lim, ceilingLimit(ground, sx, sz));
            }
            limit[p] = lim;
        }
        for (int round = 0; round < 2; round++) {
            double excessEnds = Double.NEGATIVE_INFINITY;
            double excess = Double.NEGATIVE_INFINITY;
            for (int p = 0; p < line.size(); p++) {
                if (Double.isInfinite(limit[p])) {
                    continue;
                }
                double s = line.s[p];
                double ceiling = profile.at(s) + hh;
                for (int k = 0; k < domeH.length; k++) {
                    double c = crest.applyAsDouble(k);
                    if (!Double.isNaN(c) && Math.abs(s - c) <= DOME_REACH + W_MAX) {
                        ceiling = Math.max(ceiling, Mth.floor(profile.at(c) + hh) + domeH[k] + 1.0);
                    }
                }
                double over = ceiling - limit[p];
                if (s < endA || s > length - endB) {
                    excessEnds = Math.max(excessEnds, over);
                } else {
                    excess = Math.max(excess, over);
                }
            }
            if (excessEnds > 0.0) {
                return false;
            }
            if (excess <= 0.0) {
                return profile.lowest() >= Y_MIN;
            }
            if (round == 1 || !profile.lowerInterior(excess + 0.5, floors == null ? Double.NEGATIVE_INFINITY : floors[0])) {
                return false;
            }
        }
        return false;
    }

    /** The entrance of a flooded node: a tunnel down to a valley, then up into an island's hole that holds the sea. */
    @Nullable
    Edge floodEntrance(int i, int j) {
        return memo().once(memo().floodEntrances, KeyedSlots.key(i, j), () -> Optional.ofNullable(decideFloodEntrance(i, j)))
                .orElse(null);
    }

    @Nullable
    private Edge decideFloodEntrance(int i, int j) {
        Node a = node(i, j);
        if (a == null || !a.flooded) {
            return null;
        }
        long h = CellShapes.cell(field.salt(), i, j, KIND_ENTRANCE);
        if (CellShapes.unit(h, 0) >= ENTRANCE_CHANCE) {
            return null;
        }
        List<IslandShape> holes = new ArrayList<>();
        int r = (int) ENTRANCE_MAX;
        for (IslandShape shape : field.shapesWithin(Mth.floor(a.x) - r, Mth.floor(a.z) - r, Mth.floor(a.x) + r, Mth.floor(a.z) + r)) {
            double d = Math.hypot(shape.x() - a.x, shape.z() - a.z);
            if (d < ENTRANCE_MIN || d > ENTRANCE_MAX) {
                continue;
            }
            double floor = shape.holeFloor(shape.x(), shape.z());
            if (Double.isNaN(floor) || floor > ENTRANCE_HOLE_FLOOR || !inBand(shape.x(), shape.z(), W_MAX + 2.0)
                    || field.details(shape).lakeTop() != Integer.MIN_VALUE) {
                continue;
            }
            holes.add(shape);
        }
        holes.sort(Comparator.comparingDouble((IslandShape s) -> Math.hypot(s.x() - a.x, s.z() - a.z))
                .thenComparingDouble(IslandShape::x).thenComparingDouble(IslandShape::z));
        for (IslandShape hole : holes) {
            // Never under a lake above the sea: the hole's centre outside every lake's closed band.
            if (underLake(hole.x(), hole.z())) {
                continue;
            }
            double floor = field.surfaceUncached(hole.x(), hole.z());
            double hh = HH_MIN + (HH_MAX - HH_MIN) * CellShapes.unit(h, 4);
            // The ceiling breaks 3 blocks into the hole's water: the tunnel opens there.
            double yE = floor + 3.0 - hh;
            Line line = Line.between(a.x, a.z, hole.x(), hole.z(), h, 0.1, 2.0);
            double length = line.length();
            double dA = valleyDistance(a);
            double valley = Math.min(a.y, yE) - drop(h, 5);
            if (length - dA < (yE - valley) / RAMP_SLOPE) {
                continue;
            }
            Profile profile = new Profile(List.of(new double[]{0.0, a.y}, new double[]{dA, valley}, new double[]{length, yE}));
            double[] w = line.widths(h);
            Edge edge = new Edge(a.key(), Long.MIN_VALUE);
            boolean fits = fit(line, w, hh, profile, dA, 0.0, k -> Double.NaN, new double[0], null,
                    (x, z) -> hole.holeDistance(x, z) < 1.0, false);
            if (!fits) {
                continue;
            }
            edge.parts.add(new Seg(edge, line, w, hh, profile, 0, line.size() - 1, K_FLOOD, false, a.key(), Long.MIN_VALUE));
            if (!withinNeighbours(edge, i, j)) {
                continue;
            }
            return edge;
        }
        return null;
    }

    /** Whether (x, z) lies in the closed band of a lake above the sea. */
    private boolean underLake(double x, double z) {
        for (IslandShape shape : field.shapesNear(Mth.floor(x), Mth.floor(z))) {
            double reach = shape.groundReach();
            if (Math.abs(x - shape.x()) <= reach && Math.abs(z - shape.z()) <= reach
                    && shape.holeDistance(x, z) < 1.0 + IslandShape.CAVE_BAND / shape.holeRadius()
                    && field.details(shape).lakeTop() != Integer.MIN_VALUE) {
                return true;
            }
        }
        return false;
    }

    /** The entrance of a dry node: a ramp rising into a dry peat sinkhole's bowl. */
    @Nullable
    Edge dryEntrance(int i, int j) {
        return memo().once(memo().dryEntrances, KeyedSlots.key(i, j), () -> Optional.ofNullable(decideDryEntrance(i, j)))
                .orElse(null);
    }

    @Nullable
    private Edge decideDryEntrance(int i, int j) {
        Node a = node(i, j);
        if (a == null || a.flooded) {
            return null;
        }
        long h = CellShapes.cell(field.salt(), i, j, KIND_DRY_ENTRANCE);
        if (CellShapes.unit(h, 0) >= ENTRANCE_CHANCE) {
            return null;
        }
        int cell = Ring4Relief.SINKHOLE_CELL;
        List<double[]> bowls = new ArrayList<>();
        for (int si = Math.floorDiv((int) (a.x - ENTRANCE_MAX), cell); si <= Math.floorDiv((int) (a.x + ENTRANCE_MAX), cell); si++) {
            for (int sj = Math.floorDiv((int) (a.z - ENTRANCE_MAX), cell); sj <= Math.floorDiv((int) (a.z + ENTRANCE_MAX), cell); sj++) {
                double[] hole = Ring4Relief.sinkhole(si, sj, field.salt());
                if (hole == null) {
                    continue;
                }
                double d = Math.hypot(hole[0] - a.x, hole[1] - a.z);
                if (d < ENTRANCE_MIN || d > ENTRANCE_MAX) {
                    continue;
                }
                // A bowl dry under the sea level, or one whose floor lies above it with its ground checked all the same.
                double floor = sinkFloor(si, sj, hole);
                if (Double.isNaN(floor) || !rimHolds(hole) || floor <= SEA_TOP && drySink(si, sj) == null) {
                    continue;
                }
                bowls.add(new double[]{hole[0], hole[1], hole[2], floor, d});
            }
        }
        bowls.sort(Comparator.comparingDouble((double[] b) -> b[4]).thenComparingDouble(b -> b[0]).thenComparingDouble(b -> b[1]));
        for (double[] bowl : bowls) {
            double hh = HH_MIN + (HH_MAX - HH_MIN) * CellShapes.unit(h, 4);
            // The tunnel's floor meets the bowl's floor: a ramp to walk out on.
            double yE = bowl[3] - 0.5 + hh;
            Line line = Line.between(a.x, a.z, bowl[0], bowl[1], h, 0.1, 2.0);
            double length = line.length();
            if (yE < a.y + 1.0 || yE - a.y > DRY_RAMP_SLOPE * length) {
                continue;
            }
            Profile profile = new Profile(List.of(new double[]{0.0, a.y}, new double[]{length, yE}));
            double[] w = line.widths(h);
            double cx = bowl[0];
            double cz = bowl[1];
            double free = bowl[2] + SINK_RAMP_FREE;
            boolean fits = fit(line, w, hh, profile, 0.0, 0.0, k -> Double.NaN, new double[0], null,
                    (x, z) -> Math.hypot(x - cx, z - cz) < free, true);
            if (!fits) {
                continue;
            }
            Edge edge = new Edge(a.key(), Long.MIN_VALUE);
            edge.parts.add(new Seg(edge, line, w, hh, profile, 0, line.size() - 1, K_DRY, true, a.key(), Long.MIN_VALUE));
            if (!withinNeighbours(edge, i, j)) {
                continue;
            }
            return edge;
        }
        return null;
    }

    /**
     * A sinkhole's floor at its centre, NaN where the peat does not cut it; remembered per world. The entrances meet the
     * bowl there and a dry bowl is decided by it; a bound of the ground takes {@link IslandField#deepestSinkholeFloor}.
     */
    private double sinkFloor(int si, int sj, double[] hole) {
        return memo().once(memo().sinkFloors, KeyedSlots.key(si, sj), () -> {
            Ring4Relief.Sample s = new Ring4Relief.Sample();
            field.sampleRelief(hole[0], hole[1], s);
            return s.sinkholeFloor;
        });
    }

    // ---------------------------------------------------------------- dry sinkholes

    /**
     * The sinkhole of sinkhole cell (si, sj) as a dry bowl (Q6 = A: dry entrances), or null where it stays as it is:
     * none, its floor above the sea anyway, its ground around lower than {@link #SINK_RIM}, near the zones, or a flooded
     * tunnel near its bowl.
     */
    @Nullable
    Sink drySink(int si, int sj) {
        return memo().once(memo().sinks, KeyedSlots.key(si, sj), () -> Optional.ofNullable(decideSink(si, sj))).orElse(null);
    }

    @Nullable
    private Sink decideSink(int si, int sj) {
        double[] hole = Ring4Relief.sinkhole(si, sj, field.salt());
        if (hole == null || !inBand(hole[0], hole[1], hole[2] + LIFT_SIDE + 2.0)) {
            return null;
        }
        double floor = sinkFloor(si, sj, hole);
        if (Double.isNaN(floor) || floor > SEA_TOP) {
            return null;
        }
        if (!rimHolds(hole)) {
            return null;
        }
        Sink sink = new Sink(si, sj, hole[0], hole[1], hole[2], floor);
        // Every owner cell whose elements may reach the bowl's box (an element stays within its owner's neighbours).
        for (int i = Math.floorDiv(Mth.floor(sink.minX), CELL) - 1; i <= Math.floorDiv(Mth.floor(sink.maxX), CELL) + 1; i++) {
            for (int j = Math.floorDiv(Mth.floor(sink.minZ), CELL) - 1; j <= Math.floorDiv(Mth.floor(sink.maxZ), CELL) + 1; j++) {
                for (Element e : floodedOf(i, j)) {
                    if (e.conflicts(sink)) {
                        return null;
                    }
                }
            }
        }
        return sink;
    }

    /**
     * Whether the ground around a sinkhole's bowl stands at least {@link #SINK_RIM} high out to {@link #SINK_RINGS}
     * past its radius (rings of 16 points), so neither the bowl's air nor a ramp beside it meets water.
     */
    private boolean rimHolds(double[] hole) {
        for (double ring : new double[]{hole[2] + 1.5, hole[2] + 4.0, hole[2] + SINK_RINGS}) {
            for (int k = 0; k < 16; k++) {
                double a = (k + ring / 7.0) * Math.PI / 8.0;
                if (field.surfaceUncached(hole[0] + ring * Math.cos(a), hole[1] + ring * Math.sin(a)) < SINK_RIM) {
                    return false;
                }
            }
        }
        return true;
    }

    /** The flooded elements decided by stage 2 for cell (i, j): its node's edges' flooded parts and its entrance. */
    private List<Element> floodedOf(int i, int j) {
        List<Element> found = new ArrayList<>();
        for (Edge edge : new Edge[]{edge(i, j, 0), edge(i, j, 1), floodEntrance(i, j)}) {
            if (edge != null) {
                for (Element e : edge.parts) {
                    if (e.flooded()) {
                        found.add(e);
                    }
                }
            }
        }
        // Only a flooded node's hall: a dry node's hall asks its entrance, which asks the sinkholes (no loop).
        Node n = node(i, j);
        Room hall = n != null && n.flooded ? hall(i, j) : null;
        if (hall != null) {
            found.add(hall);
        }
        return found;
    }

    // ---------------------------------------------------------------- halls

    /** The hall of node (i, j), from the edges and entrances that meet there; null without any. */
    @Nullable
    Room hall(int i, int j) {
        return memo().once(memo().halls, KeyedSlots.key(i, j), () -> Optional.ofNullable(decideHall(i, j))).orElse(null);
    }

    @Nullable
    private Room decideHall(int i, int j) {
        Node n = node(i, j);
        if (n == null) {
            return null;
        }
        Edge[] at = {edge(i, j, 0), edge(i, j, 1), edge(i - 1, j, 0), edge(i, j - 1, 1),
                n.flooded ? floodEntrance(i, j) : dryEntrance(i, j)};
        double hh = 0.0;
        boolean mixed = false;
        for (Edge edge : at) {
            if (edge == null) {
                continue;
            }
            mixed |= edge.mixed;
            for (Element e : edge.parts) {
                if (e instanceof Seg seg && (seg.nodeA == n.key() || seg.nodeB == n.key())) {
                    hh = Math.max(hh, seg.hh);
                }
            }
        }
        if (hh == 0.0) {
            return null;
        }
        long h = CellShapes.cell(field.salt(), i, j, KIND_HALL);
        double rag = CellShapes.unit(h, 1);
        if (n.flooded) {
            int level = Mth.floor(n.y + hh);
            double air = 4.0 + 2.0 * CellShapes.unit(h, 0);
            boolean dome = level < SEA_TOP;
            return new Room(null, n.x, n.z, n.y, n.hallR, hh + 1.5, level - n.y + air, K_FLOOD, dome ? level : SEA_TOP, dome,
                    rag, n.key(), Long.MIN_VALUE);
        }
        double u = CellShapes.unit(h, 0);
        if (!mixed && u < POOL_CHANCE) {
            int level = Mth.ceil(n.y - hh) - 1;
            double depth = 2.0 + CellShapes.unit(h, 2);
            return new Room(null, n.x, n.z, n.y, n.hallR, n.y - level + depth, hh + 3.0, K_POOL, level, true, rag, n.key(),
                    Long.MIN_VALUE);
        }
        byte kind = u < POOL_CHANCE + GAS_CHANCE ? K_GAS : K_DRY;
        return new Room(null, n.x, n.z, n.y, n.hallR, hh + 1.0, hh + 3.0, kind, Integer.MIN_VALUE, true, rag, n.key(), Long.MIN_VALUE);
    }

    // ---------------------------------------------------------------- stage 4: what stays

    /** All elements decided for cell (i, j) before the checks against the neighbours. */
    private List<Element> stage(int i, int j) {
        return memo().once(memo().stages, KeyedSlots.key(i, j), () -> {
            List<Element> all = new ArrayList<>();
            Room hall = hall(i, j);
            if (hall != null) {
                all.add(hall);
            }
            Node n = node(i, j);
            for (Edge edge : new Edge[]{edge(i, j, 0), edge(i, j, 1), n != null && n.flooded ? floodEntrance(i, j) : dryEntrance(i, j)}) {
                if (edge != null) {
                    all.addAll(edge.parts);
                }
            }
            return List.copyOf(all);
        });
    }

    /**
     * The elements of cell (i, j) that stay: a dry element near any flooded one is dropped (the nets never meet), a
     * dome near a flooded element it does not belong to loses its air (its water would stand beside the dome's air), a
     * pool near a dry element it does not belong to becomes a plain hall (open space beside or under the pool).
     */
    List<Element> finalCell(int i, int j) {
        return memo().once(memo().finals, KeyedSlots.key(i, j), () -> {
            List<Element> own = stage(i, j);
            if (own.isEmpty()) {
                return List.of();
            }
            List<Element> near = new ArrayList<>();
            for (int di = -2; di <= 2; di++) {
                for (int dj = -2; dj <= 2; dj++) {
                    near.addAll(stage(i + di, j + dj));
                }
            }
            List<Element> kept = new ArrayList<>();
            for (Element e : own) {
                Element result = e;
                if (!e.flooded()) {
                    for (Element f : near) {
                        if (f.flooded() && !e.attached(f) && e.conflicts(f)) {
                            result = null;
                            break;
                        }
                    }
                }
                if (result instanceof Room room && room.sealed && room.kind == K_FLOOD) {
                    for (Element f : near) {
                        if (f != e && f.flooded() && !e.attached(f) && e.conflicts(f)) {
                            // A crest's dome goes; a hall keeps its water up to the sea level, no dome.
                            result = room.owner != null ? null : room.unsealed();
                            break;
                        }
                    }
                }
                if (result instanceof Room room && room.kind == K_POOL) {
                    for (Element f : near) {
                        if (f != e && !f.flooded() && !e.attached(f) && e.conflicts(f)) {
                            result = room.plain();
                            break;
                        }
                    }
                }
                if (result != null) {
                    kept.add(result);
                }
            }
            return List.copyOf(kept);
        });
    }

    // ---------------------------------------------------------------- read access

    /** The tunnels (centrelines) whose reach comes into the box, for the features of the tunnels (W-biomes, 77). */
    List<IslandField.TunnelPath> pathsWithin(int minX, int minZ, int maxX, int maxZ) {
        List<IslandField.TunnelPath> found = new ArrayList<>();
        for (Element e : elementsWithin(minX, minZ, maxX, maxZ)) {
            if (e instanceof Seg seg) {
                List<IslandField.TunnelPoint> points = new ArrayList<>();
                for (int p = seg.from; p <= seg.to; p++) {
                    points.add(new IslandField.TunnelPoint(seg.line.x[p], seg.profile.at(seg.line.s[p]), seg.line.z[p], seg.w[p]));
                }
                found.add(new IslandField.TunnelPath(seg.kind == K_FLOOD ? Fill.WATER : Fill.DRY_AIR, seg.hh, List.copyOf(points)));
            }
        }
        return found;
    }

    /** The halls and domes whose reach comes into the box. */
    List<IslandField.TunnelRoom> roomsWithin(int minX, int minZ, int maxX, int maxZ) {
        List<IslandField.TunnelRoom> found = new ArrayList<>();
        for (Element e : elementsWithin(minX, minZ, maxX, maxZ)) {
            if (e instanceof Room room) {
                Fill fill = switch (room.kind) {
                    case K_FLOOD -> Fill.WATER;
                    case K_GAS -> Fill.GAS;
                    case K_POOL -> Fill.POOL;
                    default -> Fill.DRY_AIR;
                };
                found.add(new IslandField.TunnelRoom(fill, room.owner == null, room.cx, room.cy, room.cz, room.radius,
                        Mth.ceil(room.cy - room.down), Mth.floor(room.cy + room.up),
                        room.kind == K_FLOOD || room.kind == K_POOL ? room.level : Integer.MIN_VALUE));
            }
        }
        return found;
    }

    private List<Element> elementsWithin(int minX, int minZ, int maxX, int maxZ) {
        List<Element> found = new ArrayList<>();
        for (int i = Math.floorDiv(minX, CELL) - 1; i <= Math.floorDiv(maxX, CELL) + 1; i++) {
            for (int j = Math.floorDiv(minZ, CELL) - 1; j <= Math.floorDiv(maxZ, CELL) + 1; j++) {
                for (Element e : finalCell(i, j)) {
                    if (e.maxX >= minX && e.minX <= maxX + 1 && e.maxZ >= minZ && e.minZ <= maxZ + 1) {
                        found.add(e);
                    }
                }
            }
        }
        return found;
    }

    // ---------------------------------------------------------------- the column's span

    /**
     * The net in one column: open intervals (chambers first) with what fills them, and the intervals where vanilla's
     * caves are closed and carvers may not cut. Immutable once filled; one per column that has any.
     */
    public static final class Span {
        private static final int START = 4;
        int n;
        int[] lo = new int[START];
        int[] hi = new int[START];
        byte[] kind = new byte[START];
        int[] level = new int[START];
        boolean[] net = new boolean[START];
        /** The envelope of the intervals the terrain opens (the net's own; a dry sinkhole is the terrain's). */
        int openLo = Integer.MAX_VALUE;
        int openHi = Integer.MIN_VALUE;
        int nLift;
        int[] liftLo = new int[2];
        int[] liftHi = new int[2];
        int liftMin = Integer.MAX_VALUE;
        int liftMax = Integer.MIN_VALUE;

        void add(int from, int to, byte k, int lvl, boolean opens) {
            if (n == lo.length) {
                int size = n * 2;
                lo = java.util.Arrays.copyOf(lo, size);
                hi = java.util.Arrays.copyOf(hi, size);
                kind = java.util.Arrays.copyOf(kind, size);
                level = java.util.Arrays.copyOf(level, size);
                net = java.util.Arrays.copyOf(net, size);
            }
            lo[n] = from;
            hi[n] = to;
            kind[n] = k;
            level[n] = lvl;
            net[n] = opens;
            n++;
            if (opens) {
                openLo = Math.min(openLo, from);
                openHi = Math.max(openHi, to);
            }
        }

        void addLift(int from, int to) {
            if (nLift == liftLo.length) {
                liftLo = java.util.Arrays.copyOf(liftLo, nLift * 2);
                liftHi = java.util.Arrays.copyOf(liftHi, nLift * 2);
            }
            liftLo[nLift] = from;
            liftHi[nLift] = to;
            nLift++;
            liftMin = Math.min(liftMin, from);
            liftMax = Math.max(liftMax, to);
        }

        /** Whether the terrain opens the block for the net. */
        public boolean opens(int y) {
            if (y < openLo || y > openHi) {
                return false;
            }
            for (int k = 0; k < n; k++) {
                if (net[k] && y >= lo[k] && y <= hi[k]) {
                    return true;
                }
            }
            return false;
        }

        /** Whether vanilla's caves are closed at the block (and carvers stay out): around sealed open space. */
        public boolean lifted(int y) {
            if (y < liftMin || y > liftMax) {
                return false;
            }
            for (int k = 0; k < nLift; k++) {
                if (y >= liftLo[k] && y <= liftHi[k]) {
                    return true;
                }
            }
            return false;
        }

        /** What fills the block (open space) for the aquifer: the net's, a dry sinkhole's air, else {@link Fill#NONE}. */
        public Fill fillAt(int y) {
            return fill(y, false);
        }

        /** What fills the block as part of the net only (not a dry sinkhole). */
        public Fill netFillAt(int y) {
            return fill(y, true);
        }

        private Fill fill(int y, boolean netOnly) {
            for (int k = 0; k < n; k++) {
                if (y >= lo[k] && y <= hi[k] && (net[k] || !netOnly)) {
                    return switch (kind[k]) {
                        case K_FLOOD -> y <= Math.min(level[k], SEA_TOP) ? Fill.WATER : Fill.DOME_AIR;
                        case K_GAS -> Fill.GAS;
                        case K_POOL -> y <= level[k] ? Fill.POOL : Fill.DRY_AIR;
                        default -> Fill.DRY_AIR;
                    };
                }
            }
            return Fill.NONE;
        }

        /** The lowest block the net opens in the column ({@link Integer#MAX_VALUE}: none). */
        public int openLo() {
            return openLo;
        }

        /** The highest such block. */
        public int openHi() {
            return openHi;
        }
    }

    // ---------------------------------------------------------------- elements

    /** One piece of the net with its reach (open space plus the closed caves around it) as a box. */
    abstract static class Element {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        /** The edge it is part of (a tunnel, a crest's dome), null for a hall or a sinkhole. */
        @Nullable
        final Edge owner;
        final long nodeA;
        final long nodeB;

        Element(@Nullable Edge owner, long nodeA, long nodeB) {
            this.owner = owner;
            this.nodeA = nodeA;
            this.nodeB = nodeB;
        }

        abstract void addTo(Span span, double px, double pz, IslandField.Column c);

        abstract boolean flooded();

        /** Spheres (x, y, z, radius) covering the open space, for the checks between elements. */
        abstract double[][] spheres();

        void cover(double x, double z, double reach) {
            minX = Math.min(minX, x - reach);
            maxX = Math.max(maxX, x + reach);
            minZ = Math.min(minZ, z - reach);
            maxZ = Math.max(maxZ, z + reach);
        }

        /** Part of the same edge, or meeting at a node. */
        boolean attached(Element other) {
            if (owner != null && owner == other.owner) {
                return true;
            }
            return nodeA != Long.MIN_VALUE && (nodeA == other.nodeA || nodeA == other.nodeB)
                    || nodeB != Long.MIN_VALUE && (nodeB == other.nodeA || nodeB == other.nodeB);
        }

        /** Whether the open spaces come closer than {@link #GAP}. */
        boolean conflicts(Element other) {
            if (other.minX > maxX || other.maxX < minX || other.minZ > maxZ || other.maxZ < minZ) {
                return false;
            }
            for (double[] a : spheres()) {
                for (double[] b : other.spheres()) {
                    double dx = a[0] - b[0];
                    double dy = a[1] - b[1];
                    double dz = a[2] - b[2];
                    double reach = a[3] + b[3] + GAP;
                    if (dx * dx + dy * dy + dz * dz < reach * reach) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /** A tunnel (or its part on one side of a plug): centreline points {@code from}..{@code to}. */
    static final class Seg extends Element {
        final Line line;
        final double[] w;
        final double hh;
        final Profile profile;
        final int from;
        final int to;
        final byte kind;
        final boolean sealed;
        private final double[] y;
        private final double[][] spheres;

        Seg(Edge owner, Line line, double[] w, double hh, Profile profile, int from, int to, byte kind, boolean sealed,
            long nodeA, long nodeB) {
            super(owner, nodeA, nodeB);
            this.line = line;
            this.w = w;
            this.hh = hh;
            this.profile = profile;
            this.from = from;
            this.to = to;
            this.kind = kind;
            this.sealed = sealed;
            this.y = new double[line.size()];
            for (int p = from; p <= to; p++) {
                y[p] = profile.at(line.s[p]);
                cover(line.x[p], line.z[p], w[p] + LIFT_SIDE + 1.0);
            }
            List<double[]> list = new ArrayList<>();
            for (int p = from; p <= to; p += 2) {
                list.add(new double[]{line.x[p], y[p], line.z[p], Math.max(w[p], hh) + 0.5});
            }
            if ((to - from) % 2 != 0) {
                list.add(new double[]{line.x[to], y[to], line.z[to], Math.max(w[to], hh) + 0.5});
            }
            spheres = list.toArray(new double[0][]);
        }

        @Override
        boolean flooded() {
            return kind == K_FLOOD;
        }

        @Override
        void addTo(Span span, double px, double pz, IslandField.Column c) {
            int lo = Integer.MAX_VALUE;
            int hi = Integer.MIN_VALUE;
            int liftLo = Integer.MAX_VALUE;
            int liftHi = Integer.MIN_VALUE;
            double[] xs = line.x;
            double[] zs = line.z;
            for (int p = from; p < to; p++) {
                int q = p + 1;
                double ax = xs[p];
                double az = zs[p];
                double bx = xs[q];
                double bz = zs[q];
                double reach = Math.max(w[p], w[q]) + (sealed ? LIFT_SIDE : 0.0);
                if (px < Math.min(ax, bx) - reach || px > Math.max(ax, bx) + reach
                        || pz < Math.min(az, bz) - reach || pz > Math.max(az, bz) + reach) {
                    continue;
                }
                double vx = bx - ax;
                double vz = bz - az;
                double len2 = vx * vx + vz * vz;
                double t = len2 == 0.0 ? 0.0 : Mth.clamp(((px - ax) * vx + (pz - az) * vz) / len2, 0.0, 1.0);
                double dx = px - (ax + t * vx);
                double dz = pz - (az + t * vz);
                double d = Math.sqrt(dx * dx + dz * dz);
                double ww = Mth.lerp(t, w[p], w[q]);
                double yc = Mth.lerp(t, y[p], y[q]);
                if (d < ww) {
                    double s = Math.sqrt(1.0 - d * d / (ww * ww));
                    int a = Mth.ceil(yc - hh * s);
                    int b = Mth.floor(yc + hh * s);
                    if (a <= b) {
                        lo = Math.min(lo, a);
                        hi = Math.max(hi, b);
                    }
                }
                if (sealed && d < ww + LIFT_SIDE) {
                    liftLo = Math.min(liftLo, Mth.floor(yc - hh) - LIFT_UP);
                    liftHi = Math.max(liftHi, Mth.ceil(yc + hh) + LIFT_UP);
                }
            }
            if (lo <= hi) {
                span.add(lo, hi, kind, kind == K_FLOOD ? SEA_TOP : Integer.MIN_VALUE, true);
            }
            if (liftLo <= liftHi) {
                span.addLift(liftLo, liftHi);
            }
        }

        @Override
        double[][] spheres() {
            return spheres;
        }
    }

    /**
     * A hall or a dome: an ellipsoid with a ragged outline, its middle {@code cy}, reaching {@code down} and {@code up};
     * {@code level} the top of its water (flooded) or of its pool.
     */
    static final class Room extends Element {
        final double cx;
        final double cz;
        final double cy;
        final double radius;
        final double down;
        final double up;
        final byte kind;
        final int level;
        final boolean sealed;
        final double rag;
        private final double[][] spheres;

        Room(@Nullable Edge owner, double cx, double cz, double cy, double radius, double down, double up, byte kind, int level,
             boolean sealed, double rag, long nodeA, long nodeB) {
            super(owner, nodeA, nodeB);
            this.cx = cx;
            this.cz = cz;
            this.cy = cy;
            this.radius = radius;
            this.down = down;
            this.up = up;
            this.kind = kind;
            this.level = level;
            this.sealed = sealed;
            this.rag = rag;
            cover(cx, cz, radius * (1.0 + RAGGED) + (sealed ? LIFT_SIDE : 0.0) + 1.0);
            spheres = new double[][]{{cx, cy + (up - down) / 2.0, cz, Math.max(radius * (1.0 + RAGGED), (up + down) / 2.0)}};
        }

        /** A hall without its dome: water up to the sea level, no sealed air. */
        Room unsealed() {
            return new Room(owner, cx, cz, cy, radius, down, up, K_FLOOD, SEA_TOP, false, rag, nodeA, nodeB);
        }

        /** A pool hall as a plain dry hall (the pool's bowl stays, without the pool). */
        Room plain() {
            return new Room(owner, cx, cz, cy, radius, down, up, K_DRY, Integer.MIN_VALUE, true, rag, nodeA, nodeB);
        }

        @Override
        boolean flooded() {
            return kind == K_FLOOD;
        }

        /** The outline's radius in the direction of (dx, dz). */
        private double outline(double dx, double dz) {
            double a = Math.atan2(dz, dx);
            double phase = rag * Math.PI * 2.0;
            return radius * (1.0 + RAGGED * (0.6 * Math.sin(3.0 * a + phase) + 0.4 * Math.sin(5.0 * a - 2.0 * phase)));
        }

        @Override
        void addTo(Span span, double px, double pz, IslandField.Column c) {
            double dx = px - cx;
            double dz = pz - cz;
            double d = Math.sqrt(dx * dx + dz * dz);
            double r = outline(dx, dz);
            if (d < r) {
                double s = Math.sqrt(1.0 - d * d / (r * r));
                int a = Mth.ceil(cy - down * s);
                int b = Mth.floor(cy + up * s);
                if (a <= b) {
                    span.add(a, b, kind, level, true);
                }
            }
            if (sealed && d < r + LIFT_SIDE) {
                span.addLift(Mth.floor(cy - down) - LIFT_UP, Mth.ceil(cy + up) + LIFT_UP);
            }
        }

        @Override
        double[][] spheres() {
            return spheres;
        }
    }

    /** A dry peat sinkhole: its bowl's open space under the sea level stays air, its caves closed around it. */
    static final class Sink extends Element {
        final int si;
        final int sj;
        final double cx;
        final double cz;
        final double radius;
        final double floor;

        Sink(int si, int sj, double cx, double cz, double radius, double floor) {
            super(null, Long.MIN_VALUE, Long.MIN_VALUE);
            this.si = si;
            this.sj = sj;
            this.cx = cx;
            this.cz = cz;
            this.radius = radius;
            this.floor = floor;
            cover(cx, cz, radius + LIFT_SIDE + 1.0);
        }

        @Override
        boolean flooded() {
            return false;
        }

        @Override
        void addTo(Span span, double px, double pz, IslandField.Column c) {
            double d = Math.hypot(px - cx, pz - cz);
            // The bowl exactly where the terrain cuts it: the column's own sinkhole (its cell's), with a floor.
            int cell = Ring4Relief.SINKHOLE_CELL;
            if (Math.floorDiv(c.x, cell) == si && Math.floorDiv(c.z, cell) == sj && !Double.isNaN(c.sinkholeFloor)) {
                int from = Mth.ceil(c.sinkholeFloor);
                if (from <= SEA_TOP) {
                    span.add(from, SEA_TOP, K_DRY, Integer.MIN_VALUE, false);
                }
            }
            if (d < radius + LIFT_SIDE) {
                span.addLift(Mth.floor(floor) - LIFT_UP, SEA_TOP + LIFT_UP);
            }
        }

        @Override
        double[][] spheres() {
            return new double[][]{{cx, (floor + SEA_TOP) / 2.0, cz, Math.max(radius, (SEA_TOP - floor) / 2.0 + 1.0)}};
        }
    }

    /** An edge's parts: one tunnel, or two around a plug, with the crests' domes. */
    static final class Edge {
        final long nodeA;
        final long nodeB;
        final List<Element> parts = new ArrayList<>();
        boolean mixed;

        Edge(long nodeA, long nodeB) {
            this.nodeA = nodeA;
            this.nodeB = nodeB;
        }
    }

    // ---------------------------------------------------------------- centrelines and profiles

    /** A centreline in 2D: points with their arc length. */
    static final class Line {
        final double[] x;
        final double[] z;
        final double[] s;

        private Line(double[] x, double[] z) {
            this.x = x;
            this.z = z;
            this.s = new double[x.length];
            for (int p = 1; p < x.length; p++) {
                s[p] = s[p - 1] + Math.hypot(x[p] - x[p - 1], z[p] - z[p - 1]);
            }
        }

        /**
         * From (ax, az) to (bx, bz): a quadratic curve bent sideways by up to {@code bend} of its length, with a
         * lateral wobble of {@code wobble} blocks that fades at both ends.
         */
        static Line between(double ax, double az, double bx, double bz, long h, double bend, double wobble) {
            double vx = bx - ax;
            double vz = bz - az;
            double chord = Math.hypot(vx, vz);
            double nx = -vz / chord;
            double nz = vx / chord;
            double off = (2.0 * SeededHash.unit(h, 30) - 1.0) * bend * chord;
            double mx = (ax + bx) / 2.0 + nx * off;
            double mz = (az + bz) / 2.0 + nz * off;
            double waves = 1.0 + Math.floor(2.0 * SeededHash.unit(h, 31));
            double phase = SeededHash.unit(h, 32) * Math.PI * 2.0;
            int count = Math.max(8, (int) Math.ceil(chord * (1.0 + bend) / STEP)) + 1;
            double[] x = new double[count];
            double[] z = new double[count];
            for (int p = 0; p < count; p++) {
                double t = p / (count - 1.0);
                double u = 1.0 - t;
                double lateral = wobble * Math.sin(Math.PI * 2.0 * waves * t + phase) * Math.sin(Math.PI * t);
                x[p] = u * u * ax + 2.0 * u * t * mx + t * t * bx + nx * lateral;
                z[p] = u * u * az + 2.0 * u * t * mz + t * t * bz + nz * lateral;
            }
            return new Line(x, z);
        }

        int size() {
            return x.length;
        }

        double length() {
            return s[s.length - 1];
        }

        /** The last point at or before arc length {@code at}. */
        int indexAt(double at) {
            int p = 0;
            while (p + 1 < s.length && s[p + 1] <= at) {
                p++;
            }
            return p;
        }

        /** The unit direction at a point (x, z). */
        double dx(int p) {
            int a = Math.max(0, p - 1);
            int b = Math.min(x.length - 1, p + 1);
            double l = Math.hypot(x[b] - x[a], z[b] - z[a]);
            return l == 0.0 ? 1.0 : (x[b] - x[a]) / l;
        }

        double dz(int p) {
            int a = Math.max(0, p - 1);
            int b = Math.min(x.length - 1, p + 1);
            double l = Math.hypot(x[b] - x[a], z[b] - z[a]);
            return l == 0.0 ? 0.0 : (z[b] - z[a]) / l;
        }

        /** Half widths along the line: from one end's to the other's, with a swell. */
        double[] widths(long h) {
            double wa = W_MIN + (W_MAX - W_MIN) * SeededHash.unit(h, 33);
            double wb = W_MIN + (W_MAX - W_MIN) * SeededHash.unit(h, 34);
            double phase = SeededHash.unit(h, 35) * Math.PI * 2.0;
            double[] w = new double[x.length];
            double length = Math.max(1.0, length());
            for (int p = 0; p < x.length; p++) {
                double t = s[p] / length;
                w[p] = Mth.clamp(Mth.lerp(t, wa, wb) + 1.0 * Math.sin(s[p] / 11.0 + phase), W_MIN, W_MAX);
            }
            return w;
        }
    }

    /** A centre height along a line: key points (s, y), smootherstep between them (level at every key). */
    static final class Profile {
        private final double[] s;
        private final double[] y;

        Profile(List<double[]> keys) {
            s = new double[keys.size()];
            y = new double[keys.size()];
            for (int k = 0; k < keys.size(); k++) {
                s[k] = keys.get(k)[0];
                y[k] = keys.get(k)[1];
            }
        }

        double at(double at) {
            if (at <= s[0]) {
                return y[0];
            }
            for (int k = 0; k + 1 < s.length; k++) {
                if (at <= s[k + 1]) {
                    double span = s[k + 1] - s[k];
                    double t = span <= 0.0 ? 1.0 : (at - s[k]) / span;
                    return y[k] + (y[k + 1] - y[k]) * t * t * t * (t * (6.0 * t - 15.0) + 10.0);
                }
            }
            return y[y.length - 1];
        }

        double keyS(int k) {
            return s[k];
        }

        double lowest() {
            double low = Double.POSITIVE_INFINITY;
            for (double v : y) {
                low = Math.min(low, v);
            }
            return low;
        }

        /** Lowers the interior keys by {@code by}, none below {@code floor}; false if one would have to go lower. */
        boolean lowerInterior(double by, double floor) {
            for (int k = 1; k + 1 < y.length; k++) {
                if (y[k] - by < floor) {
                    return false;
                }
            }
            for (int k = 1; k + 1 < y.length; k++) {
                y[k] -= by;
            }
            return true;
        }
    }

    // ---------------------------------------------------------------- per world

    /**
     * The decisions of the net, per world (shared by all copies of the world's field, {@link #memo}). Never cleared
     * while the world is asked for (a {@link #WORLDS} entry may be dropped, a field keeps its memo): {@link #finalCell}
     * tells an element's own parts by identity, so a decision must not be made twice; the maps are bounded by the
     * ring's area (a few thousand cells, some ten thousand sinkhole cells).
     */
    static final class Memo {
        final Once<Optional<Node>> nodes = new Once<>();
        final Once<Optional<Edge>> edges = new Once<>();
        final Once<Optional<Edge>> floodEntrances = new Once<>();
        final Once<Optional<Edge>> dryEntrances = new Once<>();
        final Once<Optional<Sink>> sinks = new Once<>();
        final Once<Double> sinkFloors = new Once<>();
        final Once<Optional<Room>> halls = new Once<>();
        final Once<List<Element>> stages = new Once<>();
        final Once<List<Element>> finals = new Once<>();
        final KeyedSlots<List<Element>> chunks = new KeyedSlots<>(1024);

        <V> V once(Once<V> map, long key, Supplier<V> decide) {
            return map.get(key, decide);
        }
    }

    /**
     * Decisions made exactly once per key: the first thread decides, the others wait for its result. At the start of an
     * area every worldgen thread needs the same decisions at the same moment (a chunk asks for some hundred cells);
     * deciding them all in every thread cost more than the noise itself (perf, 2026-10-16). Safe from deadlock: the
     * decisions depend on each other without a cycle (leaves {@code node} and {@code sinkFloor}; {@code edge} and
     * {@code floodEntrance} on nodes; a flooded node's {@code hall} on those; {@code drySink} on flooded parts and
     * halls; {@code dryEntrance} on sinks; a dry node's {@code hall} on its entrance; {@code stage}, {@code finalCell}
     * on all). A cycle added later would make a thread wait for itself: that throws instead of hanging.
     */
    static final class Once<V> {
        private final Map<Long, Pending<V>> map = new ConcurrentHashMap<>();

        private record Pending<V>(Thread owner, java.util.concurrent.CompletableFuture<V> future) {
        }

        V get(long key, Supplier<V> decide) {
            Pending<V> known = map.get(key);
            if (known == null) {
                Pending<V> mine = new Pending<>(Thread.currentThread(), new java.util.concurrent.CompletableFuture<>());
                known = map.putIfAbsent(key, mine);
                if (known == null) {
                    try {
                        V value = decide.get();
                        mine.future().complete(value);
                        return value;
                    } catch (RuntimeException | Error e) {
                        map.remove(key, mine);
                        mine.future().completeExceptionally(e);
                        throw e;
                    }
                }
            }
            if (!known.future().isDone() && known.owner() == Thread.currentThread()) {
                throw new IllegalStateException("tunnel net decision " + key + " depends on itself");
            }
            try {
                return known.future().join();
            } catch (java.util.concurrent.CompletionException e) {
                throw e.getCause() instanceof RuntimeException cause ? cause : e;
            }
        }
    }
}
