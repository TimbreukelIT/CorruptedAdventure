package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** The caches of {@link VolcanicField} (split out, 39d): only remembered results, never decisions of their own. */
final class VolcanicCaches {
    private VolcanicCaches() {
    }

    /**
     * Per-cell decisions (volcanoes, island cells, island checks, rivers, cinder cones, bones): pure functions of the
     * seed and the cell, shared by all copies of one wired field (the noise chunk makes a copy per chunk), so they are
     * made once per world, not once per chunk. Only a cache; a map is cleared when it grows past {@link #LIMIT} entries.
     */
    static final class Decisions {
        static final int LIMIT = 100_000;
        final Map<Long, Optional<Volcano>> volcanoes = new ConcurrentHashMap<>();
        final Map<Long, List<Volcano.Island>> channelIslands = new ConcurrentHashMap<>();
        final Map<Volcano.Island, Boolean> islands = new ConcurrentHashMap<>();
        final Map<Long, Double> initialHeights = new ConcurrentHashMap<>();
        final Map<Long, java.util.concurrent.FutureTask<List<LavaRivers.River>>> rivers = new ConcurrentHashMap<>();
        final Map<Long, java.util.concurrent.FutureTask<LavaRivers.Tile>> riverTiles = new ConcurrentHashMap<>();
        final Map<Long, Optional<CinderCones.Cone>> cinders = new ConcurrentHashMap<>();
        final Map<Long, Optional<TitanBones.Rib>> ribs = new ConcurrentHashMap<>();
        final Map<Long, Optional<TitanBones.Cage>> cages = new ConcurrentHashMap<>();
        final Map<Long, Optional<Landmarks.Needle>> needles = new ConcurrentHashMap<>();
        final Map<Long, Optional<Landmarks.Arch>> riftArches = new ConcurrentHashMap<>();
        final Map<Long, Optional<Landmarks.FloatingVolcano>> floating = new ConcurrentHashMap<>();
        final Map<Long, Optional<Landmarks.Abyss>> abysses = new ConcurrentHashMap<>();
        volatile Long salt;

        <K, V> void remember(Map<K, V> map, K key, V value) {
            if (map.size() > LIMIT) {
                map.clear();
            }
            map.putIfAbsent(key, value);
        }
    }

    /** The decisions of the 3 x 3 cells around one cell (volcano cells or island cells); only a cache. */
    static final class Neighbourhood {
        boolean filled;
        int ci;
        int cj;
        final Volcano[] volcanoes = new Volcano[9];
        @SuppressWarnings("unchecked")
        final List<Volcano.Island>[] islands = new List[9];

        Neighbourhood around(VolcanicField field, int i, int j) {
            if (!filled || i != ci || j != cj) {
                for (int k = 0; k < 9; k++) {
                    volcanoes[k] = field.volcano(i + k / 3 - 1, j + k % 3 - 1);
                }
                ci = i;
                cj = j;
                filled = true;
            }
            return this;
        }

        Neighbourhood aroundIslands(VolcanicField field, int i, int j) {
            if (!filled || i != ci || j != cj) {
                for (int k = 0; k < 9; k++) {
                    islands[k] = field.channelIslands(i + k / 3 - 1, j + k % 3 - 1);
                }
                ci = i;
                cj = j;
                filled = true;
            }
            return this;
        }
    }

    /** The cinder cones of the 3 x 3 cells around one cell; only a cache. */
    static final class CinderNeighbourhood {
        boolean filled;
        int ci;
        int cj;
        final CinderCones.Cone[] cones = new CinderCones.Cone[9];

        CinderNeighbourhood around(VolcanicField field, int i, int j) {
            if (!filled || i != ci || j != cj) {
                for (int k = 0; k < 9; k++) {
                    cones[k] = field.cinder(i + k / 3 - 1, j + k % 3 - 1);
                }
                ci = i;
                cj = j;
                filled = true;
            }
            return this;
        }
    }

    /** The decision of the last cell asked for (per kind, per thread); only a cache. */
    static final class CellMemo<T> {
        boolean filled;
        int i;
        int j;
        @Nullable
        T value;
    }

    /** The river tile of the last column; only a cache. */
    static final class RiverTile {
        boolean filled;
        int ti;
        int tj;
        LavaRivers.Tile tile;
    }
}
