package nl.timtoys.corruptedadventure.command.terrainmap;

import java.util.List;

/**
 * The samples of a top-view map, row-major (row = z, column = x), as plain values so the rendering is testable.
 *
 * @param ground   Y of the visible ground per pixel ({@code minY - 1}: nothing; {@code maxY}: no opening below a ceiling)
 * @param fluidTop Y of the fluid surface above the ground, or {@link Integer#MIN_VALUE} for a dry pixel
 * @param fluid    {@link TerrainSampler#WATER}, {@link TerrainSampler#LAVA} or {@link TerrainSampler#NO_FLUID}
 * @param biome    index into {@code biomeIds} per pixel
 * @param ring     {@code Ring.ordinal()} per pixel, or {@code -1} outside a ring world
 */
record TopView(int size, int minY, int maxY, int[] ground, int[] fluidTop, byte[] fluid, int[] biome,
               List<String> biomeIds, byte[] ring) {
    static final int DRY = Integer.MIN_VALUE;

    int index(int column, int row) {
        return row * size + column;
    }
}
