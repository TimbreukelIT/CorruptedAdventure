package nl.timtoys.corruptedadventure.command.terrainmap;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Draws a vertical section: one pixel per block vertically, one per sample horizontally. A biome strip on top, Y ticks
 * on the left; dotted white lines every {@value #GRID} blocks, the sea level dashed in cyan, Y
 * {@value MapPalette#CLOUD_Y} in red and ring boundaries as magenta columns.
 */
final class SectionRenderer {
    static final int AXIS = 40;
    static final int STRIP = 8;
    static final int GAP = 2;
    private static final int GRID = 64;

    private SectionRenderer() {
    }

    static BufferedImage render(SectionView view, List<String> header) {
        int top = STRIP + GAP;
        BufferedImage map = new BufferedImage(AXIS + view.samples(), top + view.heightBlocks(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = map.createGraphics();
        try {
            graphics.setColor(new Color(MapPalette.BACKGROUND));
            graphics.fillRect(0, 0, map.getWidth(), map.getHeight());
            for (int y = Math.floorDiv(view.maxY(), GRID) * GRID; y >= view.minY(); y -= GRID) {
                LegendPanel.label(graphics, "Y " + y, 2, Math.min(map.getHeight() - 1, top + (view.maxY() - y) + 4));
            }
        } finally {
            graphics.dispose();
        }
        int[] colours = view.biomeIds().stream().mapToInt(MapPalette::biome).toArray();
        for (int sample = 0; sample < view.samples(); sample++) {
            int x = AXIS + sample;
            for (int row = 0; row < STRIP; row++) {
                map.setRGB(x, row, colours[view.biome()[sample]]);
            }
            byte ring = view.ring()[sample];
            boolean boundary = sample > 0 && ring != TerrainSampler.NO_RING && ring != view.ring()[sample - 1];
            for (int y = view.maxY(); y >= view.minY(); y--) {
                map.setRGB(x, top + (view.maxY() - y), boundary ? MapPalette.RING_LINE_ON_HEIGHT : pixel(view, sample, y));
            }
        }
        LegendPanel legend = new LegendPanel();
        header.forEach(legend::line);
        legend.entry(MapPalette.STONE, "solid (final density > 0)")
                .entry(MapPalette.SKY, "air above the ground")
                .entry(MapPalette.CAVE, "air below the ground")
                .entry(MapPalette.WATER, "water")
                .entry(MapPalette.LAVA, "lava")
                .entry(MapPalette.GUIDE_LINE, "every " + GRID + " blocks")
                .entry(MapPalette.SEA_LEVEL_LINE, "sea level " + view.seaLevel());
        if (view.maxY() >= MapPalette.CLOUD_Y) {
            legend.entry(MapPalette.CLOUD_LINE, "Y " + MapPalette.CLOUD_Y + " (clouds)");
        }
        if (TerrainSampler.hasRings(view.ring())) {
            legend.entry(MapPalette.RING_LINE_ON_HEIGHT, "ring boundary");
        }
        legend.line("");
        legend.line("Top strip: biome at the ground");
        for (int i = 0; i < colours.length; i++) {
            legend.entry(colours[i], TopViewRenderer.shortName(view.biomeIds().get(i)));
        }
        return legend.compose(map);
    }

    static int pixel(SectionView view, int sample, int y) {
        if (y == MapPalette.CLOUD_Y) {
            return MapPalette.CLOUD_LINE;
        }
        if (y == view.seaLevel() && (sample / 4) % 2 == 0) {
            return MapPalette.SEA_LEVEL_LINE;
        }
        if (Math.floorMod(y, GRID) == 0 && sample % 2 == 0) {
            return MapPalette.GUIDE_LINE;
        }
        return switch (view.cell(sample, y)) {
            case SectionView.STONE -> MapPalette.STONE;
            case SectionView.SKY -> MapPalette.SKY;
            case SectionView.WATER -> MapPalette.WATER;
            case SectionView.LAVA -> MapPalette.LAVA;
            default -> MapPalette.CAVE;
        };
    }
}
