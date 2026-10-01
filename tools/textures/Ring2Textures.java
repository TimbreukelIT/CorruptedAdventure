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
 * Generates the ring 2 block and item textures (Nether ring 2 included, later) from our own pixels: no vanilla pixels
 * are read or copied. The one texture script for ring 2: later ring 2 changes (palm, sunstone, turquoise, salt,
 * Nether ring 2) extend this file. Run from the repository root: {@code java tools/textures/Ring2Textures.java}.
 *
 * <p>Deterministic: every texture has a fixed seed (hash of its file name), so running twice gives identical PNGs.
 * Textures listed in {@link #HAND_EDITED} are never overwritten. Every texture is checked before it is written (size,
 * alpha 0/255 only, the same alpha mask in every animation frame). A contact sheet (8× and 3×3 tiled, all animation
 * frames) goes to {@code build/texture-preview/ring2.png} for review without the game.
 *
 * <p>Placeholders (change 21): simple first versions on working palettes; the final ring 2 style follows in change 37
 * (style rules in {@code fase-1-wereld.md} §1.4). The shared helpers are copied from {@code Ring1Textures.java}: a
 * single-file script cannot load a second source file.
 */
public final class Ring2Textures {
    private static final int SIZE = 16;
    private static final int PARTICLE_SIZE = 8;
    private static final int TRANSPARENT = 0x00000000;
    private static final Path OUTPUT = Path.of("src/main/resources/assets/corruptedadventure/textures");
    private static final Path PREVIEW = Path.of("build/texture-preview/ring2.png");

    /** Retouched by hand after generation; the script skips these and the preview reads them from disk. */
    private static final Set<String> HAND_EDITED = Set.of();

    // Working palettes, dark to light (RGB). Placeholders: change 37 decides the final colours.
    private static final int[] RED_CANYON_STONE = {0x6E2E1E, 0x8A3B26, 0xA44C30, 0xBC6040, 0xCF7A55};
    private static final int[] ORANGE_CANYON_STONE = {0x8E4A1C, 0xA85E24, 0xC2742E, 0xD68B3E, 0xE4A55A};
    private static final int[] CREAM_CANYON_STONE = {0xB49C7A, 0xC7B08C, 0xD8C39F, 0xE6D4B2, 0xF1E3C6};
    private static final int CANYON_MORTAR = 0x4A2418;
    private static final int[] SCORCHED_SANDSTONE = {0x4A3A2A, 0x6B5236, 0x8A6B45, 0xA88656, 0xC2A06C};
    private static final int SOOT = 0x2A221C;
    private static final int[] LOOSE_SAND = {0xC9B98A, 0xD6C799, 0xDFD1A5, 0xE8DCB2};
    private static final int[] CRACKED_CLAY = {0x7C6A58, 0x92806A, 0xA8957C, 0xBCA98F};
    private static final int CLAY_CRACK = 0x4E4236;
    private static final int[] DRY_SHRUB = {0x5A4630, 0x75603F, 0x917A52};
    private static final int[] BARREL_CACTUS = {0x2F4A2A, 0x3F6136, 0x557A44, 0x6E9455};
    private static final int CACTUS_SPINE = 0xE6D9A8;
    private static final int[] DESERT_FLOWER_PETAL = {0xA8326E, 0xC94C8A, 0xE27AAB};
    private static final int[] DESERT_FLOWER_STEM = {0x4F6A33, 0x6B8A45};

    /**
     * How a texture moves: {@code frames} frames of {@code frametime} ticks, optionally interpolated; {@code glow} is
     * the colour bright frames mix towards (per material). {@link #STILL} for textures without animation.
     */
    private record Animation(int frames, int frametime, boolean interpolate, int glow) {
        static final Animation STILL = new Animation(1, 1, false, 0);

        boolean animated() {
            return frames > 1;
        }
    }

    private record Texture(Painter painter, Animation animation) {
    }

    @FunctionalInterface
    private interface Painter {
        void paint(BufferedImage image, Random random);
    }

    /** Pulse used by glowing ores and crystals, as in ring 1: 8 frames, frame time 4, interpolated. */
    private static final int PULSE_FRAMES = 8;
    private static final int PULSE_FRAMETIME = 4;

    private static final Map<String, Texture> TEXTURES = new LinkedHashMap<>();

