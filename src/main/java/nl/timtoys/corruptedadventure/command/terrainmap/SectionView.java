package nl.timtoys.corruptedadventure.command.terrainmap;

import java.util.List;

/**
 * The samples of a vertical section: per column of the line the material of every Y from {@code maxY} down to
 * {@code minY}, plus the biome at the ground and the ring.
 *
 * @param cells  {@code samples × (maxY - minY + 1)} material codes, column by column, top first
 * @param biome  index into {@code biomeIds} per column
 * @param ring   {@code Ring.ordinal()} per column, or {@code -1} outside a ring world
 */
record SectionView(int samples, int minY, int maxY, int seaLevel, byte[] cells, int[] biome, List<String> biomeIds,
                   byte[] ring) {
    static final byte STONE = 0;
    static final byte SKY = 1;
    static final byte CAVE = 2;
    static final byte WATER = 3;
    static final byte LAVA = 4;

    int heightBlocks() {
        return maxY - minY + 1;
    }

    byte cell(int sample, int y) {
        return cells[sample * heightBlocks() + (maxY - y)];
    }

    /**
     * Materials of one column, top first. Open space above the highest solid block is sky, below it cave. Fluid as
     * the generator places it without local aquifers: with aquifers (overworld) only in the open run above the ground
     * (seas, lakes at sea level) and in the lava layer; without (Nether) in all open space below the fluid level.
     * Known gap: open water under an overhang (arch, hoodoo cap, floating island over a lake) counts as cave, where the
     * real aquifer usually fills it.
     *
     * @param solid per Y, top first
     * @param fluid per Y, top first: {@link TerrainSampler} fluid code of the generator's global fluid there
     */
    static byte[] classify(boolean[] solid, byte[] fluid, boolean aquifers) {
        byte[] cells = new byte[solid.length];
        boolean belowGround = false;
        for (int i = 0; i < solid.length; i++) {
            if (solid[i]) {
                cells[i] = STONE;
                belowGround = true;
                continue;
            }
            byte fluidHere = fluid[i];
            boolean fluidCounts = !aquifers || !belowGround || fluidHere == TerrainSampler.LAVA;
            if (fluidCounts && fluidHere == TerrainSampler.WATER) {
                cells[i] = WATER;
            } else if (fluidCounts && fluidHere == TerrainSampler.LAVA) {
                cells[i] = LAVA;
            } else {
                cells[i] = belowGround ? CAVE : SKY;
            }
        }
        return cells;
    }
}
