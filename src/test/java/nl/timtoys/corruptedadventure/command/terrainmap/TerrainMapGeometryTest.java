package nl.timtoys.corruptedadventure.command.terrainmap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TerrainMapGeometryTest {
    @Test
    void areaIsCappedAt2048PixelsPerSide() {
        assertNull(MapArea.validate(4096, 4));
        assertNotNull(MapArea.validate(4100, 4));
        assertNotNull(MapArea.validate(2, 4));
        assertNotNull(MapArea.validate(100, 0));
        assertNotNull(MapArea.validate(100, 65));
    }

    @Test
    void areaPixelsStepByTheScaleFromTheNorthWestCorner() {
        MapArea area = new MapArea(2500, -100, 64, 4);
        assertEquals(32, area.size());
        assertEquals(1024, area.columns());
        assertEquals(2436, area.blockX(0));
        assertEquals(2436 + 31 * 4, area.blockX(31));
        assertEquals(-164, area.blockZ(0));
    }

    @Test
    void sectionHasOneColumnPerBlockUpTo2048() {
        SectionLine line = new SectionLine(0, 0, 100, 0);
        assertEquals(101, line.samples());
        assertEquals(1.0, line.step(), 1.0e-9);
        assertEquals(0, line.blockX(0));
        assertEquals(100, line.blockX(100));
        assertEquals(0, line.blockZ(50));
    }

    @Test
    void longSectionsSpreadTheColumnsAndKeepBothEnds() {
        SectionLine line = new SectionLine(-5000, 300, 5000, -300);
        assertEquals(SectionLine.MAX_SAMPLES, line.samples());
        assertEquals(-5000, line.blockX(0));
        assertEquals(300, line.blockZ(0));
        assertEquals(5000, line.blockX(line.samples() - 1));
        assertEquals(-300, line.blockZ(line.samples() - 1));
    }

    @Test
    void sectionNeedsTwoDifferentEndsWithinTheLimit() {
        assertNotNull(SectionLine.validate(10, 10, 10, 10));
        assertNotNull(SectionLine.validate(0, 0, 20_000, 0));
        assertNull(SectionLine.validate(0, 0, 16_000, 0));
    }

    @Test
    void workerThreadsAreAQuarterOfTheCoresBetweenOneAndFour() {
        assertEquals(1, TerrainMapJob.workerThreads(1));
        assertEquals(2, TerrainMapJob.workerThreads(8));
        assertEquals(4, TerrainMapJob.workerThreads(32));
    }
}
