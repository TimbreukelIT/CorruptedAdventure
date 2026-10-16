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
 * arches ({@link IslandArches}). Per column ({@link #fill}) it adds a tree's blocks, cuts a sky lake's basin and adds an
 * arch's rock; per cell of the island grid it keeps what reaches into the cell ({@link Cell}), so a column without any
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
        final Map<IslandShape, List<IslandArches.Pair>> pairs = new ConcurrentHashMap<>();
        final KeyedSlots<Cell> cells = new KeyedSlots<>(256);
    }

    /** What reaches into one cell of the island grid: trees, lakes (incl. rim), arches. */
    record Cell(List<WorldTreeShape> trees, List<SkyLakes.Lake> lakes, List<IslandArches.Arch> arches) {
        static final Cell EMPTY = new Cell(List.of(), List.of(), List.of());

        boolean empty() {
            return trees.isEmpty() && lakes.isEmpty() && arches.isEmpty();
        }
    }

    // ---------------------------------------------------------------- decisions

    /** The world's trees, decided once. */
    List<WorldTreeShape> trees() {
        List<WorldTreeShape> known = memo.trees;
        if (known == null) {
            // Pure: racing threads compute the same list.
            known = WorldTrees.place(field);
            memo.trees = known;
        }
        return known;
    }

    /** The sky lake of an island, decided once (null: none). */
    @Nullable
    SkyLakes.Lake lake(IslandShape island) {
        return field.decisions().once(memo.lakes, island, () -> Optional.ofNullable(SkyLakes.decide(field, island))).orElse(null);
    }

    /** The pairs an island starts, decided once. */
    List<IslandArches.Pair> pairs(IslandShape island) {
        return field.decisions().once(memo.pairs, island, () -> IslandArches.pairsOf(field, island));
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
        for (IslandShape candidate : candidates) {
            boolean clashes = false;
            for (WorldTreeShape tree : trees) {
                if (WorldTrees.clashes(tree, candidate)) {
                    clashes = true;
                    break;
                }
            }
            if (!clashes) {
                kept.add(candidate);
            }
        }
        for (WorldTreeShape tree : trees) {
            if (WorldTrees.inCell(tree, cellSize, i, j)) {
                kept.add(tree.island);
            }
        }
        return kept.size() == candidates.size() && kept.equals(candidates) ? candidates : List.copyOf(kept);
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
        }
        if (trees.isEmpty() && lakes.isEmpty() && arches.isEmpty()) {
            return Cell.EMPTY;
        }
        return new Cell(List.copyOf(trees), List.copyOf(lakes), List.copyOf(arches));
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
    }

    // ---------------------------------------------------------------- read access

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
