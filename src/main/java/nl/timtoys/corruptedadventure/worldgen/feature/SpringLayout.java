package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The geometry and water of a wall spring ({@code canyon_spring}), without world access.
 * <p>
 * A source block sits in a canyon wall, {@code spring_height} above the floor, at the back of a cleft one block wide
 * cut into the sloping wall foot (choice of the user, 2026-10-02: the walls flatten out towards the floor, so sheer
 * faces at the floor are rare). In front of it the water falls down the cleft into a trench, one block wide and deep,
 * that runs out of the cleft over the floor ({@code stream_length} blocks in all, at most one bend) into a pool of
 * 2–3 × 2–3, 1–2 deep. Every block is given in vanilla's settled state, computed
 * here with vanilla's rules (source and pool = sources, the fall = falling water, the trench = flowing water whose
 * amount is the highest neighbour − 1), so a fluid tick on any of them changes nothing. Nothing can leak: every water
 * block of the trench, the pool and the source has only its chain neighbours or solid terrain beside it and solid
 * terrain (or water) below; the fall only touches air and the wall, and falling water onto water never spreads.
 * <p>
 * All blocks lie in the origin chunk, at least one block from its border, so the feature reads and writes only its own
 * chunk (the heightmap ahead of the wall may be read further out: it shows terrain only).
 */
final class SpringLayout {
    static final int MAX_STREAM = 6;
    /** How far behind the foot the wall may reach its height, and the source may sit. */
    static final int REACH = 3;
    /** Water this far ahead of the wall must stay open (≤ 2 above the floor): no springs in narrow slots. */
    static final int OPEN_AHEAD = 6;
    private static final int SOURCE = 8;

    private SpringLayout() {
    }

    /** What the layout reads of the world, all in absolute block coordinates. */
    interface Terrain {
        /** Y of the top terrain block ({@code WORLD_SURFACE_WG} − 1). */
        int surface(int x, int z);

        /** A full block that water cannot pass or enter. */
        boolean solid(int x, int y, int z);

        /** Ground the trench and pool may be dug in (and the rim may replace). */
        boolean floor(int x, int y, int z);

        boolean air(int x, int y, int z);

        /** A block that falls when it has nothing solid below (sand, gravel). */
        boolean gravity(int x, int y, int z);
    }

    /**
     * The random parts of one attempt.
     *
     * @param bendAt index of the trench block where it turns (1 … length − 2), or 0 for a straight trench
     * @param bendLeft direction of the turn
     * @param poolShift how far the pool sits sideways of the trench axis (0 … poolWidth − 1)
     */
    record Params(int springHeight, int streamLength, int bendAt, boolean bendLeft, int poolWidth, int poolLength, int poolShift,
                  int poolDepth) {
    }

    enum Kind {
        /** The spring in the wall. */
        SOURCE,
        /** Water with this amount (1–7), not falling. */
        FLOWING,
        /** Falling water (block level 8). */
        FALLING,
        /** Still water of the pool (sources). */
        POOL
    }

    /** One water block. {@code amount}: 8 for sources and falling water, 1–7 for flowing water. */
    record Water(BlockPos pos, Kind kind, int amount) {
        /** The block state's {@code level}: 0 for sources, 8 − amount for flowing, 8 for falling water. */
        int blockLevel() {
            return switch (kind) {
                case SOURCE, POOL -> 0;
                case FALLING -> 8;
                case FLOWING -> 8 - amount;
            };
        }
    }

    /**
     * @param carve      blocks cut away for the cleft that stay air (the fall column itself is in {@code water})
     * @param water      every water block (source, fall, trench, pool), the source first
     * @param rim        floor blocks around the pool that become rim ground
     * @param plantSpots air above rim blocks, where plants may grow
     * @param besideWater the subset of {@code plantSpots} whose ground touches water (sugar cane can grow there)
     */
    record Layout(List<BlockPos> carve, List<Water> water, List<BlockPos> rim, List<BlockPos> plantSpots, Set<BlockPos> besideWater) {
    }

    /**
     * Whether the column at x, z stands at the foot of a wall: within {@link #REACH} blocks against {@code away} the
     * terrain rises at least {@code wallHeight}.
     */
    static boolean isWallFoot(Terrain terrain, int x, int z, Direction away, int wallHeight) {
        int floorY = terrain.surface(x, z);
        for (int k = 1; k <= REACH; k++) {
            if (terrain.surface(x - away.getStepX() * k, z - away.getStepZ() * k) >= floorY + wallHeight) {
                return true;
            }
        }
        return false;
    }

