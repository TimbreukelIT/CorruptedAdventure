import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Generates the ring 5 (corrupted ring) and Nether ring 5 (white, silent Nether) block, item, sign and particle textures
 * from our own pixels: no vanilla pixels are read into a texture or copied. Run from the repository root:
 * {@code java tools/textures/Ring5Textures.java [--vanilla <minecraft resources jar>]}.
 *
 * <p>Good first versions in the 37b style (fase-1-wereld §1.4, package 99a): light from the top left, 5–7 tones per
 * material, selective outlines for items, crystals and ore overlays, a flatness lint, and a contact sheet
 * ({@code build/texture-preview/ring5.png}) with 6×6 walls (the corrupted blocks beside their vanilla counterpart), scenes
 * by day and night, the wood set, items in a slot and the particles enlarged. Vanilla is only read for that sheet, from
 * the resources jar ModDevGradle unpacks into {@code build/moddev/artifacts/} (or the jar given with {@code --vanilla}),
 * as in {@code VanillaCompare.java}; without a jar the sheet shows a dark cross instead.
 *
 * <p>Corrupted (contract §7): the vanilla layout of the healthy counterpart, leached to a cool grey, with thin violet-black
 * veins (1 px, broken, 1–3 per tile, never on a grid). Glitch: missing-texture magenta and black, broken. Void: near-black
 * with violet depth and emissive star pixels ({@code voidstone_glow}, twinkling). Nullite (§11): dark violet-black
 * crystals with a violet rim, weakly emissive. Nether ring 5: white and cool, almost without texture.
 *
 * <p>Deterministic: every texture has a fixed seed (hash of its file name), so running twice gives identical PNGs and
 * {@code .mcmeta}s. Every texture is checked before it is written. The shared helpers are copied from
 * {@code Ring4Textures.java}: a single-file script cannot load a second source file (deduplication: backlog, an
 * {@code infra} choice).
 */
public final class Ring5Textures {
    private static final int SIZE = 16;
    private static final int PARTICLE_SIZE = 8;
    private static final int TRANSPARENT = 0x00000000;
    private static final Path OUTPUT = Path.of("src/main/resources/assets/corruptedadventure/textures");
    private static final Path PREVIEW = Path.of("build/texture-preview/ring5.png");
    private static final Path ARTIFACTS = Path.of("build/moddev/artifacts");

    /** Retouched by hand after generation; the script skips these and the preview reads them from disk. */
    private static final Set<String> HAND_EDITED = Set.of();

    /** The sign entity textures (vanilla's sign and hanging sign model UV layout): 64×32. */
    private static final String SIGN_ENTITY = "entity/signs/corrupted";
    private static final String HANGING_SIGN_ENTITY = "entity/signs/hanging/corrupted";
    private static final int SIGN_WIDTH = 64;
    private static final int SIGN_HEIGHT = 32;

    // ---- Palettes, dark to light (RGB) -------------------------------------------------------------------------

    // Corrupted variants (88): the vanilla counterpart's brightness, leached to ~10–15 % of its saturation and a little
    // cool; each keeps a faint hint of its old hue so grass, dirt, stone and sand still read apart.
    private static final int[] C_GRASS = {0x4E534C, 0x5A5F57, 0x666B62, 0x72786D, 0x7E8478, 0x8B9184};
    private static final int[] C_DIRT = {0x3B3836, 0x464240, 0x524D4A, 0x5E5854, 0x6A645F, 0x77706A};
    private static final int[] C_STONE = {0x52515A, 0x5D5C65, 0x68676F, 0x737279, 0x7E7D84, 0x8A8990, 0x97969C};
    private static final int C_MORTAR = 0x2E2C34;
    // Sand a few steps darker and warmer (walk 1, question 3 = A, 2026-10-24): the old light grey (up to #DAD8D1) read as
    // snow next to the grey-green grass; now grey-beige.
    private static final int[] C_SAND = {0x8C8476, 0x978F80, 0xA19989, 0xAAA292, 0xB3AB9A, 0xBBB3A2};
    private static final int[] C_BARK = {0x2F2B2D, 0x3A3638, 0x464143, 0x524C4E, 0x5E5859, 0x6B6465};
    private static final int[] C_WOOD = {0x56524F, 0x65605D, 0x736E6A, 0x817C77, 0x8F8984, 0x9D9791};
    // Leaves a few steps lighter than 99a with a lighter leaflet tone (99b): seen from below (vanilla shades a down face to
    // half) the first palette went black, against oak's tinted green that still reads.
    private static final int[] C_LEAF = {0x353933, 0x434840, 0x51574E, 0x60665C, 0x6F756A, 0x7E8479, 0x8E9488};
    /**
     * The sign boards' faces (99b): the grey wood a few steps lighter and calmer, so black and dark red text (dye
     * colour × 0.4) reads as well as on vanilla's oak sign.
     */
    private static final int[] SIGN_WOOD = {0x7E7A76, 0x8C8884, 0x9A9692, 0xA7A39E, 0xB3AFAA, 0xBFBBB6};
    /** The veins: violet-black, 1 px; [0] the darkest heart of a vein, [2] its fading ends. */
    private static final int[] VEIN = {0x1A0E24, 0x2A1838, 0x3A1F52};
    /** The violet pith of the corrupted log's end grain. */
    private static final int[] VIOLET_CORE = {0x2A1838, 0x3A1F52, 0x4E2C6E};
    /** Iron of the hanging sign's chains. */
    private static final int[] CHAIN = {0x1C1C22, 0x2E2E36, 0x45454F, 0x5E5E68};

    // Glitch (89): vanilla's missing-texture magenta and black, with a few off colours where the image breaks.
    private static final int MAGENTA = 0xF800F8;
    private static final int MAGENTA_DIM = 0xB000B8;
    private static final int MAGENTA_LIT = 0xFF70FF;
    private static final int BLACK = 0x000000;
    private static final int BLACK_LIT = 0x2A0A30;
    private static final int GLITCH_CYAN = 0x00E8F0;
    private static final int GLITCH_WHITE = 0xF4F4F4;
    // Void (89): near-black with a violet depth; the stars white to light violet ([0] the dim tone a twinkle leans to, [1]
    // the cross star's faint arms, 99b: bright arms made one "+" per block that tiled into a lattice).
    private static final int[] VOID = {0x07050A, 0x0C0911, 0x110D18, 0x16111F, 0x1C1527, 0x231A31};
    private static final int[] VOID_DEPTH = {0x24173A, 0x30204E};
    private static final int[] STAR = {0x6A5498, 0x8E7AC2, 0xC8B4F0, 0xE8DCFF, 0xFFFFFF};
    /** Dim specks between the stars: part of the stone, not of the glow layer. */
    private static final int STAR_DUST = 0x3E3058;
    // Nullite (§11): crystal body violet-black, its rim violet (8A4FD8-like), a pale violet glint; weakly emissive.
    private static final int[] NULLITE_BODY = {0x120A1E, 0x1C1030, 0x281644};
    private static final int[] NULLITE_RIM = {0x40246A, 0x7444BC, 0x8A4FD8};
    private static final int NULLITE_GLINT = 0xC6A6FF;
    private static final int NULLITE_OUTLINE_LIGHT = 0x1E1428;
    private static final int NULLITE_OUTLINE = 0x06030A;
    /** The pulse leans towards this tone and only tones lighter than it move: the rim and the glint, not the body. */
    private static final int NULLITE_DIM = 0x3A2260;
    private static final int NULLITE_GLOW = 0xD2B8FF;

    // Nether ring 5 (90): white and cool, grey-blue shadows; calm faces with few tones (orde, stilte).
    private static final int[] CHALK = {0x9EA3AD, 0xB0B4BE, 0xC1C5CD, 0xD0D3DA, 0xDDE0E5, 0xE9EBEE};
    private static final int CHALK_CREVICE = 0x878C97;
    private static final int[] WHITE_STONE = {0xB8BCC4, 0xCDD0D7, 0xDBDDE2, 0xE5E6EA, 0xEDEEF1, 0xF5F6F8, 0xFFFFFF};
    private static final int WHITE_MORTAR = 0xA9AEB8;
    private static final int[] MIRROR = {0xAAB3C1, 0xBEC6D2, 0xCED5DF, 0xDCE2EA, 0xE7ECF2, 0xF1F5F9, 0xFFFFFF};
    private static final int[] STILL_LIGHT = {0xC8CDD7, 0xD8DCE5, 0xE6E9EF, 0xF0F2F6, 0xF8F9FB, 0xFFFFFF};
    /** White core: pearl tones [2..6] with a faint pink and blue sheen; [0]/[1] its outline. */
    private static final int[] PEARL = {0x80859A, 0x9CA0B4, 0xC4C2D8, 0xD8D5E8, 0xE9E6F4, 0xF6F3FF, 0xFFFFFF};
    private static final int PEARL_PINK = 0xFFEEF4;
    private static final int PEARL_BLUE = 0xEAF4FF;
    /** The soft white halo around a white core (emissive with the overlay): lit side, shadow side. */
    private static final int[] HALO = {0xF9FBFF, 0xEEF2FA};

    /**
     * How a texture moves: {@code frames} frames of {@code frametime} ticks, optionally interpolated; {@code glow} is the
     * colour bright frames mix towards and {@code dim} the tone dim frames lean towards. {@code cycling}: painted per
     * phase; {@code opaque}: a cycling texture without transparency.
     */
    private record Animation(int frames, int frametime, boolean interpolate, int glow, int dim, boolean cycling, boolean opaque) {
        static final Animation STILL = new Animation(1, 1, false, 0, 0, false, false);

        boolean animated() {
            return frames > 1;
        }
    }

    private record Texture(Painter painter, CyclingPainter cyclingPainter, Animation animation) {
    }

    @FunctionalInterface
    private interface Painter {
        void paint(BufferedImage image, Random random);
    }

    /** Paints one frame of a moving texture; {@code phase} runs from 0 (inclusive) to 1 (exclusive) over a cycle. */
    @FunctionalInterface
    private interface CyclingPainter {
        void paint(BufferedImage image, Random random, double phase);
    }

    /** Pulse used by glowing crystals, as in ring 1–4: 8 frames, frame time 4, interpolated. */
    private static final int PULSE_FRAMES = 8;
    private static final int PULSE_FRAMETIME = 4;

    private static final Map<String, Texture> TEXTURES = new LinkedHashMap<>();
    private static final Map<String, String> GLOW_LAYERS = new LinkedHashMap<>();
    private static final Map<String, Set<Integer>> GLOW_TONES = new LinkedHashMap<>();
    private static final Map<BufferedImage, boolean[][]> GLOW_MASKS = new IdentityHashMap<>();

    /** First texture of the Nether ring 5 and the particle sections of the contact sheet. */
    private static final String NETHER_FIRST = "block/chalkrack";
    private static final String PARTICLES_FIRST = "particle/void_mote_0";

    // ---- Particle sprites (contract §6, for A in 98): own colours, '#' light, 'o' mid, 'x' dark ---------------------

    /** Void motes: black-violet dust with a lighter heart, growing. */
    private static final int[] MOTE = {0x8A6CC0, 0x3A2458, 0x120A1C};
    private static final String[][] VOID_MOTES = {
            {"........", "........", "........", "...ox...", "...xx...", "........", "........", "........"},
            {"........", "........", "...xx...", "..x#ox..", "..xoxx..", "...x....", "........", "........"},
            {"........", "...xx...", "..xoox..", ".xo#ox..", ".xooxx..", "..xxx...", "........", "........"},
            {"...xx...", "..xoox..", ".xo##ox.", ".xo#oox.", "xooooxx.", ".xxoxx..", "..xx....", "........"}};
    /** Glitch pixels: 1–3 px blocks in magenta, cyan and black, different every frame. */
    private static final int[] GLITCH_PIXEL = {MAGENTA, GLITCH_CYAN, 0x0C0010};
    private static final String[][] GLITCH_PIXELS = {
            {"........", "........", "........", "...##...", "...##...", "........", "........", "........"},
            {"........", "........", "..xx....", "..xx##..", "....##..", "........", "........", "........"},
            {"........", ".###....", ".###....", ".###....", "....o...", "......xx", "......xx", "........"},
            {"........", "..oo.##.", "..oo.##.", ".....xx.", "..##.xx.", "..##....", "........", "........"}};
    /** Reverse raindrops: a black drop with a violet sheen, its point upwards (it falls up). */
    private static final int[] RAINDROP = {0x9A6ADA, 0x2A1A3C, 0x08040C};
    private static final String[][] REVERSE_RAINDROPS = {
            {"........", "...x....", "...x....", "..xox...", "..x#x...", "..xxx...", "...x....", "........"},
            {"...x....", "...x....", "..xox...", "..x#x...", ".xx#ox..", ".xxoxx..", "..xxx...", "........"}};

    static {
        // ---- 88: corrupted variants and the wood set ----
        still("block/corrupted_grass_block_top", Ring5Textures::corruptedGrassTop);
        still("block/corrupted_grass_block_side", Ring5Textures::corruptedGrassSide);
        still("block/corrupted_dirt", Ring5Textures::corruptedDirt);
        still("block/corrupted_stone", Ring5Textures::corruptedStone);
        still("block/corrupted_cobblestone", Ring5Textures::corruptedCobblestone);
        still("block/corrupted_sand", Ring5Textures::corruptedSand);
        still("block/corrupted_log", Ring5Textures::corruptedLog);
        still("block/corrupted_log_top", Ring5Textures::corruptedLogTop);
        still("block/stripped_corrupted_log", Ring5Textures::strippedCorruptedLog);
        still("block/stripped_corrupted_log_top", (image, random) -> strippedLogTop(image, random, "block/corrupted_log_top", C_WOOD, C_BARK));
        still("block/corrupted_planks", Ring5Textures::corruptedPlanks);
        still("block/corrupted_leaves", Ring5Textures::corruptedLeaves);
        still("block/corrupted_door_top", (image, random) -> {
            doorTop(image, random, C_WOOD, C_BARK);
            woodVein(image, random);
        });
        still("block/corrupted_door_bottom", (image, random) -> {
            doorBottom(image, random, C_WOOD, C_BARK);
            woodVein(image, random);
        });
        still("block/corrupted_trapdoor", (image, random) -> {
            trapdoor(image, random, C_WOOD, C_BARK);
            woodVein(image, random);
        });
        still("item/corrupted_door", (image, random) -> doorItem(image, random, C_WOOD, C_BARK));
        still("item/corrupted_sign", Ring5Textures::signItem);
        still("item/corrupted_hanging_sign", Ring5Textures::hangingSignItem);
        still(SIGN_ENTITY, Ring5Textures::signEntity);
        still(HANGING_SIGN_ENTITY, Ring5Textures::hangingSignEntity);
        still("gui/hanging_signs/corrupted", Ring5Textures::hangingSignGui);

        // ---- 89: glitch, void, nullite ----
        cycling("block/glitch_block", 8, 3, Ring5Textures::glitchBlock);
        still("item/glitch_shard", Ring5Textures::glitchShard);
        still("block/voidstone", Ring5Textures::voidstone);
        twinklingGlowLayer("block/voidstone", STAR);
        pulsing("block/nullite_ore_overlay", NULLITE_GLOW, NULLITE_DIM, Ring5Textures::nulliteOreOverlay);
        // INT3: the voidstone ore keeps voidstone's stars, minus the pixels its crystals cover (one more layer, no z-fight).
        twinklingGlowLayer("block/voidstone", "block/voidstone_nullite_ore_glow", STAR, "block/nullite_ore_overlay");

        // ---- 90: Nether ring 5 ----
        still("block/chalkrack", Ring5Textures::chalkrack);
        still("block/white_stone", Ring5Textures::whiteStone);
        still("block/polished_white_stone", Ring5Textures::polishedWhiteStone);
        still("block/white_stone_bricks", (image, random) -> stoneBricks(image, random, WHITE_STONE, WHITE_MORTAR));
        still("block/white_stone_pillar", Ring5Textures::whiteStonePillar);
        still("block/white_stone_pillar_top", Ring5Textures::whiteStonePillarTop);
        still("block/mirror_stone", Ring5Textures::mirrorStone);
        still("block/still_light", Ring5Textures::stillLight);
        still("block/white_core_ore_overlay", Ring5Textures::whiteCoreOreOverlay);
        still("item/white_core", Ring5Textures::whiteCoreItem);

        // ---- Particles (contract §6, sprites for A in 98): 8×8, own colours ----
        for (int i = 0; i < 4; i++) {
            int n = i;
            still("particle/void_mote_" + i, (image, random) -> particle(image, VOID_MOTES[n], MOTE));
        }
        for (int i = 0; i < 4; i++) {
            int n = i;
            still("particle/glitch_pixel_" + i, (image, random) -> particle(image, GLITCH_PIXELS[n], GLITCH_PIXEL));
        }
        for (int i = 0; i < 2; i++) {
            int n = i;
            still("particle/reverse_raindrop_" + i, (image, random) -> particle(image, REVERSE_RAINDROPS[n], RAINDROP));
        }
    }

