package nl.timtoys.corruptedadventure.command.terrainmap;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Locale;

/**
 * Draws the two top-view maps. Height: colour ramp over the dimension's height, fluid surfaces in blue/orange (darker
 * when deeper), contour lines every {@value MapPalette#CONTOUR_STEP} blocks, Y {@value MapPalette#CLOUD_Y} (clouds) in
 * red, ring boundaries in magenta. Biomes: a fixed colour per biome and the ring boundaries in black.
 */
final class TopViewRenderer {
    private static final double CONTOUR_SHADE = 0.6;

    private TopViewRenderer() {
    }

    static BufferedImage height(TopView view, List<String> header) {
        BufferedImage map = new BufferedImage(view.size(), view.size(), BufferedImage.TYPE_INT_RGB);
        for (int row = 0; row < view.size(); row++) {
            for (int column = 0; column < view.size(); column++) {
                map.setRGB(column, row, heightPixel(view, column, row));
            }
        }
        LegendPanel legend = new LegendPanel();
        header.forEach(legend::line);
        int step = rampStep(view.minY(), view.maxY());
        for (int y = Math.floorDiv(view.maxY(), step) * step; y >= view.minY(); y -= step) {
            legend.entry(MapPalette.height(y, view.minY(), view.maxY()), "Y " + y);
        }
        legend.entry(MapPalette.fluid(false, 0), "water (darker = deeper)")
                .entry(MapPalette.fluid(true, 0), "lava")
                .entry(MapPalette.scale(MapPalette.height(64, view.minY(), view.maxY()), CONTOUR_SHADE),
                        "contour every " + MapPalette.CONTOUR_STEP + " blocks");
        if (view.maxY() >= MapPalette.CLOUD_Y) {
            legend.entry(MapPalette.CLOUD_LINE, "Y " + MapPalette.CLOUD_Y + " (clouds)");
        }
        if (hasRings(view)) {
            legend.entry(MapPalette.RING_LINE_ON_HEIGHT, "ring boundary");
        }
        return legend.compose(map);
    }

    static BufferedImage biomes(TopView view, List<String> header) {
        BufferedImage map = new BufferedImage(view.size(), view.size(), BufferedImage.TYPE_INT_RGB);
        int[] colours = view.biomeIds().stream().mapToInt(MapPalette::biome).toArray();
        long[] counts = new long[colours.length];
        for (int row = 0; row < view.size(); row++) {
            for (int column = 0; column < view.size(); column++) {
                int index = view.index(column, row);
                counts[view.biome()[index]]++;
                map.setRGB(column, row, ringBoundary(view, column, row)
                        ? MapPalette.RING_LINE_ON_BIOMES : colours[view.biome()[index]]);
            }
        }
        LegendPanel legend = new LegendPanel();
        header.forEach(legend::line);
        long total = (long) view.size() * view.size();
        for (int i = 0; i < colours.length; i++) {
            legend.entry(colours[i], String.format(Locale.ROOT, "%s %.1f%%", shortName(view.biomeIds().get(i)),
                    100.0 * counts[i] / total));
        }
        if (hasRings(view)) {
            legend.entry(MapPalette.RING_LINE_ON_BIOMES, "ring boundary");
        }
        return legend.compose(map);
    }

    static int heightPixel(TopView view, int column, int row) {
        if (ringBoundary(view, column, row)) {
            return MapPalette.RING_LINE_ON_HEIGHT;
        }
        int index = view.index(column, row);
        int ground = view.ground()[index];
        if (view.fluidTop()[index] != TopView.DRY) {
            return MapPalette.fluid(view.fluid()[index] == TerrainSampler.LAVA, view.fluidTop()[index] - ground);
        }
        int right = column + 1 < view.size() ? dryGround(view, column + 1, row, ground) : ground;
        int below = row + 1 < view.size() ? dryGround(view, column, row + 1, ground) : ground;
        if (crosses(ground, right, MapPalette.CLOUD_Y) || crosses(ground, below, MapPalette.CLOUD_Y)) {
            return MapPalette.CLOUD_LINE;
        }
        int colour = MapPalette.height(ground, view.minY(), view.maxY());
        return contour(ground, right) || contour(ground, below) ? MapPalette.scale(colour, CONTOUR_SHADE) : colour;
    }

    /** The neighbour's ground, or {@code own} if it is wet: no contours along the shore. */
    private static int dryGround(TopView view, int column, int row, int own) {
        int index = view.index(column, row);
        return view.fluidTop()[index] == TopView.DRY ? view.ground()[index] : own;
    }

    static boolean contour(int a, int b) {
        return Math.floorDiv(a, MapPalette.CONTOUR_STEP) != Math.floorDiv(b, MapPalette.CONTOUR_STEP);
    }

    static boolean crosses(int a, int b, int level) {
        return (a >= level) != (b >= level);
    }

    static boolean ringBoundary(TopView view, int column, int row) {
        byte ring = view.ring()[view.index(column, row)];
        if (ring == TerrainSampler.NO_RING) {
            return false;
        }
        return column + 1 < view.size() && view.ring()[view.index(column + 1, row)] != ring
                || row + 1 < view.size() && view.ring()[view.index(column, row + 1)] != ring;
    }

    private static boolean hasRings(TopView view) {
        return TerrainSampler.hasRings(view.ring());
    }

    /** Legend steps of 32 blocks, or 16 in a low dimension such as the Nether. */
    private static int rampStep(int minY, int maxY) {
        return maxY - minY > 200 ? 32 : 16;
    }

    static String shortName(String id) {
        int colon = id.indexOf(':');
        return id.startsWith("minecraft:") || id.startsWith("corruptedadventure:") ? id.substring(colon + 1) : id;
    }
}
