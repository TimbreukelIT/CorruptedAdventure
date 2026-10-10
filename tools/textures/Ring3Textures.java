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
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Generates the ring 3 (volcanic) and Nether ring 3 (frozen Nether) block, item and particle textures from our own
 * pixels: no vanilla pixels are read or copied. Run from the repository root: {@code java tools/textures/Ring3Textures.java}.
 *
 * <p>Ring 3 starts with good first versions in the 37b style (fase-1-wereld §1.4, no placeholder round): light from
 * the top left, 5–7 tones per material with a hue shift (cool shadows, warm lights), selective outlines for items,
 * crystals and ore overlays, a flatness lint, and a contact sheet with 6×6 walls to catch tile repetition.
 *
 * <p>Glow (contract §7): every glowing block has two textures, {@code <name>} (the whole block, cracks painted in) and
 * {@code <name>_glow} (a cutout layer with only the glowing pixels, the same colours, pulsing). The model draws the
 * glow layer emissive over the block, so only the cracks glow at night. {@link #GLOW_LAYERS} checks that a glow layer
 * matches its block pixel for pixel.
 *
 * <p>Deterministic: every texture has a fixed seed (hash of its file name), so running twice gives identical PNGs and
 * {@code .mcmeta}s. Every texture is checked before it is written (size, alpha 0/255 only, the same alpha mask in every
 * animation frame, cycling textures opaque). The shared helpers are copied from {@code Ring2Textures.java}: a
 * single-file script cannot load a second source file (deduplication: backlog, an {@code infra} choice).
 */
public final class Ring3Textures {
    private static final int SIZE = 16;
    private static final int PARTICLE_SIZE = 8;
    private static final int TRANSPARENT = 0x00000000;
    private static final Path OUTPUT = Path.of("src/main/resources/assets/corruptedadventure/textures");
    private static final Path PREVIEW = Path.of("build/texture-preview/ring3.png");

    /** Retouched by hand after generation; the script skips these and the preview reads them from disk. */
    private static final Set<String> HAND_EDITED = Set.of();

    // ---- Palettes, dark to light (RGB): cool, slightly violet shadows and warm lights (hue shift) ----------------

    // Ash: warm grey powder, darker than gravel (0.50), lighter than basalt (0.29), so an ash plain reads grey-black.
    private static final int[] ASH = {0x3A3538, 0x4B4546, 0x5D5655, 0x6F6763, 0x827973, 0x968C84};
    private static final int ASH_CHAR = 0x252124;
    // Pumice: light, porous, warm grey; 57a-r1: a step darker and softer (caps read as snow/diorite at 0.63).
    private static final int[] PUMICE = {0x6F6962, 0x817A71, 0x938C82, 0xA49D92, 0xB5AEA2, 0xC5BFB3};
    private static final int[] PUMICE_PORE = {0x5A544E, 0x6A635C};
    // Smouldering stone: near-black rock (blackstone-dark) with ember cracks.
    private static final int[] SMOLDER_ROCK = {0x151214, 0x1E191B, 0x282223, 0x332B2B, 0x403533, 0x50423D};
    private static final int[] SMOLDER_HALO = {0x3A1F1C, 0x4E261E, 0x63301F};
    // Ember: the glow of ring 3 (cracks, leaves, ash casts); [0] is the dim tone a pulse leans towards.
    private static final int[] EMBER = {0x701808, 0xA82C0C, 0xDC5414, 0xFA8828, 0xFFC25A, 0xFFF0BC};
    private static final int EMBER_GLOW = 0xFFE4A0;
    private static final int EMBER_DIM = EMBER[0];
    /**
     * Share of the smouldering stone's crack network that glows (the rest is a cold black crack); 57a-r1: 0.4 → 0.25,
     * because the glow layer is full-bright and read as confetti in daylight.
     */
    private static final double SMOLDER_HOT_SHARE = 0.25;
    // Smouldering ash: the ash of the glowing casts, burnt but close to ash (57a-r1: lighter, it read as holes in the ash).
    private static final int[] SMOLDER_ASH = {0x2E292A, 0x3B3536, 0x494243, 0x58504F, 0x685F5C, 0x7A6F6A};
    /** Warmed ash beside a hot crack: reddish grey at the ash's mean brightness (the stone's dark halo read as a hole). */
    private static final int SMOLDER_ASH_HALO = 0x6A4438;
    // Charred wood: charcoal bark with a cool silver sheen, the wood under it dark grey-brown.
    private static final int[] CHAR = {0x110F11, 0x1A1618, 0x241F21, 0x2F2929, 0x3B3433, 0x4A423F};
    private static final int[] CHAR_SHEEN = {0x56525C, 0x726E7A};
    private static final int CHAR_EMBER = 0x4C1C14;
    private static final int[] CHARRED_WOOD = {0x29201D, 0x362A25, 0x44352D, 0x534137, 0x644E41, 0x765D4C};
    // Ember leaves: dark smouldering crowns.
    private static final int[] EMBER_LEAVES = {0x151113, 0x201A1B, 0x2C2422, 0x3A2F2A, 0x4B3B32, 0x604A3B};
    // Fire flora.
    private static final int[] FIRE_BLOOM = {0x6A1208, 0xA4240C, 0xD64612, 0xF47620, 0xFFAE3E, 0xFFE48C};
    private static final int[] FIRE_STEM = {0x23201A, 0x353222, 0x4C4830, 0x666040};
    private static final int[] ASH_SPROUTS = {0x2A2823, 0x3E3B31, 0x56533F, 0x726E55, 0x928D72, 0xC2BBA2};
    private static final int[] BERRY_LEAF = {0x211F19, 0x312D23, 0x433D2E, 0x57503A, 0x6C6448};
    private static final int[] BERRY_TWIG = {0x1C1615, 0x2C221F};
    private static final int[] BERRY_UNRIPE = {0x5C3416, 0x864E22, 0xAE7438};
    private static final int[] BERRY_RIPE = {0x6E120A, 0xB0220E, 0xE24618, 0xFF9C5C};
    // Ores (matte: volcanite, sulfur; glowing: fire ruby): outline → glint.
    private static final int[] RAW_VOLCANITE = {0x261D29, 0x3A2E3E, 0x514256, 0x6E5A72, 0x9A6A86, 0xE2AEC2};
    private static final int[] VOLCANITE = {0x120D14, 0x231A27, 0x362838, 0x4C3449, 0x66405A, 0x8A5370, 0xC88EA4};
    private static final int[] SULFUR = {0x4A3A08, 0x7A6610, 0xAC961C, 0xD6BE30, 0xEDDA50, 0xF9EF8C, 0xFFFBD0};
    private static final int[] FIRE_RUBY = {0x24040A, 0x4E0A14, 0x82121E, 0xB81E2C, 0xE03A44, 0xFF7E6E, 0xFFD0C2};
    private static final int FIRE_RUBY_GLOW = 0xFFA090;
    private static final int FIRE_RUBY_DIM = FIRE_RUBY[2];
    // Fire crystal: a flame, deep orange at the foot to gold and white at the tips.
    private static final int[] FIRE_CRYSTAL = {0x581606, 0x94300A, 0xCE5E14, 0xEE8E26, 0xFFBE44, 0xFFE68A, 0xFFFBE0};
    private static final int FIRE_CRYSTAL_OUTLINE_LIGHT = 0x7A2808;
    private static final int FIRE_CRYSTAL_OUTLINE = 0x3A0E04;
    private static final int FIRE_CRYSTAL_GLOW = 0xFFF2C0;
    private static final int FIRE_CRYSTAL_DIM = FIRE_CRYSTAL[1];
    // Sulfur fields, steam fens.
    // 57a-r1: warmer sulfur yellow (it read olive-green from above) on a narrower ladder.
    private static final int[] SULFUR_CRUST = {0x6E5A22, 0x85702A, 0x9C8733, 0xB29D3F, 0xC6B24E, 0xD8C566};
    private static final int[] SULFUR_STAIN = {0x80542A, 0xA07238};
    private static final int SULFUR_SPARKLE = 0xF8F0B0;
    private static final int[] FUMAROLE_ROCK = {0x282423, 0x35302D, 0x443D39, 0x544B45, 0x655B53, 0x776B61};
    private static final int[] VENT = {0x100C09, 0x211A11, 0x3A2E14};
    private static final int[] MUD = {0x2C2520, 0x393029, 0x483D33, 0x584B3F, 0x695A4C, 0x7C6B5A};
    private static final int MUD_SHEEN = 0x9A8873;
    // Coloured sulfur mud (46b, 57): wetter and more saturated than the crust around the pools, each its own mineral.
    private static final int[] YELLOW_MUD = {0x6A6414, 0x7E781A, 0x948E1E, 0xAAA426, 0xBEB82E, 0xD0CA3A};
    private static final int YELLOW_MUD_SHEEN = 0xEEE88A;
    private static final int[] GREEN_MUD = {0x34441C, 0x3E5020, 0x485C24, 0x546A2A, 0x607830, 0x6E8636};
    private static final int GREEN_MUD_SHEEN = 0x9CB466;
    private static final int[] ORANGE_MUD = {0x5E2A12, 0x6C3216, 0x7A3A1A, 0x8A441E, 0x9A4E22, 0xAA5A28};
    private static final int ORANGE_MUD_SHEEN = 0xD08850;
    /** Tone spread of the coloured muds: their ladders are closer than the grey mud's, so the grain needs a little more. */
    private static final double SULFUR_MUD_SPREAD = 1.4;
    private static final int[] SINTER = {0x67625A, 0x807A70, 0x999389, 0xB0ABA0, 0xC6C2B7, 0xDAD7CD};
    private static final int[] GEYSER_WATER = {0x1E2C32, 0x2F454E, 0x4A6870, 0x8AB0B8};

    // Nether ring 3.
    private static final int[] COLD_LAVA = {0x22252D, 0x2C3039, 0x383D47, 0x454B56, 0x535A66, 0x636B78, 0x78808D};
    private static final int COLD_LAVA_MORTAR = 0x17191F;
    private static final int[] NETHER_ICE = {0x435C7A, 0x52708F, 0x6382A3, 0x7696B6, 0x8CAAC8, 0xA6C0DA, 0xC6DAEC};
    private static final int NETHER_ICE_CRACK = 0xE2EEF8;
    private static final int NETHER_ICE_DEEP = 0x34465E;
    private static final int[] PACKED_NETHER_ICE = {0x3A4F6A, 0x465D7C, 0x546C8E, 0x637CA0, 0x758EB2, 0x8AA2C4};
    // Vanilla netherrack's tone ladder (37b lesson: vanilla nether ores carry netherrack pixels, so the rock they sit in
    // must share its hue and tones); rime as thin mauve frost that melts into the red, white only on a few glints.
    private static final int[] RIME_NETHERRACK = {0x411616, 0x501B1B, 0x572121, 0x652828, 0x723232, 0x854242};
    private static final int[] RIME = {0x7C5664, 0xA28C9C};
    /** Lowest base tone of a rime netherrack lump: tuned so the frosted tile keeps vanilla netherrack's mean (~0.20). */
    private static final double RIME_ROCK_BASE = 0.38;
    private static final int[] RIME_BARK = {0x2C3244, 0x3A4256, 0x4A546A, 0x5C6880, 0x727E96, 0x8A96AE};
    private static final int[] FROST = {0xB4C4D8, 0xDCE6F4};
    private static final int[] RIME_WOOD = {0x444A64, 0x535A76, 0x646C88, 0x76809C, 0x8A94AE};
    private static final int[] RIME_CAP = {0x323E58, 0x414E6C, 0x526082, 0x667696, 0x7C8EAC, 0x96A6C2};
    private static final int[] ICE_STALACTITE = {0x587AA0, 0x6E94B8, 0x88ACCE, 0xA6C4E0, 0xC6DCF0};
    private static final int ICE_GLOSS = 0xF2F8FF;
    private static final int ICE_STALACTITE_OUTLINE = 0x2E4664;
    private static final int[] LAVA = {0x6E1608, 0xB0300C, 0xE4601A, 0xFF962E, 0xFFCC5E, 0xFFF4C4};
    private static final int LAVA_GLOW = 0xFFE8A8;
    /**
     * Where the glowing fissure meets every tile edge (57): the middle, so the crack joins its neighbour in each of the 32
     * variants of the block (46b: rotations and mirroring); see {@link #fissureSegments}.
     */
    private static final double FISSURE_EDGE_POINT = SIZE / 2.0;
    /**
     * Crack width where it meets a tile edge: thin, so a fissure next to a non-fissure block ends in a dim hairline
     * instead of a bright lava stump, while neighbouring fissure blocks still join.
     */
    private static final double FISSURE_EDGE_WIDTH = 0.3;
    /** Length of the straight stub square to each edge: the crack's profile at an edge is mirror-symmetric and the same on all four. */
    private static final double FISSURE_STUB = 3.0;
    /** Inner nodes and branch points stay this far from the edges, so only the stubs (and their halo) reach an edge. */
    private static final double FISSURE_MARGIN = 4.0;
    /** The crack is crusted over (dark red, not glowing) on this many outer rows: the seams between fissure blocks. */
    private static final int FISSURE_CRUSTED = 2;
    private static final int[] FISSURE_HALO = {0x3E2A2A, 0x55302A};
    private static final int[] PRIMORDIAL_ICE = {0x88B6DA, 0xA2C8E6, 0xBAD8F0, 0xD0E6F8, 0xE4F1FC, 0xF6FBFF};
    private static final int PRIMORDIAL_DEEP = 0x6A9AC6;

    /**
     * How a texture moves: {@code frames} frames of {@code frametime} ticks, optionally interpolated; {@code glow} is
     * the colour bright frames mix towards and {@code dim} the tone dim frames lean towards (per material).
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

    /** Pulse used by glowing ores, crystals and cracks, as in ring 1 and 2: 8 frames, frame time 4, interpolated. */
    private static final int PULSE_FRAMES = 8;
    private static final int PULSE_FRAMETIME = 4;

    private static final Map<String, Texture> TEXTURES = new LinkedHashMap<>();

    /** Glow layers and their block, and the glow tones of each block ({@link #checkGlowLayers}). */
    private static final Map<String, String> GLOW_LAYERS = new LinkedHashMap<>();
    private static final Map<String, Set<Integer>> GLOW_TONES = new LinkedHashMap<>();

    /**
     * Which pixels of an image glow, as its painter marked them with {@link #glow} (by image identity; {@link #copy}
     * carries the mask along). The glow layer takes exactly these pixels.
     */
    private static final Map<BufferedImage, boolean[][]> GLOW_MASKS = new IdentityHashMap<>();

    /** First texture of the Nether ring 3 section of the contact sheet. */
    private static final String NETHER_FIRST = "block/cold_lava";

    static {
        // Ring 3: ash, pumice, smouldering stone, charred wood (38).
        still("block/ash_block", Ring3Textures::ashBlock);
        still("block/pumice", Ring3Textures::pumice);
        still("block/polished_pumice", Ring3Textures::polishedPumice);
        still("block/pumice_bricks", (image, random) -> stoneBricks(image, random, PUMICE, PUMICE_PORE[0]));
        still("block/smoldering_stone", Ring3Textures::smolderingStone);
        glowLayer("block/smoldering_stone", EMBER, EMBER_GLOW, EMBER_DIM);
        still("block/charred_log", Ring3Textures::charredLog);
        still("block/charred_log_top", Ring3Textures::charredLogTop);
        still("block/stripped_charred_log", (image, random) -> strippedLog(image, random, CHARRED_WOOD));
        still("block/stripped_charred_log_top", (image, random) -> strippedLogTop(image, random, "block/charred_log_top", CHARRED_WOOD, CHAR));
        still("block/charred_planks", (image, random) -> planks(image, random, CHARRED_WOOD));
        still("block/charred_door_top", (image, random) -> doorTop(image, random, CHARRED_WOOD, CHAR));
        still("block/charred_door_bottom", (image, random) -> doorBottom(image, random, CHARRED_WOOD, CHAR));
        still("block/charred_trapdoor", (image, random) -> trapdoor(image, random, CHARRED_WOOD, CHAR));
        still("item/charred_door", (image, random) -> doorItem(image, random, CHARRED_WOOD, CHAR));
        // Ember woods, fire flora, ash crust, smouldering ash (38b).
        still("block/ember_leaves", Ring3Textures::emberLeaves);
        glowLayer("block/ember_leaves", EMBER, EMBER_GLOW, EMBER_DIM);
        still("block/fire_bloom", Ring3Textures::fireBloom);
        still("block/ash_sprouts", Ring3Textures::ashSprouts);
        for (int stage = 0; stage < 4; stage++) {
            int s = stage;
            still("block/ember_berry_bush_stage" + stage, (image, random) -> emberBerryBush(image, random, s));
        }
        still("block/ash_crust", Ring3Textures::ashCrust);
        still("block/smoldering_ash", Ring3Textures::smolderingAsh);
        glowLayer("block/smoldering_ash", EMBER, EMBER_GLOW, EMBER_DIM);
        // Ores (40).
        still("block/volcanite_ore_overlay", Ring3Textures::volcaniteOreOverlay);
        still("block/sulfur_ore_overlay", Ring3Textures::sulfurOreOverlay);
        pulsing("block/fire_ruby_ore_overlay", FIRE_RUBY_GLOW, FIRE_RUBY_DIM, Ring3Textures::fireRubyOreOverlay);
        still("item/raw_volcanite", Ring3Textures::rawVolcanite);
        still("item/sulfur", Ring3Textures::sulfurItem);
        pulsing("item/fire_ruby", FIRE_RUBY_GLOW, FIRE_RUBY_DIM, Ring3Textures::fireRubyItem);
        // Fire crystal (40b).
        pulsing("block/fire_crystal_block", FIRE_CRYSTAL_GLOW, FIRE_CRYSTAL_DIM, Ring3Textures::fireCrystalBlock);
        pulsing("block/small_fire_crystal_bud", FIRE_CRYSTAL_GLOW, FIRE_CRYSTAL_DIM, (image, random) -> fireCrystalBud(image, random, 1));
        pulsing("block/medium_fire_crystal_bud", FIRE_CRYSTAL_GLOW, FIRE_CRYSTAL_DIM, (image, random) -> fireCrystalBud(image, random, 2));
        pulsing("block/large_fire_crystal_bud", FIRE_CRYSTAL_GLOW, FIRE_CRYSTAL_DIM, (image, random) -> fireCrystalBud(image, random, 3));
        pulsing("block/fire_crystal_cluster", FIRE_CRYSTAL_GLOW, FIRE_CRYSTAL_DIM, (image, random) -> fireCrystalBud(image, random, 4));
        pulsing("item/fire_crystal", FIRE_CRYSTAL_GLOW, FIRE_CRYSTAL_DIM, Ring3Textures::fireCrystalItem);
        // Sulfur crust, fumarole, boiling mud, geyser (46).
        still("block/sulfur_crust", Ring3Textures::sulfurCrust);
        still("block/fumarole_top", Ring3Textures::fumaroleTop);
        still("block/fumarole_side", Ring3Textures::fumaroleSide);
        cycling("block/boiling_mud", 16, 3, (image, random, phase) -> boilingMud(image, random, phase, MUD, MUD_SHEEN, 1.0));
        still("block/geyser_top", Ring3Textures::geyserTop);
        still("block/geyser_side", Ring3Textures::geyserSide);
        // Sulfur fields (46b; 57 replaces the tinted placeholders): coloured boiling mud and sulfur crystals.
        cycling("block/yellow_sulfur_mud", 16, 3, (image, random, phase) -> boilingMud(image, random, phase, YELLOW_MUD, YELLOW_MUD_SHEEN, SULFUR_MUD_SPREAD));
        cycling("block/green_sulfur_mud", 16, 3, (image, random, phase) -> boilingMud(image, random, phase, GREEN_MUD, GREEN_MUD_SHEEN, SULFUR_MUD_SPREAD));
        cycling("block/orange_sulfur_mud", 16, 3, (image, random, phase) -> boilingMud(image, random, phase, ORANGE_MUD, ORANGE_MUD_SHEEN, SULFUR_MUD_SPREAD));
        still("block/small_sulfur_crystal_bud", (image, random) -> sulfurCrystal(image, random, 1));
        still("block/medium_sulfur_crystal_bud", (image, random) -> sulfurCrystal(image, random, 2));
        still("block/large_sulfur_crystal_bud", (image, random) -> sulfurCrystal(image, random, 3));
        still("block/sulfur_crystal", (image, random) -> sulfurCrystal(image, random, 4));

        // Nether ring 3 (51, 52).
        still("block/cold_lava", Ring3Textures::coldLava);
        still("block/polished_cold_lava", Ring3Textures::polishedColdLava);
        still("block/cold_lava_bricks", (image, random) -> stoneBricks(image, random, COLD_LAVA, COLD_LAVA_MORTAR));
        still("block/nether_ice", Ring3Textures::netherIce);
        still("block/packed_nether_ice", Ring3Textures::packedNetherIce);
        still("block/rime_netherrack", Ring3Textures::rimeNetherrack);
        still("block/rimeshroom_stem", Ring3Textures::rimeshroomStem);
        still("block/rimeshroom_stem_top", Ring3Textures::rimeshroomStemTop);
        still("block/stripped_rimeshroom_stem", (image, random) -> strippedLog(image, random, RIME_WOOD));
        still("block/stripped_rimeshroom_stem_top", (image, random) -> strippedLogTop(image, random, "block/rimeshroom_stem_top", RIME_WOOD, RIME_BARK));
        still("block/rimeshroom_planks", (image, random) -> planks(image, random, RIME_WOOD));
        still("block/rimeshroom_door_top", (image, random) -> doorTop(image, random, RIME_WOOD, RIME_BARK));
        still("block/rimeshroom_door_bottom", (image, random) -> doorBottom(image, random, RIME_WOOD, RIME_BARK));
        still("block/rimeshroom_trapdoor", (image, random) -> trapdoor(image, random, RIME_WOOD, RIME_BARK));
        still("item/rimeshroom_door", (image, random) -> doorItem(image, random, RIME_WOOD, RIME_BARK));
        still("block/rimeshroom_cap", Ring3Textures::rimeshroomCap);
        still("block/ice_stalactite_down_base", (image, random) -> iceStalactite(image, random, 12, false));
        still("block/ice_stalactite_down_middle", (image, random) -> iceStalactite(image, random, 10, false));
        still("block/ice_stalactite_down_frustum", (image, random) -> iceStalactite(image, random, 8, false));
        still("block/ice_stalactite_down_tip_merge", (image, random) -> iceStalactite(image, random, 6, false));
        still("block/ice_stalactite_down_tip", (image, random) -> iceStalactite(image, random, 6, true));
        for (String thickness : new String[] {"base", "middle", "frustum", "tip_merge", "tip"}) {
            String down = "block/ice_stalactite_down_" + thickness;
            still("block/ice_stalactite_up_" + thickness, (image, random) -> flippedVertically(image, down));
        }
        still("item/ice_stalactite", Ring3Textures::iceStalactiteItem);
        still("block/glowing_fissure", Ring3Textures::glowingFissure);
        glowLayer("block/glowing_fissure", LAVA, LAVA_GLOW, LAVA[0]);
        still("block/primordial_ice", Ring3Textures::primordialIce);

        // Particles (contract §6, sprites for A): 8×8.
        for (int i = 0; i < 4; i++) {
            int n = i;
            still("particle/ash_flake_" + i, (image, random) -> particle(image, Ring3Textures.ASH_FLAKES[n], Ring3Textures.GREYS));
            still("particle/ember_" + i, (image, random) -> particle(image, Ring3Textures.EMBERS[n], Ring3Textures.GREYS));
            still("particle/steam_puff_" + i, (image, random) -> particle(image, Ring3Textures.STEAM_PUFFS[n], Ring3Textures.GREYS));
            still("particle/ice_glint_" + i, (image, random) -> particle(image, Ring3Textures.ICE_GLINTS[n], Ring3Textures.ICE_SPARK));
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
     * A moving, opaque pattern (boiling mud): {@code frames} frames of {@code frametime} ticks, not interpolated, looping
     * seamlessly because the painter is periodic in {@code phase}. Never for cutout textures.
     */
    private static void cycling(String name, int frames, int frametime, CyclingPainter painter) {
        TEXTURES.put(name, new Texture(null, painter, new Animation(frames, frametime, false, 0, 0, true)));
    }

    /**
     * {@code <block>_glow}: the pixels the block's painter marked as glowing ({@link #glow}), everything else
     * transparent; pulsing towards {@code glow}. {@code tones[1..]} are the colours a glowing pixel may have.
     */
    private static void glowLayer(String block, int[] tones, int glow, int dim) {
        String name = block + "_glow";
        GLOW_LAYERS.put(name, block);
        Set<Integer> glowing = new HashSet<>();
        for (int i = 1; i < tones.length; i++) {
            glowing.add(tones[i]);
        }
        GLOW_TONES.put(block, glowing);
        pulsing(name, glow, dim, (image, random) -> {
            BufferedImage source = renderUnchecked(block);
            boolean[][] mask = glowMask(source);
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    if (mask[x][y]) {
                        image.setRGB(x, y, source.getRGB(x, y));
                    }
                }
            }
        });
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
        checkGlowLayers();
        checkFissureEdges();
        lint();
        writePreview();
    }

    // ---- Checks -------------------------------------------------------------------------------------------------

    /** Textures that are calm or sparse on purpose, with the reason (flatness lint). */
    private static final Map<String, String> FLAT_OK = Map.of();

    private static final List<String> LINT_WARNINGS = new ArrayList<>();

    /**
     * Warns (does not fail) about textures that read flat or half drawn: an opaque block with fewer than 5 colours or one
     * colour on more than half the pixels (vanilla: 6-9 colours, the commonest on 20-37 %), a sprite with fewer than 5
     * colours, an item whose outline is a single colour all round. Glow layers (only the cracks of their block) and
     * particles are skipped; exceptions with their reason in {@link #FLAT_OK}.
     */
    private static void lint() throws IOException {
        for (String name : TEXTURES.keySet()) {
            if (name.startsWith("particle/") || GLOW_LAYERS.containsKey(name) || FLAT_OK.containsKey(name)) {
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
     * 8×8), half-transparent pixels (cutout and mipmaps need alpha 0 or 255), animation frames whose alpha mask differs
     * (interpolation would blend alpha), or a cycling texture that is not opaque.
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
     * Every glowing block agrees with its glow layer: each pixel its painter marked as glowing has a glow tone, no other
     * pixel uses a glow tone (rock painted in an ember colour would look hot but stay dark at night), and the layer is
     * not empty. The glow layer itself is built from the same marks.
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

    // ---- Ash ----------------------------------------------------------------------------------------------------

    /**
     * Ash (38): fine warm-grey powder with soft drifts (broad, tileable noise), small lumps lit from the top left
     * (a light pixel with a shadow pixel below right). 57a-r1: calmer up close: a weaker drift (one blotch per tile) and
     * no off-ladder charcoal/cinder flecks (they made a grid on a plain); layout picked by {@link #bestOf}.
     */
    private static void ashBlock(BufferedImage image, Random random) {
        bestOf(image, random, Ring3Textures::ashBlockLayout);
    }

    private static void ashBlockLayout(BufferedImage image, Random random) {
        double[][] broad = valueNoise(random, 8, 8);
        double[][] mid = valueNoise(random, 4, 4);
        double[][] fine = valueNoise(random, 2, 2);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double t = 0.5 + 0.25 * (broad[x][y] - 0.5) + 0.35 * (mid[x][y] - 0.5) + 0.3 * (fine[x][y] - 0.5)
                        + 0.26 * (random.nextDouble() - 0.5);
                set(image, x, y, shade(ASH, t));
            }
        }
        for (int i = 0; i < 4; i++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            setWrapped(image, x, y, ASH[4]);
            setWrapped(image, x + 1, y + 1, ASH[2]);
        }
    }

    /**
     * Ash crust (38b): the same ash, set into a thin crust of plates with hairline cracks; the plate edge on the lit
     * side curls up (lighter), the one on the far side sinks (darker), and the cracks open into dark holes where they
     * meet. Close to ash on purpose, but readable for a player who watches the ground.
     */
    private static void ashCrust(BufferedImage image, Random random) {
        bestOf(image, random, Ring3Textures::ashCrustLayout);
    }

    private static void ashCrustLayout(BufferedImage image, Random random) {
        Cells cells = Cells.random(random, 7);
        double[] base = new double[cells.count()];
        for (int i = 0; i < base.length; i++) {
            base[i] = 0.46 + 0.22 * random.nextDouble();
        }
        double[][] mid = valueNoise(random, 4, 4);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                Cells.Nearest nearest = cells.nearest(x, y);
                double gap = nearest.secondDistance() - nearest.distance();
                double slope = nearest.dx() + nearest.dy();
                if (gap < 0.32) {
                    set(image, x, y, nearest.secondDistance() < 1.6 ? ASH_CHAR : ASH[0]);
                    continue;
                }
                double light = base[nearest.index()] + 0.26 * (mid[x][y] - 0.5) + 0.24 * (random.nextDouble() - 0.5);
                if (gap < 1.1) {
                    light += slope < 0 ? 0.14 : -0.11;
                }
                set(image, x, y, shade(ASH, light));
            }
        }
    }

    /**
     * Smouldering ash (38b, the glowing ash casts): burnt ash close to plain ash (57a-r1: lighter, it read as holes in the
     * ash) with thin cracks of which about half glow dimly (deep red and orange, rarely a brighter spot), the ash beside a
     * hot crack warmed to a reddish grey, so a cast glows faintly at night.
     */
    private static void smolderingAsh(BufferedImage image, Random random) {
        Cells cells = Cells.random(random, 8);
        double[][] heat = valueNoise(random, 8, 8);
        double[][] mid = valueNoise(random, 4, 4);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                Cells.Nearest nearest = cells.nearest(x, y);
                double gap = nearest.secondDistance() - nearest.distance();
                double h = heat[x][y];
                if (gap < 0.38) {
                    if (h > 0.55) {
                        glow(image, x, y, h > 0.82 ? EMBER[3] : h > 0.68 ? EMBER[2] : EMBER[1]);
                    } else {
                        set(image, x, y, SMOLDER_ASH[0]);
                    }
                    continue;
                }
                double light = 0.55 + 0.3 * (mid[x][y] - 0.5) - 0.25 * (nearest.dx() + nearest.dy()) + 0.24 * (random.nextDouble() - 0.5);
                set(image, x, y, gap < 0.75 && h > 0.68 ? SMOLDER_ASH_HALO : shade(SMOLDER_ASH, light));
            }
        }
    }

    // ---- Pumice -------------------------------------------------------------------------------------------------

    /**
     * Pumice (38): light, warm, porous stone: mottled grain and many vesicles (round and stretched holes). Light from
     * the top left falls into each hole onto its lower right wall; the upper left lip catches it. 57a-r1: fewer, softer
     * pores and less grain (the pores made a grid on a volcano cap), layout picked by {@link #bestOf}.
     */
    private static void pumice(BufferedImage image, Random random) {
        bestOf(image, random, Ring3Textures::pumiceLayout);
    }

    private static void pumiceLayout(BufferedImage image, Random random) {
        double[][] noise = valueNoise(random, 4, 4);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, shade(PUMICE, 0.48 + 0.3 * (noise[x][y] - 0.5) + 0.2 * (random.nextDouble() - 0.5)));
            }
        }
        for (int i = 0; i < 10; i++) {
            double cx = random.nextDouble() * SIZE;
            double cy = random.nextDouble() * SIZE;
            double rx = 0.6 + 0.9 * random.nextDouble();
            double ry = rx * (0.55 + 0.45 * random.nextDouble());
            pore(image, cx, cy, rx, ry, PUMICE, PUMICE_PORE);
        }
    }

    /** One vesicle: dark inside, its lower right inner wall lit, the upper left lip just outside lit (one tone below the top). */
    private static void pore(BufferedImage image, double cx, double cy, double rx, double ry, int[] rock, int[] hole) {
        for (int dy = -3; dy <= 3; dy++) {
            for (int dx = -3; dx <= 3; dx++) {
                int x = (int) Math.floor(cx) + dx;
                int y = (int) Math.floor(cy) + dy;
                double ex = (x + 0.5 - cx) / rx;
                double ey = (y + 0.5 - cy) / ry;
                double d = ex * ex + ey * ey;
                if (d < 1) {
                    setWrapped(image, x, y, ex + ey > 0.45 ? rock[1] : hole[d < 0.4 ? 0 : 1]);
                } else if (d < 2.2 && ex + ey < -0.6) {
                    setWrapped(image, x, y, rock[rock.length - 2]);
                }
            }
        }
    }

    /** Polished pumice: a smooth mottled face with a few filled pores and vanilla's bevel (lit top left, dark bottom right). */
    private static void polishedPumice(BufferedImage image, Random random) {
        double[][] noise = valueNoise(random, 4, 4);
        double[][] broad = valueNoise(random, 8, 8);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, shade(PUMICE, 0.55 + 0.25 * (noise[x][y] - 0.5) + 0.25 * (broad[x][y] - 0.5) + 0.18 * (random.nextDouble() - 0.5)));
            }
        }
        for (int i = 0; i < 6; i++) {
            int x = 2 + random.nextInt(SIZE - 4);
            int y = 2 + random.nextInt(SIZE - 4);
            set(image, x, y, PUMICE[1]);
            if (random.nextBoolean()) {
                set(image, x + 1, y, PUMICE[2]);
            }
        }
        bevel(image, PUMICE);
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
     * Vanilla's stone brick layout (as the canyon stone bricks, 37b-5 A): two rows of 16×8 bricks offset by half a brick,
     * a dark mortar, each brick bevelled (lit top row and left column, shadowed bottom row and right column) over grain,
     * with a few chips on the lit edges.
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

    // ---- Smouldering stone --------------------------------------------------------------------------------------

    /**
     * Smouldering stone (38): near-black lumpy rock (lit from the top left) split by a network of cracks; where a heat
     * field is high ({@link #SMOLDER_HOT_SHARE} of the tile) a crack glows (red-orange, deep orange in the hottest part,
     * which also gets a dark red rim) and warms the rock beside it, elsewhere it is a cold black crack. The glow layer
     * takes the ember pixels. 57a-r1: fewer and thinner cracks, a smaller hot share and no yellow or white-hot tones, so a
     * hot crack reads as a thin glowing line instead of sparks. 57: the heat stays inside the tile ({@link #awayFromEdges}),
     * because the 32 variants of 46b no longer join the wrapping cracks at the edges.
     */
    private static void smolderingStone(BufferedImage image, Random random) {
        bestOf(image, random, Ring3Textures::smolderingStoneLayout);
    }

    private static void smolderingStoneLayout(BufferedImage image, Random random) {
        Cells cells = Cells.random(random, 9);
        double[] base = new double[cells.count()];
        for (int i = 0; i < base.length; i++) {
            base[i] = 0.35 + 0.35 * random.nextDouble();
        }
        // 57a-r1: a mid-sized heat field, so the hot share falls on a few runs of the crack network, not scattered sparks.
        // 57: the heat dies out towards the edges, so with the 32 variants (46b) a glowing crack never ends cut off at an
        // edge where a rotated neighbour's crack does not continue; only cold black cracks reach the edges.
        double[][] heat = ranked(awayFromEdges(blend(valueNoise(random, 4, 4), valueNoise(random, 2, 2), 0.35)));
        double[][] broad = valueNoise(random, 4, 4);
        // the hottest ~55 % of the hot share: deep orange crack with a dark red rim
        double core = 1 - 0.55 * SMOLDER_HOT_SHARE;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                Cells.Nearest nearest = cells.nearest(x, y);
                double gap = nearest.secondDistance() - nearest.distance();
                double slope = nearest.dx() + nearest.dy();
                double h = heat[x][y];
                boolean hot = h > 1 - SMOLDER_HOT_SHARE;
                if (gap < 0.38) {
                    if (hot) {
                        glow(image, x, y, h > core ? EMBER[3] : EMBER[2]);
                    } else {
                        set(image, x, y, SMOLDER_ROCK[0]);
                    }
                    continue;
                }
                if (gap < 0.7 && h > core) {
                    glow(image, x, y, EMBER[1]);
                    continue;
                }
                double light = base[nearest.index()] - 0.4 * slope + 0.2 * (broad[x][y] - 0.5) + 0.1 * random.nextDouble();
                if (gap < 1.4 && slope > 0.1) {
                    light -= 0.15;
                }
                if (gap < 1.3 && hot) {
                    set(image, x, y, SMOLDER_HALO[light > 0.6 ? 2 : light > 0.35 ? 1 : 0]);
                } else {
                    set(image, x, y, shade(SMOLDER_ROCK, light));
                }
            }
        }
    }

    /**
     * {@code field} faded to 0 towards the tile edges: 0 on the outer row, full from three pixels in. Ranked afterwards,
     * the hot share then lies inside the tile.
     */
    private static double[][] awayFromEdges(double[][] field) {
        double[][] faded = new double[SIZE][SIZE];
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                int edge = Math.min(Math.min(x, y), Math.min(SIZE - 1 - x, SIZE - 1 - y));
                faded[x][y] = field[x][y] * smoothstep(Math.min(1, edge / 2.5));
            }
        }
        return faded;
    }

    /** {@code a} mixed with {@code weight} of {@code b}, per pixel. */
    private static double[][] blend(double[][] a, double[][] b, double weight) {
        double[][] mixed = new double[SIZE][SIZE];
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                mixed[x][y] = lerp(a[x][y], b[x][y], weight);
            }
        }
        return mixed;
    }

    /** The values of {@code field} replaced by their rank in [0, 1): the same share of pixels lies above a threshold for every seed. */
    private static double[][] ranked(double[][] field) {
        double[] sorted = new double[SIZE * SIZE];
        for (int x = 0; x < SIZE; x++) {
            System.arraycopy(field[x], 0, sorted, x * SIZE, SIZE);
        }
        java.util.Arrays.sort(sorted);
        double[][] rank = new double[SIZE][SIZE];
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                rank[x][y] = java.util.Arrays.binarySearch(sorted, field[x][y]) / (double) sorted.length;
            }
        }
        return rank;
    }

    /**
     * Paints eight layouts of a cell pattern from the seed and keeps the one that repeats least visibly on a wall
     * ({@link #stripeScore} plus {@link #motifScore}): random cell centres sometimes line up into diagonal bands, or put
     * one bright spot in the tile that a wall repeats as a motif.
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

    /**
     * How strongly one spot of a tile stands out, so that a wall shows the same motif in every tile: the largest
     * difference in mean brightness between the sixteen 4x4 blocks of the tile.
     */
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

    // ---- Charred wood -------------------------------------------------------------------------------------------

    /**
     * Charred log side (38): alligatored charcoal, as burnt wood cracks: columns of 3–5 px, each split into blocks of 2–4
     * rows with deep lengthwise cracks and shallower cross cracks. Each block is lit on its top row and left column, with
     * a silvery charcoal sheen on some lit corners; a few cracks still hold a dull red (cold, not glowing).
     */
    private static void charredLog(BufferedImage image, Random random) {
        int x0 = 0;
        while (x0 < SIZE) {
            int width = Math.min(SIZE - x0, 3 + random.nextInt(3));
            if (SIZE - x0 - width > 0 && SIZE - x0 - width < 3) {
                width = SIZE - x0;
            }
            int y0 = random.nextInt(SIZE);
            int covered = 0;
            while (covered < SIZE) {
                int height = Math.min(SIZE - covered, 3 + random.nextInt(4));
                if (SIZE - covered - height < 3 && SIZE - covered - height > 0) {
                    height = SIZE - covered;
                }
                double tone = 0.3 + 0.4 * random.nextDouble();
                // Cross cracks are shallower than the lengthwise ones: they do not always reach across the column.
                int crossLength = random.nextInt(width);
                for (int dy = 0; dy < height; dy++) {
                    int y = Math.floorMod(y0 + covered + dy, SIZE);
                    for (int dx = 0; dx < width; dx++) {
                        int x = x0 + dx;
                        int color;
                        if (dx == width - 1) {
                            color = CHAR[0];
                        } else if (dy == height - 1 && dx < crossLength) {
                            color = CHAR[1];
                        } else if (dy == 0 || dx == 0) {
                            color = shade(CHAR, tone + 0.32);
                            if ((dy == 0 && dx == 0 || random.nextInt(5) == 0) && random.nextInt(3) == 0) {
                                color = CHAR_SHEEN[random.nextInt(2)];
                            }
                        } else {
                            color = shade(CHAR, tone + 0.14 * (random.nextDouble() - 0.5));
                        }
                        set(image, x, y, color);
                    }
                }
                covered += height;
            }
            x0 += width;
        }
        for (int i = 0; i < 3; i++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            if ((image.getRGB(x, y) & 0xFFFFFF) == CHAR[0]) {
                set(image, x, y, CHAR_EMBER);
            }
        }
    }

    /**
     * Charred log end (38): a charcoal rim (two pixels, checked) burnt into the wood, then dark growth rings in square-ish
     * rounds (as vanilla's log tops) lit towards the top left, three radial heat cracks and a dark pith.
     */
    private static void charredLogTop(BufferedImage image, Random random) {
        double start = random.nextDouble() * 2 * Math.PI;
        double[] cracks = {start, start + 2.1 + 0.3 * random.nextDouble(), start + 4.2 + 0.3 * random.nextDouble()};
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double dx = x - 7.5;
                double dy = y - 7.5;
                double r = 0.55 * Math.max(Math.abs(dx), Math.abs(dy)) + 0.45 * Math.hypot(dx, dy);
                if (r > 6.6) {
                    set(image, x, y, shade(CHAR, 0.25 + 0.5 * random.nextDouble()));
                    continue;
                }
                if (r > 5.7) {
                    set(image, x, y, shade(CHAR, 0.55 + 0.35 * random.nextDouble()));
                    continue;
                }
                double angle = Math.atan2(dy, dx);
                boolean crack = false;
                for (double c : cracks) {
                    double diff = Math.abs(Math.atan2(Math.sin(angle - c), Math.cos(angle - c)));
                    crack |= diff * r < 0.6 && r > 1.5 && r < 5.0;
                }
                if (crack) {
                    set(image, x, y, CHAR[1]);
                } else if (r < 1.2) {
                    set(image, x, y, CHARRED_WOOD[0]);
                } else {
                    double light = 0.45 + 0.22 * Math.cos(r * 2.0) - 0.025 * (dx + dy) + 0.14 * (random.nextDouble() - 0.5);
                    set(image, x, y, shade(CHARRED_WOOD, light));
                }
            }
        }
    }

    // ---- Wood helpers (copied from Ring2Textures.java, 37b) ----------------------------------------------------

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
     * The log's end grain {@code topName} (same pixels) with every pixel in a {@code bark} tone turned into wood (bark
     * that reaches deeper than the rim, such as the charred log's charcoal ring and corners) and a wood rim.
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

    // ---- Ember woods and fire flora -----------------------------------------------------------------------------

    /**
     * Ember leaves (38b, V3 A): dark smouldering crowns with gaps (cutout_mipped, like vanilla leaves), clumps lit on
     * their upper left edge and shadowed on their lower right edge, and scattered glowing specks: single embers, a few
     * pairs, one hot spot. The glow layer takes the ember pixels (emissive, no block light).
     */
    private static void emberLeaves(BufferedImage image, Random random) {
        double[][] clumps = valueNoise(random, 4, 4);
        boolean[][] leaf = new boolean[SIZE][SIZE];
        // Small leaves of 3-4 px (lit tip top left, mid body, shadowed base bottom right), piled with wrapping; the
        // broad noise thins them out in places so the crown has holes like vanilla's leaves.
        for (int i = 0; i < EMBER_LEAF_COUNT; i++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            if (clumps[x][y] < 0.3 && random.nextBoolean()) {
                continue;
            }
            int[][] shape = LEAFLETS[random.nextInt(LEAFLETS.length)];
            double tone = 0.3 * clumps[x][y] + 0.15 * random.nextDouble();
            for (int j = 0; j < shape.length; j++) {
                int px = Math.floorMod(x + shape[j][0], SIZE);
                int py = Math.floorMod(y + shape[j][1], SIZE);
                double light = tone + (j == 0 ? 0.55 : j == shape.length - 1 ? 0.0 : 0.3);
                set(image, px, py, shade(EMBER_LEAVES, light));
                leaf[px][py] = true;
            }
        }
        // Embers: one hot spot, three pairs, singles; only on leaf pixels (the glow layer must lie on the leaves).
        int placed = 0;
        for (int attempt = 0; attempt < 400 && placed < 11; attempt++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            if (!leaf[x][y]) {
                continue;
            }
            if (placed == 0) {
                glow(image, x, y, EMBER[4]);
                setIfLeaf(image, leaf, x + 1, y, EMBER[3]);
                setIfLeaf(image, leaf, x, y + 1, EMBER[2]);
            } else if (placed < 4) {
                glow(image, x, y, EMBER[3]);
                setIfLeaf(image, leaf, x + 1, y + 1, EMBER[1]);
            } else {
                glow(image, x, y, EMBER[placed % 2 == 0 ? 2 : 1]);
            }
            placed++;
        }
    }

    /** Leaf shapes for {@link #emberLeaves}: offsets from the lit tip to the shadowed base. */
    private static final int[][][] LEAFLETS = {
            {{0, 0}, {1, 0}, {1, 1}},
            {{0, 0}, {0, 1}, {1, 1}},
            {{0, 0}, {1, 0}, {0, 1}, {1, 1}},
            {{1, 0}, {0, 1}, {1, 1}, {2, 1}},
            {{0, 0}, {1, 1}, {2, 2}}};
    private static final int EMBER_LEAF_COUNT = 78;

    private static void setIfLeaf(BufferedImage image, boolean[][] leaf, int x, int y, int rgb) {
        int wx = Math.floorMod(x, SIZE);
        int wy = Math.floorMod(y, SIZE);
        if (leaf[wx][wy]) {
            glow(image, wx, wy, rgb);
        }
    }

    /**
     * Fire bloom (38b): a flower of three flame tongues (the middle one tallest) on a dark stem with two narrow leaves:
     * deep red outside, orange inside, gold to pale yellow at the tips; lit on the left. Cross model, cutout.
     */
    private static void fireBloom(BufferedImage image, Random random) {
        sprite(image, new String[] {
                "................",
                ".......6........",
                "......656.......",
                "..6...545...6...",
                "..5..54443.54...",
                "..45.44432.43...",
                "...4434332432...",
                "...24433332321..",
                "....2433322211..",
                ".....2222111....",
                ".......Gg.......",
                "..LL...Gg.......",
                "...lLL.Gg.LLl...",
                ".....lLGgLl.....",
                ".......Gg.......",
                ".......Gg......."}, Map.of(
                '1', FIRE_BLOOM[0], '2', FIRE_BLOOM[1], '3', FIRE_BLOOM[2], '4', FIRE_BLOOM[3], '5', FIRE_BLOOM[4],
                '6', FIRE_BLOOM[5], 'G', FIRE_STEM[2], 'g', FIRE_STEM[0], 'L', FIRE_STEM[3], 'l', FIRE_STEM[1]));
    }

    /**
     * Ash sprouts (38b): a tuft of thin, dry blades in ash-olive greys from the bottom edge, darker at the foot (two
     * pixels wide, shadowed right), lighter towards pale ash tips, a few bent over. Cross model, cutout.
     */
    private static void ashSprouts(BufferedImage image, Random random) {
        int[] columns = {2, 4, 6, 8, 10, 12, 13};
        for (int column : columns) {
            int x = column + random.nextInt(2) - 1;
            int height = 5 + random.nextInt(7);
            int lean = random.nextInt(3) - 1;
            for (int row = 0; row < height; row++) {
                int y = SIZE - 1 - row;
                int bx = x + (row > height / 2 ? lean : 0) + (row > height - 2 ? lean : 0);
                double t = row / (double) height;
                int tone = row == height - 1 ? 5 : t > 0.7 ? 4 : t > 0.4 ? 3 : 2;
                if (bx >= 0 && bx < SIZE) {
                    set(image, bx, y, ASH_SPROUTS[tone]);
                }
                if (row < 2 && bx + 1 < SIZE) {
                    set(image, bx + 1, y, ASH_SPROUTS[row == 0 ? 0 : 1]);
                }
            }
        }
    }

    /**
     * Ember berry bush (38b, V7 A), stages 0–3 as vanilla's sweet berry bush: 0 a small sprig, 1 the bush (dark olive
     * leaf clumps on charred twigs), 2 the bush with dull orange unripe berries, 3 with glowing-red ripe berries (2×2,
     * a pale highlight top left). Stages 2 and 3 share the bush of stage 1 and the berry spots. Cross model, cutout.
     */
    private static void emberBerryBush(BufferedImage image, Random random, int stage) {
        if (stage >= 2) {
            copy(renderUnchecked("block/ember_berry_bush_stage1"), image);
            Random spots = seeded("block/ember_berry_bush_berries");
            int[] berry = stage == 2 ? BERRY_UNRIPE : BERRY_RIPE;
            int placed = 0;
            for (int attempt = 0; attempt < 300 && placed < 6; attempt++) {
                int x = 2 + spots.nextInt(11);
                int y = 4 + spots.nextInt(9);
                if (!isOpaque(image, x, y) || !isOpaque(image, x + 1, y + 1) || !isOpaque(image, x + 1, y) || !isOpaque(image, x, y + 1)
                        || isBerry(image, x - 1, y) || isBerry(image, x, y - 1) || isBerry(image, x + 2, y) || isBerry(image, x, y + 2)) {
                    continue;
                }
                set(image, x, y, berry[berry.length - 1]);
                set(image, x + 1, y, berry[berry.length - 2]);
                set(image, x, y + 1, berry[berry.length - 2]);
                set(image, x + 1, y + 1, berry[stage == 2 ? 0 : 1]);
                placed++;
            }
            return;
        }
        double cx = 7.5;
        double cy = stage == 0 ? 11.0 : 8.5;
        double radius = stage == 0 ? 4.2 : 6.6;
        double wobble = random.nextDouble() * 2 * Math.PI;
        double[][] noise = valueNoise(random, 4, 4);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double dx = x + 0.5 - cx;
                double dy = (y + 0.5 - cy) * 1.15;
                double edge = radius * (1 + 0.15 * Math.sin(5 * Math.atan2(dy, dx) + wobble)) - 1.4 * noise[x][y];
                if (Math.hypot(dx, dy) > edge || random.nextInt(7) == 0) {
                    continue;
                }
                double light = 0.55 - 0.08 * (dx + dy) + 0.3 * (noise[x][y] - 0.5) + 0.2 * (random.nextDouble() - 0.5);
                set(image, x, y, shade(BERRY_LEAF, light));
            }
        }
        // Charred twigs from the foot into the bush.
        int top = stage == 0 ? 9 : 6;
        for (int y = SIZE - 1; y >= top; y--) {
            set(image, 7, y, BERRY_TWIG[1]);
            set(image, 8, y, BERRY_TWIG[0]);
            if (y < SIZE - 3 && y % 3 == 0) {
                set(image, y % 2 == 0 ? 6 : 9, y - 1, BERRY_TWIG[1]);
            }
        }
    }

    private static boolean isBerry(BufferedImage image, int x, int y) {
        if (!isOpaque(image, x, y)) {
            return false;
        }
        int rgb = image.getRGB(x, y) & 0xFFFFFF;
        for (int[] berry : new int[][] {BERRY_UNRIPE, BERRY_RIPE}) {
            for (int tone : berry) {
                if (tone == rgb) {
                    return true;
                }
            }
        }
        return false;
    }

    // ---- Ores (40) ----------------------------------------------------------------------------------------------

    /**
     * Volcanite overlay (shared by stone, deepslate and basalt): four angular metal chunks of 2–6 px, split into facets
     * lit from the top left (dark metal, a purple-red sheen on the lit facets, a pale glint on the brightest corner) and
     * the selective outline. Matte: no pulse, not emissive.
     */
    private static void volcaniteOreOverlay(BufferedImage image, Random random) {
        chunks(image, random, new double[] {1.8, 1.3, 1.2, 1.0, 0.9}, VOLCANITE);
    }

    /**
     * Angular chunks as the ring 2 sunstone overlay (37b): each a diamond with random stretch and one corner cut off,
     * facet tones from {@code palette} [2..5] by the light from the top left, a glint [6] on the corner nearest the
     * light, outline [1]/[0]; chunks never touch.
     */
    private static void chunks(BufferedImage image, Random random, double[] sizes, int[] palette) {
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
                    int tone = light > 0.5 ? 5 : light > -0.1 ? (dx < dy ? 4 : 5) : light > -0.6 ? 3 : 2;
                    set(chunk, x, y, palette[tone]);
                    if (glintX < 0 || x + y < glintX + glintY) {
                        glintX = x;
                        glintY = y;
                    }
                }
            }
            if (glintX < 0) {
                continue;
            }
            set(chunk, glintX, glintY, palette[6]);
            outlineSelective(chunk, palette[1], palette[0]);
            if (merge(image, chunk)) {
                placed++;
            }
        }
    }

    /**
     * Sulfur overlay (stone and basalt): three or four crusty clusters of small yellow crystals, uneven outline (the
     * radius wobbles), lit from the top left with tiny bright crystal points, the selective outline in dark ochre. Matte.
     */
    private static void sulfurOreOverlay(BufferedImage image, Random random) {
        int target = 4 + random.nextInt(2);
        int placed = 0;
        for (int attempt = 0; attempt < 800 && placed < target; attempt++) {
            double radius = (placed == 0 ? 1.8 : 1.2) + 0.4 * random.nextDouble();
            double cx = 2 + radius + random.nextDouble() * (SIZE - 4 - 2 * radius);
            double cy = 2 + radius + random.nextDouble() * (SIZE - 4 - 2 * radius);
            double wobble = random.nextDouble() * 2 * Math.PI;
            BufferedImage cluster = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            for (int y = 1; y < SIZE - 1; y++) {
                for (int x = 1; x < SIZE - 1; x++) {
                    double dx = x + 0.5 - cx;
                    double dy = y + 0.5 - cy;
                    double edge = radius * (1 + 0.18 * Math.sin(3 * Math.atan2(dy, dx) + wobble));
                    if (Math.hypot(dx, dy) > edge) {
                        continue;
                    }
                    double light = -(dx + dy) / (2 * radius);
                    int tone = (int) Math.round(3.4 + 2.2 * light + 1.1 * (random.nextDouble() - 0.5));
                    set(cluster, x, y, SULFUR[Math.max(2, Math.min(5, tone))]);
                }
            }
            for (int i = 0; i < 2; i++) {
                int x = (int) cx - 1 + random.nextInt(2);
                int y = (int) cy - 1 + random.nextInt(2);
                if (isOpaque(cluster, x, y)) {
                    set(cluster, x, y, SULFUR[6]);
                }
            }
            outlineSelective(cluster, SULFUR[1], SULFUR[0]);
            if (merge(image, cluster)) {
                placed++;
            }
        }
    }

    /**
     * Fire ruby overlay (stone and deepslate): two or three small hexagonal gems with a bright table, lit upper left
     * facets, darker lower right facets, a glint, the selective outline; pulsing and emissive (contract §7).
     */
    private static void fireRubyOreOverlay(BufferedImage image, Random random) {
        int target = 2 + random.nextInt(2);
        int placed = 0;
        for (int attempt = 0; attempt < 800 && placed < target; attempt++) {
            double radius = (placed == 0 ? 2.0 : 1.5) + 0.3 * random.nextDouble();
            double cx = 2 + radius + random.nextDouble() * (SIZE - 4 - 2 * radius);
            double cy = 2 + radius + random.nextDouble() * (SIZE - 4 - 2 * radius);
            BufferedImage gem = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            gem(gem, cx, cy, radius, FIRE_RUBY);
            outlineSelective(gem, FIRE_RUBY[1], FIRE_RUBY[0]);
            if (merge(image, gem)) {
                placed++;
            }
        }
    }

    /**
     * A hexagonal gem centred on (cx, cy): the table (inner hexagon) bright, the crown facets lit on the upper left and
     * darker on the lower right, one glint at the upper left of the table. Palette: [2..6] dark to glint.
     */
    private static void gem(BufferedImage image, double cx, double cy, double radius, int[] palette) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double dx = x + 0.5 - cx;
                double dy = y + 0.5 - cy;
                double hex = Math.max(Math.abs(dy) * 1.15, (Math.abs(dx) * 0.866 + Math.abs(dy) * 0.5) * 1.15);
                if (hex > radius) {
                    continue;
                }
                int tone;
                if (hex < radius * 0.45) {
                    tone = 4;
                } else {
                    double facing = -(dx + dy);
                    tone = facing > 0.5 ? 5 : facing > -0.5 ? 3 : 2;
                }
                set(image, x, y, palette[tone]);
            }
        }
        set(image, (int) Math.floor(cx - radius * 0.3), (int) Math.floor(cy - radius * 0.3), palette[6]);
    }

    /**
     * Raw volcanite: three angular nuggets of dark metal, back to front like vanilla's raw iron, each split into facets
     * by the light from the top left (a purple-red sheen on the lit facets, a glint on the corner nearest the light), a
     * dark seam where a front nugget overlaps a back one; selective outline.
     */
    private static void rawVolcanite(BufferedImage image, Random random) {
        double[][] nuggets = {{10.2, 5.6, 3.4, 0.9, 1.1}, {5.6, 7.4, 4.2, 1.0, 0.85}, {9.4, 10.6, 3.6, 1.15, 0.9}};
        for (double[] nugget : nuggets) {
            nugget(image, nugget[0], nugget[1], nugget[2], nugget[3], nugget[4], RAW_VOLCANITE);
        }
        outlineSelective(image, VOLCANITE[1], VOLCANITE[0]);
    }

    /**
     * One angular nugget centred on (cx, cy): an octagon (diamond and square blended) stretched by (sx, sy), with facet
     * tones from {@code palette} [0..5] by the light from the top left, its lower right rim in the darkest tone (the seam
     * against what lies behind it) and one glint on the lit corner.
     */
    private static void nugget(BufferedImage image, double cx, double cy, double radius, double sx, double sy, int[] palette) {
        int glintX = -1;
        int glintY = -1;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double dx = (x + 0.5 - cx) / sx;
                double dy = (y + 0.5 - cy) / sy;
                double edge = Math.max(Math.abs(dx) + Math.abs(dy), 1.3 * Math.max(Math.abs(dx), Math.abs(dy))) / (1.3 * radius);
                if (edge > 1) {
                    continue;
                }
                double light = -(dx + dy) / radius;
                int tone = light > 0.45 ? 4 : light > 0.0 ? 3 : light > -0.45 ? 2 : 1;
                if (edge > 0.82 && dx + dy > 0) {
                    tone = 0;
                }
                set(image, x, y, palette[tone]);
                if (glintX < 0 || x + y < glintX + glintY) {
                    glintX = x;
                    glintY = y;
                }
            }
        }
        set(image, glintX + 1, glintY + 1, palette[5]);
    }

    /**
     * Sulfur: a crumbly lump of bright yellow crystal crust, lit from the top left, darker ochre in the lower right,
     * a few pale crystal points; selective outline in dark ochre.
     */
    private static void sulfurItem(BufferedImage image, Random random) {
        // Back to front: sulfur grows as small double pyramids.
        bipyramid(image, 10.6, 6.6, 2.6, 4.6);
        bipyramid(image, 5.6, 7.6, 3.0, 5.4);
        bipyramid(image, 9.0, 10.8, 3.4, 3.8);
        outlineSelective(image, SULFUR[1], SULFUR[0]);
    }

    /**
     * One sulfur crystal seen from the side, a rhombus around (cx, cy): the upper left face lit, the upper right face
     * mid, the lower faces in shadow (darkest lower right), a bright ridge on the upper half and a pale tip.
     */
    private static void bipyramid(BufferedImage image, double cx, double cy, double halfWidth, double halfHeight) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double dx = x + 0.5 - cx;
                double dy = y + 0.5 - cy;
                double edge = Math.abs(dx) / halfWidth + Math.abs(dy) / halfHeight;
                if (edge > 1) {
                    continue;
                }
                int tone = dy < 0 ? (dx < -0.5 ? 5 : dx < 0.5 ? 6 : 4) : (dx < 0 ? 3 : 2);
                if (dy >= 0 && edge > 0.75) {
                    tone = dx < 0 ? 2 : 1;
                }
                set(image, x, y, SULFUR[tone]);
            }
        }
        set(image, (int) Math.floor(cx), (int) Math.floor(cy - halfHeight + 0.5), SULFUR[6]);
    }

    /**
     * Fire ruby: a cut gem like vanilla's emerald and diamond, an elongated hexagon with a bright table, lit upper left
     * crown facets, deep red lower right facets and a glint; selective outline; pulsing (not emissive as an item).
     */
    private static void fireRubyItem(BufferedImage image, Random random) {
        sprite(image, new String[] {
                "................",
                "................",
                "......6555......",
                ".....g55554.....",
                "....65566554....",
                "...655g666544...",
                "...5566666443...",
                "...5556666443...",
                "...4455554433...",
                "...3444444332...",
                "....33333322....",
                ".....333222.....",
                "......3222......",
                ".......22.......",
                "................",
                "................"}, Map.of(
                '2', 0x6A0E18, '3', FIRE_RUBY[2], '4', FIRE_RUBY[3], '5', FIRE_RUBY[4], '6', FIRE_RUBY[5], 'g', FIRE_RUBY[6]));
        outlineSelective(image, FIRE_RUBY[1], FIRE_RUBY[0]);
    }

    // ---- Fire crystal (40b) -------------------------------------------------------------------------------------

    /**
     * Fire crystal block (as the amethyst block): flat crystal faces (wrapping cells), each with its own tone by the way it
     * faces the light, edges lit on the upper left of a face and dark on the lower right, a few glints; warm flame
     * colours. Pulsing.
     */
    private static void fireCrystalBlock(BufferedImage image, Random random) {
        Cells cells = Cells.random(random, 8);
        double[] base = new double[cells.count()];
        for (int i = 0; i < base.length; i++) {
            base[i] = 0.3 + 0.45 * random.nextDouble();
        }
        int[] faces = {FIRE_CRYSTAL[1], FIRE_CRYSTAL[2], FIRE_CRYSTAL[3], FIRE_CRYSTAL[4], FIRE_CRYSTAL[5]};
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                Cells.Nearest nearest = cells.nearest(x, y);
                double gap = nearest.secondDistance() - nearest.distance();
                double slope = nearest.dx() + nearest.dy();
                if (gap < 0.5) {
                    set(image, x, y, slope < 0 ? FIRE_CRYSTAL[5] : FIRE_CRYSTAL[1]);
                    continue;
                }
                double light = base[nearest.index()] - 0.25 * slope + 0.08 * random.nextDouble();
                set(image, x, y, shade(faces, light));
            }
        }
        for (int i = 0; i < 4; i++) {
            setWrapped(image, random.nextInt(SIZE), random.nextInt(SIZE), FIRE_CRYSTAL[6]);
        }
    }

    /**
     * Fire crystal stage 1 to 4 (small, medium, large, cluster): 1, 2, 3 and 5 pointed crystals from the bottom edge, as
     * the dew stages (37b), coloured like a flame: deep orange at the foot, gold higher up, pale at the tips.
     */
    private static void fireCrystalBud(BufferedImage image, Random random, int stage) {
        switch (stage) {
            case 1 -> flameSpike(image, 7.5, 1, 5, 0);
            case 2 -> {
                flameSpike(image, 4.5, 1, 6, -1);
                flameSpike(image, 10.5, 1, 8 + random.nextInt(2), 1);
            }
            case 3 -> {
                flameSpike(image, 3.5, 1, 7, -1);
                flameSpike(image, 11.5, 1, 8, 1);
                flameSpike(image, 7.5, 2, 11 + random.nextInt(2), 0);
            }
            default -> {
                flameSpike(image, 2.5, 1, 6 + random.nextInt(2), -2);
                flameSpike(image, 12.5, 1, 7 + random.nextInt(2), 2);
                flameSpike(image, 5.5, 2, 10, -1);
                flameSpike(image, 9.5, 2, 11, 1);
                flameSpike(image, 7.5, 2, 14, 0);
            }
        }
        outlineSelective(image, FIRE_CRYSTAL_OUTLINE_LIGHT, FIRE_CRYSTAL_OUTLINE);
    }

    /**
     * One crystal from the bottom row up: light left edge, lit left facet, a bright ridge, shaded right facet, dark right
     * edge; one tone darker in the lower third (the flame's foot), the tip pale.
     */
    private static void flameSpike(BufferedImage image, double centre, int halfWidth, int height, int lean) {
        for (int row = 0; row < height; row++) {
            int y = SIZE - 1 - row;
            int ridge = (int) Math.round(centre + lean * row / (double) height);
            int fromTop = height - 1 - row;
            int half = Math.min(halfWidth, fromTop * halfWidth / (halfWidth + 1));
            int foot = row < height / 3 ? 1 : 0;
            for (int x = ridge - half; x <= ridge + half; x++) {
                if (x < 0 || x >= SIZE) {
                    continue;
                }
                int tone = x == ridge - half && half > 0 ? 4 : x < ridge ? 3 : x == ridge ? 5 : x == ridge + half ? 1 : 2;
                if (row == 0) {
                    tone = x <= ridge ? 2 : 1;
                }
                set(image, x, y, fromTop == 0 ? FIRE_CRYSTAL[6] : FIRE_CRYSTAL[Math.max(1, tone - foot)]);
            }
        }
    }

    /**
     * Fire crystal item: one leaning crystal (as one of the crystal stages, so the item reads as a piece broken off a
     * cluster), deep orange at the foot and pale at the tip, lifted one pixel off the bottom so the selective outline
     * closes underneath.
     */
    private static void fireCrystalItem(BufferedImage image, Random random) {
        BufferedImage spikes = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        flameSpike(spikes, 6.0, 2, 15, 4);
        for (int y = 1; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                image.setRGB(x, y - 1, spikes.getRGB(x, y));
            }
        }
        outlineSelective(image, FIRE_CRYSTAL_OUTLINE_LIGHT, FIRE_CRYSTAL_OUTLINE);
    }

    // ---- Sulfur crystal (46b, 57) -------------------------------------------------------------------------------

    /**
     * Sulfur crystal stage 1 to 4 (small, medium, large bud, cluster) for the cross model: sulfur grows as stubby
     * prisms with a pyramid tip, not as flames like the fire crystal: 2, 3, 4 and 6 crystals from the bottom edge, the
     * taller ones leaning outwards. Matte and still like the sulfur ore (the block's faint light comes from its light
     * level, not from an emissive texture); selective outline in ochre.
     */
    private static void sulfurCrystal(BufferedImage image, Random random, int stage) {
        switch (stage) {
            case 1 -> {
                sulfurPrism(image, 5.5, 1, 4, 0);
                sulfurPrism(image, 10.5, 1, 3, 0);
            }
            case 2 -> {
                sulfurPrism(image, 3.5, 1, 4, -1);
                sulfurPrism(image, 11.5, 1, 5, 1);
                sulfurPrism(image, 7.5, 1, 7 + random.nextInt(2), 0);
            }
            case 3 -> {
                sulfurPrism(image, 2.5, 1, 5, -1);
                sulfurPrism(image, 12.5, 1, 6, 1);
                sulfurPrism(image, 9.5, 1, 8, 1);
                sulfurPrism(image, 5.5, 2, 10 + random.nextInt(2), 0);
            }
            default -> {
                sulfurPrism(image, 1.5, 1, 5, -1);
                sulfurPrism(image, 13.5, 1, 6, 1);
                sulfurPrism(image, 4.5, 1, 9, -2);
                sulfurPrism(image, 11.5, 1, 10, 2);
                sulfurPrism(image, 9.5, 2, 13, 1);
                sulfurPrism(image, 6.5, 2, 12, -1);
            }
        }
        outlineSelective(image, SULFUR[1], SULFUR[0]);
    }

    /**
     * One sulfur crystal from the bottom row up: a prism of constant width (lit left facet, a bright ridge, shaded right
     * facet) ending in a pyramid tip over its top {@code halfWidth + 1} rows; ochre at the foot, a pale point.
     */
    private static void sulfurPrism(BufferedImage image, double centre, int halfWidth, int height, int lean) {
        int tip = halfWidth + 1;
        for (int row = 0; row < height; row++) {
            int y = SIZE - 1 - row;
            int ridge = (int) Math.round(centre + lean * row / (double) height);
            int fromTop = height - 1 - row;
            int half = fromTop < tip ? fromTop * halfWidth / tip : halfWidth;
            for (int x = ridge - half; x <= ridge + half; x++) {
                if (x < 0 || x >= SIZE) {
                    continue;
                }
                int tone = x < ridge ? (x == ridge - half ? 5 : 4) : x == ridge ? 5 : x == ridge + half ? 2 : 3;
                if (fromTop < tip && x > ridge) {
                    // the pyramid's right face catches more light than the prism's shaded side
                    tone = 3;
                }
                if (row < 2) {
                    tone = Math.max(1, tone - 2);
                }
                set(image, x, y, fromTop == 0 ? SULFUR[6] : SULFUR[tone]);
            }
        }
    }

    // ---- Sulfur crust, fumarole, boiling mud, geyser (46) -------------------------------------------------------

    /**
     * Sulfur crust (46): nodular yellow crust (wrapping cells lit from the top left) with ochre crevices, orange-brown
     * stains in broad patches and a few pale crystal sparkles on the lit sides of the nodules. 57a-r1: calmer (softer
     * nodule light, shallower crevices, fewer stains), the scaly plates and brown crescents repeated on a terrace.
     */
    private static void sulfurCrust(BufferedImage image, Random random) {
        bestOf(image, random, Ring3Textures::sulfurCrustLayout);
    }

    private static void sulfurCrustLayout(BufferedImage image, Random random) {
        Cells cells = Cells.random(random, 14);
        double[] base = new double[cells.count()];
        for (int i = 0; i < base.length; i++) {
            base[i] = 0.45 + 0.18 * random.nextDouble();
        }
        double[][] stain = valueNoise(random, 4, 4);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                Cells.Nearest nearest = cells.nearest(x, y);
                double gap = nearest.secondDistance() - nearest.distance();
                double slope = nearest.dx() + nearest.dy();
                if (gap < 0.34) {
                    set(image, x, y, SULFUR_CRUST[0]);
                    continue;
                }
                double light = base[nearest.index()] - 0.22 * slope + 0.12 * (random.nextDouble() - 0.5);
                if (gap < 1.1 && slope > 0.1) {
                    light -= 0.1;
                }
                int color = shade(SULFUR_CRUST, light);
                if (stain[x][y] > 0.84 && random.nextInt(3) != 0) {
                    color = SULFUR_STAIN[light > 0.5 ? 1 : 0];
                }
                if (slope < -0.3 && gap > 1.2 && random.nextInt(14) == 0) {
                    color = SULFUR_SPARKLE;
                }
                set(image, x, y, color);
            }
        }
    }

    /**
     * Fumarole top (46): sulfur crust around a central vent: a black throat, a dark ochre inner wall lit on its lower
     * right side (light falls in from the top left), and a rim of bright, freshly grown crystals that fades into the
     * crust.
     */
    private static void fumaroleTop(BufferedImage image, Random random) {
        copy(renderUnchecked("block/sulfur_crust"), image);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double dx = x + 0.5 - 8.0;
                double dy = y + 0.5 - 8.0;
                double r = Math.hypot(dx, dy) * (1 + 0.08 * Math.sin(3 * Math.atan2(dy, dx)));
                if (r < 1.3) {
                    set(image, x, y, VENT[0]);
                } else if (r < 2.2) {
                    set(image, x, y, dx + dy > 0.6 ? VENT[2] : VENT[1]);
                } else if (r < 3.6) {
                    double light = 0.65 - 0.12 * (dx + dy) + 0.3 * (random.nextDouble() - 0.5);
                    set(image, x, y, light > 0.85 && random.nextInt(3) == 0 ? SULFUR[6] : SULFUR[Math.max(2, Math.min(5, (int) Math.round(2 + 3.5 * light)))]);
                } else if (r < 4.6 && random.nextBoolean()) {
                    set(image, x, y, SULFUR[3 + random.nextInt(2)]);
                }
            }
        }
    }

    /**
     * Fumarole side (46): dark, lumpy volcanic rock with a ragged sulfur crust on top, yellow streaks running down from it
     * and an orange-brown stain below each streak.
     */
    private static void fumaroleSide(BufferedImage image, Random random) {
        Cells cells = Cells.random(random, 9);
        double[] base = new double[cells.count()];
        for (int i = 0; i < base.length; i++) {
            base[i] = 0.35 + 0.35 * random.nextDouble();
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                Cells.Nearest nearest = cells.nearest(x, y);
                double gap = nearest.secondDistance() - nearest.distance();
                double slope = nearest.dx() + nearest.dy();
                double light = gap < 0.42 ? 0.05 : base[nearest.index()] - 0.4 * slope + 0.1 * random.nextDouble();
                set(image, x, y, shade(FUMAROLE_ROCK, light));
            }
        }
        for (int x = 0; x < SIZE; x++) {
            int depth = 2 + random.nextInt(3);
            for (int y = 0; y < depth; y++) {
                set(image, x, y, y == depth - 1 ? SULFUR_CRUST[1] : shade(SULFUR_CRUST, 0.6 + 0.4 * random.nextDouble() - 0.2 * y));
            }
            if (random.nextInt(4) == 0) {
                int length = 2 + random.nextInt(6);
                for (int j = 0; j < length; j++) {
                    set(image, x, depth + j, j == length - 1 ? SULFUR_STAIN[0] : SULFUR_CRUST[3 - j % 2]);
                }
                if (depth + length < SIZE) {
                    set(image, x, depth + length, SULFUR_STAIN[0]);
                }
            }
        }
    }

    /**
     * Boiling mud (46): grey-brown mud with a soft sheen and four bubble spots, each on its own beat: a dome grows (lit
     * top left, dark rim lower right), pops into a dark ring with splashes, a fading ripple, then rest. Periodic in
     * {@code phase}, so the loop is seamless; opaque. The coloured sulfur muds (46b, 57) are the same mud in their own
     * {@code mud} palette (dark to light, six tones), {@code sheen} and tone {@code spread} (1 for the grey mud).
     */
    private static void boilingMud(BufferedImage image, Random random, double phase, int[] mud, int sheen, double spread) {
        double[][] broad = valueNoise(random, 8, 8);
        double[][] mid = valueNoise(random, 4, 4);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double t = 0.45 + spread * (0.35 * (broad[x][y] - 0.5) + 0.3 * (mid[x][y] - 0.5) + 0.2 * (random.nextDouble() - 0.5));
                set(image, x, y, shade(mud, t));
            }
        }
        for (int i = 0; i < 4; i++) {
            double cx = random.nextDouble() * SIZE;
            double cy = random.nextDouble() * SIZE;
            double offset = i / 4.0 + 0.1 * random.nextDouble();
            double maxRadius = 1.4 + 1.2 * random.nextDouble();
            double local = (phase + offset) % 1.0;
            for (int dy = -4; dy <= 4; dy++) {
                for (int dx = -4; dx <= 4; dx++) {
                    int x = (int) Math.floor(cx) + dx;
                    int y = (int) Math.floor(cy) + dy;
                    double ox = x + 0.5 - cx;
                    double oy = y + 0.5 - cy;
                    double d = Math.hypot(ox, oy);
                    if (local < 0.55) {
                        double r = 0.6 + maxRadius * local / 0.55;
                        if (d < r) {
                            double light = -(ox + oy) / r;
                            setWrapped(image, x, y, light > 0.6 ? sheen : light > -0.2 ? mud[4] : d > r - 0.8 ? mud[1] : mud[3]);
                        }
                    } else if (local < 0.7) {
                        if (Math.abs(d - maxRadius) < 0.6) {
                            setWrapped(image, x, y, mud[0]);
                        } else if (Math.abs(d - maxRadius - 1.2) < 0.5 && Math.floorMod(dx * 7 + dy * 3, 4) == 0) {
                            setWrapped(image, x, y, mud[5]);
                        }
                    } else if (local < 0.82 && Math.abs(d - maxRadius - 0.8) < 0.5) {
                        setWrapped(image, x, y, mud[3]);
                    }
                }
            }
        }
    }

    /**
     * Geyser top (46): a vent of dark water in the middle, a dark throat rim, then pale siliceous sinter in concentric
     * terraces lit from the top left, with orange-brown mineral stains running out along a few rays.
     */
    private static void geyserTop(BufferedImage image, Random random) {
        double[] rays = {random.nextDouble() * 2 * Math.PI, random.nextDouble() * 2 * Math.PI, random.nextDouble() * 2 * Math.PI};
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double dx = x + 0.5 - 8.0;
                double dy = y + 0.5 - 8.0;
                double r = Math.hypot(dx, dy);
                if (r < 1.9) {
                    set(image, x, y, dx + dy < -1.2 ? GEYSER_WATER[3] : dx + dy < 0 ? GEYSER_WATER[2] : r < 1.2 ? GEYSER_WATER[0] : GEYSER_WATER[1]);
                    continue;
                }
                if (r < 2.8) {
                    set(image, x, y, dx + dy > 0 ? SINTER[2] : SINTER[0]);
                    continue;
                }
                double terrace = Math.cos(r * 2.1);
                double light = 0.55 + 0.18 * terrace - 0.03 * (dx + dy) + 0.2 * (random.nextDouble() - 0.5);
                int color = shade(SINTER, light);
                double angle = Math.atan2(dy, dx);
                for (double ray : rays) {
                    double diff = Math.abs(Math.atan2(Math.sin(angle - ray), Math.cos(angle - ray)));
                    if (diff * r < 0.9 && random.nextInt(3) != 0) {
                        color = SULFUR_STAIN[terrace > 0 ? 1 : 0];
                    }
                }
                set(image, x, y, color);
            }
        }
    }

    /**
     * Geyser side (46): sinter laid down in thin wavy layers (short and broken, never a full-width line), a few darker
     * runnels down the face, mineral stain under the rim.
     */
    private static void geyserSide(BufferedImage image, Random random) {
        double wobble = random.nextDouble() * 2 * Math.PI;
        double[][] patches = valueNoise(random, 4, 4);
        int[] runnels = {random.nextInt(SIZE), random.nextInt(SIZE)};
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double layer = Math.sin(2 * Math.PI * (y + 1.3 * Math.sin(2 * Math.PI * x / SIZE + wobble)) / 4.0);
                double light = 0.5 + 0.2 * layer + 0.3 * (patches[x][y] - 0.5) + 0.18 * (random.nextDouble() - 0.5);
                if (x == runnels[0] || x == runnels[1]) {
                    light -= 0.25;
                }
                int color = shade(SINTER, light);
                if (y < 3 && random.nextInt(3) == 0) {
                    color = SULFUR_STAIN[random.nextInt(2)];
                }
                set(image, x, y, color);
            }
        }
    }

    // ---- Nether ring 3 ------------------------------------------------------------------------------------------

    /**
     * Cold lava (51): solidified pahoehoe, grey-blue. Plates (wrapping cells) each covered in ropy folds running in their
     * own direction and curving, folds lit on the side facing the top left; dark cracks between the plates, a few
     * vesicles.
     */
    private static void coldLava(BufferedImage image, Random random) {
        bestOf(image, random, Ring3Textures::coldLavaLayout);
    }

    private static void coldLavaLayout(BufferedImage image, Random random) {
        Cells cells = Cells.random(random, 6);
        double[] angle = new double[cells.count()];
        double[] phase = new double[cells.count()];
        double[] base = new double[cells.count()];
        for (int i = 0; i < cells.count(); i++) {
            angle[i] = random.nextDouble() * Math.PI;
            phase[i] = random.nextDouble() * 2 * Math.PI;
            base[i] = 0.4 + 0.25 * random.nextDouble();
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                Cells.Nearest nearest = cells.nearest(x, y);
                double gap = nearest.secondDistance() - nearest.distance();
                if (gap < 0.45) {
                    set(image, x, y, COLD_LAVA[0]);
                    continue;
                }
                int i = nearest.index();
                double ox = nearest.dx() * 4;
                double oy = nearest.dy() * 4;
                double cos = Math.cos(angle[i]);
                double sin = Math.sin(angle[i]);
                double u = ox * cos + oy * sin;
                double v = -ox * sin + oy * cos;
                double s = u + 0.12 * v * v;
                double rope = Math.sin(2 * Math.PI * s / 3.2 + phase[i]);
                double lit = Math.cos(2 * Math.PI * s / 3.2 + phase[i]) * -(cos + sin) / Math.sqrt(2);
                double slope = nearest.dx() + nearest.dy();
                double light = base[i] + 0.15 * rope + 0.22 * lit - 0.2 * slope + 0.1 * (random.nextDouble() - 0.5);
                if (gap < 1.0 && slope > 0.1) {
                    light -= 0.15;
                }
                set(image, x, y, shade(COLD_LAVA, light));
            }
        }
        for (int i = 0; i < 4; i++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            setWrapped(image, x, y, COLD_LAVA[1]);
            setWrapped(image, x + 1, y + 1, COLD_LAVA[5]);
        }
    }

    /** Polished cold lava: a smooth, mottled grey-blue face with vanilla's bevel. */
    private static void polishedColdLava(BufferedImage image, Random random) {
        double[][] noise = valueNoise(random, 4, 4);
        double[][] broad = valueNoise(random, 8, 8);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                set(image, x, y, shade(COLD_LAVA, 0.5 + 0.25 * (noise[x][y] - 0.5) + 0.25 * (broad[x][y] - 0.5) + 0.15 * (random.nextDouble() - 0.5)));
            }
        }
        bevel(image, COLD_LAVA);
    }

    /**
     * Nether ice (51): opaque, smoky grey-blue ice, darker and duller than vanilla's ice so it belongs in the Nether:
     * soft depth (broad noise), two short diagonal sheen streaks from the top left, white fracture lines with a shadow
     * beside them, trapped bubbles and a dark inclusion.
     */
    private static void netherIce(BufferedImage image, Random random) {
        double[][] broad = valueNoise(random, 8, 8);
        double[][] mid = valueNoise(random, 4, 4);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double t = 0.5 + 0.25 * (broad[x][y] - 0.5) + 0.5 * (mid[x][y] - 0.5) + 0.24 * (random.nextDouble() - 0.5);
                set(image, x, y, shade(NETHER_ICE, t));
            }
        }
        for (int i = 0; i < 2; i++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            int length = 4 + random.nextInt(4);
            for (int j = 0; j < length; j++) {
                setWrapped(image, x + j, y - j, NETHER_ICE[5 + (j > 0 && j < length - 1 ? 1 : 0)]);
            }
        }
        for (int i = 0; i < 1; i++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            int length = 5 + random.nextInt(4);
            int dx = random.nextBoolean() ? 1 : -1;
            for (int j = 0; j < length; j++) {
                int px = x + j * dx;
                int py = y + j / 2 + (j % 3 == 0 ? 1 : 0);
                setWrapped(image, px, py, j % 3 == 1 ? NETHER_ICE_CRACK : NETHER_ICE[5]);
                setWrapped(image, px, py + 1, NETHER_ICE[2]);
            }
        }
        for (int i = 0; i < 5; i++) {
            setWrapped(image, random.nextInt(SIZE), random.nextInt(SIZE), NETHER_ICE[6]);
        }
        int x = random.nextInt(SIZE);
        int y = random.nextInt(SIZE);
        setWrapped(image, x, y, NETHER_ICE_DEEP);
        setWrapped(image, x + 1, y, NETHER_ICE_DEEP);
        setWrapped(image, x, y + 1, NETHER_ICE[0]);
    }

    /**
     * Packed Nether ice (51): compressed chunks (wrapping cells) of darker, denser ice, each with its own tone, a lit
     * upper left edge, fine dark seams; a few white specks.
     */
    private static void packedNetherIce(BufferedImage image, Random random) {
        bestOf(image, random, Ring3Textures::packedNetherIceLayout);
    }

    private static void packedNetherIceLayout(BufferedImage image, Random random) {
        Cells cells = Cells.random(random, 10);
        double[] base = new double[cells.count()];
        for (int i = 0; i < base.length; i++) {
            base[i] = 0.35 + 0.4 * random.nextDouble();
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                Cells.Nearest nearest = cells.nearest(x, y);
                double gap = nearest.secondDistance() - nearest.distance();
                double slope = nearest.dx() + nearest.dy();
                if (gap < 0.35) {
                    set(image, x, y, PACKED_NETHER_ICE[0]);
                    continue;
                }
                double light = base[nearest.index()] - 0.2 * slope + 0.12 * (random.nextDouble() - 0.5);
                if (gap < 1.0) {
                    light += slope < 0 ? 0.2 : -0.12;
                }
                set(image, x, y, shade(PACKED_NETHER_ICE, light));
            }
        }
        for (int i = 0; i < 4; i++) {
            setWrapped(image, random.nextInt(SIZE), random.nextInt(SIZE), NETHER_ICE[6]);
        }
    }

    /**
     * Rime netherrack (51): netherrack lumps on vanilla netherrack's tone ladder (so vanilla's quartz and gold ores sit in
     * it without a square) under thin frost: mauve rime on the exposed upper left of some lumps and in some crevices,
     * pale frost on fewer pixels; no white glints (one per tile reads as a grid of dots on a wall). Of eight layouts from the
     * seed the one with the least visible repetition is kept ({@link #stripeScore}).
     */
    private static void rimeNetherrack(BufferedImage image, Random random) {
        bestOf(image, random, Ring3Textures::rimeNetherrackLayout);
    }

    private static void rimeNetherrackLayout(BufferedImage image, Random random) {
        Cells cells = Cells.random(random, 16);
        double[] base = new double[cells.count()];
        boolean[] frosted = new boolean[cells.count()];
        for (int i = 0; i < base.length; i++) {
            base[i] = RIME_ROCK_BASE + 0.35 * random.nextDouble();
            frosted[i] = random.nextInt(5) < 3;
        }
        double[][] broad = valueNoise(random, 8, 8);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                Cells.Nearest nearest = cells.nearest(x, y);
                double gap = nearest.secondDistance() - nearest.distance();
                double slope = nearest.dx() + nearest.dy();
                if (gap < 0.45) {
                    set(image, x, y, random.nextInt(5) == 0 ? RIME[0] : RIME_NETHERRACK[0]);
                    continue;
                }
                double light = base[nearest.index()] + 0.2 * (broad[x][y] - 0.5) - 0.3 * slope + 0.08 * random.nextDouble();
                if (gap < 1.1 && slope > 0.15) {
                    light -= 0.12;
                }
                int color = shade(RIME_NETHERRACK, light);
                if (frosted[nearest.index()] && slope < -0.15 && gap > 0.7) {
                    int roll = random.nextInt(10);
                    color = roll < 3 ? RIME[0] : roll < 5 ? RIME[1] : color;
                }
                set(image, x, y, color);
            }
        }
    }

    /**
     * Rimeshroom stem (51): frozen fungus bark in cold blue-grey with fibres that sway (a sine across the height, so the
     * tile repeats seamlessly), grooves lit on their right wall, frost settled in the grooves and a few frost specks on
     * the ridges.
     */
    private static void rimeshroomStem(BufferedImage image, Random random) {
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
                    set(image, x, y, random.nextInt(7) == 0 ? FROST[0] : RIME_BARK[0]);
                    continue;
                }
                if (groove[Math.floorMod(column - 1, SIZE)]) {
                    light += 0.3;
                } else if (groove[Math.floorMod(column + 1, SIZE)]) {
                    light -= 0.15;
                }
                int color = shade(RIME_BARK, light);
                if (light > 0.8 && random.nextInt(8) == 0) {
                    color = FROST[0];
                }
                set(image, x, y, color);
            }
        }
    }

    /** Rimeshroom stem end: rings of pale lilac flesh around a darker pith, lit to the top left, a frosted bark rim. */
    private static void rimeshroomStemTop(BufferedImage image, Random random) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double radius = Math.hypot(x - 7.5, y - 7.5);
                double light = 0.55 + 0.25 * Math.cos(radius * 1.6) - 0.02 * (x + y - 15) + 0.15 * (random.nextDouble() - 0.5);
                set(image, x, y, radius < 1.5 ? RIME_WOOD[1] : shade(RIME_WOOD, light));
            }
        }
        rim(image, random, RIME_BARK, 0.0, 0.7);
        for (int i = 0; i < 5; i++) {
            int along = random.nextInt(SIZE);
            set(image, along, 0, FROST[random.nextInt(2)]);
            set(image, 0, along, FROST[0]);
        }
    }

    /**
     * Rimeshroom cap (51): fleshy lumps like vanilla's wart blocks in cold violet-blue, lit from the top left and
     * shadowed along their lower right edge, dark seams, frost crystals on the lit tops.
     */
    private static void rimeshroomCap(BufferedImage image, Random random) {
        bestOf(image, random, Ring3Textures::rimeshroomCapLayout);
    }

    private static void rimeshroomCapLayout(BufferedImage image, Random random) {
        Cells cells = Cells.random(random, 12);
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
                    set(image, x, y, RIME_CAP[0]);
                    continue;
                }
                double light = base[nearest.index()] - 0.45 * slope + 0.1 * random.nextDouble();
                if (gap < 1.3 && slope > 0.1) {
                    light -= 0.15;
                }
                int color = shade(RIME_CAP, light);
                if (slope < -0.35 && random.nextInt(6) == 0) {
                    color = FROST[random.nextInt(2)];
                }
                set(image, x, y, color);
            }
        }
    }

    /**
     * An ice stalactite hanging from the top of the tile; {@code up} variants are these flipped. Widths follow the block
     * shapes (base 12, middle 10, frustum 8, merged tip 6 px), the lone tip tapers to a point after 11 rows. Lit on the
     * left with a broken gloss line, shadowed on the right, trapped bubbles; the left edge sways a pixel.
     */
    private static void iceStalactite(BufferedImage image, Random random, int width, boolean taper) {
        double sway = random.nextDouble() * 2 * Math.PI;
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
                int color = x == right ? ICE_STALACTITE[0] : shade(ICE_STALACTITE, 0.9 - 0.8 * t + 0.12 * random.nextDouble());
                if (x == left + 1 && right - left > 2 && random.nextInt(4) != 0) {
                    color = ICE_GLOSS;
                } else if (x > left + 1 && x < right - 1 && random.nextInt(14) == 0) {
                    color = ICE_STALACTITE[4];
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
     * The ice stalactite in the inventory, slender like vanilla's pointed dripstone: from a broad root to a fine tip, a
     * gloss line down its lit left side, a shadowed right side, two ring ridges, a selective outline.
     */
    private static void iceStalactiteItem(BufferedImage image, Random random) {
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
                '0', ICE_STALACTITE[0], '1', ICE_STALACTITE[1], '2', ICE_STALACTITE[2], '3', ICE_STALACTITE[3],
                '4', ICE_STALACTITE[4], 'G', ICE_GLOSS));
        outlineSelective(image, ICE_STALACTITE[1], ICE_STALACTITE_OUTLINE);
    }

    /**
     * Glowing fissure (51): dark cold-lava crust split by an irregular fissure of live lava (a small yellow core where it
     * is widest, orange body, red edge) that reaches the middle of all four edges ({@link #fissureSegments}); the crust
     * beside it is warmed (non-glowing), the crust far from it carries a little rime. The glow layer takes the lava
     * pixels. 57a-r1: narrower and a step darker (no white-hot tone). 57: the edge points lie in the middle of every edge
     * with the same hairline profile, so the crack joins its neighbour in each of the 32 variants (46b) and a patch of
     * fissure blocks reads as one irregular network.
     */
    private static void glowingFissure(BufferedImage image, Random random) {
        bestOf(image, random, Ring3Textures::glowingFissureLayout);
    }

    private static void glowingFissureLayout(BufferedImage image, Random random) {
        Cells cells = Cells.random(random, 8);
        double[] base = new double[cells.count()];
        for (int i = 0; i < base.length; i++) {
            base[i] = 0.25 + 0.35 * random.nextDouble();
        }
        List<double[]> segments = fissureSegments(random);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double[] near = nearestSegment(segments, x + 0.5, y + 0.5);
                double d = near[0];
                double width = near[1];
                double v = d / width;
                int edgeDistance = Math.min(Math.min(x, y), Math.min(SIZE - 1 - x, SIZE - 1 - y));
                if (edgeDistance < FISSURE_CRUSTED && d < width + 0.5) {
                    // 57: crusted over where it meets the edge (not glowing): the crack runs on into every neighbour,
                    // but at night a patch shows glowing pieces with dark seams instead of a lattice of bright lines
                    set(image, x, y, LAVA[0]);
                } else if (v < 0.45 && width > 0.5) {
                    // a small bright core only where the main crack is widest (its middle width is 0.45-0.8)
                    glow(image, x, y, LAVA[4]);
                } else if (v < 0.6) {
                    glow(image, x, y, LAVA[3]);
                } else if (v < 1.0) {
                    glow(image, x, y, LAVA[2]);
                } else if (d < width + 0.5) {
                    glow(image, x, y, LAVA[1]);
                } else {
                    Cells.Nearest nearest = cells.nearest(x, y);
                    double gap = nearest.secondDistance() - nearest.distance();
                    double light = gap < 0.42 ? 0.0 : base[nearest.index()] - 0.35 * (nearest.dx() + nearest.dy()) + 0.1 * random.nextDouble();
                    int color = shade(COLD_LAVA, light);
                    if (d < width + 1.8) {
                        // one halo tone on the edge rows, so the halo joins the neighbour's in every variant
                        boolean edge = x == 0 || y == 0 || x == SIZE - 1 || y == SIZE - 1;
                        color = FISSURE_HALO[!edge && light > 0.4 ? 1 : 0];
                    } else if (light > 0.55 && random.nextInt(7) == 0) {
                        color = RIME[1];
                    }
                    set(image, x, y, color);
                }
            }
        }
    }

    /**
     * The fissure as segments {x1, y1, x2, y2, width1, width2}. Four straight stubs ({@link #FISSURE_STUB} long, width
     * {@link #FISSURE_EDGE_WIDTH}) leave the middle of each edge square to it, so every edge shows the same, mirror-
     * symmetric hairline and a rotated or mirrored neighbour joins it. Inside the tile ({@link #FISSURE_MARGIN} from the
     * edges) the left and right stubs run into the main crack through two junctions, where it is widest; the top and
     * bottom stubs join it as dim, narrow arms, both at the same junction; plus one short branch that kinks off and thins
     * out inside the tile. So the tile is no plain cross: the junctions and the wide part move with the seed.
     */
    private static List<double[]> fissureSegments(Random random) {
        double m = FISSURE_EDGE_POINT;
        double s = FISSURE_STUB;
        double edge = FISSURE_EDGE_WIDTH;
        double stubEnd = edge + 0.1;
        double main = 0.45 + 0.35 * random.nextDouble();
        // the arms stay a dim hairline (red, rarely orange), so a patch does not read as a lattice of bright crosses
        double arm = 0.18 + 0.08 * random.nextDouble();
        List<double[]> segments = new ArrayList<>();
        segments.add(new double[] {0, m, s, m, edge, stubEnd});
        segments.add(new double[] {SIZE, m, SIZE - s, m, edge, stubEnd});
        segments.add(new double[] {m, 0, m, s, edge, stubEnd});
        segments.add(new double[] {m, SIZE, m, SIZE - s, edge, stubEnd});
        // two junctions, the left one in the left half and the right one in the right half
        double[] left = {innerPoint(4.5 + 2 * random.nextDouble()), innerPoint(m + 6 * (random.nextDouble() - 0.5))};
        double[] right = {innerPoint(9.5 + 2 * random.nextDouble()), innerPoint(m + 6 * (random.nextDouble() - 0.5))};
        double leftWidth = lerp(stubEnd, main, 0.6 + 0.4 * random.nextDouble());
        double rightWidth = lerp(stubEnd, main, 0.6 + 0.4 * random.nextDouble());
        segments.add(new double[] {s, m, left[0], left[1], stubEnd, leftWidth});
        double[] middle = {innerPoint(lerp(left[0], right[0], 0.5) + (random.nextDouble() - 0.5)),
                innerPoint(lerp(left[1], right[1], 0.5) + 4 * (random.nextDouble() - 0.5))};
        segments.add(new double[] {left[0], left[1], middle[0], middle[1], leftWidth, main});
        segments.add(new double[] {middle[0], middle[1], right[0], right[1], main, rightWidth});
        segments.add(new double[] {right[0], right[1], SIZE - s, m, rightWidth, stubEnd});
        // both arms join the same junction, each through one wandering node: arms to different junctions with the S of
        // the main crack made a pinwheel that a patch repeated in every tile
        double[] top = random.nextBoolean() ? left : right;
        double[] bottom = top;
        double[] topNode = {innerPoint(lerp(m, top[0], 0.5) + 2 * (random.nextDouble() - 0.5)), innerPoint(lerp(s, top[1], 0.5))};
        double[] bottomNode = {innerPoint(lerp(m, bottom[0], 0.5) + 2 * (random.nextDouble() - 0.5)), innerPoint(lerp(SIZE - s, bottom[1], 0.5))};
        segments.add(new double[] {m, s, topNode[0], topNode[1], stubEnd, arm});
        segments.add(new double[] {topNode[0], topNode[1], top[0], top[1], arm, arm});
        segments.add(new double[] {m, SIZE - s, bottomNode[0], bottomNode[1], stubEnd, arm});
        segments.add(new double[] {bottomNode[0], bottomNode[1], bottom[0], bottom[1], arm, arm});
        // one branch off the main crack's middle, kinking and thinning out
        double turn = 2 * Math.PI * random.nextDouble();
        double reach = 2.5 + 2 * random.nextDouble();
        double x2 = innerPoint(middle[0] + reach * Math.cos(turn));
        double y2 = innerPoint(middle[1] + reach * Math.sin(turn));
        double kink = turn + 0.9 * (random.nextDouble() - 0.5);
        double x3 = innerPoint(x2 + (1.5 + 1.5 * random.nextDouble()) * Math.cos(kink));
        double y3 = innerPoint(y2 + (1.5 + 1.5 * random.nextDouble()) * Math.sin(kink));
        double w = 0.35 + 0.3 * main;
        segments.add(new double[] {middle[0], middle[1], x2, y2, w, 0.4 * w + 0.12});
        segments.add(new double[] {x2, y2, x3, y3, 0.4 * w + 0.12, 0.1});
        return segments;
    }

    /** Keeps an inner node or branch point {@link #FISSURE_MARGIN} inside the tile, so only the edge stubs reach an edge. */
    private static double innerPoint(double coordinate) {
        return Math.max(FISSURE_MARGIN, Math.min(SIZE - FISSURE_MARGIN, coordinate));
    }

    /** {distance, width there} to the segment nearest relative to its width (no wrap: the stubs meet the edges). */
    private static double[] nearestSegment(List<double[]> segments, double px, double py) {
        double best = Double.MAX_VALUE;
        double bestWidth = 0.1;
        double bestScore = Double.MAX_VALUE;
        for (double[] segment : segments) {
            double dx = segment[2] - segment[0];
            double dy = segment[3] - segment[1];
            double qx = px - segment[0];
            double qy = py - segment[1];
            double t = Math.max(0, Math.min(1, (qx * dx + qy * dy) / (dx * dx + dy * dy)));
            double distance = Math.hypot(qx - t * dx, qy - t * dy);
            double width = lerp(segment[4], segment[5], t);
            if (distance / width < bestScore) {
                bestScore = distance / width;
                best = distance;
                bestWidth = width;
            }
        }
        return new double[] {best, bestWidth};
    }

    /**
     * Throws unless the glowing fissure shows the same edge pattern on all four edges, mirror-symmetric about the
     * middle (glow, halo or crust per pixel of the outer row), so it joins its neighbour in every variant (46b).
     */
    private static void checkFissureEdges() throws IOException {
        BufferedImage image = render("block/glowing_fissure");
        boolean[][] mask = glowMask(image);
        String[] edges = new String[4];
        for (int side = 0; side < 4; side++) {
            StringBuilder pattern = new StringBuilder();
            for (int i = 0; i < SIZE; i++) {
                int x = side == 0 ? 0 : side == 1 ? SIZE - 1 : i;
                int y = side == 2 ? 0 : side == 3 ? SIZE - 1 : i;
                int rgb = image.getRGB(x, y) & 0xFFFFFF;
                pattern.append(mask[x][y] ? (rgb == LAVA[1] ? 'r' : 'g') : rgb == LAVA[0] ? 'c' : rgb == FISSURE_HALO[0] || rgb == FISSURE_HALO[1] ? 'h' : '.');
            }
            edges[side] = pattern.toString();
            if (!edges[side].equals(pattern.reverse().toString()) || !edges[side].equals(edges[0])) {
                throw new IllegalStateException("glowing_fissure edge " + side + " is " + edges[side] + ", edge 0 " + edges[0]
                        + ": the fissure would not join a rotated neighbour");
            }
        }
        System.out.println("glowing_fissure edges: " + edges[0]);
    }

    /**
     * Primordial ice (52): ice from before the corruption, clear and almost white-blue: a few large internal facets with
     * bright upper left edges and soft blue lower right edges, a deep blue core where the broad noise is low, and tiny
     * trapped bubbles. Still (no pulse, contract §7).
     */
    private static void primordialIce(BufferedImage image, Random random) {
        bestOf(image, random, Ring3Textures::primordialIceLayout);
    }

    private static void primordialIceLayout(BufferedImage image, Random random) {
        Cells cells = Cells.random(random, 7);
        double[] base = new double[cells.count()];
        for (int i = 0; i < base.length; i++) {
            base[i] = 0.45 + 0.3 * random.nextDouble();
        }
        double[][] broad = valueNoise(random, 8, 8);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                Cells.Nearest nearest = cells.nearest(x, y);
                double gap = nearest.secondDistance() - nearest.distance();
                double slope = nearest.dx() + nearest.dy();
                int color;
                if (gap < 0.4) {
                    color = slope < 0 ? PRIMORDIAL_ICE[5] : PRIMORDIAL_ICE[2];
                } else {
                    double light = base[nearest.index()] - 0.25 * slope + 0.35 * (broad[x][y] - 0.5) + 0.1 * random.nextDouble();
                    color = broad[x][y] < 0.1 ? PRIMORDIAL_DEEP : shade(PRIMORDIAL_ICE, light);
                }
                set(image, x, y, color);
            }
        }
        for (int i = 0; i < 4; i++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            setWrapped(image, x, y, PRIMORDIAL_ICE[5]);
            setWrapped(image, x, y + 1, PRIMORDIAL_ICE[2]);
        }
    }

    // ---- Particles ----------------------------------------------------------------------------------------------

    /**
     * Light grey particle tones, tinted by the particle class (A, golf 1: ash ~#8A8580, ember orange-yellow and
     * full-bright; steam puff in golf 2): '#' light, 'o' mid, 'x' shadow (for embers: core, soft edge, fringe).
     */
    private static final int[] GREYS = {0xF2F2F2, 0xCFCFCF, 0xA8A8A8};
    /** Own colours (not tinted; golf 2): '#' white, 'o' pale blue, 'x' blue. */
    private static final int[] ICE_SPARK = {0xFFFFFF, 0xCDEBFF, 0x8CC4EE};

    /** Ash flakes, small to large: irregular, slightly curled flakes. */
    private static final String[][] ASH_FLAKES = {
            {"........", "........", "...#o...", "...ox...", "........", "........", "........", "........"},
            {"........", "........", "..##o...", "..#oox..", "...xx...", "........", "........", "........"},
            {"........", "..#o....", ".##oo...", "..#ooxo.", "...oxx..", "....x...", "........", "........"},
            {"........", ".##o....", ".#oo#o..", "..ooooo.", "...#oox.", "...oxx..", "....x...", "........"}};
    /** Embers: a bright core of 1–2 px with a soft edge, small to large. */
    private static final String[][] EMBERS = {
            {"........", "........", "........", "...#....", "........", "........", "........", "........"},
            {"........", "........", "...o....", "..o#o...", "...x....", "........", "........", "........"},
            {"........", "........", "...o....", "..o#o...", "..o#ox..", "...ox...", "........", "........"},
            {"........", "...x....", "..xoo...", ".xo#ox..", ".oo#oo..", "..xoox..", "...x....", "........"}};
    /** Steam puffs: round, lit top left, small to large. */
    private static final String[][] STEAM_PUFFS = {
            {"........", "........", "...##...", "..###o..", "..##ox..", "...xx...", "........", "........"},
            {"........", "..###...", ".####o..", ".###oo..", ".##ooox.", "..oxxx..", "........", "........"},
            {"..##....", ".####.#.", "######o.", "#####oo.", ".###ooox", "..ooooxx", "...xxx..", "........"},
            {".###.##.", "#######o", "######oo", "#####ooo", "####ooox", ".##oooxx", "..ooxxx.", "...xx..."}};
    /** Ice glints: a sparkle cross, small to large. */
    private static final String[][] ICE_GLINTS = {
            {"........", "........", "........", "...#....", "........", "........", "........", "........"},
            {"........", "........", "...o....", "..o#o...", "...o....", "........", "........", "........"},
            {"........", "...x....", "...o....", ".xo#ox..", "...o....", "...x....", "........", "........"},
            {"...x....", "...o....", "..xox...", "xoo#oox.", "..xox...", "...o....", "...x....", "........"}};

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

    // ---- Animation ----------------------------------------------------------------------------------------------

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

    private static BufferedImage animationStrip(String name, BufferedImage rest, Animation animation) {
        return animation.cycling() ? cyclingStrip(name, animation) : pulseStrip(rest, animation);
    }

    // ---- Shared patterns (copied from Ring2Textures.java) -------------------------------------------------------

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

    /** A hand-laid sprite: 16 strings of 16 characters, {@code '.'} transparent, others a colour from {@code legend}. */
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

    /** Opaque pixel inside the tile; outside counts as transparent. */
    private static boolean isOpaque(BufferedImage image, int x, int y) {
        return x >= 0 && y >= 0 && x < SIZE && y < SIZE && (image.getRGB(x, y) >>> 24) != 0;
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

    // ---- Preview ------------------------------------------------------------------------------------------------

    /** Contact sheet layout: cells of one texture each (6× rest frame, 3×3 at 2×, frames at 2×), six per row. */
    private static final int CELL_WIDTH = 350;
    private static final int CELL_HEIGHT = 150;
    private static final int COLUMNS = 6;

    /** 6×6 walls (2×) to judge repetition, and a few scenes; own textures only (vanilla: VanillaCompare). */
    private static final String[] WALLS = {"block/ash_block", "block/ash_crust", "block/pumice", "block/pumice_bricks",
            "block/smoldering_stone", "block/smoldering_ash", "block/charred_log", "block/charred_planks", "block/sulfur_crust",
            "block/cold_lava", "block/rime_netherrack", "block/nether_ice", "block/packed_nether_ice", "block/primordial_ice",
            "block/rimeshroom_cap", "block/glowing_fissure", "block/boiling_mud", "block/yellow_sulfur_mud",
            "block/green_sulfur_mud", "block/orange_sulfur_mud"};

    /** Blocks with the 32 random variants of 46b (rotations and mirroring): drawn as a floor of random orientations. */
    private static final String[] VARIANT_FLOORS = {"block/glowing_fissure", "block/smoldering_stone"};

    /** Blocks drawn at night: the block darkened as by a dark sky, its glow layer (emissive) at full brightness on top. */
    private static final String[] NIGHT = {"block/smoldering_stone", "block/smoldering_ash", "block/ember_leaves",
            "block/glowing_fissure", "block/fire_crystal_block", "block/fire_crystal_cluster"};

    private static final String[] SLOT_SPRITES = {"item/raw_volcanite", "item/sulfur", "item/fire_ruby", "item/fire_crystal",
            "item/charred_door", "item/rimeshroom_door", "item/ice_stalactite", "block/fire_bloom", "block/ash_sprouts",
            "block/ember_berry_bush_stage3", "block/fire_crystal_cluster", "block/large_fire_crystal_bud",
            "block/medium_fire_crystal_bud", "block/small_fire_crystal_bud", "block/sulfur_crystal",
            "block/large_sulfur_crystal_bud", "block/medium_sulfur_crystal_bud", "block/small_sulfur_crystal_bud"};

    private static void writePreview() throws IOException {
        int block = SIZE * 2;
        int wallSize = 6 * block;
        int wallsPerRow = 6;
        int wallRows = (WALLS.length + wallsPerRow - 1) / wallsPerRow;
        int extras = 30 + wallRows * (wallSize + 30) + 30 + 3 * SIZE * 4 + 40 + 30 + 2 * 64 + 20 + 40 + 6 * SIZE * 4 + 20 + 20 + VARIANT_FLOOR_SIZE * SIZE * 3 + 34;
        int names = TEXTURES.size() + 2;
        int rows = (names + COLUMNS - 1) / COLUMNS + 2;
        int width = Math.max(COLUMNS * CELL_WIDTH + 20, 33 * SIZE * 4 + 20);
        int height = extras + rows * CELL_HEIGHT + 20;
        BufferedImage sheet = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = sheet.createGraphics();
        graphics.setColor(new Color(0x2B2B2B));
        graphics.fillRect(0, 0, width, height);
        int top = drawExtras(graphics, 10);
        int index = 0;
        for (Map.Entry<String, Texture> entry : TEXTURES.entrySet()) {
            String name = entry.getKey();
            if (index == 0 || name.equals(NETHER_FIRST)) {
                if (index % COLUMNS != 0) {
                    index += COLUMNS - index % COLUMNS;
                }
                graphics.setColor(new Color(0xFFC857));
                graphics.drawString(index == 0 ? "== RING 3 ==" : "== NETHER RING 3 ==", 10, top + (index / COLUMNS) * CELL_HEIGHT + 12);
                index += COLUMNS;
            }
            int left = 10 + (index % COLUMNS) * CELL_WIDTH;
            int cellTop = top + (index / COLUMNS) * CELL_HEIGHT;
            BufferedImage frame = render(name);
            graphics.setColor(Color.WHITE);
            graphics.drawString(name.replace("block/", "").replace("item/", "item: ").replace("particle/", "particle: ")
                    + (HAND_EDITED.contains(name) ? " (hand)" : ""), left, cellTop + 12);
            int scale = frame.getWidth() == SIZE ? 6 : 12;
            draw(graphics, frame, left, cellTop + 16, scale);
            for (int ty = 0; ty < 3; ty++) {
                for (int tx = 0; tx < 3; tx++) {
                    draw(graphics, frame, left + 104 + tx * frame.getWidth() * 2, cellTop + 16 + ty * frame.getWidth() * 2, 2 * SIZE / frame.getWidth());
                }
            }
            Animation animation = entry.getValue().animation();
            if (animation.animated()) {
                BufferedImage strip = animationStrip(name, frame, animation);
                int size = frame.getWidth();
                int shown = Math.min(animation.frames(), 8);
                for (int i = 0; i < shown; i++) {
                    int frameIndex = i * animation.frames() / shown;
                    BufferedImage single = strip.getSubimage(0, frameIndex * size, size, size);
                    draw(graphics, single, left + 208 + (i % 4) * 34, cellTop + 16 + (i / 4) * 34, 2);
                }
            }
            index++;
        }
        graphics.dispose();
        Files.createDirectories(PREVIEW.getParent());
        ImageIO.write(sheet, "png", PREVIEW.toFile());
        System.out.println("wrote " + PREVIEW);
    }

    /** Walls, the night view and the items in a slot; returns the top of the per-texture cells. */
    private static int drawExtras(Graphics2D graphics, int top) throws IOException {
        int block = SIZE * 2;
        int wallSize = 6 * block;
        graphics.setColor(Color.WHITE);
        graphics.drawString("6x6 walls at 2x (repetition) | lint: " + LINT_WARNINGS.size() + " warning(s), see the console", 10, top + 12);
        top += 20;
        for (int i = 0; i < WALLS.length; i++) {
            int left = 10 + (i % 6) * (wallSize + 24);
            int wallTop = top + (i / 6) * (wallSize + 30);
            graphics.setColor(Color.LIGHT_GRAY);
            graphics.drawString(WALLS[i].replace("block/", ""), left, wallTop + 10);
            BufferedImage tile = render(WALLS[i]);
            for (int ty = 0; ty < 6; ty++) {
                for (int tx = 0; tx < 6; tx++) {
                    draw(graphics, tile, left + tx * block, wallTop + 14 + ty * block, 2);
                }
            }
        }
        top += ((WALLS.length + 5) / 6) * (wallSize + 30) + 10;
        graphics.setColor(Color.WHITE);
        graphics.drawString("night: block at 20 % light, glow layer / whole crystal at full brightness (emissive), 3x3 at 4x", 10, top + 12);
        top += 20;
        for (int i = 0; i < NIGHT.length; i++) {
            // Fully emissive textures (the fire crystal) have no glow layer: the whole texture stays bright.
            BufferedImage night = TEXTURES.containsKey(NIGHT[i] + "_glow") ? atNight(NIGHT[i]) : render(NIGHT[i]);
            for (int ty = 0; ty < 3; ty++) {
                for (int tx = 0; tx < 3; tx++) {
                    drawOn(graphics, night, 10 + i * (3 * SIZE * 4 + 16) + tx * SIZE * 4, top + ty * SIZE * 4, 4, 0x0A0A12);
                }
            }
        }
        top += 3 * SIZE * 4 + 20;
        graphics.setColor(Color.WHITE);
        graphics.drawString("items in a slot at 1x, 2x and 3x (inventory grey)", 10, top + 12);
        top += 20;
        for (int i = 0; i < SLOT_SPRITES.length; i++) {
            int left = 10 + (i % 9) * 128;
            int rowTop = top + (i / 9) * 64;
            BufferedImage sprite = render(SLOT_SPRITES[i]);
            int x = left;
            for (int scale = 1; scale <= 3; scale++) {
                int size = SIZE * scale;
                graphics.setColor(new Color(0xC6C6C6));
                graphics.fillRect(x, rowTop, size + 4, size + 4);
                graphics.setColor(new Color(0x8B8B8B));
                graphics.fillRect(x + 2, rowTop + 2, size, size);
                drawOn(graphics, sprite, x + 2, rowTop + 2, scale, -1);
                x += size + 8;
            }
        }
        top += 2 * 64 + 20;
        graphics.setColor(Color.WHITE);
        graphics.drawString("ember woods scene (day): ash ground, charred trunks, ember leaves, flora, smouldering stone", 10, top + 12);
        top += 20;
        String[][] scene = {
                {"", "block/ember_leaves", "block/ember_leaves", "block/ember_leaves", "", "", "block/ember_leaves", "block/ember_leaves", "block/ember_leaves", ""},
                {"block/ember_leaves", "block/ember_leaves", "block/ember_leaves", "block/ember_leaves", "block/ember_leaves", "block/ember_leaves", "block/ember_leaves", "block/ember_leaves", "block/ember_leaves", "block/ember_leaves"},
                {"", "block/ember_leaves", "block/charred_log", "block/ember_leaves", "", "", "block/ember_leaves", "block/charred_log", "block/ember_leaves", ""},
                {"", "", "block/charred_log", "", "", "", "", "block/charred_log", "", ""},
                {"block/fire_bloom", "block/ash_sprouts", "block/charred_log", "block/ember_berry_bush_stage3", "", "block/ash_sprouts", "block/fire_bloom", "block/charred_log", "block/ash_sprouts", ""},
                {"block/ash_block", "block/ash_block", "block/ash_block", "block/ash_crust", "block/ash_crust", "block/ash_block", "block/smoldering_stone", "block/ash_block", "block/ash_block", "block/pumice"}};
        drawScene(graphics, scene, 10, top, 0x8A6E66, false);
        drawScene(graphics, NETHER_FLOOR, 10 + 11 * SIZE * 4, top, 0x8A6E66, false);
        drawScene(graphics, NETHER_FLOOR, 10 + 22 * SIZE * 4, top, 0x0A0A12, true);
        graphics.setColor(Color.WHITE);
        graphics.drawString("Nether ring 3 floor (day | night): cold lava, ice, rime netherrack, a line of glowing fissures", 10 + 11 * SIZE * 4, top - 8);
        top += scene.length * SIZE * 4 + 20;
        graphics.setColor(Color.WHITE);
        graphics.drawString("46b variants: 8x8 floors of random rotations/mirrorings at 3x (day | night)", 10, top + 12);
        top += 20;
        int floor = VARIANT_FLOOR_SIZE * SIZE * 3;
        for (int i = 0; i < VARIANT_FLOORS.length; i++) {
            for (int n = 0; n < 2; n++) {
                int left = 10 + (2 * i + n) * (floor + 20);
                graphics.setColor(Color.LIGHT_GRAY);
                graphics.drawString(VARIANT_FLOORS[i].replace("block/", "") + (n == 0 ? " (day)" : " (night)"), left, top + 10);
                drawVariantFloor(graphics, VARIANT_FLOORS[i], left, top + 14, n == 1);
            }
        }
        top += floor + 34;
        return top;
    }

    private static final int VARIANT_FLOOR_SIZE = 8;

    /**
     * A floor of {@code name} in random orientations (one of the 4 rotations, mirrored or not, as the 32 variants of 46b
     * show a face), from a fixed seed, at 3x; at {@code night} darkened with the glow layer on top.
     */
    private static void drawVariantFloor(Graphics2D graphics, String name, int left, int top, boolean night) throws IOException {
        Random random = seeded(name + "#floor");
        BufferedImage tile = night ? atNight(name) : render(name);
        for (int ty = 0; ty < VARIANT_FLOOR_SIZE; ty++) {
            for (int tx = 0; tx < VARIANT_FLOOR_SIZE; tx++) {
                drawOn(graphics, oriented(tile, random.nextInt(8)), left + tx * SIZE * 3, top + ty * SIZE * 3, 3, 0x0A0A12);
            }
        }
    }

    /** {@code image} turned {@code orientation % 4} quarter turns clockwise, mirrored left-right first when {@code orientation >= 4}. */
    private static BufferedImage oriented(BufferedImage image, int orientation) {
        BufferedImage result = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int sx = orientation >= 4 ? SIZE - 1 - x : x;
                int sy = y;
                for (int turn = 0; turn < orientation % 4; turn++) {
                    int t = sx;
                    sx = sy;
                    sy = SIZE - 1 - t;
                }
                result.setRGB(x, y, image.getRGB(sx, sy));
            }
        }
        return result;
    }

    /** A Nether ring 3 floor seen from above, with fissure blocks laid in a line as a feature would. */
    private static final String[][] NETHER_FLOOR = {
            {"block/cold_lava", "block/cold_lava", "block/nether_ice", "block/nether_ice", "block/cold_lava", "block/rime_netherrack", "block/cold_lava", "block/cold_lava", "block/nether_ice", "block/nether_ice"},
            {"block/cold_lava", "block/glowing_fissure", "block/glowing_fissure", "block/nether_ice", "block/cold_lava", "block/cold_lava", "block/cold_lava", "block/nether_ice", "block/nether_ice", "block/packed_nether_ice"},
            {"block/rime_netherrack", "block/cold_lava", "block/glowing_fissure", "block/glowing_fissure", "block/glowing_fissure", "block/cold_lava", "block/nether_ice", "block/nether_ice", "block/packed_nether_ice", "block/packed_nether_ice"},
            {"block/rime_netherrack", "block/cold_lava", "block/cold_lava", "block/cold_lava", "block/glowing_fissure", "block/glowing_fissure", "block/cold_lava", "block/nether_ice", "block/nether_ice", "block/primordial_ice"},
            {"block/rime_netherrack", "block/rime_netherrack", "block/cold_lava", "block/cold_lava", "block/cold_lava", "block/glowing_fissure", "block/cold_lava", "block/cold_lava", "block/nether_ice", "block/nether_ice"},
            {"block/rime_netherrack", "block/rime_netherrack", "block/rime_netherrack", "block/cold_lava", "block/cold_lava", "block/cold_lava", "block/cold_lava", "block/rime_netherrack", "block/cold_lava", "block/nether_ice"}};

    /** Draws a grid of textures at 4x; at {@code night} blocks are darkened and glow layers drawn on top at full brightness. */
    private static void drawScene(Graphics2D graphics, String[][] scene, int left0, int top, int backdrop, boolean night) throws IOException {
        for (int ty = 0; ty < scene.length; ty++) {
            for (int tx = 0; tx < scene[ty].length; tx++) {
                int left = left0 + tx * SIZE * 4;
                int rowTop = top + ty * SIZE * 4;
                graphics.setColor(new Color(backdrop));
                graphics.fillRect(left, rowTop, SIZE * 4, SIZE * 4);
                if (!scene[ty][tx].isEmpty()) {
                    drawOn(graphics, night ? atNight(scene[ty][tx]) : render(scene[ty][tx]), left, rowTop, 4, -1);
                }
            }
        }
    }

    /** The block darkened as by a dark sky, its glow layer (emissive) at full brightness on top. */
    private static BufferedImage atNight(String name) throws IOException {
        BufferedImage base = render(name);
        String glowName = name + "_glow";
        BufferedImage glow = TEXTURES.containsKey(glowName) ? render(glowName) : null;
        BufferedImage night = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (glow != null && isOpaque(glow, x, y)) {
                    night.setRGB(x, y, glow.getRGB(x, y));
                } else if (isOpaque(base, x, y)) {
                    set(night, x, y, mix(base.getRGB(x, y) & 0xFFFFFF, 0x000000, 0.8));
                }
            }
        }
        return night;
    }

    /** Draws opaque pixels only; transparent pixels show {@code backdrop} (or what is already there when it is -1). */
    private static void drawOn(Graphics2D graphics, BufferedImage image, int left, int top, int scale, int backdrop) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                if (argb >>> 24 != 0) {
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

    /** Draws a texture; transparency over a dusty grey-brown backdrop (the ash plain at dusk). */
    private static void draw(Graphics2D graphics, BufferedImage image, int left, int top, int scale) {
        drawOn(graphics, image, left, top, scale, 0x5E4A44);
    }

    // ---- Helpers (copied from Ring2Textures.java) ---------------------------------------------------------------

    private static Random seeded(String name) {
        return new Random(name.hashCode());
    }

    /** Tileable value noise in [0, 1): random values on a grid of {@code cellX} × {@code cellY} pixels, smoothly interpolated. */
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

    private Ring3Textures() {
    }
}
