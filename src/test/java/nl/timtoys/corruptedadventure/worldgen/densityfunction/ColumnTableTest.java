package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColumnTableTest {
    /** A column remembers where it was filled. */
    private static final class Cell {
        int x;
        int z;
    }

    private static void fill(Cell cell, int x, int z) {
        cell.x = x;
        cell.z = z;
    }

    @Test
    void givesTheRightColumnAndFillsItOncePerWindow() {
        AtomicInteger fills = new AtomicInteger();
        ColumnTable<Cell> table = new ColumnTable<>(Cell::new);
        ColumnTable.Filler<Cell> filler = (cell, x, z) -> {
            fills.incrementAndGet();
            fill(cell, x, z);
        };
        for (int x = 32; x < 48; x++) {
            for (int z = -16; z < 0; z++) {
                Cell cell = table.get(x, z, filler);
                assertEquals(x, cell.x);
                assertEquals(z, cell.z);
            }
        }
        assertEquals(256, fills.get());
        // Again in the same chunk, and the rim around it: no new fills for the chunk itself.
        table.get(40, -8, filler);
        assertEquals(256, fills.get());
        Cell rim = table.get(32 - ColumnTable.MARGIN, -16 - ColumnTable.MARGIN, filler);
        assertEquals(32 - ColumnTable.MARGIN, rim.x);
        assertEquals(257, fills.get());
        table.get(40, -8, filler);
        assertEquals(257, fills.get());
    }

    @Test
    void movesToAnotherChunkAndBack() {
        ColumnTable<Cell> table = new ColumnTable<>(Cell::new);
        for (int[] p : new int[][]{{5, 5}, {1000, -300}, {5, 5}, {-17, 20}, {-1, -1}, {15, 15}}) {
            Cell cell = table.get(p[0], p[1], ColumnTableTest::fill);
            assertEquals(p[0], cell.x);
            assertEquals(p[1], cell.z);
        }
    }

    @Test
    void threadsHaveTheirOwnTables() throws Exception {
        ColumnTable<Cell> table = new ColumnTable<>(Cell::new);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            Future<?>[] tasks = new Future<?>[8];
            for (int t = 0; t < tasks.length; t++) {
                int offset = t * 37;
                tasks[t] = pool.submit(() -> {
                    for (int i = 0; i < 20_000; i++) {
                        int x = offset + i % 97 * 3;
                        int z = -offset + i % 89 * 5;
                        Cell cell = table.get(x, z, ColumnTableTest::fill);
                        assertEquals(x, cell.x);
                        assertEquals(z, cell.z);
                    }
                });
            }
            for (Future<?> task : tasks) {
                task.get();
            }
        } finally {
            pool.shutdown();
        }
    }
}