    static {
        still("block/red_canyon_stone", (image, random) -> canyonStone(image, random, RED_CANYON_STONE));
        still("block/orange_canyon_stone", (image, random) -> canyonStone(image, random, ORANGE_CANYON_STONE));
        still("block/cream_canyon_stone", (image, random) -> canyonStone(image, random, CREAM_CANYON_STONE));
        still("block/canyon_cobblestone", Ring2Textures::canyonCobblestone);
        still("block/canyon_stone_bricks", Ring2Textures::canyonStoneBricks);
        still("block/scorched_sandstone_top", Ring2Textures::scorchedSandstoneTop);
        still("block/scorched_sandstone", Ring2Textures::scorchedSandstoneSide);
        still("block/scorched_sandstone_bottom", Ring2Textures::scorchedSandstoneBottom);
        still("block/cut_scorched_sandstone", Ring2Textures::cutScorchedSandstone);
        still("block/chiseled_scorched_sandstone", Ring2Textures::chiseledScorchedSandstone);
        still("block/loose_sand", Ring2Textures::looseSand);
        still("block/cracked_clay", Ring2Textures::crackedClay);
        still("block/dry_shrub", Ring2Textures::dryShrub);
        still("block/desert_flower", Ring2Textures::desertFlower);
        still("block/barrel_cactus_top", Ring2Textures::barrelCactusTop);
        still("block/barrel_cactus_side", Ring2Textures::barrelCactusSide);
    }

    private static void still(String name, Painter painter) {
        TEXTURES.put(name, new Texture(painter, Animation.STILL));
    }

