import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Generates the phase 2 golf 1 material textures (stream D) from our own pixels: no vanilla pixels are read or copied.
 * The ingots and the turquoise gem ({@code item/}) and the storage blocks ({@code block/}): raw blocks, ingot blocks
 * and gem blocks. Run from the repository root: {@code java tools/textures/Golf1MaterialTextures.java}.
 *
 * <p>Placeholders: one shape per kind, coloured with the palette of the material's existing raw item or gem (sampled
 * from {@code raw_sunstone.png} and friends), so a material reads as itself. Deterministic: every texture has a fixed
 * seed (hash of its name). Every texture is checked before it is written (16 x 16, alpha 0 or 255 only). A contact
 * sheet (8x, blocks also 3 x 3 tiled at 2x) goes to {@code build/texture-preview/golf1-materials.png}.
 */
public final class Golf1MaterialTextures {
    private static final int SIZE = 16;
    private static final int TRANSPARENT = 0x00000000;
    private static final Path OUTPUT = Path.of("src/main/resources/assets/corruptedadventure/textures");
    private static final Path PREVIEW = Path.of("build/texture-preview/golf1-materials.png");

    // Palettes dark to light (RGB), from the existing raw items and gems.
    private static final int[] SUNSTONE = {0x5A1A06, 0x9A300C, 0xD15515, 0xE77C26, 0xF3A851};
    private static final int[] TURQUOISE = {0x0E3B3A, 0x1C6B66, 0x2E9C92, 0x4FC6B5, 0x8FE6D6};
    private static final int[] TIDESILVER = {0x2C3E4C, 0x48606E, 0x6E8896, 0x96B2BC, 0xBCD8DC};
    private static final int[] NULLITE = {0x1A0E2A, 0x24143A, 0x301C4C, 0x42286A, 0x7444BC};
    private static final int[] FIRE_RUBY = {0x4E0A14, 0x82121E, 0xB21D2A, 0xD5353F, 0xF07164};
    private static final int[] JADE = {0x1C4A3C, 0x2E6A54, 0x44886C, 0x62A688, 0xC4E6D2};

    /** An ingot seen from the front and top: o outline, s side, f front, t top, h highlight. */
    private static final String[] INGOT = {
            "................",
            "................",
            "................",
            "................",
            "....oooooooo....",
            "...ohhhhttttso..",
            "..ohhttttttttso.",
            ".offffffffffffso",
            ".ofhffffffffffso",
            ".offffffffffffso",
            ".offffffffffffo.",
            "..oooooooooooo..",
            "................",
            "................",
            "................",
            "................"};

    private static final Map<String, BufferedImage> WRITTEN = new LinkedHashMap<>();
    private static final List<String> BLOCKS = new ArrayList<>();

    private Golf1MaterialTextures() {
    }

    public static void main(String[] args) throws IOException {
        // item/{sunstone,volcanite,tidesilver}_ingot, block/{raw_,}volcanite_block: final textures from
        // Golf2SunstoneTextures and Golf34*Textures.
        ingot("item/nullite_ingot", NULLITE);
        gem("item/turquoise", TURQUOISE);

        rawBlock("block/raw_sunstone_block", SUNSTONE);
        rawBlock("block/raw_turquoise_block", TURQUOISE);
        rawBlock("block/raw_tidesilver_block", TIDESILVER);
        rawBlock("block/raw_nullite_block", NULLITE);
        metalBlock("block/sunstone_block", SUNSTONE);
        metalBlock("block/tidesilver_block", TIDESILVER);
        metalBlock("block/nullite_block", NULLITE);
        gemBlock("block/turquoise_block", TURQUOISE);
        gemBlock("block/fire_ruby_block", FIRE_RUBY);
        gemBlock("block/jade_block", JADE);

        preview();
    }

