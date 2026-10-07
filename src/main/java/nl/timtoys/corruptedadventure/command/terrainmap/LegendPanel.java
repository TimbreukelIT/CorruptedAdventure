package nl.timtoys.corruptedadventure.command.terrainmap;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * A map with a legend column on the right: header lines, then a colour swatch with a label per entry. Text needs the
 * JVM's fonts; a headless server without them still gets the map and the swatches.
 */
final class LegendPanel {
    static final int WIDTH = 300;
    private static final int LINE = 14;
    private static final int MARGIN = 6;
    private static final int SWATCH = 10;

    record Entry(int rgb, String label) {
    }

    private final List<String> header = new ArrayList<>();
    private final List<Entry> entries = new ArrayList<>();

    LegendPanel line(String text) {
        header.add(text);
        return this;
    }

    LegendPanel entry(int rgb, String label) {
        entries.add(new Entry(rgb, label));
        return this;
    }

    int height() {
        return 2 * MARGIN + (header.size() + entries.size() + 1) * LINE;
    }

    /** A new image with {@code map} on the left and this legend on the right. */
    BufferedImage compose(BufferedImage map) {
        int height = Math.max(map.getHeight(), height());
        BufferedImage image = new BufferedImage(map.getWidth() + WIDTH, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(MapPalette.BACKGROUND));
            graphics.fillRect(0, 0, image.getWidth(), height);
            graphics.drawImage(map, 0, 0, null);
            draw(graphics, map.getWidth() + MARGIN);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private void draw(Graphics2D graphics, int left) {
        boolean text = setUpText(graphics);
        int y = MARGIN;
        for (String line : header) {
            if (text) {
                text = drawText(graphics, line, left, y + LINE - 3);
            }
            y += LINE;
        }
        y += LINE;
        for (Entry entry : entries) {
            graphics.setColor(new Color(entry.rgb()));
            graphics.fillRect(left, y + 2, SWATCH, SWATCH);
            if (text) {
                text = drawText(graphics, entry.label(), left + SWATCH + 6, y + LINE - 3);
            }
            y += LINE;
        }
    }

    /** A loose label (e.g. an axis tick); false if this machine cannot draw text. */
    static boolean label(Graphics2D graphics, String text, int x, int y) {
        return setUpText(graphics) && drawText(graphics, text, x, y);
    }

    private static boolean setUpText(Graphics2D graphics) {
        try {
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            graphics.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            return true;
        } catch (Throwable error) {
            return fontFailure(error);
        }
    }

    /** False once text fails (no fonts on this machine), so the rest of the legend is drawn without it. */
    private static boolean drawText(Graphics2D graphics, String text, int x, int y) {
        try {
            graphics.setColor(new Color(MapPalette.TEXT));
            graphics.drawString(text, x, y);
            return true;
        } catch (Throwable error) {
            return fontFailure(error);
        }
    }

    /** Missing fonts surface as any of several errors (AWTError, UnsatisfiedLinkError, a plain Error); only the VM's own are fatal. */
    private static boolean fontFailure(Throwable error) {
        if (error instanceof VirtualMachineError fatal) {
            throw fatal;
        }
        return false;
    }
}