    /** An 8-frame pulse towards {@code glow}, the glow colour of this material. */
    private static void pulsing(String name, int glow, Painter painter) {
        TEXTURES.put(name, new Texture(painter, new Animation(PULSE_FRAMES, PULSE_FRAMETIME, true, glow)));
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
            Animation animation = entry.getValue().animation();
            BufferedImage output = animation.animated() ? pulseStrip(render(name), animation) : render(name);
            check(name, output, animation);
            Files.createDirectories(file.getParent());
            ImageIO.write(output, "png", file.toFile());
            System.out.println("wrote " + file);
            if (animation.animated()) {
                Path meta = OUTPUT.resolve(name + ".png.mcmeta");
                Files.writeString(meta, "{\n  \"animation\": {\n    \"frametime\": " + animation.frametime()
                        + ",\n    \"interpolate\": " + animation.interpolate() + "\n  }\n}\n", StandardCharsets.UTF_8);
                System.out.println("wrote " + meta);
            }
        }
        writePreview();
    }

    /** The texture as the game shows it at rest (first frame); hand-edited textures come from disk. */
    private static BufferedImage render(String name) throws IOException {
        int size = size(name);
        if (HAND_EDITED.contains(name)) {
            BufferedImage file = ImageIO.read(OUTPUT.resolve(name + ".png").toFile());
            return file.getSubimage(0, 0, size, size);
        }
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
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

    private static int size(String name) {
        return name.startsWith("particle/") ? PARTICLE_SIZE : SIZE;
    }

    /**
     * Fails the run on a texture the game would show wrongly: wrong size (16×16, or a strip of 16×16 frames; particles
     * 8×8), half-transparent pixels (cutout and mipmaps need alpha 0 or 255), or animation frames whose alpha mask
     * differs (interpolation would blend alpha).
     */
    private static void check(String name, BufferedImage image, Animation animation) {
        int width = size(name);
        if (image.getWidth() != width || image.getHeight() != width * animation.frames()) {
            throw new IllegalStateException(name + ": " + image.getWidth() + "×" + image.getHeight() + ", expected "
                    + width + "×" + width * animation.frames());
        }
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < width; x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                if (alpha != 0 && alpha != 255) {
                    throw new IllegalStateException(name + ": alpha " + alpha + " at " + x + "," + y);
                }
                boolean opaque = alpha != 0;
                if (opaque != ((image.getRGB(x, y % width) >>> 24) != 0)) {
                    throw new IllegalStateException(name + ": frame " + y / width + " has another alpha mask at " + x + "," + y % width);
                }
            }
        }
    }

    // ---- Canyon stone ---------------------------------------------------------------------------------------

    /**
     * Natural canyon stone: grain stretched horizontally (sedimentary), soft tint variation, a few dark pits and light
     * specks. No hard layer line inside a block: the strata come from bands of the three tints in the terrain.
     */
    private static void canyonStone(BufferedImage image, Random random, int[] palette) {
        double[][] streaks = valueNoise(random, 8, 2);
        double[][] fine = valueNoise(random, 4, 1);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, shade(palette, 0.1 + 0.45 * streaks[x][y] + 0.3 * fine[x][y] + 0.2 * random.nextDouble()));
            }
        }
        for (int i = 0; i < 5; i++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            int length = 1 + random.nextInt(3);
            for (int j = 0; j < length; j++) {
                setWrapped(image, x + j, y, palette[0]);
            }
            setWrapped(image, x, y - 1, palette[palette.length - 1]);
        }
    }

    /** Rounded stones in all three tints with a dark mortar between them (wrapping Voronoi cells). */
    private static void canyonCobblestone(BufferedImage image, Random random) {
        int[][] palettes = {RED_CANYON_STONE, ORANGE_CANYON_STONE, CREAM_CANYON_STONE};
        Cells cells = Cells.random(random, 7);
        int[] tint = new int[cells.count()];
        for (int i = 0; i < tint.length; i++) {
            tint[i] = i % palettes.length;
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                Cells.Nearest nearest = cells.nearest(x, y);
                if (nearest.secondDistance() - nearest.distance() < 0.7) {
                    set(image, x, y, CANYON_MORTAR);
                    continue;
                }
                // Lit towards the top left of each stone, shadowed towards the bottom right.
                double light = 0.55 - 0.12 * (nearest.dx() + nearest.dy()) + 0.2 * random.nextDouble();
                set(image, x, y, shade(palettes[tint[nearest.index()]], light));
            }
        }
    }

    /** Red canyon stone in vanilla's stone brick layout: rows of 8×4 bricks, offset by half a brick, dark joints. */
    private static void canyonStoneBricks(BufferedImage image, Random random) {
        for (int row = 0; row < 4; row++) {
            int offset = row % 2 == 0 ? 0 : 4;
            double[] brickShade = {0.35 + 0.25 * random.nextDouble(), 0.35 + 0.25 * random.nextDouble()};
            for (int dy = 0; dy < 4; dy++) {
                int y = row * 4 + dy;
                for (int x = 0; x < SIZE; x++) {
                    int inBrick = Math.floorMod(x - offset, 8);
                    int brick = Math.floorMod(x - offset, SIZE) / 8;
                    if (dy == 3 || inBrick == 7) {
                        set(image, x, y, CANYON_MORTAR);
                    } else if (dy == 0 || inBrick == 0) {
                        set(image, x, y, shade(RED_CANYON_STONE, brickShade[brick] + 0.3 + 0.1 * random.nextDouble()));
                    } else if (dy == 2 || inBrick == 6) {
                        set(image, x, y, shade(RED_CANYON_STONE, brickShade[brick] - 0.2 + 0.1 * random.nextDouble()));
                    } else {
                        set(image, x, y, shade(RED_CANYON_STONE, brickShade[brick] + 0.2 * (random.nextDouble() - 0.5)));
                    }
                }
            }
        }
    }

    // ---- Scorched sandstone ---------------------------------------------------------------------------------

    /** Grainy top (also the smooth variant and the ends of cut/chiseled) with a few soot specks. */
    private static void scorchedSandstoneTop(BufferedImage image, Random random) {
        double[][] noise = valueNoise(random, 4, 4);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, shade(SCORCHED_SANDSTONE, 0.4 + 0.35 * noise[x][y] + 0.25 * random.nextDouble()));
            }
        }
        soot(image, random, 3, 1);
    }

    /** Like vanilla sandstone's side: a lighter band on top, layered body, darker foot; soot smudges low down. */
    private static void scorchedSandstoneSide(BufferedImage image, Random random) {
        double[][] streaks = valueNoise(random, 8, 1);
        for (int y = 0; y < SIZE; y++) {
            double band = y < 3 ? 0.25 : y >= 13 ? -0.2 : 0.0;
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, shade(SCORCHED_SANDSTONE, 0.35 + band + 0.35 * streaks[x][y] + 0.15 * random.nextDouble()));
            }
        }
        for (int x = 0; x < SIZE; x++) {
            set(image, x, 3, SCORCHED_SANDSTONE[1]);
        }
        soot(image, random, 4, 8);
    }

    private static void scorchedSandstoneBottom(BufferedImage image, Random random) {
        double[][] noise = valueNoise(random, 4, 4);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, shade(SCORCHED_SANDSTONE, 0.2 + 0.35 * noise[x][y] + 0.25 * random.nextDouble()));
            }
        }
        soot(image, random, 6, 0);
    }

    /** Cut: a dark 1-pixel frame around a flat, lightly grained face with one inner groove. */
    private static void cutScorchedSandstone(BufferedImage image, Random random) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, shade(SCORCHED_SANDSTONE, 0.55 + 0.2 * random.nextDouble()));
            }
        }
        for (int x = 1; x < SIZE - 1; x++) {
            set(image, x, 7, SCORCHED_SANDSTONE[1]);
            set(image, x, 8, SCORCHED_SANDSTONE[4]);
        }
        rim(image, random, SCORCHED_SANDSTONE, 0.0, 0.35);
        soot(image, random, 2, 9);
    }

    /** Chiseled: the cut frame with a carved diamond (shadowed lower right) in the middle. */
    private static void chiseledScorchedSandstone(BufferedImage image, Random random) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, shade(SCORCHED_SANDSTONE, 0.55 + 0.2 * random.nextDouble()));
            }
        }
        for (int y = 2; y < SIZE - 2; y++) {
            for (int x = 2; x < SIZE - 2; x++) {
                int distance = Math.abs(x - 7) + Math.abs(y - 7);
                if (distance == 5 || distance == 2) {
                    set(image, x, y, x + y > 14 ? SCORCHED_SANDSTONE[0] : SCORCHED_SANDSTONE[1]);
                } else if (distance == 4 || distance == 1) {
                    set(image, x, y, SCORCHED_SANDSTONE[4]);
                }
            }
        }
        set(image, 7, 7, SOOT);
        rim(image, random, SCORCHED_SANDSTONE, 0.0, 0.35);
    }

    /** {@code count} small soot smudges (2–4 pixels) at or below row {@code minY}. */
    private static void soot(BufferedImage image, Random random, int count, int minY) {
        for (int i = 0; i < count; i++) {
            int x = random.nextInt(SIZE);
            int y = minY + random.nextInt(SIZE - minY);
            int pixels = 2 + random.nextInt(3);
            for (int j = 0; j < pixels; j++) {
                setWrapped(image, x, Math.min(SIZE - 1, y), j == 0 ? SOOT : SCORCHED_SANDSTONE[0]);
                x += random.nextInt(3) - 1;
                y += random.nextInt(2);
            }
        }
    }

    // ---- Loose sand and cracked clay ------------------------------------------------------------------------

    /** Sand in 2×2 clumps: coarser and a touch darker than plain sand, with dark gaps between some clumps. */
    private static void looseSand(BufferedImage image, Random random) {
        for (int cy = 0; cy < SIZE; cy += 2) {
            for (int cx = 0; cx < SIZE; cx += 2) {
                double clump = random.nextDouble();
                for (int y = cy; y < cy + 2; y++) {
                    for (int x = cx; x < cx + 2; x++) {
                        set(image, x, y, shade(LOOSE_SAND, 0.15 + 0.6 * clump + 0.25 * random.nextDouble()));
                    }
                }
                if (random.nextInt(5) == 0) {
                    set(image, cx + 1, cy + 1, LOOSE_SAND[0]);
                }
            }
        }
    }

    /** Dried mud: polygonal plates (wrapping Voronoi cells) parted by dark cracks, each plate lit along its top edge. */
    private static void crackedClay(BufferedImage image, Random random) {
        Cells cells = Cells.random(random, 6);
        double[] plate = new double[cells.count()];
        for (int i = 0; i < plate.length; i++) {
            plate[i] = 0.3 + 0.4 * random.nextDouble();
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                Cells.Nearest nearest = cells.nearest(x, y);
                double gap = nearest.secondDistance() - nearest.distance();
                if (gap < 0.9) {
                    set(image, x, y, CLAY_CRACK);
                } else {
                    double edge = gap < 1.9 && nearest.dy() < 0 ? 0.25 : 0.0;
                    set(image, x, y, shade(CRACKED_CLAY, plate[nearest.index()] + edge + 0.15 * random.nextDouble()));
                }
            }
        }
    }

    // ---- Flora (cross models and the cactus) ----------------------------------------------------------------

    /** Bare, forked twigs from one root at the bottom; transparent background (cutout). */
    private static void dryShrub(BufferedImage image, Random random) {
        twig(image, random, 8, 15, 0, 12, 0);
        twig(image, random, 7, 15, -1, 11, 0);
        twig(image, random, 8, 15, 1, 11, 0);
        twig(image, random, 7, 14, -1, 6, 1);
        twig(image, random, 9, 14, 1, 6, 1);
    }

    /** A twig growing up from (x, y), leaning {@code lean} per few rows, forking once or twice. */
    private static void twig(BufferedImage image, Random random, int x, int y, int lean, int length, int depth) {
        for (int i = 0; i < length && y >= 1 && x >= 0 && x < SIZE; i++) {
            set(image, x, y, DRY_SHRUB[i < 2 ? 0 : 1 + random.nextInt(2)]);
            y--;
            if (lean != 0 && random.nextInt(2) == 0) {
                x += lean;
            }
            if (depth < 2 && i > 2 && random.nextInt(3) == 0) {
                twig(image, random, x, y, lean == 0 ? (random.nextBoolean() ? 1 : -1) : -lean, length - i - 1, depth + 1);
            }
        }
    }

    /** A stem with two leaves and a five-petal flower head; transparent background (cutout). */
    private static void desertFlower(BufferedImage image, Random random) {
        for (int y = 8; y < SIZE; y++) {
            set(image, 7 + (y > 12 ? 1 : 0), y, DESERT_FLOWER_STEM[y % 3 == 0 ? 1 : 0]);
        }
        set(image, 6, 12, DESERT_FLOWER_STEM[1]);
        set(image, 5, 11, DESERT_FLOWER_STEM[1]);
        set(image, 9, 11, DESERT_FLOWER_STEM[1]);
        set(image, 10, 10, DESERT_FLOWER_STEM[0]);
        int[][] petals = {{7, 3}, {5, 5}, {9, 5}, {6, 7}, {8, 7}};
        for (int[] petal : petals) {
            for (int dy = 0; dy < 2; dy++) {
                for (int dx = 0; dx < 2; dx++) {
                    set(image, petal[0] + dx - (petal[0] > 7 ? 1 : 0), petal[1] + dy,
                            DESERT_FLOWER_PETAL[1 + random.nextInt(2)]);
                }
            }
        }
        set(image, 7, 5, CACTUS_SPINE);
        set(image, 7, 6, DESERT_FLOWER_PETAL[0]);
        set(image, 6, 6, DESERT_FLOWER_PETAL[0]);
    }

    /**
     * Seen from above (the model uses pixels 3–12): ribs as spokes from a lighter crown, spines on the ribs, darker
     * towards the rim.
     */
    private static void barrelCactusTop(BufferedImage image, Random random) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double dx = x - 7.5;
                double dy = y - 7.5;
                double radius = Math.hypot(dx, dy);
                double angle = Math.atan2(dy, dx);
                boolean rib = Math.cos(angle * 8) > 0.55;
                double light = 0.85 - radius / 8.0 + (rib ? 0.2 : -0.1) + 0.1 * random.nextDouble();
                set(image, x, y, shade(BARREL_CACTUS, light));
                if (rib && radius > 2 && radius < 5.5 && random.nextInt(4) == 0) {
                    set(image, x, y, CACTUS_SPINE);
                }
            }
        }
        set(image, 7, 7, CACTUS_SPINE);
        set(image, 8, 8, CACTUS_SPINE);
    }

    /** Vertical ribs (lit ridge, shadowed groove) with spines along the ridges; the model shows rows 6–15. */
    private static void barrelCactusSide(BufferedImage image, Random random) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int phase = Math.floorMod(x - 3, 3);
                double light = phase == 0 ? 0.85 : phase == 1 ? 0.5 : 0.15;
                set(image, x, y, shade(BARREL_CACTUS, light + 0.15 * (random.nextDouble() - 0.5)));
            }
        }
        for (int x = 3; x < 13; x += 3) {
            for (int y = 7; y < SIZE; y += 2 + random.nextInt(2)) {
                set(image, x, y, CACTUS_SPINE);
            }
        }
    }

    // ---- Animation ------------------------------------------------------------------------------------------

    /**
     * The animation strip: the frames under each other, each the rest frame darkened (dim) or mixed towards the
     * material's glow colour (bright) along a cosine. Only colours change; the alpha mask stays identical, so
     * interpolation never creates half-transparent pixels.
     */
    private static BufferedImage pulseStrip(BufferedImage frame, Animation animation) {
        int size = frame.getWidth();
        BufferedImage strip = new BufferedImage(size, size * animation.frames(), BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < animation.frames(); i++) {
            double glow = 0.5 - 0.5 * Math.cos(2 * Math.PI * i / animation.frames());
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int argb = frame.getRGB(x, y);
                    if ((argb >>> 24) == 0) {
                        strip.setRGB(x, i * size + y, TRANSPARENT);
                    } else {
                        int rgb = mix(mix(argb & 0xFFFFFF, 0x000000, 0.22 * (1 - glow)), animation.glow(), 0.28 * glow);
                        set(strip, x, i * size + y, rgb);
                    }
                }
            }
        }
        return strip;
    }

    // ---- Shared patterns ------------------------------------------------------------------------------------

    /** Random cell centres in a wrapping tile, for stones, plates and cracks. */
    private record Cells(double[][] points) {
        record Nearest(int index, double distance, double secondDistance, double dx, double dy) {
        }

        static Cells random(Random random, int count) {
            double[][] points = new double[count][2];
            for (double[] point : points) {
                point[0] = random.nextDouble() * SIZE;
                point[1] = random.nextDouble() * SIZE;
            }
            return new Cells(points);
        }

        int count() {
            return points.length;
        }

        /** Nearest and second-nearest centre (wrapping), and the offset to the nearest one scaled to about [-1, 1]. */
        Nearest nearest(int x, int y) {
            int best = -1;
            double first = Double.MAX_VALUE;
            double second = Double.MAX_VALUE;
            double bestDx = 0;
            double bestDy = 0;
            for (int i = 0; i < points.length; i++) {
                double dx = wrap(x + 0.5 - points[i][0]);
                double dy = wrap(y + 0.5 - points[i][1]);
                double distance = Math.hypot(dx, dy);
                if (distance < first) {
                    second = first;
                    first = distance;
                    best = i;
                    bestDx = dx;
                    bestDy = dy;
                } else if (distance < second) {
                    second = distance;
                }
            }
            return new Nearest(best, first, second, bestDx / 4.0, bestDy / 4.0);
        }

        private static double wrap(double delta) {
            return delta - SIZE * Math.round(delta / SIZE);
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

    // ---- Preview --------------------------------------------------------------------------------------------

    /**
     * One row per texture: name, the rest frame 8×, a 3×3 tiling at 8× (seams and repetition) and, if animated, every
     * frame at 4×. Transparency over a dusty brown backdrop, roughly the tone of the canyon floor.
     */
    private static void writePreview() throws IOException {
        int scale = 8;
        int tile = SIZE * scale;
        int label = 220;
        int frameScale = 4;
        int maxFrames = TEXTURES.values().stream().mapToInt(texture -> texture.animation().frames()).max().orElse(1);
        int width = label + tile + 16 + 3 * tile + 16 + maxFrames * (SIZE * frameScale + 4);
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
            Animation animation = entry.getValue().animation();
            if (animation.animated()) {
                BufferedImage strip = pulseStrip(frame, animation);
                int size = frame.getWidth();
                for (int i = 0; i < animation.frames(); i++) {
                    BufferedImage single = strip.getSubimage(0, i * size, size, size);
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
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                graphics.setColor(new Color((argb >>> 24) == 0 ? 0x6B4A32 : argb & 0xFFFFFF));
                graphics.fillRect(left + x * scale, top + y * scale, scale, scale);
            }
        }
    }

    // ---- Helpers (copied from Ring1Textures.java) -----------------------------------------------------------

    private static Random seeded(String name) {
        return new Random(name.hashCode());
    }

    /**
     * Tileable value noise in [0, 1): random values on a grid of {@code cellX} × {@code cellY} pixels, smoothly
     * interpolated. Wide, flat cells give horizontal streaks.
     */
    private static double[][] valueNoise(Random random, int cellX, int cellY) {
        int pointsX = SIZE / cellX;
        int pointsY = SIZE / cellY;
        double[][] grid = new double[pointsX][pointsY];
        for (int i = 0; i < pointsX; i++) {
            for (int j = 0; j < pointsY; j++) {
                grid[i][j] = random.nextDouble();
            }
        }
        double[][] noise = new double[SIZE][SIZE];
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                int gx = x / cellX;
                int gy = y / cellY;
                double fx = smoothstep((x % cellX) / (double) cellX);
                double fy = smoothstep((y % cellY) / (double) cellY);
                double top = lerp(grid[gx][gy], grid[(gx + 1) % pointsX][gy], fx);
                double bottom = lerp(grid[gx][(gy + 1) % pointsY], grid[(gx + 1) % pointsX][(gy + 1) % pointsY], fx);
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

    private Ring2Textures() {
    }
}
