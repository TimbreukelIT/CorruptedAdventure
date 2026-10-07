package nl.timtoys.corruptedadventure.command.terrainmap;

import java.awt.image.BufferedImage;
import java.util.Map;

/** One terrain map split into units (rows of a top view, columns of a section) that worker threads sample in any order. */
interface MapWork {
    int units();

    int columnsPerUnit();

    /** Fills the samples of one unit; called once per unit, from any worker thread, with that thread's sampler. */
    void sample(TerrainSampler sampler, int unit);

    /** After all units: the images by file name (without directory). */
    Map<String, BufferedImage> render();
}
