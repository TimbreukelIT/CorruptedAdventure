package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What package 74 adds to the islands of ring 4, behind a few hooks in {@link IslandField}: the world trees
 * ({@link WorldTrees}, {@link WorldTreeShape}), the sky lakes ({@link SkyLakes}) and the island pairs with their rock
 * arches ({@link IslandArches}), and (sparring 2026-10-20, B2-3) the floating debris round the big islands
 * ({@link IslandDebris}). Per column ({@link #fill}) it adds a tree's blocks, cuts a sky lake's basin and adds an
 * arch's rock and the debris; per cell of the island grid it keeps what reaches into the cell ({@link Cell}), so a column without any
 * of them costs one lookup.
 * <p>
 * Pure: everything is a function of the field's seed, relief and ring layout, decided once per world in
 * {@link Memo} (shared by all copies of one wired field, like {@link IslandField.Decisions}).
 */
final class IslandLandmarks {
    private final IslandField field;
    private final Memo memo;

    IslandLandmarks(IslandField field, Memo memo) {
        this.field = field;
        this.memo = memo;
    }

    /** Per-world decisions and remembered results of 74. */
    static final class Memo {
        volatile List<WorldTreeShape> trees;
        final Map<IslandShape, Optional<SkyLakes.Lake>> lakes = new ConcurrentHashMap<>();
        final Map<IslandShape, List<IslandArches.Pair>> candidatePairs = new ConcurrentHashMap<>();
        final Map<IslandShape, List<IslandArches.Pair>> archCandidates = new ConcurrentHashMap<>();
        final Map<IslandArches.Pair, Boolean> archKept = new ConcurrentHashMap<>();
        final Map<IslandShape, List<IslandArches.Pair>> pairs = new ConcurrentHashMap<>();
        final Map<IslandShape, List<IslandDebris.Piece>> debris = new ConcurrentHashMap<>();
        final KeyedSlots<Cell> cells = new KeyedSlots<>(256);
    }

    /** What reaches into one cell of the island grid: trees, lakes (incl. rim), arches, debris. */
    record Cell(List<WorldTreeShape> trees, List<SkyLakes.Lake> lakes, List<IslandArches.Arch> arches,
                List<IslandDebris.Piece> debris) {
        static final Cell EMPTY = new Cell(List.of(), List.of(), List.of(), List.of());

        boolean empty() {
            return trees.isEmpty() && lakes.isEmpty() && arches.isEmpty() && debris.isEmpty();
        }
    }

    // ---------------------------------------------------------------- decisions

    /** The world's trees, decided once. */
    List<WorldTreeShape> trees() {
        List<WorldTreeShape> known = memo.trees;
        if (known == null) {
            // Review 74 #5: once, not once per worker thread arriving at the start of a world.
            synchronized (memo) {
                known = memo.trees;
                if (known == null) {
                    known = WorldTrees.place(field);
                    memo.trees = known;
                }
            }
        }
        return known;
    }

    /** The sky lake of an island, decided once (null: none). */
    @Nullable
    SkyLakes.Lake lake(IslandShape island) {
        return field.decisions().once(memo.lakes, island, () -> Optional.ofNullable(SkyLakes.decide(field, island))).orElse(null);
    }

    /** The pairs an island starts, each with the arch it keeps ({@link IslandArches#archKept}), decided once. */
    List<IslandArches.Pair> pairs(IslandShape island) {
        return field.decisions().once(memo.pairs, island, () -> candidatePairs(island).stream()
                .map(pair -> pair.arch() == null || archKept(pair) ? pair : pair.withArch(null))
                .filter(pair -> !pair.archOnly() || pair.arch() != null)
                .toList());
    }

    /** The pairs an island starts with their candidate arches ({@link IslandArches#pairsOf}), decided once. */
    List<IslandArches.Pair> candidatePairs(IslandShape island) {
        return field.decisions().once(memo.candidatePairs, island, () -> IslandArches.pairsOf(field, island));
    }

    /** The pairs with a candidate arch that an island is part of, decided once. */
    List<IslandArches.Pair> archCandidates(IslandShape island) {
        return field.decisions().once(memo.archCandidates, island, () -> IslandArches.archCandidatesOf(field, island));
    }

    /** Whether a candidate arch stays ({@link IslandArches#archKept}), decided once. */
    boolean archKept(IslandArches.Pair pair) {
        return field.decisions().once(memo.archKept, pair, () -> IslandArches.archKept(field, pair));
    }

    /** The floating debris round an island ({@link IslandDebris#of}), decided once. */
    List<IslandDebris.Piece> debris(IslandShape island) {
        return field.decisions().once(memo.debris, island, () -> IslandDebris.of(field, island));
    }

    /**
     * Hook of {@link IslandField#candidates}: a cell's candidates without those that would grow into a world tree, plus
     * the island of a tree whose centre lies in the cell.
     */
    List<IslandShape> withTrees(int i, int j, List<IslandShape> candidates) {
        List<WorldTreeShape> trees = trees();
        if (trees.isEmpty()) {
            return candidates;
        }
        int cellSize = field.settings().cellSize();
        List<IslandShape> kept = new ArrayList<>(candidates.size() + 1);
        boolean changed = false;
        for (IslandShape candidate : candidates) {
            boolean clashes = false;
            for (WorldTreeShape tree : trees) {
                if (WorldTrees.clashes(tree, candidate)) {
                    clashes = true;
                    break;
                }
            }
            if (clashes) {
                changed = true;
            } else {
                kept.add(candidate);
            }
        }
        for (WorldTreeShape tree : trees) {
            if (WorldTrees.inCell(tree, cellSize, i, j)) {
                kept.add(tree.island);
                changed = true;
            }
        }
        return changed ? List.copyOf(kept) : candidates;
    }

    /** Hook of {@link IslandField#details}: a tree's island has its falls beside its roots. */
    IslandField.Details treeDetails(IslandShape island) {
        WorldTreeShape tree = WorldTrees.of(trees(), island);
        if (tree == null) {
            throw new IllegalStateException("no world tree stands on " + island);
        }
        return WorldTrees.details(field, tree);
    }

    // ---------------------------------------------------------------- per cell and column

    /** What reaches into the grid cell of (x, z). */
    private Cell cell(int x, int z) {
        int size = field.settings().cellSize();
        int i = Math.floorDiv(x, size);
        int j = Math.floorDiv(z, size);
        return memo.cells.get(KeyedSlots.key(i, j), key -> cellOf(i, j, size));
    }

    private Cell cellOf(int i, int j, int size) {
        int minX = i * size;
        int minZ = j * size;
        int maxX = minX + size - 1;
        int maxZ = minZ + size - 1;
        List<WorldTreeShape> trees = new ArrayList<>();
        for (WorldTreeShape tree : trees()) {
            if (tree.reaches(minX, minZ, maxX, maxZ)) {
                trees.add(tree);
            }
        }
        List<SkyLakes.Lake> lakes = new ArrayList<>();
        List<IslandArches.Arch> arches = new ArrayList<>();
        List<IslandDebris.Piece> debris = new ArrayList<>();
        // An arch reaches from its first island's edge past the second's: within an island's reach and a gap of both.
        int margin = Mth.ceil(IslandField.MAX_REACH + IslandArches.MAX_GAP);
        for (IslandShape island : field.shapesWithin(minX - margin, minZ - margin, maxX + margin, maxZ + margin)) {
            SkyLakes.Lake lake = lake(island);
            if (lake != null && lake.maxX() + 4 >= minX && lake.minX() - 4 <= maxX && lake.maxZ() + 4 >= minZ && lake.minZ() - 4 <= maxZ) {
                lakes.add(lake);
            }
            for (IslandArches.Pair pair : pairs(island)) {
                IslandArches.Arch arch = pair.arch();
                if (arch != null && arch.maxX() >= minX && arch.minX() <= maxX && arch.maxZ() >= minZ && arch.minZ() <= maxZ) {
                    arches.add(arch);
                }
            }
            // Debris lies within IslandDebris.MAX_OUT of its island's reach, well inside the margin.
            for (IslandDebris.Piece piece : debris(island)) {
                if (piece.maxX() >= minX && piece.minX() <= maxX && piece.maxZ() >= minZ && piece.minZ() <= maxZ) {
                    debris.add(piece);
                }
            }
        }
        if (trees.isEmpty() && lakes.isEmpty() && arches.isEmpty() && debris.isEmpty()) {
            return Cell.EMPTY;
        }
        return new Cell(List.copyOf(trees), List.copyOf(lakes), List.copyOf(arches), List.copyOf(debris));
    }

    /**
     * Hook at the end of {@link IslandField}'s island pass of a column: the tree's blocks in it, a sky lake's basin cut
     * into its island (and the water), an arch's rock.
     */
    void fill(IslandField.Column c) {
        Cell cell = cell(c.x, c.z);
        if (cell.empty()) {
            return;
        }
        for (WorldTreeShape tree : cell.trees()) {
            WorldTreeShape.Slice slice = tree.slice(c.x, c.z);
            if (slice != null) {
                c.tree = slice;
            }
        }
        for (SkyLakes.Lake lake : cell.lakes()) {
            // The rim band reaches past the basin's box by a few blocks.
            if (c.x >= lake.minX() - 4 && c.x <= lake.maxX() + 4 && c.z >= lake.minZ() - 4 && c.z <= lake.maxZ() + 4) {
                SkyLakes.apply(lake, c);
            }
        }
        for (IslandArches.Arch arch : cell.arches()) {
            int[] interval = arch.interval(c.x, c.z);
            if (interval != null) {
                c.addIsland(interval[0], interval[1]);
            }
        }
        for (IslandDebris.Piece piece : cell.debris()) {
            int[] interval = piece.interval(c.x, c.z);
            if (interval != null) {
                c.addIsland(interval[0], interval[1]);
            }
        }
    }

    // ---------------------------------------------------------------- read access

    /** Whether a world tree's bounding square holds the column (no column computed). */
    boolean nearTree(int x, int z) {
        for (WorldTreeShape tree : trees()) {
            if (tree.touches(x, z)) {
                return true;
            }
        }
        return false;
    }

    /** Whether a piece of debris ({@link IslandDebris}) has a block in the column. */
    boolean inDebris(int x, int z) {
        for (IslandDebris.Piece piece : cell(x, z).debris()) {
            if (piece.interval(x, z) != null) {
                return true;
            }
        }
        return false;
    }

    List<IslandField.WorldTree> worldTrees(int minX, int minZ, int maxX, int maxZ) {
        List<IslandField.WorldTree> found = new ArrayList<>();
        for (WorldTreeShape tree : trees()) {
            if (tree.reaches(minX, minZ, maxX, maxZ)) {
                found.add(tree.view());
            }
        }
        return found;
    }

    List<IslandField.IslandPair> islandPairs(int minX, int minZ, int maxX, int maxZ) {
        int margin = Mth.ceil(IslandField.MAX_REACH + IslandArches.MAX_GAP);
        List<IslandField.IslandPair> found = new ArrayList<>();
        for (IslandShape island : field.shapesWithin(minX - margin, minZ - margin, maxX + margin, maxZ + margin)) {
            for (IslandArches.Pair pair : pairs(island)) {
                if (pair.reaches(minX, minZ, maxX, maxZ)) {
                    found.add(pair.view());
                }
            }
        }
        return found;
    }

    List<IslandField.SkyLake> skyLakes(int minX, int minZ, int maxX, int maxZ) {
        List<IslandField.SkyLake> found = new ArrayList<>();
        for (IslandShape island : field.shapesWithin(minX, minZ, maxX, maxZ)) {
            SkyLakes.Lake lake = lake(island);
            if (lake != null && lake.maxX() >= minX && lake.minX() <= maxX && lake.maxZ() >= minZ && lake.minZ() <= maxZ) {
                found.add(lake.view());
            }
        }
        return found;
    }
}
