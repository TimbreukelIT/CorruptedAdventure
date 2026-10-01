import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Generates the final ring 1 block and item textures (Nether ring 1 included) from our own pixels: no vanilla pixels
 * are read or copied. Ore overlays are transparent and drawn over a vanilla texture that the model only references.
 * Run from the repository root: {@code java tools/textures/Ring1Textures.java}.
 *
 * <p>Deterministic: every texture has a fixed seed (hash of its file name), so running twice gives identical PNGs.
 * Textures listed in {@link #HAND_EDITED} are never overwritten. A contact sheet (8× and 3×3 tiled, all animation
 * frames) goes to {@code build/texture-preview/ring1.png} for review without the game.
 *
 * <p>Style (change 17, see {@code fase-1-wereld.md} §1.4): bleached grey-brown driftwood, dull yellow-brown straw on
 * grey-brown withered dirt, soul-blue crystals with a dark outline that pulse (8 frames, interpolated).
 */
public final class Ring1Textures {
    private static final int SIZE = 16;
    private static final int TRANSPARENT = 0x00000000;
    private static final Path OUTPUT = Path.of("src/main/resources/assets/corruptedadventure/textures");
    private static final Path PREVIEW = Path.of("build/texture-preview/ring1.png");

    /** Retouched by hand after generation; the script skips these and the preview reads them from disk. */
    private static final Set<String> HAND_EDITED = Set.of();

    // Palettes, dark to light (RGB). Documented in fase-1-wereld.md §1.4; later rings reuse the approach.
    private static final int[] WITHERED_DIRT = {0x4A3F36, 0x5A4D42, 0x6B5D50, 0x7B6C5D, 0x8C7D6B};
    private static final int[] DEAD_GRASS = {0x6E6544, 0x857B53, 0x9A8F62, 0xAEA272, 0xC2B686};
    private static final int[] DEAD_BARK = {0x3F3832, 0x524A42, 0x655C52, 0x786E62, 0x8B8174};
    private static final int[] DEAD_WOOD = {0x7E7466, 0x918777, 0xA39988, 0xB4AA98, 0xC4BBA9};
    private static final int[] SNOW = {0xC9D3D8, 0xDCE6EA, 0xEDF3F5, 0xFFFFFF};
    /** Soul crystal: outline, shadow, base (soul fire cyan), light, lighter, highlight. */
    private static final int[] SOUL_CRYSTAL = {0x0B3446, 0x17758C, 0x2DB8C8, 0x5FE3E6, 0xA8F7F4, 0xE6FFFF};

    /** Pulse of the soul crystal: same frame count and frame time on every animated texture, so all ores pulse alike. */
    private static final int PULSE_FRAMES = 8;
    private static final int PULSE_FRAMETIME = 4;
    private static final int PULSE_GLOW = 0xD8FFFF;

    private record Texture(Painter painter, boolean animated) {
    }

    @FunctionalInterface
    private interface Painter {
        void paint(BufferedImage image, Random random);
    }

    private static final Map<String, Texture> TEXTURES = new LinkedHashMap<>();

    static {
        still("block/withered_dirt", Ring1Textures::witheredDirt);
        still("block/dead_grass_block_top", Ring1Textures::deadGrassTop);
        still("block/dead_grass_block_side", Ring1Textures::deadGrassSide);
        still("block/dead_grass_block_snow", Ring1Textures::deadGrassSnow);
        still("block/dead_log", Ring1Textures::deadLog);
        still("block/dead_log_top", Ring1Textures::deadLogTop);
        still("block/stripped_dead_log", Ring1Textures::strippedDeadLog);
        still("block/stripped_dead_log_top", Ring1Textures::strippedDeadLogTop);
        still("block/dead_planks", Ring1Textures::deadPlanks);
        still("block/dead_door_top", Ring1Textures::deadDoorTop);
        still("block/dead_door_bottom", Ring1Textures::deadDoorBottom);
        still("block/dead_trapdoor", Ring1Textures::deadTrapdoor);
        still("item/dead_door", Ring1Textures::deadDoorItem);
        pulsing("block/soul_crystal_ore_overlay", Ring1Textures::soulCrystalOreOverlay);
        pulsing("block/crimson_soul_crystal_ore_overlay_top", Ring1Textures::crimsonSoulCrystalOreOverlayTop);
        pulsing("block/crimson_soul_crystal_ore_overlay_side", Ring1Textures::crimsonSoulCrystalOreOverlaySide);
        pulsing("item/soul_crystal", Ring1Textures::soulCrystalItem);
    }