    private static void still(String name, Painter painter) {
        TEXTURES.put(name, new Texture(painter, null, Animation.STILL));
    }

    /** An 8-frame pulse towards {@code glow}, the glow colour of this material, and back towards its {@code dim} tone. */
    private static void pulsing(String name, int glow, int dim, Painter painter) {
        TEXTURES.put(name, new Texture(painter, null, new Animation(PULSE_FRAMES, PULSE_FRAMETIME, true, glow, dim, false, false)));
    }

    /** A moving, opaque pattern, looping seamlessly because the painter is periodic in its phase. */
    private static void cycling(String name, int frames, int frametime, CyclingPainter painter) {
        TEXTURES.put(name, new Texture(null, painter, new Animation(frames, frametime, false, 0, 0, true, true)));
    }

    /**
     * {@code <block>_glow}: the pixels the block's painter marked as glowing ({@link #glow}), everything else
     * transparent; every star twinkles on its own slow beat (8 interpolated frames of 8 ticks, ~3 s), each leaning
     * towards {@code tones[0]} at its own phase. Only colours change, so the alpha mask stays identical over the frames.
     */
    private static void twinklingGlowLayer(String block, int[] tones) {
        twinklingGlowLayer(block, registerGlowLayer(block, tones), tones, null);
    }

