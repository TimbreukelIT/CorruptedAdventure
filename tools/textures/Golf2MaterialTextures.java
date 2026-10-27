import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/**
 * Generates the placeholder textures of the phase 2 golf 2 material blocks (stream M): the turquoise inlaid sandstone
 * (side and top), the soul crystal block and the salt lamp. Stream T replaces them with the final textures. Built from
 * our own textures and palettes only (scorched sandstone, salt block, the soul crystal and turquoise palettes); no
 * vanilla pixels are read or copied. Run from the repository root: {@code java tools/textures/Golf2MaterialTextures.java}.
 *
 * <p>Deterministic (a fixed seed per texture) and checked before writing (16 x 16, alpha 0 or 255 only).
 */
public final class Golf2MaterialTextures {
    private static final int SIZE = 16;
    private static final Path TEXTURES = Path.of("src/main/resources/assets/corruptedadventure/textures");

    /** Turquoise, dark to light (the golf 1 gem palette). */
    private static final int[] TURQUOISE = {0x0E3B3A, 0x1C6B66, 0x2E9C92, 0x4FC6B5, 0x8FE6D6};
    /** Soul crystal, dark to light (the ring 1 crystal palette). */
    private static final int[] SOUL_CRYSTAL = {0x0B3446, 0x17758C, 0x2DB8C8, 0x5FE3E6, 0xA8F7F4};
    /** Warm glow mixed into the salt block for the lamp. */
    private static final int LAMP_GLOW = 0xF29A4A;

    private Golf2MaterialTextures() {
    }

    public static void main(String[] args) throws IOException {
        BufferedImage sandstoneSide = read("block/scorched_sandstone");
        BufferedImage sandstoneTop = read("block/scorched_sandstone_top");
        write("block/turquoise_inlaid_sandstone", inlaidSide(sandstoneSide));
        write("block/turquoise_inlaid_sandstone_top", inlaidTop(sandstoneTop));
        write("block/soul_crystal_block", crystalBlock(SOUL_CRYSTAL, new Random("soul_crystal_block".hashCode())));
        write("block/salt_lamp", saltLamp(read("block/salt_block")));
    }

    /** The sandstone side with a band of turquoise diamonds through the middle, framed by a dark groove. */
    private static BufferedImage inlaidSide(BufferedImage sandstone) {
        BufferedImage image = copy(sandstone);
        for (int x = 0; x < SIZE; x++) {
            image.setRGB(x, 5, opaque(darken(sandstone.getRGB(x, 5), 0.7)));
            image.setRGB(x, 10, opaque(darken(sandstone.getRGB(x, 10), 0.7)));
            for (int y = 6; y <= 9; y++) {
                int dx = Math.abs((x % 4) - 1);
                int dy = y <= 7 ? 7 - y : y - 8;
                if (dx + dy <= 1) {
                    image.setRGB(x, y, opaque(TURQUOISE[dx + dy == 0 ? 4 : 2]));
                }
            }
        }
        return image;
    }

    /** The sandstone top with a turquoise diamond in the middle and a turquoise dot in each corner. */
    private static BufferedImage inlaidTop(BufferedImage sandstone) {
        BufferedImage image = copy(sandstone);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int d = Math.abs(x * 2 - 15) + Math.abs(y * 2 - 15);
                if (d <= 9) {
                    image.setRGB(x, y, opaque(TURQUOISE[d <= 3 ? 4 : d <= 6 ? 3 : 2]));
                } else if (d <= 11) {
                    image.setRGB(x, y, opaque(darken(sandstone.getRGB(x, y), 0.7)));
                }
            }
        }
        for (int corner : new int[] {0, 15}) {
            image.setRGB(corner, 0, opaque(TURQUOISE[2]));
            image.setRGB(corner, 15, opaque(TURQUOISE[2]));
        }
        return image;
    }

    /** Facets like a crystal wall: light upper-left halves, darker lower-right halves, some sparkle. */
    private static BufferedImage crystalBlock(int[] palette, Random random) {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int tx = (x + (y / 8) * 4) % 8;
                int ty = y % 8;
                int shade;
                if (tx == 7 || ty == 7) {
                    shade = 0;
                } else if (tx + ty < 6) {
                    shade = random.nextInt(9) == 0 ? 4 : 3;
                } else if (tx + ty == 6) {
                    shade = 4;
                } else {
                    shade = random.nextInt(9) == 0 ? 2 : 1;
                }
                image.setRGB(x, y, opaque(palette[shade]));
            }
        }
        return image;
    }

    /** The salt block lit from within: warm in the middle, fading to the plain salt at the edge. */
    private static BufferedImage saltLamp(BufferedImage salt) {
        BufferedImage image = copy(salt);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double distance = Math.hypot(x - 7.5, y - 7.5) / 10.6;
                double glow = 0.75 * Math.max(0, 1 - distance);
                image.setRGB(x, y, opaque(mix(salt.getRGB(x, y), LAMP_GLOW, 0.25 + glow)));
            }
        }
        return image;
    }

    private static BufferedImage read(String name) throws IOException {
        BufferedImage source = ImageIO.read(TEXTURES.resolve(name + ".png").toFile());
        // Animated textures stack their frames vertically: the first frame is enough.
        return source.getSubimage(0, 0, SIZE, SIZE);
    }

    private static BufferedImage copy(BufferedImage source) {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                image.setRGB(x, y, opaque(source.getRGB(x, y)));
            }
        }
        return image;
    }

    private static int darken(int rgb, double factor) {
        return mix(rgb, 0x000000, 1 - factor);
    }

    private static int mix(int a, int b, double t) {
        t = Math.min(1, Math.max(0, t));
        int r = (int) Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = (int) Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = (int) Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static int opaque(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
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
        Path file = TEXTURES.resolve(name + ".png");
        Files.createDirectories(file.getParent());
        ImageIO.write(image, "png", file.toFile());
        System.out.println("wrote " + file);
    }
}