    private static void still(String name, Painter painter) {
        TEXTURES.put(name, new Texture(painter, false));
    }

    private static void pulsing(String name, Painter painter) {
        TEXTURES.put(name, new Texture(painter, true));
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("java.awt.headless", "true");
        if (!Files.isDirectory(Path.of("src/main/resources"))) {
            throw new IllegalStateException("Run from the repository root (src/main/resources not found)");
        }
        for (Map.Entry<String, Texture> entry : TEXTURES.entrySet()) {
            String name = entry.getKey();
            Path file = OUTPUT.resolve(name + ".png");
            if (HAND_EDITED.contains(name)) {
                System.out.println("skipped (hand-edited) " + file);
                continue;
            }
            BufferedImage frame = render(name);
            Files.createDirectories(file.getParent());
            ImageIO.write(entry.getValue().animated() ? pulseStrip(frame) : frame, "png", file.toFile());
            System.out.println("wrote " + file);
            if (entry.getValue().animated()) {
                Path meta = OUTPUT.resolve(name + ".png.mcmeta");
                Files.writeString(meta, "{\n  \"animation\": {\n    \"frametime\": " + PULSE_FRAMETIME
                        + ",\n    \"interpolate\": true\n  }\n}\n", StandardCharsets.UTF_8);
                System.out.println("wrote " + meta);
            }
        }
        writePreview();
    }

    /** The texture as the game shows it at rest (first frame); hand-edited textures come from disk. */
    private static BufferedImage render(String name) throws IOException {
        if (HAND_EDITED.contains(name)) {
            BufferedImage file = ImageIO.read(OUTPUT.resolve(name + ".png").toFile());
            BufferedImage frame = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            copy(file, frame);
            return frame;
        }
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        try {
            TEXTURES.get(name).painter().paint(image, seeded(name));
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
        return image;
    }

    /** {@link #render} for painters, which cannot throw checked exceptions. */
    private static BufferedImage renderUnchecked(String name) {
        try {
            return render(name);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ---- Ground ---------------------------------------------------------------------------------------------

    /** Dry, clumpy grey-brown dirt: two octaves of noise, lit clods with a shadow, a few hairline cracks. */
    private static void witheredDirt(BufferedImage image, Random random) {
        double[][] coarse = valueNoise(random, 8);
        double[][] fine = valueNoise(random, 4);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, shade(WITHERED_DIRT, 0.35 * coarse[x][y] + 0.45 * fine[x][y] + 0.2 * random.nextDouble()));
            }
        }
        for (int i = 0; i < 7; i++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            setWrapped(image, x, y, WITHERED_DIRT[4]);
            setWrapped(image, x + 1, y + 1, WITHERED_DIRT[0]);
        }
        for (int i = 0; i < 2; i++) {
            crack(image, random, random.nextInt(SIZE), random.nextInt(SIZE), 3 + random.nextInt(2), WITHERED_DIRT[0]);
        }
    }