    /**
     * {@link #twinklingGlowLayer} as {@code name}, leaving out the pixels where {@code cover} (an overlay drawn over it in
     * the same model, null for none) is opaque.
     */
    private static void twinklingGlowLayer(String block, String name, int[] tones, String cover) {
        GLOW_LAYERS.put(name, block);
        CyclingPainter painter = (image, random, phase) -> {
            BufferedImage source = renderUnchecked(block);
            boolean[][] mask = glowMask(source);
            if (cover != null) {
                BufferedImage covering = renderUnchecked(cover);
                boolean[][] uncovered = new boolean[SIZE][SIZE];
                for (int y = 0; y < SIZE; y++) {
                    for (int x = 0; x < SIZE; x++) {
                        uncovered[x][y] = mask[x][y] && !isOpaque(covering, x, y);
                    }
                }
                mask = uncovered;
            }
            int[][] star = components(glowMask(source));
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    if (!mask[x][y]) {
                        continue;
                    }
                    double offset = frac(star[x][y] * 0.618034);
                    double bright = 0.5 + 0.5 * Math.cos(2 * Math.PI * (phase + offset));
                    image.setRGB(x, y, 0xFF000000 | mix(source.getRGB(x, y) & 0xFFFFFF, tones[0], 0.6 * (1 - bright)));
                }
            }
        };
        TEXTURES.put(name, new Texture(null, painter, new Animation(8, 8, true, 0, tones[0], true, false)));
    }

    /** Labels the 8-connected groups of marked pixels 0, 1, 2, … (scan order); unmarked pixels get -1. */
    private static int[][] components(boolean[][] mask) {
        int[][] label = new int[SIZE][SIZE];
        for (int[] column : label) {
            java.util.Arrays.fill(column, -1);
        }
        int next = 0;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (!mask[x][y] || label[x][y] >= 0) {
                    continue;
                }
                List<int[]> stack = new ArrayList<>();
                stack.add(new int[] {x, y});
                label[x][y] = next;
                while (!stack.isEmpty()) {
                    int[] p = stack.remove(stack.size() - 1);
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            int nx = p[0] + dx;
                            int ny = p[1] + dy;
                            if (nx >= 0 && ny >= 0 && nx < SIZE && ny < SIZE && mask[nx][ny] && label[nx][ny] < 0) {
                                label[nx][ny] = next;
                                stack.add(new int[] {nx, ny});
                            }
                        }
                    }
                }
                next++;
            }
        }
        return label;
    }

    /** Records {@code <block>_glow} as the glow layer of {@code block} with the glow tones {@code tones[1..]}; returns its name. */
    private static String registerGlowLayer(String block, int[] tones) {
        String name = block + "_glow";
        GLOW_LAYERS.put(name, block);
        Set<Integer> glowing = new HashSet<>();
        for (int i = 1; i < tones.length; i++) {
            glowing.add(tones[i]);
        }
        GLOW_TONES.put(block, glowing);
        return name;
    }

    /** Paints a glowing pixel: the colour, and the mark the glow layer reads. */
    private static void glow(BufferedImage image, int x, int y, int rgb) {
        set(image, x, y, rgb);
        GLOW_MASKS.computeIfAbsent(image, key -> new boolean[SIZE][SIZE])[x][y] = true;
    }

    private static boolean[][] glowMask(BufferedImage image) {
        return GLOW_MASKS.getOrDefault(image, new boolean[SIZE][SIZE]);
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("java.awt.headless", "true");
        if (!Files.isDirectory(Path.of("src/main/resources"))) {
            throw new IllegalStateException("Run from the repository root (src/main/resources not found)");
        }
        if (args.length != 0 && !(args.length == 2 && args[0].equals("--vanilla"))) {
            throw new IllegalArgumentException("usage: java tools/textures/Ring5Textures.java [--vanilla <minecraft resources jar>]");
        }
        Path vanillaJar = args.length == 2 ? Path.of(args[1]) : findJar();
        // Checked before anything is written (rendering is pure), so a failed check leaves no broken texture behind.
        checkGlowLayers();
        checkSideRules();
        Map<String, BufferedImage> outputs = new LinkedHashMap<>();
        for (Map.Entry<String, Texture> entry : TEXTURES.entrySet()) {
            String name = entry.getKey();
            if (HAND_EDITED.contains(name)) {
                continue;
            }
            Animation animation = entry.getValue().animation();
            BufferedImage output = animation.animated() ? animationStrip(name, render(name), animation) : render(name);
            check(name, output, animation);
            outputs.put(name, output);
        }
        for (Map.Entry<String, BufferedImage> entry : outputs.entrySet()) {
            String name = entry.getKey();
            Path file = OUTPUT.resolve(name + ".png");
            Files.createDirectories(file.getParent());
            ImageIO.write(entry.getValue(), "png", file.toFile());
            System.out.println("wrote " + file);
            Animation animation = TEXTURES.get(name).animation();
            if (animation.animated()) {
                Path meta = OUTPUT.resolve(name + ".png.mcmeta");
                Files.writeString(meta, "{\n  \"animation\": {\n    \"frametime\": " + animation.frametime()
                        + ",\n    \"interpolate\": " + animation.interpolate() + "\n  }\n}\n", StandardCharsets.UTF_8);
                System.out.println("wrote " + meta);
            }
        }
        System.out.println(outputs.size() + " textures");
        lint();
        try (Vanilla vanilla = Vanilla.open(vanillaJar)) {
            writePreview(vanilla);
        }
    }

    // ---- Checks -------------------------------------------------------------------------------------------------

    /** Textures that are calm or sparse on purpose, with the reason (flatness lint). */
    private static final Map<String, String> FLAT_OK = Map.of(
            "block/glitch_block", "vanilla's missing texture: two flat colours (magenta, black) by design, the breaks add the rest");

    private static final List<String> LINT_WARNINGS = new ArrayList<>();

    /**
     * Warns (does not fail) about textures that read flat or half drawn: an opaque block with fewer than 5 colours or one
     * colour on more than half the pixels (vanilla: 6-9 colours, the commonest on 20-37 %), a sprite with fewer than 5
     * colours, an item whose outline is a single colour all round. Glow layers, particles and the 64×32 sign textures are
     * skipped; exceptions with their reason in {@link #FLAT_OK}.
     */
    private static void lint() throws IOException {
        for (String name : TEXTURES.keySet()) {
            if (name.startsWith("particle/") || name.startsWith("entity/") || GLOW_LAYERS.containsKey(name) || FLAT_OK.containsKey(name)) {
                continue;
            }
            BufferedImage image = render(name);
            Map<Integer, Integer> colours = new HashMap<>();
            Set<Integer> outline = new HashSet<>();
            int opaque = 0;
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    if (!isOpaque(image, x, y)) {
                        continue;
                    }
                    int rgb = image.getRGB(x, y) & 0xFFFFFF;
                    colours.merge(rgb, 1, Integer::sum);
                    opaque++;
                    if (!isOpaque(image, x - 1, y) || !isOpaque(image, x + 1, y) || !isOpaque(image, x, y - 1) || !isOpaque(image, x, y + 1)) {
                        outline.add(rgb);
                    }
                }
            }
            int commonest = colours.values().stream().max(Integer::compare).orElse(0);
            boolean block = opaque == SIZE * SIZE;
            if (colours.size() < 5) {
                LINT_WARNINGS.add(name + ": " + colours.size() + " colours");
            }
            if (block && commonest * 2 > opaque) {
                LINT_WARNINGS.add(name + ": one colour on " + 100 * commonest / opaque + " %");
            }
            if (name.startsWith("item/") && outline.size() < 2) {
                LINT_WARNINGS.add(name + ": single-colour outline");
            }
        }
        LINT_WARNINGS.forEach(warning -> System.out.println("lint: " + warning));
        System.out.println("lint: " + LINT_WARNINGS.size() + " warning(s)");
    }

    /** The texture as the game shows it at rest (first frame); hand-edited textures come from disk. */
    private static BufferedImage render(String name) throws IOException {
        int width = width(name);
        int height = height(name);
        if (HAND_EDITED.contains(name)) {
            BufferedImage file = ImageIO.read(OUTPUT.resolve(name + ".png").toFile());
            return file.getSubimage(0, 0, width, height);
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        try {
            Texture texture = TEXTURES.get(name);
            if (texture.cyclingPainter() != null) {
                texture.cyclingPainter().paint(image, seeded(name), 0.0);
            } else {
                texture.painter().paint(image, seeded(name));
            }
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

    private static boolean isSign(String name) {
        return name.equals(SIGN_ENTITY) || name.equals(HANGING_SIGN_ENTITY);
    }

    private static int width(String name) {
        return isSign(name) ? SIGN_WIDTH : name.startsWith("particle/") ? PARTICLE_SIZE : SIZE;
    }

    /** Height of one frame. */
    private static int height(String name) {
        return isSign(name) ? SIGN_HEIGHT : name.startsWith("particle/") ? PARTICLE_SIZE : SIZE;
    }

    /**
     * Fails the run on a texture the game would show wrongly: wrong size (a strip of square frames: 16×16, particles
     * 8×8; the sign entity textures 64×32), half-transparent pixels (cutout and mipmaps need alpha 0 or 255), interpolated
     * frames whose alpha mask differs (interpolation would blend alpha), or an opaque cycling texture with a hole.
     */
    private static void check(String name, BufferedImage image, Animation animation) {
        int width = width(name);
        int height = height(name);
        if (image.getWidth() != width || image.getHeight() != height * animation.frames()) {
            throw new IllegalStateException(name + ": " + image.getWidth() + "×" + image.getHeight() + ", expected "
                    + width + "×" + height * animation.frames());
        }
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < width; x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                if (alpha != 0 && alpha != 255) {
                    throw new IllegalStateException(name + ": alpha " + alpha + " at " + x + "," + y);
                }
                boolean opaque = alpha != 0;
                if (animation.opaque() && !opaque) {
                    throw new IllegalStateException(name + ": a cycling texture must be opaque (transparent at " + x + "," + y + ")");
                }
                if (animation.interpolate() && opaque != ((image.getRGB(x, y % height) >>> 24) != 0)) {
                    throw new IllegalStateException(name + ": frame " + y / height + " has another alpha mask at " + x + "," + y % height);
                }
            }
        }
    }

    /**
     * Every glowing block agrees with its glow layer: each pixel its painter marked as glowing has a glow tone, no other
     * pixel uses a glow tone, and the layer is not empty.
     */
    private static void checkGlowLayers() throws IOException {
        for (Map.Entry<String, String> entry : GLOW_LAYERS.entrySet()) {
            String block = entry.getValue();
            BufferedImage image = render(block);
            boolean[][] mask = glowMask(image);
            Set<Integer> tones = GLOW_TONES.get(block);
            int pixels = 0;
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    if (!isOpaque(image, x, y)) {
                        if (mask[x][y]) {
                            throw new IllegalStateException(block + " at " + x + "," + y + ": glowing pixel is transparent");
                        }
                        continue;
                    }
                    boolean glowTone = tones.contains(image.getRGB(x, y) & 0xFFFFFF);
                    if (mask[x][y] != glowTone) {
                        throw new IllegalStateException(block + " at " + x + "," + y + ": "
                                + (mask[x][y] ? "glowing pixel without a glow tone" : "glow tone on a pixel that does not glow"));
                    }
                    pixels += mask[x][y] ? 1 : 0;
                }
            }
            if (pixels == 0) {
                throw new IllegalStateException(entry.getKey() + " is empty");
            }
        }
    }

    /**
     * Side textures that show their ground below a crust must carry exactly the pixels of that ground from the given
     * row down: {side, ground, first row that is ground}.
     */
    private static final String[][] SIDE_RULES = {
            {"block/corrupted_grass_block_side", "block/corrupted_dirt", "6"}};

    private static void checkSideRules() throws IOException {
        for (String[] rule : SIDE_RULES) {
            BufferedImage side = render(rule[0]);
            BufferedImage ground = render(rule[1]);
            for (int y = Integer.parseInt(rule[2]); y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    if (side.getRGB(x, y) != ground.getRGB(x, y)) {
                        throw new IllegalStateException(rule[0] + " differs from " + rule[1] + " at " + x + "," + y);
                    }
                }
            }
        }
    }

    // ---- Veins (the corrupted mark) ----------------------------------------------------------------------------

    /**
     * {@code count} thin violet-black veins: each a 1-px line of {@code minLength}–{@code maxLength} pixels that meanders
     * from a random start in one of the eight directions (every step one pixel forward, now and then 45° to a side, so
     * the line never doubles back into a clump), broken once by a one-pixel gap; dark in the middle, fading at the ends.
     * Wraps around the tile edges; only paints over pixels for which {@code canHost} holds (so a vein stays on the stone or
     * the wood and never fills a hole).
     */
    private static void veins(BufferedImage image, Random random, int count, int minLength, int maxLength, Host canHost) {
        for (int v = 0; v < count; v++) {
            veinAlong(image, random, random.nextInt(SIZE), random.nextInt(SIZE), random.nextInt(8), minLength, maxLength, canHost);
        }
    }

    /** The eight step directions, turning by 45° from one to the next (0 = right, 2 = down, 4 = left, 6 = up). */
    private static final int[][] STEPS = {{1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}};

    /** One vein from (x0, y0) heading along {@link #STEPS}[{@code heading}], see {@link #veins}. */
    private static void veinAlong(BufferedImage image, Random random, int x0, int y0, int heading, int minLength, int maxLength, Host canHost) {
        int length = minLength + random.nextInt(maxLength - minLength + 1);
        int gap = 2 + random.nextInt(Math.max(1, length - 3));
        int x = x0;
        int y = y0;
        for (int j = 0; j < length; j++) {
            int roll = random.nextInt(6);
            int[] step = STEPS[Math.floorMod(heading + (roll == 0 ? -1 : roll == 1 ? 1 : 0), 8)];
            x += step[0];
            y += step[1];
            if (j == gap) {
                continue;
            }
            int px = Math.floorMod(x, SIZE);
            int py = Math.floorMod(y, SIZE);
            if (!isOpaque(image, px, py) || !canHost.test(image.getRGB(px, py) & 0xFFFFFF)) {
                continue;
            }
            boolean end = j == 0 || j == length - 1;
            set(image, px, py, end ? VEIN[2] : j % 3 == 1 ? VEIN[1] : VEIN[0]);
        }
    }

    /** Which existing colours a vein may be painted over. */
    @FunctionalInterface
    private interface Host {
        boolean test(int rgb);

        static Host of(int[]... palettes) {
            Set<Integer> tones = new HashSet<>();
            for (int[] palette : palettes) {
                for (int tone : palette) {
                    tones.add(tone);
                }
            }
            return tones::contains;
        }
    }

    /**
     * One short vein running down the boards of a door or trapdoor (wood tones only, never on the frame or a hole). It
     * starts high enough and far enough from the sides to end inside the tile: door halves do not tile, so a vein must
     * not wrap to the other edge.
     */
    private static void woodVein(BufferedImage image, Random random) {
        veinAlong(image, random, 4 + random.nextInt(8), random.nextInt(SIZE - 7), 2, 4, 6, Host.of(C_WOOD));
    }

    // ---- Corrupted ground (88) ---------------------------------------------------------------------------------

    /**
     * Corrupted grass top: vanilla's grass speckle (fine noise and short blades, dark root and lit tip, lying in every
     * direction) in a leached grey-green with a fixed colour (no tint), one or two thin violet-black veins. Layout picked by
     * {@link #bestOf}.
     */
    private static void corruptedGrassTop(BufferedImage image, Random random) {
        bestOf(image, random, (canvas, r) -> {
            double[][] mid = valueNoise(r, 4, 4);
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    set(canvas, x, y, shade(C_GRASS, 0.5 + 0.3 * (mid[x][y] - 0.5) + 0.5 * (r.nextDouble() - 0.5)));
                }
            }
            int[][] directions = {{0, 1}, {1, 1}, {-1, 1}, {1, 0}};
            for (int i = 0; i < 18; i++) {
                int[] direction = directions[r.nextInt(directions.length)];
                int x = r.nextInt(SIZE);
                int y = r.nextInt(SIZE);
                setWrapped(canvas, x, y, C_GRASS[r.nextInt(2)]);
                setWrapped(canvas, x + direction[0], y + direction[1], C_GRASS[4 + r.nextInt(2)]);
            }
            veins(canvas, r, 1 + r.nextInt(2), 5, 8, Host.of(C_GRASS));
        });
    }

    /**
     * Corrupted grass side: exactly the corrupted dirt (checked: {@link #SIDE_RULES}) under a ragged grey-green band,
     * 2–3 px deep with a few blades hanging 1–2 px lower and a soft shadow under it; no overlay, no tint.
     */
    private static void corruptedGrassSide(BufferedImage image, Random random) {
        copy(renderUnchecked("block/corrupted_dirt"), image);
        double phase = random.nextDouble() * 2 * Math.PI;
        double phase2 = random.nextDouble() * 2 * Math.PI;
        for (int x = 0; x < SIZE; x++) {
            double wave = 0.5 + 0.3 * Math.sin(2 * Math.PI * x / SIZE + phase) + 0.2 * Math.sin(6 * Math.PI * x / SIZE + phase2);
            int depth = 2 + (int) (Math.max(0, Math.min(0.999, wave)) * 2);
            if (random.nextInt(10) < 3) {
                depth += 1 + random.nextInt(2);
            }
            for (int y = 0; y < depth; y++) {
                double t = 1.0 - (double) y / depth;
                set(image, x, y, shade(C_GRASS, 0.2 + 0.6 * t + 0.3 * (random.nextDouble() - 0.5)));
            }
            set(image, x, depth - 1, C_GRASS[random.nextInt(2)]);
            if (random.nextBoolean()) {
                set(image, x, depth, C_DIRT[0]);
            }
        }
        int x = random.nextInt(SIZE);
        set(image, x, 1, VEIN[1]);
        set(image, (x + 1) % SIZE, 1, VEIN[2]);
    }

    /**
     * Corrupted dirt: vanilla dirt's clods (mottled noise, small lit clods with a shadow pixel) in a leached grey-brown,
     * one or two short veins. Layout picked by {@link #bestOf}.
     */
    private static void corruptedDirt(BufferedImage image, Random random) {
        bestOf(image, random, (canvas, r) -> {
            double[][] mid = valueNoise(r, 4, 4);
            double[][] fine = valueNoise(r, 2, 2);
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    double t = 0.48 + 0.35 * (mid[x][y] - 0.5) + 0.3 * (fine[x][y] - 0.5) + 0.3 * (r.nextDouble() - 0.5);
                    set(canvas, x, y, shade(C_DIRT, t));
                }
            }
            for (int i = 0; i < 7; i++) {
                int x = r.nextInt(SIZE);
                int y = r.nextInt(SIZE);
                setWrapped(canvas, x, y, C_DIRT[5]);
                setWrapped(canvas, x + 1, y + 1, C_DIRT[0]);
            }
            veins(canvas, r, 1 + r.nextInt(2), 4, 7, Host.of(C_DIRT));
        });
    }

    /**
     * Corrupted stone: vanilla stone's soft mottle and horizontal smears in a cool grey, a few dark specks, and one or two
     * veins like hairline cracks. Layout picked by {@link #bestOf}.
     */
    private static void corruptedStone(BufferedImage image, Random random) {
        bestOf(image, random, Ring5Textures::corruptedStoneLayout);
    }

    private static void corruptedStoneLayout(BufferedImage image, Random random) {
        double[][] mid = valueNoise(random, 4, 4);
        double[][] smear = valueNoise(random, 8, 2);
        double[][] fine = valueNoise(random, 2, 2);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double t = 0.5 + 0.26 * (mid[x][y] - 0.5) + 0.28 * (smear[x][y] - 0.5) + 0.2 * (fine[x][y] - 0.5)
                        + 0.18 * (random.nextDouble() - 0.5);
                set(image, x, y, shade(C_STONE, t));
            }
        }
        for (int i = 0; i < 4; i++) {
            setWrapped(image, random.nextInt(SIZE), random.nextInt(SIZE), C_STONE[1]);
        }
        veins(image, random, 1 + random.nextInt(2), 6, 9, Host.of(C_STONE));
    }

    /**
     * Corrupted cobblestone: rounded stones (wrapping cells) lit on their upper left in a dark joint, as vanilla's
     * cobblestone; the veins run in the joints (stretches of the joint turned violet-black). Layout picked by
     * {@link #bestOf}.
     */
    private static void corruptedCobblestone(BufferedImage image, Random random) {
        bestOf(image, random, (canvas, r) -> {
            Cells cells = Cells.random(r, 13);
            double[] base = new double[cells.count()];
            for (int i = 0; i < base.length; i++) {
                base[i] = 0.4 + 0.35 * r.nextDouble();
            }
            double[][] veinField = valueNoise(r, 4, 4);
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    Cells.Nearest nearest = cells.nearest(x, y);
                    double gap = nearest.secondDistance() - nearest.distance();
                    double slope = nearest.dx() + nearest.dy();
                    if (gap < 0.3) {
                        set(canvas, x, y, veinField[x][y] > 0.8 ? VEIN[0] : veinField[x][y] > 0.72 ? VEIN[1] : C_MORTAR);
                        continue;
                    }
                    if (gap < 0.6) {
                        set(canvas, x, y, C_STONE[0]);
                        continue;
                    }
                    double light = base[nearest.index()] - 0.4 * slope + 0.14 * (r.nextDouble() - 0.5);
                    if (gap < 1.2) {
                        light += slope < 0 ? 0.16 : -0.14;
                    }
                    set(canvas, x, y, shade(C_STONE, light));
                }
            }
        });
    }

    /**
     * Corrupted sand: fine grey-beige grains in soft drifts (as vanilla sand), a few darker grains and three violet-black
     * flecks that share no row or column, one with a second pixel. Layout picked by {@link #bestOf}.
     */
    private static void corruptedSand(BufferedImage image, Random random) {
        bestOf(image, random, (canvas, r) -> {
            double[][] mid = valueNoise(r, 4, 4);
            double[][] fine = valueNoise(r, 2, 2);
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    double t = 0.5 + 0.3 * (mid[x][y] - 0.5) + 0.25 * (fine[x][y] - 0.5) + 0.35 * (r.nextDouble() - 0.5);
                    set(canvas, x, y, shade(C_SAND, t));
                }
            }
            for (int i = 0; i < 6; i++) {
                setWrapped(canvas, r.nextInt(SIZE), r.nextInt(SIZE), C_SAND[0]);
            }
            List<int[]> flecks = new ArrayList<>();
            for (int attempt = 0; attempt < 200 && flecks.size() < 3; attempt++) {
                int x = r.nextInt(SIZE);
                int y = r.nextInt(SIZE);
                boolean free = true;
                for (int[] f : flecks) {
                    // also apart from the first fleck's second pixel at (x + 1, y + 1)
                    free &= Math.abs(f[0] - x) > 1 && Math.abs(f[1] - y) > 2;
                }
                if (free) {
                    flecks.add(new int[] {x, y});
                }
            }
            for (int i = 0; i < flecks.size(); i++) {
                int x = flecks.get(i)[0];
                int y = flecks.get(i)[1];
                setWrapped(canvas, x, y, VEIN[i == 0 ? 1 : 2]);
                if (i == 0) {
                    setWrapped(canvas, x + 1, y + 1, VEIN[2]);
                }
            }
        });
    }

    // ---- Corrupted wood (88) -----------------------------------------------------------------------------------

    /**
     * Corrupted log side: oak's bark in a leached grey: vertical ridges (lengthwise fibre) split by four wandering dark
     * furrows, each with a lit right wall; the furrows sway as a sine over the height, so the log repeats seamlessly. One
     * or two stretches of a furrow are a vein instead of shadow.
     */
    private static void corruptedLog(BufferedImage image, Random random) {
        double[][] fibre = valueNoise(random, 2, 8);
        double[][] broad = valueNoise(random, 8, 8);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double t = 0.55 + 0.4 * (fibre[x][y] - 0.5) + 0.2 * (broad[x][y] - 0.5) + 0.14 * (random.nextDouble() - 0.5);
                set(image, x, y, shade(C_BARK, t));
            }
        }
        int first = random.nextInt(4);
        int veined = random.nextInt(4);
        for (int f = 0; f < 4; f++) {
            int x0 = first + 4 * f;
            double phase = random.nextDouble() * 2 * Math.PI;
            int veinStart = random.nextInt(SIZE);
            int veinLength = f == veined ? 4 + random.nextInt(3) : 0;
            for (int y = 0; y < SIZE; y++) {
                int x = x0 + (int) Math.round(0.9 * Math.sin(2 * Math.PI * y / SIZE + phase));
                boolean broken = random.nextInt(9) == 0;
                if (broken) {
                    continue;
                }
                boolean vein = Math.floorMod(y - veinStart, SIZE) < veinLength;
                setWrapped(image, x, y, vein ? VEIN[Math.floorMod(y - veinStart, SIZE) % 2] : C_BARK[0]);
                setWrapped(image, x + 1, y, C_BARK[4 + random.nextInt(2)]);
            }
        }
    }

    /** Corrupted log end grain: grey growth rings in a bark rim, and a violet pith where the corruption sits. */
    private static void corruptedLogTop(BufferedImage image, Random random) {
        logTop(image, random, C_WOOD, C_BARK);
        set(image, 7, 7, VIOLET_CORE[2]);
        set(image, 8, 7, VIOLET_CORE[1]);
        set(image, 7, 8, VIOLET_CORE[1]);
        set(image, 8, 8, VIOLET_CORE[0]);
        int[][] around = {{6, 7}, {9, 8}, {8, 6}, {7, 9}};
        for (int i = 0; i < 2; i++) {
            int[] p = around[random.nextInt(around.length)];
            set(image, p[0], p[1], VEIN[2]);
        }
    }

    /** Stripped corrupted log: grey wood fibre ({@link #strippedLog}) with a single vein running along it. */
    private static void strippedCorruptedLog(BufferedImage image, Random random) {
        strippedLog(image, random, C_WOOD);
        veinAlong(image, random, random.nextInt(SIZE), random.nextInt(SIZE), 2, 5, 7, Host.of(C_WOOD));
    }

    /** Corrupted planks: oak's four planks in grey wood, a short vein along the grain of two or three planks, at a varying spot (not every plank, so a
     * floor is not dashed all over). */
    private static void corruptedPlanks(BufferedImage image, Random random) {
        planks(image, random, C_WOOD);
        int[] joints = {5, 12, 2, 9};
        int skip = random.nextInt(4);
        for (int plank = 0; plank < 4; plank++) {
            if (plank == skip || (plank == (skip + 2) % 4 && random.nextBoolean())) {
                continue;
            }
            int y = plank * 4 + 1 + random.nextInt(2);
            int length = 2 + random.nextInt(3);
            int x0 = random.nextInt(SIZE);
            for (int j = 0; j < length; j++) {
                int x = Math.floorMod(x0 + j, SIZE);
                if (x == joints[plank]) {
                    break;
                }
                set(image, x, y, j == 0 || j == length - 1 ? VEIN[2] : VEIN[1]);
            }
        }
    }

    /**
     * Corrupted leaves (99b): small leaflets of three to five pixels (diagonal strokes both ways and diamonds), lit on
     * their upper-left pixel and dark on their lower right, scattered over the tile until ~60 % is covered (thinner than
     * oak's ~67 %; cutout_mipped), two dead violet-black spots; fixed colour, no tint. The first version's large holes
     * (one per tile) repeated visibly and read as black lace from below, where vanilla shades a down face to half.
     */
    private static void corruptedLeaves(BufferedImage image, Random random) {
        double[][] clumps = valueNoise(random, 4, 4);
        boolean[][] leaf = new boolean[SIZE][SIZE];
        int covered = 0;
        for (int attempt = 0; attempt < 2000 && covered < SIZE * SIZE * 0.6; attempt++) {
            int[][] shape = LEAFLETS[random.nextInt(LEAFLETS.length)];
            int x0 = random.nextInt(SIZE);
            int y0 = random.nextInt(SIZE);
            double base = 0.1 + 0.5 * clumps[x0][y0] + 0.2 * (random.nextDouble() - 0.5);
            for (int i = 0; i < shape.length; i++) {
                int x = Math.floorMod(x0 + shape[i][0], SIZE);
                int y = Math.floorMod(y0 + shape[i][1], SIZE);
                double t = i == 0 ? base + 0.3 : i == shape.length - 1 ? base - 0.25 : base;
                covered += leaf[x][y] ? 0 : 1;
                leaf[x][y] = true;
                set(image, x, y, shade(C_LEAF, t));
            }
        }
        for (int placed = 0, attempt = 0; placed < 2 && attempt < 100; attempt++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            if (leaf[x][y] && (image.getRGB(x, y) & 0xFFFFFF) != VEIN[0]) {
                set(image, x, y, VEIN[placed]);
                placed++;
            }
        }
    }

    /** Leaflet shapes for {@link #corruptedLeaves}, first pixel the lit tip, last the shadowed base. */
    private static final int[][][] LEAFLETS = {
            {{0, 0}, {1, 0}, {1, 1}, {2, 1}, {2, 2}},
            {{2, 0}, {1, 0}, {1, 1}, {0, 1}, {0, 2}},
            {{1, 0}, {0, 1}, {2, 1}, {1, 1}, {1, 2}},
            {{0, 0}, {1, 0}, {0, 1}, {1, 1}, {2, 2}},
            {{1, 0}, {2, 0}, {1, 1}, {2, 1}, {0, 2}},
            {{0, 0}, {1, 1}, {2, 2}},
            {{2, 0}, {1, 1}, {0, 2}},
            {{0, 0}, {0, 1}, {1, 1}, {1, 2}},
            {{1, 0}, {1, 1}, {0, 1}, {0, 2}}};

    /**
     * Log end grain: a bark rim (two pixels, as a ring of {@code bark}), growth rings in squarish rounds lit to the top
     * left, a darker pith.
     */
    private static void logTop(BufferedImage image, Random random, int[] wood, int[] bark) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double dx = x - 7.5;
                double dy = y - 7.5;
                double r = 0.55 * Math.max(Math.abs(dx), Math.abs(dy)) + 0.45 * Math.hypot(dx, dy);
                if (r > 6.6) {
                    set(image, x, y, shade(bark, 0.35 + 0.55 * random.nextDouble()));
                } else if (r > 5.8) {
                    set(image, x, y, shade(bark, 0.05 + 0.3 * random.nextDouble()));
                } else if (r < 1.2) {
                    set(image, x, y, wood[1]);
                } else {
                    double light = 0.5 + 0.22 * Math.cos(r * 2.0) - 0.025 * (dx + dy) + 0.14 * (random.nextDouble() - 0.5);
                    set(image, x, y, shade(wood, light));
                }
            }
        }
    }

    // ---- Signs (88): vanilla's sign model UV layout, own pixels -------------------------------------------------

    /**
     * The sign (entity texture, 64×32, vanilla's {@code SignRenderer} layout). Board: a 24×12×2 box at UV (0, 0): top
     * (2..25, 0..1), bottom (26..49, 0..1), right side (0..1, 2..13), front (2..25, 2..13), left side (26..27, 2..13), back
     * (28..51, 2..13). Post: a 2×14×2 box at (0, 14): top (2..3, 14..15), bottom (4..5, 14..15), its four sides (0..7,
     * 16..29). Front and back are three planks lit on their top row with a dark seam; the post is darker wood.
     */
    private static void signEntity(BufferedImage image, Random random) {
        signFace(image, random, 2, 2, 24, 12);
        signFace(image, random, 28, 2, 24, 12);
        for (int y = 2; y < 14; y++) {
            for (int x : new int[] {0, 1, 26, 27}) {
                set(image, x, y, shade(SIGN_WOOD, 0.3 + 0.25 * random.nextDouble() + ((y - 2) % 4 == 3 ? -0.25 : 0)));
            }
        }
        for (int x = 2; x < 50; x++) {
            set(image, x, 0, shade(SIGN_WOOD, 0.6 + 0.3 * random.nextDouble()));
            set(image, x, 1, shade(SIGN_WOOD, 0.35 + 0.25 * random.nextDouble()));
        }
        for (int x = 2; x < 6; x++) {
            set(image, x, 14, shade(C_BARK, 0.5 + 0.3 * random.nextDouble()));
            set(image, x, 15, shade(C_BARK, 0.3 + 0.3 * random.nextDouble()));
        }
        for (int y = 16; y < 30; y++) {
            for (int x = 0; x < 8; x++) {
                double lit = x % 2 == 0 ? 0.15 : -0.1;
                set(image, x, y, shade(C_BARK, 0.45 + lit + 0.3 * (random.nextDouble() - 0.5)));
            }
        }
        // The vein runs in the bottom seam of the front and back, under the last line of text, never through it.
        for (int face : new int[] {2, 28}) {
            int veinX = face + 1 + random.nextInt(3);
            for (int j = 0; j < 4; j++) {
                set(image, veinX + j, 13, j == 0 || j == 3 ? VEIN[2] : VEIN[j % 2]);
            }
        }
    }

    /**
     * A sign face: three planks of four rows (lit top row, a soft seam at the bottom), staggered joints, faint grain; in
     * {@link #SIGN_WOOD} with little noise, so the text on it stays readable.
     */
    private static void signFace(BufferedImage image, Random random, int x0, int y0, int width, int height) {
        int rows = height / 3;
        for (int plank = 0; plank < 3; plank++) {
            double tone = 0.5 + 0.15 * random.nextDouble();
            int joint = x0 + 3 + random.nextInt(width - 6);
            for (int dy = 0; dy < rows; dy++) {
                int y = y0 + plank * rows + dy;
                for (int x = x0; x < x0 + width; x++) {
                    if (dy == rows - 1 || (x == joint && plank == 1)) {
                        set(image, x, y, SIGN_WOOD[0]);
                    } else {
                        double lit = dy == 0 ? 0.18 : 0.0;
                        double edge = x == x0 ? 0.15 : x == x0 + width - 1 ? -0.2 : 0.0;
                        set(image, x, y, shade(SIGN_WOOD, tone + lit + edge + 0.14 * (random.nextDouble() - 0.5)));
                    }
                }
            }
            int y = y0 + plank * rows + 1 + random.nextInt(2);
            int x = x0 + random.nextInt(width - 4);
            int length = 3 + random.nextInt(3);
            for (int j = 0; j < length; j++) {
                set(image, x + j, y, SIGN_WOOD[1]);
            }
        }
    }

    /**
     * The hanging sign (entity texture, 64×32, vanilla's {@code HangingSignRenderer} layout). Wall bar: a 16×2×4 box at
     * (0, 0): top (4..19, 0..3), bottom (20..35, 0..3), sides and faces (0..39, 4..5). Chains: two 3×6 planes at (0, 6) and
     * (6, 6) (front and back each 3 wide) and the V-chains, a 12×6 plane at (14, 6) (front 14..25, back 26..37). Board: a
     * 14×10×2 box at (0, 12): top (2..15, 12..13), bottom (16..29, 12..13), sides and faces (0..31, 14..23). The board is
     * stripped grey wood in a darker frame; the chains are dark iron links, see-through between the links.
     */
    private static void hangingSignEntity(BufferedImage image, Random random) {
        for (int y = 0; y < 4; y++) {
            for (int x = 4; x < 36; x++) {
                set(image, x, y, shade(C_WOOD, 0.35 + (y == 0 || y == 3 ? -0.15 : 0.15) + 0.3 * (random.nextDouble() - 0.5)));
            }
        }
        for (int y = 4; y < 6; y++) {
            for (int x = 0; x < 40; x++) {
                set(image, x, y, shade(C_WOOD, (y == 4 ? 0.6 : 0.25) + 0.25 * (random.nextDouble() - 0.5)));
            }
        }
        for (int x0 : new int[] {0, 3, 6, 9}) {
            chainStrip(image, x0, 6);
        }
        vChain(image, 14);
        vChain(image, 26);
        for (int x = 2; x < 30; x++) {
            set(image, x, 12, shade(C_WOOD, 0.6 + 0.3 * random.nextDouble()));
            set(image, x, 13, shade(C_WOOD, 0.3 + 0.3 * random.nextDouble()));
        }
        hangingBoard(image, random, 2, 14, 14, 10);
        hangingBoard(image, random, 18, 14, 14, 10);
        for (int y = 14; y < 24; y++) {
            for (int x : new int[] {0, 1, 16, 17}) {
                set(image, x, y, shade(C_WOOD, 0.3 + 0.3 * random.nextDouble()));
            }
        }
    }

    /**
     * A hanging sign's board face: a frame of darker wood lit on its top and left, calm light fibre inside
     * ({@link #SIGN_WOOD}, for the text), one vein in the bottom of the frame.
     */
    private static void hangingBoard(BufferedImage image, Random random, int x0, int y0, int width, int height) {
        double[][] fibre = valueNoise(random, 8, 2);
        for (int dy = 0; dy < height; dy++) {
            for (int dx = 0; dx < width; dx++) {
                int x = x0 + dx;
                int y = y0 + dy;
                boolean frameLit = dy == 0 || dx == 0;
                boolean frameDark = dy == height - 1 || dx == width - 1;
                if (frameDark) {
                    set(image, x, y, C_WOOD[0]);
                } else if (frameLit) {
                    set(image, x, y, C_WOOD[2 + random.nextInt(2)]);
                } else if (dy == 1 || dx == 1) {
                    set(image, x, y, C_WOOD[1]);
                } else {
                    set(image, x, y, shade(SIGN_WOOD, 0.55 + 0.3 * (fibre[dx % SIZE][dy % SIZE] - 0.5) + 0.12 * (random.nextDouble() - 0.5)));
                }
            }
        }
        int x = x0 + 1 + random.nextInt(width - 6);
        for (int j = 0; j < 4; j++) {
            set(image, x + j, y0 + height - 1, j == 0 || j == 3 ? VEIN[2] : VEIN[j % 2]);
        }
    }

    /** A straight chain, 3 px wide and 6 rows: a link seen face-on (two rails, a gap), then one edge-on (the middle). */
    private static void chainStrip(BufferedImage image, int x0, int y0) {
        String[] rows = {"o.x", "o.x", ".#.", ".o.", "o.x", "o.x"};
        for (int dy = 0; dy < rows.length; dy++) {
            for (int dx = 0; dx < 3; dx++) {
                char c = rows[dy].charAt(dx);
                if (c != '.') {
                    set(image, x0 + dx, y0 + dy, c == '#' ? CHAIN[3] : c == 'o' ? CHAIN[2] : CHAIN[1]);
                }
            }
        }
    }

    /** The V-chains (12×6 from {@code x0}, rows 6..11): two chains running down and out from the middle of the bar. */
    private static void vChain(BufferedImage image, int x0) {
        for (int dy = 0; dy < 6; dy++) {
            int left = x0 + 5 - dy;
            int right = x0 + 6 + dy;
            int tone = dy % 2 == 0 ? CHAIN[2] : CHAIN[1];
            set(image, left, 6 + dy, tone);
            set(image, right, 6 + dy, tone);
            if (dy % 2 == 0) {
                set(image, left - 1 < x0 ? left : left - 1, 6 + dy, CHAIN[0]);
                set(image, right + 1 >= x0 + 12 ? right : right + 1, 6 + dy, CHAIN[3]);
            }
        }
    }

    /** The hanging sign in its edit screen (16×16): the wall bar, two chains and the board, as vanilla's gui image. */
    private static void hangingSignGui(BufferedImage image, Random random) {
        for (int x = 0; x < SIZE; x++) {
            set(image, x, 0, shade(C_WOOD, 0.6 + 0.3 * random.nextDouble()));
            set(image, x, 1, shade(C_WOOD, 0.25 + 0.25 * random.nextDouble()));
        }
        for (int y = 2; y < 6; y++) {
            for (int x : new int[] {2, 12}) {
                boolean faceOn = y % 2 == 0;
                set(image, x, y, faceOn ? CHAIN[3] : CHAIN[1]);
                set(image, x + 1, y, faceOn ? CHAIN[1] : CHAIN[2]);
            }
        }
        hangingBoard(image, random, 1, 6, 14, 10);
    }

    /** The sign item: a board of three grey planks (lit top rows, dark seams) on a dark post, selective outline. */
    private static void signItem(BufferedImage image, Random random) {
        for (int y = 2; y < 11; y++) {
            for (int x = 2; x < 15; x++) {
                int row = (y - 2) % 3;
                double t = row == 0 ? 0.75 : row == 1 ? 0.5 : 0.15;
                if (x == 2) {
                    t += 0.1;
                } else if (x == 14) {
                    t -= 0.2;
                }
                set(image, x, y, shade(C_WOOD, t + 0.2 * (random.nextDouble() - 0.5)));
            }
        }
        for (int y = 11; y < 16; y++) {
            set(image, 7, y, C_BARK[4]);
            set(image, 8, y, C_BARK[2]);
            set(image, 9, y, C_BARK[1]);
        }
        set(image, 9, 6, VEIN[1]);
        set(image, 10, 6, VEIN[0]);
        set(image, 11, 6, VEIN[2]);
        outlineInside(image, C_WOOD[1], C_BARK[0]);
    }

    /** The hanging sign item: the bar, two short chains and a framed board, selective outline. */
    private static void hangingSignItem(BufferedImage image, Random random) {
        for (int x = 1; x < 15; x++) {
            set(image, x, 1, shade(C_WOOD, 0.65 + 0.3 * random.nextDouble()));
            set(image, x, 2, shade(C_WOOD, 0.25 + 0.2 * random.nextDouble()));
        }
        for (int x : new int[] {4, 11}) {
            set(image, x, 3, CHAIN[3]);
            set(image, x - 1, 4, CHAIN[2]);
            set(image, x + 1, 4, CHAIN[1]);
            set(image, x, 5, CHAIN[2]);
        }
        for (int y = 6; y < 15; y++) {
            for (int x = 2; x < 14; x++) {
                boolean lit = y == 6 || x == 2;
                boolean dark = y == 14 || x == 13;
                set(image, x, y, dark ? C_WOOD[0] : lit ? C_WOOD[4] : shade(C_WOOD, ((y - 7) % 3 == 0 ? 0.6 : 0.4) + 0.2 * (random.nextDouble() - 0.5)));
            }
        }
        set(image, 6, 10, VEIN[2]);
        set(image, 7, 10, VEIN[0]);
        set(image, 8, 11, VEIN[1]);
        outlineInside(image, C_WOOD[1], C_BARK[0]);
    }

    /**
     * The selective outline drawn on the shape's own edge pixels (for sprites that fill the tile to its edge): the
     * lowest and rightmost opaque pixels become {@code dark}, the topmost and leftmost {@code light}.
     */
    private static void outlineInside(BufferedImage image, int light, int dark) {
        boolean[][] opaque = new boolean[SIZE][SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                opaque[x][y] = isOpaque(image, x, y);
            }
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (!opaque[x][y]) {
                    continue;
                }
                boolean darkEdge = !isOpaqueIn(opaque, x + 1, y) || !isOpaqueIn(opaque, x, y + 1);
                boolean lightEdge = !isOpaqueIn(opaque, x - 1, y) || !isOpaqueIn(opaque, x, y - 1);
                if (darkEdge) {
                    set(image, x, y, dark);
                } else if (lightEdge) {
                    set(image, x, y, light);
                }
            }
        }
    }

    private static boolean isOpaqueIn(boolean[][] opaque, int x, int y) {
        return x >= 0 && y >= 0 && x < SIZE && y < SIZE && opaque[x][y];
    }

    // ---- Glitch (89) -------------------------------------------------------------------------------------------

    /**
     * The order of glitch states over the 8 frames: a state repeats now and then, so with one frame time the image
     * holds, jumps, stutters and holds again instead of ticking evenly.
     */
    private static final int[] GLITCH_STATES = {0, 0, 0, 1, 2, 2, 3, 4};

    /**
     * Glitch block: vanilla's missing texture (8×8 checker of magenta and black) gone wrong: slipped a few pixels
     * sideways, with one quadrant fallen to a 4×4 checker (fixed over the frames). Every state shifts one to
     * three horizontal bands sideways (scanline tearing), moves or inverts one or two 4×4 blocks, dims one row, and in
     * some states drops in a band of a wrong colour (cyan, white). State 0 (the rest frame, shown longest) is only a
     * little broken, so the block still reads as the missing texture.
     */
    private static void glitchBlock(BufferedImage image, Random random, double phase) {
        int state = GLITCH_STATES[(int) Math.floor(phase * GLITCH_STATES.length + 1e-9) % GLITCH_STATES.length];
        Random r = new Random(random.nextLong() + 7919L * state);
        // The same in every frame: the checker slips a few pixels sideways and one quadrant drops to a 4-px checker.
        // Alone that would still tile into one even (if shifted) checkerboard; the 32 orientations of the blockstate
        // break it up, so neighbouring blocks never join (99b).
        Random layout = new Random(random.nextLong());
        int slip = 2 + layout.nextInt(3);
        int fineX = 8 * layout.nextInt(2);
        int fineY = 8 * layout.nextInt(2);
        int[][] pixels = new int[SIZE][SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                boolean fine = x >= fineX && x < fineX + 8 && y >= fineY && y < fineY + 8;
                boolean magenta = fine ? (x / 4 + y / 4) % 2 == 0 : (Math.floorMod(x - slip, SIZE) / 8 + y / 8) % 2 == 0;
                pixels[x][y] = magenta ? MAGENTA : BLACK;
            }
        }
        int bands = state == 0 ? 1 : 1 + r.nextInt(3);
        for (int b = 0; b < bands; b++) {
            int y0 = r.nextInt(SIZE);
            int h = 1 + r.nextInt(state == 0 ? 2 : 3);
            int shift = (r.nextBoolean() ? 1 : -1) * (2 + r.nextInt(state == 0 ? 2 : 5));
            for (int dy = 0; dy < h; dy++) {
                int y = (y0 + dy) % SIZE;
                int[] row = new int[SIZE];
                for (int x = 0; x < SIZE; x++) {
                    row[x] = pixels[Math.floorMod(x - shift, SIZE)][y];
                }
                for (int x = 0; x < SIZE; x++) {
                    pixels[x][y] = row[x];
                }
            }
        }
        int blocks = state == 0 ? 1 : 1 + r.nextInt(2);
        for (int b = 0; b < blocks; b++) {
            int bx = 4 * r.nextInt(4);
            int by = 4 * r.nextInt(4);
            int fromX = 4 * r.nextInt(4);
            int fromY = 4 * r.nextInt(4);
            boolean invert = r.nextBoolean();
            int[][] copy = new int[4][4];
            for (int dy = 0; dy < 4; dy++) {
                for (int dx = 0; dx < 4; dx++) {
                    copy[dx][dy] = pixels[fromX + dx][fromY + dy];
                }
            }
            for (int dy = 0; dy < 4; dy++) {
                for (int dx = 0; dx < 4; dx++) {
                    int rgb = copy[dx][dy];
                    pixels[bx + dx][by + dy] = invert ? invertGlitch(rgb) : rgb;
                }
            }
        }
        int dimRow = r.nextInt(SIZE);
        for (int x = 0; x < SIZE; x++) {
            pixels[x][dimRow] = pixels[x][dimRow] == MAGENTA ? MAGENTA_DIM : BLACK_LIT;
        }
        if (state == 2 || state == 4 || state == 0) {
            int y = r.nextInt(SIZE);
            int x0 = r.nextInt(SIZE);
            int length = state == 0 ? 2 : 3 + r.nextInt(5);
            int odd = state == 4 ? GLITCH_WHITE : GLITCH_CYAN;
            for (int j = 0; j < length; j++) {
                pixels[Math.floorMod(x0 + j, SIZE)][y] = odd;
            }
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, pixels[x][y]);
            }
        }
    }

    private static int invertGlitch(int rgb) {
        return switch (rgb) {
            case MAGENTA -> BLACK;
            case BLACK -> MAGENTA;
            default -> rgb;
        };
    }

    /**
     * Glitch shard: an angular splinter slanting up to the right, checkered magenta and black in 3-px squares, lit on its
     * upper left and dim on its lower right, with one pixel row slipped a pixel sideways (the glitch); selective outline.
     */
    private static void glitchShard(BufferedImage image, Random random) {
        double[][] poly = {{2.6, 14.2}, {3.4, 8.6}, {8.2, 3.2}, {13.6, 1.4}, {11.4, 6.6}, {12.6, 9.0}, {6.8, 13.0}};
        int slipRow = 7 + random.nextInt(2);
        BufferedImage shard = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (!inside(poly, x + 0.5, y + 0.5)) {
                    continue;
                }
                boolean magenta = ((x + 1) / 3 + (y + 2) / 3) % 2 == 0;
                boolean litEdge = !inside(poly, x - 0.5, y + 0.5) || !inside(poly, x + 0.5, y - 0.5);
                boolean dimEdge = !inside(poly, x + 1.5, y + 0.5) || !inside(poly, x + 0.5, y + 1.5);
                int rgb = magenta ? (litEdge ? MAGENTA_LIT : dimEdge ? MAGENTA_DIM : MAGENTA)
                        : (litEdge ? BLACK_LIT : BLACK);
                set(shard, x, y, rgb);
            }
        }
        for (int x = SIZE - 1; x > 0; x--) {
            shard.setRGB(x, slipRow, shard.getRGB(x - 1, slipRow));
        }
        shard.setRGB(0, slipRow, TRANSPARENT);
        copy(shard, image);
        set(image, 12, 2, GLITCH_WHITE);
        outlineSelective(image, 0x5A0060, 0x14001A);
    }

    /** Whether (x, y) lies inside the polygon (even-odd rule). */
    private static boolean inside(double[][] poly, double x, double y) {
        boolean in = false;
        for (int i = 0, j = poly.length - 1; i < poly.length; j = i++) {
            if ((poly[i][1] > y) != (poly[j][1] > y)
                    && x < (poly[j][0] - poly[i][0]) * (y - poly[i][1]) / (poly[j][1] - poly[i][1]) + poly[i][0]) {
                in = !in;
            }
        }
        return in;
    }

    // ---- Void and nullite (89) ---------------------------------------------------------------------------------

    /**
     * Voidstone: near-black with a violet depth (soft nebula wisps of a deeper violet, no grey noise), four or five star
     * pixels of mixed brightness (one with a 1-px cross) and three dim, non-glowing specks of star dust between them. The
     * stars keep apart over the tile edge too, so neither one tile nor the 32 orientations of its blockstate line them
     * up into a grid (99b: the first version's single brightness read as a lattice on large faces). All keep one pixel
     * from the edge (INT3): across the edge of a turned or mirrored neighbour two edge stars would meet.
     * The stars are marked as glowing: {@code voidstone_glow} carries them (emissive, twinkling).
     */
    private static void voidstone(BufferedImage image, Random random) {
        double[][] broad = valueNoise(random, 8, 8);
        double[][] mid = valueNoise(random, 4, 4);
        double[][] wisp = valueNoise(random, 16, 4);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double depth = 0.45 * broad[x][y] + 0.3 * mid[x][y] + 0.25 * wisp[x][y];
                int rgb = depth > 0.8 ? VOID_DEPTH[1] : depth > 0.68 ? VOID_DEPTH[0]
                        : shade(VOID, 0.5 + 1.5 * (depth - 0.5) + 0.25 * (random.nextDouble() - 0.5));
                set(image, x, y, rgb);
            }
        }
        int target = 4 + random.nextInt(2);
        List<int[]> stars = new ArrayList<>();
        for (int attempt = 0; attempt < 500 && stars.size() < target; attempt++) {
            // One pixel in from the edge (the cross star's arms too): the neighbours are turned and mirrored (32
            // orientations), so a star on x = 15 could meet one on x = 0 of the next block as a double star.
            int x = 1 + random.nextInt(SIZE - 2);
            int y = 1 + random.nextInt(SIZE - 2);
            if (starFits(stars, x, y)) {
                stars.add(new int[] {x, y});
            }
        }
        for (int i = 0; i < stars.size(); i++) {
            int x = stars.get(i)[0];
            int y = stars.get(i)[1];
            if (i == 0) {
                glow(image, x, y, STAR[4]);
                glow(image, x - 1, y, STAR[1]);
                glow(image, x + 1, y, STAR[1]);
                glow(image, x, y - 1, STAR[1]);
                glow(image, x, y + 1, STAR[1]);
            } else {
                glow(image, x, y, STAR[2 + i % 3]);
            }
        }
        List<int[]> taken = new ArrayList<>(stars);
        for (int placed = 0, attempt = 0; placed < 3 && attempt < 500; attempt++) {
            int x = 1 + random.nextInt(SIZE - 2);
            int y = 1 + random.nextInt(SIZE - 2);
            if (dustFits(taken, x, y)) {
                taken.add(new int[] {x, y});
                set(image, x, y, STAR_DUST);
                placed++;
            }
        }
    }

    /**
     * Whether a star at (x, y) keeps clear of the stars so far, measured across the tile edge: never on the row or column
     * of another star, more than one apart from the cross star ({@code stars[0]}) in both so it never meets its arms, and
     * more than four apart in Manhattan distance.
     */
    private static boolean starFits(List<int[]> stars, int x, int y) {
        for (int i = 0; i < stars.size(); i++) {
            int dx = wrappedDistance(stars.get(i)[0], x);
            int dy = wrappedDistance(stars.get(i)[1], y);
            int lane = i == 0 ? 1 : 0;
            if (dx <= lane || dy <= lane || dx + dy <= 4) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether a dust speck at (x, y) keeps more than two apart (Manhattan, across the tile edge) from every point, and from
     * the cross star ({@code points[0]}) more than two from the ends of its arms too.
     */
    private static boolean dustFits(List<int[]> points, int x, int y) {
        for (int i = 0; i < points.size(); i++) {
            int[] p = points.get(i);
            if (wrappedDistance(p[0], x) + wrappedDistance(p[1], y) <= (i == 0 ? 3 : 2)) {
                return false;
            }
        }
        return true;
    }

    private static int wrappedDistance(int a, int b) {
        return Math.min(Math.floorMod(a - b, SIZE), Math.floorMod(b - a, SIZE));
    }

    /**
     * Nullite overlay (over corrupted stone and voidstone, §11): three dark violet-black crystals, angular
     * chunks with facets (body tones by the light from the top left), a violet rim on their edge pixels (lighter on the
     * lit upper left), a pale glint on the corner nearest the light, and the selective outline. The whole overlay is
     * emissive; the pulse moves only the rim and the glint (lighter than {@link #NULLITE_DIM}), so in a cave the crystals
     * just show by their violet rims.
     */
    private static void nulliteOreOverlay(BufferedImage image, Random random) {
        double[] sizes = {3.0, 2.3, 1.8};
        int placed = 0;
        for (int attempt = 0; attempt < 800 && placed < sizes.length; attempt++) {
            double radius = sizes[placed] + 0.3 * random.nextDouble();
            double cx = 2 + radius + random.nextDouble() * (SIZE - 4 - 2 * radius);
            double cy = 2 + radius + random.nextDouble() * (SIZE - 4 - 2 * radius);
            double stretchX = 0.7 + 0.5 * random.nextDouble();
            double stretchY = 0.7 + 0.5 * random.nextDouble();
            double cutX = random.nextBoolean() ? 1 : -1;
            double cutY = random.nextBoolean() ? 1 : -1;
            boolean[][] shape = new boolean[SIZE][SIZE];
            int count = 0;
            for (int y = 1; y < SIZE - 1; y++) {
                for (int x = 1; x < SIZE - 1; x++) {
                    double dx = (x + 0.5 - cx) * stretchX;
                    double dy = (y + 0.5 - cy) * stretchY;
                    shape[x][y] = Math.abs(dx) + Math.abs(dy) <= radius * 1.2 && cutX * dx + cutY * dy <= radius * 0.7;
                    count += shape[x][y] ? 1 : 0;
                }
            }
            if (count < 5) {
                continue;
            }
            BufferedImage crystal = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            int glintX = -1;
            int glintY = -1;
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    if (!shape[x][y]) {
                        continue;
                    }
                    double dx = (x + 0.5 - cx) * stretchX;
                    double dy = (y + 0.5 - cy) * stretchY;
                    boolean litEdge = !shape[x - 1][y] || !shape[x][y - 1];
                    boolean darkEdge = !shape[x + 1][y] || !shape[x][y + 1];
                    int rgb;
                    if (litEdge) {
                        rgb = NULLITE_RIM[2];
                    } else if (darkEdge) {
                        rgb = NULLITE_RIM[0];
                    } else {
                        double light = -(dx + dy) / radius;
                        rgb = light > 0.15 ? NULLITE_BODY[2] : light > -0.35 ? NULLITE_BODY[1] : NULLITE_BODY[0];
                        if (dx < dy && light > -0.35 && light <= 0.15) {
                            rgb = NULLITE_BODY[2];
                        }
                    }
                    set(crystal, x, y, rgb);
                    if (glintX < 0 || x + y < glintX + glintY) {
                        glintX = x;
                        glintY = y;
                    }
                }
            }
            set(crystal, glintX, glintY, NULLITE_GLINT);
            if (shape[glintX + 1][glintY + 1]) {
                set(crystal, glintX + 1, glintY + 1, NULLITE_RIM[1]);
            }
            outlineSelective(crystal, NULLITE_OUTLINE_LIGHT, NULLITE_OUTLINE);
            if (merge(image, crystal)) {
                placed++;
            }
        }
    }

    // ---- Nether ring 5 (90) ------------------------------------------------------------------------------------

    /**
     * Chalkrack: netherrack's lumpy build (wrapping cells lit on the upper left, dark crevices) in a chalk white with cold
     * grey-blue shadows; whiter, greyer and cooler than the beige-pink bleached netherrack of Nether ring 4. Layout
     * picked by {@link #bestOf}.
     */
    private static void chalkrack(BufferedImage image, Random random) {
        bestOf(image, random, (canvas, r) -> {
            Cells cells = Cells.random(r, 16);
            double[] base = new double[cells.count()];
            for (int i = 0; i < base.length; i++) {
                base[i] = 0.4 + 0.35 * r.nextDouble();
            }
            double[][] broad = valueNoise(r, 8, 8);
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    Cells.Nearest nearest = cells.nearest(x, y);
                    double gap = nearest.secondDistance() - nearest.distance();
                    double slope = nearest.dx() + nearest.dy();
                    if (gap < 0.45) {
                        set(canvas, x, y, r.nextInt(4) == 0 ? CHALK_CREVICE : CHALK[0]);
                        continue;
                    }
                    double light = base[nearest.index()] + 0.2 * (broad[x][y] - 0.5) - 0.3 * slope + 0.08 * r.nextDouble();
                    if (gap < 1.1 && slope > 0.15) {
                        light -= 0.12;
                    }
                    set(canvas, x, y, shade(CHALK, light));
                }
            }
        });
    }

    /**
     * White stone: smooth white, almost without texture (Nether ring 5 is order and stillness): a calm face over three
     * light tones, with a few soft grey-blue clouds. Layout picked by {@link #bestOf}.
     */
    private static void whiteStone(BufferedImage image, Random random) {
        bestOf(image, random, (canvas, r) -> {
            double[][] broad = valueNoise(r, 8, 8);
            double[][] mid = valueNoise(r, 4, 4);
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    double t = 0.62 + 0.5 * (broad[x][y] - 0.5) + 0.3 * (mid[x][y] - 0.5) + 0.3 * (r.nextDouble() - 0.5);
                    set(canvas, x, y, shade(WHITE_STONE, t));
                }
            }
        });
    }

    /** Polished white stone: smoother still (two tones of calm face) inside vanilla's bevel. */
    private static void polishedWhiteStone(BufferedImage image, Random random) {
        double[][] broad = valueNoise(random, 8, 8);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, shade(WHITE_STONE, 0.6 + 0.26 * (broad[x][y] - 0.5) + 0.2 * (random.nextDouble() - 0.5)));
            }
        }
        bevel(image, WHITE_STONE);
    }

    /**
     * White stone pillar side (as vanilla's quartz pillar): a lit left edge and a shadowed right edge, two vertical
     * grooves (dark line, lit right wall) and a faint lengthwise grain; nothing runs across, so a pillar is seamless.
     */
    private static void whiteStonePillar(BufferedImage image, Random random) {
        double[][] grain = valueNoise(random, 2, 16);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int rgb;
                if (x == 0) {
                    rgb = WHITE_STONE[6];
                } else if (x == SIZE - 1) {
                    rgb = WHITE_STONE[1];
                } else if (x == 3 || x == 12) {
                    rgb = WHITE_STONE[0];
                } else if (x == 4 || x == 13) {
                    rgb = WHITE_STONE[5];
                } else if (x == 2 || x == 11) {
                    rgb = WHITE_STONE[2];
                } else {
                    rgb = shade(WHITE_STONE, 0.6 + 0.3 * (grain[x][y] - 0.5) + 0.1 * (random.nextDouble() - 0.5));
                }
                set(image, x, y, rgb);
            }
        }
    }

    /** White stone pillar top: concentric squares (as vanilla's quartz pillar top): bevelled rim, a groove ring, a calm centre. */
    private static void whiteStonePillarTop(BufferedImage image, Random random) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int ring = Math.min(Math.min(x, y), Math.min(SIZE - 1 - x, SIZE - 1 - y));
                boolean upperLeft = x + y < SIZE - 1;
                int rgb;
                if (ring == 0) {
                    rgb = upperLeft ? WHITE_STONE[6] : WHITE_STONE[1];
                } else if (ring == 3) {
                    rgb = upperLeft ? WHITE_STONE[0] : WHITE_STONE[2];
                } else if (ring == 4) {
                    rgb = upperLeft ? WHITE_STONE[5] : WHITE_STONE[3];
                } else {
                    rgb = shade(WHITE_STONE, (ring < 3 ? 0.55 : 0.68) + 0.14 * (random.nextDouble() - 0.5));
                }
                set(image, x, y, rgb);
            }
        }
    }

    /**
     * Mirror stone: glossy, very light grey-white with a cool blue tint. A soft sheen runs diagonally and a bright
     * reflection streak lies along it, broken into dashes; both are periodic in x and y over 16 px, so a floor shows the
     * reflection continuing over the blocks without a seam.
     */
    private static void mirrorStone(BufferedImage image, Random random) {
        double phase = random.nextDouble();
        double dash = random.nextDouble();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double along = frac((x + y) / (double) SIZE + phase);
                double across = frac((x - y) / (double) SIZE + dash);
                double sheen = 0.55 + 0.12 * Math.cos(2 * Math.PI * along);
                double streak = Math.exp(-Math.pow((along - 0.5) * SIZE / 0.9, 2)) * Math.max(0, Math.cos(2 * Math.PI * across));
                double t = sheen + 0.45 * streak + 0.06 * (random.nextDouble() - 0.5);
                set(image, x, y, shade(MIRROR, t));
            }
        }
    }

    /** Still light: a calm white panel glowing softly from its centre, a pale frame; no flicker, no animation. */
    private static void stillLight(BufferedImage image, Random random) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int ring = Math.min(Math.min(x, y), Math.min(SIZE - 1 - x, SIZE - 1 - y));
                boolean upperLeft = x + y < SIZE - 1;
                int rgb;
                if (ring == 0) {
                    rgb = upperLeft ? STILL_LIGHT[2] : STILL_LIGHT[0];
                } else if (ring == 1) {
                    rgb = upperLeft ? STILL_LIGHT[3] : STILL_LIGHT[1];
                } else {
                    double r = Math.hypot(x - 7.5, y - 7.5) / 7.0;
                    rgb = shade(STILL_LIGHT, 1.0 - 0.6 * r * r + 0.08 * (random.nextDouble() - 0.5));
                }
                set(image, x, y, rgb);
            }
        }
    }

    /**
     * White core overlay (over white stone): two or three small pearl nodules (white-pearl, a faint pink and blue sheen,
     * lit upper left), the selective outline in a grey-blue so they read on the white stone, and a 1-px soft white halo
     * around each (emissive with the overlay: by light it nearly vanishes into the stone, in the dark it glows). Still,
     * like everything in Nether ring 5.
     */
    private static void whiteCoreOreOverlay(BufferedImage image, Random random) {
        int target = 2 + random.nextInt(2);
        int placed = 0;
        for (int attempt = 0; attempt < 800 && placed < target; attempt++) {
            double rx = (placed == 0 ? 2.5 : 1.8) + 0.3 * random.nextDouble();
            double ry = rx * (0.8 + 0.25 * random.nextDouble());
            double cx = 3 + rx + random.nextDouble() * (SIZE - 6 - 2 * rx);
            double cy = 3 + ry + random.nextDouble() * (SIZE - 6 - 2 * ry);
            BufferedImage core = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            pearl(core, random, cx, cy, rx, ry);
            outlineSelective(core, PEARL[1], PEARL[0]);
            outlineSelective(core, HALO[0], HALO[1]);
            if (merge(image, core)) {
                placed++;
            }
        }
    }

    /** White core item: one polished pearl with a faint sheen, its grey-blue outline and the soft white halo. */
    private static void whiteCoreItem(BufferedImage image, Random random) {
        pearl(image, random, 8.0, 8.2, 4.6, 5.0);
        outlineSelective(image, PEARL[1], PEARL[0]);
        outlineSelective(image, HALO[0], HALO[1]);
    }

    /** A pearl (ellipse) in {@link #PEARL} [2..6]: lit upper left, a pink and a blue sheen, a white gloss. */
    private static void pearl(BufferedImage image, Random random, double cx, double cy, double rx, double ry) {
        double swirl = random.nextDouble() * 2 * Math.PI;
        int glossX = -1;
        int glossY = -1;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double dx = (x + 0.5 - cx) / rx;
                double dy = (y + 0.5 - cy) / ry;
                double d = dx * dx + dy * dy;
                if (d > 1) {
                    continue;
                }
                double light = -0.6 * (dx + dy) - 0.3 * d;
                int rgb = PEARL[light > 0.4 ? 5 : light > 0.0 ? 4 : light > -0.45 ? 3 : 2];
                double sheen = Math.sin(3 * dx - 2 * dy + swirl);
                if (light > -0.3 && sheen > 0.85) {
                    rgb = PEARL_PINK;
                } else if (light > -0.3 && sheen < -0.85) {
                    rgb = PEARL_BLUE;
                }
                set(image, x, y, rgb);
                if (glossX < 0 || x + y < glossX + glossY) {
                    glossX = x;
                    glossY = y;
                }
            }
        }
        if (glossX >= 0) {
            set(image, Math.min(SIZE - 1, glossX + 1), Math.min(SIZE - 1, glossY + 1), PEARL[6]);
        }
    }

    // ---- Shared helpers (copied from Ring4Textures.java) --------------------------------------------------------

    /** Tileable value noise in [0, 1) over a 16×16 tile: random values on a grid of {@code cellX} × {@code cellY} pixels. */
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

    private static double frac(double value) {
        return value - Math.floor(value);
    }

    /**
     * Opaque pixel; every texture uses alpha 0 or 255 only (cutout-safe). Overpainting a glowing pixel clears its glow
     * mark, so a glow layer never keeps a pixel that a later stroke covered.
     */
    private static void set(BufferedImage image, int x, int y, int rgb) {
        image.setRGB(x, y, 0xFF000000 | rgb);
        boolean[][] mask = GLOW_MASKS.get(image);
        if (mask != null && x < SIZE && y < SIZE) {
            mask[x][y] = false;
        }
    }

    /** {@link #set} wrapping around the tile edges, for seamless textures. */
    private static void setWrapped(BufferedImage image, int x, int y, int rgb) {
        set(image, Math.floorMod(x, SIZE), Math.floorMod(y, SIZE), rgb);
    }

    /** Vanilla's polished bevel: top row and left column lit, bottom row and right column dark, a softer second ring. */
    private static void bevel(BufferedImage image, int[] palette) {
        int last = palette.length - 1;
        for (int i = 0; i < SIZE; i++) {
            set(image, i, 0, palette[last]);
            set(image, 0, i, palette[last]);
        }
        for (int i = 1; i < SIZE - 1; i++) {
            set(image, i, 1, palette[last - 1]);
            set(image, 1, i, palette[last - 1]);
            set(image, i, SIZE - 2, palette[1]);
            set(image, SIZE - 2, i, palette[1]);
        }
        for (int i = 0; i < SIZE; i++) {
            set(image, i, SIZE - 1, palette[0]);
            set(image, SIZE - 1, i, palette[0]);
        }
    }

    /**
     * Vanilla's stone brick layout: two rows of 16×8 bricks offset by half a brick, a mortar, each brick bevelled (lit
     * top row and left column, shadowed bottom row and right column) over grain, with a few chips on the lit edges.
     */
    private static void stoneBricks(BufferedImage image, Random random, int[] palette, int mortar) {
        double[][] grain = valueNoise(random, 4, 2);
        for (int row = 0; row < 2; row++) {
            int offset = row == 0 ? 0 : 8;
            double brickShade = 0.38 + 0.22 * random.nextDouble();
            for (int dy = 0; dy < 8; dy++) {
                int y = row * 8 + dy;
                for (int x = 0; x < SIZE; x++) {
                    int inBrick = Math.floorMod(x - offset, SIZE);
                    double light = brickShade + 0.35 * (grain[x][y] - 0.5) + 0.14 * random.nextDouble();
                    if (dy == 7 || inBrick == 15) {
                        set(image, x, y, mortar);
                        continue;
                    }
                    if (dy == 0 || inBrick == 0) {
                        light += 0.3;
                    } else if (dy == 6 || inBrick == 14) {
                        light -= 0.3;
                    }
                    set(image, x, y, shade(palette, light));
                }
            }
            for (int i = 0; i < 2; i++) {
                int x = Math.floorMod(offset + 2 + random.nextInt(11), SIZE);
                set(image, x, row * 8, palette[1]);
            }
        }
    }

    /**
     * Paints eight layouts from the seed and keeps the one that repeats least visibly on a wall ({@link #stripeScore}
     * plus {@link #motifScore}).
     */
    private static void bestOf(BufferedImage image, Random random, Painter layout) {
        BufferedImage best = null;
        double bestScore = Double.MAX_VALUE;
        for (int attempt = 0; attempt < 8; attempt++) {
            BufferedImage candidate = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            layout.paint(candidate, new Random(random.nextLong()));
            double score = stripeScore(candidate) + motifScore(candidate);
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        copy(best, image);
    }

    /** The largest difference in mean brightness between the sixteen 4×4 blocks of the tile (a motif a wall repeats). */
    private static double motifScore(BufferedImage image) {
        double[] blocks = new double[16];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                blocks[(y / 4) * 4 + x / 4] += luminance(image.getRGB(x, y) & 0xFFFFFF) / 16;
            }
        }
        double min = Double.MAX_VALUE;
        double max = 0;
        for (double block : blocks) {
            min = Math.min(min, block);
            max = Math.max(max, block);
        }
        return max - min;
    }

    /**
     * How strongly a tile reads as stripes when repeated: the largest step in mean brightness between neighbouring rows,
     * columns, diagonals or anti-diagonals (all wrapping).
     */
    private static double stripeScore(BufferedImage image) {
        double[][] lines = new double[4][SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double l = luminance(image.getRGB(x, y) & 0xFFFFFF) / SIZE;
                lines[0][y] += l;
                lines[1][x] += l;
                lines[2][(x + y) % SIZE] += l;
                lines[3][Math.floorMod(x - y, SIZE)] += l;
            }
        }
        double worst = 0;
        for (double[] line : lines) {
            for (int i = 0; i < SIZE; i++) {
                worst = Math.max(worst, Math.abs(line[i] - line[(i + 1) % SIZE]));
            }
        }
        return worst;
    }

    // ---- Wood helpers (copied from Ring4Textures.java, 37b) ---------------------------------------------------

    /** Stripped wood: lengthwise fibre streaks in five tones, a few long dark grain lines and pale glints. */
    private static void strippedLog(BufferedImage image, Random random, int[] wood) {
        double[][] streaks = valueNoise(random, 2, 8);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double edge = streaks[x][y] - streaks[Math.floorMod(x - 1, SIZE)][y];
                set(image, x, y, shade(wood, 0.15 + 0.7 * streaks[x][y] + 0.6 * edge + 0.12 * random.nextDouble()));
            }
        }
        for (int i = 0; i < 4; i++) {
            int x = 1 + 4 * i + random.nextInt(2);
            int start = random.nextInt(SIZE);
            int length = 5 + random.nextInt(8);
            for (int j = 0; j < length; j++) {
                setWrapped(image, x, start + j, wood[0]);
            }
            setWrapped(image, x + 1, start + random.nextInt(length), wood[wood.length - 1]);
        }
    }

    /**
     * The log's end grain {@code topName} (same pixels) with every pixel in a {@code bark} tone turned into wood and a
     * wood rim; other tones (the violet pith) stay.
     */
    private static void strippedLogTop(BufferedImage image, Random random, String topName, int[] wood, int[] bark) {
        copy(renderUnchecked(topName), image);
        Set<Integer> barkTones = new HashSet<>();
        for (int tone : bark) {
            barkTones.add(tone);
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (barkTones.contains(image.getRGB(x, y) & 0xFFFFFF)) {
                    set(image, x, y, shade(wood, 0.25 + 0.3 * random.nextDouble()));
                }
            }
        }
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

    /** Upper door half: bevelled boards in a frame lit on its top and left, two window holes, one rail, a hinge. */
    private static void doorTop(BufferedImage image, Random random, int[] wood, int[] bark) {
        boards(image, random, wood);
        frame(image, random, bark, true, false);
        rail(image, random, bark, 11);
        window(image, wood, bark, 3, 3, 3, 5);
        window(image, wood, bark, 10, 3, 3, 5);
        hinge(image, bark, 2);
    }

    /** Lower door half: boards, two rails with a diagonal brace between them, a hinge and the handle. */
    private static void doorBottom(BufferedImage image, Random random, int[] wood, int[] bark) {
        boards(image, random, wood);
        frame(image, random, bark, false, true);
        rail(image, random, bark, 2);
        rail(image, random, bark, 12);
        for (int x = 2; x < SIZE - 2; x++) {
            int y = 10 - (x - 2) * 6 / 11;
            set(image, x, y, wood[wood.length - 1]);
            set(image, x, y + 1, wood[1]);
        }
        hinge(image, bark, 13);
        set(image, 12, 0, bark[bark.length - 1]);
        set(image, 12, 1, bark[0]);
        set(image, 13, 1, bark[1]);
    }

    /** Trapdoor as a lattice: slats lit along their top row with see-through gaps, a centre brace, a frame, nails. */
    private static void trapdoor(BufferedImage image, Random random, int[] wood, int[] bark) {
        boards(image, random, wood);
        for (int slat : new int[] {0, 4, 8, 12}) {
            for (int x = 1; x < SIZE - 1; x++) {
                set(image, x, slat, wood[wood.length - 2]);
                set(image, x, slat + 2, wood[1]);
            }
        }
        frame(image, random, bark, true, true);
        for (int gap : new int[] {3, 7, 11}) {
            for (int x = 2; x < SIZE - 2; x++) {
                if (x != 7 && x != 8) {
                    image.setRGB(x, gap, TRANSPARENT);
                }
            }
        }
        for (int y = 1; y < SIZE - 1; y++) {
            set(image, 7, y, bark[bark.length - 2]);
            set(image, 8, y, bark[1]);
        }
        for (int y : new int[] {1, 5, 9, 13}) {
            set(image, 2, y, bark[0]);
            set(image, 13, y, bark[0]);
        }
    }

    /** The door in the inventory (8 wide, columns 4–11): frame, boards, two window holes with a sill, rails, handle. */
    private static void doorItem(BufferedImage image, Random random, int[] wood, int[] bark) {
        int light = bark[bark.length - 2];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 4; x < 12; x++) {
                if (x == 4 || y == 0) {
                    set(image, x, y, x == 4 && y == SIZE - 1 ? bark[1] : light);
                } else if (x == 11 || y == SIZE - 1) {
                    set(image, x, y, bark[0]);
                } else if (x == 8) {
                    set(image, x, y, wood[0]);
                } else {
                    set(image, x, y, shade(wood, (x == 5 || x == 9 ? 0.6 : 0.3) + random.nextDouble() * 0.3));
                }
            }
        }
        for (int y = 2; y <= 4; y++) {
            image.setRGB(6, y, TRANSPARENT);
            image.setRGB(9, y, TRANSPARENT);
        }
        set(image, 6, 5, wood[wood.length - 1]);
        set(image, 9, 5, wood[wood.length - 1]);
        for (int y : new int[] {7, 13}) {
            for (int x = 5; x < 11; x++) {
                set(image, x, y - 1, bark[bark.length - 2]);
                set(image, x, y, bark[1]);
            }
        }
        set(image, 10, 9, bark[bark.length - 1]);
        set(image, 10, 10, bark[0]);
    }

    /** Three vertical boards (seams at x 5 and 10), each with its own tone, a lit bevel right of each seam and grain. */
    private static void boards(BufferedImage image, Random random, int[] wood) {
        double[] boards = {0.35 + random.nextDouble() * 0.3, 0.35 + random.nextDouble() * 0.3, 0.35 + random.nextDouble() * 0.3};
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double board = boards[x < 5 ? 0 : x < 10 ? 1 : 2];
                if (x == 5 || x == 10) {
                    set(image, x, y, wood[0]);
                } else if (x == 1 || x == 6 || x == 11) {
                    set(image, x, y, shade(wood, board + 0.25));
                } else {
                    set(image, x, y, shade(wood, board + (random.nextDouble() - 0.5) * 0.2));
                }
            }
        }
        int[] grain = {2, 3, 7, 8, 12, 13};
        for (int i = 0; i < 5; i++) {
            int x = grain[random.nextInt(grain.length)];
            int start = random.nextInt(SIZE);
            int length = 3 + random.nextInt(5);
            for (int j = 0; j < length; j++) {
                setWrapped(image, x, start + j, wood[1]);
            }
        }
    }

    /** Frame edges: lit on the left and top, dark on the right and bottom (light from the top left). */
    private static void frame(BufferedImage image, Random random, int[] bark, boolean top, boolean bottom) {
        int lit = bark.length - 2;
        for (int i = 0; i < SIZE; i++) {
            set(image, 0, i, bark[lit - random.nextInt(2)]);
            set(image, SIZE - 1, i, bark[random.nextInt(2)]);
            if (top) {
                set(image, i, 0, bark[i > 0 && i < SIZE - 1 ? lit + random.nextInt(2) : lit]);
            }
            if (bottom) {
                set(image, i, SIZE - 1, bark[random.nextInt(2)]);
            }
        }
    }

    /** A two-row rail: lit top row, shadowed bottom row, a nail near each end. */
    private static void rail(BufferedImage image, Random random, int[] bark, int y) {
        for (int x = 1; x < SIZE - 1; x++) {
            set(image, x, y, bark[bark.length - 2 + random.nextInt(2)]);
            set(image, x, y + 1, bark[1 + random.nextInt(2)]);
        }
        set(image, 2, y, bark[0]);
        set(image, SIZE - 3, y, bark[0]);
    }

    /** Two dark hinge pixels on the left frame from row {@code y}, a lit pixel above them. */
    private static void hinge(BufferedImage image, int[] bark, int y) {
        set(image, 1, y - 1, bark[bark.length - 1]);
        set(image, 1, y, bark[0]);
        set(image, 1, y + 1, bark[0]);
    }

    /** A see-through hole sunk into the door: rim above and left in shadow, sill below and right lit. */
    private static void window(BufferedImage image, int[] wood, int[] bark, int x0, int y0, int width, int height) {
        for (int y = y0 - 1; y <= y0 + height; y++) {
            for (int x = x0 - 1; x <= x0 + width; x++) {
                boolean inside = x >= x0 && x < x0 + width && y >= y0 && y < y0 + height;
                if (inside) {
                    image.setRGB(x, y, TRANSPARENT);
                } else if (y == y0 - 1 || x == x0 - 1) {
                    set(image, x, y, bark[0]);
                } else {
                    set(image, x, y, wood[wood.length - 1]);
                }
            }
        }
    }

    private static void particle(BufferedImage image, String[] rows, int[] tones) {
        for (int y = 0; y < PARTICLE_SIZE; y++) {
            for (int x = 0; x < PARTICLE_SIZE; x++) {
                switch (rows[y].charAt(x)) {
                    case '#' -> set(image, x, y, tones[0]);
                    case 'o' -> set(image, x, y, tones[1]);
                    case 'x' -> set(image, x, y, tones[2]);
                    default -> {
                    }
                }
            }
        }
    }

    /**
     * The animation strip: the frames under each other, each the rest frame leaning towards the material's dim tone or
     * its glow colour along a cosine. Only tones lighter than the dim tone move; outline and shadows stay (37b). Only
     * colours change; the alpha mask stays identical.
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
                        int rgb = argb & 0xFFFFFF;
                        if (luminance(rgb) > luminance(animation.dim())) {
                            rgb = mix(mix(rgb, animation.dim(), 0.12 * (1 - glow)), animation.glow(), 0.3 * glow);
                        }
                        set(strip, x, i * size + y, rgb);
                    }
                }
            }
        }
        return strip;
    }

    /** The frames of a cycling texture under each other: the same painter and seed, only the phase moves. */
    private static BufferedImage cyclingStrip(String name, Animation animation) {
        int size = width(name);
        BufferedImage strip = new BufferedImage(size, size * animation.frames(), BufferedImage.TYPE_INT_ARGB);
        CyclingPainter painter = TEXTURES.get(name).cyclingPainter();
        for (int i = 0; i < animation.frames(); i++) {
            BufferedImage frame = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            painter.paint(frame, seeded(name), i / (double) animation.frames());
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    strip.setRGB(x, i * size + y, frame.getRGB(x, y));
                }
            }
        }
        return strip;
    }

    private static BufferedImage animationStrip(String name, BufferedImage rest, Animation animation) {
        return animation.cycling() ? cyclingStrip(name, animation) : pulseStrip(rest, animation);
    }

    /** Random cell centres in a wrapping tile, for stones, lumps and cracks. */
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

    /**
     * Selective outline (37b-3 A): every transparent pixel next to the shape becomes {@code dark} below or right of the
     * shape and {@code light} (a dark tint of the material) above or left of it.
     */
    private static void outlineSelective(BufferedImage image, int light, int dark) {
        boolean[][] opaque = new boolean[SIZE][SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                opaque[x][y] = isOpaque(image, x, y);
            }
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (opaque[x][y]) {
                    continue;
                }
                boolean belowOrRight = (x > 0 && opaque[x - 1][y]) || (y > 0 && opaque[x][y - 1]);
                boolean aboveOrLeft = (x < SIZE - 1 && opaque[x + 1][y]) || (y < SIZE - 1 && opaque[x][y + 1]);
                if (belowOrRight) {
                    set(image, x, y, dark);
                } else if (aboveOrLeft) {
                    set(image, x, y, light);
                }
            }
        }
    }

    /** Copies the opaque pixels of {@code part} into {@code image} if none touches an opaque pixel; returns whether it did. */
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

    /** Opaque pixel inside the 16×16 tile; outside counts as transparent. */
    private static boolean isOpaque(BufferedImage image, int x, int y) {
        return x >= 0 && y >= 0 && x < SIZE && y < SIZE && x < image.getWidth() && y < image.getHeight()
                && (image.getRGB(x, y) >>> 24) != 0;
    }

    private static Random seeded(String name) {
        return new Random(name.hashCode());
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

    /** Perceived brightness of an RGB colour in [0, 1] (Rec. 709 weights). */
    private static double luminance(int rgb) {
        return (0.2126 * (rgb >> 16 & 0xFF) + 0.7152 * (rgb >> 8 & 0xFF) + 0.0722 * (rgb & 0xFF)) / 255.0;
    }

    /** Copies the pixels, and the glow marks if {@code from} has any. */
    private static void copy(BufferedImage from, BufferedImage to) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                to.setRGB(x, y, from.getRGB(x, y));
            }
        }
        boolean[][] mask = GLOW_MASKS.get(from);
        if (mask != null) {
            boolean[][] copied = new boolean[SIZE][SIZE];
            for (int x = 0; x < SIZE; x++) {
                copied[x] = mask[x].clone();
            }
            GLOW_MASKS.put(to, copied);
        }
    }

    private static double smoothstep(double t) {
        return t * t * (3 - 2 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    // ---- Preview (contact sheet, build/texture-preview/ring5.png; not in git) --------------------------------------

    /** The one Minecraft resources jar in {@link #ARTIFACTS}, or {@code null} (the sheet then shows crosses for vanilla). */
    private static Path findJar() throws IOException {
        if (!Files.isDirectory(ARTIFACTS)) {
            return null;
        }
        List<Path> found = new ArrayList<>();
        try (DirectoryStream<Path> jars = Files.newDirectoryStream(ARTIFACTS, "*client-extra*.jar")) {
            jars.forEach(found::add);
        }
        return found.size() == 1 ? found.get(0) : null;
    }

    /** Vanilla textures read from the resources jar, only for the sheet (never written anywhere else). */
    private static final class Vanilla implements AutoCloseable {
        private final ZipFile jar;

        private Vanilla(ZipFile jar) {
            this.jar = jar;
        }

        static Vanilla open(Path path) throws IOException {
            if (path == null) {
                System.out.println("preview: no Minecraft resources jar, vanilla columns show a cross");
                return new Vanilla(null);
            }
            System.out.println("preview: vanilla from " + path.getFileName());
            return new Vanilla(new ZipFile(path.toFile()));
        }

        /** The first frame of {@code minecraft:textures/<name>.png}, multiplied by {@code tint} (-1: none), or null. */
        BufferedImage get(String name, int tint) throws IOException {
            if (jar == null) {
                return null;
            }
            ZipEntry entry = jar.getEntry("assets/minecraft/textures/" + name + ".png");
            if (entry == null) {
                return null;
            }
            BufferedImage strip;
            try (InputStream in = jar.getInputStream(entry)) {
                strip = ImageIO.read(in);
            }
            int size = strip.getWidth();
            BufferedImage frame = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int argb = rawArgb(strip, x, y);
                    if (tint >= 0 && (argb >>> 24) != 0) {
                        argb = argb & 0xFF000000 | multiply(argb & 0xFFFFFF, tint);
                    }
                    frame.setRGB(x, y, argb);
                }
            }
            return frame;
        }

        /**
         * The pixel as stored: Java converts a greyscale PNG (vanilla's stone, grass, leaves) from a linear grey space,
         * which brightens it, so grey images are read straight from the raster.
         */
        private static int rawArgb(BufferedImage image, int x, int y) {
            if (image.getColorModel().getColorSpace().getType() != java.awt.color.ColorSpace.TYPE_GRAY) {
                return image.getRGB(x, y);
            }
            java.awt.image.Raster raster = image.getRaster();
            int grey = raster.getSample(x, y, 0);
            int alpha = raster.getNumBands() > 1 ? raster.getSample(x, y, 1) : 255;
            return alpha << 24 | grey << 16 | grey << 8 | grey;
        }

        /** Vanilla's grass block side: the dirt side with the tinted grass overlay on top. */
        BufferedImage grassSide() throws IOException {
            BufferedImage side = get("block/grass_block_side", -1);
            BufferedImage overlay = get("block/grass_block_side_overlay", GRASS_TINT);
            if (side == null || overlay == null) {
                return null;
            }
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    if ((overlay.getRGB(x, y) >>> 24) != 0) {
                        side.setRGB(x, y, overlay.getRGB(x, y));
                    }
                }
            }
            return side;
        }

        @Override
        public void close() throws IOException {
            if (jar != null) {
                jar.close();
            }
        }
    }

    /** Plains grass and foliage colours, for vanilla's grey grass and leaves on the sheet. */
    private static final int GRASS_TINT = 0x91BD59;
    private static final int FOLIAGE_TINT = 0x77AB2F;

    private static int multiply(int rgb, int tint) {
        int r = (rgb >> 16 & 0xFF) * (tint >> 16 & 0xFF) / 255;
        int g = (rgb >> 8 & 0xFF) * (tint >> 8 & 0xFF) / 255;
        int b = (rgb & 0xFF) * (tint & 0xFF) / 255;
        return r << 16 | g << 8 | b;
    }

    /** Ours beside vanilla (6×6 walls): {ours, vanilla texture or "grass_side", tint}. */
    private static final String[][] PAIRS = {
            {"block/corrupted_grass_block_top", "block/grass_block_top", "grass"},
            {"block/corrupted_grass_block_side", "grass_side", ""},
            {"block/corrupted_dirt", "block/dirt", ""},
            {"block/corrupted_stone", "block/stone", ""},
            {"block/corrupted_cobblestone", "block/cobblestone", ""},
            {"block/corrupted_sand", "block/sand", ""},
            {"block/corrupted_log", "block/oak_log", ""},
            {"block/corrupted_log_top", "block/oak_log_top", ""},
            {"block/stripped_corrupted_log", "block/stripped_oak_log", ""},
            {"block/corrupted_planks", "block/oak_planks", ""},
            {"block/corrupted_leaves", "block/oak_leaves", "foliage"},
            {"block/chalkrack", "block/netherrack", ""},
            {"block/white_stone_bricks", "block/stone_bricks", ""},
            {"block/white_stone_pillar", "block/quartz_pillar", ""},
            {"block/white_stone_pillar_top", "block/quartz_pillar_top", ""}};

    /** Our other blocks as 6×6 walls (repetition). */
    private static final String[] WALLS = {"block/stripped_corrupted_log_top", "block/glitch_block", "block/voidstone",
            "night:block/voidstone", "block/nullite_ore_overlay@block/corrupted_stone", "block/nullite_ore_overlay@block/voidstone",
            "block/white_stone", "block/polished_white_stone", "block/mirror_stone", "block/still_light",
            "block/white_core_ore_overlay@block/white_stone", "night:block/white_core_ore_overlay@block/white_stone"};

    private static final String[] SLOTS = {"item/corrupted_door", "item/corrupted_sign", "item/corrupted_hanging_sign",
            "item/glitch_shard", "item/raw_nullite", "item/white_core", "block/corrupted_leaves", "gui/hanging_signs/corrupted"};

    private static final String GS = "block/corrupted_grass_block_side";
    private static final String DI = "block/corrupted_dirt";
    private static final String ST = "block/corrupted_stone";
    private static final String LO = "block/corrupted_log";
    private static final String LE = "block/corrupted_leaves";
    /** Inner band (day, side view): a dead tree and a bare one on corrupted grass, sand, cobble, stone with a vein. */
    private static final String[][] INNER_BAND = {
            {"", LE, LE, LE, "", "", "", "", "", ""},
            {LE, LE, LO, LE, LE, "", "", LE, "", ""},
            {"", "", LO, "", "", "", LE, LO, LE, ""},
            {"", "", LO, "", "", "", "", LO, "", "block/glitch_block"},
            {GS, GS, GS, GS, "block/corrupted_sand", "block/corrupted_sand", GS, GS, GS, GS},
            {DI, DI, DI, DI, "block/corrupted_sand", DI, DI, DI, "block/corrupted_cobblestone", DI},
            {ST, ST, ST, ST, ST, ST, "block/nullite_ore_overlay@block/corrupted_stone", ST, ST, ST}};
    private static final String VS = "block/voidstone";
    private static final String VN = "block/nullite_ore_overlay@block/voidstone";
    private static final String SN = "block/nullite_ore_overlay@block/corrupted_stone";
    /** A void scar at night (side view): the walls of a crack, voidstone with nullite in the walls, stone above. */
    private static final String[][] VOID_SCAR = {
            {ST, ST, ST, ST, "", "", ST, ST, ST, ST},
            {ST, SN, ST, VS, "", "", VS, ST, ST, ST},
            {ST, VS, VS, VN, "", "", "", VS, SN, ST},
            {VS, VN, VS, "", "", "", "", VS, VS, VS},
            {VS, VS, "", "", "", "", "", VN, VS, VS},
            {VS, VS, VN, "", "", "", VS, VS, VN, VS},
            {VS, VS, VS, VS, "", VS, VS, VS, VS, VS}};
    private static final String WS = "block/white_stone";
    private static final String PI = "block/white_stone_pillar";
    private static final String MI = "block/mirror_stone";
    private static final String CR = "block/chalkrack";
    /** A white hall (side view): chalkrack ceiling, pillars in a row, a wall of white stone and bricks, the mirror floor. */
    private static final String[][] WHITE_HALL = {
            {CR, CR, CR, CR, CR, CR, CR, CR, CR, CR},
            {"block/polished_white_stone", PI, "", "", PI, "", "", PI, "", "block/white_stone_bricks"},
            {"block/still_light", PI, "", "", PI, "", "", PI, "", "block/white_stone_bricks"},
            {WS, PI, "", "", "block/white_core_ore_overlay@block/white_stone", "", "", PI, "", "block/still_light"},
            {WS, PI, "", "", PI, "", "", PI, "", "block/white_stone_bricks"},
            {MI, MI, MI, MI, MI, MI, MI, MI, MI, MI},
            {WS, WS, WS, "block/white_core_ore_overlay@block/white_stone", WS, WS, WS, WS, WS, WS}};
    private static final String PT = "block/white_stone_pillar_top";
    /** The mirror floor from above, with pillar tops in a grid. */
    private static final String[][] MIRROR_FLOOR = {
            {MI, MI, MI, MI, MI, MI, MI, MI},
            {MI, PT, MI, MI, MI, PT, MI, MI},
            {MI, MI, MI, MI, MI, MI, MI, MI},
            {MI, MI, MI, MI, MI, MI, MI, MI},
            {MI, PT, MI, MI, MI, PT, MI, MI},
            {MI, MI, MI, MI, MI, MI, MI, MI},
            {MI, MI, MI, MI, MI, MI, MI, MI}};

    private static final int CELL_WIDTH = 350;
    private static final int CELL_HEIGHT = 150;
    private static final int COLUMNS = 6;
    private static final int SHEET_WIDTH = COLUMNS * CELL_WIDTH + 20;

    private static void writePreview(Vanilla vanilla) throws IOException {
        BufferedImage sheet = new BufferedImage(SHEET_WIDTH, 9000, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = sheet.createGraphics();
        graphics.setColor(new Color(0x2B2B2B));
        graphics.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        int top = drawExtras(graphics, vanilla, 10);
        int index = 0;
        for (Map.Entry<String, Texture> entry : TEXTURES.entrySet()) {
            String name = entry.getKey();
            if (index == 0 || name.equals(NETHER_FIRST) || name.equals(PARTICLES_FIRST)) {
                if (index % COLUMNS != 0) {
                    index += COLUMNS - index % COLUMNS;
                }
                graphics.setColor(new Color(0xFFC857));
                String title = index == 0 ? "== RING 5 ==" : name.equals(NETHER_FIRST) ? "== NETHER RING 5 ==" : "== PARTICLES (8x8) ==";
                graphics.drawString(title, 10, top + (index / COLUMNS) * CELL_HEIGHT + 12);
                index += COLUMNS;
            }
            int left = 10 + (index % COLUMNS) * CELL_WIDTH;
            int cellTop = top + (index / COLUMNS) * CELL_HEIGHT;
            BufferedImage frame = render(name);
            graphics.setColor(Color.WHITE);
            graphics.drawString(name.replace("block/", "").replace("item/", "item: ").replace("particle/", "particle: ")
                    + (HAND_EDITED.contains(name) ? " (hand)" : ""), left, cellTop + 12);
            if (isSign(name)) {
                drawOn(graphics, frame, left, cellTop + 16, 4, 0x4A4652);
                index += 2;
                continue;
            }
            int scale = 96 / frame.getWidth();
            drawOn(graphics, frame, left, cellTop + 16, scale, 0x4A4652);
            int tileScale = 2 * SIZE / frame.getWidth();
            for (int ty = 0; ty < 3; ty++) {
                for (int tx = 0; tx < 3; tx++) {
                    drawOn(graphics, frame, left + 104 + tx * SIZE * 2, cellTop + 16 + ty * SIZE * 2, tileScale, 0x4A4652);
                }
            }
            Animation animation = entry.getValue().animation();
            if (animation.animated()) {
                BufferedImage strip = animationStrip(name, frame, animation);
                for (int i = 0; i < animation.frames(); i++) {
                    BufferedImage single = strip.getSubimage(0, i * SIZE, SIZE, SIZE);
                    drawOn(graphics, single, left + 208 + (i % 4) * 34, cellTop + 16 + (i / 4) * 34, 2, 0x4A4652);
                }
            }
            index++;
        }
        int bottom = top + ((index + COLUMNS - 1) / COLUMNS) * CELL_HEIGHT + 10;
        graphics.dispose();
        Files.createDirectories(PREVIEW.getParent());
        ImageIO.write(sheet.getSubimage(0, 0, sheet.getWidth(), Math.min(sheet.getHeight(), bottom)), "png", PREVIEW.toFile());
        System.out.println("wrote " + PREVIEW);
    }

    /** Pairs with vanilla, walls, scenes, the wood set, items in a slot, particles; returns the top of the per-texture cells. */
    private static int drawExtras(Graphics2D graphics, Vanilla vanilla, int top) throws IOException {
        int block = SIZE * 2;
        int wallSize = 6 * block;
        label(graphics, "corrupted (left) beside vanilla (right), 6x6 walls at 2x; vanilla grass and leaves tinted plains | lint: "
                + LINT_WARNINGS.size() + " warning(s)", 10, top);
        top += 20;
        for (int i = 0; i < PAIRS.length; i++) {
            int left = 10 + (i % 3) * (2 * wallSize + 40);
            int wallTop = top + (i / 3) * (wallSize + 30);
            graphics.setColor(Color.LIGHT_GRAY);
            graphics.drawString(PAIRS[i][0].replace("block/", "") + "  |  " + PAIRS[i][1].replace("block/", "mc:"), left, wallTop + 10);
            String[] pair = PAIRS[i];
            BufferedImage theirs = pair[1].equals("grass_side") ? vanilla.grassSide()
                    : vanilla.get(pair[1], pair[2].equals("grass") ? GRASS_TINT : pair[2].equals("foliage") ? FOLIAGE_TINT : -1);
            drawWall(graphics, render(pair[0]), left, wallTop + 14);
            drawWall(graphics, theirs, left + wallSize + 4, wallTop + 14);
        }
        top += ((PAIRS.length + 2) / 3) * (wallSize + 30) + 10;

        label(graphics, "more walls (6x6 at 2x); 'night' = 20 % light with the emissive layers at full brightness; ores over their hosts", 10, top);
        top += 20;
        for (int i = 0; i < WALLS.length; i++) {
            int left = 10 + (i % 6) * (wallSize + 24);
            int wallTop = top + (i / 6) * (wallSize + 30);
            graphics.setColor(Color.LIGHT_GRAY);
            graphics.drawString(WALLS[i].replace("block/", "").replace("_ore_overlay", " ore"), left, wallTop + 10);
            drawWall(graphics, sceneCell(WALLS[i].replace("night:", ""), WALLS[i].startsWith("night:")), left, wallTop + 14);
        }
        top += ((WALLS.length + 5) / 6) * (wallSize + 30) + 10;

        label(graphics, "inner band by day (side view) | void scar at night (emissive stars and nullite) | white hall and mirror floor (Nether 5)", 10, top);
        top += 20;
        drawScene(graphics, INNER_BAND, 10, top, 0x4A4652, false);
        drawScene(graphics, VOID_SCAR, 10 + 10 * SIZE * 4 + 16, top, 0x030206, true);
        top += INNER_BAND.length * SIZE * 4 + 16;
        drawScene(graphics, WHITE_HALL, 10, top, 0xE4E6EC, false);
        drawScene(graphics, MIRROR_FLOOR, 10 + 10 * SIZE * 4 + 16, top, 0xE4E6EC, false);
        top += WHITE_HALL.length * SIZE * 4 + 20;

        label(graphics, "wood set at 4x: door, trapdoor, fence (preview stand-in from the planks), sign and hanging sign as their models show the entity texture", 10, top);
        top += 20;
        drawWoodSet(graphics, 10, top);
        top += 2 * SIZE * 4 + 30;

        label(graphics, "items in a slot at 1x, 2x and 3x (inventory grey)", 10, top);
        top += 20;
        for (int i = 0; i < SLOTS.length; i++) {
            int left = 10 + (i % 9) * 128;
            BufferedImage sprite = render(SLOTS[i]);
            int x = left;
            for (int scale = 1; scale <= 3; scale++) {
                int size = SIZE * scale;
                graphics.setColor(new Color(0xC6C6C6));
                graphics.fillRect(x, top, size + 4, size + 4);
                graphics.setColor(new Color(0x8B8B8B));
                graphics.fillRect(x + 2, top + 2, size, size);
                drawOn(graphics, sprite, x + 2, top + 2, scale, -1);
                x += size + 8;
            }
        }
        top += 64 + 20;

        label(graphics, "particles at 8x over the inner band sky and over black", 10, top);
        top += 20;
        int p = 0;
        for (String name : TEXTURES.keySet()) {
            if (!name.startsWith("particle/")) {
                continue;
            }
            BufferedImage sprite = render(name);
            drawOn(graphics, sprite, 10 + p * 140, top, 8, 0x4A4652);
            drawOn(graphics, sprite, 10 + p * 140 + 68, top, 8, 0x000000);
            p++;
        }
        top += 64 + 20;
        return top;
    }

    /** A 6×6 wall at 2×; a missing image (no vanilla jar) shows a dark cross. */
    private static void drawWall(Graphics2D graphics, BufferedImage tile, int left, int top) {
        int block = SIZE * 2;
        if (tile == null) {
            graphics.setColor(new Color(0x601010));
            graphics.drawLine(left, top, left + 6 * block - 1, top + 6 * block - 1);
            graphics.drawLine(left + 6 * block - 1, top, left, top + 6 * block - 1);
            return;
        }
        for (int ty = 0; ty < 6; ty++) {
            for (int tx = 0; tx < 6; tx++) {
                drawOn(graphics, tile, left + tx * block, top + ty * block, 2, 0x4A4652);
            }
        }
    }

    /**
     * A scene cell: {@code name}, or {@code overlay@host} (an ore overlay over its host, as the two-layer model shows it).
     * At {@code night} the block is darkened as by a dark sky and its emissive parts are drawn at full brightness: the
     * glow layer ({@code <host>_glow}) and the whole ore overlay.
     */
    private static BufferedImage sceneCell(String name, boolean night) throws IOException {
        String[] parts = name.split("@");
        String host = parts.length == 2 ? parts[1] : parts[0];
        BufferedImage base = render(host);
        BufferedImage cell = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        BufferedImage glow = TEXTURES.containsKey(host + "_glow") ? render(host + "_glow") : null;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (!isOpaque(base, x, y)) {
                    continue;
                }
                boolean glowing = glow != null && isOpaque(glow, x, y);
                cell.setRGB(x, y, night && !glowing ? 0xFF000000 | mix(base.getRGB(x, y) & 0xFFFFFF, 0x000000, 0.8) : base.getRGB(x, y));
            }
        }
        if (parts.length == 2) {
            BufferedImage overlay = render(parts[0]);
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    if (isOpaque(overlay, x, y)) {
                        cell.setRGB(x, y, overlay.getRGB(x, y));
                    }
                }
            }
        }
        return cell;
    }

    /** Draws a grid of scene cells at 4×. */
    private static void drawScene(Graphics2D graphics, String[][] scene, int left0, int top, int backdrop, boolean night) throws IOException {
        for (int ty = 0; ty < scene.length; ty++) {
            for (int tx = 0; tx < scene[ty].length; tx++) {
                int left = left0 + tx * SIZE * 4;
                int rowTop = top + ty * SIZE * 4;
                graphics.setColor(new Color(backdrop));
                graphics.fillRect(left, rowTop, SIZE * 4, SIZE * 4);
                if (!scene[ty][tx].isEmpty()) {
                    drawOn(graphics, sceneCell(scene[ty][tx], night), left, rowTop, 4, -1);
                }
            }
        }
    }

    /**
     * The wood set at 4× over the inner band sky: a door (top over bottom), a trapdoor, a fence (preview stand-in: two
     * posts and two rails cut from the planks' pixels), the sign (its board front and post front from the entity
     * texture) and the hanging sign (bar, chains and board fronts), all as their models place those UV regions.
     */
    private static void drawWoodSet(Graphics2D graphics, int left, int top) throws IOException {
        int s = 4;
        int cell = SIZE * s;
        int sky = 0x4A4652;
        graphics.setColor(new Color(sky));
        graphics.fillRect(left, top, 12 * cell, 2 * cell);
        drawOn(graphics, render("block/corrupted_door_top"), left, top, s, -1);
        drawOn(graphics, render("block/corrupted_door_bottom"), left, top + cell, s, -1);
        drawOn(graphics, render("block/corrupted_trapdoor"), left + cell + 16, top + cell, s, -1);
        BufferedImage planks = render("block/corrupted_planks");
        BufferedImage fence = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                boolean post = (x >= 6 && x < 10);
                boolean rail = (y >= 3 && y < 6) || (y >= 9 && y < 12);
                if (post || rail) {
                    fence.setRGB(x, y, planks.getRGB(x, y));
                }
            }
        }
        for (int i = 0; i < 3; i++) {
            drawOn(graphics, fence, left + 2 * cell + 32 + i * cell, top + cell, s, -1);
        }
        BufferedImage sign = render(SIGN_ENTITY);
        int signLeft = left + 6 * cell;
        drawRegion(graphics, sign, 2, 2, 24, 12, signLeft, top + 16, s);
        drawRegion(graphics, sign, 2, 16, 2, 14, signLeft + 11 * s, top + 16 + 12 * s, s);
        BufferedImage hanging = render(HANGING_SIGN_ENTITY);
        int hangLeft = left + 9 * cell;
        drawRegion(graphics, hanging, 4, 4, 16, 2, hangLeft, top + 8, s);
        drawRegion(graphics, hanging, 0, 6, 3, 6, hangLeft + 2 * s, top + 8 + 2 * s, s);
        drawRegion(graphics, hanging, 0, 6, 3, 6, hangLeft + 11 * s, top + 8 + 2 * s, s);
        drawRegion(graphics, hanging, 2, 14, 14, 10, hangLeft + s, top + 8 + 8 * s, s);
    }

    /** Draws the region (u, v, w, h) of a texture at {@code scale}. */
    private static void drawRegion(Graphics2D graphics, BufferedImage image, int u, int v, int w, int h, int left, int top, int scale) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = image.getRGB(u + x, v + y);
                if ((argb >>> 24) == 0) {
                    continue;
                }
                graphics.setColor(new Color(argb & 0xFFFFFF));
                graphics.fillRect(left + x * scale, top + y * scale, scale, scale);
            }
        }
    }

    private static void label(Graphics2D graphics, String text, int left, int top) {
        graphics.setColor(Color.WHITE);
        graphics.drawString(text, left, top + 12);
    }

    /** Draws opaque pixels only; transparent pixels show {@code backdrop} (or what is already there when it is -1). */
    private static void drawOn(Graphics2D graphics, BufferedImage image, int left, int top, int scale, int backdrop) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                if ((argb >>> 24) != 0) {
                    graphics.setColor(new Color(argb & 0xFFFFFF));
                } else if (backdrop >= 0) {
                    graphics.setColor(new Color(backdrop));
                } else {
                    continue;
                }
                graphics.fillRect(left + x * scale, top + y * scale, scale, scale);
            }
        }
    }

    private Ring5Textures() {
    }
}