    private static void ingot(String name, int[] palette) throws IOException {
        Random random = random(name);
        BufferedImage image = blank();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int shade = switch (INGOT[y].charAt(x)) {
                    case 'o' -> 0;
                    case 's' -> 1;
                    case 'f' -> random.nextInt(8) == 0 ? 3 : 2;
                    case 't' -> random.nextInt(8) == 0 ? 4 : 3;
                    case 'h' -> 4;
                    default -> -1;
                };
                if (shade >= 0) {
                    image.setRGB(x, y, opaque(palette[shade]));
                }
            }
        }
        write(name, image);
    }

    /** A cut gem: a rhombus with four facets, lit from the top left. */
    private static void gem(String name, int[] palette) throws IOException {
        BufferedImage image = blank();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double dx = x - 7.5;
                double dy = y - 7.5;
                double distance = Math.abs(dx) + Math.abs(dy);
                if (distance > 7.0) {
                    continue;
                }
                int shade;
                if (distance > 6.0) {
                    shade = 0;
                } else if (distance < 1.5) {
                    shade = 4;
                } else if (dx < 0 && dy < 0) {
                    shade = 4;
                } else if (dy < 0) {
                    shade = 3;
                } else if (dx < 0) {
                    shade = 2;
                } else {
                    shade = 1;
                }
                image.setRGB(x, y, opaque(palette[shade]));
            }
        }
        write(name, image);
    }

    /** Raw material pressed into a block: lumps of the light shades on the middle shade, with dark gaps between them. */
    private static void rawBlock(String name, int[] palette) throws IOException {
        Random random = random(name);
        double[][] height = new double[SIZE][SIZE];
        for (int lump = 0; lump < 14; lump++) {
            double cx = random.nextInt(SIZE);
            double cy = random.nextInt(SIZE);
            double radius = 2.0 + random.nextDouble() * 2.5;
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    // Wrap around so the block tiles seamlessly.
                    double dx = wrap(x - cx);
                    double dy = wrap(y - cy);
                    double d = Math.sqrt(dx * dx + dy * dy) / radius;
                    if (d < 1.0) {
                        height[y][x] = Math.max(height[y][x], 1.0 - d);
                    }
                }
            }
        }
        BufferedImage image = blank();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double h = height[y][x] + random.nextDouble() * 0.12;
                int shade = h < 0.08 ? 0 : h < 0.3 ? 1 : h < 0.55 ? 2 : h < 0.8 ? 3 : 4;
                image.setRGB(x, y, opaque(palette[shade]));
            }
        }
        write(name, image);
        BLOCKS.add(name);
    }

    /** An ingot block: a plate with a light top-left bevel and a dark bottom-right one, and a few specks. */
    private static void metalBlock(String name, int[] palette) throws IOException {
        Random random = random(name);
        BufferedImage image = blank();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int shade;
                if (x == 0 || y == 0 || x == SIZE - 1 || y == SIZE - 1) {
                    shade = 1;
                } else if (x == 1 || y == 1) {
                    shade = 4;
                } else if (x == SIZE - 2 || y == SIZE - 2) {
                    shade = 2;
                } else {
                    int roll = random.nextInt(12);
                    shade = roll == 0 ? 2 : roll == 1 ? 4 : 3;
                }
                image.setRGB(x, y, opaque(palette[shade]));
            }
        }
        write(name, image);
        BLOCKS.add(name);
    }

    /** A gem block: four faceted tiles of 8 x 8, each lit from the top left. */
    private static void gemBlock(String name, int[] palette) throws IOException {
        Random random = random(name);
        BufferedImage image = blank();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int tx = x % 8;
                int ty = y % 8;
                int shade;
                if (tx == 7 || ty == 7) {
                    shade = 0;
                } else if (tx == 0 || ty == 0) {
                    shade = 4;
                } else if (tx + ty < 6) {
                    shade = random.nextInt(10) == 0 ? 4 : 3;
                } else if (tx + ty == 6) {
                    shade = 4;
                } else {
                    shade = random.nextInt(10) == 0 ? 1 : 2;
                }
                image.setRGB(x, y, opaque(palette[shade]));
            }
        }
        write(name, image);
        BLOCKS.add(name);
    }

    private static double wrap(double d) {
        if (d > SIZE / 2.0) {
            return d - SIZE;
        }
        if (d < -SIZE / 2.0) {
            return d + SIZE;
        }
        return d;
    }

    private static Random random(String name) {
        return new Random(name.hashCode());
    }

    private static BufferedImage blank() {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                image.setRGB(x, y, TRANSPARENT);
            }
        }
        return image;
    }

    private static int opaque(int rgb) {
        return 0xFF000000 | rgb;
    }

    private static void write(String name, BufferedImage image) throws IOException {
        if (image.getWidth() != SIZE || image.getHeight() != SIZE) {
            throw new IllegalStateException(name + ": not " + SIZE + " x " + SIZE);
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                if (alpha != 0 && alpha != 255) {
                    throw new IllegalStateException(name + ": alpha " + alpha + " at " + x + "," + y);
                }
            }
        }
        Path file = OUTPUT.resolve(name + ".png");
        Files.createDirectories(file.getParent());
        ImageIO.write(image, "png", file.toFile());
        WRITTEN.put(name, image);
        System.out.println("wrote " + file);
    }

    private static void preview() throws IOException {
        int scale = 8;
        int cell = SIZE * scale + 8;
        int tiled = SIZE * 3 * 2;
        int width = Math.max(WRITTEN.size(), 1) * cell;
        BufferedImage sheet = new BufferedImage(width, cell + tiled + 8, BufferedImage.TYPE_INT_ARGB);
        int column = 0;
        for (Map.Entry<String, BufferedImage> entry : WRITTEN.entrySet()) {
            BufferedImage image = entry.getValue();
            int left = column * cell;
            for (int y = 0; y < SIZE * scale; y++) {
                for (int x = 0; x < SIZE * scale; x++) {
                    sheet.setRGB(left + x, y, image.getRGB(x / scale, y / scale));
                }
            }
            if (BLOCKS.contains(entry.getKey())) {
                for (int y = 0; y < tiled; y++) {
                    for (int x = 0; x < tiled && left + x < width; x++) {
                        sheet.setRGB(left + x, cell + y, image.getRGB((x / 2) % SIZE, (y / 2) % SIZE));
                    }
                }
            }
            column++;
        }
        Files.createDirectories(PREVIEW.getParent());
        ImageIO.write(sheet, "png", PREVIEW.toFile());
        Files.writeString(PREVIEW.resolveSibling("golf1-materials.txt"), String.join("\n", WRITTEN.keySet()) + "\n",
                StandardCharsets.UTF_8);
        System.out.println("preview " + PREVIEW);
    }
}