    /** Matted straw: noisy base, blades lying in random directions (dark root, light tip), bare dirt spots. */
    private static void deadGrassTop(BufferedImage image, Random random) {
        double[][] noise = valueNoise(random, 4);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, shade(DEAD_GRASS, 0.05 + 0.5 * noise[x][y] + 0.35 * random.nextDouble()));
            }
        }
        int[][] directions = {{0, 1}, {1, 1}, {-1, 1}, {1, 0}};
        for (int i = 0; i < 22; i++) {
            int[] direction = directions[random.nextInt(directions.length)];
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            int length = 2 + random.nextInt(2);
            for (int j = 0; j < length; j++) {
                int color = j == 0 ? DEAD_GRASS[random.nextInt(2)] : j == length - 1 ? DEAD_GRASS[4] : DEAD_GRASS[3];
                setWrapped(image, x + j * direction[0], y + j * direction[1], color);
            }
        }
        for (int i = 0; i < 3; i++) {
            setWrapped(image, random.nextInt(SIZE), random.nextInt(SIZE), WITHERED_DIRT[2 + random.nextInt(2)]);
        }
    }

    /** Withered dirt (the same pixels as {@code withered_dirt}) under a ragged straw band with hanging blades. */
    private static void deadGrassSide(BufferedImage image, Random random) {
        copy(renderUnchecked("block/withered_dirt"), image);
        topBand(image, random, DEAD_GRASS, 2, 3, 0.3);
    }

    private static void deadGrassSnow(BufferedImage image, Random random) {
        copy(renderUnchecked("block/withered_dirt"), image);
        topBand(image, random, SNOW, 3, 2, 0.15);
    }

    /**
     * A band along the top edge, {@code minDepth} to {@code minDepth + variation - 1} pixels deep and smooth over the
     * columns, lightest at the top; with chance {@code dripChance} a column hangs 1–2 pixels further. Leaves a soft
     * shadow on the dirt under the band.
     */
    private static void topBand(BufferedImage image, Random random, int[] palette, int minDepth, int variation,
                                double dripChance) {
        double[] columns = smoothColumns(random);
        for (int x = 0; x < SIZE; x++) {
            int depth = minDepth + (int) (columns[x] * variation);
            if (random.nextDouble() < dripChance) {
                depth += 1 + random.nextInt(2);
            }
            for (int y = 0; y < depth; y++) {
                double t = 1.0 - (double) y / depth;
                set(image, x, y, shade(palette, 0.25 + 0.6 * t + 0.25 * (random.nextDouble() - 0.5)));
            }
            set(image, x, depth - 1, palette[random.nextInt(2)]);
            if (random.nextBoolean()) {
                set(image, x, depth, WITHERED_DIRT[0]);
            }
        }
    }

    // ---- Wood -----------------------------------------------------------------------------------------------

    /**
     * Driftwood bark: ridged columns, wandering dark furrows broken into plates, a few lit ridge tops and one peeled
     * patch where the bleached wood shows through. Tiles in both directions.
     */
    private static void deadLog(BufferedImage image, Random random) {
        double[] columns = smoothColumns(random);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, shade(DEAD_BARK, 0.3 + 0.5 * columns[x] + 0.2 * random.nextDouble()));
            }
        }
        for (int i = 0; i < 4; i++) {
            int breakAt = random.nextInt(SIZE);
            furrow(image, random, 4 * i + random.nextInt(2), DEAD_BARK[0], DEAD_BARK[1], breakAt);
        }
        for (int i = 0; i < 6; i++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            if (image.getRGB(x, y) != (0xFF000000 | DEAD_BARK[0])) {
                set(image, x, y, DEAD_BARK[4]);
                setWrapped(image, x, y + 1, DEAD_BARK[3]);
            }
        }
        int px = random.nextInt(SIZE);
        int py = random.nextInt(SIZE);
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 2; x++) {
                setWrapped(image, px + x, py + y, DEAD_WOOD[y == 0 ? 3 : 1 + x]);
            }
            setWrapped(image, px - 1, py + y, DEAD_BARK[0]);
        }
    }

    /** Bleached inner wood: soft column shading, a few fine wandering grain lines and one long dry crack. */
    private static void strippedDeadLog(BufferedImage image, Random random) {
        double[] columns = smoothColumns(random);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, shade(DEAD_WOOD, 0.35 + 0.45 * columns[x] + 0.2 * random.nextDouble()));
            }
        }
        for (int i = 0; i < 3; i++) {
            furrow(image, random, 5 * i + 1 + random.nextInt(3), DEAD_WOOD[1], DEAD_WOOD[1], -1);
        }
        furrow(image, random, random.nextInt(SIZE), DEAD_WOOD[0], DEAD_WOOD[0], random.nextInt(SIZE));
    }

    /** End grain: rounded year rings, dark pith, a radial dry crack, a 1-pixel bark rim. */
    private static void deadLogTop(BufferedImage image, Random random) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double dx = Math.abs(x - 7.5);
                double dy = Math.abs(y - 7.5);
                double radius = 0.55 * Math.max(dx, dy) + 0.45 * Math.hypot(dx, dy) + 0.3 * random.nextDouble();
                int ring = (int) (radius / 2.0);
                boolean line = (radius / 2.0) % 1.0 < 0.35;
                int wood = random.nextInt(8) == 0 ? DEAD_WOOD[4] : DEAD_WOOD[ring % 2 == 0 ? 3 : 2];
                set(image, x, y, line ? DEAD_WOOD[1] : wood);
            }
        }
        set(image, 7, 7, DEAD_WOOD[0]);
        set(image, 8, 8, DEAD_WOOD[0]);
        set(image, 8, 7, DEAD_WOOD[1]);
        set(image, 7, 8, DEAD_WOOD[1]);
        int[][] directions = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}};
        int[] direction = directions[random.nextInt(directions.length)];
        for (int step = 1; step <= 6; step++) {
            int x = 8 + step * direction[0] - (direction[0] < 0 ? 1 : 0);
            int y = 8 + step * direction[1] - (direction[1] < 0 ? 1 : 0);
            set(image, x, y, DEAD_WOOD[0]);
        }
        rim(image, random, DEAD_BARK, 0.0, 0.7);
    }

    /** The same end grain as the log (same pixels), with a rim of bleached wood instead of bark. */
    private static void strippedDeadLogTop(BufferedImage image, Random random) {
        copy(renderUnchecked("block/dead_log_top"), image);
        rim(image, random, DEAD_WOOD, 0.0, 0.45);
    }

    /**
     * Four planks of four rows (like vanilla's layout): dark seam row, lit top row, staggered butt joints, horizontal
     * grain streaks and one weathered crack.
     */
    private static void deadPlanks(BufferedImage image, Random random) {
        int[] joints = {4, 11, 1, 8};
        for (int plank = 0; plank < 4; plank++) {
            double plankShade = 0.3 + random.nextDouble() * 0.35;
            for (int row = 0; row < 4; row++) {
                int y = plank * 4 + row;
                for (int x = 0; x < SIZE; x++) {
                    if (row == 3) {
                        set(image, x, y, DEAD_WOOD[0]);
                    } else if (x == joints[plank]) {
                        set(image, x, y, DEAD_WOOD[0]);
                    } else {
                        double lit = row == 0 ? 0.2 : 0.0;
                        set(image, x, y, shade(DEAD_WOOD, plankShade + lit + (random.nextDouble() - 0.5) * 0.3));
                    }
                }
            }
            for (int i = 0; i < 2; i++) {
                int y = plank * 4 + 1 + random.nextInt(2);
                int x = random.nextInt(SIZE);
                int length = 3 + random.nextInt(4);
                for (int j = 0; j < length; j++) {
                    if ((x + j) % SIZE != joints[plank]) {
                        setWrapped(image, x + j, y, DEAD_WOOD[1]);
                    }
                }
            }
        }
        int plank = random.nextInt(4);
        int x = random.nextInt(SIZE - 6);
        for (int j = 0; j < 5; j++) {
            set(image, x + j, plank * 4 + 1 + (j / 3), DEAD_WOOD[0]);
        }
    }

    // ---- Door and trapdoor ----------------------------------------------------------------------------------

    /** Upper door half: boards, two window slits with a dark rim, a nailed rail. */
    private static void deadDoorTop(BufferedImage image, Random random) {
        doorBoards(image, random);
        frame(image, random, true, false);
        rail(image, random, 11);
        window(image, 3, 3, 3, 5);
        window(image, 10, 3, 3, 5);
    }

    /** Lower door half: boards, two nailed rails with a diagonal brace, a handle just below the middle of the door. */
    private static void deadDoorBottom(BufferedImage image, Random random) {
        doorBoards(image, random);
        frame(image, random, false, true);
        rail(image, random, 2);
        rail(image, random, 12);
        for (int i = 0; i < 8; i++) {
            int x = 3 + i * 10 / 7;
            int y = 11 - i;
            set(image, x, y, DEAD_BARK[2]);
            set(image, x + 1, y, DEAD_BARK[1]);
        }
        set(image, 12, 0, DEAD_BARK[0]);
        set(image, 12, 1, DEAD_BARK[0]);
        set(image, 11, 1, DEAD_BARK[3]);
    }

    /** Boards in a dark frame, a nailed cross rail and four square holes. */
    private static void deadTrapdoor(BufferedImage image, Random random) {
        doorBoards(image, random);
        frame(image, random, true, true);
        rail(image, random, 7);
        window(image, 3, 2, 3, 3);
        window(image, 10, 2, 3, 3);
        window(image, 3, 11, 3, 3);
        window(image, 10, 11, 3, 3);
    }

    /**
     * The door in the inventory, drawn at item scale (8 wide, columns 4–11) with the features of the block halves:
     * two window slits, rails, the diagonal brace and the handle.
     */
    private static void deadDoorItem(BufferedImage image, Random random) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 4; x < 12; x++) {
                boolean edge = x == 4 || x == 11 || y == 0 || y == SIZE - 1;
                if (edge) {
                    set(image, x, y, shade(DEAD_BARK, 0.15 + random.nextDouble() * 0.45));
                } else if (x == 8) {
                    set(image, x, y, DEAD_WOOD[0]);
                } else {
                    set(image, x, y, shade(DEAD_WOOD, 0.35 + random.nextDouble() * 0.4));
                }
            }
        }
        for (int y = 2; y <= 4; y++) {
            image.setRGB(6, y, TRANSPARENT);
            image.setRGB(9, y, TRANSPARENT);
        }
        for (int y : new int[] {6, 9, 14}) {
            for (int x = 5; x < 11; x++) {
                set(image, x, y, DEAD_BARK[2]);
            }
        }
        for (int i = 0; i < 4; i++) {
            set(image, 6 + i, 13 - i, DEAD_BARK[1]);
        }
        set(image, 10, 8, DEAD_BARK[0]);
    }

    /** Three vertical boards (seams at x 5 and 10) with their own tone and vertical grain. */
    private static void doorBoards(BufferedImage image, Random random) {
        double[] boards = {0.3 + random.nextDouble() * 0.35, 0.3 + random.nextDouble() * 0.35, 0.3 + random.nextDouble() * 0.35};
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (x == 5 || x == 10) {
                    set(image, x, y, DEAD_WOOD[0]);
                } else {
                    double board = boards[x < 5 ? 0 : x < 10 ? 1 : 2];
                    set(image, x, y, shade(DEAD_WOOD, board + (random.nextDouble() - 0.5) * 0.3));
                }
            }
        }
    }

    /** Weathered dark edges left and right, and optionally along the top and bottom row. */
    private static void frame(BufferedImage image, Random random, boolean top, boolean bottom) {
        for (int i = 0; i < SIZE; i++) {
            set(image, 0, i, shade(DEAD_BARK, 0.2 + random.nextDouble() * 0.5));
            set(image, SIZE - 1, i, shade(DEAD_BARK, 0.2 + random.nextDouble() * 0.5));
            if (top) {
                set(image, i, 0, shade(DEAD_BARK, 0.2 + random.nextDouble() * 0.5));
            }
            if (bottom) {
                set(image, i, SIZE - 1, shade(DEAD_BARK, 0.2 + random.nextDouble() * 0.5));
            }
        }
    }

    /** A two-row horizontal rail (lit top row) with a nail near each end. */
    private static void rail(BufferedImage image, Random random, int y) {
        for (int x = 1; x < SIZE - 1; x++) {
            set(image, x, y, shade(DEAD_BARK, 0.6 + random.nextDouble() * 0.4));
            set(image, x, y + 1, shade(DEAD_BARK, 0.35 + random.nextDouble() * 0.3));
        }
        set(image, 2, y, DEAD_BARK[0]);
        set(image, SIZE - 3, y, DEAD_BARK[0]);
    }

    /** A see-through hole with a dark rim; cutout textures only use alpha 0 or 255. */
    private static void window(BufferedImage image, int x0, int y0, int width, int height) {
        for (int y = y0 - 1; y <= y0 + height; y++) {
            for (int x = x0 - 1; x <= x0 + width; x++) {
                boolean inside = x >= x0 && x < x0 + width && y >= y0 && y < y0 + height;
                if (inside) {
                    image.setRGB(x, y, TRANSPARENT);
                } else {
                    set(image, x, y, DEAD_BARK[1]);
                }
            }
        }
    }

    // ---- Soul crystal ---------------------------------------------------------------------------------------

    /** Overlay over netherrack (also the bottom of the crimson variant): four upright clusters. */
    private static void soulCrystalOreOverlay(BufferedImage image, Random random) {
        crystalClusters(image, random, 4, 0);
    }

    /** Overlay over the crimson nylium top: crystal tips poking up through the nylium, seen from above. */
    private static void crimsonSoulCrystalOreOverlayTop(BufferedImage image, Random random) {
        int[][] large = {
                {SOUL_CRYSTAL[4], SOUL_CRYSTAL[4], SOUL_CRYSTAL[3]},
                {SOUL_CRYSTAL[3], SOUL_CRYSTAL[5], SOUL_CRYSTAL[2]},
                {SOUL_CRYSTAL[2], SOUL_CRYSTAL[1], SOUL_CRYSTAL[1]}};
        int[][] small = {
                {SOUL_CRYSTAL[4], SOUL_CRYSTAL[3]},
                {SOUL_CRYSTAL[2], SOUL_CRYSTAL[1]}};
        int placed = 0;
        for (int attempt = 0; attempt < 300 && placed < 6; attempt++) {
            // A tip seen from above: lit top-left, shadowed bottom-right, highlight on the point.
            int[][] shape = random.nextInt(3) == 0 ? small : large;
            BufferedImage tip = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            int x0 = 1 + random.nextInt(SIZE - 1 - shape.length);
            int y0 = 1 + random.nextInt(SIZE - 1 - shape.length);
            for (int y = 0; y < shape.length; y++) {
                for (int x = 0; x < shape.length; x++) {
                    set(tip, x0 + x, y0 + y, shape[y][x]);
                }
            }
            outline(tip);
            if (merge(image, tip)) {
                placed++;
            }
        }
    }

    /** Overlay over the crimson nylium side: clusters below the top 6 rows, so the red nylium edge stays visible. */
    private static void crimsonSoulCrystalOreOverlaySide(BufferedImage image, Random random) {
        crystalClusters(image, random, 3, 6);
    }

    /** The item: a tall hexagonal crystal between a smaller and a tiny one; no overlap, dark outlines touch. */
    private static void soulCrystalItem(BufferedImage image, Random random) {
        crystal(image, 8, 1, 14, 3);
        crystal(image, 3, 7, 14, 2);
        crystal(image, 13, 9, 14, 2);
    }

    /**
     * Up to {@code count} clusters of 1–3 upright shards (the middle one tallest) with a dark outline, at least one
     * pixel apart, entirely inside the tile and with nothing above row {@code minTop} (ore overlays do not wrap).
     */
    private static void crystalClusters(BufferedImage image, Random random, int count, int minTop) {
        int placed = 0;
        for (int attempt = 0; attempt < 500 && placed < count; attempt++) {
            int shards = 1 + random.nextInt(3);
            int[] widths = new int[shards];
            int[] heights = new int[shards];
            int totalWidth = 0;
            int maxHeight = 0;
            for (int i = 0; i < shards; i++) {
                boolean middle = i == shards / 2;
                widths[i] = middle ? 2 + random.nextInt(2) : 2;
                heights[i] = middle ? 4 + random.nextInt(3) : 3 + random.nextInt(2);
                totalWidth += widths[i];
                maxHeight = Math.max(maxHeight, heights[i]);
            }
            // Outline included: columns x0 - 1 … x0 + totalWidth, rows baseline - maxHeight … baseline + 1.
            int minBaseline = minTop + maxHeight;
            if (totalWidth + 2 > SIZE || minBaseline > SIZE - 2) {
                continue;
            }
            int x0 = 1 + random.nextInt(SIZE - 1 - totalWidth);
            int baseline = minBaseline + random.nextInt(SIZE - 1 - minBaseline);
            BufferedImage cluster = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            int x = x0;
            for (int i = 0; i < shards; i++) {
                shard(cluster, x, baseline, heights[i], widths[i]);
                x += widths[i];
            }
            outline(cluster);
            if (merge(image, cluster)) {
                placed++;
            }
        }
    }

    /**
     * An upright shard, 2 or 3 wide: light left face, (base middle), shadow right face, a pointed highlighted tip and a
     * dark foot.
     */
    private static void shard(BufferedImage image, int x, int baseline, int height, int width) {
        int tipColumn = width == 3 ? x + 1 : x;
        for (int i = 0; i < height; i++) {
            int y = baseline - i;
            boolean top = i == height - 1;
            for (int column = x; column < x + width; column++) {
                if (top && column != tipColumn) {
                    continue;
                }
                int color;
                if (top) {
                    color = SOUL_CRYSTAL[5];
                } else if (i == 0) {
                    color = column == x ? SOUL_CRYSTAL[2] : SOUL_CRYSTAL[1];
                } else if (column == x) {
                    color = i == height - 2 ? SOUL_CRYSTAL[4] : SOUL_CRYSTAL[3];
                } else if (column == x + width - 1) {
                    color = SOUL_CRYSTAL[1];
                } else {
                    color = SOUL_CRYSTAL[2];
                }
                set(image, column, y, color);
            }
        }
    }

    /**
     * An upright hexagonal crystal centred on column {@code cx} from row {@code top} to {@code bottom}, at most
     * {@code halfWidth} pixels to each side: dark outline, light left facets, base middle, shadow right facets.
     */
    private static void crystal(BufferedImage image, int cx, int top, int bottom, int halfWidth) {
        for (int y = top; y <= bottom; y++) {
            int width = Math.min(halfWidth, Math.min(y - top, bottom - y) + 1);
            for (int x = cx - width; x < cx + width; x++) {
                boolean edge = x == cx - width || x == cx + width - 1 || y == top || y == bottom;
                int color;
                if (edge) {
                    color = SOUL_CRYSTAL[0];
                } else if (x < cx - 1) {
                    color = SOUL_CRYSTAL[3];
                } else if (x == cx - 1) {
                    color = y < top + 3 ? SOUL_CRYSTAL[5] : SOUL_CRYSTAL[4];
                } else if (x == cx) {
                    color = SOUL_CRYSTAL[2];
                } else {
                    color = SOUL_CRYSTAL[1];
                }
                set(image, x, y, color);
            }
        }
    }

    /** Every transparent pixel next to (4-neighbourhood) an opaque one becomes outline. */
    private static void outline(BufferedImage image) {
        boolean[][] opaque = new boolean[SIZE][SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                opaque[x][y] = isOpaque(image, x, y);
            }
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                boolean next = (x > 0 && opaque[x - 1][y]) || (x < SIZE - 1 && opaque[x + 1][y])
                        || (y > 0 && opaque[x][y - 1]) || (y < SIZE - 1 && opaque[x][y + 1]);
                if (!opaque[x][y] && next) {
                    set(image, x, y, SOUL_CRYSTAL[0]);
                }
            }
        }
    }

    /**
     * Copies the opaque pixels of {@code part} into {@code image} if none of them touches (4-neighbourhood) a pixel
     * that is already opaque; returns whether it did.
     */
    private static boolean merge(BufferedImage image, BufferedImage part) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (isOpaque(part, x, y) && (isOpaque(image, x, y) || isOpaque(image, x - 1, y)
                        || isOpaque(image, x + 1, y) || isOpaque(image, x, y - 1) || isOpaque(image, x, y + 1))) {
                    return false;
                }
            }
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (isOpaque(part, x, y)) {
                    image.setRGB(x, y, part.getRGB(x, y));
                }
            }
        }
        return true;
    }

    /**
     * The animation strip: {@link #PULSE_FRAMES} frames under each other, each the rest frame darkened (dim) or mixed
     * towards {@link #PULSE_GLOW} (bright) along a cosine. Only colours change; the alpha mask stays identical, so
     * interpolation never creates half-transparent pixels.
     */
    private static BufferedImage pulseStrip(BufferedImage frame) {
        BufferedImage strip = new BufferedImage(SIZE, SIZE * PULSE_FRAMES, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < PULSE_FRAMES; i++) {
            double glow = 0.5 - 0.5 * Math.cos(2 * Math.PI * i / PULSE_FRAMES);
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    int argb = frame.getRGB(x, y);
                    if ((argb >>> 24) == 0) {
                        strip.setRGB(x, i * SIZE + y, TRANSPARENT);
                    } else {
                        int rgb = mix(mix(argb & 0xFFFFFF, 0x000000, 0.22 * (1 - glow)), PULSE_GLOW, 0.28 * glow);
                        set(strip, x, i * SIZE + y, rgb);
                    }
                }
            }
        }
        return strip;
    }

    // ---- Shared patterns ------------------------------------------------------------------------------------

    /**
     * A vertical line that wanders sideways along a sine with a whole number of periods per tile (so it tiles
     * vertically), with a shadow pixel to its right; no pixel at row {@code breakAt} (-1: unbroken).
     */
    private static void furrow(BufferedImage image, Random random, int x0, int color, int shadow, int breakAt) {
        int periods = 1 + random.nextInt(2);
        double phase = random.nextDouble() * 2 * Math.PI;
        boolean withShadow = shadow != color;
        for (int y = 0; y < SIZE; y++) {
            if (y == breakAt) {
                continue;
            }
            int x = x0 + (int) Math.round(Math.sin(2 * Math.PI * periods * y / SIZE + phase));
            setWrapped(image, x, y, color);
            if (withShadow && random.nextInt(3) != 0) {
                setWrapped(image, x + 1, y, shadow);
            }
        }
    }

    /** A hairline crack of {@code length} pixels downwards, stepping at most one pixel sideways per row. */
    private static void crack(BufferedImage image, Random random, int x, int y, int length, int color) {
        for (int i = 0; i < length; i++) {
            setWrapped(image, x, y + i, color);
            x += random.nextInt(3) - 1;
        }
    }

    /** The outermost ring of pixels, shaded from {@code palette} between {@code from} and {@code to}. */
    private static void rim(BufferedImage image, Random random, int[] palette, double from, double to) {
        for (int i = 0; i < SIZE; i++) {
            set(image, i, 0, shade(palette, from + random.nextDouble() * (to - from)));
            set(image, i, SIZE - 1, shade(palette, from + random.nextDouble() * (to - from)));
            set(image, 0, i, shade(palette, from + random.nextDouble() * (to - from)));
            set(image, SIZE - 1, i, shade(palette, from + random.nextDouble() * (to - from)));
        }
    }

    /** A value in [0, 1) per column, smoothed with both neighbours (wrapping), for vertical grain and ragged bands. */
    private static double[] smoothColumns(Random random) {
        double[] raw = new double[SIZE];
        for (int x = 0; x < SIZE; x++) {
            raw[x] = random.nextDouble();
        }
        double[] columns = new double[SIZE];
        for (int x = 0; x < SIZE; x++) {
            columns[x] = (raw[(x + SIZE - 1) % SIZE] + 2 * raw[x] + raw[(x + 1) % SIZE]) / 4;
        }
        return columns;
    }

    // ---- Preview --------------------------------------------------------------------------------------------

    /**
     * One row per texture: name, the rest frame 8×, a 3×3 tiling at 8× (seams and repetition) and, if animated, every
     * frame at 4×. Transparency over a dark red backdrop, roughly the tone of netherrack and crimson nylium.
     */
    private static void writePreview() throws IOException {
        int scale = 8;
        int tile = SIZE * scale;
        int label = 220;
        int frameScale = 4;
        int width = label + tile + 16 + 3 * tile + 16 + PULSE_FRAMES * (SIZE * frameScale + 4);
        int height = TEXTURES.size() * (3 * tile + 16);
        BufferedImage sheet = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = sheet.createGraphics();
        graphics.setColor(new Color(0x2B2B2B));
        graphics.fillRect(0, 0, width, height);
        int row = 0;
        for (Map.Entry<String, Texture> entry : TEXTURES.entrySet()) {
            BufferedImage frame = render(entry.getKey());
            int top = row * (3 * tile + 16) + 8;
            graphics.setColor(Color.WHITE);
            graphics.drawString(entry.getKey() + (HAND_EDITED.contains(entry.getKey()) ? " (hand)" : ""), 8, top + 16);
            draw(graphics, frame, label, top, scale);
            for (int ty = 0; ty < 3; ty++) {
                for (int tx = 0; tx < 3; tx++) {
                    draw(graphics, frame, label + tile + 16 + tx * tile, top + ty * tile, scale);
                }
            }
            if (entry.getValue().animated()) {
                BufferedImage strip = pulseStrip(frame);
                for (int i = 0; i < PULSE_FRAMES; i++) {
                    BufferedImage single = strip.getSubimage(0, i * SIZE, SIZE, SIZE);
                    draw(graphics, single, label + 4 * tile + 32 + i * (SIZE * frameScale + 4), top, frameScale);
                }
            }
            row++;
        }
        graphics.dispose();
        Files.createDirectories(PREVIEW.getParent());
        ImageIO.write(sheet, "png", PREVIEW.toFile());
        System.out.println("wrote " + PREVIEW);
    }

    private static void draw(Graphics2D graphics, BufferedImage image, int left, int top, int scale) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int argb = image.getRGB(x, y);
                graphics.setColor(new Color((argb >>> 24) == 0 ? 0x5A2626 : argb & 0xFFFFFF));
                graphics.fillRect(left + x * scale, top + y * scale, scale, scale);
            }
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

    /** Palette colour for {@code t} in [0, 1] (clamped), dark to light: quantises everything onto the palette. */
    private static int shade(int[] palette, double t) {
        int index = (int) (Math.max(0, Math.min(0.999, t)) * palette.length);
        return palette[index];
    }

    /** Per-channel mix of two RGB colours. */
    private static int mix(int from, int to, double t) {
        int r = (int) Math.round(lerp(from >> 16 & 0xFF, to >> 16 & 0xFF, t));
        int g = (int) Math.round(lerp(from >> 8 & 0xFF, to >> 8 & 0xFF, t));
        int b = (int) Math.round(lerp(from & 0xFF, to & 0xFF, t));
        return r << 16 | g << 8 | b;
    }

    private static void copy(BufferedImage from, BufferedImage to) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                to.setRGB(x, y, from.getRGB(x, y));
            }
        }
    }

    private static boolean isOpaque(BufferedImage image, int x, int y) {
        return x >= 0 && y >= 0 && x < SIZE && y < SIZE && (image.getRGB(x, y) >>> 24) != 0;
    }

    /** Opaque pixel; every texture uses alpha 0 or 255 only (cutout-safe). */
    private static void set(BufferedImage image, int x, int y, int rgb) {
        image.setRGB(x, y, 0xFF000000 | rgb);
    }

    /** {@link #set} wrapping around the tile edges, for seamless textures. */
    private static void setWrapped(BufferedImage image, int x, int y, int rgb) {
        set(image, Math.floorMod(x, SIZE), Math.floorMod(y, SIZE), rgb);
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
