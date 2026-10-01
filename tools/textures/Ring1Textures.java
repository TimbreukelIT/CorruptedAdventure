import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;

/**
 * Generates the ring 1 block and item textures from our own pixels (no vanilla pixels are read or copied).
 * Run from the repository root: {@code java tools/textures/Ring1Textures.java}.
 *
 * <p>Deterministic: every texture has a fixed seed (hash of its file name), so running twice gives identical PNGs.
 * Change 10 version: simple working placeholders. Change 17 refines palettes and patterns into the final textures.
 */
public final class Ring1Textures {
    private static final int SIZE = 16;
    private static final int TRANSPARENT = 0x00000000;
    private static final Path OUTPUT = Path.of("src/main/resources/assets/corruptedadventure/textures");

    // Palettes, dark to light (RGB).
    private static final int[] WITHERED_DIRT = {0x54483E, 0x65584B, 0x756757, 0x857664};
    private static final int[] DEAD_GRASS = {0x77704F, 0x8C8460, 0x9E956E, 0xB0A67E};
    private static final int[] DEAD_BARK = {0x4F463F, 0x635950, 0x776C61, 0x8A7F72};
    private static final int[] DEAD_WOOD = {0x8A8072, 0x9C9282, 0xADA392, 0xBDB3A1};
    private static final int[] SNOW = {0xDCE6EA, 0xEDF3F5, 0xFFFFFF};

    public static void main(String[] args) throws IOException {
        if (!Files.isDirectory(Path.of("src/main/resources"))) {
            throw new IllegalStateException("Run from the repository root (src/main/resources not found)");
        }
        Map<String, BiConsumer<BufferedImage, Random>> textures = new LinkedHashMap<>();
        textures.put("block/withered_dirt", Ring1Textures::witheredDirt);
        textures.put("block/dead_grass_block_top", Ring1Textures::deadGrassTop);
        textures.put("block/dead_grass_block_side", Ring1Textures::deadGrassSide);
        textures.put("block/dead_grass_block_snow", Ring1Textures::deadGrassSnow);
        textures.put("block/dead_log", Ring1Textures::deadLog);
        textures.put("block/dead_log_top", Ring1Textures::deadLogTop);
        textures.put("block/stripped_dead_log", Ring1Textures::strippedDeadLog);
        textures.put("block/stripped_dead_log_top", Ring1Textures::strippedDeadLogTop);
        textures.put("block/dead_planks", Ring1Textures::deadPlanks);
        textures.put("block/dead_door_top", Ring1Textures::deadDoorTop);
        textures.put("block/dead_door_bottom", Ring1Textures::deadDoorBottom);
        textures.put("block/dead_trapdoor", Ring1Textures::deadTrapdoor);
        textures.put("item/dead_door", Ring1Textures::deadDoorItem);

        for (Map.Entry<String, BiConsumer<BufferedImage, Random>> texture : textures.entrySet()) {
            BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            texture.getValue().accept(image, seeded(texture.getKey()));
            Path file = OUTPUT.resolve(texture.getKey() + ".png");
            Files.createDirectories(file.getParent());
            ImageIO.write(image, "png", file.toFile());
            System.out.println("wrote " + file);
        }
    }

    // ---- Textures -------------------------------------------------------------------------------------------

