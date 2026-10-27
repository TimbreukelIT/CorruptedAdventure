import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates the placeholder textures of the phase 2 golf 4 material blocks (stream R4): the jade inlaid tidestone and the
 * aquamarine inlaid lagoon sandstone (side and top each), in the layout of golf 2's turquoise inlaid sandstone. Stream T
 * replaces them with the final textures. Built from our own textures and palettes only (tidestone, lagoon sandstone, the
 * ring 4 jade and aquamarine palettes); no vanilla pixels are read or copied. Run from the repository root:
 * {@code java tools/textures/Golf4MaterialTextures.java}.
 *
 * <p>Deterministic (no randomness) and checked before writing (16 x 16, alpha 0 or 255 only).
 */
public final class Golf4MaterialTextures {
    private static final int SIZE = 16;
    private static final Path TEXTURES = Path.of("src/main/resources/assets/corruptedadventure/textures");

    /** Jade, dark to light (from the ring 4 palette in {@code Ring4Textures}). */
    private static final int[] JADE = {0x1C4A3C, 0x2E6A54, 0x44886C, 0x62A688, 0x8CC4A6};
    /** Aquamarine, dark to light (from the ring 4 palette in {@code Ring4Textures}). */
    private static final int[] AQUAMARINE = {0x185066, 0x26758E, 0x3A9AB2, 0x5EBED0, 0x98DEE8};

    private Golf4MaterialTextures() {
    }

    public static void main(String[] args) throws IOException {
        BufferedImage tidestone = read("block/tidestone");
        write("block/jade_inlaid_tidestone", inlaidSide(tidestone, JADE));
        write("block/jade_inlaid_tidestone_top", inlaidTop(tidestone, JADE));
        write("block/aquamarine_inlaid_lagoon_sandstone", inlaidSide(read("block/lagoon_sandstone"), AQUAMARINE));
        write("block/aquamarine_inlaid_lagoon_sandstone_top", inlaidTop(read("block/lagoon_sandstone_top"), AQUAMARINE));
    }

    /** The stone's side with a band of gem diamonds through the middle, framed by a dark groove. */
    private static BufferedImage inlaidSide(BufferedImage stone, int[] gem) {
        BufferedImage image = copy(stone);
        for (int x = 0; x < SIZE; x++) {
            image.setRGB(x, 5, opaque(darken(stone.getRGB(x, 5), 0.7)));
            image.setRGB(x, 10, opaque(darken(stone.getRGB(x, 10), 0.7)));
            for (int y = 6; y <= 9; y++) {
                int dx = Math.abs((x % 4) - 1);
                int dy = y <= 7 ? 7 - y : y - 8;
                if (dx + dy <= 1) {
                    image.setRGB(x, y, opaque(gem[dx + dy == 0 ? 4 : 2]));
                }
            }
        }
        return image;
    }

    /** The stone's top with a gem diamond in the middle and a gem dot in each corner. */
    private static BufferedImage inlaidTop(BufferedImage stone, int[] gem) {
        BufferedImage image = copy(stone);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int d = Math.abs(x * 2 - 15) + Math.abs(y * 2 - 15);
                if (d <= 9) {
                    image.setRGB(x, y, opaque(gem[d <= 3 ? 4 : d <= 6 ? 3 : 2]));
                } else if (d <= 11) {
                    image.setRGB(x, y, opaque(darken(stone.getRGB(x, y), 0.7)));
                }
            }
        }
        for (int corner : new int[] {0, 15}) {
            image.setRGB(corner, 0, opaque(gem[2]));
            image.setRGB(corner, 15, opaque(gem[2]));
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
