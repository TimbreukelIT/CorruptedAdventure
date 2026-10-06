import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
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
    private static final int[] LOOSE_SAND = {0xC9B98A, 0xD6C799, 0xDFD1A5, 0xE8DCB2};
    private static final int[] CRACKED_CLAY = {0x7C6A58, 0x92806A, 0xA8957C, 0xBCA98F, 0xD0BFA6};
    private static final int CLAY_CRACK = 0x4E4236;
    // Own plants (change 37, decision 10 A): accents vanilla's plants do not have, readable at ~10 blocks.
    private static final int[] DRY_SHRUB = {0x4E4436, 0x5F5648, 0x7B7060, 0x9A8F7C};
    private static final int DRY_SHRUB_TIP = 0xD2C7AE;
    private static final int[] BARREL_CACTUS = {0x264A3C, 0x356052, 0x4A7A68, 0x66987F};
    private static final int CACTUS_SPINE = 0xE6D9A8;
    private static final int[] CACTUS_BUD = {0xD94F7A, 0xF28DB0};
    private static final int CACTUS_BUD_CORE = 0xF4D35E;
    private static final int[] DESERT_FLOWER_PETAL = {0xC4452A, 0xE8703F, 0xF5A05E};
    private static final int DESERT_FLOWER_TIP = 0xFFE9B8;
    private static final int[] DESERT_FLOWER_CORE = {0x1B7F76, 0x2FB5A8};
    private static final int DESERT_FLOWER_CORE_GLINT = 0x8FE6D6;
    private static final int[] DESERT_FLOWER_STEM = {0x5B6B45, 0x7E9160};
    private static final int CANYON_MORTAR_WARM = 0x6B3A22;
    private static final int GREY = 0x8C7F74;
    private static final int[] SALT_BLOCK = {0xB8AE9A, 0xC6BEAB, 0xD6CFBF, 0xE2DCCF};
    private static final int[] PALM_BARK = {0x463B30, 0x5C4F40, 0x716250, 0x877762, 0x9C8C75};
    private static final int[] PALM_WOOD = {0x9A7F52, 0xB49862, 0xC9AE76, 0xDAC28A, 0xE8D3A0};
    private static final int[] PALM_LEAVES = {0x28561C, 0x377425, 0x4A9030, 0x62AA3E};
    // Ore working palettes (change 24, placeholders; final in change 37): outline → highlight.
    // Orange-red "sun" (37b, feedback 2026-10-06): further from vanilla's raw gold than the yellow working palette.
    private static final int[] SUNSTONE = {0x5A1A06, 0x9A300C, 0xD95A16, 0xF2862A, 0xFFB85A, 0xFFEBC0};
    private static final int[] TURQUOISE = {0x0E3B3A, 0x1C6B66, 0x2E9C92, 0x4FC6B5, 0x8FE6D6};
    private static final int TURQUOISE_VEIN = 0x3A2A1E;
    // Salt working palettes (change 29, placeholders; final in change 37): dark to light.
    private static final int[] SALT = {0xB8AE9A, 0xCFC7B6, 0xE2DCCF, 0xEFEBE2, 0xF7F5EF};
    private static final int SALT_RIDGE = 0xFFFFFF;
    private static final int SALT_RIDGE_SHADOW = 0xC9C0AE;
    private static final int SALT_OUTLINE = 0x8A8070;
    private static final int SALT_OUTLINE_DARK = 0x6E6656;
    private static final int[] SALT_CRYSTAL = {0xBFCFD8, 0xDCE6EB, 0xF1F5F7, 0xFFFFFF};
    private static final int SALT_CRYSTAL_BLUSH = 0xEBD9D6;
    private static final int SALT_CRYSTAL_OUTLINE_LIGHT = 0xA9B6C0;
    private static final int SALT_CRYSTAL_OUTLINE = 0x8C9AA6;
    // Nether ring 2 working palettes (change 33, placeholders; final in change 37): dark to light.
    // On vanilla netherrack's tone ladder (37b-1 A), a touch darker: vanilla's nether ores carry netherrack pixels, so
    // the rock they sit in must share its hue and tones; "soaked" comes from wet gloss, dark pores and damp specks.
    private static final int[] SOAKED_NETHERRACK = {0x3C1314, 0x4A191A, 0x59201F, 0x682828, 0x773231, 0x863E3C};
    private static final int SOAKED_PORE = 0x2A0E10;
    private static final int SOAKED_DAMP = 0x3A4634;
    private static final int SOAKED_GLOSS = 0x9A5650;
    /** Crevice pixels of {@link #soakedNetherrack}: {@code SOAKED_PORE_ODDS} in {@code SOAKED_CREVICE_ROLLS} become a pore. */
    private static final int SOAKED_CREVICE_ROLLS = 18;
    private static final int SOAKED_PORE_ODDS = 3;
    /** Damp specks per tile, each on its own row, column and diagonal (wrapping) so they never line up as a stripe. */
    private static final int SOAKED_DAMP_SPECKS = 4;
    // Six tones (37b): blue-green in the shadows, warmer in the light.
    private static final int[] SPORE_MOSS = {0x34403A, 0x45533F, 0x586A4E, 0x6E8160, 0x879877, 0xA3B28C};
    private static final int SPORE_DOT = 0xC8D4B0;
    private static final int SPORE_DOT_LIGHT = 0xE2EAC8;
    private static final int[] DRIPSHROOM_BARK = {0x2C2734, 0x3A3444, 0x4B4558, 0x5E5A6E, 0x747286};
    private static final int[] DRIPSHROOM_SLIME = {0x4E7458, 0x6E9A78, 0x9CC4A2};
    // Darker than in 37 (37b-4 A, mean ~0.40): the light planks lit up like birch in the dark Nether.
    private static final int[] DRIPSHROOM_WOOD = {0x48523F, 0x5A654F, 0x6D7962, 0x818E74, 0x97A388};
    private static final int[] DRIPSHROOM_CAP = {0x3A3046, 0x4A3F58, 0x5D506E, 0x72648A, 0x8A7CA4};
    private static final int DRIPSHROOM_CAP_DROP = 0x9FC3A8;
    private static final int[] DRIPSHROOM_LAMP = {0xB8D27A, 0xCFE38F, 0xE3F0AE, 0xF4FAD2};
    // Cell walls and the brightest core of the lamp (37b): contrast like vanilla's shroomlight.
    private static final int[] DRIPSHROOM_LAMP_WALL = {0x6E8A3A, 0x8FA84E};
    private static final int DRIPSHROOM_LAMP_CORE = 0xFAFFE0;
    private static final int[] GLOW_MOLD = {0x5FD6B8, 0x8BEACF, 0xC2F7E6};
    // One step darker and duller than in 37 (37b): the bright lime glowed against the dark halls.
    private static final int[] SLIME_STALACTITE = {0x3E6E3E, 0x528A4E, 0x6AA460, 0x86BC78, 0xA8D896};
    private static final int SLIME_GLOSS = 0xC8ECB4;
    private static final int SLIME_STALACTITE_OUTLINE = 0x284A2C;
    // Dew working palettes (change 34, placeholders; final in change 37): dark to light.
    private static final int[] DEW_SOURCE = {0x173F52, 0x1F5F78, 0x2E86A6, 0x4FAFC8, 0x86D2E0};
    private static final int DEW_SOURCE_GLINT = 0xD6F4FA;
    // Clear, almost colourless drop of water with a pale blue core (decision 2 A); the soul crystal is saturated cyan.
    private static final int[] DEW_CRYSTAL = {0x5E9FB4, 0x82BACB, 0xA6D3E0, 0xD6EEF4, 0xF4FCFF};
    private static final int DEW_CRYSTAL_OUTLINE = 0x1B4A5C;
    private static final int DEW_CRYSTAL_OUTLINE_LIGHT = 0x2E6F86;
    private static final int SUNSTONE_GLOW = 0xFFE0A8;
    private static final int DEW_GLOW = 0xF0FCFF;
    /** The tone the dim half of a pulse leans towards (37b): a dark tint of the material instead of black. */
    private static final int SUNSTONE_DIM = 0x9A300C;
    private static final int DEW_DIM = 0x5E9FB4;

    /**
     * How a texture moves: {@code frames} frames of {@code frametime} ticks, optionally interpolated; {@code glow} is
     * the colour bright frames mix towards and {@code dim} the tone dim frames lean towards (per material). {@link #STILL}
     * for textures without animation.
     */
    private record Animation(int frames, int frametime, boolean interpolate, int glow, int dim, boolean cycling) {
        static final Animation STILL = new Animation(1, 1, false, 0, 0, false);

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

    /** Paints one frame of an opaque, moving texture; {@code phase} runs from 0 (inclusive) to 1 (exclusive) over a cycle. */
    @FunctionalInterface
    private interface CyclingPainter {
        void paint(BufferedImage image, Random random, double phase);
    }

    /** Pulse used by glowing ores and crystals, as in ring 1: 8 frames, frame time 4, interpolated. */
    private static final int PULSE_FRAMES = 8;
    private static final int PULSE_FRAMETIME = 4;

    private static final Map<String, Texture> TEXTURES = new LinkedHashMap<>();

    /** Preview only (never written): variants the contact sheet compares with the chosen texture. */
    private static final Map<String, Painter> PREVIEW_ONLY = new LinkedHashMap<>();

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
        pulsing("block/sunstone_ore_overlay", SUNSTONE_GLOW, SUNSTONE_DIM, Ring2Textures::sunstoneOreOverlay);
        still("block/turquoise_ore_overlay", Ring2Textures::turquoiseOreOverlay);
        pulsing("item/raw_sunstone", SUNSTONE_GLOW, SUNSTONE_DIM, Ring2Textures::rawSunstone);
        still("item/raw_turquoise", Ring2Textures::rawTurquoise);
        still("block/salt_crust_top", Ring2Textures::saltCrustTop);
        still("block/salt_crust_side", Ring2Textures::saltCrustSide);
        still("block/salt_block", Ring2Textures::saltBlock);
        still("block/polished_salt", Ring2Textures::polishedSalt);
        still("block/salt_crystal", Ring2Textures::saltCrystal);
        still("item/salt", Ring2Textures::saltItem);
        for (char variant : new char[] {'O', 'B', 'C'}) {
            PREVIEW_ONLY.put("variant/cobble_" + variant, (image, random) -> cobblestoneVariant(image, variant));
        }
        // Nether ring 2 (change 33).
        still("block/soaked_netherrack", Ring2Textures::soakedNetherrack);
        still("block/spore_moss", Ring2Textures::sporeMoss);
        still("block/spore_moss_side", Ring2Textures::sporeMossSide);
        cycling("block/dripshroom_stem", 16, 8, Ring2Textures::dripshroomStem);
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
        // Nether ring 2 dew (change 34).
        cycling("block/dew_source", 16, 4, Ring2Textures::dewSource);
        pulsing("block/small_dew_crystal_bud", DEW_GLOW, DEW_DIM, (image, random) -> dewBud(image, random, 1));
        pulsing("block/medium_dew_crystal_bud", DEW_GLOW, DEW_DIM, (image, random) -> dewBud(image, random, 2));
        pulsing("block/large_dew_crystal_bud", DEW_GLOW, DEW_DIM, (image, random) -> dewBud(image, random, 3));
        pulsing("block/dew_crystal_cluster", DEW_GLOW, DEW_DIM, (image, random) -> dewBud(image, random, 4));
        pulsing("item/dew_crystal", DEW_GLOW, DEW_DIM, Ring2Textures::dewCrystalItem);
        // Particles (decision 7 A): light grey grains, tinted sand by SandGustParticle.
        for (int i = 0; i < 4; i++) {
            int grain = i;
            still("particle/sand_gust_" + i, (image, random) -> sandGrain(image, grain));
        }
    }

    private static void still(String name, Painter painter) {
        TEXTURES.put(name, new Texture(painter, null, Animation.STILL));
    }

    /** An 8-frame pulse towards {@code glow}, the glow colour of this material, and back towards its {@code dim} tone. */
    private static void pulsing(String name, int glow, int dim, Painter painter) {
        TEXTURES.put(name, new Texture(painter, null, new Animation(PULSE_FRAMES, PULSE_FRAMETIME, true, glow, dim, false)));
    }

    /**
     * A moving, opaque pattern (water, slime): {@code frames} frames of {@code frametime} ticks, not interpolated, that
     * loop seamlessly because the painter is periodic in {@code phase}. Never for cutout textures: frames would not
     * share an alpha mask (the script checks that the texture is fully opaque).
     */
    private static void cycling(String name, int frames, int frametime, CyclingPainter painter) {
        TEXTURES.put(name, new Texture(null, painter, new Animation(frames, frametime, false, 0, 0, true)));
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
            BufferedImage output = animation.cycling() ? cyclingStrip(name, animation)
                    : animation.animated() ? pulseStrip(render(name), animation) : render(name);
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
        checkSideRules();
        lint();
        writePreview();
    }

    /** Opaque blocks that are calm on purpose, with the reason (37b flatness lint). */
    private static final Map<String, String> FLAT_OK = Map.of(
            "block/salt_crust_top", "calm crust, the hexagons come from the world (37-9 A)",
            "block/salt_block", "calm, coarser than the crust (37-9 A)",
            "block/polished_salt", "smooth face, like vanilla's smooth stone",
            "block/loose_sand", "sand grain, like vanilla sand",
            "block/red_canyon_stone", "natural grain, unchanged in 37b (P3)",
            "block/barrel_cactus_top", "the model shows only pixels 3-12; the rim is never seen",
            "block/glow_mold", "a decal of a few glowing specks",
            "block/palm_leaves", "fixed green leaves, unchanged in 37b (P3)");

    private static final List<String> LINT_WARNINGS = new ArrayList<>();

    /**
     * Warns (does not fail) about textures that read flat or half drawn: an opaque block with fewer than 5 colours or one
     * colour on more than half the pixels (vanilla: 6-9 colours, the commonest on 20-37 %), a sprite with fewer than 5
     * colours, an item whose outline is a single colour all round (vanilla shades it: lighter top left, darkest bottom
     * right). Exceptions with their reason in {@link #FLAT_OK}.
     */
    private static void lint() throws IOException {
        for (String name : TEXTURES.keySet()) {
            if (name.startsWith("particle/") || FLAT_OK.containsKey(name)) {
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
        int size = size(name);
        if (HAND_EDITED.contains(name)) {
            BufferedImage file = ImageIO.read(OUTPUT.resolve(name + ".png").toFile());
            return file.getSubimage(0, 0, size, size);
        }
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
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

    private static BufferedImage rendered(Map<String, Painter> painters, String name) {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        painters.get(name).paint(image, seeded(name));
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
                if (animation.cycling() && !opaque) {
                    throw new IllegalStateException(name + ": a cycling texture must be opaque (transparent at " + x + "," + y + ")");
                }
                if (opaque != ((image.getRGB(x, y % width) >>> 24) != 0)) {
                    throw new IllegalStateException(name + ": frame " + y / width + " has another alpha mask at " + x + "," + y % width);
                }
            }
        }
    }

    /**
     * Side textures that show their ground (a block under a crust or moss rim) must carry exactly the pixels of that
     * ground below the rim: {side, ground, first row that is ground}.
     */
    private static final String[][] SIDE_RULES = {
            {"block/salt_crust_side", "block/salt_block", "3"},
            {"block/spore_moss_side", "block/soaked_netherrack", "6"}};

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

    /**
     * Broken canyon stone (decision 8 A, redrawn in 37b): one dominant orange tint with two smaller guest stones that lean
     * red or cream (mixed half-way with the orange, so they read as chance, not as a repeating chip) and a warm 1-px
     * mortar. Every stone is rounded: lit towards the top left with a highlight, shadowed along its lower right edge.
     */
    private static void canyonCobblestone(BufferedImage image, Random random) {
        Cells cells = Cells.random(random, 8);
        int[] red = new int[ORANGE_CANYON_STONE.length];
        int[] cream = new int[ORANGE_CANYON_STONE.length];
        for (int i = 0; i < red.length; i++) {
            red[i] = mix(ORANGE_CANYON_STONE[i], RED_CANYON_STONE[i], 0.5);
            cream[i] = mix(ORANGE_CANYON_STONE[i], CREAM_CANYON_STONE[i], 0.5);
        }
        double[] base = new double[cells.count()];
        for (int i = 0; i < base.length; i++) {
            base[i] = 0.35 + 0.3 * random.nextDouble();
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                Cells.Nearest nearest = cells.nearest(x, y);
                double gap = nearest.secondDistance() - nearest.distance();
                if (gap < 0.45) {
                    set(image, x, y, CANYON_MORTAR_WARM);
                    continue;
                }
                int[] palette = nearest.index() == 0 ? red : nearest.index() == 1 ? cream : ORANGE_CANYON_STONE;
                double slope = nearest.dx() + nearest.dy();
                double light = base[nearest.index()] - 0.45 * slope + 0.1 * random.nextDouble();
                if (gap < 1.2 && slope > 0.1) {
                    light -= 0.2;
                }
                set(image, x, y, shade(palette, light));
            }
        }
    }

    /**
     * Preview only: the first placeholder 'O' (all three tints per stone, black-red mortar: the one that "feels
     * strange"), 'B' (all tints 40 % towards grey, dark mortar) and 'C' (all tints, muted, light mortar), to compare
     * with the choice.
     */
    private static void cobblestoneVariant(BufferedImage image, char variant) {
        Random random = seeded("block/canyon_cobblestone");
        int[][] palettes = {RED_CANYON_STONE, ORANGE_CANYON_STONE, CREAM_CANYON_STONE};
        double muting = variant == 'B' ? 0.4 : variant == 'C' ? 0.2 : 0.0;
        int mortar = variant == 'B' ? 0x3A2C26 : variant == 'C' ? 0x9A6A50 : CANYON_MORTAR;
        Cells cells = Cells.random(random, 7);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                Cells.Nearest nearest = cells.nearest(x, y);
                if (nearest.secondDistance() - nearest.distance() < 0.7) {
                    set(image, x, y, mortar);
                    continue;
                }
                double light = 0.55 - 0.12 * (nearest.dx() + nearest.dy()) + 0.2 * random.nextDouble();
                set(image, x, y, mix(shade(palettes[nearest.index() % 3], light), GREY, muting));
            }
        }
    }

    /**
     * Red canyon stone in vanilla's stone brick layout (37b-5 A): two rows of 16×8 bricks offset by half a brick, a warm
     * mortar, each brick bevelled (lit top row and left column, shadowed bottom row and right column) over horizontally
     * stretched canyon-stone grain.
     */
    private static void canyonStoneBricks(BufferedImage image, Random random) {
        double[][] grain = valueNoise(random, 4, 2);
        for (int row = 0; row < 2; row++) {
            int offset = row == 0 ? 0 : 8;
            // One 16-pixel brick per row (it wraps across the tile edge in the offset row): one tone per row.
            double brickShade = 0.4 + 0.2 * random.nextDouble();
            for (int dy = 0; dy < 8; dy++) {
                int y = row * 8 + dy;
                for (int x = 0; x < SIZE; x++) {
                    int inBrick = Math.floorMod(x - offset, SIZE);
                    double light = brickShade + 0.35 * (grain[x][y] - 0.5) + 0.1 * random.nextDouble();
                    if (dy == 7 || inBrick == 15) {
                        set(image, x, y, CANYON_MORTAR_WARM);
                        continue;
                    }
                    if (dy == 0 || inBrick == 0) {
                        light += 0.3;
                    } else if (dy == 6 || inBrick == 14) {
                        light -= 0.3;
                    }
                    set(image, x, y, shade(RED_CANYON_STONE, light));
                }
            }
        }
    }

    // ---- Scorched sandstone ---------------------------------------------------------------------------------

    /** Grainy top (also the smooth variant and the ends of cut/chiseled) with soft soot smudges. */
    private static void scorchedSandstoneTop(BufferedImage image, Random random) {
        double[][] noise = valueNoise(random, 4, 4);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, shade(SCORCHED_SANDSTONE, 0.2 + 0.55 * noise[x][y] + 0.3 * random.nextDouble()));
            }
        }
        soot(image, random, 2, 0);
    }

    /**
     * Like vanilla sandstone's side (37b): a lighter band on top with a dark line under it, a body of short grainy layers
     * (not long streaks, which read as planks), a darker foot; soot smudges rising from below.
     */
    private static void scorchedSandstoneSide(BufferedImage image, Random random) {
        double[][] layers = valueNoise(random, 4, 2);
        for (int y = 0; y < SIZE; y++) {
            double band = y < 3 ? 0.3 : y >= 13 ? -0.2 : 0.0;
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, shade(SCORCHED_SANDSTONE, 0.3 + band + 0.35 * layers[x][y] + 0.25 * random.nextDouble()));
            }
        }
        for (int x = 0; x < SIZE; x++) {
            set(image, x, 3, SCORCHED_SANDSTONE[random.nextInt(4) == 0 ? 2 : 1]);
        }
        soot(image, random, 3, 9);
    }

    /** The underside: darker grain with more soot smudges. */
    private static void scorchedSandstoneBottom(BufferedImage image, Random random) {
        double[][] noise = valueNoise(random, 4, 4);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, shade(SCORCHED_SANDSTONE, 0.15 + 0.45 * noise[x][y] + 0.3 * random.nextDouble()));
            }
        }
        soot(image, random, 4, 0);
    }

    /** Cut: a 1-pixel frame (lit top left, dark bottom right) around a lightly grained face with one inner groove. */
    private static void cutScorchedSandstone(BufferedImage image, Random random) {
        double[][] noise = valueNoise(random, 4, 2);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, shade(SCORCHED_SANDSTONE, 0.3 + 0.45 * noise[x][y] + 0.3 * random.nextDouble()));
            }
        }
        for (int x = 1; x < SIZE - 1; x++) {
            set(image, x, 7, SCORCHED_SANDSTONE[1]);
            set(image, x, 8, SCORCHED_SANDSTONE[4]);
        }
        rim(image, random, SCORCHED_SANDSTONE, 0.0, 0.35);
        for (int i = 1; i < SIZE - 1; i++) {
            set(image, i, 0, SCORCHED_SANDSTONE[3]);
            set(image, 0, i, SCORCHED_SANDSTONE[3]);
        }
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
        set(image, 7, 7, SCORCHED_SANDSTONE[0]);
        rim(image, random, SCORCHED_SANDSTONE, 0.0, 0.35);
    }

    /**
     * {@code count} soft soot smudges at or below row {@code minY} (37b): pixels within a ragged radius drop one tone on
     * the palette, more often towards the middle (dithered falloff), so the soot shades the stone instead of sitting on
     * it as black dots.
     */
    private static void soot(BufferedImage image, Random random, int count, int minY) {
        for (int i = 0; i < count; i++) {
            int cx = random.nextInt(SIZE);
            int cy = minY + random.nextInt(SIZE - minY);
            double radius = 2.5 + random.nextDouble();
            for (int dy = -4; dy <= 4; dy++) {
                for (int dx = -4; dx <= 4; dx++) {
                    double falloff = 1.0 - Math.hypot(dx, dy * 1.4) / radius;
                    if (falloff <= 0 || random.nextDouble() > falloff + 0.2) {
                        continue;
                    }
                    int x = Math.floorMod(cx + dx, SIZE);
                    int y = cy + dy;
                    if (y < minY || y >= SIZE) {
                        // No vertical wrap: soot from the foot must not leak into the lit band at the top.
                        continue;
                    }
                    int tone = paletteIndex(SCORCHED_SANDSTONE, image.getRGB(x, y) & 0xFFFFFF);
                    set(image, x, y, SCORCHED_SANDSTONE[Math.max(0, tone - 1)]);
                }
            }
        }
    }

    /** Index of {@code rgb} on {@code palette}, or the middle when it is not on it. */
    private static int paletteIndex(int[] palette, int rgb) {
        for (int i = 0; i < palette.length; i++) {
            if (palette[i] == rgb) {
                return i;
            }
        }
        return palette.length / 2;
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

    /**
     * Dried mud (37b): flat polygonal plates (wrapping Voronoi cells) parted by 1-px dark cracks, with a thin lit lip
     * where a plate curls up along its upper edge and short hairline cracks inside the larger plates, so it reads as
     * cracked clay and not as paving stones.
     */
    private static void crackedClay(BufferedImage image, Random random) {
        Cells cells = Cells.random(random, 7);
        double[] plate = new double[cells.count()];
        for (int i = 0; i < plate.length; i++) {
            plate[i] = 0.3 + 0.35 * random.nextDouble();
        }
        double[][] grain = valueNoise(random, 4, 4);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                Cells.Nearest nearest = cells.nearest(x, y);
                double gap = nearest.secondDistance() - nearest.distance();
                if (gap < 0.5) {
                    set(image, x, y, CLAY_CRACK);
                    continue;
                }
                double light = plate[nearest.index()] + 0.25 * (grain[x][y] - 0.5) + 0.1 * random.nextDouble();
                if (gap < 1.2 && nearest.dy() < -0.15) {
                    light += 0.2;
                }
                set(image, x, y, shade(CRACKED_CLAY, light));
            }
        }
        // Hairline cracks only well inside a plate (never across a crack or a lip).
        for (int i = 0; i < 4; i++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            int dx = random.nextBoolean() ? 1 : -1;
            for (int j = 0; j < 3; j++) {
                int px = Math.floorMod(x + j * dx, SIZE);
                int py = Math.floorMod(y + (j == 2 ? 1 : 0), SIZE);
                Cells.Nearest nearest = cells.nearest(px, py);
                if (nearest.secondDistance() - nearest.distance() > 1.5) {
                    set(image, px, py, CRACKED_CLAY[0]);
                }
            }
        }
    }

    // ---- Flora (cross models and the cactus) ----------------------------------------------------------------

    /** Bare, forked twigs from a dark two-pixel root at the bottom (37b); transparent background (cutout). */
    private static void dryShrub(BufferedImage image, Random random) {
        twig(image, random, 8, 15, 0, 12, 0);
        twig(image, random, 7, 15, -1, 11, 0);
        twig(image, random, 8, 15, 1, 11, 0);
        twig(image, random, 7, 14, -1, 6, 1);
        twig(image, random, 9, 14, 1, 6, 1);
        for (int y = 13; y < SIZE; y++) {
            set(image, 7, y, DRY_SHRUB[0]);
            set(image, 8, y, DRY_SHRUB[y == 13 ? 1 : 0]);
        }
    }

    /** A twig growing up from (x, y), leaning {@code lean} per few rows, forking once or twice; its tip is pale. */
    private static void twig(BufferedImage image, Random random, int x, int y, int lean, int length, int depth) {
        for (int i = 0; i < length && y >= 1 && x >= 0 && x < SIZE; i++) {
            boolean tip = i == length - 1 || y == 1;
            set(image, x, y, tip ? DRY_SHRUB_TIP : DRY_SHRUB[i < 2 ? 0 : i < 4 ? 1 : 2 + random.nextInt(2)]);
            y--;
            if (lean != 0 && random.nextInt(2) == 0) {
                x += lean;
            }
            if (depth < 2 && i > 2 && random.nextInt(3) == 0) {
                twig(image, random, x, y, lean == 0 ? (random.nextBoolean() ? 1 : -1) : -lean, length - i - 1, depth + 1);
            }
        }
    }

    /**
     * A sage-green stem with two leaves and a six-pointed star flower (coral with cream tips, a turquoise heart): a
     * silhouette and colours none of vanilla's flowers have (10 A). Since 37b the petals are filled: two pixels wide at
     * the base with a shadowed side, so the star reads as a flower and not as a spark. Transparent background (cutout).
     */
    private static void desertFlower(BufferedImage image, Random random) {
        for (int y = 9; y < SIZE; y++) {
            set(image, 7 + (y > 12 ? 1 : 0), y, DESERT_FLOWER_STEM[y % 3 == 0 ? 1 : 0]);
        }
        set(image, 6, 13, DESERT_FLOWER_STEM[1]);
        set(image, 5, 12, DESERT_FLOWER_STEM[1]);
        set(image, 4, 12, DESERT_FLOWER_STEM[0]);
        set(image, 5, 13, DESERT_FLOWER_STEM[0]);
        set(image, 9, 12, DESERT_FLOWER_STEM[1]);
        set(image, 10, 11, DESERT_FLOWER_STEM[1]);
        set(image, 11, 11, DESERT_FLOWER_STEM[0]);
        set(image, 10, 12, DESERT_FLOWER_STEM[0]);
        for (int arm = 0; arm < 6; arm++) {
            double angle = Math.toRadians(-90 + 60 * arm);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            for (int r = 1; r <= 4; r++) {
                int x = (int) Math.round(7.5 + cos * r);
                int y = (int) Math.round(5.5 + sin * r);
                if (r <= 2) {
                    // The petal's second pixel on its shadow side (towards the bottom right).
                    int side = (int) Math.round(7.5 + cos * r - sin * 0.9);
                    int sideY = (int) Math.round(5.5 + sin * r + cos * 0.9);
                    set(image, side, sideY, DESERT_FLOWER_PETAL[0]);
                }
                set(image, x, y, r == 4 ? DESERT_FLOWER_TIP : DESERT_FLOWER_PETAL[r == 1 ? 1 : 2]);
            }
        }
        // A 2×2 turquoise heart lit from the top left: one glint pixel, one light, two dark.
        set(image, 7, 5, DESERT_FLOWER_CORE_GLINT);
        set(image, 8, 5, DESERT_FLOWER_CORE[1]);
        set(image, 7, 6, DESERT_FLOWER_CORE[1]);
        set(image, 8, 6, DESERT_FLOWER_CORE[0]);
    }

    /**
     * Seen from above (the model uses pixels 3-12): ribs as spokes, a crown of pale spines on the rim and a rose-pink
     * flower bud with a yellow heart in the middle.
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
                if (rib && radius > 3.6 && radius < 6.2 && random.nextInt(2) == 0) {
                    set(image, x, y, CACTUS_SPINE);
                }
            }
        }
        for (int y = 6; y <= 9; y++) {
            for (int x = 6; x <= 9; x++) {
                boolean corner = (x == 6 || x == 9) && (y == 6 || y == 9);
                if (!corner) {
                    set(image, x, y, x >= 7 && x <= 8 && y >= 7 && y <= 8 ? CACTUS_BUD_CORE : CACTUS_BUD[(x + y) % 2]);
                }
            }
        }
    }

    /** Vertical ribs (lit ridge, shadowed groove) with V-shaped spine tufts along the ridges; the model shows rows 6-15. */
    private static void barrelCactusSide(BufferedImage image, Random random) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int phase = Math.floorMod(x - 3, 3);
                double light = phase == 0 ? 0.85 : phase == 1 ? 0.5 : 0.15;
                set(image, x, y, shade(BARREL_CACTUS, light + 0.15 * (random.nextDouble() - 0.5)));
            }
        }
        for (int x = 3; x < 13; x += 3) {
            for (int y = 8; y < SIZE; y += 3 + random.nextInt(2)) {
                set(image, x, y, CACTUS_SPINE);
                set(image, x - 1, y - 1, CACTUS_SPINE);
                set(image, x + 1, y - 1, CACTUS_SPINE);
            }
        }
    }

    // ---- Palm -----------------------------------------------------------------------------------------------

    /**
     * Palm trunk (37b): grey-brown bark in horizontal rings 3–5 pixels high (the scars of old fronds), each ring a lit
     * upper row, a body and a dark seam, with V-shaped scale notches that alternate from ring to ring, so the trunk reads
     * as a crosshatch of scales instead of a barcode; no vertical grain.
     */
    private static void palmLog(BufferedImage image, Random random) {
        int[] heights = {5, 3, 4, 4};
        for (int i = heights.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int swap = heights[i];
            heights[i] = heights[j];
            heights[j] = swap;
        }
        int y0 = 0;
        for (int ring = 0; ring < heights.length; ring++) {
            int seam = y0 + heights[ring] - 1;
            double tone = 0.35 + 0.3 * random.nextDouble();
            int notchOffset = ring % 2 == 0 ? 0 : 2;
            for (int y = y0; y <= seam; y++) {
                for (int x = 0; x < SIZE; x++) {
                    if (y == seam) {
                        set(image, x, y, PALM_BARK[random.nextInt(3) == 0 ? 1 : 0]);
                    } else if (y == y0) {
                        set(image, x, y, shade(PALM_BARK, tone + 0.35 + 0.1 * random.nextDouble()));
                    } else {
                        set(image, x, y, shade(PALM_BARK, tone + 0.2 * (random.nextDouble() - 0.5)));
                    }
                }
            }
            // V notches hanging from the seam above: a dark tip on the seam row, two shadowed flanks one row up.
            for (int x = notchOffset; x < SIZE; x += 4) {
                set(image, x, seam, PALM_BARK[0]);
                if (seam - 1 > y0) {
                    setWrapped(image, x - 1, seam - 1, PALM_BARK[1]);
                    setWrapped(image, x + 1, seam - 1, PALM_BARK[1]);
                }
            }
            y0 = seam + 1;
        }
    }

    /**
     * End grain without year rings (palm wood is fibrous, 37b): sand-yellow fading darker towards a dark heart, dense
     * dark fibre dots, a 1-pixel bark rim and a ring of dark fibres just inside it.
     */
    private static void palmLogTop(BufferedImage image, Random random) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double radius = Math.hypot(x - 7.5, y - 7.5);
                double light = 0.85 - radius / 14.0 - (radius < 2.5 ? 0.4 : 0.0) + 0.2 * random.nextDouble();
                int roll = random.nextInt(5);
                set(image, x, y, roll == 0 ? PALM_WOOD[0] : roll == 1 ? PALM_WOOD[1] : shade(PALM_WOOD, light));
            }
        }
        for (int i = 1; i < SIZE - 1; i++) {
            for (int[] p : new int[][] {{i, 1}, {i, SIZE - 2}, {1, i}, {SIZE - 2, i}}) {
                if (random.nextBoolean()) {
                    set(image, p[0], p[1], PALM_WOOD[0]);
                }
            }
        }
        rim(image, random, PALM_BARK, 0.0, 0.6);
    }

    /**
     * Stripped wood (37b): lengthwise fibre streaks in five tones (stretched noise), a few long dark grain lines and pale
     * glints, lit a little more on the left of each streak.
     */
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

    /**
     * Upper door half (37b): bevelled boards in a frame lit on its top and left, two window holes sunk into the door
     * (shadowed top and left inside, a lit sill below and right), one rail, two hinge pixels.
     */
    private static void doorTop(BufferedImage image, Random random, int[] wood, int[] bark) {
        boards(image, random, wood);
        frame(image, random, bark, true, false);
        rail(image, random, bark, 11);
        window(image, wood, bark, 3, 3, 3, 5);
        window(image, wood, bark, 10, 3, 3, 5);
        hinge(image, bark, 2);
    }

    /** Lower door half (37b): boards, two rails with a diagonal brace between them, a hinge and the handle. */
    private static void doorBottom(BufferedImage image, Random random, int[] wood, int[] bark) {
        boards(image, random, wood);
        frame(image, random, bark, false, true);
        rail(image, random, bark, 2);
        rail(image, random, bark, 12);
        // Brace from the lower left to the upper right: lit upper edge, shadowed lower edge.
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

    /**
     * Trapdoor as a lattice (37b): slats lit along their top row with see-through gaps between them, a centre brace lit
     * on its left, a frame lit on its top and left, nails where the frame meets the slats.
     */
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

    /**
     * The door in the inventory (8 wide, columns 4–11; 37b): a frame lit on the left and top and dark on the right and
     * bottom, bevelled boards, two window holes with a lit sill, two rails and the handle.
     */
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

    /**
     * A see-through hole sunk into the door (cutout: alpha 0 or 255): the rim above and left of it lies in shadow, the
     * sill below and right of it catches the light.
     */
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
        for (int i = 0; i < 20; i++) {
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

    /**
     * A small palm (37b): a thin curved trunk (two-pixel foot, shadowed on the right) and four arching fronds with a lit
     * midrib and a shadow row below; transparent background (cutout).
     */
    private static void palmSapling(BufferedImage image, Random random) {
        for (int y = 15; y >= 8; y--) {
            int x = y > 12 ? 8 : 7;
            set(image, x, y, PALM_BARK[y % 2 == 0 ? 3 : 2]);
            if (y > 13) {
                set(image, x + 1, y, PALM_BARK[1]);
            }
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
                set(image, x, y, PALM_LEAVES[j == length - 1 ? 1 : 3]);
                if (j > 0 && j < length - 1) {
                    set(image, x, y + 1, PALM_LEAVES[j % 2 == 0 ? 0 : 1]);
                }
            }
        }
        set(image, 7, 7, PALM_LEAVES[3]);
        set(image, 7, 6, PALM_LEAVES[2]);
        set(image, 7, 5, PALM_LEAVES[1]);
    }

    // ---- Ores (sunstone, turquoise) -------------------------------------------------------------------------

    /**
     * Sunstone overlay, shared by all host rocks (37b): one larger and three small angular chunks of 2–6 pixels,
     * irregular (a diamond with random stretch and one corner cut off), split into facets lit from the top left with a
     * glint on the brightest corner, and the selective outline (darkest only below and right), so the chunks read as
     * crystals in the rock rather than stickers.
     */
    private static void sunstoneOreOverlay(BufferedImage image, Random random) {
        double[] sizes = {2.6, 1.6, 1.4, 1.3};
        int placed = 0;
        for (int attempt = 0; attempt < 800 && placed < sizes.length; attempt++) {
            double radius = sizes[placed] + 0.3 * random.nextDouble();
            double cx = 2 + radius + random.nextDouble() * (SIZE - 4 - 2 * radius);
            double cy = 2 + radius + random.nextDouble() * (SIZE - 4 - 2 * radius);
            double stretchX = 0.75 + 0.5 * random.nextDouble();
            double stretchY = 0.75 + 0.5 * random.nextDouble();
            double cutX = random.nextBoolean() ? 1 : -1;
            double cutY = random.nextBoolean() ? 1 : -1;
            BufferedImage chunk = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            int glintX = -1;
            int glintY = -1;
            for (int y = 1; y < SIZE - 1; y++) {
                for (int x = 1; x < SIZE - 1; x++) {
                    double dx = (x + 0.5 - cx) * stretchX;
                    double dy = (y + 0.5 - cy) * stretchY;
                    if (Math.abs(dx) + Math.abs(dy) > radius * 1.2 || cutX * dx + cutY * dy > radius * 0.7) {
                        continue;
                    }
                    double light = -(dx + dy) / radius;
                    int tone = light > 0.5 ? 4 : light > -0.1 ? (dx < dy ? 3 : 4) : light > -0.6 ? 3 : 2;
                    set(chunk, x, y, SUNSTONE[tone]);
                    if (glintX < 0 || x + y < glintX + glintY) {
                        glintX = x;
                        glintY = y;
                    }
                }
            }
            if (glintX < 0) {
                continue;
            }
            set(chunk, glintX, glintY, SUNSTONE[5]);
            outlineSelective(chunk, SUNSTONE[1], SUNSTONE[0]);
            if (merge(image, chunk)) {
                placed++;
            }
        }
    }

    /**
     * Turquoise overlay, shared by all host rocks (37b): three or four small, uneven nodules (the radius wobbles with the
     * angle), matte (no glint), lit from the top left, a brown matrix vein through the larger ones and the selective
     * outline.
     */
    private static void turquoiseOreOverlay(BufferedImage image, Random random) {
        int target = 3 + random.nextInt(2);
        int placed = 0;
        for (int attempt = 0; attempt < 800 && placed < target; attempt++) {
            double radius = (placed == 0 ? 2.2 : 1.4) + 0.5 * random.nextDouble();
            double cx = 2 + radius + random.nextDouble() * (SIZE - 4 - 2 * radius);
            double cy = 2 + radius + random.nextDouble() * (SIZE - 4 - 2 * radius);
            double wobble = random.nextDouble() * 2 * Math.PI;
            BufferedImage nodule = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            for (int y = 1; y < SIZE - 1; y++) {
                for (int x = 1; x < SIZE - 1; x++) {
                    double dx = x + 0.5 - cx;
                    double dy = y + 0.5 - cy;
                    double edge = radius * (1 + 0.2 * Math.sin(3 * Math.atan2(dy, dx) + wobble));
                    if (Math.hypot(dx, dy) > edge) {
                        continue;
                    }
                    double light = -(dx + dy) / (2 * radius);
                    set(nodule, x, y, shade(TURQUOISE, 0.5 + 0.5 * light + 0.15 * random.nextDouble()));
                }
            }
            if (radius > 2) {
                vein(nodule, random, (int) cx, (int) cy);
            }
            outlineSelective(nodule, TURQUOISE[1], TURQUOISE[0]);
            if (merge(image, nodule)) {
                placed++;
            }
        }
    }

    /**
     * Raw sunstone (37b): a cluster of three golden nuggets like vanilla's raw ores, lit from the top left with a pale
     * glint on each, red canyon stone wedged between them, selective outline.
     */
    private static void rawSunstone(BufferedImage image, Random random) {
        sprite(image, new String[] {
                "................",
                "................",
                ".........44.....",
                "........45443...",
                ".......4544321..",
                "...4433rR43321..",
                "..45443r443211..",
                "..4554432r3211..",
                ".44544443321....",
                ".4544433221r44..",
                ".444433321rR543.",
                ".33433221r14321.",
                "..2222111.2211..",
                "...11111...11...",
                "................",
                "................"}, Map.of(
                '1', SUNSTONE[1], '2', SUNSTONE[2], '3', SUNSTONE[3], '4', SUNSTONE[4], '5', SUNSTONE[5],
                'r', RED_CANYON_STONE[1], 'R', RED_CANYON_STONE[3]));
        outlineSelective(image, SUNSTONE[1], SUNSTONE[0]);
    }

    /**
     * Raw turquoise (37b, redrawn after feedback): one rough, angular chunk like vanilla's raw copper and raw iron, not a
     * bunch of balls: a flat lit top face, a mid front face and a shadowed right face meeting at straight edges, brown
     * matrix veins, bits of cream canyon stone stuck to it, matte (no glint, decision 24-5 A); selective outline.
     */
    private static void rawTurquoise(BufferedImage image, Random random) {
        sprite(image, new String[] {
                "................",
                "................",
                "......3332......",
                ".....334322.....",
                "....3343v221....",
                "...33433v22211..",
                "..3343322v2211..",
                "..33322Ck22211..",
                ".2332221kc2111..",
                ".23322v11cC211..",
                ".2222111v12111..",
                "..22111111211...",
                "...1111..111....",
                "................",
                "................",
                "................"}, Map.of(
                '1', TURQUOISE[1], '2', TURQUOISE[2], '3', TURQUOISE[3], '4', TURQUOISE[4], 'v', TURQUOISE_VEIN,
                'k', CREAM_CANYON_STONE[0], 'c', CREAM_CANYON_STONE[2], 'C', CREAM_CANYON_STONE[4]));
        outlineSelective(image, TURQUOISE[1], TURQUOISE[0]);
    }

    /**
     * One thin vein through ({@code throughX}, {@code throughY}), wandering a little, drawn only on opaque pixels that are
     * not on the shape's edge (so the shape and its outline stay intact).
     */
    private static void vein(BufferedImage image, Random random, int throughX, int throughY) {
        boolean[][] inner = new boolean[SIZE][SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                inner[x][y] = isOpaque(image, x, y) && isOpaque(image, x - 1, y) && isOpaque(image, x + 1, y)
                        && isOpaque(image, x, y - 1) && isOpaque(image, x, y + 1);
            }
        }
        boolean horizontal = random.nextBoolean();
        int across = horizontal ? throughY : throughX;
        for (int step = 0; step < SIZE; step++) {
            int x = horizontal ? step : across;
            int y = horizontal ? across : step;
            if (inner[x][y]) {
                set(image, x, y, TURQUOISE_VEIN);
            }
            // Straight up to the crossing point, wandering only after it, so the vein always passes through it.
            int along = horizontal ? throughX : throughY;
            if (step > along && random.nextInt(3) == 0) {
                across = Math.max(1, Math.min(SIZE - 2, across + (random.nextBoolean() ? 1 : -1)));
            }
        }
    }

    // ---- Salt -----------------------------------------------------------------------------------------------

    /**
     * Salt flat crust from above (decision 9 A): calm, no polygons of its own (the hexagons are the {@code salt_block}
     * lines of the world, 30b): fine grain and soft blotches in the three lightest salt tints, tiling without a seam.
     */
    private static void saltCrustTop(BufferedImage image, Random random) {
        int[] crust = {SALT[2], SALT[3], SALT[4]};
        double[][] broad = valueNoise(random, 8, 8);
        double[][] mid = valueNoise(random, 4, 4);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, shade(crust, 0.1 + 0.4 * broad[x][y] + 0.25 * mid[x][y] + 0.3 * random.nextDouble()));
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

    /**
     * Coarser and darker than the crust (decision 9 A), so the block lines the world draws in the crust stand out:
     * clumped grain in the four darker salt tints and a few small crystal faces (lit top-left, shadowed bottom-right).
     */
    private static void saltBlock(BufferedImage image, Random random) {
        double[][] noise = valueNoise(random, 4, 4);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, shade(SALT_BLOCK, 0.1 + 0.5 * noise[x][y] + 0.35 * random.nextDouble()));
            }
        }
        for (int i = 0; i < 3; i++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            setWrapped(image, x, y, SALT[3]);
            setWrapped(image, x + 1, y, SALT[2]);
            setWrapped(image, x, y + 1, SALT[2]);
            setWrapped(image, x + 1, y + 1, SALT_BLOCK[1]);
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
     * there, and the selective outline crystals have (§1.4, 37b-3 A). Transparent background.
     */
    private static void saltCrystal(BufferedImage image, Random random) {
        crystalSpike(image, random, 7.5, 3, 13, 0);
        crystalSpike(image, random, 3.5, 2, 8 + random.nextInt(2), -2);
        crystalSpike(image, random, 11.5, 2, 7 + random.nextInt(2), 2);
        outlineSelective(image, SALT_CRYSTAL_OUTLINE_LIGHT, SALT_CRYSTAL_OUTLINE);
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
                if (row == 0) {
                    color = x <= ridge ? SALT_CRYSTAL[1] : SALT_CRYSTAL[0];
                }
                if (x != ridge && random.nextInt(9) == 0) {
                    color = SALT_CRYSTAL_BLUSH;
                }
                set(image, x, y, color);
            }
        }
    }

    /**
     * A heap of salt (37b): an uneven mound of 2×2 grains (lit corner top left, shadow bottom right) with three small
     * cubic crystals on its flank, since salt grows in cubes; selective outline.
     */
    private static void saltItem(BufferedImage image, Random random) {
        String[] mound = {
                "................",
                "................",
                "................",
                "................",
                ".......##.......",
                "......####......",
                ".....######.....",
                "....########.##.",
                "...############.",
                "..#############.",
                "..#############.",
                ".##############.",
                ".##############.",
                "..#####.#######.",
                "................",
                "................"};
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (mound[y].charAt(x) != '#') {
                    continue;
                }
                // The mound is lit from the top left; each 2×2 grain repeats that light in small.
                double light = 0.95 - 0.045 * (x + y - 8) + 0.15 * (random.nextDouble() - 0.5);
                int corner = (x + (y / 2) % 2) % 2 + 2 * (y % 2);
                light += corner == 0 ? 0.15 : corner == 3 ? -0.15 : 0.0;
                set(image, x, y, shade(SALT, light));
            }
        }
        for (int[] cube : new int[][] {{6, 5}, {10, 8}, {4, 10}}) {
            int x = cube[0];
            int y = cube[1];
            set(image, x, y, SALT_RIDGE);
            set(image, x + 1, y, SALT[4]);
            set(image, x, y + 1, SALT[3]);
            set(image, x + 1, y + 1, SALT[1]);
            set(image, x + 2, y + 1, SALT[0]);
            set(image, x + 1, y + 2, SALT[0]);
        }
        outlineSelective(image, SALT_OUTLINE, SALT_OUTLINE_DARK);
    }

    // ---- Animation ------------------------------------------------------------------------------------------

    /**
     * The animation strip: the frames under each other, each the rest frame leaning towards the material's dim tone
     * (dim) or its glow colour (bright) along a cosine. Only the tones lighter than the dim tone move; the outline and
     * the shadows stay, so the pulse never flattens the volume (37b). Only colours change; the alpha mask stays
     * identical, so interpolation never creates half-transparent pixels.
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
                        // Outline and shadow tones (no lighter than the dim tone) stay put, so the volume reads in every frame.
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
        BufferedImage strip = new BufferedImage(SIZE, SIZE * animation.frames(), BufferedImage.TYPE_INT_ARGB);
        CyclingPainter painter = TEXTURES.get(name).cyclingPainter();
        for (int i = 0; i < animation.frames(); i++) {
            BufferedImage frame = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            painter.paint(frame, seeded(name), i / (double) animation.frames());
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    strip.setRGB(x, i * SIZE + y, frame.getRGB(x, y));
                }
            }
        }
        return strip;
    }

    /** All frames of an animated texture, for the contact sheet. */
    private static BufferedImage animationStrip(String name, BufferedImage rest, Animation animation) {
        return animation.cycling() ? cyclingStrip(name, animation) : pulseStrip(rest, animation);
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

    /**
     * Selective outline (37b-3 A), as vanilla's items: every transparent pixel next to the shape becomes {@code dark} where
     * it lies below or right of the shape (the side away from the light) and {@code light} (a dark tint of the material)
     * above or left of it. Gives volume where a single colour all round reads as a sticker.
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

    /**
     * Paints a hand-laid sprite: one string of 16 characters per row, {@code '.'} transparent, every other character a
     * colour from {@code legend}. Used where a silhouette matters more than a pattern (items, 37b).
     */
    private static void sprite(BufferedImage image, String[] rows, Map<Character, Integer> legend) {
        if (rows.length != SIZE) {
            throw new IllegalStateException("sprite needs " + SIZE + " rows, got " + rows.length);
        }
        for (int y = 0; y < SIZE; y++) {
            if (rows[y].length() != SIZE) {
                throw new IllegalStateException("sprite row " + y + " has " + rows[y].length() + " characters");
            }
            for (int x = 0; x < SIZE; x++) {
                char c = rows[y].charAt(x);
                if (c == '.') {
                    continue;
                }
                Integer rgb = legend.get(c);
                if (rgb == null) {
                    throw new IllegalStateException("sprite row " + y + ": no colour for '" + c + "'");
                }
                set(image, x, y, rgb);
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

    /**
     * Wet netherrack (37b-1 A): lumps on vanilla netherrack's scale and tone ladder, so vanilla's nether ores sit in it
     * without a square. Each lump has its own base tone, is lit from the top left and shadowed along its lower right
     * edge. What makes it soaked stays soft, so the lumps dominate and the 16-px repetition does not read as a grid:
     * damp zones (a broad, tileable noise) darken the rock by about one tone and carry the wet gloss on their lumps;
     * the crevices hold black-red pores ({@link #SOAKED_PORE_ODDS} in {@link #SOAKED_CREVICE_ROLLS}) and a few damp
     * grey-green specks ({@link #SOAKED_DAMP_SPECKS}, scattered so no two share a row, column or diagonal). The damp
     * runs softly over the whole tile and across its seams (tileable noise); the lumps are lit so the tile's mean
     * brightness matches vanilla netherrack's (~0.20), so a neighbouring ore block shows no lighter or darker square and
     * no frame appears at the seams. The outer pixel ring carries no gloss, pores or specks.
     */
    private static void soakedNetherrack(BufferedImage image, Random random) {
        // Several layouts from the same seed; keep the one that repeats least visibly (rows, columns and diagonals).
        BufferedImage best = null;
        double bestScore = Double.MAX_VALUE;
        for (int attempt = 0; attempt < 8; attempt++) {
            BufferedImage candidate = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            if (!soakedNetherrackLayout(candidate, new Random(random.nextLong()))) {
                continue;
            }
            double score = stripeScore(candidate);
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        if (best == null) {
            throw new IllegalStateException("soaked_netherrack: no layout fits " + SOAKED_DAMP_SPECKS + " damp specks");
        }
        copy(best, image);
    }

    /**
     * How strongly a tile reads as stripes when repeated: the largest step in mean brightness between neighbouring rows,
     * columns, diagonals or anti-diagonals (all wrapping, so the seam between two tiles counts too).
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

    /** One layout of {@link #soakedNetherrack}; false when its crevices do not fit all the damp specks. */
    private static boolean soakedNetherrackLayout(BufferedImage image, Random random) {
        Cells cells = Cells.random(random, 16);
        double[] base = new double[cells.count()];
        boolean[] glossy = new boolean[cells.count()];
        for (int i = 0; i < base.length; i++) {
            base[i] = 0.55 + 0.35 * random.nextDouble();
            glossy[i] = random.nextInt(5) < 3;
        }
        double[][] broad = valueNoise(random, 8, 8);
        double[][] damp = valueNoise(random, 8, 8);
        List<int[]> crevices = new ArrayList<>();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                boolean rim = x == 0 || y == 0 || x == SIZE - 1 || y == SIZE - 1;
                Cells.Nearest nearest = cells.nearest(x, y);
                double gap = nearest.secondDistance() - nearest.distance();
                double slope = nearest.dx() + nearest.dy();
                if (gap < 0.45) {
                    boolean pore = !rim && random.nextInt(SOAKED_CREVICE_ROLLS) < SOAKED_PORE_ODDS;
                    set(image, x, y, pore ? SOAKED_PORE : SOAKED_NETHERRACK[0]);
                    if (!rim && !pore) {
                        crevices.add(new int[] {x, y});
                    }
                    continue;
                }
                double wet = Math.max(0, damp[x][y] - 0.5) * 2;
                double light = base[nearest.index()] + 0.2 * (broad[x][y] - 0.5) - 0.3 * slope - 0.2 * wet + 0.08 * random.nextDouble();
                if (gap < 1.1 && slope > 0.15) {
                    light -= 0.12;
                }
                int color = shade(SOAKED_NETHERRACK, light);
                if (!rim && glossy[nearest.index()] && wet > 0.2 && gap > 0.9 && slope < -0.15 && light > 0.5
                        && random.nextInt(3) != 0) {
                    color = SOAKED_GLOSS;
                }
                set(image, x, y, color);
            }
        }
        // Every crevice pixel once, in a seeded random order: the first ones that share no line with an earlier speck.
        Collections.shuffle(crevices, random);
        List<int[]> specks = new ArrayList<>();
        for (int[] spot : crevices) {
            if (specks.size() < SOAKED_DAMP_SPECKS && specks.stream().noneMatch(other -> sharesLine(spot, other))) {
                specks.add(spot);
                set(image, spot[0], spot[1], SOAKED_DAMP);
            }
        }
        return specks.size() == SOAKED_DAMP_SPECKS;
    }

    /** Whether two pixels of a repeating tile lie on (or next to) the same row, column, diagonal or anti-diagonal. */
    private static boolean sharesLine(int[] a, int[] b) {
        int dx = Math.floorMod(a[0] - b[0], SIZE);
        int dy = Math.floorMod(a[1] - b[1], SIZE);
        int diagonal = Math.floorMod(dx - dy, SIZE);
        int anti = Math.floorMod(dx + dy, SIZE);
        return near(dx) || near(dy) || near(diagonal) || near(anti);
    }

    /** A wrapped offset within one pixel of zero. */
    private static boolean near(int offset) {
        return offset <= 1 || offset >= SIZE - 1;
    }

    /**
     * Spore moss from above (37b): cushions (wrapping cells) lit from the top left with darker seams between them, and
     * small groups of pale spores on the cushions.
     */
    private static void sporeMoss(BufferedImage image, Random random) {
        Cells cells = Cells.random(random, 12);
        double[] base = new double[cells.count()];
        for (int i = 0; i < base.length; i++) {
            base[i] = 0.4 + 0.3 * random.nextDouble();
        }
        double[][] broad = valueNoise(random, 8, 8);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                Cells.Nearest nearest = cells.nearest(x, y);
                double gap = nearest.secondDistance() - nearest.distance();
                double slope = nearest.dx() + nearest.dy();
                double light = gap < 0.5 ? 0.1 + 0.15 * random.nextDouble()
                        : base[nearest.index()] - 0.4 * slope + 0.2 * (broad[x][y] - 0.5) + 0.1 * random.nextDouble();
                set(image, x, y, shade(SPORE_MOSS, light));
            }
        }
        for (int i = 0; i < 6; i++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            setWrapped(image, x, y, SPORE_DOT_LIGHT);
            if (random.nextBoolean()) {
                setWrapped(image, x + 1, y, SPORE_DOT);
            }
            if (random.nextBoolean()) {
                setWrapped(image, x, y + 1, SPORE_DOT);
            }
        }
    }

    /**
     * Soaked netherrack (exactly its pixels) under a ragged moss rim, 3–5 pixels deep, lit along its top row, with a
     * few tufts hanging one pixel lower.
     */
    private static void sporeMossSide(BufferedImage image, Random random) {
        copy(renderUnchecked("block/soaked_netherrack"), image);
        double[][] fine = valueNoise(random, 2, 2);
        for (int x = 0; x < SIZE; x++) {
            int depth = 3 + random.nextInt(3);
            for (int y = 0; y < depth; y++) {
                double edge = y == 0 ? 0.25 : y == depth - 1 ? -0.2 : 0.0;
                set(image, x, y, shade(SPORE_MOSS, 0.25 + 0.5 * fine[x][y] + edge));
            }
            if (depth < 5 && random.nextInt(4) == 0) {
                set(image, x, depth, SPORE_MOSS[0]);
            }
            if (random.nextInt(6) == 0) {
                set(image, x, 1, SPORE_DOT);
            }
        }
    }

    /**
     * Grey-purple stem bark (37b): fibres that sway like vanilla's nether stems (a sine across the height, so the tile
     * repeats seamlessly), lumpy tone, grooves lit on their right wall, and slime running down the grooves: the streaks
     * glide down one pixel per frame ({@code phase} 0 to 1 is one full turn of the 16-px tile, so the loop is seamless).
     */
    private static void dripshroomStem(BufferedImage image, Random random, double phase) {
        int shift = (int) Math.round(phase * SIZE);
        double[] fibre = new double[SIZE];
        for (int x = 0; x < SIZE; x++) {
            fibre[x] = random.nextDouble();
        }
        double[][] lumps = valueNoise(random, 4, 8);
        double sway = random.nextDouble() * 2 * Math.PI;
        int first = random.nextInt(3);
        boolean[] groove = new boolean[SIZE];
        for (int x = first; x < SIZE; x += 5) {
            groove[x] = true;
        }
        for (int y = 0; y < SIZE; y++) {
            int offset = (int) Math.round(1.5 * Math.sin(2 * Math.PI * y / SIZE + sway));
            for (int x = 0; x < SIZE; x++) {
                int column = Math.floorMod(x - offset, SIZE);
                double light = 0.2 + 0.45 * fibre[column] + 0.35 * (lumps[x][y] - 0.5) + 0.1 * random.nextDouble();
                if (groove[column]) {
                    set(image, x, y, DRIPSHROOM_BARK[0]);
                    continue;
                }
                if (groove[Math.floorMod(column - 1, SIZE)]) {
                    light += 0.3;
                } else if (groove[Math.floorMod(column + 1, SIZE)]) {
                    light -= 0.15;
                }
                set(image, x, y, shade(DRIPSHROOM_BARK, light));
            }
        }
        for (int i = 0; i < 4; i++) {
            int grooveColumn = first + 5 * random.nextInt(3);
            int start = random.nextInt(SIZE);
            int length = 3 + random.nextInt(5);
            for (int j = 0; j < length; j++) {
                int y = Math.floorMod(start + j + shift, SIZE);
                int x = grooveColumn + (int) Math.round(1.5 * Math.sin(2 * Math.PI * y / SIZE + sway));
                setWrapped(image, x, y, DRIPSHROOM_SLIME[j == length - 1 ? 2 : j % 2]);
            }
        }
    }

    /** End grain (37b): rings of pale flesh around a darker pith, lit towards the top left, and a bark rim. */
    private static void dripshroomStemTop(BufferedImage image, Random random) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double radius = Math.hypot(x - 7.5, y - 7.5);
                double light = 0.55 + 0.25 * Math.cos(radius * 1.6) - 0.02 * (x + y - 15) + 0.15 * (random.nextDouble() - 0.5);
                set(image, x, y, radius < 1.5 ? DRIPSHROOM_WOOD[1] : shade(DRIPSHROOM_WOOD, light));
            }
        }
        rim(image, random, DRIPSHROOM_BARK, 0.0, 0.7);
    }

    /**
     * The cap (37b): fleshy lumps like vanilla's wart blocks, each lit from the top left and shadowed along its lower
     * right edge, dark seams between them, and round slime drops with a highlight.
     */
    private static void dripshroomCap(BufferedImage image, Random random) {
        Cells cells = Cells.random(random, 9);
        double[] base = new double[cells.count()];
        for (int i = 0; i < base.length; i++) {
            base[i] = 0.4 + 0.3 * random.nextDouble();
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                Cells.Nearest nearest = cells.nearest(x, y);
                double gap = nearest.secondDistance() - nearest.distance();
                double slope = nearest.dx() + nearest.dy();
                if (gap < 0.6) {
                    set(image, x, y, DRIPSHROOM_CAP[0]);
                    continue;
                }
                double light = base[nearest.index()] - 0.45 * slope + 0.1 * random.nextDouble();
                if (gap < 1.3 && slope > 0.1) {
                    light -= 0.15;
                }
                set(image, x, y, shade(DRIPSHROOM_CAP, light));
            }
        }
        for (int i = 0; i < 4; i++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            setWrapped(image, x, y, DRIPSHROOM_SLIME[2]);
            setWrapped(image, x + 1, y, DRIPSHROOM_SLIME[1]);
            setWrapped(image, x, y + 1, DRIPSHROOM_SLIME[1]);
            setWrapped(image, x + 1, y + 1, DRIPSHROOM_SLIME[0]);
        }
    }

    /**
     * Glowing cells (37b), with the contrast of vanilla's shroomlight: olive cell walls, lime bodies that brighten
     * towards their centre and towards the top left, a near-white core in each cell.
     */
    private static void dripshroomLamp(BufferedImage image, Random random) {
        Cells cells = Cells.random(random, 7);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                Cells.Nearest nearest = cells.nearest(x, y);
                double gap = nearest.secondDistance() - nearest.distance();
                if (gap < 1.0) {
                    set(image, x, y, DRIPSHROOM_LAMP_WALL[gap < 0.45 ? 0 : 1]);
                    continue;
                }
                if (nearest.distance() < 1.0) {
                    set(image, x, y, DRIPSHROOM_LAMP_CORE);
                    continue;
                }
                double light = 1.0 - nearest.distance() / 4.5 - 0.3 * (nearest.dx() + nearest.dy()) + 0.1 * random.nextDouble();
                set(image, x, y, shade(DRIPSHROOM_LAMP, light));
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

    /**
     * Small tuft (37b): four stalks of different heights, darker at the foot and with a shadowed right side low down,
     * each with a 2×2 spore cap (bright corner top left); transparent background (cutout).
     */
    private static void sporeSprouts(BufferedImage image, Random random) {
        int[] columns = {3, 6, 9, 12};
        for (int column : columns) {
            int x = column + random.nextInt(2) - (column == 12 ? 1 : 0);
            int top = 4 + random.nextInt(6);
            for (int y = SIZE - 1; y > top + 1; y--) {
                set(image, x, y, SPORE_MOSS[y > 12 ? 1 : 3 + random.nextInt(2)]);
                if (y > 12) {
                    set(image, x + 1, y, SPORE_MOSS[0]);
                }
            }
            set(image, x - 1, top, SPORE_DOT_LIGHT);
            set(image, x, top, SPORE_DOT);
            set(image, x - 1, top + 1, SPORE_MOSS[5]);
            set(image, x, top + 1, SPORE_MOSS[2]);
        }
    }

    /**
     * A slime stalactite hanging from the top of the tile (37b); {@code up} variants are these flipped. Widths follow the
     * block shapes (base 12, middle 10, frustum 8, merged tip 6 px); the lone tip tapers to a point after 11 rows. Lit on
     * the left with a broken wet gloss line, shadowed on the right, a few darker drip streaks running down; the left
     * edge sways a pixel so the column does not read as a pipe.
     */
    private static void slimeStalactite(BufferedImage image, Random random, int width, boolean taper) {
        double sway = random.nextDouble() * 2 * Math.PI;
        int[] streaks = {random.nextInt(3), 3 + random.nextInt(3)};
        for (int y = 0; y < SIZE; y++) {
            double rowWidth = width;
            if (taper) {
                if (y > 10) {
                    continue;
                }
                rowWidth = width - (width - 1) * y / 10.0;
            }
            int left = (int) Math.round(7.5 - rowWidth / 2.0);
            int right = (int) Math.round(7.5 + rowWidth / 2.0) - 1;
            if (!taper && Math.sin(2 * Math.PI * y / SIZE + sway) > 0.6) {
                left++;
            }
            for (int x = left; x <= right; x++) {
                double t = right == left ? 0.5 : (x - left) / (double) (right - left);
                int color = x == right ? SLIME_STALACTITE[0] : shade(SLIME_STALACTITE, 0.9 - 0.8 * t + 0.12 * random.nextDouble());
                if (x == left + 1 && right - left > 2 && random.nextInt(4) != 0) {
                    color = SLIME_GLOSS;
                }
                for (int streak : streaks) {
                    if (x == right - 1 - streak && x > left + 1 && Math.floorMod(y + 5 * streak, 9) < 5) {
                        color = SLIME_STALACTITE[1];
                    }
                }
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

    /**
     * The stalactite in the inventory (37b), slender like vanilla's pointed dripstone: hanging from a broad root at the
     * top to a fine tip, a wet gloss line down its lit left side, a shadowed right side, two ring ridges and a selective
     * outline.
     */
    private static void slimeStalactiteItem(BufferedImage image, Random random) {
        sprite(image, new String[] {
                "................",
                ".....4433210....",
                ".....G332210....",
                "......G32211....",
                ".....4332210....",
                "......G3221.....",
                "......G3221.....",
                "......43211.....",
                ".......G221.....",
                ".......G321.....",
                ".......321......",
                ".......G21......",
                ".......32.......",
                ".......21.......",
                ".......3........",
                "................"}, Map.of(
                '0', SLIME_STALACTITE[0], '1', SLIME_STALACTITE[1], '2', SLIME_STALACTITE[2], '3', SLIME_STALACTITE[3],
                '4', SLIME_STALACTITE[4], 'G', SLIME_GLOSS));
        outlineSelective(image, SLIME_STALACTITE[1], SLIME_STALACTITE_OUTLINE);
    }

    /**
     * Water in a block: dark blue-green with wavy lines of lighter water that drift one wave length per cycle (slow,
     * seamless), a few fixed glints; opaque. The waves wrap around the tile edges.
     */
    private static void dewSource(BufferedImage image, Random random, double phase) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double wave = Math.sin((x + 2.0 * Math.sin(y * Math.PI / 4.0)) * Math.PI / 4.0 + y * Math.PI / 2.0 - 2 * Math.PI * phase);
                double t = 0.45 + 0.3 * wave + 0.2 * (random.nextDouble() - 0.5);
                set(image, x, y, shade(DEW_SOURCE, t));
            }
        }
        for (int i = 0; i < 6; i++) {
            set(image, random.nextInt(SIZE), random.nextInt(SIZE), DEW_SOURCE_GLINT);
        }
    }

    /**
     * Dew crystal bud stage 1 to 4 (small, medium, large, cluster): 1, 2, 3 and 5 pointed crystals from the bottom edge
     * (cross model, cutout), the later stages taller. Transparent background.
     */
    private static void dewBud(BufferedImage image, Random random, int stage) {
        switch (stage) {
            case 1 -> dewSpike(image, 7.5, 1, 5, 0);
            case 2 -> {
                dewSpike(image, 4.5, 1, 6, -1);
                dewSpike(image, 10.5, 1, 8 + random.nextInt(2), 1);
            }
            case 3 -> {
                dewSpike(image, 3.5, 1, 7, -1);
                dewSpike(image, 11.5, 1, 8, 1);
                dewSpike(image, 7.5, 2, 11 + random.nextInt(2), 0);
            }
            default -> {
                dewSpike(image, 2.5, 1, 6 + random.nextInt(2), -2);
                dewSpike(image, 12.5, 1, 7 + random.nextInt(2), 2);
                dewSpike(image, 5.5, 2, 10, -1);
                dewSpike(image, 9.5, 2, 11, 1);
                dewSpike(image, 7.5, 2, 14, 0);
            }
        }
        outlineSelective(image, DEW_CRYSTAL_OUTLINE_LIGHT, DEW_CRYSTAL_OUTLINE);
    }

    /**
     * One crystal from the bottom row up, as {@link #crystalSpike} (37b): light left edge, lit left facet, a glinting ridge
     * on the upper part, shaded right facet, dark right edge, a bright tip and a shadowed foot.
     */
    private static void dewSpike(BufferedImage image, double centre, int halfWidth, int height, int lean) {
        for (int row = 0; row < height; row++) {
            int y = SIZE - 1 - row;
            int ridge = (int) Math.round(centre + lean * row / (double) height);
            int fromTop = height - 1 - row;
            int half = Math.min(halfWidth, fromTop * halfWidth / (halfWidth + 1));
            for (int x = ridge - half; x <= ridge + half; x++) {
                if (x < 0 || x >= SIZE) {
                    continue;
                }
                int color = x == ridge - half && half > 0 ? DEW_CRYSTAL[3] : x < ridge ? DEW_CRYSTAL[2]
                        : x == ridge ? DEW_CRYSTAL[fromTop < height / 2 ? 4 : 3] : DEW_CRYSTAL[x == ridge + half ? 0 : 1];
                if (row == 0) {
                    color = x <= ridge ? DEW_CRYSTAL[1] : DEW_CRYSTAL[0];
                }
                set(image, x, y, fromTop == 0 ? DEW_CRYSTAL[4] : color);
            }
        }
    }

    /**
     * One dew crystal shard in the inventory (37b, redrawn after feedback): a broad, uneven shard, not a rod: a polygon
     * with a sharp tip at the top right, the widest part in the middle and a blunter, broken foot at the bottom left. A ridge runs
     * from the tip to the middle of the foot: the face left of it is lit (brightest along the upper-left edge), the face
     * right of it is shaded and darkest along the lower-right edge; the foot row lies in shadow. Selective outline.
     */
    private static void dewCrystalItem(BufferedImage image, Random random) {
        double[][] shard = {{13.0, 1.5}, {13.8, 6.5}, {10.8, 12.0}, {7.0, 14.2}, {3.5, 12.5}, {4.8, 8.0}, {8.5, 4.0}};
        double[] tip = shard[0];
        double[] foot = shard[3];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double px = x + 0.5;
                double py = y + 0.5;
                if (!insidePolygon(shard, px, py)) {
                    continue;
                }
                // Which side of the ridge (tip to foot): negative is the lit left face.
                double side = (foot[0] - tip[0]) * (py - tip[1]) - (foot[1] - tip[1]) * (px - tip[0]);
                double ridge = Math.abs(side) / Math.hypot(foot[0] - tip[0], foot[1] - tip[1]);
                int tone;
                if (side > 0) {
                    tone = ridge < 1.2 ? 4 : edgeDistance(shard, px, py, 4, 7) < 1.3 ? 4 : 3;
                } else {
                    tone = ridge < 1.0 ? 2 : edgeDistance(shard, px, py, 1, 3) < 1.3 ? 0 : 1;
                }
                if (y >= 13) {
                    tone = Math.max(0, tone - 2);
                }
                set(image, x, y, DEW_CRYSTAL[tone]);
            }
        }
        outlineSelective(image, DEW_CRYSTAL_OUTLINE_LIGHT, DEW_CRYSTAL_OUTLINE);
    }

    /** Even-odd test of the point against a closed polygon. */
    private static boolean insidePolygon(double[][] polygon, double x, double y) {
        boolean inside = false;
        for (int i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
            if ((polygon[i][1] > y) != (polygon[j][1] > y)
                    && x < (polygon[j][0] - polygon[i][0]) * (y - polygon[i][1]) / (polygon[j][1] - polygon[i][1]) + polygon[i][0]) {
                inside = !inside;
            }
        }
        return inside;
    }

    /** Shortest distance from the point to the polygon edges from vertex {@code from} up to vertex {@code to} (wrapping). */
    private static double edgeDistance(double[][] polygon, double x, double y, int from, int to) {
        double best = Double.MAX_VALUE;
        for (int i = from; i < to; i++) {
            double[] a = polygon[i % polygon.length];
            double[] b = polygon[(i + 1) % polygon.length];
            double dx = b[0] - a[0];
            double dy = b[1] - a[1];
            double t = Math.max(0, Math.min(1, ((x - a[0]) * dx + (y - a[1]) * dy) / (dx * dx + dy * dy)));
            best = Math.min(best, Math.hypot(x - a[0] - t * dx, y - a[1] - t * dy));
        }
        return best;
    }

    // ---- Particles ------------------------------------------------------------------------------------------

    /** 8x8 grains, small to large: '#' light, 'o' mid, 'x' shadow, '.' transparent. */
    private static final String[][] SAND_GRAINS = {
            {"........", "........", "........", "...##...", "...ox...", "........", "........", "........"},
            {"........", "........", "..###...", "..###o..", "..#oox..", "...xx...", "........", "........"},
            {"........", "..###...", ".#####..", ".####o..", ".###oox.", "..ooxx..", "...xx...", "........"},
            {"...##...", ".######.", ".#####o.", "######o.", ".####oo.", ".ooooxx.", "..oxxx..", "........"}};

    private static void sandGrain(BufferedImage image, int grain) {
        for (int y = 0; y < PARTICLE_SIZE; y++) {
            for (int x = 0; x < PARTICLE_SIZE; x++) {
                switch (SAND_GRAINS[grain][y].charAt(x)) {
                    case '#' -> set(image, x, y, 0xF2F2F2);
                    case 'o' -> set(image, x, y, 0xCFCFCF);
                    case 'x' -> set(image, x, y, 0xA8A8A8);
                    default -> {
                    }
                }
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
        int height = EXTRAS_HEIGHT + TEXTURES.size() * (3 * tile + 16) + oreRows * (oreTile + 16);
        BufferedImage sheet = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = sheet.createGraphics();
        graphics.setColor(new Color(0x2B2B2B));
        graphics.fillRect(0, 0, width, height);
        drawExtras(graphics);
        int row = 0;
        for (Map.Entry<String, Texture> entry : TEXTURES.entrySet()) {
            BufferedImage frame = render(entry.getKey());
            int top = EXTRAS_HEIGHT + row * (3 * tile + 16) + 8;
            if (row == 0 || entry.getKey().equals("block/soaked_netherrack")) {
                graphics.setColor(new Color(0xFFC857));
                graphics.drawString(row == 0 ? "== RING 2 ==" : "== NETHER RING 2 ==", 8, top + 48);
            }
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
                BufferedImage strip = animationStrip(entry.getKey(), frame, animation);
                int size = frame.getWidth();
                for (int i = 0; i < animation.frames(); i++) {
                    BufferedImage single = strip.getSubimage(0, i * size, size, size);
                    draw(graphics, single, label + 4 * tile + 32 + i * (SIZE * frameScale + 4), top, frameScale);
                }
            }
            row++;
        }
        // Ores: every overlay over each of its host rocks, tiled 3×3 at 4×; one row per overlay (preview only).
        int oreTop = EXTRAS_HEIGHT + row * (3 * tile + 16) + 8 - (oreTile + 16);
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

    private static final int EXTRAS_HEIGHT = 1230;

    /** Preview only: tile {@code name} {@code columns} x {@code rows} at 4x, from (left, top). */
    private static void drawTiled(Graphics2D graphics, BufferedImage tile, int left, int top, int columns, int rows) {
        for (int ty = 0; ty < rows; ty++) {
            for (int tx = 0; tx < columns; tx++) {
                draw(graphics, tile, left + tx * SIZE * 4, top + ty * SIZE * 4, 4);
            }
        }
    }

    private static BufferedImage rotated(BufferedImage image, int quarterTurns) {
        BufferedImage turned = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int rx = x;
                int ry = y;
                for (int i = 0; i < quarterTurns; i++) {
                    int nx = SIZE - 1 - ry;
                    ry = rx;
                    rx = nx;
                }
                turned.setRGB(rx, ry, image.getRGB(x, y));
            }
        }
        return turned;
    }

    /**
     * Extra preview bands above the per-texture rows: the cobblestone choice next to the old placeholder, the other
     * variants and the three tints; the strata of a canyon wall (7 blocks, three tints in bands); cracked clay with
     * random rotations; a salt flat of crust with some salt blocks (the world's lines) between.
     */
    private static void drawExtras(Graphics2D graphics) throws IOException {
        int block = SIZE * 4;
        int top = 8;
        graphics.setColor(Color.WHITE);
        graphics.drawString("canyon cobblestone: placeholder (O), chosen (A, the real texture), B, C | red, orange, cream, bricks", 8, top + 12);
        top += 20;
        BufferedImage[] row = {rendered(PREVIEW_ONLY, "variant/cobble_O"), render("block/canyon_cobblestone"),
                rendered(PREVIEW_ONLY, "variant/cobble_B"), rendered(PREVIEW_ONLY, "variant/cobble_C"),
                render("block/red_canyon_stone"), render("block/orange_canyon_stone"), render("block/cream_canyon_stone"),
                render("block/canyon_stone_bricks")};
        for (int i = 0; i < row.length; i++) {
            drawTiled(graphics, row[i], 8 + i * (3 * block + 16), top, 3, 3);
        }
        top += 3 * block + 24;
        graphics.setColor(Color.WHITE);
        graphics.drawString("strata (red, orange, cream bands as in the wall) with cobblestone at the foot; cracked clay 4x4 random rotations; salt flat 5x5", 8, top + 12);
        top += 20;
        String[] bands = {"red", "red", "orange", "cream", "cream", "red", "orange"};
        for (int i = 0; i < bands.length; i++) {
            drawTiled(graphics, render("block/" + bands[i] + "_canyon_stone"), 8, top + i * block, 3, 1);
        }
        Random rotation = new Random(7);
        for (int ty = 0; ty < 4; ty++) {
            for (int tx = 0; tx < 4; tx++) {
                draw(graphics, rotated(render("block/cracked_clay"), rotation.nextInt(4)), 8 + 3 * block + 24 + tx * block, top + ty * block, 4);
            }
        }
        BufferedImage crust = render("block/salt_crust_top");
        BufferedImage saltBlock = render("block/salt_block");
        int saltLeft = 8 + 3 * block + 24 + 4 * block + 24;
        for (int ty = 0; ty < 5; ty++) {
            for (int tx = 0; tx < 5; tx++) {
                draw(graphics, (tx + 2 * ty) % 5 == 0 ? saltBlock : crust, saltLeft + tx * block, top + ty * block, 4);
            }
        }
        top += 7 * block + 16;
        graphics.setColor(Color.WHITE);
        graphics.drawString("items in a slot at 1x, 2x and 3x (inventory grey); lint: " + LINT_WARNINGS.size() + " warning(s), see the console", 8, top + 12);
        top += 20;
        for (int i = 0; i < SLOT_SPRITES.length; i++) {
            int left = 8 + (i % 9) * 128;
            int rowTop = top + (i / 9) * 64;
            BufferedImage sprite = render(SLOT_SPRITES[i]);
            int x = left;
            for (int scale = 1; scale <= 3; scale++) {
                int size = SIZE * scale;
                graphics.setColor(new Color(0xC6C6C6));
                graphics.fillRect(x, rowTop, size + 4, size + 4);
                graphics.setColor(new Color(0x8B8B8B));
                graphics.fillRect(x + 2, rowTop + 2, size, size);
                drawOver(graphics, sprite, x + 2, rowTop + 2, scale);
                x += size + 8;
            }
        }
        top += 2 * 64 + 8;
        graphics.setColor(Color.WHITE);
        graphics.drawString("Nether ring 2 wall: moss rim, stem, cap, lamp, planks, dew source in soaked netherrack (own textures only)", 8, top + 12);
        top += 20;
        for (int ty = 0; ty < NETHER_WALL.length; ty++) {
            for (int tx = 0; tx < NETHER_WALL[ty].length; tx++) {
                draw(graphics, render(NETHER_WALL[ty][tx]), 8 + tx * block, top + ty * block, 4);
            }
        }
    }

    /** Inventory sprites for the "items in a slot" band: the items and the blocks shown flat in the inventory. */
    private static final String[] SLOT_SPRITES = {"item/raw_sunstone", "item/raw_turquoise", "item/salt", "item/dew_crystal",
            "item/slime_stalactite", "item/palm_door", "item/dripshroom_door", "block/salt_crystal", "block/dew_crystal_cluster",
            "block/large_dew_crystal_bud", "block/dry_shrub", "block/desert_flower", "block/palm_sapling", "block/spore_sprouts",
            "block/glow_mold", "block/small_dew_crystal_bud", "block/medium_dew_crystal_bud"};

    private static final String[][] NETHER_WALL = {
            {"block/spore_moss_side", "block/spore_moss_side", "block/dripshroom_stem", "block/spore_moss_side", "block/dripshroom_cap", "block/dripshroom_cap", "block/dripshroom_lamp"},
            {"block/soaked_netherrack", "block/soaked_netherrack", "block/dripshroom_stem", "block/soaked_netherrack", "block/soaked_netherrack", "block/dripshroom_cap", "block/dripshroom_cap"},
            {"block/soaked_netherrack", "block/dew_source", "block/dripshroom_stem", "block/soaked_netherrack", "block/dripshroom_planks", "block/dripshroom_planks", "block/soaked_netherrack"},
            {"block/soaked_netherrack", "block/soaked_netherrack", "block/stripped_dripshroom_stem", "block/soaked_netherrack", "block/soaked_netherrack", "block/soaked_netherrack", "block/soaked_netherrack"}};

    /** {@link #draw} without the backdrop: transparent pixels show what is already there (the slot grey). */
    private static void drawOver(Graphics2D graphics, BufferedImage image, int left, int top, int scale) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                if (argb >>> 24 != 0) {
                    graphics.setColor(new Color(argb & 0xFFFFFF));
                    graphics.fillRect(left + x * scale, top + y * scale, scale, scale);
                }
            }
        }
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

    /** Perceived brightness of an RGB colour in [0, 1] (Rec. 709 weights). */
    private static double luminance(int rgb) {
        return (0.2126 * (rgb >> 16 & 0xFF) + 0.7152 * (rgb >> 8 & 0xFF) + 0.0722 * (rgb & 0xFF)) / 255.0;
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
