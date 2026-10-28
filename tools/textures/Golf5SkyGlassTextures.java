import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/**
 * Placeholder block textures of phase 2 golf 5, stream H (design §7.2, §7.4): sky sand, sky glass, the sky glass pane's
 * edge and the flickering glitch shard block (two frames stacked, animated by its hand-written {@code .png.mcmeta}).
 * Stream T replaces them with the final ones on the same paths. Fixed seeds, so the output is reproducible. Run
 * from the repository root: {@code java tools/textures/Golf5SkyGlassTextures.java}.
 */
public final class Golf5SkyGlassTextures {
    private static final Path TEXTURES = Path.of("src/main/resources/assets/corruptedadventure/textures");
    private static final int SIZE = 16;

    private Golf5SkyGlassTextures() {
    }

    public static void main(String[] args) throws IOException {
        write("block/sky_sand", skySand());
        write("block/sky_glass", skyGlass());
        write("block/sky_glass_pane_top", paneTop());
        write("block/glitch_shard_block", glitchShardBlock());
    }

    /** Grainy pale sky blue with lighter specks, like sand. */
    private static BufferedImage skySand() {
        int[] shades = {0xFFA9C6DC, 0xFFB8D2E6, 0xFFC6DCEC, 0xFFD4E6F2, 0xFFE6F1F8};
        Random random = new Random(5_501L);
        BufferedImage image = image();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int roll = random.nextInt(100);
                int shade = roll < 8 ? 0 : roll < 30 ? 1 : roll < 75 ? 2 : roll < 94 ? 3 : 4;
                image.setRGB(x, y, shades[shade]);
            }
        }
        return image;
    }

    /** A light blue frame, a faint blue fill and two diagonal glints. */
    private static BufferedImage skyGlass() {
        BufferedImage image = image();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                boolean edge = x == 0 || y == 0 || x == SIZE - 1 || y == SIZE - 1;
                boolean glint = (x + y == 9 && x >= 3 && x <= 6) || (x + y == 13 && x >= 4 && x <= 8);
                image.setRGB(x, y, edge ? 0xFFB4D8F0 : glint ? 0xC8F0F8FF : 0x40A8D0F0);
            }
        }
        return image;
    }

    /** The pane's edge: the frame colour in the two middle columns, like vanilla's {@code glass_pane_top}. */
    private static BufferedImage paneTop() {
        BufferedImage image = image();
        for (int y = 0; y < SIZE; y++) {
            image.setRGB(7, y, 0xFFB4D8F0);
            image.setRGB(8, y, 0xFF9CC8E6);
        }
        return image;
    }

    /** Magenta and cyan shard facets on near-black; the second frame swaps a few of them (the flicker). */
    private static BufferedImage glitchShardBlock() {
        int[] colors = {0xFF1A1024, 0xFF6A2A8C, 0xFFC04CE0, 0xFF3FD8E8};
        BufferedImage image = new BufferedImage(SIZE, 2 * SIZE, BufferedImage.TYPE_INT_ARGB);
        Random random = new Random(5_502L);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int roll = random.nextInt(100);
                int color = roll < 45 ? 0 : roll < 75 ? 1 : roll < 92 ? 2 : 3;
                int flicker = random.nextInt(100) < 15 ? (color + 2) % colors.length : color;
                image.setRGB(x, y, colors[color]);
                image.setRGB(x, y + SIZE, colors[flicker]);
            }
        }
        return image;
    }

    private static BufferedImage image() {
        return new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
    }

    private static void write(String name, BufferedImage image) throws IOException {
        Path file = TEXTURES.resolve(name + ".png");
        Files.createDirectories(file.getParent());
        ImageIO.write(image, "png", file.toFile());
        System.out.println("wrote " + file);
    }
}
