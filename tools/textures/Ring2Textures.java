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

    /** Preview only: each ore overlay over every host rock texture it is used on (overlay, base). */
    private static final String[][] ORE_PREVIEWS = {
            {"block/sunstone_ore_overlay", "block/red_canyon_stone"},
            {"block/sunstone_ore_overlay", "block/orange_canyon_stone"},
            {"block/sunstone_ore_overlay", "block/cream_canyon_stone"},
            {"block/sunstone_ore_overlay", "block/scorched_sandstone"},
            {"block/turquoise_ore_overlay", "block/red_canyon_stone"},
            {"block/turquoise_ore_overlay", "block/orange_canyon_stone"},
            {"block/turquoise_ore_overlay", "block/cream_canyon_stone"}};

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
    private static final int[] PALM_BARK = {0x463B30, 0x5C4F40, 0x716250, 0x877762, 0x9C8C75};
    private static final int[] PALM_WOOD = {0x9A7F52, 0xB49862, 0xC9AE76, 0xDAC28A, 0xE8D3A0};
    private static final int[] PALM_LEAVES = {0x28561C, 0x377425, 0x4A9030, 0x62AA3E};
    // Ore working palettes (change 24, placeholders; final in change 37): outline → highlight.
    private static final int[] SUNSTONE = {0x5A2A08, 0x9A4A0C, 0xD9801A, 0xF2B23A, 0xFFE07A, 0xFFF6C8};
    private static final int[] TURQUOISE = {0x0E3B3A, 0x1C6B66, 0x2E9C92, 0x4FC6B5, 0x8FE6D6};
    private static final int TURQUOISE_VEIN = 0x3A2A1E;
    // Salt working palettes (change 29, placeholders; final in change 37): dark to light.
    private static final int[] SALT = {0xB8AE9A, 0xCFC7B6, 0xE2DCCF, 0xEFEBE2, 0xF7F5EF};
    private static final int SALT_RIDGE = 0xFFFFFF;
    private static final int SALT_RIDGE_SHADOW = 0xC9C0AE;
    private static final int SALT_OUTLINE = 0x8A8070;
    private static final int[] SALT_CRYSTAL = {0xBFCFD8, 0xDCE6EB, 0xF1F5F7, 0xFFFFFF};
    private static final int SALT_CRYSTAL_BLUSH = 0xEBD9D6;
    // Nether ring 2 working palettes (change 33, placeholders; final in change 37): dark to light.
    private static final int[] SOAKED_NETHERRACK = {0x2E1A1A, 0x47282A, 0x5E3634, 0x6F4A44};
    private static final int SOAKED_DAMP = 0x3E4A3C;
    private static final int SOAKED_GLOSS = 0x8A7A70;
    private static final int[] SPORE_MOSS = {0x3E4A3C, 0x556450, 0x6E7F66, 0x8C9A80};
    private static final int SPORE_DOT = 0xC8D4B0;
    private static final int[] DRIPSHROOM_BARK = {0x3A3440, 0x4E4A58, 0x66667A};
    private static final int[] DRIPSHROOM_SLIME = {0x5E8466, 0x7FA88A};
    private static final int[] DRIPSHROOM_WOOD = {0x6E7A6A, 0x87947F, 0xA0AC94, 0xB8C2A8};
    private static final int[] DRIPSHROOM_CAP = {0x4A3F55, 0x5F516C, 0x75688A};
    private static final int DRIPSHROOM_CAP_DROP = 0x9FC3A8;
    private static final int[] DRIPSHROOM_LAMP = {0xB8D27A, 0xCFE38F, 0xE3F0AE, 0xF4FAD2};
    private static final int[] GLOW_MOLD = {0x5FD6B8, 0x8BEACF, 0xC2F7E6};
    private static final int[] SLIME_STALACTITE = {0x4E8A4A, 0x6AA860, 0x8CC47A, 0xB2DE9E};

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
        still("block/palm_log", Ring2Textures::palmLog);
        still("block/palm_log_top", Ring2Textures::palmLogTop);
        still("block/stripped_palm_log", (image, random) -> strippedLog(image, random, PALM_WOOD));
        still("block/stripped_palm_log_top", (image, random) -> strippedLogTop(image, random, "block/palm_log_top", PALM_WOOD));
        still("block/palm_planks", (image, random) -> planks(image, random, PALM_WOOD));
        still("block/palm_door_top", (image, random) -> doorTop(image, random, PALM_WOOD, PALM_BARK));
        still("block/palm_door_bottom", (image, random) -> doorBottom(image, random, PALM_WOOD, PALM_BARK));
        still("block/palm_trapdoor", (image, random) -> trapdoor(image, random, PALM_WOOD, PALM_BARK));
        still("block/palm_leaves", Ring2Textures::palmLeaves);
        still("block/palm_sapling", Ring2Textures::palmSapling);
        still("item/palm_door", (image, random) -> doorItem(image, random, PALM_WOOD, PALM_BARK));
        still("block/sunstone_ore_overlay", Ring2Textures::sunstoneOreOverlay);
        still("block/turquoise_ore_overlay", Ring2Textures::turquoiseOreOverlay);
        still("item/raw_sunstone", Ring2Textures::rawSunstone);
        still("item/raw_turquoise", Ring2Textures::rawTurquoise);
        still("block/salt_crust_top", Ring2Textures::saltCrustTop);
        still("block/salt_crust_side", Ring2Textures::saltCrustSide);
        still("block/salt_block", Ring2Textures::saltBlock);
        still("block/polished_salt", Ring2Textures::polishedSalt);
        still("block/salt_crystal", Ring2Textures::saltCrystal);
        still("item/salt", Ring2Textures::saltItem);
        // Nether ring 2 (change 33).
        still("block/soaked_netherrack", Ring2Textures::soakedNetherrack);
        still("block/spore_moss", Ring2Textures::sporeMoss);
        still("block/spore_moss_side", Ring2Textures::sporeMossSide);
        still("block/dripshroom_stem", Ring2Textures::dripshroomStem);
        still("block/dripshroom_stem_top", Ring2Textures::dripshroomStemTop);
        still("block/stripped_dripshroom_stem", (image, random) -> strippedLog(image, random, DRIPSHROOM_WOOD));
        still("block/stripped_dripshroom_stem_top", (image, random) -> strippedLogTop(image, random, "block/dripshroom_stem_top", DRIPSHROOM_WOOD));
        still("block/dripshroom_planks", (image, random) -> planks(image, random, DRIPSHROOM_WOOD));
        still("block/dripshroom_door_top", (image, random) -> doorTop(image, random, DRIPSHROOM_WOOD, DRIPSHROOM_BARK));
        still("block/dripshroom_door_bottom", (image, random) -> doorBottom(image, random, DRIPSHROOM_WOOD, DRIPSHROOM_BARK));
        still("block/dripshroom_trapdoor", (image, random) -> trapdoor(image, random, DRIPSHROOM_WOOD, DRIPSHROOM_BARK));
        still("item/dripshroom_door", (image, random) -> doorItem(image, random, DRIPSHROOM_WOOD, DRIPSHROOM_BARK));
        still("block/dripshroom_cap", Ring2Textures::dripshroomCap);
        still("block/dripshroom_lamp", Ring2Textures::dripshroomLamp);
        still("block/glow_mold", Ring2Textures::glowMold);
        still("block/spore_sprouts", Ring2Textures::sporeSprouts);
        still("block/slime_stalactite_down_base", (image, random) -> slimeStalactite(image, random, 12, false));
        still("block/slime_stalactite_down_middle", (image, random) -> slimeStalactite(image, random, 10, false));
        still("block/slime_stalactite_down_frustum", (image, random) -> slimeStalactite(image, random, 8, false));
        still("block/slime_stalactite_down_tip_merge", (image, random) -> slimeStalactite(image, random, 6, false));
        still("block/slime_stalactite_down_tip", (image, random) -> slimeStalactite(image, random, 6, true));
        for (String thickness : new String[] {"base", "middle", "frustum", "tip_merge", "tip"}) {
            String down = "block/slime_stalactite_down_" + thickness;
            still("block/slime_stalactite_up_" + thickness, (image, random) -> flippedVertically(image, down));
        }
        still("item/slime_stalactite", Ring2Textures::slimeStalactiteItem);
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

    // ---- Palm -----------------------------------------------------------------------------------------------

    /**
     * Palm trunk: grey-brown bark in horizontal rings (the scars of old fronds), each ring a lit upper row, a body and a
     * dark seam, with small scale notches; no vertical grain.
     */
    private static void palmLog(BufferedImage image, Random random) {
        int[] seams = {3, 7, 11, 15};
        int y0 = 0;
        for (int seam : seams) {
            double ring = 0.35 + 0.3 * random.nextDouble();
            int shift = random.nextInt(4);
            for (int y = y0; y <= seam; y++) {
                for (int x = 0; x < SIZE; x++) {
                    if (y == seam) {
                        set(image, x, y, Math.floorMod(x + shift, 4) == 0 ? PALM_BARK[1] : PALM_BARK[0]);
                    } else if (y == y0) {
                        set(image, x, y, shade(PALM_BARK, ring + 0.3 + 0.15 * random.nextDouble()));
                    } else {
                        set(image, x, y, shade(PALM_BARK, ring + 0.25 * (random.nextDouble() - 0.5)));
                    }
                }
            }
            // A scale notch: a short dark tick below the seam above, every few pixels.
            for (int x = shift; x < SIZE; x += 4 + random.nextInt(2)) {
                set(image, x, Math.min(seam - 1, y0 + 1), PALM_BARK[1]);
            }
            y0 = seam + 1;
        }
    }

    /** End grain without year rings (palm wood is fibrous): sand-yellow with darker fibre dots, a 1-pixel bark rim. */
    private static void palmLogTop(BufferedImage image, Random random) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double radius = Math.hypot(x - 7.5, y - 7.5);
                int color = shade(PALM_WOOD, 0.75 - radius / 18.0 + 0.2 * random.nextDouble());
                set(image, x, y, random.nextInt(6) == 0 ? PALM_WOOD[0] : color);
            }
        }
        rim(image, random, PALM_BARK, 0.0, 0.6);
    }

    /** Wood with fine lengthwise fibres. */
    private static void strippedLog(BufferedImage image, Random random, int[] wood) {
        double[] columns = new double[SIZE];
        for (int x = 0; x < SIZE; x++) {
            columns[x] = random.nextDouble();
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, shade(wood, 0.35 + 0.4 * columns[x] + 0.15 * random.nextDouble()));
            }
        }
        for (int i = 0; i < 4; i++) {
            int x = 1 + 4 * i + random.nextInt(2);
            int start = random.nextInt(SIZE);
            int length = 6 + random.nextInt(8);
            for (int j = 0; j < length; j++) {
                setWrapped(image, x, start + j, wood[1]);
            }
        }
    }

    /** The log's end grain {@code topName} (same pixels) with a wood rim instead of bark. */
    private static void strippedLogTop(BufferedImage image, Random random, String topName, int[] wood) {
        copy(renderUnchecked(topName), image);
        rim(image, random, wood, 0.2, 0.55);
    }

    /** Four planks of four rows: dark seam row, lit top row, staggered joints, grain streaks. */
    private static void planks(BufferedImage image, Random random, int[] wood) {
        int[] joints = {5, 12, 2, 9};
        for (int plank = 0; plank < 4; plank++) {
            double plankShade = 0.35 + random.nextDouble() * 0.3;
            for (int row = 0; row < 4; row++) {
                int y = plank * 4 + row;
                for (int x = 0; x < SIZE; x++) {
                    if (row == 3 || x == joints[plank]) {
                        set(image, x, y, wood[0]);
                    } else {
                        double lit = row == 0 ? 0.2 : 0.0;
                        set(image, x, y, shade(wood, plankShade + lit + (random.nextDouble() - 0.5) * 0.25));
                    }
                }
            }
            for (int i = 0; i < 2; i++) {
                int y = plank * 4 + 1 + random.nextInt(2);
                int x = random.nextInt(SIZE);
                int length = 3 + random.nextInt(4);
                for (int j = 0; j < length; j++) {
                    if ((x + j) % SIZE != joints[plank]) {
                        setWrapped(image, x + j, y, wood[1]);
                    }
                }
            }
        }
    }

    /** Upper door half: boards in a bark frame, two window slits, one rail. */
    private static void doorTop(BufferedImage image, Random random, int[] wood, int[] bark) {
        boards(image, random, wood);
        frame(image, random, bark, true, false);
        rail(image, random, bark, 11);
        window(image, bark, 3, 3, 3, 5);
        window(image, bark, 10, 3, 3, 5);
    }

    /** Lower door half: boards, two rails and a handle near the top edge. */
    private static void doorBottom(BufferedImage image, Random random, int[] wood, int[] bark) {
        boards(image, random, wood);
        frame(image, random, bark, false, true);
        rail(image, random, bark, 2);
        rail(image, random, bark, 12);
        set(image, 12, 0, bark[0]);
        set(image, 12, 1, bark[0]);
    }

    /** Trapdoor as a lattice: horizontal slats with see-through gaps between them, in a bark frame. */
    private static void trapdoor(BufferedImage image, Random random, int[] wood, int[] bark) {
        boards(image, random, wood);
        frame(image, random, bark, true, true);
        for (int gap : new int[] {3, 7, 11}) {
            for (int x = 2; x < SIZE - 2; x++) {
                if (x != 7 && x != 8) {
                    image.setRGB(x, gap, TRANSPARENT);
                }
            }
        }
        for (int y = 1; y < SIZE - 1; y++) {
            set(image, 7, y, bark[2]);
            set(image, 8, y, bark[1]);
        }
    }

    /** The door in the inventory (8 wide, columns 4–11): boards, rails, two window slits and the handle. */
    private static void doorItem(BufferedImage image, Random random, int[] wood, int[] bark) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 4; x < 12; x++) {
                boolean edge = x == 4 || x == 11 || y == 0 || y == SIZE - 1;
                if (edge) {
                    set(image, x, y, shade(bark, 0.2 + random.nextDouble() * 0.4));
                } else if (x == 8) {
                    set(image, x, y, wood[0]);
                } else {
                    set(image, x, y, shade(wood, 0.35 + random.nextDouble() * 0.4));
                }
            }
        }
        for (int y = 2; y <= 4; y++) {
            image.setRGB(6, y, TRANSPARENT);
            image.setRGB(9, y, TRANSPARENT);
        }
        for (int y : new int[] {6, 13}) {
            for (int x = 5; x < 11; x++) {
                set(image, x, y, bark[2]);
            }
        }
        set(image, 10, 8, bark[0]);
    }

    /** Three vertical boards (seams at x 5 and 10), each with its own tone. */
    private static void boards(BufferedImage image, Random random, int[] wood) {
        double[] boards = {0.35 + random.nextDouble() * 0.3, 0.35 + random.nextDouble() * 0.3, 0.35 + random.nextDouble() * 0.3};
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (x == 5 || x == 10) {
                    set(image, x, y, wood[0]);
                } else {
                    set(image, x, y, shade(wood, boards[x < 5 ? 0 : x < 10 ? 1 : 2] + (random.nextDouble() - 0.5) * 0.25));
                }
            }
        }
    }

    /** Bark edges left and right, optionally along the top and bottom row. */
    private static void frame(BufferedImage image, Random random, int[] bark, boolean top, boolean bottom) {
        for (int i = 0; i < SIZE; i++) {
            set(image, 0, i, shade(bark, 0.2 + random.nextDouble() * 0.5));
            set(image, SIZE - 1, i, shade(bark, 0.2 + random.nextDouble() * 0.5));
            if (top) {
                set(image, i, 0, shade(bark, 0.2 + random.nextDouble() * 0.5));
            }
            if (bottom) {
                set(image, i, SIZE - 1, shade(bark, 0.2 + random.nextDouble() * 0.5));
            }
        }
    }

    /** A two-row bark rail with a nail near each end. */
    private static void rail(BufferedImage image, Random random, int[] bark, int y) {
        for (int x = 1; x < SIZE - 1; x++) {
            set(image, x, y, shade(bark, 0.6 + random.nextDouble() * 0.4));
            set(image, x, y + 1, shade(bark, 0.3 + random.nextDouble() * 0.3));
        }
        set(image, 2, y, bark[0]);
        set(image, SIZE - 3, y, bark[0]);
    }

    /** A see-through hole with a dark rim (cutout: alpha 0 or 255). */
    private static void window(BufferedImage image, int[] bark, int x0, int y0, int width, int height) {
        for (int y = y0 - 1; y <= y0 + height; y++) {
            for (int x = x0 - 1; x <= x0 + width; x++) {
                boolean inside = x >= x0 && x < x0 + width && y >= y0 && y < y0 + height;
                if (inside) {
                    image.setRGB(x, y, TRANSPARENT);
                } else {
                    set(image, x, y, bark[1]);
                }
            }
        }
    }

    /**
     * Fixed bright green (no biome tint): long narrow leaflets running diagonally in two directions, wrapping, with
     * see-through gaps between them (cutout_mipped).
     */
    private static void palmLeaves(BufferedImage image, Random random) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                image.setRGB(x, y, TRANSPARENT);
            }
        }
        for (int i = 0; i < 11; i++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            int dx = i % 2 == 0 ? 1 : -1;
            int length = 6 + random.nextInt(5);
            for (int j = 0; j < length; j++) {
                // Lit edge on top, darker body below: a narrow leaflet two pixels thick.
                int tone = j == 0 || j == length - 1 ? 1 : 2 + random.nextInt(2);
                setWrapped(image, x + j * dx, y + j, PALM_LEAVES[tone]);
                setWrapped(image, x + j * dx, y + j + 1, PALM_LEAVES[Math.max(0, tone - 2)]);
            }
        }
    }

    /** A small palm: a thin curved trunk and four arching fronds; transparent background (cutout). */
    private static void palmSapling(BufferedImage image, Random random) {
        for (int y = 15; y >= 8; y--) {
            int x = y > 12 ? 8 : 7;
            set(image, x, y, PALM_BARK[y % 2 == 0 ? 2 : 1]);
        }
        int[][] fronds = {{-1, -1}, {1, -1}, {-1, 0}, {1, 0}};
        for (int[] frond : fronds) {
            int x = 7;
            int y = 7;
            int length = 5 + random.nextInt(2);
            for (int j = 0; j < length; j++) {
                x += frond[0];
                // Rising first, then drooping at the tip.
                y += j < 2 ? frond[1] : j >= length - 2 ? 1 : 0;
                set(image, x, y, PALM_LEAVES[j == length - 1 ? 1 : 2 + random.nextInt(2)]);
                if (j > 0 && j < length - 1) {
                    set(image, x, y + 1, PALM_LEAVES[0]);
                }
            }
        }
        set(image, 7, 7, PALM_LEAVES[3]);
        set(image, 7, 6, PALM_LEAVES[2]);
        set(image, 7, 5, PALM_LEAVES[1]);
    }

    // ---- Ores (sunstone, turquoise) -------------------------------------------------------------------------

    /**
     * Sunstone overlay, shared by all host rocks: 4–5 angular chunks with a dark 1-px outline (crystal style), lit
     * top-left, shadowed bottom-right. The outline keeps them readable on red, orange, cream and brown.
     */
    private static void sunstoneOreOverlay(BufferedImage image, Random random) {
        int target = 4 + random.nextInt(2);
        int placed = 0;
        for (int attempt = 0; attempt < 500 && placed < target; attempt++) {
            double radius = 1.4 + random.nextDouble() * 0.9;
            double cx = 1.5 + radius + random.nextDouble() * (SIZE - 3 - 2 * radius);
            double cy = 1.5 + radius + random.nextDouble() * (SIZE - 3 - 2 * radius);
            double stretch = 0.75 + random.nextDouble() * 0.5;
            BufferedImage chunk = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            int highlightX = -1;
            int highlightY = -1;
            for (int y = 1; y < SIZE - 1; y++) {
                for (int x = 1; x < SIZE - 1; x++) {
                    double dx = (x + 0.5 - cx) * stretch;
                    double dy = (y + 0.5 - cy) / stretch;
                    // Octagon: a square with its corners cut off, so the chunk looks faceted, not round.
                    if (Math.abs(dx) + Math.abs(dy) > radius * 1.25 || Math.max(Math.abs(dx), Math.abs(dy)) > radius) {
                        continue;
                    }
                    double facet = (dx + dy) / radius;
                    int color = facet < -0.45 ? SUNSTONE[4] : facet < 0.2 ? SUNSTONE[3] : facet < 0.8 ? SUNSTONE[2] : SUNSTONE[1];
                    set(chunk, x, y, color);
                    if (highlightX < 0 || x + y < highlightX + highlightY) {
                        highlightX = x;
                        highlightY = y;
                    }
                }
            }
            if (highlightX < 0) {
                continue;
            }
            set(chunk, highlightX, highlightY, SUNSTONE[5]);
            outline(chunk, SUNSTONE[0]);
            if (merge(image, chunk)) {
                placed++;
            }
        }
    }

    /**
     * Turquoise overlay, shared by all host rocks: 2–3 round, matte nodules (a gem in the rock, no facets) with a dark
     * teal outline and thin brown matrix veins.
     */
    private static void turquoiseOreOverlay(BufferedImage image, Random random) {
        int target = 2 + random.nextInt(2);
        int placed = 0;
        for (int attempt = 0; attempt < 500 && placed < target; attempt++) {
            double radius = 2.3 + random.nextDouble() * 0.9;
            double cx = 1.5 + radius + random.nextDouble() * (SIZE - 3 - 2 * radius);
            double cy = 1.5 + radius + random.nextDouble() * (SIZE - 3 - 2 * radius);
            BufferedImage nodule = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            for (int y = 1; y < SIZE - 1; y++) {
                for (int x = 1; x < SIZE - 1; x++) {
                    double dx = x + 0.5 - cx;
                    double dy = y + 0.5 - cy;
                    if (Math.hypot(dx, dy) > radius) {
                        continue;
                    }
                    double light = -(dx + dy) / (2 * radius);
                    set(nodule, x, y, shade(TURQUOISE, 0.45 + 0.45 * light + 0.25 * random.nextDouble()));
                }
            }
            veins(nodule, random, 1 + random.nextInt(2));
            outline(nodule, TURQUOISE[0]);
            if (merge(image, nodule)) {
                placed++;
            }
        }
    }

    /** Raw sunstone, like vanilla raw iron: an irregular lump with a golden core and bits of red canyon stone. */
    private static void rawSunstone(BufferedImage image, Random random) {
        rawLump(image, random, SUNSTONE, RED_CANYON_STONE, SUNSTONE[0]);
    }

    /** Raw turquoise: an unpolished nodule with matrix veins and bits of cream canyon stone (no cut gem). */
    private static void rawTurquoise(BufferedImage image, Random random) {
        rawLump(image, random, TURQUOISE, CREAM_CANYON_STONE, TURQUOISE[0]);
        veins(image, random, 3);
    }

    /**
     * An irregular lump (radius 4.5–6 around the middle, varying with the angle) lit from the top-left, with patches of
     * host rock ({@code rock}) and a 1-px {@code outline}. The core skips its darkest colour (index 0, the outline).
     */
    private static void rawLump(BufferedImage image, Random random, int[] core, int[] rock, int outline) {
        int corners = 7;
        double[] radii = new double[corners];
        for (int i = 0; i < corners; i++) {
            radii[i] = 4.5 + random.nextDouble() * 1.5;
        }
        double[][] patches = valueNoise(random, 4, 4);
        double cx = 8.0;
        double cy = 8.3;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double dx = x + 0.5 - cx;
                double dy = y + 0.5 - cy;
                double angle = (Math.atan2(dy, dx) / (2 * Math.PI) + 1) % 1 * corners;
                int i = (int) angle;
                double radius = lerp(radii[i], radii[(i + 1) % corners], smoothstep(angle - i));
                double distance = Math.hypot(dx, dy);
                if (distance > radius) {
                    continue;
                }
                double light = 0.5 - 0.5 * (dx + dy) / (radius * 1.4) + 0.15 * random.nextDouble();
                if (patches[x][y] > 0.62 && distance > radius * 0.35) {
                    set(image, x, y, shade(rock, light));
                } else {
                    set(image, x, y, core[1 + (int) (Math.max(0, Math.min(0.999, light)) * (core.length - 1))]);
                }
            }
        }
        outline(image, outline);
    }

    /**
     * {@code count} thin veins wandering across the tile, drawn only on opaque pixels that are not outline (so the
     * shape and its outline stay intact).
     */
    private static void veins(BufferedImage image, Random random, int count) {
        boolean[][] inner = new boolean[SIZE][SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                inner[x][y] = isOpaque(image, x, y) && isOpaque(image, x - 1, y) && isOpaque(image, x + 1, y)
                        && isOpaque(image, x, y - 1) && isOpaque(image, x, y + 1);
            }
        }
        for (int v = 0; v < count; v++) {
            boolean horizontal = random.nextBoolean();
            int across = 3 + random.nextInt(SIZE - 6);
            for (int step = 0; step < SIZE; step++) {
                int x = horizontal ? step : across;
                int y = horizontal ? across : step;
                if (inner[x][y] && random.nextInt(5) != 0) {
                    set(image, x, y, TURQUOISE_VEIN);
                }
                if (random.nextInt(3) == 0) {
                    across = Math.max(1, Math.min(SIZE - 2, across + (random.nextBoolean() ? 1 : -1)));
                }
            }
        }
    }

    // ---- Salt -----------------------------------------------------------------------------------------------

    /**
     * Salt flat crust from above: coarse polygons (wrapping cells, like the cracked clay plates) parted by raised white
     * ridges, each ridge casting a thin shadow on the plate below it.
     */
    private static void saltCrustTop(BufferedImage image, Random random) {
        Cells cells = Cells.random(random, 5);
        double[] plate = new double[cells.count()];
        for (int i = 0; i < plate.length; i++) {
            plate[i] = 0.3 + 0.3 * random.nextDouble();
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                Cells.Nearest nearest = cells.nearest(x, y);
                double gap = nearest.secondDistance() - nearest.distance();
                if (gap < 0.7) {
                    set(image, x, y, SALT_RIDGE);
                } else if (gap < 1.6 && nearest.dy() < 0) {
                    set(image, x, y, SALT_RIDGE_SHADOW);
                } else {
                    set(image, x, y, shade(SALT, plate[nearest.index()] + 0.2 * random.nextDouble()));
                }
            }
        }
    }

    /**
     * Crust side: the salt block's own pixels (taken from that texture, so the two always match) under a crust band of
     * 2–3 px with a shadowed lower edge.
     */
    private static void saltCrustSide(BufferedImage image, Random random) {
        copy(renderUnchecked("block/salt_block"), image);
        for (int x = 0; x < SIZE; x++) {
            int depth = 2 + random.nextInt(2);
            for (int y = 0; y < depth - 1; y++) {
                set(image, x, y, random.nextInt(3) == 0 ? SALT[4] : SALT_RIDGE);
            }
            set(image, x, depth - 1, SALT_RIDGE_SHADOW);
        }
    }

    /** Fine-grained salt with a few small crystal faces (lit top-left, shadowed bottom-right). */
    private static void saltBlock(BufferedImage image, Random random) {
        double[][] noise = valueNoise(random, 4, 4);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, shade(SALT, 0.15 + 0.4 * noise[x][y] + 0.3 * random.nextDouble()));
            }
        }
        for (int i = 0; i < 4; i++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            setWrapped(image, x, y, SALT_RIDGE);
            setWrapped(image, x + 1, y, SALT[3]);
            setWrapped(image, x, y + 1, SALT[3]);
            setWrapped(image, x + 1, y + 1, SALT[1]);
        }
    }

    /** Polished, as vanilla lays it out: a smooth face with a light top-left and a dark bottom-right rim. */
    private static void polishedSalt(BufferedImage image, Random random) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, shade(SALT, 0.6 + 0.2 * random.nextDouble()));
            }
        }
        for (int i = 0; i < SIZE; i++) {
            set(image, i, 0, SALT[4]);
            set(image, 0, i, SALT[4]);
        }
        for (int i = 0; i < SIZE; i++) {
            set(image, i, SIZE - 1, SALT[0]);
            set(image, SIZE - 1, i, SALT[0]);
        }
        for (int i = 1; i < SIZE - 1; i++) {
            set(image, i, SIZE - 2, SALT[1]);
            set(image, SIZE - 2, i, SALT[1]);
        }
    }

    /**
     * Three pointed crystals growing from the bottom edge (cross model, cutout): a tall one in the middle and two
     * shorter ones leaning outwards; light left facet, bright ridge, shaded right facet, a faint pink blush here and
     * there. Transparent background.
     */
    private static void saltCrystal(BufferedImage image, Random random) {
        crystalSpike(image, random, 7.5, 3, 13, 0);
        crystalSpike(image, random, 3.5, 2, 8 + random.nextInt(2), -2);
        crystalSpike(image, random, 11.5, 2, 7 + random.nextInt(2), 2);
    }

    /**
     * One crystal from the bottom row up: {@code halfWidth} px either side of the ridge, tapering to a point over the top
     * rows, its ridge shifting {@code lean} px over the full {@code height}.
     */
    private static void crystalSpike(BufferedImage image, Random random, double centre, int halfWidth, int height, int lean) {
        for (int row = 0; row < height; row++) {
            int y = SIZE - 1 - row;
            int ridge = (int) Math.round(centre + lean * row / (double) height);
            int fromTop = height - 1 - row;
            int half = Math.min(halfWidth, fromTop * halfWidth / (halfWidth + 1));
            for (int x = ridge - half; x <= ridge + half; x++) {
                if (x < 0 || x >= SIZE) {
                    continue;
                }
                int color = x < ridge ? SALT_CRYSTAL[2] : x == ridge ? SALT_CRYSTAL[3] : SALT_CRYSTAL[x == ridge + half ? 0 : 1];
                if (x != ridge && random.nextInt(9) == 0) {
                    color = SALT_CRYSTAL_BLUSH;
                }
                set(image, x, y, color);
            }
        }
    }

    /** A small heap of salt grains with a few sparkling crystals and a dark outline (like vanilla's dusts). */
    private static void saltItem(BufferedImage image, Random random) {
        for (int y = 6; y < SIZE - 1; y++) {
            for (int x = 1; x < SIZE - 1; x++) {
                double dx = (x + 0.5 - 8.0) / 6.5;
                double dy = (y + 0.5 - 15.0) / 8.0;
                if (dx * dx + dy * dy > 1) {
                    continue;
                }
                double light = 0.75 - 0.35 * (dx - dy) + 0.3 * (random.nextDouble() - 0.5);
                set(image, x, y, shade(SALT, light));
            }
        }
        for (int i = 0; i < 4; i++) {
            int x = 4 + random.nextInt(8);
            int y = 10 + random.nextInt(4);
            if (isOpaque(image, x, y)) {
                set(image, x, y, SALT_RIDGE);
            }
        }
        outline(image, SALT_OUTLINE);
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

    /** Every transparent pixel next to (4-neighbourhood) an opaque one becomes {@code color}. */
    private static void outline(BufferedImage image, int color) {
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
                    set(image, x, y, color);
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

    /** Opaque pixel inside the tile; outside counts as transparent. */
    private static boolean isOpaque(BufferedImage image, int x, int y) {
        return x >= 0 && y >= 0 && x < SIZE && y < SIZE && (image.getRGB(x, y) >>> 24) != 0;
    }

    // ---- Nether ring 2 --------------------------------------------------------------------------------------

    /** Dark red-brown, wet: two noise scales, a few grey-green damp spots and short horizontal gloss highlights. */
    private static void soakedNetherrack(BufferedImage image, Random random) {
        double[][] broad = valueNoise(random, 4, 4);
        double[][] fine = valueNoise(random, 2, 2);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, shade(SOAKED_NETHERRACK, 0.7 * broad[x][y] + 0.3 * fine[x][y]));
            }
        }
        for (int i = 0; i < 14; i++) {
            set(image, random.nextInt(SIZE), random.nextInt(SIZE), SOAKED_DAMP);
        }
        for (int i = 0; i < 5; i++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            setWrapped(image, x, y, SOAKED_GLOSS);
            setWrapped(image, x + 1, y, SOAKED_GLOSS);
        }
    }

    /** Grey-green moss with pale spore dots. */
    private static void sporeMoss(BufferedImage image, Random random) {
        double[][] broad = valueNoise(random, 4, 4);
        double[][] fine = valueNoise(random, 2, 2);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, shade(SPORE_MOSS, 0.65 * broad[x][y] + 0.35 * fine[x][y]));
            }
        }
        for (int i = 0; i < 9; i++) {
            set(image, random.nextInt(SIZE), random.nextInt(SIZE), SPORE_DOT);
        }
    }

    /** Soaked netherrack (exactly its pixels) under a ragged moss rim, 3–5 pixels deep. */
    private static void sporeMossSide(BufferedImage image, Random random) {
        copy(renderUnchecked("block/soaked_netherrack"), image);
        double[][] fine = valueNoise(random, 2, 2);
        for (int x = 0; x < SIZE; x++) {
            int depth = 3 + random.nextInt(3);
            for (int y = 0; y < depth; y++) {
                set(image, x, y, shade(SPORE_MOSS, 0.2 + 0.6 * fine[x][y] + (y == 0 ? 0.2 : 0.0)));
            }
            if (random.nextInt(6) == 0) {
                set(image, x, 1, SPORE_DOT);
            }
        }
    }

    /** Grey-purple bark in vertical furrows with slime running down it. */
    private static void dripshroomStem(BufferedImage image, Random random) {
        double[] columns = new double[SIZE];
        for (int x = 0; x < SIZE; x++) {
            columns[x] = random.nextDouble();
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, shade(DRIPSHROOM_BARK, 0.25 + 0.5 * columns[x] + 0.2 * random.nextDouble()));
            }
        }
        for (int x = 1 + random.nextInt(3); x < SIZE; x += 4 + random.nextInt(2)) {
            for (int y = 0; y < SIZE; y++) {
                if (random.nextInt(4) != 0) {
                    set(image, x, y, DRIPSHROOM_BARK[0]);
                }
            }
        }
        for (int i = 0; i < 4; i++) {
            int x = random.nextInt(SIZE);
            int start = random.nextInt(SIZE);
            int length = 3 + random.nextInt(5);
            for (int j = 0; j < length; j++) {
                setWrapped(image, x, start + j, DRIPSHROOM_SLIME[j == length - 1 ? 1 : 0]);
            }
        }
    }

    /** End grain: pale inner flesh in faint rings, a bark rim. */
    private static void dripshroomStemTop(BufferedImage image, Random random) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double radius = Math.hypot(x - 7.5, y - 7.5);
                set(image, x, y, shade(DRIPSHROOM_WOOD, 0.5 + 0.2 * Math.cos(radius * 1.6) + 0.2 * (random.nextDouble() - 0.5)));
            }
        }
        rim(image, random, DRIPSHROOM_BARK, 0.0, 0.7);
    }

    /** Four slimy lumps with seams, like a cap's underside: wart-block mottling in the cap palette, dripping spots. */
    private static void dripshroomCap(BufferedImage image, Random random) {
        double[][] broad = valueNoise(random, 4, 4);
        double[][] fine = valueNoise(random, 2, 2);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, shade(DRIPSHROOM_CAP, 0.6 * broad[x][y] + 0.4 * fine[x][y]));
            }
        }
        for (int i = 0; i < 4; i++) {
            int x = random.nextInt(SIZE);
            int start = random.nextInt(SIZE);
            int length = 2 + random.nextInt(3);
            for (int j = 0; j < length; j++) {
                setWrapped(image, x, start + j, DRIPSHROOM_CAP_DROP);
            }
        }
    }

    /** Glowing cells: random seed points, bright in the middle of each cell and dark along the borders between cells. */
    private static void dripshroomLamp(BufferedImage image, Random random) {
        int count = 6;
        double[] px = new double[count];
        double[] py = new double[count];
        for (int i = 0; i < count; i++) {
            px[i] = random.nextDouble() * SIZE;
            py[i] = random.nextDouble() * SIZE;
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double nearest = Double.MAX_VALUE;
                double second = Double.MAX_VALUE;
                for (int i = 0; i < count; i++) {
                    double dx = Math.abs(x - px[i]);
                    double dy = Math.abs(y - py[i]);
                    double distance = Math.hypot(Math.min(dx, SIZE - dx), Math.min(dy, SIZE - dy));
                    if (distance < nearest) {
                        second = nearest;
                        nearest = distance;
                    } else if (distance < second) {
                        second = distance;
                    }
                }
                double border = second - nearest;
                set(image, x, y, border < 1.4 ? DRIPSHROOM_LAMP[0] : shade(DRIPSHROOM_LAMP, 0.9 - nearest / 9.0 + 0.1 * random.nextDouble()));
            }
        }
    }

    /** Loose, tiny glowing patches on a transparent background (cutout): a bright core with a dimmer fringe. */
    private static void glowMold(BufferedImage image, Random random) {
        for (int i = 0; i < 9; i++) {
            int x = 1 + random.nextInt(SIZE - 2);
            int y = 1 + random.nextInt(SIZE - 2);
            set(image, x, y, GLOW_MOLD[2]);
            for (int[] step : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                if (random.nextInt(3) != 0) {
                    set(image, x + step[0], y + step[1], GLOW_MOLD[random.nextInt(2)]);
                }
            }
        }
    }

    /** Small tuft: a few thin stalks of different heights, each with a pale spore cap; transparent background (cutout). */
    private static void sporeSprouts(BufferedImage image, Random random) {
        int[] columns = {3, 5, 7, 9, 11, 12};
        for (int column : columns) {
            int x = column + random.nextInt(2) - (column == 12 ? 1 : 0);
            int top = 5 + random.nextInt(7);
            for (int y = 15; y > top; y--) {
                set(image, x, y, SPORE_MOSS[y > 12 ? 0 : 1 + random.nextInt(2)]);
            }
            set(image, x, top, SPORE_DOT);
            set(image, x - 1, top + 1, SPORE_MOSS[3]);
            set(image, x + 1, top + 1, SPORE_MOSS[3]);
        }
    }

    /**
     * A slime stalactite hanging from the top of the tile; {@code up} variants are these flipped. Widths follow the
     * block shapes (base 12, middle 10, frustum 8, merged tip 6 px); the lone tip tapers to a point after 11 rows.
     * Lit on the left, dark on the right, a darker outline and a few glints.
     */
    private static void slimeStalactite(BufferedImage image, Random random, int width, boolean taper) {
        for (int y = 0; y < SIZE; y++) {
            double rowWidth = width;
            if (taper) {
                if (y > 10) {
                    continue;
                }
                rowWidth = width - (width - 1) * y / 10.0;
            }
            int left = (int) Math.round(7.5 - rowWidth / 2.0) + (taper ? 0 : random.nextInt(2));
            int right = (int) Math.round(7.5 + rowWidth / 2.0) - 1 - (taper ? 0 : random.nextInt(2));
            for (int x = left; x <= right; x++) {
                double t = right == left ? 0.5 : (x - left) / (double) (right - left);
                int color = x == left || x == right ? SLIME_STALACTITE[0]
                        : random.nextInt(14) == 0 ? SLIME_STALACTITE[3] : shade(SLIME_STALACTITE, 0.9 - 0.6 * t + 0.2 * random.nextDouble());
                set(image, x, y, color);
            }
        }
    }

    /** The hanging stalactite {@code name} mirrored top to bottom: a stalagmite. */
    private static void flippedVertically(BufferedImage image, String name) {
        BufferedImage source = renderUnchecked(name);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                image.setRGB(x, y, source.getRGB(x, SIZE - 1 - y));
            }
        }
    }

    /** The whole stalactite in the inventory: a diagonal cone from the top-left, tapering to a point. */
    private static void slimeStalactiteItem(BufferedImage image, Random random) {
        for (int y = 0; y < SIZE; y++) {
            double rowWidth = 12.0 - 11.0 * y / 15.0;
            int left = (int) Math.round(7.5 - rowWidth / 2.0);
            int right = (int) Math.round(7.5 + rowWidth / 2.0) - 1;
            for (int x = left; x <= right; x++) {
                double t = right == left ? 0.5 : (x - left) / (double) (right - left);
                int color = x == left || x == right ? SLIME_STALACTITE[0]
                        : random.nextInt(12) == 0 ? SLIME_STALACTITE[3] : shade(SLIME_STALACTITE, 0.9 - 0.6 * t + 0.2 * random.nextDouble());
                set(image, x, y, color);
            }
        }
    }

    // ---- Preview --------------------------------------------------------------------------------------------

    /**
     * One row per texture: name, the rest frame 8×, a 3×3 tiling at 8× (seams and repetition) and, if animated, every
     * frame at 4×. Transparency over a dusty brown backdrop, roughly the tone of the canyon floor. Then one row per ore
     * overlay ({@link #ORE_PREVIEWS}): the overlay over each of its host rocks, tiled 3×3 at 4×.
     */
    private static void writePreview() throws IOException {
        int scale = 8;
        int tile = SIZE * scale;
        int label = 220;
        int frameScale = 4;
        int maxFrames = TEXTURES.values().stream().mapToInt(texture -> texture.animation().frames()).max().orElse(1);
        int width = label + tile + 16 + 3 * tile + 16 + maxFrames * (SIZE * frameScale + 4);
        int oreTile = 3 * SIZE * 4;
        int oreRows = 0;
        int oreColumns = 0;
        int columnsInRow = 0;
        for (int i = 0; i < ORE_PREVIEWS.length; i++) {
            boolean newRow = i == 0 || !ORE_PREVIEWS[i][0].equals(ORE_PREVIEWS[i - 1][0]);
            oreRows += newRow ? 1 : 0;
            columnsInRow = newRow ? 1 : columnsInRow + 1;
            oreColumns = Math.max(oreColumns, columnsInRow);
        }
        width = Math.max(width, label + oreColumns * (oreTile + 8));
        int height = TEXTURES.size() * (3 * tile + 16) + oreRows * (oreTile + 16);
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
        // Ores: every overlay over each of its host rocks, tiled 3×3 at 4×; one row per overlay (preview only).
        int oreTop = row * (3 * tile + 16) + 8 - (oreTile + 16);
        String overlay = null;
        int column = 0;
        for (String[] pair : ORE_PREVIEWS) {
            if (!pair[0].equals(overlay)) {
                overlay = pair[0];
                oreTop += oreTile + 16;
                column = 0;
                graphics.setColor(Color.WHITE);
                graphics.drawString("ores: " + overlay, 8, oreTop + 16);
            }
            BufferedImage composite = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            copy(render(pair[1]), composite);
            BufferedImage ore = render(pair[0]);
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    if (isOpaque(ore, x, y)) {
                        composite.setRGB(x, y, ore.getRGB(x, y));
                    }
                }
            }
            for (int ty = 0; ty < 3; ty++) {
                for (int tx = 0; tx < 3; tx++) {
                    draw(graphics, composite, label + column * (oreTile + 8) + tx * SIZE * 4, oreTop + ty * SIZE * 4, 4);
                }
            }
            column++;
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

    private static void copy(BufferedImage from, BufferedImage to) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                to.setRGB(x, y, from.getRGB(x, y));
            }
        }
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