    private static void witheredDirt(BufferedImage image, Random random) {
        double[][] noise = valueNoise(random, 4);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, shade(WITHERED_DIRT, 0.6 * noise[x][y] + 0.4 * random.nextDouble()));
            }
        }
        speckle(image, random, WITHERED_DIRT[0], 10);
    }

    private static void deadGrassTop(BufferedImage image, Random random) {
        double[][] noise = valueNoise(random, 4);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, shade(DEAD_GRASS, 0.5 * noise[x][y] + 0.5 * random.nextDouble()));
            }
        }
        // Dark blades of two pixels, wrapping around the tile edge so it stays seamless.
        for (int i = 0; i < 14; i++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            set(image, x, y, DEAD_GRASS[0]);
            set(image, x, (y + 1) % SIZE, DEAD_GRASS[random.nextBoolean() ? 0 : 1]);
        }
        speckle(image, random, DEAD_GRASS[3], 8);
    }

    private static void deadGrassSide(BufferedImage image, Random random) {
        witheredDirt(image, random);
        overhang(image, random, DEAD_GRASS);
    }

    private static void deadGrassSnow(BufferedImage image, Random random) {
        witheredDirt(image, random);
        overhang(image, random, SNOW);
    }

    private static void deadLog(BufferedImage image, Random random) {
        verticalGrain(image, random, DEAD_BARK);
        // Bark cracks: short dark vertical streaks.
        for (int i = 0; i < 6; i++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            int length = 3 + random.nextInt(4);
            for (int j = 0; j < length; j++) {
                set(image, x, (y + j) % SIZE, DEAD_BARK[0]);
            }
        }
    }

    private static void deadLogTop(BufferedImage image, Random random) {
        rings(image, random);
        for (int i = 0; i < SIZE; i++) {
            set(image, i, 0, shade(DEAD_BARK, random.nextDouble() * 0.6));
            set(image, i, SIZE - 1, shade(DEAD_BARK, random.nextDouble() * 0.6));
            set(image, 0, i, shade(DEAD_BARK, random.nextDouble() * 0.6));
            set(image, SIZE - 1, i, shade(DEAD_BARK, random.nextDouble() * 0.6));
        }
    }

    private static void strippedDeadLog(BufferedImage image, Random random) {
        verticalGrain(image, random, DEAD_WOOD);
    }

    private static void strippedDeadLogTop(BufferedImage image, Random random) {
        rings(image, random);
        for (int i = 0; i < SIZE; i++) {
            set(image, i, 0, DEAD_WOOD[1]);
            set(image, i, SIZE - 1, DEAD_WOOD[1]);
            set(image, 0, i, DEAD_WOOD[1]);
            set(image, SIZE - 1, i, DEAD_WOOD[1]);
        }
    }

    private static void deadPlanks(BufferedImage image, Random random) {
        // Four planks of four rows; the bottom row of each plank is the seam, butt joints staggered per plank.
        int[] joints = {4, 11, 1, 8};
        for (int plank = 0; plank < 4; plank++) {
            double plankShade = 0.35 + random.nextDouble() * 0.3;
            for (int row = 0; row < 4; row++) {
                int y = plank * 4 + row;
                for (int x = 0; x < SIZE; x++) {
                    if (row == 3 || x == joints[plank]) {
                        set(image, x, y, DEAD_WOOD[0]);
                    } else {
                        set(image, x, y, shade(DEAD_WOOD, plankShade + (random.nextDouble() - 0.5) * 0.45));
                    }
                }
            }
        }
    }

    private static void deadDoorTop(BufferedImage image, Random random) {
        doorPanel(image, random);
        hole(image, 3, 3, 4, 4);
        hole(image, 9, 3, 4, 4);
        hole(image, 3, 9, 4, 4);
        hole(image, 9, 9, 4, 4);
    }

    private static void deadDoorBottom(BufferedImage image, Random random) {
        doorPanel(image, random);
        // Diagonal brace and a handle.
        for (int i = 2; i < SIZE - 2; i++) {
            set(image, i, SIZE - 1 - i, DEAD_WOOD[0]);
        }
        set(image, 12, 2, DEAD_BARK[0]);
        set(image, 12, 3, DEAD_BARK[0]);
    }

    private static void deadTrapdoor(BufferedImage image, Random random) {
        doorPanel(image, random);
        hole(image, 3, 3, 2, 10);
        hole(image, 7, 3, 2, 10);
        hole(image, 11, 3, 2, 10);
    }

    /** Door seen from afar: both halves squeezed into one 8×16 silhouette. */
    private static void deadDoorItem(BufferedImage image, Random random) {
        BufferedImage top = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        BufferedImage bottom = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        deadDoorTop(top, seeded("block/dead_door_top"));
        deadDoorBottom(bottom, seeded("block/dead_door_bottom"));
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                image.setRGB(x, y, TRANSPARENT);
            }
        }
        for (int y = 0; y < SIZE; y++) {
            BufferedImage half = y < SIZE / 2 ? top : bottom;
            int sourceY = (y % (SIZE / 2)) * 2;
            for (int x = 0; x < SIZE / 2; x++) {
                image.setRGB(4 + x, y, half.getRGB(x * 2, sourceY));
            }
        }
    }

    // ---- Shared patterns ------------------------------------------------------------------------------------

    /** A 2–4 pixel band along the top edge, hanging unevenly down the side. */
    private static void overhang(BufferedImage image, Random random, int[] palette) {
        for (int x = 0; x < SIZE; x++) {
            int depth = 2 + random.nextInt(3);
            for (int y = 0; y < depth; y++) {
                set(image, x, y, shade(palette, 0.35 + random.nextDouble() * 0.65));
            }
            set(image, x, depth - 1, palette[random.nextInt(2)]);
        }
    }

    /** Vertical grain: a smooth shade per column with per-pixel variation. */
    private static void verticalGrain(BufferedImage image, Random random, int[] palette) {
        double[] columns = new double[SIZE];
        for (int x = 0; x < SIZE; x++) {
            columns[x] = random.nextDouble();
        }
        for (int x = 0; x < SIZE; x++) {
            double column = (columns[(x + SIZE - 1) % SIZE] + 2 * columns[x] + columns[(x + 1) % SIZE]) / 4;
            for (int y = 0; y < SIZE; y++) {
                set(image, x, y, shade(palette, 0.7 * column + 0.3 * random.nextDouble()));
            }
        }
    }

    /** Year rings around the centre in the wood palette. */
    private static void rings(BufferedImage image, Random random) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double distance = Math.hypot(x - 7.5, y - 7.5) + random.nextDouble() * 0.6;
                int ring = (int) distance;
                set(image, x, y, ring % 2 == 0 ? DEAD_WOOD[2 + random.nextInt(2)] : DEAD_WOOD[random.nextInt(2)]);
            }
        }
    }

    /** Vertical planks inside a darker frame; used by the door halves and the trapdoor. */
    private static void doorPanel(BufferedImage image, Random random) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                boolean frame = x <= 1 || x >= SIZE - 2 || y <= 1 || y >= SIZE - 2;
                boolean seam = x == 5 || x == 10;
                if (frame) {
                    set(image, x, y, shade(DEAD_BARK, 0.4 + random.nextDouble() * 0.6));
                } else if (seam) {
                    set(image, x, y, DEAD_WOOD[0]);
                } else {
                    set(image, x, y, shade(DEAD_WOOD, 0.3 + random.nextDouble() * 0.6));
                }
            }
        }
    }

    /** Fully transparent rectangle: cutout textures only use alpha 0 or 255. */
    private static void hole(BufferedImage image, int x0, int y0, int width, int height) {
        for (int y = y0; y < y0 + height; y++) {
            for (int x = x0; x < x0 + width; x++) {
                image.setRGB(x, y, TRANSPARENT);
            }
        }
    }

    private static void speckle(BufferedImage image, Random random, int color, int count) {
        for (int i = 0; i < count; i++) {
            set(image, random.nextInt(SIZE), random.nextInt(SIZE), color);
        }
    }

    // ---- Helpers --------------------------------------------------------------------------------------------

    private static Random seeded(String name) {
        return new Random(name.hashCode());
    }

    /** Tileable value noise in [0, 1): random values on a grid of {@code cell} pixels, smoothly interpolated. */
    private static double[][] valueNoise(Random random, int cell) {
        int points = SIZE / cell;
        double[][] grid = new double[points][points];
        for (int i = 0; i < points; i++) {
            for (int j = 0; j < points; j++) {
                grid[i][j] = random.nextDouble();
            }
        }
        double[][] noise = new double[SIZE][SIZE];
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                int gx = x / cell;
                int gy = y / cell;
                double fx = smoothstep((x % cell) / (double) cell);
                double fy = smoothstep((y % cell) / (double) cell);
                double top = lerp(grid[gx][gy], grid[(gx + 1) % points][gy], fx);
                double bottom = lerp(grid[gx][(gy + 1) % points], grid[(gx + 1) % points][(gy + 1) % points], fx);
                noise[x][y] = lerp(top, bottom, fy);
            }
        }
        return noise;
    }

    /** Palette colour for {@code t} in [0, 1] (clamped), dark to light. */
    private static int shade(int[] palette, double t) {
        int index = (int) (Math.max(0, Math.min(0.999, t)) * palette.length);
        return palette[index];
    }

    private static void set(BufferedImage image, int x, int y, int rgb) {
        image.setRGB(x, y, 0xFF000000 | rgb);
    }

    private static double smoothstep(double t) {
        return t * t * (3 - 2 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private Ring1Textures() {
    }
}