    /**
     * The layout for the wall foot at column {@code x, z}, with the wall on the side opposite {@code away} (the stream
     * runs towards {@code away}), or empty where it does not fit.
     * <p>
     * Canyon walls flatten out towards the floor, so the source sits in the nearest column behind the foot (at most
     * {@link #REACH} away) that rises above it, and a cleft one block wide is cut from there to the foot: the column in
     * front of the source down to the floor (the fall hangs in it), the columns between open to the sky. The trench
     * starts under the fall and runs out through the cleft onto the floor.
     */
    static Optional<Layout> build(Terrain terrain, int minX, int minZ, int x, int z, Direction away, Params params, int wallHeight, int minWaterY,
                                  int maxFloorY) {
        int floorY = terrain.surface(x, z);
        if (floorY < minWaterY || floorY > maxFloorY || !terrain.floor(x, floorY, z) || !terrain.air(x, floorY + 1, z)
                || !isWallFoot(terrain, x, z, away, wallHeight)) {
            return Optional.empty();
        }
        int sourceY = floorY + params.springHeight();
        int depth = 0;
        for (int k = 1; k <= REACH && depth == 0; k++) {
            if (terrain.surface(x - away.getStepX() * k, z - away.getStepZ() * k) > sourceY) {
                depth = k;
            }
        }
        if (depth == 0 || params.streamLength() < depth) {
            return Optional.empty();
        }
        for (int k = 1; k <= OPEN_AHEAD; k++) {
            if (terrain.surface(x + away.getStepX() * k, z + away.getStepZ() * k) > floorY + 2) {
                return Optional.empty();
            }
        }
        BlockPos foot = new BlockPos(x, floorY, z);
        BlockPos fallColumn = foot.relative(away, -(depth - 1));

        // The cleft: the fall column up to the source, the columns between it and the foot up to their surface.
        Set<BlockPos> carve = new LinkedHashSet<>();
        for (int i = 1; i < depth; i++) {
            BlockPos column = foot.relative(away, -i);
            int top = i == depth - 1 ? sourceY : terrain.surface(column.getX(), column.getZ());
            for (int y = floorY + 1; y <= top; y++) {
                carve.add(column.atY(y));
            }
        }

        // Every water block in chain order, with the links each may have to other water blocks.
        Map<BlockPos, Kind> kinds = new LinkedHashMap<>();
        Set<Link> links = new HashSet<>();
        BlockPos source = foot.relative(away, -depth).atY(sourceY);
        kinds.put(source, Kind.SOURCE);
        BlockPos previous = source;
        for (int y = sourceY; y > floorY; y--) {
            BlockPos fall = fallColumn.atY(y);
            kinds.put(fall, y == sourceY ? Kind.FLOWING : Kind.FALLING);
            link(links, previous, fall);
            previous = fall;
        }
        // A bend only out on the floor, not inside the cleft.
        int bendAt = params.bendAt() >= depth - 1 ? params.bendAt() : 0;
        List<BlockPos> trench = new ArrayList<>();
        Direction step = away;
        BlockPos cell = fallColumn;
        for (int i = 0; i < params.streamLength(); i++) {
            if (i > 0) {
                if (bendAt > 0 && i == bendAt + 1) {
                    step = params.bendLeft() ? away.getCounterClockWise() : away.getClockWise();
                }
                cell = cell.relative(step);
            }
            trench.add(cell);
            kinds.put(cell, i == 0 ? Kind.FALLING : Kind.FLOWING);
            link(links, previous, cell);
            previous = cell;
        }
        Direction across = step.getClockWise();
        BlockPos end = trench.get(trench.size() - 1);
        List<BlockPos> poolTop = new ArrayList<>();
        for (int a = 1; a <= params.poolLength(); a++) {
            for (int b = 0; b < params.poolWidth(); b++) {
                poolTop.add(end.relative(step, a).relative(across, b - params.poolShift()));
            }
        }
        if (!poolTop.contains(end.relative(step))) {
            return Optional.empty();
        }
        link(links, end, end.relative(step));
        for (int layer = 0; layer < params.poolDepth(); layer++) {
            for (BlockPos top : poolTop) {
                BlockPos pos = top.below(layer);
                if (kinds.put(pos, Kind.POOL) != null) {
                    return Optional.empty();
                }
                for (BlockPos other : poolTop) {
                    if (top.distManhattan(other) == 1) {
                        link(links, pos, other.below(layer));
                    }
                }
                if (layer > 0) {
                    link(links, pos, top.below(layer - 1));
                }
            }
        }
        if (kinds.size() != 1 + params.springHeight() + params.streamLength() + poolTop.size() * params.poolDepth()) {
            return Optional.empty();
        }

        Terrain cut = carved(terrain, carve);
        if (!fits(terrain, cut, minX, minZ, kinds, links, source, carve, trench.subList(0, depth - 1),
                trench.subList(depth - 1, trench.size()), poolTop, floorY, params)) {
            return Optional.empty();
        }
        Optional<List<Water>> water = settle(cut, kinds);
        if (water.isEmpty()) {
            return Optional.empty();
        }

        // Rim: the floor ring around the pool (corners too), outside the trench; plants on it where there is air.
        List<BlockPos> rim = new ArrayList<>();
        List<BlockPos> spots = new ArrayList<>();
        Set<BlockPos> besideWater = new HashSet<>();
        Set<BlockPos> ring = new HashSet<>();
        for (BlockPos top : poolTop) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    BlockPos pos = top.offset(dx, 0, dz);
                    if (!kinds.containsKey(pos) && inside(minX, minZ, pos) && ring.add(pos)
                            && terrain.floor(pos.getX(), floorY, pos.getZ()) && terrain.air(pos.getX(), floorY + 1, pos.getZ())) {
                        rim.add(pos);
                        spots.add(pos.above());
                        for (Direction side : Direction.Plane.HORIZONTAL) {
                            if (kinds.containsKey(pos.relative(side))) {
                                besideWater.add(pos.above());
                            }
                        }
                    }
                }
            }
        }
        // The fall column fills with water; only the rest of the cleft stays air.
        carve.removeAll(kinds.keySet());
        return Optional.of(new Layout(List.copyOf(carve), water.get(), rim, spots, besideWater));
    }

    /** The terrain with the cleft cut out (air). */
    private static Terrain carved(Terrain terrain, Set<BlockPos> carve) {
        return new Terrain() {
            @Override
            public int surface(int x, int z) {
                return terrain.surface(x, z);
            }

            @Override
            public boolean solid(int x, int y, int z) {
                return !carve.contains(new BlockPos(x, y, z)) && terrain.solid(x, y, z);
            }

            @Override
            public boolean floor(int x, int y, int z) {
                return !carve.contains(new BlockPos(x, y, z)) && terrain.floor(x, y, z);
            }

            @Override
            public boolean air(int x, int y, int z) {
                return carve.contains(new BlockPos(x, y, z)) || terrain.air(x, y, z);
            }

            @Override
            public boolean gravity(int x, int y, int z) {
                return !carve.contains(new BlockPos(x, y, z)) && terrain.gravity(x, y, z);
            }
        };
    }

    /**
     * All blocks inside the chunk with one block to spare, every water block held in by terrain or its chain.
     * {@code terrain} is the world as it is, {@code cut} the world with the cleft cut out.
     */
    private static boolean fits(Terrain terrain, Terrain cut, int minX, int minZ, Map<BlockPos, Kind> kinds, Set<Link> links,
                                BlockPos source, Set<BlockPos> carve, List<BlockPos> inCleft, List<BlockPos> onFloor,
                                List<BlockPos> poolTop, int floorY, Params params) {
        for (BlockPos pos : kinds.keySet()) {
            if (!inside(minX, minZ, pos)) {
                return false;
            }
        }
        // The cleft: cut through terrain or air only (no water, no plants), with nothing loose left hanging over it.
        for (BlockPos pos : carve) {
            if (!inside(minX, minZ, pos)
                    || !terrain.solid(pos.getX(), pos.getY(), pos.getZ()) && !terrain.air(pos.getX(), pos.getY(), pos.getZ())) {
                return false;
            }
            if (!carve.contains(pos.above()) && cut.gravity(pos.getX(), pos.getY() + 1, pos.getZ())) {
                return false;
            }
        }
        // The source: wall all around except in front, and no sand above that could slide into the fall.
        for (Direction side : Direction.values()) {
            BlockPos next = source.relative(side);
            if (!kinds.containsKey(next) && !cut.solid(next.getX(), next.getY(), next.getZ())) {
                return false;
            }
        }
        if (cut.gravity(source.getX(), source.getY() + 1, source.getZ())) {
            return false;
        }
        // The fall: open air in front of the source (the cleft is cut already).
        BlockPos fall = inCleft.isEmpty() ? onFloor.get(0) : inCleft.get(0);
        for (int y = floorY + 1; y <= source.getY(); y++) {
            if (!cut.air(fall.getX(), y, fall.getZ())) {
                return false;
            }
        }
        // In the cleft the trench is dug in whatever the wall foot is made of; out on the floor trench and pool are dug
        // in flat floor with air above.
        for (BlockPos pos : inCleft) {
            if (!terrain.solid(pos.getX(), floorY, pos.getZ())) {
                return false;
            }
        }
        List<BlockPos> dug = new ArrayList<>(onFloor);
        dug.addAll(poolTop);
        for (BlockPos pos : dug) {
            if (terrain.surface(pos.getX(), pos.getZ()) != floorY || !terrain.floor(pos.getX(), floorY, pos.getZ())
                    || !terrain.air(pos.getX(), floorY + 1, pos.getZ())) {
                return false;
            }
        }
        for (BlockPos top : poolTop) {
            for (int layer = 1; layer < params.poolDepth(); layer++) {
                if (!terrain.solid(top.getX(), floorY - layer, top.getZ())) {
                    return false;
                }
            }
        }
        // Held in: beside every water block (but the fall) only chain neighbours or solid terrain; solid ground below.
        for (BlockPos pos : kinds.keySet()) {
            if (pos.getX() == fall.getX() && pos.getZ() == fall.getZ() && pos.getY() > floorY) {
                continue;
            }
            for (Direction side : Direction.Plane.HORIZONTAL) {
                BlockPos next = pos.relative(side);
                if (kinds.containsKey(next) ? !links.contains(new Link(pos, next)) : !cut.solid(next.getX(), next.getY(), next.getZ())) {
                    return false;
                }
            }
            BlockPos below = pos.below();
            if (!kinds.containsKey(below) && (!cut.solid(below.getX(), below.getY(), below.getZ())
                    || cut.gravity(below.getX(), below.getY(), below.getZ())
                    && !cut.solid(below.getX(), below.getY() - 1, below.getZ()))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Vanilla's settled state of the chain: sources stay, every other block becomes what {@code getNewLiquid} gives
     * it, repeated until nothing changes. Empty if a block would dry up or turn into a source.
     */
    static Optional<List<Water>> settle(Terrain terrain, Map<BlockPos, Kind> kinds) {
        Map<BlockPos, Integer> amount = new LinkedHashMap<>();
        Map<BlockPos, Boolean> falling = new LinkedHashMap<>();
        for (Map.Entry<BlockPos, Kind> entry : kinds.entrySet()) {
            boolean source = entry.getValue() == Kind.SOURCE || entry.getValue() == Kind.POOL;
            amount.put(entry.getKey(), source ? SOURCE : 0);
            falling.put(entry.getKey(), false);
        }
        boolean changed = true;
        for (int round = 0; changed; round++) {
            if (round > 64) {
                return Optional.empty();
            }
            changed = false;
            for (BlockPos pos : kinds.keySet()) {
                Kind kind = kinds.get(pos);
                if (kind == Kind.SOURCE || kind == Kind.POOL) {
                    continue;
                }
                int highest = 0;
                int sources = 0;
                for (Direction side : Direction.Plane.HORIZONTAL) {
                    BlockPos next = pos.relative(side);
                    Kind neighbour = kinds.get(next);
                    if (neighbour != null && amount.get(next) > 0) {
                        if (neighbour == Kind.SOURCE || neighbour == Kind.POOL) {
                            sources++;
                        }
                        highest = Math.max(highest, amount.get(next));
                    }
                }
                BlockPos below = pos.below();
                Kind under = kinds.get(below);
                if (sources >= 2 && (under == null ? terrain.solid(below.getX(), below.getY(), below.getZ()) : under == Kind.SOURCE || under == Kind.POOL)) {
                    return Optional.empty();
                }
                BlockPos above = pos.above();
                boolean fromAbove = kinds.containsKey(above) && amount.get(above) > 0;
                int next = fromAbove ? SOURCE : Math.max(0, highest - 1);
                if (next != amount.get(pos) || fromAbove != falling.get(pos)) {
                    amount.put(pos, next);
                    falling.put(pos, fromAbove);
                    changed = true;
                }
            }
        }
        List<Water> water = new ArrayList<>();
        for (BlockPos pos : kinds.keySet()) {
            Kind kind = kinds.get(pos);
            int a = amount.get(pos);
            if (a <= 0) {
                return Optional.empty();
            }
            Kind settled = kind == Kind.SOURCE || kind == Kind.POOL ? kind : falling.get(pos) ? Kind.FALLING : Kind.FLOWING;
            water.add(new Water(pos, settled, a));
        }
        return Optional.of(water);
    }

    private static boolean inside(int minX, int minZ, BlockPos pos) {
        int x = pos.getX() - minX;
        int z = pos.getZ() - minZ;
        return x >= 1 && x <= 14 && z >= 1 && z <= 14;
    }

    private static void link(Set<Link> links, BlockPos a, BlockPos b) {
        links.add(new Link(a, b));
        links.add(new Link(b, a));
    }

    /** Two water blocks of the chain that may touch. */
    private record Link(BlockPos from, BlockPos to) {
    }
}
